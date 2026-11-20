#requires -Version 7.3
param(
    [Parameter(Mandatory=$true)][string]$Minecraft,
    [Parameter(Mandatory=$true)][string]$ForgeBuild,
    [Parameter(Mandatory=$true)][string]$Directory,
    [Parameter(Mandatory=$true)][string]$Java,
    [Parameter(Mandatory=$true)][string]$Mod,
    [Parameter(Mandatory=$true)][string]$Probe,
    [string]$LibraryCache = ''
)
$ErrorActionPreference = 'Stop'
if ($Minecraft -notmatch '^[0-9]+\.[0-9]+(\.[0-9]+)?$' -or $ForgeBuild -notmatch '^[0-9]+(\.[0-9]+)+$') { throw 'Invalid Minecraft or Forge version' }
$server = [IO.Path]::GetFullPath($Directory)
$javaPath = (Resolve-Path -LiteralPath $Java).Path
$modPath = (Resolve-Path -LiteralPath $Mod).Path
$probePath = (Resolve-Path -LiteralPath $Probe).Path
if ((Test-Path "$server/world") -or (Test-Path "$server/lorax-test")) { throw 'Use a fresh disposable server directory' }
if ((Test-Path $server) -and (Get-ChildItem -LiteralPath $server -Force) -and !(Test-Path "$server/.lorax-test-prepared")) { throw 'Refusing a nonempty directory not created by this test preparer' }
New-Item -ItemType Directory -Force "$server/mods" | Out-Null
Set-Content "$server/.lorax-test-prepared" 'Disposable Lorax dedicated-server integration test'
if ($LibraryCache) {
    $cache = (Resolve-Path -LiteralPath $LibraryCache).Path
    New-Item -ItemType Directory -Force "$server/libraries" | Out-Null
    Get-ChildItem -LiteralPath $cache | Copy-Item -Destination "$server/libraries" -Recurse -Force
    Write-Output "Reusing library cache $cache; Forge installer will verify its library checksums."
}
$version = "$Minecraft-$ForgeBuild"
$url = "https://maven.minecraftforge.net/net/minecraftforge/forge/$version/forge-$version-installer.jar"
Invoke-WebRequest $url -OutFile "$server/installer.jar" -TimeoutSec 180
@{ url=$url; sha256=(Get-FileHash "$server/installer.jar").Hash.ToLowerInvariant(); minecraft=$Minecraft; forge=$ForgeBuild } | ConvertTo-Json | Set-Content "$server/server-source.json"
Push-Location $server
try {
    & $javaPath -jar installer.jar --installServer $server *> installer-console.log
    if ($LASTEXITCODE -ne 0) { throw "Forge installation failed (exit $LASTEXITCODE); inspect $server/installer-console.log" }
} finally { Pop-Location }
Copy-Item -LiteralPath $modPath,$probePath -Destination "$server/mods/" -Force
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
$shim = "$server/forge-$version-shim.jar"
$legacy = "$server/forge-$version.jar"
$argumentName = if ($IsWindows) { 'win_args.txt' } else { 'unix_args.txt' }
$arguments = "libraries/net/minecraftforge/forge/$version/$argumentName"
if (Test-Path $shim) { $launch = @{ jar="forge-$version-shim.jar" } }
elseif (Test-Path "$server/$arguments") { $launch = @{ argument_file=$arguments } }
elseif (Test-Path $legacy) { $launch = @{ jar="forge-$version.jar" } }
else { throw "Forge installer produced no supported launcher; inspect $server/installer-console.log" }
$launch | ConvertTo-Json | Set-Content "$server/server-launch.json"
Write-Output "Prepared Forge $version at $server. Test preparation accepts Minecraft's EULA. Run Test-Paper.ps1 -LaunchManifest server-launch.json -ResultPath lorax-test/result.json."
