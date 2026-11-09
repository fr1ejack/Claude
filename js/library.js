import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { clone as cloneSkinned } from 'three/addons/utils/SkeletonUtils.js';
import { CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js';
import { U, IS_ARTIFACT } from './util.js';
import { lineMat, sharedState } from './materials.js';

/*
 * Echte 3D-Modelle (frei lizenziert, siehe data/models/CREDITS.md), mitgeliefert in data/models/.
 * Texturen wurden entfernt – Jarvis zeigt nur die Geometrie als Hologramm. Tiere sind animiert.
 */
export const MODELS = [
  // Tiere
  { key: 'horse', re: /\b(pferd|pferde|hengst|stute|horse)\b/, title: 'Pferd', subtitle: 'Galoppierendes Pferd', kind: 'tier', anim: 0 },
  { key: 'flamingo', re: /flamingo/, title: 'Flamingo', subtitle: 'Fliegender Flamingo', kind: 'tier', anim: 0 },
  { key: 'parrot', re: /(papagei|parrot|ara\b)/, title: 'Papagei', subtitle: 'Fliegender Papagei', kind: 'tier', anim: 0 },
  { key: 'stork', re: /(storch|stork|vogel|vögel)/, title: 'Storch', subtitle: 'Fliegender Storch', kind: 'tier', anim: 0 },
  { key: 'fox', re: /\b(fuchs|füchse|fox)\b/, title: 'Fuchs', subtitle: 'Rotfuchs', kind: 'tier', anim: 'Walk' },
  { key: 'fish', re: /(fisch|barramundi|barsch)/, title: 'Barramundi-Fisch', subtitle: 'Riesenbarsch', kind: 'tier' },
  { key: 'mosquito', re: /(mücke|muecke|moskito|mosquito|insekt|bernstein)/, title: 'Mücke in Bernstein', subtitle: 'Fossiles Insekt', kind: 'tier' },
  // Gegenstände
  { key: 'robot', re: /(roboter|robot)/, title: 'Roboter', subtitle: 'Animierter Roboter', kind: 'objekt', anim: 'Dance' },
  { key: 'nefertiti', re: /(nofretete|nefertiti|büste|buste)/, title: 'Büste der Nofretete', subtitle: '3D-Scan, Ägypten um 1340 v. Chr.', kind: 'objekt' },
  { key: 'camera', re: /(kamera|camera|fotoapparat)/, title: 'Antike Kamera', subtitle: 'Balgenkamera auf Stativ', kind: 'objekt' },
  { key: 'avocado', re: /avocado/, title: 'Avocado', subtitle: 'Frucht', kind: 'objekt' },
  { key: 'boombox', re: /(boombox|ghettoblaster|radio|kassettenrekorder)/, title: 'Boombox', subtitle: 'Tragbares Radio', kind: 'objekt' },
  { key: 'carconcept', re: /\b(auto|autos|sportwagen|wagen|pkw|car|fahrzeug)\b/, title: 'Konzeptauto', subtitle: 'Sportwagen-Studie', kind: 'objekt' },
  { key: 'truck', re: /(lkw|lastwagen|laster|truck|milchwagen)/, title: 'Lieferwagen', subtitle: 'Milch-Lastwagen', kind: 'objekt', anim: 0 },
  { key: 'fridge', re: /(kühlschrank|kuehlschrank|kühltruhe|fridge)/, title: 'Kühlschrank', subtitle: 'Gewerbe-Kühlschrank', kind: 'objekt', anim: 0 },
  { key: 'lantern', re: /(laterne|lantern|straßenlaterne)/, title: 'Laterne', subtitle: 'Alte Straßenlaterne', kind: 'objekt' },
  { key: 'chair', re: /(stuhl|sessel|chair)/, title: 'Stuhl', subtitle: 'Gepolsterter Stuhl', kind: 'objekt' },
  { key: 'sunglasses', re: /(sonnenbrille|brille|sunglasses)/, title: 'Sonnenbrille', subtitle: 'Brille mit Gestell', kind: 'objekt' },
  { key: 'toycar', re: /(spielzeugauto|spielzeug|toy ?car)/, title: 'Spielzeugauto', subtitle: 'Aufzieh-Spielzeugauto', kind: 'objekt' },
  { key: 'cone', re: /(pylone|leitkegel|verkehrshütchen|hütchen|kegel|traffic cone)/, title: 'Leitkegel', subtitle: 'Verkehrshütchen', kind: 'objekt' },
  { key: 'bottle', re: /(trinkflasche|wasserflasche|flasche|bottle)/, title: 'Trinkflasche', subtitle: 'Wasserflasche', kind: 'objekt' },
  { key: 'chess', re: /(schach|chess|schachbrett|schachfigur)/, title: 'Schachspiel', subtitle: 'Brett mit allen Figuren', kind: 'objekt' },
  { key: 'vase', re: /(vase|blumen|blume)/, title: 'Blumenvase', subtitle: 'Glasvase mit Blumen', kind: 'objekt' },
  { key: 'lamp', re: /(lampe|leuchte|lamp)/, title: 'Lampe', subtitle: 'Designerlampe', kind: 'objekt' },
  { key: 'shoe', re: /(schuh|sneaker|turnschuh|shoe)/, title: 'Sneaker', subtitle: 'Sportschuh', kind: 'objekt' },
  { key: 'sofa', re: /(sofa|couch)/, title: 'Sofa', subtitle: 'Samtsofa', kind: 'objekt' },
  { key: 'skull', re: /(schädel|schaedel|totenkopf|skull)/, title: 'Schädel', subtitle: 'Menschlicher Schädel', kind: 'objekt' },
  { key: 'coffeemat', re: /(kaffeemaschine|kaffeeautomat|espressomaschine|coffee)/, title: 'Kaffeemaschine', subtitle: 'Kaffeevollautomat', kind: 'objekt' },
];

export function findModel(text) { return MODELS.find(m => m.re.test(text)) || null; }

/* ---------- Hologramm-Material mit Unterstützung für Skelett- und Morph-Animation ---------- */
const VS = `
  #include <common>
  #include <morphtarget_pars_vertex>
  #include <skinning_pars_vertex>
  varying vec3 vN; varying vec3 vW;
  void main() {
    #include <beginnormal_vertex>
    #include <morphnormal_vertex>
    #include <skinbase_vertex>
    #include <skinnormal_vertex>
    #include <begin_vertex>
    #include <morphtarget_vertex>
    #include <skinning_vertex>
    vec4 w = modelMatrix * vec4(transformed, 1.0);
    vW = w.xyz; vec3 nn = mat3(modelMatrix) * objectNormal; float ln = length(nn); vN = ln > 1e-6 ? nn / ln : vec3(0.0, 1.0, 0.0);
    gl_Position = projectionMatrix * viewMatrix * w;
  }`;
const FS = `
  uniform vec3 uColor; uniform float uReveal, uFade, uTime;
  varying vec3 vN; varying vec3 vW;
  void main() {
    if (vW.y > uReveal) discard;
    vec3 V = normalize(cameraPosition - vW);
    float f = pow(clamp(1.0 - abs(dot(vN, V)), 0.0, 1.0), 2.0);
    float lines = 0.7 + 0.3 * sin(vW.y * 110.0 - uTime * 3.0);
    float front = smoothstep(0.14, 0.0, uReveal - vW.y);
    float a = (0.04 + f * 0.55) * lines + front * 0.8;
    gl_FragColor = vec4(uColor * (1.0 + front * 1.5), a * uFade);
  }`;
function modelMat(shared, color) {
  return new THREE.ShaderMaterial({
    uniforms: { uColor: { value: new THREE.Color(color) }, uReveal: shared.reveal, uFade: shared.fade, uTime: U.time },
    vertexShader: VS, fragmentShader: FS, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, side: THREE.DoubleSide,
  });
}

const loader = new GLTFLoader();
const cache = new Map();
// (claude.ai-Artifacts liefern keine .glb-Dateien aus – dort liegen dieselben Bytes unter .glb.wasm)
function loadGLB(key) {
  if (!cache.has(key)) cache.set(key, loader.loadAsync(new URL(`../data/models/${key}${IS_ARTIFACT ? '.glb.wasm' : '.glb'}`, import.meta.url).href));
  return cache.get(key);
}

const PALETTE = [0x5fd8ff, 0x7fe9ff, 0x3fa6ff, 0x9fe9ff, 0x42ffd5, 0xffc36b];
const GENERIC = /^(mesh|object|node|cube|cylinder|sphere|plane|polysurface|group|default|material|untitled|geo|primitive|\d)/i;

/** Lädt ein Katalog-Modell und baut daraus ein Hologramm-Objekt für die Szene */
export async function buildModel(entry) {
  const gltf = await loadGLB(entry.key);
  const shared = sharedState();
  const scene = cloneSkinned(gltf.scene);   // Kopie, damit das Modell mehrfach gezeigt werden kann
  const animated = gltf.animations.length > 0;
  let rigged = false;
  scene.traverse(o => { if (o.isSkinnedMesh || o.morphTargetInfluences) rigged = true; });

  // Normalisieren: auf Anzeigegröße skalieren und auf den Projektor stellen
  scene.updateMatrixWorld(true);
  const box = new THREE.Box3().setFromObject(scene);
  const size = box.getSize(new THREE.Vector3()), c = box.getCenter(new THREE.Vector3());
  const s = 3.6 / Math.max(size.x, size.y * 1.15, size.z, 1e-6);
  const root = new THREE.Group();
  root.add(scene);
  scene.position.copy(c).multiplyScalar(-1);
  root.scale.setScalar(s);
  const wrap = new THREE.Group(); wrap.add(root); wrap.position.y = 0.15;

  // Materialien ersetzen, Kanten ergänzen
  let i = 0;
  const meshes = [];
  scene.traverse(o => {
    if (!o.isMesh) return;
    if (!o.geometry.attributes.normal) o.geometry.computeVertexNormals();   // manche Modelle haben keine Normalen
    o.material = modelMat(shared, PALETTE[i++ % 3]);
    o.frustumCulled = false;
    meshes.push(o);
    if (!o.isSkinnedMesh && !o.morphTargetInfluences && o.geometry.index && o.geometry.index.count < 150000) {
      try { o.add(new THREE.LineSegments(new THREE.EdgesGeometry(o.geometry, 32), lineMat({ opacity: 0.3, shared }))); } catch { /* */ }
    }
  });

  // Zerlegbare Bauteile (nur bei mehrteiligen, nicht animierten Modellen)
  const parts = [];
  if (!rigged && meshes.length >= 2) {
    wrap.updateMatrixWorld(true);
    const sized = meshes.map(m => { const b = new THREE.Box3().setFromObject(m); return { m, b, v: b.getSize(new THREE.Vector3()).length() }; }).sort((a, b) => b.v - a.v);
    const ctr = new THREE.Box3().setFromObject(wrap).getCenter(new THREE.Vector3());
    sized.slice(0, 24).forEach((x, idx) => {
      const mc = x.b.getCenter(new THREE.Vector3());
      const dir = mc.clone().sub(ctr); if (dir.lengthSq() < 1e-8) dir.set(0, 1, 0);
      dir.normalize().add(new THREE.Vector3(0, 0.35, 0)).normalize();
      const world = dir.multiplyScalar(idx === 0 ? 0.3 : 1.4);          // gewünschter Weg in der Anzeige
      const holder = x.m;
      const inv = new THREE.Matrix3().setFromMatrix4(new THREE.Matrix4().copy(holder.parent.matrixWorld).invert());
      const off = world.applyMatrix3(inv);                                // in lokale Koordinaten des Elternknotens
      const raw = (holder.name || holder.parent?.name || '').replace(/[_.\-]+/g, ' ').trim();
      const div = document.createElement('div');
      div.className = 'part-label';
      div.textContent = raw && !GENERIC.test(raw) ? raw.slice(0, 28) : '';
      const label = new CSS2DObject(div);
      label.position.copy(holder.worldToLocal(mc.clone()));
      if (!div.textContent || idx >= 8) label.visible = false;
      holder.add(label);
      parts.push({ name: div.textContent || `Bauteil ${idx + 1}`, info: '', obj: holder, label, off, delay: Math.min(0.5, idx * 0.03) });
    });
  }

  let mixer = null;
  if (animated) {
    mixer = new THREE.AnimationMixer(scene);
    const clip = typeof entry.anim === 'string' ? (gltf.animations.find(a => a.name.toLowerCase().includes(entry.anim.toLowerCase())) || gltf.animations[0]) : gltf.animations[entry.anim || 0];
    if (clip) mixer.clipAction(clip).play();
  }
  return {
    key: 'model:' + entry.key, title: entry.title, subtitle: entry.subtitle, kind: entry.kind, group: wrap, parts, shared,
    dist: 6.4, distOpen: 9.5, el: 0.35, facts: [], mixer, model: true,
  };
}
