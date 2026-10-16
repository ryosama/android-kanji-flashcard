# Catalogue Marugoto

Une carte correspond à une ligne du fichier `MarugotoIntermediate2KanjiList.pdf`, y compris lorsqu'elle contient un mot complet, une expression ou un suffixe. Les 668 lignes ne sont pas fusionnées avec les cartes JLPT, ni entre elles.

## Liste utilisée par l'application

`listes/francais/Marugoto.csv` contient exactement six colonnes, sans en-tête, séparées par des points-virgules :

1. Numéro original du PDF, de 1 à 668.
2. Mot ou expression écrit dans le PDF.
3. Lecture originale en kana, avec ses variantes ou exemples.
4. Topic : de 3 à 9 pour le premier livre, de 1 à 9 pour les autres. Les termes annexes du livre 4 rejoignent le topic 9.
5. Significations françaises, séparées par des virgules.
6. Numéro du livre, de 1 à 6, dans l’ordre du PDF.

Dans les trois premiers livres, les deux leçons d’un même topic sont réunies : les leçons 5/6 du PDF deviennent le topic 3, les leçons 17/18 le topic 9. Le livre 4 utilise directement les topics de la colonne du PDF ; ses 14 termes annexes, lignes 481 à 494, rejoignent le topic 9. Dans les livres 5 et 6, la colonne « 4 » désigne Part 4 : le CSV reprend les numéros des en-têtes de topic, de 1 à 9. Les frontières des livres et des topics intermédiaires sont documentées dans le script d’extraction.

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

## Réponses complémentaires en mode normal

`NormalAnswers.expand` ajoute les lectures et significations des cartes JLPT dont le caractère correspond exactement à la carte Marugoto. La carte 月 accepte ainsi `tsuki`, `getsu`, `gatsu`, leurs kana, ainsi que « lune », « mois » et « lundi ». Les réponses supplémentaires sont aussi affichées dans la correction après validation ou « Je ne sais pas ».

Le rapprochement porte sur le texte entier : un composé ou un verbe ne reçoit pas les réponses des kanji pris séparément. Les variantes du PDF et les formes verbales déjà acceptées sont conservées. Cette extension concerne le mode normal ; le catalogue des propositions du QCM et les CSV JLPT restent inchangés. Les paliers des cartes Marugoto restent indépendants de ceux des cartes JLPT.

## Filtre et progression

Le panneau d'accueil propose d'abord le livre, puis le dernier topic à inclure. Les deux choix sont mémorisés sur le téléphone et servent au tirage des cartes, aux distracteurs du QCM et aux statistiques. Tous les livres précédents sont inclus entièrement, y compris les termes anciennement placés en annexe.

| Livre | Numéro CSV | Lignes PDF | Cartes du livre | Total avec les précédents |
| --- | ---: | --- | ---: | ---: |
| にゅうもん | 1 | 1–65 | 65 | 65 |
| しょきゅう 1 | 2 | 66–221 | 156 | 221 |
| しょきゅう 2 | 3 | 222–391 | 170 | 391 |
| しょちゅうきゅう | 4 | 392–494 | 103 | 494 |
| ちゅうきゅう 1 | 5 | 495–578 | 84 | 578 |
| ちゅうきゅう 2 | 6 | 579–668 | 90 | 668 |

Exemple : `しょきゅう 1 · Topic 2` inclut les 65 cartes du premier livre et les lignes 66 à 104 du second, soit 104 cartes. Le tableau des statistiques suit le numéro original du PDF et affiche le livre avec le topic. Le tirage de révision conserve son fonctionnement Leitner.

Le sous-menu propose les topics 3 à 9 pour にゅうもん et 1 à 9 pour les cinq autres livres. À la mise à jour, les anciens choix de leçon sont convertis une seule fois en topics. Par exemple, une limite de leçon 4 dans しょきゅう 1 devient le topic 2, avec le même ensemble de cartes.

Les identifiants Marugoto ont la forme `marugoto:5`. Les identifiants JLPT conservent leur forme `5:水`. Modifier le livre ou la limite masque ou révèle des cartes sans modifier leurs paliers, dates et compteurs. L'export TSV inclut les progressions des deux catalogues, même pour les topics masqués ; les anciennes sauvegardes JLPT restent importables.

Le bouton « Réinitialiser Marugoto » remet à zéro les 668 cartes après confirmation, sans toucher aux JLPT. Les choix de livre et de topic mémorisés restent inchangés.

Seuls les CSV et les variantes TSV sont embarqués dans l'APK. Les PDF restent dans le dépôt comme documents sources et de référence.
