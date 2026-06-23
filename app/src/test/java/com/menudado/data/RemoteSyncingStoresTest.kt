package com.menudado.data

import com.menudado.backend.BackendAppMetadata
import com.menudado.backend.MenuDadoRemoteDataSource
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.MenuAudience
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteSyncingStoresTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun `dietary profile save writes local first and mirrors remote`() = scope.runTest {
        val local = FakeLocalDietaryProfileStore()
        val remote = RecordingStoresRemoteDataSource()
        val pending = FakeBackendPendingSyncStore()
        val store = RemoteSyncingDietaryProfileStore(
            localStore = local,
            remoteDataSource = remote,
            pendingSyncStore = pending,
            scope = this
        )
        val profile = DietaryProfile(isEnabled = true, ageRange = "2-12 anos")

        store.saveProfile(profile, MenuAudience.CHILD)

        assertEquals(profile, local.getProfile(MenuAudience.CHILD))
        assertEquals(setOf(MenuAudience.CHILD), pending.getPendingDietaryProfileAudiences())
        advanceUntilIdle()
        assertEquals(listOf(MenuAudience.CHILD to profile), remote.dietaryProfiles)
        assertEquals(setOf(MenuAudience.CHILD), pending.getPendingDietaryProfileAudiences())
    }

    @Test
    fun `ai usage save writes local first and mirrors remote`() = scope.runTest {
        val local = FakeLocalAiDailyUsageStore()
        val remote = RecordingStoresRemoteDataSource()
        val pending = FakeBackendPendingSyncStore()
        val store = RemoteSyncingAiDailyUsageStore(
            localStore = local,
            remoteDataSource = remote,
            pendingSyncStore = pending,
            scope = this
        )
        val state = AiDailyUsageState(dateKey = "2026-06-23", usedCount = 4)

        store.saveUsageState(state)

        assertEquals(state, local.getUsageState())
        assertEquals(true, pending.isAiUsagePending())
        advanceUntilIdle()
        assertEquals(listOf(state), remote.aiUsageStates)
        assertEquals(true, pending.isAiUsagePending())
    }

    @Test
    fun `onboarding completion writes local first and mirrors remote`() = scope.runTest {
        val local = FakeLocalOnboardingStore()
        val remote = RecordingStoresRemoteDataSource()
        val pending = FakeBackendPendingSyncStore()
        val store = RemoteSyncingOnboardingStore(
            localStore = local,
            remoteDataSource = remote,
            pendingSyncStore = pending,
            scope = this
        )

        store.markOnboardingCompleted(2)

        assertEquals(true, local.isOnboardingCompleted(2))
        assertEquals(2, pending.getPendingOnboardingVersion())
        advanceUntilIdle()
        assertEquals(listOf(2), remote.onboardingVersions)
        assertEquals(2, pending.getPendingOnboardingVersion())
    }

    @Test
    fun `pending stored data syncer retries failed store writes`() = scope.runTest {
        val profileStore = FakeLocalDietaryProfileStore()
        val aiUsageStore = FakeLocalAiDailyUsageStore()
        val pending = FakeBackendPendingSyncStore()
        val remote = RecordingStoresRemoteDataSource()
        val syncer = BackendStoredDataSyncer(profileStore, aiUsageStore, pending, remote)
        val profile = DietaryProfile(isEnabled = true, ageRange = "18+ anos")
        val usage = AiDailyUsageState(dateKey = "2026-06-23", usedCount = 2)

        profileStore.saveProfile(profile, MenuAudience.ADULT)
        aiUsageStore.saveUsageState(usage)
        pending.markDietaryProfilePending(MenuAudience.ADULT)
        pending.markAiUsagePending()
        pending.markOnboardingPending(2)

        syncer.syncPending()

        assertEquals(listOf(MenuAudience.ADULT to profile), remote.dietaryProfiles)
        assertEquals(listOf(usage), remote.aiUsageStates)
        assertEquals(listOf(2), remote.onboardingVersions)
        assertEquals(emptySet<MenuAudience>(), pending.getPendingDietaryProfileAudiences())
        assertEquals(false, pending.isAiUsagePending())
        assertEquals(null, pending.getPendingOnboardingVersion())
    }
}

private class FakeLocalDietaryProfileStore : DietaryProfileStore {
    private val profiles = mutableMapOf<MenuAudience, DietaryProfile>()

    override fun getProfile(audience: MenuAudience): DietaryProfile {
        return profiles[audience] ?: DietaryProfile()
    }

    override fun saveProfile(profile: DietaryProfile, audience: MenuAudience) {
        profiles[audience] = profile
    }
}

private class FakeLocalAiDailyUsageStore : AiDailyUsageStore {
    private var state: AiDailyUsageState? = null

    override fun getUsageState(): AiDailyUsageState? = state

    override fun saveUsageState(state: AiDailyUsageState) {
        this.state = state
    }
}

private class FakeLocalOnboardingStore : OnboardingStore {
    private var completedVersion = 0

    override fun isOnboardingCompleted(requiredVersion: Int): Boolean {
        return completedVersion >= requiredVersion
    }

    override fun markOnboardingCompleted(version: Int) {
        completedVersion = version
    }
}

private class RecordingStoresRemoteDataSource : MenuDadoRemoteDataSource {
    val dietaryProfiles = mutableListOf<Pair<MenuAudience, DietaryProfile>>()
    val aiUsageStates = mutableListOf<AiDailyUsageState>()
    val onboardingVersions = mutableListOf<Int>()

    override suspend fun upsertMetadata(metadata: BackendAppMetadata) = Unit
    override suspend fun upsertMenu(menu: FoodMenu) = Unit
    override suspend fun deleteMenu(menu: FoodMenu) = Unit

    override suspend fun upsertDietaryProfile(audience: MenuAudience, profile: DietaryProfile) {
        dietaryProfiles += audience to profile
    }

    override suspend fun upsertAiUsage(state: AiDailyUsageState) {
        aiUsageStates += state
    }

    override suspend fun upsertOnboardingCompleted(contentVersion: Int) {
        onboardingVersions += contentVersion
    }
}

private class FakeBackendPendingSyncStore : BackendPendingSyncStore {
    private val pendingProfiles = mutableSetOf<MenuAudience>()
    private var pendingAiUsage = false
    private var pendingOnboardingVersion: Int? = null

    override fun markDietaryProfilePending(audience: MenuAudience) {
        pendingProfiles += audience
    }

    override fun clearDietaryProfilePending(audience: MenuAudience) {
        pendingProfiles -= audience
    }

    override fun getPendingDietaryProfileAudiences(): Set<MenuAudience> = pendingProfiles.toSet()

    override fun markAiUsagePending() {
        pendingAiUsage = true
    }

    override fun clearAiUsagePending() {
        pendingAiUsage = false
    }

    override fun isAiUsagePending(): Boolean = pendingAiUsage

    override fun markOnboardingPending(version: Int) {
        pendingOnboardingVersion = version
    }

    override fun clearOnboardingPending() {
        pendingOnboardingVersion = null
    }

    override fun getPendingOnboardingVersion(): Int? = pendingOnboardingVersion
}
