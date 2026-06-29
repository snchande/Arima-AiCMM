# setup-agents.ps1 — Install AiCMM agents & skills so the Copilot CLI finds @aicmm.
# Mirrors repo .copilot/agents and .copilot/skills into the user-global ~/.copilot.
# Idempotent: copies only when source is newer. Repo is the source of truth.
$ErrorActionPreference = 'SilentlyContinue'
$repo = Split-Path $PSScriptRoot -Parent
$dest = Join-Path $env:USERPROFILE '.copilot'

foreach ($kind in 'agents', 'skills') {
    $src = Join-Path $repo ".copilot\$kind"
    if (-not (Test-Path $src)) { continue }
    $out = Join-Path $dest $kind
    New-Item -ItemType Directory -Force -Path $out | Out-Null
    Get-ChildItem -Path $src -Filter *.md | ForEach-Object {
        $t = Join-Path $out $_.Name
        if (-not (Test-Path $t) -or $_.LastWriteTimeUtc -gt (Get-Item $t).LastWriteTimeUtc) {
            Copy-Item $_.FullName $t -Force
        }
    }
}
"AiCMM agents/skills installed to $dest — '@aicmm' is now available in the Copilot CLI."
