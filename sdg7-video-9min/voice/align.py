# Ordnet die Wörter einer Aufnahme (Whisper-Transkript) den Sätzen im Drehbuch zu.
#   python3 voice/align.py <teil> <aufnahme.wav> <words.json>
# Ergebnis: voice/clean<teil>.wav (entrauscht), voice/cuts<teil>.json (Schnittpunkte je Satz),
#           voice/real.js (echte Satzlängen + Pausen für die Zeitachse, alle bisher aufgenommenen Teile)
import sys, json, re, difflib, subprocess, os, glob, wave, numpy as np
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
part, src, wjson = int(sys.argv[1]), sys.argv[2], sys.argv[3]

# Drehbuch-Sätze dieses Teils aus script.js holen
lines = json.loads(subprocess.check_output(['node', '-e', f"""
eval(require('fs').readFileSync('{ROOT}/script.js','utf8')+';global.SCRIPT=SCRIPT');
console.log(JSON.stringify(SCRIPT.filter(s=>s.part==={part}).map(s=>({{id:s.id,lines:s.lines}}))));"""]))

NUM = {'5': 'fünf', '17': 'siebzehn'}
def norm(w):
    w = w.lower().replace('ß', 'ss')
    w = re.sub(r'[^a-zäöü0-9.]', '', w).strip('.')
    return NUM.get(w, w)

script_tok, owner = [], []
for si, sc in enumerate(lines):
    for li, l in enumerate(sc['lines']):
        for w in re.split(r'\s+', l.replace('–', ' ').replace('…', ' ')):
            n = norm(w)
            if n: script_tok.append(n); owner.append((si, li))
words = json.load(open(wjson))
rec_tok = [norm(w[2]) for w in words]

sm = difflib.SequenceMatcher(None, script_tok, rec_tok, autojunk=False)
s2r = {}
for a, b, n in sm.get_matching_blocks():
    for k in range(n): s2r[a + k] = b + k
print('Übereinstimmung: %.0f %% der Drehbuch-Wörter gefunden' % (100 * len(s2r) / len(script_tok)))

# erstes/letztes erkanntes Wort je Satz
keys = sorted(set(owner), key=lambda x: (x[0], x[1]))
span = {}
for k in keys:
    idx = [i for i, o in enumerate(owner) if o == k]
    m = [s2r[i] for i in idx if i in s2r]
    if not m: raise SystemExit(f'Satz nicht gefunden: {lines[k[0]]["lines"][k[1]]}')
    span[k] = [min(m), max(m)]
# Lücken schließen: nicht erkannte Wörter zwischen zwei Sätzen zuordnen –
# fehlt dem nächsten Satz sein erstes Wort, gehören sie zum nächsten Satz, sonst zum vorherigen
ks = keys
first_matched = {k: (owner.index(k) in s2r) for k in ks}
for i, k in enumerate(ks):
    if i + 1 < len(ks):
        k2 = ks[i + 1]
        if span[k][1] >= span[k2][0]: span[k][1] = span[k2][0] - 1
        if span[k2][0] - span[k][1] > 1:
            if not first_matched[k2]: span[k2][0] = span[k][1] + 1
            else: span[k][1] = span[k2][0] - 1

# Entrauschen (tiefes Brummen raus, sanfte Rauschminderung, Lautheit angleichen)
clean = os.path.join(HERE, f'clean{part}.wav')
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', src, '-af',
                'highpass=f=85,afftdn=nf=-40:nr=10:tn=1,loudnorm=I=-17:TP=-1.5:LRA=9', '-ar', '48000', '-ac', '1', clean], check=True)

cuts = []
for i, k in enumerate(ks):
    a, b = span[k]
    s = max(0, words[a][0] - 0.15); e = words[b][1] + 0.40
    if a > 0: s = max(s, words[a - 1][1] + 0.02)
    if b + 1 < len(words): e = min(e, words[b + 1][0] - 0.03)
    gap = (words[span[ks[i + 1]][0]][0] - words[b][1]) if i + 1 < len(ks) else 0.6
    cuts.append({'id': lines[k[0]]['id'], 'line': k[1], 'start': round(s, 3), 'end': round(e, 3),
                 'gap': round(min(1.1, max(0.45, gap)), 3), 'text': ''.join(w[2] for w in words[a:b + 1]).strip()})
json.dump(cuts, open(os.path.join(HERE, f'cuts{part}.json'), 'w'), ensure_ascii=False, indent=1)
for c in cuts: print(f"{c['id']:>10}[{c['line']}] {c['end']-c['start']:5.1f}s  {c['text'][:70]}")

# real.js aus allen vorhandenen cuts*.json bauen
real = {}
for f in sorted(glob.glob(os.path.join(HERE, 'cuts*.json'))):
    for c in json.load(open(f)):
        r = real.setdefault(c['id'], {'lens': [], 'gaps': []})
        r['lens'].append(round(c['end'] - c['start'] - 0.2, 3))  # Ein-/Ausklang zählt nicht voll
        r['gaps'].append(round(c['gap'] + 0.2, 3))
open(os.path.join(HERE, 'real.js'), 'w').write('// Automatisch erzeugt von voice/align.py\nvar REAL = ' + json.dumps(real) + ';\nif(typeof globalThis!=="undefined") globalThis.REAL = REAL;\n')
print('real.js geschrieben für:', ', '.join(real))
