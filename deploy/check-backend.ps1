# ============================================
# Fengbushi Backend Status Check Script
# Usage: .\check-backend.ps1
# ============================================

param()

$APP_DIR = "C:\fengbushi"
$CONSUL_HOST = "192.168.50.100"
$CONSUL_PORT = 8500

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Fengbushi Backend Status Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check PID file
$pidFile = "$APP_DIR\logs\fengbushi.pid"
$logFile = "$APP_DIR\logs\fengbushi.log"

if (Test-Path $pidFile) {
    $appPid = Get-Content $pidFile
    $process = Get-Process -Id $appPid -ErrorAction SilentlyContinue
    
    if ($process) {
        Write-Host "Local Process: " -NoNewline
        Write-Host "RUNNING" -ForegroundColor Green
        Write-Host "PID: $appPid" -ForegroundColor Gray
        Write-Host "CPU: $($process.CPU)s" -ForegroundColor Gray
        Write-Host "Memory: $([math]::Round($process.WorkingSet64 / 1MB, 2)) MB" -ForegroundColor Gray
    } else {
        Write-Host "Local Process: " -NoNewline
        Write-Host "NOT RUNNING (stale PID file)" -ForegroundColor Red
        Write-Host "Stale PID: $appPid" -ForegroundColor Gray
    }
} else {
    Write-Host "Local Process: " -NoNewline
    Write-Host "NOT RUNNING" -ForegroundColor Yellow
}

# Check log file
if (Test-Path $logFile) {
    $lastWrite = (Get-Item $logFile).LastWriteTime
    $fileSize = (Get-Item $logFile).Length
    Write-Host "Log File: $([math]::Round($fileSize / 1KB, 2)) KB (Updated: $lastWrite)" -ForegroundColor Gray
}

# Check Consul registration
try {
    $url = "http://${CONSUL_HOST}:${CONSUL_PORT}/v1/catalog/service/fengbushi-chat"
    $response = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 5 -ErrorAction Stop
    
    Write-Host "Consul Registration: " -NoNewline
    Write-Host "REGISTERED ($($response.Count) instances)" -ForegroundColor Green
    
    foreach ($service in $response) {
        Write-Host "  - $($service.ServiceAddress):$($service.ServicePort) (ID: $($service.ServiceID))" -ForegroundColor Gray
    }
    
    # Check health status
    $healthUrl = "http://${CONSUL_HOST}:${CONSUL_PORT}/v1/health/service/fengbushi-chat?passing"
    $healthResponse = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 5 -ErrorAction Stop
    
    Write-Host "Health Status: " -NoNewline
    Write-Host "HEALTHY ($($healthResponse.Count) instances)" -ForegroundColor Green
} catch {
    Write-Host "Consul Check: " -NoNewline
    Write-Host "FAILED (Consul unreachable)" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Gray
}

Write-Host "----------------------------------------" -ForegroundColor Gray
Write-Host ""
Write-Host "Consul Server: http://$CONSUL_HOST`:$CONSUL_PORT" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
