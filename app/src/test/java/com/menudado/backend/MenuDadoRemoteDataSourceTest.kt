package com.menudado.backend

import com.menudado.analytics.DeviceInfo
import com.menudado.data.AiDailyUsageState
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MenuDadoRemoteDataSourceTest {
    @Test
    fun `metadata document contains app and device fields without precise identifiers`() {
        val metadata = BackendAppMetadata.fromDeviceInfo(
            deviceInfo = DeviceInfo(
                manufacturer = "Google",
                model = "Pixel 8",
                androidVersion = "15",
                localeCountry = "ES",
                timeZone = "Europe/Madrid"
            ),
            appVersionName = "1.2.3",
            appVersionCode = 7
        )

        val document = BackendFirestoreMapper.metadataDocument(metadata)

        assertEquals("ES", document["country"])
        assertEquals("Europe/Madrid", document["timeZone"])
        assertEquals("Google", document["deviceManufacturer"])
        assertEquals("Pixel 8", document["deviceModel"])
        assertEquals("15", document["androidVersion"])
        assertEquals("1.2.3", document["appVersionName"])
        assertEquals(7, document["appVersionCode"])
        assertEquals("anonymous", document["authMode"])
        assertFalse(document.containsKey("latitude"))
        assertFalse(document.containsKey("longitude"))
        assertFalse(document.containsKey("gps"))
        assertFalse(document.containsKey("adId"))
    }

    @Test
    fun `menu document serializes enums health analysis and timestamps`() {
        val menu = FoodMenu(
            id = 42L,
            name = "Cena ligera",
            mealType = MealType.DINNER,
            audience = MenuAudience.CHILD,
            description = "Tortilla francesa con tomate.",
            notes = "Sin picante.",
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Buena proteina y verduras.",
                suggestion = "Anade fruta si queda hambre.",
                calories = 360
            ),
            calories = 400,
            imageUri = "content://menu/42",
            lastPickedDate = "2026-06-22",
            createdAt = 1_719_000_000_000L
        )

        val document = BackendFirestoreMapper.menuDocument(menu)

        assertEquals("Cena ligera", document["name"])
        assertEquals("DINNER", document["mealType"])
        assertEquals("CHILD", document["audience"])
        assertEquals("Tortilla francesa con tomate.", document["description"])
        assertEquals("Sin picante.", document["notes"])
        assertEquals("HEALTHY", document["healthStatus"])
        assertEquals("Buena proteina y verduras.", document["healthReason"])
        assertEquals("Anade fruta si queda hambre.", document["healthSuggestion"])
        assertEquals(360, document["calories"])
        assertEquals("content://menu/42", document["imageUri"])
        assertEquals("2026-06-22", document["lastPickedDate"])
        assertEquals(1_719_000_000_000L, document["createdAt"])
    }

    @Test
    fun `dietary profile document serializes audience profile and allergens by enum name`() {
        val profile = DietaryProfile(
            isEnabled = true,
            ageRange = "2-12 anos",
            isPregnant = false,
            isVegan = true,
            hasAllergies = true,
            allergens = setOf(DietaryAllergen.EGG, DietaryAllergen.GLUTEN),
            otherAvoidances = "sin picante"
        )

        val document = BackendFirestoreMapper.dietaryProfileDocument(profile)

        assertEquals(true, document["isEnabled"])
        assertEquals("2-12 anos", document["ageRange"])
        assertEquals(false, document["isPregnant"])
        assertEquals(true, document["isVegan"])
        assertEquals(true, document["hasAllergies"])
        assertEquals(listOf("EGG", "GLUTEN"), document["allergens"])
        assertEquals("sin picante", document["otherAvoidances"])
    }

    @Test
    fun `ai usage and onboarding documents use stable keys`() {
        assertEquals(
            mapOf("dateKey" to "2026-06-22", "usedCount" to 3),
            BackendFirestoreMapper.aiUsageDocument(AiDailyUsageState(dateKey = "2026-06-22", usedCount = 3))
        )
        assertEquals(
            mapOf("completed" to true, "contentVersion" to 2),
            BackendFirestoreMapper.onboardingCompletedDocument(contentVersion = 2)
        )
    }
}
