package fr.kanjiflashcards.core

import kotlin.random.Random

/**
 * Moteur de répétition espacée selon le système de Leitner.
 * Une réussite monte d'un palier et espace la prochaine révision ; une erreur ramène au début.
 * Le tirage porte sur les cartes dues. L'entraînement libre conserve les paliers/échéances.
 * Cet objet ne lit ni n'écrit de fichier : l'activité enregistre la progression calculée.
 */
object Leitner {
    /** Durée de 24 heures en millisecondes, utilisée pour calculer les échéances Unix. */
    const val DAY = 86_400_000L

    /** Délais après réussite aux paliers 1 à 5 ; une erreur attend aussi le délai du premier palier. */
    private val days = listOf(1, 2, 4, 8, 16)

    /**
     * Renvoie la progression après une réponse, sans modifier l'objet reçu.
     * En révision : réussite jusqu'au palier 5, erreur au palier 0 et nouvelle échéance.
     * En entraînement libre : seuls les compteurs correct/incorrect changent.
     * La présentation a déjà été comptée lors du tirage, elle n'est donc pas ajoutée ici.
     *
     * @param now instant de validation, en millisecondes Unix.
     */
    fun answer(progress: Progress, correct: Boolean, now: Long, practice: Boolean): Progress {
        val stage = when {
            practice -> progress.stage
            correct -> (progress.stage + 1).coerceAtMost(5)
            else -> 0
        }

        val due = if (practice) {
            progress.due
        } else {
            // Le palier 0 après une erreur utilise le même délai d'un jour que le palier 1.
            val intervalIndex = (stage - 1).coerceAtLeast(0)
            now + days[intervalIndex] * DAY
        }

        return progress.copy(
            stage = stage,
            correct = progress.correct + if (correct) 1 else 0,
            wrong = progress.wrong + if (correct) 0 else 1,
            due = due,
        )
    }

    /**
     * Tire au hasard une carte due, ou toute carte du niveau si practice vaut true.
     * Évite la carte précédente lorsque d'autres sont disponibles ; renvoie null si aucune ne l'est.
     * Le générateur aléatoire est injectable pour rendre les tests reproductibles.
     */
    fun pick(
        cards: List<Kanji>,
        progress: Map<String, Progress>,
        now: Long,
        practice: Boolean,
        previous: String? = null,
        random: Random = Random.Default,
    ): Kanji? {
        val available = cards.filter { card ->
            practice || (progress[card.id]?.due ?: 0) <= now
        }

        val choices = available.filter { it.id != previous }.ifEmpty { available }
        return choices.randomOrNull(random)
    }
}
