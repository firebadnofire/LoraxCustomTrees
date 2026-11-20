#requires -Version 7.3
param([string]$Directory, [string]$Java, [int]$Timeout = 300,
      [string]$Jar = 'server.jar', [string]$ResultPath = 'plugins/LoraxIntegrationProbe/result.json', [string]$ArgumentFile = '', [string]$LaunchManifest = '', [string]$ClientVersion = '', [switch]$Restart)
$ErrorActionPreference = 'Stop'
$server = [IO.Path]::GetFullPath($Directory)
if (!(Test-Path "$server/server.properties")) { throw 'Prepare a disposable server directory first' }
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = $listener.LocalEndpoint.Port
$listener.Stop()
$properties = Get-Content "$server/server.properties" -Raw
$properties = $properties -replace '(?m)^server-port=.*$', "server-port=$port"
$properties = $properties -replace '(?m)^server-ip=.*$', 'server-ip=127.0.0.1'
if ($ClientVersion) { $properties = $properties -replace '(?m)^max-players=.*$', 'max-players=2' }
$properties | Set-Content "$server/server.properties"
if ($Restart) {
    if ($ResultPath -ne 'plugins/LoraxIntegrationProbe/result.json') { throw 'Restart mode requires the Paper probe' }
    $probe = "$server/plugins/LoraxIntegrationProbe"
    if (!(Test-Path "$probe/restart.properties") -or !(Test-Path "$probe/result.json")) { throw 'Restart mode requires a successful snapshot-producing first run' }
    $previous = Get-Content "$probe/result.json" -Raw | ConvertFrom-Json
    if (!$previous.passed -or $previous.restart_check) { throw 'Restart mode requires a successful initial generation run' }
    if (Test-Path "$probe/initial-result.json") { throw 'Restart check already attempted; use another disposable server' }
    foreach ($log in @('server-console.log', 'server-stderr.log', 'runner-result.json')) {
        if (Test-Path "$server/$log") { Copy-Item -LiteralPath "$server/$log" -Destination "$server/initial-$log" }
    }
    Move-Item -LiteralPath "$probe/result.json" -Destination "$probe/initial-result.json"
    Set-Content "$probe/restart-request" 'Compare existing chunks and player edits; do not run placement checks again'
} elseif (Test-Path "$server/$ResultPath") { throw 'Use a fresh disposable server directory; old probe results exist' }
$start = [Diagnostics.ProcessStartInfo]::new()
$start.FileName = [IO.Path]::GetFullPath($Java)
if ($LaunchManifest) {
    if ($LaunchManifest -ne 'server-launch.json') { throw 'Use the prepared server-launch.json manifest' }
    $launch = Get-Content "$server/$LaunchManifest" -Raw | ConvertFrom-Json
    if ($launch.jar) { $Jar = $launch.jar }
    elseif ($launch.argument_file) { $ArgumentFile = $launch.argument_file }
    else { throw 'Launch manifest contains no supported launcher' }
    Write-Output "Using prepared launch manifest $server/$LaunchManifest"
}
if ($Jar -notmatch '^[A-Za-z0-9._-]+\.jar$') { throw 'Jar must be a filename inside the disposable server directory' }
$start.Arguments = "-Xms256M -Xmx3G -jar $Jar nogui"
if ($ArgumentFile) {
    $argumentPath = [IO.Path]::GetFullPath((Join-Path $server $ArgumentFile))
    if (!$argumentPath.StartsWith($server + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or !(Test-Path -LiteralPath $argumentPath)) {
        throw 'Argument file must exist inside the disposable server directory'
    }
    $start.Arguments = "-Xms256M -Xmx3G `"@$argumentPath`" nogui"
}
$start.WorkingDirectory = $server
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardInput = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
$process = [Diagnostics.Process]::new()
$process.StartInfo = $start
$null = $process.Start()
$output = $process.StandardOutput.ReadToEndAsync()
$errors = $process.StandardError.ReadToEndAsync()
$done = $false
$shutdownFailed = $false
$runFailed = $false
try {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    while ($timer.Elapsed.TotalSeconds -lt $Timeout) {
        if ($process.HasExited) { break }
        if (Test-Path "$server/$ResultPath") {
            $done = $true
            $generation = Get-Content "$server/$ResultPath" -Raw | ConvertFrom-Json
            if ($ClientVersion -and $generation.passed) {
                $process.StandardInput.WriteLine('whitelist add loraxprobea')
                $process.StandardInput.WriteLine('whitelist add loraxprobeb')
                Start-Sleep -Seconds 1
                & node "$PSScriptRoot/../client-probe/join.cjs" $port $ClientVersion "$server/lorax-test/client-result.json" *> "$server/client-console.log"
                if ($LASTEXITCODE -ne 0) { throw "Vanilla protocol join failed; inspect $server/client-console.log" }
            }
            break
        }
        Start-Sleep -Seconds 2
    }
} catch {
    $runFailed = $true
    throw
} finally {
    if (!$process.HasExited) {
        try { $process.StandardInput.WriteLine('stop') }
        catch { Write-Warning "Could not send stop to server PID $($process.Id): $_" }
        if (!$process.WaitForExit(15000)) {
            $dump = Join-Path (Split-Path $start.FileName) $(if ($IsWindows) { 'jcmd.exe' } else { 'jcmd' })
            if (Test-Path -LiteralPath $dump) {
                try {
                    & $dump $process.Id Thread.print *> "$server/shutdown-threads.log"
                    if ($LASTEXITCODE -ne 0) { Write-Warning "Thread dump failed; inspect $server/shutdown-threads.log" }
                } catch { Write-Warning "Thread dump failed: $_" }
            }
            if (!$process.WaitForExit(40000)) { $process.Kill($true); $process.WaitForExit(); $shutdownFailed = $true }
        }
    }
    $output.GetAwaiter().GetResult() | Set-Content "$server/server-console.log"
    $errors.GetAwaiter().GetResult() | Set-Content "$server/server-stderr.log"
    $probePassed = $done -and (Test-Path "$server/$ResultPath") -and (Get-Content "$server/$ResultPath" -Raw | ConvertFrom-Json).passed
    @{
        checked_at = [DateTime]::UtcNow.ToString('o')
        passed = [bool]($probePassed -and !$runFailed -and !$shutdownFailed -and $process.ExitCode -eq 0)
        controlled_shutdown = !$shutdownFailed -and $process.ExitCode -eq 0
        exit_code = $process.ExitCode
        probe_result = $ResultPath
        client_version = $ClientVersion
        restart_check = [bool]$Restart
    } | ConvertTo-Json | Set-Content "$server/runner-result.json"
}
if ($shutdownFailed) { throw "Server failed controlled shutdown; inspect $server/server-console.log" }
if ($process.ExitCode -ne 0) { throw "Server exited with code $($process.ExitCode); inspect $server/server-console.log" }
Select-String "$server/server-console.log" -Pattern 'ERROR|Exception|LORAX_INTEGRATION|difference|Different material|Loaded 40|Done \(' -Context 0,4
if (Test-Path "$server/server-stderr.log") { Get-Content "$server/server-stderr.log" -Tail 20 }
if (!$done) { throw "Probe timed out or server exited; inspect $server/server-console.log" }
$result = Get-Content "$server/$ResultPath" | ConvertFrom-Json
if (!$result.passed) { throw "Integration assertion failed; inspect $server/server-console.log" }
$result | ConvertTo-Json
