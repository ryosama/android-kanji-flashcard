package fr.kanjiflashcards.ui

import android.graphics.Color

/**
 * Palette sombre commune à tous les écrans.
 * Modifier une couleur ici change les composants qui l'utilisent dans toute l'application.
 */
object AppColors {
    /** Fond général des pages et couleur du texte sur les boutons clairs. */
    val background = Color.rgb(16, 20, 30)

    /** Fond des panneaux, des champs de réponse et des lignes du tableau. */
    val surface = Color.rgb(28, 35, 49)

    /** Texte principal et texte secondaire sur fond sombre. */
    val text = Color.rgb(238, 242, 250)
    val secondaryText = Color.rgb(169, 182, 202)

    /** Boutons principaux et indications de révision. */
    val accent = Color.rgb(116, 220, 197)

    /** Boutons secondaires : statistiques, choix du QCM et sauvegarde. */
    val secondaryAction = Color.rgb(188, 196, 245)

    /** Retours de correction : vert pour un succès, rouge pour une erreur. */
    val success = Color.rgb(125, 225, 155)
    val error = Color.rgb(255, 139, 151)

    /** Couleur de l'en-tête du tableau des statistiques. */
    val tableHeader = Color.rgb(44, 55, 74)

    /** Étapes intermédiaires du dégradé de maîtrise : rouge → orange → jaune → vert. */
    private val masteryStops = intArrayOf(
        error,
        Color.rgb(255, 183, 110),
        Color.rgb(244, 223, 113),
        success,
    )

    /** Convertit une maîtrise de 0 à 100 % en une couleur du dégradé des statistiques. */
    fun mastery(value: Int): Int {
        val position = value.coerceIn(0, 100) / 100f * 3
        val index = position.toInt().coerceAtMost(2)
        val fraction = position - index
        val start = masteryStops[index]
        val end = masteryStops[index + 1]

        /** Interpole une composante rouge, verte ou bleue entre deux étapes du dégradé. */
        fun interpolate(from: Int, to: Int): Int {
            return (from + (to - from) * fraction).toInt()
        }

        return Color.rgb(
            interpolate(Color.red(start), Color.red(end)),
            interpolate(Color.green(start), Color.green(end)),
            interpolate(Color.blue(start), Color.blue(end)),
        )
    }
}
