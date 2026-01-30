@echo off
REM Stop Development Environment Script for Windows

echo ========================================
echo Stopping Splitter Development Environment
echo ========================================
echo.

cd /d "%~dp0..\infrastructure\docker"
docker compose -f docker-compose.dev.yml down

echo.
echo Development environment stopped.
echo.
echo To remove volumes (WARNING: deletes all data):
echo   docker compose -f infrastructure/docker/docker-compose.dev.yml down -v
echo.
