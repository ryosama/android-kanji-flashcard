#!/usr/bin/env bash
set -euo pipefail

# Compiler une release signée et lancer Lint ; --offline peut être fourni en argument.
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
"$project_dir/scripts/build.sh" :app:assembleRelease :app:lintRelease "$@"

# Vérifier la signature de l'APK avant de préparer les fichiers destinés à GitHub.
android_sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$project_dir/.tools/android-sdk}}"
export JAVA_HOME="${JAVA_HOME:-$project_dir/.tools/jdk}"
export PATH="$JAVA_HOME/bin:$PATH"
"$android_sdk/build-tools/35.0.0/apksigner" verify --verbose \
    app/build/outputs/apk/release/app-release.apk

# Utiliser la version réellement compilée et joindre une somme de contrôle au téléchargement.
python3 - <<'PY'
from pathlib import Path
import hashlib
import json
import shutil

outputs = Path('app/build/outputs/apk/release')
metadata = json.loads((outputs / 'output-metadata.json').read_text())
version = metadata['elements'][0]['versionName']
destination = Path('app/build/outputs/github-release')
destination.mkdir(parents=True, exist_ok=True)
apk = destination / f'kanji-flashcards-{version}.apk'
shutil.copyfile(outputs / 'app-release.apk', apk)
checksum = hashlib.sha256(apk.read_bytes()).hexdigest()
(apk.with_suffix('.apk.sha256')).write_text(f'{checksum}  {apk.name}\n')
print(f'APK prêt pour GitHub : {apk}')
print(f'Somme de contrôle : {apk}.sha256')
PY
