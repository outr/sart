# sxs — side-by-side pixel comparison for a Sart port

Screenshots a hand-written Flutter web app and its Sart port page by page
in headless Chrome and reports pixel differences. Used to drive the
LogicalNetwork port to pixel identity; generic enough for any port.

Pieces (Python 3, `websockets`, `Pillow`; Chrome at `/usr/bin/google-chrome`):

- `cdp.py` — minimal Chrome DevTools Protocol driver. Launches headless
  Chrome with swiftshader (CanvasKit needs WebGL), fixes the viewport at
  1440×900, enables Flutter's semantics tree (`enable_semantics()`) so the
  canvas UI can be driven by aria labels (`click_label`, `type_text`), and
  records console output, unhandled rejections and network responses.
- `spa_server.py <dir> <port>` — static server with SPA fallback (missing
  paths and missing `*.html` routes serve `index.html`).
- `compare.py [page …]` — diffs `shots/orig/<page>.png` against
  `shots/sart/<page>.png`, prints the differing-pixel percentage and writes
  `shots/diff/<page>.png` (orig | sart | highlighted diff).
- `decode_stack.py <main.dart.js.map> "<stack>"` — resolves minified
  `main.dart.js:L:C` frames through the source map (`flutter build web
  --source-maps`) to Dart lines — the fast way to find what threw behind a
  release-mode grey box.

A crawl script is app-specific: it navigates the routes, logs in, clicks
through tabs/dialogs and calls `page.screenshot(...)` for each state, once
per build (see the LN app-sart `sxs/crawl.py` for the shape).

Things that bit us, so you don't have to rediscover them:

- Build BOTH apps with the same renderer (CanvasKit) and the same package
  versions — pin with `sartPubspecLock`; a minor `flutter_markdown_plus`
  bump moved a docs page by 3px.
- Ship the original app's `web/` folder via `sartWebDir`; a stock
  `index.html` differs in title/meta and any custom scripts.
- Use `localhost`, not `127.0.0.1`, if the app rewrites its API port by
  host name.
- Chrome profile dirs persist logins between runs; `cdp.Browser` wipes its
  profile on start so every crawl begins logged out.
