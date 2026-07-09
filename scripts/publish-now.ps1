# Publish repo + APK to GitHub (run after: gh auth login)
param(
    [string]$Tag = "v1.0",
    [string]$ApiUrl = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $ProjectRoot

$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

gh auth status 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "gh auth status failed — trying Git Credential Manager..." -ForegroundColor Yellow
    try {
        $gcmToken = ("protocol=https`nhost=github.com`n`n" | git credential fill | Select-String "^password=").ToString().Split("=", 2)[1].Trim()
        if ($gcmToken) {
            $env:GH_TOKEN = $gcmToken
            Write-Host "Token loaded from Git Credential Manager." -ForegroundColor Green
        } else {
            throw "empty token"
        }
    } catch {
        throw "Run first: gh auth login  OR  git credential-manager github login"
    }
}

if (-not (Test-Path ".git")) {
    git init
    git branch -M main
}

$remote = git remote get-url origin 2>$null
if (-not $remote) {
    gh repo create telemedicine-vkr --public --source=. --remote=origin --description "MedConnect telemedicine VKR demo" --push
} else {
    git push -u origin main
}

$publishArgs = @("-Tag", $Tag)
if ($ApiUrl) { $publishArgs += @("-ApiUrl", $ApiUrl) }
if ($SkipBuild) { $publishArgs += "-SkipBuild" }

& (Join-Path $PSScriptRoot "publish-github.ps1") @publishArgs

Get-Content (Join-Path $ProjectRoot "github-release.url")
