@echo off
setlocal
title Vinylia - Reset RabbitMQ

REM ============================================================
REM  VINYLIA - Reset RabbitMQ (files et messages)
REM  A lancer APRES reset.bat, dans la sequence :
REM     start.bat -> stop.bat -> reset.bat -> resetRabbit.bat -> start.bat
REM  A ce stade le broker est eteint (arrete par stop.bat) : ce script
REM  le redemarre (comme start.bat), supprime toutes les files (donc
REM  aussi leurs messages), puis le reeteint (comme stop.bat).
REM  Les files sont recreees vides au prochain start.bat.
REM ============================================================

echo ============================================
echo    VINYLIA - Reset RabbitMQ
echo ============================================
echo.

REM --- 1. Demarrer RabbitMQ (identique a start.bat) ---
echo [1/3] Demarrage de RabbitMQ...
docker start rabbitmq >nul 2>&1
if errorlevel 1 (
  echo        Premier lancement : creation du conteneur RabbitMQ...
  docker run -d --name rabbitmq --hostname rabbit -p 5672:5672 -p 15672:15672 --entrypoint sh rabbitmq:3-management -c "chmod 700 /var/lib/rabbitmq; printf mp3cookie123 > /var/lib/rabbitmq/.erlang.cookie; chown -R rabbitmq:rabbitmq /var/lib/rabbitmq; chmod 600 /var/lib/rabbitmq/.erlang.cookie; exec gosu rabbitmq rabbitmq-server"
  if errorlevel 1 (
    echo        ECHEC : impossible de demarrer RabbitMQ. Arret.
    goto fin
  )
)

REM --- 2. Attendre que le noeud soit pret, puis supprimer les files ---
echo [2/3] Attente du broker puis suppression des files...
set /a _tries=0
:wait
docker exec rabbitmq rabbitmqctl await_startup >nul 2>&1
if not errorlevel 1 goto ready
set /a _tries+=1
if %_tries% geq 30 (
  echo        RabbitMQ n'a pas repondu a temps : files non reinitialisees.
  goto stopbroker
)
REM Pause ~2s robuste (timeout echoue si l'entree est redirigee).
ping -n 3 127.0.0.1 >nul
goto wait

:ready
for %%q in (queue.scan queue.metadata queue.scanned queue.extracted queue.deleted queue.send queue.failed queue.dead-letter queue.notifications) do (
  docker exec rabbitmq rabbitmqctl delete_queue %%q >nul 2>&1
)
echo        Files RabbitMQ supprimees ^(recreees vides au prochain start.bat^).

REM --- 3. Reeteindre RabbitMQ (identique a stop.bat) ---
:stopbroker
echo [3/3] Arret de RabbitMQ...
docker stop rabbitmq >nul 2>&1

:fin
echo.
echo ============================================
echo    Reset RabbitMQ termine.
echo ============================================
echo.
ping -n 3 127.0.0.1 >nul
endlocal
