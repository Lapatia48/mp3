#!/usr/bin/env bash
# ============================================================
# Lance les 3 programmes standalone (Scanner, Extracteur, Uploader)
# en arriere-plan. C'est ce qui fait fonctionner l'import automatique
# des MP3 deposes dans back/incoming/.
#
# Pre-requis : RabbitMQ demarre (bash tools/start-rabbitmq.sh) ET l'API
# lancee (java -jar target/mp3-0.0.1-SNAPSHOT.jar).
#
# Usage :
#   bash tools/start-pipeline.sh            # intervalle de scan = 5 min (defaut sujet)
#   bash tools/start-pipeline.sh 5000       # scan toutes les 5 s (pratique pour tester)
#
# Logs : back/logs/{scanner,extractor,uploader}.log
# Arret : bash tools/stop-pipeline.sh   (ou fermez les processus java)
# ============================================================
set -u
cd "$(dirname "$0")/.."          # -> back/
JAR=target/mp3-0.0.1-SNAPSHOT.jar
INTERVAL="${1:-300000}"          # ms

if [ ! -f "$JAR" ]; then
  echo "Jar introuvable. Lancez d'abord : ./mvnw clean package -DskipTests" >&2
  exit 1
fi

start() {
  local profile="$1"; shift
  nohup java -jar "$JAR" --spring.profiles.active="$profile" "$@" > "logs/run-$profile.out" 2>&1 &
  echo "  - $profile demarre (PID $!)"
}

echo "Demarrage des 3 programmes (intervalle de scan : ${INTERVAL} ms)..."
start extractor
start uploader
start scanner --app.scan.interval-ms="$INTERVAL" --app.scan.initial-delay-ms=2000

echo
echo "OK. Deposez des .mp3 dans : back/incoming/"
echo "Logs : back/logs/scanner.log, extractor.log, uploader.log"
