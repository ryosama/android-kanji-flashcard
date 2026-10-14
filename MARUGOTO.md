# Catalogue Marugoto

Une carte correspond à une ligne du fichier `MarugotoIntermediate2KanjiList.pdf`, y compris lorsqu'elle contient un mot complet, une expression ou un suffixe. Les 668 lignes ne sont pas fusionnées avec les cartes JLPT, ni entre elles.

## Liste utilisée par l'application

`listes/francais/Marugoto.csv` contient exactement cinq colonnes, sans en-tête, séparées par des points-virgules :

1. Numéro original du PDF, de 1 à 668.
2. Mot ou expression écrit dans le PDF.
3. Lecture originale en kana, avec ses variantes ou exemples.
4. Numéro de la colonne de leçon du PDF ; une cellule vide devient 18.
5. Significations françaises, séparées par des virgules.

Les numéros sont utilisés tels quels, sans notion de livre ou de thème dans l'application. En particulier, la valeur 4 des sections intermédiaires reste la leçon 4, conformément au périmètre demandé. Les 14 termes sans numéro, lignes 481 à 494, sont affectés à la leçon 18.

Les traductions tiennent compte du mot et de son usage dans la méthode : les deux lignes `水` correspondent respectivement à « eau » (`みず`) et « mercredi » (`すい`). Elles ont deux identifiants et deux progressions distinctes.

Les glossaires français fournis servent de références pour les termes correspondants. Les formes verbales et les mots ambigus ont également été contrôlés avec le dictionnaire [JMdict de l'EDRDG](https://www.edrdg.org/). Ce dictionnaire n'est pas embarqué dans l'application ; les significations françaises du CSV restent modifiables.

## Réextraire le PDF

Le script utilise Python 3 et `pdftotext` (Poppler). Il respecte les deux tableaux de chaque page et rassemble les cellules de lecture sur plusieurs lignes :

```bash
./scripts/extract-marugoto.py \
  listes/francais/MarugotoIntermediate2KanjiList.pdf \
  listes/francais/Marugoto.csv
```

Lors d'une réextraction, la cinquième colonne existante est conservée par numéro. Le script refuse une extraction dont les identifiants ne correspondent pas exactement à 1..668. Il est adapté à la disposition du PDF fourni ; un autre document pourra nécessiter des positions de colonnes différentes.

## Lectures et formes verbales

`listes/francais/Marugoto variantes.tsv` contient les lectures additionnelles contrôlées. Chaque ligne indique le numéro du PDF, une tabulation puis les variantes séparées par `|`. Les lignes commençant par `#` sont des commentaires.

Exemple : la carte 5 affiche `食べます`, accepte `たべます` et `たべる`, ainsi que `tabemasu` et `taberu`. Les verbes présentés à la forme du dictionnaire acceptent aussi la forme polie lorsqu'elle est renseignée. Les annotations comme `～ようび(げつようび)` sont traitées comme un suffixe et son exemple, tandis que `(する)` produit une forme nominale et une forme verbale.

`MarugotoReadings.kt` prépare ces lectures, et `KanaRomaji.kt` produit les rōmaji hors ligne. Les formes Marugoto usuelles comme `gakusei` / `gakusee` et les variantes de consonnes doublées sont acceptées. Les autres règles de correction de l'application restent communes aux deux catalogues.

## Filtre et progression

Le panneau d'accueil contient un curseur de 1 à 18. La limite est mémorisée sur le téléphone et sert simultanément au tirage des cartes, aux distracteurs du QCM et aux statistiques. La leçon 1 est proposée lors de la première utilisation.

| Jusqu'à la leçon | Cartes incluses |
| --- | ---: |
| 1 | 26 |
| 2 | 62 |
| 3 | 93 |
| 4 | 291 |
| 5 | 326 |
| 6 | 358 |
| 7 | 384 |
| 8 | 414 |
| 9 | 442 |
| 10 | 467 |
| 11 | 491 |
| 12 | 521 |
| 13 | 550 |
| 14 | 565 |
| 15 | 583 |
| 16 | 612 |
| 17 | 629 |
| 18 | 668 |

Les identifiants Marugoto ont la forme `marugoto:5`. Les identifiants JLPT conservent leur forme `5:水`. Modifier le curseur masque ou révèle des cartes sans modifier leurs paliers, dates et compteurs. L'export TSV inclut les progressions des deux catalogues, même pour les leçons masquées ; les anciennes sauvegardes JLPT restent importables.

Le bouton « Réinitialiser Marugoto » remet à zéro les 668 cartes après confirmation, sans toucher aux JLPT. La limite de leçon mémorisée reste inchangée.

Seuls les CSV et les variantes TSV sont embarqués dans l'APK. Les PDF restent dans le dépôt comme documents sources et de référence.
