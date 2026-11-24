# Baut die Sprachspur (voice/voice_track.wav) aus allen bisher geschnittenen Teilen und schreibt timeline.json.
#   python3 voice/build.py
import json, os, glob, subprocess, wave, numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); ROOT = os.path.dirname(HERE)
SR = 48000
rng = np.random.default_rng(7)

tl = json.loads(subprocess.check_output(['node', '-e', f"""
require('{HERE}/real.js'); eval(require('fs').readFileSync('{ROOT}/script.js','utf8')+';global.TL=TL');
console.log(JSON.stringify({{dur:TL.dur, scenes:TL.scenes.map(s=>({{id:s.id,part:s.part,start:s.start,dur:s.dur,cues:s.cues}}))}}));"""]))
json.dump({'dur': tl['dur'], 'starts': {s['id']: s['start'] for s in tl['scenes']}}, open(os.path.join(ROOT, 'timeline.json'), 'w'), indent=1)
SC = {s['id']: s for s in tl['scenes']}

def load(p):
    w = wave.open(p); return np.frombuffer(w.readframes(w.getnframes()), np.int16).astype(np.float32) / 32768

def roomtone(a, length):
    fr = int(0.02 * SR); n = len(a) // fr
    rms = np.sqrt((a[:n * fr].reshape(n, fr) ** 2).mean(1)); thr = np.percentile(rms, 12) * 1.8
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

track = np.zeros(int((tl['dur'] + 1) * SR), np.float32)
for f in sorted(glob.glob(os.path.join(HERE, 'cuts*.json'))):
    part = int(os.path.basename(f)[4:-5]); cuts = json.load(open(f))
    a = load(os.path.join(HERE, f'clean{part}.wav'))
    ps = [s for s in tl['scenes'] if s['part'] == part]
    s0 = ps[0]['start'] + 0.3; s1 = ps[-1]['start'] + ps[-1]['dur'] - 0.2
    bed = roomtone(a, s1 - s0) * 0.75
    f1 = int(0.8 * SR); bed[:f1] *= np.linspace(0, 1, f1); bed[-f1:] *= np.linspace(1, 0, f1)
    i0 = int(s0 * SR); track[i0:i0 + len(bed)] += bed
    for c in cuts:
        clip = a[int(c['start'] * SR):int(c['end'] * SR)].copy()
        fi, fo = int(0.05 * SR), int(0.15 * SR)
        clip[:fi] *= np.linspace(0, 1, fi); clip[-fo:] *= np.linspace(1, 0, fo)
        sc = SC[c['id']]; at = sc['start'] + sc['cues'][c['line']] - 0.1
        j = int(at * SR); track[j:j + len(clip)] += clip
    print(f'Teil {part}: {len(cuts)} Sätze eingesetzt ({ps[0]["start"]:.1f}–{s1:.1f} s)')
d = (np.clip(track, -1, 1) * 32767).astype(np.int16)
w = wave.open(os.path.join(HERE, 'voice_track.wav'), 'wb'); w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(d.tobytes()); w.close()
print('voice_track.wav: %.1f s' % tl['dur'])
