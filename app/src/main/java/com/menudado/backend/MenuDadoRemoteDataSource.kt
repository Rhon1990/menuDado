package com.menudado.backend

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.menudado.BuildConfig
import com.menudado.analytics.AndroidDeviceInfoProvider
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

data class BackendAppMetadata(
    val country: String,
    val timeZone: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersionName: String,
    val appVersionCode: Int,
    val authMode: String,
    val accountEmail: String?
) {
    companion object {
        fun current(authSession: MenuDadoAuthSession?): BackendAppMetadata {
            return fromDeviceInfo(
                deviceInfo = AndroidDeviceInfoProvider.current(),
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                authSession = authSession
            )
        }

        fun fromDeviceInfo(
            deviceInfo: DeviceInfo,
            appVersionName: String,
            appVersionCode: Int,
            authSession: MenuDadoAuthSession? = null
        ): BackendAppMetadata {
            return BackendAppMetadata(
                country = deviceInfo.localeCountry,
                timeZone = deviceInfo.timeZone,
                deviceManufacturer = deviceInfo.manufacturer,
                deviceModel = deviceInfo.model,
                androidVersion = deviceInfo.androidVersion,
                appVersionName = appVersionName,
                appVersionCode = appVersionCode,
                authMode = when {
                    authSession == null -> "none"
                    authSession.isAnonymous -> "guest"
                    else -> "signed_in"
                },
                accountEmail = authSession?.email?.takeUnless { authSession.isAnonymous }
            )
        }
    }
}

interface MenuDadoRemoteDataSource {
    suspend fun upsertMetadata(metadata: BackendAppMetadata)
    suspend fun fetchMenus(): List<FoodMenu>
    suspend fun fetchDietaryProfiles(): Map<MenuAudience, DietaryProfile>
    suspend fun fetchOnboardingCompletedVersion(): Int?
    suspend fun upsertMenu(menu: FoodMenu)
    suspend fun deleteMenu(menu: FoodMenu)
    suspend fun upsertDietaryProfile(audience: MenuAudience, profile: DietaryProfile)
    suspend fun upsertAiUsage(state: AiDailyUsageState)
    suspend fun upsertOnboardingCompleted(contentVersion: Int)
}

class FirebaseMenuDadoRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val session: MenuDadoBackendSession = FirebaseMenuDadoBackendSession()
) : MenuDadoRemoteDataSource {
    override suspend fun upsertMetadata(metadata: BackendAppMetadata) {
        val userDocument = userDocument()
        val document = BackendFirestoreMapper.metadataDocument(metadata)
            .withServerTimestamp("lastSeenAt")

        userDocument
            .collection("metadata")
            .document("current")
            .set(document, SetOptions.merge())
            .awaitBackendTask()
    }

    override suspend fun fetchMenus(): List<FoodMenu> {
        val userDocument = userDocument()
        return userDocument
            .collection("menus")
            .get()
            .awaitBackendTask()
            .documents
            .mapNotNull { snapshot ->
                BackendFirestoreMapper.menuFromDocument(
                    documentId = snapshot.id,
                    document = snapshot.data.orEmpty()
                )
            }
    }

    override suspend fun fetchDietaryProfiles(): Map<MenuAudience, DietaryProfile> {
        val userDocument = userDocument()
        return userDocument
            .collection("dietaryProfiles")
            .get()
            .awaitBackendTask()
            .documents
            .mapNotNull { snapshot ->
                val audience = runCatching { MenuAudience.valueOf(snapshot.id) }.getOrNull()
                    ?: return@mapNotNull null
                audience to BackendFirestoreMapper.dietaryProfileFromDocument(
                    audience = audience,
                    document = snapshot.data.orEmpty()
                )
            }
            .toMap()
    }

    override suspend fun fetchOnboardingCompletedVersion(): Int? {
        val userDocument = userDocument()
        return userDocument
            .collection("onboarding")
            .document("current")
            .get()
            .awaitBackendTask()
            .data
            ?.let(BackendFirestoreMapper::onboardingCompletedVersionFromDocument)
    }

    override suspend fun upsertMenu(menu: FoodMenu) {
        val userDocument = userDocument()
        val document = BackendFirestoreMapper.menuDocument(menu)
            .withServerTimestamp("updatedAt")
            .plus("deletedAt" to null)

        userDocument
            .collection("menus")
            .document(menu.id.toString())
            .set(document, SetOptions.merge())
            .awaitBackendTask()
    }

    override suspend fun deleteMenu(menu: FoodMenu) {
        val userDocument = userDocument()
        val document = mapOf(
            "deletedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        userDocument
            .collection("menus")
            .document(menu.id.toString())
            .set(document, SetOptions.merge())
            .awaitBackendTask()
    }

    override suspend fun upsertDietaryProfile(audience: MenuAudience, profile: DietaryProfile) {
        val userDocument = userDocument()
        val document = BackendFirestoreMapper.dietaryProfileDocument(profile)
            .withServerTimestamp("updatedAt")

        userDocument
            .collection("dietaryProfiles")
            .document(audience.name)
            .set(document, SetOptions.merge())
            .awaitBackendTask()
    }

    override suspend fun upsertAiUsage(state: AiDailyUsageState) {
        val userDocument = userDocument()
        val document = BackendFirestoreMapper.aiUsageDocument(state)
            .withServerTimestamp("updatedAt")

        userDocument
            .collection("aiUsage")
            .document(state.dateKey)
            .set(document, SetOptions.merge())
            .awaitBackendTask()
    }

    override suspend fun upsertOnboardingCompleted(contentVersion: Int) {
        val userDocument = userDocument()
        val document = BackendFirestoreMapper.onboardingCompletedDocument(contentVersion)
            .withServerTimestamp("updatedAt")

        userDocument
            .collection("onboarding")
            .document("current")
            .set(document, SetOptions.merge())
            .awaitBackendTask()
    }

    private suspend fun userDocument() = firestore.collection("users").document(
        requireNotNull(session.userId()) { "MenuDado backend user is unavailable" }
    )
}

internal object BackendFirestoreMapper {
    fun metadataDocument(metadata: BackendAppMetadata): Map<String, Any?> {
        return mapOf(
            "country" to metadata.country,
            "timeZone" to metadata.timeZone,
            "deviceManufacturer" to metadata.deviceManufacturer,
            "deviceModel" to metadata.deviceModel,
            "androidVersion" to metadata.androidVersion,
            "appVersionName" to metadata.appVersionName,
            "appVersionCode" to metadata.appVersionCode,
            "authMode" to metadata.authMode,
            "accountEmail" to metadata.accountEmail
        )
    }

    fun menuDocument(menu: FoodMenu): Map<String, Any?> {
        val healthAnalysis = menu.healthAnalysis

        return mapOf(
            "name" to menu.name,
            "mealType" to menu.mealType.name,
            "audience" to menu.audience.name,
            "description" to menu.description,
            "notes" to menu.notes,
            "healthStatus" to healthAnalysis?.status?.name,
            "healthReason" to healthAnalysis?.reason,
            "healthSuggestion" to healthAnalysis?.suggestion,
            "calories" to (healthAnalysis?.calories ?: menu.calories),
            "imageUri" to menu.imageUri,
            "isFavorite" to menu.isFavorite,
            "favoritedAt" to menu.favoritedAt,
            "lastPickedDate" to menu.lastPickedDate,
            "createdAt" to menu.createdAt
        )
    }

    fun menuFromDocument(documentId: String, document: Map<String, Any?>): FoodMenu? {
        if (document["deletedAt"] != null) return null
        val id = documentId.toLongOrNull() ?: return null
        val name = document["name"] as? String ?: return null
        val mealType = (document["mealType"] as? String)
            ?.let { runCatching { MealType.valueOf(it) }.getOrNull() }
            ?: return null
        val audience = (document["audience"] as? String)
            ?.let { runCatching { MenuAudience.valueOf(it) }.getOrNull() }
            ?: MenuAudience.ADULT
        val calories = (document["calories"] as? Number)?.toInt()
        val healthAnalysis = (document["healthStatus"] as? String)
            ?.let { status ->
                runCatching {
                    HealthAnalysis(
                        status = HealthStatus.valueOf(status),
                        reason = (document["healthReason"] as? String).orEmpty(),
                        suggestion = (document["healthSuggestion"] as? String).orEmpty(),
                        calories = calories
                    )
                }.getOrNull()
            }

        return FoodMenu(
            id = id,
            name = name,
            mealType = mealType,
            audience = audience,
            description = (document["description"] as? String).orEmpty(),
            notes = (document["notes"] as? String).orEmpty(),
            healthAnalysis = healthAnalysis,
            calories = calories,
            imageUri = document["imageUri"] as? String,
            isFavorite = document["isFavorite"] as? Boolean ?: false,
            favoritedAt = (document["favoritedAt"] as? Number)?.toLong(),
            lastPickedDate = document["lastPickedDate"] as? String,
            createdAt = (document["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }

    fun dietaryProfileDocument(profile: DietaryProfile): Map<String, Any?> {
        return mapOf(
            "isEnabled" to profile.isEnabled,
            "ageRange" to profile.ageRange,
            "isPregnant" to profile.isPregnant,
            "isVegan" to profile.isVegan,
            "hasAllergies" to profile.hasAllergies,
            "allergens" to profile.allergens.map { it.name }.sorted(),
            "otherAvoidances" to profile.otherAvoidances
        )
    }

    fun dietaryProfileFromDocument(audience: MenuAudience, document: Map<String, Any?>): DietaryProfile {
        val allergens = (document["allergens"] as? List<*>)
            .orEmpty()
            .mapNotNull { value ->
                (value as? String)?.let {
                    runCatching { DietaryAllergen.valueOf(it) }.getOrNull()
                }
            }
            .toSet()

        return DietaryProfile(
            isEnabled = document["isEnabled"] as? Boolean ?: (audience == MenuAudience.ADULT),
            ageRange = (document["ageRange"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: audience.defaultAgeRange,
            isPregnant = document["isPregnant"] as? Boolean ?: false,
            isVegan = document["isVegan"] as? Boolean ?: false,
            hasAllergies = document["hasAllergies"] as? Boolean ?: false,
            allergens = allergens,
            otherAvoidances = (document["otherAvoidances"] as? String).orEmpty()
        )
    }

    fun aiUsageDocument(state: AiDailyUsageState): Map<String, Any?> {
        return mapOf(
            "dateKey" to state.dateKey,
            "usedCount" to state.usedCount
        )
    }

    fun onboardingCompletedDocument(contentVersion: Int): Map<String, Any?> {
        return mapOf(
            "completed" to true,
            "contentVersion" to contentVersion
        )
    }

    fun onboardingCompletedVersionFromDocument(document: Map<String, Any?>): Int? {
        val isCompleted = document["completed"] as? Boolean ?: false
        return if (isCompleted) {
            (document["contentVersion"] as? Number)?.toInt()
        } else {
            null
        }
    }
}

private fun Map<String, Any?>.withServerTimestamp(key: String): Map<String, Any?> {
    return this + (key to FieldValue.serverTimestamp())
}
