package fr.kanjiflashcards.core

import java.io.File
import kotlin.random.Random

/** Vérifications exécutables sans framework : vrais fichiers, collisions des QCM,
 * limites de correction, cycle Leitner et sauvegardes invalides. */
fun main(args: Array<String>) {
    var checks = 0
    fun verify(condition: Boolean, message: String) { check(condition) { message }; checks++ }
    fun invalid(block: () -> Unit) { verify(runCatching(block).isFailure, "Une donnée invalide a été acceptée") }
    val directory = File(args.single())
    val catalog = (1..5).flatMap { level -> Catalog.parse(level, File(directory, "JLPT $level francais.csv").readText()) }
    val counts = mapOf(1 to 1504, 2 to 374, 3 to 370, 4 to 167, 5 to 80)
    counts.forEach { (n, size) -> verify(catalog.count { it.level == n } == size, "Nombre N$n") }
    val ids = catalog.map { it.id }.toSet()
    verify(ids.size == 2495, "Identifiants du catalogue")
    catalog.forEach { verify(it.readings.isNotEmpty() && it.meanings.isNotEmpty(), "Carte vide ${it.id}") }
    val one = catalog.first { it.id == "5:一" }
    for(answer in listOf("ichi", "ICHI", "いち", "イチ", "hito", "hitotsu", "ひと", "ひとつ"))
        verify(Answers.acceptsReading(answer, one), "Lecture rejetée : $answer")
    verify(!Answers.acceptsReading("ni", one), "Lecture incorrecte acceptée")
    verify(!Answers.acceptsReading("", one), "Lecture vide acceptée")
    val large = catalog.first { it.id == "5:大" }
    for(answer in listOf("oo", "ou", "ô", "ō", "ookii", "ouk ii", "おおきい", "オオキイ"))
        verify(Answers.acceptsReading(answer, large), "Voyelle longue : $answer")
    val seven = catalog.first { it.id == "5:七" }
    verify(Answers.acceptsReading("tchichi", seven) == Answers.acceptsReading("chichi", seven), "Équivalence tchi")
    val fictional = Kanji(5, "試", "chiチ", "", listOf("réunion", "présentation", "un", "nom de famille"))
    verify(Answers.acceptsReading("tchi", fictional), "tchi/chi")
    for(answer in listOf("REUNION", "réunoin", "reunon", "presantation", "  nom   de famille "))
        verify(Answers.acceptsMeaning(answer, fictional), "Sens rejeté : $answer")
    for(answer in listOf("", "une", "nom", "réun", "constitution"))
        verify(!Answers.acceptsMeaning(answer, fictional), "Sens invalide accepté : $answer")
    val right = catalog.first { it.id == "5:右" }
    verify(Answers.acceptsMeaning("droite", right), "Précision entre parenthèses obligatoire")
    val escaped = Catalog.parse(5, "1;試;shiシ;;\"essai; test, \"\"épreuve\"\"\"\r\n")
    verify(escaped.single().meanings == listOf("essai; test", "\"épreuve\""), "Guillemets CSV")
    invalid { Catalog.parse(5, "1;試;shiシ") }
    invalid { Catalog.parse(5, "1;試;shiシ;;\"ouvert") }
    val now = 1_000_000L
    var p = Progress()
    for(stage in 1..5) {
        p = p.copy(shown = p.shown + 1)
        p = Leitner.answer(p, true, now, false)
        verify(p.stage == stage && p.mastery == stage * 20, "Palier $stage")
        verify(p.due == now + listOf(1, 2, 4, 8, 16)[stage-1] * Leitner.DAY, "Échéance $stage")
        p.validate()
    }
    val practice = Leitner.answer(p.copy(shown = 6), false, now, true)
    verify(practice.stage == 5 && practice.due == p.due && practice.wrong == 1, "Entraînement libre")
    val failed = Leitner.answer(p.copy(shown = 6), false, now, false)
    verify(failed.stage == 0 && failed.due == now + Leitner.DAY, "Retour premier palier")
    verify(Leitner.pick(listOf(one), mapOf(one.id to p), now, false) == null, "Carte non due sélectionnée")
    verify(Leitner.pick(listOf(one), mapOf(one.id to p), now, true) == one, "Entraînement sans carte due")
    val persisted = mapOf(one.id to p, large.id to failed)
    verify(Backup.decode(Backup.encode(persisted), ids) == persisted, "Aller-retour sauvegarde")
    verify(Backup.decode(Backup.encode(emptyMap()), ids).isEmpty(), "Sauvegarde vide")
    invalid { Backup.decode("KANJI_FLASHCARDS\t2\n", ids) }
    invalid { Backup.decode("KANJI_FLASHCARDS\t1\n5:一\t6\t0\t0\t0\t0\n", ids) }
    invalid { Backup.decode("KANJI_FLASHCARDS\t1\n5:一\t0\t1\t2\t0\t0\n", ids) }
    invalid { Backup.decode("KANJI_FLASHCARDS\t1\n9:無\t0\t0\t0\t0\t0\n", ids) }
    invalid { Backup.decode(Backup.encode(persisted) + Backup.encode(persisted).substringAfter('\n'), ids) }
    invalid { Backup.decode("x".repeat(2_000_001), ids) }
    val random = Random(42)
    for(level in 1..5) {
        val cards = catalog.filter { it.level == level }
        for(k in cards) for(type in QuestionType.entries) {
            val q = Quizzes.make(k, cards, type, random)
            verify(q.options.size == 4 && q.correctIndex in 0..3, "QCM incomplet ${k.id}")
            for((i, value) in q.options.withIndex()) {
                val accepts = if(q.type == QuestionType.MEANING) Answers.acceptsMeaning(value, k) else Answers.acceptsReading(value, k)
                verify(accepts == (i == q.correctIndex), "Collision QCM ${k.id}: $value")
            }
        }
    }
    println("$checks vérifications réussies sur ${catalog.size} kanji.")
}
