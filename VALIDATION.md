# Vérification sur téléphone Android 12

## Vérifications automatisées

- Compilation de l'APK de test réussie.
- 52 457 vérifications du moteur réussies sur les 2 495 kanji, y compris les quatre types de QCM pour chaque entrée.
- Analyse Android Lint : aucune erreur bloquante.
- Les cinq CSV français sont présents dans l'APK.
- Après la réorganisation : compilation des cinq écrans séparés, tests du moteur et analyse Android réussis.
- Documentation vérifiée sur les 92 fonctions Kotlin : chacune possède un commentaire expliquant son rôle. Le découpage est décrit dans [CODE_GUIDE.md](CODE_GUIDE.md).

## Essais sur téléphone à effectuer

Aucun appareil n'était connecté lors du développement et aucun émulateur n'était installé. Les tests du moteur et l'analyse Android automatisée ne remplacent pas ces essais sur le téléphone.

1. Installer l'APK de test. Ouvrir les cinq niveaux et vérifier que les pourcentages commencent à zéro.
2. En mode normal, saisir une lecture en rōmaji, puis un sens. Valider dans les deux ordres. Vérifier les corrections après succès et erreur, et le passage à la carte suivante.
3. Vérifier les équivalences de kana et de longues voyelles, un sens avec accent absent et une petite faute. Vérifier qu'une réponse vide et un sens sans rapport sont refusés.
4. Faire plusieurs questions dans chacun des quatre modes faciles. Vérifier qu'une seule option est valide et que la bonne option apparaît en vert après une erreur.
5. Tourner le téléphone pendant une saisie, après la validation d'un champ et après une réponse complète. La carte, les textes, le résultat et les compteurs doivent rester identiques.
6. Quitter puis relancer l'application : les statistiques et les échéances doivent être conservées. Une carte répondue doit attendre son échéance. Lorsque le niveau est à jour, l'entraînement libre ne doit pas modifier la maîtrise ni les dates.
7. Ouvrir les statistiques N1 : rechercher, filtrer et trier ; faire défiler le tableau dans les deux directions et afficher les lignes suivantes.
8. Exporter, remettre un niveau à zéro, puis importer la sauvegarde. Vérifier la restauration après confirmation. Annuler un import, puis essayer un fichier invalide : la progression doit rester identique.
9. Augmenter la taille du texte dans Android et vérifier les boutons, les champs et le clavier. Vérifier le retour à l'accueil depuis chaque écran.
