@echo off
setlocal
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
  echo Install Node.js 24.15+ from the 24.x family, then run this launcher again.
  pause
  exit /b 1
)
node server\index.mjs
if errorlevel 1 pause
