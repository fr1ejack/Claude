import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import { U, DEG, ll2v, northAt, clamp, smooth } from './util.js';

/*
 * Echte 3D-Gebäude als Hologramm – aus OpenStreetMap (Overpass API, kostenlos, ohne Key).
 * Grundrisse und Höhen kommen aus den OSM-Daten (height, building:levels, min_height);
 * Stadien, Kirchen, Hochhäuser usw. erscheinen dadurch in ihrer echten Form und Größe.
 */
const OVERPASS = ['https://overpass-api.de/api/interpreter', 'https://overpass.kumi.systems/api/interpreter'];
const EARTH = 6371000;

function heightOf(t) {
  const num = v => { const m = String(v ?? '').replace(',', '.').match(/[\d.]+/); return m ? parseFloat(m[0]) : NaN; };
  let h = num(t.height);
  if (isNaN(h)) { const lv = num(t['building:levels']); if (!isNaN(lv)) h = lv * 3.2 + (num(t['roof:levels']) || 0) * 2.5; }
  if (isNaN(h)) {
    const b = t.building || t['building:part'] || '';
    h = { house: 7, detached: 7, garage: 3, garages: 3, shed: 3, hut: 3, roof: 5, church: 22, cathedral: 45, stadium: 30, industrial: 10, warehouse: 10, apartments: 15, commercial: 14, office: 18, retail: 8, school: 11, hospital: 18, university: 16, train_station: 15 }[b] || 9;
  }
  let min = num(t.min_height);
  if (isNaN(min)) { const ml = num(t['building:min_level']); min = isNaN(ml) ? 0 : ml * 3.2; }
  return { h: Math.max(2, Math.min(h, 900)), min: Math.max(0, min) };
}

const VS = `
  uniform float uGrow; attribute float aH;
  varying vec3 vL; varying vec3 vN; varying vec3 vW; varying float vH;
  void main(){ vec3 p = position; p.y *= uGrow; vL = p; vH = aH;
    vec4 w = modelMatrix * vec4(p, 1.0); vW = w.xyz; vN = normalize(mat3(modelMatrix) * normal);
    gl_Position = projectionMatrix * viewMatrix * w; }`;
const FS = `
  uniform float uTime, uFade, uRadius; uniform vec3 uColor;
  varying vec3 vL; varying vec3 vN; varying vec3 vW; varying float vH;
  void main(){
    vec3 V = normalize(cameraPosition - vW);
    float f = pow(1.0 - abs(dot(normalize(vN), V)), 2.0);
    float d = length(vL.xz);
    float ring = smoothstep(25.0, 0.0, abs(d - mod(uTime * 260.0, uRadius * 1.4)));
    float floors = 0.8 + 0.2 * step(0.5, fract(vL.y / 3.2));
    float top = smoothstep(0.85, 1.0, vL.y / max(vH, 1.0));
    float edge = 1.0 - smoothstep(uRadius * 0.75, uRadius, d);
    float a = (0.06 + f * 0.5) * floors + top * 0.12 + ring * 0.5;
    vec3 col = mix(uColor, vec3(0.75, 0.95, 1.0), clamp(vH / 120.0, 0.0, 1.0));
    gl_FragColor = vec4(col, a * uFade * edge);
  }`;
const LFS = `
  uniform float uTime, uFade, uRadius; varying vec3 vL;
  void main(){ float d = length(vL.xz); float edge = 1.0 - smoothstep(uRadius * 0.75, uRadius, d);
    float ring = smoothstep(25.0, 0.0, abs(d - mod(uTime * 260.0, uRadius * 1.4)));
    gl_FragColor = vec4(vec3(0.55, 0.92, 1.0), (0.38 + ring * 0.6) * uFade * edge); }`;
const LVS = `uniform float uGrow; varying vec3 vL; void main(){ vec3 p = position; p.y *= uGrow; vL = p; gl_Position = projectionMatrix * modelViewMatrix * vec4(p, 1.0); }`;

export class Buildings {
  constructor(parent) {
    this.parent = parent;
    this.area = null;       // {lat, lon, r}
    this.loading = false;
    this.group = null;
    this.uniforms = { uTime: U.time, uFade: { value: 0 }, uGrow: { value: 0 }, uRadius: { value: 800 }, uColor: { value: new THREE.Color(0x5fd8ff) } };
    this.wanted = null;
    this.failedAt = 0;
    this.onStatus = null;
  }

  /** pro Frame: entscheiden, ob neue Gebäude geladen werden müssen */
  want(lat, lon, alt, moving) {
    if (alt > 0.0045 || moving) { this.wanted = null; return; }
    const r = clamp(alt * EARTH * 0.55, 350, 1100);
    if (this.area) {
      const dx = (lon - this.area.lon) * Math.cos(lat * DEG) * 111320, dy = (lat - this.area.lat) * 110540;
      if (Math.hypot(dx, dy) < this.area.r * 0.45 && r <= this.area.r * 1.3) return;
    }
    if (performance.now() - this.failedAt < 15000) return;
    if (!this.wanted || Math.abs(this.wanted.lat - lat) > 1e-5) this.wanted = { lat, lon, r, t: performance.now() };
    if (!this.loading && performance.now() - this.wanted.t > 700) { const w = this.wanted; this.wanted = null; this.load(w); }
  }

  async load({ lat, lon, r }) {
    this.loading = true;
    this.onStatus?.('GEBÄUDE …');
    const dLat = r / 110540, dLon = r / (111320 * Math.cos(lat * DEG));
    const bb = `${(lat - dLat).toFixed(6)},${(lon - dLon).toFixed(6)},${(lat + dLat).toFixed(6)},${(lon + dLon).toFixed(6)}`;
    const q = `[out:json][timeout:25];(way["building"](${bb});way["building:part"](${bb});relation["building"](${bb}););out geom;`;
    let data = null;
    for (const url of OVERPASS) {
      try {
        const c = new AbortController(); const t = setTimeout(() => c.abort(), 30000);
        const res = await fetch(url, { method: 'POST', body: 'data=' + encodeURIComponent(q), headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, signal: c.signal });
        clearTimeout(t);
        if (res.ok) { data = await res.json(); break; }
      } catch { /* nächster Server */ }
    }
    this.loading = false;
    if (!data) { this.failedAt = performance.now(); this.onStatus?.('GEBÄUDE ✗'); return; }
    this.build(data, lat, lon, r);
    this.onStatus?.(`${this.count} GEBÄUDE ✓`);
  }

  build(data, lat0, lon0, r) {
    const kx = Math.cos(lat0 * DEG) * 111320, kz = 110540;
    const toXZ = p => new THREE.Vector2((p.lon - lon0) * kx, (p.lat - lat0) * kz); // Shape-y = Norden
    // Gebäude, die in building:part zerlegt sind, nur über ihre Teile zeichnen
    const elems = data.elements || [];
    const hasParts = elems.some(e => e.tags?.['building:part']);
    const geoms = [], lines = [];
    let count = 0;
    const addRing = (pts, tags, holes = []) => {
      if (pts.length < 4) return;
      const shape = new THREE.Shape(pts.map(toXZ));
      for (const h of holes) if (h.length >= 4) shape.holes.push(new THREE.Path(h.map(toXZ)));
      const { h, min } = heightOf(tags);
      if (h - min < 1) return;
      let g;
      try { g = new THREE.ExtrudeGeometry(shape, { depth: h - min, bevelEnabled: false, curveSegments: 1 }); } catch { return; }
      g.rotateX(-Math.PI / 2);      // Shape-y (Norden) -> -z, Extrusion -> +y
      g.translate(0, min, 0);
      g.deleteAttribute('uv');
      const n = g.attributes.position.count;
      g.setAttribute('aH', new THREE.Float32BufferAttribute(new Float32Array(n).fill(h), 1));
      geoms.push(g);
      lines.push(new THREE.EdgesGeometry(g, 35));
      count++;
    };
    for (const e of elems) {
      const t = e.tags || {};
      if (hasParts && t.building && !t['building:part'] && e.type === 'way' && (t.height === undefined && t['building:levels'] === undefined)) {
        // Umriss mit Teilen: nur als flache Kontur, Teile liefern die 3D-Form
        if (elems.some(p => p.tags?.['building:part'] && p.bounds && e.bounds && p.bounds.minlat >= e.bounds.minlat && p.bounds.maxlat <= e.bounds.maxlat && p.bounds.minlon >= e.bounds.minlon && p.bounds.maxlon <= e.bounds.maxlon)) continue;
      }
      if (e.type === 'way' && e.geometry) addRing(e.geometry, t);
      else if (e.type === 'relation' && e.members) {
        const outers = e.members.filter(m => m.role === 'outer' && m.geometry), inners = e.members.filter(m => m.role === 'inner' && m.geometry).map(m => m.geometry);
        for (const o of outers) addRing(o.geometry, t, inners);
      }
      if (count > 6000) break;
    }
    this.clear();
    if (!geoms.length) { this.area = { lat: lat0, lon: lon0, r }; this.count = 0; return; }
    const mesh = new THREE.Mesh(mergeGeometries(geoms, false), new THREE.ShaderMaterial({
      uniforms: this.uniforms, vertexShader: VS, fragmentShader: FS,
      transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, side: THREE.DoubleSide,
    }));
    const edges = new THREE.LineSegments(mergeGeometries(lines, false), new THREE.ShaderMaterial({
      uniforms: this.uniforms, vertexShader: LVS, fragmentShader: LFS, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    }));
    geoms.forEach(g => g.dispose()); lines.forEach(g => g.dispose());
    const grp = new THREE.Group();
    grp.add(mesh, edges);
    // lokales Ost/Oben/Süd-Koordinatensystem am Mittelpunkt, 1 Einheit = 1 Meter
    const n = ll2v(lat0, lon0, 1).normalize(), N = northAt(n), E = new THREE.Vector3().crossVectors(N, n);
    const m = new THREE.Matrix4().makeBasis(E, n, N.clone().negate());
    m.scale(new THREE.Vector3(1 / EARTH, 1 / EARTH, 1 / EARTH));
    m.setPosition(n.multiplyScalar(1.00002));
    grp.matrixAutoUpdate = false; grp.matrix.copy(m);
    grp.renderOrder = 6;
    this.parent.add(grp);
    this.group = grp; this.area = { lat: lat0, lon: lon0, r }; this.count = count;
    this.uniforms.uRadius.value = r; this.uniforms.uGrow.value = 0; this.born = performance.now();
  }

  clear() {
    if (!this.group) return;
    this.parent.remove(this.group);
    this.group.traverse(o => o.geometry?.dispose());
    this.group = null;
  }

  update(alt, fade) {
    if (!this.group) return;
    const grow = clamp((performance.now() - this.born) / 1800, 0, 1);
    this.uniforms.uGrow.value = 1 - Math.pow(1 - grow, 3);
    this.uniforms.uFade.value = fade * smooth(0.009, 0.004, alt);
    this.group.visible = this.uniforms.uFade.value > 0.01;
  }
}
