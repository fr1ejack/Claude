@echo off
cd /d "%~dp0"
echo J.A.R.V.I.S. laeuft auf http://localhost:8000  (Beenden mit Strg+C)
start "" http://localhost:8000
python -m http.server 8000
