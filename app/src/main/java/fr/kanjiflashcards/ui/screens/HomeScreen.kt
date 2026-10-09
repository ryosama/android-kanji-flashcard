package fr.kanjiflashcards.ui.screens

import android.app.AlertDialog
import android.widget.LinearLayout
import fr.kanjiflashcards.core.Kanji
import fr.kanjiflashcards.core.Progress
import fr.kanjiflashcards.core.QuestionType
import fr.kanjiflashcards.ui.AppColors
import fr.kanjiflashcards.ui.UiComponents

/**
 * Page d'accueil : un panneau par niveau, avec les boutons Normal, Statistiques et Facile.
 * Les callbacks transmettent les choix à l'activité ; cet écran ne modifie pas la progression.
 */
class HomeScreen(
    private val ui: UiComponents,
    private val catalog: List<Kanji>,
    private val progress: Map<String, Progress>,
    private val questionType: QuestionType,
    private val onNormal: (Int) -> Unit,
    private val onEasy: (Int, QuestionType) -> Unit,
    private val onStatistics: (Int) -> Unit,
    private val onSettings: () -> Unit,
) {
    /** Affiche le titre, le total des cartes disponibles et les cinq panneaux JLPT. */
    fun show() {
        val content = ui.screen("Kanji\nFlashcards", showHomeButton = false)
        content.addView(ui.text("Un caractère à la fois.", 17f, AppColors.secondaryText))

        val dueCount = catalog.count { (progress[it.id]?.due ?: 0) <= System.currentTimeMillis() }
        content.addView(ui.text("$dueCount cartes disponibles · 5 niveaux JLPT", 14f, AppColors.accent))

        // Les débutants trouvent le niveau N5 en premier, le niveau N1 étant le plus avancé.
        for (level in 5 downTo 1) {
            content.addView(levelPanel(level))
        }

        content.addView(ui.button("Sauvegarde et réglages", AppColors.secondaryAction, onSettings))
    }

    /** Construit le panneau d'un niveau et aligne ses trois boutons à largeur égale. */
    private fun levelPanel(level: Int): LinearLayout {
        val cards = catalog.filter { it.level == level }
        val masteredCount = cards.count { (progress[it.id]?.stage ?: 0) == 5 }

        // Le pourcentage compte les kanji arrivés à 100 %, pas la moyenne de leurs paliers.
        val masteredPercent = masteredCount * 100 / cards.size
        val panel = ui.panel()
        panel.addView(ui.text("JLPT N$level", 23f, bold = true))
        panel.addView(ui.text("${cards.size} kanji · $masteredCount maîtrisés", 14f, AppColors.secondaryText))

        // Ligne d'actions : révision saisie à gauche, statistiques au centre, QCM à droite.
        val actions = ui.row()
        actions.addView(levelButton("Normal") { onNormal(level) })
        actions.addView(levelButton("$masteredPercent %", AppColors.secondaryAction) {
            onStatistics(level)
        }.apply {
            contentDescription = "Statistiques JLPT N$level : $masteredPercent pour cent maîtrisés"
        })
        actions.addView(levelButton("Facile") { chooseQuiz(level) })
        panel.addView(actions)

        return panel
    }

    /** Crée un bouton d'accueil avec un poids égal à celui des deux autres boutons du niveau. */
    private fun levelButton(
        label: String,
        color: Int = AppColors.accent,
        action: () -> Unit,
    ) = ui.button(label, color, action).apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(ui.dp(3), ui.dp(8), ui.dp(3), 0)
        }
    }

    /** Ouvre le choix Signification/Rōmaji/Kana/Mélange avant de démarrer le mode facile. */
    private fun chooseQuiz(level: Int) {
        val types = QuestionType.entries.toTypedArray()

        AlertDialog.Builder(ui.activity)
            .setTitle("Quel exercice ?")
            .setSingleChoiceItems(types.map { it.title }.toTypedArray(), types.indexOf(questionType)) { dialog, index ->
                dialog.dismiss()
                onEasy(level, types[index])
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
