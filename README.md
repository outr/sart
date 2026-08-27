# Sart — Scala 3 → Dart / Flutter

[![CI](https://github.com/outr/sart/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/outr/sart/actions/workflows/ci.yml)
[![Pages](https://github.com/outr/sart/actions/workflows/pages.yml/badge.svg?branch=master)](https://outr.github.io/sart/)
[![Scala 3](https://img.shields.io/badge/Scala-3.8-DC322F?logo=scala&logoColor=white)](https://www.scala-lang.org/)
[![Flutter](https://img.shields.io/badge/Flutter-stable-02569B?logo=flutter&logoColor=white)](https://flutter.dev/)
[![Lines of code](https://sloc.xyz/github/outr/sart/?category=code)](https://github.com/outr/sart)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Sart compiles Scala 3 source to Dart via TASTy inspection and drops the
result into a ready-to-build Flutter project. Write your UI in pure Scala;
build it for web, Linux, and Android (verified), or macOS/Windows/iOS
from the matching host. The first reference application — a 33k-line
production Flutter app — now builds entirely from Scala with zero
hand-written Dart (see [Reference app](#reference-app)).

```
Scala 3 source
     │ scalac (-Yretain-trees)
     ▼
.tasty files
     │ scala3-tasty-inspector
     ▼
sart.compiler.Main
     │ walks TASTy trees, emits
     ▼
Dart source + pubspec.yaml + (shims)
     │ flutter build linux
     ▼
native binary
```

## Repo layout

| Module                | Purpose                                                                 |
| --------------------- | ----------------------------------------------------------------------- |
| `sart-dart/`          | Annotation library: `@native`, `@DartImport`, `@DartAlias`, `@DartName`, `@DartPackage`, `@DartPubspec`, `@DartTopLevel`, the JSON-codec annotations (`@JsonModel`, `@JsonTag`, `@JsonField`), the `native.value` sentinel, the `Dyn` dynamic bridge, `Json`, `Completer`, `nn`/`cast`/`nullValue`, and direct-style `async {}` / `await(...)`. Analogous to `scalajs-library`. |
| `sart-stdlib/`        | Hand-ported stdlib facades mapped to Dart: `Option`/`Try`/`Either` (Dart shims emitted alongside user code), `Duration`/`Timer`, `Stream`, `Regex`, `dart:math`, `dart:convert` (JSON/utf8/base64), `Uint8List`, `dart:core` statics (`int.parse`, `String.fromEnvironment`, `print`, `FormatException`), and `num`/`String` extension methods (`toStringAsFixed`, `clamp`, `padLeft`, `codeUnits`, …). |
| `flutter-facades/`    | Facades for Flutter `material`, `services`, `gestures`, and `dart:ui` — ~360 declarations, ~320 of them generated from the SDK sources, the rest curated (`State[W]`, canvas/painting, `AsyncSnapshot`, `Autocomplete`, …). Each carries `@DartImport` + `@DartPackage` so the emitter auto-generates imports and pubspec. |
| `example/`            | Sample Scala apps exercising the compiler — counter app, todo app, two-screen nav app, plus the feature fixtures under `compiler/src/test`. |
| `compiler/`           | The Sart Dart emitter (~3.8k lines). `sart.compiler.Main` is the CLI; `DartEmitter.scala` walks TASTy and writes Dart. |
| `sart-facadegen/`     | Facade generator, driven by `.conf` manifests. Runs resolved `package:analyzer` analysis (real constructor signatures incl. `super.`-params, named/factory constructors, required/default fidelity, ancestor-chain type collapse) against SDK sources or any pub package resolved through an app's analysis context, and emits Scala facades — including **subclassable** ones (`DataGridSource`, `CustomPainter`). |
| `sbt-sart/`           | Autoplugin that exposes the Sart pipeline as sbt tasks. Separate build (Scala 2.12 / sbt 1.x). |
| `out/`                | Generated Flutter project. `lib/main.dart` is the compiled output; `pubspec.yaml` and `linux/` are scaffolded by Flutter. |

## Try it

With the Sart repo checked out:

```bash
sbt sartRun      # emit Dart, build Linux binary, launch it
sbt sartLinux    # emit + build Linux, no launch
sbt sartWeb      # emit + build a Flutter web bundle into out/build/web
sbt sartAndroid  # emit + build a debug Android APK
sbt sartIOS      # emit + build an iOS bundle (macOS + Xcode host)
sbt sartMacOS    # emit + build a macOS bundle (macOS host)
sbt sartWindows  # emit + build a Windows bundle (Windows host)
sbt sartEmit     # just emit Dart into ./out
sbt sartAnalyze  # emit + run flutter analyze with errors remapped to Scala sources
sbt ~sartDev     # hot-reload dev loop: spawn flutter run once, hot-reload on each save
```

`sartDev` wraps `flutter run` so a save in your Scala source triggers an
emit + Flutter hot reload without leaving sbt. Defaults to the `linux`
device; override with `-DsartDev.device=<id>` (e.g. `chrome`, `macos`,
`windows`, or any id from `flutter devices`). Press Ctrl-C in the sbt
shell to quit; a JVM shutdown hook sends `q` to flutter and waits.

Regression gates:

```bash
sbt sartGoldenVerify   # diff emission against checked-in golden files
sbt sartGoldenAccept   # refresh the golden files from current emission
```

Facade generation — the Flutter facades are themselves generated from
the real SDK sources (resolved `package:analyzer` analysis: real
constructor signatures incl. `super.`-params, required/optional
fidelity, named/factory constructors, full `Icons`/`Colors` catalogs).
The manifests are `flutter-facades/facadegen.conf` (material) and
`facadegen-services.conf`; the curated semantic core stays hand-written
in `material.scala`. Adding a Flutter class to the facade set is a
one-line `keep` in the manifest plus a regen:

```bash
sbt sartFacadesRegen   # re-derive the *_generated.scala facade files
```

Generation for any pub package, resolved through the consuming app's
analysis context (so Flutter-based ancestors resolve and the emitted
facades are subclassable):

```bash
sbt 'sart-facadegen/runMain sart.facadegen.Main --config <facadegen-<pkg>.conf>'
```

Generics still defeat the generator (`TreeNode<T>`, `MultiSelectCheckList<T>`);
those facades are curated by hand in app code, Scala.js-style — a
`@native` class with `@DartImport`/`@DartPackage` is all it takes.

## Using Sart in your own project

```scala
// project/plugins.sbt
addSbtPlugin("com.outr" % "sbt-sart" % "0.1.0-SNAPSHOT")
```

```scala
// build.sbt
enablePlugins(SartPlugin)
```

After `sbt sartRun`, your Scala 3 code emits to `out/lib/main.dart` and
builds into a Linux native binary. See [`sbt-sart/README.md`](sbt-sart/README.md)
for plugin-specific settings.

**First-time bootstrap**: run `sbt sartPublishLocalAll` from this repo to
publish all Sart core artifacts (`sart-dart`, `sart-stdlib`,
`flutter-facades`, `sart-compiler`) AND the `sbt-sart` plugin to your
local Ivy cache in one step. After that the plugin auto-resolves
everything it needs.

## What works today

**Scala 3 language surface**: classes, traits (→ `abstract mixin class`,
or `mixin X on Parent` for parameterless traits with a parent), `case
class` (synthesised `==`/`hashCode`/`toString`/`copyWith`; `Object.hashAll`
past 20 fields), `enum` (simple + sealed hierarchies), objects → static
classes (companions folded into their class), generics on
classes/methods, pattern matching → Dart 3 switch expressions, statement
forms for `if`/`match`/`while`/`try` in `Unit` position, extension
methods → Dart extension calls (and `@native` extensions as facades for
real Dart extensions), `given`/`using`, `inline def`, `lazy val` →
`late final`, null-initialised `var`s → `late`, named/default parameters
(literal, `None`, empty-collection, and const tear-off defaults become
Dart named sections; non-literal defaults get `$default$` getters),
`super.key` forwarding, bitwise operators, `Function0..N` types,
for-comprehensions, string interpolation, curried calls, and more.

**Async**: direct-style `async { … }` bodies with `await(f)` anywhere —
the emitter marks the enclosing function `async`, propagates through
closures/IIFEs, and never hoists an awaited temp across a closure
boundary. `Future.map`/`flatMap`/`foreach` → `.then`, `Futures.ensure` →
`whenComplete`, `onError` → `catchError`, `Completer`, `Timer`/
`Timer.periodic`, `Stream` (`listen`/`map`/`expand`/`forEach`/`periodic`).

**The wire boundary**: `@JsonModel` case classes get `fromJson`/`toJson`
synthesised (nested models, `List`/`Map`/`Option` fields, `@JsonField`
renames such as `"_id"`), sealed hierarchies dispatch on a `@JsonTag`
`type` discriminator, and `Dyn` is the typed face of `dynamic`
(`d("k")`, `d.str`/`toInt`/`toDouble`/`toBool`/`isNull`/`toList`,
`d(k) = v`) for untyped payloads.

**Stdlib mappings**: `Option[T]` ↔ `T?` (via native operators and a Dart
extension shim), `List` (literals, `:+`/`++`/spread, `updated`, `slice` →
`sublist`, `take`/`drop`, `map`/`filter`/`flatMap`/`fold`/`find`/`sortBy`/
`distinct`/`zipWithIndex`/`mkString`, …), `Map` (literals, `m(k)`, `get`,
`getOrElse`, `++` → spread, `keys`/`values`), `Set` literals, `Range`,
`Tuple` → records, `Try`/`Either` as sealed Dart hierarchies, `Long.toInt`
elision, Predef implicit-wrapper stripping, `String` concat coercion,
`replace`/`capitalize`/`split`/`substring` and the Dart-native `num`/
`String` members via `sart.stdlib` extensions.

**Flutter facades**: ~360 declarations across `material`, `services`,
`gestures`, and `dart:ui` — the full widget/layout/input/list/navigation
catalog, `Icons`/`Colors`, themes, `Slider`/`RangeSlider`/chips/menus,
`InteractiveViewer` + `TransformationController`/`Matrix4`,
`CustomPainter`/`Canvas`/`Path`/`Paint`, `AnimationController`,
`Listener`/`PointerEvent`/`MouseRegion`/`Focus`, `StreamBuilder`/
`FutureBuilder`/`AsyncSnapshot`, `RenderRepaintBoundary.toImage` → PNG
bytes, `Clipboard`, `rootBundle`, and `StatelessWidget`/`StatefulWidget`/
`State[W]` with the full lifecycle (`initState`/`dispose`/
`didUpdateWidget`/`didChangeDependencies`) and `TickerProviderStateMixin`.

**Toolchain**: deterministic emission order (sorted TASTy), auto-format via
`dart format`, per-top-level-member `/// Source:` attribution comments,
golden-file regression gates, `flutter analyze` error remapping to Scala
source lines, idempotent Flutter project scaffolding, `sartAssets` for
bundled assets, `@DartPubspec` YAML merging, and a generated
`analysis_options.yaml` that silences only shim-noise diagnostics.

**Published artifacts** (local Ivy): `sart-dart_3`, `sart-stdlib_3`,
`flutter-facades_3`, `sart-compiler_3`, `sbt-sart` (all at `0.1.0-SNAPSHOT`).

## Reference app

The 1.0 plan's first reference application, **LogicalNetwork** (a
production web app: 105 widgets, 12 screens, a 114-endpoint service
layer over ~230 wire models, Syncfusion data grids and maps, two
force-directed graph explorers with custom painters, WebSocket
streaming, file upload/download, Sentry), is **fully ported** — 153
Scala files / 33k lines emitting 65k lines of Dart, including the shared
`outr_flutter` UI library it depends on. Zero hand-written Dart; the
emitted app is analyzer-clean and `flutter build web` passes. The port
is line-by-line — every Dart file has a Scala twin, class-for-class —
and drove most of the emitter and facade surface above. Pub packages it
uses beyond Flutter (syncfusion datagrid/maps/datepicker, go_router,
flex_color_scheme, http, web, web_socket_channel, intl, animated_tree_view,
dropdown_button2, flutter_multi_select_items, file_picker, file_saver,
feedback, sentry_flutter, logger, overlay_support, flutter_markdown_plus,
flutter_svg, shimmer, flutter_spinkit) are a mix of facadegen output and
hand-curated `@native` facades in the app itself.

## Platforms

| Target  | sbt task        | Host required     | Build command under the hood         |
| ------- | --------------- | ----------------- | ------------------------------------ |
| Linux   | `sartLinux`     | Linux (verified)  | `flutter build linux`                |
| Web     | `sartWeb`       | any (verified)    | `flutter build web`                  |
| Android | `sartAndroid`   | any + JDK/SDK (verified) | `flutter build apk --debug`   |
| macOS   | `sartMacOS`     | macOS             | `flutter build macos`                |
| Windows | `sartWindows`   | Windows           | `flutter build windows`              |
| iOS     | `sartIOS`       | macOS + Xcode     | `flutter build ios --no-codesign`    |

Same Scala source compiles to every target. The Linux/Web/Android paths
were verified on the author's Linux host (48M native bundle, 35M web,
143M debug APK); macOS/Windows/iOS are wired identically but require
the matching host OS to actually run `flutter build`.

## Not yet
- Full scala3-library TASTy compile-through (the "Layer B" of Phase 2).
- Conditional exports / `dart:js_interop` extension-type facades /
  platform channels — the platform-variant machinery the second
  reference app (NaboTV) needs.
- A shared Scala "core" module compiled to both the JVM backend and the
  Sart frontend (the LN port consolidates its models by hand instead).
- Maven Central releases (everything is `0.1.0-SNAPSHOT` in local Ivy).

See [ROADMAP.md](ROADMAP.md) for the 1.0 plan: language completeness,
generated facades at scale, a shared Scala core module for frontend +
backend, and Maven Central releases. [PORTING.md](PORTING.md) is the
step-by-step playbook for the two 1.0 reference-app ports.

## Design principles

1. **Mirror Scala.js where the pattern already exists** — `@native` as the
   facade marker, `native.value` as the Nothing-typed body sentinel,
   companion-object conventions, per-package annotation-driven imports.
2. **Annotations self-describe the artifact** — a single `@DartImport` on
   a facade drives the Dart `import` line; `@DartPackage` drives pubspec
   dependencies; `@DartPubspec` injects arbitrary YAML blocks. Adding a
   new library is one annotation, not edits scattered across tooling.
3. **Dart's toolchain stays authoritative** — `dart format`, `dart
   analyze`, `flutter build` do what they do. Sart doesn't reimplement
   them.
4. **Loud, not silent** — unrecognised tree shapes produce `/* TODO: … */`
   comments in the emitted Dart so gaps are visible and tracked, not
   silently dropped.
5. **Deterministic** — sorted TASTy, stable emit order, golden-file
   regression tests so every emitter change is reviewed as a diff.
