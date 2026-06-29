#!/usr/bin/env bash
# Integration complete : Programme 1+2+3 (RabbitMQ) -> API reelle -> PostgreSQL.
set -u
cd "$(dirname "$0")/.."   # -> back/
JAR=target/mp3-0.0.1-SNAPSHOT.jar
BASE=http://localhost:8080
PIDS=()
cleanup() { for p in "${PIDS[@]}"; do kill "$p" >/dev/null 2>&1; done; }
trap cleanup EXIT

rm -f incoming/*.mp3 failed/*.mp3 logs/*.log logs/*.out 2>/dev/null
mkdir -p incoming failed logs

# 1) API (profil par defaut) + 2) les 3 programmes standalone
java -jar "$JAR" > logs/run-api.out 2>&1 &                                           PIDS+=($!)
java -jar "$JAR" --spring.profiles.active=extractor > logs/run-extractor.out 2>&1 &  PIDS+=($!)
java -jar "$JAR" --spring.profiles.active=uploader  > logs/run-uploader.out 2>&1 &   PIDS+=($!)
java -jar "$JAR" --spring.profiles.active=scanner --app.scan.interval-ms=3000 --app.scan.initial-delay-ms=1000 > logs/run-scanner.out 2>&1 &  PIDS+=($!)

echo "Demarrage (API + 3 programmes)..."
for i in $(seq 1 45); do
  grep -q "Started Mp3Application" logs/run-api.out 2>/dev/null \
   && grep -q "Started Mp3Application" logs/run-uploader.out 2>/dev/null \
   && grep -q "Started Mp3Application" logs/run-scanner.out 2>/dev/null && break
  sleep 1
done
echo "Tout est demarre."

# Verifie que le scanner standalone tourne SANS base (JPA exclu)
echo; echo "--- Scanner boot sans DataSource (JPA exclu) ? ---"
grep -ciE "DataSource|HikariPool|Hibernate" logs/run-scanner.out | sed 's/^/lignes DataSource\/Hibernate dans le scanner : /'

# Depose un morceau a importer
cp tools/song1.mp3 incoming/song1.mp3
echo; echo "Fichier depose : incoming/song1.mp3"

# Attend la suppression (= succes complet du flux)
RES="TIMEOUT"
for i in $(seq 1 40); do
  if [ ! -f incoming/song1.mp3 ]; then
    [ -f failed/song1.mp3 ] && RES="ECHEC (failed/)" || RES="SUCCES (supprime de incoming/)"
    break
  fi
  sleep 1
done

echo; echo "=================== FLUX : $RES ==================="
echo "--- logs (extraits) ---"
grep -h "detecte" logs/scanner.log 2>/dev/null | tail -1
grep -h "extraites" logs/extractor.log 2>/dev/null | tail -1
grep -hE "succes|Suppression" logs/uploader.log 2>/dev/null | tail -2

# Verifie la persistance via l'API
U="itg_$RANDOM"
TOKEN=$(curl -s -X POST $BASE/api/auth/register -H "Content-Type: application/json" \
  -d "{\"username\":\"$U\",\"password\":\"secret123\"}" | python -c "import sys,json;print(json.load(sys.stdin)['token'])")
echo; echo "--- GET /api/tracks (persistance en base) ---"
curl -s $BASE/api/tracks -H "Authorization: Bearer $TOKEN"
echo
