# Lizenzfreie Hintergrundmusik für die 9-Minuten-Fassung (music.wav), passend zu timeline.json.
import numpy as np, wave, json, os
HERE = os.path.dirname(os.path.abspath(__file__))
TLJ = json.load(open(os.path.join(HERE, 'timeline.json')))
st = TLJ['starts']; DUR = TLJ['dur']
SR = 44100
t = np.arange(int(SR * DUR)) / SR
out = np.zeros_like(t)
note = lambda m: 440 * 2 ** ((m - 69) / 12)

def add(start, sig):
    i = int(start * SR); j = min(len(out), i + len(sig))
    if j > i: out[i:j] += sig[:j - i]
def env(n, a, r):
    e = np.ones(n); na, nr = int(a * SR), int(r * SR)
    e[:na] = np.linspace(0, 1, na); e[-nr:] *= np.linspace(1, 0, nr); return e

# Abschnitte: (Start, Akkorde, Pluck-Lautstärke, Beat an?)
AM = [[57, 60, 64], [53, 57, 60], [48, 55, 64], [55, 59, 62]]   # Am F C G
DM = [[50, 53, 57], [46, 50, 53], [53, 57, 60], [48, 52, 55]]   # Dm Bb F C
UP = [[48, 52, 55], [55, 59, 62], [57, 60, 64], [53, 57, 60]]   # C G Am F
FU = [[53, 57, 60], [48, 52, 55], [55, 59, 62], [57, 60, 64]]   # F C G Am
SECT = [(0, AM, 0.0, False), (st['world'], AM, 0.0, False), (st['tile'], AM, 0.045, False), (st['coal'], DM, 0.0, False),
        (st['goals1'], UP, 0.08, True), (st['why'], FU, 0.06, False), (st['room'], FU, 0.085, True), (st['outro'], FU, 0.085, True)]
def sect(x):
    s = SECT[0]
    for q in SECT:
        if x >= q[0]: s = q
    return s

bar = 2.4
for k in range(int(DUR / bar) + 1):
    s0 = k * bar; ch = sect(s0)[1][k % 4]
    n = int(bar * 1.15 * SR); tt = np.arange(n) / SR
    sig = sum(np.sin(2 * np.pi * note(m) * tt) + 0.3 * np.sin(2 * np.pi * note(m) * 2.003 * tt) for m in ch)
    sig += 0.6 * np.sin(2 * np.pi * note(ch[0] - 12) * tt)
    add(s0, 0.05 * sig * env(n, 0.6, 0.8))

step = bar / 8
for k in range(int(DUR / step)):
    s0 = k * step; S_ = sect(s0); vol = S_[2]
    if vol <= 0 or s0 > DUR - 3: continue
    ch = S_[1][int(s0 / bar) % 4]
    m = (ch + [ch[0] + 12])[[0, 1, 2, 3, 2, 1, 2, 3][k % 8]] + 12
    n = int(0.5 * SR); tt = np.arange(n) / SR
    add(s0, vol * (np.sin(2 * np.pi * note(m) * tt) + 0.4 * np.sin(4 * np.pi * note(m) * tt)) * np.exp(-tt * 7))

rng = np.random.default_rng(1)
for k in range(int(DUR / (bar / 4))):
    s0 = k * bar / 4
    if not sect(s0)[3] or s0 > DUR - 5: continue
    n = int(0.25 * SR); tt = np.arange(n) / SR
    add(s0, 0.2 * np.sin(2 * np.pi * (50 + 90 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 14))
    hn = int(0.05 * SR); hat = np.diff(rng.standard_normal(hn) * np.exp(-np.arange(hn) / SR * 80), prepend=0)
    add(s0 + bar / 8, 0.028 * hat)

g = np.full_like(t, 0.8)
def ramp(a, b, va, vb):
    m = (t >= a) & (t < b); g[m] = va + (vb - va) * (t[m] - a) / (b - a)
ramp(0, 1, 0, 0.8)
bo = st['blackout']
ramp(bo + 0.5, bo + 0.9, 0.8, 0.07); g[(t >= bo + 0.9) & (t < st['world'] - 1)] = 0.07
ramp(st['world'] - 1, st['world'] + 1, 0.07, 0.75)
g[t >= st['coal']] = 0.7; ramp(st['coal'] - 1, st['coal'] + 1, 0.8, 0.7)
g[t >= st['goals1']] = 0.9; ramp(st['goals1'] - 0.5, st['goals1'] + 1.5, 0.7, 0.9)
ramp(DUR - 5, DUR, 0.9, 0)
out *= g
out /= np.max(np.abs(out)) / 0.8
R = np.concatenate([np.zeros(int(0.012 * SR)), out])[:len(out)]
data = (np.stack([out, R], 1) * 32767).astype(np.int16)
with wave.open(os.path.join(HERE, 'music.wav'), 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data.tobytes())
print('music.wav geschrieben, %.1f s' % DUR)
