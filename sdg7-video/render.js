// Rendert animation.html Bild für Bild und baut daraus mit ffmpeg ein MP4.
// Nutzung:
//   node render.js stills 2 12 50          -> PNG-Standbilder zu den Zeiten (Sekunden)
//   node render.js video [fps] [scale]      -> sdg7-video.mp4 (Standard 30 fps, 1920x1080)
const { chromium } = require('playwright');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

const DIR = __dirname;
const URL = 'file://' + path.join(DIR, 'animation.html');
const TM = JSON.parse(fs.readFileSync(path.join(__dirname, 'timemap.json'), 'utf8'));
const DUR = TM.dur;

async function openPage(browser, scale) {
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: scale });
  await page.addInitScript(() => { window.RENDER_MODE = true; });
  await page.goto(URL);
  await page.evaluate(() => document.fonts.ready);
  await page.evaluate(() => Promise.all([...document.fonts].map(f => f.load())));
  return page;
}

async function stills(times) {
  const browser = await chromium.launch();
  const page = await openPage(browser, 1);
  fs.mkdirSync(path.join(DIR, 'stills'), { recursive: true });
  for (const t of times) {
    await page.evaluate(t => window.renderFrame(t), t);
    await page.locator('#stage').screenshot({ path: path.join(DIR, 'stills', `t${String(t).padStart(6, '0')}.png`) });
  }
  await browser.close();
}

async function renderChunk(browser, scale, fps, from, to, file) {
  const page = await openPage(browser, scale);
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(fps), '-c:v', 'mjpeg', '-i', '-',
    '-c:v', 'libx264', '-preset', 'medium', '-crf', '20', '-pix_fmt', 'yuv420p', file], { stdio: ['pipe', 'inherit', 'inherit'] });
  for (let i = from; i < to; i++) {
    await page.evaluate(T => window.renderFrame(window.mapT(T)), i / fps);
    const buf = await page.locator('#stage').screenshot({ type: 'jpeg', quality: 95 });
    if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
  }
  ff.stdin.end();
  await new Promise(r => ff.on('close', r));
  await page.close();
}

async function video(fps = 30, scale = 1.5, workers = 4) {
  const total = Math.round(DUR * fps);
  const browser = await chromium.launch();
  const tmp = path.join(DIR, '.chunks');
  fs.mkdirSync(tmp, { recursive: true });
  const per = Math.ceil(total / workers);
  const files = [];
  const jobs = [];
  for (let w = 0; w < workers; w++) {
    const file = path.join(tmp, `chunk${w}.mp4`);
    files.push(file);
    jobs.push(renderChunk(browser, scale, fps, w * per, Math.min(total, (w + 1) * per), file));
  }
  const t0 = Date.now();
  await Promise.all(jobs);
  await browser.close();
  console.log('Frames gerendert in', ((Date.now() - t0) / 1000).toFixed(0), 's');
  fs.writeFileSync(path.join(tmp, 'list.txt'), files.map(f => `file '${f}'`).join('\n'));
  const args = ['-y', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', path.join(tmp, 'list.txt')];
  const music = fs.existsSync(path.join(DIR, 'mix.wav')) ? path.join(DIR, 'mix.wav') : path.join(DIR, 'music.wav');
  if (fs.existsSync(music)) args.push('-i', music, '-c:a', 'aac', '-b:a', '160k', '-shortest');
  args.push('-c:v', 'copy', '-movflags', '+faststart', path.join(DIR, 'sdg7-video.mp4'));
  await new Promise((res, rej) => spawn('ffmpeg', args, { stdio: 'inherit' }).on('close', c => c ? rej(c) : res()));
  fs.rmSync(tmp, { recursive: true, force: true });
  console.log('Fertig: sdg7-video.mp4');
}

const [mode, ...rest] = process.argv.slice(2);
if (mode === 'stills') stills(rest.map(Number));
else video(Number(rest[0]) || 30, Number(rest[1]) || 1.5);
