"""Erzeugt realistischere Menschen-Skins (64x64, Spieler-Layout) für Zivilisten und Soldaten."""
import os, random
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src/main/resources/assets/hiveinvasion/textures/entity/human")
os.makedirs(OUT, exist_ok=True)

def cl(v): return max(0, min(255, int(v)))
def sh(c, f): return (cl(c[0] * f), cl(c[1] * f), cl(c[2] * f), 255)

# Flächen je Körperteil: (u, v, w, h, d) im Standard-Layout; Overlay = zweite Schicht
PARTS = {
    "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
    "rarm": (40, 16, 4, 12, 4), "rsleeve": (40, 32, 4, 12, 4),
    "larm": (32, 48, 4, 12, 4), "lsleeve": (48, 48, 4, 12, 4),
    "rleg": (0, 16, 4, 12, 4), "rpants": (0, 32, 4, 12, 4),
    "lleg": (16, 48, 4, 12, 4), "lpants": (0, 48, 4, 12, 4),
}

def faces(part):
    u, v, w, h, d = PARTS[part]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}

class Skin:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); self.px = self.img.load(); self.r = random.Random(seed)

    def fill(self, part, color, noise=6, faces_=None, rows=None, shade=True, alpha=255):
        for f, (x, y, w, h) in faces(part).items():
            if faces_ and f not in faces_: continue
            light = {"top": 1.12, "bottom": 0.7, "front": 1.0, "back": 0.85, "left": 0.92, "right": 0.92}[f] if shade else 1
            for j in range(h):
                if rows and not (rows[0] <= j < rows[1]) and f not in ("top", "bottom"): continue
                for i in range(w):
                    n = self.r.randint(-noise, noise)
                    grad = 1.04 - 0.1 * j / max(1, h - 1) if f not in ("top", "bottom") else 1
                    c = sh((color[0] + n, color[1] + n, color[2] + n), light * grad)
                    self.px[x + i, y + j] = c[:3] + (alpha,)

    def put(self, part, face, i, j, color):
        x, y, w, h = faces(part)[face]
        if 0 <= i < w and 0 <= j < h: self.px[x + i, y + j] = tuple(color[:3]) + (255,)

    def camo(self, part, cols, faces_=None, rows=None):
        for f, (x, y, w, h) in faces(part).items():
            if faces_ and f not in faces_: continue
            for j in range(h):
                if rows and not (rows[0] <= j < rows[1]) and f not in ("top", "bottom"): continue
                for i in range(w):
                    k = ((i * 7 + j * 3 + self.r.randint(0, 3)) // 3 + (j // 2)) % len(cols)
                    self.px[x + i, y + j] = sh(cols[k], 1.0 if f != "bottom" else 0.7)

    def face(self, skin, eye, hair, beard=False, female=False):
        p = "head"
        self.fill(p, skin, 4)
        # Haare oben/hinten/seitlich
        self.fill(p, hair, 6, faces_=("top", "back"))
        for f in ("left", "right"):
            for j in range(3 if not female else 7):
                for i in range(8): self.put(p, f, i, j, sh(hair, 0.95 + self.r.random() * 0.1))
        for i in range(8):
            for j in range(2 if not female else 2): self.put(p, "front", i, j, sh(hair, 1.0))
        if female:
            for j in range(2, 8): self.put(p, "front", 0, j, hair); self.put(p, "front", 7, j, hair)
            for j in range(8):
                for i in range(8): self.put(p, "back", i, j, sh(hair, 0.9))
        # Augen
        white = (235, 235, 230)
        for x0 in (1, 5):
            self.put(p, "front", x0, 4, white); self.put(p, "front", x0 + 1, 4, eye)
        if True:  # Spiegel: rechtes Auge Pupille innen
            self.put(p, "front", 5, 4, eye); self.put(p, "front", 6, 4, white)
        # Brauen
        for x0 in (1, 2, 5, 6): self.put(p, "front", x0, 3, sh(hair, 0.8))
        # Nase, Mund
        self.put(p, "front", 3, 5, sh(skin, 0.88)); self.put(p, "front", 4, 5, sh(skin, 0.82))
        for x0 in (3, 4): self.put(p, "front", x0, 6, sh(skin, 0.7) if not female else (170, 80, 90))
        self.put(p, "front", 2, 6, sh(skin, 0.9)); self.put(p, "front", 5, 6, sh(skin, 0.9))
        if beard:
            for i in range(1, 7):
                self.put(p, "front", i, 7, sh(hair, 0.9))
            for i in (1, 2, 5, 6): self.put(p, "front", i, 6, sh(hair, 0.9))
        # Kinn / Hals Schatten
        for i in range(8): self.put(p, "bottom", i % 8, i % 8, sh(skin, 0.75))

    def hair_volume(self, hair, female=False):
        # Haar-Overlay für Volumen
        for f in ("top",):
            x, y, w, h = faces("hat")[f]
            for j in range(h):
                for i in range(w): self.px[x + i, y + j] = sh(hair, 0.95 + self.r.random() * 0.15)
        for f in ("back", "left", "right"):
            x, y, w, h = faces("hat")[f]
            for j in range(2 if not female else 7):
                for i in range(w): self.px[x + i, y + j] = sh(hair, 0.9 + self.r.random() * 0.15)
        x, y, w, h = faces("hat")["front"]
        for i in range(w): self.px[x + i, y] = sh(hair, 1.05)

    def save(self, name): self.img.save(os.path.join(OUT, name + ".png"))


SKIN_TONES = [(232, 190, 160), (214, 168, 132), (186, 136, 100), (150, 100, 70), (110, 72, 50), (240, 205, 180)]
HAIR = [(40, 28, 20), (90, 60, 35), (160, 120, 70), (20, 18, 18), (120, 40, 25), (190, 170, 120), (70, 70, 70)]
EYES = [(60, 90, 140), (70, 50, 30), (60, 110, 70), (40, 30, 20)]
SHIRTS = [(170, 40, 40), (40, 80, 150), (230, 230, 225), (60, 120, 70), (200, 160, 50), (90, 60, 110), (40, 40, 45), (210, 120, 60)]
PANTS = [(40, 55, 90), (30, 30, 35), (90, 80, 60), (60, 60, 65), (110, 90, 70)]
SHOES = [(30, 25, 20), (220, 220, 220), (90, 50, 30), (40, 40, 40)]

def civilian(i):
    r = random.Random(1000 + i)
    s = Skin(2000 + i)
    female = i % 2 == 1
    skin, hair, eye = r.choice(SKIN_TONES), r.choice(HAIR), r.choice(EYES)
    shirt, pants, shoe = r.choice(SHIRTS), r.choice(PANTS), r.choice(SHOES)
    s.face(skin, eye, hair, beard=(not female and r.random() < 0.35), female=female)
    s.hair_volume(hair, female)
    s.fill("body", shirt, 7)
    for f in ("front",):
        for j in range(12):
            s.put("body", f, 3, j, sh(shirt, 0.9)); s.put("body", f, 4, j, sh(shirt, 0.9))
    s.put("body", "front", 3, 0, skin); s.put("body", "front", 4, 0, skin); s.put("body", "front", 3, 1, skin)
    jacket = r.random() < 0.45
    for arm in ("rarm", "larm"):
        s.fill(arm, skin, 4)
        s.fill(arm, shirt if not jacket else sh(r.choice(PANTS), 1.2), 6, rows=(0, 4 if not jacket else 10))
    if jacket:
        jc = sh(r.choice([(60, 50, 40), (30, 40, 60), (80, 30, 30), (50, 60, 50)]), 1.0)
        s.fill("jacket", jc, 6, faces_=("left", "right", "back", "top"))
        x, y, w, h = faces("jacket")["front"]
        for j in range(h):
            for ii in (0, 1, 6, 7): s.px[x + ii, y + j] = sh(jc, 1.0)
        for sl in ("rsleeve", "lsleeve"): s.fill(sl, jc, 6, rows=(0, 10))
    # Gürtel
    for f in ("front", "left", "right", "back"):
        x, y, w, h = faces("body")[f]
        for ii in range(w): s.px[x + ii, y + 11] = (40, 30, 25, 255)
    for leg in ("rleg", "lleg"):
        s.fill(leg, pants, 6)
        s.fill(leg, shoe, 4, rows=(10, 12)); s.fill(leg, shoe, 4, faces_=("bottom",))
    s.save(f"civilian_{i}")

def soldier(i):
    r = random.Random(5000 + i)
    s = Skin(6000 + i)
    skin, eye = SKIN_TONES[i % len(SKIN_TONES)], r.choice(EYES)
    camo = [(86, 96, 62), (62, 72, 46), (110, 104, 74), (48, 52, 38)]
    s.face(skin, eye, (40, 30, 22), beard=False)
    # Helm (Overlay) mit Riemen
    helm = (78, 88, 58)
    for f in ("top", "left", "right", "back", "front"):
        x, y, w, h = faces("hat")[f]
        rows = h if f in ("top",) else (4 if f == "front" else 6)
        for j in range(rows):
            for ii in range(w): s.px[x + ii, y + j] = sh(helm, 0.95 + r.random() * 0.12 - (0.15 if j == rows - 1 else 0))
    x, y, w, h = faces("hat")["front"]
    for ii in range(1, 7): s.px[x + ii, y + 3] = (30, 32, 26, 255)      # Helmkante
    for j in range(4, 8): s.px[x + 0, y + j] = (40, 35, 25, 255); s.px[x + 7, y + j] = (40, 35, 25, 255)  # Kinnriemen
    # Uniform
    s.camo("body", camo)
    for arm in ("rarm", "larm"):
        s.camo(arm, camo); s.fill(arm, (40, 40, 38), 3, rows=(10, 12)); s.fill(arm, (40, 40, 38), 3, faces_=("bottom",))  # Handschuhe
    for leg in ("rleg", "lleg"):
        s.camo(leg, camo); s.fill(leg, (35, 30, 25), 4, rows=(8, 12)); s.fill(leg, (35, 30, 25), 4, faces_=("bottom",))  # Stiefel
    for leg in ("rpants", "lpants"):   # Knieschoner
        x, y, w, h = faces(leg)["front"]
        for j in (5, 6):
            for ii in range(w): s.px[x + ii, y + j] = (45, 48, 40, 255)
    # Schutzweste mit Taschen (Overlay)
    vest = (64, 70, 52)
    s.fill("jacket", vest, 5, rows=(0, 10))
    x, y, w, h = faces("jacket")["front"]
    for (px_, py_) in ((1, 5), (3, 5), (5, 5)):
        for dx in range(2):
            for dy in range(3): s.px[x + px_ + dx, y + py_ + dy] = sh(vest, 0.8 if dy == 0 else 0.92)
    for ii in range(w): s.px[x + ii, y + 0] = sh(vest, 1.15)
    s.px[x + 6, y + 2] = (200, 200, 200, 255)       # Abzeichen
    # Schulterflagge
    xs, ys, ws, hs = faces("rsleeve")["right"]
    s.px[xs + 1, ys + 1] = (220, 220, 220, 255); s.px[xs + 2, ys + 1] = (200, 40, 40, 255)
    s.save(f"soldier_{i}")

for i in range(10): civilian(i)
for i in range(4): soldier(i)
print("Skins erzeugt")
