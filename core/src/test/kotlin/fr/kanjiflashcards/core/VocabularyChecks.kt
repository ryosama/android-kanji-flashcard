package fr.kanjiflashcards.core

import java.io.File
import kotlin.random.Random

/** Régression du catalogue vocabulaire, de ses limites, des QCM et des sauvegardes communes. */
object VocabularyChecks {
    /** Vérifie chaque carte dans son premier topic disponible et dans le catalogue complet. */
    fun verifyCatalog(directory: File): Int {
        var checks = 0

        /** Compte un contrôle et précise le comportement en cause lors d'une régression. */
        fun verify(condition: Boolean, message: String) {
            check(condition) { message }
            checks++
        }

        val cards = Catalog.parseVocabulary(File(directory, "Marugoto vocabulaire.csv").readText())
        verify(cards.size == 1746, "Vocabulaire incomplet")
        verify(cards.map { it.id }.distinct().size == cards.size, "Identifiants de vocabulaire dupliqués")
        verify(cards.all { it.level == StudySelection.VOCABULARY && it.book in 1..2 && it.lesson in 1..9 }, "Périmètre invalide")
        verify(cards.any { it.character == "魚" && it.kun == "さかな" }, "Kana et kanji non réunis")
        verify(cards.filter { it.kun == "かきます" }.map { it.meanings }.distinct().size >= 2, "Écrire et dessiner ont fusionné")
        verify(cards.any { it.kun == "ぼんおどり" }, "Page image 54 absente")
        verify(cards.any { it.kun == "ひとり" && it.meanings.any { value -> value.contains("personne") } }, "Tableau image 81 absent")
        verify(cards.any { it.kun == "にじゅうよっか" }, "Calendrier absent")
        verify(cards.any { it.kun == "スリッパ" }, "Page image 38 absente")
        verify(cards.any { it.kun == "じゅこうしゃ" && it.book == 1 && it.lesson == 1 }, "Premier topic incomplet")

        val drawings = cards.filter { it.kun == "かきます" && "dessiner" in it.meanings }
        verify(drawings.size == 1 && "peindre" in drawings.single().meanings, "Doublons de traduction non réunis")
        verify(cards.count { it.kun == "かきます" && "écrire" in it.meanings } == 1, "Sens distinct écrire perdu")

        val jlpt = (1..5).flatMap { level -> Catalog.parse(level, File(directory, "JLPT $level francais.csv").readText()) }
        val kanji = Catalog.parseMarugoto(File(directory, "Marugoto.csv").readText(), File(directory, "Marugoto variantes.tsv").readText())
        val catalog = jlpt + kanji + cards
        val ids = catalog.map { it.id }.toSet()
        verify(ids.size == 3163 + cards.size, "Collision entre catalogues")
        val progress = mapOf(
            "5:月" to Progress(stage = 2, shown = 3, correct = 2, wrong = 1),
            "marugoto:14" to Progress(stage = 1, shown = 1, correct = 1),
            cards.first().id to Progress(stage = 3, shown = 3, correct = 3),
        )
        verify(Backup.decode(Backup.encode(progress), ids) == progress, "Sauvegarde vocabulaire altérée")
        val legacy = progress.filterKeys { !it.startsWith("vocabulary:") }
        verify(Backup.decode(Backup.encode(legacy), ids) == legacy, "Ancienne sauvegarde refusée")
        verify(progress.filterKeys { it !in cards.map(Kanji::id) } == legacy, "Réinitialisation non isolée")

        for (book in 1..2) {
            for (topic in 1..9) {
                val selected = StudySelection(StudySelection.VOCABULARY, topic, book).cards(catalog)
                verify(selected == cards.filter { requireNotNull(it.book) < book || (it.book == book && requireNotNull(it.lesson) <= topic) }, "Cumul vocabulaire incorrect")
                verify(selected.size >= 4, "Pas assez de cartes au premier topic")
            }
        }
        verify(StudySelection(0, 9, 6).cards(catalog) == kanji, "Vocabulaire mélangé aux kanji")
        verify(StudySelection(5).cards(catalog).size == 80, "Vocabulaire mélangé aux JLPT")

        val random = Random(20261016)
        // Les index servent uniquement à retrouver les propriétaires des boutons pendant les contrôles.
        val pools = mutableMapOf<StudySelection, List<Kanji>>()
        val ownersByLabel = mutableMapOf<Pair<StudySelection, Boolean>, Map<String, List<Kanji>>>()
        for (card in cards) {
            val first = StudySelection(StudySelection.VOCABULARY, requireNotNull(card.lesson), requireNotNull(card.book))
            for (selection in listOf(first, StudySelection(StudySelection.VOCABULARY, 9, 2))) {
                val pool = pools.getOrPut(selection) { selection.cards(cards) }
                for (direction in VocabularyDirection.entries) {
                    val quiz = VocabularyQuizzes.make(card, pool, direction, random)
                    val french = direction == VocabularyDirection.JAPANESE_TO_FRENCH
                    verify(quiz.options.size == 4 && quiz.options.distinct().size == 4, "QCM incomplet ou répété ${card.id}")
                    verify(quiz.correctIndex in 0..3, "Bonne réponse absente")
                    verify(quiz.prompt == if (french) card.japaneseLabel else card.meanings.joinToString(" · "), "Mauvais sens du QCM")
                    verify(quiz.instruction != null, "Consigne absente")
                    for ((index, option) in quiz.options.withIndex()) {
                        val indexByLabel = ownersByLabel.getOrPut(selection to french) {
                            pool.groupBy { candidate ->
                                if (french) candidate.meanings.joinToString(" · ") else candidate.japaneseLabel
                            }
                        }
                        val owners = indexByLabel[option].orEmpty()
                        verify(owners.isNotEmpty(), "Proposition hors du périmètre")
                        verify(if (index == quiz.correctIndex) card in owners else owners.none { VocabularyQuizzes.overlaps(it, card) }, "Proposition ambiguë ${card.id}, $direction : $option")
                    }
                }
            }
        }
        verify(cards.first().copy(character = "となりの ひと", kun = "となりのひと").japaneseLabel == "となりの ひと", "Expression kana affichée deux fois")
        val variant = cards.first().copy(character = "仮", kun = "いいえ/いえ", meanings = listOf("non"))
        val homophone = cards.first().copy(character = "偽", kun = "いえ", meanings = listOf("maison"))
        verify(VocabularyQuizzes.overlaps(variant, homophone), "Une variante kana partagée est devenue un distracteur")
        val synonym = homophone.copy(kun = "べつ", meanings = listOf("la bibliothèque"))
        verify(VocabularyQuizzes.overlaps(variant.copy(meanings = listOf("bibliothèque")), synonym), "Article français créant une fausse différence")
        val bad = "bad;1;1;魚;さかな;sakana;poisson;source"
        verify(runCatching { Catalog.parseVocabulary(bad) }.isFailure, "Identifiant invalide accepté")
        return checks
    }
}
