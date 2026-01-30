@echo off
REM Start Development Environment Script for Windows
REM This script starts all required infrastructure services for local development

echo ========================================
echo Starting Splitter Development Environment
echo ========================================
echo.

REM Check if Docker is running
docker info > nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running. Please start Docker Desktop first.
    exit /b 1
)

echo [1/3] Starting infrastructure services...
cd /d "%~dp0infrastructure\docker"
docker compose -f docker-compose.dev.yml up -d

if errorlevel 1 (
    echo ERROR: Failed to start infrastructure services
    exit /b 1
)

echo.
echo [2/3] Waiting for services to be healthy...
timeout /t 10 /nobreak > nul

echo.
echo [3/3] Checking service status...
docker compose -f docker-compose.dev.yml ps

echo.
echo ========================================
echo Development Environment Started!
echo ========================================
echo.
echo Available Services:
echo   - PostgreSQL:    localhost:5432
echo   - Redis:         localhost:6379
echo   - Kafka:         localhost:9094
echo   - Kafka UI:      http://localhost:8090
echo   - pgAdmin:       http://localhost:5050
echo   - Mailhog:       http://localhost:8025
echo.
echo To view logs: docker compose -f infrastructure/docker/docker-compose.dev.yml logs -f
echo To stop:      scripts\stop-dev.bat
echo.
