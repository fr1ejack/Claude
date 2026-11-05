// Erzeugt modell/mausefallen_auto.stl und modell/mausefallen_auto.glb
// aus modell.js.   Aufruf:  npm install && npm run export
import * as THREE from 'three';
import { STLExporter } from 'three/examples/jsm/exporters/STLExporter.js';
import { GLTFExporter } from 'three/examples/jsm/exporters/GLTFExporter.js';
import { writeFileSync, mkdirSync } from 'node:fs';
import { baueAuto } from './modell.js';

// Minimaler FileReader-Ersatz fuer Node (GLTFExporter braucht ihn fuer GLB)
globalThis.FileReader ??= class {
  readAsArrayBuffer(blob) {
    blob.arrayBuffer().then((b) => { this.result = b; this.onloadend?.(); });
  }
};

const { welt, auto } = baueAuto(THREE);
auto.traverse((o) => { if (o.isLine) o.visible = false; }); // Linien nicht exportieren
welt.updateMatrixWorld(true);
mkdirSync('modell', { recursive: true });

const stl = new STLExporter().parse(welt, { binary: true });
writeFileSync('modell/mausefallen_auto.stl', Buffer.from(stl.buffer));

const glb = await new Promise((res, rej) =>
  new GLTFExporter().parse(welt, res, rej, { binary: true, onlyVisible: true }));
writeFileSync('modell/mausefallen_auto.glb', Buffer.from(glb));
console.log('STL + GLB geschrieben nach modell/');
