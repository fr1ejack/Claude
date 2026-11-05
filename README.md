# J.A.R.V.I.S. – holografischer KI-Assistent

Ein Jarvis-artiger Sprachassistent im Browser mit blauem, holografischem 3D-Design (Three.js + Bloom).

## Starten

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
js/services.js    Datenquellen (Orte, Flüge, Wetter, Wikipedia)
```

Neue Hologramme kommen in `js/objects.js` hinzu: ein Builder mit Einzelteilen und Explosions-Versatz, dazu ein Eintrag in `LIBRARY`.
