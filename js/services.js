import { DEG, IS_ARTIFACT } from './util.js';
import { offlineGeocode, offlineNearest } from './offline.js';

/* Alle Datenquellen sind kostenlos und ohne API-Key nutzbar (CORS-fähig). */

async function j(url, opts = {}, ms = 12000) {
  const c = new AbortController();
  const t = setTimeout(() => c.abort(), ms);
  try {
    const r = await fetch(url, { ...opts, signal: c.signal });
    if (!r.ok) throw new Error('HTTP ' + r.status);
    return await r.json();
  } finally { clearTimeout(t); }
}

/* ---------------- Orte (OpenStreetMap Nominatim) ---------------- */
const NOMI = 'https://nominatim.openstreetmap.org';
export async function geocode(q) {
  if (IS_ARTIFACT) return offlineGeocode(q);
  const d = await j(`${NOMI}/search?format=jsonv2&limit=1&addressdetails=1&accept-language=de&q=${encodeURIComponent(q)}`);
  if (!d.length) return null;
  const r = d[0], bb = r.boundingbox.map(Number);
  return {
    name: r.name || q, display: r.display_name, lat: +r.lat, lon: +r.lon,
    bbox: { s: bb[0], n: bb[1], w: bb[2], e: bb[3] },
    cls: r.category || r.class, type: r.type, addr: r.address || {}, importance: r.importance ?? 0,
  };
}
export async function reverse(lat, lon) {
  if (IS_ARTIFACT) return offlineNearest(lat, lon);
  const r = await j(`${NOMI}/reverse?format=jsonv2&zoom=12&addressdetails=1&accept-language=de&lat=${lat}&lon=${lon}`);
  const a = r.address || {};
  return {
    city: a.city || a.town || a.village || a.municipality || a.suburb || a.county || r.name || 'Unbekannt',
    district: a.suburb || a.city_district || '', state: a.state || '', country: a.country || '', display: r.display_name,
  };
}
export function regionOf(addr = {}) {
  return [addr.state, addr.country].filter(Boolean).join(', ');
}
export function spanKm(bbox) {
  const h = (bbox.n - bbox.s) * 111, w = (bbox.e - bbox.w) * 111 * Math.cos(((bbox.n + bbox.s) / 2) * DEG);
  return Math.max(h, w, 1);
}

/* ---------------- Eigener Standort ---------------- */
export async function locate() {
  if (IS_ARTIFACT) throw new Error('In der privaten claude.ai-Version ist GPS gesperrt');
  try {
    const pos = await new Promise((res, rej) => {
      if (!navigator.geolocation) return rej(new Error('keine Geolocation'));
      navigator.geolocation.getCurrentPosition(res, rej, { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 });
    });
    return { lat: pos.coords.latitude, lon: pos.coords.longitude, acc: pos.coords.accuracy, src: 'GPS / Browser' };
  } catch (e) {
    try {
      const d = await j('https://ipapi.co/json/');
      if (d.latitude) return { lat: d.latitude, lon: d.longitude, acc: 5000, src: 'IP-Ortung', city: d.city };
    } catch { /* weiter */ }
    const d = await j('https://ipwho.is/');
    if (d.success) return { lat: d.latitude, lon: d.longitude, acc: 5000, src: 'IP-Ortung', city: d.city };
    throw new Error('Standort nicht verfügbar');
  }
}

/* ---------------- Wetter (Open-Meteo) ---------------- */
export async function weather(lat, lon) {
  const d = await j(`https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto`);
  return d.current;
}
export const WMO = { 0: 'klarer Himmel', 1: 'überwiegend klar', 2: 'teilweise bewölkt', 3: 'bedeckt', 45: 'Nebel', 48: 'Reifnebel', 51: 'leichter Nieselregen', 53: 'Nieselregen', 55: 'starker Nieselregen', 61: 'leichter Regen', 63: 'Regen', 65: 'starker Regen', 66: 'gefrierender Regen', 67: 'starker gefrierender Regen', 71: 'leichter Schneefall', 73: 'Schneefall', 75: 'starker Schneefall', 77: 'Schneegriesel', 80: 'leichte Regenschauer', 81: 'Regenschauer', 82: 'heftige Regenschauer', 85: 'Schneeschauer', 86: 'starke Schneeschauer', 95: 'Gewitter', 96: 'Gewitter mit Hagel', 99: 'schweres Gewitter mit Hagel' };

/* ---------------- Wissen (Wikipedia) ---------------- */
export async function wiki(q) {
  const s = await j(`https://de.wikipedia.org/w/api.php?action=query&list=search&srsearch=${encodeURIComponent(q)}&srlimit=1&format=json&origin=*`);
  const hit = s?.query?.search?.[0];
  if (!hit) return null;
  const d = await j(`https://de.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(hit.title.replace(/ /g, '_'))}`);
  let img = d.thumbnail?.source || null;
  if (img) img = img.replace(/\/(\d+)px-/, '/800px-');
  return { title: d.title, extract: d.extract, image: img, coords: d.coordinates, url: d.content_urls?.desktop?.page };
}

/* ---------------- Flugdaten (adsb.lol, Fallback OpenSky) ---------------- */
const ADSB = 'https://api.adsb.lol';
function normAdsb(a) {
  if (a.lat == null || a.lon == null) return null;
  const ground = a.alt_baro === 'ground';
  return {
    hex: a.hex, cs: (a.flight || '').trim(), reg: a.r || '', type: a.t || '', desc: a.desc || '',
    lat: a.lat, lon: a.lon, altFt: ground ? 0 : (+a.alt_baro || +a.alt_geom || 0), gsKt: +a.gs || 0,
    track: +(a.track ?? a.true_heading ?? a.mag_heading ?? 0), vr: +(a.baro_rate ?? a.geom_rate ?? 0), squawk: a.squawk || '',
    ground, t: Date.now() - (a.seen_pos ?? a.seen ?? 0) * 1000, src: 'adsb.lol',
  };
}
const adsbList = d => (d.ac || d.aircraft || []).map(normAdsb).filter(Boolean);
export const adsbPoint = async (lat, lon, nm) => adsbList(await j(`${ADSB}/v2/point/${lat.toFixed(4)}/${lon.toFixed(4)}/${Math.round(Math.min(250, Math.max(5, nm)))}`));
export const adsbCallsign = async cs => adsbList(await j(`${ADSB}/v2/callsign/${encodeURIComponent(cs)}`));
export const adsbReg = async r => adsbList(await j(`${ADSB}/v2/reg/${encodeURIComponent(r)}`));
export const adsbHex = async h => adsbList(await j(`${ADSB}/v2/hex/${encodeURIComponent(h)}`));
export const adsbType = async t => adsbList(await j(`${ADSB}/v2/type/${encodeURIComponent(t)}`));

function normOpensky(d) {
  const now = Date.now();
  return (d.states || []).filter(s => s[5] != null && s[6] != null).map(s => ({
    hex: s[0], cs: (s[1] || '').trim(), reg: '', type: '', desc: '', country: s[2],
    lat: s[6], lon: s[5], altFt: (s[7] ?? s[13] ?? 0) * 3.28084, gsKt: (s[9] || 0) * 1.94384, track: s[10] || 0,
    vr: (s[11] || 0) * 196.85, squawk: s[14] || '', ground: !!s[8], t: (s[3] || s[4]) ? (s[3] || s[4]) * 1000 : now, src: 'OpenSky',
  }));
}
export const openskyBox = async (s, w, n, e) => normOpensky(await j(`https://opensky-network.org/api/states/all?lamin=${s.toFixed(3)}&lomin=${w.toFixed(3)}&lamax=${n.toFixed(3)}&lomax=${e.toFixed(3)}`, {}, 20000));
export const openskyAll = async () => normOpensky(await j('https://opensky-network.org/api/states/all', {}, 25000));

export async function flightsAround(lat, lon, nm) {
  try {
    return { list: await adsbPoint(lat, lon, nm), src: 'adsb.lol' };
  } catch (e) {
    const km = Math.min(nm, 250) * 1.852, dLat = km / 111, dLon = km / (111 * Math.max(0.1, Math.cos(lat * DEG)));
    return { list: await openskyBox(lat - dLat, lon - dLon, lat + dLat, lon + dLon), src: 'OpenSky' };
  }
}

const routeCache = new Map();
export async function routes(planes) {
  const need = planes.filter(p => p.cs && !routeCache.has(p.cs)).slice(0, 60);
  if (need.length) {
    try {
      const r = await j(`${ADSB}/api/0/routeset`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ planes: need.map(p => ({ callsign: p.cs, lat: p.lat, lng: p.lon })) }),
      });
      for (const p of need) routeCache.set(p.cs, null);
      for (const x of r || []) {
        if (!x || !x._airports || x._airports.length < 2) continue;
        routeCache.set(x.callsign, {
          codes: x.airport_codes, plausible: x.plausible !== false,
          airports: x._airports.map(a => ({ name: a.name, city: a.location, iata: a.iata, icao: a.icao, lat: +a.lat, lon: +a.lon, cc: a.countryiso2 })),
        });
      }
    } catch (e) { console.warn('Routen nicht verfügbar', e); }
  }
  return Object.fromEntries(planes.map(p => [p.cs, routeCache.get(p.cs) || null]));
}

/* ---------------- Namens-Tabellen ---------------- */
export const AIRLINES = {
  DLH: 'Lufthansa', EWG: 'Eurowings', CFG: 'Condor', TUI: 'TUIfly', TUW: 'TUIfly', RYR: 'Ryanair', EZY: 'easyJet', EJU: 'easyJet Europe', BAW: 'British Airways', AFR: 'Air France', KLM: 'KLM',
  UAE: 'Emirates', QTR: 'Qatar Airways', THY: 'Turkish Airlines', PGT: 'Pegasus', SWR: 'Swiss', AUA: 'Austrian', SAS: 'SAS', FIN: 'Finnair', IBE: 'Iberia', VLG: 'Vueling', WZZ: 'Wizz Air',
  UAL: 'United', AAL: 'American Airlines', DAL: 'Delta', ACA: 'Air Canada', SIA: 'Singapore Airlines', ETD: 'Etihad', LOT: 'LOT Polish', TAP: 'TAP Portugal', ITY: 'ITA Airways', BEL: 'Brussels Airlines',
  DLA: 'Air Dolomiti', CLH: 'Lufthansa CityLine', GEC: 'Lufthansa Cargo', BCS: 'European Air Transport (DHL)', DHK: 'DHL', FDX: 'FedEx', UPS: 'UPS', NOZ: 'Norwegian', NSZ: 'Norwegian', SXS: 'SunExpress',
  CPA: 'Cathay Pacific', JAL: 'Japan Airlines', ANA: 'All Nippon', KAL: 'Korean Air', CCA: 'Air China', CES: 'China Eastern', CSN: 'China Southern', QFA: 'Qantas', AEE: 'Aegean', ELY: 'El Al', MSR: 'EgyptAir',
  ICE: 'Icelandair', AZA: 'Alitalia', LGL: 'Luxair', DAT: 'DAT', SVA: 'Saudia', RAM: 'Royal Air Maroc', ETH: 'Ethiopian', AIC: 'Air India', JBU: 'JetBlue', SWA: 'Southwest', ASA: 'Alaska',
};
export const IATA2ICAO = { LH: 'DLH', EW: 'EWG', DE: 'CFG', X3: 'TUI', FR: 'RYR', U2: 'EZY', EC: 'EJU', BA: 'BAW', AF: 'AFR', KL: 'KLM', EK: 'UAE', QR: 'QTR', TK: 'THY', PC: 'PGT', LX: 'SWR', OS: 'AUA', SK: 'SAS', AY: 'FIN', IB: 'IBE', VY: 'VLG', W6: 'WZZ', UA: 'UAL', AA: 'AAL', DL: 'DAL', AC: 'ACA', SQ: 'SIA', EY: 'ETD', LO: 'LOT', TP: 'TAP', AZ: 'ITY', SN: 'BEL', XQ: 'SXS', CX: 'CPA', JL: 'JAL', NH: 'ANA', KE: 'KAL', CA: 'CCA', QF: 'QFA', A3: 'AEE', LY: 'ELY', MS: 'MSR', FI: 'ICE', LG: 'LGL', SV: 'SVA', AT: 'RAM', ET: 'ETH', AI: 'AIC', B6: 'JBU', WN: 'SWA', AS: 'ASA', DY: 'NOZ', EN: 'DLA', WK: 'EDW' };
export const TYPES = {
  A318: 'Airbus A318', A319: 'Airbus A319', A320: 'Airbus A320', A321: 'Airbus A321', A19N: 'Airbus A319neo', A20N: 'Airbus A320neo', A21N: 'Airbus A321neo', A332: 'Airbus A330-200', A333: 'Airbus A330-300', A339: 'Airbus A330-900neo',
  A359: 'Airbus A350-900', A35K: 'Airbus A350-1000', A388: 'Airbus A380-800', A306: 'Airbus A300-600', A343: 'Airbus A340-300', A346: 'Airbus A340-600', BCS1: 'Airbus A220-100', BCS3: 'Airbus A220-300',
  B737: 'Boeing 737-700', B738: 'Boeing 737-800', B739: 'Boeing 737-900', B37M: 'Boeing 737 MAX 7', B38M: 'Boeing 737 MAX 8', B39M: 'Boeing 737 MAX 9', B744: 'Boeing 747-400', B748: 'Boeing 747-8', B752: 'Boeing 757-200',
  B763: 'Boeing 767-300', B772: 'Boeing 777-200', B77L: 'Boeing 777-200LR', B77W: 'Boeing 777-300ER', B788: 'Boeing 787-8 Dreamliner', B789: 'Boeing 787-9 Dreamliner', B78X: 'Boeing 787-10 Dreamliner', B78: 'Boeing 787',
  E190: 'Embraer E190', E195: 'Embraer E195', E290: 'Embraer E190-E2', E295: 'Embraer E195-E2', E75L: 'Embraer E175', CRJ9: 'Bombardier CRJ900', CRJ7: 'Bombardier CRJ700', DH8D: 'De Havilland Dash 8-400',
  AT72: 'ATR 72', AT75: 'ATR 72-500', AT76: 'ATR 72-600', AT45: 'ATR 42-500', C172: 'Cessna 172', C152: 'Cessna 152', C208: 'Cessna Caravan', PC12: 'Pilatus PC-12', PC24: 'Pilatus PC-24', SR22: 'Cirrus SR22',
  GLF6: 'Gulfstream G650', GLEX: 'Bombardier Global Express', CL35: 'Challenger 350', C68A: 'Cessna Latitude', EC35: 'Airbus H135', EC45: 'Airbus H145', A400: 'Airbus A400M', C17: 'Boeing C-17', C130: 'Lockheed C-130', K35R: 'Boeing KC-135', A30B: 'Airbus A300',
};
/** Natürliche Sprache -> ICAO-Typcodes */
export const TYPE_WORDS = [
  [/a ?380/, ['A388']], [/a ?350/, ['A359', 'A35K']], [/a ?330/, ['A333', 'A332', 'A339']], [/a ?321/, ['A321', 'A21N']], [/a ?320/, ['A320', 'A20N']], [/a ?319/, ['A319', 'A19N']], [/a ?220/, ['BCS3', 'BCS1']],
  [/(747|jumbo)/, ['B748', 'B744']], [/(787|dreamliner)/, ['B789', 'B788', 'B78X']], [/777/, ['B77W', 'B772', 'B77L']], [/737/, ['B738', 'B38M', 'B737', 'B739']], [/767/, ['B763']], [/757/, ['B752']],
  [/a ?400 ?m/, ['A400']], [/(hubschrauber|helikopter)/, ['EC35', 'EC45']], [/cessna/, ['C172', 'C152', 'C208']],
];
export function describePlane(p) {
  const pre = p.cs.slice(0, 3).toUpperCase();
  return {
    airline: /^[A-Z]{3}\d/.test(p.cs) ? (AIRLINES[pre] || '') : '',
    type: p.desc ? titleCase(p.desc) : (TYPES[p.type] || p.type || 'unbekannter Typ'),
  };
}
function titleCase(s) { return s.toLowerCase().replace(/\b[a-z]/g, c => c.toUpperCase()).replace(/\b(A|B)(\d)/g, '$1$2'); }

/* ---------------- Freie Internet-Recherche (ohne API-Key) ----------------
 * Wikipedia (de + en) Volltextsuche mit Auszügen, plus DuckDuckGo-Sofortantworten.
 * Die Ergebnisse speichert Jarvis als "Erfahrungen" in seinem Gedächtnis. */
async function wikiSearch(lang, q, n = 3) {
  const d = await j(`https://${lang}.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=${encodeURIComponent(q)}&gsrlimit=${n}&prop=extracts|pageimages|info&inprop=url&exintro=1&explaintext=1&exchars=1200&piprop=thumbnail&pithumbsize=800&format=json&origin=*`, {}, 9000);
  const pages = Object.values(d?.query?.pages || {}).sort((a, b) => (a.index || 0) - (b.index || 0));
  return pages.filter(p => p.extract).map(p => ({ title: p.title, text: p.extract, url: p.fullurl, image: p.thumbnail?.source, src: `Wikipedia (${lang})` }));
}
async function ddgInstant(q) {
  const d = await j(`https://api.duckduckgo.com/?q=${encodeURIComponent(q)}&format=json&no_html=1&skip_disambig=1&t=jarvis-holo`, {}, 6000);
  const out = [];
  if (d.Answer) out.push({ title: q, text: String(d.Answer), url: '', src: 'DuckDuckGo' });
  if (d.AbstractText) out.push({ title: d.Heading || q, text: d.AbstractText, url: d.AbstractURL, image: d.Image ? 'https://duckduckgo.com' + d.Image : '', src: d.AbstractSource || 'DuckDuckGo' });
  return out;
}
export async function webResearch(q) {
  const [de, ddg] = await Promise.allSettled([wikiSearch('de', q), ddgInstant(q)]);
  let res = [...(ddg.value || []), ...(de.value || [])];
  if (res.length < 2) { try { res.push(...await wikiSearch('en', q, 2)); } catch { /* */ } }
  const seen = new Set();
  return res.filter(r => r.text && !seen.has(r.title) && seen.add(r.title)).slice(0, 4);
}
