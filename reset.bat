@echo off
set PGPASSWORD=lapatia1706
psql -h localhost -p 5432 -U postgres -d mp3 -c "TRUNCATE TABLE playlist_tracks, playlists, tracks, users RESTART IDENTITY CASCADE;"
if exist back\logs rmdir /s /q back\logs
if exist back\storage rmdir /s /q back\storage
if exist back\blacklisted rmdir /s /q back\blacklisted
echo Reset termine.
