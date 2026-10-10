# Vérification sur téléphone Android 12

## Vérifications automatisées

- Compilation de l'APK de test réussie.
- 164 837 vérifications du moteur réussies sur les 2 495 kanji, y compris les quatre types de QCM pour chaque entrée, les groupes complets de réponses, le mélange des trois formats et les équivalences numériques du catalogue.
- Analyse Android Lint : aucune erreur bloquante.
- Les cinq CSV français sont présents dans l'APK.
- Après la réorganisation : compilation des cinq écrans séparés, tests du moteur et analyse Android réussis.
- Documentation vérifiée sur les 96 fonctions Kotlin : chacune possède un commentaire expliquant son rôle. Le découpage est décrit dans [CODE_GUIDE.md](CODE_GUIDE.md).
- Vérification GitHub : moteur, APK debug et Android Lint ont réussi sur un runner GitHub. Le [lancement validé](https://github.com/ryosama/android-kanji-flashcard/actions/runs/38037999039) conserve l'APK de test et les rapports.
- Release 0.1.0 : APK signé compilé, Lint release réussi et signature vérifiée avec `apksigner`. Le paquet n'est pas débogable ; la clé et les mots de passe restent hors de Git et de l'APK.
- La [release publique 0.1.0](https://github.com/ryosama/android-kanji-flashcard/releases/tag/v0.1.0) contient l'APK signé et sa somme SHA-256. Le téléchargement depuis GitHub a été comparé à l'APK local : les fichiers sont identiques et la somme de contrôle est valide.

- Release 0.1.1 : validation commune des deux champs, équivalences numériques testées, APK debug et release compilés, analyses Android réussies et signature identique à la release 0.1.0. La [release publique 0.1.1](https://github.com/ryosama/android-kanji-flashcard/releases/tag/v0.1.1) a été téléchargée et sa somme SHA-256 vérifiée.

## Essais de non-régression sur téléphone

Le fonctionnement de l'application sur téléphone a été confirmé par l'utilisateur avant la préparation de la publication. Aucun appareil ni émulateur n'a été utilisé pour les vérifications automatisées du développement. Les scénarios ci-dessous servent aux essais de non-régression lors des prochaines modifications.

1. Installer l'APK de test. Ouvrir les cinq niveaux et vérifier que les pourcentages commencent à zéro.
2. En mode normal, saisir une lecture en rōmaji, puis un sens. Vérifier qu’un seul bouton « Valider » apparaît sous les deux champs et corrige les deux réponses ensemble. Une saisie vide doit afficher une erreur sans compter de réponse. Vérifier les corrections après succès et erreur, et le passage à la carte suivante. Utiliser « Je ne sais pas » avant toute saisie, puis après avoir rempli un seul champ : une seule erreur doit être comptée et les deux champs doivent être bloqués.
3. Pour les kanji numériques, saisir une prononciation correcte et une signification en chiffres (`2`, `100`, `1000`, `10 000`). Vérifier qu’un nombre voisin est refusé et que les formes en lettres restent acceptées. Vérifier les équivalences de kana et de longues voyelles, un sens avec accent absent et une petite faute. Vérifier qu'une réponse vide et un sens sans rapport sont refusés.
4. Faire plusieurs questions dans chacun des quatre modes faciles. Chaque bouton doit présenter toutes les lectures on/kun du format choisi (parenthèses conservées) ou tous les sens français ; vérifier que les textes longs restent entièrement lisibles. En Mélange, vérifier que les quatre propositions combinent français, rōmaji et kana. Vérifier qu'une seule option est valide et que la bonne option apparaît en vert après une erreur. Appuyer sur « Je ne sais pas » : une erreur doit être comptée, la correction affichée et les cinq boutons bloqués jusqu'au passage au kanji suivant.
5. Tourner le téléphone pendant une saisie et après une réponse complète. La carte, les textes, le résultat et les compteurs doivent rester identiques.
6. Quitter puis relancer l'application : les statistiques et les échéances doivent être conservées. Une carte répondue doit attendre son échéance. Lorsque le niveau est à jour, l'entraînement libre ne doit pas modifier la maîtrise ni les dates.
7. Ouvrir les statistiques N1 : rechercher, filtrer et trier ; faire défiler le tableau dans les deux directions et afficher les lignes suivantes.
8. Exporter, remettre un niveau à zéro, puis importer la sauvegarde. Vérifier la restauration après confirmation. Annuler un import, puis essayer un fichier invalide : la progression doit rester identique.
9. Augmenter la taille du texte dans Android et vérifier les boutons, les champs et le clavier. Vérifier le retour à l'accueil depuis chaque écran.
