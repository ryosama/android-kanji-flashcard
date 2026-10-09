package fr.kanjiflashcards.core

/**
 * Progression durable d'un kanji. Les compteurs incluent l'entraînement libre,
 * mais seuls les exercices de révision due modifient les paliers et les échéances.
 *
 * @property stage palier Leitner de 0 à 5, correspondant à 0–100 % de maîtrise.
 * @property shown nombre de présentations, y compris les cartes quittées sans réponse.
 * @property correct nombre de réponses globales correctes enregistrées.
 * @property wrong nombre de réponses globales incorrectes enregistrées.
 * @property due prochaine révision en millisecondes Unix ; 0 pour une carte jamais révisée.
 */
data class Progress(
    val stage: Int = 0,
    val shown: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val due: Long = 0,
) {
    /** Pourcentage individuel : chaque palier supplémentaire représente 20 %. */
    val mastery: Int
        get() = stage * 20

    /** Refuse des compteurs négatifs, un palier impossible ou plus de réponses que de présentations. */
    fun validate() {
        require(stage in 0..5 && shown >= 0 && correct >= 0 && wrong >= 0 && due >= 0)
        require(correct.toLong() + wrong.toLong() <= shown.toLong()) { "Compteurs incohérents" }
    }
}
