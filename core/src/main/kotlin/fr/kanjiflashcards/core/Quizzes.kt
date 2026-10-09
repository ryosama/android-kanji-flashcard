package fr.kanjiflashcards.core

import kotlin.random.Random

/**
 * Générateur des QCM du mode facile, à partir des autres cartes du même niveau.
 * Les distracteurs ne peuvent pas être un autre sens/une autre lecture valide du kanji courant.
 */
object Quizzes {
    /**
     * Prépare quatre choix mélangés : une réponse valide et trois distracteurs distincts.
     * Le mode Mélange choisit une consigne disponible ; un type sans lecture est aussi remplacé.
     * Une erreur explicite signale un catalogue trop pauvre pour fournir trois distracteurs.
     */
    fun make(
        card: Kanji,
        catalog: List<Kanji>,
        requested: QuestionType,
        random: Random = Random.Default,
    ): Quiz {
        val possibleTypes = QuestionType.entries.filter { type ->
            type != QuestionType.MIXED && values(card, type).isNotEmpty()
        }
        val type = if (requested == QuestionType.MIXED || requested !in possibleTypes) {
            possibleTypes.random(random)
        } else {
            requested
        }

        // Toutes les réponses valides sont exclues des distracteurs, pas seulement celle affichée.
        val accepted = values(card, type).map { normalize(it, type) }.toSet()
        val correct = values(card, type).random(random)
        val distractors = catalog.filter { it.id != card.id }
            .flatMap { values(it, type) }
            .filter { normalize(it, type) !in accepted }
            .filter { type != QuestionType.MEANING || !Answers.acceptsMeaning(it, card) }
            .distinctBy { normalize(it, type) }
            .shuffled(random)
            .take(3)

        require(distractors.size == 3) { "Pas assez de réponses distinctes" }
        val options = (distractors + correct).shuffled(random)
        return Quiz(type, options, options.indexOf(correct))
    }

    /** Renvoie les sens ou les lectures adaptés à une consigne concrète de QCM. */
    private fun values(card: Kanji, type: QuestionType): List<String> {
        return when (type) {
            QuestionType.MEANING -> card.meanings
            QuestionType.ROMAJI -> card.romaji
            QuestionType.KANA -> card.kana
            QuestionType.MIXED -> emptyList()
        }
    }

    /** Compare les options avec les mêmes équivalences que les réponses du mode normal. */
    private fun normalize(value: String, type: QuestionType): String {
        return if (type == QuestionType.MEANING) {
            Answers.meaning(value)
        } else {
            Answers.pronunciation(value)
        }
    }
}
