# J.A.R.V.I.S. – holografischer KI-Assistent

Ein Jarvis-artiger Sprachassistent im Browser mit blauem, holografischem 3D-Design (Three.js + Bloom).

## Zwei Versionen

| | Private claude.ai-Version | Vollversion (eigene Website) |
|---|---|---|
| Wer kann sie öffnen? | Nur Personen, mit denen du den Link über *Teilen* auf claude.ai teilst | Nur eingeladene E-Mail-Adressen (Cloudflare Access) |
| Holo-Globus, Hologramme, Explosionsansicht | ✓ | ✓ |
| Städtesuche | ✓ (eingebaut, ~29.000 Städte) | ✓ (weltweit, jede Adresse) |
| Freie Antworten | ✓ über Claude (Konto des Betrachters) | ✓ lokales Modell im Browser |
| Live-Flüge, Wetter, Internet-Recherche | – | ✓ |
| Straßenkarte und **3D-Gebäude 1:1** (OpenStreetMap) | – | ✓ |
| Mikrofon, GPS, als App installierbar | – | ✓ |

Die private Version entsteht aus `artifact.html` (wird mit `python3 tools/build_artifact.py` aus `index.html` erzeugt).

## Vollversion privat veröffentlichen (Cloudflare, kostenlos)

Das Repo kann dabei **privat bleiben**.

1. Auf <https://dash.cloudflare.com> kostenlos registrieren.
2. *Workers & Pages → Erstellen → Pages → Mit Git verbinden* → GitHub verbinden → Repo `fr1ejack/Claude` wählen.
3. Produktionszweig: `main` (bzw. diesen Branch) · Framework: *Keins* · Build-Befehl: leer lassen · Ausgabeverzeichnis: `/` → *Speichern und bereitstellen*.
   Du bekommst eine Adresse wie `https://jarvis-xyz.pages.dev`.
4. Zugriff beschränken: *Zero Trust → Access → Anwendungen → Hinzufügen → Self-hosted* →
   Domain = deine `….pages.dev`-Adresse → Richtlinie *Allow* mit *Emails* = die Adressen deiner Freunde (und deine).
5. Ab jetzt muss jeder, der den Link öffnet, einen Code eingeben, der an seine E-Mail geht. Nur eingeladene Adressen kommen rein.
6. Auf dem Handy: Link öffnen → iPhone: Teilen → „Zum Home-Bildschirm“ · Android: Menü → „App installieren“.

**3D-Gebäude:** Beim Heranzoomen unter ca. 25 km Höhe lädt Jarvis die Gebäude der Umgebung aus OpenStreetMap
(Grundriss + Höhe bzw. Stockwerke) und baut sie als Hologramm auf, z. B. „Zeig mir den Signal Iduna Park“.

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
