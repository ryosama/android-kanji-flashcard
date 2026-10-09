package fr.kanjiflashcards.ui.screens

import fr.kanjiflashcards.ReviewSession
import fr.kanjiflashcards.ui.AppColors
import fr.kanjiflashcards.ui.ReviewLayout
import fr.kanjiflashcards.ui.UiComponents

/** Écran du mode facile : consigne de lecture/sens et quatre boutons de réponse. */
class EasyReviewScreen(
    private val ui: UiComponents,
    private val session: ReviewSession,
    private val layout: ReviewLayout,
    private val onAnswer: (Boolean) -> Unit,
) {
    /** Affiche les choix ; après validation, tous sont bloqués et la bonne réponse devient verte. */
    fun show() {
        layout.show { content ->
            val quiz = requireNotNull(session.quiz)
            content.addView(ui.text(quiz.type.title, 20f, bold = true))

            for ((index, option) in quiz.options.withIndex()) {
                val isCorrectOption = index == quiz.correctIndex
                val color = if (session.finished && isCorrectOption) {
                    AppColors.success
                } else {
                    AppColors.secondaryAction
                }

                // Cliquer sur une option valide immédiatement la réponse de cette carte.
                val answerButton = ui.button(option, color) { onAnswer(isCorrectOption) }.apply {
                    isEnabled = !session.finished
                }
                content.addView(answerButton)
            }
        }
    }
}
