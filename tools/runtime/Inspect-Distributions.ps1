#requires -Version 7.3
param([string]$Output = 'build/validation/packaging.json')
$ErrorActionPreference = 'Stop'
function Get-ArchivePayloadHash([IO.Compression.ZipArchive]$Archive) {
    $digest = [Security.Cryptography.IncrementalHash]::CreateHash([Security.Cryptography.HashAlgorithmName]::SHA256)
    try {
        [string[]]$names = @($Archive.Entries | Where-Object { !$_.FullName.EndsWith('/') } | ForEach-Object FullName)
        [Array]::Sort($names, [StringComparer]::Ordinal)
        foreach ($name in $names) {
            $digest.AppendData([Text.Encoding]::UTF8.GetBytes($name))
            $digest.AppendData([byte[]]@(0))
            $stream = $Archive.GetEntry($name).Open()
            try { $digest.AppendData([Security.Cryptography.SHA256]::HashData($stream)) }
            finally { $stream.Dispose() }
        }
        return [Convert]::ToHexString($digest.GetHashAndReset()).ToLowerInvariant()
    } finally { $digest.Dispose() }
}
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$targets = @(
    @{ Path='build/libs/Lorax-6.2.jar'; Platform='paper'; Major=69 },
    @{ Path='spigotModern/build/libs/LoraxCustomTrees-spigot-mc1.20-26.3-6.2.jar'; Platform='spigot'; Major=61 },
    @{ Path='spigot112/build/libs/LoraxCustomTrees-spigot-1.12.2-6.2.jar'; Platform='spigot'; Major=52 },
    @{ Path='forge112/build/libs/LoraxCustomTrees-forge-1.12.2-6.2.jar'; Platform='forge'; Major=52 },
    @{ Path='forge12110/build/libs/LoraxCustomTrees-forge-1.21.10-6.2.jar'; Platform='forge'; Major=65 },
    @{ Path='forge120/build/libs/LoraxCustomTrees-forge-1.20-6.2.jar'; Platform='forge'; Major=61 },
    @{ Path='forge1206/build/libs/LoraxCustomTrees-forge-1.20.6-6.2.jar'; Platform='forge'; Major=65 },
    @{ Path='forge121/build/libs/LoraxCustomTrees-forge-1.21-6.2.jar'; Platform='forge'; Major=65 },
    @{ Path='forge261/build/libs/LoraxCustomTrees-forge-26.1-6.2.jar'; Platform='forge'; Major=69 },
    @{ Path='forge262/build/libs/LoraxCustomTrees-forge-26.2-6.2.jar'; Platform='forge'; Major=69 },
    @{ Path='forge26/build/libs/LoraxCustomTrees-forge-26.3-6.2.jar'; Platform='forge'; Major=69 }
)
$results = foreach ($target in $targets) {
    $path = Join-Path $root $target.Path
    if (!(Test-Path -LiteralPath $path)) { throw "Missing distribution: $path" }
    $archive = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        $descriptor = if ($target.Platform -ne 'forge') { 'plugin.yml' }
                      elseif ($target.Path.StartsWith('forge112/')) { 'mcmod.info' }
                      else { 'META-INF/mods.toml' }
        if (!$archive.GetEntry($descriptor)) { throw "Missing platform descriptor: $path / $descriptor" }
        if ($target.Platform -eq 'forge' -and $archive.GetEntry('plugin.yml')) { throw "Bukkit descriptor in Forge distribution: $path" }
        $assets = @($archive.Entries | Where-Object FullName -Match '^[^/]+\.bo5$')
        if ($assets.Count -ne 40) { throw "$path contains $($assets.Count) assets, expected 40" }
        foreach ($entry in $assets) {
            $stream = $entry.Open()
            try { $actual = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($stream)) }
            finally { $stream.Dispose() }
            $expected = (Get-FileHash -LiteralPath (Join-Path $root "src/main/resources/$($entry.FullName)")).Hash
            if ($actual -ne $expected) { throw "Altered canonical asset: $path / $($entry.FullName)" }
        }
        $maximum = 0
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -match '^(org/bukkit/|io/papermc/|net/minecraftforge/|net/minecraft/|com/sk89q/worldedit/)') {
                throw "Bundled platform API: $path / $($entry.FullName)"
            }
            if ($entry.FullName -match '/probe/') { throw "Bundled test probe: $path / $($entry.FullName)" }
            if ($entry.FullName.EndsWith('.class')) {
                $stream = $entry.Open()
                try {
                    $header = [byte[]]::new(8)
                    $stream.ReadExactly($header)
                    $major = [int]$header[6] * 256 + $header[7]
                    if ($major -gt $target.Major) { throw "Incompatible bytecode $major in $path / $($entry.FullName)" }
                    $maximum = [Math]::Max($major, $maximum)
                } finally { $stream.Dispose() }
            }
        }
        if ($maximum -ne $target.Major) { throw "Unexpected target bytecode: $path ($maximum)" }
        [pscustomobject][ordered]@{ artifact=$target.Path; platform=$target.Platform; sha256=(Get-FileHash $path).Hash.ToLowerInvariant(); payload_sha256=(Get-ArchivePayloadHash $archive); assets=40; bytecode_major=$maximum; descriptor=$descriptor; packaged_api=$false; packaged_probe=$false }
    } finally { $archive.Dispose() }
}
$destination = [IO.Path]::GetFullPath((Join-Path $root $Output))
New-Item -ItemType Directory -Force (Split-Path $destination) | Out-Null
@{ checked_at=[DateTime]::UtcNow.ToString('o'); payload_hash_format='SHA256 of ordinal-sorted UTF8(entry name), NUL, SHA256(uncompressed entry bytes), excluding directory entries'; distributions=@($results) } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $destination
$results | Format-Table artifact,assets,bytecode_major
