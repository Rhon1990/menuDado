package com.menudado.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal const val MENU_DADO_DATABASE_VERSION = 11
internal const val MIGRATION_9_TO_10_SQL =
    "ALTER TABLE menus ADD COLUMN cuisineInspiration TEXT"
internal val MIGRATION_10_TO_11_SQL = listOf(
    """
    CREATE TABLE IF NOT EXISTS menu_shopping_products (
        menuId INTEGER NOT NULL,
        productKey TEXT NOT NULL,
        normalizedName TEXT NOT NULL,
        displayName TEXT NOT NULL,
        isActive INTEGER NOT NULL,
        PRIMARY KEY(menuId, productKey)
    )
    """.trimIndent(),
    "CREATE INDEX IF NOT EXISTS index_menu_shopping_products_productKey " +
        "ON menu_shopping_products (productKey)",
    "CREATE INDEX IF NOT EXISTS index_menu_shopping_products_isActive " +
        "ON menu_shopping_products (isActive)",
    """
    CREATE TABLE IF NOT EXISTS market_product_states (
        productKey TEXT NOT NULL,
        isPurchased INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL,
        remoteSyncState TEXT NOT NULL,
        remoteSyncToken TEXT,
        PRIMARY KEY(productKey)
    )
    """.trimIndent(),
    "CREATE INDEX IF NOT EXISTS index_market_product_states_isPurchased " +
        "ON market_product_states (isPurchased)",
    "CREATE INDEX IF NOT EXISTS index_market_product_states_remoteSyncState " +
        "ON market_product_states (remoteSyncState)"
)

internal val MIGRATION_9_TO_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(MIGRATION_9_TO_10_SQL)
    }
}

internal val MIGRATION_10_TO_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_10_TO_11_SQL.forEach(db::execSQL)
    }
}
