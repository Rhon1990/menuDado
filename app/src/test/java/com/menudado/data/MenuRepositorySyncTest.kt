package com.menudado.data

import com.menudado.ai.HealthAnalyzer
import com.menudado.backend.MenuDadoRemoteDataSource
import com.menudado.domain.AppLanguage
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuRepositorySyncTest {
    @Test
    fun `saving new menu syncs remote with generated local id`() = runTest {
        val dao = FakeSyncMenuDao()
        val remote = RecordingMenuRemoteDataSource()
        val repository = MenuRepository(dao, NoOpHealthAnalyzer, remote)

        repository.save(sampleMenu(id = 0L, name = "Menu nuevo"))

        assertEquals(1L, dao.saved.single().id)
        assertEquals(1L, remote.upsertedMenus.single().id)
        assertEquals("Menu nuevo", remote.upsertedMenus.single().name)
    }

    @Test
    fun `saving menu remains local when remote sync fails`() = runTest {
        val dao = FakeSyncMenuDao()
        val remote = RecordingMenuRemoteDataSource().apply {
            failUpsert = true
        }
        val repository = MenuRepository(dao, NoOpHealthAnalyzer, remote)

        repository.save(sampleMenu(id = 0L, name = "Menu local"))

        assertEquals("Menu local", dao.saved.single().name)
        assertTrue(remote.upsertAttempts == 1)
    }

    @Test
    fun `deleting menu removes local row and attempts remote delete`() = runTest {
        val dao = FakeSyncMenuDao()
        val remote = RecordingMenuRemoteDataSource()
        val repository = MenuRepository(dao, NoOpHealthAnalyzer, remote)
        val menu = sampleMenu(id = 12L, name = "Menu a borrar")
        dao.seed(listOf(menu))

        repository.delete(menu)

        assertTrue(dao.saved.isEmpty())
        assertEquals(listOf(menu), remote.deletedMenus)
    }

    @Test
    fun `failed remote delete leaves hidden tombstone for pending retry`() = runTest {
        val dao = FakeSyncMenuDao()
        val remote = RecordingMenuRemoteDataSource().apply {
            failDelete = true
        }
        val repository = MenuRepository(
            menuDao = dao,
            healthAnalyzer = NoOpHealthAnalyzer,
            remoteDataSource = remote,
            clockMillisProvider = { 123L }
        )
        val menu = sampleMenu(id = 12L, name = "Menu a borrar")
        dao.seed(listOf(menu))

        repository.delete(menu)

        assertTrue(dao.observeVisibleMenus().isEmpty())
        assertEquals(RemoteSyncState.PENDING_DELETE.name, dao.saved.single().remoteSyncState)
        assertEquals(123L, dao.saved.single().deletedAt)

        remote.failDelete = false
        repository.syncPendingMenus()

        assertTrue(dao.saved.isEmpty())
        assertEquals(listOf(menu), remote.deletedMenus)
    }

    @Test
    fun `stale remote upsert success does not clear newer pending save`() = runTest {
        val dao = FakeSyncMenuDao()
        val remote = RecordingMenuRemoteDataSource()
        val repository = MenuRepository(
            menuDao = dao,
            healthAnalyzer = NoOpHealthAnalyzer,
            remoteDataSource = remote,
            clockMillisProvider = { 100L },
            syncTokenProvider = { "old-token" }
        )
        dao.seed(listOf(sampleMenu(id = 7L, name = "Version inicial")))

        repository.save(sampleMenu(id = 7L, name = "Version vieja"))
        dao.update(
            sampleMenu(id = 7L, name = "Version nueva").toEntity(
                remoteSyncState = RemoteSyncState.PENDING_UPSERT,
                updatedAt = 100L,
                remoteSyncToken = "new-token"
            )
        )

        remote.failUpsert = false
        dao.markUpsertSynced(id = 7L, remoteSyncToken = "old-token")

        val saved = dao.saved.single()
        assertEquals("Version nueva", saved.name)
        assertEquals(RemoteSyncState.PENDING_UPSERT.name, saved.remoteSyncState)
        assertEquals(100L, saved.updatedAt)
        assertEquals("new-token", saved.remoteSyncToken)
    }

    private fun sampleMenu(id: Long, name: String): FoodMenu {
        return FoodMenu(
            id = id,
            name = name,
            mealType = MealType.LUNCH,
            audience = MenuAudience.ADULT,
            description = "Descripcion",
            notes = "",
            createdAt = 1_719_000_000_000L
        )
    }
}

private class FakeSyncMenuDao : MenuDao {
    private val menus = MutableStateFlow<List<MenuEntity>>(emptyList())
    val saved: List<MenuEntity>
        get() = menus.value

    fun seed(foodMenus: List<FoodMenu>) {
        menus.value = foodMenus.map { it.toEntity() }
    }

    override fun observeMenus(): Flow<List<MenuEntity>> = menus

    fun observeVisibleMenus(): List<MenuEntity> = menus.value.filter { it.deletedAt == null }

    override suspend fun getPendingSyncMenus(): List<MenuEntity> {
        return menus.value.filter { it.remoteSyncState != RemoteSyncState.SYNCED.name }
    }

    override suspend fun countPendingSyncMenus(): Int {
        return getPendingSyncMenus().size
    }

    override suspend fun markUpsertSynced(id: Long, remoteSyncToken: String): Int {
        var updatedCount = 0
        menus.value = menus.value.map { existing ->
            if (
                existing.id == id &&
                existing.remoteSyncToken == remoteSyncToken &&
                existing.remoteSyncState == RemoteSyncState.PENDING_UPSERT.name
            ) {
                updatedCount += 1
                existing.copy(
                    remoteSyncState = RemoteSyncState.SYNCED.name,
                    remoteSyncToken = null
                )
            } else {
                existing
            }
        }
        return updatedCount
    }

    override suspend fun insert(menu: MenuEntity): Long {
        val nextId = (menus.value.maxOfOrNull { it.id } ?: 0L) + 1L
        menus.value = listOf(menu.copy(id = nextId)) + menus.value
        return nextId
    }

    override suspend fun update(menu: MenuEntity) {
        menus.value = menus.value.map { existing ->
            if (existing.id == menu.id) menu else existing
        }
    }

    override suspend fun delete(menu: MenuEntity) {
        menus.value = menus.value.filterNot { it.id == menu.id }
    }
}

private class RecordingMenuRemoteDataSource : MenuDadoRemoteDataSource {
    val upsertedMenus = mutableListOf<FoodMenu>()
    val deletedMenus = mutableListOf<FoodMenu>()
    var upsertAttempts = 0
    var failUpsert = false
    var failDelete = false

    override suspend fun upsertMetadata(metadata: com.menudado.backend.BackendAppMetadata) = Unit

    override suspend fun upsertMenu(menu: FoodMenu) {
        upsertAttempts += 1
        if (failUpsert) error("Remote unavailable")
        upsertedMenus += menu
    }

    override suspend fun deleteMenu(menu: FoodMenu) {
        if (failDelete) error("Remote unavailable")
        deletedMenus += menu
    }

    override suspend fun upsertDietaryProfile(audience: MenuAudience, profile: DietaryProfile) = Unit

    override suspend fun upsertAiUsage(state: AiDailyUsageState) = Unit

    override suspend fun upsertOnboardingCompleted(contentVersion: Int) = Unit
}

private object NoOpHealthAnalyzer : HealthAnalyzer {
    override suspend fun analyze(menu: FoodMenu, language: AppLanguage): Result<HealthAnalysis> {
        error("Not needed")
    }

    override suspend fun analyzeBatch(
        menus: List<FoodMenu>,
        language: AppLanguage
    ): Result<Map<Long, HealthAnalysis>> {
        error("Not needed")
    }

    override suspend fun generateMenu(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile,
        audience: MenuAudience,
        baseIngredients: String,
        language: AppLanguage
    ): Result<GeneratedMenu> {
        error("Not needed")
    }
}
