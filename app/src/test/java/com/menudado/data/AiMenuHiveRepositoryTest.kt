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
import org.junit.Assert.assertFalse
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
    fun `baby search rejects honey candidate even with matching eligibility`() = runTest {
        val unsafe = sharedMenu(
            key = "yogurt|fruit|mixed",
            description = "Yogur natural con miel."
        )
        val dataSource = RecordingAiMenuHiveDataSource(
            server = Result.success(listOf(unsafe))
        )
        val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

        val result = repository.findCompatibleMenu(
            request(
                audience = MenuAudience.BABY,
                profile = DietaryProfile(ageRange = MenuAudience.BABY.defaultAgeRange)
            )
        ).getOrThrow()

        assertNull(result)
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
    fun `all unseen candidates are selected before rotation restarts`() = runTest {
        val first = sharedMenu("rice|vegetables|bowl")
        val second = sharedMenu("lentils|vegetables|stew")
        val dataSource = RecordingAiMenuHiveDataSource(
            server = Result.success(listOf(first, second))
        )
        val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

        val result = requireNotNull(
            repository.findCompatibleMenu(
                request(
                    recentSemanticHashes = setOf(first.semanticHash),
                    lastShownHash = first.semanticHash
                )
            ).getOrThrow()
        )

        assertEquals(second.semanticHash, result.semanticHash)
        assertFalse(result.startsNewRotationCycle)
    }

    @Test
    fun `completed rotation starts new cycle without immediate repeat`() = runTest {
        val first = sharedMenu("rice|vegetables|bowl")
        val second = sharedMenu("lentils|vegetables|stew")
        val dataSource = RecordingAiMenuHiveDataSource(
            server = Result.success(listOf(first, second))
        )
        val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

        val result = requireNotNull(
            repository.findCompatibleMenu(
                request(
                    recentSemanticHashes = setOf(first.semanticHash, second.semanticHash),
                    lastShownHash = first.semanticHash
                )
            ).getOrThrow()
        )

        assertEquals(second.semanticHash, result.semanticHash)
        assertTrue(result.startsNewRotationCycle)
    }

    @Test
    fun `single compatible candidate can repeat after its cycle`() = runTest {
        val only = sharedMenu("rice|vegetables|bowl")
        val dataSource = RecordingAiMenuHiveDataSource(server = Result.success(listOf(only)))
        val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

        val result = requireNotNull(
            repository.findCompatibleMenu(
                request(
                    recentSemanticHashes = setOf(only.semanticHash),
                    lastShownHash = only.semanticHash
                )
            ).getOrThrow()
        )

        assertEquals(only.semanticHash, result.semanticHash)
        assertTrue(result.startsNewRotationCycle)
    }

    @Test
    fun `legacy equivalent documents count as one canonical candidate`() = runTest {
        val first = legacySharedMenu(
            key = "salad|lentils+vegetable|mixed",
            storedHash = "1".repeat(64)
        )
        val second = legacySharedMenu(
            key = "salad|lentils+vegetables|mix",
            storedHash = "2".repeat(64)
        )
        val dataSource = RecordingAiMenuHiveDataSource(
            server = Result.success(listOf(first, second))
        )
        var selectableCount = 0
        val repository = AiMenuHiveRepository(
            dataSource,
            AiMenuHiveFeatureToggle(true)
        ) { size ->
            selectableCount = size
            0
        }

        val result = requireNotNull(repository.findCompatibleMenu(request()).getOrThrow())
        val expected = requireNotNull(
            AiMenuHiveIdentity.from(
                AppLanguage.SPANISH,
                "salad|lentils+vegetables|mixed"
            )
        )

        assertEquals(1, selectableCount)
        assertEquals(expected.semanticHash, result.semanticHash)
        assertEquals(expected.canonicalKey, result.generatedMenu.deduplicationKey)
    }

    @Test
    fun `reported multilingual toast documents count as one candidate`() = runTest {
        val first = legacySharedMenu(
            key = "toast|avocado|egg",
            storedHash = "3".repeat(64)
        )
        val second = legacySharedMenu(
            key = "tostada|aguacate|huevo",
            storedHash = "4".repeat(64)
        )
        var selectableCount = 0
        val repository = AiMenuHiveRepository(
            dataSource = RecordingAiMenuHiveDataSource(
                server = Result.success(listOf(first, second))
            ),
            featureToggle = AiMenuHiveFeatureToggle(true)
        ) { size ->
            selectableCount = size
            0
        }

        repository.findCompatibleMenu(request()).getOrThrow()

        assertEquals(1, selectableCount)
    }

    @Test
    fun `equivalent documents keep the same representative regardless of fetch order`() = runTest {
        val english = legacySharedMenu(
            key = "toast|avocado|egg",
            storedHash = "c4aeb4543dee6ba5d9e7ae72fcc5b322a1009879292e5033d3863fa21ff0e32d",
            name = "English representative"
        )
        val spanish = legacySharedMenu(
            key = "tostada|aguacate|huevo",
            storedHash = "547f2c44bdefc54d2181b50fc1cb3ef5ad4c062b44753dc4c6a740052568ab99",
            name = "Spanish representative"
        )

        suspend fun selectedName(candidates: List<SharedAiMenu>): String {
            val repository = AiMenuHiveRepository(
                dataSource = RecordingAiMenuHiveDataSource(
                    server = Result.success(candidates)
                ),
                featureToggle = AiMenuHiveFeatureToggle(true)
            ) { 0 }
            return requireNotNull(
                repository.findCompatibleMenu(request()).getOrThrow()
            ).generatedMenu.name
        }

        assertEquals("Spanish representative", selectedName(listOf(english, spanish)))
        assertEquals("Spanish representative", selectedName(listOf(spanish, english)))
    }

    @Test
    fun `minor garnish variants count as one candidate`() = runTest {
        val first = legacySharedMenu(
            key = "toast|avocado+egg|assembled",
            storedHash = "5".repeat(64)
        )
        val second = legacySharedMenu(
            key = "tostada|aguacate+huevo+cilantro|montada",
            storedHash = "6".repeat(64)
        )
        var selectableCount = 0
        val repository = AiMenuHiveRepository(
            dataSource = RecordingAiMenuHiveDataSource(
                server = Result.success(listOf(first, second))
            ),
            featureToggle = AiMenuHiveFeatureToggle(true)
        ) { size ->
            selectableCount = size
            0
        }

        repository.findCompatibleMenu(request()).getOrThrow()

        assertEquals(1, selectableCount)
    }

    @Test
    fun `different preparation variants remain separate candidates`() = runTest {
        val fried = legacySharedMenu(
            key = "potato|potato|fried",
            storedHash = "7".repeat(64)
        )
        val baked = legacySharedMenu(
            key = "potato|potato|baked",
            storedHash = "8".repeat(64)
        )
        var selectableCount = 0
        val repository = AiMenuHiveRepository(
            dataSource = RecordingAiMenuHiveDataSource(
                server = Result.success(listOf(fried, baked))
            ),
            featureToggle = AiMenuHiveFeatureToggle(true)
        ) { size ->
            selectableCount = size
            0
        }

        repository.findCompatibleMenu(request()).getOrThrow()

        assertEquals(2, selectableCount)
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
        audience: MenuAudience = MenuAudience.ADULT,
        profile: DietaryProfile = DietaryProfile(ageRange = "18+ años"),
        recentSemanticHashes: Set<String> = emptySet(),
        lastShownHash: String? = null
    ) = AiMenuHiveSearchRequest(
        language = AppLanguage.SPANISH,
        mealType = MealType.LUNCH,
        audience = audience,
        profile = profile,
        baseIngredients = "",
        recentSemanticHashes = recentSemanticHashes,
        lastShownHash = lastShownHash
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

    private fun legacySharedMenu(
        key: String,
        storedHash: String,
        name: String = "Idea recuperada"
    ): SharedAiMenu {
        val canonical = sharedMenu(key)
        return canonical.copy(
            semanticHash = storedHash,
            semanticKey = key,
            generatedMenu = canonical.generatedMenu.copy(
                name = name,
                deduplicationKey = key
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
