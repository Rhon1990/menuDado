package com.menudado.ai

import com.menudado.domain.DietaryProfile
import com.menudado.domain.MealType

internal object MenuGenerationPrompt {
    fun build(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile = DietaryProfile(),
        baseIngredients: String = ""
    ): String {
        val guidance = mealType.generationGuidance()
        val avoidBlock = if (avoidIdeas.isEmpty()) {
            "No hay platos previos que evitar."
        } else {
            avoidIdeas.joinToString(separator = "\n") { "- $it" }
        }
        val dietaryProfileBlock = dietaryProfile.toPromptBlock()
        val baseIngredientsBlock = baseIngredients.toBaseIngredientsPromptBlock()

        return """
            ${guidance.opening}
            Tipo seleccionado en la app: ${mealType.label}.
            ${guidance.fitRule}
            ${guidance.exclusionRule}
            ${guidance.effortRule.orEmpty()}
            $dietaryProfileBlock
            $baseIngredientsBlock
            Debe ser saludable, rica, simple y con ingredientes comunes de supermercado.
            Evita productos raros, caros o dificiles de conseguir.
            Debe ser claramente distinta de los platos previos listados abajo:
            $avoidBlock
            No repitas el mismo plato, ni una variante demasiado parecida.
            Cambia la base principal, la proteina principal, la preparacion y el estilo cuando sea posible.
            Responde solo JSON valido, sin markdown, con estas claves:
            {
              "name": "nombre breve del plato en espanol",
              "description": "ingredientes y preparacion breve en espanol",
              "notes": "nota practica opcional en espanol",
              "calories": 520,
              "health_status": "saludable|intermedio|no_saludable",
              "health_reason": "resumen breve de por que tiene ese estado",
              "health_suggestion": "una sugerencia practica para mejorarlo o mantenerlo"
            }
            Las calorias deben ser una estimacion numerica realista para una racion adulta.
        """.trimIndent()
    }

    private fun String.toBaseIngredientsPromptBlock(): String {
        val ingredients = trim()
        if (ingredients.isBlank()) {
            return "No hay ingredientes base solicitados por el usuario."
        }

        return """
            Ingredientes base solicitados por el usuario: $ingredients.
            Debe usar esos ingredientes como base principal, sin anadir ingredientes que contradigan el perfil alimentario.
        """.trimIndent()
    }

    private fun DietaryProfile.toPromptBlock(): String {
        if (!hasRestrictions) {
            return "No hay perfil alimentario configurado."
        }

        val rules = mutableListOf("Perfil alimentario del usuario:")
        if (isVegan) {
            rules += "La receta debe ser vegana y sin ingredientes de origen animal."
        }
        if (hasAllergies && allergens.isNotEmpty()) {
            val allergenNames = allergens.joinToString { it.promptName }
            rules += "No incluyas estos alergenos ni ingredientes que los contengan: $allergenNames."
        }
        if (otherAvoidances.isNotBlank()) {
            rules += "No incluyas tambien estos alimentos indicados por el usuario: ${otherAvoidances.trim()}."
        }
        rules += "Respeta estas restricciones por seguridad alimentaria."
        return rules.joinToString(separator = "\n")
    }

    private fun MealType.generationGuidance(): GenerationGuidance {
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
}

private data class GenerationGuidance(
    val opening: String,
    val fitRule: String,
    val exclusionRule: String,
    val effortRule: String? = null
)
