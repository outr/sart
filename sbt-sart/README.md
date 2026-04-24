# sbt-sart

An sbt autoplugin that wraps the Sart Scala 3 → Dart compiler.

## Consume

```scala
// project/plugins.sbt
addSbtPlugin("com.outr" % "sbt-sart" % "0.1.0-SNAPSHOT")
```

```scala
// build.sbt
enablePlugins(SartPlugin)

// Jars that run `sart.compiler.Main`. Produce these by running
// `sbt compiler/publishLocal` in the main Sart repo and pointing
// these at the resulting jars + their transitive deps.
sartCompilerClasspath := Seq(
  file("/path/to/sart-compiler_3-0.1.0-SNAPSHOT.jar"),
  file("/path/to/scala3-tasty-inspector_3-3.8.3.jar"),
  file("/path/to/scala3-compiler_3-3.8.3.jar"),
  // … plus whatever else Coursier resolves as transitive
)

// Facade/stdlib jars that TASTy references in your code need to resolve.
sartFacadeClasspath := Seq(
  file("/path/to/sart-dart_3-0.1.0-SNAPSHOT.jar"),
  file("/path/to/sart-stdlib_3-0.1.0-SNAPSHOT.jar"),
  file("/path/to/flutter-facades_3-0.1.0-SNAPSHOT.jar")
)
```

Then:

- `sbt sartEmit` — compile your Scala 3 code and emit Dart into `out/lib/`.
- `sbt sartLinux` — scaffold Flutter's Linux embedder and build a native binary.
- `sbt sartRun` — do the above and launch the binary.
- `sbt sartGoldenVerify` / `sartGoldenAccept` — regression gates against
  a checked-in `sart-golden/` directory.

## Settings

| Key                      | Default                        | Purpose                                               |
| ------------------------ | ------------------------------ | ----------------------------------------------------- |
| `sartCompilerClasspath`  | (empty — must be set)          | Jars to run `sart.compiler.Main` in a forked JVM.     |
| `sartFacadeClasspath`    | (empty — must be set)          | TASTy-bearing jars the inspector needs to resolve.    |
| `sartOutDir`             | `<base>/out`                   | Where Dart + pubspec are written.                     |
| `sartSourceRoot`         | `baseDirectory.value`          | Used to relativise `/// Source:` comments.            |
| `sartGoldenDir`          | `<base>/sart-golden`           | Directory of golden files for `sartGoldenVerify`.     |

## Implementation notes

The plugin is Scala 2.12 / sbt 1.x. It runs the Scala 3 Sart compiler as
a **forked JVM subprocess** — no cross-compilation is required. That
keeps the consumer project free to use any Scala 3 version Sart supports.
