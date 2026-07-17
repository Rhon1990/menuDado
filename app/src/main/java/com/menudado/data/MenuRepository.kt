package com.menudado.data

import com.menudado.ai.HealthAnalyzer
import com.menudado.backend.MenuDadoRemoteDataSource
import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.MealType
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MenuAudience
import com.menudado.domain.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MenuRepository(
    private val menuDao: MenuDao,
    private val healthAnalyzer: HealthAnalyzer,
    private val remoteDataSource: MenuDadoRemoteDataSource? = null,
    private val clockMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val syncTokenProvider: () -> String = { UUID.randomUUID().toString() }
) {
    private val remoteMutationMutexes = ConcurrentHashMap<Long, Mutex>()
    val menus: Flow<List<FoodMenu>> = menuDao.observeMenus()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun save(menu: FoodMenu) {
        val updatedAt = clockMillisProvider()
        val pendingSyncToken = remoteDataSource?.let { syncTokenProvider() }
        val savedMenu = if (menu.id == 0L) {
            val generatedId = menuDao.insert(
                menu.toEntity(
                    remoteSyncState = if (remoteDataSource == null) {
                        RemoteSyncState.SYNCED
                    } else {
                        RemoteSyncState.PENDING_UPSERT
                    },
                    updatedAt = updatedAt,
                    remoteSyncToken = pendingSyncToken
                )
            )
            menu.copy(id = generatedId)
        } else {
            menuDao.update(
                menu.toEntity(
                    remoteSyncState = if (remoteDataSource == null) {
                        RemoteSyncState.SYNCED
                    } else {
                        RemoteSyncState.PENDING_UPSERT
                    },
                    updatedAt = updatedAt,
                    remoteSyncToken = pendingSyncToken
                )
            )
            menu
        }
        if (pendingSyncToken != null) {
            CoroutineScope(currentCoroutineContext()).launch(start = CoroutineStart.UNDISPATCHED) {
                remoteMutationMutex(savedMenu.id).withLock {
                    if (isCurrentPendingUpsert(savedMenu.id, pendingSyncToken)) {
                        syncMenuUpsert(savedMenu, pendingSyncToken)
                    }
                }
            }
        }
    }

    suspend fun delete(menu: FoodMenu) {
        val remote = remoteDataSource
        if (remote == null) {
            menuDao.delete(menu.toEntity())
            return
        }

        val deletedAt = clockMillisProvider()
        val tombstone = menu.toEntity(
            remoteSyncState = RemoteSyncState.PENDING_DELETE,
            updatedAt = deletedAt,
            deletedAt = deletedAt
        )
        menuDao.update(tombstone)

        remoteMutationMutex(menu.id).withLock {
            if (isCurrentPendingDelete(menu.id, deletedAt)) {
                runCatching { remote.deleteMenu(menu) }
                    .onSuccess { menuDao.deletePendingTombstone(menu.id, deletedAt) }
            }
        }
    }

    suspend fun syncPendingMenus() {
        val remote = remoteDataSource ?: return
        menuDao.getPendingSyncMenus().forEach { entity ->
            val syncState = runCatching {
                RemoteSyncState.valueOf(entity.remoteSyncState)
            }.getOrDefault(RemoteSyncState.PENDING_UPSERT)
            val menu = entity.toDomain()

            when (syncState) {
                RemoteSyncState.SYNCED -> Unit
                RemoteSyncState.PENDING_UPSERT -> {
                    val syncToken = entity.remoteSyncToken ?: syncTokenProvider().also { token ->
                        menuDao.update(entity.copy(remoteSyncToken = token))
                    }
                    remoteMutationMutex(menu.id).withLock {
                        if (isCurrentPendingUpsert(menu.id, syncToken)) {
                            syncMenuUpsert(menu, syncToken)
                        }
                    }
                }
                RemoteSyncState.PENDING_DELETE -> {
                    remoteMutationMutex(menu.id).withLock {
                        val deletedAt = entity.deletedAt
                        if (deletedAt != null && isCurrentPendingDelete(menu.id, deletedAt)) {
                            runCatching { remote.deleteMenu(menu) }
                                .onSuccess { menuDao.deletePendingTombstone(menu.id, deletedAt) }
                        }
                    }
                }
            }
        }
    }

    suspend fun syncRemoteMenus() {
        val remote = remoteDataSource ?: return
        if (menuDao.countPendingSyncMenus() > 0) return
        remote.fetchMenus().forEach { menu ->
            menuDao.insert(
                menu.toEntity(
                    remoteSyncState = RemoteSyncState.SYNCED,
                    updatedAt = clockMillisProvider(),
                    remoteSyncToken = null
                )
            )
        }
    }

    suspend fun pendingSyncMenuCount(): Int {
        return menuDao.countPendingSyncMenus()
    }

    suspend fun markVisibleMenusPendingBackendSync() {
        menuDao.markVisibleMenusPendingUpsert(clockMillisProvider())
    }

    suspend fun analyze(menu: FoodMenu, language: AppLanguage): Result<HealthAnalysis> {
        return healthAnalyzer.analyze(menu, language)
    }

    suspend fun analyzeBatch(menus: List<FoodMenu>, language: AppLanguage): Result<Map<Long, HealthAnalysis>> {
        return healthAnalyzer.analyzeBatch(menus, language)
    }

    suspend fun generateMenu(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile,
        audience: MenuAudience,
        baseIngredients: String,
        language: AppLanguage
    ): Result<GeneratedMenu> {
        return healthAnalyzer.generateMenu(mealType, avoidIdeas, dietaryProfile, audience, baseIngredients, language)
    }

    private suspend fun syncMenuUpsert(menu: FoodMenu, syncToken: String) {
        val remote = remoteDataSource ?: return
        runCatching { remote.upsertMenu(menu) }
            .onSuccess { menuDao.markUpsertSynced(menu.id, syncToken) }
    }

    private fun remoteMutationMutex(menuId: Long): Mutex {
        return remoteMutationMutexes.getOrPut(menuId) { Mutex() }
    }

    private suspend fun isCurrentPendingUpsert(menuId: Long, syncToken: String): Boolean {
        return menuDao.getPendingSyncMenus().any { entity ->
            entity.id == menuId &&
                entity.remoteSyncState == RemoteSyncState.PENDING_UPSERT.name &&
                entity.remoteSyncToken == syncToken
        }
    }

    private suspend fun isCurrentPendingDelete(menuId: Long, deletedAt: Long): Boolean {
        return menuDao.getPendingSyncMenus().any { entity ->
            entity.id == menuId &&
                entity.remoteSyncState == RemoteSyncState.PENDING_DELETE.name &&
                entity.deletedAt == deletedAt
        }
    }
}
