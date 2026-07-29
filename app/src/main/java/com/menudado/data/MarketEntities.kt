package com.menudado.data

import androidx.room.Entity
import androidx.room.Index
import com.menudado.domain.ShoppingProduct

@Entity(
    tableName = "menu_shopping_products",
    primaryKeys = ["menuId", "productKey"],
    indices = [
        Index(value = ["productKey"]),
        Index(value = ["isActive"])
    ]
)
data class MenuShoppingProductEntity(
    val menuId: Long,
    val productKey: String,
    val normalizedName: String,
    val displayName: String,
    val isActive: Boolean
) {
    fun toDomain(): ShoppingProduct = ShoppingProduct(
        key = productKey,
        normalizedName = normalizedName,
        displayName = displayName
    )
}

@Entity(
    tableName = "market_product_states",
    primaryKeys = ["productKey"],
    indices = [
        Index(value = ["isPurchased"]),
        Index(value = ["remoteSyncState"])
    ]
)
data class MarketProductStateEntity(
    val productKey: String,
    val isPurchased: Boolean,
    val updatedAt: Long,
    val remoteSyncState: String = RemoteSyncState.SYNCED.name,
    val remoteSyncToken: String? = null
)
