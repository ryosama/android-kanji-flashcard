# Kanji Flashcards

Application Android native en Kotlin, en français et en thème sombre. Compatible avec Android 8 et versions suivantes, notamment Android 12. Elle fonctionne entièrement hors ligne, sans compte, publicité ni permission réseau.

## Fonctionnement

- 2 495 kanji provenant des cinq CSV français fournis, du JLPT N5 au N1.
- Mode normal : saisir une lecture et un sens, puis valider chaque champ avec OK. Une lecture valide parmi les lectures on/kun suffit. Les formes complètes et les radicaux indiqués par les parenthèses sont acceptés, ainsi que les hiragana et katakana équivalents.
- Mode facile : choisir signification, rōmaji, kana ou mélange. Chaque question propose quatre réponses distinctes, sans distracteur qui serait une réponse valide pour la carte courante.
- Correction : insensible aux majuscules, accents français, ponctuation et espaces superflus. Une faute est tolérée à partir de cinq lettres, deux à partir de dix ; une transposition adjacente compte pour une faute. Les mots courts restent stricts. Seuls les sens présents dans le catalogue sont acceptés ; les précisions entre parenthèses sont facultatives (par exemple « droite » pour « droite (direction) »).
- Équivalences de lecture : `oo`, `ou`, `ô`, `ō`, ainsi que `tchi` et `chi`. Aucune correction approximative générale des lectures, pour conserver les distinctions entre sons japonais.
- Progression commune aux deux modes. Chaque carte commence à 0 %. Les réussites successives lors des révisions dues conduisent à 20, 40, 60, 80 puis 100 %, avec des intervalles de 1, 2, 4, 8 puis 16 jours. Une erreur ramène à 0 % et programme la carte pour le lendemain. À 100 %, la carte reste à revoir tous les 16 jours ; une erreur la ramène aussi à 0 %.
- Les cartes dues sont tirées au hasard, avec évitement de la répétition immédiate lorsque plusieurs sont disponibles. Tous les nouveaux kanji sont immédiatement disponibles.
- Séance sans limite. Lorsque les cartes sont à jour, l'entraînement libre reste disponible. Il affecte les compteurs mais conserve le palier et l'échéance.
- Le pourcentage d'accueil représente la proportion de kanji à 100 %, arrondie à l'entier inférieur. Le tableau montre aussi les lectures, sens, maîtrise, présentations, succès et erreurs. Recherche, filtres et tri sont disponibles ; les lignes sont chargées par groupes de 50.
- Une présentation est comptée dès qu'une carte s'affiche. Une réponse n'est comptée qu'une fois la question complètement validée. Quitter une carte ne compte pas comme une erreur. Une rotation conserve la carte et les saisies sans doubler les compteurs.

## Sauvegardes

La progression est enregistrée dans l'espace privé de l'application par écriture atomique. Dans « Sauvegarde et réglages », exporter un fichier `kanji-progression.tsv` avec le sélecteur de fichiers Android, puis l'importer sur un autre téléphone. Le format est ouvert, versionné et sans information personnelle.

L'import valide tout le fichier avant de proposer une confirmation. Il remplace ensuite la progression de tous les niveaux. Une sauvegarde invalide ne modifie pas les données existantes. Une remise à zéro par niveau est disponible avec confirmation. La désinstallation supprime les données locales : exporter avant de désinstaller. L'application ne configure aucune sauvegarde automatique en ligne.

## Compilation

Outils : JDK 17, Gradle 8.11.1, Android SDK plateforme 36 et build-tools 35.0.0. Plugins Android 8.9.3 et Kotlin 2.1.20, versions fixes. Le projet utilise les composants natifs Android et n'exige pas de bibliothèque d'interface supplémentaire.

Définir `JAVA_HOME` et `ANDROID_HOME`, ou installer les outils dans `.tools/jdk`, `.tools/android-sdk` et `.tools/gradle-8.11.1`. Le répertoire `.tools` et `local.properties` sont exclus de Git. Le wrapper Gradle permet aussi de télécharger Gradle sur un nouvel environnement.

```bash
./scripts/build.sh
# En réutilisant des dépendances déjà téléchargées :
./scripts/build.sh --offline :core:check :app:assembleDebug :app:lintDebug
```

APK : `app/build/outputs/apk/debug/app-debug.apk`. Il s'agit d'une version de test signée avec une clé de développement, pas d'une publication finale.

## Organisation

- `core` : import CSV, correction, moteur Leitner, QCM et format de sauvegarde ; vérifications indépendantes d'Android.
- `app` : interface Android, fichiers privés et export/import via le sélecteur de documents.
- `listes/francais` : CSV sources, intégrés directement aux ressources de l'application.
- `design UI` : maquettes SVG d'origine.

Les tests parcourent toutes les cartes et les quatre types de QCM, vérifient les collisions de réponses, les délais Leitner, les limites de correction et les sauvegardes invalides.

Avant une diffusion publique, documenter la provenance et les conditions de réutilisation des listes fournies. Aucun compte GitHub, secret ou identité de développeur n'est nécessaire à la compilation.
