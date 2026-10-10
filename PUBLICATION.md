# Publication sur GitHub

Destination : https://github.com/ryosama/android-kanji-flashcard

Le dépôt est public. La [dernière release](https://github.com/ryosama/android-kanji-flashcard/releases/latest) fournit l'APK signé et sa somme SHA-256.

## État du projet

- Branche principale : `main` ; historique conservé avec des identités de contribution génériques.
- Dépôt distant `origin` configuré en HTTPS vers la destination ci-dessus.
- Sources, catalogue, maquettes, icône, wrapper Gradle et documentation suivis par Git.
- Outils locaux, caches, APK, clés de signature, fichiers d'environnement et sauvegardes personnelles exclus du dépôt.
- Compilation et vérifications automatisées dans `.github/workflows/android.yml`.
- Code sans licence pour le moment, selon le choix du mainteneur.

## Premier envoi

Le dépôt doit exister dans le compte `ryosama`. S'il est à créer, utiliser GitHub pour créer `android-kanji-flashcard` **sans initialiser de README, de licence ou de .gitignore**, car ces fichiers et l'historique existent déjà localement. Choisir sa visibilité lors de la création.

Configurer une authentification GitHub valide sur l'ordinateur, puis :

```bash
git remote -v
git status
git push -u origin main
```

Ne jamais placer un jeton dans l'URL du dépôt ou dans un fichier suivi par Git. L'authentification peut passer par GitHub CLI, un gestionnaire d'identifiants ou une clé SSH.

Avec une clé SSH déjà enregistrée dans le compte GitHub, il est aussi possible de remplacer l'URL distante :

```bash
git remote set-url origin git@github.com:ryosama/android-kanji-flashcard.git
git push -u origin main
```

Si le dépôt distant contient déjà des commits, récupérer et examiner cet historique avant de l'intégrer ; aucun envoi forcé n'est prévu.

## Après l'envoi

Dans GitHub, vérifier les fichiers et le lancement du workflow **Android** dans l'onglet **Actions**. Une compilation réussie fournit un APK de test et les rapports d'analyse dans les artefacts du lancement.

Description suggérée du dépôt : « Application Android hors ligne de révision des kanji JLPT N5 à N1, en français, avec QCM, système Leitner et sauvegarde de progression. »

Sujets suggérés : `android`, `kotlin`, `kanji`, `jlpt`, `flashcards`, `leitner`, `offline`, `french`.

## Distribution de l'application

La version `0.1.1` est préparée avec une signature release stable. Le script suivant compile l'APK, lance Android Lint, vérifie sa signature et prépare le téléchargement et sa somme SHA-256 :

```bash
./scripts/build-release.sh
# Avec les dépendances déjà présentes :
./scripts/build-release.sh --offline
```

Python 3 est nécessaire pour préparer le nom versionné et la somme de contrôle. Les fichiers destinés à GitHub se trouvent dans `app/build/outputs/github-release/`.

### Conserver la clé de signature

La clé privée est `.signing/release.jks`. Ses mots de passe sont dans `keystore.properties`. Ces deux fichiers sont exclus de Git et ne doivent jamais être joints à une release. **Sauvegarder les deux dans un emplacement privé et sûr** : les mises à jour doivent être signées avec la même clé.

Sur un nouveau poste, restaurer ces deux fichiers pour continuer à publier les mises à jour. `keystore.properties.example` décrit la configuration attendue sans contenir de mot de passe. Les builds debug et le workflow de vérification GitHub fonctionnent sans clé release.

### Publier une version

Après compilation et commit des sources, créer et envoyer le tag de version, puis joindre les fichiers à la release avec GitHub CLI :

```bash
git push -u origin main
git tag -a v0.1.1 -m 'Kanji Flashcards 0.1.1'
git push origin v0.1.1
gh release create v0.1.1 \
  app/build/outputs/github-release/kanji-flashcards-0.1.1.apk \
  app/build/outputs/github-release/kanji-flashcards-0.1.1.apk.sha256 \
  --repo ryosama/android-kanji-flashcard \
  --verify-tag --title 'Kanji Flashcards 0.1.1' \
  --notes-file release/v0.1.1.md
```

Pour une nouvelle version, augmenter `versionCode` et `versionName` dans `app/build.gradle.kts`, rédiger ses notes et adapter le tag et les noms des fichiers. Ne pas remplacer une clé de signature déjà utilisée.

Les APK debug fournis par le workflow de vérification restent des versions de test. Pour passer d'une version debug à la release, exporter la progression, désinstaller le debug, installer la release puis importer le TSV : les signatures sont différentes.

La documentation des essais est dans [VALIDATION.md](VALIDATION.md). Le fonctionnement de l'application est présenté dans [README.md](README.md).
