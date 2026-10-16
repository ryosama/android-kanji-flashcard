# Vocabulaire Marugoto

La section « Marugoto vocabulaire » regroupe 1 746 cartes issues de 2 656 entrées sources, après fusion des doublons mot/sens. Elle reste indépendante des kanji Marugoto et des cinq niveaux JLPT.

## Documents et rattachement

- `MarugotoStarterCompetencesVocabularyIndex2_FR.pdf` : にゅうもん, index complet.
- `MarugotoElementary1CompetencesVocabularyIndex2_FR.pdf` : しょきゅう 1, index complet.
- `MarugotoStarterWordbook_FR.pdf` : にゅうもん, pages PDF 6 à 81 incluses. Les pages imprimées correspondent aux numéros PDF.

Les index indiquent des leçons 1 à 18 : chaque paire forme un topic 1 à 9. Le lexique illustré indique directement ses topics ; une mention « 6/9 » est disponible dès le topic 6. Les pages « divers », calendriers et compteurs sans topic sont placées au topic 9.

Le choix du livre et du topic maximum est mémorisé séparément de celui des kanji. Une sélection dans しょきゅう 1 inclut tout にゅうもん. Le vocabulaire commence au topic 1 dans les deux livres.

## CSV et extraction

`listes/francais/Marugoto vocabulaire.csv` utilise huit colonnes, sans en-tête et séparées par des points-virgules :

1. Identifiant stable : empreinte du mot kana et de ses significations normalisées.
2. Livre : 1 pour にゅうもん, 2 pour しょきゅう 1.
3. Premier topic d'apparition, de 1 à 9.
4. Graphie japonaise, avec les kanji lorsque le document les fournit.
5. Kana, avec les variantes éventuelles.
6. Rōmaji source, facultatif ; l'application le calcule hors ligne lorsqu'il est absent.
7. Significations françaises séparées par ` | `.
8. Provenances séparées par ` | ` : nom du PDF, page et entrée.

Les entrées dont le mot est identique et les traductions partagent un même sens sont fusionnées. Les différences d'articles, d'espaces ou de ponctuation n'empêchent pas cette fusion. Les significations complémentaires des doublons sont réunies. Le premier livre/topic est conservé ; une graphie kanji trouvée dans un doublon enrichit l'affichage. Des significations différentes, comme « écrire » et « dessiner » pour かきます, restent des cartes distinctes.

La fusion est volontairement prudente : des traductions reformulées sans équivalence textuelle normalisée peuvent rester deux cartes. La provenance permet leur contrôle. Modifier le mot ou les significations peut changer son identifiant ; ajouter un doublon ou changer l'ordre des entrées ne le change pas.

Pour régénérer le CSV, Python 3 et Poppler (`pdftotext`, `pdftohtml`) suffisent :

```bash
python3 scripts/extract-vocabulary.py
```

Le script utilise les coordonnées des tableaux et les styles du lexique illustré. Les onze pages images ont été examinées avec un OCR local puis transcrites et contrôlées visuellement dans `Marugoto vocabulaire complements.tsv`. Ce fichier contient aussi les lectures et traductions des calendriers, nombres, compteurs et légendes isolées. L'OCR n'est requis ni pour régénérer le CSV ni dans l'application. Le script refuse les cellules incomplètes non couvertes par une transcription contrôlée.

Un relevé des entrées sources est produit dans `.tools/vocabulary/extraction.json`, exclu de Git. Les images, les outils OCR et les PDF ne sont pas nécessaires à l'exécution de l'application.

## QCM et progression

Le bouton « Facile » ouvre uniquement ces deux choix :

- Japonais → français : mot et kana visibles, quatre groupes de significations.
- Français → japonais : significations visibles, quatre mots avec leurs kana.

Aucun exercice ne demande de recopier une lecture kana déjà affichée. Les propositions viennent exclusivement du livre/topic choisi et des livres/topics précédents. Les mots identiques, homophones et significations équivalentes sont écartés des distracteurs. Chaque bouton présente le groupe complet, et « Je ne sais pas » révèle la correction en comptant une erreur.

Les cartes utilisent les mêmes paliers Leitner et l'entraînement libre que les autres catalogues. Leurs identifiants commencent par `vocabulary:` ; les identifiants JLPT et `marugoto:` restent inchangés. L'export/import contient les trois catalogues, y compris les cartes masquées par la sélection. Réinitialiser le vocabulaire efface uniquement sa progression, après confirmation.

## Repères pour les essais sur téléphone

| Livre et topic maximum | Cartes incluses |
| --- | ---: |
| にゅうもん · 1 | 135 |
| にゅうもん · 9 | 1 189 |
| しょきゅう 1 · 1 | 1 259 |
| しょきゅう 1 · 9 | 1 746 |

Vérifier les deux sens du QCM, « Je ne sais pas », les textes longs et les mots sans kanji. Tourner l'écran avant et après une réponse : la question, ses propositions et les compteurs doivent rester identiques. Relancer l'application et vérifier les choix de livre/topic/sens. Exporter une progression contenant JLPT, kanji et vocabulaire, puis vérifier l'import et la remise à zéro isolée du vocabulaire.
