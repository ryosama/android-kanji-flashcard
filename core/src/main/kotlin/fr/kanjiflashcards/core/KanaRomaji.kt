package fr.kanjiflashcards.core

import java.text.Normalizer

/** Transcription Hepburn des lectures kana du catalogue Marugoto, sans service réseau. */
object KanaRomaji {
    /** Syllabes simples, y compris les kana petits utilisés dans les emprunts. */
    private val syllables = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'を' to "wo", 'ん' to "n", 'ゐ' to "i", 'ゑ' to "e",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
        'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゎ' to "wa", 'ゔ' to "vu",
    )

    /** Combinaisons de deux kana : sons contractés et syllabes étrangères usuelles. */
    private val combinations = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "てぃ" to "ti", "でぃ" to "di", "とぅ" to "tu", "どぅ" to "du",
        "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
        "うぃ" to "wi", "うぇ" to "we", "うぉ" to "wo", "しぇ" to "she", "ちぇ" to "che",
    )

    /**
     * Convertit les katakana en hiragana, puis transcrit les syllabes et consonnes doublées.
     * Le signe ー répète la voyelle précédente ; ん devant une voyelle ou y reçoit une apostrophe.
     * Les chiffres et lettres des exemples du PDF sont conservés.
     */
    fun convert(raw: String): String {
        val kana = Normalizer.normalize(raw, Normalizer.Form.NFKC).map { character ->
            if (character in '\u30a1'..'\u30f6') (character.code - 0x60).toChar() else character
        }.joinToString("")
        val result = StringBuilder()
        var index = 0
        while (index < kana.length) {
            val character = kana[index]
            when (character) {
                'っ' -> {
                    val next = syllable(kana, index + 1).first
                    result.append(if (next.startsWith("ch")) "t" else next.take(1))
                    index++
                }
                'ー' -> {
                    result.lastOrNull { it in "aeiou" }?.let { result.append(it) }
                    index++
                }
                'ん' -> {
                    val next = syllable(kana, index + 1).first
                    result.append(if (next.firstOrNull()?.let { it in "aeiouy" } == true) "n'" else "n")
                    index++
                }
                else -> {
                    val (latin, length) = syllable(kana, index)
                    result.append(latin)
                    index += length
                }
            }
        }
        return result.toString().lowercase()
    }

    /** Lit une syllabe en donnant priorité à une combinaison de deux caractères. */
    private fun syllable(kana: String, index: Int): Pair<String, Int> {
        if (index >= kana.length) return "" to 1
        val pair = kana.substring(index, minOf(index + 2, kana.length))
        combinations[pair]?.let { return it to 2 }
        val character = kana[index]
        return (syllables[character] ?: character.toString()) to 1
    }
}
