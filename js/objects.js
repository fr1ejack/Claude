import * as THREE from 'three';
import { RoundedBoxGeometry } from 'three/addons/geometries/RoundedBoxGeometry.js';
import { CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js';
import { holoMesh, sharedState } from './materials.js';

/*
 * Prozedurale Hologramm-Objekte. Jedes Objekt besteht aus Einzelteilen mit
 * einem Explosions-Versatz, damit Jarvis sie "öffnen" (zerlegen) kann.
 */

const COL = { shell: 0x5fd8ff, inner: 0x3fa6ff, board: 0x42ffd5, metal: 0xa8ecff, hot: 0xffc36b, glass: 0x9fe9ff };

const rbox = (w, h, d, r = 0.04) => new RoundedBoxGeometry(w, h, d, 3, Math.min(r, Math.min(w, h, d) / 2 - 0.002));
const cyl = (rt, rb, h, s = 32) => new THREE.CylinderGeometry(rt, rb, h, s);

class Builder {
  constructor() {
    this.shared = sharedState();
    this.root = new THREE.Group();
    this.parts = [];
  }
  mesh(geom, opts = {}) { return holoMesh(geom, { shared: this.shared, ...opts }); }
  /** Teil registrieren: obj (bereits positioniert), Versatz, Verzögerung, Beschriftung */
  part(name, info, obj, off, delay = 0, extra = {}) {
    const box = new THREE.Box3().setFromObject(obj);
    const c = box.getCenter(new THREE.Vector3());
    const holder = new THREE.Group();
    holder.add(obj);
    const div = document.createElement('div');
    div.className = 'part-label';
    div.innerHTML = `${name}${info ? `<small>${info}</small>` : ''}`;
    const label = new CSS2DObject(div);
    label.position.set(box.max.x + 0.05, c.y, c.z);
    holder.add(label);
    this.root.add(holder);
    this.parts.push({ name, info, obj: holder, label, off: new THREE.Vector3(...off), delay, ...extra });
    return holder;
  }
}

/* ======================= Game-Controller ======================= */
function controllerShape(scale = 1) {
  const right = [[0, 0.62], [0.55, 0.66], [1.05, 0.73], [1.42, 0.63], [1.66, 0.38], [1.78, 0.02], [1.87, -0.45], [1.92, -0.92], [1.83, -1.27], [1.6, -1.44], [1.35, -1.39], [1.15, -1.1], [0.95, -0.74], [0.7, -0.52], [0.35, -0.45], [0, -0.45]];
  const left = right.slice(1, -1).reverse().map(([x, y]) => [-x, y]);
  const pts = [...right, ...left].map(([x, y]) => new THREE.Vector2(x * scale, (y + 0.35) * scale - 0.35 * scale + 0.35 * (scale - 1)));
  const s = new THREE.Shape();
  s.moveTo(pts[0].x, pts[0].y);
  s.splineThru([...pts.slice(1), pts[0]]);
  return s;
}
function shellGeom(depth, y, scale = 1) {
  const g = new THREE.ExtrudeGeometry(controllerShape(scale), { depth, bevelEnabled: true, bevelThickness: 0.06, bevelSize: 0.07, bevelSegments: 3, curveSegments: 10 });
  g.rotateX(-Math.PI / 2);
  g.translate(0, y, 0);
  return g;
}
// 2D-Layout (x, f) -> Welt (x, y, -f); anschließend wird die Gruppe zentriert
const P = (x, y, f) => new THREE.Vector3(x, y, -f);

function buildController() {
  const B = new Builder();
  const at = (obj, v, rot) => { obj.position.copy(v); if (rot) obj.rotation.set(...rot); return obj; };
  const pair = (fn) => { const g = new THREE.Group(); g.add(fn(-1)); g.add(fn(1)); return g; };

  B.part('Obere Gehäuseschale', 'Oberschale mit Aussparungen für Tasten & Sticks',
    B.mesh(shellGeom(0.22, -0.02 + 0.0), { color: COL.shell, base: 0.035, rim: 0.75, edgeAngle: 35 }), [0, 1.7, 0], 0.05);

  B.part('Touchpad', 'Kapazitives 2-Punkt-Touchpad, klickbar',
    at(B.mesh(rbox(0.95, 0.05, 0.52, 0.05), { color: COL.glass, base: 0.12 }), P(0, 0.33, 0.32)), [0, 2.65, -0.25], 0.22);

  B.part('LED-Lichtleiste', 'RGB-Lichtleiter neben dem Touchpad',
    pair(sx => at(B.mesh(rbox(0.035, 0.03, 0.5, 0.012), { color: COL.hot, base: 0.6 }), P(sx * 0.52, 0.32, 0.32))), [0, 2.25, -0.65], 0.25);

  B.part('Analogstick-Kappen', 'Gummierte Daumenkappen',
    pair(sx => {
      const g = new THREE.Group();
      g.add(at(B.mesh(cyl(0.2, 0.19, 0.07, 40), { color: COL.shell, base: 0.08 }), new THREE.Vector3(0, 0.46, 0)));
      g.add(at(B.mesh(cyl(0.06, 0.07, 0.2, 16), { color: COL.metal, edges: false }), new THREE.Vector3(0, 0.33, 0)));
      g.position.copy(P(sx * 0.52, 0, -0.2));
      return g;
    }), [0, 2.35, 0.7], 0.3);

  B.part('Stick-Module', '3D-Analog-Sensoren (Potentiometer)',
    pair(sx => at(B.mesh(rbox(0.38, 0.2, 0.38, 0.03), { color: COL.inner, base: 0.06 }), P(sx * 0.52, 0.08, -0.2))), [0, 1.2, 0.7], 0.45);

  B.part('Steuerkreuz', 'Richtungstasten',
    (() => { const g = new THREE.Group(); g.add(B.mesh(rbox(0.4, 0.06, 0.12, 0.02))); const b = B.mesh(rbox(0.12, 0.06, 0.4, 0.02)); g.add(b); g.position.copy(P(-1.2, 0.33, 0.05)); return g; })(),
    [-0.55, 2.1, 0], 0.3);

  B.part('Aktionstasten', '△ ○ ✕ □',
    (() => {
      const g = new THREE.Group();
      for (const [dx, df] of [[0.19, 0], [-0.19, 0], [0, 0.19], [0, -0.19]]) g.add(at(B.mesh(cyl(0.075, 0.075, 0.06, 24), { base: 0.1 }), new THREE.Vector3(dx, 0, -df)));
      g.position.copy(P(1.2, 0.33, 0.05)); return g;
    })(), [0.55, 2.1, 0], 0.3);

  B.part('Systemtasten', 'Home-, Create- & Options-Taste, Mikrofon-Taste',
    (() => {
      const g = new THREE.Group();
      g.add(at(B.mesh(cyl(0.07, 0.07, 0.05, 24), { color: COL.hot, base: 0.25 }), P(0, 0.32, -0.14)));
      g.add(at(B.mesh(rbox(0.16, 0.025, 0.05, 0.01), { base: 0.2 }), P(0, 0.31, -0.3)));
      g.add(at(B.mesh(rbox(0.06, 0.04, 0.15, 0.02)), P(-0.66, 0.33, 0.45)));
      g.add(at(B.mesh(rbox(0.06, 0.04, 0.15, 0.02)), P(0.66, 0.33, 0.45)));
      return g;
    })(), [0, 1.95, 1.0], 0.35);

  B.part('Schultertasten L1/R1', '',
    pair(sx => at(B.mesh(rbox(0.56, 0.1, 0.16, 0.04)), P(sx * 1.2, 0.2, 0.72))), [0, 1.45, -0.7], 0.2);

  B.part('Trigger L2/R2', 'Analoge Abzugstasten',
    pair(sx => at(B.mesh(rbox(0.46, 0.32, 0.2, 0.06), { base: 0.06 }), P(sx * 1.2, -0.03, 0.84), [-0.35, 0, 0])), [0, 0.55, -1.05], 0.2);

  B.part('Adaptive-Trigger-Motoren', 'Erzeugen variablen Widerstand an L2/R2',
    pair(sx => {
      const g = new THREE.Group();
      g.add(at(B.mesh(cyl(0.085, 0.085, 0.3, 24), { color: COL.hot, base: 0.12 }), new THREE.Vector3(0, 0, 0), [0, 0, Math.PI / 2]));
      g.add(at(B.mesh(rbox(0.12, 0.12, 0.14, 0.02), { color: COL.metal }), new THREE.Vector3(sx * -0.2, 0, 0)));
      g.position.copy(P(sx * 1.05, -0.02, 0.5)); return g;
    }), [0, 0.35, -1.6], 0.4);

  B.part('Hauptplatine', 'SoC, Bluetooth-Modul, Gyroskop & Beschleunigungssensor',
    (() => {
      const g = new THREE.Group();
      const bg = new THREE.ExtrudeGeometry(controllerShape(0.82), { depth: 0.03, bevelEnabled: false, curveSegments: 10 });
      bg.rotateX(-Math.PI / 2); bg.translate(0, -0.03, -0.06);
      g.add(B.mesh(bg, { color: COL.board, base: 0.08, edgeAngle: 35 }));
      g.add(at(B.mesh(rbox(0.3, 0.05, 0.3, 0.01), { color: COL.board, base: 0.25 }), new THREE.Vector3(0, 0.02, 0.05)));
      g.add(at(B.mesh(rbox(0.16, 0.04, 0.12, 0.01), { color: COL.board, base: 0.2 }), new THREE.Vector3(0.36, 0.02, 0.1)));
      g.add(at(B.mesh(rbox(0.12, 0.04, 0.12, 0.01), { color: COL.board, base: 0.2 }), new THREE.Vector3(-0.34, 0.02, 0.05)));
      return g;
    })(), [0, 0.2, 0], 0.55);

  B.part('Akku', 'Lithium-Ionen, ca. 1.560 mAh',
    at(B.mesh(rbox(1.0, 0.14, 0.4, 0.05), { color: COL.hot, base: 0.08 }), new THREE.Vector3(0, -0.15, 0.08)), [0, -1.05, 0.75], 0.45);

  B.part('Haptik-Aktuatoren', 'Voice-Coil-Aktuatoren für haptisches Feedback',
    pair(sx => at(B.mesh(cyl(0.15, 0.15, 0.26, 32), { color: COL.metal, base: 0.06 }), P(sx * 1.55, -0.16, -0.95), [Math.PI / 2, 0, 0])), [0, -0.55, 0.65], 0.5);

  B.part('Lautsprecher & Mikrofone', 'Mono-Lautsprecher, Mikrofon-Array',
    at(B.mesh(cyl(0.13, 0.13, 0.05, 32), { color: COL.metal }), new THREE.Vector3(0, -0.12, 0.36)), [0, -0.7, 1.45], 0.55);

  B.part('Untere Gehäuseschale', 'Griffschale mit Haptik-Halterungen',
    B.mesh(shellGeom(0.2, -0.3), { color: COL.shell, base: 0.03, rim: 0.7, edgeAngle: 35 }), [0, -2.0, 0], 0.05);

  B.root.position.z = -0.35;
  const wrap = new THREE.Group();
  wrap.add(B.root);
  wrap.scale.setScalar(1.05);
  return {
    key: 'controller', title: 'Game-Controller', subtitle: 'Next-Gen-Wireless-Gamepad (DualSense-Bauart)',
    group: wrap, parts: B.parts, shared: B.shared, dist: 6.4, distOpen: 11.5, el: 0.5,
    facts: ['Kabellos über Bluetooth', 'Haptisches Feedback statt klassischer Rumble-Motoren', 'Adaptive Trigger mit motorisiertem Widerstand', 'Integriertes Mikrofon & Lautsprecher'],
  };
}

/* ======================= Smartphone ======================= */
function frameGeom(w, h, r, t, depth) {
  const rr = (s, W, H, R) => {
    s.moveTo(-W / 2 + R, -H / 2); s.lineTo(W / 2 - R, -H / 2); s.quadraticCurveTo(W / 2, -H / 2, W / 2, -H / 2 + R);
    s.lineTo(W / 2, H / 2 - R); s.quadraticCurveTo(W / 2, H / 2, W / 2 - R, H / 2); s.lineTo(-W / 2 + R, H / 2);
    s.quadraticCurveTo(-W / 2, H / 2, -W / 2, H / 2 - R); s.lineTo(-W / 2, -H / 2 + R); s.quadraticCurveTo(-W / 2, -H / 2, -W / 2 + R, -H / 2);
  };
  const s = new THREE.Shape(); rr(s, w, h, r);
  const hole = new THREE.Path(); rr(hole, w - t * 2, h - t * 2, r - t);
  s.holes.push(hole);
  const g = new THREE.ExtrudeGeometry(s, { depth, bevelEnabled: false, curveSegments: 8 });
  g.translate(0, 0, -depth / 2);
  return g;
}
function buildPhone() {
  const B = new Builder();
  const at = (o, x, y, z) => { o.position.set(x, y, z); return o; };
  B.part('Frontglas', 'Gehärtetes Schutzglas', at(B.mesh(rbox(1.5, 3.1, 0.03, 0.015), { color: COL.glass, base: 0.05, edgeAngle: 50 }), 0, 0, 0.14), [0, 0, 2.1], 0.05);
  B.part('OLED-Display', 'Touch-Layer & Panel', at(B.mesh(rbox(1.42, 3.0, 0.03, 0.012), { color: 0x7f9cff, base: 0.12, edgeAngle: 50 }), 0, 0, 0.1), [0, 0, 1.45], 0.15);
  B.part('Rahmen', 'Aluminium-Mittelrahmen mit Antennen', B.mesh(frameGeom(1.52, 3.12, 0.22, 0.05, 0.24), { color: COL.metal, base: 0.06 }), [0, 0, 0.55], 0.2);
  B.part('Hauptplatine', 'SoC, RAM, Speicher, Modem',
    (() => { const g = new THREE.Group(); g.add(B.mesh(rbox(1.3, 1.0, 0.04, 0.02), { color: COL.board, base: 0.08 }));
      g.add(at(B.mesh(rbox(0.4, 0.4, 0.05, 0.01), { color: COL.board, base: 0.3 }), 0.1, 0.0, 0.03));
      g.add(at(B.mesh(rbox(0.2, 0.25, 0.04, 0.01), { color: COL.board, base: 0.2 }), -0.4, 0.2, 0.03));
      g.position.set(0, 0.9, 0); return g; })(), [0.2, 0.25, -0.5], 0.35);
  B.part('Akku', 'Li-Ionen-Zelle', at(B.mesh(rbox(1.25, 1.65, 0.08, 0.04), { color: COL.hot, base: 0.08 }), 0, -0.55, 0), [-0.1, -0.25, -1.05], 0.35);
  B.part('Kameramodul', 'Weitwinkel, Ultraweitwinkel, Tele',
    (() => { const g = new THREE.Group();
      for (const [x, y] of [[-0.12, 0.12], [0.12, 0.12], [-0.12, -0.12]]) g.add(at(B.mesh(cyl(0.1, 0.1, 0.08, 32).rotateX(Math.PI / 2), { base: 0.15 }), x, y, 0));
      g.position.set(-0.38, 1.2, -0.06); return g; })(), [0, 0.55, -1.6], 0.45);
  B.part('Lautsprecher & Taptic-Motor', '', at(B.mesh(rbox(0.9, 0.2, 0.08, 0.02), { color: COL.metal }), 0, -1.38, 0), [0, -0.8, -0.5], 0.4);
  B.part('Rückseite', 'Glas mit Kamera-Ausschnitt', at(B.mesh(rbox(1.5, 3.1, 0.03, 0.015), { color: COL.glass, base: 0.04, edgeAngle: 50 }), 0, 0, -0.14), [0, 0, -2.3], 0.05);
  const wrap = new THREE.Group();
  B.root.rotation.y = -0.75;
  B.root.position.y = 0.1;
  wrap.add(B.root);
  return { key: 'smartphone', title: 'Smartphone', subtitle: 'Modernes Flaggschiff-Smartphone', group: wrap, parts: B.parts, shared: B.shared, dist: 6.5, distOpen: 10, el: 0.25,
    facts: ['Sandwich-Bauweise: Glas – Display – Rahmen – Platine – Akku – Glas', 'Mehrfach-Kamerasystem', 'OLED-Display'] };
}

/* ======================= Drohne ======================= */
function buildDrone() {
  const B = new Builder();
  const at = (o, x, y, z, ry = 0) => { o.position.set(x, y, z); o.rotation.y = ry; return o; };
  const corners = [[1, 1], [-1, 1], [1, -1], [-1, -1]];
  B.part('Rumpf', 'Kohlefaser-Gehäuse', B.mesh(rbox(1.0, 0.32, 1.4, 0.12), { base: 0.04 }), [0, 0, 0], 0);
  B.part('Flugcontroller', 'IMU, GPS, Kompass, Barometer', at(B.mesh(rbox(0.55, 0.05, 0.55, 0.02), { color: COL.board, base: 0.2 }), 0, 0.05, 0), [0, 1.4, 0], 0.4);
  B.part('Akku', 'LiPo 4S', at(B.mesh(rbox(0.6, 0.24, 1.0, 0.05), { color: COL.hot, base: 0.08 }), 0, 0.3, -0.05), [0, 2.0, -0.2], 0.3);
  B.part('Ausleger', '', (() => { const g = new THREE.Group();
    for (const [x, z] of corners) g.add(at(B.mesh(rbox(1.5, 0.08, 0.13, 0.03)), x * 0.62, 0.02, z * 0.62, -Math.atan2(z, x)));
    return g; })(), [0, 0.5, 0], 0.2);
  const motors = new THREE.Group(), props = [];
  for (const [x, z] of corners) motors.add(at(B.mesh(cyl(0.14, 0.15, 0.2, 32), { color: COL.metal, base: 0.08 }), x * 1.18, 0.12, z * 1.18));
  B.part('Bürstenlose Motoren', '4× BLDC-Außenläufer', motors, [0, 0.9, 0], 0.35);
  const propG = new THREE.Group();
  for (const [x, z] of corners) {
    const p = new THREE.Group();
    p.add(B.mesh(rbox(1.1, 0.012, 0.1, 0.005), { base: 0.15, edgeOp: 0.4 }));
    p.position.set(x * 1.18, 0.26, z * 1.18);
    propG.add(p); props.push(p);
  }
  B.part('Propeller', 'Rotorblätter', propG, [0, 1.6, 0], 0.45, { spin: props });
  B.part('Kamera-Gimbal', '3-Achsen-Stabilisierung, 4K-Kamera', (() => { const g = new THREE.Group();
    g.add(B.mesh(new THREE.SphereGeometry(0.17, 24, 16), { base: 0.1 }));
    g.add(at(B.mesh(cyl(0.07, 0.07, 0.06, 24).rotateX(Math.PI / 2), { color: COL.glass, base: 0.3 }), 0, 0, 0.16));
    g.position.set(0, -0.3, 0.62); return g; })(), [0, -0.9, 0.9], 0.4);
  B.part('Landegestell', '', (() => { const g = new THREE.Group();
    for (const x of [-0.4, 0.4]) { g.add(at(B.mesh(rbox(0.06, 0.35, 0.06, 0.02)), x, -0.32, 0)); g.add(at(B.mesh(rbox(0.06, 0.05, 1.1, 0.02)), x, -0.5, 0)); }
    return g; })(), [0, -1.4, -0.2], 0.3);
  const wrap = new THREE.Group(); wrap.add(B.root); wrap.scale.setScalar(1.2); wrap.position.y = 0.35;
  return { key: 'drone', title: 'Quadcopter-Drohne', subtitle: 'Kameradrohne mit Gimbal', group: wrap, parts: B.parts, shared: B.shared, dist: 6.5, distOpen: 10.5, el: 0.5,
    facts: ['Vier gegenläufige Rotoren', 'GPS-gestützte Positionshaltung', 'Stabilisierte Kamera'] };
}

export const LIBRARY = [
  { key: 'controller', re: /(controller|kontroller|gamepad|joypad|ps5|ps 5|playstation|dualsense|spielkonsole|konsolen?controller)/, build: buildController },
  { key: 'smartphone', re: /(smartphone|handy|iphone|telefon|phone|mobiltelefon)/, build: buildPhone },
  { key: 'drone', re: /(drohne|drone|quadrocopter|quadcopter|multicopter)/, build: buildDrone },
];

export function findObject(text) {
  return LIBRARY.find(o => o.re.test(text)) || null;
}

/** Erstellt ein holografisches Bild-Panel (für beliebige Dinge aus Wikipedia) */
export function buildImageHologram(title, imgUrl) {
  const shared = sharedState();
  const group = new THREE.Group();
  const mat = new THREE.ShaderMaterial({
    uniforms: { uMap: { value: null }, uReveal: shared.reveal, uFade: shared.fade, uTime: { value: 0 }, uHas: { value: 0 } },
    vertexShader: `varying vec2 vUv; varying vec3 vW; void main(){ vUv=uv; vec4 w=modelMatrix*vec4(position,1.0); vW=w.xyz; gl_Position=projectionMatrix*viewMatrix*w; }`,
    fragmentShader: `uniform sampler2D uMap; uniform float uReveal,uFade,uTime,uHas; varying vec2 vUv; varying vec3 vW;
      void main(){ if(vW.y>uReveal) discard; vec3 c = uHas>0.5 ? texture2D(uMap,vUv).rgb : vec3(0.2);
        float l = dot(c, vec3(0.299,0.587,0.114));
        float scan = 0.75+0.25*sin(vUv.y*400.0 - uTime*6.0);
        float glitch = step(0.985, fract(sin(floor(vUv.y*60.0)+floor(uTime*8.0))*43758.5));
        vec2 d = abs(vUv-0.5)*2.0; float edge = 1.0 - smoothstep(0.85, 1.0, max(d.x,d.y));
        float border = smoothstep(0.97,0.99,max(d.x,d.y));
        vec3 col = vec3(0.25,0.8,1.0)*(0.15+pow(l,0.9)*1.4)*scan + border*vec3(0.6,0.95,1.0);
        float front = smoothstep(0.12,0.0,uReveal-vW.y);
        gl_FragColor = vec4(col + front, (0.85*edge + border + glitch*0.3)*uFade); }`,
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, side: THREE.DoubleSide,
  });
  const plane = new THREE.Mesh(new THREE.PlaneGeometry(3.2, 2.2), mat);
  plane.position.y = 0.1;
  group.add(plane);
  if (imgUrl) {
    new THREE.TextureLoader().setCrossOrigin('anonymous').load(imgUrl, tex => {
      const a = tex.image.width / tex.image.height;
      const h = 2.6, w = Math.min(4.2, h * a);
      plane.geometry.dispose();
      plane.geometry = new THREE.PlaneGeometry(w, w / a);
      mat.uniforms.uMap.value = tex; mat.uniforms.uHas.value = 1;
    });
  }
  return { key: 'image:' + title, title, subtitle: 'Holografische Projektion', group, parts: [], shared, dist: 6.2, distOpen: 6.2, el: 0.15, imageMat: mat, facts: [] };
}
