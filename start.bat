@echo off
setlocal
title Vinylia - Lanceur

REM ============================================================
REM  VINYLIA - Demarrage complet du projet
REM  Lance : conteneur RabbitMQ + API + Scanner/Extracteur/Uploader + Web
REM  Pre-requis : Docker Desktop (moteur) deja demarre, PostgreSQL demarre.
REM ============================================================

set "ROOT=%~dp0"
set "BACK=%ROOT%back"
set "FRONT=%ROOT%front"
set "JAR=target\mp3-0.0.1-SNAPSHOT.jar"

REM Intervalle de scan du dossier incoming (ms). Sujet : 300000 = 5 min.
REM 15000 = 15 s (plus reactif pour l'usage courant). Modifiez si besoin.
set "SCAN_MS=15000"

echo ============================================
echo    VINYLIA - Demarrage complet
echo ============================================
echo.

REM --- 0. Build backend (toujours) pour embarquer les dernieres modifs du code ---
REM    (avant : build uniquement si le jar etait absent -> les changements de code
REM     n'etaient jamais repackages, le jar restait perime).
echo [build] Construction du backend ^(Maven^), patientez...
pushd "%BACK%"
call mvnw.cmd clean package -DskipTests
popd
if not exist "%BACK%\%JAR%" (
  echo [build] ECHEC de la construction du backend. Arret.
  pause
  exit /b 1
)

REM --- 0bis. Dependances front si absentes ---
if not exist "%FRONT%\node_modules" (
  echo [build] Installation des dependances front ^(npm install^), patientez...
  pushd "%FRONT%"
  call npm install
  popd
)

REM --- 1. RabbitMQ (conteneur uniquement, pas le moteur Docker) ---
echo [1/5] RabbitMQ...
docker start rabbitmq >nul 2>&1
if errorlevel 1 (
  echo        Premier lancement : creation du conteneur RabbitMQ...
  docker run -d --name rabbitmq --hostname rabbit -p 5672:5672 -p 15672:15672 --entrypoint sh rabbitmq:3-management -c "chmod 700 /var/lib/rabbitmq; printf mp3cookie123 > /var/lib/rabbitmq/.erlang.cookie; chown -R rabbitmq:rabbitmq /var/lib/rabbitmq; chmod 600 /var/lib/rabbitmq/.erlang.cookie; exec gosu rabbitmq rabbitmq-server"
)
echo        Attente du broker...
timeout /t 10 /nobreak >nul

REM --- 2. API (port 8080) ---
echo [2/5] API ^(port 8080^)...
start "Vinylia - API" /D "%BACK%" cmd /k java -jar %JAR%
echo        Attente du demarrage de l'API...
timeout /t 25 /nobreak >nul

REM --- 3. Les 3 programmes standalone ---
echo [3/5] Scanner / Extracteur / Uploader...
start "Vinylia - Extracteur" /D "%BACK%" cmd /k java -jar %JAR% --spring.profiles.active=extractor
start "Vinylia - Uploader"   /D "%BACK%" cmd /k java -jar %JAR% --spring.profiles.active=uploader
start "Vinylia - Scanner"    /D "%BACK%" cmd /k java -jar %JAR% --spring.profiles.active=scanner --app.scan.interval-ms=%SCAN_MS% --app.scan.initial-delay-ms=2000
timeout /t 3 /nobreak >nul

REM --- 4. Application web (port 5173) ---
echo [4/5] Application web ^(port 5173^)...
start "Vinylia - Web" /D "%FRONT%" cmd /k npm run dev
timeout /t 8 /nobreak >nul

REM --- 5. Navigateur ---
@REM echo [5/5] Ouverture du navigateur...
@REM start "" http://localhost:5173

echo.
echo ============================================
echo  Tout est lance.
echo    Web      : http://localhost:5173
echo    API      : http://localhost:8080
echo    RabbitMQ : http://localhost:15672  (guest / guest)
echo    Deposez vos .mp3 dans : %BACK%\incoming
echo  Pour tout arreter : double-cliquez sur shutdown.bat
echo ============================================
echo.
endlocal
