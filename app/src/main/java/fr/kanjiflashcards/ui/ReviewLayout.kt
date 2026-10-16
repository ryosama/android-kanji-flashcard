package fr.kanjiflashcards.ui

import android.view.Gravity
import android.widget.LinearLayout
import fr.kanjiflashcards.ReviewSession
import fr.kanjiflashcards.core.StudySelection
import fr.kanjiflashcards.core.Kanji

/**
 * Présentation commune aux deux écrans de révision : en-tête, kanji et résultat global.
 * La partie centrale (champs du mode normal ou boutons du mode facile) est fournie par l'écran.
 */
class ReviewLayout(
    private val ui: UiComponents,
    private val session: ReviewSession,
    private val mastery: Int,
    private val nextDue: Long?,
    private val onNext: () -> Unit,
    private val onPractice: () -> Unit,
    /** Réponses élargies en mode normal ; la carte de séance garde son identité et sa progression. */
    private val answerCard: Kanji? = session.card,
) {
    /** Assemble la page de révision et insère les contrôles propres au mode au centre. */
    fun show(answerContent: (LinearLayout) -> Unit) {
        val content = ui.screen(session.selection.title)
        if (session.selection.subtitle.isNotBlank()) {
            content.addView(ui.text(session.selection.subtitle, 14f, AppColors.secondaryText))
        }
        val modeLabel = if (session.easy) "Mode facile" else "Mode normal"
        val scheduleLabel = if (session.practice) "Entraînement libre" else "Révision espacée"

        content.addView(ui.text("$modeLabel · $scheduleLabel", 14f, AppColors.accent))
        content.addView(ui.text(
            "Séance : ${session.correctCount} / ${session.answerCount} réponses correctes",
            14f,
            AppColors.secondaryText,
        ))

        val card = session.card
        if (card == null) {
            showCompletedLevel(content)
            return
        }

        // Carte centrale : le caractère à reconnaître, affiché en grand sur un fond sombre.
        val wordSize = when {
            card.character.length <= 1 -> 108f
            card.character.length <= 4 -> 64f
            card.character.length <= 8 -> 44f
            else -> 32f
        }
        // En vocabulaire, le sens du QCM détermine si la carte montre le français ou le japonais.
        val prompt = session.quiz?.prompt ?: card.character
        val displaySize = if (card.level == StudySelection.VOCABULARY) 28f else wordSize
        val kanjiView = ui.text(prompt, displaySize, bold = true).apply {
            gravity = Gravity.CENTER
            background = ui.roundedBackground(AppColors.surface)
            setPadding(0, ui.dp(12), 0, ui.dp(12))
            contentDescription = when {
                card.level == StudySelection.VOCABULARY -> prompt
                card.level == StudySelection.MARUGOTO -> "Mot ${card.character}"
                else -> "Kanji ${card.character}"
            }
        }
        content.addView(kanjiView)
        answerContent(content)

        if (session.finished) {
            showResult(content)
        }
    }

    /** Affiche la prochaine échéance et le bouton d'entraînement quand le niveau est à jour. */
    private fun showCompletedLevel(content: LinearLayout) {
        content.addView(ui.text("À jour pour cette sélection", 26f, bold = true))
        content.addView(ui.text(
            "Prochaine révision : ${nextDue?.let { ui.date(it) } ?: "maintenant"}",
            color = AppColors.secondaryText,
        ))
        content.addView(ui.text(
            "Tu peux continuer à t'entraîner. Le palier de maîtrise et l'échéance resteront identiques.",
        ))
        content.addView(ui.button("Entraînement libre", action = onPractice))
    }

    /** Affiche les bonnes réponses puis le bouton coloré qui permet de passer au kanji suivant. */
    private fun showResult(content: LinearLayout) {
        val card = requireNotNull(answerCard)
        content.addView(ui.text("Lectures : ${card.readings.joinToString(" · ")}", color = AppColors.success))
        content.addView(ui.text("Sens : ${card.meanings.joinToString(", ")}", color = AppColors.success))

        val label = if (session.resultCorrect) "Bonne réponse →" else "Mauvaise réponse →"
        val color = if (session.resultCorrect) AppColors.success else AppColors.error
        content.addView(ui.button(label, color, onNext))
        content.addView(ui.text("Maîtrise : $mastery %", 14f, AppColors.secondaryText))
    }
}
