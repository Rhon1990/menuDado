package com.menudado.data

import com.menudado.ai.HealthAnalyzer
import com.menudado.backend.BackendMarketProductState
import com.menudado.backend.MenuDadoRemoteDataSource
import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.MarketProduct
import com.menudado.domain.MenuAiDetails
import com.menudado.domain.MealType
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MenuAudience
import com.menudado.domain.AppLanguage
import com.menudado.domain.CuisineInspiration
import com.menudado.domain.aggregateMarketProducts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    private val marketDao: MarketDao? = null,
    private val clockMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val syncTokenProvider: () -> String = { UUID.randomUUID().toString() }
) {
    private val remoteMutationMutexes = ConcurrentHashMap<Long, Mutex>()
    private val marketRemoteMutationMutexes = ConcurrentHashMap<String, Mutex>()
    val menus: Flow<List<FoodMenu>> = marketDao?.let { shoppingDao ->
        combine(
            menuDao.observeMenus(),
            shoppingDao.observeMenuProducts()
        ) { menuEntities, productEntities ->
            val productsByMenu = productEntities.groupBy(MenuShoppingProductEntity::menuId)
            menuEntities.map { entity -> entity.toDomain(productsByMenu[entity.id].orEmpty()) }
        }
    } ?: menuDao.observeMenus().map { entities -> entities.map { it.toDomain() } }

    val marketProducts: Flow<List<MarketProduct>> = combine(
        menus,
        marketDao?.observePurchasedProductStates() ?: flowOf(emptyList())
    ) { savedMenus, purchasedStates ->
        aggregateMarketProducts(
            menus = savedMenus,
            purchasedProductKeys = purchasedStates.mapTo(mutableSetOf()) { it.productKey }
        )
    }

    suspend fun save(menu: FoodMenu) {
        val updatedAt = clockMillisProvider()
        val pendingSyncToken = remoteDataSource?.let { syncTokenProvider() }
        val previousActiveProductKeys = marketDao
            ?.getMenuProducts(menu.id)
            .orEmpty()
            .filter(MenuShoppingProductEntity::isActive)
            .mapTo(mutableSetOf(), MenuShoppingProductEntity::productKey)
        val purchasedProductKeys = marketDao
            ?.getPurchasedProductStates()
            .orEmpty()
            .mapTo(mutableSetOf(), MarketProductStateEntity::productKey)
        val entity = menu.toEntity(
            remoteSyncState = if (remoteDataSource == null) {
                RemoteSyncState.SYNCED
            } else {
                RemoteSyncState.PENDING_UPSERT
            },
            updatedAt = updatedAt,
            remoteSyncToken = pendingSyncToken
        )
        val savedMenu = if (marketDao != null) {
            val generatedId = marketDao.upsertMenuWithProducts(
                menu = entity,
                products = menu.toShoppingProductEntities()
            )
            menu.copy(id = generatedId)
        } else if (menu.id == 0L) {
            val generatedId = menuDao.insert(
                entity
            )
            menu.copy(id = generatedId)
        } else {
            menuDao.update(
                entity
            )
            menu
        }
        if (savedMenu.activeShoppingProductKeys.isNotEmpty()) {
            val newlyActivatedPurchasedKeys = savedMenu.activeShoppingProductKeys
                .filter { key -> key !in previousActiveProductKeys && key in purchasedProductKeys }
            newlyActivatedPurchasedKeys.forEach { key ->
                setMarketProductPurchased(key, false)
            }
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
            if (marketDao == null) {
                menuDao.delete(menu.toEntity())
            } else {
                marketDao.deleteMenuWithProducts(menu.id)
            }
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
                    .onSuccess {
                        if (marketDao == null) {
                            menuDao.deletePendingTombstone(menu.id, deletedAt)
                        } else {
                            marketDao.deletePendingTombstoneWithProducts(menu.id, deletedAt)
                        }
                    }
            }
        }
    }

    suspend fun syncPendingMenus() {
        val remote = remoteDataSource ?: return
        menuDao.getPendingSyncMenus().forEach { entity ->
            val syncState = runCatching {
                RemoteSyncState.valueOf(entity.remoteSyncState)
            }.getOrDefault(RemoteSyncState.PENDING_UPSERT)
            val menu = entity.toDomain(marketDao?.getMenuProducts(entity.id).orEmpty())

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
                                .onSuccess {
                                    if (marketDao == null) {
                                        menuDao.deletePendingTombstone(menu.id, deletedAt)
                                    } else {
                                        marketDao.deletePendingTombstoneWithProducts(menu.id, deletedAt)
                                    }
                                }
                        }
                    }
                }
            }
        }
        marketDao?.getPendingProductStates()?.forEach { state ->
            syncMarketProductState(state)
        }
    }

    suspend fun syncRemoteMenus() {
        val remote = remoteDataSource ?: return
        if (menuDao.countPendingSyncMenus() > 0) return
        remote.fetchMenus().forEach { menu ->
            val remoteEntity = menu.toEntity(
                remoteSyncState = RemoteSyncState.SYNCED,
                updatedAt = clockMillisProvider(),
                remoteSyncToken = null
            )
            if (marketDao == null) {
                menuDao.insert(remoteEntity)
            } else {
                marketDao.upsertMenuWithProducts(
                    menu = remoteEntity,
                    products = menu.toShoppingProductEntities()
                )
            }
        }
        val shoppingDao = marketDao ?: return
        if (shoppingDao.getPendingProductStates().isEmpty()) {
            shoppingDao.replacePurchasedProductStates(
                remote.fetchMarketProductStates()
                    .filter(BackendMarketProductState::isPurchased)
                    .map { state ->
                    MarketProductStateEntity(
                        productKey = state.productKey,
                        isPurchased = true,
                        updatedAt = state.updatedAt,
                        remoteSyncState = RemoteSyncState.SYNCED.name,
                        remoteSyncToken = null
                    )
                }
            )
        }
    }

    suspend fun pendingSyncMenuCount(): Int {
        return menuDao.countPendingSyncMenus()
    }

    suspend fun markVisibleMenusPendingBackendSync() {
        val updatedAt = clockMillisProvider()
        menuDao.markVisibleMenusPendingUpsert(updatedAt)
        marketDao?.markPurchasedProductStatesPendingUpsert(
            updatedAt = updatedAt,
            remoteSyncToken = syncTokenProvider()
        )
    }

    suspend fun setMarketProductPurchased(productKey: String, isPurchased: Boolean) {
        val dao = marketDao ?: return
        val existingState = dao.getProductState(productKey)
        val updatedAt = maxOf(
            clockMillisProvider(),
            existingState?.updatedAt?.plus(1L) ?: Long.MIN_VALUE
        )
        val pendingSyncToken = remoteDataSource?.let { syncTokenProvider() }
        val state = MarketProductStateEntity(
            productKey = productKey,
            isPurchased = isPurchased,
            updatedAt = updatedAt,
            remoteSyncState = if (remoteDataSource == null) {
                RemoteSyncState.SYNCED.name
            } else {
                RemoteSyncState.PENDING_UPSERT.name
            },
            remoteSyncToken = pendingSyncToken
        )
        dao.upsertProductState(state)
        if (remoteDataSource == null && !isPurchased) {
            dao.deleteProductStateNow(productKey)
        } else if (remoteDataSource != null) {
            syncMarketProductState(state)
        }
    }

    suspend fun clearPurchasedMarketProducts() {
        val dao = marketDao ?: return
        val updatedAt = maxOf(
            clockMillisProvider(),
            dao.getPurchasedProductStates().maxOfOrNull(MarketProductStateEntity::updatedAt)
                ?.plus(1L)
                ?: Long.MIN_VALUE
        )
        val clearedCount = dao.clearPurchasedLocally(
            updatedAt = updatedAt,
            shouldSyncRemote = remoteDataSource != null,
            remoteSyncToken = remoteDataSource?.let { syncTokenProvider() }
        )
        if (clearedCount > 0 && remoteDataSource != null) {
            syncPendingMenus()
        }
    }

    suspend fun clearAllMarketProducts() {
        val dao = marketDao ?: return
        val updatedAt = maxOf(
            clockMillisProvider(),
            dao.getPurchasedProductStates().maxOfOrNull(MarketProductStateEntity::updatedAt)
                ?.plus(1L)
                ?: Long.MIN_VALUE
        )
        val clearedCount = dao.clearAllMarketProductsLocally(
            updatedAt = updatedAt,
            shouldSyncRemote = remoteDataSource != null,
            remoteSyncToken = remoteDataSource?.let { syncTokenProvider() }
        )
        if (clearedCount > 0 && remoteDataSource != null) {
            syncPendingMenus()
        }
    }

    suspend fun analyze(menu: FoodMenu, language: AppLanguage): Result<MenuAiDetails> {
        return healthAnalyzer.analyze(menu, language)
    }

    suspend fun analyzeBatch(menus: List<FoodMenu>, language: AppLanguage): Result<Map<Long, MenuAiDetails>> {
        return healthAnalyzer.analyzeBatch(menus, language)
    }

    suspend fun generateMenu(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile,
        audience: MenuAudience,
        baseIngredients: String,
        language: AppLanguage,
        cuisineInspiration: CuisineInspiration
    ): Result<GeneratedMenu> {
        return healthAnalyzer.generateMenu(
            mealType,
            avoidIdeas,
            dietaryProfile,
            audience,
            baseIngredients,
            language,
            cuisineInspiration
        )
    }

    private suspend fun syncMenuUpsert(menu: FoodMenu, syncToken: String) {
        val remote = remoteDataSource ?: return
        runCatching { remote.upsertMenu(menu) }
            .onSuccess { menuDao.markUpsertSynced(menu.id, syncToken) }
    }

    private suspend fun syncMarketProductState(state: MarketProductStateEntity) {
        val remote = remoteDataSource ?: return
        val dao = marketDao ?: return
        marketMutationMutex(state.productKey).withLock {
            val currentState = dao.getProductState(state.productKey)
            if (
                currentState?.remoteSyncToken != state.remoteSyncToken ||
                currentState?.remoteSyncState == RemoteSyncState.SYNCED.name
            ) {
                return@withLock
            }
            val remoteState = runCatching {
                remote.upsertMarketProductState(
                    BackendMarketProductState(
                        productKey = state.productKey,
                        isPurchased = state.isPurchased,
                        updatedAt = state.updatedAt,
                        mutationToken = requireNotNull(state.remoteSyncToken)
                    )
                )
            }.getOrNull() ?: return@withLock
            val latestLocalState = dao.getProductState(state.productKey)
            if (latestLocalState?.remoteSyncToken != state.remoteSyncToken) {
                return@withLock
            }
            if (remoteState.isPurchased) {
                dao.upsertProductState(
                    MarketProductStateEntity(
                        productKey = remoteState.productKey,
                        isPurchased = true,
                        updatedAt = remoteState.updatedAt,
                        remoteSyncState = RemoteSyncState.SYNCED.name,
                        remoteSyncToken = null
                    )
                )
            } else {
                dao.deleteProductStateIfCurrent(
                    productKey = state.productKey,
                    remoteSyncToken = requireNotNull(state.remoteSyncToken)
                )
            }
        }
    }

    private fun FoodMenu.toShoppingProductEntities(): List<MenuShoppingProductEntity> {
        return shoppingProducts.map { product ->
            MenuShoppingProductEntity(
                menuId = id,
                productKey = product.key,
                normalizedName = product.normalizedName,
                displayName = product.displayName,
                isActive = product.key in activeShoppingProductKeys
            )
        }
    }

    private fun remoteMutationMutex(menuId: Long): Mutex {
        return remoteMutationMutexes.getOrPut(menuId) { Mutex() }
    }

    private fun marketMutationMutex(productKey: String): Mutex {
        return marketRemoteMutationMutexes.getOrPut(productKey) { Mutex() }
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
