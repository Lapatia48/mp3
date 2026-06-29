@echo off
set PGPASSWORD=lapatia1706
psql -h localhost -p 5432 -U postgres -d mp3 -c "TRUNCATE TABLE playlist_tracks, playlists, tracks, users RESTART IDENTITY CASCADE;"
echo Reset termine.
