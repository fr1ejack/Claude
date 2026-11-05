import * as THREE from 'three';
import { U } from './util.js';
import { holoMat, lineMat, holoMesh, sharedState } from './materials.js';

/** Der "KI-Kern": rotierende Ringe, Ikosaeder, Partikelhülle. Pulsiert beim Sprechen/Zuhören. */
export class Core {
  constructor() {
    this.shared = sharedState();
    this.group = new THREE.Group();
    this.rings = [];
    const s = this.shared;

    const ringDefs = [
      { r: 1.15, t: 0.012, arc: Math.PI * 2, tilt: [1.2, 0, 0], speed: 0.4 },
      { r: 1.35, t: 0.025, arc: Math.PI * 1.3, tilt: [0.3, 0.6, 0], speed: -0.6 },
      { r: 1.55, t: 0.008, arc: Math.PI * 2, tilt: [-0.5, 0.2, 0.3], speed: 0.25 },
      { r: 1.75, t: 0.035, arc: Math.PI * 0.6, tilt: [1.57, 0, 0], speed: 0.9 },
      { r: 1.95, t: 0.01, arc: Math.PI * 1.7, tilt: [0.9, -0.8, 0], speed: -0.3 },
    ];
    for (const d of ringDefs) {
      const pivot = new THREE.Group();
      pivot.rotation.set(...d.tilt);
      const m = new THREE.Mesh(new THREE.TorusGeometry(d.r, d.t, 8, 160, d.arc), holoMat({ base: 0.32, rim: 0.4, side: THREE.DoubleSide, shared: s }));
      pivot.add(m);
      this.group.add(pivot);
      this.rings.push({ m, speed: d.speed });
    }
    // Tick-Ring (Segmente)
    const ticks = new THREE.BufferGeometry();
    const tp = [];
    for (let i = 0; i < 120; i++) {
      const a = (i / 120) * Math.PI * 2, r0 = 2.2, r1 = i % 10 === 0 ? 2.38 : 2.28;
      tp.push(Math.cos(a) * r0, Math.sin(a) * r0, 0, Math.cos(a) * r1, Math.sin(a) * r1, 0);
    }
    ticks.setAttribute('position', new THREE.Float32BufferAttribute(tp, 3));
    this.ticks = new THREE.LineSegments(ticks, lineMat({ opacity: 0.55, shared: s }));
    this.group.add(this.ticks);

    this.ico = holoMesh(new THREE.IcosahedronGeometry(0.62, 1), { base: 0.06, rim: 0.9, edgeOp: 0.9, edgeAngle: 1, shared: s });
    this.group.add(this.ico);
    this.inner = new THREE.Mesh(new THREE.SphereGeometry(0.26, 32, 16), holoMat({ color: 0xbff4ff, base: 0.55, rim: 0.3, shared: s }));
    this.group.add(this.inner);

    // Partikelhülle
    const n = 900, pos = new Float32Array(n * 3);
    for (let i = 0; i < n; i++) {
      const v = new THREE.Vector3().randomDirection().multiplyScalar(0.9 + Math.random() * 1.6);
      pos.set([v.x, v.y, v.z], i * 3);
    }
    const pg = new THREE.BufferGeometry();
    pg.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    this.particles = new THREE.Points(pg, new THREE.PointsMaterial({ color: 0x7fe3ff, size: 0.025, transparent: true, opacity: 0.6, blending: THREE.AdditiveBlending, depthWrite: false }));
    this.group.add(this.particles);
    this.level = 0;
  }
  update(dt, activity) {
    this.level += (activity - this.level) * Math.min(1, dt * 6);
    for (const r of this.rings) r.m.rotation.z += r.speed * dt * (1 + this.level * 2.5);
    this.ticks.rotation.z -= dt * 0.15;
    this.ico.rotation.x += dt * 0.3; this.ico.rotation.y += dt * 0.45;
    this.particles.rotation.y += dt * 0.05;
    const t = U.time.value;
    const pulse = 1 + Math.sin(t * (activity > 0 ? 14 : 2)) * (0.04 + this.level * 0.12);
    this.inner.scale.setScalar(pulse * (1 + this.level * 0.4));
    this.ico.scale.setScalar(1 + this.level * 0.15);
    this.particles.material.opacity = 0.6 * this.shared.fade.value;
  }
}

/** Projektor-Plattform unter den Hologrammen */
export class Projector {
  constructor() {
    this.shared = sharedState();
    this.group = new THREE.Group();
    this.group.position.y = -2.35;
    const s = this.shared;
    for (const [r0, r1, op] of [[0.4, 0.42, 0.9], [1.2, 1.215, 0.5], [2.0, 2.06, 0.8], [2.6, 2.61, 0.35], [3.2, 3.205, 0.25]]) {
      const m = new THREE.Mesh(new THREE.RingGeometry(r0, r1, 128), lineMatMesh(op, s));
      m.rotation.x = -Math.PI / 2;
      this.group.add(m);
    }
    // Segment-Ring
    this.seg = new THREE.Mesh(new THREE.RingGeometry(2.15, 2.3, 64, 1, 0, Math.PI * 1.4), lineMatMesh(0.35, s));
    this.seg.rotation.x = -Math.PI / 2;
    this.group.add(this.seg);
    // Lichtkegel
    const cone = new THREE.Mesh(new THREE.CylinderGeometry(3.0, 0.5, 4.8, 64, 1, true), holoMat({ base: 0.0, rim: 0.05, shared: s }));
    cone.position.y = 2.4;
    this.group.add(cone);
    // Bodenscheibe
    const disc = new THREE.Mesh(new THREE.CircleGeometry(0.4, 48), holoMat({ color: 0xbff4ff, base: 0.45, rim: 0, side: THREE.DoubleSide, shared: s }));
    disc.rotation.x = -Math.PI / 2;
    this.group.add(disc);
  }
  update(dt) { this.seg.rotation.z += dt * 0.4; }
}

function lineMatMesh(op, shared) {
  const m = holoMat({ color: 0x5fd8ff, base: op * 0.6, rim: 0, side: THREE.DoubleSide, shared });
  return m;
}
