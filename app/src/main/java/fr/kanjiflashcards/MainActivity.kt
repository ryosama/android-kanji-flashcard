package fr.kanjiflashcards

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import fr.kanjiflashcards.core.Answers
import fr.kanjiflashcards.core.Catalog
import fr.kanjiflashcards.core.Kanji
import fr.kanjiflashcards.core.Leitner
import fr.kanjiflashcards.core.Progress
import fr.kanjiflashcards.core.QuestionType
import fr.kanjiflashcards.core.Quizzes
import fr.kanjiflashcards.ui.ReviewLayout
import fr.kanjiflashcards.ui.UiComponents
import fr.kanjiflashcards.ui.screens.EasyReviewScreen
import fr.kanjiflashcards.ui.screens.HomeScreen
import fr.kanjiflashcards.ui.screens.NormalReviewScreen
import fr.kanjiflashcards.ui.screens.SettingsScreen
import fr.kanjiflashcards.ui.screens.StatisticsScreen

/**
 * Point d'entrée Android et coordinateur : chargement des données, navigation et validation.
 * Les vues sont dans ui/screens, les règles pédagogiques dans core et la séance dans ReviewSession.
 */
class MainActivity : Activity() {
    /** Catalogue des cinq niveaux, chargé depuis les CSV embarqués. */
    private lateinit var catalog: List<Kanji>

    /** Stockage privé et progression durable, indexée par « niveau:kanji ». */
    private lateinit var store: ProgressStore
    private var progress = mutableMapOf<String, Progress>()

    /** Composants graphiques partagés et gestionnaire des documents de sauvegarde. */
    private lateinit var ui: UiComponents
    private lateinit var documents: BackupDocuments

    /** Séance consultée par les écrans ; l'activité reste responsable de sa validation. */
    private val session = ReviewSession()

    /** Page active, également conservée dans le Bundle Android lors d'une rotation. */
    private var page = "home"

    /** Charge les données, restaure l'état Android et affiche la dernière page ouverte. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UiComponents(
            activity = this,
            actionsAllowed = { !::documents.isInitialized || !documents.busy },
            onHome = { showHome() },
        )

        try {
            catalog = loadCatalog()
            store = ProgressStore(this, catalog.map { it.id }.toSet())
            progress = store.load().toMutableMap()
        } catch (exception: Exception) {
            showLoadingError(exception)
            return
        }

        documents = BackupDocuments(
            activity = this,
            ui = ui,
            knownIds = catalog.map { it.id }.toSet(),
            currentProgress = { progress },
            onImportConfirmed = { restored -> importProgress(restored) },
        )

        val preferredType = getPreferences(MODE_PRIVATE).getString("questionType", "MIXED") ?: "MIXED"
        session.questionType = QuestionType.valueOf(preferredType)

        if (savedInstanceState != null) {
            page = savedInstanceState.getString("page") ?: "home"
            session.restoreFrom(savedInstanceState, catalog)
            documents.pendingExport = savedInstanceState.getString("pendingExport")
        }

        when (page) {
            "review" -> showReview()
            "stats" -> showStatistics(session.level)
            "settings" -> showSettings()
            else -> showHome()
        }
    }

    /** Lit les cinq CSV français sans modifier les fichiers sources ni leur ordre. */
    private fun loadCatalog(): List<Kanji> {
        return (1..5).flatMap { level ->
            assets.open("JLPT $level francais.csv").bufferedReader().use { reader ->
                Catalog.parse(level, reader.readText())
            }
        }
    }

    /** Affiche l'échec de chargement en conservant les fichiers existants pour éviter une perte. */
    private fun showLoadingError(exception: Exception) {
        val errorView = ui.text(
            "Impossible de charger les données. La progression a été conservée.\n${exception.message}",
        ).apply {
            setPadding(24, 48, 24, 24)
        }
        setContentView(errorView)
    }

    /** Ferme la carte courante et affiche les panneaux de niveaux sur l'accueil. */
    private fun showHome() {
        ui.hideKeyboard()
        page = "home"
        session.card = null

        HomeScreen(
            ui = ui,
            catalog = catalog,
            progress = progress,
            questionType = session.questionType,
            onNormal = { level -> beginReview(level, easy = false) },
            onEasy = { level, type -> beginEasyReview(level, type) },
            onStatistics = { level -> showStatistics(level) },
            onSettings = { showSettings() },
        ).show()
    }

    /** Mémorise le type de QCM sélectionné avant de démarrer la séance facile. */
    private fun beginEasyReview(level: Int, type: QuestionType) {
        session.questionType = type
        getPreferences(MODE_PRIVATE).edit().putString("questionType", type.name).apply()
        beginReview(level, easy = true)
    }

    /** Initialise le niveau, le mode et les compteurs avant de tirer la première carte. */
    private fun beginReview(level: Int, easy: Boolean) {
        session.begin(level, easy)
        nextCard()
    }

    /** Écrit la progression avant de remplacer sa copie en mémoire, et signale un éventuel échec. */
    private fun persist(next: Map<String, Progress>): Boolean {
        return try {
            store.save(next)
            progress = next.toMutableMap()
            true
        } catch (_: Exception) {
            ui.message("Enregistrement impossible. Réessaie avant de continuer.")
            false
        }
    }

    /** Tire une carte admissible, compte sa présentation et prépare le QCM si nécessaire. */
    private fun nextCard() {
        ui.hideKeyboard()
        val levelCards = catalog.filter { it.level == session.level }
        val selected = Leitner.pick(
            cards = levelCards,
            progress = progress,
            now = System.currentTimeMillis(),
            practice = session.practice,
            previous = session.card?.id,
        )
        session.select(selected)
        page = "review"

        if (selected == null) {
            showReview()
            return
        }

        val previousProgress = progress[selected.id] ?: Progress()
        val updated = previousProgress.copy(shown = previousProgress.shown + 1)
        if (!persist(progress + (selected.id to updated))) {
            showHome()
            return
        }

        session.quiz = if (session.easy) {
            Quizzes.make(selected, levelCards, session.questionType)
        } else {
            null
        }
        showReview()
    }

    /** Choisit l'écran de révision et lui fournit la présentation commune et ses actions. */
    private fun showReview() {
        val nextDue = catalog.filter { it.level == session.level }
            .mapNotNull { progress[it.id]?.due }
            .minOrNull()
        val mastery = session.card?.let { progress[it.id]?.mastery } ?: 0
        val layout = ReviewLayout(
            ui = ui,
            session = session,
            mastery = mastery,
            nextDue = nextDue,
            onNext = { nextCard() },
            onPractice = {
                session.practice = true
                nextCard()
            },
        )

        if (session.easy) {
            EasyReviewScreen(ui, session, layout) { correct -> completeAnswer(correct) }.show()
        } else {
            NormalReviewScreen(
                ui = ui,
                session = session,
                layout = layout,
                onTextChanged = { field, value -> updateAnswerText(field, value) },
                onSubmit = { reading, meaning -> validateAnswers(reading, meaning) },
                onUnknown = { completeAnswer(false) },
            ).show()
        }
    }

    /** Conserve la saisie pour qu'une validation ou une rotation ne l'efface pas. */
    private fun updateAnswerText(field: AnswerField, value: String) {
        when (field) {
            AnswerField.PRONUNCIATION -> session.readingText = value
            AnswerField.MEANING -> session.meaningText = value
        }
    }

    /** Corrige les deux saisies ensemble et enregistre une seule réponse pour la carte. */
    private fun validateAnswers(reading: String, meaning: String) {
        if (session.finished || reading.isBlank() || meaning.isBlank()) {
            return
        }

        val card = requireNotNull(session.card)
        session.readingText = reading
        session.meaningText = meaning
        session.readingResult = Answers.acceptsReading(reading, card)
        session.meaningResult = Answers.acceptsMeaning(meaning, card)
        completeAnswer(session.readingResult == true && session.meaningResult == true)
    }

    /** Enregistre une réponse une seule fois, applique Leitner et affiche la correction globale. */
    private fun completeAnswer(correct: Boolean) {
        if (session.finished) {
            return
        }

        val card = requireNotNull(session.card)
        val previousProgress = progress[card.id] ?: Progress(shown = 1)
        val updated = Leitner.answer(previousProgress, correct, System.currentTimeMillis(), session.practice)

        if (!persist(progress + (card.id to updated))) {
            // Conserver les textes mais réautoriser la validation pour retenter l'enregistrement.
            session.readingResult = null
            session.meaningResult = null
            showReview()
            return
        }

        session.complete(correct)
        ui.hideKeyboard()
        showReview()
    }

    /** Affiche la recherche et le tableau de progression du niveau demandé. */
    private fun showStatistics(level: Int) {
        page = "stats"
        session.level = level
        StatisticsScreen(ui, level, catalog, progress).show()
    }

    /** Affiche les commandes de sauvegarde et de réinitialisation par niveau. */
    private fun showSettings() {
        page = "settings"
        SettingsScreen(
            ui = ui,
            onExport = { documents.startExport() },
            onImport = { documents.startImport() },
            onReset = { level -> resetLevel(level) },
        ).show()
    }

    /** Efface uniquement le niveau confirmé, puis rafraîchit les réglages. */
    private fun resetLevel(level: Int) {
        if (persist(progress.filterKeys { !it.startsWith("$level:") })) {
            ui.message("Niveau réinitialisé")
            showSettings()
        }
    }

    /** Remplace la progression après confirmation d'import et abandonne la carte précédente. */
    private fun importProgress(restored: Map<String, Progress>) {
        if (persist(restored)) {
            session.card = null
            ui.message("Progression importée")
            showSettings()
        }
    }

    /** Transmet le document choisi par Android au gestionnaire d'export/import. */
    @Deprecated("API native pour compatibilité sans dépendance supplémentaire")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        documents.handleResult(requestCode, resultCode, data)
    }

    /** Conserve la page, la séance et l'export en attente avant une recréation Android. */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("page", page)
        session.saveInto(outState)

        if (::documents.isInitialized) {
            outState.putString("pendingExport", documents.pendingExport)
        }
    }

    /** Revient à l'accueil depuis une page secondaire ; Android quitte depuis l'accueil. */
    @Suppress("DEPRECATION")
    @Deprecated("Navigation native")
    override fun onBackPressed() {
        if (page == "home") {
            super.onBackPressed()
        } else {
            showHome()
        }
    }
}
