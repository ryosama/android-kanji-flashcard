package fr.kanjiflashcards.core

/**
 * Lecteur des listes de kanji au format CSV avec séparateur point-virgule.
 * Les guillemets protègent les séparateurs à l'intérieur des champs.
 * Aucun fichier source n'est modifié : le résultat est une liste d'objets Kanji en mémoire.
 */
object Catalog {
    /**
     * Convertit le contenu d'un CSV en cartes d'un niveau JLPT.
     * Refuse les guillemets non fermés, les lignes à colonnes manquantes et les kanji dupliqués.
     * La première colonne numérique est ignorée ; les fichiers n'ont pas de ligne d'en-tête.
     */
    fun parse(level: Int, text: String): List<Kanji> {
        return records(text).map { columns ->
            require(columns.size == 5 && columns[1].isNotBlank()) { "Entrée CSV invalide" }

            Kanji(
                level = level,
                character = columns[1].trim(),
                on = columns[2].trim(),
                kun = columns[3].trim(),
                meanings = columns[4].split(',').map(String::trim).filter(String::isNotEmpty),
            )
        }.also { cards ->
            require(cards.map { it.id }.distinct().size == cards.size) { "Kanji dupliqué" }
        }
    }

    /** Lit les cinq colonnes Marugoto et complète les lectures à l'aide des variantes contrôlées. */
    fun parseMarugoto(text: String, variantsText: String): List<Kanji> {
        val variants = linkedMapOf<Int, List<String>>()
        for (line in variantsText.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }) {
            val fields = line.split('\t')
            require(fields.size == 2) { "Variantes Marugoto invalides" }
            val number = fields[0].toInt()
            require(number > 0 && number !in variants) { "Numéro de variante invalide ou dupliqué" }
            variants[number] = fields[1].split('|').filter(String::isNotBlank)
        }

        val cards = records(text).map { columns ->
            require(columns.size == 5) { "Entrée Marugoto invalide" }
            val number = columns[0].toInt()
            val lesson = columns[3].toInt()
            require(number > 0 && lesson in 1..18) { "Numéro ou leçon Marugoto invalide" }
            require(columns[1].isNotBlank() && columns[2].isNotBlank() && columns[4].isNotBlank()) {
                "Carte Marugoto incomplète"
            }
            val readings = MarugotoReadings.make(columns[2], variants[number].orEmpty())
            Kanji(
                level = StudySelection.MARUGOTO,
                character = columns[1].trim(),
                on = readings.romaji.joinToString(", "),
                kun = readings.kana.joinToString(", "),
                meanings = columns[4].split(',').map(String::trim).filter(String::isNotEmpty),
                entryNumber = number,
                lesson = lesson,
            )
        }
        require(cards.map { it.id }.distinct().size == cards.size) { "Numéro Marugoto dupliqué" }
        require(variants.keys.all { number -> cards.any { it.entryNumber == number } }) {
            "Variante sans carte Marugoto correspondante"
        }
        return cards
    }

    /** Découpe un CSV à points-virgules en cellules, en respectant les guillemets et retours de ligne. */
    private fun records(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()

        // État de lecture : un séparateur n'agit que si l'on est hors d'un champ entre guillemets.
        var quoted = false
        var index = 0
        val input = text.removePrefix("\uFEFF")

        while (index < input.length) {
            val character = input[index]

            when {
                character == '"' && quoted && input.getOrNull(index + 1) == '"' -> {
                    // Deux guillemets consécutifs représentent un guillemet littéral dans le champ.
                    field.append('"')
                    index++
                }

                character == '"' -> {
                    quoted = !quoted
                }

                character == ';' && !quoted -> {
                    row.add(field.toString())
                    field.clear()
                }

                character == '\n' && !quoted -> {
                    row.add(field.toString().trimEnd('\r'))
                    field.clear()

                    if (row.any { it.isNotBlank() }) {
                        records.add(row)
                    }
                    row = mutableListOf()
                }

                else -> {
                    field.append(character)
                }
            }
            index++
        }

        require(!quoted) { "Guillemets CSV non fermés" }

        // La dernière ligne peut ne pas se terminer par un retour à la ligne.
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            records.add(row)
        }

        return records
    }

}
