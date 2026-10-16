package fr.kanjiflashcards.core

import kotlin.random.Random

/** Les deux exercices de vocabulaire ; aucun ne demande de recopier la lecture déjà affichée. */
enum class VocabularyDirection(val title: String) {
    JAPANESE_TO_FRENCH("Japonais → français"),
    FRENCH_TO_JAPANESE("Français → japonais"),
}

/** Génère les QCM de vocabulaire à partir du seul périmètre déjà étudié. */
object VocabularyQuizzes {
    /** Construit quatre choix et élimine les mots ou sens équivalents à la carte courante. */
    fun make(
        card: Kanji,
        catalog: List<Kanji>,
        direction: VocabularyDirection,
        random: Random = Random.Default,
    ): Quiz {
        require(card.level == StudySelection.VOCABULARY)
        val french = direction == VocabularyDirection.JAPANESE_TO_FRENCH
        val correct = option(card, french)
        // Un même libellé peut appartenir à plusieurs entrées : chacune doit être sans ambiguïté.
        val groups = catalog.filter { it.level == card.level && it.id != card.id }
            .groupBy { option(it, french) }
        val distractors = groups.entries.shuffled(random).asSequence()
            .filter { (label, owners) -> label != correct && owners.none { overlaps(it, card) } }
            .map { it.key }
            .take(3).toList()
        require(distractors.size == 3) { "Pas assez de réponses distinctes dans ce topic" }
        val options = (distractors + correct).shuffled(random)
        return Quiz(
            type = if (french) QuestionType.MEANING else QuestionType.KANA,
            options = options,
            correctIndex = options.indexOf(correct),
            prompt = if (french) card.japaneseLabel else card.meanings.joinToString(" · "),
            instruction = if (french) "Choisis la signification française" else "Choisis le mot japonais",
        )
    }

    /** Affiche toujours le groupe complet de sens ou le mot avec ses kana. */
    private fun option(card: Kanji, french: Boolean): String {
        return if (french) card.meanings.joinToString(" · ") else card.japaneseLabel
    }

    /** Écarte aussi les homographes polysémiques et les synonymes dans le sens inverse. */
    fun overlaps(first: Kanji, second: Kanji): Boolean {
        return first.character == second.character ||
            readingKeys(first).intersect(readingKeys(second)).isNotEmpty() ||
            first.meanings.any { a -> second.meanings.any { b -> meaningKey(a) == meaningKey(b) } } ||
            first.meanings.any { Answers.acceptsMeaning(it, second) } ||
            second.meanings.any { Answers.acceptsMeaning(it, first) }
    }

    /** Compare les variantes kana séparément, y compris les terminaisons optionnelles. */
    private fun readingKeys(card: Kanji): Set<String> {
        return card.kun.split('/', '／', ',', '、').flatMap { value ->
            listOf(value.replace("(", "").replace(")", ""), value.replace(Regex("\\([^)]*\\)"), ""))
        }.map { value ->
            Answers.pronunciation(value).replace("〜", "").replace("～", "").replace("~", "")
        }.filter(String::isNotBlank).toSet()
    }

    /** Des articles différents ne rendent pas deux traductions synonymes utilisables comme pièges. */
    private fun meaningKey(value: String): String {
        return Answers.meaning(value).replace(Regex("^(le |la |les |un |une |des |l )"), "")
    }
}
