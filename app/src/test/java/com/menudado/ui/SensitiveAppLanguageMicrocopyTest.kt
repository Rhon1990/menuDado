package com.menudado.ui

import com.menudado.domain.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SensitiveAppLanguageMicrocopyTest {
    @Test
    fun `timeout messages do not attribute the delay to the connection`() {
        val expected = mapOf(
            AppLanguage.SPANISH to "La IA tardó más de lo esperado. Inténtalo de nuevo.",
            AppLanguage.ENGLISH to "AI took longer than expected. Try again.",
            AppLanguage.FRENCH to "L’IA a mis plus de temps que prévu. Réessayez."
        )

        expected.forEach { (language, expectedMessage) ->
            val message = language.aiTimeoutMessage()

            assertFalse(message.containsConnectionCause())
            assertEquals(expectedMessage, message)
        }
    }

    @Test
    fun `generic failure messages avoid an unverified connection cause`() {
        val expected = mapOf(
            AppLanguage.SPANISH to "La IA no pudo completar la solicitud. Inténtalo de nuevo más tarde.",
            AppLanguage.ENGLISH to "AI could not complete the request. Try again later.",
            AppLanguage.FRENCH to "L’IA n’a pas pu terminer la demande. Réessayez plus tard."
        )

        expected.forEach { (language, expectedMessage) ->
            val message = language.aiGenericFailureMessage()

            assertFalse(message.containsConnectionCause())
            assertEquals(expectedMessage, message)
        }
    }

    @Test
    fun `daily limit messages explain the free quota without promising tomorrow`() {
        val expected = mapOf(
            AppLanguage.SPANISH to "Ya agotaste los usos gratuitos de IA de hoy. Tus menús siguen disponibles; vuelve a intentarlo más tarde.",
            AppLanguage.ENGLISH to "You have used all of today's free AI uses. Your menus are still available; try again later.",
            AppLanguage.FRENCH to "Vous avez épuisé les usages gratuits de l’IA pour aujourd’hui. Vos menus restent disponibles ; réessayez plus tard."
        )

        expected.forEach { (language, expectedMessage) ->
            val message = language.aiLocalDailyLimitMessage()

            assertFalse(message.containsNextDayPromise())
            assertEquals(expectedMessage, message)
        }
    }

    @Test
    fun `manual edit messages use the localized review with AI action`() {
        val expected = mapOf(
            AppLanguage.SPANISH to "Cambiaste la receta generada. Para comprobar de nuevo si es saludable, guarda el menú y toca Revisar con IA.",
            AppLanguage.ENGLISH to "You changed the generated recipe. To check whether it is still healthy, save the menu and tap Review with AI.",
            AppLanguage.FRENCH to "Vous avez modifié la recette générée. Pour vérifier à nouveau si elle est équilibrée, enregistrez le menu et touchez Vérifier avec l’IA."
        )

        expected.forEach { (language, expectedMessage) ->
            assertEquals(
                expectedMessage,
                language.generatedAnalysisManualEditMessage()
            )
        }
    }

    private fun String.containsConnectionCause(): Boolean {
        val normalized = lowercase()
        return listOf("conexión", "connection", "connexion").any(normalized::contains)
    }

    private fun String.containsNextDayPromise(): Boolean {
        val normalized = lowercase()
        return listOf("mañana", "tomorrow", "demain").any(normalized::contains)
    }
}
