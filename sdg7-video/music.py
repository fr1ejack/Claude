# Erzeugt eine einfache, lizenzfreie Hintergrundmusik (music.wav, 170 s).
# Ruhig/nachdenklich bis 1:15, dann optimistisch mit Beat. Blackout bei 0:15 = fast Stille.
import numpy as np, wave

SR = 44100
DUR = 170
t = np.arange(int(SR * DUR)) / SR
out = np.zeros_like(t)

def note(f):  # MIDI -> Hz
    return 440 * 2 ** ((f - 69) / 12)

def env(n, a=0.01, r=0.3):
    e = np.ones(n)
    na, nr = int(a * SR), int(r * SR)
    e[:na] = np.linspace(0, 1, na)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e

def add(start, dur, sig):
    i = int(start * SR)
    j = min(len(out), i + len(sig))
    out[i:j] += sig[: j - i]

# Akkorde (Am F C G) – Pad
chords = [[57, 60, 64], [53, 57, 60], [48, 55, 64], [55, 59, 62]]
bar = 2.4  # 100 BPM, 4/4
for k in range(int(DUR / bar) + 1):
    st = k * bar
    ch = chords[k % 4]
    n = int(bar * 1.15 * SR)
    tt = np.arange(n) / SR
    sig = sum(np.sin(2 * np.pi * note(m) * tt) + 0.3 * np.sin(2 * np.pi * note(m) * 2.003 * tt) for m in ch)
    sig += 0.6 * np.sin(2 * np.pi * note(ch[0] - 12) * tt)
    add(st, bar, 0.05 * sig * env(n, 0.6, 0.8))

# Arpeggio-Pluck ab 75 s (optimistisch), leise auch 20–45 s
def pluck(st, m, vol):
    n = int(0.5 * SR); tt = np.arange(n) / SR
    s = (np.sin(2 * np.pi * note(m) * tt) + 0.4 * np.sin(4 * np.pi * note(m) * tt)) * np.exp(-tt * 7)
    add(st, 0.5, vol * s)
step = bar / 8
for k in range(int(DUR / step)):
    st = k * step
    ch = chords[int(st / bar) % 4]
    m = (ch + [ch[0] + 12])[[0, 1, 2, 3, 2, 1, 2, 3][k % 8]] + 12
    if 75 <= st < 168: pluck(st, m, 0.09)
    elif 20.5 <= st < 44: pluck(st, m, 0.04)

# Kick + Hi-Hat ab 75 s
rng = np.random.default_rng(1)
for k in range(int(DUR / (bar / 4))):
    st = k * bar / 4
    if not (75 <= st < 166): continue
    n = int(0.25 * SR); tt = np.arange(n) / SR
    kick = np.sin(2 * np.pi * (50 + 90 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 14)
    add(st, 0.25, 0.22 * kick)
    hn = int(0.05 * SR)
    hat = rng.standard_normal(hn) * np.exp(-np.arange(hn) / SR * 80)
    hat = np.diff(hat, prepend=0)
    add(st + bar / 8, 0.05, 0.03 * hat)

# Lautstärkeverlauf: Blackout-Stille bei 15–20 s, Intro etwas leiser, Fade-Out
g = np.ones_like(t)
def ramp(a, b, va, vb):
    m = (t >= a) & (t < b); g[m] = va + (vb - va) * (t[m] - a) / (b - a)
g[:] = 0.8
ramp(0, 1, 0, 0.8)
ramp(15.6, 16.0, 0.8, 0.08); g[(t >= 16) & (t < 19.5)] = 0.08
ramp(19.5, 21, 0.08, 0.8)
ramp(45, 75, 0.8, 0.7)
g[(t >= 75)] = 1.0
ramp(74, 76, 0.7, 1.0)
ramp(166, 170, 1.0, 0.0)
out *= g
out /= np.max(np.abs(out)) / 0.8

# leichtes Stereo-Delay
L = out
R = np.concatenate([np.zeros(int(0.012 * SR)), out])[: len(out)]
data = (np.stack([L, R], 1) * 32767).astype(np.int16)
with wave.open(__file__.replace('music.py', 'music.wav'), 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data.tobytes())
print('music.wav geschrieben')
