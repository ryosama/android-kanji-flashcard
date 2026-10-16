package fr.kanjiflashcards.core

/** Réponses élargies du mode normal Marugoto, indépendantes des choix du QCM. */
object NormalAnswers {
    /**
     * Complète une carte Marugoto avec les lectures et sens de ses correspondances JLPT exactes.
     * Un mot composé comme 日本 ne reçoit pas les sens de 日 ou de 本 séparément.
     * La copie garde l'identifiant PDF ; les fichiers sources et la progression restent inchangés.
     */
    fun expand(card: Kanji, catalog: List<Kanji>): Kanji {
        if (card.level != StudySelection.MARUGOTO) return card

        val matches = catalog.filter { it.level in 1..5 && it.character == card.character }
        if (matches.isEmpty()) return card

        val readings = (card.readings + matches.flatMap { it.readings }).distinct()
        return card.copy(
            on = readings.filter { reading -> reading.any { it in 'a'..'z' } }.joinToString(", "),
            kun = readings.filter { reading -> reading.any { it in '\u3040'..'\u30ff' } }.joinToString(", "),
            meanings = (card.meanings + matches.flatMap { it.meanings }).distinct(),
        )
    }
}
