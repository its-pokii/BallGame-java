#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [ ! -f dist/BallGame-Pro-V2.jar ]; then ./build.sh; fi
exec java -jar dist/BallGame-Pro-V2.jar
