package com.menudado.data

import com.menudado.backend.MenuDadoRemoteDataSource
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MenuAudience
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RemoteSyncingDietaryProfileStore(
    private val localStore: DietaryProfileStore,
    private val remoteDataSource: MenuDadoRemoteDataSource,
    private val pendingSyncStore: BackendPendingSyncStore,
    private val storedDataSyncer: BackendStoredDataSyncer? = null,
    private val scope: CoroutineScope
) : DietaryProfileStore {
    override fun getProfile(audience: MenuAudience): DietaryProfile {
        return localStore.getProfile(audience)
    }

    override fun saveProfile(profile: DietaryProfile, audience: MenuAudience) {
        localStore.saveProfile(profile, audience)
        pendingSyncStore.markDietaryProfilePending(audience)
        scope.launch {
            runCatching { remoteDataSource.upsertDietaryProfile(audience, profile) }
            runCatching { storedDataSyncer?.syncPending() }
        }
    }
}

class RemoteSyncingAiDailyUsageStore(
    private val localStore: AiDailyUsageStore,
    private val remoteDataSource: MenuDadoRemoteDataSource,
    private val pendingSyncStore: BackendPendingSyncStore,
    private val storedDataSyncer: BackendStoredDataSyncer? = null,
    private val scope: CoroutineScope
) : AiDailyUsageStore {
    override fun getUsageState(): AiDailyUsageState? {
        return localStore.getUsageState()
    }

    override fun saveUsageState(state: AiDailyUsageState) {
        localStore.saveUsageState(state)
        pendingSyncStore.markAiUsagePending()
        scope.launch {
            runCatching { remoteDataSource.upsertAiUsage(state) }
            runCatching { storedDataSyncer?.syncPending() }
        }
    }
}

class RemoteSyncingOnboardingStore(
    private val localStore: OnboardingStore,
    private val remoteDataSource: MenuDadoRemoteDataSource,
    private val pendingSyncStore: BackendPendingSyncStore,
    private val storedDataSyncer: BackendStoredDataSyncer? = null,
    private val scope: CoroutineScope
) : OnboardingStore {
    override fun isOnboardingCompleted(requiredVersion: Int): Boolean {
        return localStore.isOnboardingCompleted(requiredVersion)
    }

    override fun markOnboardingCompleted(version: Int) {
        localStore.markOnboardingCompleted(version)
        pendingSyncStore.markOnboardingPending(version)
        scope.launch {
            runCatching { remoteDataSource.upsertOnboardingCompleted(version) }
            runCatching { storedDataSyncer?.syncPending() }
        }
    }
}
