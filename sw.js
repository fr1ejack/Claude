/* Service Worker: macht Jarvis installierbar und lädt die App-Oberfläche auch offline/schneller.
 * Live-Daten (Flüge, Karten, Wetter, Recherche) und das KI-Modell werden NICHT hier gecacht. */
const CACHE = 'jarvis-v4';
const SHELL = ['./', 'index.html', 'css/style.css', 'manifest.webmanifest', 'icons/icon.svg', 'icons/icon-192.png', 'icons/icon-512.png',
  'js/main.js', 'js/util.js', 'js/materials.js', 'js/core.js', 'js/objects.js', 'js/globe.js', 'js/flights.js', 'js/services.js',
  'js/assistant.js', 'js/ui.js', 'js/brain.js', 'js/memory.js', 'js/llm-worker.js', 'js/buildings.js', 'js/offline.js', 'data/countries-50m.json', 'data/cities.json'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
const cacheable = url => url.origin === location.origin
  || (url.hostname === 'cdn.jsdelivr.net' && /\/npm\/(three@|world-atlas@|@mlc-ai\/web-llm@)/.test(url.pathname))
  || url.hostname === 'fonts.googleapis.com' || url.hostname === 'fonts.gstatic.com';

// Stale-while-revalidate: sofort aus dem Cache, im Hintergrund aktualisieren
self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (!cacheable(url)) return;
  e.respondWith(caches.open(CACHE).then(async c => {
    const hit = await c.match(req);
    const net = fetch(req).then(r => { if (r.ok || r.type === 'opaque') c.put(req, r.clone()); return r; }).catch(() => hit);
    return hit || net;
  }));
});
