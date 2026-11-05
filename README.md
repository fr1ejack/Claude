# J.A.R.V.I.S. – holografischer KI-Assistent

Ein Jarvis-artiger Sprachassistent im Browser mit blauem, holografischem 3D-Design (Three.js + Bloom).

## Veröffentlichen (für alle nutzbar)

Die App ist eine **installierbare Web-App (PWA)**: Andere öffnen einfach den Link und können Jarvis
auf PC, Android oder iPhone wie eine normale App installieren (Chrome/Edge: Knopf „⤓ APP INSTALLIEREN“ oder
Adressleiste → Installieren · iPhone: Teilen → „Zum Home-Bildschirm“).

**Mit GitHub Pages (kostenlos):**
1. Branch in `main` mergen – der Workflow `.github/workflows/pages.yml` veröffentlicht dann automatisch.
2. Einmalig auf GitHub: *Settings → Pages → Source: „GitHub Actions“*.
3. Die Adresse lautet dann `https://fr1ejack.github.io/Claude/`.

> Hinweis: GitHub Pages für **private** Repos gibt es nur mit GitHub Pro/Team. Kostenlose Alternativen:
> Repo auf *öffentlich* stellen, oder den Ordner bei [Netlify Drop](https://app.netlify.com/drop) bzw.
> Cloudflare Pages hochladen (einfach den Projektordner hineinziehen). HTTPS ist Pflicht – sonst gehen Mikrofon und Standort nicht.

## Starten (lokal)

Die App braucht einen lokalen Webserver (wegen ES-Modulen, Mikrofon und Standort):

- **Windows:** `start.bat` doppelklicken
- **macOS / Linux:** `./start.sh`
- oder manuell: `python -m http.server 8000` → <http://localhost:8000>

Empfohlen ist **Chrome oder Edge** (Spracherkennung). In Firefox funktioniert alles per Texteingabe.
Beim Start auf **SYSTEM STARTEN** klicken und Mikrofon und Standort erlauben.

## Was Jarvis kann

| Sagen / tippen | Ergebnis |
|---|---|
| „Wo bin ich?“ | GPS-/Browser-Ortung (Fallback: IP), Marker auf dem Globus |
| „Wo ist Dortmund?“ / „Zeig mir Tokio“ | Die Erde fliegt aus der Tiefe heran, dreht sich zum Ziel und zoomt hinein. Nah dran erscheint eine holografische Straßenkarte, die direkt **auf der Kugel** liegt |
| „Flüge über Frankfurt“ / „Flugzeuge über mir“ | Live-Flugzeuge (ADS-B) im Umkreis, Liste mit Airline, Typ, Höhe und Route |
| „Suche Flug LH400“ / „DLH4AB“ | Flugsuche per Flugnummer, Callsign oder Kennzeichen. Die Kamera folgt dem Flugzeug, die Route wird als Bogen gezeichnet |
| „Wo fliegen gerade A380?“ | Suche nach Flugzeugtyp weltweit (A380, 747, 787, A350 …) |
| „Zeig mir den Flugverkehr“ | Flugverkehr-Ebene an. Weit weg: weltweit (OpenSky), nah dran: regional (adsb.lol) |
| „Zeig mir einen PS5-Controller“ | Holografisches 3D-Modell, das sich von unten nach oben aufbaut |
| „Öffne ihn“ / „Schließe ihn“ | Animierte Explosionsansicht mit beschrifteten Einzelteilen (Platine, Akku, Haptik-Aktuatoren, Trigger-Motoren …) |
| „Zeig mir eine Drohne“ / „ein Smartphone“ | Weitere zerlegbare Hologramme |
| „Was ist ein Schwarzes Loch?“ | Wikipedia-Zusammenfassung, das Bild wird als Hologramm projiziert |
| „Wetter in Berlin“ | Aktuelles Wetter (Open-Meteo) |
| „Zurück“ | Zurück zum KI-Kern |
| *jede andere Frage* | Mit aktivem **KI-Kern** recherchiert Jarvis selbst im Internet und antwortet frei |
| „Merke dir, dass …“ / „Was weißt du über mich?“ | Persönliches Gedächtnis |
| „Ab jetzt …“ / „Hör auf …“ | Eigene Regeln, die Jarvis immer befolgt (auch im 🧠-Menü editierbar) |

## KI-Kern – freies Reden ohne API-Key

Über **🧠 KI-KERN** wird ein offenes Sprachmodell (Qwen 2.5, wahlweise 1,5 / 3 / 7 Milliarden Parameter) per
[WebLLM](https://webllm.mlc.ai) **direkt im Browser** auf der Grafikkarte ausgeführt – kein API-Key, keine Kosten,
keine Server. Beim ersten Mal wird das Modell heruntergeladen (≈ 1–5 GB), danach startet es aus dem Cache.

Vor jeder Antwort recherchiert Jarvis selbst (Wikipedia de/en, DuckDuckGo) und speichert das Gelernte als
„Erfahrungen“ in seinem Gedächtnis, das er bei späteren Fragen wieder nutzt. Gedächtnis und Regeln liegen
im Browser des jeweiligen Nutzers – jeder hat seinen eigenen Jarvis.

Voraussetzung: WebGPU (aktuelles Chrome/Edge auf PC, Mac, Android; Safari ab iOS 26). **💬 GESPRÄCH**
schaltet den Dauergesprächsmodus ein: Nach jeder Antwort hört Jarvis automatisch wieder zu.

Weitere Bedienung: **Leertaste** = Sprechen, **„JARVIS“-WECKWORT** = dauerhaft zuhören („Jarvis, wo ist Paris?“).
Globus und Hologramme lassen sich mit der Maus ziehen und mit dem Mausrad zoomen. Ein Klick auf ein Flugzeug zeigt dessen Details.

## Datenquellen (kostenlos, ohne API-Key)

- **Flüge:** [adsb.lol](https://adsb.lol) (Positionen und Routen), Fallback [OpenSky Network](https://opensky-network.org). Flightradar24 bietet keine freie öffentliche API. Diese Netzwerke nutzen dieselben ADS-B-Empfängerdaten.
- **Orte:** OpenStreetMap Nominatim · **Karte:** CARTO Dark Tiles (holografisch eingefärbt)
- **Geodaten Globus:** Natural Earth über `world-atlas`
- **Wissen:** Wikipedia (de) · **Wetter:** Open-Meteo

## Aufbau

```
index.html        HUD-Layout
css/style.css     holografisches Design
js/main.js        Renderer, Kamera-Flüge, Übergänge, Eingabe
js/globe.js       Punkt-Globus, Küsten/Grenzen, Atmosphäre, Kachelkarte auf der Kugel, Marker
js/flights.js     Flugzeug-Instanzen mit Koppelnavigation, Routenbögen, Klick-Auswahl
js/objects.js     prozedurale, zerlegbare Hologramme (Controller, Smartphone, Drohne)
js/core.js        KI-Kern & Projektor-Plattform
js/assistant.js   Sprachein-/ausgabe (Web Speech API), Befehlserkennung
js/services.js    Datenquellen (Orte, Flüge, Wetter, Wikipedia, Web-Recherche)
js/brain.js       lokales Sprachmodell (WebLLM) + System-Prompt
js/memory.js      Gedächtnis: Fakten, Erfahrungen, Regeln
sw.js, manifest.webmanifest, icons/   installierbare App (PWA)
```

Neue Hologramme kommen in `js/objects.js` hinzu: ein Builder mit Einzelteilen und Explosions-Versatz, dazu ein Eintrag in `LIBRARY`.
