package fr.kanjiflashcards.core

/** Ordre des six livres dans le PDF ; les index du CSV commencent à 1. */
object MarugotoBooks {
    /** Noms affichés dans la liste de livres et les en-têtes de révision. */
    val names = listOf("にゅうもん", "しょきゅう 1", "しょきゅう 2", "しょちゅうきゅう", "ちゅうきゅう 1", "ちゅうきゅう 2")

    /** Renvoie le nom d'un livre à partir de son numéro dans le CSV. */
    fun name(book: Int): String = names[book - 1]

    /** Chaque livre se termine au topic 9, y compris l'annexe du livre 4. */
    fun lessonLimit(book: Int): Int {
        require(book in 1..6) { "Livre Marugoto invalide" }
        return 9
    }

    /** Affiche le numéro du topic choisi dans les sélecteurs et en-têtes. */
    fun lessonLabel(book: Int, lesson: Int): String {
        require(lesson in lessons(book)) { "Topic Marugoto invalide" }
        return "Topic $lesson"
    }

    /** Le livre にゅうもん commence au topic 3 ; les cinq autres commencent au topic 1. */
    fun lessons(book: Int): List<Int> {
        return ((if (book == 1) 3 else 1)..lessonLimit(book)).toList()
    }

    /** Ramène une préférence aux topics disponibles du livre choisi. */
    fun normalizeLesson(book: Int, lesson: Int): Int = lesson.coerceIn(lessons(book).first(), lessonLimit(book))

    /** Convertit une ancienne leçon 1..18 en topic ; l'annexe 18 du livre 4 rejoint le topic 9. */
    fun migrateLesson(book: Int, oldLesson: Int): Int {
        val topic = if (book <= 3) (oldLesson + 1) / 2 else oldLesson
        return normalizeLesson(book, topic)
    }
}
