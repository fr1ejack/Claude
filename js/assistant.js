import * as S from './services.js';
import * as ui from './ui.js';
import { findObject } from './objects.js';
import { fmt, fmtCoord, haversine, compass, esc } from './util.js';

const STRIP_START = /^(hey |ok |okay |hallo )?(jarvis)[,!. ]*/;
const FILLER = /\b(bitte|mal|doch|kurz|eigentlich|jetzt|gerade|einmal)\b/g;

function cleanSubject(s) {
  return s
    .replace(/^(mir |uns )/, '')
    .replace(/^(bitte |mal |doch )+/, '')
    .replace(/^(nach|zu|zum|zur|in|im|auf|bei|über|ueber|um|nahe|an|am)\s+/, '')
    .replace(/^(den|die|das|der|dem|ein|eine|einen|einem|einer)\s+/, '')
    .replace(/\s+(ist|liegt|befindet sich|aus|an|auf der karte|für mich|fuer mich|bitte|an der karte|so)$/g, '')
    .replace(/[?!.]+$/, '')
    .trim();
}

export class Assistant {
  constructor(app) {
    this.app = app;
    this.voiceOn = true;
    this.speaking = false;
    this.listening = false;
    this.voice = null;
    this.queue = Promise.resolve();
    this.pickVoice();
    if ('speechSynthesis' in window) speechSynthesis.onvoiceschanged = () => this.pickVoice();
    this.initRecognition();
  }

  /* ---------------- Sprache aus ---------------- */
  pickVoice() {
    if (!('speechSynthesis' in window)) return;
    const vs = speechSynthesis.getVoices().filter(v => v.lang && v.lang.toLowerCase().startsWith('de'));
    this.voice = vs.find(v => /male|männ|conrad|stefan|markus|daniel|google deutsch/i.test(v.name)) || vs[0] || null;
  }
  say(text, { speak = true } = {}) {
    ui.log('ai', text);
    if (!speak || !this.voiceOn || !('speechSynthesis' in window)) return Promise.resolve();
    return new Promise(res => {
      try {
        speechSynthesis.cancel();
        const u = new SpeechSynthesisUtterance(text.replace(/[„“"]/g, ''));
        u.lang = 'de-DE'; if (this.voice) u.voice = this.voice;
        u.rate = 1.04; u.pitch = 0.92;
        u.onstart = () => { this.speaking = true; ui.setState('SPRICHT', 'think'); };
        u.onend = u.onerror = () => { this.speaking = false; ui.setState('ONLINE'); res(); };
        speechSynthesis.speak(u);
        setTimeout(res, 25000);
      } catch { res(); }
    });
  }

  /* ---------------- Sprache ein ---------------- */
  initRecognition() {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    this.SR = SR;
    this.wake = false;
    if (!SR) return;
    this.rec = new SR();
    this.rec.lang = 'de-DE';
    this.rec.interimResults = true;
    this.rec.maxAlternatives = 1;
    this.rec.onresult = e => {
      let final = '', interim = '';
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const r = e.results[i];
        if (r.isFinal) final += r[0].transcript; else interim += r[0].transcript;
      }
      const input = document.getElementById('cmd');
      if (interim) input.value = interim;
      if (final) {
        input.value = '';
        if (this.wake) {
          const m = final.toLowerCase().match(/jarvis[,.!]?\s*(.*)$/);
          if (m) { if (m[1].trim()) this.handle(m[1]); else this.say('Ja?'); }
        } else this.handle(final);
      }
    };
    this.rec.onstart = () => { this.listening = true; ui.setState(this.wake ? 'WARTET AUF „JARVIS“' : 'HÖRT ZU …', 'listen'); document.getElementById('micBtn').classList.add('on'); };
    this.rec.onend = () => {
      this.listening = false; document.getElementById('micBtn').classList.remove('on');
      if (!this.speaking) ui.setState('ONLINE');
      if (this.wake) setTimeout(() => { try { this.rec.start(); } catch { /* läuft */ } }, 250);
    };
    this.rec.onerror = e => { if (e.error === 'not-allowed') { this.wake = false; this.say('Ich habe keinen Zugriff auf das Mikrofon.'); } };
  }
  listenOnce() {
    if (!this.rec) return this.say('Spracherkennung wird von diesem Browser nicht unterstützt. Nutzen Sie Chrome oder Edge – oder tippen Sie einfach.');
    speechSynthesis?.cancel();
    this.rec.continuous = false;
    try { this.rec.start(); } catch { this.rec.stop(); }
  }
  setWake(on) {
    if (!this.rec) { this.say('Spracherkennung wird von diesem Browser nicht unterstützt.'); return false; }
    this.wake = on;
    this.rec.continuous = on;
    try { if (on) this.rec.start(); else this.rec.stop(); } catch { /* */ }
    return on;
  }

  /* ---------------- Befehle ---------------- */
  handle(raw) {
    const text = String(raw || '').trim();
    if (!text) return;
    ui.log('user', text);
    this.queue = this.queue.then(() => this.process(text)).catch(err => {
      console.error(err);
      return this.say('Da ist leider etwas schiefgelaufen: ' + (err.message || err));
    }).finally(() => ui.setState(this.listening ? 'HÖRT ZU …' : 'ONLINE', this.listening ? 'listen' : ''));
    return this.queue;
  }

  async process(text) {
    ui.setState('VERARBEITE …', 'think');
    const app = this.app;
    let t = text.toLowerCase().replace(STRIP_START, '').replace(/[„“"]/g, '').replace(/ß/g, 'ss').trim();
    const tf = t.replace(FILLER, ' ').replace(/\s+/g, ' ').trim();

    // Begrüßung / Hilfe / Smalltalk
    if (/^(hallo|hi|hey|guten (morgen|tag|abend)|servus|moin|grüss gott|gruess gott)\b/.test(tf) && tf.split(' ').length <= 4)
      return this.say(`${greeting()}. Was kann ich für Sie tun?`);
    if (/(wer bist du|was kannst du|hilfe|was geht|funktionen)/.test(tf))
      return this.say('Ich bin J.A.R.V.I.S. Ich kann Ihren Standort bestimmen, jeden Ort der Welt auf dem Holo-Globus zeigen, Flugzeuge in Echtzeit verfolgen und suchen, das Wetter abrufen, Wissen nachschlagen und Objekte als Hologramm darstellen – zum Beispiel einen Game-Controller, den ich auch für Sie öffnen kann.');
    if (/(wie spät|wieviel uhr|wie viel uhr|uhrzeit)/.test(tf))
      return this.say(`Es ist ${new Date().toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })} Uhr.`);
    if (/(welcher tag|welches datum|datum heute|den wievielten)/.test(tf))
      return this.say(`Heute ist ${new Date().toLocaleDateString('de-DE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}.`);
    if (/^(danke|vielen dank|dankeschön)/.test(tf)) return this.say('Jederzeit gern.');

    // Objekt öffnen / schließen
    const lib = findObject(tf);
    if (/(öffne|oeffne|aufmachen|mach .*auf|zerleg|auseinander|explosions|innenleben|was ist (da )?drin|von innen|innere)/.test(tf) && !/(karte|globus|erde)/.test(tf)) {
      if (lib && (!app.obj || app.obj.key !== lib.key)) await app.showObject(lib);
      if (!app.obj || !app.obj.parts.length) return this.say('Es ist gerade kein Objekt geladen, das ich öffnen könnte. Sagen Sie zum Beispiel: Zeig mir einen PS5-Controller.');
      app.explode(true);
      return this.say(`Öffne ${app.obj.title}. Ich zerlege es in ${app.obj.parts.length} Komponenten: ${app.obj.parts.slice(0, 6).map(p => p.name).join(', ')} und weitere.`);
    }
    if (/(schliess|zusammen(bauen|setzen|fügen|fuegen)|mach .*zu\b|wieder zu)/.test(tf) && app.mode === 'object' && !/(karte|globus|erde)/.test(tf)) {
      app.explode(false);
      return this.say(`Setze ${app.obj.title} wieder zusammen.`);
    }
    if (/^(zurück|zurueck|schliessen|beenden|ausblenden|abbrechen|stopp?|home|kern|ruhemodus|standby)\b/.test(tf) || /(alles schliessen|schliesse (die )?(karte|globus|hologramm))/.test(tf)) {
      await app.showCore();
      return this.say('Zurück im Bereitschaftsmodus.');
    }

    // Eigener Standort
    if (/(wo bin ich|mein(en)? standort|wo befinde ich mich|meine position|wo sind wir)/.test(tf)) {
      const me = await app.myLocation(true);
      await app.goPlace({ name: me.city || 'Ihr Standort', lat: me.lat, lon: me.lon, km: me.acc > 3000 ? 40 : 6, kind: 'me', sub: me.region });
      return this.say(`Sie befinden sich in ${me.city}${me.region ? ', ' + me.region : ''}. Koordinaten ${fmt(me.lat, 4)} Grad Nord, ${fmt(me.lon, 4)} Grad Ost. Quelle: ${me.src}.`);
    }

    // Flüge
    const flightWord = /(flug|flüge|fluege|flugzeug|flieger|maschinen|luftverkehr|flugverkehr|airliner|jets?\b|überfl|ueberfl)/.test(tf);
    const cs = extractCallsign(text);
    if (flightWord || (cs && tf.split(' ').length <= 3)) {
      if (/(aus|ausblenden|verstecken|deaktivier|stopp)/.test(tf) && !/über|ueber|nach/.test(tf)) { app.setFlights(false); return this.say('Flugverkehr ausgeblendet.'); }
      if (cs) return this.findFlight(cs);
      const type = S.TYPE_WORDS.find(([re]) => re.test(tf));
      if (type) return this.findType(tf, type[1]);
      if (/(über mir|ueber mir|in meiner nähe|in der nähe|hier|um mich|bei mir|über uns)/.test(tf)) {
        const me = await app.myLocation();
        return this.flightsAt({ name: me.city || 'Ihrem Standort', lat: me.lat, lon: me.lon, km: 60, kind: 'me' });
      }
      const m = tf.match(/(?:über|ueber|bei|in|um|nahe|rund um|im raum|oberhalb von|von)\s+(.+)$/);
      if (m) {
        const subj = cleanSubject(m[1]);
        const place = await S.geocode(subj);
        if (!place) return this.say(`Ich konnte „${subj}“ nicht finden.`);
        return this.flightsAt({ ...place, km: S.spanKm(place.bbox) });
      }
      await app.setFlights(true, true);
      return this.say('Flugverkehr aktiviert. Ich zeige Ihnen den Luftraum in Echtzeit. Klicken Sie auf ein Flugzeug für Details – oder fragen Sie: Welche Flüge sind über Dortmund?');
    }

    // Wetter
    if (/(wetter|temperatur|regnet|wie warm|wie kalt|grad draussen)/.test(tf)) {
      const m = tf.match(/(?:in|für|fuer|bei|von)\s+(.+)$/);
      let place;
      if (m) place = await S.geocode(cleanSubject(m[1]));
      if (!place) { const me = await app.myLocation(); place = { name: me.city, lat: me.lat, lon: me.lon, km: 30 }; }
      const w = await S.weather(place.lat, place.lon);
      app.goPlace({ ...place, km: place.km || S.spanKm(place.bbox || { n: place.lat + .2, s: place.lat - .2, e: place.lon + .2, w: place.lon - .2 }) }, { quiet: true });
      const desc = S.WMO[w.weather_code] || 'unbekannt';
      ui.showPanel('WETTER · ' + (place.name || '').toUpperCase(), `<h2>${fmt(w.temperature_2m, 1)} °C</h2><p>${esc(desc)}</p>
        <div class="kv"><span>Gefühlt</span><span>${fmt(w.apparent_temperature, 1)} °C</span><span>Luftfeuchte</span><span>${fmt(w.relative_humidity_2m)} %</span><span>Wind</span><span>${fmt(w.wind_speed_10m)} km/h</span></div>`);
      return this.say(`In ${place.name} sind es aktuell ${fmt(w.temperature_2m, 1)} Grad, ${desc}. Gefühlt ${fmt(w.apparent_temperature, 0)} Grad, Wind mit ${fmt(w.wind_speed_10m)} km/h.`);
    }

    // Wissen
    let m = tf.match(/^(?:was|wer) (?:ist|sind|war|waren) (?:ein |eine |der |die |das )?(.+)$/) || tf.match(/^(?:erzähl|erzaehl|erkläre|erklaere|erklär) (?:mir )?(?:etwas |was )?(?:über |ueber )?(.+)$/);
    if (m) {
      const subj = cleanSubject(m[1]);
      const o = findObject(subj);
      if (o) { await app.showObject(o); return this.say(`${app.obj.title}: ${app.obj.subtitle}. ${app.obj.facts.join('. ')}. Sagen Sie „öffne ihn“, um ins Innere zu sehen.`); }
      return this.knowledge(subj, true);
    }

    // Zeigen / Hologramm
    m = tf.match(/wie sieht (?:ein |eine |einen |der |die |das )?(.+?) aus/) || tf.match(/(?:zeig|zeige|präsentier|praesentier|visualisier|projizier|hologramm (?:von|eines|einer)?|lade|öffne|oeffne)\s*(?:mir|uns)?\s*(.+)$/);
    if (m) {
      const raw = m[1].trim();
      const subj = cleanSubject(raw);
      const indefinite = /^(mir |uns )?(ein|eine|einen)\b/.test(raw) || /wie sieht/.test(tf);
      if (!subj || /^(die |der )?(erde|globus|welt|weltkarte|karte|planet)/.test(subj)) {
        await app.showGlobe();
        return this.say('Holografischer Globus aktiv. Wohin soll es gehen?');
      }
      const o = findObject(subj);
      if (o) {
        await app.showObject(o);
        return this.say(`Hier ist das Hologramm: ${app.obj.title}. ${app.obj.subtitle}. Sagen Sie „öffne ihn“, und ich zeige Ihnen die Einzelteile.`);
      }
      if (!indefinite) {
        const place = await S.geocode(subj);
        if (place && place.importance > 0.25) return this.showPlace(place);
      }
      return this.knowledge(subj, true);
    }

    // Ort suchen
    m = tf.match(/(?:wo (?:ist|liegt|befindet sich|sind|liegen)|wie komme ich (?:nach|zu)|suche?|finde|navigier(?:e)?(?: mich)?|flieg(?:e)?(?: mich)?|zoom(?:e)?|bring(?:e)? mich|gehe?|reise)\s+(.+)$/);
    if (m) {
      const subj = cleanSubject(m[1]);
      const o = findObject(subj);
      if (o) { await app.showObject(o); return this.say(`Hier ist das Hologramm: ${app.obj.title}.`); }
      const place = await S.geocode(subj);
      if (!place) return this.say(`Ich konnte „${subj}“ leider nicht finden.`);
      return this.showPlace(place);
    }

    // Letzter Versuch: Kurzer Text = Ort, sonst Wissen
    if (tf.split(' ').length <= 3) {
      const o = findObject(tf);
      if (o) { await app.showObject(o); return this.say(`Hier ist das Hologramm: ${app.obj.title}.`); }
      const place = await S.geocode(tf);
      if (place && place.importance > 0.3) return this.showPlace(place);
      return this.knowledge(tf, true);
    }
    const k = await S.wiki(tf).catch(() => null);
    if (k) return this.knowledge(tf, true, k);
    return this.say('Das habe ich leider nicht verstanden. Versuchen Sie: „Wo ist Dortmund?“, „Flüge über Frankfurt“ oder „Zeig mir einen PS5-Controller“.');
  }

  async showPlace(place) {
    const app = this.app;
    const km = S.spanKm(place.bbox);
    await app.goPlace({ ...place, km, sub: S.regionOf(place.addr) });
    const region = S.regionOf(place.addr);
    let dist = '';
    if (app.userLoc) {
      const d = haversine(app.userLoc.lat, app.userLoc.lon, place.lat, place.lon);
      dist = d < 1 ? ' Sie sind direkt dort.' : ` Von Ihrem Standort sind das etwa ${fmt(d < 20 ? d : Math.round(d / 10) * 10)} Kilometer Luftlinie.`;
    }
    ui.showPanel('ZIEL ERFASST', `<h2>${esc(place.name)}</h2><p>${esc(place.display)}</p>
      <div class="kv"><span>Koordinaten</span><span>${fmtCoord(place.lat, place.lon)}</span><span>Kategorie</span><span>${esc(place.type)}</span>${app.userLoc ? `<span>Entfernung</span><span>${fmt(haversine(app.userLoc.lat, app.userLoc.lon, place.lat, place.lon))} km</span>` : ''}</div>
      <p class="hint">Tipp: „Flüge über ${esc(place.name)}“ · „Wetter in ${esc(place.name)}“</p>`);
    return this.say(`${place.name} liegt ${region ? 'in ' + region : 'hier'}, bei ${fmt(Math.abs(place.lat), 2)} Grad ${place.lat >= 0 ? 'Nord' : 'Süd'} und ${fmt(Math.abs(place.lon), 2)} Grad ${place.lon >= 0 ? 'Ost' : 'West'}.${dist}`);
  }

  async knowledge(subj, holo, pre) {
    const k = pre || await S.wiki(subj).catch(() => null);
    if (!k) return this.say(`Zu „${subj}“ habe ich leider nichts gefunden.`);
    if (k.coords && /(stadt|gemeinde|land|staat|berg|see|fluss|insel|gebäude|turm|bauwerk)/i.test(k.extract || '')) {
      await this.app.goPlace({ name: k.title, lat: k.coords.lat, lon: k.coords.lon, km: 20 }, { quiet: true });
    } else if (holo) {
      await this.app.showImageHolo(k.title, k.image);
    }
    ui.showPanel('WISSENSDATENBANK', `${k.image ? `<img class="holo-img" src="${esc(k.image)}" alt="">` : ''}<h2>${esc(k.title)}</h2><p>${esc(k.extract)}</p>${k.url ? `<p class="hint"><a href="${esc(k.url)}" target="_blank" rel="noopener" style="color:inherit">Quelle: Wikipedia</a></p>` : ''}`);
    const sentences = (k.extract || '').split(/(?<=[.!?])\s+/).slice(0, 2).join(' ');
    return this.say(sentences || k.title);
  }

  async flightsAt(place) {
    const app = this.app;
    const km = place.km || 30;
    const nm = Math.min(250, Math.max(25, km * 0.54 * 1.6 + 20));
    await app.goPlace({ ...place, km: Math.max(km, nm * 1.852 * 1.3), sub: place.sub }, { quiet: true });
    ui.setStat('stLink', 'ABFRAGE …');
    let res;
    try { res = await app.loadFlightsAround(place.lat, place.lon, nm); }
    catch (e) { ui.setStat('stLink', 'FEHLER'); return this.say('Die Flugdatenquellen antworten gerade nicht. Bitte versuchen Sie es gleich noch einmal.'); }
    const list = res.list.filter(p => !p.ground).map(p => ({ p, d: haversine(place.lat, place.lon, p.lat, p.lon) })).sort((a, b) => a.d - b.d);
    if (!list.length) return this.say(`Über ${place.name} ist gerade kein Flugzeug zu sehen.`);
    const top = list.slice(0, 14).map(x => x.p);
    const rt = await S.routes(top);
    for (const p of top) p.route = rt[p.cs] || null;
    this.renderFlightList(`LUFTRAUM ${place.name}`.toUpperCase(), top, place);
    const n = top[0], dn = S.describePlane(n);
    const r = n.route ? `, unterwegs von ${apName(n.route.airports[0])} nach ${apName(n.route.airports.at(-1))}` : '';
    return this.say(`Im Umkreis von ${fmt(nm * 1.852)} Kilometern um ${place.name} verfolge ich ${list.length} Flugzeuge. Am nächsten ist ${speakCs(n)}${dn.airline ? ' von ' + dn.airline : ''}, ein ${dn.type} auf ${fmt(Math.round(n.altFt / 100) * 100)} Fuß${r}.`);
  }

  renderFlightList(title, list, place) {
    const items = list.map(p => {
      const d = S.describePlane(p);
      const r = p.route ? `${p.route.airports[0].iata || p.route.airports[0].icao} → ${p.route.airports.at(-1).iata || p.route.airports.at(-1).icao}` : '';
      const dist = place ? ` · ${fmt(haversine(place.lat, place.lon, p.lat, p.lon))} km` : '';
      return `<li data-id="${esc(p.hex)}"><span class="cs">${esc(p.cs || p.reg || p.hex.toUpperCase())}</span><span class="alt">${fmt(p.altFt)} ft</span><span class="meta">${esc([d.airline, d.type].filter(Boolean).join(' · '))}${r ? ' · ' + esc(r) : ''}${dist}</span></li>`;
    }).join('');
    ui.showPanel(title, `<ul class="flist">${items}</ul><p class="hint">Antippen zum Verfolgen · Quelle: ${esc(list[0]?.src || '')}</p>`, id => {
      const p = this.app.flights.planes.get(id) || list.find(x => x.hex === id);
      if (p) this.flightDetail(p, true);
    });
  }

  async flightDetail(p, speak = false, follow = true) {
    if (p.route === undefined) { const r = await S.routes([p]); p.route = r[p.cs] || null; }
    this.app.focusFlight(p, follow);
    const d = S.describePlane(p);
    const A = p.route?.airports[0], B = p.route?.airports.at(-1);
    ui.showPanel('FLUGZIEL · ' + (p.cs || p.hex).toUpperCase(), `
      <h2>${esc(p.cs || p.hex.toUpperCase())}</h2><p>${esc([d.airline, d.type].filter(Boolean).join(' · '))}</p>
      ${A ? `<div class="route"><div class="ap"><b>${esc(A.iata || A.icao)}</b><small>${esc(A.city || A.name)}</small></div><div class="line"></div><div class="ap"><b>${esc(B.iata || B.icao)}</b><small>${esc(B.city || B.name)}</small></div></div>` : '<p class="hint">Route unbekannt</p>'}
      <div class="kv">
        <span>Höhe</span><span>${fmt(p.altFt)} ft · ${fmt(p.altFt * 0.3048)} m</span>
        <span>Geschw.</span><span>${fmt(p.gsKt)} kn · ${fmt(p.gsKt * 1.852)} km/h</span>
        <span>Kurs</span><span>${fmt(p.track)}° (${compass(p.track)})</span>
        <span>Steigrate</span><span>${fmt(p.vr)} ft/min</span>
        <span>Kennzeichen</span><span>${esc(p.reg || '–')}</span>
        <span>Typ</span><span>${esc(p.type || '–')}</span>
        <span>Transponder</span><span>${esc(p.hex.toUpperCase())}${p.squawk ? ' · Squawk ' + esc(p.squawk) : ''}</span>
        <span>Position</span><span>${fmtCoord(p._lat ?? p.lat, p._lon ?? p.lon)}</span>
      </div><p class="hint">Quelle: ${esc(p.src)} · live, zwischen Updates hochgerechnet</p>`);
    if (speak) {
      return this.say(`${speakCs(p)}${d.airline ? ', ' + d.airline : ''}, ein ${d.type}. Flughöhe ${fmt(Math.round(p.altFt / 100) * 100)} Fuß, ${fmt(p.gsKt * 1.852)} Kilometer pro Stunde, Kurs Richtung ${compass(p.track)}${A ? `. Von ${apName(A)} nach ${apName(B)}` : ''}.`);
    }
  }

  async findFlight(cs) {
    const app = this.app;
    ui.setStat('stLink', 'SUCHE …');
    let list = [];
    try { list = await S.adsbCallsign(cs); } catch { /* */ }
    if (!list.length && /^[A-Z]{1,2}-?[A-Z0-9]{3,5}$/.test(cs)) { try { list = await S.adsbReg(cs); } catch { /* */ } }
    if (!list.length) {
      try { const all = await S.openskyAll(); list = all.filter(p => p.cs.toUpperCase() === cs); } catch { /* */ }
    }
    ui.setStat('stLink', 'BEREIT');
    if (!list.length) return this.say(`Flug ${cs.split('').join(' ')} ist derzeit nicht in der Luft oder sendet keine Positionsdaten.`);
    const p = list[0];
    await app.showGlobe();
    app.flights.set([p], { replace: false });
    await app.setFlights(true);
    return this.flightDetail(p, true);
  }

  async findType(tf, codes) {
    const app = this.app;
    ui.setStat('stLink', 'SUCHE …');
    let all = [];
    for (const c of codes) { try { all.push(...await S.adsbType(c)); } catch { /* */ } }
    all = all.filter(p => !p.ground);
    if (!all.length) return this.say('Von diesem Flugzeugtyp ist aktuell keiner in meinen Datenquellen.');
    await app.showGlobe();
    app.flights.set(all, { replace: false });
    await app.setFlights(true);
    const me = app.userLoc;
    if (me) all.sort((a, b) => haversine(me.lat, me.lon, a.lat, a.lon) - haversine(me.lat, me.lon, b.lat, b.lon));
    const top = all.slice(0, 14);
    const rt = await S.routes(top); for (const p of top) p.route = rt[p.cs] || null;
    app.flyTo(top[0].lat, top[0].lon, 2.2);
    this.renderFlightList(`${S.TYPES[codes[0]] || codes[0]} · WELTWEIT`.toUpperCase(), top, me);
    return this.say(`Ich habe ${all.length} Flugzeuge vom Typ ${S.TYPES[codes[0]] || codes[0]} in der Luft gefunden.${me ? ' Das nächste zu Ihnen ist ' + speakCs(top[0]) + '.' : ''}`);
  }
}

function extractCallsign(text) {
  const words = text.toUpperCase().replace(/[^A-Z0-9 -]/g, ' ').split(/\s+/);
  for (const w0 of words) {
    const w = w0.replace(/-/g, '');
    let m = w.match(/^([A-Z]{3})(\d{1,4}[A-Z]{0,2})$/);
    if (m && (S.AIRLINES[m[1]])) return w;
    m = w.match(/^([A-Z][A-Z0-9]|[A-Z0-9][A-Z])(\d{1,4}[A-Z]?)$/);
    if (m && S.IATA2ICAO[m[1]]) return S.IATA2ICAO[m[1]] + m[2];
    if (/^D[A-Z]{4}$/.test(w) && w0.includes('-')) return w; // deutsches Kennzeichen D-ABCD
  }
  return null;
}
function speakCs(p) {
  const cs = p.cs || p.reg || p.hex;
  const m = cs.match(/^([A-Z]{3})(.*)$/);
  return m ? `${m[1].split('').join(' ')} ${m[2].split('').join(' ')}` : cs;
}
function apName(a) { return a.city || a.name || a.iata; }
function greeting() {
  const h = new Date().getHours();
  return h < 5 ? 'Guten Abend' : h < 11 ? 'Guten Morgen' : h < 18 ? 'Guten Tag' : 'Guten Abend';
}
