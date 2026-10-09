package fr.kanjiflashcards.core

/** Format ouvert, versionné, sans données personnelles. Un import est validé intégralement
 * avant d'être enregistré : une sauvegarde abîmée ne doit pas effacer la progression. */
object Backup {
    private const val HEADER = "KANJI_FLASHCARDS\t1"
    fun encode(progress: Map<String, Progress>): String = buildString {
        append(HEADER).append('\n')
        for ((id, p) in progress.toSortedMap()) {
            p.validate()
            require(!id.contains('\t') && !id.contains('\n'))
            append(listOf(id, p.stage, p.shown, p.correct, p.wrong, p.due).joinToString("\t")).append('\n')
        }
    }
    fun decode(text: String, knownIds: Set<String>): Map<String, Progress> {
        require(text.length <= 2_000_000) { "Sauvegarde trop volumineuse" }
        val lines = text.removePrefix("\uFEFF").lineSequence().toList()
        require(lines.firstOrNull()?.trimEnd('\r') == HEADER) { "Format ou version de sauvegarde inconnu" }
        val result = linkedMapOf<String, Progress>()
        for (line in lines.drop(1).filter { it.isNotBlank() }) {
            val fields = line.trimEnd('\r').split('\t')
            require(fields.size == 6) { "Ligne de sauvegarde invalide" }
            val id = fields[0]
            require(id in knownIds && id !in result) { "Kanji inconnu ou dupliqué" }
            val p = Progress(fields[1].toInt(), fields[2].toInt(), fields[3].toInt(), fields[4].toInt(), fields[5].toLong())
            p.validate()
            require(p.due <= 32_503_680_000_000L) { "Échéance invalide" }
            result[id] = p
        }
        return result
    }
}
