package fr.kanjiflashcards.core

/**
 * Entrée du catalogue provenant d'une ligne CSV française.
 *
 * @property level niveau JLPT, de 1 à 5 ; 0 identifie le catalogue Marugoto.
 * @property character caractère JLPT ou mot Marugoto présenté sur la carte.
 * @property on lectures on JLPT ; lectures rōmaji pour Marugoto.
 * @property kun lectures kun JLPT ; lectures kana pour Marugoto.
 * @property meanings sens français possibles, séparés en éléments indépendants.
 */
data class Kanji(
    val level: Int,
    val character: String,
    val on: String,
    val kun: String,
    val meanings: List<String>,
    /** Numéro de ligne du PDF Marugoto ; absent pour les cartes JLPT. */
    val entryNumber: Int? = null,
    /** Topic Marugoto, de 3 à 9 pour le premier livre, de 1 à 9 pour les autres ; indépendante des niveaux JLPT. */
    val lesson: Int? = null,
    /** Livre Marugoto, de 1 (にゅうもん) à 6 (ちゅうきゅう 2), sans changer l'identifiant. */
    val book: Int? = null,
) {
    /** Identifiant durable : niveau/caractère pour JLPT, numéro du PDF pour Marugoto. */
    val id: String
        get() = if (level == StudySelection.MARUGOTO) "marugoto:${requireNotNull(entryNumber)}" else "$level:$character"

    /** Toutes les formes de lecture acceptées, sans doublons, issues des deux colonnes. */
    val readings: Set<String>
        get() = if (level == StudySelection.MARUGOTO) {
            // Les colonnes Marugoto sont déjà séparées ; un exemple « 7さい » reste entier.
            displayReadings.toSet()
        } else {
            (Answers.readings(on) + Answers.readings(kun)).toSet()
        }

    /** Lectures prêtes pour l'affichage, sans découper les préfixes numériques des exemples. */
    private val displayReadings: List<String>
        get() = if (level == StudySelection.MARUGOTO) {
            (on.split(',') + kun.split(',')).map(String::trim).filter(String::isNotBlank).distinct()
        } else {
            (Answers.displayReadings(on) + Answers.displayReadings(kun)).distinct()
        }

    /** Toutes les lectures latines à afficher ensemble, avec leurs terminaisons optionnelles. */
    val romaji: List<String>
        get() = displayReadings
            .filter { reading -> reading.any { it in 'a'..'z' } }

    /** Toutes les lectures japonaises à afficher ensemble, avec leurs terminaisons optionnelles. */
    val kana: List<String>
        get() = displayReadings
            .filter { reading -> reading.any { it in '\u3040'..'\u30ff' } }
}
