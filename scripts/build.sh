#!/usr/bin/env bash
set -euo pipefail

# Retrouver la racine du projet, quel que soit le répertoire depuis lequel le script est appelé.
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

# Réutiliser les outils configurés, ou les installations locales exclues du dépôt Git.
export JAVA_HOME="${JAVA_HOME:-$project_dir/.tools/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$project_dir/.tools/android-sdk}}"
# Respecter les répertoires configurés sur un autre poste ou par GitHub Actions.
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$project_dir/.tools/android-user}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$project_dir/.tools/gradle-home}"

# Vérifier les prérequis avant de lancer Gradle, avec un message compréhensible en cas d'absence.
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
    echo "JDK 17 requis : définir JAVA_HOME ou installer dans .tools/jdk." >&2
    exit 1
fi
if [[ ! -d "$ANDROID_HOME/platforms/android-36" ]]; then
    echo "SDK Android requis : plateforme 36 et build-tools 35.0.0." >&2
    exit 1
fi

# Préférer la distribution locale ; le wrapper peut télécharger Gradle sur un autre poste.
if [[ -x "$project_dir/.tools/gradle-8.11.1/bin/gradle" ]]; then
    gradle_bin="$project_dir/.tools/gradle-8.11.1/bin/gradle"
else
    gradle_bin="$project_dir/gradlew"
fi

# Sans argument : vérifier le moteur, produire l'APK de test et analyser le code Android.
if [[ $# -eq 0 ]]; then
    set -- :core:check :app:assembleDebug :app:lintDebug
fi

exec "$gradle_bin" --no-daemon "$@"
