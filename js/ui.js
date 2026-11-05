import { esc } from './util.js';

const $ = id => document.getElementById(id);
const logEl = $('log');
const panel = $('ctxPanel');

export function log(role, text) {
  const d = document.createElement('div');
  d.className = 'msg ' + role;
  d.innerHTML = `<b>${role === 'user' ? 'SIE' : 'J.A.R.V.I.S.'}</b>${esc(text)}`;
  logEl.appendChild(d);
  while (logEl.children.length > 60) logEl.firstChild.remove();
  logEl.scrollTop = logEl.scrollHeight;
  return d;
}

let onPanelClick = null;
export function showPanel(title, html, onClick) {
  panel.hidden = false;
  panel.innerHTML = `<div class="panel-head"><span>${esc(title)}</span><button type="button" aria-label="Schließen" data-close>×</button></div><div class="panel-body">${html}</div>`;
  panel.classList.remove('enter'); void panel.offsetWidth; panel.classList.add('enter');
  onPanelClick = onClick || null;
}
export function hidePanel() { panel.hidden = true; panel.innerHTML = ''; }
panel.addEventListener('click', e => {
  if (e.target.closest('[data-close]')) return hidePanel();
  const it = e.target.closest('[data-id]');
  if (it && onPanelClick) onPanelClick(it.dataset.id, it);
});

export function setState(text, cls = '') {
  const s = $('sysState');
  s.textContent = text; s.className = 'state ' + cls;
}
export function setStat(id, v) { const e = $(id); if (e) e.textContent = v; }
export function setLocation(name, coords) { $('locName').textContent = name; $('locCoords').textContent = coords || ''; }

export function chips(list, onPick) {
  const c = $('chips');
  c.innerHTML = '';
  for (const t of list) {
    const b = document.createElement('button');
    b.type = 'button'; b.className = 'chip'; b.textContent = t;
    b.onclick = () => onPick(t);
    c.appendChild(b);
  }
}

export function startClock() {
  const tick = () => {
    const d = new Date();
    $('clock').textContent = d.toLocaleTimeString('de-DE');
    $('date').textContent = d.toLocaleDateString('de-DE', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' }).toUpperCase();
  };
  tick(); setInterval(tick, 1000);
}
