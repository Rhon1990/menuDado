package com.menudado.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDaoClearTest {
    @Test
    fun `clear all locally deactivates every contribution and removes purchased state`() = runTest {
        val dao = RecordingMarketDao(
            activeMenuIds = listOf(4L, 9L),
            activeProductCount = 3,
            purchasedStates = listOf(purchasedState("tomate"))
        )

        val clearedCount = dao.clearAllMarketProductsLocally(
            updatedAt = 500L,
            shouldSyncRemote = false,
            remoteSyncToken = null
        )

        assertEquals(4, clearedCount)
        assertEquals(1, dao.deactivateAllCalls)
        assertTrue(dao.productStates.isEmpty())
        assertTrue(dao.markedMenuBatches.isEmpty())
    }

    @Test
    fun `clear all with remote keeps reset tombstones and marks affected menus pending`() = runTest {
        val dao = RecordingMarketDao(
            activeMenuIds = listOf(4L, 9L),
            activeProductCount = 2,
            purchasedStates = listOf(
                purchasedState("tomate"),
                purchasedState("arroz")
            )
        )

        val clearedCount = dao.clearAllMarketProductsLocally(
            updatedAt = 700L,
            shouldSyncRemote = true,
            remoteSyncToken = "clear-token"
        )

        assertEquals(4, clearedCount)
        assertEquals(listOf(listOf(4L, 9L) to 700L), dao.markedMenuBatches)
        assertTrue(dao.productStates.all { !it.isPurchased })
        assertTrue(
            dao.productStates.all {
                it.remoteSyncState == RemoteSyncState.PENDING_UPSERT.name
            }
        )
        assertTrue(dao.productStates.all { it.remoteSyncToken == "clear-token" })
    }

    @Test
    fun `clear all is an idempotent no-op when market is empty`() = runTest {
        val dao = RecordingMarketDao()

        val clearedCount = dao.clearAllMarketProductsLocally(
            updatedAt = 800L,
            shouldSyncRemote = true,
            remoteSyncToken = "unused-token"
        )

        assertEquals(0, clearedCount)
        assertEquals(1, dao.deactivateAllCalls)
        assertTrue(dao.markedMenuBatches.isEmpty())
    }
}

private fun purchasedState(key: String) = MarketProductStateEntity(
    productKey = key,
    isPurchased = true,
    updatedAt = 100L
)

internal class RecordingMarketDao(
    private val activeMenuIds: List<Long> = emptyList(),
    private var activeProductCount: Int = 0,
    purchasedStates: List<MarketProductStateEntity> = emptyList(),
    var throwOnDeactivateAll: Boolean = false
) : MarketDao {
    private val menuProducts =
        MutableStateFlow<List<MenuShoppingProductEntity>>(emptyList())
    private val purchasedProducts =
        MutableStateFlow(purchasedStates.filter(MarketProductStateEntity::isPurchased))
    private val states = purchasedStates.associateByTo(linkedMapOf()) { it.productKey }

    var deactivateAllCalls = 0
    val markedMenuBatches = mutableListOf<Pair<List<Long>, Long>>()
    val productStates: List<MarketProductStateEntity>
        get() = states.values.toList()

    override suspend fun insertMenu(menu: MenuEntity): Long = menu.id

    override fun observeMenuProducts(): Flow<List<MenuShoppingProductEntity>> =
        menuProducts

    override suspend fun getMenuProducts(menuId: Long): List<MenuShoppingProductEntity> =
        menuProducts.value.filter { it.menuId == menuId }

    override fun observePurchasedProductStates(): Flow<List<MarketProductStateEntity>> =
        purchasedProducts

    override suspend fun getPurchasedProductStates(): List<MarketProductStateEntity> =
        states.values.filter(MarketProductStateEntity::isPurchased)

    override suspend fun getPendingProductStates(): List<MarketProductStateEntity> =
        states.values.filter { it.remoteSyncState != RemoteSyncState.SYNCED.name }

    override suspend fun getProductState(productKey: String): MarketProductStateEntity? =
        states[productKey]

    override suspend fun getActiveMenuIdsForProducts(productKeys: List<String>): List<Long> =
        activeMenuIds

    override suspend fun deactivateProducts(productKeys: List<String>): Int =
        minOf(activeProductCount, productKeys.size)

    override suspend fun getActiveMarketMenuIds(): List<Long> = activeMenuIds

    override suspend fun deactivateAllMarketProducts(): Int {
        deactivateAllCalls += 1
        if (throwOnDeactivateAll) error("market clear failed")
        return activeProductCount.also { activeProductCount = 0 }
    }

    override suspend fun markMenusPendingUpsert(
        menuIds: List<Long>,
        updatedAt: Long
    ): Int {
        markedMenuBatches += menuIds to updatedAt
        return menuIds.size
    }

    override suspend fun deleteMenuProducts(menuId: Long) = Unit

    override suspend fun insertMenuProducts(
        products: List<MenuShoppingProductEntity>
    ) = Unit

    override suspend fun deleteMenuById(menuId: Long) = Unit

    override suspend fun deletePendingMenuTombstone(
        menuId: Long,
        deletedAt: Long
    ): Int = 0

    override suspend fun upsertProductState(state: MarketProductStateEntity) {
        states[state.productKey] = state
        publishPurchasedStates()
    }

    override suspend fun deleteProductStateNow(productKey: String) {
        states.remove(productKey)
        publishPurchasedStates()
    }

    override suspend fun deleteProductStateIfCurrent(
        productKey: String,
        remoteSyncToken: String
    ): Int {
        val current = states[productKey]
        if (current?.remoteSyncToken != remoteSyncToken) return 0
        states.remove(productKey)
        publishPurchasedStates()
        return 1
    }

    override suspend fun clearProductStates() {
        states.clear()
        publishPurchasedStates()
    }

    override suspend fun markPurchasedProductStatesPendingUpsert(
        updatedAt: Long,
        remoteSyncToken: String
    ): Int {
        val purchased = getPurchasedProductStates()
        purchased.forEach { state ->
            states[state.productKey] = state.copy(
                updatedAt = updatedAt,
                remoteSyncState = RemoteSyncState.PENDING_UPSERT.name,
                remoteSyncToken = remoteSyncToken
            )
        }
        publishPurchasedStates()
        return purchased.size
    }

    private fun publishPurchasedStates() {
        purchasedProducts.value =
            states.values.filter(MarketProductStateEntity::isPurchased)
    }
}
