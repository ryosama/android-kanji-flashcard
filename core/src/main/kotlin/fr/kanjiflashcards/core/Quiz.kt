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
 * @property type consigne commune, ou MIXED si les propositions combinent plusieurs types.
 * @property options quatre réponses dans leur ordre d'affichage.
 * @property correctIndex position de l'unique bonne réponse, de 0 à 3.
 * @property optionTypes type de chaque proposition, dans le même ordre que options.
 * Le type commun sert de valeur par défaut pour les QCM classiques et les anciennes séances.
 */
data class Quiz(
    val type: QuestionType,
    val options: List<String>,
    val correctIndex: Int,
    val optionTypes: List<QuestionType> = List(options.size) { type },
)
