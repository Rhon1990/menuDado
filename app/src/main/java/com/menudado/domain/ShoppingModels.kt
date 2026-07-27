package com.menudado.domain

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

data class ShoppingProduct(
    val key: String,
    val normalizedName: String,
    val displayName: String
) {
    companion object {
        private const val MAX_NAME_LENGTH = 80

        fun fromAi(rawName: String): ShoppingProduct? {
            if (
                rawName.isBlank() ||
                rawName.length > MAX_NAME_LENGTH ||
                rawName.contains('\n') ||
                rawName.contains('\r')
            ) {
                return null
            }

            val displayName = rawName
                .trim()
                .replace(Regex("""\s+"""), " ")
                .replaceFirstChar { character ->
                    if (character.isLowerCase()) {
                        character.titlecase(Locale.getDefault())
                    } else {
                        character.toString()
                    }
                }
            val normalizedName = Normalizer
                .normalize(displayName, Normalizer.Form.NFD)
                .replace(Regex("""\p{M}+"""), "")
                .lowercase(Locale.ROOT)
            val key = MessageDigest
                .getInstance("SHA-256")
                .digest(normalizedName.toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { byte -> "%02x".format(byte) }

            return ShoppingProduct(
                key = key,
                normalizedName = normalizedName,
                displayName = displayName
            )
        }
    }
}

data class MenuAiDetails(
    val healthAnalysis: HealthAnalysis,
    val shoppingProducts: List<ShoppingProduct>
)

data class MarketProduct(
    val key: String,
    val displayName: String,
    val sourceMenuIds: Set<Long>,
    val isPurchased: Boolean
)

fun aggregateMarketProducts(
    menus: List<FoodMenu>,
    purchasedProductKeys: Set<String>
): List<MarketProduct> {
    return menus
        .asSequence()
        .flatMap { menu ->
            menu.shoppingProducts
                .asSequence()
                .filter { product -> product.key in menu.activeShoppingProductKeys }
                .map { product -> menu.id to product }
        }
        .groupBy { (_, product) -> product.key }
        .map { (key, contributions) ->
            MarketProduct(
                key = key,
                displayName = contributions.first().second.displayName,
                sourceMenuIds = contributions.mapTo(linkedSetOf()) { it.first },
                isPurchased = key in purchasedProductKeys
            )
        }
        .sortedWith(compareBy<MarketProduct> { it.isPurchased }.thenBy { it.displayName.lowercase(Locale.ROOT) })
}

internal fun String.parseShoppingProducts(): List<ShoppingProduct> {
    val array = Regex(
        pattern = """"shopping_products"\s*:\s*\[(.*?)]""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).find(this)?.groupValues?.getOrNull(1) ?: return emptyList()

    return Regex(""""((?:\\.|[^"\\])*)"""")
        .findAll(array)
        .mapNotNull { match -> ShoppingProduct.fromAi(match.groupValues[1].unescapeShoppingJson()) }
        .distinctBy(ShoppingProduct::key)
        .take(30)
        .toList()
}

private fun String.unescapeShoppingJson(): String {
    val decoded = StringBuilder(length)
    var index = 0
    while (index < length) {
        val character = this[index]
        if (character == '\\' && index + 1 < length) {
            decoded.append(
                when (val escaped = this[index + 1]) {
                    '"' -> '"'
                    '\\' -> '\\'
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    else -> escaped
                }
            )
            index += 2
        } else {
            decoded.append(character)
            index += 1
        }
    }
    return decoded.toString()
}
