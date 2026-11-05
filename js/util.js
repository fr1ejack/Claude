import * as THREE from 'three';

export const DEG = Math.PI / 180;
/** Gemeinsame Uniforms (Zeit) für alle Hologramm-Shader */
export const U = { time: { value: 0 } };

export const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
export const lerp = (a, b, t) => a + (b - a) * t;
export const smooth = (a, b, v) => { const t = clamp((v - a) / (b - a), 0, 1); return t * t * (3 - 2 * t); };
export const easeInOut = t => (t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
export const easeOut = t => 1 - Math.pow(1 - t, 3);
export const linear = t => t;
export const wait = ms => new Promise(r => setTimeout(r, ms));

/** Geo-Koordinaten -> Punkt auf Kugel (Radius r) */
export function ll2v(lat, lon, r = 1, out = new THREE.Vector3()) {
  const phi = (90 - lat) * DEG, th = (lon + 180) * DEG;
  return out.set(-r * Math.sin(phi) * Math.cos(th), r * Math.cos(phi), r * Math.sin(phi) * Math.sin(th));
}
export function v2ll(v) {
  const r = v.length();
  const lat = 90 - Math.acos(clamp(v.y / r, -1, 1)) / DEG;
  let lon = Math.atan2(v.z, -v.x) / DEG - 180;
  if (lon < -180) lon += 360;
  return { lat, lon };
}
/** Tangente Richtung Norden am Kugelpunkt mit Normale n */
export function northAt(n, out = new THREE.Vector3()) {
  out.set(0, 1, 0).addScaledVector(n, -n.y);
  if (out.lengthSq() < 1e-8) out.set(0, 0, -1);
  return out.normalize();
}
export function haversine(lat1, lon1, lat2, lon2) {
  const dLat = (lat2 - lat1) * DEG, dLon = (lon2 - lon1) * DEG;
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * DEG) * Math.cos(lat2 * DEG) * Math.sin(dLon / 2) ** 2;
  return 2 * 6371 * Math.asin(Math.sqrt(Math.min(1, a)));
}
export function destPoint(lat, lon, bearing, km) {
  const d = km / 6371, b = bearing * DEG, p1 = lat * DEG, l1 = lon * DEG;
  const p2 = Math.asin(Math.sin(p1) * Math.cos(d) + Math.cos(p1) * Math.sin(d) * Math.cos(b));
  const l2 = l1 + Math.atan2(Math.sin(b) * Math.sin(d) * Math.cos(p1), Math.cos(d) - Math.sin(p1) * Math.sin(p2));
  let lo = l2 / DEG; lo = ((lo + 540) % 360) - 180;
  return { lat: p2 / DEG, lon: lo };
}
export function bearing(lat1, lon1, lat2, lon2) {
  const p1 = lat1 * DEG, p2 = lat2 * DEG, dl = (lon2 - lon1) * DEG;
  const y = Math.sin(dl) * Math.cos(p2), x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
  return ((Math.atan2(y, x) / DEG) + 360) % 360;
}

export const fmt = (n, d = 0) => Number(n).toLocaleString('de-DE', { maximumFractionDigits: d, minimumFractionDigits: d });
export const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
export function compass(deg) {
  const n = ['Norden', 'Nordosten', 'Osten', 'Südosten', 'Süden', 'Südwesten', 'Westen', 'Nordwesten'];
  return n[Math.round((((deg % 360) + 360) % 360) / 45) % 8];
}
export function fmtCoord(lat, lon) {
  return `${fmt(Math.abs(lat), 4)}° ${lat >= 0 ? 'N' : 'S'}  ${fmt(Math.abs(lon), 4)}° ${lon >= 0 ? 'O' : 'W'}`;
}

/* ---------- Mini-Tween-System ---------- */
const tweens = new Set();
export function tween(duration, update, ease = easeInOut) {
  let handle;
  const p = new Promise(res => {
    handle = { start: performance.now(), duration, update, ease, res, dead: false };
    tweens.add(handle);
  });
  handle.p = p; p.done = false;
  p.cancel = () => { if (!handle.dead) { handle.dead = true; p.done = true; tweens.delete(handle); handle.res(false); } };
  return p;
}
export function updateTweens(now) {
  for (const t of [...tweens]) {
    const k = Math.min(1, (now - t.start) / t.duration);
    t.update(t.ease(k), k);
    if (k >= 1 && !t.dead) { t.dead = true; t.p.done = true; tweens.delete(t); t.res(true); }
  }
}
