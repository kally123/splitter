@echo off
REM Build All Services Script for Windows

echo ========================================
echo Building Splitter Services
echo ========================================
echo.

cd /d "%~dp0.."

REM Build shared libraries first
echo [1/4] Building shared libraries...
cd shared
call mvn clean install -DskipTests
if errorlevel 1 (
    echo ERROR: Failed to build shared libraries
    exit /b 1
)
cd ..

REM Build API Gateway
echo [2/4] Building API Gateway...
cd services\api-gateway
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Failed to build API Gateway
    exit /b 1
)
cd ..\..

REM Build User Service
echo [3/4] Building User Service...
cd services\user-service
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Failed to build User Service
    exit /b 1
)
cd ..\..

echo.
echo [4/4] Build complete!
echo.
echo To run services:
echo   API Gateway:   cd services\api-gateway ^&^& mvn spring-boot:run
echo   User Service:  cd services\user-service ^&^& mvn spring-boot:run
echo.
