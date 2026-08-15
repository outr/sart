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

## Implementation notes

The plugin runs the Scala 3 Sart compiler as a **forked JVM
subprocess** — no cross-compilation against the consumer's Scala
version is required. That keeps the consumer project free to use any
Scala 3 version Sart supports.
