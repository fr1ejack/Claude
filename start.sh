#!/usr/bin/env sh
# Startet einen lokalen Webserver und öffnet J.A.R.V.I.S. im Browser
PORT=${PORT:-8000}
cd "$(dirname "$0")"
echo "J.A.R.V.I.S. läuft auf http://localhost:$PORT  (Beenden mit Strg+C)"
( sleep 1; (xdg-open "http://localhost:$PORT" || open "http://localhost:$PORT") >/dev/null 2>&1 ) &
python3 -m http.server "$PORT"
