import * as THREE from 'three';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js';
import { CSS2DRenderer } from 'three/addons/renderers/CSS2DRenderer.js';
import { IS_ARTIFACT, U, DEG, ll2v, v2ll, northAt, clamp, lerp, smooth, easeInOut, easeOut, linear, tween, updateTweens, wait, haversine, fmtCoord, fmt } from './util.js';
import { Globe } from './globe.js';
import { FlightLayer } from './flights.js';
import { Buildings } from './buildings.js';
import { Core, Projector } from './core.js';
import { buildImageHologram } from './objects.js';
import { Assistant } from './assistant.js';
import { Brain, ClaudeBrain, MODELS } from './brain.js';
import { memory } from './memory.js';
import * as S from './services.js';
import * as ui from './ui.js';

/* ================= Renderer & Szene ================= */
const stage = document.getElementById('stage');
const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
renderer.setSize(innerWidth, innerHeight);
renderer.setClearColor(0x01050b, 1);
stage.appendChild(renderer.domElement);
const labels = new CSS2DRenderer();
labels.setSize(innerWidth, innerHeight);
labels.domElement.className = 'labels';
stage.appendChild(labels.domElement);

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(45, innerWidth / innerHeight, 0.01, 400);
camera.position.set(0, 2, 7);

const composer = new EffectComposer(renderer);
composer.addPass(new RenderPass(scene, camera));
const bloom = new UnrealBloomPass(new THREE.Vector2(innerWidth, innerHeight), 0.75, 0.5, 0.18);
composer.addPass(bloom);
composer.addPass(new OutputPass());

// Hintergrund-Partikel
{
  const n = 2500, p = new Float32Array(n * 3);
  for (let i = 0; i < n; i++) { const v = new THREE.Vector3().randomDirection().multiplyScalar(30 + Math.random() * 90); p.set([v.x, v.y, v.z], i * 3); }
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.BufferAttribute(p, 3));
  const stars = new THREE.Points(g, new THREE.PointsMaterial({ color: 0x4fb8ff, size: 0.22, transparent: true, opacity: 0.45, depthWrite: false, blending: THREE.AdditiveBlending }));
  scene.add(stars);
  scene.userData.stars = stars;
}

/* ================= Module ================= */
const globe = new Globe();
globe.group.visible = false;
scene.add(globe.group);
const flights = new FlightLayer(globe.group);
const buildings = new Buildings(globe.group);
buildings.onStatus = t => ui.setStat('stLink', t);
const core = new Core();
scene.add(core.group);
const projector = new Projector();
scene.add(projector.group);

/* ================= App-Zustand ================= */
const app = {
  mode: 'core', obj: null, flights, globe, userLoc: null,
  gcam: { lat: 30, lon: 0, alt: 6 }, gscale: 0.02,
  ocam: { dist: 7, el: 0.3, rotY: 0, tgtDist: 7 },
  flightsOn: false, follow: null, camTween: null, lastFetch: 0, fetching: false, lastGlobal: 0,
  explodeT: 0, explodeTarget: 0, userRot: 0, lastInteract: 0,
};
window.jarvis = app; // zum Debuggen in der Konsole

/* ---------- Übergänge ---------- */
async function hideCurrent() {
  if (app.mode === 'globe') {
    app.camTween?.cancel(); app.follow = null;
    const s0 = app.gscale, a0 = app.gcam.alt;
    await tween(800, k => { app.gscale = lerp(s0, 0.02, k); app.gcam.alt = lerp(a0, Math.max(a0, 4), k); globe.fade.value = 1 - k; }, easeInOut);
    globe.group.visible = false;
  } else if (app.mode === 'object' && app.obj) {
    const o = app.obj;
    await tween(650, k => { o.shared.reveal.value = lerp(3.6, -2.6, k); }, easeInOut);
    disposeObject(o);
    app.obj = null;
  } else if (app.mode === 'core') {
    await tween(500, k => { core.shared.fade.value = 1 - k; core.group.scale.setScalar(1 - k * 0.6); }, easeInOut);
    core.group.visible = false;
  }
  app.mode = 'none';
}
function disposeObject(o) {
  scene.remove(o.group);
  o.group.traverse(c => { if (c.isCSS2DObject) c.element.remove(); c.geometry?.dispose?.(); });
  for (const p of o.parts) p.label.element.remove();
}

app.showCore = async () => {
  if (app.mode === 'core') return;
  await hideCurrent();
  ui.hidePanel();
  app.mode = 'core';
  ui.setStat('stMode', 'KERN');
  core.group.visible = true; projector.group.visible = true;
  app.ocam.tgtDist = 7; app.ocam.el = 0.3;
  await tween(700, k => { core.shared.fade.value = k; core.group.scale.setScalar(0.4 + 0.6 * k); projector.shared.fade.value = k; }, easeOut);
};

app.showGlobe = async () => {
  if (app.mode === 'globe') return;
  await hideCurrent();
  projector.group.visible = false;
  app.mode = 'globe';
  ui.setStat('stMode', 'GLOBUS');
  globe.group.visible = true;
  const start = app.userLoc || { lat: 30, lon: 10 };
  app.gcam.lat = clamp(start.lat * 0.6, -60, 60); app.gcam.lon = start.lon - 70;
  const lon0 = app.gcam.lon;
  // Die Erde kommt aus der Tiefe nach vorne und wird größer
  await tween(1900, (k, raw) => {
    app.gscale = lerp(0.02, 1, k);
    app.gcam.alt = lerp(9, 2.7, k);
    app.gcam.lon = lon0 + 70 * k;
    globe.fade.value = clamp(raw * 2, 0, 1);
  }, easeOut);
};

app.showObject = async (entry) => {
  if (app.mode === 'object' && app.obj && app.obj.key === entry.key) return;
  await hideCurrent();
  const o = entry.build();
  mountObject(o);
};
app.showImageHolo = async (title, url) => {
  await hideCurrent();
  mountObject(buildImageHologram(title, url));
};
async function mountObject(o) {
  app.obj = o; app.mode = 'object';
  ui.setStat('stMode', 'HOLOGRAMM');
  projector.group.visible = true; projector.shared.fade.value = 1;
  app.explodeT = 0; app.explodeTarget = 0; app.userRot = 0;
  app.ocam.tgtDist = o.dist; app.ocam.el = o.el;
  for (const p of o.parts) p.base = p.obj.position.clone();
  o.shared.reveal.value = -2.6;
  scene.add(o.group);
  o.group.rotation.y = -0.6;
  await tween(1700, k => { o.shared.reveal.value = lerp(-2.6, 3.8, k); }, linear);
  o.shared.reveal.value = 100;
  if (o.parts.length) ui.showPanel('HOLOGRAMM · ' + o.title.toUpperCase(), `<h2>${o.title}</h2><p>${o.subtitle}</p><ul class="parts">${o.parts.map(p => `<li>${p.name}</li>`).join('')}</ul><p class="hint">„Öffne ihn“ zerlegt das Objekt · Ziehen zum Drehen · Mausrad zum Zoomen</p>`);
}

app.explode = (on) => {
  if (!app.obj) return;
  app.explodeTarget = on ? 1 : 0;
  app.ocam.tgtDist = on ? app.obj.distOpen : app.obj.dist;
  if (app.obj.parts.length) ui.showPanel((on ? 'EXPLOSIONSANSICHT · ' : 'HOLOGRAMM · ') + app.obj.title.toUpperCase(),
    `<h2>${app.obj.title}</h2><p>${app.obj.subtitle}</p><ul class="parts">${app.obj.parts.map(p => `<li>${p.name}${p.info ? `<br><small class="hint">${p.info}</small>` : ''}</li>`).join('')}</ul><p class="hint">${on ? '„Schließe ihn“ setzt alles wieder zusammen.' : '„Öffne ihn“ zerlegt das Objekt.'}</p>`);
};

/* ---------- Kamera-Flug über den Globus ---------- */
app.flyTo = (lat, lon, alt, ms) => {
  app.camTween?.cancel();
  app.follow = null;
  const a0 = { ...app.gcam };
  let dLon = ((lon - a0.lon + 540) % 360) - 180;
  const ang = haversine(a0.lat, a0.lon, lat, lon) / 6371;
  const hop = Math.max(0, Math.min(2.2, ang * 1.1) - Math.min(a0.alt, alt) * 0.4);
  const dur = ms ?? 1600 + ang * 700 + Math.abs(Math.log(alt / a0.alt)) * 350;
  const la0 = Math.log(a0.alt), la1 = Math.log(alt);
  app.camTween = tween(dur, k => {
    app.gcam.lat = lerp(a0.lat, lat, k);
    app.gcam.lon = a0.lon + dLon * k;
    app.gcam.alt = Math.exp(lerp(la0, la1, k)) + hop * Math.sin(Math.PI * k);
  }, easeInOut);
  return app.camTween;
};
const altForKm = km => clamp(km / 6371 * 1.7, 0.00028, 3);

app.goPlace = async (place, { quiet = false } = {}) => {
  await app.showGlobe();
  if (place.kind !== 'me') globe.clearMarkers('place');
  else globe.clearMarkers('me');
  globe.addMarker(place.lat, place.lon, place.name, place.sub || fmtCoord(place.lat, place.lon), place.kind || 'place');
  if (app.userLoc && place.kind !== 'me' && !globe.markers.some(m => m.kind === 'me') && haversine(app.userLoc.lat, app.userLoc.lon, place.lat, place.lon) > 3) globe.addMarker(app.userLoc.lat, app.userLoc.lon, 'Ihr Standort', app.userLoc.city, 'me');
  await app.flyTo(place.lat, place.lon, altForKm(place.km || 50));
};

app.setUserLocation = (place) => {
  const loc = { lat: place.lat, lon: place.lon, acc: 5000, src: 'Ihre Angabe', city: place.name, region: S.regionOf(place.addr) };
  app.userLoc = loc;
  try { localStorage.setItem('jarvis.home', JSON.stringify(loc)); } catch { /* */ }
  ui.setLocation(loc.city + (loc.region ? ', ' + loc.region : ''), fmtCoord(loc.lat, loc.lon));
};
app.myLocation = async (fresh = false) => {
  if (IS_ARTIFACT) {
    if (app.userLoc) return app.userLoc;
    throw new Error('Sagen Sie mir Ihren Ort, z. B. „Ich bin in Dortmund“.');
  }
  if (app.userLoc && !fresh) return app.userLoc;
  ui.setStat('stLink', 'ORTUNG …');
  const loc = await S.locate();
  try {
    const r = await S.reverse(loc.lat, loc.lon);
    loc.city = r.city; loc.region = [r.state, r.country].filter(Boolean).join(', ');
  } catch { loc.city = loc.city || 'Unbekannter Ort'; loc.region = ''; }
  app.userLoc = loc;
  ui.setLocation(`${loc.city}${loc.region ? ', ' + loc.region.split(', ').pop() : ''}`, fmtCoord(loc.lat, loc.lon));
  ui.setStat('stLink', 'BEREIT');
  return loc;
};

/* ---------- Flugverkehr ---------- */
app.setFlights = async (on, show = false) => {
  app.flightsOn = on;
  flights.visible = on;
  document.getElementById('tglFlights').classList.toggle('on', on);
  if (!on) { flights.select(null); app.follow = null; return; }
  if (show && app.mode !== 'globe') await app.showGlobe();
  app.lastFetch = 0;
};
app.loadFlightsAround = async (lat, lon, nm) => {
  const res = await S.flightsAround(lat, lon, nm);
  flights.set(res.list);
  await app.setFlights(true);
  app.lastFetch = performance.now();
  app.flightCenter = { lat, lon, nm };
  ui.setStat('stLink', res.src.toUpperCase() + ' ✓');
  ui.setStat('stFlights', fmt(flights.list.length));
  return res;
};
app.focusFlight = (p, follow = true) => {
  flights.select(p);
  if (follow) {
    app.camTween?.cancel();
    flights.now(p);
    const alt = clamp(app.gcam.alt, 0.06, 0.35);
    app.flyTo(p._lat, p._lon, alt, 1500).then(ok => { if (ok) app.follow = p; });
  }
};
async function autoRefreshFlights(now) {
  if (!app.flightsOn || app.mode !== 'globe' || app.fetching) return;
  const alt = app.gcam.alt;
  const interval = alt > 1.4 ? 60000 : 12000;
  if (now - app.lastFetch < interval) return;
  app.fetching = true;
  try {
    if (alt > 1.4) {
      ui.setStat('stLink', 'OPENSKY …');
      const list = await S.openskyAll();
      flights.set(list); ui.setStat('stLink', 'OPENSKY ✓');
    } else {
      const km = alt * 6371 * 0.5 * Math.max(1, innerWidth / innerHeight);
      const c = app.follow ? { lat: app.follow._lat, lon: app.follow._lon } : app.gcam;
      const res = await S.flightsAround(c.lat, c.lon, clamp(km / 1.852, 20, 250));
      flights.set(res.list); ui.setStat('stLink', res.src.toUpperCase() + ' ✓');
    }
    ui.setStat('stFlights', fmt(flights.list.length));
  } catch (e) {
    console.warn(e); ui.setStat('stLink', 'KEINE VERBINDUNG');
  } finally { app.fetching = false; app.lastFetch = performance.now(); }
}

/* ================= Eingabe (Maus / Touch) ================= */
let drag = null;
const touches = new Map();
let pinch = null;
function zoomBy(f) {
  app.lastInteract = performance.now();
  if (app.mode === 'globe') { app.camTween?.cancel(); app.gcam.alt = clamp(app.gcam.alt * f, 0.00012, 7); }
  else app.ocam.tgtDist = clamp(app.ocam.tgtDist * f, 3, 16);
}
stage.addEventListener('pointerdown', e => {
  touches.set(e.pointerId, { x: e.clientX, y: e.clientY });
  stage.setPointerCapture(e.pointerId);
  if (touches.size === 2) {
    const [a, b] = [...touches.values()];
    pinch = { d: Math.hypot(a.x - b.x, a.y - b.y) };
    if (drag) drag.moved = true;
    return;
  }
  drag = { x: e.clientX, y: e.clientY, sx: e.clientX, sy: e.clientY, moved: false };
});
stage.addEventListener('pointermove', e => {
  if (touches.has(e.pointerId)) touches.set(e.pointerId, { x: e.clientX, y: e.clientY });
  if (pinch && touches.size >= 2) {
    const [a, b] = [...touches.values()];
    const d = Math.hypot(a.x - b.x, a.y - b.y);
    if (d > 0 && pinch.d > 0) zoomBy(pinch.d / d);
    pinch.d = d;
    return;
  }
  if (!drag) return;
  const dx = e.clientX - drag.x, dy = e.clientY - drag.y;
  drag.x = e.clientX; drag.y = e.clientY;
  if (Math.abs(e.clientX - drag.sx) + Math.abs(e.clientY - drag.sy) > 6) drag.moved = true;
  if (!drag.moved) return;
  app.lastInteract = performance.now();
  if (app.mode === 'globe') {
    app.camTween?.cancel(); app.follow = null;
    const k = app.gcam.alt * 0.9 / DEG * (2 * Math.tan(camera.fov * DEG / 2)) / innerHeight;
    app.gcam.lon -= dx * k / Math.max(0.2, Math.cos(app.gcam.lat * DEG));
    app.gcam.lat = clamp(app.gcam.lat + dy * k, -82, 82);
  } else {
    app.userRot += dx * 0.008;
    app.ocam.el = clamp(app.ocam.el + dy * 0.004, -0.2, 1.3);
  }
});
const endPointer = e => {
  touches.delete(e.pointerId);
  if (touches.size < 2) pinch = null;
  if (e.type === 'pointerup' && drag && !drag.moved && app.mode === 'globe' && app.flightsOn) {
    const p = flights.pick(e.clientX, e.clientY, camera, innerWidth, innerHeight);
    if (p) assistant.flightDetail(p, true, false);
  }
  if (touches.size === 0) drag = null;
};
stage.addEventListener('pointerup', endPointer);
stage.addEventListener('pointercancel', endPointer);
stage.addEventListener('wheel', e => {
  e.preventDefault();
  zoomBy(Math.exp(e.deltaY * (app.mode === 'globe' ? 0.0012 : 0.001)));
}, { passive: false });
const fitFov = () => { camera.fov = camera.aspect < 1 ? Math.min(78, 45 / Math.pow(camera.aspect, 0.65)) : 45; };
fitFov();
addEventListener('resize', () => {
  camera.aspect = innerWidth / innerHeight; fitFov(); camera.updateProjectionMatrix();
  renderer.setSize(innerWidth, innerHeight); composer.setSize(innerWidth, innerHeight); labels.setSize(innerWidth, innerHeight);
  bloom.setSize(innerWidth, innerHeight);
});

/* ================= Kamera ================= */
const _T = new THREE.Vector3(), _n = new THREE.Vector3(), _N = new THREE.Vector3(), _up = new THREE.Vector3();
function updateCamera(dt) {
  if (app.mode === 'globe' || (globe.group.visible && app.mode === 'none')) {
    if (app.follow && (!app.camTween || app.camTween.done)) {
      flights.now(app.follow);
      let dl = ((app.follow._lon - app.gcam.lon + 540) % 360) - 180;
      app.gcam.lat = lerp(app.gcam.lat, app.follow._lat, Math.min(1, dt * 3));
      app.gcam.lon += dl * Math.min(1, dt * 3);
    }
    // sanfte Eigenrotation, wenn nichts passiert
    if (!app.camTween || app.camTween.done) {
      if (performance.now() - app.lastInteract > 6000 && app.gcam.alt > 1.2 && !app.follow) app.gcam.lon += dt * 3;
    }
    const s = app.gscale, alt = app.gcam.alt;
    globe.group.scale.setScalar(s);
    ll2v(app.gcam.lat, app.gcam.lon, s, _T);
    _n.copy(_T).normalize(); northAt(_n, _N);
    const tau = (1 - smooth(0.02, 0.9, alt)) * 52 * DEG;
    camera.position.copy(_T).addScaledVector(_n, Math.cos(tau) * alt).addScaledVector(_N, -Math.sin(tau) * alt);
    _up.copy(_N).multiplyScalar(Math.cos(tau)).addScaledVector(_n, Math.sin(tau));
    camera.up.copy(_up);
    camera.lookAt(_T);
    camera.near = clamp(alt * 0.2, 0.000004, 0.5);
    camera.far = alt < 0.5 ? 8 : 400;
  } else {
    app.ocam.dist = lerp(app.ocam.dist, app.ocam.tgtDist, Math.min(1, dt * 2.5));
    const d = app.ocam.dist, el = app.ocam.el;
    camera.up.set(0, 1, 0);
    const ty = 0.15 + 0.35 * app.explodeT, px = innerWidth > 900 ? 0.5 : 0;
    camera.position.set(px, Math.sin(el) * d + ty, Math.cos(el) * d);
    camera.lookAt(px, ty, 0);
    camera.near = 0.05; camera.far = 400;
  }
  camera.updateProjectionMatrix();
}

/* ================= Hauptschleife ================= */
let last = performance.now(), fpsAcc = 0, fpsN = 0;
function frame(now) {
  requestAnimationFrame(frame);
  const rdt = Math.min(0.25, (now - last) / 1000), dt = Math.min(0.05, rdt); last = now;
  U.time.value = now / 1000;
  updateTweens(now);

  const activity = assistant.speaking ? 1 : assistant.listening ? 0.6 : 0;
  if (core.group.visible) core.update(dt, activity);
  if (projector.group.visible) projector.update(dt);

  if (app.obj) {
    const o = app.obj;
    o.group.rotation.y += dt * (performance.now() - app.lastInteract > 4000 ? 0.18 : 0);
    o.group.rotation.y += app.userRot; app.userRot = 0;
    if (o.imageMat) { o.imageMat.uniforms.uTime.value = U.time.value; o.group.rotation.y = Math.sin(U.time.value * 0.4) * 0.35; }
    // Explosion
    const sp = rdt / 2.2;
    app.explodeT = app.explodeTarget > app.explodeT ? Math.min(app.explodeTarget, app.explodeT + sp) : Math.max(app.explodeTarget, app.explodeT - sp);
    for (const p of o.parts) {
      const k = easeInOut(clamp((app.explodeT * 1.6 - p.delay) / 1.0, 0, 1));
      p.obj.position.copy(p.base).addScaledVector(p.off, k);
      p.label.element.classList.toggle('show', k > 0.85);
      if (p.spin) for (const s of p.spin) s.rotation.y += dt * 18;
    }
  }

  updateCamera(dt);
  if (globe.group.visible) {
    globe.update(dt, { camera, alt: app.gcam.alt, fade: globe.fade.value, heightPx: innerHeight * renderer.getPixelRatio() });
    if (app.mode === 'globe' && (!app.camTween || app.camTween.done)) globe.requestPatch(app.gcam.lat, app.gcam.lon, app.gcam.alt, camera.aspect);
    flights.update(app.gcam.alt, globe.fade.value);
    if (!IS_ARTIFACT) {
      buildings.want(app.gcam.lat, app.gcam.lon, app.gcam.alt, !!(app.camTween && !app.camTween.done) || !!drag?.moved);
      buildings.update(app.gcam.alt, globe.fade.value);
    }
  } else flights.update(app.gcam.alt, 0);
  autoRefreshFlights(now);
  scene.userData.stars.rotation.y += dt * 0.004;

  composer.render();
  labels.render(scene, camera);

  fpsAcc += dt; fpsN++;
  if (fpsAcc > 1) { ui.setStat('stFps', Math.round(fpsN / fpsAcc)); fpsAcc = 0; fpsN = 0; }
}

/* ================= Assistent & UI ================= */
const brain = IS_ARTIFACT ? new ClaudeBrain() : new Brain();
const assistant = new Assistant(app, brain);
app.brain = brain;
const form = document.getElementById('cmdForm');
const input = document.getElementById('cmd');
form.addEventListener('submit', e => { e.preventDefault(); const v = input.value; input.value = ''; assistant.handle(v); });
if (IS_ARTIFACT) {
  for (const id of ['micBtn', 'tglWake', 'tglConvo', 'tglFlights', 'btnShare', 'btnInstall']) document.getElementById(id).hidden = true;
  input.placeholder = 'Schreib Jarvis … z. B. „Wo ist Dortmund?“';
  document.getElementById('attrib').textContent = 'Private Version · Geodaten Natural Earth & GeoNames · Antworten von Claude';
}
const mic = document.getElementById('micBtn');
if (!assistant.SR) mic.classList.add('na');
mic.addEventListener('click', () => assistant.listenOnce());
addEventListener('keydown', e => {
  if (e.code === 'Space' && document.activeElement !== input && !e.repeat) { e.preventDefault(); assistant.listenOnce(); }
});
document.getElementById('tglFlights').addEventListener('click', () => assistant.handle(app.flightsOn ? 'Flugverkehr ausblenden' : 'Zeig mir den Flugverkehr'));
document.getElementById('tglVoice').addEventListener('click', e => {
  assistant.voiceOn = !assistant.voiceOn; e.currentTarget.classList.toggle('on', assistant.voiceOn);
  if (!assistant.voiceOn) speechSynthesis?.cancel();
});
document.getElementById('tglWake').addEventListener('click', e => {
  const on = assistant.setWake(!assistant.wake);
  e.currentTarget.classList.toggle('on', on);
  if (on) ui.log('ai', 'Weckwort aktiv – sagen Sie „Jarvis, …“ gefolgt von Ihrer Frage.');
});
document.getElementById('btnHome').addEventListener('click', () => assistant.handle('zurück'));
document.getElementById('tglConvo').addEventListener('click', e => {
  if (!assistant.SR) return assistant.say('Spracherkennung wird von diesem Browser nicht unterstützt. Bitte Chrome oder Edge verwenden.');
  assistant.convo = !assistant.convo;
  e.currentTarget.classList.toggle('on', assistant.convo);
  if (assistant.convo) { assistant.say('Gesprächsmodus aktiv. Ich höre nach jeder Antwort automatisch wieder zu.'); }
});

/* ---------- KI-Kern (lokales Sprachmodell) ---------- */
const brainLabel = { off: 'AUS', loading: 'LÄDT', ready: 'BEREIT', error: 'FEHLER' };
function brainStatus() {
  ui.setStat('stBrain', brain.state === 'loading' ? `LÄDT ${Math.round(brain.progress * 100)} %` : brainLabel[brain.state]);
  ui.setStat('stMem', `${memory.experiences().length} / ${memory.facts().length}`);
  document.getElementById('btnBrain').classList.toggle('on', brain.ready);
}
let brainPanelOpen = false;
brain.onChange((state, progress, text) => {
  brainStatus();
  if (brainPanelOpen && document.getElementById('brainBar')) {
    document.getElementById('brainBar').style.width = `${Math.round(progress * 100)}%`;
    document.getElementById('brainTxt').textContent = text;
  }
});
setInterval(brainStatus, 3000);
function openBrainPanel() {
  brainPanelOpen = true;
  if (IS_ARTIFACT) {
    ui.showPanel('KI-KERN · GEHIRN', `
      <p>In dieser privaten Version denkt Jarvis mit <b>Claude</b> – über das claude.ai-Konto der Person, die die Seite öffnet. Kein Download, kein API-Key. Beim ersten Gespräch fragt claude.ai einmal um Erlaubnis.</p>
      <p class="hint">Status: ${brain.ready ? 'verbunden' : brain.state === 'loading' ? 'verbinde …' : 'nicht verbunden'}</p>
      <div class="sect"><h3>IHRE REGELN FÜR JARVIS</h3>
        <textarea class="rules" id="rulesBox" placeholder="z. B. Nenn mich Boss. Antworte immer ausführlich.">${memory.rules().replace(/</g, '&lt;')}</textarea>
        <button type="button" class="btn" data-id="rules">REGELN SPEICHERN</button>
        <p class="hint">Auch per Text: „Ab jetzt …“, „Hör auf …“, „Merke dir, dass …“</p></div>
      <div class="sect"><h3>GEDÄCHTNIS</h3><p>${memory.facts().length} Fakten über Sie</p>
        ${memory.facts().slice(-5).map(f => `<p class="hint">• ${f.text.replace(/</g, '&lt;')}</p>`).join('')}
        <button type="button" class="btn ghost" data-id="forget">GEDÄCHTNIS LÖSCHEN</button></div>`, id => {
      if (id === 'rules') { memory.setRules(document.getElementById('rulesBox').value); assistant.say('Ihre Regeln sind gespeichert.'); }
      else if (id === 'forget') { memory.clear(); brain.reset(); brainStatus(); openBrainPanel(); }
    });
    return;
  }
  const cur = brain.model || memory.model() || MODELS[1].id;
  const ok = Brain.supported();
  ui.showPanel('KI-KERN · GEHIRN', `
    <p>Jarvis' Gehirn ist ein offenes Sprachmodell, das <b>direkt in Ihrem Browser</b> läuft – ohne API-Key, ohne Kosten, ohne Konto. Es wird einmalig heruntergeladen und danach gespeichert. Vor jeder Antwort recherchiert Jarvis selbst im Internet und merkt sich, was er lernt.</p>
    ${ok ? '' : '<p style="color:var(--warn)">⚠ Dieser Browser unterstützt kein WebGPU. Bitte aktuelles Chrome oder Edge (PC/Mac/Android) bzw. Safari ab iOS 26 verwenden.</p>'}
    <div class="models">${MODELS.map(mo => `<label><input type="radio" name="model" value="${mo.id}" ${mo.id === cur ? 'checked' : ''}><b>${mo.name}</b><span class="hint">${mo.size}</span><small>${mo.note}</small></label>`).join('')}</div>
    <button type="button" class="btn" data-id="load">${brain.ready ? 'MODELL WECHSELN' : 'KI-KERN AKTIVIEREN'}</button>
    <div class="bar"><i id="brainBar" style="width:${Math.round(brain.progress * 100)}%"></i></div>
    <div id="brainTxt" class="hint">${brain.ready ? 'Bereit · ' + brain.model : brain.state === 'loading' ? 'Lädt …' : 'Nicht geladen'}</div>
    <div class="sect"><h3>IHRE REGELN FÜR JARVIS</h3>
      <textarea class="rules" id="rulesBox" placeholder="z. B. Nenn mich Boss. Antworte immer ausführlich. Keine Witze über …">${memory.rules().replace(/</g, '&lt;')}</textarea>
      <button type="button" class="btn" data-id="rules">REGELN SPEICHERN</button>
      <p class="hint">Auch per Sprache: „Ab jetzt …“, „Hör auf …“, „Merke dir, dass …“</p></div>
    <div class="sect"><h3>GEDÄCHTNIS</h3>
      <p>${memory.experiences().length} gesammelte Erfahrungen aus dem Internet · ${memory.facts().length} Fakten über Sie</p>
      ${memory.facts().slice(-5).map(f => `<p class="hint">• ${f.text.replace(/</g, '&lt;')}</p>`).join('')}
      <button type="button" class="btn ghost" data-id="forget">GEDÄCHTNIS LÖSCHEN</button></div>`, async id => {
    if (id === 'load') {
      const sel = document.querySelector('input[name=model]:checked')?.value || MODELS[1].id;
      assistant.say('Aktiviere meinen KI-Kern. Beim ersten Mal lade ich das Modell herunter, das kann einige Minuten dauern.');
      try { await brain.load(sel); assistant.say('KI-Kern online. Sie können mich jetzt alles fragen.'); }
      catch (e) { assistant.say('Der KI-Kern konnte nicht starten: ' + (Brain.supported() ? 'zu wenig Grafikspeicher oder Download unterbrochen. Versuchen Sie das leichte Modell.' : 'Ihr Browser unterstützt kein WebGPU.')); }
    } else if (id === 'rules') {
      memory.setRules(document.getElementById('rulesBox').value);
      assistant.say('Ihre Regeln sind gespeichert.');
    } else if (id === 'forget') {
      memory.clear(); brain.reset(); brainStatus(); openBrainPanel();
    }
  });
  const box = document.querySelector('[data-close]');
  box?.addEventListener('click', () => { brainPanelOpen = false; });
}
document.getElementById('btnBrain').addEventListener('click', openBrainPanel);

/* ---------- Teilen ---------- */
document.getElementById('btnShare').addEventListener('click', async () => {
  const url = location.origin + location.pathname;
  const data = { title: 'J.A.R.V.I.S.', text: 'Probier meinen holografischen KI-Assistenten aus:', url };
  try {
    if (navigator.share) { await navigator.share(data); return; }
    await navigator.clipboard.writeText(url);
    ui.log('ai', 'Link kopiert: ' + url);
  } catch (e) { if (e?.name !== 'AbortError') ui.log('ai', 'Link zum Weitergeben: ' + url); }
});

/* ---------- Installierbare App (PWA) ---------- */
let installEvt = null;
addEventListener('beforeinstallprompt', e => { e.preventDefault(); installEvt = e; document.getElementById('btnInstall').hidden = false; });
document.getElementById('btnInstall').addEventListener('click', async () => {
  if (!installEvt) return;
  installEvt.prompt();
  await installEvt.userChoice;
  installEvt = null; document.getElementById('btnInstall').hidden = true;
});

ui.chips(IS_ARTIFACT ? ['Ich bin in Dortmund', 'Wo ist Tokio?', 'Zeig mir einen PS5-Controller', 'Öffne ihn', 'Zeig mir eine Drohne', 'Erzähl mir einen Witz', 'Wie funktioniert ein Schwarzes Loch?', 'Merke dir, dass ich Fußball mag'] : ['Wo bin ich?', 'Wo ist Dortmund?', 'Zeig mir den Signal Iduna Park', 'Flüge über Frankfurt', 'Suche Flug LH400', 'Zeig mir einen PS5-Controller', 'Öffne ihn', 'Wie hoch ist der Eiffelturm?'], t => assistant.handle(t));
ui.startClock();

/* ================= Boot ================= */
const bootLog = document.getElementById('bootLog');
const bootBtn = document.getElementById('bootBtn');
const bl = t => { bootLog.textContent += `> ${t}\n`; };
requestAnimationFrame(frame);
(async () => {
  bl('Initialisiere Holo-Renderer … OK');
  bl('Lade neuronale Sprachmodule … ' + (assistant.SR ? 'OK' : 'nur Text (Browser ohne Spracherkennung)'));
  await globe.init(bl);
  try { const { loadCities } = await import('./offline.js'); globe.addCities(await loadCities()); bl('Städte-Datenbank geladen'); } catch { /* optional */ }
  bl('Verbinde Flugdaten-Netzwerk (adsb.lol / OpenSky) … BEREIT');
  bootBtn.disabled = false;
  bootBtn.textContent = 'SYSTEM STARTEN';
})();
bootBtn.addEventListener('click', async () => {
  document.getElementById('boot').classList.add('gone');
  ui.setState('ONLINE');
  const h = new Date().getHours();
  const g = h < 5 ? 'Guten Abend' : h < 11 ? 'Guten Morgen' : h < 18 ? 'Guten Tag' : 'Guten Abend';
  assistant.say(`${g}. Alle Systeme sind online. Wie kann ich helfen?`);
  if (IS_ARTIFACT) {
    let home = null; try { home = JSON.parse(localStorage.getItem('jarvis.home') || 'null'); } catch { /* */ }
    if (home) { app.userLoc = home; ui.setLocation(home.city + (home.region ? ', ' + home.region : ''), fmtCoord(home.lat, home.lon)); }
    else ui.setLocation('Sagen Sie: „Ich bin in …“', '');
    ui.setStat('stLink', 'OFFLINE-MODUS');
    brain.load().catch(() => {});
    return;
  }
  try { await app.myLocation(); } catch { ui.setLocation('Standort unbekannt', ''); ui.setStat('stLink', 'BEREIT'); }
  // KI-Kern automatisch starten, wenn er schon einmal geladen wurde (kommt dann aus dem Cache)
  const saved = memory.model();
  if (saved && Brain.supported()) brain.load(saved).catch(() => {});
  else if (Brain.supported() && !IS_ARTIFACT) ui.log('ai', 'Tipp: Aktivieren Sie meinen KI-Kern (🧠 unten), dann kann ich frei mit Ihnen reden und jede Frage beantworten – ganz ohne API-Key.');
});
