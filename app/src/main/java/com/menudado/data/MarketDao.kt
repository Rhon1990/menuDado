package com.menudado.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(menu: MenuEntity): Long

    @Query("SELECT * FROM menu_shopping_products ORDER BY displayName COLLATE NOCASE")
    fun observeMenuProducts(): Flow<List<MenuShoppingProductEntity>>

    @Query("SELECT * FROM menu_shopping_products WHERE menuId = :menuId")
    suspend fun getMenuProducts(menuId: Long): List<MenuShoppingProductEntity>

    @Query("SELECT * FROM market_product_states WHERE isPurchased = 1")
    fun observePurchasedProductStates(): Flow<List<MarketProductStateEntity>>

    @Query("SELECT * FROM market_product_states WHERE isPurchased = 1")
    suspend fun getPurchasedProductStates(): List<MarketProductStateEntity>

    @Query("SELECT * FROM market_product_states WHERE remoteSyncState != 'SYNCED'")
    suspend fun getPendingProductStates(): List<MarketProductStateEntity>

    @Query("SELECT * FROM market_product_states WHERE productKey = :productKey LIMIT 1")
    suspend fun getProductState(productKey: String): MarketProductStateEntity?

    @Query("SELECT DISTINCT menuId FROM menu_shopping_products WHERE productKey IN (:productKeys) AND isActive = 1")
    suspend fun getActiveMenuIdsForProducts(productKeys: List<String>): List<Long>

    @Query("UPDATE menu_shopping_products SET isActive = 0 WHERE productKey IN (:productKeys)")
    suspend fun deactivateProducts(productKeys: List<String>): Int

    @Query("SELECT DISTINCT menuId FROM menu_shopping_products WHERE isActive = 1")
    suspend fun getActiveMarketMenuIds(): List<Long>

    @Query("UPDATE menu_shopping_products SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllMarketProducts(): Int

    @Query("UPDATE menus SET remoteSyncState = 'PENDING_UPSERT', remoteSyncToken = NULL, updatedAt = :updatedAt WHERE id IN (:menuIds)")
    suspend fun markMenusPendingUpsert(menuIds: List<Long>, updatedAt: Long): Int

    @Query("DELETE FROM menu_shopping_products WHERE menuId = :menuId")
    suspend fun deleteMenuProducts(menuId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuProducts(products: List<MenuShoppingProductEntity>)

    @Transaction
    suspend fun replaceMenuProducts(
        menuId: Long,
        products: List<MenuShoppingProductEntity>
    ) {
        deleteMenuProducts(menuId)
        if (products.isNotEmpty()) {
            insertMenuProducts(products)
        }
    }

    @Transaction
    suspend fun upsertMenuWithProducts(
        menu: MenuEntity,
        products: List<MenuShoppingProductEntity>
    ): Long {
        val insertedId = insertMenu(menu)
        val savedId = menu.id.takeIf { it != 0L } ?: insertedId
        replaceMenuProducts(
            menuId = savedId,
            products = products.map { product -> product.copy(menuId = savedId) }
        )
        return savedId
    }

    @Query("DELETE FROM menus WHERE id = :menuId")
    suspend fun deleteMenuById(menuId: Long)

    @Transaction
    suspend fun deleteMenuWithProducts(menuId: Long) {
        deleteMenuProducts(menuId)
        deleteMenuById(menuId)
    }

    @Query("DELETE FROM menus WHERE id = :menuId AND remoteSyncState = 'PENDING_DELETE' AND deletedAt = :deletedAt")
    suspend fun deletePendingMenuTombstone(menuId: Long, deletedAt: Long): Int

    @Transaction
    suspend fun deletePendingTombstoneWithProducts(menuId: Long, deletedAt: Long): Int {
        val deletedCount = deletePendingMenuTombstone(menuId, deletedAt)
        if (deletedCount > 0) {
            deleteMenuProducts(menuId)
        }
        return deletedCount
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductState(state: MarketProductStateEntity)

    @Query("DELETE FROM market_product_states WHERE productKey = :productKey")
    suspend fun deleteProductStateNow(productKey: String)

    @Query("DELETE FROM market_product_states WHERE productKey = :productKey AND remoteSyncToken = :remoteSyncToken")
    suspend fun deleteProductStateIfCurrent(productKey: String, remoteSyncToken: String): Int

    @Query("DELETE FROM market_product_states")
    suspend fun clearProductStates()

    @Query(
        """
        UPDATE market_product_states
        SET remoteSyncState = 'PENDING_UPSERT',
            updatedAt = CASE WHEN updatedAt >= :updatedAt THEN updatedAt + 1 ELSE :updatedAt END,
            remoteSyncToken = :remoteSyncToken
        WHERE isPurchased = 1
        """
    )
    suspend fun markPurchasedProductStatesPendingUpsert(
        updatedAt: Long,
        remoteSyncToken: String
    ): Int

    @Transaction
    suspend fun replacePurchasedProductStates(states: List<MarketProductStateEntity>) {
        clearProductStates()
        states.forEach { state -> upsertProductState(state) }
    }

    @Transaction
    suspend fun clearPurchasedLocally(
        updatedAt: Long,
        shouldSyncRemote: Boolean,
        remoteSyncToken: String?
    ): Int {
        val purchasedStates = getPurchasedProductStates()
        if (purchasedStates.isEmpty()) return 0
        val productKeys = purchasedStates.map(MarketProductStateEntity::productKey)
        val affectedMenuIds = getActiveMenuIdsForProducts(productKeys)
        deactivateProducts(productKeys)
        if (shouldSyncRemote) {
            purchasedStates.forEach { state ->
                upsertProductState(
                    state.copy(
                        isPurchased = false,
                        updatedAt = updatedAt,
                        remoteSyncState = RemoteSyncState.PENDING_UPSERT.name,
                        remoteSyncToken = requireNotNull(remoteSyncToken)
                    )
                )
            }
            if (affectedMenuIds.isNotEmpty()) {
                markMenusPendingUpsert(affectedMenuIds, updatedAt)
            }
        } else {
            clearProductStates()
        }
        return purchasedStates.size
    }

    @Transaction
    suspend fun clearAllMarketProductsLocally(
        updatedAt: Long,
        shouldSyncRemote: Boolean,
        remoteSyncToken: String?
    ): Int {
        val affectedMenuIds = getActiveMarketMenuIds()
        val deactivatedCount = deactivateAllMarketProducts()
        val purchasedStates = getPurchasedProductStates()
        if (shouldSyncRemote) {
            val token = requireNotNull(remoteSyncToken)
            purchasedStates.forEach { state ->
                upsertProductState(
                    state.copy(
                        isPurchased = false,
                        updatedAt = updatedAt,
                        remoteSyncState = RemoteSyncState.PENDING_UPSERT.name,
                        remoteSyncToken = token
                    )
                )
            }
            if (affectedMenuIds.isNotEmpty()) {
                markMenusPendingUpsert(affectedMenuIds, updatedAt)
            }
        } else {
            clearProductStates()
        }
        return deactivatedCount + purchasedStates.size
    }
}
