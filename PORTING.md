# Porting LogicalNetwork and NaboTV to Sart

The practical companion to [ROADMAP.md](ROADMAP.md): what's ready today,
what to build next, and the concrete first steps for each app port.

## What's ready right now (2026-08-19)

- **Toolchain**: sbt 2.0.1, Scala 3.8.4, Flutter 3.44.8 / Dart 3.12,
  `sbt-sart` cross-built for sbt 1.x and 2.x. All CI gates green
  (61 emitter tests + goldens).
- **The LogicalNetwork port is complete** — see the LN section below.
  Everything it needed is in the emitter and facade set, so the playbook
  is now a record of how, not a plan.
- **Generated facades**: `flutter-facades` is ~360 declarations, ~320
  generated from the real Flutter SDK by `sbt sartFacadesRegen` from the
  `facadegen*.conf` manifests (full `Icons`/`Colors`, real constructor
  signatures). **Adding a Flutter class is a one-line `keep` + regen.**
- **facadegen v2**: resolved-AST analysis; `context`/`library` config
  modes resolve pub packages through the consuming app's analysis
  context and emit **subclassable** facades (`DataGridSource`). Generics
  are the remaining gap — curate those by hand (`@native` class +
  `@DartImport`/`@DartPackage` in app code; `app-sart`'s
  `facades_packages.scala` is the template for ~10 packages).
- **Emitter**: everything in the README's "what works" list — direct
  async/await, statement forms, `@JsonModel` codecs incl. sealed
  `@JsonTag` dispatch, named/default params with `$default$` getters,
  mixins, `late`/`lazy`, bitwise, `Dyn`, extension calls, and the
  port-driven lowerings (`m(k)`, `slice`, `Future.foreach`, …).
- **User-authored facades**: `@native` objects/classes in app code
  (annotations recorded, no Dart leakage); `@DartPackage` feeds pubspec.

## Not built yet — the gating work, in build order

These are roadmap Workstreams A–E; the order below is the practical
dependency order for the ports. Build each against fixtures first
(`compiler/src/test/scala/sart/compiler/fixtures/`), keep
`sbt compiler/testFull sartGoldenVerify` green, and let `--strict`
find what's missing.

1. ~~Named-parameter emission~~ **✅ landed** (literal defaults; the
   non-literal-default follow-up is default-getter emission).
2. ~~Super-parameter forwarding & multi-parent emission~~ **✅ landed**
   (`super.key` with a real parent chain, `with` mixins, `mixin X on P`).
3. ~~Sealed-hierarchy JSON codecs~~ **✅ landed** (`@JsonModel` +
   `@JsonTag` dispatch, `copyWith`, Map/List/Option fields, `@JsonField`
   renames) — LN's whole wire layer runs on them. The *shared Scala
   module* half is still open (LN consolidates models by hand).
4. ~~`Dyn` type~~ **✅ landed** (with `sart.stdlib.convert`; richer
   `Map[String, Dyn]` ergonomics as needed).
5. ~~go_router facade~~ **✅ landed** (curated — its `FutureOr` callback
   types defeat the generator).
6. **`const` inference** — start early, validate on real hardware; it's
   a perf cliff, not a correctness one.
7. **Overload disambiguation, `@DartCovariant`** — round out the 0.2
   milestone (switch statements and getter-emission rules landed).
8. **Conditional-export mechanism** (14 sites in nabo, 2 in
   outr_flutter) and `package:web`/JS-interop facades — needed by 0.5+.

## LogicalNetwork port (first target — milestone 0.6) — ✅ code-complete

**Status (2026-08-19):** every Dart file in `app/lib` and `outr_flutter/lib`
has a line-by-line Scala twin in `logicalnetwork-server/app-sart/`
(153 files, 33k lines → 65k lines of Dart). Zero hand-written Dart,
zero stubs, `flutter analyze` clean, `flutter build web` green, all 114
endpoints + ~230 models verified field-for-field against the wire.
A method-by-method review pass (eight parallel reviewers) fixed ~50
fidelity divergences before runtime testing. **Open gates:** runtime
verification against the server, `--wasm`, deployment. Three bugs in
the Dart original were found along the way (a dead `args is
PickerDateRange` guard in the date-range filter, a tooltip interpolating
a whole `MapEntry`, a sort-description lookup keyed by the prefixed
name) — the port implements the evident intent, with comments.

How it went, step by step (kept as the playbook for nabo):

**Step 0 — vertical slice.** `app-sart` as an sbt-sart project; login
screen first (MaterialApp/theme, TextField, buttons, async service
call, navigation). `sbt sartAnalyze` until clean; every `/* TODO */` in
the output became an emitter work item.

**Step 1 — models.** Rather than the shared-module play, all models were
consolidated by hand in one `models.scala` (the originals are
machine-generated, so a single file mirrors them fine): `@JsonModel`
case classes, wire-string `object`s for enums, `@JsonTag` sealed
hierarchies. Wire format byte-matches. The shared module + retiring
`GenerateDart.scala` remain roadmap items.

**Step 2 — service layer.** `service.scala` mirrors `service.dart`
method-for-method over facaded `package:http` (`restful`/`restGet`/
`restPost`/`restDownload` + FileSaver, and `multiPart` over the
http `MultipartRequest` facade). The `transport` hook is the one thing
unported.

**Step 3 — outr_flutter → Scala.** Ported as `app-sart`'s `outr`
package (`Application`, `Screen` as `mixin Screen on StatelessWidget`,
`MessageService` as a singleton registered from `Application.init`
instead of provider — the one structural collapse — with the real
WebSocket, reconnect/ping timers, and chunked file upload, `CustomForm`,
`LoadingWidget`, `TableResults`, `Persistent` on `localStorage`).
Conditional imports collapsed to their web branches (web-only app).

**Step 4 — screens, easiest-first.** Static screens → the search/filter
stack (23 `FilterSupport`s + editors) → the Syncfusion grids (generated
subclassable `DataGridSource`, server-side sort, infinite scroll) →
entity views (13 dialect views) → the explorers (`network_explorer`,
`entity_relationship_graph`: `CustomPainter`/`Canvas`, `Matrix4`,
`InteractiveViewer`, `AnimationController`, seeded `Random`) → maps →
export editors → admin → startup fidelity (Sentry, feedback,
url strategy). Mechanical conversions farmed out to parallel agents
with an exemplar file + idiom sheet; near-zero compile fallout.

**Port idioms that stuck** (all in the README's "what works"): enums as
wire strings, `Option` for nullable (`sart.stdlib.Option` where a facade
needs a primitive `T?`), `var x: T = null` for `late`, `lazy val` for
`late final`, reassigned immutable collections for Dart's in-place
mutation, hoisted private methods for local closures, explicit element
types (`List[SortColumnDetails]()`) for any list a facade mutates —
Dart covariance-checks writes through `<Never>[]`.

**Facades LN uses beyond Flutter**: syncfusion (datagrid generated;
maps + datepicker curated), go_router, flex_color_scheme, dropdown_button2,
intl, email_validator, shimmer, flutter_spinkit, flutter_svg (all
generated); http, web, web_socket_channel, overlay_support,
flutter_markdown_plus, animated_tree_view, flutter_multi_select_items,
file_picker, file_saver, feedback, sentry_flutter, logger,
flutter_web_plugins (curated in `facades.scala`/`facades_packages.scala`).

**Verification without tests**: LN has no test suite. What worked:
(a) `flutter analyze` at zero errors as a hard gate after every wave,
(b) the review pass — reviewers reading Scala and Dart side by side
caught ~50 real divergences the compiler couldn't, (c) next: runtime
side-by-side per screen against the server.

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
sbt compiler/testFull            # emitter unit tests (61)
sbt sartGoldenVerify             # emission regression gate
sbt sartAnalyze                  # emit + flutter analyze, errors mapped to Scala
sbt sartFacadesRegen             # re-derive flutter facades from the SDK
sbt sartPublishLocalAll          # publish everything for consumer projects
sbt ~sartDev                     # hot-reload dev loop

# Facades for a new Flutter SDK class: add a `file`/`keep` line to
# flutter-facades/facadegen.conf, run sartFacadesRegen, done.

# Facades for a pub package: write a facadegen-<pkg>.conf in the
# consuming app (`context <appDir>` + `library <package:uri>` resolve
# through the app's analysis context — see app-sart's
# facadegen-syncfusion.conf), then:
sbt 'sart-facadegen/runMain sart.facadegen.Main --config facadegen-<pkg>.conf'
```
