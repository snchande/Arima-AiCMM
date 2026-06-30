# restart-aicmm.ps1 — One-command restart with secret-coded takeover.
#
# Flow:
#   1. Send the secret shutdown code to any AiCMM server already on the port so it
#      exits cleanly and releases the jar lock.
#   2. (Default) Rebuild the site jar so code/static changes are picked up.
#   3. Launch the jar. On boot the server itself also runs takeOverPort() as a
#      belt-and-suspenders guard against any stragglers, then serves on the port.
#
# Usage:
#   scripts/restart-aicmm.ps1            # shutdown + rebuild + start
#   scripts/restart-aicmm.ps1 -NoBuild   # shutdown + start (no rebuild)
param(
    [switch]$NoBuild,
    [int]$Port = 8080,
    [string]$Token = $(if ($env:AICMM_ADMIN_TOKEN) { $env:AICMM_ADMIN_TOKEN } else { 'aicmm-secret-restart' })
)
$ErrorActionPreference = 'SilentlyContinue'
$repo = Split-Path $PSScriptRoot -Parent
$jar  = Join-Path $repo 'aicmm-site\target\aicmm-site-0.1.0-SNAPSHOT.jar'
$base = "http://localhost:$Port"

function Test-Up { param($u) try { (Invoke-WebRequest $u -UseBasicParsing -TimeoutSec 2).StatusCode -eq 200 } catch { $false } }

# 1. Secret-coded shutdown of any running instance.
if (Test-Up $base) {
    Write-Host "Sending secret shutdown code to running server on port $Port..." -ForegroundColor Cyan
    try {
        Invoke-WebRequest "$base/api/admin/shutdown" -Method Post -Headers @{ 'X-AiCMM-Token' = $Token } -UseBasicParsing -TimeoutSec 5 | Out-Null
    } catch {}
    for ($i = 0; $i -lt 20; $i++) {
        if (-not (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)) { break }
        Start-Sleep -Milliseconds 500
    }
    if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
        Write-Host "Port $Port still busy after shutdown request; the new instance will force takeover on boot." -ForegroundColor Yellow
    } else {
        Write-Host "Old server stopped." -ForegroundColor Green
    }
}

# 2. Rebuild so changes are picked up (unless -NoBuild).
if (-not $NoBuild) {
    Write-Host "Rebuilding site jar..." -ForegroundColor Cyan
    Push-Location $repo
    & mvn -q -pl aicmm-site -am clean package -DskipTests
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Write-Host "Build failed (exit $code). Aborting restart." -ForegroundColor Red; return }
}

if (-not (Test-Path $jar)) {
    Write-Host "Jar not found. Build first: mvn -q -pl aicmm-site -am clean package -DskipTests" -ForegroundColor Yellow
    return
}

# 3. Launch new instance (its takeOverPort() guards against stragglers).
Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar) -WorkingDirectory $repo -WindowStyle Hidden
for ($i = 0; $i -lt 40; $i++) {
    if (Test-Up $base) { break }
    Start-Sleep -Milliseconds 500
}
if (Test-Up $base) { "AiCMM restarted at $base" } else { Write-Host "Server did not respond in time; check logs." -ForegroundColor Yellow }
