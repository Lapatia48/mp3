#!/usr/bin/env bash
# Test du chemin SUCCES : mock API -> 200 -> fichier supprime de incoming/
set -u
cd "$(dirname "$0")/.."   # -> back/
JAR=target/mp3-0.0.1-SNAPSHOT.jar
PIDS=()

cleanup() {
  for p in "${PIDS[@]}"; do kill "$p" >/dev/null 2>&1; done
  kill "$MOCK_PID" >/dev/null 2>&1
}
trap cleanup EXIT

# Reset
rm -f incoming/*.mp3 failed/*.mp3 logs/*.log 2>/dev/null
mkdir -p incoming failed logs

# Mock API (port 8080)
python tools/mock_api.py > logs/mock-api.log 2>&1 &
MOCK_PID=$!

COMMON="--spring.rabbitmq.host=localhost"
java -jar $JAR --spring.profiles.active=extractor $COMMON > logs/run-extractor.out 2>&1 &
PIDS+=($!)
java -jar $JAR --spring.profiles.active=uploader  $COMMON --app.upload.max-retries=3 --app.upload.retry-delay-ms=1000 > logs/run-uploader.out 2>&1 &
PIDS+=($!)
java -jar $JAR --spring.profiles.active=scanner    $COMMON --app.scan.interval-ms=3000 --app.scan.initial-delay-ms=1000 > logs/run-scanner.out 2>&1 &
PIDS+=($!)

# Laisser les contextes Spring demarrer
for i in $(seq 1 20); do
  grep -q "Started Mp3Application" logs/run-uploader.out 2>/dev/null && \
  grep -q "Started Mp3Application" logs/run-extractor.out 2>/dev/null && break
  sleep 1
done

# Deposer le fichier a importer
cp tools/song1.mp3 incoming/song1.mp3
echo "ceci n'est pas un mp3" > incoming/notes.txt   # doit etre ignore

# Attendre la fin de traitement ou timeout
RESULT="TIMEOUT"
for i in $(seq 1 30); do
  if [ ! -f incoming/song1.mp3 ]; then
    if [ -f failed/song1.mp3 ]; then RESULT="MOVED_TO_FAILED (echec)"; else RESULT="DELETED (succes)"; fi
    break
  fi
  sleep 1
done

echo "=================== RESULTAT : $RESULT ==================="
echo "--- incoming/ ---"; ls incoming/
echo "--- failed/ ---"; ls failed/
echo "--- mock-api.log ---"; cat logs/mock-api.log
echo "--- scanner.log ---"; cat logs/scanner.log 2>/dev/null | grep -iE "scan|detect" | head
echo "--- extractor.log ---"; cat logs/extractor.log 2>/dev/null | grep -iE "extraction|succes|erreur" | head
echo "--- uploader.log ---"; cat logs/uploader.log 2>/dev/null | grep -iE "envoi|succes|suppression|echec" | head
