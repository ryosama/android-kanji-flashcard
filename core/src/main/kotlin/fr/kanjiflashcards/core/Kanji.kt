package fr.kanjiflashcards.core

/**
 * Entrée du catalogue provenant d'une ligne CSV française.
 *
 * @property level niveau JLPT, de 1 (avancé) à 5 (débutant).
 * @property character caractère japonais présenté sur la carte.
 * @property on lectures d'origine chinoise, conservées dans leur format CSV rōmaji/kana.
 * @property kun lectures japonaises, parfois complétées par des terminaisons entre parenthèses.
 * @property meanings sens français possibles, séparés en éléments indépendants.
 */
data class Kanji(
    val level: Int,
    val character: String,
    val on: String,
    val kun: String,
    val meanings: List<String>,
) {
    /** Identifiant durable : le numéro de ligne CSV peut changer, le niveau et le kanji restent stables. */
    val id: String
        get() = "$level:$character"

    /** Toutes les formes de lecture acceptées, sans doublons, issues des deux colonnes. */
    val readings: Set<String>
        get() = (Answers.readings(on) + Answers.readings(kun)).toSet()

    /** Lectures en alphabet latin utilisées pour les questions rōmaji du mode facile. */
    val romaji: List<String>
        get() = readings.filter { reading -> reading.any { it in 'a'..'z' } }.sorted()

    /** Lectures japonaises utilisées pour les questions en hiragana/katakana du mode facile. */
    val kana: List<String>
        get() = readings.filter { reading -> reading.any { it in '\u3040'..'\u30ff' } }.sorted()
}
