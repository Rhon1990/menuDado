package com.menudado.data

import com.menudado.domain.AiMenuHiveIdentity
import com.menudado.domain.AppLanguage
import com.menudado.domain.CuisineInspiration
import com.menudado.domain.DietaryProfile
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.MenuAudience
import com.menudado.domain.MealType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMenuHiveRepositoryTest {
    @Test
    fun `live server candidate is preferred and query is capped at twelve`() = runTest {
        val dataSource = RecordingAiMenuHiveDataSource(
            server = Result.success(listOf(sharedMenu("pasta|tomato|sauce")))
        )
        val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

        val result = repository.findCompatibleMenu(request()).getOrThrow()

        assertEquals("pasta|tomato|sauce", result?.generatedMenu?.deduplicationKey)
        assertEquals(AiMenuHiveLookupSource.SERVER, result?.source)
        assertEquals(12L, dataSource.requests.single().limit)
    }

    @Test
    fun `cache is used only when server fails`() = runTest {
        val cached = sharedMenu("pasta|tomato|sauce")
        val dataSource = RecordingAiMenuHiveDataSource(
            server = Result.failure(IllegalStateException("offline")),
            cache = Result.success(listOf(cached))
        )
        val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

        val result = repository.findCompatibleMenu(request()).getOrThrow()

        assertEquals(AiMenuHiveLookupSource.CACHE, result?.source)
        assertEquals(
            listOf(AiMenuHiveReadSource.SERVER, AiMenuHiveReadSource.CACHE),
            dataSource.requests.map(AiMenuHiveDataSourceRequest::source)
        )
    }

    @Test
    fun `unsafe and recently shown candidates lose priority to unseen safe menu`() = runTest {
        val unsafe = sharedMenu("pasta|tomato|sauce", "Pasta con queso")
        val repeated = sharedMenu("rice|vegetables|bowl")
        val fresh = sharedMenu("lentils|vegetables|stew")
        val dataSource = RecordingAiMenuHiveDataSource(
            server = Result.success(listOf(unsafe, repeated, fresh))
        )
        val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

        val result = repository.findCompatibleMenu(
            request(
                profile = DietaryProfile(isVegan = true),
                recentSemanticHashes = setOf(repeated.semanticHash)
            )
        ).getOrThrow()

        assertEquals(fresh.semanticHash, result?.semanticHash)
    }

    @Test
    fun `all safe menus already seen reuses one instead of returning unsafe failure`() = runTest {
        val repeated = sharedMenu("rice|vegetables|bowl")
        val dataSource = RecordingAiMenuHiveDataSource(server = Result.success(listOf(repeated)))
        val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

        val result = repository.findCompatibleMenu(
            request(recentSemanticHashes = setOf(repeated.semanticHash))
        ).getOrThrow()

        assertEquals(repeated.semanticHash, result?.semanticHash)
    }

    @Test
    fun `disabled hive performs no read or contribution`() = runTest {
        val dataSource = RecordingAiMenuHiveDataSource()
        val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(false)) { 0 }

        assertNull(repository.findCompatibleMenu(request()).getOrThrow())
        repository.contribute(contribution()).getOrThrow()

        assertTrue(dataSource.requests.isEmpty())
        assertTrue(dataSource.upserts.isEmpty())
    }

    private fun request(
        profile: DietaryProfile = DietaryProfile(ageRange = "18+ años"),
        recentSemanticHashes: Set<String> = emptySet()
    ) = AiMenuHiveSearchRequest(
        language = AppLanguage.SPANISH,
        mealType = MealType.LUNCH,
        audience = MenuAudience.ADULT,
        profile = profile,
        baseIngredients = "",
        recentSemanticHashes = recentSemanticHashes
    )

    private fun contribution() = AiMenuHiveContribution(
        language = AppLanguage.SPANISH,
        mealType = MealType.LUNCH,
        audience = MenuAudience.ADULT,
        profile = DietaryProfile(ageRange = "18+ años"),
        generatedMenu = GeneratedMenu(
            name = "Pasta con tomate",
            description = "Pasta integral con salsa de tomate.",
            notes = "Lista en 10 minutos.",
            calories = 430,
            deduplicationKey = "pasta|tomato|sauce"
        ),
        cuisineInspiration = CuisineInspiration.ITALIAN
    )

    private fun sharedMenu(
        key: String,
        description: String = "Pasta integral con salsa de tomate."
    ): SharedAiMenu {
        val identity = requireNotNull(AiMenuHiveIdentity.from(AppLanguage.SPANISH, key))
        return SharedAiMenu(
            semanticHash = identity.semanticHash,
            semanticKey = identity.canonicalKey,
            language = AppLanguage.SPANISH,
            generatedMenu = GeneratedMenu(
                name = "Idea recuperada",
                description = description,
                notes = "Lista en 10 minutos.",
                calories = 430,
                deduplicationKey = identity.canonicalKey
            ),
            cuisineInspiration = CuisineInspiration.ITALIAN,
            eligibilityKeys = setOf(
                AiMenuHiveIdentity.eligibilityKey(
                    AppLanguage.SPANISH,
                    MealType.LUNCH,
                    MenuAudience.ADULT,
                    DietaryProfile(ageRange = "18+ años")
                )
            )
        )
    }
}

private class RecordingAiMenuHiveDataSource(
    var server: Result<List<SharedAiMenu>> = Result.success(emptyList()),
    var cache: Result<List<SharedAiMenu>> = Result.success(emptyList())
) : AiMenuHiveDataSource {
    val requests = mutableListOf<AiMenuHiveDataSourceRequest>()
    val upserts = mutableListOf<SharedAiMenu>()

    override suspend fun fetch(request: AiMenuHiveDataSourceRequest): Result<List<SharedAiMenu>> {
        requests += request
        return when (request.source) {
            AiMenuHiveReadSource.SERVER -> server
            AiMenuHiveReadSource.CACHE -> cache
        }
    }

    override suspend fun upsert(menu: SharedAiMenu): Result<Unit> {
        upserts += menu
        return Result.success(Unit)
    }
}
