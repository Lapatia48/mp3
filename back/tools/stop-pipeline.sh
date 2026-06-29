#!/usr/bin/env bash
# Arrete les 3 programmes standalone (scanner, extracteur, uploader).
# N'arrete PAS l'API par defaut. Sous Windows (Git Bash) on filtre par profil.
echo "Arret des programmes standalone (scanner/extractor/uploader)..."
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'spring.profiles.active=(scanner|extractor|uploader)' } | ForEach-Object { Write-Host ('  - arret PID ' + \$_.ProcessId); Stop-Process -Id \$_.ProcessId -Force }"
echo "Termine."
