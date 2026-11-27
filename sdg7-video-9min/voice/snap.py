# Korrigiert die Schnittpunkte in cuts<teil>.json anhand echter Sprechpausen.
# Whisper-Zeitstempel können um bis zu ~1,5 s danebenliegen (besonders am Aufnahmeanfang).
# Ein Satzanfang/-ende wird nur verschoben, wenn in der Nähe eine echte Pause liegt; längere Pausen werden bevorzugt.
#   python3 voice/snap.py <teil>
import sys, json, os, wave, numpy as np
HERE = os.path.dirname(os.path.abspath(__file__))
part = sys.argv[1]
cuts = json.load(open(os.path.join(HERE, f'cuts{part}.json')))
w = wave.open(os.path.join(HERE, f'clean{part}.wav')); sr = w.getframerate()
a = np.frombuffer(w.readframes(w.getnframes()), np.int16).astype(float) / 32768
F = 0.02; fr = int(F * sr); n = len(a) // fr
db = 20 * np.log10(np.sqrt((a[:n * fr].reshape(n, fr) ** 2).mean(1)) + 1e-9)
thr = max(np.percentile(db, 15) + 12, np.percentile(db, 90) - 26)
v = db > thr
# sehr kurze Geräusche (< 0,1 s, z. B. Atmer, Klicks) zählen nicht als Sprache
i = 0
while i < n:
    if v[i]:
        j = i
        while j < n and v[j]: j += 1
        before = i - (np.where(v[:i])[0][-1] + 1 if v[:i].any() else 0)
        after = (np.where(v[j:])[0][0] if v[j:].any() else n - j)
        if j - i < 5 and before >= 15 and after >= 15: v[i:j] = False   # nur isolierte Geräusche
        i = j
    else: i += 1
# Pausen = stille Abschnitte >= 0,15 s
sil = []; i = 0
while i < n:
    if not v[i]:
        j = i
        while j < n and not v[j]: j += 1
        if (j - i) * F >= 0.15 or i == 0: sil.append((i * F, j * F))
        i = j
    else: i += 1
def best_onset(t):   # Sprachbeginn nach einer Pause, nahe t
    c = [(min(1.0, e - s) - 0.3 * abs(e - t), e) for s, e in sil if abs(e - t) <= 1.5 and e < n * F]
    return max(c)[1] if c else None
def best_offset(t):  # Sprachende vor einer Pause, nahe t
    c = [(min(1.0, e - s) - 0.3 * abs(s - t), s) for s, e in sil if abs(s - t) <= 1.5 and s > 0]
    return max(c)[1] if c else None
# zusammenhängende Sprachblöcke (Lücken < 0,12 s überbrückt)
blocks = []; i = 0
while i < n:
    if v[i]:
        j = i
        while j < n and (v[j] or v[j:j + 6].any()): j += 1
        blocks.append((i * F, j * F)); i = j
    else: i += 1
def block_at(t):
    for b0, b1 in blocks:
        if b0 <= t <= b1: return (b0, b1)
    return None
changed = 0; prev_end = 0
for k, c in enumerate(cuts):
    ws, we = c['start'] + 0.15, c['end'] - 0.40      # ungefähre Wortgrenzen laut Whisper
    on, off = best_onset(ws), best_offset(we)
    s = (on - 0.12) if on is not None else c['start']
    # Satzbeginn mitten in Sprache? Dann an den Blockanfang ziehen, wenn der vorige Satz dort schon vorbei ist
    blk = block_at(ws)
    if blk and blk[0] < ws - 0.25 and blk[0] >= prev_end - 0.35 and ws - blk[0] <= 2.5:
        s = min(s, blk[0] - 0.12)
    e = (off + 0.3) if off is not None else c['end']
    s = max(s, prev_end - 0.1, 0)
    if e <= s + 0.5: s, e = c['start'], c['end']
    if abs(s - c['start']) > 0.25 or abs(e - c['end']) > 0.25:
        print(f"{c['id']:>10}[{c['line']}]  {c['start']:6.2f}–{c['end']:6.2f}  →  {s:6.2f}–{e:6.2f}   {c['text'][:45]}"); changed += 1
    c['start'], c['end'] = round(s, 3), round(e, 3); prev_end = e
for k, c in enumerate(cuts[:-1]):
    nx = cuts[k + 1]
    same_block = block_at(c['end'] - 0.35) is not None and block_at(c['end'] - 0.35) == block_at(nx['start'] + 0.15)
    if nx['id'] == c['id'] and (nx['start'] - (c['end'] - 0.3) <= 0.5 or same_block):
        # Sätze in derselben Szene ohne echte Pause: Audio läuft durch, kein Schnitt, keine Blende
        c['end'] = nx['start']; c['gap'] = 0.0; c['join'] = True
    else:
        c.pop('join', None)
        c['gap'] = round(min(1.1, max(0.45, nx['start'] - c['end'] + 0.42)), 3)
json.dump(cuts, open(os.path.join(HERE, f'cuts{part}.json'), 'w'), ensure_ascii=False, indent=1)
print(f'Teil {part}: {changed} Schnittpunkte korrigiert')

# real.js aus allen cuts*.json neu schreiben
import glob
real = {}
for f in sorted(glob.glob(os.path.join(HERE, 'cuts*.json'))):
    for c in json.load(open(f)):
        r = real.setdefault(c['id'], {'lens': [], 'gaps': []})
        r['lens'].append(round(c['end'] - c['start'] - 0.2, 3)); r['gaps'].append(round(c['gap'] + 0.2, 3))
open(os.path.join(HERE, 'real.js'), 'w').write('// Automatisch erzeugt von voice/align.py / snap.py\nvar REAL = ' + json.dumps(real) + ';\nif(typeof globalThis!=="undefined") globalThis.REAL = REAL;\n')
