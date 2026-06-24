# Build APK for phone install
# Usage:
#   .\scripts\build-apk.ps1                    # LAN IP
#   .\scripts\build-apk.ps1 -UsePublicUrl      # from .public-url (after start-public.ps1)
#   .\scripts\build-apk.ps1 -ApiUrl "https://your-server.com/"

param(
    [string]$ApiUrl = "",
    [switch]$UsePublicUrl
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$AndroidDir = Join-Path $ProjectRoot "android"
$JavaHome = "C:\Program Files\Android\Android Studio\jbr"
$SdkDir = "$env:LOCALAPPDATA\Android\Sdk"
$GradleProps = Join-Path $AndroidDir "gradle.properties"
$PublicUrlFile = Join-Path $ProjectRoot ".public-url"

if (-not (Test-Path $JavaHome)) {
    throw "Android Studio JDK not found: $JavaHome"
}

$env:JAVA_HOME = $JavaHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Content (Join-Path $AndroidDir "local.properties") "sdk.dir=$($SdkDir -replace '\\','\\')" -Encoding ASCII

if ($UsePublicUrl -and (Test-Path $PublicUrlFile)) {
    $ApiUrl = (Get-Content $PublicUrlFile -Raw).Trim()
}

if (-not $ApiUrl) {
    $LanIp = (Get-NetIPAddress -AddressFamily IPv4 |
        Where-Object { $_.InterfaceAlias -notmatch 'Loopback|WSL|Default Switch|vEthernet' -and $_.IPAddress -notlike '169.254*' } |
        Select-Object -First 1).IPAddress
    if ($LanIp) {
        $ApiUrl = "http://${LanIp}:8000/"
    }
}

if (-not $ApiUrl) {
    throw "API URL not set. Use -ApiUrl or -UsePublicUrl or run start-public.ps1"
}

if ($ApiUrl -notmatch '/$') { $ApiUrl += '/' }

$content = Get-Content $GradleProps -Raw
$content = $content -replace "MEDCONNECT_API_URL=.*", "MEDCONNECT_API_URL=$ApiUrl"
Set-Content $GradleProps $content -Encoding UTF8
Write-Host "API URL for APK: $ApiUrl" -ForegroundColor Cyan

Push-Location $AndroidDir

if (-not (Test-Path "gradlew.bat")) {
    throw "gradlew.bat missing. Open android/ in Android Studio once."
}

Write-Host "Building APK..." -ForegroundColor Cyan
& .\gradlew.bat assembleDebug --no-daemon

$ApkSource = Join-Path $AndroidDir "app\build\outputs\apk\debug\app-debug.apk"
$ApkDest = Join-Path $ProjectRoot "MedConnect.apk"
$DownloadWeb = Join-Path $ProjectRoot "download-web"
$DownloadBackend = Join-Path $ProjectRoot "backend\downloads"

if (Test-Path $ApkSource) {
    New-Item -ItemType Directory -Force -Path $DownloadWeb | Out-Null
    New-Item -ItemType Directory -Force -Path $DownloadBackend | Out-Null
    Copy-Item $ApkSource $ApkDest -Force
    Copy-Item $ApkSource (Join-Path $DownloadWeb "MedConnect.apk") -Force
    Copy-Item $ApkSource (Join-Path $DownloadBackend "MedConnect.apk") -Force
    Write-Host ""
    Write-Host "APK ready: $ApkDest" -ForegroundColor Green
    Write-Host "Download page: ${ApiUrl}download/" -ForegroundColor Green
    Write-Host "Server: $ApiUrl" -ForegroundColor Green
} else {
    throw "APK build failed"
}

Pop-Location
