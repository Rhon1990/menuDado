package com.menudado.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarketDaoClearInstrumentedTest {
    private lateinit var database: MenuDadoDatabase
    private lateinit var menuDao: MenuDao
    private lateinit var marketDao: MarketDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MenuDadoDatabase::class.java
        ).allowMainThreadQueries().build()
        menuDao = database.menuDao()
        marketDao = database.marketDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun clearAll_preservesPendingDeleteMenuAndItsProducts() = runBlocking {
        val visibleMenu = menuEntity(id = 1L)
        val deletedAt = 200L
        val pendingDeleteMenu = menuEntity(
            id = 2L,
            syncState = RemoteSyncState.PENDING_DELETE,
            deletedAt = deletedAt
        )
        menuDao.insert(visibleMenu)
        menuDao.insert(pendingDeleteMenu)
        marketDao.insertMenuProducts(
            listOf(
                product(menuId = visibleMenu.id, key = "tomate"),
                product(menuId = pendingDeleteMenu.id, key = "arroz")
            )
        )

        val clearedCount = marketDao.clearAllMarketProductsLocally(
            updatedAt = 900L,
            shouldSyncRemote = true,
            remoteSyncToken = "clear-token"
        )

        val pendingMenus = menuDao.getPendingSyncMenus().associateBy(MenuEntity::id)
        assertEquals(1, clearedCount)
        assertEquals(
            RemoteSyncState.PENDING_UPSERT.name,
            pendingMenus.getValue(visibleMenu.id).remoteSyncState
        )
        val preservedDelete = pendingMenus.getValue(pendingDeleteMenu.id)
        assertEquals(RemoteSyncState.PENDING_DELETE.name, preservedDelete.remoteSyncState)
        assertEquals(deletedAt, preservedDelete.deletedAt)
        assertFalse(marketDao.getMenuProducts(visibleMenu.id).single().isActive)
        assertTrue(marketDao.getMenuProducts(pendingDeleteMenu.id).single().isActive)
    }
}

private fun menuEntity(
    id: Long,
    syncState: RemoteSyncState = RemoteSyncState.SYNCED,
    deletedAt: Long? = null
) = MenuEntity(
    id = id,
    name = "Menú $id",
    mealType = "LUNCH",
    description = "Descripción",
    notes = "",
    healthStatus = null,
    healthReason = null,
    healthSuggestion = null,
    calories = null,
    lastPickedDate = null,
    remoteSyncState = syncState.name,
    deletedAt = deletedAt
)

private fun product(menuId: Long, key: String) = MenuShoppingProductEntity(
    menuId = menuId,
    productKey = key,
    normalizedName = key,
    displayName = key,
    isActive = true
)
