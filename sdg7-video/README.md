# SDG 7 – Erklärvideo („Wie schaffen wir die globale Energiewende?“)

Animiertes Erklärvideo (ca. 2:00 min, 1920×1080, 30 fps, Voice-Over von drei Sprecher:innen) im flachen Vektor-Stil mit runden Figuren.

- `sdg7-video.mp4` – fertiges Video mit Untertiteln und Hintergrundmusik
- `animation.html` – die Animation (im Browser öffnen = Vorschau, Klick startet mit Musik, `?t=75` springt zu Sekunde 75)
- `render.js` – rendert das Video neu: `node render.js video` (benötigt Playwright + ffmpeg)
- `music.py` – erzeugt die Hintergrundmusik `music.wav`

## Ablauf (Zeiten der ursprünglichen 2:50-Fassung; die Endfassung ist über `timemap.js` gestrafft)

| Zeit | Szene |
|---|---|
| 0:00–0:20 | Hook: Handy lädt, Zug fährt, Stadtlichter gehen an – Blackout, nur eine Kerze |
| 0:20–0:45 | SDG-7-Kachel erscheint, danach die Erde mit den 17 UN-Zielen (Ziel 7 hervorgehoben) |
| 0:45–1:15 | Problem: Kohlekraftwerk, CO₂ legt sich um die Erde, Preise steigen, Titelfrage |
| 1:15–2:00 | Die drei Kernziele: Zugang für alle · Erneuerbare Energie · Energieeffizienz |
| 2:00–2:30 | Warum es uns alle betrifft: Sonne → Krankenhaus, Schule/Laptop, Fabrik |
| 2:30–2:50 | Call to Action: Licht aus, Heizung runter, Ökostrom – Outro mit SDG-7-Kachel |

## Sprechertext (für ein Voice-Over, passend zu den Untertiteln)

- **0:00** Morgens: Handy laden.
- **0:05** Mit dem Zug zur Schule.
- **0:10** Abends gehen überall die Lichter an.
- **0:15** Und plötzlich … ist der Strom weg.
- **0:20** Für fast 700 Millionen Menschen ist das Alltag – sie haben gar keinen Strom.
- **0:26** Genau darum geht es in SDG 7: Bezahlbare und saubere Energie.
- **0:32** Es ist eines von 17 Zielen, die die Vereinten Nationen 2015 beschlossen haben.
- **0:38** Bis 2030 soll die Welt gerechter und nachhaltiger werden – für alle.
- **0:45** Das Problem: Ein großer Teil unserer Energie kommt noch aus Kohle, Öl und Gas.
- **0:52** Beim Verbrennen entsteht CO₂. Es legt sich wie eine Decke um die Erde – der Treibhauseffekt.
- **1:00** Gleichzeitig wird Energie für viele Menschen immer teurer.
- **1:07** Wie schaffen wir also die globale Energiewende?
- **1:15** SDG 7 hat drei Kernziele.
- **1:20** Erstens: Jeder Mensch soll Zugang zu bezahlbarem und verlässlichem Strom haben.
- **1:32** Zweitens: Viel mehr Energie aus erneuerbaren Quellen – wie Wind und Sonne.
- **1:46** Drittens: Energie effizienter nutzen. Eine LED leuchtet genauso hell, braucht aber viel weniger Strom.
- **2:00** Warum betrifft das uns alle?
- **2:07** Krankenhäuser brauchen Strom, um Leben zu retten.
- **2:14** Schulen brauchen Strom – für Licht, Computer und Lernen.
- **2:21** Und Fabriken brauchen Strom für Arbeitsplätze.
- **2:30** Was kannst du tun? Licht aus, wenn du den Raum verlässt.
- **2:35** Die Heizung ein bisschen runterdrehen.
- **2:39** Oder zu Ökostrom wechseln.
- **2:44** Jede Kilowattstunde zählt. SDG 7 – Energie für alle.

Hinweis: Die Zahl „fast 700 Millionen“ (Menschen ohne Stromzugang) bezieht sich auf die Berichte der letzten Jahre (Tracking SDG 7) – vor der Abgabe kurz mit der aktuellen Zahl abgleichen.

## Neu bauen

1. `python3 voice/cut.py` – schneidet die Sätze, berechnet die gestraffte Zeitachse (`timemap.js/json`) und die Sprachspur
2. `python3 music.py` – Musik passend zur Zeitachse
3. Stimme + Musik mischen (`mix.wav`, mit ffmpeg sidechaincompress)
4. `node render.js video`
