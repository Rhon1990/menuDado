package com.menudado.ui

import com.menudado.domain.AppLanguage
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiGenerationFailureMessageTest {
    @Test
    fun `daily limit includes meal audience and default baby age`() {
        val message = contextualAiGenerationFailureMessage(
            language = AppLanguage.SPANISH,
            reason = AiGenerationFailureReason.DAILY_LIMIT,
            mealType = MealType.LUNCH,
            audience = MenuAudience.BABY,
            profile = DietaryProfile(ageRange = "")
        )

        assertEquals(
            "Hoy no podemos preparar más ideas para Almuerzo · Bebé (6-24 meses). " +
                "Vuelve a intentarlo mañana.",
            message
        )
    }

    @Test
    fun `restricted child profile uses custom age without exposing restrictions`() {
        val message = contextualAiGenerationFailureMessage(
            language = AppLanguage.SPANISH,
            reason = AiGenerationFailureReason.HIGH_DEMAND,
            mealType = MealType.DINNER,
            audience = MenuAudience.CHILD,
            profile = DietaryProfile(
                ageRange = "4-8 años",
                isVegan = true,
                otherAvoidances = "texto privado"
            )
        )

        assertEquals(
            "La IA está con mucha demanda y no pudo preparar una idea para " +
                "Cena · Peques (4-8 años) con tu perfil actual. Inténtalo más tarde.",
            message
        )
        assertFalse(message.contains("veg", ignoreCase = true))
        assertFalse(message.contains("texto privado", ignoreCase = true))
    }

    @Test
    fun `every audience uses its localized label and effective age`() {
        val cases = listOf(
            MenuAudience.ADULT to "Persona adulta (18+ años)",
            MenuAudience.CHILD to "Peques (2-12 años)",
            MenuAudience.BABY to "Bebé (6-24 meses)"
        )

        cases.forEach { (audience, expectedContext) ->
            val message = contextualAiGenerationFailureMessage(
                language = AppLanguage.SPANISH,
                reason = AiGenerationFailureReason.CONNECTION,
                mealType = MealType.BREAKFAST,
                audience = audience,
                profile = DietaryProfile(ageRange = "")
            )

            assertTrue(message.contains("Desayuno · $expectedContext"))
        }
    }

    @Test
    fun `every failure reason gives the expected recovery action`() {
        val expectedActions = mapOf(
            AiGenerationFailureReason.DAILY_LIMIT to "mañana",
            AiGenerationFailureReason.HIGH_DEMAND to "más tarde",
            AiGenerationFailureReason.TIMEOUT to "Revisa tu conexión",
            AiGenerationFailureReason.CONNECTION to "Revisa tu conexión",
            AiGenerationFailureReason.SERVICE_UNAVAILABLE to "más tarde"
        )

        expectedActions.forEach { (reason, expectedAction) ->
            val message = contextualAiGenerationFailureMessage(
                language = AppLanguage.SPANISH,
                reason = reason,
                mealType = MealType.BREAKFAST,
                audience = MenuAudience.ADULT,
                profile = DietaryProfile(ageRange = "18+ años")
            )

            assertTrue(
                "$reason omitted $expectedAction",
                message.contains(expectedAction)
            )
        }
    }

    @Test
    fun `all localized messages hide internal fallback details`() {
        val forbidden = listOf(
            "colmena",
            "hive",
            "firestore",
            "shared",
            "compartid",
            "partagé",
            "fallback"
        )

        AppLanguage.entries.forEach { language ->
            AiGenerationFailureReason.entries.forEach { reason ->
                val message = contextualAiGenerationFailureMessage(
                    language = language,
                    reason = reason,
                    mealType = MealType.BREAKFAST,
                    audience = MenuAudience.ADULT,
                    profile = DietaryProfile(ageRange = "18+ años")
                )

                forbidden.forEach { term ->
                    assertFalse(
                        "$language/$reason leaked $term",
                        message.contains(term, ignoreCase = true)
                    )
                }
                assertTrue(message.isNotBlank())
            }
        }
    }
}
