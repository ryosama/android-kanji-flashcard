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
        verify(cards.all { it.level == StudySelection.MARUGOTO && it.lesson in MarugotoBooks.lessons(requireNotNull(it.book)) }, "Leçons invalides")
        verify((481..494).all { byNumber.getValue(it).lesson == 9 }, "Annexe hors du topic 9")
        verify(byNumber.getValue(495).lesson == 1 && byNumber.getValue(579).lesson == 1, "Partie 4 confondue avec le topic")
        verify(byNumber.getValue(568).lesson == 8 && byNumber.getValue(659).lesson == 9, "Limites des topics intermédiaires")
        val ranges = listOf(1..65, 66..221, 222..391, 392..494, 495..578, 579..668)
        for ((index, range) in ranges.withIndex()) {
            verify(range.all { byNumber.getValue(it).book == index + 1 }, "Livre incorrect ${index + 1}")
        }
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
        // Le mode normal accepte toute réponse JLPT du même caractère, sans modifier le QCM.
        val moon = NormalAnswers.expand(byNumber.getValue(14), jlpt + cards)
        for (reading in listOf("tsuki", "getsu", "gatsu", "つき", "ゲツ", "ガツ")) {
            verify(Answers.acceptsReading(reading, moon), "Lecture de 月 refusée : $reading")
        }
        for (meaning in listOf("lune", "mois", "lundi")) {
            verify(Answers.acceptsMeaning(meaning, moon), "Sens de 月 refusé : $meaning")
        }
        verify(!Answers.acceptsMeaning("soleil", moon), "Sens sans rapport accepté")
        verify(!Answers.acceptsReading("nichi", moon), "Lecture sans rapport acceptée")
        verify(!Answers.acceptsMeaning("lune", byNumber.getValue(14)), "Le catalogue QCM a été modifié")
        verify(NormalAnswers.expand(byNumber.getValue(5), jlpt + cards) == byNumber.getValue(5), "食べます a reçu les sens de 食")
        for (card in cards) {
            val expanded = NormalAnswers.expand(card, jlpt + cards)
            verify(expanded.id == card.id && expanded.book == card.book && expanded.lesson == card.lesson,
                "Identité ou périmètre modifié par l'enrichissement")
            val matches = jlpt.filter { it.character == card.character }
            for (reading in card.readings + matches.flatMap { it.readings }) {
                verify(Answers.acceptsReading(reading, expanded), "Lecture source perdue ${card.id} : $reading")
            }
            for (meaning in card.meanings + matches.flatMap { it.meanings }) {
                verify(Answers.acceptsMeaning(meaning, expanded), "Sens source perdu ${card.id} : $meaning")
            }
        }
        verify(NormalAnswers.expand(jlpt.first(), jlpt + cards) == jlpt.first(), "Carte JLPT modifiée")
        verify(MarugotoBooks.lessons(1) == (3..9).toList(), "Topics にゅうもん incorrects")
        for (book in 2..6) {
            verify(MarugotoBooks.lessons(book) == (1..9).toList(), "Topics incorrects livre $book")
        }
        verify(MarugotoBooks.migrateLesson(1, 1) == 3, "Ancien début にゅうもん invalide")
        verify(MarugotoBooks.migrateLesson(2, 4) == 2, "Ancienne leçon 4 non convertie en topic 2")
        verify(MarugotoBooks.migrateLesson(3, 18) == 9, "Ancienne leçon 18 non convertie")
        verify(MarugotoBooks.migrateLesson(4, 18) == 9, "Ancienne annexe perdue")
        verify(byNumber.getValue(1).lesson == 3 && byNumber.getValue(6).lesson == 3, "Première paire de leçons non regroupée")
        verify(StudySelection(0, 9, 4).cards(cards).last().entryNumber == 494, "Annexe absente du topic 9")
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

        // Toutes les limites : livres précédents complets, puis leçon/topic dans le livre courant.
        for (book in 1..6) {
            for (limit in MarugotoBooks.lessons(book)) {
                val selected = StudySelection(StudySelection.MARUGOTO, limit, book).cards(jlpt + cards)
                val expected = cards.filter { requireNotNull(it.book) < book || (it.book == book && requireNotNull(it.lesson) <= limit) }
                verify(selected == expected, "Cumul incorrect livre $book, limite $limit")
                verify(selected.map { it.entryNumber } == selected.map { it.entryNumber }.sortedBy { it }, "Ordre PDF modifié")
                verify(selected.none { requireNotNull(it.book) > book }, "Livre futur inclus")
            }
            verify(StudySelection(0, MarugotoBooks.lessonLimit(book), book).cards(cards).size == ranges[book - 1].last,
                "Livre précédent incomplet $book")
        }
        val example = StudySelection(0, 2, 2).cards(cards)
        verify(example.size == 104 && example.last().entryNumber == 104, "Exemple しょきゅう 1 / topic 2 incorrect")
        verify(StudySelection(0, 8, 4).cards(cards).last().entryNumber == 472, "Annexe incluse avant le topic 9")
        verify(StudySelection(0, 1, 5).cards(cards).any { it.entryNumber == 494 }, "Annexe du livre précédent oubliée")
        verify(StudySelection(5, 1).cards(jlpt + cards).size == 80, "Le filtre Marugoto modifie JLPT")
        verify(!StudySelection(StudySelection.MARUGOTO, 4, 4).contains(byNumber.getValue(481)), "Annexe débloquée trop tôt")
        verify(stats.containsKey("marugoto:16"), "La réduction de filtre efface une carte masquée")
        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;19;poisson;1", "") }.isFailure, "Leçon 19 acceptée")
        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;3;;1", "") }.isFailure, "Traduction vide acceptée")
        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;3;poisson;1", "2\tにく") }.isFailure, "Variante inconnue acceptée")
        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;3;poisson;1\n1;肉;にく;3;viande;1", "") }.isFailure, "Numéro dupliqué accepté")

        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;1;poisson;7", "") }.isFailure, "Livre 7 accepté")
        verify(runCatching { Catalog.parseMarugoto("1;魚;さかな;10;poisson;5", "") }.isFailure, "Topic 10 intermédiaire accepté")

        // Chaque carte est testée à sa première sélection admissible et dans le catalogue complet.
        val random = Random(2026)
        for (card in cards) {
            val selections = listOf(StudySelection(0, requireNotNull(card.lesson), requireNotNull(card.book)), StudySelection(0))
            for (selection in selections) {
                val selected = selection.cards(cards)
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
