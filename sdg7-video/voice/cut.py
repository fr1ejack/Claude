# Schneidet die drei Sprachaufnahmen in Sätze, berechnet daraus eine gestraffte Zeitachse
# (Pausen zwischen den Sätzen werden gekürzt) und baut die Sprachspur inkl. Raumton.
# Ausgabe: voice_track.wav, ../timemap.js (für Animation/Renderer), ../timemap.json (für die Musik)
import json, wave, re, numpy as np
SR = 48000
HERE = __file__.rsplit('/', 1)[0] + '/'
W = json.load(open(HERE + 'words.json'))
rng = np.random.default_rng(7)

cues = {'1': ['morgens', 'mit dem', 'abends', 'und plötzlich', 'für', 'genau', 'es ist', 'bis'],
        '2': ['das', 'beim', 'gleichzeitig', 'wie', 'stg7', 'erstens', 'zweitens'],
        '3': ['drittens', 'warum', 'krankenhäuser', 'schulen', 'und fabriken', 'was', 'die heizung', 'oder', 'jede']}
# Startzeiten der Sätze in der ursprünglichen Animation (alte Zeitachse)
starts = [0.3, 5, 10, 15, 20.6, 26, 32, 38.3, 45.2, 52, 60.2, 67.2, 75.3, 80, 92, 106, 120.2, 127.5, 134.5, 141.5, 150.2, 154.6, 159, 164.4]
OLD_DUR = 170.0
# Mindestlänge (neue Zeit) einzelner Abschnitte, damit Bildideen nicht zu hektisch werden
MINLEN = {0: 3.2, 1: 3.2, 2: 3.2, 3: 4.8, 12: 3.4, 22: 3.6}
GAPDEF = {12: 0.9, 22: 1.0}
LEAD, TAIL, GAP, END = 0.15, 0.30, 0.70, 0.45

def load(k):
    w = wave.open(HERE + f'clean{k}.wav')
    return np.frombuffer(w.readframes(w.getnframes()), np.int16).astype(np.float32) / 32768

# 1) Sätze ausschneiden
clips = []
audio = {}
for k in '123':
    a = load(k); audio[k] = a
    words = W[k]; norm = [re.sub(r'[^a-zäöüß0-9]', '', x[2].lower()) for x in words]
    pos = []; j = 0
    for c in cues[k]:
        cc = c.split()
        while not all(norm[j + i].startswith(cc[i]) for i in range(len(cc))): j += 1
        pos.append(j); j += 1
    for n, p in enumerate(pos):
        q = pos[n + 1] - 1 if n + 1 < len(pos) else len(words) - 1
        s = max(0, words[p][0] - 0.15); e = words[q][1] + 0.40
        if n + 1 < len(pos): e = min(e, words[pos[n + 1]][0] - 0.03)
        if p > 0: s = max(s, words[p - 1][1] + 0.02)
        clip = a[int(s * SR):int(e * SR)].copy()
        fi, fo = int(0.05 * SR), int(0.15 * SR)
        clip[:fi] *= np.linspace(0, 1, fi); clip[-fo:] *= np.linspace(1, 0, fo)
        clips.append((k, clip, ''.join(x[2] for x in words[p:q + 1]).strip()))

# 2) Zeitachse straffen: Segmente [neuStart, neuEnde, altStart, altEnde]
segs = []; T = 0.0
for i, a0 in enumerate(starts):
    b0 = starts[i + 1] if i + 1 < len(starts) else OLD_DUR
    d = len(clips[i][1]) / SR
    if i == len(starts) - 1:                       # Outro komplett in Echtzeit
        segs.append([T, T + b0 - a0, a0, b0]); T += b0 - a0; continue
    aEnd = min(b0 - END, a0 + LEAD + d + TAIL)
    A = aEnd - a0
    mid = (b0 - END) - aEnd
    g = max(GAPDEF.get(i, GAP), MINLEN.get(i, 0) - A - END)
    g = min(g, mid)
    segs.append([T, T + A, a0, aEnd]); T += A
    if mid > 0: segs.append([T, T + g, aEnd, b0 - END]); T += g
    segs.append([T, T + END, b0 - END, b0]); T += END
NEW_DUR = round(T, 3)

def old2new(t):
    for ns, ne, os_, oe in segs:
        if os_ <= t <= oe: return ns + (t - os_) / (oe - os_) * (ne - ns) if oe > os_ else ns
    return NEW_DUR

# 3) Sprachspur auf neuer Zeitachse
track = np.zeros(int((NEW_DUR + 1) * SR), np.float32)
SECT = {'1': (0.4, old2new(45.2)), '2': (old2new(45.2), old2new(106.0)), '3': (old2new(106.0), NEW_DUR - 1.0)}

def roomtone(a, length):
    fr = int(0.02 * SR); n = len(a) // fr
    rms = np.sqrt((a[:n * fr].reshape(n, fr) ** 2).mean(1))
    thr = np.percentile(rms, 12) * 1.8
    q = rms < thr; pieces = []; i = 0
    while i < n:
        if q[i]:
            j = i
            while j < n and q[j]: j += 1
            if j - i >= 4: pieces.append(a[(i + 1) * fr:(j - 1) * fr].copy())
            i = j
        else: i += 1
    out = np.zeros(int(length * SR) + SR, np.float32); p0 = 0; xf = int(0.03 * SR)
    while p0 < len(out) - xf:
        p = pieces[rng.integers(len(pieces))].copy()
        if len(p) < 3 * xf: continue
        p[:xf] *= np.linspace(0, 1, xf); p[-xf:] *= np.linspace(1, 0, xf)
        e = min(len(out), p0 + len(p)); out[p0:e] += p[:e - p0]; p0 += len(p) - xf
    return out[:int(length * SR)]

for k in '123':
    s0, s1 = SECT[k]; bed = roomtone(audio[k], s1 - s0) * 0.75
    f1 = int(0.8 * SR); bed[:f1] *= np.linspace(0, 1, f1); bed[-f1:] *= np.linspace(1, 0, f1)
    i0 = int(s0 * SR); track[i0:i0 + len(bed)] += bed

for i, (k, clip, text) in enumerate(clips):
    at = old2new(starts[i] + LEAD)
    print(f'{i:2d} @{at:6.1f}s ({len(clip)/SR:4.1f}s)  {text[:60]}')
    j = int(at * SR); track[j:j + len(clip)] += clip

d = (np.clip(track, -1, 1) * 32767).astype(np.int16)
w = wave.open(HERE + 'voice_track.wav', 'wb'); w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(d.tobytes()); w.close()

json.dump({'dur': NEW_DUR, 'segs': segs}, open(HERE + '../timemap.json', 'w'))
open(HERE + '../timemap.js', 'w').write(
    '// Automatisch erzeugt von voice/cut.py – gestraffte Zeitachse (neue Zeit -> Animationszeit)\n'
    f'window.TIMEMAP={json.dumps({"dur": NEW_DUR, "segs": [[round(x, 4) for x in s] for s in segs]})};\n'
    'window.mapT=function(T){for(const s of TIMEMAP.segs){if(T<=s[1])return s[1]>s[0]?s[2]+(T-s[0])/(s[1]-s[0])*(s[3]-s[2]):s[2];}return 170;};\n')
print('Neue Länge: %.1f s (vorher 170 s)' % NEW_DUR)
