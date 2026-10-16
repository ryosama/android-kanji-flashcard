# Vérification sur téléphone Android 12

## Vérifications automatisées

- Compilation de l'APK de test réussie.
- Version 0.2.0 : 240 187 vérifications du moteur réussies sur les 2 495 kanji JLPT et les 668 cartes Marugoto : QCM, filtres des 18 leçons, lectures, nombres et compatibilité des sauvegardes.
- Analyse Android Lint : aucune erreur bloquante.
- Les six CSV français et les variantes Marugoto sont présents dans l'APK ; les PDF sources ne sont pas embarqués.
- Après la réorganisation : compilation des cinq écrans séparés, tests du moteur et analyse Android réussis.
- Les fonctions Kotlin possèdent un commentaire expliquant leur rôle. Le découpage est décrit dans [CODE_GUIDE.md](CODE_GUIDE.md).
- Vérification GitHub : moteur, APK debug et Android Lint ont réussi sur un runner GitHub. Le [lancement validé](https://github.com/ryosama/android-kanji-flashcard/actions/runs/38619660891) conserve l'APK de test et les rapports.
- Release 0.1.0 : APK signé compilé, Lint release réussi et signature vérifiée avec `apksigner`. Le paquet n'est pas débogable ; la clé et les mots de passe restent hors de Git et de l'APK.
- La [release publique 0.1.0](https://github.com/ryosama/android-kanji-flashcard/releases/tag/v0.1.0) contient l'APK signé et sa somme SHA-256. Le téléchargement depuis GitHub a été comparé à l'APK local : les fichiers sont identiques et la somme de contrôle est valide.

- Release 0.1.1 : validation commune des deux champs, équivalences numériques testées, APK debug et release compilés, analyses Android réussies et signature identique à la release 0.1.0. La [release publique 0.1.1](https://github.com/ryosama/android-kanji-flashcard/releases/tag/v0.1.1) a été téléchargée et sa somme SHA-256 vérifiée.

- Version 0.2.0 : extraction des 668 lignes contrôlée contre le PDF, 14 entrées sans leçon affectées à 18, traductions présentes, builds debug/release et analyses Android réussis. Les contrôles GitHub ont réussi ; l’APK de la [release 0.2.0](https://github.com/ryosama/android-kanji-flashcard/releases/tag/v0.2.0) a été téléchargé et sa somme SHA-256 vérifiée. Les essais d’interface Marugoto sur téléphone restent à effectuer.

## Version 0.2.1 — sélection Marugoto par livre

- 242 659 vérifications du moteur réussies : six livres, toutes les limites réelles, cumul des livres précédents, QCM et sauvegardes.
- Exemple contrôlé : しょきゅう 1, leçon 4 inclut exactement les lignes 1 à 104 du PDF.
- Les 668 numéros, mots, lectures originales et traductions sont conservés ; six colonnes dans le CSV. La réextraction produit un fichier identique.
- APK debug 0.2.1 (code 4) compilé et Android Lint réussi. Le CSV embarqué est identique à celui du projet ; les PDF sont exclus de l’APK.
- Signature debug vérifiée : même certificat que l’APK installé sur le téléphone. La mise à jour avec `adb install -r app/build/outputs/apk/debug/app-debug.apk` conserve les données.
- Les nouveaux sélecteurs et leur mémorisation restent à vérifier sur téléphone. Cette version n’a pas été publiée sur GitHub.

## Version 0.2.2 — réponses du mode normal et topics

- 247 153 vérifications du moteur réussies sur les 2 495 cartes JLPT et les 668 cartes Marugoto.
- En mode normal Marugoto, les lectures et sens JLPT du même caractère sont acceptés et affichés dans la correction. Le contrôle vérifie toutes les correspondances, le cas 月, les réponses sans rapport et l'absence de rapprochement par simple sous-caractère.
- Topics disponibles : 3..9 pour にゅうもん, 1..9 pour les autres livres. Les anciennes leçons 1..18 sont regroupées par paire dans les trois premiers livres ; les 14 termes annexes du livre 4 rejoignent le topic 9.
- APK debug 0.2.2 (code 5) compilé, Android Lint réussi, CSV embarqué contrôlé et signature identique à la version debug précédente.
- Migration des préférences testée ; identifiants et format de sauvegarde conservés. Réextraction PDF identique au CSV ; mots, lectures et traductions des 668 cartes inchangés.
- Les scénarios d'interface ci-dessous restent à vérifier sur téléphone. Version conservée en local, sans push ni publication GitHub.

## Version 0.3.0 — Marugoto vocabulaire

- 331 019 vérifications réussies : JLPT, kanji Marugoto, 1 746 cartes de vocabulaire, deux sens de QCM et deux périmètres par carte.
- 2 656 entrées sources extraites des trois PDF, pages 6 à 81 incluses pour le lexique illustré. Les onze pages images et les tableaux de calendrier/compteurs ont une transcription contrôlée avec provenance.
- Doublons fusionnés à leur première apparition ; traductions complémentaires conservées et sens disjoints séparés. La réextraction produit le même CSV et les 1 746 identifiants sont uniques.
- Les tests contrôlent toutes les entrées partageant le même bouton : une proposition est écartée si un de ses propriétaires peut répondre à la question. Variantes kana et différences d'articles français contrôlées.
- Sauvegardes anciennes et complètes, séparation des progressions et cumul livre/topic vérifiés. Les identifiants des 2 495 kanji JLPT et des 668 cartes Marugoto restent inchangés.
- APK debug 0.3.0 (code 6) compilé, Android Lint réussi, CSV embarqués comparés aux sources et signature debug conservée. Aucun PDF embarqué.
- Les essais d'interface sur téléphone restent à effectuer : voir [VOCABULAIRE.md](VOCABULAIRE.md). Aucun push ni publication GitHub.

## Release 0.3.0

- Vérifications GitHub des sources réussies : [workflow Android](https://github.com/ryosama/android-kanji-flashcard/actions/runs/38875414917).
- APK release 0.3.0 (code 6) compilé et Android Lint release réussi ; application non débogable.
- Signature vérifiée : même certificat que les releases précédentes. Les deux catalogues Marugoto embarqués correspondent aux sources ; aucun PDF embarqué.
- APK versionné et somme SHA-256 préparés pour la [release 0.3.0](https://github.com/ryosama/android-kanji-flashcard/releases/tag/v0.3.0).

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

## Essais Marugoto sur téléphone

1. Mettre à jour la release, vérifier la conservation des statistiques JLPT et ouvrir le panneau Marugoto.
2. Choisir にゅうもん, topic 9 : 65 cartes ; しょきゅう 1, topic 2 : 104 ; しょきゅう 2, topic 9 : 391 ; ちゅうきゅう 2, topic 9 : 668. Vérifier que にゅうもん propose seulement les topics 3 à 9 et les autres livres 1 à 9 ; le topic 9 du livre 4 inclut ses 14 anciens termes annexes.
3. Fermer et rouvrir l’application, puis tourner l’écran : le livre et la limite choisis doivent rester identiques.
4. En mode normal, vérifier que 月 accepte tsuki/getsu/gatsu et lune/mois/lundi. Répondre aussi à un verbe avec sa forme polie, puis avec sa forme du dictionnaire, en kana et en rōmaji.
5. Dans chaque type du mode facile, vérifier que les réponses viennent seulement des leçons incluses et que les mots longs restent lisibles.
6. Dans les statistiques, contrôler la colonne livre et leçon/topic et la séparation entre les deux cartes 水, eau et mercredi.
7. Répondre à une carte de leçon élevée, choisir un livre ou une limite antérieure puis rétablir la sélection : retrouver ses progrès et compteurs.
8. Exporter et importer une progression contenant JLPT et Marugoto ; vérifier aussi l’import d’un ancien TSV JLPT.
9. Réinitialiser Marugoto après confirmation : ses 668 progressions doivent disparaître sans modifier les JLPT.
