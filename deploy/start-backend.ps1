# ============================================
# Fengbushi Backend Start Script
# Usage: .\start-backend.ps1
# ============================================

param()

$APP_DIR = "C:\fengbushi"
$APP_JAR = "$APP_DIR\app.jar"
$LOG_FILE = "$APP_DIR\logs\fengbushi.log"
$PID_FILE = "$APP_DIR\logs\fengbushi.pid"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Fengbushi Backend" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check if app.jar exists
if (-Not (Test-Path $APP_JAR)) {
    Write-Host "ERROR: Application JAR not found at $APP_JAR" -ForegroundColor Red
    exit 1
}

# Create logs directory if not exists
if (-Not (Test-Path "$APP_DIR\logs")) {
    New-Item -ItemType Directory -Path "$APP_DIR\logs" | Out-Null
}

# Check if already running
if (Test-Path $PID_FILE) {
    $oldPid = Get-Content $PID_FILE
    $process = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "WARNING: Application may already be running (PID: $oldPid)" -ForegroundColor Yellow
        $choice = Read-Host "Do you want to stop it first? (y/n)"
        if ($choice -eq "y" -or $choice -eq "Y") {
            & "$PSScriptRoot\stop-backend.ps1"
            Start-Sleep -Seconds 3
        } else {
            Write-Host "Aborted." -ForegroundColor Yellow
            exit 0
        }
    }
}

# Start the application
Write-Host "Starting application..." -ForegroundColor Green
Write-Host "Log file: $LOG_FILE" -ForegroundColor Green

$javaArgs = @(
    "-jar", $APP_JAR
)

Start-Process -FilePath "java" `
    -ArgumentList $javaArgs `
    -RedirectStandardOutput $LOG_FILE `
    -RedirectStandardError "$APP_DIR\logs\fengbushi-error.log" `
    -NoNewWindow `
    -PassThru | ForEach-Object {
    $_.Id | Out-File -FilePath $PID_FILE -Encoding UTF8
    Write-Host "Application started with PID: $($_.Id)" -ForegroundColor Green
}

Write-Host "Waiting for application to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Check if process is still running
if (Test-Path $PID_FILE) {
    $appPid = Get-Content $PID_FILE
    $process = Get-Process -Id $appPid -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "Application started successfully!" -ForegroundColor Green
        Write-Host "PID: $appPid" -ForegroundColor Green
        Write-Host "Log: $LOG_FILE" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
    } else {
        Write-Host "ERROR: Application failed to start. Check log file: $LOG_FILE" -ForegroundColor Red
        Remove-Item $PID_FILE -ErrorAction SilentlyContinue
        exit 1
    }
}