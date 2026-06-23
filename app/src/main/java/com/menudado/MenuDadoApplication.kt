package com.menudado

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.analytics.FirebaseAnalytics
import com.menudado.analytics.FirebaseMenuDadoAnalytics
import com.menudado.analytics.MenuDadoAnalytics
import com.menudado.ai.FirebaseHealthAnalyzer
import com.menudado.backend.BackendAppMetadata
import com.menudado.backend.FirebaseMenuDadoRemoteDataSource
import com.menudado.backend.MenuDadoRemoteDataSource
import com.menudado.data.AiDailyUsageStore
import com.menudado.data.AiQuotaRetryStore
import com.menudado.data.BackendPendingSyncStore
import com.menudado.data.BackendStoredDataSyncer
import com.menudado.data.MenuDadoDatabase
import com.menudado.data.MenuRepository
import com.menudado.data.DietaryProfileStore
import com.menudado.data.OnboardingStore
import com.menudado.data.RemoteSyncingAiDailyUsageStore
import com.menudado.data.RemoteSyncingDietaryProfileStore
import com.menudado.data.RemoteSyncingOnboardingStore
import com.menudado.data.SharedPreferencesBackendPendingSyncStore
import com.menudado.data.SharedPreferencesAiDailyUsageStore
import com.menudado.data.SharedPreferencesAiQuotaRetryStore
import com.menudado.data.SharedPreferencesDietaryProfileStore
import com.menudado.data.SharedPreferencesOnboardingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MenuDadoApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    private val database: MenuDadoDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            MenuDadoDatabase::class.java,
            "menu-dado.db"
        )
            .addMigrations(migration1To2, migration2To3, migration3To4, migration4To5, migration5To6)
            .build()
    }

    val remoteDataSource: MenuDadoRemoteDataSource by lazy {
        FirebaseMenuDadoRemoteDataSource()
    }

    val repository: MenuRepository by lazy {
        MenuRepository(
            menuDao = database.menuDao(),
            healthAnalyzer = FirebaseHealthAnalyzer(),
            remoteDataSource = remoteDataSource
        )
    }

    val aiQuotaRetryStore: AiQuotaRetryStore by lazy {
        SharedPreferencesAiQuotaRetryStore(applicationContext)
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
            pendingSyncStore = pendingSyncStore,
            remoteDataSource = remoteDataSource
        )
    }

    val analytics: MenuDadoAnalytics by lazy {
        FirebaseMenuDadoAnalytics(FirebaseAnalytics.getInstance(applicationContext))
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            runCatching { remoteDataSource.upsertMetadata(BackendAppMetadata.current()) }
            runCatching { repository.syncPendingMenus() }
            runCatching { backendStoredDataSyncer.syncPending() }
        }
    }
}
