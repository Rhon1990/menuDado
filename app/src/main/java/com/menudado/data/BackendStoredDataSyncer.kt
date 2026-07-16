package com.menudado.data

import com.menudado.backend.MenuDadoRemoteDataSource

class BackendStoredDataSyncer(
    private val dietaryProfileStore: DietaryProfileStore,
    private val aiDailyUsageStore: AiDailyUsageStore,
    private val onboardingStore: OnboardingStore,
    private val pendingSyncStore: BackendPendingSyncStore,
    private val remoteDataSource: MenuDadoRemoteDataSource,
    private val onDietaryProfilesHydrated: () -> Unit = {}
) {
    suspend fun syncPending() {
        pendingSyncStore.getPendingDietaryProfileAudiences().forEach { audience ->
            val profile = dietaryProfileStore.getProfile(audience)
            runCatching { remoteDataSource.upsertDietaryProfile(audience, profile) }
                .onSuccess { pendingSyncStore.clearDietaryProfilePending(audience) }
        }

        if (pendingSyncStore.isAiUsagePending()) {
            aiDailyUsageStore.getUsageState()?.let { state ->
                runCatching { remoteDataSource.upsertAiUsage(state) }
                    .onSuccess { pendingSyncStore.clearAiUsagePending() }
            }
        }

        pendingSyncStore.getPendingOnboardingVersion()?.let { version ->
            runCatching { remoteDataSource.upsertOnboardingCompleted(version) }
                .onSuccess { pendingSyncStore.clearOnboardingPending() }
        }
    }

    suspend fun hydrateRemoteStoredData() {
        val pendingProfileAudiences = pendingSyncStore.getPendingDietaryProfileAudiences()
        val hydratedProfiles = runCatching { remoteDataSource.fetchDietaryProfiles() }
            .getOrDefault(emptyMap())
            .filterKeys { audience -> audience !in pendingProfileAudiences }
        hydratedProfiles.forEach { (audience, profile) ->
            dietaryProfileStore.saveProfile(profile, audience)
        }
        if (hydratedProfiles.isNotEmpty()) {
            onDietaryProfilesHydrated()
        }

        if (pendingSyncStore.getPendingOnboardingVersion() == null) {
            runCatching { remoteDataSource.fetchOnboardingCompletedVersion() }
                .getOrNull()
                ?.let(onboardingStore::markOnboardingCompleted)
        }
    }
}
