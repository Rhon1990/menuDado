package com.menudado.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MenuEntity::class],
    version = 4,
    exportSchema = false
)
abstract class MenuDadoDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
}
