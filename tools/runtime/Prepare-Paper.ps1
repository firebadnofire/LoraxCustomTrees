#requires -Version 7.3
param(
    [Parameter(Mandatory=$true)][string]$Minecraft,
    [Parameter(Mandatory=$true)][string]$Directory,
    [Parameter(Mandatory=$true)][string]$Plugin,
    [Parameter(Mandatory=$true)][string]$Probe,
    [string]$Build = 'latest'
)
$ErrorActionPreference = 'Stop'
if ($Minecraft -notmatch '^[0-9]+\.[0-9]+(\.[0-9]+)?$' -or $Build -notmatch '^([0-9]+|latest)$') { throw 'Invalid Minecraft version or Paper build' }
$server = [IO.Path]::GetFullPath($Directory)
if ((Test-Path "$server/world") -or (Test-Path "$server/plugins/LoraxIntegrationProbe/result.json")) {
    throw 'Use a fresh disposable directory; existing world or probe results found'
}
New-Item -ItemType Directory -Force "$server/plugins" | Out-Null
$metadata = Invoke-RestMethod -Headers @{'User-Agent'='Lorax-Compatibility-Tests/1.0 (local development)'} "https://fill.papermc.io/v3/projects/paper/versions/$Minecraft/builds/$Build"
$download = $metadata.downloads.'server:default'
if (!$download.url -or !$download.checksums.sha256) { throw 'Paper API returned no checksummed server download' }
$metadata | ConvertTo-Json -Depth 10 | Set-Content "$server/server-source.json"
if (!(Test-Path "$server/server.jar") -or (Get-FileHash "$server/server.jar").Hash.ToLowerInvariant() -ne $download.checksums.sha256) {
    Invoke-WebRequest $download.url -OutFile "$server/server.jar"
}
if ((Get-FileHash "$server/server.jar").Hash.ToLowerInvariant() -ne $download.checksums.sha256) { throw 'Paper SHA-256 mismatch' }
Copy-Item -LiteralPath $Plugin,$Probe -Destination "$server/plugins/" -Force
Set-Content "$server/eula.txt" 'eula=true'
@'
server-ip=127.0.0.1
server-port=25587
online-mode=false
white-list=true
max-players=1
view-distance=2
simulation-distance=2
spawn-protection=0
enable-rcon=false
level-seed=8675309
'@ | Set-Content "$server/server.properties"
Write-Output "Prepared Paper $Minecraft build $($metadata.id), SHA-256 verified, at $server"
