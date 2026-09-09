// Verification harness: run the emitted app.js under a tiny DOM stub (NOT part
// of the app bundle) and assert the render + keydown behaviour. Not shipped.
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
const Xhr = { get:function(url,onOk,onErr){ onOk('{}'); } };
const sandbox = { document, localStorage, Xhr, String, XMLHttpRequest:function(){}, console };
vm.createContext(sandbox);
vm.runInContext(fs.readFileSync(path.join(process.argv[2]),'utf8'), sandbox);
let ok=true; function check(c,m){ if(!c){ok=false;console.error('FAIL: '+m);}else console.log('ok  : '+m); }
check(appEl.children.length===1,'one rail section appended');
const s=appEl.children[0]; check(s&&s.attrs['class']==='rail','section class=rail');
check(s.children[0].tag==='h2'&&s.children[0].children[0].text==='Continue Watching','heading text');
const row=s.children[1]; check(row.attrs['class']==='row'&&row.children.length===3,'row has 3 cards');
const c0=row.children[0];
check(c0.attrs['data-id']==='1','card0 data-id="1" (Int.toString)');
check(c0.children[0].attrs['src']==='https://image.tmdb.org/t/p/w342/a.jpg','card0 img src (interp)');
check(c0.children[1].children[0].text==='Alpha','card0 label=title');
const k=appEl.listeners['keydown']; check(typeof k==='function','keydown registered');
check(sandbox.Lite.focus===0,'focus 0'); k({keyCode:39}); check(sandbox.Lite.focus===1,'RIGHT->1');
k({keyCode:39}); check(sandbox.Lite.focus===2,'RIGHT->2'); k({keyCode:37}); check(sandbox.Lite.focus===1,'LEFT->1');
console.log(ok?'\nALL PASS':'\nSOME FAILED'); process.exit(ok?0:1);
