package com.menudado.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuDadoMigrationsTest {
    @Test
    fun `database version ten adds nullable cuisine without rewriting rows`() {
        assertEquals(10, MENU_DADO_DATABASE_VERSION)
        assertEquals(9, MIGRATION_9_TO_10.startVersion)
        assertEquals(10, MIGRATION_9_TO_10.endVersion)
        assertEquals(
            "ALTER TABLE menus ADD COLUMN cuisineInspiration TEXT",
            MIGRATION_9_TO_10_SQL
        )
    }
}
