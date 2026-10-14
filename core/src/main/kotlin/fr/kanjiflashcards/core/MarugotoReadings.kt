package fr.kanjiflashcards.core

import java.text.Normalizer

/** Lectures du PDF, exemples de suffixes, variantes verbales et rōmaji associés. */
object MarugotoReadings {
    /** Deux groupes destinés à la validation et aux boutons du mode facile. */
    data class Readings(val kana: List<String>, val romaji: List<String>)

    /**
     * Sépare les lectures avec ／ et traite correctement les annotations du PDF.
     * « ～ようび(げつようび) » donne deux lectures, pas « ようびげつようび ».
     * « じゅんび(する) » donne la forme nominale et la forme verbale.
     */
    fun make(raw: String, additional: List<String>): Readings {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        val kana = normalized.split('/', ',', '、', ';').flatMap { part ->
            val base = part.replace(Regex("\\([^)]*\\)"), "").replace("~", "").trim()
            val annotations = Regex("\\(([^)]*)\\)").findAll(part).map { it.groupValues[1] }.toList()
            listOf(base) + annotations.map { annotation ->
                if (annotation == "する") base + annotation else annotation.replace("~", "").trim()
            }
        }.plus(additional).map { it.replace(" ", "") }.filter(String::isNotBlank).distinct()
        require(kana.isNotEmpty()) { "Aucune lecture Marugoto" }
        val romaji = kana.flatMap { reading ->
            val latin = KanaRomaji.convert(reading)
            // Les supports Marugoto utilisent aussi ee pour la prononciation longue de ei.
            val spellings = listOf(latin, latin.replace("ei", "ee"), latin.replace("tch", "cch"))
            spellings.flatMap { listOf(it, it.replace("'", "")) }
        }.distinct()
        return Readings(kana, romaji)
    }
}
