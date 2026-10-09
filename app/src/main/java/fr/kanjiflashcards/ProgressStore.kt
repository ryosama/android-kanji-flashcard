package fr.kanjiflashcards

import android.content.Context
import android.util.AtomicFile
import fr.kanjiflashcards.core.Backup
import fr.kanjiflashcards.core.Progress
import java.io.File

/**
 * Stockage de la progression dans le répertoire privé de l'application.
 * AtomicFile conserve une version cohérente si l'écriture est interrompue ou échoue.
 * @param ids identifiants du catalogue autorisés, contrôlés lors du chargement.
 */
class ProgressStore(context: Context, private val ids: Set<String>) {
    /** Fichier local utilisant le même format TSV versionné que les sauvegardes exportées. */
    private val file = AtomicFile(File(context.filesDir, "progression.tsv"))

    /** Charge et valide la progression ; une première installation renvoie une carte vide. */
    fun load(): Map<String, Progress> {
        if (!file.baseFile.exists()) {
            return emptyMap()
        }

        val contents = file.openRead().bufferedReader().use { it.readText() }
        return Backup.decode(contents, ids)
    }

    /** Remplace atomiquement les données ; restaure l'état antérieur en cas d'échec d'écriture. */
    fun save(progress: Map<String, Progress>) {
        val data = Backup.encode(progress).toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()

        try {
            stream.write(data)
            file.finishWrite(stream)
        } catch (exception: Exception) {
            file.failWrite(stream)
            throw exception
        }
    }
}
