@echo off
cd /d "%~dp0"
if not exist "dist\BallGame-Pro-V2.jar" call "build.bat"
if not exist "dist\BallGame-Pro-V2.jar" exit /b 1
java -jar "dist\BallGame-Pro-V2.jar"
if errorlevel 1 pause
