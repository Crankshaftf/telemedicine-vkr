# Start backend + public HTTPS tunnel (internet access from anywhere)
# Usage: .\scripts\start-public.ps1
#
# Requires: backend venv, optional Docker for PostgreSQL
# Creates: .public-url with https://....trycloudflare.com/

param(
    [int]$Port = 8000
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$BackendDir = Join-Path $ProjectRoot "backend"
$Python = Join-Path $BackendDir "venv\Scripts\python.exe"
$PublicUrlFile = Join-Path $ProjectRoot ".public-url"
$Cloudflared = Join-Path $ProjectRoot "tools\cloudflared.exe"

function Ensure-BackendEnv {
    if (-not (Test-Path $Python)) {
        throw "Python venv not found. Run: cd backend; python -m venv venv; pip install -r requirements.txt"
    }

    try {
        Push-Location $ProjectRoot
        docker compose up -d db 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "PostgreSQL starting..." -ForegroundColor Cyan
            Start-Sleep -Seconds 8
            Pop-Location
            return
        }
        Pop-Location
    } catch { Pop-Location }

    $DataDir = Join-Path $BackendDir "data"
    New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
    $dbPath = ($DataDir -replace '\\', '/') + "/telemedicine.db"
    @"
DATABASE_URL=sqlite:///$dbPath
SECRET_KEY=dev-public-secret-key
ACCESS_TOKEN_EXPIRE_HOURS=24
UPLOAD_DIR=uploads
MAX_FILE_SIZE_MB=10
"@ | Set-Content (Join-Path $BackendDir ".env") -Encoding ASCII
    Write-Host "Using SQLite (backend/data/telemedicine.db)" -ForegroundColor Yellow
}

function Ensure-Cloudflared {
    if (Test-Path $Cloudflared) { return }
    Write-Host "Downloading cloudflared..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path (Split-Path $Cloudflared) | Out-Null
    $zip = Join-Path $env:TEMP "cloudflared-windows-amd64.exe"
    Invoke-WebRequest -Uri "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe" -OutFile $zip
    Copy-Item $zip $Cloudflared -Force
}

function Sync-ApkToDownload {
    param([string]$Root)
    $src = Join-Path $Root "MedConnect.apk"
    if (-not (Test-Path $src)) { return }
    $targets = @(
        (Join-Path $Root "download-web\MedConnect.apk"),
        (Join-Path $Root "backend\downloads\MedConnect.apk")
    )
    foreach ($dest in $targets) {
        $dir = Split-Path $dest -Parent
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        Copy-Item $src $dest -Force
    }
}

function Start-Backend {
    Sync-ApkToDownload -Root $ProjectRoot
    Push-Location $BackendDir
    & $Python scripts\seed.py
    $proc = Start-Process -FilePath $Python -ArgumentList "-m", "uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", $Port -PassThru -WindowStyle Hidden
    Pop-Location
    Start-Sleep -Seconds 3
    return $proc
}

function Start-Tunnel {
    $logFile = Join-Path $ProjectRoot "tunnel.log"
    if (Test-Path $logFile) { Remove-Item $logFile -Force }

    $proc = Start-Process -FilePath $Cloudflared -ArgumentList "tunnel", "--url", "http://127.0.0.1:$Port", "--logfile", $logFile, "--loglevel", "info" -PassThru -WindowStyle Hidden

    $publicUrl = $null
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        if (Test-Path $logFile) {
            $log = Get-Content $logFile -Raw -ErrorAction SilentlyContinue
            if ($log -match 'https://[a-z0-9-]+\.trycloudflare\.com') {
                $publicUrl = $Matches[0]
                break
            }
        }
    }

    if (-not $publicUrl) {
        throw "Tunnel URL not received. Check tunnel.log"
    }

    $apiUrl = "$publicUrl/"
    Set-Content $PublicUrlFile $apiUrl -Encoding ASCII -NoNewline

    $gradleProps = Join-Path $ProjectRoot "android\gradle.properties"
    if (Test-Path $gradleProps) {
        $content = Get-Content $gradleProps -Raw
        $content = $content -replace "MEDCONNECT_API_URL=.*", "MEDCONNECT_API_URL=$apiUrl"
        Set-Content $gradleProps $content -Encoding UTF8
    }

    return @{ Url = $apiUrl; Backend = $null; Tunnel = $proc }
}

Write-Host ""
Write-Host "=== MedConnect PUBLIC mode (internet) ===" -ForegroundColor Cyan
Write-Host ""

Ensure-BackendEnv
Ensure-Cloudflared

# Stop old processes on port
Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
}

$backendProc = Start-Backend
$result = Start-Tunnel

Write-Host "PUBLIC API URL:" -ForegroundColor Green
Write-Host "  $($result.Url)" -ForegroundColor White
Write-Host ""
Write-Host "Swagger:  $($result.Url)docs"
Write-Host "Panel:    $($result.Url)panel/"
Write-Host "Download: $($result.Url)download/" -ForegroundColor Green
Write-Host "Health:   $($result.Url)health"
Write-Host ""
Write-Host "URL saved to .public-url" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Build APK:  .\scripts\build-apk.ps1 -UsePublicUrl"
Write-Host "  2. On phone open: $($result.Url)download/  (install without USB)"
Write-Host "  3. Login: user@test.ru / user123"
Write-Host ""
Write-Host "Keep this window open. Ctrl+C stops tunnel and server."
Write-Host ""

try {
    while ($true) { Start-Sleep -Seconds 60 }
} finally {
    Stop-Process -Id $backendProc.Id -Force -ErrorAction SilentlyContinue
    Stop-Process -Id $result.Tunnel.Id -Force -ErrorAction SilentlyContinue
}
