"""Erzeugt artifact.html (Seiteninhalt für die private claude.ai-Version) aus index.html."""
import re, pathlib
root = pathlib.Path(__file__).resolve().parent.parent
html = (root / 'index.html').read_text(encoding='utf-8')
head = re.search(r'<head>(.*?)</head>', html, re.S).group(1)
body = re.search(r'<body>(.*?)</body>', html, re.S).group(1)
drop = ('rel="manifest"', 'name="theme-color"', 'apple-', 'mobile-web-app', 'name="viewport"', 'charset=')
head = '\n'.join(l for l in head.splitlines() if not any(d in l for d in drop))
body = re.sub(r'\s*<script>\s*// Installierbare App.*?</script>', '', body, flags=re.S)
# Das claude.ai-Artifact ist bereits privat – dort kein Zugangscode-Schloss
body = re.sub(r'\s*<!-- Zugangscode-Schloss.*?</script>', '', body, count=1, flags=re.S)
out = '<!-- automatisch erzeugt von tools/build_artifact.py – nicht von Hand bearbeiten -->\n' + head.strip() + '\n' + body.strip() + '\n'
(root / 'artifact.html').write_text(out, encoding='utf-8')
print('artifact.html', len(out), 'Zeichen')
