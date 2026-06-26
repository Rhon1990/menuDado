package com.menudado.ai

import com.menudado.domain.DietaryProfile
import com.menudado.domain.MenuAudience
import com.menudado.domain.MealType
import com.menudado.domain.AppLanguage
import com.menudado.domain.localizedLabel

internal object MenuGenerationPrompt {
    fun build(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile = DietaryProfile(),
        audience: MenuAudience = MenuAudience.ADULT,
        baseIngredients: String = "",
        language: AppLanguage = AppLanguage.SPANISH
    ): String {
        val guidance = mealType.generationGuidance(language)
        val audienceGuidance = audience.generationGuidance(dietaryProfile.ageRange, language)
        val avoidBlock = if (avoidIdeas.isEmpty()) {
            "No hay platos previos que evitar."
        } else {
            avoidIdeas.joinToString(separator = "\n") { "- $it" }
        }
        val dietaryProfileBlock = dietaryProfile.toPromptBlock()
        val baseIngredientsBlock = baseIngredients.toBaseIngredientsPromptBlock()

        return """
            ${guidance.opening}
            Tipo seleccionado en la app: ${mealType.localizedLabel(language)}.
            Publico seleccionado en la app: ${audience.localizedLabel(language)}.
            ${guidance.fitRule}
            ${guidance.exclusionRule}
            ${guidance.effortRule.orEmpty()}
            ${audienceGuidance}
            $dietaryProfileBlock
            $baseIngredientsBlock
            Debe ser saludable, rica, simple y con ingredientes comunes de supermercado.
            Evita productos raros, caros o dificiles de conseguir.
            Variedad sugerida: puedes proponer cremas o sopas ligeras, ensaladas completas como ensalada Cesar saludable, bowls equilibrados, salteados simples, tortillas, legumbres, wraps, tostas, pasta integral o arroz integral.
            Debe ser claramente distinta de los platos previos listados abajo:
            $avoidBlock
            No repitas el mismo plato, ni una variante demasiado parecida.
            Cambia la base principal, la proteina principal, la preparacion y el estilo cuando sea posible.
            La evaluacion saludable debe ser breve, practica y sin tono de juicio.
            Write name, description, notes, reason and suggestion in ${language.promptLanguageName}.
            Responde solo JSON valido, sin markdown, con estas claves:
            {
              "name": "nombre breve del plato",
              "description": "ingredientes y preparacion breve",
              "notes": "nota practica opcional",
              "calories": 520,
              "health_status": "saludable|intermedio|no_saludable",
              "health_reason": "resumen breve de por que tiene ese estado",
              "health_suggestion": "una sugerencia practica para mejorarlo o mantenerlo"
            }
            Las calorias deben ser una estimacion numerica realista para una racion adecuada al publico seleccionado.
        """.trimIndent()
    }

    private fun String.toBaseIngredientsPromptBlock(): String {
        val ingredients = trim()
        if (ingredients.isBlank()) {
            return "No hay ingredientes base solicitados por el usuario."
        }

        return """
            Ingredientes base solicitados por el usuario: $ingredients.
            Debe incluir esos ingredientes de forma natural en la receta, sin anadir ingredientes que contradigan el perfil alimentario.
        """.trimIndent()
    }

    private fun DietaryProfile.toPromptBlock(): String {
        if (!hasRestrictions) {
            return "No hay perfil alimentario configurado."
        }

        val rules = mutableListOf("Perfil alimentario del usuario:")
        if (isPregnant) {
            rules += "La receta debe ser adecuada para una persona embarazada, cuidando especialmente la seguridad alimentaria: evita alcohol, ingredientes crudos o poco cocinados, lacteos no pasteurizados y pescados de alto mercurio."
        }
        if (isVegan) {
            rules += "La receta debe ser vegana y sin ingredientes de origen animal."
        }
        if (hasAllergies && allergens.isNotEmpty()) {
            val allergenNames = allergens.joinToString { it.promptName }
            rules += "No incluyas estos alergenos ni ingredientes que los contengan: $allergenNames."
        }
        if (otherAvoidances.isNotBlank()) {
            rules += "Ten en cuenta estos alimentos a evitar o condiciones de salud indicadas por el usuario: ${otherAvoidances.trim()}. Si son condiciones de salud como diabetes o hipertension, ajusta el menu de forma prudente y sin dar consejos medicos."
        }
        rules += "Respeta estas restricciones por seguridad alimentaria."
        return rules.joinToString(separator = "\n")
    }

    private fun MealType.generationGuidance(language: AppLanguage): GenerationGuidance {
        if (language != AppLanguage.SPANISH) {
            return when (this) {
                MealType.BREAKFAST -> GenerationGuidance(
                    opening = "Generate a healthy ${localizedLabel(language).lowercase()} idea.",
                    fitRule = "The recipe must clearly fit as ${localizedLabel(language).lowercase()}.",
                    exclusionRule = "Do not generate recipes for other meal types."
                )
                MealType.LUNCH -> GenerationGuidance(
                    opening = "Generate a healthy ${localizedLabel(language).lowercase()} idea.",
                    fitRule = "The recipe must clearly fit as ${localizedLabel(language).lowercase()}.",
                    exclusionRule = "Do not generate breakfast or dinner ideas."
                )
                MealType.DINNER -> GenerationGuidance(
                    opening = "Generate a healthy ${localizedLabel(language).lowercase()} idea.",
                    fitRule = "The recipe must clearly fit as a light, quick, low-energy night dinner.",
                    exclusionRule = "Do not generate lunch or midday meal ideas.",
                    effortRule = "For dinners use at most 10 minutes, at most 5 main ingredients and a preparation as simple as breakfast. Avoid oven dishes, roasts, multiple sides and multi-step preparations."
                )
            }
        }
        return when (this) {
            MealType.BREAKFAST -> GenerationGuidance(
                opening = "Genera un desayuno saludable.",
                fitRule = "La receta debe encajar claramente como desayuno cotidiano.",
                exclusionRule = "No generes almuerzos ni cenas."
            )
            MealType.LUNCH -> GenerationGuidance(
                opening = "Genera un almuerzo saludable.",
                fitRule = "La receta debe encajar claramente como almuerzo o comida de mediodia.",
                exclusionRule = "No generes desayunos ni cenas."
            )
            MealType.DINNER -> GenerationGuidance(
                opening = "Genera una cena saludable.",
                fitRule = "La receta debe encajar claramente como cena de noche, ligera, rapida y de baja energia para alguien cansado.",
                exclusionRule = "No generes almuerzos, comidas de mediodia ni platos pensados como menu de almuerzo; no uses la palabra almuerzo en la receta.",
                effortRule = "Para cenas usa maximo 10 minutos, maximo 5 ingredientes principales y una preparacion similar de sencilla que un desayuno. Evita horno, asados, guarniciones multiples y preparaciones con varios pasos."
            )
        }
    }

    private fun MenuAudience.generationGuidance(ageRange: String, language: AppLanguage): String {
        val range = ageRange.trim().ifBlank { defaultAgeRange }
        if (language != AppLanguage.SPANISH) {
            return when (this) {
                MenuAudience.ADULT -> "The recipe must be designed for an adult in this age range: $range."
                MenuAudience.CHILD -> "The recipe must be designed for kids in this age range: $range. Use moderate portions, familiar flavors, low salt and avoid obvious choking risks."
                MenuAudience.BABY -> "The recipe must be designed for a baby in this age range: $range, already eating complementary foods. It must be soft or mashable, with no honey, no added salt, no added sugar, no hard or round pieces, no whole nuts, popcorn, whole grapes or other choking risks. If the dish is not appropriate for a baby, adapt it to a safe version."
            }
        }
        return when (this) {
            MenuAudience.ADULT -> "La receta debe estar pensada para una persona adulta en este rango de edad: $range."
            MenuAudience.CHILD -> "La receta debe estar pensada para un niño o niña en este rango de edad: $range. Usa porcion moderada, sabor familiar, baja en sal y sin riesgos obvios de atragantamiento."
            MenuAudience.BABY -> "La receta debe estar pensada para un bebe en este rango de edad: $range, que ya toma alimentacion complementaria. Debe tener textura blanda o triturable, sin miel, sin sal anadida, sin azucar anadida, sin piezas duras o redondas, sin frutos secos enteros, palomitas, uvas enteras ni otros riesgos de atragantamiento. Si el plato no es apropiado para bebe, adapta la idea a una version segura."
        }
    }
}

private data class GenerationGuidance(
    val opening: String,
    val fitRule: String,
    val exclusionRule: String,
    val effortRule: String? = null
)
