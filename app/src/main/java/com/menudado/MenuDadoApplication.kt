package com.menudado

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.analytics.FirebaseAnalytics
import com.menudado.analytics.FirebaseMenuDadoAnalytics
import com.menudado.analytics.MenuDadoAnalytics
import com.menudado.ai.FirebaseHealthAnalyzer
import com.menudado.appcheck.installMenuDadoAppCheckProvider
import com.menudado.backend.BackendAppMetadata
import com.menudado.backend.FirebaseMenuDadoRemoteDataSource
import com.menudado.backend.FirebaseAiMenuHiveDataSource
import com.menudado.backend.MenuDadoRemoteDataSource
import com.menudado.auth.FirebaseMenuDadoAuthService
import com.menudado.auth.MenuDadoAuthSession
import com.menudado.auth.MenuDadoAuthService
import com.menudado.auth.MenuDadoAuthStore
import com.menudado.auth.SharedPreferencesMenuDadoAuthStore
import com.menudado.auth.shouldStartMenuDadoBackendSync
import com.menudado.data.AiDailyUsageStore
import com.menudado.data.AiQuotaRetryStore
import com.menudado.data.AiRequestThrottleStore
import com.menudado.data.AiMenuHiveFeatureToggle
import com.menudado.data.AiMenuHiveGateway
import com.menudado.data.AiMenuHiveRepository
import com.menudado.data.CuisineRotation
import com.menudado.data.BackendPendingSyncStore
import com.menudado.data.BackendStoredDataSyncer
import com.menudado.data.MenuDadoDatabase
import com.menudado.data.MIGRATION_9_TO_10
import com.menudado.data.MIGRATION_10_TO_11
import com.menudado.data.MenuRepository
import com.menudado.data.DietaryProfileStore
import com.menudado.data.GuestUsageStore
import com.menudado.data.OnboardingStore
import com.menudado.data.RemoteSyncingAiDailyUsageStore
import com.menudado.data.RemoteSyncingDietaryProfileStore
import com.menudado.data.RemoteSyncingOnboardingStore
import com.menudado.data.SharedPreferencesBackendPendingSyncStore
import com.menudado.data.SharedPreferencesAiDailyUsageStore
import com.menudado.data.SharedPreferencesAiQuotaRetryStore
import com.menudado.data.SharedPreferencesAiRequestThrottleStore
import com.menudado.data.SharedPreferencesCuisineRotationStateStore
import com.menudado.data.SharedPreferencesDietaryProfileStore
import com.menudado.data.SharedPreferencesGuestUsageStore
import com.menudado.data.SharedPreferencesOnboardingStore
import com.menudado.domain.MenuAudience
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class MenuDadoApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _dietaryProfileHydrationRevision = MutableStateFlow(0L)
    val dietaryProfileHydrationRevision: StateFlow<Long> = _dietaryProfileHydrationRevision.asStateFlow()
    private val backendAccountPreferences by lazy {
        applicationContext.getSharedPreferences(BACKEND_ACCOUNT_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE menus ADD COLUMN calories INTEGER")
            db.execSQL("ALTER TABLE menus ADD COLUMN lastPickedDate TEXT")
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE menus ADD COLUMN audience TEXT NOT NULL DEFAULT 'ADULT'")
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE menus ADD COLUMN imageUri TEXT")
        }
    }

    private val migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE menus ADD COLUMN remoteSyncState TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE menus ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE menus ADD COLUMN deletedAt INTEGER")
            db.execSQL("UPDATE menus SET updatedAt = createdAt WHERE updatedAt = 0")
        }
    }

    private val migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE menus ADD COLUMN remoteSyncToken TEXT")
        }
    }

    private val migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE menus
                SET remoteSyncState = 'PENDING_UPSERT',
                    remoteSyncToken = NULL
                WHERE deletedAt IS NULL
                    AND remoteSyncState = 'SYNCED'
                """.trimIndent()
            )
        }
    }

    private val migration7To8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE menus ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val migration8To9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE menus ADD COLUMN favoritedAt INTEGER")
            db.execSQL("UPDATE menus SET favoritedAt = createdAt WHERE isFavorite = 1")
        }
    }

    private val database: MenuDadoDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            MenuDadoDatabase::class.java,
            "menu-dado.db"
        )
            .addMigrations(
                migration1To2,
                migration2To3,
                migration3To4,
                migration4To5,
                migration5To6,
                migration6To7,
                migration7To8,
                migration8To9,
                MIGRATION_9_TO_10,
                MIGRATION_10_TO_11
            )
            .build()
    }

    val remoteDataSource: MenuDadoRemoteDataSource by lazy {
        FirebaseMenuDadoRemoteDataSource()
    }

    val authStore: MenuDadoAuthStore by lazy {
        SharedPreferencesMenuDadoAuthStore(applicationContext)
    }

    val authService: MenuDadoAuthService by lazy {
        FirebaseMenuDadoAuthService(authStore = authStore)
    }

    val repository: MenuRepository by lazy {
        MenuRepository(
            menuDao = database.menuDao(),
            marketDao = database.marketDao(),
            healthAnalyzer = FirebaseHealthAnalyzer(),
            remoteDataSource = remoteDataSource
        )
    }

    val aiMenuHiveFeatureToggle by lazy {
        AiMenuHiveFeatureToggle(isEnabled = true)
    }

    val aiMenuHiveGateway: AiMenuHiveGateway by lazy {
        AiMenuHiveRepository(
            dataSource = FirebaseAiMenuHiveDataSource(),
            featureToggle = aiMenuHiveFeatureToggle
        )
    }

    val aiQuotaRetryStore: AiQuotaRetryStore by lazy {
        SharedPreferencesAiQuotaRetryStore(applicationContext)
    }

    val aiRequestThrottleStore: AiRequestThrottleStore by lazy {
        SharedPreferencesAiRequestThrottleStore(applicationContext)
    }

    val cuisineRotation: CuisineRotation by lazy {
        CuisineRotation(SharedPreferencesCuisineRotationStateStore(applicationContext))
    }

    private val localAiDailyUsageStore: AiDailyUsageStore by lazy {
        SharedPreferencesAiDailyUsageStore(applicationContext)
    }

    private val localDietaryProfileStore: DietaryProfileStore by lazy {
        SharedPreferencesDietaryProfileStore(applicationContext)
    }

    private val localOnboardingStore: OnboardingStore by lazy {
        SharedPreferencesOnboardingStore(applicationContext)
    }

    val guestUsageStore: GuestUsageStore by lazy {
        SharedPreferencesGuestUsageStore(applicationContext)
    }

    private val pendingSyncStore: BackendPendingSyncStore by lazy {
        SharedPreferencesBackendPendingSyncStore(applicationContext)
    }

    val aiDailyUsageStore: AiDailyUsageStore by lazy {
        RemoteSyncingAiDailyUsageStore(
            localStore = localAiDailyUsageStore,
            remoteDataSource = remoteDataSource,
            pendingSyncStore = pendingSyncStore,
            storedDataSyncer = backendStoredDataSyncer,
            scope = applicationScope
        )
    }

    val dietaryProfileStore: DietaryProfileStore by lazy {
        RemoteSyncingDietaryProfileStore(
            localStore = localDietaryProfileStore,
            remoteDataSource = remoteDataSource,
            pendingSyncStore = pendingSyncStore,
            storedDataSyncer = backendStoredDataSyncer,
            scope = applicationScope
        )
    }

    val onboardingStore: OnboardingStore by lazy {
        RemoteSyncingOnboardingStore(
            localStore = localOnboardingStore,
            remoteDataSource = remoteDataSource,
            pendingSyncStore = pendingSyncStore,
            storedDataSyncer = backendStoredDataSyncer,
            scope = applicationScope
        )
    }

    private val backendStoredDataSyncer: BackendStoredDataSyncer by lazy {
        BackendStoredDataSyncer(
            dietaryProfileStore = localDietaryProfileStore,
            aiDailyUsageStore = localAiDailyUsageStore,
            onboardingStore = localOnboardingStore,
            pendingSyncStore = pendingSyncStore,
            remoteDataSource = remoteDataSource,
            onDietaryProfilesHydrated = {
                _dietaryProfileHydrationRevision.update { revision -> revision + 1L }
            }
        )
    }

    val analytics: MenuDadoAnalytics by lazy {
        FirebaseMenuDadoAnalytics(FirebaseAnalytics.getInstance(applicationContext))
    }

    override fun onCreate() {
        super.onCreate()
        installMenuDadoAppCheckProvider()
        if (shouldStartMenuDadoBackendSync(authService.currentSession(), authStore.isGuestModeSelected())) {
            syncBackendNow(BACKEND_SYNC_SOURCE_APP_START)
        }
    }

    fun syncBackendNow(source: String = BACKEND_SYNC_SOURCE_AUTH) {
        applicationScope.launch {
            val session = authService.currentSession()
            prepareLocalDataForBackendAccountIfNeeded(session)
            val pendingMenuCount = runCatching { repository.pendingSyncMenuCount() }.getOrDefault(0)
            analytics.trackBackendSyncRetried(source, pendingMenuCount)
            val syncResult = runCatching {
                remoteDataSource.upsertMetadata(BackendAppMetadata.current(session))
                repository.syncPendingMenus()
                repository.syncRemoteMenus()
                backendStoredDataSyncer.hydrateRemoteStoredData()
                backendStoredDataSyncer.syncPending()
            }
            if (syncResult.isSuccess && session?.isAnonymous == false) {
                markBackendAccountSynced(session.userId)
            }
            analytics.trackBackendSyncFinished(
                source = source,
                status = if (syncResult.isSuccess) BACKEND_SYNC_STATUS_SUCCESS else BACKEND_SYNC_STATUS_FAILURE,
                pendingMenuCount = runCatching { repository.pendingSyncMenuCount() }.getOrDefault(pendingMenuCount)
            )
        }
    }

    suspend fun prepareLocalDataForSignedInBackendSync() {
        withContext(Dispatchers.IO) {
            markLocalStoredDataPendingBackendSync()
        }
    }

    private suspend fun prepareLocalDataForBackendAccountIfNeeded(session: MenuDadoAuthSession?) {
        if (session?.isAnonymous != false) return
        val lastSyncedUserId = backendAccountPreferences.getString(KEY_LAST_SYNCED_USER_ID, null)
        if (lastSyncedUserId != session.userId) {
            markLocalStoredDataPendingBackendSync()
        }
    }

    private suspend fun markLocalStoredDataPendingBackendSync() {
        repository.markVisibleMenusPendingBackendSync()
        MenuAudience.entries.forEach(pendingSyncStore::markDietaryProfilePending)
        if (localAiDailyUsageStore.getUsageState() != null) {
            pendingSyncStore.markAiUsagePending()
        }
    }

    private fun markBackendAccountSynced(userId: String) {
        backendAccountPreferences.edit()
            .putString(KEY_LAST_SYNCED_USER_ID, userId)
            .apply()
    }

    private companion object {
        const val BACKEND_ACCOUNT_PREFERENCES_NAME = "menu-dado-backend-account"
        const val KEY_LAST_SYNCED_USER_ID = "last_synced_user_id"
        const val BACKEND_SYNC_SOURCE_APP_START = "app_start"
        const val BACKEND_SYNC_SOURCE_AUTH = "auth"
        const val BACKEND_SYNC_STATUS_SUCCESS = "success"
        const val BACKEND_SYNC_STATUS_FAILURE = "failure"
    }
}
