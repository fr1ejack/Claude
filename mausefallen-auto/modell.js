// 3D-Modell des Mausefallen-Autos "Leichtlaeufer 50".
// Parametrisch, ohne eigene Abhaengigkeiten: THREE wird uebergeben,
// damit dieselbe Datei im Browser (index.html) und in Node (export.mjs) laeuft.
//
// Koordinaten wie im Bauplan: x nach vorn, y nach links, z nach oben, Masse in mm.
// Ursprung: Bodenpunkt unter der Hinterachse.

export const MASSE = {
  // Hinterachse / Hinterraeder
  radHintenD: 200, radHintenDicke: 6, spur: 88, // Radmitte y = +-44
  achseHintenD: 3, achseHintenL: 100,
  // Vorderrad
  radVornD: 100, radVornDicke: 3, radstand: 440,
  // Rahmen
  holmD: 4, holmAbstand: 42, holmZ: 93, holmVon: -10, holmBis: 455,
  // Falle
  falleL: 80, falleB: 46, falleH: 9, drehpunktX: 355, drehpunktZ: 110,
  // Hebel
  hebelL: 350, hebelD: 4,
  // Lager 683: 3 x 7 x 3 mm
  lagerD: 7, lagerB: 3,
  wickelR: 1.75,
};

const FARBEN = {
  carbon: 0x2b2d31, balsa: 0xe2c48f, depron: 0xdfe9f2, gummi: 0x9c2f2f,
  holz: 0xb07a45, stahl: 0xc9ccd1, sperrholz: 0xc89b62, schnur: 0xff7a1a,
  messing: 0xd8b04a,
};

export function hebelSpitze(phi, m = MASSE) {
  return {
    x: m.drehpunktX - m.hebelL * Math.cos(phi),
    z: m.drehpunktZ + m.hebelL * Math.sin(phi),
  };
}

export function schnurLaenge(phi, m = MASSE) {
  const t = hebelSpitze(phi, m);
  return Math.hypot(t.x, t.z - m.radHintenD / 2);
}

export function baueAuto(THREE, m = MASSE) {
  const mats = {};
  for (const [k, c] of Object.entries(FARBEN)) {
    mats[k] = new THREE.MeshStandardMaterial({
      color: c,
      roughness: k === 'stahl' || k === 'messing' ? 0.35 : 0.75,
      metalness: k === 'stahl' || k === 'messing' ? 0.8 : 0.0,
      side: THREE.DoubleSide,
    });
  }

  const auto = new THREE.Group();   // z-up Inhalt
  auto.name = 'Mausefallen-Auto';
  const teile = {};                  // name -> Object3D (fuer Beschriftungen)

  const box = (name, mat, x0, x1, y0, y1, z0, z1, parent = auto) => {
    const g = new THREE.BoxGeometry(x1 - x0, y1 - y0, z1 - z0);
    const mesh = new THREE.Mesh(g, mats[mat]);
    mesh.position.set((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2);
    mesh.name = name;
    parent.add(mesh);
    return mesh;
  };
  // Zylinder entlang einer Achse ('x' | 'y' | 'z')
  const zyl = (name, mat, r, len, achse, x, y, z, parent = auto, seg = 24) => {
    const g = new THREE.CylinderGeometry(r, r, len, seg);
    if (achse === 'x') g.rotateZ(Math.PI / 2);
    if (achse === 'z') g.rotateX(Math.PI / 2);
    const mesh = new THREE.Mesh(g, mats[mat]);
    mesh.position.set(x, y, z);
    mesh.name = name;
    parent.add(mesh);
    return mesh;
  };
  // Scheibe mit Loechern, Achse = y
  const scheibe = (name, mat, r, dicke, loecher, lochR, lochAbst, nabeR, y, parent) => {
    const s = new THREE.Shape();
    s.absarc(0, 0, r, 0, Math.PI * 2, false);
    const loch = (cx, cy, rr) => {
      const p = new THREE.Path();
      p.absarc(cx, cy, rr, 0, Math.PI * 2, true);
      s.holes.push(p);
    };
    loch(0, 0, nabeR);
    for (let i = 0; i < loecher; i++) {
      const a = (i / loecher) * Math.PI * 2;
      loch(Math.cos(a) * lochAbst, Math.sin(a) * lochAbst, lochR);
    }
    const g = new THREE.ExtrudeGeometry(s, { depth: dicke, bevelEnabled: false, curveSegments: 48 });
    g.translate(0, 0, -dicke / 2);
    g.rotateX(Math.PI / 2); // Extrusionsachse z -> y
    const mesh = new THREE.Mesh(g, mats[mat]);
    mesh.position.y = y;
    mesh.name = name;
    parent.add(mesh);
    return mesh;
  };
  const ring = (name, mat, rA, rI, breite, y, parent) => {
    const s = new THREE.Shape();
    s.absarc(0, 0, rA, 0, Math.PI * 2, false);
    const h = new THREE.Path();
    h.absarc(0, 0, rI, 0, Math.PI * 2, true);
    s.holes.push(h);
    const g = new THREE.ExtrudeGeometry(s, { depth: breite, bevelEnabled: false, curveSegments: 64 });
    g.translate(0, 0, -breite / 2);
    g.rotateX(Math.PI / 2);
    const mesh = new THREE.Mesh(g, mats[mat]);
    mesh.position.y = y;
    mesh.name = name;
    parent.add(mesh);
    return mesh;
  };

  const RH = m.radHintenD / 2, RV = m.radVornD / 2;
  const yHolm = m.holmAbstand / 2;
  const zH = m.holmZ;

  // ---------------------------------------------------------------- Rahmen
  for (const s of [1, -1]) {
    const h = zyl(`Holm ${s > 0 ? 'links' : 'rechts'}`, 'carbon', m.holmD / 2,
      m.holmBis - m.holmVon, 'x', (m.holmVon + m.holmBis) / 2, s * yHolm, zH);
    if (s > 0) teile.holm = h;
  }
  for (const x of [15, 160, 290]) {
    const q = box(`Querstrebe x=${x}`, 'balsa', x - 2.5, x + 2.5, -yHolm + 2, yHolm - 2, zH - 2.5, zH + 2.5);
    if (x === 160) teile.quer = q;
  }

  // ------------------------------------------------- Hinterachse + Lagerboecke
  const yPlatte = yHolm + 2 + 1.5;
  for (const s of [1, -1]) {
    const p = box('Lagerbock hinten', 'balsa', -15, 20, s * yPlatte - 1.5, s * yPlatte + 1.5, zH - 10, RH + 12);
    if (s > 0) teile.lagerbock = p;
    zyl('Verstaerkung 1 mm Sperrholz', 'sperrholz', 7, 1, 'y', 0, s * (yPlatte + 2), RH);
    zyl('Kugellager 683', 'stahl', m.lagerD / 2, m.lagerB + 0.2, 'y', 0, s * yPlatte, RH);
    // Schraegstrebe Balsa 3x3 vom Lagerbock nach vorn zum Holm
    const len = Math.hypot(45, RH + 10 - zH);
    const st = new THREE.Mesh(new THREE.BoxGeometry(len, 3, 3), mats.balsa);
    st.position.set(20 + 45 / 2 - 4, s * yPlatte, (RH + 8 + zH) / 2);
    st.rotation.y = Math.atan2(RH + 8 - zH, 45);
    st.name = 'Schraegstrebe';
    auto.add(st);
  }

  // Drehende Hinterachs-Gruppe
  const hinten = new THREE.Group();
  hinten.position.set(0, 0, RH);
  hinten.name = 'Hinterachse (dreht)';
  auto.add(hinten);
  teile.achse = zyl('Hinterachse Carbon 3 mm', 'carbon', m.achseHintenD / 2, m.achseHintenL, 'y', 0, 0, 0, hinten, 16);
  const yRad = m.spur / 2;
  for (const s of [1, -1]) {
    const rad = scheibe('Hinterrad Depron 6 mm', 'depron', RH, m.radHintenDicke, 5, 22, 58, 1.5, s * yRad, hinten);
    if (s > 0) teile.radHinten = rad;
    const lauf = ring('Laufflaeche Gummi', 'gummi', RH + 0.6, RH - 0.2, m.radHintenDicke, s * yRad, hinten);
    if (s > 0) teile.lauf = lauf;
    for (const d of [-1, 1]) {
      scheibe('Nabe Balsa 1,5 mm', 'balsa', 15, 1.5, 0, 0, 0, 1.5, s * yRad + d * (m.radHintenDicke / 2 + 0.75), hinten);
    }
  }
  // Wickelhaken: Draht quer durch die Achse
  const haken = zyl('Wickelhaken 0,5 mm Draht', 'messing', 0.3, 5, 'z', 0, 0, 2, hinten, 8);
  teile.haken = haken;
  // Speichen-Markierung, damit man die Drehung sieht
  for (const s of [1, -1]) {
    box('Markierung', 'gummi', 60, 95, s * (yRad + s * 3.2) - 0.2, s * (yRad + s * 3.2) + 0.2, -2, 2, hinten);
  }

  // ---------------------------------------------------------------- Vorderrad
  const xV = m.radstand;
  const yGabel = yHolm - 2 - 1.5;
  for (const s of [1, -1]) {
    const g = box('Gabel Balsa 3 mm', 'balsa', xV - 12, xV + 12, s * yGabel - 1.5, s * yGabel + 1.5, RV - 10, zH + 4);
    if (s > 0) teile.gabel = g;
    zyl('Distanzroehrchen', 'stahl', 2, yGabel - 1.5 - 1.6, 'y', xV, s * (1.6 + (yGabel - 1.5 - 1.6) / 2), RV, auto, 12)
      .material = new THREE.MeshStandardMaterial({ color: 0xf5f5f5, roughness: 0.6 });
  }
  zyl('Vorderachse Carbon 3 mm', 'carbon', 1.5, 2 * yGabel + 3, 'y', xV, 0, RV, auto, 12);
  const vorn = new THREE.Group();
  vorn.position.set(xV, 0, RV);
  vorn.name = 'Vorderrad (dreht)';
  auto.add(vorn);
  teile.radVorn = scheibe('Vorderrad Balsa 2x1,5 mm kreuzverleimt', 'balsa', RV, m.radVornDicke, 4, 12, 30, 3.5, 0, vorn);
  zyl('Kugellager 683 vorn', 'stahl', 3.5, 3.2, 'y', 0, 0, 0, vorn, 16);
  box('Markierung', 'gummi', 36, 48, 1.6, 1.9, -1.5, 1.5, vorn);

  // --------------------------------------------------------------- Mausefalle
  const xP = m.drehpunktX, zP = m.drehpunktZ;
  const zBasis = zH + m.holmD / 2;
  teile.falle = box('Mausefalle (Holzbrett gekuerzt)', 'holz',
    xP - m.falleL / 2, xP + m.falleL / 2, -m.falleB / 2, m.falleB / 2, zBasis, zBasis + m.falleH);
  // Federwindungen
  for (const s of [1, -1]) {
    const pts = [];
    const windungen = 6, rr = 4.5, breite = 9;
    for (let i = 0; i <= windungen * 24; i++) {
      const a = (i / 24) * Math.PI * 2;
      pts.push(new THREE.Vector3(xP + Math.cos(a) * rr, s * (6 + (i / (windungen * 24)) * breite), zP + Math.sin(a) * rr));
    }
    const f = new THREE.Mesh(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(pts), 200, 0.8, 6), mats.stahl);
    f.name = 'Feder';
    auto.add(f);
    if (s > 0) teile.feder = f;
  }
  // Klammern, die den Buegel halten
  for (const s of [1, -1]) box('Halteklammer', 'stahl', xP - 2, xP + 2, s * 18 - 1, s * 18 + 1, zBasis + m.falleH, zP + 1);

  // Schwenkende Gruppe: Buegel + Hebel. Drehung um y-Achse am Drehpunkt.
  const schwenk = new THREE.Group();
  schwenk.position.set(xP, 0, zP);
  schwenk.name = 'Buegel + Hebel (schwenkt)';
  auto.add(schwenk);
  // In der Gruppe zeigt +x' entlang des Hebels.
  const buegelL = 36, buegelB = 40;
  const drahtMat = mats.stahl;
  const draht = (a, b) => {
    const v = new THREE.Vector3().subVectors(b, a);
    const g = new THREE.CylinderGeometry(0.9, 0.9, v.length(), 8);
    const mesh = new THREE.Mesh(g, drahtMat);
    mesh.position.copy(a).add(b).multiplyScalar(0.5);
    mesh.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), v.clone().normalize());
    mesh.name = 'Schlagbuegel';
    schwenk.add(mesh);
  };
  const V = (x, y, z) => new THREE.Vector3(x, y, z);
  draht(V(0, buegelB / 2, 0), V(buegelL, buegelB / 2, 0));
  draht(V(0, -buegelB / 2, 0), V(buegelL, -buegelB / 2, 0));
  draht(V(buegelL, -buegelB / 2, 0), V(buegelL, buegelB / 2, 0));
  draht(V(0, -buegelB / 2, 0), V(0, buegelB / 2, 0));
  const hebel = zyl('Hebel Carbonrohr 4x3 mm', 'carbon', m.hebelD / 2, m.hebelL, 'x', m.hebelL / 2, 0, 3, schwenk, 16);
  teile.hebel = hebel;
  // Bindungen (Faden + Sekundenkleber) Hebel <-> Buegel
  for (const x of [2, buegelL]) zyl('Bindung', 'schnur', 2.6, 5, 'x', x, 0, 2, schwenk, 12);
  // Oese an der Hebelspitze
  const oese = new THREE.Mesh(new THREE.TorusGeometry(2.2, 0.5, 6, 16), mats.messing);
  oese.position.set(m.hebelL - 1, 0, 3);
  oese.name = 'Oese';
  schwenk.add(oese);
  teile.oese = oese;

  // ---------------------------------------------------------------- Schnur
  const schnurGeo = new THREE.BufferGeometry().setFromPoints([V(0, 0, 0), V(1, 0, 0)]);
  const schnur = new THREE.Line(schnurGeo, new THREE.LineBasicMaterial({ color: FARBEN.schnur }));
  schnur.name = 'Zugschnur';
  auto.add(schnur);
  // Wickel auf der Achse
  const wickel = zyl('Schnurwickel', 'schnur', 2.6, 8, 'y', 0, 0, 0, hinten, 16);
  teile.schnur = wickel;

  // ---------------------------------------------------------------- Zustand
  const s0 = schnurLaenge(0, m);
  const sMax = schnurLaenge(Math.PI, m) - s0;
  /** t: 0 = gespannt, 1 = abgelaufen. Gibt den gefahrenen Weg in m zurueck. */
  function setzeStellung(t, extraWeg = 0) {
    const phi = Math.PI * Math.min(Math.max(t, 0), 1);
    // Gruppe: lokales +x soll in Richtung (-cos phi, 0, sin phi) zeigen
    schwenk.rotation.set(0, 0, 0);
    schwenk.rotation.y = -(Math.PI - phi); // Drehung um y
    const abgezogen = schnurLaenge(phi, m) - s0;
    const rest = sMax - abgezogen;
    const weg = (abgezogen / m.wickelR) * RH + extraWeg; // mm
    const winkelH = weg / RH;
    hinten.rotation.y = winkelH;
    vorn.rotation.y = weg / RV;
    // Schnur von Oese zur Achsoberseite
    const tip = hebelSpitze(phi, m);
    const zielZ = RH + m.wickelR;
    const sichtbar = rest > 0.5;
    schnur.visible = sichtbar;
    wickel.visible = sichtbar;
    wickel.scale.set(1, Math.max(rest / sMax, 0.05), 1);
    const pos = schnur.geometry.attributes.position;
    pos.setXYZ(0, tip.x, 0, tip.z);
    pos.setXYZ(1, 0, 0, zielZ);
    pos.needsUpdate = true;
    schnur.geometry.computeBoundingSphere();
    return weg / 1000;
  }
  setzeStellung(0);

  // In y-up Welt drehen: z-up -> y-up
  const welt = new THREE.Group();
  welt.add(auto);
  auto.rotation.x = -Math.PI / 2;
  return { welt, auto, teile, setzeStellung, schnurMax: sMax };
}
