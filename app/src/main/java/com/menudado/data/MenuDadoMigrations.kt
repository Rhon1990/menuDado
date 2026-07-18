package com.menudado.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal const val MENU_DADO_DATABASE_VERSION = 10
internal const val MIGRATION_9_TO_10_SQL =
    "ALTER TABLE menus ADD COLUMN cuisineInspiration TEXT"

internal val MIGRATION_9_TO_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(MIGRATION_9_TO_10_SQL)
    }
}
