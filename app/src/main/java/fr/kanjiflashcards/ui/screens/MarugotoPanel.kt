package fr.kanjiflashcards.ui.screens

import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import fr.kanjiflashcards.core.Kanji
import fr.kanjiflashcards.core.MarugotoBooks
import fr.kanjiflashcards.core.Progress
import fr.kanjiflashcards.core.StudySelection
import fr.kanjiflashcards.ui.AppColors
import fr.kanjiflashcards.ui.UiComponents

/** Panneau d'accueil Marugoto : choix du livre puis de la dernière leçon/topic incluse. */
class MarugotoPanel(
    private val ui: UiComponents,
    private val catalog: List<Kanji>,
    private val progress: Map<String, Progress>,
    private var lesson: Int,
    private var book: Int,
    private val onSelectionChanged: (Int, Int) -> Unit,
    private val onNormal: () -> Unit,
    private val onEasy: () -> Unit,
    private val onStatistics: () -> Unit,
) {
    /** Installe les deux listes déroulantes et les compteurs du périmètre cumulatif. */
    fun create(): LinearLayout {
        val panel = ui.panel()
        panel.addView(ui.text("Marugoto", 23f, bold = true))

        // Premier choix : les six livres, dans leur ordre d'apprentissage.
        val bookChoice = selector(MarugotoBooks.names, "Livre Marugoto")
        bookChoice.setSelection(book - 1)
        panel.addView(ui.text("Livre", 16f).apply { labelFor = bookChoice.id })
        panel.addView(bookChoice)

        // Second choix : les leçons/topics réels du livre ; l'annexe reste disponible en 18.
        var lessons = MarugotoBooks.lessons(book)
        lesson = lessons.lastOrNull { it <= lesson } ?: lessons.first()
        val lessonChoice = selector(lessons.map { MarugotoBooks.lessonLabel(book, it) }, "Dernière leçon ou topic inclus")
        lessonChoice.setSelection(lessons.indexOf(lesson))
        panel.addView(ui.text("Jusqu'à", 16f).apply { labelFor = lessonChoice.id })
        panel.addView(lessonChoice)
        val scope = ui.text("", 14f, AppColors.secondaryText)
        panel.addView(scope)
        val totals = ui.text("", 14f, AppColors.secondaryText)
        panel.addView(totals)
        val actions = ui.row()
        val statistics = actionButton("", AppColors.secondaryAction, onStatistics)
        actions.addView(actionButton("Normal", AppColors.accent, onNormal))
        actions.addView(statistics)
        actions.addView(actionButton("Facile", AppColors.accent, onEasy))
        panel.addView(actions)

        /** Actualise les compteurs et mémorise les deux choix sans modifier les progrès. */
        fun updateLabels() {
            val cards = StudySelection(StudySelection.MARUGOTO, lesson, book).cards(catalog)
            val mastered = cards.count { (progress[it.id]?.stage ?: 0) == 5 }
            val percent = if (cards.isEmpty()) 0 else mastered * 100 / cards.size
            scope.text = if (book == 1) "Le livre choisi jusqu'à la limite incluse." else "Les ${book - 1} livres précédents sont inclus entièrement."
            totals.text = "${cards.size} cartes · $mastered maîtrisées"
            statistics.text = "$percent %"
            statistics.contentDescription = "Statistiques Marugoto : ${MarugotoBooks.name(book)}, ${MarugotoBooks.lessonLabel(book, lesson)} : $percent pour cent maîtrisés"
            onSelectionChanged(book, lesson)
        }

        ui.onSelectionChanged(bookChoice) {
            val selectedBook = bookChoice.selectedItemPosition + 1
            if (selectedBook != book) {
                book = selectedBook
                lessons = MarugotoBooks.lessons(book)
                lesson = lessons.lastOrNull { it <= lesson } ?: lessons.first()
                lessonChoice.adapter = ArrayAdapter(ui.activity, android.R.layout.simple_spinner_dropdown_item,
                    lessons.map { MarugotoBooks.lessonLabel(book, it) })
                lessonChoice.setSelection(lessons.indexOf(lesson))
                updateLabels()
            }
        }
        ui.onSelectionChanged(lessonChoice) {
            lessons.getOrNull(lessonChoice.selectedItemPosition)?.let { selectedLesson ->
                lesson = selectedLesson
                updateLabels()
            }
        }
        updateLabels()
        return panel
    }

    /** Crée une liste Android native dont le libellé est accessible au lecteur d'écran. */
    private fun selector(labels: List<String>, description: String): Spinner {
        return Spinner(ui.activity).apply {
            id = View.generateViewId()
            contentDescription = description
            adapter = ArrayAdapter(ui.activity, android.R.layout.simple_spinner_dropdown_item, labels)
        }
    }

    /** Crée un bouton occupant un tiers de la ligne d'actions du panneau. */
    private fun actionButton(label: String, color: Int, action: () -> Unit) = ui.button(label, color, action).apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(ui.dp(3), ui.dp(8), ui.dp(3), 0)
        }
    }
}
