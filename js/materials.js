import * as THREE from 'three';
import { U } from './util.js';

/*
 * Hologramm-Materialien.
 * uReveal: alles oberhalb dieser Welt-Y-Höhe wird verworfen -> "Materialisieren" von unten nach oben.
 * uFade:   globale Deckkraft.
 */
const VS = /* glsl */`
  varying vec3 vN; varying vec3 vW;
  void main() {
    vec4 w = modelMatrix * vec4(position, 1.0);
    vW = w.xyz;
    vN = normalize(mat3(modelMatrix) * normal);
    gl_Position = projectionMatrix * viewMatrix * w;
  }`;

const FS = /* glsl */`
  uniform vec3 uColor; uniform float uBase, uRim, uReveal, uFade, uTime;
  varying vec3 vN; varying vec3 vW;
  void main() {
    if (vW.y > uReveal) discard;
    vec3 V = normalize(cameraPosition - vW);
    float f = pow(1.0 - abs(dot(normalize(vN), V)), 2.2);
    float lines = 0.72 + 0.28 * sin(vW.y * 95.0 - uTime * 3.0);
    float front = smoothstep(0.14, 0.0, uReveal - vW.y);
    float sweep = smoothstep(0.07, 0.0, abs(vW.y - (mod(uTime * 0.8, 7.0) - 3.0)));
    float a = (uBase + f * uRim) * lines + front * 0.7 + sweep * 0.12;
    gl_FragColor = vec4(uColor * (1.0 + front * 1.5), a * uFade);
  }`;

const LVS = /* glsl */`
  varying vec3 vW;
  void main() {
    vec4 w = modelMatrix * vec4(position, 1.0);
    vW = w.xyz;
    gl_Position = projectionMatrix * viewMatrix * w;
  }`;
const LFS = /* glsl */`
  uniform vec3 uColor; uniform float uOp, uReveal, uFade, uTime;
  varying vec3 vW;
  void main() {
    if (vW.y > uReveal) discard;
    float front = smoothstep(0.14, 0.0, uReveal - vW.y);
    float flick = 0.85 + 0.15 * sin(uTime * 7.0 + vW.y * 20.0);
    gl_FragColor = vec4(uColor * (1.0 + front * 2.0), (uOp * flick + front) * uFade);
  }`;

export function sharedState() {
  return { reveal: { value: 100 }, fade: { value: 1 } };
}

export function holoMat({ color = 0x5fd8ff, base = 0.03, rim = 0.42, side = THREE.FrontSide, shared = sharedState() } = {}) {
  return new THREE.ShaderMaterial({
    uniforms: {
      uTime: U.time, uColor: { value: new THREE.Color(color) }, uBase: { value: base }, uRim: { value: rim },
      uReveal: shared.reveal, uFade: shared.fade,
    },
    vertexShader: VS, fragmentShader: FS,
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, side,
  });
}

export function lineMat({ color = 0x9feeff, opacity = 0.42, shared = sharedState() } = {}) {
  return new THREE.ShaderMaterial({
    uniforms: { uTime: U.time, uColor: { value: new THREE.Color(color) }, uOp: { value: opacity }, uReveal: shared.reveal, uFade: shared.fade },
    vertexShader: LVS, fragmentShader: LFS,
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
  });
}

/** Mesh + leuchtende Kanten als Gruppe */
export function holoMesh(geom, { color, base, rim, edgeColor, edgeOp = 0.42, edgeAngle = 28, edges = true, shared } = {}) {
  const g = new THREE.Group();
  g.add(new THREE.Mesh(geom, holoMat({ color, base, rim, shared })));
  if (edges) {
    const e = new THREE.LineSegments(new THREE.EdgesGeometry(geom, edgeAngle), lineMat({ color: edgeColor ?? color ?? 0x9feeff, opacity: edgeOp, shared }));
    g.add(e);
  }
  return g;
}
