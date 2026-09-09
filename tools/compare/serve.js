// Compare server: serves the Flutter Web build and the Sart web-lite build
// side by side behind one origin, plus a live size readout, so you can flip
// between them and see the difference.
//
//   Flutter Web : out/build/web   (sbt sartWeb)   -> /flutter/
//   web-lite    : out-js          (sbt sartEmitJs)-> /lite/
//   compare UI  : tools/compare/compare.html      -> /
//
// Usage: node tools/compare/serve.js [port]   (default 8099)
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
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.map': 'application/json', '.txt': 'text/plain'
};
function mimeOf(f) { return MIME[p.extname(f).toLowerCase()] || 'application/octet-stream'; }

function dirBytes(dir) {
  let total = 0;
  let stack = [dir];
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
    if (err) { res.writeHead(404, { 'content-type': 'text/plain' }); res.end('not found: ' + file); return; }
    res.writeHead(200, { 'content-type': mimeOf(file) });
    res.end(buf);
  });
}

function safeJoin(base, urlPath) {
  const rel = decodeURIComponent(urlPath).replace(/^\/+/, '');
  const full = p.normalize(p.join(base, rel));
  return full.startsWith(base) ? full : null; // no path traversal out of base
}

http.createServer((req, res) => {
  let u = req.url.split('?')[0];
  if (u === '/' || u === '/index.html') return serveFile(res, UI);
  if (u === '/sizes.json') {
    res.writeHead(200, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ flutter: dirBytes(FLUTTER), lite: dirBytes(LITE) }));
    return;
  }
  if (u.startsWith('/flutter/')) {
    let f = safeJoin(FLUTTER, u.slice('/flutter'.length));
    if (f && fs.existsSync(f) && fs.statSync(f).isDirectory()) f = p.join(f, 'index.html');
    return f ? serveFile(res, f) : res.end('bad path');
  }
  if (u.startsWith('/lite/')) {
    let f = safeJoin(LITE, u.slice('/lite'.length));
    if (f && fs.existsSync(f) && fs.statSync(f).isDirectory()) f = p.join(f, 'index.html');
    return f ? serveFile(res, f) : res.end('bad path');
  }
  res.writeHead(404, { 'content-type': 'text/plain' });
  res.end('not found');
}).listen(port, () => {
  const have = d => fs.existsSync(d) ? 'ok' : 'MISSING — build it first';
  console.log('compare server: http://localhost:' + port + '/');
  console.log('  /flutter/  <- ' + FLUTTER + '  (' + have(FLUTTER) + ')');
  console.log('  /lite/     <- ' + LITE + '  (' + have(LITE) + ')');
});
