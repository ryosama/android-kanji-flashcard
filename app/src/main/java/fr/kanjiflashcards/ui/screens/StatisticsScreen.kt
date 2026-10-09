package fr.kanjiflashcards.ui.screens

import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Spinner
import fr.kanjiflashcards.core.Answers
import fr.kanjiflashcards.core.Kanji
import fr.kanjiflashcards.core.Progress
import fr.kanjiflashcards.ui.AppColors
import fr.kanjiflashcards.ui.UiComponents

/**
 * Écran des statistiques : recherche, filtre de maîtrise, tri et tableau du niveau choisi.
 * Le tableau défile horizontalement, la page verticalement. Les lignes sont chargées par 50.
 */
class StatisticsScreen(
    private val ui: UiComponents,
    private val level: Int,
    private val catalog: List<Kanji>,
    private val progress: Map<String, Progress>,
) {
    /** Nombre de nouvelles lignes ajoutées par appui sur « Afficher la suite ». */
    private val pageSize = 50

    /** Largeurs en dp, dans l'ordre des huit colonnes de showTableHeader. */
    private val columnWidths = listOf(72, 200, 240, 300, 110, 95, 95, 95)

    /** Recherche, filtre et tri qui déterminent les cartes affichées. */
    private lateinit var search: EditText
    private lateinit var filter: Spinner
    private lateinit var order: Spinner

    /** Conteneurs du tableau et bouton de pagination, reconstruits à chaque filtrage. */
    private lateinit var tableHolder: LinearLayout
    private lateinit var table: LinearLayout
    private lateinit var moreButton: Button

    /** Cartes filtrées/triées et nombre de lignes déjà créées pour la sélection courante. */
    private var visibleCards = emptyList<Kanji>()
    private var displayedCount = 0

    /** Installe la recherche et les commandes, puis affiche les premières lignes du niveau. */
    fun show() {
        val content = ui.screen("Statistiques N$level")
        content.addView(ui.text(
            "100 % = cinq réussites espacées. Les compteurs incluent l'entraînement libre.",
            14f,
            AppColors.secondaryText,
        ))

        // Case de recherche : un kanji, une lecture ou une partie de sens français.
        search = EditText(ui.activity).apply {
            hint = "Rechercher un kanji, une lecture, un sens"
            setTextColor(AppColors.text)
            setHintTextColor(AppColors.secondaryText)
            setSingleLine()
        }
        content.addView(search)
        addFilterControls(content)
        content.addView(ui.text(
            "Fais glisser le tableau horizontalement pour voir toutes les colonnes.",
            13f,
            AppColors.secondaryText,
        ))

        tableHolder = ui.column()
        content.addView(tableHolder)
        ui.onSelectionChanged(filter) { renderTable() }
        ui.onSelectionChanged(order) { renderTable() }
        ui.onTextChanged(search) { renderTable() }
        renderTable()
    }

    /** Ajoute côte à côte le filtre Tous/À travailler/Maîtrisés et le choix de tri. */
    private fun addFilterControls(content: LinearLayout) {
        val controls = ui.row()
        filter = spinner(listOf("Tous", "À travailler", "Maîtrisés"))
        order = spinner(listOf("Ordre du fichier", "Maîtrise croissante"))
        controls.addView(filter, LinearLayout.LayoutParams(0, ui.dp(56), 1f))
        controls.addView(order, LinearLayout.LayoutParams(0, ui.dp(56), 1f))
        content.addView(controls)
    }

    /** Crée une liste déroulante Android native pour le filtre ou le tri. */
    private fun spinner(options: List<String>): Spinner {
        return Spinner(ui.activity).apply {
            adapter = ArrayAdapter(ui.activity, android.R.layout.simple_spinner_dropdown_item, options)
        }
    }

    /** Reconstruit le tableau quand la recherche, le filtre ou le tri change. */
    private fun renderTable() {
        tableHolder.removeAllViews()
        visibleCards = filteredCards()
        displayedCount = 0
        tableHolder.addView(ui.text("${visibleCards.size} kanji", 14f, AppColors.secondaryText))

        // Défilement horizontal pour lire les huit colonnes sur un écran de téléphone.
        val horizontalScroll = HorizontalScrollView(ui.activity)
        table = ui.column()
        horizontalScroll.addView(table)
        tableHolder.addView(horizontalScroll)
        showTableHeader()

        // Ce bouton ajoute des lignes sans effacer celles qui sont déjà visibles.
        moreButton = ui.button("Afficher la suite", AppColors.secondaryAction) { appendPage() }
        tableHolder.addView(moreButton)
        appendPage()
    }

    /** Sélectionne les cartes du niveau, applique la recherche, puis le filtre et le tri. */
    private fun filteredCards(): List<Kanji> {
        val query = Answers.meaning(search.text.toString())
        var cards = catalog.filter { it.level == level }.filter { card ->
            val searchableText = "${card.character} ${card.on} ${card.kun} ${card.meanings.joinToString(" ")}"
            query.isBlank() || Answers.meaning(searchableText).contains(query)
        }.filter { card ->
            val mastered = (progress[card.id]?.stage ?: 0) == 5

            when (filter.selectedItemPosition) {
                1 -> !mastered
                2 -> mastered
                else -> true
            }
        }

        if (order.selectedItemPosition == 1) {
            cards = cards.sortedBy { progress[it.id]?.stage ?: 0 }
        }
        return cards
    }

    /** Affiche les titres dans le même ordre que les valeurs construites par appendPage. */
    private fun showTableHeader() {
        addTableRow(
            values = listOf(
                "Kanji", "Lecture on", "Lecture kun", "Significations",
                "Maîtrise", "Proposé", "Correct", "Incorrect",
            ),
            heading = true,
        )
    }

    /** Ajoute jusqu'à 50 cartes et masque le bouton quand toutes les lignes sont affichées. */
    private fun appendPage() {
        for (card in visibleCards.drop(displayedCount).take(pageSize)) {
            val stats = progress[card.id] ?: Progress()
            addTableRow(
                values = listOf(
                    card.character,
                    card.on.ifBlank { "—" },
                    card.kun.ifBlank { "—" },
                    card.meanings.joinToString(", "),
                    "${stats.mastery} %",
                    "${stats.shown}",
                    "${stats.correct}",
                    "${stats.wrong}",
                ),
                colors = listOf(
                    AppColors.text, AppColors.text, AppColors.text, AppColors.text,
                    AppColors.mastery(stats.mastery), AppColors.text, AppColors.success, AppColors.error,
                ),
            )
        }

        displayedCount = (displayedCount + pageSize).coerceAtMost(visibleCards.size)
        moreButton.visibility = if (displayedCount < visibleCards.size) View.VISIBLE else View.GONE
        moreButton.text = "Afficher la suite ($displayedCount / ${visibleCards.size})"
    }

    /** Crée une ligne à huit cellules ; les sens longs peuvent occuper plusieurs lignes. */
    private fun addTableRow(
        values: List<String>,
        colors: List<Int> = List(8) { AppColors.text },
        heading: Boolean = false,
    ) {
        val row = ui.row().apply {
            background = ui.roundedBackground(if (heading) AppColors.tableHeader else AppColors.surface)
        }

        for ((index, value) in values.withIndex()) {
            // Le kanji reçoit une police plus grande que celle des lectures et des compteurs.
            val cell = ui.text(
                value = value,
                size = if (index == 0 && !heading) 26f else 14f,
                color = colors[index],
                bold = heading,
            ).apply {
                setPadding(ui.dp(10), ui.dp(12), ui.dp(10), ui.dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    ui.dp(columnWidths[index]),
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            }
            row.addView(cell)
        }

        table.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = ui.dp(2)
        })
    }
}
