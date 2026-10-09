package fr.kanjiflashcards.ui.screens

import fr.kanjiflashcards.ReviewSession
import fr.kanjiflashcards.core.QuestionType
import fr.kanjiflashcards.ui.AppColors
import fr.kanjiflashcards.ui.ReviewLayout
import fr.kanjiflashcards.ui.UiComponents

/** Écran du mode facile : quatre propositions et un bouton « Je ne sais pas ». */
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
            val instruction = if (quiz.type == QuestionType.MIXED) {
                "Lecture ou signification"
            } else {
                quiz.type.title
            }
            content.addView(ui.text(instruction, 20f, bold = true))

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

            // Renoncer compte comme une erreur ; la correction et le bouton suivant restent affichés.
            val unknownButton = ui.button("Je ne sais pas", AppColors.secondaryText) {
                onAnswer(false)
            }.apply {
                isEnabled = !session.finished
            }
            content.addView(unknownButton)
        }
    }
}
