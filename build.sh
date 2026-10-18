#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
task="${1:-demo}"
case "$task" in
  demo|test|compile) ;;
  *) echo 'Usage: sh build.sh [demo|test|compile]' >&2; exit 2 ;;
esac
mkdir -p build/classes
find src/main/java src/test/java -name '*.java' | sort > build/sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes @build/sources.txt
case "$task" in
  test) java -cp build/classes theater.TheaterTest ;;
  demo) java -cp build/classes theater.Main ;;
esac
