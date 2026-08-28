"""Static server with SPA fallback: serves files from ROOT, and any path
without a file extension falls back to index.html (deep links)."""
import http.server, os, sys, functools

root = os.path.abspath(sys.argv[1])
port = int(sys.argv[2])


class H(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **k):
        super().__init__(*a, directory=root, **k)

    def do_GET(self):
        path = self.path.split("?", 1)[0]
        fs = os.path.join(root, path.lstrip("/"))
        if not os.path.isfile(fs) and ("." not in os.path.basename(path) or path.endswith(".html")):
            self.path = "/index.html"
        return super().do_GET()

    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def log_message(self, *a):
        pass


http.server.ThreadingHTTPServer.allow_reuse_address = True
http.server.ThreadingHTTPServer(("127.0.0.1", port), H).serve_forever()
