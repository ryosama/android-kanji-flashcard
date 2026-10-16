package fr.kanjiflashcards

import android.os.Bundle
import fr.kanjiflashcards.core.Kanji
import fr.kanjiflashcards.core.MarugotoBooks
import fr.kanjiflashcards.core.QuestionType
import fr.kanjiflashcards.core.Quiz
import fr.kanjiflashcards.core.StudySelection
import fr.kanjiflashcards.core.VocabularyDirection

/** Identifie le champ de saisie dans le mode normal, sans utiliser un booléen ambigu. */
enum class AnswerField {
    PRONUNCIATION,
    MEANING,
}

/**
 * État d'une séance en cours, partagé avec l'écran de révision.
 * La progression durable reste dans ProgressStore ; cette classe conserve la carte,
 * les saisies et les compteurs de séance lors d'une recréation de l'activité Android.
 */
class ReviewSession {
    /** Niveau JLPT courant, de N1 à N5. */
    var level = 5

    /** Limite cumulative Marugoto ; changer cette limite ne supprime aucune statistique. */
    var marugotoLesson = 3

    /** Livre courant ; les livres précédents sont inclus entièrement dans la séance. */
    var marugotoBook = 1

    /** Périmètre commun du tirage, des distracteurs et du tableau de progression. */
    val selection: StudySelection
        get() = if (level == StudySelection.VOCABULARY) {
            StudySelection(level, vocabularyLesson, vocabularyBook)
        } else {
            StudySelection(level, marugotoLesson, marugotoBook)
        }

    /** Sélection indépendante du vocabulaire, comprenant toujours les livres précédents. */
    var vocabularyBook = 1
    var vocabularyLesson = 1

    /** Sens de traduction préféré pour les QCM de vocabulaire. */
    var vocabularyDirection = VocabularyDirection.JAPANESE_TO_FRENCH

    /** true pour les quatre choix du QCM, false pour les deux champs du mode normal. */
    var easy = false

    /** Type de question préféré en mode facile ; également conservé dans les préférences. */
    var questionType = QuestionType.MIXED

    /** En entraînement libre, les réponses ne changent ni les paliers ni les échéances. */
    var practice = false

    /** Carte affichée ; null signifie qu'aucune carte n'est due pour le niveau choisi. */
    var card: Kanji? = null

    /** Options et bonne réponse du QCM courant ; absent en mode normal. */
    var quiz: Quiz? = null

    /** null = champ non validé, true = bonne réponse, false = mauvaise réponse. */
    var readingResult: Boolean? = null
    var meaningResult: Boolean? = null

    /** Saisies conservées pendant une rotation du téléphone ou une tentative de validation. */
    var readingText = ""
    var meaningText = ""

    /** Évite de compter plusieurs fois une réponse déjà enregistrée. */
    var finished = false

    /** Résultat global utilisé pour le bouton « Bonne/Mauvaise réponse → ». */
    var resultCorrect = false

    /** Bilan de la séance actuelle, indépendant des compteurs durables par kanji. */
    var correctCount = 0
    var answerCount = 0

    /** Prépare une nouvelle séance et remet son bilan à zéro. */
    fun begin(level: Int, easy: Boolean) {
        this.level = level
        this.easy = easy || level == StudySelection.VOCABULARY
        practice = false
        correctCount = 0
        answerCount = 0
    }

    /** Remplace la carte et efface les validations et les saisies de la carte précédente. */
    fun select(card: Kanji?) {
        this.card = card
        readingResult = null
        meaningResult = null
        readingText = ""
        meaningText = ""
        finished = false
    }

    /** Note le résultat de la carte une fois son enregistrement sur le téléphone réussi. */
    fun complete(correct: Boolean) {
        finished = true
        resultCorrect = correct
        answerCount++

        if (correct) {
            correctCount++
        }
    }

    /** Copie la séance dans le Bundle Android, sans recompter la présentation de la carte. */
    fun saveInto(state: Bundle) {
        state.putInt("level", level)
        state.putInt("marugotoLesson", marugotoLesson)
        state.putInt("marugotoBook", marugotoBook)
        state.putBoolean("marugotoTopics", true)
        state.putInt("vocabularyBook", vocabularyBook)
        state.putInt("vocabularyLesson", vocabularyLesson)
        state.putString("vocabularyDirection", vocabularyDirection.name)
        state.putBoolean("easy", easy)
        state.putBoolean("practice", practice)
        state.putString("card", card?.id)
        state.putString("type", questionType.name)
        state.putBoolean("finished", finished)
        state.putBoolean("result", resultCorrect)
        state.putInt("total", answerCount)
        state.putInt("correct", correctCount)
        state.putString("readingText", readingText)
        state.putString("meaningText", meaningText)
        state.putInt("readingResult", encodeResult(readingResult))
        state.putInt("meaningResult", encodeResult(meaningResult))

        quiz?.let { currentQuiz ->
            state.putStringArrayList("options", ArrayList(currentQuiz.options))
            state.putString("quizType", currentQuiz.type.name)
            state.putString("quizPrompt", currentQuiz.prompt)
            state.putString("quizInstruction", currentQuiz.instruction)
            state.putInt("quizCorrect", currentQuiz.correctIndex)
            state.putStringArrayList("optionTypes", ArrayList(currentQuiz.optionTypes.map { it.name }))
        }
    }

    /** Restaure les saisies, la carte et les options exactes après une rotation de l'écran. */
    fun restoreFrom(state: Bundle, catalog: List<Kanji>) {
        level = state.getInt("level", 5)
        marugotoBook = state.getInt("marugotoBook", marugotoBook).coerceIn(1, 6)
        val restoredLesson = state.getInt("marugotoLesson", marugotoLesson)
        marugotoLesson = if (state.getBoolean("marugotoTopics")) {
            MarugotoBooks.normalizeLesson(marugotoBook, restoredLesson)
        } else {
            MarugotoBooks.migrateLesson(marugotoBook, restoredLesson)
        }
        vocabularyBook = state.getInt("vocabularyBook", vocabularyBook).coerceIn(1, 2)
        vocabularyLesson = state.getInt("vocabularyLesson", vocabularyLesson).coerceIn(1, 9)
        vocabularyDirection = VocabularyDirection.valueOf(state.getString("vocabularyDirection") ?: vocabularyDirection.name)
        easy = state.getBoolean("easy")
        practice = state.getBoolean("practice")
        card = catalog.find { it.id == state.getString("card") }
        questionType = QuestionType.valueOf(state.getString("type") ?: "MIXED")
        finished = state.getBoolean("finished")
        resultCorrect = state.getBoolean("result")
        answerCount = state.getInt("total")
        correctCount = state.getInt("correct")
        readingText = state.getString("readingText") ?: ""
        meaningText = state.getString("meaningText") ?: ""
        readingResult = decodeResult(state.getInt("readingResult", -1))
        meaningResult = decodeResult(state.getInt("meaningResult", -1))

        state.getStringArrayList("options")?.let { options ->
            val quizType = QuestionType.valueOf(requireNotNull(state.getString("quizType")))
            val optionTypes = state.getStringArrayList("optionTypes")?.map { QuestionType.valueOf(it) }
                ?: List(options.size) { quizType }

            quiz = Quiz(
                type = quizType,
                options = options,
                correctIndex = state.getInt("quizCorrect"),
                optionTypes = optionTypes,
                prompt = state.getString("quizPrompt"),
                instruction = state.getString("quizInstruction"),
            )
        }
    }

    /** Représente les trois états d'une validation par -1 (absente), 0 (fausse), 1 (vraie). */
    private fun encodeResult(result: Boolean?): Int {
        return when (result) {
            null -> -1
            false -> 0
            true -> 1
        }
    }

    /** Reconstitue une validation facultative à partir de sa valeur sauvegardée. */
    private fun decodeResult(value: Int): Boolean? {
        return if (value < 0) null else value == 1
    }
}
