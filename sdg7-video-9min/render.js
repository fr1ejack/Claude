// Rendert index.html Bild für Bild zu einem MP4.
//   node render.js check                 -> Kontaktbögen (je Szene mehrere Standbilder) in ./stills
//   node render.js video [fps] [scale]   -> sdg7-video-9min.mp4
const { chromium } = require('playwright');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const DIR = __dirname;
const URL = 'file://' + path.join(DIR, 'index.html');

async function openPage(browser, scale) {
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: scale });
  page.on('pageerror', e => { console.error('JS-Fehler:', e.message); process.exitCode = 1; });
  await page.addInitScript(() => { window.RENDER_MODE = true; });
  await page.goto(URL);
  await page.evaluate(() => Promise.all([...document.fonts].map(f => f.load())));
  return page;
}

async function check(only) {
  const browser = await chromium.launch();
  const page = await openPage(browser, 1);
  const scenes = await page.evaluate(() => TL.scenes.map(s => ({ id: s.id, start: s.start, dur: s.dur, cues: s.cues, lens: s.lens })));
  fs.mkdirSync(path.join(DIR, 'stills'), { recursive: true });
  for (const sc of scenes) {
    if (only && !only.includes(sc.id)) continue;
    const times = [sc.dur * 0.15, ...sc.cues.map((c, i) => c + sc.lens[i] * 0.85), sc.dur - 0.45];
    const shots = [];
    for (const t of times) {
      try { await page.evaluate(T => window.renderFrame(T), sc.start + t); }
      catch (e) { console.error(sc.id, t.toFixed(1), e.message); process.exitCode = 1; }
      shots.push(await page.locator('#stage').screenshot());
    }
    // zu einem Bogen zusammensetzen (Browser macht das Layout)
    const html = `<body style="margin:0;background:#000;display:grid;grid-template-columns:repeat(2,640px);gap:4px">${shots.map(b => `<img style="width:640px" src="data:image/png;base64,${b.toString('base64')}">`).join('')}</body>`;
    const p2 = await browser.newPage({ viewport: { width: 1284, height: 400 } });
    await p2.setContent(html);
    await p2.screenshot({ path: path.join(DIR, 'stills', `${String(scenes.indexOf(sc)).padStart(2, '0')}_${sc.id}.png`), fullPage: true });
    await p2.close();
  }
  await browser.close();
}

async function renderChunk(browser, scale, fps, from, to, file) {
  const page = await openPage(browser, scale);
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(fps), '-c:v', 'mjpeg', '-i', '-',
    '-c:v', 'libx264', '-preset', 'medium', '-crf', '21', '-pix_fmt', 'yuv420p', file], { stdio: ['pipe', 'inherit', 'inherit'] });
  for (let i = from; i < to; i++) {
    await page.evaluate(t => window.renderFrame(t), i / fps);
    const buf = await page.locator('#stage').screenshot({ type: 'jpeg', quality: 93 });
    if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
  }
  ff.stdin.end();
  await new Promise(r => ff.on('close', r));
  await page.close();
}

async function video(fps = 30, scale = 1.5, workers = 4) {
  const browser = await chromium.launch();
  const probe = await openPage(browser, 1);
  const DUR = await probe.evaluate(() => TL.dur);
  await probe.close();
  const total = Math.round(DUR * fps);
  const tmp = path.join(DIR, '.chunks');
  fs.mkdirSync(tmp, { recursive: true });
  const per = Math.ceil(total / workers), files = [], jobs = [];
  for (let w = 0; w < workers; w++) {
    const file = path.join(tmp, `chunk${w}.mp4`); files.push(file);
    jobs.push(renderChunk(browser, scale, fps, w * per, Math.min(total, (w + 1) * per), file));
  }
  const t0 = Date.now();
  await Promise.all(jobs);
  await browser.close();
  console.log('Frames gerendert in', ((Date.now() - t0) / 1000).toFixed(0), 's');
  fs.writeFileSync(path.join(tmp, 'list.txt'), files.map(f => `file '${f}'`).join('\n'));
  const args = ['-y', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', path.join(tmp, 'list.txt')];
  const audio = ['mix.wav', 'music.wav'].map(f => path.join(DIR, f)).find(f => fs.existsSync(f));
  if (audio) args.push('-i', audio, '-c:a', 'aac', '-b:a', '160k', '-shortest');
  args.push('-c:v', 'copy', '-movflags', '+faststart', path.join(DIR, 'sdg7-video-9min.mp4'));
  await new Promise((res, rej) => spawn('ffmpeg', args, { stdio: 'inherit' }).on('close', c => c ? rej(c) : res()));
  fs.rmSync(tmp, { recursive: true, force: true });
  console.log('Fertig: sdg7-video-9min.mp4');
}

const [mode, ...rest] = process.argv.slice(2);
if (mode === 'check') check(rest.length ? rest : null);
else video(Number(rest[0]) || 30, Number(rest[1]) || 1.5);
