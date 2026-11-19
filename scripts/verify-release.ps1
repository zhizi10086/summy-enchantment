[CmdletBinding()]
param(
    [string]$RepoRoot = '',
    [string]$FabricJar = '',
    [string]$ForgeJar = ''
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $scriptRoot = $PSScriptRoot
    if ([string]::IsNullOrWhiteSpace($scriptRoot)) {
        $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
    }
    $RepoRoot = Split-Path -Parent $scriptRoot
}
if ([string]::IsNullOrWhiteSpace($FabricJar) -or [string]::IsNullOrWhiteSpace($ForgeJar)) {
    throw 'Specify this build''s candidates with -FabricJar and -ForgeJar.'
}
$fabricJar = (Resolve-Path -LiteralPath $FabricJar).ProviderPath
$forgeJar = (Resolve-Path -LiteralPath $ForgeJar).ProviderPath
if ($fabricJar -eq $forgeJar) {
    throw 'Provide a distinct candidate JAR for each loader.'
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
if ($fabricProperties -notmatch '(?m)^minecraft_version=([^\r\n]+)\r?$') {
    throw '无法从 fabric/gradle.properties 读取 minecraft_version'
}
$fabricMinecraftVersion = $Matches[1].Trim()
if ($forgeProperties -notmatch '(?m)^mod_version=([^\r\n]+)\r?$') {
    throw '无法从 forge/gradle.properties 读取 mod_version'
}
$forgeVersion = $Matches[1].Trim()
if ($forgeProperties -notmatch '(?m)^minecraft_version=([^\r\n]+)\r?$') {
    throw '无法从 forge/gradle.properties 读取 minecraft_version'
}
$forgeMinecraftVersion = $Matches[1].Trim()
if ($fabricMinecraftVersion -ne $forgeMinecraftVersion) {
    throw "Fabric 与 Forge 的 minecraft_version 不一致：$fabricMinecraftVersion / $forgeMinecraftVersion"
}
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
Check-Release 'NeoForge JAR exists' (Test-Path -LiteralPath $forgeJar) $forgeJar

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
Check-Release 'NeoForge source version' (-not [string]::IsNullOrWhiteSpace($forgeVersion)) $forgeVersion
Check-Release 'Release notes scale' ($releaseNotes -match '32.*99.*11') '32/99/11'
Check-Release 'Release notes verification' ($releaseNotes -match '(?s)(?:Forge|NeoForge)[^\r\n]*(?:\d+/0|0 failures)') 'platform x/0'
Check-Release 'Compatibility record' ($compatibility -match '(?s)(?:Forge|NeoForge)[^\r\n]*(?:\d+/0|0 failures)') 'platform x/0'
Check-Release 'No local absolute paths in public docs' (-not (($releaseNotes + $compatibility) -match '[A-Z]:\\')) 'docs/releases + docs/compatibility'

if ($failures.Count -eq 0) {
    foreach ($pair in @(
            @{Loader = 'Fabric'; Path = $fabricJar; Metadata = 'fabric.mod.json'},
            @{Loader = 'NeoForge'; Path = $forgeJar; Metadata = 'META-INF/neoforge.mods.toml'})) {
        $entries = @(& jar tf $pair.Path)
        $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $pair.Path).Hash
        $devcheckCount = @($entries | Where-Object { $_ -like '*devcheck*' }).Count
        $externalCount = @($entries | Where-Object {
            $_ -match '(?i)^(celestial|apotheosis|majrusz|dev/shadowsoffire|dev/latvian|com/summy/reliquary|com/notunanancyowen/spears)'
        }).Count
        $licenseCount = @($entries | Where-Object { $_ -eq 'LICENSE_summy-enchantment' }).Count
        $metadataText = Get-ZipEntryText $pair.Path $pair.Metadata

        Check-Release "$($pair.Loader) metadata exists" ($null -ne $metadataText) $pair.Metadata
        $mainMixin = 'summy_enchantment.mixins.json'
        $spearMixin = 'summy_enchantment.spears.mixins.json'
        $srMixin = 'summy_enchantment.summy_reliquary.mixins.json'
        $kubePlugin = 'com/summy/enchantment/compat/kubejs/SummyEnchantmentKubeJSPlugin.class'
        $srClasses = @($entries | Where-Object { $_ -match 'SummyReliquary.+\.class$' })
        $hasKubePlugin = $entries -contains 'kubejs.plugins.txt' -and $entries -contains $kubePlugin
        $hasSpear = $entries -contains 'com/summy/enchantment/mixin/SpearPiercingWeaponCompatMixin.class' -and
            $entries -contains 'com/summy/enchantment/mixin/SpearKineticWeaponCompatMixin.class'
        Check-Release "$($pair.Loader) complete Mixin set" (
            $entries -contains $mainMixin -and $entries -contains $spearMixin -and $hasSpear -and
            ([string]$metadataText).Contains($mainMixin) -and ([string]$metadataText).Contains($spearMixin)
        ) 'main + Spears'
        if ($pair.Loader -eq 'Fabric') {
            Check-Release 'Fabric excludes SR and KubeJS hooks' (
                $entries -notcontains $srMixin -and $srClasses.Count -eq 0 -and
                -not ([string]$metadataText).Contains($srMixin) -and
                $entries -notcontains 'kubejs.plugins.txt' -and $entries -notcontains $kubePlugin
            ) 'platform package'
        } else {
            Check-Release 'NeoForge has SR and KubeJS hooks' (
                $hasKubePlugin -and $entries -contains $srMixin -and
                ([string]$metadataText).Contains($srMixin) -and
                $entries -contains 'com/summy/enchantment/mixin/SummyReliquaryShadowDashMixin.class' -and
                $entries -contains 'com/summy/enchantment/mixin/SummyReliquaryThrownSpearMixin.class'
            ) 'platform package'
        }
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
            Check-Release 'NeoForge version' (
                $metadataText -match ('version\s*=\s*"{0}"' -f [regex]::Escape($forgeVersion))
            ) $forgeVersion
            Check-Release 'NeoForge license' ($metadataText -match 'license\s*=\s*"MIT"') 'MIT'
            Check-Release 'NeoForge icon metadata' ($metadataText -match 'logoFile\s*=\s*"icon\.png"') 'icon.png'
            $iconSha = Get-ZipEntrySha256 $pair.Path 'icon.png'
            Check-Release 'NeoForge embedded icon' ($iconSha -eq $expectedIconSha256) $iconSha
            # NeoForge 在运行时提供 MixinExtras，不需要把实现嵌套进模组 JAR。
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
