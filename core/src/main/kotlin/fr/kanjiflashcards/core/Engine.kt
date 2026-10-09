package fr.kanjiflashcards.core

import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

/** Une entrée du catalogue. L'identifiant dépend du niveau et du caractère, pas du numéro CSV. */
data class Kanji(val level: Int, val character: String, val on: String, val kun: String, val meanings: List<String>) {
    val id: String get() = "$level:$character"
    val readings: Set<String> get() = (Answers.readings(on) + Answers.readings(kun)).toSet()
    val romaji: List<String> get() = readings.filter { it.any { c -> c in 'a'..'z' } }.sorted()
    val kana: List<String> get() = readings.filter { it.any { c -> c in '\u3040'..'\u30ff' } }.sorted()
}

/** Import CSV avec guillemets et points-virgules ; aucune modification des fichiers d'origine. */
object Catalog {
    fun parse(level: Int, text: String): List<Kanji> {
        val records = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        val input = text.removePrefix("\uFEFF")
        while (index < input.length) {
            val c = input[index]
            when {
                c == '"' && quoted && input.getOrNull(index + 1) == '"' -> { field.append('"'); index++ }
                c == '"' -> quoted = !quoted
                c == ';' && !quoted -> { row.add(field.toString()); field.clear() }
                c == '\n' && !quoted -> {
                    row.add(field.toString().trimEnd('\r')); field.clear()
                    if (row.any { it.isNotBlank() }) records.add(row)
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
            index++
        }
        require(!quoted) { "Guillemets CSV non fermés" }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); records.add(row) }
        return records.map { columns ->
            require(columns.size == 5 && columns[1].isNotBlank()) { "Entrée CSV invalide" }
            Kanji(level, columns[1].trim(), columns[2].trim(), columns[3].trim(),
                columns[4].split(',').map(String::trim).filter(String::isNotEmpty))
        }.also { list -> require(list.map { it.id }.distinct().size == list.size) { "Kanji dupliqué" } }
    }
}

/** Normalisations limitées aux équivalences convenues, pour éviter les faux positifs. */
object Answers {
    private val kanaStart = Regex("[\\u3040-\\u30ff]")
    fun readings(raw: String): List<String> {
        if (raw.isBlank() || raw == "(none)") return emptyList()
        val split = kanaStart.find(raw)?.range?.first ?: raw.length
        return listOf(raw.take(split), raw.drop(split)).flatMap { part ->
            part.split(',', '、', ';').flatMap { value ->
                val full = value.trim().trim('-').replace("(", "").replace(")", "")
                val stem = value.replace(Regex("\\([^)]*\\)"), "").trim().trim('-')
                listOf(full, stem).filter { it.isNotBlank() }
            }
        }.distinct()
    }
    fun pronunciation(value: String): String {
        // Préserver les longues avant de supprimer les autres diacritiques.
        val long = value.lowercase(Locale.ROOT).replace("ô", "ou").replace("ō", "ou")
            .replace("ū", "uu").replace("û", "uu").replace("ā", "aa")
            .replace("ī", "ii").replace("ē", "ee")
        val folded = Normalizer.normalize(long, Normalizer.Form.NFKC).map { c ->
            if (c in '\u30a1'..'\u30f6') (c.code - 0x60).toChar() else c
        }.joinToString("")
        return folded.replace(Regex("[\\s()\\-]"), "").replace("tchi", "chi").replace("oo", "ou")
    }
    fun acceptsReading(input: String, kanji: Kanji): Boolean = input.isNotBlank() &&
        kanji.readings.any { pronunciation(it) == pronunciation(input) }

    fun meaning(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace('’', '\'')
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

    /** Une faute à partir de 5 lettres, deux à partir de 10 ; mots courts stricts.
     * Les transpositions adjacentes comptent pour une faute. Les sens partiels sont refusés. */
    fun acceptsMeaning(input: String, kanji: Kanji): Boolean {
        val answer = meaning(input)
        if (answer.isEmpty()) return false
        return kanji.meanings.any {
            val expected = meaning(it)
            val length = minOf(answer.count(Char::isLetter), expected.count(Char::isLetter))
            val tolerance = when { length >= 10 -> 2; length >= 5 -> 1; else -> 0 }
            distance(answer, expected) <= tolerance
        }
    }
    private fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            d[i][j] = minOf(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1]+if(a[i-1]==b[j-1]) 0 else 1)
            if (i > 1 && j > 1 && a[i-1] == b[j-2] && a[i-2] == b[j-1])
                d[i][j] = minOf(d[i][j], d[i-2][j-2]+1)
        }
        return d[a.length][b.length]
    }
}

/** Les compteurs incluent l'entraînement libre ; seul le travail dû change les échéances. */
data class Progress(val stage: Int = 0, val shown: Int = 0, val correct: Int = 0,
    val wrong: Int = 0, val due: Long = 0) {
    val mastery: Int get() = stage * 20
    fun validate() {
        require(stage in 0..5 && shown >= 0 && correct >= 0 && wrong >= 0 && due >= 0)
        require(correct.toLong() + wrong.toLong() <= shown.toLong()) { "Compteurs incohérents" }
    }
}

object Leitner {
    const val DAY = 86_400_000L
    private val days = listOf(1, 2, 4, 8, 16)
    fun answer(p: Progress, correct: Boolean, now: Long, practice: Boolean): Progress {
        val stage = if (practice) p.stage else if (correct) (p.stage + 1).coerceAtMost(5) else 0
        val due = if (practice) p.due else now + days[(stage - 1).coerceAtLeast(0)] * DAY
        return p.copy(stage = stage, correct = p.correct + if (correct) 1 else 0,
            wrong = p.wrong + if (correct) 0 else 1, due = due)
    }
    fun pick(cards: List<Kanji>, progress: Map<String, Progress>, now: Long,
        practice: Boolean, previous: String? = null, random: Random = Random.Default): Kanji? {
        val available = cards.filter { practice || (progress[it.id]?.due ?: 0) <= now }
        // Éviter de répéter immédiatement la même carte lorsque d'autres sont disponibles.
        val choices = available.filter { it.id != previous }.ifEmpty { available }
        return choices.randomOrNull(random)
    }
}

enum class QuestionType(val title: String) {
    MEANING("Signification"), ROMAJI("Lecture en rōmaji"), KANA("Lecture en kana"), MIXED("Mélange")
}
data class Quiz(val type: QuestionType, val options: List<String>, val correctIndex: Int)

object Quizzes {
    /** Les distracteurs sont vérifiés contre toutes les réponses de la carte courante.
     * Deux kanji peuvent partager un sens ou une lecture : ce n'est jamais une fausse réponse. */
    fun make(card: Kanji, catalog: List<Kanji>, requested: QuestionType, random: Random = Random.Default): Quiz {
        fun values(k: Kanji, type: QuestionType) = when(type) {
            QuestionType.MEANING -> k.meanings
            QuestionType.ROMAJI -> k.romaji
            QuestionType.KANA -> k.kana
            QuestionType.MIXED -> emptyList()
        }
        val possible = QuestionType.entries.filter { it != QuestionType.MIXED && values(card, it).isNotEmpty() }
        val type = if (requested == QuestionType.MIXED || requested !in possible) possible.random(random) else requested
        fun normalize(s: String) = if (type == QuestionType.MEANING) Answers.meaning(s) else Answers.pronunciation(s)
        val accepted = values(card, type).map(::normalize).toSet()
        val correct = values(card, type).random(random)
        val distractors = catalog.filter { it.id != card.id }.flatMap { values(it, type) }
            .filter { normalize(it) !in accepted }
            .filter { type != QuestionType.MEANING || !Answers.acceptsMeaning(it, card) }
            .distinctBy(::normalize).shuffled(random).take(3)
        require(distractors.size == 3) { "Pas assez de réponses distinctes" }
        val options = (distractors + correct).shuffled(random)
        return Quiz(type, options, options.indexOf(correct))
    }
}
