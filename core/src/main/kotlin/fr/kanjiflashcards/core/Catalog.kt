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

        return records.map { columns ->
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
}
