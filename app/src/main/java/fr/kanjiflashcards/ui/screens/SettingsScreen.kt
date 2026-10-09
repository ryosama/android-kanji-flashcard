package fr.kanjiflashcards.ui.screens

import android.app.AlertDialog
import fr.kanjiflashcards.ui.AppColors
import fr.kanjiflashcards.ui.UiComponents

/** Écran de sauvegarde : export, import et remise à zéro de la progression par niveau. */
class SettingsScreen(
    private val ui: UiComponents,
    private val onExport: () -> Unit,
    private val onImport: () -> Unit,
    private val onReset: (Int) -> Unit,
) {
    /** Affiche les explications et les boutons ; le transfert effectif appartient à BackupDocuments. */
    fun show() {
        val content = ui.screen("Sauvegarde")
        content.addView(ui.text(
            "Tes progrès restent sur ce téléphone. Exporte une sauvegarde avant de désinstaller l'application ou de changer de téléphone.",
            color = AppColors.secondaryText,
        ))

        // Ces boutons ouvrent le sélecteur de documents Android, sans permission de stockage globale.
        content.addView(ui.button("Exporter la progression", action = onExport))
        content.addView(ui.button("Importer une sauvegarde", AppColors.secondaryAction, onImport))
        content.addView(ui.text(
            "L'import remplace la progression actuelle après confirmation. Seuls les fichiers de sauvegarde de cette application sont acceptés.",
            14f,
            AppColors.secondaryText,
        ))

        content.addView(ui.text("Réinitialiser un niveau", 21f, bold = true))
        for (level in 5 downTo 1) {
            content.addView(ui.button("Réinitialiser JLPT N$level", AppColors.error) {
                confirmReset(level)
            })
        }

        content.addView(ui.text(
            "Kanji Flashcards · 0.1.0\nSans compte · Sans connexion · Sans publicité",
            14f,
            AppColors.secondaryText,
        ))
    }

    /** Demande confirmation dans l'application avant d'effacer les statistiques d'un niveau. */
    private fun confirmReset(level: Int) {
        AlertDialog.Builder(ui.activity)
            .setTitle("Effacer la progression N$level ?")
            .setMessage("Les paliers, les échéances et les compteurs de ce niveau seront supprimés. Pense à exporter une sauvegarde.")
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Réinitialiser") { _, _ -> onReset(level) }
            .show()
    }
}
