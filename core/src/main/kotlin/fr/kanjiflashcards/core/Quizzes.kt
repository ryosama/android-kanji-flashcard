package fr.kanjiflashcards.core

import kotlin.random.Random

/**
 * Générateur des QCM du mode facile, à partir des autres cartes du même niveau.
 * Chaque bouton présente toutes les lectures ou tous les sens d'une carte.
 * Un distracteur partageant une réponse acceptée avec la carte courante est écarté.
 */
object Quizzes {
    /** Groupe complet de réponses et format du bouton, conservés pendant le mélange. */
    private data class Candidate(val parts: List<String>, val type: QuestionType) {
        /** Séparateur visible entre les lectures on/kun ou entre les significations françaises. */
        val text: String
            get() = parts.joinToString(" · ")
    }

    /**
     * Prépare quatre boutons dont un seul correspond au kanji courant.
     * Mélange réserve une place à chacun des trois formats, puis tire le quatrième librement.
     * Un format classique absent de la carte est remplacé par un format disponible.
     */
    fun make(
        card: Kanji,
        catalog: List<Kanji>,
        requested: QuestionType,
        random: Random = Random.Default,
    ): Quiz {
        val types = QuestionType.entries.filter { it != QuestionType.MIXED }
        val available = types.filter { values(card, it).isNotEmpty() }
        val correctType = if (requested == QuestionType.MIXED || requested !in available) {
            available.random(random)
        } else {
            requested
        }
        val correct = Candidate(values(card, correctType), correctType)
        val selected = mutableListOf(correct)
        val poolTypes = if (requested == QuestionType.MIXED) types else listOf(correctType)

        // Le tirage précède la correction coûteuse ; seuls les premiers candidats valides sont utiles.
        val pools = poolTypes.associateWith { type ->
            catalog.filter { it.level == card.level && it.id != card.id }
                .shuffled(random)
                .asSequence()
                .map { Candidate(values(it, type), type) }
                .filter { it.parts.isNotEmpty() }
                .filter { !overlaps(it, card) }
                .distinctBy { optionKey(it) }
                .take(4)
                .toList()
        }

        if (requested == QuestionType.MIXED) {
            // Les deux autres formats complètent celui de la bonne réponse, dans un ordre aléatoire.
            for (type in types.filter { it != correctType }.shuffled(random)) {
                pickUnused(pools.getValue(type), selected, random)?.let { selected.add(it) }
            }
        }

        val allDistractors = pools.values.flatten()
        while (selected.size < 4) {
            val next = pickUnused(allDistractors, selected, random)
            require(next != null) { "Pas assez de réponses distinctes" }
            selected.add(next)
        }

        val shuffled = selected.shuffled(random)
        return Quiz(
            type = if (requested == QuestionType.MIXED) QuestionType.MIXED else correctType,
            options = shuffled.map { it.text },
            correctIndex = shuffled.indexOf(correct),
            optionTypes = shuffled.map { it.type },
        )
    }

    /**
     * Écarte un groupe dès qu'un de ses éléments serait accepté comme réponse à la carte courante.
     * Les lectures optionnelles sont examinées avec et sans terminaison, comme en mode normal.
     */
    private fun overlaps(candidate: Candidate, card: Kanji): Boolean {
        val parts = if (candidate.type == QuestionType.MEANING) {
            candidate.parts
        } else {
            candidate.parts.flatMap(Answers::readings)
        }
        return parts.any { Answers.acceptsReading(it, card) || Answers.acceptsMeaning(it, card) }
    }

    /** Tire un distracteur dont le groupe n'est pas équivalent à un bouton déjà retenu. */
    private fun pickUnused(
        candidates: List<Candidate>,
        selected: List<Candidate>,
        random: Random,
    ): Candidate? {
        val usedKeys = selected.map { optionKey(it) }.toSet()
        return candidates.filter { optionKey(it) !in usedKeys }.randomOrNull(random)
    }

    /**
     * Compare les groupes indépendamment de l'ordre de leurs éléments.
     * Les accents français sont harmonisés, tandis que les marques sonores des kana sont conservées.
     */
    private fun optionKey(candidate: Candidate): List<String> {
        return candidate.parts.map { value ->
            if (candidate.type == QuestionType.MEANING) {
                Answers.meaning(value)
            } else {
                Answers.pronunciation(value)
            }
        }.distinct().sorted()
    }

    /** Renvoie tous les sens ou toutes les lectures à réunir sur un même bouton. */
    private fun values(card: Kanji, type: QuestionType): List<String> {
        return when (type) {
            QuestionType.MEANING -> card.meanings
            QuestionType.ROMAJI -> card.romaji
            QuestionType.KANA -> card.kana
            QuestionType.MIXED -> emptyList()
        }
    }
}
