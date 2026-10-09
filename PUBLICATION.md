# Publication sur GitHub

Destination : https://github.com/ryosama/android-kanji-flashcard

## État du projet

- Branche principale : `main` ; historique conservé avec des identités de contribution génériques.
- Dépôt distant `origin` configuré en HTTPS vers la destination ci-dessus.
- Sources, catalogue, maquettes, icône, wrapper Gradle et documentation suivis par Git.
- Outils locaux, caches, APK, clés de signature, fichiers d'environnement et sauvegardes personnelles exclus du dépôt.
- Compilation et vérifications automatisées dans `.github/workflows/android.yml`.
- Code sans licence pour le moment, selon le choix du mainteneur. La provenance des ressources reste à compléter dans [CREDITS.md](CREDITS.md).

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

L'envoi des sources et la distribution d'une version Android sont deux étapes distinctes. Les artefacts du workflow sont des APK de test signés avec la clé de développement du runner ; cette clé peut changer entre deux compilations. Pour distribuer des mises à jour installables durablement, préparer une version release avec une clé de signature stable conservée hors de Git, puis joindre son APK à une GitHub Release.

La documentation des essais est dans [VALIDATION.md](VALIDATION.md). Le fonctionnement de l'application est présenté dans [README.md](README.md).
