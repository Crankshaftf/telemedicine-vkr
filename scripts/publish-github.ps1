# Publish APK to GitHub Releases (permanent phone download link)
# Usage:
#   .\scripts\publish-github.ps1
#   .\scripts\publish-github.ps1 -Tag v1.1 -ApiUrl "https://your-server.com/"
#   .\scripts\publish-github.ps1 -SkipBuild

param(
    [string]$Tag = "v1.0",
    [string]$ApiUrl = "",
    [switch]$SkipBuild,
    [switch]$UsePublicUrl
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$ApkPath = Join-Path $ProjectRoot "MedConnect.apk"
$UrlFile = Join-Path $ProjectRoot "github-release.url"
$PublicUrlFile = Join-Path $ProjectRoot ".public-url"

function Ensure-Gh {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        throw @"
GitHub CLI (gh) not found.
Install: winget install GitHub.cli
Then: gh auth login
"@
    }
    gh auth status 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Run: gh auth login"
    }
}

function Ensure-GitRepo {
    Push-Location $ProjectRoot
    if (-not (Test-Path ".git")) {
        Write-Host "Initializing git repository..." -ForegroundColor Cyan
        git init
        git branch -M main
    }

    $remote = git remote get-url origin 2>$null
    if (-not $remote) {
        Write-Host ""
        Write-Host "No GitHub remote yet. Create repo:" -ForegroundColor Yellow
        Write-Host "  gh repo create telemedicine-vkr --public --source=. --remote=origin"
        Write-Host "Or add remote manually:"
        Write-Host "  git remote add origin https://github.com/YOUR_USER/telemedicine-vkr.git"
        Write-Host ""
        throw "Configure git remote origin first"
    }
    Pop-Location
}

Ensure-Gh
Ensure-GitRepo

if (-not $SkipBuild) {
    $buildArgs = @()
    if ($ApiUrl) {
        $buildArgs += "-ApiUrl", $ApiUrl
    } elseif ($UsePublicUrl) {
        $buildArgs += "-UsePublicUrl"
    } elseif (Test-Path $PublicUrlFile) {
        $buildArgs += "-UsePublicUrl"
    }
    & (Join-Path $PSScriptRoot "build-apk.ps1") @buildArgs
}

if (-not (Test-Path $ApkPath)) {
    throw "MedConnect.apk not found. Run build-apk.ps1 first."
}

Push-Location $ProjectRoot

$repo = gh repo view --json nameWithOwner -q .nameWithOwner
$existing = gh release view $Tag 2>$null

if ($LASTEXITCODE -eq 0) {
    Write-Host "Updating release $Tag..." -ForegroundColor Cyan
    gh release upload $Tag $ApkPath --clobber
} else {
    Write-Host "Creating release $Tag..." -ForegroundColor Cyan
    $notes = @"
MedConnect Android app (VKR demo).

Test login: user@test.ru / user123
"@
    gh release create $Tag $ApkPath --title "MedConnect $Tag" --notes $notes
}

$downloadUrl = "https://github.com/$repo/releases/latest/download/MedConnect.apk"
Set-Content $UrlFile $downloadUrl -Encoding ASCII -NoNewline

Write-Host ""
Write-Host "=== GitHub Release published ===" -ForegroundColor Green
Write-Host ""
Write-Host "Permanent download link (open on phone):" -ForegroundColor Cyan
Write-Host "  $downloadUrl" -ForegroundColor White
Write-Host ""
Write-Host "Releases page:"
Write-Host "  https://github.com/$repo/releases"
Write-Host ""
Write-Host "Link saved to github-release.url" -ForegroundColor Green
Write-Host ""

Pop-Location
