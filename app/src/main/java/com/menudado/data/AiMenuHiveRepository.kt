package com.menudado.data

import com.menudado.domain.AiMenuHiveIdentity
import com.menudado.domain.AppLanguage
import com.menudado.domain.CuisineInspiration
import com.menudado.domain.DietaryProfile
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.MenuAudience
import com.menudado.domain.MealType
import com.menudado.domain.ShoppingProduct
import com.menudado.domain.accepts
import kotlin.random.Random

enum class AiMenuHiveReadSource { SERVER, CACHE }
enum class AiMenuHiveLookupSource { SERVER, CACHE }
enum class AiMenuHiveQueryField { SCOPE, ELIGIBILITY }

data class SharedAiMenu(
    val semanticHash: String,
    val semanticKey: String,
    val language: AppLanguage,
    val generatedMenu: GeneratedMenu,
    val cuisineInspiration: CuisineInspiration?,
    val eligibilityKeys: Set<String>,
    val scopeKey: String? = null
)

data class AiMenuHiveDataSourceRequest(
    val queryField: AiMenuHiveQueryField,
    val key: String,
    val source: AiMenuHiveReadSource,
    val limit: Long
)

interface AiMenuHiveDataSource {
    suspend fun fetch(request: AiMenuHiveDataSourceRequest): Result<List<SharedAiMenu>>
    suspend fun upsert(menu: SharedAiMenu): Result<Unit>
}

data class AiMenuHiveSearchRequest(
    val language: AppLanguage,
    val mealType: MealType,
    val audience: MenuAudience,
    val profile: DietaryProfile,
    val baseIngredients: String,
    val recentSemanticHashes: Set<String>,
    val lastShownHash: String? = null
)

data class AiMenuHiveContribution(
    val language: AppLanguage,
    val mealType: MealType,
    val audience: MenuAudience,
    val profile: DietaryProfile,
    val generatedMenu: GeneratedMenu,
    val cuisineInspiration: CuisineInspiration?
)

data class AiMenuHiveCandidate(
    val generatedMenu: GeneratedMenu,
    val cuisineInspiration: CuisineInspiration?,
    val semanticHash: String,
    val source: AiMenuHiveLookupSource,
    val startsNewRotationCycle: Boolean = false
)

interface AiMenuHiveGateway {
    suspend fun findCompatibleMenu(request: AiMenuHiveSearchRequest): Result<AiMenuHiveCandidate?>
    suspend fun contribute(contribution: AiMenuHiveContribution): Result<Unit>
}

object NoOpAiMenuHiveGateway : AiMenuHiveGateway {
    override suspend fun findCompatibleMenu(
        request: AiMenuHiveSearchRequest
    ): Result<AiMenuHiveCandidate?> = Result.success(null)

    override suspend fun contribute(contribution: AiMenuHiveContribution): Result<Unit> =
        Result.success(Unit)
}

class AiMenuHiveFeatureToggle(@Volatile var isEnabled: Boolean)

class AiMenuHiveRepository(
    private val dataSource: AiMenuHiveDataSource,
    private val featureToggle: AiMenuHiveFeatureToggle,
    private val pickIndexProvider: (Int) -> Int = Random::nextInt
) : AiMenuHiveGateway {
    override suspend fun findCompatibleMenu(
        request: AiMenuHiveSearchRequest
    ): Result<AiMenuHiveCandidate?> {
        if (!featureToggle.isEnabled) return Result.success(null)
        val scopeRequest = AiMenuHiveDataSourceRequest(
            queryField = AiMenuHiveQueryField.SCOPE,
            key = AiMenuHiveIdentity.scopeKey(
                request.language,
                request.mealType,
                request.audience
            ),
            source = AiMenuHiveReadSource.SERVER,
            limit = AI_MENU_HIVE_SCOPE_QUERY_LIMIT
        )
        val scope = fetchWithCache(scopeRequest).getOrElse {
            return Result.failure(it)
        }
        selectCandidate(scope.first, request)?.let { selection ->
            return Result.success(selection.toCandidate(scope.second))
        }

        val legacyRequest = AiMenuHiveDataSourceRequest(
            queryField = AiMenuHiveQueryField.ELIGIBILITY,
            key = AiMenuHiveIdentity.eligibilityKey(
                request.language,
                request.mealType,
                request.audience,
                request.profile
            ),
            source = AiMenuHiveReadSource.SERVER,
            limit = AI_MENU_HIVE_LEGACY_QUERY_LIMIT
        )
        val legacy = fetchWithCache(legacyRequest).getOrElse {
            return Result.failure(it)
        }
        val selection = selectCandidate(legacy.first, request)
            ?: return Result.success(null)
        return Result.success(selection.toCandidate(legacy.second))
    }

    private suspend fun fetchWithCache(
        request: AiMenuHiveDataSourceRequest
    ): Result<Pair<List<SharedAiMenu>, AiMenuHiveLookupSource>> {
        val server = dataSource.fetch(
            request.copy(source = AiMenuHiveReadSource.SERVER)
        )
        if (server.isSuccess) {
            return Result.success(
                server.getOrThrow() to AiMenuHiveLookupSource.SERVER
            )
        }
        return dataSource.fetch(
            request.copy(source = AiMenuHiveReadSource.CACHE)
        ).map { menus ->
            menus to AiMenuHiveLookupSource.CACHE
        }
    }

    override suspend fun contribute(contribution: AiMenuHiveContribution): Result<Unit> {
        if (!featureToggle.isEnabled) return Result.success(Unit)
        if (
            !contribution.profile.accepts(
                menu = contribution.generatedMenu,
                audience = contribution.audience
            )
        ) {
            return Result.success(Unit)
        }
        val identity = AiMenuHiveIdentity.from(
            contribution.language,
            contribution.generatedMenu.deduplicationKey
        ) ?: return Result.success(Unit)
        return dataSource.upsert(
            SharedAiMenu(
                semanticHash = identity.semanticHash,
                semanticKey = identity.canonicalKey,
                language = contribution.language,
                generatedMenu = contribution.generatedMenu.copy(
                    deduplicationKey = identity.canonicalKey
                ),
                cuisineInspiration = contribution.cuisineInspiration,
                eligibilityKeys = setOf(
                    AiMenuHiveIdentity.eligibilityKey(
                        contribution.language,
                        contribution.mealType,
                        contribution.audience,
                        contribution.profile
                    )
                )
            )
        )
    }

    private fun selectCandidate(
        candidates: List<SharedAiMenu>,
        request: AiMenuHiveSearchRequest
    ): HiveSelection? {
        val safe = candidates
            .sortedBy(SharedAiMenu::semanticHash)
            .mapNotNull(SharedAiMenu::canonicalized)
            .filter { candidate ->
                request.profile.accepts(
                    menu = candidate.generatedMenu,
                    audience = request.audience
                )
            }
            .collapseSimilarCandidates()
        if (safe.isEmpty()) return null
        val unseen = safe.filterNot { it.semanticHash in request.recentSemanticHashes }
        val startsNewCycle = unseen.isEmpty()
        val cycleCandidates = when {
            !startsNewCycle -> unseen
            safe.size > 1 -> safe.filterNot { it.semanticHash == request.lastShownHash }
                .ifEmpty { safe }
            else -> safe
        }
        val preferred = cycleCandidates.filter { candidate ->
            candidate.generatedMenu.searchableText()
                .containsAnyRequestedIngredient(request.baseIngredients)
        }.ifEmpty { cycleCandidates }
        return HiveSelection(
            menu = preferred[pickIndexProvider(preferred.size).coerceIn(preferred.indices)],
            startsNewCycle = startsNewCycle
        )
    }
}

private fun List<SharedAiMenu>.collapseSimilarCandidates(): List<SharedAiMenu> = buildList {
    this@collapseSimilarCandidates.forEach { candidate ->
        val alreadyRepresented = any { existing ->
            AiMenuHiveIdentity.areSimilar(
                existing.semanticKey,
                candidate.semanticKey
            )
        }
        if (!alreadyRepresented) add(candidate)
    }
}

private fun SharedAiMenu.canonicalized(): SharedAiMenu? {
    val identity = AiMenuHiveIdentity.from(language, semanticKey) ?: return null
    return copy(
        semanticHash = identity.semanticHash,
        semanticKey = identity.canonicalKey,
        generatedMenu = generatedMenu.copy(deduplicationKey = identity.canonicalKey)
    )
}

private data class HiveSelection(
    val menu: SharedAiMenu,
    val startsNewCycle: Boolean
) {
    fun toCandidate(source: AiMenuHiveLookupSource) = AiMenuHiveCandidate(
        generatedMenu = menu.generatedMenu,
        cuisineInspiration = menu.cuisineInspiration,
        semanticHash = menu.semanticHash,
        source = source,
        startsNewRotationCycle = startsNewCycle
    )
}

private fun GeneratedMenu.searchableText(): String = buildList {
    add(name)
    add(description)
    add(notes)
    addAll(shoppingProducts.map(ShoppingProduct::displayName))
}.joinToString(" ").lowercase()

private fun String.containsAnyRequestedIngredient(input: String): Boolean {
    val requested = input.split(',', ';', '\n')
        .map { it.trim().lowercase() }
        .filter(String::isNotBlank)
    return requested.isNotEmpty() && requested.any(::contains)
}

internal const val AI_MENU_HIVE_SCOPE_QUERY_LIMIT = 36L
internal const val AI_MENU_HIVE_LEGACY_QUERY_LIMIT = 12L
