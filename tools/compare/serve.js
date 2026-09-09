// Compare server: serves the Flutter Web build and the Sart web-lite build so
// you can flip between them side by side.
//
// Each build is served on its OWN port at its OWN root, because Flutter's
// index.html uses `<base href="/">` and requests assets (flutter_bootstrap.js,
// main.dart.js, manifest.json, canvaskit/…) from the origin root — serving it
// under a subpath 404s those. So:
//   compare UI  : tools/compare/compare.html   -> http://localhost:<port>/      (default 8099)
//   Flutter Web : out/build/web                -> http://localhost:<port+1>/
//   web-lite    : out-js                        -> http://localhost:<port+2>/
// compare.html iframes the two app ports (derived from its own port).
//
// Usage: node tools/compare/serve.js [port]
'use strict';
const http = require('http');
const fs = require('fs');
const p = require('path');

const repo = p.resolve(__dirname, '..', '..');
const FLUTTER = p.join(repo, 'out', 'build', 'web');
const LITE = p.join(repo, 'out-js');
const UI = p.join(__dirname, 'compare.html');
const port = parseInt(process.argv[2] || '8099', 10);

const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.mjs': 'text/javascript',
  '.css': 'text/css', '.json': 'application/json', '.wasm': 'application/wasm',
  '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.gif': 'image/gif',
  '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.otf': 'font/otf', '.ttf': 'font/ttf',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.map': 'application/json', '.txt': 'text/plain',
  '.bin': 'application/octet-stream', '.symbols': 'text/plain'
};
const mimeOf = f => MIME[p.extname(f).toLowerCase()] || 'application/octet-stream';

function dirBytes(dir) {
  let total = 0, stack = [dir];
  while (stack.length) {
    const d = stack.pop();
    let entries;
    try { entries = fs.readdirSync(d, { withFileTypes: true }); } catch (e) { continue; }
    for (const e of entries) {
      const full = p.join(d, e.name);
      if (e.isDirectory()) stack.push(full);
      else try { total += fs.statSync(full).size; } catch (e2) {}
    }
  }
  return total;
}

function serveFile(res, file) {
  fs.readFile(file, (err, buf) => {
    if (err) { res.writeHead(404, { 'content-type': 'text/plain' }); res.end('404: ' + file); return; }
    res.writeHead(200, { 'content-type': mimeOf(file) });
    res.end(buf);
  });
}

// A plain static file server rooted at `base` (each app at its own origin root).
function staticServer(base, label) {
  return http.createServer((req, res) => {
    let u = req.url.split('?')[0];
    if (u === '/' || u === '') u = '/index.html';
    const rel = decodeURIComponent(u).replace(/^\/+/, '');
    const full = p.normalize(p.join(base, rel));
    if (!full.startsWith(base)) { res.writeHead(403); res.end('forbidden'); return; }
    fs.stat(full, (err, st) => {
      if (err) { res.writeHead(404, { 'content-type': 'text/plain' }); res.end('404 ' + label + ': ' + u); return; }
      serveFile(res, st.isDirectory() ? p.join(full, 'index.html') : full);
    });
  });
}

// Compare-UI server: the page + a live size readout.
http.createServer((req, res) => {
  const u = req.url.split('?')[0];
  if (u === '/' || u === '/index.html') return serveFile(res, UI);
  if (u === '/sizes.json') {
    res.writeHead(200, { 'content-type': 'application/json', 'access-control-allow-origin': '*' });
    res.end(JSON.stringify({ flutter: dirBytes(FLUTTER), lite: dirBytes(LITE) }));
    return;
  }
  res.writeHead(404, { 'content-type': 'text/plain' }); res.end('not found');
}).listen(port);

staticServer(FLUTTER, 'flutter').listen(port + 1);
staticServer(LITE, 'lite').listen(port + 2);

const have = d => fs.existsSync(d) ? 'ok' : 'MISSING — build it first (sbt sartCompare)';
console.log('compare UI   : http://localhost:' + port + '/');
console.log('Flutter Web  : http://localhost:' + (port + 1) + '/   <- ' + FLUTTER + '  (' + have(FLUTTER) + ')');
console.log('web-lite     : http://localhost:' + (port + 2) + '/   <- ' + LITE + '  (' + have(LITE) + ')');
