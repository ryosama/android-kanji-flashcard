package fr.kanjiflashcards

import android.content.Context
import android.util.AtomicFile
import fr.kanjiflashcards.core.Backup
import fr.kanjiflashcards.core.Progress
import java.io.File

/** Écriture atomique : la version précédente reste disponible si l'écriture échoue. */
class ProgressStore(context: Context, private val ids: Set<String>) {
    private val file = AtomicFile(File(context.filesDir, "progression.tsv"))
    fun load(): Map<String, Progress> = if (file.baseFile.exists())
        Backup.decode(file.openRead().bufferedReader().use { it.readText() }, ids) else emptyMap()
    fun save(progress: Map<String, Progress>) {
        val data = Backup.encode(progress).toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try { stream.write(data); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
}
