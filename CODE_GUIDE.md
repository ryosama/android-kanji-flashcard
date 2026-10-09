# Lire et modifier le code

Le code est organisé par responsabilité. Les commentaires des fonctions expliquent leur rôle ; les commentaires au-dessus des blocs graphiques indiquent les éléments visibles concernés.

## Où modifier un écran ?

Les fichiers sont dans `app/src/main/java/fr/kanjiflashcards/ui/screens/`.

| Fichier | Partie visible | Fonctions à consulter |
| --- | --- | --- |
| `HomeScreen.kt` | Accueil et panneaux N5 → N1 | `show`, `levelPanel`, `levelButton` |
| `NormalReviewScreen.kt` | Cases Prononciation et Signification, boutons OK et « Je ne sais pas » | `show`, `addAnswerField` |
| `EasyReviewScreen.kt` | Consigne, quatre choix et bouton « Je ne sais pas » | `show` |
| `StatisticsScreen.kt` | Recherche, filtre, tri, tableau et pagination | `show`, `addFilterControls`, `addTableRow`, `appendPage` |
| `SettingsScreen.kt` | Export/import et boutons de réinitialisation | `show`, `confirmReset` |

Chaque écran reçoit les données à afficher et des actions à appeler lorsqu'un bouton est utilisé. Par exemple, le bouton OK du mode normal appelle `onSubmit` ; l'activité transmet alors la saisie au moteur de correction. Dans les deux modes, « Je ne sais pas » appelle `completeAnswer(false)` pour enregistrer une erreur sur la carte entière. Les écrans ne lisent pas les CSV et n'enregistrent pas directement la progression.

## Où modifier l'apparence commune ?

Dans `app/src/main/java/fr/kanjiflashcards/ui/` :

- `AppColors.kt` contient les couleurs des pages, panneaux, textes, boutons, corrections et du dégradé de maîtrise.
- `UiComponents.kt` fabrique les textes, boutons, panneaux, lignes et colonnes. `screen` installe le défilement vertical et l'en-tête avec le bouton Accueil. Les tailles et marges communes se modifient ici.
- `ReviewLayout.kt` construit les parties communes aux deux modes : titre, bilan de séance, grand kanji, corrections et bouton de carte suivante. Il affiche aussi la prochaine échéance et l'entraînement libre lorsqu'un niveau est à jour.

Exemples : changer la couleur de tous les boutons principaux dans `AppColors.accent` ; changer leur hauteur minimale dans `UiComponents.button` ; changer la taille du grand kanji dans `ReviewLayout.show` ; changer la largeur des colonnes du tableau dans `StatisticsScreen.columnWidths`.

Les dimensions de mise en page sont exprimées en **dp**, converties par `UiComponents.dp`. Les tailles de caractères sont exprimées en **sp** et suivent le réglage de taille de texte Android. Un poids de `1f` dans une ligne signifie que le composant partage l'espace restant avec les autres composants de même poids.

## Comment circulent les données ?

Dans `app/src/main/java/fr/kanjiflashcards/` :

- `MainActivity.kt` coordonne le chargement, la navigation et les réponses. `nextCard` choisit et compte une présentation ; `validateField` corrige un champ ; `completeAnswer` enregistre le résultat global ; `persist` actualise la mémoire uniquement après une écriture réussie.
- `ReviewSession.kt` garde le kanji affiché, les saisies, le résultat de chaque champ et le bilan de séance. `saveInto` et `restoreFrom` conservent cet état lors d'une rotation, sans tirer une nouvelle carte ni recompter une réponse.
- `ProgressStore.kt` charge/enregistre la progression dans un fichier privé avec écriture atomique.
- `BackupDocuments.kt` ouvre le sélecteur Android et réalise les transferts sur un thread secondaire. Il valide l'import, demande confirmation puis appelle l'activité pour enregistrer les données.

Une bonne réponse suit ce chemin : **bouton de l'écran → MainActivity → moteur core → ProgressStore → écran rafraîchi**. Les saisies non validées sont conservées dans `ReviewSession`, pas dans le fichier de progression.

## Où modifier les règles d'apprentissage ?

Les fichiers sont dans `core/src/main/kotlin/fr/kanjiflashcards/core/`. Ce module ne dépend pas d'Android.

| Fichier | Responsabilité |
| --- | --- |
| `Kanji.kt` | Carte du catalogue, identifiant et variantes de lecture |
| `Catalog.kt` | Lecture des cinq colonnes CSV et validation des listes |
| `Answers.kt` | Équivalences des lectures, normalisation française et tolérance aux fautes |
| `Progress.kt` | Paliers, échéances, compteurs et contrôle de leur cohérence |
| `Leitner.kt` | Progression après une réponse et sélection des cartes dues |
| `Quiz.kt` | Types de questions et données d'un QCM |
| `Quizzes.kt` | Quatre groupes complets de lectures/sens, classiques ou mélangés, avec exclusion des réponses ambiguës |
| `Backup.kt` | Format TSV versionné et validation complète des sauvegardes |

Exemples : les délais sont dans `Leitner.days`, les seuils de fautes dans `Answers.acceptsMeaning`, et les exclusions des distracteurs dans `Quizzes.make`.

Les vérifications existantes sont dans `core/src/test/kotlin/fr/kanjiflashcards/core/EngineChecks.kt`. Elles restent organisées par catalogue, lectures, sens, CSV, Leitner, sauvegardes et QCM. La commande `./scripts/build.sh` exécute ces vérifications, compile l'APK et lance l'analyse Android Lint.
