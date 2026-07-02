package com.menudado.backend

import com.menudado.analytics.DeviceInfo
import com.menudado.auth.MenuDadoAuthSession
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
        assertEquals("none", document["authMode"])
        assertEquals(null, document["accountEmail"])
        assertFalse(document.containsKey("latitude"))
        assertFalse(document.containsKey("longitude"))
        assertFalse(document.containsKey("gps"))
        assertFalse(document.containsKey("adId"))
    }

    @Test
    fun `metadata document records signed in account mode and email`() {
        val metadata = BackendAppMetadata.fromDeviceInfo(
            deviceInfo = DeviceInfo(
                manufacturer = "Google",
                model = "Pixel 8",
                androidVersion = "15",
                localeCountry = "ES",
                timeZone = "Europe/Madrid"
            ),
            appVersionName = "1.2.3",
            appVersionCode = 7,
            authSession = MenuDadoAuthSession(
                userId = "registered-user",
                email = "user@example.com",
                isAnonymous = false
            )
        )

        val document = BackendFirestoreMapper.metadataDocument(metadata)

        assertEquals("signed_in", document["authMode"])
        assertEquals("user@example.com", document["accountEmail"])
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
            isFavorite = true,
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
        assertEquals(true, document["isFavorite"])
        assertEquals("2026-06-22", document["lastPickedDate"])
        assertEquals(1_719_000_000_000L, document["createdAt"])
    }

    @Test
    fun `menu document deserializes remote menu and skips deleted documents`() {
        val document = mapOf(
            "name" to "Cena ligera",
            "mealType" to "DINNER",
            "audience" to "CHILD",
            "description" to "Tortilla francesa con tomate.",
            "notes" to "Sin picante.",
            "healthStatus" to "HEALTHY",
            "healthReason" to "Buena proteina y verduras.",
            "healthSuggestion" to "Anade fruta si queda hambre.",
            "calories" to 360L,
            "imageUri" to "content://menu/42",
            "isFavorite" to true,
            "lastPickedDate" to "2026-06-22",
            "createdAt" to 1_719_000_000_000L
        )

        val menu = BackendFirestoreMapper.menuFromDocument("42", document)

        assertEquals(42L, menu?.id)
        assertEquals("Cena ligera", menu?.name)
        assertEquals(MealType.DINNER, menu?.mealType)
        assertEquals(MenuAudience.CHILD, menu?.audience)
        assertEquals(HealthStatus.HEALTHY, menu?.healthAnalysis?.status)
        assertEquals(360, menu?.calories)
        assertEquals(true, menu?.isFavorite)
        assertEquals(
            null,
            BackendFirestoreMapper.menuFromDocument("42", document + ("deletedAt" to Any()))
        )
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
    fun `dietary profile document deserializes remote profile and skips invalid allergens`() {
        val document = mapOf(
            "isEnabled" to true,
            "ageRange" to "2-12 anos",
            "isPregnant" to false,
            "isVegan" to true,
            "hasAllergies" to true,
            "allergens" to listOf("EGG", "UNKNOWN"),
            "otherAvoidances" to "sin picante"
        )

        val profile = BackendFirestoreMapper.dietaryProfileFromDocument(MenuAudience.CHILD, document)

        assertEquals(true, profile.isEnabled)
        assertEquals("2-12 anos", profile.ageRange)
        assertEquals(false, profile.isPregnant)
        assertEquals(true, profile.isVegan)
        assertEquals(true, profile.hasAllergies)
        assertEquals(setOf(DietaryAllergen.EGG), profile.allergens)
        assertEquals("sin picante", profile.otherAvoidances)
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
        assertEquals(
            2,
            BackendFirestoreMapper.onboardingCompletedVersionFromDocument(
                mapOf("completed" to true, "contentVersion" to 2L)
            )
        )
    }
}
