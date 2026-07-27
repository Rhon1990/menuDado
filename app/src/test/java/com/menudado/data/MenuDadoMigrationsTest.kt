package com.menudado.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuDadoMigrationsTest {
    @Test
    fun `database version ten adds nullable cuisine without rewriting rows`() {
        assertEquals(11, MENU_DADO_DATABASE_VERSION)
        assertEquals(9, MIGRATION_9_TO_10.startVersion)
        assertEquals(10, MIGRATION_9_TO_10.endVersion)
        assertEquals(
            "ALTER TABLE menus ADD COLUMN cuisineInspiration TEXT",
            MIGRATION_9_TO_10_SQL
        )
    }

    @Test
    fun `migration eleven creates market product and purchased state tables`() {
        assertEquals(10, MIGRATION_10_TO_11.startVersion)
        assertEquals(11, MIGRATION_10_TO_11.endVersion)
        assertTrue(MIGRATION_10_TO_11_SQL.any { it.contains("menu_shopping_products") })
        assertTrue(MIGRATION_10_TO_11_SQL.any { it.contains("market_product_states") })
        assertTrue(MIGRATION_10_TO_11_SQL.any { it.contains("remoteSyncState") })
        assertTrue(MIGRATION_10_TO_11_SQL.any { it.contains("remoteSyncToken") })
        assertEquals(4, MIGRATION_10_TO_11_SQL.count { it.startsWith("CREATE INDEX") })
    }
}
