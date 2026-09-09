// Verification harness for the web-lite backend: run the emitted app.js under a
// tiny DOM stub (NOT part of the app bundle) and drive each example app the way
// a user would — click buttons, type into inputs, fire the stopwatch timer,
// navigate screens — asserting the rendered DOM reflects the new state.
//
//   node web-example/verify/run.js out-js/app.js
//
// Covers Counter, Todos, Dice, Stopwatch, Two-screen, plus a Lite async smoke
// test (the CPS-lowered await chain). Exits non-zero if any assertion fails.
const fs = require('fs'), vm = require('vm'), path = require('path');

// ── DOM stub ──────────────────────────────────────────────────────────────
function El(tag) {
  this.tag = tag; this.attrs = {}; this.children = []; this.listeners = {};
  this._text = ''; this.value = ''; this.className = '';
}
El.prototype.appendChild = function(c) { this.children.push(c); return c; };
El.prototype.setAttribute = function(k, v) { this.attrs[k] = String(v); };
El.prototype.addEventListener = function(ev, fn) { (this.listeners[ev] = this.listeners[ev] || []).push(fn); };
Object.defineProperty(El.prototype, 'textContent', {
  get: function() { return this._text; },
  set: function(v) { this._text = String(v); this.children = []; }
});
Object.defineProperty(El.prototype, 'innerHTML', {
  get: function() { return ''; },
  set: function(v) { if (String(v) === '') { this.children = []; this._text = ''; } }
});

const roots = {};
const docListeners = {};
const document = {
  getElementById: function(id) { return findById(document, id) || (roots[id] = roots[id] || new El('#' + id)); },
  createElement: function(t) { return new El(t); },
  createTextNode: function(t) { var e = new El('#text'); e._text = String(t); return e; },
  addEventListener: function(ev, fn) { (docListeners[ev] = docListeners[ev] || []).push(fn); }
};
// Find a node by its `id` attribute anywhere under `roots` (the mounted trees).
function findById(node, id) {
  if (node && node.attrs && node.attrs.id === id) return node;
  var kids = (node && node.children) || [];
  for (var i = 0; i < kids.length; i++) { var r = findById(kids[i], id); if (r) return r; }
  return null;
}
document.getElementById = function(id) {
  for (var k in roots) { var r = findById(roots[k], id); if (r) return r; }
  if (roots[id]) return roots[id];
  roots[id] = new El('#' + id); return roots[id];
};
function fireKey(code) { (docListeners['keydown'] || []).forEach(function(f) { f({ keyCode: code }); }); }
// Viewport spy — records the last id centered into view.
const Viewport = { lastId: null, centerById: function(id) { this.lastId = id; } };
const store = {};
const localStorage = { getItem: function(k) { return k in store ? store[k] : null; }, setItem: function(k, v) { store[k] = String(v); } };

// Async primitive — same shape as the host Deferred the emitter injects.
function Deferred() { this.cbs = []; this.done = false; this.val = undefined; }
Deferred.prototype.onComplete = function(f) { if (this.done) { f(this.val); } else { this.cbs.push(f); } };
Deferred.prototype.resolve = function(v) { this.done = true; this.val = v; for (var i = 0; i < this.cbs.length; i++) { this.cbs[i](this.val); } this.cbs = []; };

// Controllable Timer: capture callbacks, fire them on demand, honour cancel.
const timers = [];
const Timer = { periodic: function(ms, cb) { var t = { cb: cb, cancelled: false }; t.cancel = function() { t.cancelled = true; }; timers.push(t); return t; } };
function tickAll() { for (var i = 0; i < timers.length; i++) { if (!timers[i].cancelled) timers[i].cb(); } }

// Deterministic Random: nextInt(6) always yields 2 → a roll of 3.
const Random = { nextInt: function(b) { return 2 % b; } };

// Async Xhr for the Lite smoke test; records call order.
const order = [];
const Xhr = { get: function(url) { order.push(url); var d = new Deferred(); setTimeout(function() { d.resolve('tok-' + order.length); }, 5); return d; } };

// Launcher reads location.hash at load (deep-link routing); stub it empty.
const location = { hash: '' };
const sandbox = { document, localStorage, Xhr, Deferred, Timer, Random, Viewport, location, String, Math, XMLHttpRequest: function() {}, setTimeout, console };
vm.createContext(sandbox);
vm.runInContext(fs.readFileSync(path.join(process.argv[2]), 'utf8'), sandbox);

// ── helpers ─────────────────────────────────────────────────────────────────
function findById(n, id) {
  if (n.attrs && n.attrs.id === id) return n;
  for (var i = 0; i < n.children.length; i++) { var r = findById(n.children[i], id); if (r) return r; }
  return null;
}
function text(n) { var t = n._text || ''; for (var i = 0; i < n.children.length; i++) t += text(n.children[i]); return t; }
function tagCount(n, tag) { var c = 0; for (var i = 0; i < n.children.length; i++) { if (n.children[i].tag === tag) c++; c += tagCount(n.children[i], tag); } return c; }
function click(n) { var ls = (n.listeners['click'] || []); for (var i = 0; i < ls.length; i++) ls[i]({}); }

let ok = true, group = '';
function check(c, m) { if (!c) { ok = false; console.error('  FAIL [' + group + ']: ' + m); } else console.log('  ok   [' + group + ']: ' + m); }

// ── Counter ───────────────────────────────────────────────────────────────
group = 'Counter';
(function() {
  var root = new El('div'), c = new sandbox.Counter();
  c.mountInto(root);
  check(text(findById(root, 'count')) === 'Count: 0', 'starts at 0');
  click(findById(root, 'inc'));
  check(text(findById(root, 'count')) === 'Count: 1', '+ → 1');
  click(findById(root, 'inc'));
  check(text(findById(root, 'count')) === 'Count: 2', '+ → 2');
})();

// ── Todos ─────────────────────────────────────────────────────────────────
group = 'Todos';
(function() {
  var root = new El('div'), t = new sandbox.Todos();
  t.mountInto(root);
  check(tagCount(findById(root, 'list'), 'li') === 0, 'empty list');
  findById(root, 'todo-input').value = 'milk';
  click(findById(root, 'add'));
  check(tagCount(findById(root, 'list'), 'li') === 1, 'add → 1 item');
  check(text(findById(root, 'list')) === 'milk', 'item text = milk');
  findById(root, 'todo-input').value = 'eggs';
  click(findById(root, 'add'));
  check(tagCount(findById(root, 'list'), 'li') === 2, 'add → 2 items');
  check(text(findById(root, 'list')) === 'milkeggs', 'both items present');
})();

// ── Dice ──────────────────────────────────────────────────────────────────
group = 'Dice';
(function() {
  var root = new El('div'), d = new sandbox.Dice();
  d.mountInto(root);
  check(text(findById(root, 'count')) === 'Rolls: 0', 'no rolls yet');
  click(findById(root, 'roll'));
  check(text(findById(root, 'last')) === 'Last: 3', 'roll → face 3 (seeded)');
  check(text(findById(root, 'count')) === 'Rolls: 1', 'history 1');
  check(tagCount(findById(root, 'history'), 'li') === 1, 'history list 1');
  click(findById(root, 'roll'));
  check(text(findById(root, 'count')) === 'Rolls: 2', 'history 2');
  check(tagCount(findById(root, 'history'), 'li') === 2, 'history list 2');
})();

// ── Stopwatch ───────────────────────────────────────────────────────────────
group = 'Stopwatch';
(function() {
  timers.length = 0;
  var root = new El('div'), s = new sandbox.Stopwatch();
  s.mountInto(root);
  check(text(findById(root, 'elapsed')) === 'Elapsed: 0s', 'starts at 0');
  click(findById(root, 'start'));
  tickAll(); tickAll(); tickAll();
  check(text(findById(root, 'elapsed')) === 'Elapsed: 3s', '3 ticks → 3s');
  click(findById(root, 'stop'));
  tickAll();
  check(text(findById(root, 'elapsed')) === 'Elapsed: 3s', 'stopped → stays 3s');
})();

// ── Two-screen ───────────────────────────────────────────────────────────────
group = 'Two-screen';
(function() {
  roots['app'] = new El('#app');
  sandbox.App.mount('app');            // App.root ← #app, shows Counter
  sandbox.App.show(new sandbox.ScreenA());
  var app = document.getElementById('app');
  check(text(findById(app, 'title')) === 'Home', 'screen A shown');
  click(findById(app, 'go'));
  check(findById(app, 'msg') && text(findById(app, 'msg')) === 'You made it!', 'navigated to B');
  check(text(findById(app, 'title')) === 'Detail', 'B title');
  click(findById(app, 'back'));
  check(text(findById(app, 'title')) === 'Home', 'back to A');
})();

// ── Media (Lite Component): D-pad focus follows + scrolls into view ──────────
group = 'Media';
roots['media'] = new El('#media-root');
docListeners['keydown'] = [];
var media = new sandbox.Lite();
media.mountInto(roots['media']);
check(findById(roots['media'], 'poster-focus') !== null, 'a poster is focused on mount');
check(findById(roots['media'], 'poster-focus').attrs['data-id'] === '1', 'focus starts on card 1');
Viewport.lastId = null;
fireKey(39); // right
check(findById(roots['media'], 'poster-focus').attrs['data-id'] === '2', 'right → focus card 2');
check(Viewport.lastId === 'poster-focus', 'right → focused poster scrolled into view');
fireKey(39); // right
check(findById(roots['media'], 'poster-focus').attrs['data-id'] === '3', 'right → focus card 3');
fireKey(37); // left
check(findById(roots['media'], 'poster-focus').attrs['data-id'] === '2', 'left → focus card 2');

// ── Lite async smoke test (CPS await chain) ──────────────────────────────────
group = 'Lite-async';
roots['app'] = new El('#app');
order.length = 0;
sandbox.LiteAsync.start();
check(order.length === 1 && order[0] === '/token', 'first await fires /token, unresolved');
check(tagCount(roots['app'], 'div') === 0, 'rail not rendered before awaits resolve');
setTimeout(function() {
  check(order.length === 2, 'two sequential fetches (composition)');
  check(order[1].indexOf('/api/rails?t=tok-1') === 0, 'second fetch used awaited token');
  check(tagCount(roots['app'], 'div') > 0, 'rail rendered after awaits resolved');
  console.log(ok ? '\nALL PASS' : '\nSOME FAILED');
  process.exit(ok ? 0 : 1);
}, 50);
