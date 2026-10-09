package fr.kanjiflashcards.ui

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

/**
 * Fonctions graphiques communes : textes, boutons, panneaux et structure des pages.
 * Les tailles sont exprimées en dp pour la mise en page, et en sp pour les textes.
 *
 * @param activity activité qui héberge les vues et fournit les ressources Android.
 * @param actionsAllowed vérifie qu'aucun transfert de sauvegarde ne bloque les boutons.
 * @param onHome action du bouton « Accueil » présent dans l'en-tête des pages secondaires.
 */
class UiComponents(
    val activity: Activity,
    private val actionsAllowed: () -> Boolean,
    private val onHome: () -> Unit,
) {
    /** Convertit une dimension indépendante de la densité (dp) en pixels du téléphone. */
    fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }

    /** Crée un fond arrondi, avec une bordure facultative, pour un panneau ou un bouton. */
    fun roundedBackground(color: Int, border: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(16).toFloat()

            if (border != null) {
                setStroke(dp(1), border)
            }
        }
    }

    /** Crée un libellé ; taille, couleur et graisse peuvent être adaptés par chaque écran. */
    fun text(
        value: String,
        size: Float = 16f,
        color: Int = AppColors.text,
        bold: Boolean = false,
    ): TextView {
        return TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(color)
            setPadding(0, dp(8), 0, dp(8))

            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }
    }

    /** Crée un conteneur qui dispose ses enfants verticalement, du haut vers le bas. */
    fun column(): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
    }

    /** Crée un conteneur horizontal dont les enfants sont centrés sur l'axe vertical. */
    fun row(): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    /** Crée un bouton arrondi et exécute son action si les transferts ne sont pas en cours. */
    fun button(
        label: String,
        color: Int = AppColors.accent,
        action: () -> Unit,
    ): Button {
        return Button(activity).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTextColor(AppColors.background)
            background = roundedBackground(color)
            minHeight = dp(52)
            setPadding(dp(12), dp(10), dp(12), dp(10))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(0, dp(6), 0, dp(6))
            }

            setOnClickListener {
                if (actionsAllowed()) {
                    action()
                }
            }
        }
    }

    /** Crée le panneau sombre et arrondi utilisé pour présenter un niveau sur l'accueil. */
    fun panel(): LinearLayout {
        return column().apply {
            background = roundedBackground(AppColors.surface)
            setPadding(dp(16), dp(12), dp(16), dp(12))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(0, dp(8), 0, dp(8))
            }
        }
    }

    /**
     * Installe une page défilable et son en-tête, puis renvoie la colonne de contenu.
     * Le défilement vertical permet de garder les champs accessibles avec le clavier ouvert.
     */
    fun screen(title: String, showHomeButton: Boolean = true): LinearLayout {
        val content = column().apply {
            setPadding(dp(20), dp(12), dp(20), dp(24))
            setBackgroundColor(AppColors.background)
        }

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            fitsSystemWindows = true
            addView(content)
        }
        activity.setContentView(scroll)

        // En-tête commun : bouton de retour à gauche, titre de l'écran à droite.
        val header = row()

        if (showHomeButton) {
            val homeButton = button("‹ Accueil", action = onHome).apply {
                setTextColor(AppColors.accent)
                background = roundedBackground(AppColors.surface)
                layoutParams = LinearLayout.LayoutParams(dp(115), dp(52))
                contentDescription = "Revenir à l'accueil"
            }
            header.addView(homeButton)
        }

        val titleView = text(
            value = title,
            size = if (showHomeButton) 21f else 30f,
            bold = true,
        ).apply {
            setPadding(if (showHomeButton) dp(16) else 0, dp(12), 0, dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(titleView)
        content.addView(header)

        return content
    }

    /** Observe la saisie d'un champ et transmet sa nouvelle valeur à l'écran appelant. */
    fun onTextChanged(input: EditText, action: (String) -> Unit) {
        input.addTextChangedListener(object : TextWatcher {
            /** Aucun traitement préalable : seule la nouvelle valeur complète est nécessaire. */
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            /** Transmet la valeur modifiée pour la sauvegarde de séance ou la recherche. */
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                action(s.toString())
            }

            /** Aucun traitement supplémentaire après la notification de changement. */
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /** Relance une action lorsque l'utilisateur change un filtre ou un tri déroulant. */
    fun onSelectionChanged(spinner: Spinner, action: () -> Unit) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            /** Signale le choix d'une option, y compris la sélection initiale d'Android. */
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                action()
            }

            /** L'absence de sélection ne modifie pas le tableau courant. */
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /** Affiche brièvement une confirmation ou une erreur sans remplacer la page actuelle. */
    fun message(value: String) {
        Toast.makeText(activity, value, Toast.LENGTH_LONG).show()
    }

    /** Formate une échéance en respectant la langue et le fuseau horaire du téléphone. */
    fun date(time: Long): String {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(time))
    }

    /** Ferme le clavier avant un changement de carte ou un retour à l'accueil. */
    fun hideKeyboard() {
        val focusedView = activity.currentFocus ?: return
        val keyboard = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        keyboard.hideSoftInputFromWindow(focusedView.windowToken, 0)
    }
}
