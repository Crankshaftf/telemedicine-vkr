# One-time GitHub setup for the project
# Usage: .\scripts\setup-github.ps1

param(
    [string]$RepoName = "telemedicine-vkr",
    [ValidateSet("public", "private")]
    [string]$Visibility = "public"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "Install GitHub CLI: winget install GitHub.cli"
}

Push-Location $ProjectRoot

if (-not (Test-Path ".git")) {
    git init
    git branch -M main
}

$remote = git remote get-url origin 2>$null
if (-not $remote) {
    Write-Host "Creating GitHub repo: $RepoName ($Visibility)..." -ForegroundColor Cyan
    gh repo create $RepoName --$Visibility --source=. --remote=origin --description "MedConnect telemedicine VKR demo"
} else {
    Write-Host "Remote already set: $remote" -ForegroundColor Yellow
}

git add .
$status = git status --porcelain
if ($status) {
    git commit -m "MedConnect telemedicine VKR prototype"
}

Write-Host "Pushing to GitHub..." -ForegroundColor Cyan
git push -u origin main

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Set server URL and publish APK:"
Write-Host "       .\scripts\publish-github.ps1 -ApiUrl `"https://your-server.com/`""
Write-Host "  2. Share link from github-release.url with testers"
Write-Host ""

Pop-Location
