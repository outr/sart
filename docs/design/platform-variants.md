# Platform-variant emission

Status: **design** (2026-09-03). Workstream E item 1; the one structural
feature between Sart and NaboTV support.

## Problem

Flutter web and native builds cannot share one import graph: `dart:io`
does not exist on web, `package:web`/`dart:js_interop` do not exist on
native. The idiom (16 switch files over ~50 variant files in NaboTV) is a
trio of same-named APIs behind a conditional export:

```dart
// lib/platform/fullscreen.dart — what app code imports
export 'fullscreen_stub.dart' if (dart.library.js_interop) 'fullscreen_web.dart';
```

`fullscreen_stub.dart` (no-op) and `fullscreen_web.dart` (package:web)
declare the same top-level API; the compiler picks one per target.

Sart today emits a single `main.dart` with one global import header, so a
port of any such file drags `dart:io` and `package:web` into every build.

## Authoring model

One Scala object is the **switch**: a `@native` facade (so call sites
type-check and get the right import) that also carries the variant table.
Each **implementation** is an ordinary Scala object routed to its own
library file. Scala names differ (they must — one package); `@DartName`
unifies the emitted name so the conditional export resolves.

```scala
// The switch: app code calls Fullscreen.isFullscreen() like any facade.
// The emitter GENERATES lib/platform/fullscreen.dart from @DartVariants.
@native @DartImport("platform/fullscreen.dart")
@DartVariants(default = "platform/fullscreen_stub.dart",
              web     = "platform/fullscreen_web.dart")
object Fullscreen:
  def isFullscreen(): Boolean = native.value
  def toggleFullscreen(): Unit = native.value

// Emitted into lib/platform/fullscreen_stub.dart
@DartLibrary("platform/fullscreen_stub.dart") @DartName("Fullscreen")
object FullscreenStub:
  def isFullscreen(): Boolean = false
  def toggleFullscreen(): Unit = ()

// Emitted into lib/platform/fullscreen_web.dart
@DartLibrary("platform/fullscreen_web.dart") @DartName("Fullscreen")
object FullscreenWeb:
  def isFullscreen(): Boolean = web.document.fullscreenElement != null
  def toggleFullscreen(): Unit =
    if web.document.fullscreenElement != null then web.document.exitFullscreen()
    else web.document.documentElement.requestFullscreen()
```

Emitted (4 files):

```dart
// lib/platform/fullscreen.dart            (generated switch)
export 'fullscreen_stub.dart' if (dart.library.js_interop) 'fullscreen_web.dart';

// lib/platform/fullscreen_stub.dart
class Fullscreen { Fullscreen._();
  static bool isFullscreen() => false;
  static void toggleFullscreen() {} }

// lib/platform/fullscreen_web.dart
import 'package:web/web.dart' as web;
import '../main.dart';
class Fullscreen { … }

// main.dart call sites: `import 'platform/fullscreen.dart';` +
// `Fullscreen.isFullscreen()` — via the ordinary facade machinery.
```

The JVM never minds that stub/io/web impls coexist in one compile unit —
facades are `native.value` stubs there. Only the *Dart* import graphs
must stay disjoint, which is exactly what the split libraries give.

### Annotations

- `@DartLibrary(path)` — on a top-level class/object: emit it (and its
  companion) into `lib/<path>` instead of `main.dart`. Useful beyond
  variants (splitting the 65k-line LN `main.dart` is future work on the
  same mechanism).
- `@DartVariants(default, io = "", web = "")` — on a `@native` object that
  has `@DartImport(path)`: generate `lib/<path>` as
  `export '<default>' [if (dart.library.io) '<io>'] [if (dart.library.js_interop) '<web>'];`
  (relative to the switch file's directory). Omitted axes fall back to
  `default`. Members of the object are facade API only; nothing else is
  emitted for it.

## Emitter changes

1. **Per-library targets.** `bodyBuf` + `imports` become a map
   `path → LibraryTarget(body, imports)`; `runTop` switches the current
   target when a top-level symbol carries `@DartLibrary`. All `line()` /
   import recording routes through the current target. `main.dart` is the
   default target; shims and pubspec logic unchanged.
2. **Per-library headers.** Each non-main library's header = its own
   recorded imports + `import '<rel>main.dart';` (shared emitted types —
   models, traits — live there; Dart import cycles are legal). Relative
   paths computed from the library's directory (`platform/x.dart` →
   `../main.dart`, shims → `../sart_option.dart`).
3. **Switch generation.** `writeOutput` renders every `@DartVariants`
   into its `@DartImport` path. Error if the annotation lacks
   `@DartImport`/`@native`, or if a referenced variant path was never
   emitted.
4. **Isolation guard.** A symbol annotated `@DartLibrary` referenced from
   any *other* library is a strict-mode gap ("go through the variant
   facade") — cross-library references to emitted classes are otherwise
   unresolvable, and reaching around the switch defeats the pattern.
5. **Name collisions.** Emitted names may repeat *across* libraries (the
   whole point); symbol-keyed maps (`userClasses`, `classDefs`) are
   unaffected, but any name-keyed logic must scope per target.

## Verification

- Fixtures: a stub/web pair + switch in the test corpus; asserts the four
  files, per-library headers, and that `main.dart` contains neither
  `package:web` nor the impl classes. Golden example under `example/`.
- `flutter analyze` resolves the *default* export, so stub/API drift
  surfaces immediately; io/web drift surfaces at platform build. v2: a
  strict-mode audit comparing emitted member signatures across a variant
  set (Sart sees all three — it can diff them, which Dart never could).
- End-to-end: port one real NaboTV triple (`fullscreen`, then
  `platform_os`) and build web + a native target from the same emission.

## Out of scope

- Per-platform *pubspec* dependencies (pubspec is shared in Flutter;
  variant-only packages are still listed — same as hand-written apps).
- `roku/`, `webos-lite/` (non-Flutter siblings).
- Automatic variant synthesis (generating a stub from the io impl's
  shape) — attractive later; explicit files first.

## Implementation order

1. `LibraryTarget` refactor (mechanical; goldens must not change for
   annotation-free projects).
2. `@DartLibrary` + per-library headers + fixtures.
3. `@DartVariants` switch generation + isolation guard + fixtures.
4. Example-app triple + golden; README/PORTING docs.
5. NaboTV `fullscreen` proof; then the strict cross-variant audit (v2).
