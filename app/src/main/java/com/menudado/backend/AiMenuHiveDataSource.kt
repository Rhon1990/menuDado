package com.menudado.backend

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.menudado.data.AiMenuHiveDataSource
import com.menudado.data.AiMenuHiveDataSourceRequest
import com.menudado.data.AiMenuHiveQueryField
import com.menudado.data.AiMenuHiveReadSource
import com.menudado.data.SharedAiMenu
import com.menudado.domain.AppLanguage
import com.menudado.domain.CuisineInspiration
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.ShoppingProduct

class FirebaseAiMenuHiveDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AiMenuHiveDataSource {
    override suspend fun fetch(
        request: AiMenuHiveDataSourceRequest
    ): Result<List<SharedAiMenu>> = runCatching {
        val collection = firestore.collection(COLLECTION)
        val query = when (request.queryField) {
            AiMenuHiveQueryField.SCOPE ->
                collection.whereEqualTo(request.queryField.firestoreFieldName(), request.key)
            AiMenuHiveQueryField.ELIGIBILITY ->
                collection.whereArrayContains(request.queryField.firestoreFieldName(), request.key)
        }
        query
            .limit(request.limit)
            .get(
                when (request.source) {
                    AiMenuHiveReadSource.SERVER -> Source.SERVER
                    AiMenuHiveReadSource.CACHE -> Source.CACHE
                }
            )
            .awaitBackendTask()
            .documents
            .mapNotNull { snapshot ->
                AiMenuHiveFirestoreMapper.fromDocument(
                    documentId = snapshot.id,
                    document = snapshot.data.orEmpty()
                )
            }
    }

    override suspend fun upsert(menu: SharedAiMenu): Result<Unit> = runCatching {
        val reference = firestore.collection(COLLECTION).document(menu.semanticHash)
        firestore.runTransaction { transaction ->
            if (transaction.get(reference).exists()) {
                transaction.update(
                    reference,
                    mapOf(
                        FIELD_IDENTITY_VERSION to 3,
                        FIELD_ELIGIBILITY_KEYS to FieldValue.arrayUnion(menu.eligibilityKeys.single()),
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                    )
                )
            } else {
                transaction.set(
                    reference,
                    AiMenuHiveFirestoreMapper.toDocument(menu) + mapOf(
                        FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                    )
                )
            }
        }.awaitBackendTask()
        Unit
    }

    private companion object {
        const val COLLECTION = "sharedAiMenus"
        const val FIELD_IDENTITY_VERSION = "identityVersion"
        const val FIELD_ELIGIBILITY_KEYS = "eligibilityKeys"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}

internal fun AiMenuHiveQueryField.firestoreFieldName(): String = when (this) {
    AiMenuHiveQueryField.SCOPE -> "scopeKey"
    AiMenuHiveQueryField.ELIGIBILITY -> "eligibilityKeys"
}

internal object AiMenuHiveFirestoreMapper {
    fun toDocument(menu: SharedAiMenu): Map<String, Any?> {
        val health = menu.generatedMenu.healthAnalysis
        return mapOf(
            "schemaVersion" to 1,
            "identityVersion" to 3,
            "semanticHash" to menu.semanticHash,
            "semanticKey" to menu.semanticKey,
            "language" to menu.language.name,
            "name" to menu.generatedMenu.name,
            "description" to menu.generatedMenu.description,
            "notes" to menu.generatedMenu.notes,
            "calories" to menu.generatedMenu.calories,
            "healthStatus" to health?.status?.name,
            "healthReason" to health?.reason,
            "healthSuggestion" to health?.suggestion,
            "shoppingProducts" to menu.generatedMenu.shoppingProducts.map { product ->
                mapOf(
                    "key" to product.key,
                    "normalizedName" to product.normalizedName,
                    "displayName" to product.displayName
                )
            },
            "cuisineInspiration" to menu.cuisineInspiration?.name,
            "eligibilityKeys" to menu.eligibilityKeys.sorted(),
            "scopeKey" to requireNotNull(menu.scopeKey)
        )
    }

    fun fromDocument(
        documentId: String,
        document: Map<String, Any?>
    ): SharedAiMenu? {
        if ((document["schemaVersion"] as? Number)?.toInt() != 1) return null
        val identityVersion = (document["identityVersion"] as? Number)?.toInt()
        val scopeKey = (document["scopeKey"] as? String)
            ?.takeIf { it.matches(SEMANTIC_HASH_REGEX) }
        when (identityVersion) {
            null, 2 -> Unit
            3 -> if (scopeKey == null) return null
            else -> return null
        }
        val semanticHash = document["semanticHash"] as? String
        if (semanticHash != documentId || !semanticHash.matches(SEMANTIC_HASH_REGEX)) return null
        val semanticKey = (document["semanticKey"] as? String).nonBlankOrNull() ?: return null
        val language = enumValueOrNull<AppLanguage>(document["language"] as? String) ?: return null
        val name = (document["name"] as? String).nonBlankOrNull() ?: return null
        val description = (document["description"] as? String).nonBlankOrNull() ?: return null
        val notes = document["notes"] as? String ?: return null
        val calories = (document["calories"] as? Number)?.toInt()?.takeIf { it > 0 } ?: return null
        val eligibilityKeys = (document["eligibilityKeys"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter { it.matches(SEMANTIC_HASH_REGEX) }
            ?.toSet()
            ?.takeIf(Set<String>::isNotEmpty)
            ?: return null
        val rawProducts = document["shoppingProducts"] as? List<*> ?: emptyList<Any?>()
        if (rawProducts.size > MAX_PRODUCTS) return null
        val shoppingProducts = rawProducts
            .mapNotNull(::shoppingProduct)
            .distinctBy(ShoppingProduct::key)
        val healthAnalysis = healthAnalysis(document, calories) ?: return null
        val cuisine = (document["cuisineInspiration"] as? String)
            ?.let { enumValueOrNull<CuisineInspiration>(it) }

        return SharedAiMenu(
            semanticHash = semanticHash,
            semanticKey = semanticKey,
            language = language,
            generatedMenu = GeneratedMenu(
                name = name,
                description = description,
                notes = notes,
                calories = calories,
                healthAnalysis = healthAnalysis,
                shoppingProducts = shoppingProducts,
                deduplicationKey = semanticKey
            ),
            cuisineInspiration = cuisine,
            eligibilityKeys = eligibilityKeys,
            scopeKey = scopeKey
        )
    }

    private fun healthAnalysis(document: Map<String, Any?>, calories: Int): HealthAnalysis? {
        val status = enumValueOrNull<HealthStatus>(document["healthStatus"] as? String) ?: return null
        val reason = (document["healthReason"] as? String).nonBlankOrNull() ?: return null
        val suggestion = (document["healthSuggestion"] as? String).nonBlankOrNull() ?: return null
        return HealthAnalysis(status, reason, suggestion, calories)
    }

    private fun shoppingProduct(raw: Any?): ShoppingProduct? {
        val product = raw as? Map<*, *> ?: return null
        val displayName = product["displayName"] as? String ?: return null
        val parsed = ShoppingProduct.fromAi(displayName) ?: return null
        return parsed.takeIf {
            product["key"] == parsed.key && product["normalizedName"] == parsed.normalizedName
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

    private fun String?.nonBlankOrNull(): String? = this?.takeIf(String::isNotBlank)

    private val SEMANTIC_HASH_REGEX = Regex("[a-f0-9]{64}")
    private const val MAX_PRODUCTS = 20
}
