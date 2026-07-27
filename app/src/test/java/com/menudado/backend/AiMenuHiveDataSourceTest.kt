package com.menudado.backend

import com.menudado.data.SharedAiMenu
import com.menudado.domain.AiMenuHiveIdentity
import com.menudado.domain.AppLanguage
import com.menudado.domain.CuisineInspiration
import com.menudado.domain.DietaryProfile
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MenuAudience
import com.menudado.domain.MealType
import com.menudado.domain.ShoppingProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AiMenuHiveDataSourceTest {
    @Test
    fun `shared document excludes identity profile and local-only fields`() {
        val menu = sampleSharedAiMenu()

        val document = AiMenuHiveFirestoreMapper.toDocument(menu)

        assertEquals(1, document["schemaVersion"])
        assertEquals(menu.semanticHash, document["semanticHash"])
        assertEquals("SPANISH", document["language"])
        assertEquals(1, (document["eligibilityKeys"] as List<*>).size)
        val firstProduct = (document["shoppingProducts"] as List<*>).first() as Map<*, *>
        assertEquals("Pasta integral", firstProduct["displayName"])
        assertFalse(document.containsKey("uid"))
        assertFalse(document.containsKey("email"))
        assertFalse(document.containsKey("profile"))
        assertFalse(document.containsKey("baseIngredients"))
        assertFalse(document.containsKey("imageUri"))
        assertFalse(document.containsKey("mealType"))
        assertFalse(document.containsKey("audience"))
    }

    @Test
    fun `malformed or incomplete shared document is ignored`() {
        assertNull(
            AiMenuHiveFirestoreMapper.fromDocument(
                documentId = "bad",
                document = mapOf("name" to "Sin contrato")
            )
        )
    }

    private fun sampleSharedAiMenu(): SharedAiMenu {
        val profile = DietaryProfile(ageRange = "18+ años")
        val identity = requireNotNull(
            AiMenuHiveIdentity.from(AppLanguage.SPANISH, "pasta|tomato|sauce")
        )
        return SharedAiMenu(
            semanticHash = identity.semanticHash,
            semanticKey = identity.canonicalKey,
            language = AppLanguage.SPANISH,
            generatedMenu = GeneratedMenu(
                name = "Pasta con tomate",
                description = "Pasta integral con salsa de tomate.",
                notes = "Lista en 10 minutos.",
                calories = 430,
                healthAnalysis = HealthAnalysis(
                    status = HealthStatus.HEALTHY,
                    reason = "Incluye cereal y tomate.",
                    suggestion = "Acompaña con verduras.",
                    calories = 430
                ),
                shoppingProducts = listOf(
                    requireNotNull(ShoppingProduct.fromAi("Pasta integral")),
                    requireNotNull(ShoppingProduct.fromAi("Tomate"))
                ),
                deduplicationKey = identity.canonicalKey
            ),
            cuisineInspiration = CuisineInspiration.ITALIAN,
            eligibilityKeys = setOf(
                AiMenuHiveIdentity.eligibilityKey(
                    AppLanguage.SPANISH,
                    MealType.LUNCH,
                    MenuAudience.ADULT,
                    profile
                )
            )
        )
    }
}
