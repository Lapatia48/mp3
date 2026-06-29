@echo off
setlocal
title Vinylia - Arret

echo ============================================
echo       VINYLIA - Arret complet
echo ============================================
echo.

echo [1/5] Arret du frontend (5173)...

for /f "tokens=5" %%a in ('netstat -ano ^| findstr LISTENING ^| findstr ":5173"') do (
taskkill /PID %%a /T /F >nul 2>&1
)

echo [2/5] Arret de l'API (8080)...

for /f "tokens=5" %%a in ('netstat -ano ^| findstr LISTENING ^| findstr ":8080"') do (
taskkill /PID %%a /T /F >nul 2>&1
)

echo [3/5] Arret des processus Java restants...

taskkill /IM java.exe /T /F >nul 2>&1
taskkill /IM javaw.exe /T /F >nul 2>&1

echo [4/5] Arret de RabbitMQ...

docker stop rabbitmq >nul 2>&1

echo [5/5] Fermeture des terminaux Vinylia...

taskkill /FI "WINDOWTITLE eq Vinylia - API*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Vinylia - Extracteur*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Vinylia - Uploader*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Vinylia - Scanner*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Vinylia - Web*" /F >nul 2>&1

echo.
echo ============================================
echo      Tous les services Vinylia arretes
echo ============================================
echo.

timeout /t 2 >nul
pause
endlocal
