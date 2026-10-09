package fr.kanjiflashcards

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import fr.kanjiflashcards.core.Backup
import fr.kanjiflashcards.core.Progress
import fr.kanjiflashcards.ui.UiComponents
import java.io.ByteArrayOutputStream

/**
 * Transferts via le sélecteur de documents Android (Storage Access Framework).
 * Les accès au fournisseur de fichiers se font hors du thread graphique.
 * Un import est validé entièrement et confirmé avant de remplacer la progression.
 */
class BackupDocuments(
    private val activity: Activity,
    private val ui: UiComponents,
    private val knownIds: Set<String>,
    private val currentProgress: () -> Map<String, Progress>,
    private val onImportConfirmed: (Map<String, Progress>) -> Unit,
) {
    /** Identifiants et limite de lecture propres aux échanges avec le sélecteur Android. */
    companion object {
        /** Codes qui distinguent export et import dans le résultat du sélecteur Android. */
        private const val EXPORT_REQUEST = 41
        private const val IMPORT_REQUEST = 42

        /** Limite de lecture pour refuser un fichier trop grand avant son décodage. */
        private const val MAX_IMPORT_BYTES = 2_000_000
    }

    /** Bloque les boutons durant le transfert, mais pas pendant le choix du document. */
    var busy = false
        private set

    /** Instantané pris avant le sélecteur et conservé par l'activité lors d'une rotation. */
    var pendingExport: String? = null

    /** Prend l'instantané puis demande à Android où créer le fichier de sauvegarde. */
    @Suppress("DEPRECATION")
    fun startExport() {
        pendingExport = Backup.encode(currentProgress())
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "kanji-progression.tsv")
        }
        activity.startActivityForResult(intent, EXPORT_REQUEST)
    }

    /** Ouvre le choix d'un fichier existant ; son format réel sera contrôlé après lecture. */
    @Suppress("DEPRECATION")
    fun startImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        activity.startActivityForResult(intent, IMPORT_REQUEST)
    }

    /** Traite le résultat du sélecteur sur un thread secondaire pour garder l'interface fluide. */
    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        val uri = data?.data ?: return
        busy = true
        ui.message(if (requestCode == EXPORT_REQUEST) "Export en cours…" else "Lecture de la sauvegarde…")

        Thread {
            try {
                when (requestCode) {
                    EXPORT_REQUEST -> writeExport(uri)
                    IMPORT_REQUEST -> readImport(uri)
                    else -> activity.runOnUiThread { busy = false }
                }
            } catch (exception: Exception) {
                activity.runOnUiThread {
                    busy = false
                    ui.message("Opération impossible : ${exception.message}")
                }
            }
        }.start()
    }

    /** Écrit l'instantané dans le document choisi, puis signale la réussite à l'utilisateur. */
    private fun writeExport(uri: Uri) {
        val contents = pendingExport ?: Backup.encode(currentProgress())
        val output = activity.contentResolver.openOutputStream(uri, "wt") ?: error("Fichier inaccessible")
        output.bufferedWriter().use { it.write(contents) }

        activity.runOnUiThread {
            busy = false
            pendingExport = null
            ui.message("Sauvegarde exportée")
        }
    }

    /** Lit un fichier de taille bornée, le valide puis demande confirmation d'import. */
    private fun readImport(uri: Uri) {
        val input = activity.contentResolver.openInputStream(uri) ?: error("Fichier inaccessible")
        val contents = input.use { stream ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8192)

            // Un bloc au-delà de la limite suffit pour détecter un fichier trop volumineux.
            while (buffer.size() <= MAX_IMPORT_BYTES) {
                val count = stream.read(chunk)
                if (count < 0) {
                    break
                }
                buffer.write(chunk, 0, count)
            }

            val bytes = buffer.toByteArray()
            require(bytes.size <= MAX_IMPORT_BYTES) { "Sauvegarde trop volumineuse" }
            bytes.toString(Charsets.UTF_8)
        }

        val restored = Backup.decode(contents, knownIds)
        activity.runOnUiThread {
            busy = false
            confirmImport(restored)
        }
    }

    /** Présente le fichier validé ; seul « Importer » déclenche son enregistrement local. */
    private fun confirmImport(restored: Map<String, Progress>) {
        val responseCount = restored.values.sumOf { it.correct.toLong() + it.wrong }
        AlertDialog.Builder(activity)
            .setTitle("Importer ${restored.size} fiches ?")
            .setMessage("Cette sauvegarde contient $responseCount réponses. Elle remplacera la progression actuelle de tous les niveaux.")
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Importer") { _, _ -> onImportConfirmed(restored) }
            .show()
    }
}
