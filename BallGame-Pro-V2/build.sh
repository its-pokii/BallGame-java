#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/classes dist
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -d build/classes
cp -R src/main/resources/. build/classes/
printf 'Manifest-Version: 1.0\nMain-Class: com.ballgame.pro.BallGame\n\n' > build/manifest.mf
jar --create --file dist/BallGame-Pro-V2.jar --manifest build/manifest.mf -C build/classes .
echo "JAR prêt : dist/BallGame-Pro-V2.jar"
