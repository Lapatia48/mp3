#!/usr/bin/env bash
# Test live de l'API (Partie 2) : auth, upload, CRUD, playlists, ZIP.
set -u
cd "$(dirname "$0")/.."   # -> back/
JAR=target/mp3-0.0.1-SNAPSHOT.jar
BASE=http://localhost:8080

cleanup() { kill "$API_PID" >/dev/null 2>&1; }
trap cleanup EXIT

rm -f logs/api.log logs/run-api.out 2>/dev/null
# Repartir d'une base propre pour un test reproductible
java -jar "$JAR" > logs/run-api.out 2>&1 &
API_PID=$!

echo "Demarrage de l'API..."
for i in $(seq 1 40); do
  grep -q "Started Mp3Application" logs/run-api.out 2>/dev/null && break
  if ! kill -0 "$API_PID" 2>/dev/null; then echo "L'API s'est arretee !"; tail -30 logs/run-api.out; exit 1; fi
  sleep 1
done
echo "API prete."

json() { python -c "import sys,json;print(json.load(sys.stdin)$1)"; }
U="user_$RANDOM"

echo; echo "=== 1) Register ($U) ==="
REG=$(curl -s -X POST $BASE/api/auth/register -H "Content-Type: application/json" \
  -d "{\"username\":\"$U\",\"email\":\"$U@test.com\",\"password\":\"secret123\"}")
echo "$REG"
TOKEN=$(echo "$REG" | json "['token']")

echo; echo "=== 2) Login ==="
curl -s -X POST $BASE/api/auth/login -H "Content-Type: application/json" \
  -d "{\"username\":\"$U\",\"password\":\"secret123\"}" | json "['username']"

echo; echo "=== 3) Protection : GET /api/playlists SANS token ==="
echo "HTTP $(curl -s -o /dev/null -w '%{http_code}' $BASE/api/playlists)"

echo; echo "=== 4) Upload song1.mp3 + song2.mp3 (comme le Programme 3) ==="
for f in song1 song2; do
  curl -s -X POST $BASE/api/tracks/upload \
    -F "file=@tools/$f.mp3;type=audio/mpeg" \
    -F "metadata={\"fileName\":\"$f.mp3\",\"title\":\"T_$f\",\"artist\":\"A_$f\",\"album\":\"Alb\",\"genre\":\"Rock\",\"duration\":180,\"year\":\"1971\"};type=application/json"
  echo
done

echo; echo "=== 4b) Idempotence : re-upload song1.mp3 (doit renvoyer le meme id) ==="
curl -s -X POST $BASE/api/tracks/upload \
  -F "file=@tools/song1.mp3;type=audio/mpeg" \
  -F "metadata={\"fileName\":\"song1.mp3\",\"title\":\"T_song1\"};type=application/json" | json "['id']"

echo; echo "=== 5) GET /api/tracks (avec token) ==="
TRACKS=$(curl -s $BASE/api/tracks -H "Authorization: Bearer $TOKEN")
echo "$TRACKS"
ID1=$(echo "$TRACKS" | json "[0]['id']")

echo; echo "=== 6) Stream track $ID1 (entetes) ==="
curl -s -o /dev/null -D - $BASE/api/tracks/$ID1/stream | grep -iE "HTTP/|content-type|content-disposition"

echo; echo "=== 7) Generate playlist (1 min) ==="
GEN=$(curl -s -X POST $BASE/api/playlists/generate -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"durationMinutes":1}')
echo "$GEN"

echo; echo "=== 8) Save playlist (tous les morceaux) ==="
IDS=$(echo "$TRACKS" | python -c "import sys,json;print(','.join(str(t['id']) for t in json.load(sys.stdin)))")
SAVE=$(curl -s -X POST $BASE/api/playlists -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d "{\"name\":\"Ma playlist\",\"trackIds\":[$IDS]}")
echo "$SAVE"
PID=$(echo "$SAVE" | json "['id']")

echo; echo "=== 9) GET /api/playlists (avec token) ==="
curl -s $BASE/api/playlists -H "Authorization: Bearer $TOKEN" | json "[0]['name']"

echo; echo "=== 10) Download ZIP de la playlist $PID ==="
curl -s -o logs/playlist.zip $BASE/api/playlists/$PID/download -H "Authorization: Bearer $TOKEN"
python -c "import zipfile;z=zipfile.ZipFile('logs/playlist.zip');print('ZIP OK, contenu =', z.namelist())"

echo; echo "=== 11) Update playlist (retirer un morceau) ==="
curl -s -X PUT $BASE/api/playlists/$PID -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d "{\"name\":\"Ma playlist (modifiee)\",\"trackIds\":[$ID1]}" | json "['totalDuration']"

echo; echo "=== 12) DELETE playlist ==="
echo "HTTP $(curl -s -o /dev/null -w '%{http_code}' -X DELETE $BASE/api/playlists/$PID -H "Authorization: Bearer $TOKEN")"

echo; echo "=== TERMINE ==="
