# run-foundational-tests.ps1 — AiCMM integrity gate.
#
# Runs the foundational test suite that protects the integrity of the AiCMM framework
# (the 7 governance rules, the agent threshold, the signed Agency ladder, and the
# 12-dimension structure). The Develop & Extend / contribute flow MUST run this before
# opening a Pull Request and paste the printed summary into the PR description.
#
# Exit code 0 = all foundational tests pass (safe to open a PR).
# Exit code 1 = a foundational invariant broke (DO NOT open a PR).
#
# Usage:
#   scripts/run-foundational-tests.ps1            # core integrity tests (fast)
#   scripts/run-foundational-tests.ps1 -All       # every module's tests
param(
    [switch]$All
)
$ErrorActionPreference = 'SilentlyContinue'
$repo = Split-Path $PSScriptRoot -Parent
Push-Location $repo

$modules = if ($All) { @() } else { @('-pl', 'aicmm-core') }
$args = @('test') + $modules
$out = & mvn @args 2>&1
$code = $LASTEXITCODE
Pop-Location

# Aggregate the per-class "Tests run: N, Failures: F, Errors: E, Skipped: S" lines.
$run = 0; $fail = 0; $err = 0; $skip = 0
foreach ($line in ($out -split "`n")) {
    if ($line -match 'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)') {
        # Maven prints a per-class line and a final total; take the max as the total.
        $run = [Math]::Max($run, [int]$Matches[1])
    }
}
# Recompute totals from the final summary line (last match wins — it is the module total).
$finals = [regex]::Matches(($out -join "`n"), 'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)')
if ($finals.Count -gt 0) {
    $m = $finals[$finals.Count - 1]
    $run = [int]$m.Groups[1].Value
    $fail = [int]$m.Groups[2].Value
    $err = [int]$m.Groups[3].Value
    $skip = [int]$m.Groups[4].Value
}

$status = if ($code -eq 0 -and $fail -eq 0 -and $err -eq 0) { 'PASS' } else { 'FAIL' }
$scope = if ($All) { 'all modules' } else { 'aicmm-core (foundational)' }

Write-Host ''
Write-Host '<!-- AICMM-FOUNDATIONAL-TESTS -->'
Write-Host "### AiCMM Foundational Tests: $status"
Write-Host ''
Write-Host "- Scope: $scope"
Write-Host "- Tests run: $run | Failures: $fail | Errors: $err | Skipped: $skip"
Write-Host "- Protects: 7 governance rules, agent threshold, Agency ladder (-2..+5), 12-dimension structure"
Write-Host '<!-- /AICMM-FOUNDATIONAL-TESTS -->'
Write-Host ''

if ($status -ne 'PASS') {
    Write-Host 'Foundational integrity tests FAILED — do not open a PR. Failing output:' -ForegroundColor Red
    $out | Select-String 'FAIL|ERROR|Tests run' | Select-Object -Last 30 | ForEach-Object { Write-Host $_ }
    exit 1
}
exit 0
