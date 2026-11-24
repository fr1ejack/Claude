# SDG 7 – Erklärvideo, 9-Minuten-Fassung

Animiertes Erklärvideo (9:00 min, 1920×1080, 30 fps) im gleichen Stil wie die Kurzfassung – drei Teile à 3 Minuten mit eigenem Unterthema, ohne Voice-Over (Text als Untertitel, Sprechertext für drei Personen in `SPRECHERTEXT.md`).

| Teil | Zeit | Thema | Szenen |
|---|---|---|---|
| 1 | 0:00–3:00 | Energie im Alltag & Was ist SDG 7? | Hook mit Blackout, 700 Mio. ohne Strom, Folgen, Kochen über offenem Feuer, Energie im Alltag & Verkehr, SDG-7-Kachel, 17 Ziele, Zusammenhänge, Versprechen, 5 Unterziele, Fortschritt |
| 2 | 3:00–6:00 | Das Problem & die Lösungen | Kohle/Öl/Gas, Treibhauseffekt, Anteil der Energie an Treibhausgasen, Preise & Krisen, die 3 Kernziele mit Mini-Grids, sauberen Kochern, erneuerbaren Quellen, Deutschland, Speicher & Netze, Effizienz |
| 3 | 6:00–9:00 | Warum es uns alle betrifft & was wir tun können | Krankenhäuser, Schulen, Arbeit, Klima ohne Grenzen, Energiewende in Deutschland, Tipps (Licht, Heizung, Stand-by, Verkehr, Ökostrom, Schule), Zusammenfassung |

## Dateien
- `sdg7-video-9min.mp4` – fertiges Video mit Untertiteln und Musik
- `index.html` – Animation (im Browser öffnen = Vorschau; `?t=200` springt zu Sekunde 200)
- `script.js` – Drehbuch: alle Sätze, Szenen und die automatische Zeitberechnung
- `lib.js`, `scenes1.js`, `scenes2.js`, `scenes3.js` – Figuren, Icons und die Szenen der drei Teile
- `music.py` – erzeugt `music.wav` passend zu `timeline.json`
- `render.js` – `node render.js check` (Standbilder je Szene) bzw. `node render.js video`
