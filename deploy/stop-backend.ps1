# ============================================
# Fengbushi Backend Stop Script
# Usage: .\stop-backend.ps1
# ============================================

param()

$APP_DIR = "C:\fengbushi"
$PID_FILE = "$APP_DIR\logs\fengbushi.pid"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Stopping Fengbushi Backend" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check if PID file exists
if (-Not (Test-Path $PID_FILE)) {
    Write-Host "WARNING: PID file not found. Application may not be running." -ForegroundColor Yellow

    # Try to find by process name
    $processes = Get-Process | Where-Object { $_.CommandLine -like "*fengbushi*" }
    if ($processes) {
        Write-Host "Found running processes:" -ForegroundColor Yellow
        $processes | ForEach-Object { Write-Host "  PID: $($_.Id)" -ForegroundColor Yellow }
        $choice = Read-Host "Do you want to stop them? (y/n)"
        if ($choice -eq "y" -or $choice -eq "Y") {
            $processes | Stop-Process -Force
            Write-Host "Processes stopped." -ForegroundColor Green
        }
    } else {
        Write-Host "No running application found." -ForegroundColor Yellow
    }
    exit 0
}

# Read PID
$appPid = Get-Content $PID_FILE
Write-Host "Stopping application (PID: $appPid)..." -ForegroundColor Yellow

# Check if process exists
$process = Get-Process -Id $appPid -ErrorAction SilentlyContinue
if (-Not $process) {
    Write-Host "WARNING: Process not found. Cleaning up PID file." -ForegroundColor Yellow
    Remove-Item $PID_FILE -ErrorAction SilentlyContinue
    exit 0
}

# Graceful shutdown
try {
    Stop-Process -Id $appPid -ErrorAction Stop
    Write-Host "Stop signal sent. Waiting for process to exit..." -ForegroundColor Yellow

    # Wait for process to stop
    $timeout = 30
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        Start-Sleep -Seconds 1
        $elapsed++
        $process = Get-Process -Id $appPid -ErrorAction SilentlyContinue
        if (-Not $process) {
            break
        }
        Write-Host "  Waiting... ($elapsed/$timeout)" -ForegroundColor Gray
    }

    # Check if still running
    $process = Get-Process -Id $appPid -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "Process did not stop gracefully. Force killing..." -ForegroundColor Yellow
        Stop-Process -Id $appPid -Force
        Start-Sleep -Seconds 2
    }

    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Application stopped successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green

} catch {
    Write-Host "ERROR: Failed to stop application: $_" -ForegroundColor Red
    exit 1
} finally {
    # Clean up PID file
    Remove-Item $PID_FILE -ErrorAction SilentlyContinue
}