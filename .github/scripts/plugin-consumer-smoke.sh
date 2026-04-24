#!/usr/bin/env bash
# Stand up a minimal Scala 3 + Flutter project that uses sbt-sart via
# `enablePlugins(SartPlugin)` and prove sartEmit produces Dart that
# `flutter analyze` is happy with. Treated as the canonical
# "external user" smoke test in CI.
set -euo pipefail

WORK="$(mktemp -d)"
echo "consumer smoke test dir: $WORK"
cd "$WORK"

mkdir -p project src/main/scala/smoke

cat > project/build.properties <<'EOF'
sbt.version=1.10.7
EOF

cat > project/plugins.sbt <<'EOF'
addSbtPlugin("com.outr" % "sbt-sart" % "0.1.0-SNAPSHOT")
EOF

cat > build.sbt <<'EOF'
ThisBuild / scalaVersion := "3.8.3"
ThisBuild / organization := "smoke"

lazy val root = (project in file("."))
  .enablePlugins(SartPlugin)
  .settings(name := "sart_smoke")
EOF

cat > src/main/scala/smoke/Hello.scala <<'EOF'
package smoke

import flutter.runApp
import flutter.material.*

@main def smokeMain(): Unit = runApp(SmokeApp())

class SmokeApp extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    MaterialApp(
      title = "Sart CI smoke",
      home = Scaffold(
        appBar = AppBar(title = Text("CI smoke")),
        body = Center(child = Text("ok"))
      )
    )
EOF

# Emit + analyze
sbt -Dsbt.color=false sartEmit

pushd out
flutter analyze lib/ --suppress-analytics
popd

echo "plugin consumer smoke: PASS"
