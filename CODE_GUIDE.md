# Lire et modifier le code

Le code est organisé par responsabilité. Les commentaires des fonctions expliquent leur rôle ; les commentaires au-dessus des blocs graphiques indiquent les éléments visibles concernés.

## Où modifier un écran ?

Les fichiers sont dans `app/src/main/java/fr/kanjiflashcards/ui/screens/`.

| Fichier | Partie visible | Fonctions à consulter |
| --- | --- | --- |
| `HomeScreen.kt` | Accueil et panneaux N5 → N1 | `show`, `levelPanel`, `levelButton` |
| `MarugotoPanel.kt` | Panneau d’accueil Marugoto, choix livre puis topic maximum et actions Normal/Statistiques/Facile | `create`, `selector`, `actionButton` |
| `NormalReviewScreen.kt` | Cases Prononciation et Signification, bouton commun « Valider » et « Je ne sais pas » | `show`, `addAnswerField` |
| `EasyReviewScreen.kt` | Consigne, quatre choix et bouton « Je ne sais pas » | `show` |
| `StatisticsScreen.kt` | Recherche, filtre, tri, tableau et pagination | `show`, `addFilterControls`, `addTableRow`, `appendPage` |
| `SettingsScreen.kt` | Export/import et boutons de réinitialisation | `show`, `confirmReset` |

Chaque écran reçoit les données à afficher et des actions à appeler lorsqu'un bouton est utilisé. Par exemple, le bouton « Valider » du mode normal appelle `onSubmit` ; l'activité transmet alors les deux saisies au moteur de correction. Dans les deux modes, « Je ne sais pas » appelle `completeAnswer(false)` pour enregistrer une erreur sur la carte entière. Les écrans ne lisent pas les CSV et n'enregistrent pas directement la progression.

## Où modifier l'apparence commune ?

Dans `app/src/main/java/fr/kanjiflashcards/ui/` :

- `AppColors.kt` contient les couleurs des pages, panneaux, textes, boutons, corrections et du dégradé de maîtrise.
- `UiComponents.kt` fabrique les textes, boutons, panneaux, lignes et colonnes. `screen` installe le défilement vertical et l'en-tête avec le bouton Accueil. Les tailles et marges communes se modifient ici.
- `ReviewLayout.kt` construit les parties communes aux deux modes : titre, bilan de séance, grand kanji, corrections et bouton de carte suivante. Il affiche aussi la prochaine échéance et l'entraînement libre lorsqu'un niveau est à jour.

Exemples : changer la couleur de tous les boutons principaux dans `AppColors.accent` ; changer leur hauteur minimale dans `UiComponents.button` ; changer la taille du grand kanji dans `ReviewLayout.show` ; changer la largeur des colonnes du tableau dans `StatisticsScreen.columnWidths`.

L'icône de l'application provient de `icon.png`, à la racine du projet. Sa copie intégrée à l'APK est `app/src/main/res/drawable-nodpi/ic_launcher.png`, référencée par `android:icon` dans `AndroidManifest.xml`. Après modification de l'image source, remplacer cette copie puis recompiler l'APK.

Les dimensions de mise en page sont exprimées en **dp**, converties par `UiComponents.dp`. Les tailles de caractères sont exprimées en **sp** et suivent le réglage de taille de texte Android. Un poids de `1f` dans une ligne signifie que le composant partage l'espace restant avec les autres composants de même poids.

## Comment circulent les données ?

Dans `app/src/main/java/fr/kanjiflashcards/` :

- `MainActivity.kt` coordonne le chargement, la navigation et les réponses. `nextCard` choisit et compte une présentation ; `validateAnswers` corrige les deux champs ensemble ; `completeAnswer` enregistre le résultat global ; `persist` actualise la mémoire uniquement après une écriture réussie.
- `ReviewSession.kt` garde le kanji affiché, les saisies, le résultat de chaque champ et le bilan de séance. `saveInto` et `restoreFrom` conservent cet état lors d'une rotation, sans tirer une nouvelle carte ni recompter une réponse.
- `ProgressStore.kt` charge/enregistre la progression dans un fichier privé avec écriture atomique.
- `BackupDocuments.kt` ouvre le sélecteur Android et réalise les transferts sur un thread secondaire. Il valide l'import, demande confirmation puis appelle l'activité pour enregistrer les données.

Une bonne réponse suit ce chemin : **bouton de l'écran → MainActivity → moteur core → ProgressStore → écran rafraîchi**. Les saisies non validées sont conservées dans `ReviewSession`, pas dans le fichier de progression.

## Où modifier les règles d'apprentissage ?

Les fichiers sont dans `core/src/main/kotlin/fr/kanjiflashcards/core/`. Ce module ne dépend pas d'Android.

| Fichier | Responsabilité |
| --- | --- |
| `Kanji.kt` | Carte du catalogue, identifiant JLPT ou numéro Marugoto et variantes de lecture |
| `StudySelection.kt` | Filtre commun du tirage, des distracteurs et des statistiques |
| `MarugotoBooks.kt` | Ordre et noms des six livres, topics disponibles et migration des anciennes limites |
| `VocabularyQuizzes.kt` | Deux sens de traduction du vocabulaire et élimination des propositions ambiguës |
| `NormalAnswers.kt` | Réponses JLPT supplémentaires en mode normal Marugoto et correspondance exacte |
| `MarugotoReadings.kt` | Lectures du PDF, annotations, variantes verbales et rōmaji |
| `KanaRomaji.kt` | Transcription kana → rōmaji hors ligne |
| `Catalog.kt` | Lecture des cinq colonnes CSV et validation des listes |
| `Answers.kt` | Équivalences des lectures, normalisation française et tolérance aux fautes |
| `MeaningNumbers.kt` | Équivalences exactes entre les significations numériques en chiffres et en lettres |
| `Progress.kt` | Paliers, échéances, compteurs et contrôle de leur cohérence |
| `Leitner.kt` | Progression après une réponse et sélection des cartes dues |
| `Quiz.kt` | Types de questions et données d'un QCM |
| `Quizzes.kt` | Quatre groupes complets de lectures/sens, classiques ou mélangés, avec exclusion des réponses ambiguës |
| `Backup.kt` | Format TSV versionné et validation complète des sauvegardes |

Exemples : les délais sont dans `Leitner.days`, les seuils de fautes dans `Answers.acceptsMeaning`, et les exclusions des distracteurs dans `Quizzes.make`.

Le CSV Marugoto et les lectures additionnelles sont décrits dans [MARUGOTO.md](MARUGOTO.md). La sélection passe par `ReviewSession.selection`, et les préférences `marugotoBook` et `marugotoLesson` sont enregistrées par l’activité. La remise à zéro utilise les identifiants du catalogue concerné pour préserver les autres sources.

Les vérifications existantes sont dans `core/src/test/kotlin/fr/kanjiflashcards/core/EngineChecks.kt`. `MarugotoChecks.kt` couvre aussi les 668 lignes, toutes les limites par livre et leçon/topic, les formes verbales et la compatibilité des sauvegardes. Les contrôles JLPT restent organisés par catalogue, lectures, sens, CSV, Leitner, sauvegardes et QCM. La commande `./scripts/build.sh` exécute ces vérifications, compile l'APK et lance l'analyse Android Lint.

Le vocabulaire est décrit dans [VOCABULAIRE.md](VOCABULAIRE.md). `Catalog.parseVocabulary` lit le CSV fusionné ; `StudySelection.VOCABULARY` définit son périmètre. `ReviewSession` conserve ses propres livre/topic et sens de traduction, ainsi que le texte exact du QCM lors des rotations. Le panneau `MarugotoPanel` est commun aux deux sections, avec deux livres et deux actions seulement pour le vocabulaire. `VocabularyChecks` contrôle chaque carte dans les deux sens et dans deux périmètres.
