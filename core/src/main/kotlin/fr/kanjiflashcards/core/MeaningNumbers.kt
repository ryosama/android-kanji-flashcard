package fr.kanjiflashcards.core

import java.math.BigInteger

/**
 * Équivalences des significations numériques du catalogue, en chiffres ou en lettres françaises.
 * Seul un sens entièrement numérique est converti : « les deux » et « deuxième » restent des mots.
 */
object MeaningNumbers {
    /** Nombres exprimés en lettres dans les listes, dont les unités japonaises 万 et 億. */
    private val writtenNumbers = mapOf(
        "zero" to 0L,
        "un" to 1L,
        "deux" to 2L,
        "trois" to 3L,
        "quatre" to 4L,
        "cinq" to 5L,
        "six" to 6L,
        "sept" to 7L,
        "huit" to 8L,
        "neuf" to 9L,
        "dix" to 10L,
        "cent" to 100L,
        "mille" to 1_000L,
        "dix mille" to 10_000L,
        "cent millions" to 100_000_000L,
        "billion" to 1_000_000_000_000L,
    ).mapValues { BigInteger.valueOf(it.value) } + mapOf(
        "dix puissance vingt quatre" to BigInteger.TEN.pow(24),
        "dix puissance vingt huit" to BigInteger.TEN.pow(28),
    )

    /** Entier simple ou regroupé par milliers, avec espaces ordinaires ou insécables. */
    private val digits = Regex("(?:[0-9]+|[0-9]{1,3}(?:[\\s\\u00a0\\u202f]+[0-9]{3})+)")

    /** Séparateurs retirés après vérification du regroupement des chiffres par milliers. */
    private val spacing = Regex("[\\s\\u00a0\\u202f]+")

    /**
     * Reconnaît un nombre exact, sans arrondi ni correction approximative de ses chiffres.
     * BigInteger conserve les valeurs exactes au-delà des limites de Long.
     * Les décimales, signes et textes mêlés aux chiffres sont refusés.
     */
    fun value(raw: String): BigInteger? {
        val trimmed = raw.trim()
        if (digits.matches(trimmed)) {
            return trimmed.replace(spacing, "").toBigIntegerOrNull()
        }

        return writtenNumbers[Answers.meaning(trimmed)]
    }
}
