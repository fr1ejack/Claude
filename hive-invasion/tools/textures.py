"""Block-, Item-, Partikel- und Effekt-Texturen für Hive Invasion.  Aufruf: python3 tools/textures.py"""
import os, random, math
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/hiveinvasion/")
rnd = random.Random(42)

def save(img, path):
    os.makedirs(os.path.dirname(A + path), exist_ok=True); img.save(A + path)

def cl(v): return max(0, min(255, int(v)))
def shade(c, f): return tuple(cl(x * f) for x in c[:3]) + ((c[3],) if len(c) > 3 else (255,))

# ---------- Brutkapsel: lederartige Haut mit Adern und Lappen
def pod_side():
    im = Image.new("RGBA", (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            base = (96, 102, 64)
            f = 0.75 + 0.35 * math.sin((x / 15) * math.pi)       # rund wirkend
            f *= 1.1 - y / 40
            c = shade(base, f + rnd.uniform(-0.06, 0.06)); px[x, y] = c
    d = ImageDraw.Draw(im)
    for x0 in (3, 8, 12):   # Adern
        pts = [(x0 + rnd.randint(-1, 1), y) for y in range(0, 16, 3)]
        d.line(pts, fill=(70, 40, 40, 255))
    for x in range(16): px[x, 15] = shade(px[x, 15], 0.6)
    return im

def pod_top(opened):
    im = Image.new("RGBA", (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            px[x, y] = shade((104, 110, 70), 1.15 - r / 14 + rnd.uniform(-0.05, 0.05))
    d = ImageDraw.Draw(im)
    if opened:
        d.ellipse([3, 3, 12, 12], fill=(40, 12, 14, 255))
        d.ellipse([5, 5, 10, 10], fill=(80, 22, 24, 255))
        for a in range(0, 360, 90):   # zurückgeklappte Lappen
            x = 7.5 + 6 * math.cos(math.radians(a)); y = 7.5 + 6 * math.sin(math.radians(a))
            d.point((x, y), fill=(140, 150, 100, 255))
    else:
        d.line([(3, 7), (12, 8)], fill=(50, 30, 28, 255)); d.line([(8, 3), (7, 12)], fill=(50, 30, 28, 255))
        d.point((7, 7), fill=(30, 15, 15, 255))
    return im

# ---------- Nest-Harz: schwarz glänzend, Röhren und Rippen
def resin():
    im = Image.new("RGBA", (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = shade((26, 29, 36), 1 + rnd.uniform(-0.12, 0.12))
    d = ImageDraw.Draw(im)
    for i in range(3):
        x = 2 + i * 5 + rnd.randint(-1, 1)
        d.line([(x, 0), (x + rnd.randint(-2, 2), 15)], fill=(44, 52, 66, 255), width=2)
        d.line([(x, 0), (x + rnd.randint(-2, 2), 15)], fill=(70, 82, 100, 255), width=1)
    for y in (4, 11):
        d.line([(0, y), (15, y + rnd.randint(-1, 1))], fill=(36, 40, 50, 255))
    for _ in range(6):
        px[rnd.randrange(16), rnd.randrange(16)] = (110, 125, 145, 255)   # Glanzpunkte
    return im

# ---------- Blutlachen (transparent, unregelmäßig)
def puddle(col, glow=False):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    for _ in range(5):
        cx, cy, r = rnd.randint(4, 11), rnd.randint(4, 11), rnd.randint(2, 5)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col + (235,))
    for _ in range(8):
        x, y = rnd.randrange(16), rnd.randrange(16); d.point((x, y), fill=col + (235,))
    px = im.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3]:
                px[x, y] = shade(px[x, y], 1 + rnd.uniform(-0.15, 0.25))
    return im

# ---------- Partikel (8x8 Tropfen in 4 Größen)
def drop(size):
    im = Image.new("RGBA", (8, 8), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    o = (8 - size) / 2
    d.ellipse([o, o, o + size - 1, o + size - 1], fill=(255, 255, 255, 255))
    if size > 3: d.point((o + 1, o + 1), fill=(255, 255, 255, 255))
    return im

# ---------- Energieschild (Hexagon-Muster, halbtransparent)
def shield():
    W = 64; im = Image.new("RGBA", (W, W), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    r = 6
    for row in range(-1, 8):
        for col in range(-1, 7):
            cx = col * r * 1.75 + (row % 2) * r * 0.875; cy = row * r * 1.5
            pts = [(cx + r * math.cos(math.radians(60 * k + 30)), cy + r * math.sin(math.radians(60 * k + 30))) for k in range(6)]
            d.polygon(pts, fill=(255, 255, 255, 60), outline=(255, 255, 255, 230))
    for y in range(W):
        for x in range(0, W, 1):
            if y % 4 == 0:
                p = im.getpixel((x, y)); im.putpixel((x, y), p[:3] + (min(255, p[3] + 40),))
    return im

# ---------- Items 16x16
def exo_suit_icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    olive, metal, dark, visor = (92, 102, 70), (140, 146, 154), (64, 70, 78), (80, 210, 255)
    d.rectangle([5, 1, 10, 5], fill=olive); d.rectangle([6, 3, 9, 3], fill=visor)          # Helm
    d.rectangle([4, 6, 11, 10], fill=olive); d.line([(7, 6), (7, 10)], fill=dark)          # Brust
    d.rectangle([2, 6, 3, 11], fill=metal); d.rectangle([12, 6, 13, 11], fill=metal)       # Arme
    d.rectangle([13, 9, 15, 10], fill=dark)                                                # Kanone
    d.rectangle([5, 11, 6, 15], fill=metal); d.rectangle([9, 11, 10, 15], fill=metal)      # Beine
    d.point((5, 8), fill=(255, 70, 50))
    return im

def trigger_icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rounded_rectangle([4, 3, 11, 14], 2, fill=(60, 66, 74), outline=(30, 32, 36))
    d.ellipse([5, 4, 10, 9], fill=(200, 30, 30), outline=(120, 10, 10)); d.point((6, 5), fill=(255, 150, 150))
    d.rectangle([6, 11, 9, 12], fill=(220, 170, 40))
    return im

def plasma_icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.ellipse([3, 3, 12, 12], fill=(60, 180, 255, 120)); d.ellipse([5, 5, 10, 10], fill=(150, 230, 255, 255))
    d.ellipse([6, 6, 9, 9], fill=(255, 255, 255, 255))
    return im

def effect_icon():
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    for i in range(4):
        d.line([(5, 6 + i * 2), (1, 3 + i * 3)], fill=(150, 130, 100)); d.line([(12, 6 + i * 2), (16, 3 + i * 3)], fill=(150, 130, 100))
    d.ellipse([4, 5, 13, 13], fill=(200, 182, 150), outline=(120, 100, 80))
    d.line([(8, 13), (9, 17)], fill=(150, 130, 100))
    return im

def icon():
    im = Image.new("RGBA", (128, 128), (10, 12, 16, 255)); d = ImageDraw.Draw(im)
    for r in range(60, 0, -4):
        d.ellipse([64 - r, 70 - r, 64 + r, 70 + r], fill=(20 + r // 3, 24 + r // 3, 18 + r // 4, 255))
    d.ellipse([34, 50, 94, 124], fill=(82, 90, 56)); d.ellipse([44, 60, 84, 116], fill=(104, 112, 72))
    d.ellipse([54, 52, 74, 70], fill=(40, 12, 14))
    d.ellipse([46, 8, 82, 44], fill=(26, 29, 36)); d.ellipse([52, 14, 70, 30], fill=(60, 70, 88))
    return im

save(pod_side(), "textures/block/brood_pod_side.png")
save(pod_top(False), "textures/block/brood_pod_top.png")
save(pod_top(True), "textures/block/brood_pod_top_open.png")
save(resin(), "textures/block/hive_resin.png")
save(puddle((120, 8, 10)), "textures/block/blood_puddle.png")
save(puddle((120, 200, 30)), "textures/block/acid_puddle.png")
for i, s in enumerate((2, 3, 4, 5)):
    save(drop(s), f"textures/particle/drop_{i}.png")
save(shield(), "textures/entity/exo_shield.png")
save(exo_suit_icon(), "textures/item/exo_suit.png")
save(trigger_icon(), "textures/item/exo_trigger.png")
save(plasma_icon(), "textures/item/plasma_bolt.png")
save(effect_icon(), "textures/mob_effect/parasite.png")
save(icon(), "icon.png")
print("Texturen erzeugt")
