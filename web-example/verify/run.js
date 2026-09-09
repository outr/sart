// Verification harness: run the emitted app.js under a tiny DOM stub (NOT part
// of the app bundle) and assert the async startup + render + keydown behaviour.
// Xhr resolves ASYNCHRONOUSLY (setTimeout) so this proves the CPS-lowered
// await chain sequences correctly. Not shipped.
const fs = require('fs'), vm = require('vm'), path = require('path');
function El(tag){ this.tag=tag; this.id=null; this.attrs={}; this.children=[]; this.listeners={}; }
El.prototype.appendChild=function(c){ this.children.push(c); return c; };
El.prototype.setAttribute=function(k,v){ this.attrs[k]=v; };
El.prototype.addEventListener=function(ev,fn){ this.listeners[ev]=fn; };
function TextNode(t){ this.text=String(t); }
const appEl = new El('div'); appEl.id='app';
const document = {
  getElementById:function(id){ return id==='app'?appEl:new El('div'); },
  createElement:function(t){ return new El(t); },
  createTextNode:function(t){ return new TextNode(t); }
};
const store = {};
const localStorage = { getItem:function(k){return k in store?store[k]:null;}, setItem:function(k,v){store[k]=v;} };
// Deferred: same shape as the host runtime the emitter injects into index.html.
function Deferred(){ this.cbs=[]; this.done=false; this.val=undefined; }
Deferred.prototype.onComplete=function(f){ if(this.done){f(this.val);} else {this.cbs.push(f);} };
Deferred.prototype.resolve=function(v){ this.done=true; this.val=v; for(var i=0;i<this.cbs.length;i++){this.cbs[i](this.val);} this.cbs=[]; };
// Fake Xhr: resolves ASYNC (setTimeout) and records call order, to prove the
// awaits sequence and the second fetch sees the first's awaited result.
const order = [];
const Xhr = { get:function(url){ order.push(url); var d=new Deferred(); setTimeout(function(){ d.resolve('tok-'+order.length); }, 5); return d; } };
const sandbox = { document, localStorage, Xhr, Deferred, String, XMLHttpRequest:function(){}, setTimeout, console };
vm.createContext(sandbox);
vm.runInContext(fs.readFileSync(path.join(process.argv[2]),'utf8'), sandbox);

let ok=true; function check(c,m){ if(!c){ok=false;console.error('FAIL: '+m);}else console.log('ok  : '+m); }

// --- synchronous assertions: run before any await resolves ---
const k = appEl.listeners['keydown'];
check(typeof k==='function','keydown registered synchronously, before any await');
check(sandbox.Lite.focus===0,'focus starts 0'); k({keyCode:39}); check(sandbox.Lite.focus===1,'RIGHT->1');
k({keyCode:39}); check(sandbox.Lite.focus===2,'RIGHT->2'); k({keyCode:37}); check(sandbox.Lite.focus===1,'LEFT->1');
check(appEl.children.length===0,'rail NOT rendered yet (awaits still pending)');
check(order.length===1 && order[0]==='/token','first fetch = loadToken /token (fired, unresolved)');

// --- after the async chain drains ---
setTimeout(function(){
  check(order.length===2,'two sequential fetches happened (composition)');
  check(order[1].indexOf('/api/rails?t=tok-1')===0,'second fetch URL used the awaited token (in order)');
  check(appEl.children.length===1,'rail rendered after both awaits resolved');
  const s=appEl.children[0]; check(s&&s.attrs['class']==='rail','section class=rail');
  check(s.children[0].tag==='h2'&&s.children[0].children[0].text==='Continue Watching','heading text');
  const row=s.children[1]; check(row.attrs['class']==='row'&&row.children.length===3,'row has 3 cards');
  const c0=row.children[0];
  check(c0.attrs['data-id']==='1','card0 data-id="1" (Int.toString)');
  check(c0.children[0].attrs['src']==='https://image.tmdb.org/t/p/w342/a.jpg','card0 img src (interp)');
  check(c0.children[1].children[0].text==='Alpha','card0 label=title');
  console.log(ok?'\nALL PASS':'\nSOME FAILED'); process.exit(ok?0:1);
}, 50);
