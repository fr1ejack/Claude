"""
Modell- und Textur-Generator für Hive Invasion.

Jedes Modell wird hier als Baum aus Teilen (Part) mit Quadern (Cube) beschrieben.
Das Skript
  1. verteilt die UV-Flächen automatisch auf der Textur,
  2. malt jede Fläche passend zu ihrem Material (Schattierung, Kanten, Details),
  3. schreibt Java-Code (GenModels.java) mit den passenden TexturedModelData.
So passen Modell und Textur immer exakt zusammen.

Aufruf:  python3 tools/modelgen.py
"""
import os, random, math
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/hiveinvasion/textures/entity")
JAVA = os.path.join(ROOT, "src/client/java/de/hiveinvasion/client/model/GenModels.java")


class Cube:
    def __init__(self, x, y, z, w, h, d, mat, mirror=False, dil=0.0):
        self.o = (x, y, z); self.s = (w, h, d); self.mat = mat; self.mirror = mirror; self.dil = dil
        self.uv = None


class Part:
    def __init__(self, name, pivot=(0, 0, 0), rot=(0, 0, 0), cubes=(), children=()):
        self.name = name; self.pivot = pivot; self.rot = rot
        self.cubes = list(cubes); self.children = list(children)

    def all_cubes(self):
        for c in self.cubes: yield c
        for ch in self.children: yield from ch.all_cubes()


def pack(root, tw, th):
    cubes = sorted(root.all_cubes(), key=lambda c: -(c.s[2] + c.s[1]))
    x = y = row = 0
    for c in cubes:
        w, h, d = (int(math.ceil(v)) for v in c.s)
        fw, fh = 2 * (w + d), d + h
        if x + fw > tw:
            x = 0; y += row; row = 0
        if y + fh > th:
            raise SystemExit(f"Textur zu klein für {root.name}")
        c.uv = (x, y); x += fw; row = max(row, fh)


# ---------------------------------------------------------------- Malen
def clamp(v): return max(0, min(255, int(v)))
def shade(c, f): return tuple(clamp(v * f) for v in c[:3])
def mix(a, b, t): return tuple(clamp(a[i] * (1 - t) + b[i] * t) for i in range(3))

MATS = {
    # Name: (Grundfarbe, Rauschen, Stil, leuchtend)
    "carapace":  ((24, 27, 36), 7, "gloss", False),
    "carapace2": ((34, 40, 54), 7, "gloss", False),
    "ribbed":    ((26, 29, 38), 6, "ribs", False),
    "tube":      ((40, 46, 58), 6, "rings", False),
    "teeth":     ((205, 208, 200), 8, "teeth", False),
    "gum":       ((70, 28, 34), 8, "plain", False),
    "claw":      ((60, 64, 70), 6, "gloss", False),
    "skin":      ((198, 182, 150), 10, "veins", False),
    "skin2":     ((176, 158, 126), 10, "joints", False),
    "flesh":     ((214, 186, 168), 10, "blood", False),
    "metal":     ((132, 138, 146), 8, "panel", False),
    "metal_dark":((70, 76, 84), 6, "panel", False),
    "olive":     ((88, 98, 66), 8, "panel", False),
    "hazard":    ((210, 160, 40), 6, "stripes", False),
    "visor":     ((60, 200, 255), 4, "visor", True),
    "thruster":  ((255, 140, 40), 6, "plain", True),
    "glow_red":  ((255, 60, 40), 4, "plain", True),
    "rubber":    ((36, 36, 38), 5, "plain", False),
}


def paint_face(px, glow, x0, y0, w, h, mat, face, rnd):
    base, var, style, lit = MATS[mat]
    light = {"top": 1.18, "bottom": 0.62, "front": 1.0, "back": 0.82, "left": 0.9, "right": 0.9}[face]
    for j in range(h):
        for i in range(w):
            c = shade(base, light * (1.06 - 0.12 * j / max(1, h - 1)))
            n = rnd.randint(-var, var); c = tuple(clamp(v + n) for v in c)
            edge = i == 0 or j == 0 or i == w - 1 or j == h - 1
            if style == "gloss":
                if (i + j) % 7 == 0 and face in ("top", "front", "left", "right"): c = shade(c, 1.45)
                if edge: c = shade(c, 0.75)
            elif style == "ribs":
                if j % 2 == 0: c = shade(c, 1.35)
                if i == 0 or i == w - 1: c = shade(c, 0.7)
            elif style == "rings":
                if j % 3 == 0 or (face in ("top", "bottom")): c = shade(c, 1.3 if j % 3 == 0 else 1.0)
            elif style == "teeth":
                if i % 2 == 1: c = shade(c, 0.7)
            elif style == "veins":
                if rnd.random() < 0.06: c = mix(c, (150, 70, 70), 0.5)
                if edge: c = shade(c, 0.85)
            elif style == "joints":
                if j % 3 == 2: c = shade(c, 0.75)
            elif style == "blood":
                if rnd.random() < 0.08: c = (140, 20, 22)
            elif style == "panel":
                if edge: c = shade(c, 0.6)
                elif (i == 1 or i == w - 2) and (j == 1 or j == h - 2) and w > 4 and h > 4: c = shade(c, 1.5)  # Nieten
                elif h > 6 and j == h // 2: c = shade(c, 0.78)  # Plattenfuge
                elif j == 1 and face != "bottom": c = shade(c, 1.2)
            elif style == "stripes":
                if (i + j) // 2 % 2 == 0: c = (30, 30, 30)
            elif style == "visor":
                t = j / max(1, h - 1)
                c = mix((170, 240, 255), base, t)
                if i == j: c = (230, 255, 255)
            px[x0 + i, y0 + j] = c + (255,)
            if lit:
                glow[x0 + i, y0 + j] = c + (255,)


def paint(root, tw, th, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (tw, th), (0, 0, 0, 0)); glow = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    px, gp = img.load(), glow.load()
    for c in root.all_cubes():
        u, v = c.uv; w, h, d = (int(math.ceil(s)) for s in c.s)
        faces = [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d),
                 ("left", u, v + d, d, h), ("front", u + d, v + d, w, h),
                 ("right", u + d + w, v + d, d, h), ("back", u + 2 * d + w, v + d, w, h)]
        for f, x0, y0, fw, fh in faces:
            if fw > 0 and fh > 0: paint_face(px, gp, x0, y0, fw, fh, c.mat, f, rnd)
    has_glow = any(MATS[c.mat][3] for c in root.all_cubes())
    return img, (glow if has_glow else None)


# ---------------------------------------------------------------- Java
def f(v): return f"{float(v)}f"

def java_part(p, parent_var, out, ind):
    var = "p_" + p.name
    b = "ModelPartBuilder.create()"
    for c in p.cubes:
        b += f".uv({c.uv[0]}, {c.uv[1]})"
        if c.mirror: b += ".mirrored()"
        dil = f", new Dilation({f(c.dil)})" if c.dil else ""
        b += f".cuboid({f(c.o[0])}, {f(c.o[1])}, {f(c.o[2])}, {f(c.s[0])}, {f(c.s[1])}, {f(c.s[2])}{dil})"
        if c.mirror: b += ".mirrored(false)"
    t = f"ModelTransform.of({f(p.pivot[0])}, {f(p.pivot[1])}, {f(p.pivot[2])}, {f(p.rot[0])}, {f(p.rot[1])}, {f(p.rot[2])})"
    out.append(f"{ind}ModelPartData {var} = {parent_var}.addChild(\"{p.name}\", {b}, {t});")
    for ch in p.children: java_part(ch, var, out, ind)


def java_model(method, roots, tw, th):
    out = [f"    public static TexturedModelData {method}() {{",
           "        ModelData data = new ModelData();",
           "        ModelPartData root = data.getRoot();"]
    for r in roots: java_part(r, "root", out, "        ")
    out.append(f"        return TexturedModelData.of(data, {tw}, {th});")
    out.append("    }")
    return "\n".join(out)


# ================================================================ Modelle
R = math.radians

def stalker():
    # Beine: Oberschenkel nach vorn, Schienbein nach hinten (Vogelbein), Fuß.
    def leg(side):
        s = 1 if side == "left" else -1
        return Part(f"{side}_leg", (2.6 * s, 10, 1), (R(-25), 0, 0), [Cube(-1.5, 0, -1.5, 3, 7, 3, "carapace")], [
            Part(f"{side}_shin", (0, 6.5, 0), (R(55), 0, 0), [Cube(-1, 0, -1, 2, 7, 2, "ribbed")], [
                Part(f"{side}_foot", (0, 6.5, 0), (R(-30), 0, 0), [Cube(-1.5, 0, -3.5, 3, 1, 5, "claw")])])])

    def arm(side):
        s = 1 if side == "left" else -1
        return Part(f"{side}_arm", (5.2 * s, -9.5, 0), (R(-20), 0, R(-8) * s), [Cube(-1, -1, -1, 2, 8, 2, "carapace"),
                                                                            Cube(-0.5 if s > 0 else -1.5, -1.5, -1.5, 2, 3, 3, "tube")], [
            Part(f"{side}_forearm", (0, 7, 0), (R(-35), 0, 0), [Cube(-0.75, 0, -0.75, 1.5, 7, 1.5, "ribbed")], [
                Part(f"{side}_hand", (0, 7, 0), (R(10), 0, 0), [
                    Cube(-1, 0, -1, 2, 1, 2, "carapace"),
                    Cube(-1, 1, -1.5, 0.5, 3, 0.5, "claw"), Cube(0.5, 1, -1.5, 0.5, 3, 0.5, "claw"),
                    Cube(-0.25, 1, -1.5, 0.5, 3.5, 0.5, "claw")])])])

    tail_parts = []
    def tail(i):
        size = max(0.8, 2.2 - i * 0.25)
        kids = [tail(i + 1)] if i < 5 else [Part("tail_blade", (0, 0, 4.5), (0, 0, 0),
                                                  [Cube(-0.25, -2, 0, 0.5, 4, 5, "claw")])]
        return Part(f"tail{i}", (0, 0, 4.5 if i else 2), (R(-50) if i == 0 else R(8), 0, 0),
                    [Cube(-size / 2, -size / 2, 0, size, size, 5, "ribbed" if i % 2 else "carapace")], kids)

    head = Part("head", (0, -12, -1), (R(-30), 0, 0), [
        Cube(-2.5, -3, -6, 5, 4, 6, "carapace2"),          # Gesicht
        Cube(-2.5, -4, -1.5, 5, 4, 5, "carapace2"),        # Schädel
        Cube(-2, -4.5, 3, 4, 4, 4, "carapace"),            # Hinterkopf 1
        Cube(-1.5, -4.5, 6.5, 3, 3, 4, "carapace"),        # Hinterkopf 2
        Cube(-1, -4.2, 10, 2, 2, 3, "carapace"),           # Spitze
        Cube(-2, 0.5, -6.2, 4, 1, 1, "teeth"),             # obere Zähne
        Cube(-1.8, -2.5, -6.4, 3.6, 3, 1, "gum")], [       # Lippen
        Part("jaw", (0, 1, -0.5), (R(8), 0, 0), [
            Cube(-2, 0, -5.5, 4, 1, 5, "carapace"), Cube(-1.8, -1, -5.6, 3.6, 1, 1, "teeth")]),
    ])
    body = Part("body", (0, 11, 1), (R(35), 0, 0), [
        Cube(-3, -5, -2, 6, 5, 4, "ribbed"),               # Becken/Bauch
        Cube(-4, -12, -2.5, 8, 7, 5, "ribbed"),            # Brustkorb
        Cube(-1, -12, 2.5, 2, 12, 1, "carapace2")], [      # Wirbelsäule
        head, arm("left"), arm("right"), tail(0),
        Part("tubes", (0, -11, 2.5), (R(30), 0, 0), [
            Cube(-3.5, -1, 0, 1.5, 1.5, 6, "tube"), Cube(2, -1, 0, 1.5, 1.5, 6, "tube"),
            Cube(-2.8, 2, 0, 1.2, 1.2, 4.5, "tube"), Cube(1.6, 2, 0, 1.2, 1.2, 4.5, "tube")]),
    ])
    return [leg("left"), leg("right"), body], 128, 64


def clinger():
    legs = []
    for i in range(8):
        left = i < 4; k = i % 4; s = 1 if left else -1
        z = -3.2 + k * 1.9
        yaw = s * (R(50) - k * R(30))
        legs.append(Part(f"leg{i}", (3 * s, 0.3, z), (0, yaw, R(20) * s),
                         [Cube(0 if left else -5, -0.5, -0.5, 5, 1, 1, "skin2")], [
                             Part(f"leg{i}_tip", (5 * s, 0, 0), (0, 0, R(-55) * s),
                                  [Cube(0 if left else -4, -0.4, -0.4, 4, 0.8, 0.8, "skin")])]))
    tail = Part("tail", (0, 0, 3.5), (R(-5), 0, 0), [Cube(-0.75, -0.75, 0, 1.5, 1.5, 4, "skin2")], [
        Part("tail2", (0, 0, 4), (0, 0, 0), [Cube(-0.6, -0.6, 0, 1.2, 1.2, 4, "skin2")], [
            Part("tail3", (0, 0, 4), (0, 0, 0), [Cube(-0.45, -0.45, 0, 0.9, 0.9, 4, "skin2")])])])
    body = Part("body", (0, 21.8, 0), (0, 0, 0), [
        Cube(-3, -1.5, -4, 6, 2.5, 7.5, "skin"), Cube(-2, -2.3, -3, 4, 1, 5, "skin2"),
        Cube(-1.5, 0.8, -3, 3, 0.7, 3, "flesh")], legs + [tail])
    return [body], 64, 32


def larva():
    head = Part("head", (0, -0.2, -2.5), (0, 0, 0), [
        Cube(-1.6, -3.4, -4, 3.2, 3.2, 4, "flesh"), Cube(-1.2, -1.4, -4.4, 2.4, 0.8, 0.6, "teeth")])
    tail = Part("back", (0, 0, 4.5), (0, 0, 0), [Cube(-1.2, -2.3, 0, 2.4, 2.3, 4, "flesh")], [
        Part("tip", (0, 0, 4), (0, 0, 0), [Cube(-0.8, -1.5, 0, 1.6, 1.5, 3, "flesh")])])
    mid = Part("mid", (0, 0, -2.5), (0, 0, 0), [Cube(-1.6, -3, 0, 3.2, 3, 5, "flesh")], [
        tail,
        Part("arm_l", (1.6, -1, 1), (0, R(30), R(30)), [Cube(0, -0.3, -0.3, 1.5, 0.6, 0.6, "flesh")]),
        Part("arm_r", (-1.6, -1, 1), (0, R(-30), R(-30)), [Cube(-1.5, -0.3, -0.3, 1.5, 0.6, 0.6, "flesh")])])
    body = Part("body", (0, 24, 0), (0, 0, 0), [], [head, mid])
    return [body], 64, 32


def exosuit():
    """Exo-Anzug, Basisgröße wie ein Spieler (wird im Renderer x1.5 skaliert).
    Offene Teile werden in Java animiert (Brustplatten, Helm-Visier, Arme)."""
    def leg(side):
        s = 1 if side == "left" else -1
        return Part(f"{side}_leg", (2.3 * s, 12, 0), (0, 0, 0), [
            Cube(-2.2, 0, -2.2, 4.4, 6, 4.4, "metal"),
            Cube(-1.6, 5, -1.6, 3.2, 2, 3.2, "metal_dark"),     # Knie-Gelenk
            Cube(-2.4, 1, -2.6, 4.8, 1, 0.6, "hazard")], [
            Part(f"{side}_shin", (0, 6.5, 0), (0, 0, 0), [
                Cube(-2, 0, -2, 4, 5, 4, "metal"), Cube(-2.3, 0.5, -2.6, 4.6, 3, 1, "metal_dark"),
                Cube(-2.4, 4.5, -3.6, 4.8, 1.5, 5.8, "metal_dark")])])     # Stiefel

    def arm(side):
        s = 1 if side == "left" else -1
        cubes = [Cube(-2, -2, -2, 4, 6, 4, "metal"),
                 Cube(-2.8 if s < 0 else -2.2, -3, -2.6, 5, 3, 5.2, "olive")]   # Schulterpanzer
        kids = [Part(f"{side}_forearm", (0, 5.5, 0), (0, 0, 0), [
            Cube(-1.8, 0, -1.8, 3.6, 6, 3.6, "metal"), Cube(-2, 4.5, -2, 4, 2, 4, "rubber")], [])]
        if s < 0:   # Waffe am rechten Arm
            kids[0].children.append(Part("cannon", (-2, 2, 0), (0, 0, 0), [
                Cube(-2, -1.5, -5, 2, 3, 8, "metal_dark"), Cube(-1.6, -0.6, -7, 1.2, 1.2, 2, "metal"),
                Cube(-1.4, -0.4, -7.1, 0.8, 0.8, 0.2, "glow_red")]))
        else:       # Schildprojektor am linken Arm
            kids[0].children.append(Part("shield_emitter", (2, 2, 0), (0, 0, 0), [
                Cube(0, -1.5, -2.5, 1.2, 3, 5, "olive"), Cube(1.0, -1, -2, 0.4, 2, 4, "visor")]))
            # Auslöseknopf
            kids[0].children.append(Part("trigger_button", (0, 2, -2), (0, 0, 0),
                                         [Cube(-0.7, 0, -0.5, 1.4, 1, 0.6, "glow_red")]))
        return Part(f"{side}_arm", (6.3 * s, -10, 0), (0, 0, 0), cubes, kids)

    helmet = Part("helmet", (0, -12.2, 0.5), (0, 0, 0), [
        Cube(-3.8, -7.5, -0.8, 7.6, 7.5, 4.6, "olive"),          # Hinterkopf
        Cube(-1, -8.6, -2, 2, 1.2, 6, "metal_dark")], [           # Kamm
        Part("visor", (0, -7.5, -0.8), (0, 0, 0), [
            Cube(-3.8, 0, -4.2, 7.6, 7.5, 4.2, "olive"),
            Cube(-3, 2.2, -4.5, 6, 2.2, 0.5, "visor"),
            Cube(-2, 5.2, -4.4, 4, 1.4, 0.4, "metal_dark")])])
    body = Part("body", (0, 12, 0), (0, 0, 0), [
        Cube(-4.5, -3, -2.6, 9, 3, 5.2, "metal_dark"),           # Hüfte
        Cube(-4.5, -12, 0.6, 9, 9, 2.4, "metal"),                # Rückenplatte
        Cube(-1.5, -12, -0.6, 3, 9, 1.4, "rubber")], [           # Wirbel/Rahmen
        Part("jetpack", (0, -11, 3), (0, 0, 0), [
            Cube(-4, 0, 0, 3, 8, 3, "metal_dark"), Cube(1, 0, 0, 3, 8, 3, "metal_dark"),
            Cube(-1, 1, 0, 2, 5, 2, "hazard"),
            Cube(-3.5, 8, 0.5, 2, 1, 2, "thruster"), Cube(1.5, 8, 0.5, 2, 1, 2, "thruster")]),
        Part("chest_left", (4.5, -7.5, 0.6), (0, 0, 0), [
            Cube(-4.5, -4.5, -3.6, 4.5, 9, 3.6, "olive"), Cube(-4, -2.5, -3.9, 3, 2, 0.4, "glow_red"),
            Cube(-4.6, 2.5, -3.8, 4.6, 1, 0.4, "hazard")]),
        Part("chest_right", (-4.5, -7.5, 0.6), (0, 0, 0), [
            Cube(0, -4.5, -3.6, 4.5, 9, 3.6, "olive"), Cube(0.2, -3.5, -3.9, 3.5, 1, 0.4, "metal_dark"),
            Cube(0, 2.5, -3.8, 4.6, 1, 0.4, "hazard")]),
        helmet, arm("left"), arm("right")])
    return [leg("left"), leg("right"), body], 128, 128


def main():
    random.seed(1)
    methods = []
    for name, fn, seed in [("stalker", stalker, 3), ("clinger", clinger, 5), ("larva", larva, 9), ("exosuit", exosuit, 11)]:
        roots, tw, th = fn()
        holder = Part(name, children=roots)
        pack(holder, tw, th)
        img, glow = paint(holder, tw, th, seed)
        img.save(os.path.join(TEX, f"{name}.png"))
        if glow: glow.save(os.path.join(TEX, f"{name}_glow.png"))
        methods.append(java_model(name, roots, tw, th))
    src = ("// AUTOMATISCH ERZEUGT von tools/modelgen.py – nicht von Hand bearbeiten.\n"
           "package de.hiveinvasion.client.model;\n\nimport net.minecraft.client.model.*;\n\n"
           "public final class GenModels {\n    private GenModels() {}\n\n" + "\n\n".join(methods) + "\n}\n")
    open(JAVA, "w").write(src)
    print("Modelle erzeugt:", ", ".join(n for n in ["stalker", "clinger", "larva", "exosuit"]))


if __name__ == "__main__":
    main()
