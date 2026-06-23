package com.menudado.data

import com.menudado.backend.MenuDadoRemoteDataSource

class BackendStoredDataSyncer(
    private val dietaryProfileStore: DietaryProfileStore,
    private val aiDailyUsageStore: AiDailyUsageStore,
    private val pendingSyncStore: BackendPendingSyncStore,
    private val remoteDataSource: MenuDadoRemoteDataSource
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
}
