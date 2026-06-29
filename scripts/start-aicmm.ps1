# start-aicmm.ps1 — Ensure the AiCMM site is running, then open it.
# Idempotent: starts the server only if port 8080 is not already listening.
$ErrorActionPreference = 'SilentlyContinue'
$WarningPreference = 'SilentlyContinue'
$repo = Split-Path $PSScriptRoot -Parent
$port = 8080
$url  = "http://localhost:$port"

# Install agents/skills so @aicmm is discoverable by the Copilot CLI on a fresh clone.
& (Join-Path $PSScriptRoot 'setup-agents.ps1') | Out-Null

function Test-Port($p) { try { (New-Object Net.Sockets.TcpClient).Connect('127.0.0.1', $p); $true } catch { $false } }
function Have($c) { [bool](Get-Command $c -ErrorAction SilentlyContinue) }

# AiCMM needs Java 17+. Detect it; auto-install via winget when possible, else guide the user.
if (-not (Have 'java')) {
    if (Have 'winget') {
        "Java not found — installing Microsoft OpenJDK 21 via winget..."
        winget install -e --id Microsoft.OpenJDK.21 --silent --accept-source-agreements --accept-package-agreements | Out-Null
        $jdk = Get-ChildItem 'C:\Program Files\Microsoft\jdk-*\bin\java.exe' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($jdk) { $env:Path = "$($jdk.Directory.FullName);$env:Path" }
    }
    if (-not (Have 'java')) {
        Write-Host "AiCMM needs Java 17+. Install it from https://learn.microsoft.com/java/openjdk/download (or 'winget install Microsoft.OpenJDK.21'), reopen Copilot, and @aicmm + the site will start. Agents/skills are already installed." -ForegroundColor Yellow
        return
    }
}

$listening = Test-Port $port
if (-not $listening) {
    $jar = Join-Path $repo 'aicmm-site\target\aicmm-site-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path $jar)) { $jar = Join-Path $repo 'aicmm-site\target\aicmm-site-0.1.0-SNAPSHOT-shaded.jar' }
    if (-not (Test-Path $jar) -and (Have 'mvn')) {
        "Building AiCMM (first run)..."
        & mvn -q -f (Join-Path $repo 'pom.xml') clean package -DskipTests | Out-Null
        $jar = Join-Path $repo 'aicmm-site\target\aicmm-site-0.1.0-SNAPSHOT.jar'
    }
    if (Test-Path $jar) {
        Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar) -WorkingDirectory $repo -WindowStyle Hidden
        for ($i = 0; $i -lt 20; $i++) {
            if (Test-Port $port) { break }
            Start-Sleep -Milliseconds 500
        }
    } else {
        Write-Host "AiCMM jar not built yet. Run 'mvn clean install' once, then the site auto-starts. @aicmm works regardless." -ForegroundColor Yellow
        return
    }
}
Start-Process $url
"AiCMM site at $url"
