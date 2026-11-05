/*
 * Das "Gehirn" von Jarvis: ein offenes Sprachmodell, das komplett im Browser läuft (WebLLM + WebGPU).
 * Kein API-Key, keine Kosten, keine Daten an fremde Server – das Modell wird einmal heruntergeladen
 * und danach vom Browser zwischengespeichert.
 */
import { memory } from './memory.js';

const WEBLLM = 'https://cdn.jsdelivr.net/npm/@mlc-ai/web-llm@0.2.84/+esm';

export const MODELS = [
  { id: 'Qwen2.5-1.5B-Instruct-q4f16_1-MLC', name: 'Leicht', size: '≈ 1 GB', note: 'schnell, auch für Laptops & Handys' },
  { id: 'Qwen2.5-3B-Instruct-q4f16_1-MLC', name: 'Standard', size: '≈ 2 GB', note: 'empfohlen – gutes Deutsch' },
  { id: 'Qwen2.5-7B-Instruct-q4f16_1-MLC', name: 'Stark', size: '≈ 5 GB', note: 'beste Antworten, braucht starke Grafikkarte' },
];

export class Brain {
  constructor() {
    this.state = 'off'; // off | loading | ready | error
    this.progress = 0;
    this.model = null;
    this.engine = null;
    this.history = [];
    this.listeners = new Set();
  }
  static supported() { return typeof navigator !== 'undefined' && !!navigator.gpu; }
  get ready() { return this.state === 'ready'; }
  onChange(fn) { this.listeners.add(fn); }
  emit(text = '') { for (const fn of this.listeners) fn(this.state, this.progress, text); }

  async load(modelId) {
    if (this.state === 'loading') return;
    if (!Brain.supported()) { this.state = 'error'; this.emit('Dieser Browser unterstützt kein WebGPU.'); throw new Error('WebGPU fehlt'); }
    this.state = 'loading'; this.progress = 0; this.emit('Starte …');
    try {
      if (this.engine) { try { await this.engine.unload(); } catch { /* */ } }
      const webllm = await import(WEBLLM);
      const worker = new Worker(new URL('./llm-worker.js', import.meta.url), { type: 'module' });
      this.engine = await webllm.CreateWebWorkerMLCEngine(worker, modelId, {
        initProgressCallback: r => { this.progress = r.progress || 0; this.emit(r.text || ''); },
      });
      this.model = modelId;
      memory.setModel(modelId);
      this.state = 'ready'; this.progress = 1; this.emit('Bereit');
    } catch (e) {
      console.error(e);
      this.state = 'error'; this.emit(String(e.message || e));
      throw e;
    }
  }

  systemPrompt(ctx) {
    const facts = memory.facts().map(f => '- ' + f.text).join('\n');
    const rules = memory.rules().trim();
    return [
      'Du bist J.A.R.V.I.S., der persönliche KI-Assistent des Nutzers – höflich, schlagfertig, mit trockenem britischem Humor.',
      'Du antwortest auf jede Frage so gut du kannst, offen und direkt. Antworte auf Deutsch (außer der Nutzer spricht eine andere Sprache).',
      'Deine Antworten werden laut vorgelesen: sprich natürlich, keine Markdown-Formatierung, keine Listen mit Sternchen, keine Links. Halte dich kurz (1–4 Sätze), außer der Nutzer möchte ausdrücklich mehr.',
      'Du bekommst Ergebnisse deiner eigenen Internet-Recherche und deines Gedächtnisses. Stütze dich darauf. Wenn sie nicht reichen, nutze dein Allgemeinwissen und sag ehrlich, wenn du dir unsicher bist. Erfinde keine Fakten.',
      `Aktuelle Zeit: ${new Date().toLocaleString('de-DE', { dateStyle: 'full', timeStyle: 'short' })}.`,
      ctx.location ? `Standort des Nutzers: ${ctx.location}.` : '',
      facts ? `Das hat dir der Nutzer über sich gesagt ("ich" = der Nutzer):\n${facts}` : '',
      rules ? `Anweisungen des Nutzers – diese haben Vorrang und gelten immer:\n${rules}` : '',
    ].filter(Boolean).join('\n\n');
  }

  /** Antwort erzeugen (gestreamt). context: Recherche-Text */
  async answer(question, { context = '', location = '', onToken } = {}) {
    if (!this.ready) throw new Error('Gehirn nicht geladen');
    const user = context ? `${question}\n\n[Rechercheergebnisse & Erinnerungen – nur als Hintergrund]\n${context}` : question;
    const messages = [{ role: 'system', content: this.systemPrompt({ location }) }, ...this.history.slice(-8), { role: 'user', content: user }];
    const stream = await this.engine.chat.completions.create({ messages, stream: true, temperature: 0.6, top_p: 0.9, max_tokens: 450 });
    let out = '';
    for await (const ch of stream) {
      out += ch.choices?.[0]?.delta?.content || '';
      onToken?.(out);
    }
    out = out.replace(/\*\*?|__|#+\s/g, '').trim();
    this.history.push({ role: 'user', content: question }, { role: 'assistant', content: out });
    if (this.history.length > 16) this.history.splice(0, this.history.length - 16);
    return out;
  }
  reset() { this.history = []; }
}

/*
 * Gehirn für die private claude.ai-Version: nutzt Claude über das claude.ai-Konto des Betrachters
 * (Artifact-Fähigkeit "sample"). Kein Download, kein API-Key. Beim ersten Mal fragt claude.ai um Erlaubnis.
 */
export class ClaudeBrain extends Brain {
  static supported() { return true; }
  async load() {
    if (this.state === 'loading' || this.ready) return;
    this.state = 'loading'; this.progress = 0.5; this.emit('Verbinde mit Claude …');
    const sample = await window.claude?.use?.('sample').catch(() => null);
    if (!sample) { this.state = 'error'; this.emit('Claude ist in dieser Ansicht nicht verfügbar'); throw new Error('sample nicht verfügbar'); }
    this.sample = sample; this.model = 'Claude';
    this.state = 'ready'; this.progress = 1; this.emit('Bereit');
  }
  async answer(question, { context = '', location = '', onToken } = {}) {
    if (!this.ready) throw new Error('Gehirn nicht geladen');
    const sys = this.systemPrompt({ location }).replace('Du bekommst Ergebnisse deiner eigenen Internet-Recherche und deines Gedächtnisses. Stütze dich darauf.', 'Du bekommst Einträge aus deinem Gedächtnis. Stütze dich darauf.')
      + '\n\nDiese Version läuft offline: Für Live-Daten (Flüge, Wetter, Nachrichten von heute) sag kurz, dass die Vollversion von Jarvis das kann.';
    const turns = this.history.slice(-8).map(m => ({ role: m.role, content: m.content }));
    const last = `${sys}\n\n---\n${context ? `[Gedächtnis – nur als Hintergrund]\n${context}\n\n` : ''}Nutzer: ${question}`;
    const input = [...turns, { role: 'user', content: last }];
    if (input[0].role !== 'user') input.shift();
    try {
      const { text } = await this.sample(input, { modelTier: 'quick', cache: false, onText: ({ text }) => onToken?.(text) });
      const out = String(text || '').replace(/\*\*?|__|#+\s/g, '').trim();
      this.history.push({ role: 'user', content: question }, { role: 'assistant', content: out });
      if (this.history.length > 16) this.history.splice(0, this.history.length - 16);
      return out;
    } catch (e) {
      if (e?.code === 'not_granted') return 'Sie haben mir den Zugriff auf Claude nicht erlaubt. Laden Sie die Seite neu und erlauben Sie es, dann kann ich frei antworten.';
      if (e?.code === 'rate_limited') return 'Einen Moment bitte, ich bekomme gerade zu viele Anfragen.';
      throw e;
    }
  }
}
