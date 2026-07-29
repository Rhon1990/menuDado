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
            "Has alcanzado el límite diario de ideas para Almuerzo · Bebé (6-24 meses). " +
                "Podrás volver a intentarlo mañana.",
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
            "Queremos proponerte algo que encaje de verdad contigo, pero ahora mismo " +
                "la IA necesita un pequeño respiro para Cena · Peques (4-8 años) " +
                "con tu perfil actual.",
            message
        )
        assertFalse(message.contains("veg", ignoreCase = true))
        assertFalse(message.contains("texto privado", ignoreCase = true))
    }

    @Test
    fun `high demand copy is human and localized without exposing restrictions`() {
        val profile = DietaryProfile(
            ageRange = MenuAudience.ADULT.defaultAgeRange,
            isVegan = true,
            otherAvoidances = "texto privado"
        )
        val expected = mapOf(
            AppLanguage.SPANISH to
                "Queremos proponerte algo que encaje de verdad contigo, pero ahora mismo " +
                "la IA necesita un pequeño respiro para Almuerzo · Persona adulta " +
                "(18+ años) con tu perfil actual.",
            AppLanguage.ENGLISH to
                "We want to suggest something that truly fits you, but AI needs a short " +
                "break before preparing an idea for Lunch · Adult (18+ years) with " +
                "your current profile.",
            AppLanguage.FRENCH to
                "Nous voulons vous proposer quelque chose qui vous corresponde vraiment, " +
                "mais l’IA a besoin d’une courte pause avant de préparer une idée pour " +
                "Déjeuner · Adulte (18 ans et plus) avec votre profil actuel."
        )

        expected.forEach { (language, copy) ->
            val message = contextualAiGenerationFailureMessage(
                language = language,
                reason = AiGenerationFailureReason.HIGH_DEMAND,
                mealType = MealType.LUNCH,
                audience = MenuAudience.ADULT,
                profile = profile
            )

            assertEquals(copy, message)
            assertFalse(message.contains("veg", ignoreCase = true))
            assertFalse(message.contains("texto privado", ignoreCase = true))
        }
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
            AiGenerationFailureReason.HIGH_DEMAND to "pequeño respiro",
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
    fun `daily limit is explicit in every supported language`() {
        val expectedLimitText = mapOf(
            AppLanguage.SPANISH to "límite diario",
            AppLanguage.ENGLISH to "daily idea limit",
            AppLanguage.FRENCH to "limite quotidienne"
        )

        expectedLimitText.forEach { (language, expectedText) ->
            val message = contextualAiGenerationFailureMessage(
                language = language,
                reason = AiGenerationFailureReason.DAILY_LIMIT,
                mealType = MealType.LUNCH,
                audience = MenuAudience.BABY,
                profile = DietaryProfile(ageRange = "")
            )

            assertTrue(message.contains(expectedText, ignoreCase = true))
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
