#!/usr/bin/env python3
"""
Physik-Simulation fuer das Mausefallen-Auto "Leichtlaeufer 50".

Rechnet aus, wie weit das Auto mit einer Standard-Mausefalle faehrt.
Reines Python, keine Zusatzpakete noetig:   python3 berechnung.py

Alle Laengen in Metern, Massen in kg, Kraefte in N, Momente in Nm.
Die Annahmen stehen gesammelt oben - wer seine eigene Falle ausmisst
(siehe BAUPLAN.md, Abschnitt "Feder messen"), traegt hier die Werte ein.
"""
import math

# ---------------------------------------------------------------- Annahmen
G = 9.81

# Federmoment einer Standard-Holz-Mausefalle (Typ "Victor"), linear abfallend
M_START = 0.35        # Nm, voll gespannt (Buegel um 180 Grad zurueckgedreht)
M_ENDE = 0.10         # Nm, entspannt (Buegel liegt vorn auf)

# Geometrie (Ursprung: Hinterachse auf Bodenhoehe-Projektion, x nach vorn)
R_RAD = 0.100         # m, Radius Hinterrad (Durchmesser 200 mm)
R_WICKEL = 0.00175    # m, wirksamer Wickelradius (3-mm-Achse + Schnur)
ACHSE = (0.0, 0.100)  # (x, z) Hinterachse
DREHPUNKT = (0.355, 0.110)  # (x, z) Federachse der Falle
L_HEBEL = 0.350       # m, Hebellaenge ab Federachse

# Gespannt liegt der Hebel waagerecht nach hinten, seine Spitze 10 mm ueber
# der Hinterachse (Schnur laeuft senkrecht nach unten = maximaler Hebelarm).
# Entspannt liegt er waagerecht nach vorn (phi = pi).
PHI_START = 0.0
SCHWENK = math.pi

# Massen
MASSE = 0.053         # kg, Gesamtmasse laut Stueckliste
M_HINTERRAD = 0.0073  # kg je Hinterrad (Depron-Scheibe + Lauffl.)
M_VORDERRAD = 0.0035  # kg
R_VORDERRAD = 0.050

# Verluste
CRR = 0.010           # effektiver Rollwiderstand inkl. Lagerreibung (konservativ)
ETA = 0.85            # Wirkungsgrad Schnur/Hebel/Falle
CW_A = 1.0 * 0.0016   # cw * Stirnflaeche [m^2]  (Falle + Raeder von vorn)
RHO = 1.2             # kg/m^3 Luft

ZIEL = 50.0           # m


# ---------------------------------------------------------------- Geometrie
def spitze(phi):
    """Hebelspitze; phi=0: Hebel waagerecht nach hinten, phi=pi: waagerecht nach vorn."""
    px, pz = DREHPUNKT
    # Hebel schwenkt in der x-z-Ebene ueber die Oberseite nach vorn
    return (px - L_HEBEL * math.cos(phi), pz + L_HEBEL * math.sin(phi))


def schnurlaenge(phi):
    tx, tz = spitze(phi)
    return math.hypot(tx - ACHSE[0], tz - ACHSE[1])


def federmoment(phi):
    return M_START - (M_START - M_ENDE) * phi / math.pi


def simulation(dx=0.001):
    s0 = schnurlaenge(PHI_START)
    # Effektive Masse inkl. Rotationstraegheit (Scheibe: I = m r^2 / 2)
    m_eff = MASSE + 2 * 0.5 * M_HINTERRAD + 0.5 * M_VORDERRAD
    x, v2 = 0.0, 0.0          # Weg, Geschwindigkeit^2
    phi = PHI_START
    e_feder = 0.0
    x_antrieb = None
    v_max = 0.0
    t = 0.0
    while True:
        f_antrieb = 0.0
        if phi < SCHWENK:
            # Schnur wird um ds = dx * r_w / R abgezogen
            ds = dx * R_WICKEL / R_RAD
            s_neu = schnurlaenge(phi) + ds
            # phi so weit erhoehen, bis Schnurlaenge passt (Bisektion)
            lo, hi = phi, SCHWENK
            if schnurlaenge(hi) <= s_neu:
                phi_neu = SCHWENK
            else:
                for _ in range(40):
                    mid = (lo + hi) / 2
                    if schnurlaenge(mid) < s_neu:
                        lo = mid
                    else:
                        hi = mid
                phi_neu = (lo + hi) / 2
            dw = 0.5 * (federmoment(phi) + federmoment(phi_neu)) * (phi_neu - phi)
            e_feder += dw
            f_antrieb = ETA * dw / dx
            phi = phi_neu
            if phi >= SCHWENK and x_antrieb is None:
                x_antrieb = x
        v = math.sqrt(max(v2, 0.0))
        f_roll = CRR * MASSE * G if (v > 0 or f_antrieb > 0) else 0.0
        f_luft = 0.5 * RHO * CW_A * v2
        v2 += 2 * (f_antrieb - f_roll - f_luft) / m_eff * dx
        if v2 <= 0 and phi >= SCHWENK:
            break
        if v2 <= 0 and f_antrieb <= f_roll:
            break  # bleibt stehen
        v_neu = math.sqrt(max(v2, 0.0))
        if v + v_neu > 0:
            t += 2 * dx / (v + v_neu)
        v_max = max(v_max, v_neu)
        x += dx
        if x > 1000:
            break
    return {
        "strecke": x,
        "antriebsstrecke": x_antrieb if x_antrieb is not None else x,
        "schnur": schnurlaenge(SCHWENK) - s0,
        "e_feder": e_feder,
        "v_max": v_max,
        "zeit": t,
        "m_eff": m_eff,
    }


def anfahrt():
    """Pruefen, ob das Auto sicher anfaehrt und die Raeder nicht durchdrehen."""
    d = 1e-4
    hebelarm = (schnurlaenge(PHI_START + d) - schnurlaenge(PHI_START)) / d  # ds/dphi
    zug = federmoment(PHI_START) / hebelarm
    f_antrieb = ETA * zug * R_WICKEL / R_RAD
    f_roll = CRR * MASSE * G
    # ~55 % der Last auf der Hinterachse, Gummi auf Hallenboden mu ~ 0.7
    f_haft = 0.7 * 0.55 * MASSE * G
    return hebelarm, zug, f_antrieb, f_roll, f_haft


if __name__ == "__main__":
    e_gespeichert = 0.5 * (federmoment(PHI_START) + M_ENDE) * (SCHWENK - PHI_START)
    r = simulation()
    arm, zug, fa, fr, fh = anfahrt()
    print("Mausefallen-Auto 'Leichtlaeufer 50' - Berechnung")
    print("=" * 52)
    print(f"Startwinkel Hebel               {math.degrees(PHI_START):6.1f} Grad")
    print(f"Abstand Hebelspitze-Achse       {schnurlaenge(PHI_START) * 1000:6.1f} mm")
    print(f"Gespeicherte Federenergie       {e_gespeichert:6.3f} J")
    print(f"Nutzbare Energie (eta={ETA:.2f})     {ETA * e_gespeichert:6.3f} J")
    print(f"Gesamtmasse                     {MASSE * 1000:6.1f} g")
    print(f"Uebersetzung Rad/Wickelachse    {R_RAD / R_WICKEL:6.1f} : 1")
    print(f"Abgezogene Schnurlaenge         {r['schnur'] * 100:6.1f} cm")
    print("-" * 52)
    print(f"Anfahren: Schnurzug {zug:5.2f} N, Antriebskraft {fa * 1000:5.1f} mN")
    print(f"          Rollwiderstand {fr * 1000:5.1f} mN, Haftgrenze {fh * 1000:5.0f} mN")
    print(f"          -> faehrt an: {'ja' if fa > fr else 'NEIN'}, "
          f"Raeder drehen durch: {'JA' if fa > fh else 'nein'}")
    print("-" * 52)
    print(f"Strecke mit Antrieb             {r['antriebsstrecke']:6.1f} m")
    print(f"Gesamtstrecke (inkl. Ausrollen) {r['strecke']:6.1f} m")
    print(f"Hoechstgeschwindigkeit          {r['v_max']:6.2f} m/s")
    print(f"Fahrzeit                        {r['zeit']:6.1f} s")
    print(f"Ziel {ZIEL:.0f} m erreicht: {'JA' if r['strecke'] >= ZIEL else 'NEIN'}"
          f"  (Reserve {r['strecke'] / ZIEL * 100 - 100:+.0f} %)")
    print("-" * 52)
    print("Empfindlichkeit (Gesamtstrecke):")
    basis = CRR
    for c in (0.006, 0.010, 0.015, 0.020, 0.025):
        CRR = c
        print(f"  Rollwiderstand {c:.3f}  ->  {simulation(0.002)['strecke']:6.1f} m")
    CRR = basis
    basis_m = MASSE
    for m in (0.053, 0.080, 0.120):
        MASSE = m
        print(f"  Masse {m * 1000:5.0f} g          ->  {simulation(0.002)['strecke']:6.1f} m")
    MASSE = basis_m
