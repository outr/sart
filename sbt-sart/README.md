# sbt-sart

An sbt autoplugin that wraps the Sart Scala 3 → Dart compiler.
Cross-built for **sbt 1.x** (Scala 2.12 artifact) and **sbt 2.x**
(Scala 3 artifact) — `addSbtPlugin` resolves the right one for your
sbt version.

## Consume

```scala
// project/plugins.sbt
addSbtPlugin("com.outr" % "sbt-sart" % "0.1.0-SNAPSHOT")
```

```scala
// build.sbt
enablePlugins(SartPlugin)
```

That's it — the plugin resolves the Sart compiler and facade artifacts
(`sart-compiler`, `sart-dart`, `sart-stdlib`, `flutter-facades`) at
`sartVersion` (defaults to the plugin's own version) through hidden Ivy
configurations, adds the facades to your compile classpath, and runs
the compiler in a forked JVM. Until Sart is on Maven Central, run
`sbt sartPublishLocalAll` in the main Sart repo once to seed your local
Ivy cache.

Then:

- `sbt sartEmit` — compile your Scala 3 code and emit Dart into `out/lib/`.
- `sbt sartLinux` / `sartWeb` / `sartAndroid` / `sartMacOS` / `sartWindows` /
  `sartIOS` — scaffold the platform embedder and `flutter build` it.
- `sbt sartTizen` / `sartWebOS` / `sartTvOS` — build for Samsung Tizen,
  LG webOS, and Apple TV (see **Smart TV targets** below).
- `sbt sartRun` — build the Linux binary and launch it.
- `sbt sartGoldenVerify` / `sartGoldenAccept` — regression gates against
  a checked-in `sart-golden/` directory.

## Settings

| Key                      | Default                        | Purpose                                               |
| ------------------------ | ------------------------------ | ----------------------------------------------------- |
| `sartVersion`            | plugin version                 | Version of the Sart core artifacts to resolve.        |
| `sartCompilerClasspath`  | auto-resolved                  | Jars to run `sart.compiler.Main` in a forked JVM.     |
| `sartFacadeClasspath`    | auto-resolved                  | TASTy-bearing jars the inspector needs to resolve.    |
| `sartOutDir`             | `<base>/out`                   | Where Dart + pubspec are written.                     |
| `sartSourceRoot`         | `baseDirectory.value`          | Used to relativise `/// Source:` comments.            |
| `sartGoldenDir`          | `<base>/sart-golden`           | Directory of golden files for `sartGoldenVerify`.     |
| `sartAssets`             | `Seq.empty`                    | Files/directories copied into `<out>/assets/` on emit. |
| `sartPubspecLock`        | `None`                         | A `pubspec.lock` copied into `<out>/` on emit so `flutter pub get` resolves exactly those versions. |
| `sartWebDir`             | `None`                         | Directory overlaid onto the scaffolded `<out>/web/` (custom `index.html`, `flutter_bootstrap.js`, icons) before `flutter build web`. |
| `sartLibraries`          | `Seq.empty`                    | Dependencies (`ModuleID`s) whose TASTy also compiles through to Dart. `dependsOn` projects are always included. |
| `sartWireMappings`       | `Map.empty`                    | Wire-primitive foreign types (`"lightdb.id.Id" -> "String"`): mapped in type position, cast/delegated in the synthesized codecs. |
| `sartStrict`             | `false`                        | Fail `sartEmit` on any untranslatable construct, reported at its Scala source location. |
| `sartTizenCommand`       | `"flutter-tizen"`              | The `flutter-tizen` CLI used by `sartTizen`. |
| `sartWebOSCommand`       | `"flutter-webos"`             | The `flutter-webos` CLI used by `sartWebOS`. |
| `sartTvOSCommand`        | `"flutter-tvos"`              | The `flutter-tvos` CLI used by `sartTvOS`. |

## TV targets

Flutter reaches TVs through community forks of the Flutter CLI. Sart emits
the same `lib/` + `pubspec.yaml` for these as for every other target — only
the build command differs — so each task routes the identical emitted app
through the fork's CLI instead of `flutter`. The forks come in two shapes:

**Additive** — grafts one more embedder onto the project via
`create --platforms=X`, leaving `flutter` itself intact:

- **`sartTizen`** (Samsung) → `flutter-tizen create --platforms=tizen …`
  then `flutter-tizen build tpk -ptv`, producing a `.tpk` under
  `out/build/tizen/tpk/`. Requires
  [`flutter-tizen`](https://developer.samsung.com/smarttv/develop/native/flutter.html)
  on `PATH` and the Tizen Studio SDK; targets Tizen 6.0+.
- **`sartWebOS`** (LG) → `flutter-webos create --platforms=webos …` then
  `flutter-webos build webos --release`, producing an `.ipk` under
  `out/build/webos/`. Requires
  [`flutter-webos`](https://github.com/lg-flutter-webos) and the webOS
  NDK on `PATH`.

**Substituting** — replaces the `flutter` CLI entirely for one target:

- **`sartTvOS`** (Apple TV) → [`flutter-tvos`](https://github.com/fluttertv/flutter-tvos)
  retargets iOS→tvOS, so `flutter-tvos create …` (no `--platforms`)
  scaffolds a `tvos/` folder and the iOS build path yields the tvOS `.app`
  under `out/build/ios/iphoneos/`. Requires `flutter-tvos` on `PATH` plus
  macOS + Xcode; `--no-codesign` builds unsigned, `flutter-tvos build ipa`
  with your signing for distribution.

Point `sartTizenCommand` / `sartWebOSCommand` / `sartTvOSCommand` at an
absolute path if the CLI isn't on `PATH`. These toolchains are
community-supplied and not bundled with Sart; without them installed the
tasks compile and cross-publish but the build step needs the CLI (and a
device or simulator to `run`).

## Implementation notes

The plugin runs the Scala 3 Sart compiler as a **forked JVM
subprocess** — no cross-compilation against the consumer's Scala
version is required. That keeps the consumer project free to use any
Scala 3 version Sart supports.
