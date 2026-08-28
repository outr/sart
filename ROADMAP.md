# Sart 1.0 Roadmap

**North star:** two real client apps build entirely from Scala 3 + Sart
with **zero custom Dart code**. Porting both is the 1.0 acceptance test:

1. **LogicalNetwork** (`~/projects/clients/logicalnetwork/logicalnetwork-server/app`)
   — web-only Flutter frontend of a Scala server. 1,043 Dart files /
   100k lines, of which **~88% is already machine-generated from Scala**
   (`GenerateDart.scala` → `spice.openapi.generator` emits `lib/model/**`
   and `service.dart`). Hand-written surface: ~127 files / 27.5k lines,
   plus the shared `outr_flutter` package (~21 files / 2.3k lines).
2. **NaboTV** (`~/projects/clients/nabo/app/flutter`) — multi-platform
   (web/Android/tvOS). 1,547 Dart files / 159k lines, **44% generated
   models** derived from the Scala server. Hand-written: ~903 files /
   89k lines. Adds platform channels, conditional imports, JS interop,
   an embedded JS runtime, and TV focus handling.

Native host code (nabo's 3.3k lines of Kotlin + Swift) stays — "zero
Dart", not "zero native". LogicalNetwork has no native code at all.

Why this is closer than the line counts suggest:

- Both model layers are *already generated from Scala*. Sart replaces a
  code generator, not hand-written Dart — and subsumes `json_serializable`,
  `copy_with_extension`, `equatable`, and `build_runner` (453 `.g.dart`
  files / 56k lines in LN alone; 644 / 70k in nabo).
- Both apps use a conservative Dart subset: **no isolates, no ffi, no
  mirrors, no operator overloading, no `noSuchMethod`** (1 trivial site),
  and almost no user-declared mixins (one: `mixin Screen on
  StatelessWidget` in outr_flutter). LN additionally uses no pattern
  matching, no records beyond `.indexed`, and no generators.
- The cost concentrates in four places: **bindings at scale** (~300
  external symbols per app), **`const` fidelity**, **named/required/
  default parameters**, and the **`Map<String, dynamic>` wire boundary**.

**Sequencing insight:** LogicalNetwork is the easier full-app proof
(web-only, Material-only, no platform code, tiny language subset) and
shares its server-model pipeline with Sart's shared-core story. Port it
first; nabo second.

## Where we are (2026-08-19)

- Toolchain current: sbt 2.0.1 (root + plugin), Scala 3.8.4, Flutter
  3.44.8 / Dart 3.12. `sbt-sart` cross-builds for sbt 1.x and 2.x.
- Emitter (`compiler/src/main/scala/sart/compiler/DartEmitter.scala`,
  ~3.8k lines): everything in the README's "what works" list — classes,
  case classes with codecs, traits → abstract mixin classes / `mixin on`,
  enums, statement + expression forms, direct-style async/await,
  named/default parameters incl. `$default$` getters, extension methods,
  `late`/`lazy`, bitwise ops, `Dyn`, a ~40-entry stdlib rewrite table
  (List/Map/Option/Set/Range/Tuple/Future/Stream) plus dedicated
  lowerings, Option/Try/Either shims.
- 61 emitter unit tests + golden-file gate + 6-platform CI matrix +
  plugin consumer smoke test (both sbt axes).
- ~360 Flutter facade declarations in `flutter-facades` (material,
  services, gestures, dart:ui), ~320 of them generated from the SDK.
- **The LogicalNetwork reference app is fully ported** (milestone 0.6's
  code-side gate): 153 Scala files / 33k lines, zero hand-written Dart,
  analyzer-clean, `flutter build web` green, every Dart file with a
  line-by-line Scala twin, outr_flutter included. Verified by an
  eight-way method-by-method review pass against the originals before
  runtime testing.

## Workstream A — Language completeness (emitter)

Gaps ranked by combined impact across both apps. Every item lands with
fixtures + unit tests + golden coverage; `--strict` (no `/* TODO */` in
output) becomes the gate.

1. **Named / required / default parameters** — the highest-volume feature
   in both codebases; every Flutter constructor call is named-param.
   Scala default args must emit as Dart named params with defaults, and
   non-nullable-without-default must emit `required`.
2. **`const` inference** — 1,208 `const X(...)` calls in LN's hand-written
   code, 3,856 in nabo; Flutter perf and `flutter_lints`
   (`prefer_const_constructors`) both depend on it. Emit `const`
   constructors on eligible classes and infer call-site `const`-ness
   bottom-up. Highest-risk novel work; start early.
3. **Super-parameter forwarding** — `const Foo({super.key})` is used by
   effectively every widget subclass in both apps; synthesize `super.x`
   forwarding (and general `: super(...)` initializers, currently absent).
4. **Multiple parents → `implements`/`with`** — `realParent` keeps only
   the first parent today. Needed for framework mixin application
   (`TickerProviderStateMixin`, `EquatableMixin` ×246 in LN models,
   `WidgetsBindingObserver`, …) and for trait → Dart `mixin` emission
   with `on` constraints (outr_flutter's `mixin Screen on
   StatelessWidget`). Traits with constructor params can't be Dart
   mixins — detect and reject/rewrite.
5. **Getter vs method emission** — Scala parameterless `def` must emit as
   a Dart getter where the framework requires it (`get props` ×246,
   `shouldRepaint`, …). Rule: parameterless `def` → getter by default;
   `@DartMethod` escape hatch if ever needed.
6. **Factory-constructor lowering** — companion `apply`/`fromJson` methods
   emitting as Dart `factory` constructors on abstract classes, so
   polymorphic `X.fromJson` hierarchies (LN has 10, one with 50 branches)
   round-trip.
7. **`while` loops and `Return` trees** — currently `/* TODO stmt */`.
8. **Method overloading disambiguation** — same-named Scala overloads emit
   colliding Dart methods today.
9. **`lazy val` → `late final`** and an explicit `late` marker for the
   `State.initState()` deferred-assignment pattern (42 `late` in LN,
   79 in nabo) — `lazy val` semantics differ; both forms are needed.
10. **`covariant` override escape hatch** — Scala forbids covariant method
    params; `CustomPainter.shouldRepaint(covariant …)` needs a
    `@DartCovariant` annotation.
11. **Pattern coverage** — switch *statements* (123 in nabo, 15 in LN),
    alternatives (`case a | b`), sequence patterns, `@`-binds.
12. **Records** — positional tuples (done) + `.indexed`-style `.$1`/`.$2`
    access; named tuples → Dart named-field records (nabo only).
13. **Visibility** — Scala `private` → Dart `_`-prefix where not
    referenced cross-library.
14. **for-comprehensions beyond List**; parameterised/enhanced enums
    (LN wires enums as tagged strings with a `label` member).
15. **Numeric fidelity** — Scala `Int` is 32-bit-wrapping; Dart web `int`
    is 53-bit. Document the mapping, detect bit-twiddling patterns
    (hashing in LN's graph code), and provide `Int32` semantics where it
    matters.

Deliberately out of scope for 1.0: variance emission, implicit search
beyond Predef stripping, full scala3-library TASTy compile-through
("Layer B") — the rewrite table covers the stdlib subset shared code is
allowed to use.

## Workstream B — Serialization & the wire boundary

Both apps speak the same dialect: `Map<String, dynamic>` everywhere
(2,407 sites in LN, 1,407 in nabo), string-tag polymorphic dispatch
(`json['type']`), `deepClone() => fromJson(toJson())`, `@JsonValue`
tagged enums, and an all-static `Service` class with pluggable static
hooks (`authToken`, `headersProvider`, `onError`, `transport`) — LN's
120 methods and nabo's 351 are both generated from the same Scala
server idiom.

1. **Sealed-hierarchy JSON codecs**: emit `fromJson` factory dispatch +
   `toJson` with `map['type'] = 'X'`, per-case `fromJson`/`toJson`/
   `copyWith`/`props`/`deepClone` — byte-matching the existing generated
   shape so the server protocol is unchanged. Include LN's
   `genericArgumentFactories` shape (16 generic model classes taking
   `T Function(Object?) fromJsonT`).
2. **A first-class `dynamic` bridge** in `sart-dart`: a `Dyn` type that
   lowers to Dart `dynamic` (Scala `Any` → `Object?` forbids member
   access), plus `DartMap[K, V]` with raw index access. Also one true
   dynamic-dispatch site (`t.toJson()` on `dynamic` in outr_flutter) —
   support via a `Dyn.invoke`/`selectDynamic` lowering.
3. **Compile-time environment constants** —
   `bool.fromEnvironment("dart.tool.dart2wasm")`,
   `String.fromEnvironment("SENTRY_DSN")` — facade + emission support.

## Workstream C — Facades at scale (the biggest lift by volume)

Each app references ~300 distinct external symbols (nabo: 287 Flutter
framework symbols + ~15 packages; LN: ~300 incl. **five Syncfusion
packages** — the hardest single binding surface: `DataGridSource`
subclassing, `SfMaps` layer trees, charts — plus go_router,
flex_color_scheme, animated_tree_view, dropdown_button2,
flutter_multi_select_items, overlay_support, feedback, sentry_flutter,
file_picker/file_saver, intl, and outr_flutter's google_fonts, provider,
shimmer, flutter_spinkit, flutter_svg, web_socket_channel).

Hand-writing that is not viable — **productize `sart-facadegen` and wire
it into `sbt-sart`** (the auto-facade plugin feature):

1. Ship the Dart-side analyzer helper as a versioned asset instead of the
   current repo-relative `tool/` lookup + hardcoded dev path
   (`sart-facadegen/.../Main.scala:86`).
2. New plugin surface:

   ```scala
   sartFacadePackages := Seq(
     dart"go_router:17.3.0",
     dart"syncfusion_flutter_datagrid:33.2.4"
   )
   ```

   An sbt task resolves each package via `dart pub`, runs facadegen over
   its public API, and drops generated sources under `src_managed`
   (wired into `Compile / sourceGenerators`, cached by package version).
   `@DartPackage` annotations on the generated facades feed the emitted
   pubspec automatically — one setting, no manual steps.
3. Generator completeness for real-world Dart: named params with
   defaults, `required`, callbacks/function types, generics, enhanced
   enums, extension getters, abstract + factory constructors, and
   **subclassable** facade classes (LN subclasses `DataGridSource`,
   `CustomPainter`; nabo subclasses 12 `CustomPainter`s). Grow it against
   Syncfusion datagrid, go_router, and media_kit first — the hardest
   surfaces the two apps need.
4. Overrides: hand-written facades shadow generated ones
   (generated = complete, curated = pleasant).
5. Flutter framework facades regenerate from the SDK checkout the same
   way, replacing hand-maintained `material.scala` growth.
   **✅ Landed (2026-08):** facadegen v2 runs resolved analysis (super-
   params, named/factory ctors, required/default fidelity, type-collapse
   via ancestor chains); `sbt sartFacadesRegen` + the
   `flutter-facades/facadegen*.conf` manifests derive ~320 material/
   services declarations (incl. full Icons/Colors) from the real SDK,
   with the curated core at ~45. **Subclassable facades** landed too
   (`context <appDir>` + `library <package:uri>` config resolves pub
   packages through the app's analysis context; `flatten-inherited`
   folds non-facaded ancestors' API in) — LN subclasses the generated
   Syncfusion `DataGridSource` and the curated `CustomPainter`.
   Per-package runs exist for syncfusion datagrid, dropdown_button2,
   flex_color_scheme, intl, email_validator, shimmer, spinkit, svg.
   Remaining: the `sartFacadePackages` pub-package resolver (today each
   package is a hand-written `.conf` in the consuming app), and
   generics — facadegen still can't emit `TreeNode<T>`-style classes, so
   ~10 packages ride on hand-curated `@native` facades in app code.

## Workstream D — Shared core module (frontend + backend)

**✅ Landed (2026-08-27): annotation-free models.** Plain case classes and
sealed hierarchies get codecs with no Sart reference, defaulting to
fabric's `RW.gen` conventions (field name = JSON key; tag =
`Outer.Inner`); classes nested in objects flatten; JVM-only companion
givens (`RW[T]`) are skipped loudly; fabric's `Json` rides as `dynamic`
with its builders lowered to literals. LN's `logicalnetwork-api` module
compiles through Sart as-is (verified: codecs for every request/response,
every `RW.gen` skipped with a comment). Remaining: `case object` enumerations as
Dart values with `RW.enumeration`-style string codecs, and switching the
LN port from `models.scala` to the shared module.

Goal: one cross-target Scala module holding models, protocol, validation,
and operations logic, consumed by the JVM backend *and* compiled to Dart
by Sart — the Scala.js `crossProject` story, but for Dart.

LogicalNetwork proves this is the natural endpoint: its Dart models are
*already* generated from the Scala server by `spice.openapi.generator`.
Sart replaces that generator with direct compilation of the shared
module — same wire format, no intermediate codegen, and the model exists
exactly once.

1. **Mechanics**: a plain Scala 3 module works today — JVM consumes
   bytecode, Sart consumes TASTy. Add a `sartCoreProject` helper (or
   documented pattern) in `sbt-sart` that adds the module to
   `sartFacadeClasspath` so its TASTy is emitted alongside app code.
2. **The portable subset**: shared code may use exactly the stdlib
   surface the rewrite table + `sart-stdlib` cover. Enforce mechanically:
   a `sartCoreVerify` task that emits the module standalone under
   `--strict`.
3. **Serialization symmetry**: the same case classes get JVM codecs
   (spice, as today) and Sart-emitted Dart codecs (Workstream B) with
   identical wire shape.
4. **outr_flutter becomes a Scala library**: port the shared UI package
   (`Application`, `Screen`, `MessageService`, `CustomForm`, …) to Scala
   once and publish it as a Sart artifact both apps depend on — the
   proof that third parties can ship Sart libraries.
5. Grow the rewrite table as shared code demands (`groupBy`, `collect`,
   `span`, `sliding` are likely early asks).

## Workstream E — Platform variants, web target & interop

1. **Web is the primary target for LN**: `flutter build web --wasm` with
   a dart2js fallback. Emitted Dart must be **dart2wasm-clean** (no
   `dart:html`/legacy `dart:js` — use `package:web`; already Sart's
   posture). Add a `--wasm` variant to `sartWeb`.
2. **`package:web` facades** — LN uses it only for `window.location.*`
   and download triggering; nabo's web files go deeper
   (`dart:js_interop` extension types, `@JS` externals,
   `registerViewFactory`, `HtmlElementView`). Facade support for
   extension types via a `@DartExtensionType` marker.
3. **Conditional imports** — 2 load-bearing sites in outr_flutter, 14 in
   nabo (`if (dart.library.io)` / `js_interop` with `_stub`/`_io`/`_web`
   variants). Proposed: `@DartConditionalExport` on facade objects or a
   Sart-level `Platform` abstraction lowering to Dart conditional
   exports. Needs a design spike.
4. **Platform channels** (nabo only): `MethodChannel`/`EventChannel`
   facades — plain facade work.

## Workstream F — Release engineering

1. **Publishing**: `publishTo` still points at decommissioned legacy
   OSSRH; no signing plugin. Move to the **Central Portal**
   (`sbt-ci-release` or Portal-aware `sbt-sonatype` + `sbt-pgp`), add
   `publish / skip := true` for `example` and root, decide whether
   `sart-facadegen` publishes.
2. **Versioning/automation**: tag-driven releases, a `release.yml`
   publishing core modules + both plugin axes, CHANGELOG.
3. **Version single-sourcing**: `0.1.0-SNAPSHOT` is hardcoded in 7 places
   (root build, plugin build, `SartPlugin.scala:110`, READMEs, smoke
   script).
4. **CI hardening**: wire `--strict` into the golden/regression jobs;
   plugin `scripted` tests; keep the two-axis consumer smoke.
5. **Docs**: getting-started tutorial (new project from scratch),
   facade-authoring guide, shared-core-module guide.

## Milestones

| Version | Theme | Acceptance gate |
|---|---|---|
| 0.2 | Language completeness | Workstream A items 1–10; `--strict` green over a fixture corpus shaped like both apps (named/required/default params, super.key, const, mixins, getters, factory fromJson, while, overloads, late/lazy) |
| 0.3 | Models + shared core | Workstream B + D: LN's `lib/model/**` + `service.dart` (916 files) replaced by direct Sart compilation of the shared Scala module; wire format byte-identical; `sartCoreVerify` in CI; `GenerateDart.scala` retired. **Status: bypassed for LN** — the codecs (Workstream B) landed and the port consolidates all ~230 models + 114 endpoints by hand in `models.scala`/`service.scala` (wire-verified field-by-field); the shared module + `GenerateDart` retirement remain open |
| 0.4 | Facades at scale | Facadegen productized + `sartFacadePackages`; Flutter framework facades regenerated; Syncfusion datagrid + go_router facades working in a demo. **Status: ✅ except `sartFacadePackages`** (per-package `.conf`s do the job today) |
| 0.5 | outr_flutter in Scala | The shared UI package ported and published as a Sart library; both apps' `Application`/`Screen`/`MessageService` layer runs from Scala. **Status: ✅ ported** (lives in `app-sart`'s `outr` package for now, not yet published as a separate library) |
| **0.6** | **LogicalNetwork ships from Scala** | `flutter build web --wasm` from Sart output, zero hand-written Dart, deployed behind the Scala server — first full-app proof. **Status: code-complete** — zero hand-written Dart, analyzer-clean, `flutter build web` green, every file ported line-by-line; open gates are runtime verification against the server, the `--wasm` build, and deployment |
| 0.7 | nabo platform layer | Conditional exports, JS interop, platform channels, media_kit/webview facades; nabo's `lib/platform/` + models from Scala |
| 0.8–0.9 | nabo migration | Port screens/ → widgets/ → games/; perf validation on TV hardware (const inference proves out) |
| **1.0** | **Both apps ship from Scala** | LN (web) and NaboTV (web/Android/tvOS) build from Sart output with zero hand-written Dart; Maven Central artifacts; docs complete |

## Risks

- **`const` inference** is novel compiler work with a performance (not
  correctness) failure mode — validate on LN web + nabo TV hardware
  early (0.2, not 0.8).
- **Syncfusion/facadegen generality**: five large generics-heavy,
  callback-laden, subclassing-required packages may surface Dart shapes
  the facade model can't express; budget a design iteration (0.4).
- **No test safety net in either app** (LN: 56 test lines; nabo: 5
  files). Correctness during ports comes from wire-format byte-diffing
  (models) and incremental porting behind the existing
  `Service.transport` seam.
- **Int semantics on web**: 32-bit Scala `Int` vs 53-bit Dart web `int`
  — audit hashing/bit-ops during the LN graph-widget port.
- **Formatter/toolchain drift**: goldens pinned via `--language-version`
  from the pubspec SDK floor; each Flutter bump can reflow goldens
  (accepted, reviewed as a diff).
- **sbt 2 caching semantics**: side-effecting tasks are `@transient` and
  the emitter runs as a directly-forked `java` process after a
  stale-read race with `runMain` delegation; keep new tasks on that
  pattern.
