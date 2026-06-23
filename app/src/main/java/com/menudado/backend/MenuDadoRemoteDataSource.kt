package com.menudado.backend

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.menudado.BuildConfig
import com.menudado.analytics.AndroidDeviceInfoProvider
import com.menudado.analytics.DeviceInfo
import com.menudado.data.AiDailyUsageState
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.MenuAudience

data class BackendAppMetadata(
    val country: String,
    val timeZone: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersionName: String,
    val appVersionCode: Int
) {
    companion object {
        fun current(): BackendAppMetadata {
            return fromDeviceInfo(
                deviceInfo = AndroidDeviceInfoProvider.current(),
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE
            )
        }

        fun fromDeviceInfo(
            deviceInfo: DeviceInfo,
            appVersionName: String,
            appVersionCode: Int
        ): BackendAppMetadata {
            return BackendAppMetadata(
                country = deviceInfo.localeCountry,
                timeZone = deviceInfo.timeZone,
                deviceManufacturer = deviceInfo.manufacturer,
                deviceModel = deviceInfo.model,
                androidVersion = deviceInfo.androidVersion,
                appVersionName = appVersionName,
                appVersionCode = appVersionCode
            )
        }
    }
}

interface MenuDadoRemoteDataSource {
    suspend fun upsertMetadata(metadata: BackendAppMetadata)
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
            "authMode" to "anonymous"
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
            "lastPickedDate" to menu.lastPickedDate,
            "createdAt" to menu.createdAt
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
}

private fun Map<String, Any?>.withServerTimestamp(key: String): Map<String, Any?> {
    return this + (key to FieldValue.serverTimestamp())
}
