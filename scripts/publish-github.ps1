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
        throw "GitHub CLI (gh) not found. Install: winget install GitHub.cli"
    }

    # Try existing auth first (suppress Stop on non-zero exit)
    $authOk = $false
    try {
        $null = & gh auth status 2>&1
        $authOk = ($LASTEXITCODE -eq 0)
    } catch { $authOk = $false }
    if ($authOk) { return }

    # Fallback: extract token from Git Credential Manager
    Write-Host "gh not authenticated - trying Git Credential Manager..." -ForegroundColor Yellow
    try {
        $gcmOut = "protocol=https`nhost=github.com`n`n" | git credential fill
        $passLine = $gcmOut | Select-String "^password="
        if ($passLine) {
            $gcmToken = $passLine.ToString().Split("=", 2)[1].Trim()
            if ($gcmToken) {
                $env:GH_TOKEN = $gcmToken
                Write-Host "Token loaded from Git Credential Manager." -ForegroundColor Green
                return
            }
        }
    } catch {}

    throw "Not logged in to GitHub. Run: gh auth login  OR  git credential-manager github login"
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
        Write-Host "No GitHub remote yet. Create repo:" -ForegroundColor Yellow
        Write-Host "  gh repo create telemedicine-vkr --public --source=. --remote=origin"
        throw "Configure git remote origin first"
    }
    Pop-Location
}

Ensure-Gh
Ensure-GitRepo

if (-not $SkipBuild) {
    $resolvedUrl = $ApiUrl
    if (-not $resolvedUrl -and (Test-Path $PublicUrlFile)) {
        $resolvedUrl = (Get-Content $PublicUrlFile -Raw).Trim()
    }
    if ($resolvedUrl) {
        & (Join-Path $PSScriptRoot "build-apk.ps1") -ApiUrl $resolvedUrl
    } else {
        & (Join-Path $PSScriptRoot "build-apk.ps1")
    }
}

if (-not (Test-Path $ApkPath)) {
    throw "MedConnect.apk not found. Run build-apk.ps1 first."
}

Push-Location $ProjectRoot

$repo = gh repo view --json nameWithOwner -q .nameWithOwner

$releaseExists = $false
try {
    $null = & gh release view $Tag 2>&1
    $releaseExists = ($LASTEXITCODE -eq 0)
} catch { $releaseExists = $false }

if ($releaseExists) {
    Write-Host "Updating release $Tag..." -ForegroundColor Cyan
    gh release upload $Tag $ApkPath --clobber
} else {
    Write-Host "Creating release $Tag..." -ForegroundColor Cyan
    $notes = "MedConnect Android app (VKR demo).`n`nTest login: user@test.ru / user123"
    gh release create $Tag $ApkPath --title "MedConnect $Tag" --notes $notes
}

$downloadUrl = "https://github.com/$repo/releases/latest/download/MedConnect.apk"
Set-Content $UrlFile $downloadUrl -Encoding ASCII -NoNewline

Write-Host ""
Write-Host "=== GitHub Release published ===" -ForegroundColor Green
Write-Host ""
Write-Host "Download link:" -ForegroundColor Cyan
Write-Host "  $downloadUrl" -ForegroundColor White
Write-Host ""
Write-Host "Releases page: https://github.com/$repo/releases"
Write-Host "Link saved to github-release.url" -ForegroundColor Green
Write-Host ""

Pop-Location
