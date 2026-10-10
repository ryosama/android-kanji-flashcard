package fr.kanjiflashcards.ui.screens

import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import fr.kanjiflashcards.AnswerField
import fr.kanjiflashcards.ReviewSession
import fr.kanjiflashcards.ui.AppColors
import fr.kanjiflashcards.ui.ReviewLayout
import fr.kanjiflashcards.ui.UiComponents

/** Écran du mode normal : deux cases de réponse et un seul bouton pour les valider ensemble. */
class NormalReviewScreen(
    private val ui: UiComponents,
    private val session: ReviewSession,
    private val layout: ReviewLayout,
    private val onTextChanged: (AnswerField, String) -> Unit,
    private val onUnknown: () -> Unit,
    private val onSubmit: (String, String) -> Unit,
) {
    /** Affiche les deux champs de réponse dans la structure commune des pages de révision. */
    fun show() {
        layout.show { content ->
            val readingInput = addAnswerField(
                content, AnswerField.PRONUNCIATION, session.readingText, session.readingResult,
            )
            val meaningInput = addAnswerField(
                content, AnswerField.MEANING, session.meaningText, session.meaningResult,
            )

            // Bouton unique placé sous les deux cases : aucune saisie n'est corrigée isolément.
            content.addView(ui.button("Valider") {
                val reading = readingInput.text.toString()
                val meaning = meaningInput.text.toString()
                if (reading.isBlank()) {
                    readingInput.error = "Écris une prononciation"
                }
                if (meaning.isBlank()) {
                    meaningInput.error = "Écris une signification"
                }
                if (reading.isNotBlank() && meaning.isNotBlank()) {
                    onSubmit(reading, meaning)
                }
            }.apply {
                isEnabled = !session.finished
            })

            // Abandon de la carte entière : une erreur est enregistrée et la correction est révélée.
            content.addView(ui.button("Je ne sais pas", AppColors.secondaryText) {
                onUnknown()
            }.apply {
                isEnabled = !session.finished
            })
        }
    }

    /**
     * Ajoute un libellé et sa case de saisie, puis son résultat après la validation commune.
     * Renvoie la case pour lire les deux réponses lors du clic sur « Valider ».
     */
    private fun addAnswerField(
        content: LinearLayout,
        field: AnswerField,
        initialText: String,
        result: Boolean?,
    ): EditText {
        val isReading = field == AnswerField.PRONUNCIATION
        val label = ui.text(if (isReading) "Prononciation" else "Signification", 18f, bold = true)
        content.addView(label)

        // Case pour renseigner une prononciation en rōmaji/kana ou un sens en français.
        val input = EditText(ui.activity).apply {
            id = View.generateViewId()
            setText(initialText)
            setTextColor(AppColors.text)
            setHintTextColor(AppColors.secondaryText)
            hint = if (isReading) "Rōmaji, hiragana ou katakana" else "Un des sens en français"
            textSize = 16f
            setSingleLine(true)
            background = ui.roundedBackground(AppColors.surface)
            setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isEnabled = !session.finished
            minHeight = ui.dp(58)
        }
        label.labelFor = input.id
        ui.onTextChanged(input) { value -> onTextChanged(field, value) }

        content.addView(input, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        // Correction propre à chaque champ après la validation de la carte entière.
        if (result != null) {
            content.addView(ui.text(
                value = if (result) "✓ Réponse correcte" else "✕ Réponse incorrecte",
                size = 14f,
                color = if (result) AppColors.success else AppColors.error,
            ))
        }
        return input
    }
}
