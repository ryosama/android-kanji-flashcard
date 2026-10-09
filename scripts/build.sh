#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
export JAVA_HOME="${JAVA_HOME:-$project_dir/.tools/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-$project_dir/.tools/android-sdk}"
export ANDROID_USER_HOME="$project_dir/.tools/android-user"
export GRADLE_USER_HOME="$project_dir/.tools/gradle-home"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
    echo "JDK 17 requis : définir JAVA_HOME ou installer dans .tools/jdk." >&2
    exit 1
fi
if [[ ! -d "$ANDROID_HOME/platforms/android-36" ]]; then
    echo "SDK Android requis : plateforme 36 et build-tools 35.0.0." >&2
    exit 1
fi
if [[ -x "$project_dir/.tools/gradle-8.11.1/bin/gradle" ]]; then
    gradle_bin="$project_dir/.tools/gradle-8.11.1/bin/gradle"
else
    gradle_bin="$project_dir/gradlew"
fi
if [[ $# -eq 0 ]]; then set -- :core:check :app:assembleDebug :app:lintDebug; fi
exec "$gradle_bin" --no-daemon "$@"
