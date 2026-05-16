# ============================================
# Windows MySQL初始化脚本
# ============================================

param(
    [string]$MySQLHost = "192.168.50.1",
    [string]$MySQLPort = "3306",
    [string]$MySQLUser = "root",
    [string]$MySQLPassword = "gyq123"
)

Write-Host "=========================================" -ForegroundColor Green
Write-Host "Initialize MySQL Database" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host "  Host: $MySQLHost" -ForegroundColor Cyan
Write-Host "  Port: $MySQLPort" -ForegroundColor Cyan
Write-Host "  User: $MySQLUser" -ForegroundColor Cyan
Write-Host ""

# Check if mysql command exists
$mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysqlCmd) {
    Write-Host "Error: mysql command not found" -ForegroundColor Red
    Write-Host "Please install MySQL Client first" -ForegroundColor Yellow
    exit 1
}

# SQL script path
$sqlScript = "$PSScriptRoot\init.sql"

if (-not (Test-Path $sqlScript)) {
    Write-Host "Error: init.sql not found at $sqlScript" -ForegroundColor Red
    exit 1
}

Write-Host "Executing SQL script..." -ForegroundColor Cyan
Write-Host ""

# Execute SQL script
$env:MYSQL_PWD = $MySQLPassword
mysql -h $MySQLHost -P $MySQLPort -u $MySQLUser < $sqlScript

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Green
    Write-Host "Database Initialized Successfully!" -ForegroundColor Green
    Write-Host "=========================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "Error: Failed to initialize database" -ForegroundColor Red
    exit 1
}
