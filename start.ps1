# Starts the whole project on localhost.
#   .\start.ps1
# Database (Docker) -> backend window (port 8080) -> frontend window (port 5173)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

Write-Host "Starting MySQL container..." -ForegroundColor Cyan
Set-Location $root
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker failed. Open Docker Desktop, wait for 'Engine running', then run this script again." -ForegroundColor Red
    exit 1
}

Write-Host "Waiting for the database to become healthy..." -ForegroundColor Cyan
for ($i = 0; $i -lt 60; $i++) {
    $health = docker inspect --format "{{.State.Health.Status}}" parlour-mysql 2>$null
    if ($health -eq "healthy") { break }
    Start-Sleep -Seconds 2
}
if ($health -ne "healthy") {
    Write-Host "Database did not become healthy. Run 'docker logs parlour-mysql' to see why." -ForegroundColor Red
    exit 1
}

Write-Host "Opening backend and frontend windows..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root\backend'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root\frontend'; npm install; npm run dev"

Write-Host ""
Write-Host "Backend:  http://localhost:8080  (wait for 'Started ParlourApplication')" -ForegroundColor Green
Write-Host "Website:  http://localhost:5173" -ForegroundColor Green
