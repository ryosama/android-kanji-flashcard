package fr.kanjiflashcards.core

/** Types d'exercices proposés dans le choix du mode facile ; title est leur libellé visible. */
enum class QuestionType(val title: String) {
    MEANING("Signification"),
    ROMAJI("Lecture en rōmaji"),
    KANA("Lecture en kana"),
    MIXED("Mélange"),
}

/**
 * QCM prêt à afficher : son ordre est conservé pendant la rotation du téléphone.
 *
 * @property type type réellement tiré ; MIXED est un choix de séance, pas une consigne de carte.
 * @property options quatre réponses dans leur ordre d'affichage.
 * @property correctIndex position de l'unique bonne réponse, de 0 à 3.
 */
data class Quiz(
    val type: QuestionType,
    val options: List<String>,
    val correctIndex: Int,
)
