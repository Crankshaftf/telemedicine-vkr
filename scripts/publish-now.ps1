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

gh auth status | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Run first: gh auth login"
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
