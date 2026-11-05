import * as THREE from 'three';
import { CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js';
import { U, DEG, ll2v, clamp, smooth, esc, fmtCoord } from './util.js';

const TOPO_URL = 'https://cdn.jsdelivr.net/npm/world-atlas@2.0.2/countries-50m.json';
const TILE_URL = (z, x, y) => `https://${'abcd'[(x + y) % 4]}.basemaps.cartocdn.com/dark_all/${z}/${x}/${y}.png`;

/* ---------- TopoJSON-Dekodierung (ohne Abhängigkeit) ---------- */
function decodeArcs(topo) {
  const t = topo.transform;
  return topo.arcs.map(arc => {
    let x = 0, y = 0;
    return arc.map(p => {
      if (!t) return p;
      x += p[0]; y += p[1];
      return [x * t.scale[0] + t.translate[0], y * t.scale[1] + t.translate[1]];
    });
  });
}
function ring(arcs, idx) {
  const out = [];
  for (const i of idx) {
    const a = i < 0 ? arcs[~i].slice().reverse() : arcs[i];
    a.forEach((p, j) => { if (j === 0 && out.length) return; out.push(p); });
  }
  return out;
}
function polys(g, arcs) {
  if (!g) return [];
  if (g.type === 'Polygon') return [g.arcs.map(r => ring(arcs, r))];
  if (g.type === 'MultiPolygon') return g.arcs.map(p => p.map(r => ring(arcs, r)));
  if (g.type === 'GeometryCollection') return g.geometries.flatMap(x => polys(x, arcs));
  return [];
}

const GLOBE_VS = `varying vec3 vN; varying vec3 vP; varying vec3 vW;
  void main(){ vP = position; vec4 w = modelMatrix*vec4(position,1.0); vW=w.xyz; vN = normalize(mat3(modelMatrix)*normal); gl_Position = projectionMatrix*viewMatrix*w; }`;

export class Globe {
  constructor() {
    this.group = new THREE.Group();
    this.fade = { value: 0 };
    this.alt = 3;
    this.markers = [];
    this.patch = null; this.patchKey = ''; this.patchLoading = false; this.patchWanted = null; this.patchTimer = 0;

    // Ozean / Grundkugel mit Gitternetz und Fresnel-Rand
    this.ocean = new THREE.Mesh(new THREE.SphereGeometry(1, 128, 96), new THREE.ShaderMaterial({
      uniforms: { uFade: this.fade, uTime: U.time, uGrid: { value: 1 } },
      vertexShader: GLOBE_VS,
      fragmentShader: `uniform float uFade, uTime, uGrid; varying vec3 vN; varying vec3 vP; varying vec3 vW;
        void main(){
          vec3 V = normalize(cameraPosition - vW);
          float f = pow(1.0 - max(dot(normalize(vN), V), 0.0), 3.0);
          vec3 p = normalize(vP);
          float lat = asin(p.y) / 0.0174533; float lon = atan(p.z, -p.x) / 0.0174533;
          vec2 g = abs(fract(vec2(lat, lon) / 10.0 + 0.5) - 0.5) * 10.0;
          vec2 w = fwidth(vec2(lat, lon)) * 1.2;
          float grid = (1.0 - min(min(g.x / w.x, g.y / w.y), 1.0)) * uGrid;
          float scan = smoothstep(0.6, 0.0, abs(lat - (mod(uTime * 18.0, 220.0) - 110.0))) * 0.25;
          vec3 col = vec3(0.004, 0.035, 0.075) + vec3(0.1, 0.55, 0.95) * (f * 0.9 + grid * 0.16 + scan * 0.25);
          gl_FragColor = vec4(col, uFade);
        }`,
      transparent: true,
    }));
    this.group.add(this.ocean);

    // Atmosphäre
    this.atmo = new THREE.Mesh(new THREE.SphereGeometry(1.14, 64, 48), new THREE.ShaderMaterial({
      uniforms: { uFade: this.fade },
      vertexShader: `varying vec3 vN; varying vec3 vW; void main(){ vec4 w=modelMatrix*vec4(position,1.0); vW=w.xyz; vN=normalize(mat3(modelMatrix)*normal); gl_Position=projectionMatrix*viewMatrix*w; }`,
      fragmentShader: `uniform float uFade; varying vec3 vN; varying vec3 vW;
        void main(){ vec3 V=normalize(cameraPosition-vW); float d=dot(normalize(vN),V); float i=pow(clamp(0.62+d,0.0,1.0),5.0);
          gl_FragColor=vec4(vec3(0.25,0.75,1.0)*i*1.6, i*uFade); }`,
      side: THREE.BackSide, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    }));
    this.group.add(this.atmo);

    // HUD-Ringe um den Globus (zeigen immer zur Kamera)
    this.hud = new THREE.Group();
    this.hudMat = [];
    const ringMat = (seg, speed, op) => {
      const m = new THREE.ShaderMaterial({
        uniforms: { uTime: U.time, uSeg: { value: seg }, uSpeed: { value: speed }, uOp: { value: op }, uFade: { value: 0 } },
        vertexShader: `varying vec3 vP; void main(){ vP=position; gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0); }`,
        fragmentShader: `uniform float uTime,uSeg,uSpeed,uOp,uFade; varying vec3 vP;
          void main(){ float a = atan(vP.y, vP.x)/6.28318+0.5; float s = uSeg>0.0 ? step(0.35, fract(a*uSeg + uTime*uSpeed)) : 1.0;
            gl_FragColor = vec4(0.4,0.85,1.0, s*uOp*uFade); }`,
        transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, side: THREE.DoubleSide,
      });
      this.hudMat.push(m); return m;
    };
    for (const [r0, r1, seg, sp, op] of [[1.22, 1.226, 0, 0, 0.5], [1.27, 1.3, 90, 0.02, 0.35], [1.36, 1.362, 6, -0.03, 0.6], [1.42, 1.425, 0, 0, 0.18]]) {
      this.hud.add(new THREE.Mesh(new THREE.RingGeometry(r0, r1, 192), ringMat(seg, sp, op)));
    }
    this.group.add(this.hud);

    this.lines = null; this.dots = null;
    this.markerGroup = new THREE.Group();
    this.group.add(this.markerGroup);
  }

  async init(onProgress) {
    let topo;
    try {
      onProgress?.('Lade Geodaten (Natural Earth) …');
      topo = await (await fetch(TOPO_URL)).json();
    } catch (e) {
      console.warn('Geodaten nicht ladbar', e);
      onProgress?.('⚠ Geodaten offline – Globus ohne Landmassen');
      return;
    }
    const arcs = decodeArcs(topo);
    onProgress?.('Berechne Küstenlinien & Grenzen …');
    // Linien (Küsten + Grenzen)
    const lp = [];
    const a = new THREE.Vector3(), b = new THREE.Vector3();
    for (const arc of arcs) {
      for (let i = 1; i < arc.length; i++) {
        const p0 = arc[i - 1], p1 = arc[i];
        if (Math.abs(p1[0] - p0[0]) > 180) continue;
        ll2v(p0[1], p0[0], 1.0012, a); ll2v(p1[1], p1[0], 1.0012, b);
        lp.push(a.x, a.y, a.z, b.x, b.y, b.z);
      }
    }
    const lg = new THREE.BufferGeometry();
    lg.setAttribute('position', new THREE.Float32BufferAttribute(lp, 3));
    this.lineMat = new THREE.LineBasicMaterial({ color: 0x7fe6ff, transparent: true, opacity: 0.55, blending: THREE.AdditiveBlending, depthWrite: false });
    this.lines = new THREE.LineSegments(lg, this.lineMat);
    this.group.add(this.lines);

    // Landmaske zeichnen
    onProgress?.('Rastere Kontinente …');
    const W = 2048, H = 1024;
    const cv = document.createElement('canvas'); cv.width = W; cv.height = H;
    const ctx = cv.getContext('2d');
    ctx.fillStyle = '#fff';
    const land = topo.objects.land || topo.objects.countries;
    for (const poly of polys(land, arcs)) {
      ctx.beginPath();
      for (const r of poly) {
        r.forEach(([lon, lat], i) => {
          const x = (lon + 180) / 360 * W, y = (90 - lat) / 180 * H;
          if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.closePath();
      }
      ctx.fill('evenodd');
    }
    const img = ctx.getImageData(0, 0, W, H).data;
    const isLand = (lat, lon) => {
      const x = Math.min(W - 1, Math.floor((lon + 180) / 360 * W)), y = Math.min(H - 1, Math.floor((90 - lat) / 180 * H));
      return img[(y * W + x) * 4] > 128;
    };

    // Punktraster (Fibonacci-Kugel)
    onProgress?.('Erzeuge holografisches Punktraster …');
    const N = 160000, golden = Math.PI * (3 - Math.sqrt(5));
    const pos = [], rnd = [];
    for (let i = 0; i < N; i++) {
      const y = 1 - (i + 0.5) * 2 / N, r = Math.sqrt(1 - y * y), th = golden * i;
      const x = Math.cos(th) * r, z = Math.sin(th) * r;
      const lat = Math.asin(y) / DEG;
      let lon = Math.atan2(z, -x) / DEG - 180; if (lon < -180) lon += 360;
      if (!isLand(lat, lon)) continue;
      pos.push(x * 1.002, y * 1.002, z * 1.002); rnd.push(Math.random());
    }
    const dg = new THREE.BufferGeometry();
    dg.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    dg.setAttribute('aRnd', new THREE.Float32BufferAttribute(rnd, 1));
    this.dotMat = new THREE.ShaderMaterial({
      uniforms: { uTime: U.time, uFade: { value: 0 }, uScale: { value: 600 }, uSize: { value: 0.0075 } },
      vertexShader: `attribute float aRnd; uniform float uTime, uScale, uSize; varying float vA;
        void main(){ vec4 mv = modelViewMatrix*vec4(position,1.0); gl_Position = projectionMatrix*mv;
          gl_PointSize = clamp(uSize*uScale / -mv.z, 1.0, 5.0);
          vec3 wp = (modelMatrix*vec4(position,1.0)).xyz; vec3 wn = normalize(mat3(modelMatrix)*position);
          float facing = dot(wn, normalize(cameraPosition-wp));
          vA = smoothstep(-0.05, 0.35, facing) * (0.6 + 0.4*sin(uTime*1.3 + aRnd*40.0)); }`,
      fragmentShader: `uniform float uFade; varying float vA;
        void main(){ float d = length(gl_PointCoord-0.5); if(d>0.5) discard;
          gl_FragColor = vec4(vec3(0.45,0.88,1.0), vA*uFade*smoothstep(0.5,0.15,d)); }`,
      transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    });
    this.dots = new THREE.Points(dg, this.dotMat);
    this.group.add(this.dots);
    onProgress?.(`Globus bereit · ${pos.length / 3 | 0} Landpunkte`);
  }

  /* ---------- Marker ---------- */
  addMarker(lat, lon, title, sub = '', kind = 'place') {
    const g = new THREE.Group();
    const n = ll2v(lat, lon, 1).normalize();
    g.position.copy(n);
    g.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), n);
    const col = kind === 'me' ? 0xffb547 : 0x7fe9ff;
    const bm = new THREE.MeshBasicMaterial({ color: col, transparent: true, opacity: 0.9, blending: THREE.AdditiveBlending, depthWrite: false });
    const beam = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.03, 1, 8, 1, true), bm);
    beam.position.y = 0.5; g.add(beam);
    const ringM = new THREE.MeshBasicMaterial({ color: col, transparent: true, opacity: 0.9, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide });
    const ring1 = new THREE.Mesh(new THREE.RingGeometry(0.22, 0.28, 48), ringM); ring1.rotation.x = -Math.PI / 2; g.add(ring1);
    const ring2 = new THREE.Mesh(new THREE.RingGeometry(0.5, 0.53, 64), ringM.clone()); ring2.rotation.x = -Math.PI / 2; g.add(ring2);
    const dot = new THREE.Mesh(new THREE.CircleGeometry(0.1, 24), bm); dot.rotation.x = -Math.PI / 2; g.add(dot);
    const div = document.createElement('div');
    div.className = 'mk-label' + (kind === 'me' ? ' me' : '');
    div.innerHTML = `${esc(title)}<small>${esc(sub || fmtCoord(lat, lon))}</small>`;
    const label = new CSS2DObject(div); label.position.y = 1.05; g.add(label);
    this.markerGroup.add(g);
    const m = { g, lat, lon, kind, label, ring2, born: performance.now() };
    this.markers.push(m);
    return m;
  }
  clearMarkers(kind) {
    this.markers = this.markers.filter(m => {
      if (kind && m.kind !== kind) return true;
      m.label.element.remove();
      this.markerGroup.remove(m.g);
      return false;
    });
  }
  setLabelsVisible(v) { this._labelsOn = v; }

  /* ---------- Holografische Kachelkarte (Mercator-Kacheln auf Kugelsegment) ---------- */
  requestPatch(lat, lon, alt, aspect) {
    if (alt > 0.95) return;
    const viewDeg = alt * 0.83 / DEG * Math.max(1, aspect) * 1.2;
    const z = clamp(Math.round(Math.log2(360 / (viewDeg * 1.5 / 6))), 4, 16);
    const n = 2 ** z;
    const latR = clamp(lat, -84, 84) * DEG;
    const fx = (lon + 180) / 360 * n, fy = (1 - Math.log(Math.tan(latR) + 1 / Math.cos(latR)) / Math.PI) / 2 * n;
    const x0 = Math.floor(fx - 3), y0 = Math.floor(fy - 3);
    const key = `${z}/${x0}/${y0}`;
    if (key === this.patchKey || (this.patchWanted && this.patchWanted.key === key)) return;
    this.patchWanted = { z, x0, y0, key, t: performance.now() };
  }
  _loadPatch({ z, x0, y0, key }) {
    const T = 6, S = 256, n = 2 ** z;
    const cv = document.createElement('canvas'); cv.width = cv.height = T * S;
    const ctx = cv.getContext('2d');
    ctx.fillStyle = '#000'; ctx.fillRect(0, 0, cv.width, cv.height);
    const tex = new THREE.CanvasTexture(cv);
    tex.anisotropy = 8;
    const jobs = [];
    for (let j = 0; j < T; j++) for (let i = 0; i < T; i++) {
      const ty = y0 + j; if (ty < 0 || ty >= n) continue;
      const tx = ((x0 + i) % n + n) % n;
      jobs.push(new Promise(res => {
        const im = new Image(); im.crossOrigin = 'anonymous';
        im.onload = () => { ctx.drawImage(im, i * S, j * S); tex.needsUpdate = true; res(true); };
        im.onerror = () => res(false);
        im.src = TILE_URL(z, tx, ty);
      }));
    }
    // Geometrie
    const SEG = 72, P = [], UV = [], IDX = [];
    const v = new THREE.Vector3();
    for (let j = 0; j <= SEG; j++) for (let i = 0; i <= SEG; i++) {
      const tx = x0 + T * i / SEG, ty = y0 + T * j / SEG;
      const lon = tx / n * 360 - 180;
      const lat = Math.atan(Math.sinh(Math.PI * (1 - 2 * ty / n))) / DEG;
      ll2v(lat, lon, 1.0006, v); P.push(v.x, v.y, v.z); UV.push(i / SEG, 1 - j / SEG);
    }
    for (let j = 0; j < SEG; j++) for (let i = 0; i < SEG; i++) {
      const a = j * (SEG + 1) + i, b = a + 1, c = a + SEG + 1, d = c + 1;
      IDX.push(a, c, b, b, c, d);
    }
    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.Float32BufferAttribute(P, 3));
    geo.setAttribute('uv', new THREE.Float32BufferAttribute(UV, 2));
    geo.setIndex(IDX);
    const mat = new THREE.ShaderMaterial({
      uniforms: { uMap: { value: tex }, uOp: { value: 0 }, uTime: U.time },
      vertexShader: `varying vec2 vUv; void main(){ vUv=uv; gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0); }`,
      fragmentShader: `uniform sampler2D uMap; uniform float uOp, uTime; varying vec2 vUv;
        void main(){ vec3 c=texture2D(uMap,vUv).rgb; float l=dot(c,vec3(0.3,0.59,0.11));
          vec2 d=abs(vUv-0.5)*2.0; float edge=1.0-smoothstep(0.55,1.0,max(d.x,d.y));
          float scan=0.85+0.15*sin(vUv.y*900.0-uTime*4.0);
          vec3 col=vec3(0.16,0.66,1.0)*min(pow(l,0.85)*1.25,0.9)*scan + vec3(0.0,0.015,0.035);
          gl_FragColor=vec4(col, uOp*edge); }`,
      transparent: true, depthWrite: false, polygonOffset: true, polygonOffsetFactor: -2,
    });
    const mesh = new THREE.Mesh(geo, mat);
    mesh.renderOrder = 2;
    this.patchLoading = true;
    Promise.all(jobs).then(r => {
      this.patchLoading = false;
      if (!r.some(Boolean)) { geo.dispose(); mat.dispose(); tex.dispose(); this.patchKey = key; return; }
      const old = this.patch;
      this.patch = mesh; this.patchKey = key; mesh.userData.born = performance.now();
      this.group.add(mesh);
      if (old) { old.userData.dying = performance.now(); this._dying = old; }
    });
  }

  /** pro Frame */
  update(dt, ctx) {
    const { camera, alt, fade, heightPx } = ctx;
    this.alt = alt;
    this.fade.value = fade;
    const near = smooth(0.06, 0.5, alt);
    if (this.dotMat) {
      this.dotMat.uniforms.uFade.value = fade * (0.25 + 0.75 * near);
      this.dotMat.uniforms.uScale.value = heightPx / (2 * Math.tan(camera.fov * DEG / 2));
    }
    if (this.lineMat) this.lineMat.opacity = fade * (0.25 + 0.4 * near);
    this.ocean.material.uniforms.uGrid.value = 0.4 + 0.6 * near;
    // HUD-Ringe
    const hudF = smooth(0.9, 1.8, alt) * fade;
    this.hud.visible = hudF > 0.01;
    this.hud.quaternion.copy(camera.quaternion);
    for (const m of this.hudMat) m.uniforms.uFade.value = hudF;
    // Karte
    const now = performance.now();
    if (this.patchWanted && !this.patchLoading && now - this.patchWanted.t > 350) { const w = this.patchWanted; this.patchWanted = null; this._loadPatch(w); }
    if (this.patch) {
      const born = clamp((now - this.patch.userData.born) / 900, 0, 1);
      this.patch.material.uniforms.uOp.value = born * fade * smooth(1.0, 0.45, alt);
    }
    if (this._dying) {
      const k = 1 - clamp((now - this._dying.userData.dying) / 600, 0, 1);
      this._dying.material.uniforms.uOp.value *= k;
      if (k <= 0) { this.group.remove(this._dying); this._dying.geometry.dispose(); this._dying.material.uniforms.uMap.value.dispose(); this._dying.material.dispose(); this._dying = null; }
    }
    // Marker skalieren & verdecken
    const s = clamp(alt * 0.12, 0.0012, 0.22);
    const wp = new THREE.Vector3(), wn = new THREE.Vector3(), cd = new THREE.Vector3();
    for (const m of this.markers) {
      const age = clamp((now - m.born) / 900, 0, 1);
      m.g.scale.setScalar(s * (0.2 + 0.8 * age));
      const p = (now / 1400 + (m.kind === 'me' ? 0.5 : 0)) % 1;
      m.ring2.scale.setScalar(0.5 + p * 1.6);
      m.ring2.material.opacity = (1 - p) * 0.9;
      m.g.getWorldPosition(wp); wn.copy(wp).sub(this.group.position).normalize();
      cd.copy(camera.position).sub(wp).normalize();
      m.label.visible = this._labelsOn !== false && fade > 0.6 && wn.dot(cd) > 0.08;
    }
  }
}
