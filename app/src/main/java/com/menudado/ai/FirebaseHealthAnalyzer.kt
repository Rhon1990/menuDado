package com.menudado.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.menudado.BuildConfig
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.GeneratedMenuParser
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthAnalysisParser
import com.menudado.domain.MealType
import com.menudado.domain.isAiQuotaExceeded

class FirebaseHealthAnalyzer : HealthAnalyzer {
    override suspend fun analyze(menu: FoodMenu): Result<HealthAnalysis> {
        return runCatching {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(BuildConfig.GEMINI_MODEL)

            val response = model.generateContent(menu.toPrompt())
            HealthAnalysisParser.parse(response.text.orEmpty())
        }.onFailure { error ->
            logAiFailure("Health analysis failed", error)
        }
    }

    override suspend fun analyzeBatch(menus: List<FoodMenu>): Result<Map<Long, HealthAnalysis>> {
        return runCatching {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(BuildConfig.GEMINI_MODEL)

            val response = model.generateContent(menus.toBatchPrompt())
            HealthAnalysisParser.parseBatch(response.text.orEmpty())
        }.onFailure { error ->
            logAiFailure("Batch health analysis failed", error)
        }
    }

    override suspend fun generateMenu(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile,
        baseIngredients: String
    ): Result<GeneratedMenu> {
        return runCatching {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(BuildConfig.GEMINI_MODEL)

            val response = model.generateContent(
                MenuGenerationPrompt.build(mealType, avoidIdeas, dietaryProfile, baseIngredients)
            )
            GeneratedMenuParser.parse(response.text.orEmpty())
        }.onFailure { error ->
            logAiFailure("Menu generation failed", error)
        }
    }

    private fun logAiFailure(operation: String, error: Throwable) {
        if (error.isAiQuotaExceeded()) {
            val retryHint = Regex("""please retry in [0-9]+(?:\.[0-9]+)?s""", RegexOption.IGNORE_CASE)
                .find(listOfNotNull(error.message, error.cause?.message).joinToString(" "))
                ?.value
            Log.w(TAG, listOfNotNull("$operation: AI quota exhausted.", retryHint).joinToString(" "))
        } else {
            Log.e(TAG, operation, error)
        }
    }

    private fun FoodMenu.toPrompt(): String {
        return """
            Evalua si este menu es saludable para una persona adulta promedio.
            Responde solo JSON valido, sin markdown, con estas claves:
            {
              "status": "saludable|intermedio|no_saludable",
              "reason": "resumen breve en espanol",
              "suggestion": "una sugerencia practica en espanol",
              "calories": 520
            }
            Las calorias deben ser una estimacion numerica realista para una racion adulta.

            Tipo de comida: ${mealType.label}
            Nombre: $name
            Ingredientes o descripcion: $description
            Notas: $notes
        """.trimIndent()
    }

    private fun List<FoodMenu>.toBatchPrompt(): String {
        val menusJson = joinToString(separator = ",\n") { menu ->
            """
            {
              "id": ${menu.id},
              "meal_type": "${menu.mealType.label}",
              "name": "${menu.name.promptSafe()}",
              "description": "${menu.description.promptSafe()}",
              "notes": "${menu.notes.promptSafe()}"
            }
            """.trimIndent()
        }

        return """
            Evalua si estos menus son saludables para una persona adulta promedio.
            Responde solo JSON valido, sin markdown, con esta estructura:
            {
              "results": [
                {
                  "id": 1,
                  "status": "saludable|intermedio|no_saludable",
                  "reason": "resumen breve en espanol",
                  "suggestion": "una sugerencia practica en espanol",
                  "calories": 520
                }
              ]
            }
            Devuelve un resultado por cada menu recibido y conserva exactamente el mismo id.
            Las calorias deben ser una estimacion numerica realista para una racion adulta.

            Menus:
            [
            $menusJson
            ]
        """.trimIndent()
    }

    private fun String.promptSafe(): String {
        return replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private companion object {
        const val TAG = "FirebaseHealthAnalyzer"
    }
}
