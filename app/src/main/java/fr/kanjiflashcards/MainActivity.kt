package fr.kanjiflashcards

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import fr.kanjiflashcards.core.*
import java.text.DateFormat
import java.util.Date

/** Interface native et hors ligne. Les décisions pédagogiques restent dans le module core. */
class MainActivity : Activity() {
    private val background = Color.rgb(16, 20, 30)
    private val surface = Color.rgb(28, 35, 49)
    private val ink = Color.rgb(238, 242, 250)
    private val muted = Color.rgb(169, 182, 202)
    private val accent = Color.rgb(116, 220, 197)
    private val green = Color.rgb(125, 225, 155)
    private val red = Color.rgb(255, 139, 151)
    private lateinit var catalog: List<Kanji>
    private lateinit var store: ProgressStore
    private var progress = mutableMapOf<String, Progress>()
    private lateinit var root: LinearLayout
    private var page = "home"
    private var level = 5
    private var easy = false
    private var questionType = QuestionType.MIXED
    private var practice = false
    private var card: Kanji? = null
    private var quiz: Quiz? = null
    private var readingResult: Boolean? = null
    private var meaningResult: Boolean? = null
    private var readingText = ""
    private var meaningText = ""
    private var finished = false
    private var resultCorrect = false
    private var sessionCorrect = 0
    private var sessionTotal = 0
    private var busy = false
    private var pendingExport: String? = null
    private val exportRequest = 41
    private val importRequest = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            catalog = (1..5).flatMap { n -> assets.open("JLPT $n francais.csv").bufferedReader().use { Catalog.parse(n, it.readText()) } }
            store = ProgressStore(this, catalog.map { it.id }.toSet())
            progress = store.load().toMutableMap()
        } catch (e: Exception) {
            val error = TextView(this).apply { text = "Impossible de charger les données. La progression a été conservée.\n${e.message}"; setPadding(24, 48, 24, 24) }
            setContentView(error)
            return
        }
        val preferences = getPreferences(MODE_PRIVATE)
        questionType = QuestionType.valueOf(preferences.getString("questionType", "MIXED") ?: "MIXED")
        savedInstanceState?.let { restore(it) }
        when(page) { "review" -> renderReview(); "stats" -> statistics(level); "settings" -> settings(); else -> home() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(16).toFloat()
        border?.let { setStroke(dp(1), it) }
    }
    private fun text(value: String, size: Float = 16f, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(8), 0, dp(8))
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun button(label: String, color: Int = accent, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 16f; setTextColor(this@MainActivity.background)
        background = shape(color); minHeight = dp(52)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setOnClickListener { if (!busy) action() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(6), 0, dp(6)) }
    }
    private fun panel() = column().apply {
        background = shape(surface); setPadding(dp(16), dp(12), dp(16), dp(12))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(8), 0, dp(8)) }
    }
    private fun screen(title: String, back: Boolean = true) {
        root = column().apply { setPadding(dp(20), dp(12), dp(20), dp(24)); setBackgroundColor(this@MainActivity.background) }
        val scroll = ScrollView(this).apply { isFillViewport = true; fitsSystemWindows = true; addView(root) }
        setContentView(scroll)
        val header = row()
        if (back) header.addView(button("‹ Accueil") { home() }.apply {
            setTextColor(accent); background = shape(surface)
            layoutParams = LinearLayout.LayoutParams(dp(115), dp(52))
            contentDescription = "Revenir à l'accueil"
        })
        header.addView(text(title, if(back) 21f else 30f, bold = true).apply {
            setPadding(if(back) dp(16) else 0, dp(12), 0, dp(12))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        root.addView(header)
    }
    private fun home() {
        hideKeyboard(); page = "home"; card = null
        screen("Kanji\nFlashcards", false)
        root.addView(text("Un caractère à la fois.", 17f, muted))
        val due = catalog.count { (progress[it.id]?.due ?: 0) <= System.currentTimeMillis() }
        root.addView(text("$due cartes disponibles · 5 niveaux JLPT", 14f, accent))
        for (n in 5 downTo 1) {
            val cards = catalog.filter { it.level == n }
            val mastered = cards.count { (progress[it.id]?.stage ?: 0) == 5 }
            val percent = mastered * 100 / cards.size
            val p = panel()
            p.addView(text("JLPT N$n", 23f, bold = true))
            p.addView(text("${cards.size} kanji · $mastered maîtrisés", 14f, muted))
            val actions = row()
            fun add(label: String, action: () -> Unit) {
                actions.addView(button(label, if(label.endsWith("%")) Color.rgb(188, 196, 245) else accent, action).apply {
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), dp(8), dp(3), 0) }
                    if(label.endsWith("%")) contentDescription = "Statistiques JLPT N$n : $percent pour cent maîtrisés"
                })
            }
            add("Normal") { begin(n, false) }
            add("$percent %") { statistics(n) }
            add("Facile") { chooseQuiz(n) }
            p.addView(actions); root.addView(p)
        }
        root.addView(button("Sauvegarde et réglages", Color.rgb(188, 196, 245)) { settings() })
    }
    private fun chooseQuiz(n: Int) {
        val types = QuestionType.entries.toTypedArray()
        AlertDialog.Builder(this).setTitle("Quel exercice ?")
            .setSingleChoiceItems(types.map { it.title }.toTypedArray(), types.indexOf(questionType)) { dialog, which ->
                questionType = types[which]
                getPreferences(MODE_PRIVATE).edit().putString("questionType", questionType.name).apply()
                dialog.dismiss(); begin(n, true)
            }.setNegativeButton("Annuler", null).show()
    }
    private fun begin(n: Int, isEasy: Boolean) {
        level = n; easy = isEasy; practice = false; sessionCorrect = 0; sessionTotal = 0
        nextCard()
    }
    private fun persist(next: Map<String, Progress>): Boolean = try {
        store.save(next); progress = next.toMutableMap(); true
    } catch (_: Exception) { message("Enregistrement impossible. Réessaie avant de continuer."); false }

    private fun nextCard() {
        hideKeyboard()
        val selected = Leitner.pick(catalog.filter { it.level == level }, progress, System.currentTimeMillis(), practice, card?.id)
        card = selected; page = "review"
        readingResult = null; meaningResult = null; readingText = ""; meaningText = ""; finished = false
        if (selected == null) { renderReview(); return }
        val previous = progress[selected.id] ?: Progress()
        if (!persist(progress + (selected.id to previous.copy(shown = previous.shown + 1)))) { home(); return }
        quiz = if(easy) Quizzes.make(selected, catalog.filter { it.level == level }, questionType) else null
        renderReview()
    }

    private fun renderReview() {
        screen("JLPT N$level")
        root.addView(text((if(easy) "Mode facile" else "Mode normal") + if(practice) " · Entraînement libre" else " · Révision espacée", 14f, accent))
        root.addView(text("Séance : $sessionCorrect / $sessionTotal réponses correctes", 14f, muted))
        val current = card
        if(current == null) {
            root.addView(text("À jour pour ce niveau", 26f, bold = true))
            val due = catalog.filter { it.level == level }.mapNotNull { progress[it.id]?.due }.minOrNull()
            root.addView(text("Prochaine révision : ${due?.let { date(it) } ?: "maintenant"}", color = muted))
            root.addView(text("Tu peux continuer à t'entraîner. Le palier de maîtrise et l'échéance resteront identiques."))
            root.addView(button("Entraînement libre") { practice = true; nextCard() })
            return
        }
        root.addView(text(current.character, 108f, bold = true).apply {
            gravity = Gravity.CENTER; background = shape(surface)
            setPadding(0, dp(12), 0, dp(12)); contentDescription = "Kanji ${current.character}"
        })
        if(easy) {
            val q = requireNotNull(quiz)
            root.addView(text(q.type.title, 20f, bold = true))
            q.options.forEachIndexed { i, option ->
                root.addView(button(option, if(finished && i == q.correctIndex) green else Color.rgb(188, 196, 245)) {
                    complete(i == q.correctIndex)
                }.apply { isEnabled = !finished })
            }
        } else {
            inputField("Prononciation", readingText, readingResult, true)
            inputField("Signification", meaningText, meaningResult, false)
        }
        if(finished) {
            root.addView(text("Lectures : ${current.readings.joinToString(" · ")}", color = green))
            root.addView(text("Sens : ${current.meanings.joinToString(", ")}", color = green))
            root.addView(button(if(resultCorrect) "Bonne réponse →" else "Mauvaise réponse →", if(resultCorrect) green else red) { nextCard() })
            root.addView(text("Maîtrise : ${(progress[current.id] ?: Progress()).mastery} %", 14f, muted))
        }
    }
    private fun inputField(label: String, initial: String, result: Boolean?, reading: Boolean) {
        val fieldLabel = text(label, 18f, bold = true)
        root.addView(fieldLabel)
        val input = EditText(this).apply {
            id = View.generateViewId()
            setText(initial); setTextColor(ink); setHintTextColor(muted)
            hint = if(reading) "Rōmaji, hiragana ou katakana" else "Un des sens en français"
            textSize = 16f; setSingleLine(true); background = shape(surface)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isEnabled = result == null && !finished
            addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if(reading) readingText = s.toString() else meaningText = s.toString()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        fieldLabel.labelFor = input.id
        val r = row()
        input.minHeight = dp(58)
        r.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(button("OK") {
            if(input.text.isNullOrBlank()) { input.error = "Écris une réponse"; return@button }
            val k = requireNotNull(card)
            val good = if(reading) Answers.acceptsReading(input.text.toString(), k) else Answers.acceptsMeaning(input.text.toString(), k)
            if(reading) readingResult = good else meaningResult = good
            hideKeyboard()
            if(readingResult != null && meaningResult != null) complete(readingResult == true && meaningResult == true)
            else renderReview()
        }.apply {
            isEnabled = result == null && !finished
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(58)).apply { setMargins(dp(8), 0, 0, 0) }
        })
        root.addView(r)
        result?.let { root.addView(text(if(it) "✓ Réponse correcte" else "✕ Réponse incorrecte", 14f, if(it) green else red)) }
    }
    private fun complete(correct: Boolean) {
        if(finished) return
        val k = requireNotNull(card)
        val p = progress[k.id] ?: Progress(shown = 1)
        if(!persist(progress + (k.id to Leitner.answer(p, correct, System.currentTimeMillis(), practice)))) {
            // Les validations restent visibles et un nouvel appui permet de retenter l'écriture.
            readingResult = null; meaningResult = null; renderReview(); return
        }
        finished = true; resultCorrect = correct; sessionTotal++; if(correct) sessionCorrect++
        hideKeyboard(); renderReview()
    }

    private fun masteryColor(value: Int): Int {
        val stops = intArrayOf(red, Color.rgb(255, 183, 110), Color.rgb(244, 223, 113), green)
        val pos = value.coerceIn(0, 100) / 100f * 3
        val index = pos.toInt().coerceAtMost(2); val fraction = pos - index
        val a = stops[index]; val b = stops[index + 1]
        fun mix(x: Int, y: Int) = (x + (y-x)*fraction).toInt()
        return Color.rgb(mix(Color.red(a), Color.red(b)), mix(Color.green(a), Color.green(b)), mix(Color.blue(a), Color.blue(b)))
    }
    private fun statistics(n: Int) {
        page = "stats"; level = n; screen("Statistiques N$n")
        root.addView(text("100 % = cinq réussites espacées. Les compteurs incluent l'entraînement libre.", 14f, muted))
        val search = EditText(this).apply {
            hint = "Rechercher un kanji, une lecture, un sens"; setTextColor(ink); setHintTextColor(muted); setSingleLine()
        }
        root.addView(search)
        val controls = row()
        val filter = Spinner(this)
        filter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Tous", "À travailler", "Maîtrisés"))
        val order = Spinner(this)
        order.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Ordre du fichier", "Maîtrise croissante"))
        controls.addView(filter, LinearLayout.LayoutParams(0, dp(56), 1f))
        controls.addView(order, LinearLayout.LayoutParams(0, dp(56), 1f))
        root.addView(controls)
        root.addView(text("Fais glisser le tableau horizontalement pour voir toutes les colonnes.", 13f, muted))
        val holder = column(); root.addView(holder)
        fun renderTable() {
            holder.removeAllViews()
            val query = Answers.meaning(search.text.toString())
            var cards = catalog.filter { it.level == n }.filter {
                query.isBlank() || Answers.meaning("${it.character} ${it.on} ${it.kun} ${it.meanings.joinToString(" ")}").contains(query)
            }.filter {
                val mastered = (progress[it.id]?.stage ?: 0) == 5
                when(filter.selectedItemPosition) { 1 -> !mastered; 2 -> mastered; else -> true }
            }
            if(order.selectedItemPosition == 1) cards = cards.sortedBy { progress[it.id]?.stage ?: 0 }
            holder.addView(text("${cards.size} kanji", 14f, muted))
            // Pagination : éviter de créer 1 504 lignes natives en même temps sur le téléphone.
            val horizontal = HorizontalScrollView(this)
            val table = column(); horizontal.addView(table); holder.addView(horizontal)
            val widths = listOf(72, 200, 240, 300, 110, 95, 95, 95)
            fun addRow(values: List<String>, colors: List<Int> = List(8) { ink }, heading: Boolean = false) {
                val r = row().apply { background = shape(if(heading) Color.rgb(44, 55, 74) else surface) }
                values.forEachIndexed { i, v -> r.addView(text(v, if(i == 0 && !heading) 26f else 14f, colors[i], heading).apply {
                    setPadding(dp(10), dp(12), dp(10), dp(12))
                    layoutParams = LinearLayout.LayoutParams(dp(widths[i]), -2)
                }) }
                table.addView(r, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(2) })
            }
            addRow(listOf("Kanji", "Lecture on", "Lecture kun", "Significations", "Maîtrise", "Proposé", "Correct", "Incorrect"), heading = true)
            var offset = 0
            val more = button("Afficher la suite", Color.rgb(188, 196, 245)) {}
            fun appendPage() {
                for(k in cards.drop(offset).take(50)) {
                    val p = progress[k.id] ?: Progress()
                    addRow(listOf(k.character, k.on.ifBlank { "—" }, k.kun.ifBlank { "—" }, k.meanings.joinToString(", "),
                        "${p.mastery} %", "${p.shown}", "${p.correct}", "${p.wrong}"),
                        listOf(ink, ink, ink, ink, masteryColor(p.mastery), ink, green, red))
                }
                offset = (offset + 50).coerceAtMost(cards.size)
                more.visibility = if(offset < cards.size) View.VISIBLE else View.GONE
                more.text = "Afficher la suite ($offset / ${cards.size})"
            }
            more.setOnClickListener { appendPage() }; holder.addView(more); appendPage()
        }
        val listener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { renderTable() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        filter.onItemSelectedListener = listener; order.onItemSelectedListener = listener
        search.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderTable() }
            override fun afterTextChanged(s: Editable?) {}
        })
        renderTable()
    }
    private fun settings() {
        page = "settings"; screen("Sauvegarde")
        root.addView(text("Tes progrès restent sur ce téléphone. Exporte une sauvegarde avant de désinstaller l'application ou de changer de téléphone.", color = muted))
        root.addView(button("Exporter la progression") {
            pendingExport = Backup.encode(progress)
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "text/plain"; putExtra(Intent.EXTRA_TITLE, "kanji-progression.tsv")
            }, exportRequest)
        })
        root.addView(button("Importer une sauvegarde", Color.rgb(188, 196, 245)) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }, importRequest)
        })
        root.addView(text("L'import remplace la progression actuelle après confirmation. Seuls les fichiers de sauvegarde de cette application sont acceptés.", 14f, muted))
        root.addView(text("Réinitialiser un niveau", 21f, bold = true))
        for(n in 5 downTo 1) root.addView(button("Réinitialiser JLPT N$n", red) {
            AlertDialog.Builder(this).setTitle("Effacer la progression N$n ?")
                .setMessage("Les paliers, les échéances et les compteurs de ce niveau seront supprimés. Pense à exporter une sauvegarde.")
                .setNegativeButton("Annuler", null).setPositiveButton("Réinitialiser") { _, _ ->
                    if(persist(progress.filterKeys { !it.startsWith("$n:") })) { message("Niveau réinitialisé"); settings() }
                }.show()
        })
        root.addView(text("Kanji Flashcards · 0.1.0\nSans compte · Sans connexion · Sans publicité", 14f, muted))
    }

    @Deprecated("API native pour compatibilité sans dépendance supplémentaire")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        busy = true
        message(if(requestCode == exportRequest) "Export en cours…" else "Lecture de la sauvegarde…")
        // Le fournisseur de documents peut être distant ; ne pas bloquer l'interface.
        Thread {
            try {
                if(requestCode == exportRequest) {
                    val contents = pendingExport ?: Backup.encode(progress)
                    val output = contentResolver.openOutputStream(uri, "wt") ?: error("Fichier inaccessible")
                    output.bufferedWriter().use { it.write(contents) }
                    runOnUiThread { busy = false; pendingExport = null; message("Sauvegarde exportée") }
                } else if(requestCode == importRequest) {
                    val input = contentResolver.openInputStream(uri) ?: error("Fichier inaccessible")
                    val contents = input.use { stream ->
                        val buffer = java.io.ByteArrayOutputStream()
                        val chunk = ByteArray(8192)
                        while (buffer.size() <= 2_000_000) {
                            val count = stream.read(chunk)
                            if (count < 0) break
                            buffer.write(chunk, 0, count)
                        }
                        val bytes = buffer.toByteArray()
                        require(bytes.size <= 2_000_000) { "Sauvegarde trop volumineuse" }
                        bytes.toString(Charsets.UTF_8)
                    }
                    val restored = Backup.decode(contents, catalog.map { it.id }.toSet())
                    runOnUiThread {
                        busy = false
                        AlertDialog.Builder(this).setTitle("Importer ${restored.size} fiches ?")
                            .setMessage("Cette sauvegarde contient ${restored.values.sumOf { it.correct.toLong() + it.wrong }} réponses. Elle remplacera la progression actuelle de tous les niveaux.")
                            .setNegativeButton("Annuler", null).setPositiveButton("Importer") { _, _ ->
                                if(persist(restored)) { card = null; message("Progression importée"); settings() }
                            }.show()
                    }
                } else runOnUiThread { busy = false }
            } catch(e: Exception) { runOnUiThread { busy = false; message("Opération impossible : ${e.message}") } }
        }.start()
    }
    private fun message(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    private fun date(time: Long) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(time))
    private fun hideKeyboard() { currentFocus?.let { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken, 0) } }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("page", page); outState.putInt("level", level); outState.putBoolean("easy", easy)
        outState.putBoolean("practice", practice); outState.putString("card", card?.id)
        outState.putString("type", questionType.name); outState.putBoolean("finished", finished)
        outState.putBoolean("result", resultCorrect); outState.putInt("total", sessionTotal); outState.putInt("correct", sessionCorrect)
        outState.putString("readingText", readingText); outState.putString("meaningText", meaningText)
        outState.putInt("readingResult", readingResult?.let { if(it) 1 else 0 } ?: -1)
        outState.putInt("meaningResult", meaningResult?.let { if(it) 1 else 0 } ?: -1)
        quiz?.let { outState.putStringArrayList("options", ArrayList(it.options)); outState.putString("quizType", it.type.name); outState.putInt("quizCorrect", it.correctIndex) }
        outState.putString("pendingExport", pendingExport)
    }
    private fun restore(state: Bundle) {
        page = state.getString("page") ?: "home"; level = state.getInt("level", 5); easy = state.getBoolean("easy")
        practice = state.getBoolean("practice"); card = catalog.find { it.id == state.getString("card") }
        questionType = QuestionType.valueOf(state.getString("type") ?: "MIXED")
        finished = state.getBoolean("finished"); resultCorrect = state.getBoolean("result")
        sessionTotal = state.getInt("total"); sessionCorrect = state.getInt("correct")
        readingText = state.getString("readingText") ?: ""; meaningText = state.getString("meaningText") ?: ""
        readingResult = state.getInt("readingResult", -1).let { if(it < 0) null else it == 1 }
        meaningResult = state.getInt("meaningResult", -1).let { if(it < 0) null else it == 1 }
        state.getStringArrayList("options")?.let { quiz = Quiz(QuestionType.valueOf(state.getString("quizType")!!), it, state.getInt("quizCorrect")) }
        pendingExport = state.getString("pendingExport")
    }
    @Deprecated("Navigation native")
    override fun onBackPressed() { if(page == "home") super.onBackPressed() else home() }
}
