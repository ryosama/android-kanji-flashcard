package fr.kanjiflashcards.core

/** Périmètre JLPT ou cumul Marugoto : livres précédents et limite dans le livre choisi. */
data class StudySelection(val level: Int, val lessonLimit: Int = 9, val bookLimit: Int = 6) {
    companion object {
        /** Valeur réservée à Marugoto ; les niveaux JLPT restent 1..5. */
        const val MARUGOTO = 0
    }

    init {
        require(level in 0..5 && bookLimit in 1..6) { "Sélection de révision invalide" }
        require(if (level == MARUGOTO) lessonLimit in MarugotoBooks.lessons(bookLimit) else lessonLimit in 1..9) {
            "Topic de révision invalide"
        }
    }

    /** Nom du catalogue affiché dans les en-têtes. */
    val title: String
        get() = if (level == MARUGOTO) "Marugoto" else "JLPT N$level"

    /** Rappelle le livre et la limite ; les livres précédents sont toujours inclus. */
    val subtitle: String
        get() = if (level == MARUGOTO) "${MarugotoBooks.name(bookLimit)} · ${MarugotoBooks.lessonLabel(bookLimit, lessonLimit)} · cumul des livres précédents" else ""

    /** Masque les cartes futures sans modifier leurs identifiants ni leur progression. */
    fun contains(card: Kanji): Boolean {
        if (card.level != level) return false
        if (level != MARUGOTO) return true

        val book = requireNotNull(card.book)
        return book < bookLimit || (book == bookLimit && requireNotNull(card.lesson) <= lessonLimit)
    }

    /** Partage le même ensemble ordonné par numéro PDF entre révision, QCM et statistiques. */
    fun cards(catalog: List<Kanji>): List<Kanji> {
        val selected = catalog.filter(::contains)
        return if (level == MARUGOTO) selected.sortedBy { it.entryNumber } else selected
    }
}
