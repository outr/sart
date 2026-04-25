# Sart — Scala 3 → Dart / Flutter

[![CI](https://github.com/outr/sart/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/outr/sart/actions/workflows/ci.yml)
[![Pages](https://github.com/outr/sart/actions/workflows/pages.yml/badge.svg?branch=master)](https://outr.github.io/sart/)
[![Scala 3](https://img.shields.io/badge/Scala-3.8-DC322F?logo=scala&logoColor=white)](https://www.scala-lang.org/)
[![Flutter](https://img.shields.io/badge/Flutter-stable-02569B?logo=flutter&logoColor=white)](https://flutter.dev/)
[![Lines of code](https://sloc.xyz/github/outr/sart/?category=code)](https://github.com/outr/sart)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Sart compiles Scala 3 source to Dart via TASTy inspection and drops the
result into a ready-to-build Flutter project. Write your UI in pure Scala;
run it as a native Flutter app on Linux (today), with web/mobile/desktop
in the pipeline.

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
| `sart-dart/`          | Annotation library: `@native`, `@DartImport`, `@DartName`, `@DartPackage`, `@DartPubspec`, and the `native.value` sentinel. Analogous to `scalajs-library`. |
| `sart-stdlib/`        | Hand-ported Scala stdlib facades mapped to Dart: `Option`, `Try`, `Either` (+ their companions). Dart shims emitted alongside user code. |
| `flutter-facades/`    | Facades for the Flutter `material` library: widgets, themes, layout, navigation. Each carries `@DartImport` + `@DartPackage` so the emitter auto-generates imports and pubspec. |
| `example/`            | Sample Scala apps exercising the compiler — counter app, todo app, two-screen nav app, plus ~20 feature fixtures. |
| `compiler/`           | The Sart Dart emitter. `sart.compiler.Main` is the CLI; `DartEmitter.scala` walks TASTy and writes Dart. |
| `sart-facadegen/`     | MVP facade-generator. A Dart helper parses a `.dart` file with `package:analyzer` and emits JSON; the Scala CLI writes Scala facades. |
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

Facade generation (MVP):

```bash
sbt 'sart-facadegen/runMain sart.facadegen.Main \
  <path/to/library.dart> <out-dir> <package:dart/import.dart>'
```

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

**Scala 3 language surface**: classes, traits (→ abstract class), `case class`
(with synthesised `==`/`hashCode`/`toString`/`copyWith`), `enum` (simple +
sealed hierarchies), generics on classes/methods, pattern matching →
Dart 3 switch expressions, extension methods, `given`/`using`, `inline def`,
`try`/`catch`/`finally`, `Function0..N` types, for-comprehensions,
string interpolation, curried method calls, and more.

**Stdlib mappings**: `Option[T]` ↔ `T?` (with `.map`/`.flatMap`/`.fold` via
a Dart extension shim; `.getOrElse`/`.isDefined`/`.isEmpty` via native
operators), `List` (literals, `:+`, `+:`, index access, `updated`,
`map`/`filter`/`flatMap`/`fold`/`mkString`/`head`/`headOption`/`reverse`,
…), `Map` literals + `.containsKey`, `Set` literals, `scala.concurrent.Future`
↔ Dart `Future` (with `.then` for `.map`/`.flatMap`), `Try`/`Either` as
sealed Dart hierarchies with full method coverage, Predef implicit-wrapper
stripping, automatic `String` ↔ non-`String` concat coercion.

**Flutter facades**: `MaterialApp`, `Scaffold`, `AppBar`, `Text`, `Icon`,
`Icons`, `Theme`/`ThemeData`/`ColorScheme`/`TextTheme`, layout (`Column`,
`Row`, `Center`, `Padding`, `SizedBox`, `Container`, `Divider`, `EdgeInsets`),
buttons (`ElevatedButton`, `TextButton`, `IconButton`, `FloatingActionButton`),
inputs (`TextField`, `TextEditingController`, `InputDecoration`, `Checkbox`,
`Switch`), lists (`ListView`, `ListView.builder`, `ListTile`), navigation
(`Navigator`, `MaterialPageRoute`, `Route`), `StatelessWidget` +
`StatefulWidget` + `State[W]`.

**Toolchain**: deterministic emission order (sorted TASTy), auto-format via
`dart format`, per-top-level-member `/// Source:` attribution comments,
golden-file regression gates, `flutter analyze` error remapping to Scala
source lines, idempotent Flutter Linux project scaffolding.

**Published artifacts** (local Ivy): `sart-dart_3`, `sart-stdlib_3`,
`flutter-facades_3`, `sart-compiler_3`, `sbt-sart` (all at `0.1.0-SNAPSHOT`).

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
- Maven Central publishing (artifacts available locally via `sartPublishLocalAll`).

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
