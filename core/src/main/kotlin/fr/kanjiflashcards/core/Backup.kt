package fr.kanjiflashcards.core

/**
 * Encode et décode le format TSV ouvert de la progression, sans dépendance Android.
 * Un import doit être entièrement validé avant toute écriture pour protéger les données actuelles.
 */
object Backup {
    /** Signature du fichier et version de son format, séparées par une tabulation. */
    private const val HEADER = "KANJI_FLASHCARDS\t1"

    /** Limite de contenu pour éviter de charger un document arbitrairement volumineux. */
    private const val MAX_TEXT_LENGTH = 2_000_000

    /** Échéance maximale autorisée, correspondant au début de l'année 3000 en UTC. */
    private const val MAX_DUE_TIME = 32_503_680_000_000L

    /**
     * Sérialise les fiches triées par identifiant pour produire un fichier stable et lisible.
     * Chaque ligne contient : identifiant, palier, présentations, succès, erreurs, échéance.
     */
    fun encode(progress: Map<String, Progress>): String = buildString {
        append(HEADER).append('\n')

        for ((id, stats) in progress.toSortedMap()) {
            stats.validate()
            require(!id.contains('\t') && !id.contains('\n'))

            val fields = listOf(id, stats.stage, stats.shown, stats.correct, stats.wrong, stats.due)
            append(fields.joinToString("\t")).append('\n')
        }
    }

    /**
     * Reconstitue la progression après contrôle de la version, des identifiants et des compteurs.
     * Refuse toute ligne incohérente plutôt que d'importer partiellement une sauvegarde abîmée.
     * @param knownIds identifiants du catalogue actuel ; aucun kanji inconnu n'est accepté.
     */
    fun decode(text: String, knownIds: Set<String>): Map<String, Progress> {
        require(text.length <= MAX_TEXT_LENGTH) { "Sauvegarde trop volumineuse" }
        val lines = text.removePrefix("\uFEFF").lineSequence().toList()
        require(lines.firstOrNull()?.trimEnd('\r') == HEADER) { "Format ou version de sauvegarde inconnu" }

        val result = linkedMapOf<String, Progress>()
        for (line in lines.drop(1).filter { it.isNotBlank() }) {
            val fields = line.trimEnd('\r').split('\t')
            require(fields.size == 6) { "Ligne de sauvegarde invalide" }

            val id = fields[0]
            require(id in knownIds && id !in result) { "Kanji inconnu ou dupliqué" }

            val stats = Progress(
                stage = fields[1].toInt(),
                shown = fields[2].toInt(),
                correct = fields[3].toInt(),
                wrong = fields[4].toInt(),
                due = fields[5].toLong(),
            )
            stats.validate()
            require(stats.due <= MAX_DUE_TIME) { "Échéance invalide" }
            result[id] = stats
        }

        return result
    }
}
