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
    $ReleaseRoot = Join-Path $RepoRoot '..\..\JAR'
}

$utf8 = [System.Text.Encoding]::UTF8
$fabricProperties = [System.IO.File]::ReadAllText(
    (Join-Path $RepoRoot 'fabric\gradle.properties'), $utf8)
$forgeProperties = [System.IO.File]::ReadAllText(
    (Join-Path $RepoRoot 'forge\gradle.properties'), $utf8)
if ($fabricProperties -notmatch '(?m)^mod_version=([^\r\n]+)\r?$') {
    throw '无法从 fabric/gradle.properties 读取 mod_version'
}
$fabricVersion = $Matches[1].Trim()
if ($forgeProperties -notmatch '(?m)^mod_version=([^\r\n]+)\r?$') {
    throw '无法从 forge/gradle.properties 读取 mod_version'
}
$forgeVersion = $Matches[1].Trim()
$fabricJar = Join-Path $ReleaseRoot (
    'summy-enchantment\summy-enchantment-{0}.jar' -f $fabricVersion)
$forgeJar = Join-Path $ReleaseRoot (
    'summy-enchantment-forge\summy-enchantment-{0}.jar' -f $forgeVersion)
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

function Get-ZipEntrySha256([string]$JarPath, [string]$EntryName) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $zip.GetEntry($EntryName)
        if ($null -eq $entry) {
            return $null
        }
        $stream = $entry.Open()
        try {
            $sha = [System.Security.Cryptography.SHA256]::Create()
            try {
                return ([System.BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-', '')
            } finally {
                $sha.Dispose()
            }
        } finally {
            $stream.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
}

Check-Release 'Fabric JAR exists' (Test-Path -LiteralPath $fabricJar) $fabricJar
Check-Release 'Forge JAR exists' (Test-Path -LiteralPath $forgeJar) $forgeJar

$expectedIconSha256 = '4B1FC2FE52542FB50D343B165CB07872D3EAEB9D57F79500AD253381186C212F'
$releaseNotePath = Join-Path $RepoRoot ('docs\releases\{0}.md' -f $fabricVersion)
Check-Release 'Release notes exists' (Test-Path -LiteralPath $releaseNotePath) $releaseNotePath
$releaseNotes = if (Test-Path -LiteralPath $releaseNotePath) {
    [System.IO.File]::ReadAllText($releaseNotePath, $utf8)
} else {
    ''
}
$compatibility = [System.IO.File]::ReadAllText((Join-Path $RepoRoot 'docs\compatibility.md'), $utf8)
Check-Release 'Fabric source version' (-not [string]::IsNullOrWhiteSpace($fabricVersion)) $fabricVersion
Check-Release 'Forge source version' (-not [string]::IsNullOrWhiteSpace($forgeVersion)) $forgeVersion
Check-Release 'Release notes scale' ($releaseNotes -match '32.*99.*11') '32/99/11'
Check-Release 'Release notes verification' ($releaseNotes -match 'Forge.*19.*0') 'Forge 19/0'
Check-Release 'Compatibility record' ($compatibility -match 'Forge.*19.*0') 'Forge 19/0 client check'
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
            Check-Release 'Fabric version' ($json.version -eq $fabricVersion) $json.version
            Check-Release 'Fabric license' ($json.license -eq 'MIT') $json.license
            Check-Release 'Fabric icon metadata' ($json.icon -eq 'assets/summy_enchantment/icon.png') $json.icon
            $iconSha = Get-ZipEntrySha256 $pair.Path 'assets/summy_enchantment/icon.png'
            Check-Release 'Fabric embedded icon' ($iconSha -eq $expectedIconSha256) $iconSha
        } else {
            Check-Release 'Forge version' (
                $metadataText -match ('version\s*=\s*"{0}"' -f [regex]::Escape($forgeVersion))
            ) $forgeVersion
            Check-Release 'Forge license' ($metadataText -match 'license\s*=\s*"MIT"') 'MIT'
            Check-Release 'Forge icon metadata' ($metadataText -match 'logoFile\s*=\s*"icon\.png"') 'icon.png'
            $iconSha = Get-ZipEntrySha256 $pair.Path 'icon.png'
            Check-Release 'Forge embedded icon' ($iconSha -eq $expectedIconSha256) $iconSha
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
