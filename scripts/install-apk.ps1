# Install APK via USB (optional)
# Usage: .\scripts\install-apk.ps1

$Apk = Join-Path (Split-Path $PSScriptRoot -Parent) "MedConnect.apk"
$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $Apk)) {
    throw "MedConnect.apk not found. Run .\scripts\build-apk.ps1 first."
}
if (-not (Test-Path $Adb)) {
    throw "adb not found. Install Android SDK platform-tools or copy APK manually."
}

& $Adb devices
& $Adb install -r $Apk
Write-Host "Done. Open MedConnect on phone." -ForegroundColor Green
