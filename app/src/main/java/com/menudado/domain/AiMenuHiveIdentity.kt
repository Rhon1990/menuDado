package com.menudado.domain

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

data class AiMenuSemanticIdentity(
    val canonicalKey: String,
    val semanticHash: String
)

object AiMenuHiveIdentity {
    private const val PROFILE_SCHEMA_VERSION = 1
    private val aliases = mapOf(
        "spaghetti" to "pasta",
        "espagueti" to "pasta",
        "espaguetis" to "pasta",
        "macaroni" to "pasta",
        "macarron" to "pasta",
        "macarrones" to "pasta",
        "tomate" to "tomato",
        "tomates" to "tomato",
        "lenteja" to "lentils",
        "lentejas" to "lentils",
        "salsa" to "sauce",
        "salsa de tomate" to "sauce"
    )

    fun from(language: AppLanguage, rawKey: String?): AiMenuSemanticIdentity? {
        val rawParts = rawKey.orEmpty().split('|')
        if (rawParts.size != 3) return null
        val parts = listOf(
            canonicalComponent(rawParts[0]),
            canonicalIngredients(rawParts[1]),
            canonicalComponent(rawParts[2])
        )
        if (parts.any(String::isBlank)) return null
        val canonicalKey = parts.joinToString("|")
        return AiMenuSemanticIdentity(
            canonicalKey = canonicalKey,
            semanticHash = sha256("${language.name}|$canonicalKey")
        )
    }

    fun eligibilityKey(
        language: AppLanguage,
        mealType: MealType,
        audience: MenuAudience,
        profile: DietaryProfile
    ): String {
        val allergens = if (profile.hasAllergies) {
            profile.allergens.map(DietaryAllergen::name).sorted().joinToString(",")
        } else {
            ""
        }
        val raw = listOf(
            "v$PROFILE_SCHEMA_VERSION",
            language.name,
            mealType.name,
            audience.name,
            profile.ageRange.normalized(),
            profile.isPregnant.toString(),
            profile.isVegan.toString(),
            allergens,
            profile.otherAvoidances.normalized()
        ).joinToString("|")
        return sha256(raw)
    }

    private fun canonicalComponent(value: String): String {
        val normalized = value.normalized()
        return aliases[normalized] ?: normalized
    }

    private fun canonicalIngredients(value: String): String = value
        .split('+', ',')
        .map(::canonicalComponent)
        .filter(String::isNotBlank)
        .distinct()
        .sorted()
        .joinToString("+")

    private fun String.normalized(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9 ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
