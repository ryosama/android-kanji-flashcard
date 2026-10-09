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

/** Écran du mode normal : une case de prononciation et une case de signification, chacune avec OK. */
class NormalReviewScreen(
    private val ui: UiComponents,
    private val session: ReviewSession,
    private val layout: ReviewLayout,
    private val onTextChanged: (AnswerField, String) -> Unit,
    private val onSubmit: (AnswerField, String) -> Unit,
) {
    /** Affiche les deux champs de réponse dans la structure commune des pages de révision. */
    fun show() {
        layout.show { content ->
            addAnswerField(content, AnswerField.PRONUNCIATION, session.readingText, session.readingResult)
            addAnswerField(content, AnswerField.MEANING, session.meaningText, session.meaningResult)
        }
    }

    /**
     * Ajoute un libellé, une case de saisie et son bouton OK, puis le résultat du champ.
     * Le champ devient non modifiable après sa validation, même si l'autre attend une réponse.
     */
    private fun addAnswerField(
        content: LinearLayout,
        field: AnswerField,
        initialText: String,
        result: Boolean?,
    ) {
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
            isEnabled = result == null && !session.finished
            minHeight = ui.dp(58)
        }
        label.labelFor = input.id
        ui.onTextChanged(input) { value -> onTextChanged(field, value) }

        // Le champ occupe la place restante ; son bouton de validation garde une largeur fixe.
        val answerRow = ui.row()
        answerRow.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val validateButton = ui.button("OK") {
            if (input.text.isNullOrBlank()) {
                input.error = "Écris une réponse"
            } else {
                onSubmit(field, input.text.toString())
            }
        }.apply {
            isEnabled = result == null && !session.finished
            layoutParams = LinearLayout.LayoutParams(ui.dp(64), ui.dp(58)).apply {
                setMargins(ui.dp(8), 0, 0, 0)
            }
        }
        answerRow.addView(validateButton)
        content.addView(answerRow)

        // Retour local du champ ; le résultat global apparaît une fois les deux champs validés.
        if (result != null) {
            content.addView(ui.text(
                value = if (result) "✓ Réponse correcte" else "✕ Réponse incorrecte",
                size = 14f,
                color = if (result) AppColors.success else AppColors.error,
            ))
        }
    }
}
