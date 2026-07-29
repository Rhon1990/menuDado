package com.menudado.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MenuEntity::class,
        MenuShoppingProductEntity::class,
        MarketProductStateEntity::class
    ],
    version = MENU_DADO_DATABASE_VERSION,
    exportSchema = false
)
abstract class MenuDadoDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun marketDao(): MarketDao
}
