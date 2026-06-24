# Start MedConnect server on LAN (Wi-Fi)
# Usage: .\scripts\start-network.ps1

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$BackendDir = Join-Path $ProjectRoot "backend"
$Python = Join-Path $BackendDir "venv\Scripts\python.exe"

function Get-LanIp {
    $ip = Get-NetIPAddress -AddressFamily IPv4 |
        Where-Object {
            $_.InterfaceAlias -notmatch 'Loopback|WSL|Default Switch|vEthernet' -and
            $_.IPAddress -notlike '169.254*'
        } |
        Select-Object -First 1 -ExpandProperty IPAddress
    if (-not $ip) { throw "LAN IP not found. Connect to Wi-Fi." }
    return $ip
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

$LanIp = Get-LanIp
Write-Host ""
Write-Host "=== MedConnect network mode ===" -ForegroundColor Cyan
Write-Host "LAN IP: $LanIp"
Write-Host ""

$GradleProps = Join-Path $ProjectRoot "android\gradle.properties"
$ApiUrl = "http://${LanIp}:8000/"
if (Test-Path $GradleProps) {
    $content = Get-Content $GradleProps -Raw
    if ($content -match "MEDCONNECT_API_URL=.*") {
        $content = $content -replace "MEDCONNECT_API_URL=.*", "MEDCONNECT_API_URL=$ApiUrl"
    } else {
        $content += "`nMEDCONNECT_API_URL=$ApiUrl`n"
    }
    Set-Content $GradleProps $content -Encoding UTF8
    Write-Host "Android API URL: $ApiUrl" -ForegroundColor Green
}

try {
    $ruleName = "MedConnect API 8000"
    if (-not (Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue)) {
        New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Protocol TCP -LocalPort 8000 -Action Allow -ErrorAction Stop | Out-Null
        Write-Host "Firewall rule added for port 8000" -ForegroundColor Green
    }
} catch {
    Write-Host "Run PowerShell as Admin if phone cannot connect" -ForegroundColor Yellow
}

$dockerOk = $false
try {
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Starting Docker..." -ForegroundColor Cyan
        Sync-ApkToDownload -Root $ProjectRoot
        Push-Location $ProjectRoot
        docker compose up -d --build
        Pop-Location
        $dockerOk = $true
        Write-Host "API:     http://${LanIp}:8000/" -ForegroundColor Green
        Write-Host "Swagger: http://${LanIp}:8000/docs"
        Write-Host "Panel:   http://${LanIp}:8000/panel/"
        Write-Host "App:     http://${LanIp}:8000/download/" -ForegroundColor Green
        exit 0
    }
} catch { }

if (-not (Test-Path $Python)) {
    throw "Python venv not found. Run: cd backend; python -m venv venv; pip install -r requirements.txt"
}

try {
    Push-Location $ProjectRoot
    docker compose up -d db 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Waiting for PostgreSQL..." -ForegroundColor Cyan
        Start-Sleep -Seconds 8
    }
    Pop-Location
} catch { }

if ($LASTEXITCODE -ne 0) {
    $DataDir = Join-Path $BackendDir "data"
    New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
    $dbPath = ($DataDir -replace '\\', '/') + "/telemedicine.db"
    $envFile = @"
DATABASE_URL=sqlite:///$dbPath
SECRET_KEY=dev-network-secret-key
ACCESS_TOKEN_EXPIRE_HOURS=24
UPLOAD_DIR=uploads
MAX_FILE_SIZE_MB=10
"@
    Set-Content (Join-Path $BackendDir ".env") $envFile -Encoding ASCII
    Write-Host "Using SQLite: backend/data/telemedicine.db" -ForegroundColor Yellow
}

Sync-ApkToDownload -Root $ProjectRoot

Push-Location $BackendDir
& $Python scripts\seed.py
Write-Host ""
Write-Host "URLs:" -ForegroundColor Cyan
Write-Host "  API:     http://${LanIp}:8000/"
Write-Host "  Swagger: http://${LanIp}:8000/docs"
Write-Host "  Panel:   http://${LanIp}:8000/panel/"
Write-Host "  App:     http://${LanIp}:8000/download/" -ForegroundColor Green
Write-Host ""
Write-Host "Phone and PC must be on the same Wi-Fi." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop."
Write-Host ""
& $Python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
Pop-Location
