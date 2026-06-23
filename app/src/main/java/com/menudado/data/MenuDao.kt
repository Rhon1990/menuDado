package com.menudado.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menus WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeMenus(): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menus WHERE remoteSyncState != 'SYNCED'")
    suspend fun getPendingSyncMenus(): List<MenuEntity>

    @Query("UPDATE menus SET remoteSyncState = 'SYNCED', remoteSyncToken = NULL WHERE id = :id AND remoteSyncToken = :remoteSyncToken AND remoteSyncState = 'PENDING_UPSERT'")
    suspend fun markUpsertSynced(id: Long, remoteSyncToken: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(menu: MenuEntity): Long

    @Update
    suspend fun update(menu: MenuEntity)

    @Delete
    suspend fun delete(menu: MenuEntity)
}
