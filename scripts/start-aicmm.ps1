# start-aicmm.ps1 — Ensure the AiCMM site is running, then open it.
# Idempotent: starts the server only if port 8080 is not already listening.
$ErrorActionPreference = 'SilentlyContinue'
$WarningPreference = 'SilentlyContinue'
$repo = Split-Path $PSScriptRoot -Parent
$port = 8080
$url  = "http://localhost:$port"

function Test-Port($p) { try { (New-Object Net.Sockets.TcpClient).Connect('127.0.0.1', $p); $true } catch { $false } }

$listening = Test-Port $port
if (-not $listening) {
    $jar = Join-Path $repo 'aicmm-site\target\aicmm-site-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path $jar)) { $jar = Join-Path $repo 'aicmm-site\target\aicmm-site-0.1.0-SNAPSHOT-shaded.jar' }
    if (Test-Path $jar) {
        Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar) -WorkingDirectory $repo -WindowStyle Hidden
        for ($i = 0; $i -lt 20; $i++) {
            if (Test-Port $port) { break }
            Start-Sleep -Milliseconds 500
        }
    }
}
Start-Process $url
"AiCMM site at $url"
