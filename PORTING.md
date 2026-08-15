# Porting LogicalNetwork and NaboTV to Sart

The practical companion to [ROADMAP.md](ROADMAP.md): what's ready today,
what to build next, and the concrete first steps for each app port.

## What's ready right now (2026-08)

- **Toolchain**: sbt 2.0.1, Scala 3.8.4, Flutter 3.44.8 / Dart 3.12,
  `sbt-sart` cross-built for sbt 1.x and 2.x. All CI gates green.
- **Generated facades**: `flutter-facades` is now mostly *generated from
  the real Flutter SDK*. `sbt sartFacadesRegen` re-derives
  `material_generated.scala` (~98 declarations incl. the full `Icons` and
  `Colors` catalogs, real constructor signatures with `required`/optional
  fidelity) from the manifest in `flutter-facades/facadegen.conf`. The
  curated core (`material.scala`) is down to 15 semantic declarations.
  **Adding a Flutter class to the facade set is now a one-line config
  change + regen**, not hand-authoring.
- **facadegen v2** (`sart-facadegen`): resolved-AST analysis — sees
  `super.`-parameters with real types, named/factory constructors,
  `required`/default-ness, typedef-expanded function types, and ancestor
  chains (unknown types collapse to the nearest facaded supertype).
  Works against SDK sources and, in principle, any pub package source.
- **Emitter**: everything in the README's "what works" list, plus (new)
  `while` loops, `lazy val` → `late final`, `Future.successful/failed`,
  **named/required/default parameters for user-defined code** (literal
  defaults become Dart named sections on methods and constructors, call
  sites shaped to match, `.copy` → `copyWith`), and the **`Dyn` dynamic
  bridge** (`sart.dart.Dyn` → `dynamic`, `d(key)`/`d.str`/`d.toInt`/
  `d.isNull` lowerings) with `sart.stdlib.convert` (`jsonEncode`/
  `jsonDecode`) for the wire boundary.
- **User-authored facades**: `@native` objects/classes declared in app
  code work (annotations recorded, no Dart leakage) — `app-sart`'s
  `package:http` facade is the template, and its `@DartPackage`
  annotation feeds the emitted pubspec. The login slice now POSTs the
  real `/authenticate` JSON protocol through it.

## Not built yet — the gating work, in build order

These are roadmap Workstreams A–E; the order below is the practical
dependency order for the ports. Build each against fixtures first
(`compiler/src/test/scala/sart/compiler/fixtures/`), keep
`sbt compiler/testFull sartGoldenVerify` green, and let `--strict`
find what's missing.

1. ~~Named-parameter emission~~ **✅ landed** (literal defaults; the
   non-literal-default follow-up is default-getter emission).
2. **Super-parameter forwarding & multi-parent emission** (`super.key`,
   `implements`/`with`, `: super(...)` initializers) — needed the moment
   a ported widget subclasses another ported widget or applies a
   framework mixin (`mixin Screen on StatelessWidget` in outr_flutter).
3. **Sealed-hierarchy JSON codecs** (`fromJson` tag dispatch, `toJson`
   with `map['type']`, `copyWith`, `props`, `deepClone`) byte-matching
   the existing generated model wire shape — unlocks replacing LN's
   `lib/model/**` + nabo's model layer with the shared Scala module.
4. ~~`Dyn` type~~ **✅ landed** (with `sart.stdlib.convert`; richer
   `Map[String, Dyn]` ergonomics as needed).
5. **go_router facade** (generate via facadegen) — LN's 14 routes and
   the app shell.
6. **`const` inference** — start early, validate on real hardware; it's
   a perf cliff, not a correctness one.
7. **Overload disambiguation, switch statements, getter-emission rules,
   `@DartCovariant`** — round out the 0.2 milestone.
8. **Conditional-export mechanism** (14 sites in nabo, 2 in
   outr_flutter) and `package:web`/JS-interop facades — needed by 0.5+.

## LogicalNetwork port (first target — milestone 0.6)

Why first: web-only, Material-only, zero native code, zero platform
channels, conservative Dart subset. ~88% of its Dart is already
generated from the Scala server, so the port is mostly *deleting* a
codegen pipeline.

**Step 0 — vertical slice (do this first).** Stand up
`logicalnetwork-server/app-sart/` as an sbt-sart project (local publish:
`sbt sartPublishLocalAll` in the Sart repo, then the two-line plugin
setup from `sbt-sart/README.md`). Recreate the **login screen** in pure
Scala. It exercises: MaterialApp/theme, TextField + controllers, buttons,
async service call, navigation. Run `sbt sartAnalyze` and iterate until
clean. Every `/* TODO */` in the output is an emitter work item — file it
against the list above.

**Step 1 — the shared model module.** Move
`com.logicalnetwork.model.*` (already Scala on the server!) into a
`logicalnetwork-core` module both the server and the Sart app depend on.
Requires item 3 above. Acceptance: emitted Dart models byte-match the
wire behavior of today's `lib/model/**` (round-trip a captured JSON
corpus through both). Then retire `GenerateDart.scala`.

**Step 2 — service layer.** `Service` is 120 one-liner statics over 5
transport primitives. Facade the primitives (`http` package or a small
Dart shim), emit the statics from shared Scala. The `transport` hook
seam means you can run *mixed*: ported screens call the Sart service,
unported screens keep the old one.

**Step 3 — outr_flutter → Scala** (`outr-flutter` as a published Sart
library). 21 files / 2.3k lines: `Application`, `Screen` (a Dart mixin —
needs emitter mixin support), `MessageService` (Provider-backed — facade
`provider` or replace with a tiny Scala-side registry), `CustomForm`,
`LoadingWidget`, `TableResults`. The 2 conditional imports here need the
conditional-export mechanism.

**Step 4 — screens, easiest-first.** 12 screens, 105 widgets. Suggested
order: static/settings screens → search/filter stack (17 `FilterSupport`
subclasses, mechanical) → `unified_entities_*` (Syncfusion DataGrid —
needs facadegen for `syncfusion_flutter_datagrid` incl. **subclassable**
`DataGridSource`) → last the two graph monsters (`network_explorer`,
`entity_relationship_graph`: CustomPainter, Canvas/Path, Matrix4,
InteractiveViewer, AnimationController — and audit `Int` semantics:
Scala 32-bit vs web 53-bit in node-ID hashing).

**Facades LN needs** (beyond Flutter): syncfusion (datagrid, core,
datepicker, charts, maps), go_router, flex_color_scheme, iconly, intl,
http, overlay_support, equatable-free (subsumed), animated_tree_view,
dropdown_button2, sentry_flutter, file_picker/file_saver,
flutter_multi_select_items, flutter_markdown_plus, flutter_svg, web.
Generate, don't hand-write: point facadegen at each package's source in
the pub cache (`~/.pub-cache/hosted/pub.dev/<pkg>/lib/`). The missing
convenience is a `sartFacadePackages` task that resolves versions and
paths automatically — worth building when the second package gets
tedious.

**Verification without tests**: LN has no test suite. Use (a) the JSON
round-trip corpus for models, (b) `flutter build web --wasm` +
side-by-side manual comparison per screen, (c) the `Service.transport`
seam to ship hybrid builds during the port.

## NaboTV port (second target — milestones 0.7–0.9)

Prerequisites beyond LN: conditional exports at scale (14 sites),
`dart:js_interop` extension-type facades (6 web files), platform
channels (17 — facade `MethodChannel`/`EventChannel`, keep the Kotlin/
Swift side as-is), media_kit + webview + the tvOS federated plugins.

Suggested order:
1. **Model layer** — same shared-module play as LN (44% of the app).
2. **`lib/platform/`** — the conditional-import backends; this is where
   the platform-variant mechanism gets proven.
3. **Screens** easiest-first; the TV focus engine (`lib/core/focus/`,
   heavy `LogicalKeyboardKey`/`KeyEventResult` use) mid-way; `lib/games/`
   (embedded JS runtime, string-keyed bridge — the least statically
   typeable code) last.
4. **`const` validation on TV hardware** as soon as the first real
   screen runs — don't defer to 0.9.

## Working loop cheat-sheet

```bash
# In the Sart repo
sbt compiler/testFull            # emitter unit tests (42)
sbt sartGoldenVerify             # emission regression gate
sbt sartAnalyze                  # emit + flutter analyze, errors mapped to Scala
sbt sartFacadesRegen             # re-derive flutter facades from the SDK
sbt sartPublishLocalAll          # publish everything for consumer projects
sbt ~sartDev                     # hot-reload dev loop

# Facades for a new Flutter SDK class: add a `file`/`keep` line to
# flutter-facades/facadegen.conf, run sartFacadesRegen, done.

# Facades for a pub package (ad-hoc, until the sbt task exists):
sbt 'sart-facadegen/runMain sart.facadegen.Main \
  ~/.pub-cache/hosted/pub.dev/go_router-17.3.0/lib/src/router.dart \
  /tmp/facades package:go_router/go_router.dart'
```
