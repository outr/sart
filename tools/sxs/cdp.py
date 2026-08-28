"""Minimal Chrome DevTools Protocol driver for side-by-side screenshotting
of Flutter web apps (CanvasKit renders to a canvas, so we enable Flutter's
semantics tree and drive the app via aria labels + coordinates).

Usage as a library:
    b = Browser(port=9333)          # launches headless Chrome
    p = b.page()                    # one target
    p.goto("http://localhost:8090/login")
    p.wait(1500)
    p.enable_semantics()
    p.screenshot("login.png")
    p.click_label("Email")          # click semantics node by label
    p.type_text("matt@outr.com")
"""
import base64, json, os, subprocess, time, urllib.request, sys
import asyncio
import websockets

CHROME = "/usr/bin/google-chrome"
WIDTH, HEIGHT = 1440, 900


class Browser:
    def __init__(self, port=9333, profile=None):
        self.port = port
        self.profile = profile or f"/tmp/sxs-chrome-{port}"
        import shutil
        shutil.rmtree(self.profile, ignore_errors=True)   # fresh session every run
        os.makedirs(self.profile, exist_ok=True)
        self.proc = subprocess.Popen([
            CHROME, "--headless=new", f"--remote-debugging-port={port}",
            f"--user-data-dir={self.profile}", "--no-first-run", "--no-default-browser-check",
            f"--window-size={WIDTH},{HEIGHT}", "--hide-scrollbars", "--force-device-scale-factor=1",
            "--enable-unsafe-swiftshader", "--use-gl=angle", "--use-angle=swiftshader",
            "--ignore-gpu-blocklist", "--no-sandbox", "--disable-dev-shm-usage", "about:blank",
        ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        for _ in range(100):
            try:
                urllib.request.urlopen(f"http://127.0.0.1:{port}/json/version", timeout=1).read()
                break
            except Exception:
                time.sleep(0.2)
        else:
            raise RuntimeError("chrome did not start")

    def page(self):
        data = json.loads(urllib.request.urlopen(f"http://127.0.0.1:{self.port}/json/new?about:blank", timeout=5).read()) \
            if False else None
        req = urllib.request.Request(f"http://127.0.0.1:{self.port}/json/new?about:blank", method="PUT")
        data = json.loads(urllib.request.urlopen(req, timeout=5).read())
        pg = Page(data["webSocketDebuggerUrl"])
        self._pages = getattr(self, "_pages", []) + [pg]
        return pg

    def close(self):
        for pg in getattr(self, "_pages", []):
            pg.close()
        self.proc.terminate()
        try:
            self.proc.wait(5)
        except Exception:
            self.proc.kill()


class Page:
    def __init__(self, ws_url):
        self.ws_url = ws_url
        self.loop = asyncio.new_event_loop()
        self.ws = self.loop.run_until_complete(websockets.connect(ws_url, max_size=64 * 1024 * 1024))
        self.msg_id = 0
        self.console = []
        self.send("Page.enable")
        self.send("Runtime.enable")
        self.send("Log.enable")
        self.send("Network.enable")
        self.requests = {}
        self.send("Emulation.setDeviceMetricsOverride",
                  width=WIDTH, height=HEIGHT, deviceScaleFactor=1, mobile=False)
        self.send("Page.addScriptToEvaluateOnNewDocument", source="""
          window.__rejections = [];
          window.addEventListener('unhandledrejection', e => {
            try { window.__rejections.push(String(e.reason && (e.reason.stack || e.reason.message || e.reason))); }
            catch (_) { window.__rejections.push('?'); }
          });
          window.addEventListener('error', e => { window.__rejections.push('error: ' + e.message); });
        """)

    def rejections(self):
        return self.eval("JSON.stringify(window.__rejections || [])")

    def close(self):
        try:
            self.loop.run_until_complete(self.ws.close())
        except Exception:
            pass

    # ---- protocol -------------------------------------------------------
    async def _send(self, method, params):
        self.msg_id += 1
        mid = self.msg_id
        await self.ws.send(json.dumps({"id": mid, "method": method, "params": params}))
        while True:
            raw = await self.ws.recv()
            msg = json.loads(raw)
            if msg.get("id") == mid:
                if "error" in msg:
                    raise RuntimeError(f"{method}: {msg['error']}")
                return msg.get("result", {})
            self._event(msg)

    def _event(self, msg):
        m = msg.get("method")
        if m == "Runtime.consoleAPICalled":
            args = msg["params"].get("args", [])
            text = " ".join(str(a.get("value", a.get("description", ""))) for a in args)
            self.console.append(f"[{msg['params'].get('type')}] {text}")
        elif m == "Runtime.exceptionThrown":
            d = msg["params"]["exceptionDetails"]
            self.console.append(f"[exception] {d.get('text')} {d.get('exception', {}).get('description', '')}")
        elif m == "Network.requestWillBeSent":
            r = msg["params"]["request"]
            self.requests[msg["params"]["requestId"]] = [r["method"], r["url"], None]
        elif m == "Network.responseReceived":
            rid = msg["params"]["requestId"]
            if rid in self.requests:
                self.requests[rid][2] = msg["params"]["response"]["status"]
        elif m == "Network.loadingFailed":
            rid = msg["params"]["requestId"]
            if rid in self.requests:
                self.requests[rid][2] = "FAIL " + msg["params"].get("errorText", "")
        elif m == "Log.entryAdded":
            e = msg["params"]["entry"]
            self.console.append(f"[{e.get('level')}] {e.get('text')}")

    def send(self, method, **params):
        return self.loop.run_until_complete(self._send(method, params))

    def pump(self, ms):
        """Sleep while draining events."""
        async def _pump():
            end = time.time() + ms / 1000
            while time.time() < end:
                try:
                    raw = await asyncio.wait_for(self.ws.recv(), timeout=max(0.01, end - time.time()))
                    self._event(json.loads(raw))
                except asyncio.TimeoutError:
                    break
        self.loop.run_until_complete(_pump())

    # ---- high level -----------------------------------------------------
    def goto(self, url, settle=2500):
        self.send("Page.navigate", url=url)
        self.wait(settle)

    def wait(self, ms):
        self.pump(ms)

    def eval(self, js):
        r = self.send("Runtime.evaluate", expression=js, returnByValue=True, awaitPromise=True)
        return r.get("result", {}).get("value")

    def screenshot(self, path):
        r = self.send("Page.captureScreenshot", format="png", captureBeyondViewport=False)
        with open(path, "wb") as f:
            f.write(base64.b64decode(r["data"]))
        return path

    def enable_semantics(self):
        """Flutter web ships a hidden 'Enable accessibility' placeholder;
        activating it materialises the semantics tree as DOM nodes."""
        js = """
        (() => {
          const ph = document.querySelector('flt-semantics-placeholder');
          if (ph) { ph.click(); return 'clicked'; }
          const el = document.querySelector('flt-glass-pane, flutter-view');
          const root = el && el.shadowRoot ? el.shadowRoot : document;
          const p2 = root.querySelector('flt-semantics-placeholder');
          if (p2) { p2.click(); return 'clicked-shadow'; }
          return 'none';
        })()"""
        r = self.eval(js)
        self.wait(800)
        return r

    def _sem_root_js(self):
        return """
        const _roots = [document];
        for (const el of document.querySelectorAll('flt-glass-pane, flutter-view')) {
          if (el.shadowRoot) _roots.push(el.shadowRoot);
        }
        function _all(sel) { return _roots.flatMap(r => Array.from(r.querySelectorAll(sel))); }
        """

    def labels(self):
        js = self._sem_root_js() + """
        return _all('flt-semantics, [role], [aria-label], input, textarea').map(e => ({
          label: e.getAttribute('aria-label') || e.textContent.trim().slice(0, 60),
          role: e.getAttribute('role'), id: e.id,
          r: e.getBoundingClientRect().toJSON()
        })).filter(x => x.label);"""
        return self.eval(f"(() => {{ {js} }})()") or []

    def find_label(self, label, exact=False):
        for n in self.labels():
            l = n["label"] or ""
            if (l == label) if exact else (label.lower() in l.lower()):
                return n
        return None

    def click_xy(self, x, y):
        for t in ("mousePressed", "mouseReleased"):
            self.send("Input.dispatchMouseEvent", type=t, x=x, y=y, button="left", clickCount=1)
        self.wait(300)

    def click_label(self, label, exact=False, settle=600):
        n = self.find_label(label, exact)
        if not n:
            raise RuntimeError(f"label not found: {label!r}; have: {[x['label'] for x in self.labels()][:40]}")
        r = n["r"]
        self.click_xy(r["x"] + r["width"] / 2, r["y"] + r["height"] / 2)
        self.wait(settle)
        return n

    def type_text(self, text):
        self.send("Input.insertText", text=text)
        self.wait(200)

    def key(self, key, code=None, keyCode=None):
        params = dict(key=key, code=code or key, windowsVirtualKeyCode=keyCode or 0)
        self.send("Input.dispatchKeyEvent", type="keyDown", **params)
        self.send("Input.dispatchKeyEvent", type="keyUp", **params)
        self.wait(200)

    def url(self):
        return self.eval("location.href")
