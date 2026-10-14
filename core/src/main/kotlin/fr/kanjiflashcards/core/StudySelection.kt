package fr.kanjiflashcards.core

/** Périmètre d'une séance : un niveau JLPT, ou les leçons Marugoto de 1 à lessonLimit. */
data class StudySelection(val level: Int, val lessonLimit: Int = 18) {
    companion object {
        /** Valeur réservée au catalogue Marugoto ; les niveaux JLPT gardent leurs valeurs 1..5. */
        const val MARUGOTO = 0
    }

    init {
        require(level in 0..5 && lessonLimit in 1..18) { "Sélection de révision invalide" }
    }

    /** Libellé affiché dans les en-têtes, sans notion de livre pour Marugoto. */
    val title: String
        get() = if (level == MARUGOTO) "Marugoto" else "JLPT N$level"

    /** Indique les leçons incluses ; le niveau JLPT se suffit à lui-même. */
    val subtitle: String
        get() = if (level == MARUGOTO) "Leçons 1 à $lessonLimit" else ""

    /** Vérifie l'appartenance d'une carte au périmètre ; changer le filtre ne modifie pas ses statistiques. */
    fun contains(card: Kanji): Boolean {
        return card.level == level && (level != MARUGOTO || requireNotNull(card.lesson) <= lessonLimit)
    }

    /** Renvoie le même ensemble de cartes pour le tirage, les distracteurs et les statistiques. */
    fun cards(catalog: List<Kanji>): List<Kanji> = catalog.filter(::contains)
}
