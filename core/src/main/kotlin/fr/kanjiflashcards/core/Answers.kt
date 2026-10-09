package fr.kanjiflashcards.core

import java.text.Normalizer
import java.util.Locale

/**
 * Correction des réponses : extraction des lectures, équivalences de prononciation
 * et comparaison des sens français avec une tolérance limitée aux fautes de frappe.
 * Les mêmes règles servent à éliminer les distracteurs ambigus dans les QCM.
 */
object Answers {
    /** Premier caractère kana : il sépare la partie rōmaji de la partie japonaise du champ CSV. */
    private val kanaStart = Regex("[\\u3040-\\u30ff]")

    /**
     * Extrait toutes les lectures d'une colonne CSV, complète et sans terminaison entre parenthèses.
     * Exemple : « hito(tsu)ひと(つ) » donne hito, hitotsu, ひと et ひとつ.
     * Les tirets de suffixe/préfixe sont retirés et « (none) » signifie aucune lecture.
     */
    fun readings(raw: String): List<String> {
        if (raw.isBlank() || raw == "(none)") {
            return emptyList()
        }

        val splitIndex = kanaStart.find(raw)?.range?.first ?: raw.length
        val parts = listOf(raw.take(splitIndex), raw.drop(splitIndex))

        return parts.flatMap { part ->
            part.split(',', '、', ';').flatMap { value ->
                val full = value.trim().trim('-').replace("(", "").replace(")", "")
                val stem = value.replace(Regex("\\([^)]*\\)"), "").trim().trim('-')
                listOf(full, stem).filter { it.isNotBlank() }
            }
        }.distinct()
    }

    /**
     * Produit une lecture comparable : minuscules, longues voyelles rōmaji équivalentes,
     * katakana convertis en hiragana, espaces/parenthèses/tirets retirés et tchi assimilé à chi.
     * Ce traitement ne fait pas de correction approximative des sons japonais.
     */
    fun pronunciation(value: String): String {
        val longVowels = value.lowercase(Locale.ROOT)
            .replace("ô", "ou")
            .replace("ō", "ou")
            .replace("ū", "uu")
            .replace("û", "uu")
            .replace("ā", "aa")
            .replace("ī", "ii")
            .replace("ē", "ee")

        // NFKC harmonise les formes Unicode ; le décalage de 0x60 convertit les kana usuels.
        val folded = Normalizer.normalize(longVowels, Normalizer.Form.NFKC).map { character ->
            if (character in '\u30a1'..'\u30f6') {
                (character.code - 0x60).toChar()
            } else {
                character
            }
        }.joinToString("")

        return folded.replace(Regex("[\\s()\\-]"), "")
            .replace("tchi", "chi")
            .replace("oo", "ou")
    }

    /** Accepte une saisie non vide correspondant à au moins une lecture on ou kun de la carte. */
    fun acceptsReading(input: String, kanji: Kanji): Boolean {
        return input.isNotBlank() && kanji.readings.any { reading ->
            pronunciation(reading) == pronunciation(input)
        }
    }

    /** Harmonise un sens français : minuscules, accents retirés, ponctuation et espaces normalisés. */
    fun meaning(value: String): String {
        return Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace('’', '\'')
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Accepte un sens du catalogue, avec ou sans sa précision entre parenthèses.
     * Les mots courts restent stricts ; une faute est admise dès cinq lettres et deux dès dix.
     * Une inversion de lettres voisines compte pour une faute ; les sens partiels sont refusés.
     */
    fun acceptsMeaning(input: String, kanji: Kanji): Boolean {
        val answer = meaning(input)
        if (answer.isEmpty()) {
            return false
        }

        // « droite (direction) » doit accepter « droite » sans demander le contexte explicatif.
        val acceptedMeanings = kanji.meanings.flatMap { value ->
            listOf(value, value.replace(Regex("\\([^)]*\\)"), "").trim())
        }.filter(String::isNotBlank)

        return acceptedMeanings.any { value ->
            val expected = meaning(value)
            val letterCount = minOf(answer.count(Char::isLetter), expected.count(Char::isLetter))
            val tolerance = when {
                letterCount >= 10 -> 2
                letterCount >= 5 -> 1
                else -> 0
            }

            distance(answer, expected) <= tolerance
        }
    }

    /**
     * Calcule le nombre minimal d'ajouts, suppressions, remplacements et transpositions adjacentes.
     * Chaque cellule représente le coût de comparaison des préfixes de deux chaînes
     * (distance dite Optimal String Alignment, une variante de Damerau-Levenshtein).
     */
    private fun distance(answer: String, expected: String): Int {
        val costs = Array(answer.length + 1) { IntArray(expected.length + 1) }

        // Comparer à un texte vide exige autant de suppressions/ajouts qu'il y a de caractères.
        for (row in 0..answer.length) {
            costs[row][0] = row
        }
        for (column in 0..expected.length) {
            costs[0][column] = column
        }

        for (row in 1..answer.length) {
            for (column in 1..expected.length) {
                val replacementCost = if (answer[row - 1] == expected[column - 1]) 0 else 1
                costs[row][column] = minOf(
                    costs[row - 1][column] + 1,
                    costs[row][column - 1] + 1,
                    costs[row - 1][column - 1] + replacementCost,
                )

                // « réunoin » et « réunion » ne diffèrent que par une inversion de deux lettres.
                if (row > 1 && column > 1 &&
                    answer[row - 1] == expected[column - 2] &&
                    answer[row - 2] == expected[column - 1]
                ) {
                    costs[row][column] = minOf(costs[row][column], costs[row - 2][column - 2] + 1)
                }
            }
        }

        return costs[answer.length][expected.length]
    }
}
