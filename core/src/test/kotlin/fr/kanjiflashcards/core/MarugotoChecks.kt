package fr.kanjiflashcards.core

import java.io.File
import kotlin.random.Random

/** Contrôles du catalogue Marugoto, de ses lectures, de ses filtres et de ses sauvegardes. */
object MarugotoChecks {
    /** Exécute les contrôles additionnels et renvoie leur nombre pour le bilan du moteur. */
    fun verifyCatalog(directory: File): Int {
        var checks = 0

        /** Compte chaque contrôle réussi ; l'erreur indique la carte ou le comportement concerné. */
        fun verify(condition: Boolean, message: String) {
            check(condition) { message }
            checks++
        }

        val source = File(directory, "Marugoto.csv").readText()
        val variants = File(directory, "Marugoto variantes.tsv").readText()
        val cards = Catalog.parseMarugoto(source, variants)
        val byNumber = cards.associateBy { requireNotNull(it.entryNumber) }
        verify(cards.size == 668, "Nombre de cartes Marugoto")
        verify(byNumber.keys == (1..668).toSet(), "Numéros Marugoto incomplets")
        verify(cards.map { it.id }.distinct().size == 668, "Identifiants Marugoto non uniques")
        verify(cards.all { it.level == StudySelection.MARUGOTO && it.lesson in 1..18 }, "Leçons invalides")
        verify((481..494).all { byNumber.getValue(it).lesson == 18 }, "Annexe hors de la leçon 18")
        verify(byNumber.getValue(495).lesson == 4 && byNumber.getValue(579).lesson == 4, "La colonne 4 a été réinterprétée")
        verify(byNumber.getValue(145).kana.contains("いちねんせい"), "Lecture repliée perdue")
        verify(byNumber.getValue(431).kana.contains("やくろくねんかん"), "Exemple replié perdu")
        verify(byNumber.getValue(4).character == byNumber.getValue(16).character, "Fixture eau/mercredi")
        verify(byNumber.getValue(4).id != byNumber.getValue(16).id, "Deux lignes ont fusionné")
        verify(byNumber.getValue(4).meanings.contains("eau"), "Sens de 水 / みず")
        verify(byNumber.getValue(16).meanings.contains("mercredi"), "Sens de 水 / すい")
        verify(Answers.acceptsMeaning("neuf", byNumber.getValue(9)), "Adjectif neuf refusé pour 新しい")
        verify(!Answers.acceptsMeaning("9", byNumber.getValue(9)), "Adjectif neuf confondu avec le chiffre 9")
        verify(Answers.acceptsMeaning("9", byNumber.getValue(36)), "Chiffre 9 refusé pour 九")

        // Transcriptions connues indépendantes du générateur et règles de lecture usuelles.
        val romanizations = mapOf(
            "さかな" to "sakana", "たべます" to "tabemasu", "たべる" to "taberu",
            "とうきょう" to "toukyou", "しゅっちょう" to "shutchou", "がっこう" to "gakkou",
            "サッカーじょう" to "sakkaajou", "にほんご" to "nihongo", "じゅんび" to "junbi",
            "かんい" to "kan'i", "げんき" to "genki", "ぎゅうにく" to "gyuuniku",
        )
        for ((kana, latin) in romanizations) {
            verify(KanaRomaji.convert(kana) == latin, "Transcription incorrecte : $kana")
        }
        for ((number, readings) in mapOf(
            5 to listOf("たべます", "たべる", "tabemasu", "taberu"),
            61 to listOf("きます", "くる", "kimasu", "kuru"),
            211 to listOf("きます", "きる", "kimasu", "kiru"),
            23 to listOf("はなします", "はなす", "hanashimasu", "hanasu"),
            145 to listOf("ねんせい", "いちねんせい", "nensei", "ichinensei"),
            534 to listOf("せんたく", "せんたくする", "せんたくします"),
        )) {
            for (reading in readings) {
                verify(Answers.acceptsReading(reading, byNumber.getValue(number)), "Lecture refusée $number : $reading")
            }
        }
        verify(!Answers.acceptsReading("7", byNumber.getValue(497)), "Préfixe numérique accepté seul")
        verify(Answers.acceptsReading("shucchoo", byNumber.getValue(172)), "Variante rōmaji de consonne doublée refusée")
        verify(!Answers.acceptsReading("たべた", byNumber.getValue(5)), "Temps non prévu accepté")
        verify(!Answers.acceptsReading("はなする", byNumber.getValue(23)), "Faux dictionnaire accepté")
        verify(!Answers.acceptsReading("くる", byNumber.getValue(211)), "来る et 着る confondus")
        verify(!Answers.acceptsReading("ようびげつようび", byNumber.getValue(21)), "Exemple concaténé au suffixe")
        verify(Answers.acceptsReading("gakusee", byNumber.getValue(144)), "Forme rōmaji Marugoto ee refusée")
        verify(Answers.acceptsReading("がくせい", byNumber.getValue(144)), "Lecture kana refusée")

        // Conservation des anciens identifiants et round-trip de la progression de toutes les sources.
        val jlpt = (1..5).flatMap { level ->
            Catalog.parse(level, File(directory, "JLPT $level francais.csv").readText())
        }
        verify(jlpt.first { it.character == "水" }.id == "5:水", "Identifiant JLPT modifié")
        val ids = (jlpt + cards).map { it.id }.toSet()
        verify(ids.size == 3163, "Collision entre catalogues")
        val stats = mapOf(
            "5:水" to Progress(stage = 2, shown = 3, correct = 2, wrong = 1),
            "marugoto:4" to Progress(stage = 1, shown = 1, correct = 1),
            "marugoto:16" to Progress(stage = 0, shown = 1, wrong = 1),
        )
        verify(Backup.decode(Backup.encode(stats), ids) == stats, "Sauvegarde Marugoto altérée")
        val oldBackup = Backup.encode(mapOf("5:水" to stats.getValue("5:水")))
        verify(Backup.decode(oldBackup, ids) == mapOf("5:水" to stats.getValue("5:水")), "Ancien TSV refusé")
        verify(stats.getValue("marugoto:4") != stats.getValue("marugoto:16"), "Progressions de deux lignes confondues")

        val expectedCounts = listOf(26, 62, 93, 291, 326, 358, 384, 414, 442, 467, 491, 521, 550, 565, 583, 612, 629, 668)
        for (limit in 1..18) {
            val selected = StudySelection(StudySelection.MARUGOTO, limit).cards(jlpt + cards)
            verify(selected.size == expectedCounts[limit - 1], "Périmètre incorrect à la leçon $limit")
            verify(selected.all { requireNotNull(it.lesson) <= limit }, "Leçon future incluse")
            verify(selected.map { it.id }.toSet() == cards.filter { requireNotNull(it.lesson) <= limit }.map { it.id }.toSet(), "Cumul incomplet")
        }
        verify(StudySelection(5, 1).cards(jlpt + cards).size == 80, "Le filtre Marugoto modifie JLPT")
        verify(!StudySelection(StudySelection.MARUGOTO, 4).contains(byNumber.getValue(481)), "Annexe débloquée trop tôt")
        verify(stats.containsKey("marugoto:16"), "La réduction de filtre efface une carte masquée")
        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;19;poisson", "") }.isFailure, "Leçon 19 acceptée")
        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;1;", "") }.isFailure, "Traduction vide acceptée")
        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;1;poisson", "2\tにく") }.isFailure, "Variante inconnue acceptée")
        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;1;poisson\n1;肉;にく;1;viande", "") }.isFailure, "Numéro dupliqué accepté")

        // Chaque carte est testée à sa première leçon admissible et dans le catalogue complet.
        val random = Random(2026)
        for (limit in 1..18) {
            val selected = StudySelection(StudySelection.MARUGOTO, limit).cards(cards)
            val tested = if (limit == 18) selected else selected.filter { it.lesson == limit }
            for (card in tested) {
                verify(card.meanings.isNotEmpty() && card.kana.isNotEmpty() && card.romaji.isNotEmpty(), "Carte incomplète ${card.id}")
                for (type in QuestionType.entries) {
                    val quiz = Quizzes.make(card, selected, type, random)
                    verify(quiz.options.size == 4 && quiz.correctIndex in 0..3, "QCM incomplet ${card.id}")
                    verify(quiz.options.distinct().size == 4, "Choix identiques ${card.id}")
                    if (type == QuestionType.MIXED) {
                        verify(quiz.optionTypes.toSet() == setOf(QuestionType.MEANING, QuestionType.ROMAJI, QuestionType.KANA), "Formats Mélange absents")
                    }
                    for ((index, option) in quiz.options.withIndex()) {
                        val parts = option.split(" · ")
                        val accepts = parts.any { Answers.acceptsReading(it, card) || Answers.acceptsMeaning(it, card) }
                        verify(accepts == (index == quiz.correctIndex), "Proposition ambiguë ${card.id}: $option")
                        val source = if (index == quiz.correctIndex) listOf(card) else selected.filter { it.id != card.id }
                        val belongs = source.any { candidate ->
                            val expected = when (quiz.optionTypes[index]) {
                                QuestionType.MEANING -> candidate.meanings
                                QuestionType.ROMAJI -> candidate.romaji
                                QuestionType.KANA -> candidate.kana
                                QuestionType.MIXED -> emptyList()
                            }
                            parts == expected
                        }
                        verify(belongs, "Choix incomplet ou pris dans une leçon future ${card.id}: $option")
                        val scriptMatches = if (quiz.optionTypes[index] == QuestionType.KANA) {
                            option.any { it in '\u3040'..'\u30ff' }
                        } else {
                            option.none { it in '\u3040'..'\u30ff' }
                        }
                        verify(scriptMatches, "Mauvais alphabet ${card.id}: $option")
                    }
                }
            }
        }
        return checks
    }
}
