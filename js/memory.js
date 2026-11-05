/*
 * Jarvis' Gedächtnis – bleibt im Browser des jeweiligen Nutzers gespeichert (localStorage).
 *  - facts:       Dinge, die der Nutzer ihm beigebracht hat („Merke dir, dass …“)
 *  - experiences: Wissen, das Jarvis bei Internet-Recherchen gesammelt hat
 *  - rules:       Anweisungen des Nutzers, die immer gelten (Vorrang vor allem anderen)
 */
const K = { facts: 'jarvis.facts', exp: 'jarvis.experiences', rules: 'jarvis.rules', model: 'jarvis.model' };
const MAX_EXP = 300;

function read(key, def) { try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : def; } catch { return def; } }
function write(key, v) { try { localStorage.setItem(key, JSON.stringify(v)); } catch { /* voll oder gesperrt */ } }

const STOP = new Set('der die das ein eine einen einem einer und oder aber ist sind war wer was wie wo wann warum wieso weshalb welche welcher welches ich du er sie es wir ihr mir mich dir dich uns euch mein dein sein ihr von zu zum zur mit auf in im an am für über unter nach bei aus den dem des nicht kein keine auch noch schon mal bitte jarvis eigentlich gerade jetzt genau denn kannst kann können sag sage erzähl erkläre mir hat haben wird werden gibt es so dann denn doch ja nein the a of to is'.split(' '));
export function keywords(text) {
  return [...new Set(String(text).toLowerCase().replace(/[^\p{L}\p{N}\s-]/gu, ' ').split(/\s+/).filter(w => w.length > 2 && !STOP.has(w)))];
}
function score(kw, text) {
  const t = text.toLowerCase();
  return kw.reduce((s, w) => s + (t.includes(w) ? (w.length > 5 ? 2 : 1) : 0), 0);
}

export const memory = {
  facts: () => read(K.facts, []),
  addFact(text) { const f = this.facts(); f.push({ text, t: Date.now() }); write(K.facts, f.slice(-100)); },
  experiences: () => read(K.exp, []),
  learn(items) {
    if (!items?.length) return;
    const exp = this.experiences();
    for (const it of items) {
      const i = exp.findIndex(e => e.title === it.title);
      const rec = { title: it.title, text: it.text.slice(0, 900), url: it.url, t: Date.now(), uses: (exp[i]?.uses || 0) + 1 };
      if (i >= 0) exp.splice(i, 1);
      exp.push(rec);
    }
    write(K.exp, exp.slice(-MAX_EXP));
  },
  relevant(query, n = 3) {
    const kw = keywords(query);
    if (!kw.length) return [];
    return this.experiences().map(e => ({ e, s: score(kw, e.title + ' ' + e.text) })).filter(x => x.s >= 2).sort((a, b) => b.s - a.s).slice(0, n).map(x => x.e);
  },
  rules: () => read(K.rules, ''),
  setRules(v) { write(K.rules, String(v || '').slice(0, 4000)); },
  model: () => read(K.model, null),
  setModel(id) { write(K.model, id); },
  clear() { write(K.facts, []); write(K.exp, []); },
};
