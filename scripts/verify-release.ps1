[CmdletBinding()]
param(
    [string]$RepoRoot = '',
    [string]$ReleaseRoot = ''
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $scriptRoot = $PSScriptRoot
    if ([string]::IsNullOrWhiteSpace($scriptRoot)) {
        $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
    }
    $RepoRoot = Split-Path -Parent $scriptRoot
}
if ([string]::IsNullOrWhiteSpace($ReleaseRoot)) {
    $ReleaseRoot = Join-Path $RepoRoot 'JAR'
}

$fabricJar = Join-Path $ReleaseRoot 'summy-enchantment\summy-enchantment-1.3.10.jar'
$forgeJar = Join-Path $ReleaseRoot 'summy-enchantment-forge\summy-enchantment-1.3.10-forge.jar'
$failures = [System.Collections.Generic.List[string]]::new()
$checks = 0

function Check-Release([string]$Label, [bool]$Ok, [string]$Detail) {
    $script:checks++
    if ($Ok) {
        Write-Host ("OK   {0}: {1}" -f $Label, $Detail) -ForegroundColor Green
    } else {
        $script:failures.Add(("{0}: {1}" -f $Label, $Detail))
        Write-Host ("FAIL {0}: {1}" -f $Label, $Detail) -ForegroundColor Red
    }
}

function Get-ZipEntryText([string]$JarPath, [string]$EntryName) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $zip.GetEntry($EntryName)
        if ($null -eq $entry) {
            return $null
        }
        $reader = [System.IO.StreamReader]::new($entry.Open())
        try {
            return $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
}

Check-Release 'Fabric JAR exists' (Test-Path -LiteralPath $fabricJar) $fabricJar
Check-Release 'Forge JAR exists' (Test-Path -LiteralPath $forgeJar) $forgeJar

$utf8 = [System.Text.Encoding]::UTF8
$fabricProperties = [System.IO.File]::ReadAllText((Join-Path $RepoRoot 'fabric\gradle.properties'), $utf8)
$forgeProperties = [System.IO.File]::ReadAllText((Join-Path $RepoRoot 'forge\gradle.properties'), $utf8)
$releaseNotes = [System.IO.File]::ReadAllText((Join-Path $RepoRoot 'docs\releases\1.3.10.md'), $utf8)
$compatibility = [System.IO.File]::ReadAllText((Join-Path $RepoRoot 'docs\compatibility.md'), $utf8)
Check-Release 'Fabric source version' ($fabricProperties -match '(?m)^mod_version=1\.3\.10$') '1.3.10'
Check-Release 'Forge source version' ($forgeProperties -match '(?m)^mod_version=1\.3\.10-forge$') '1.3.10-forge'
Check-Release 'Release notes scale' ($releaseNotes -match '32.*99.*11') '32/99/11'
Check-Release 'Release notes verification' ($releaseNotes -match 'Forge.*12.*0' -and $releaseNotes -match 'Fabric.*4.*0') 'Forge 12/0, Fabric 4/0'
Check-Release 'Compatibility record' ($compatibility -match 'Forge.*12.*0' -and $compatibility -match 'Fabric.*4.*0') 'Forge/Fabric client checks'
Check-Release 'No local absolute paths in public docs' (-not (($releaseNotes + $compatibility) -match '[A-Z]:\\')) 'docs/releases + docs/compatibility'

if ($failures.Count -eq 0) {
    foreach ($pair in @(
            @{Loader = 'Fabric'; Path = $fabricJar; Metadata = 'fabric.mod.json'},
            @{Loader = 'Forge'; Path = $forgeJar; Metadata = 'META-INF/mods.toml'})) {
        $entries = @(& jar tf $pair.Path)
        $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $pair.Path).Hash
        $devcheckCount = @($entries | Where-Object { $_ -like '*devcheck*' }).Count
        $externalCount = @($entries | Where-Object { $_ -match '(?i)^(celestial|apotheosis|majrusz)' }).Count
        $licenseCount = @($entries | Where-Object { $_ -eq 'LICENSE_summy-enchantment' }).Count
        $metadataText = Get-ZipEntryText $pair.Path $pair.Metadata

        Check-Release "$($pair.Loader) metadata exists" ($null -ne $metadataText) $pair.Metadata
        Check-Release "$($pair.Loader) no devcheck" ($devcheckCount -eq 0) "entries=$devcheckCount"
        Check-Release "$($pair.Loader) no external namespaces" ($externalCount -eq 0) "entries=$externalCount"
        Check-Release "$($pair.Loader) contains MIT license" ($licenseCount -eq 1) "entries=$licenseCount"
        if ($pair.Loader -eq 'Fabric') {
            $json = $metadataText | ConvertFrom-Json
            Check-Release 'Fabric mod id' ($json.id -eq 'summy_enchantment') $json.id
            Check-Release 'Fabric version' ($json.version -eq '1.3.10') $json.version
            Check-Release 'Fabric license' ($json.license -eq 'MIT') $json.license
        } else {
            Check-Release 'Forge version' ($metadataText -match 'version\s*=\s*"1\.3\.10-forge"') '1.3.10-forge'
            Check-Release 'Forge license' ($metadataText -match 'license\s*=\s*"MIT"') 'MIT'
            $mixinExtras = @($entries | Where-Object { $_ -like 'META-INF/jarjar/mixinextras*' }).Count
            Check-Release 'Forge nested MixinExtras' ($mixinExtras -ge 1) "entries=$mixinExtras"
        }

        Write-Host ("     File={0}" -f (Split-Path -Leaf $pair.Path))
        Write-Host ("     Size={0}" -f (Get-Item -LiteralPath $pair.Path).Length)
        Write-Host ("     SHA256={0}" -f $hash)
    }
}

Write-Host ''
if ($failures.Count -eq 0) {
    Write-Host ("Release verification passed: {0} checks, 0 failures." -f $checks) -ForegroundColor Green
    exit 0
}

Write-Host ("Release verification failed: {0} checks, {1} failures." -f $checks, $failures.Count) -ForegroundColor Red
$failures | ForEach-Object { Write-Host ("FAILED: {0}" -f $_) -ForegroundColor Red }
exit 1
