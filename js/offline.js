/*
 * Offline-Ortssuche für die private claude.ai-Version (dort sind Online-Dienste gesperrt).
 * Datensatz: GeoNames-Städte (weltweit ab 15.000 Einwohnern, DACH ab 2.500), plus deutsche Namen und Länder.
 */
import { haversine } from './util.js';

const ALIAS = {
  'münchen': 'Munich', 'muenchen': 'Munich', 'köln': 'Cologne', 'koeln': 'Cologne', 'wien': 'Vienna', 'rom': 'Rome', 'mailand': 'Milan', 'prag': 'Prague',
  'warschau': 'Warsaw', 'moskau': 'Moscow', 'brüssel': 'Brussels', 'lissabon': 'Lisbon', 'kopenhagen': 'Copenhagen', 'athen': 'Athens', 'peking': 'Beijing',
  'tokio': 'Tokyo', 'neu-delhi': 'New Delhi', 'kairo': 'Cairo', 'venedig': 'Venice', 'florenz': 'Florence', 'neapel': 'Naples', 'genf': 'Geneva', 'zürich': 'Zurich',
  'nürnberg': 'Nuremberg', 'nuernberg': 'Nuremberg', 'hannover': 'Hanover', 'den haag': 'The Hague', 'antwerpen': 'Antwerp', 'krakau': 'Krakow', 'bukarest': 'Bucharest',
  'belgrad': 'Belgrade', 'sankt petersburg': 'Saint Petersburg', 'st. petersburg': 'Saint Petersburg', 'kapstadt': 'Cape Town', 'mexiko-stadt': 'Mexico City',
  'singapur': 'Singapore', 'frankfurt': 'Frankfurt am Main', 'new york': 'New York City', 'washington': 'Washington, D.C.', 'jerusalem': 'Jerusalem', 'sevilla': 'Seville',
  'danzig': 'Gdańsk', 'breslau': 'Wrocław', 'posen': 'Poznań', 'pressburg': 'Bratislava', 'laibach': 'Ljubljana', 'agram': 'Zagreb', 'istanbul': 'Istanbul',
  'luxemburg': 'Luxembourg', 'teheran': 'Tehran', 'bagdad': 'Baghdad', 'damaskus': 'Damascus', 'algier': 'Algiers', 'havanna': 'Havana', 'peking': 'Beijing',
};
const COUNTRIES = {
  deutschland: [51.2, 10.4, 900], österreich: [47.6, 14.1, 550], schweiz: [46.8, 8.2, 350], frankreich: [46.6, 2.4, 1000], italien: [42.8, 12.6, 1100],
  spanien: [40.3, -3.7, 1000], portugal: [39.6, -8.0, 600], niederlande: [52.2, 5.5, 300], holland: [52.2, 5.5, 300], belgien: [50.6, 4.6, 280], polen: [52.0, 19.4, 700],
  tschechien: [49.8, 15.5, 450], dänemark: [56.0, 10.0, 400], schweden: [62.0, 15.0, 1500], norwegen: [64.5, 12.0, 1700], finnland: [64.0, 26.0, 1100], england: [52.5, -1.5, 600],
  großbritannien: [54.0, -2.5, 1000], grossbritannien: [54.0, -2.5, 1000], irland: [53.2, -8.0, 450], griechenland: [39.0, 22.5, 750], türkei: [39.0, 35.0, 1500], tuerkei: [39.0, 35.0, 1500],
  russland: [60.0, 90.0, 6000], ukraine: [49.0, 31.5, 1200], usa: [39.5, -98.5, 4500], amerika: [39.5, -98.5, 4500], kanada: [58.0, -100.0, 5000], mexiko: [23.5, -102.5, 2500],
  brasilien: [-10.0, -52.0, 4000], argentinien: [-35.0, -65.0, 3000], china: [35.0, 104.0, 4500], japan: [36.5, 138.0, 2000], indien: [22.5, 79.0, 3000], australien: [-25.5, 134.0, 4000],
  ägypten: [26.5, 30.0, 1200], aegypten: [26.5, 30.0, 1200], südafrika: [-29.0, 24.5, 1600], marokko: [31.8, -7.0, 1000], kroatien: [45.1, 15.2, 450], ungarn: [47.2, 19.4, 450],
  rumänien: [45.9, 25.0, 650], bulgarien: [42.7, 25.3, 450], serbien: [44.0, 20.9, 450], island: [64.9, -18.6, 500], thailand: [15.5, 101.0, 1500], indonesien: [-2.5, 118.0, 4000],
  europa: [50.0, 12.0, 4500], afrika: [3.0, 20.0, 8000], asien: [35.0, 90.0, 9000], südamerika: [-15.0, -60.0, 7000], nordamerika: [45.0, -100.0, 7000], antarktis: [-80.0, 0.0, 5000],
};

let rows = null, loading = null;
async function load() {
  if (rows) return rows;
  loading ||= fetch(new URL('../data/cities.json', import.meta.url)).then(r => r.json()).then(d => (rows = d));
  return loading;
}
const norm = s => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/ß/g, 'ss').replace(/[^a-z0-9 ]/g, ' ').replace(/\s+/g, ' ').trim();

export async function offlineGeocode(q) {
  const raw = q.trim().toLowerCase();
  const c = COUNTRIES[raw] || COUNTRIES[raw.replace(/ss/g, 'ß')];
  if (c) {
    const [lat, lon, km] = c, d = km / 111 / 2;
    return { name: q.charAt(0).toUpperCase() + q.slice(1), display: q, lat, lon, bbox: { s: lat - d, n: lat + d, w: lon - d, e: lon + d }, cls: 'boundary', type: 'country', addr: {}, importance: 0.9 };
  }
  const data = await load();
  const target = norm(ALIAS[raw] || q);
  let best = data.find(r => norm(r[0]) === target);
  if (!best) best = data.find(r => norm(r[0]).startsWith(target + ' ') || norm(r[0]).startsWith(target));
  if (!best && target.length > 4) best = data.find(r => norm(r[0]).includes(target));
  if (!best) return null;
  let [name, cc, lat, lon, pop] = best;
  if (ALIAS[raw]) name = q.trim().replace(/^./, ch => ch.toUpperCase());
  const km = Math.max(6, Math.min(60, Math.sqrt(pop) / 18)), d = km / 111 / 2;
  return { name, display: `${name}, ${regionName(cc)} · ${pop.toLocaleString('de-DE')} Einwohner`, lat, lon, bbox: { s: lat - d, n: lat + d, w: lon - d, e: lon + d }, cls: 'place', type: 'Stadt', addr: { country: regionName(cc) }, importance: 0.8 };
}
export const loadCities = load;
export async function offlineNearest(lat, lon) {
  const data = await load();
  let best = null, bd = Infinity;
  for (const r of data) { const d = haversine(lat, lon, r[2], r[3]); if (d < bd) { bd = d; best = r; } }
  return best ? { city: best[0], state: '', country: regionName(best[1]) } : null;
}
function regionName(cc) {
  try { return new Intl.DisplayNames(['de'], { type: 'region' }).of(cc); } catch { return cc; }
}
