import * as THREE from 'three';
import { CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js';
import { U, DEG, ll2v, northAt, destPoint, clamp, smooth } from './util.js';

const MAX = 15000;
const _m = new THREE.Matrix4(), _p = new THREE.Vector3(), _n = new THREE.Vector3(), _N = new THREE.Vector3(), _E = new THREE.Vector3();
const _F = new THREE.Vector3(), _R = new THREE.Vector3(), _S = new THREE.Vector3(), _c = new THREE.Color();
const LOW = new THREE.Color(0xc9f7ff), HIGH = new THREE.Color(0x2f8dff), SEL = new THREE.Color(0xffb547);

function planeGeometry() {
  const r = [[0, 1], [0.11, 0.78], [0.12, 0.25], [0.95, -0.12], [0.95, -0.3], [0.12, -0.1], [0.09, -0.62], [0.38, -0.84], [0.38, -0.97], [0, -0.88]];
  const pts = [...r, ...r.slice(1, -1).reverse().map(([x, y]) => [-x, y])];
  const s = new THREE.Shape(pts.map(([x, y]) => new THREE.Vector2(x, y)));
  return new THREE.ShapeGeometry(s);
}

/** Stellt Flugzeuge als ausgerichtete Instanzen auf dem Globus dar (mit Koppelnavigation zwischen Updates). */
export class FlightLayer {
  constructor(parent) {
    this.parent = parent;
    this.planes = new Map();
    this.list = [];
    this.visible = false;
    this.selected = null;
    this.mesh = new THREE.InstancedMesh(planeGeometry(), new THREE.MeshBasicMaterial({
      color: 0xffffff, transparent: true, opacity: 0.95, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide,
    }), MAX);
    this.mesh.instanceMatrix.setUsage(THREE.DynamicDrawUsage);
    this.mesh.setColorAt(0, LOW);
    this.mesh.count = 0;
    this.mesh.frustumCulled = false;
    this.mesh.renderOrder = 5;
    parent.add(this.mesh);
    this.route = new THREE.Group();
    parent.add(this.route);
    this.routeLabels = [];
  }

  set(list, { replace = true } = {}) {
    const m = new Map();
    for (const p of list) if (!p.ground && p.lat != null) m.set(p.hex, p);
    if (!replace) for (const [k, v] of this.planes) if (!m.has(k)) m.set(k, v);
    if (this.selected) {
      const fresh = m.get(this.selected.hex);
      if (fresh) { fresh.route = this.selected.route; this.selected = fresh; } else m.set(this.selected.hex, this.selected);
    }
    this.planes = m;
    this.list = [...m.values()];
  }

  /** aktuelle (hochgerechnete) Position */
  now(p, t = Date.now()) {
    const dt = clamp((t - p.t) / 1000, 0, 120);
    if (!p.gsKt || dt < 0.5) { p._lat = p.lat; p._lon = p.lon; return p; }
    const d = destPoint(p.lat, p.lon, p.track, p.gsKt * 1.852 * dt / 3600);
    p._lat = d.lat; p._lon = d.lon;
    return p;
  }
  radius(p) { return 1.0005 + p.altFt * 0.3048 / 6371000 * 1.6; }

  update(alt, fade) {
    this.mesh.visible = this.visible && fade > 0.02;
    this.route.visible = this.mesh.visible;
    for (const l of this.routeLabels) l.visible = this.route.visible && fade > 0.6;
    if (!this.mesh.visible) return;
    this.mesh.material.opacity = 0.95 * fade;
    const s = clamp(alt * 0.018, 0.00045, 0.022);
    const t = Date.now();
    let i = 0;
    for (const p of this.list) {
      if (i >= MAX) break;
      this.now(p, t);
      ll2v(p._lat, p._lon, 1, _n).normalize();
      _p.copy(_n).multiplyScalar(this.radius(p));
      northAt(_n, _N);
      _E.crossVectors(_N, _n);
      const h = p.track * DEG;
      _F.copy(_N).multiplyScalar(Math.cos(h)).addScaledVector(_E, Math.sin(h));
      _R.copy(_E).multiplyScalar(Math.cos(h)).addScaledVector(_N, -Math.sin(h));
      const sel = this.selected && this.selected.hex === p.hex;
      const k = s * (sel ? 1.8 : 1);
      _m.makeBasis(_R, _F, _n).scale(_S.set(k, k, k)).setPosition(_p);
      this.mesh.setMatrixAt(i, _m);
      if (sel) _c.copy(SEL); else _c.copy(LOW).lerp(HIGH, clamp(p.altFt / 40000, 0, 1));
      this.mesh.setColorAt(i, _c);
      i++;
    }
    this.mesh.count = i;
    this.mesh.instanceMatrix.needsUpdate = true;
    if (this.mesh.instanceColor) this.mesh.instanceColor.needsUpdate = true;
  }

  /** Nächstes Flugzeug zu Bildschirmkoordinate */
  pick(x, y, camera, w, h) {
    let best = null, bd = 20 * 20;
    const v = new THREE.Vector3(), n = new THREE.Vector3(), cd = new THREE.Vector3();
    this.parent.updateMatrixWorld();
    for (const p of this.list) {
      if (p._lat == null) continue;
      ll2v(p._lat, p._lon, this.radius(p), v).applyMatrix4(this.parent.matrixWorld);
      n.copy(v).sub(this.parent.position).normalize();
      cd.copy(camera.position).sub(v).normalize();
      if (n.dot(cd) < 0.02) continue;
      v.project(camera);
      if (v.z > 1) continue;
      const sx = (v.x + 1) / 2 * w, sy = (1 - v.y) / 2 * h;
      const d = (sx - x) ** 2 + (sy - y) ** 2;
      if (d < bd) { bd = d; best = p; }
    }
    return best;
  }

  select(p) {
    this.selected = p;
    this.clearRoute();
    if (!p || !p.route) return;
    const [A, B] = [p.route.airports[0], p.route.airports[p.route.airports.length - 1]];
    const a = ll2v(A.lat, A.lon, 1).normalize(), b = ll2v(B.lat, B.lon, 1).normalize();
    const om = Math.acos(clamp(a.dot(b), -1, 1));
    const N = 160, pos = [], tt = [];
    const v = new THREE.Vector3();
    for (let i = 0; i <= N; i++) {
      const t = i / N;
      if (om < 1e-4) v.copy(a); else v.copy(a).multiplyScalar(Math.sin((1 - t) * om) / Math.sin(om)).addScaledVector(b, Math.sin(t * om) / Math.sin(om));
      v.normalize().multiplyScalar(1.001 + Math.sin(Math.PI * t) * om * 0.06);
      pos.push(v.x, v.y, v.z); tt.push(t);
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    g.setAttribute('aT', new THREE.Float32BufferAttribute(tt, 1));
    const mat = new THREE.ShaderMaterial({
      uniforms: { uTime: U.time },
      vertexShader: `attribute float aT; varying float vT; void main(){ vT=aT; gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0); }`,
      fragmentShader: `uniform float uTime; varying float vT; void main(){ float d=fract(vT*30.0-uTime*0.8); float a=0.35+0.65*smoothstep(0.5,0.0,abs(d-0.5)*2.0-0.3);
        gl_FragColor=vec4(mix(vec3(0.4,0.9,1.0),vec3(1.0,0.72,0.3),vT), a); }`,
      transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    });
    this.route.add(new THREE.Line(g, mat));
    for (const ap of [A, B]) {
      const div = document.createElement('div');
      div.className = 'mk-label ap';
      div.textContent = ap.iata || ap.icao;
      const l = new CSS2DObject(div);
      l.position.copy(ll2v(ap.lat, ap.lon, 1.002));
      this.route.add(l); this.routeLabels.push(l);
    }
  }
  clearRoute() {
    for (const l of this.routeLabels) l.element.remove();
    this.routeLabels = [];
    for (const c of [...this.route.children]) { this.route.remove(c); c.geometry?.dispose(); }
  }
}
