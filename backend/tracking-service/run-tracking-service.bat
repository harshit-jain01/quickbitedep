@echo off
REM Tracking Service Quick Start

cd /d C:\Users\jhars\Desktop\QuickbiteBackend\tracking-service

echo.
echo ========================================
echo Tracking Service - Build & Run
echo ========================================
echo.

echo Step 1: Clean Maven cache...
mvn clean

echo.
echo Step 2: Compile and build...
mvn compile

echo.
echo Step 3: Running Tracking Service on port 8089...
echo.
mvn spring-boot:run

pause

