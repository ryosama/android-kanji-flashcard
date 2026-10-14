package fr.kanjiflashcards.ui.screens

import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import fr.kanjiflashcards.core.Kanji
import fr.kanjiflashcards.core.Progress
import fr.kanjiflashcards.core.StudySelection
import fr.kanjiflashcards.ui.AppColors
import fr.kanjiflashcards.ui.UiComponents

/** Panneau Marugoto de l'accueil : curseur de leçon, progression et trois actions de révision. */
class MarugotoPanel(
    private val ui: UiComponents,
    private val catalog: List<Kanji>,
    private val progress: Map<String, Progress>,
    private var lesson: Int,
    private val onLessonChanged: (Int) -> Unit,
    private val onNormal: () -> Unit,
    private val onEasy: () -> Unit,
    private val onStatistics: () -> Unit,
) {
    /** Construit le curseur 1..18 et actualise le périmètre sans effacer les statistiques masquées. */
    fun create(): LinearLayout {
        val panel = ui.panel()
        panel.addView(ui.text("Marugoto", 23f, bold = true))
        val lessonLabel = ui.text("", 16f, AppColors.accent)
        val totals = ui.text("", 14f, AppColors.secondaryText)
        panel.addView(lessonLabel)

        // Le SeekBar commence à zéro : la valeur visible correspond à progress + 1.
        val slider = SeekBar(ui.activity).apply {
            id = View.generateViewId()
            max = 17
            progress = lesson - 1
            contentDescription = "Dernière leçon Marugoto incluse"
        }
        lessonLabel.labelFor = slider.id
        panel.addView(slider)
        panel.addView(totals)
        val actions = ui.row()
        val normal = actionButton("Normal", AppColors.accent, onNormal)
        val statistics = actionButton("", AppColors.secondaryAction, onStatistics)
        val easy = actionButton("Facile", AppColors.accent, onEasy)
        actions.addView(normal)
        actions.addView(statistics)
        actions.addView(easy)
        panel.addView(actions)

        /** Recalcule les libellés pour les seules leçons incluses, en conservant les autres progrès. */
        fun updateLabels() {
            val cards = StudySelection(StudySelection.MARUGOTO, lesson).cards(catalog)
            val mastered = cards.count { (progress[it.id]?.stage ?: 0) == 5 }
            val percent = if (cards.isEmpty()) 0 else mastered * 100 / cards.size
            lessonLabel.text = "Leçons 1 à $lesson"
            slider.contentDescription = "Marugoto : leçons 1 à $lesson"
            totals.text = "${cards.size} cartes · $mastered maîtrisées"
            statistics.text = "$percent %"
            statistics.contentDescription = "Statistiques Marugoto, leçons 1 à $lesson : $percent pour cent maîtrisés"
        }

        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            /** Enregistre immédiatement la leçon choisie et adapte les compteurs visibles. */
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                lesson = value + 1
                onLessonChanged(lesson)
                updateLabels()
            }

            /** Le début du geste ne change pas les données de progression. */
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            /** La valeur finale a déjà été enregistrée par onProgressChanged. */
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        updateLabels()
        return panel
    }

    /** Crée un bouton occupant un tiers de la ligne d'actions du panneau. */
    private fun actionButton(label: String, color: Int, action: () -> Unit) = ui.button(label, color, action).apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(ui.dp(3), ui.dp(8), ui.dp(3), 0)
        }
    }
}
