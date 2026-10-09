package fr.kanjiflashcards.core

import java.io.File
import kotlin.random.Random

/**
 * Lance les vérifications du moteur sans dépendance Android ni framework de test.
 * L'argument unique est le répertoire contenant les cinq CSV français.
 * Chaque contrôle échoue immédiatement avec un message décrivant la règle en cause.
 */
fun main(args: Array<String>) {
    // Compteur de contrôles réellement exécutés, affiché à la fin de la passe complète.
    var checks = 0

    /** Vérifie une condition puis compte le contrôle ; une erreur interrompt les tests. */
    fun verify(condition: Boolean, message: String) {
        check(condition) { message }
        checks++
    }

    /** Vérifie qu'une entrée invalide déclenche une exception au lieu d'être acceptée. */
    fun invalid(block: () -> Unit) {
        verify(runCatching(block).isFailure, "Une donnée invalide a été acceptée")
    }

    // Catalogue : contrôler les fichiers fournis et la présence de lectures et de sens.
    val directory = File(args.single())
    val catalog = (1..5).flatMap { level ->
        Catalog.parse(level, File(directory, "JLPT $level francais.csv").readText())
    }
    val counts = mapOf(1 to 1504, 2 to 374, 3 to 370, 4 to 167, 5 to 80)
    for ((level, size) in counts) {
        verify(catalog.count { it.level == level } == size, "Nombre N$level")
    }
    val ids = catalog.map { it.id }.toSet()
    verify(ids.size == 2495, "Identifiants du catalogue")
    for (card in catalog) {
        verify(card.readings.isNotEmpty() && card.meanings.isNotEmpty(), "Carte vide ${card.id}")
    }

    // Lectures : vérifier les terminaisons facultatives, les kana et les longues voyelles.
    val one = catalog.first { it.id == "5:一" }
    for (answer in listOf("ichi", "ICHI", "いち", "イチ", "hito", "hitotsu", "ひと", "ひとつ")) {
        verify(Answers.acceptsReading(answer, one), "Lecture rejetée : $answer")
    }
    verify(!Answers.acceptsReading("ni", one), "Lecture incorrecte acceptée")
    verify(!Answers.acceptsReading("", one), "Lecture vide acceptée")
    val large = catalog.first { it.id == "5:大" }
    for (answer in listOf("oo", "ou", "ô", "ō", "ookii", "ouk ii", "おおきい", "オオキイ")) {
        verify(Answers.acceptsReading(answer, large), "Voyelle longue : $answer")
    }
    val seven = catalog.first { it.id == "5:七" }
    verify(Answers.acceptsReading("tchichi", seven) == Answers.acceptsReading("chichi", seven), "Équivalence tchi")

    // Sens français : accepter les petites fautes tout en refusant les mots incomplets.
    val fictional = Kanji(5, "試", "chiチ", "", listOf("réunion", "présentation", "un", "nom de famille"))
    verify(Answers.acceptsReading("tchi", fictional), "tchi/chi")
    for (answer in listOf("REUNION", "réunoin", "reunon", "presantation", "  nom   de famille ")) {
        verify(Answers.acceptsMeaning(answer, fictional), "Sens rejeté : $answer")
    }
    for (answer in listOf("", "une", "nom", "réun", "constitution")) {
        verify(!Answers.acceptsMeaning(answer, fictional), "Sens invalide accepté : $answer")
    }
    val right = catalog.first { it.id == "5:右" }
    verify(Answers.acceptsMeaning("droite", right), "Précision entre parenthèses obligatoire")

    // CSV : un séparateur ou un guillemet à l’intérieur d’un champ reste une donnée.
    val escaped = Catalog.parse(5, "1;試;shiシ;;\"essai; test, \"\"épreuve\"\"\"\r\n")
    verify(escaped.single().meanings == listOf("essai; test", "\"épreuve\""), "Guillemets CSV")
    invalid { Catalog.parse(5, "1;試;shiシ") }
    invalid { Catalog.parse(5, "1;試;shiシ;;\"ouvert") }

    // Leitner : utiliser une horloge fixe pour contrôler exactement les délais des cinq paliers.
    val now = 1_000_000L

    // Progression simulée d'une même carte au cours de cinq réponses correctes successives.
    var p = Progress()
    for (stage in 1..5) {
        p = p.copy(shown = p.shown + 1)
        p = Leitner.answer(p, true, now, false)
        verify(p.stage == stage && p.mastery == stage * 20, "Palier $stage")
        verify(p.due == now + listOf(1, 2, 4, 8, 16)[stage - 1] * Leitner.DAY, "Échéance $stage")
        p.validate()
    }
    val practice = Leitner.answer(p.copy(shown = 6), false, now, true)
    verify(practice.stage == 5 && practice.due == p.due && practice.wrong == 1, "Entraînement libre")
    val failed = Leitner.answer(p.copy(shown = 6), false, now, false)
    verify(failed.stage == 0 && failed.due == now + Leitner.DAY, "Retour premier palier")
    verify(Leitner.pick(listOf(one), mapOf(one.id to p), now, false) == null, "Carte non due sélectionnée")
    verify(Leitner.pick(listOf(one), mapOf(one.id to p), now, true) == one, "Entraînement sans carte due")

    // Sauvegardes : vérifier les allers-retours et le rejet de chaque famille de données invalides.
    val persisted = mapOf(one.id to p, large.id to failed)
    verify(Backup.decode(Backup.encode(persisted), ids) == persisted, "Aller-retour sauvegarde")
    verify(Backup.decode(Backup.encode(emptyMap()), ids).isEmpty(), "Sauvegarde vide")
    invalid { Backup.decode("KANJI_FLASHCARDS\t2\n", ids) }
    invalid { Backup.decode("KANJI_FLASHCARDS\t1\n5:一\t6\t0\t0\t0\t0\n", ids) }
    invalid { Backup.decode("KANJI_FLASHCARDS\t1\n5:一\t0\t1\t2\t0\t0\n", ids) }
    invalid { Backup.decode("KANJI_FLASHCARDS\t1\n9:無\t0\t0\t0\t0\t0\n", ids) }
    invalid { Backup.decode(Backup.encode(persisted) + Backup.encode(persisted).substringAfter('\n'), ids) }
    invalid { Backup.decode("x".repeat(2_000_001), ids) }

    // Mélange : les marques sonores doivent permettre de proposer は face à ば ou ぱ.
    val voicedFixtures = listOf(
        Kanji(5, "甲", "haハ", "", listOf("feuille")),
        Kanji(5, "乙", "baバ", "", listOf("balle")),
        Kanji(5, "丙", "paパ", "", listOf("papier")),
        Kanji(5, "丁", "daダ", "", listOf("dent")),
    )
    verify(!Answers.acceptsReading("ば", voicedFixtures.first()), "Les sons ha et ba sont confondus")
    val voicedQuizzes = (0..255).map { seed ->
        Quizzes.make(voicedFixtures.first(), voicedFixtures, QuestionType.MIXED, Random(seed))
    }
    verify(voicedQuizzes.any { quiz ->
        val kana = quiz.options.filterIndexed { index, _ -> quiz.optionTypes[index] == QuestionType.KANA }
            .map(Answers::pronunciation)
        "は" in kana && kana.any { it == "ば" || it == "ぱ" }
    }, "Les kana distincts par leurs marques sonores ne peuvent pas être proposés ensemble")

    // L'affichage conserve les terminaisons facultatives et réunit les deux familles de lectures.
    val three = catalog.first { it.character == "三" }
    verify(three.romaji.toSet() == setOf("san", "mi(tsu)"), "Lectures rōmaji de 三 incomplètes")
    verify(three.kana.toSet() == setOf("サン", "み(つ)"), "Lectures kana de 三 incomplètes")

    // QCM : une graine fixe rend le tirage reproductible sur tous les niveaux et tous les types.
    val random = Random(42)
    for (level in 1..5) {
        val cards = catalog.filter { it.level == level }
        // Index indépendant des groupes complets du catalogue, préparé une fois par niveau.
        val groupOwners = QuestionType.entries.filter { it != QuestionType.MIXED }.associateWith { format ->
            cards.groupBy { candidate ->
                when (format) {
                    QuestionType.MEANING -> candidate.meanings
                    QuestionType.ROMAJI -> candidate.romaji
                    QuestionType.KANA -> candidate.kana
                    QuestionType.MIXED -> emptyList()
                }
            }
        }

        for (card in cards) {
            for (type in QuestionType.entries) {
                val quiz = Quizzes.make(card, cards, type, random)
                verify(quiz.options.size == 4 && quiz.correctIndex in 0..3, "QCM incomplet ${card.id}")
                verify(quiz.optionTypes.size == quiz.options.size, "Types absents ${card.id}")

                if (type == QuestionType.MIXED) {
                    verify(quiz.type == QuestionType.MIXED, "Consigne non mélangée ${card.id}")
                    verify(
                        quiz.optionTypes.toSet() == setOf(QuestionType.MEANING, QuestionType.ROMAJI, QuestionType.KANA),
                        "Les trois formats ne sont pas présents ${card.id}",
                    )
                } else {
                    verify(quiz.optionTypes.all { it == quiz.type }, "Format classique mélangé ${card.id}")
                }

                val optionKeys = quiz.options.mapIndexed { index, value ->
                    value.split(" · ").map { part ->
                        if (quiz.optionTypes[index] == QuestionType.MEANING) {
                            Answers.meaning(part)
                        } else {
                            Answers.pronunciation(part)
                        }
                    }.distinct().sorted()
                }
                verify(optionKeys.distinct().size == 4, "Propositions équivalentes ${card.id}: ${quiz.options}")

                for ((index, value) in quiz.options.withIndex()) {
                    val parts = value.split(" · ")
                    val expanded = if (quiz.optionTypes[index] == QuestionType.MEANING) {
                        parts
                    } else {
                        parts.flatMap(Answers::readings)
                    }
                    val accepts = expanded.any {
                        Answers.acceptsMeaning(it, card) || Answers.acceptsReading(it, card)
                    }
                    verify(accepts == (index == quiz.correctIndex), "Collision QCM ${card.id}: $value")

                    // Chaque bouton doit représenter un groupe complet d'une carte du même niveau.
                    val owners = groupOwners.getValue(quiz.optionTypes[index])[parts].orEmpty()
                    val hasExpectedSource = if (index == quiz.correctIndex) {
                        owners.any { it.id == card.id }
                    } else {
                        owners.any { it.id != card.id }
                    }
                    verify(hasExpectedSource, "Groupe incomplet ou extérieur au niveau ${card.id}: $value")

                    val optionType = quiz.optionTypes[index]
                    val matchesDeclaredType = if (optionType == QuestionType.KANA) {
                        value.any { it in '\u3040'..'\u30ff' }
                    } else {
                        value.none { it in '\u3040'..'\u30ff' }
                    }
                    verify(matchesDeclaredType, "Format incorrect ${card.id}: $value")
                }
            }
        }
    }

    println("$checks vérifications réussies sur ${catalog.size} kanji.")
}
