package com.menudado.domain

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

data class AiMenuSemanticIdentity(
    val canonicalKey: String,
    val semanticHash: String
)

data class AiMenuConceptSignature(
    val family: String,
    val concepts: Set<String>
)

object AiMenuHiveIdentity {
    private const val PROFILE_SCHEMA_VERSION = 1
    private const val SIMILARITY_THRESHOLD = 0.80
    private val componentAliases = mapOf(
        "spaghetti" to "pasta",
        "espagueti" to "pasta",
        "espaguetis" to "pasta",
        "macaroni" to "pasta",
        "macarron" to "pasta",
        "macarrones" to "pasta",
        "toast" to "toast",
        "toasts" to "toast",
        "tostada" to "toast",
        "tostadas" to "toast",
        "tartine" to "toast",
        "tartines" to "toast",
        "salad" to "salad",
        "salads" to "salad",
        "ensalada" to "salad",
        "ensaladas" to "salad",
        "salade" to "salad",
        "salades" to "salad",
        "soup" to "soup",
        "soups" to "soup",
        "sopa" to "soup",
        "sopas" to "soup",
        "soupe" to "soup",
        "soupes" to "soup",
        "stew" to "stew",
        "stews" to "stew",
        "guiso" to "stew",
        "guisos" to "stew",
        "estofado" to "stew",
        "estofados" to "stew",
        "ragout" to "stew",
        "ragouts" to "stew",
        "bowl" to "bowl",
        "bowls" to "bowl",
        "bol" to "bowl",
        "boles" to "bowl",
        "bols" to "bowl",
        "sandwich" to "sandwich",
        "sandwiches" to "sandwich",
        "bocadillo" to "sandwich",
        "bocadillos" to "sandwich",
        "omelette" to "omelette",
        "omelettes" to "omelette",
        "omelet" to "omelette",
        "omelets" to "omelette",
        "tortilla francesa" to "omelette",
        "tomate" to "tomato",
        "tomates" to "tomato",
        "lenteja" to "lentils",
        "lentejas" to "lentils",
        "lentil" to "lentils",
        "vegetable" to "vegetables",
        "verdura" to "vegetables",
        "verduras" to "vegetables",
        "avocados" to "avocado",
        "aguacate" to "avocado",
        "aguacates" to "avocado",
        "avocat" to "avocado",
        "avocats" to "avocado",
        "eggs" to "egg",
        "huevo" to "egg",
        "huevos" to "egg",
        "oeuf" to "egg",
        "oeufs" to "egg",
        "pollo" to "chicken",
        "pollos" to "chicken",
        "poulet" to "chicken",
        "poulets" to "chicken",
        "arroz" to "rice",
        "riz" to "rice",
        "chickpea" to "chickpeas",
        "garbanzo" to "chickpeas",
        "garbanzos" to "chickpeas",
        "pois chiche" to "chickpeas",
        "pois chiches" to "chickpeas",
        "potatoes" to "potato",
        "patata" to "potato",
        "patatas" to "potato",
        "pomme de terre" to "potato",
        "pommes de terre" to "potato",
        "salsa" to "sauce",
        "salsa de tomate" to "sauce"
    )
    private val preparationAliases = mapOf(
        "mix" to "mixed",
        "mixing" to "mixed",
        "toss" to "mixed",
        "tossed" to "mixed",
        "mezcla" to "mixed",
        "mezclado" to "mixed",
        "mezclada" to "mixed",
        "melange" to "mixed",
        "melangee" to "mixed",
        "assembled" to "assembled",
        "mounted" to "assembled",
        "montado" to "assembled",
        "montada" to "assembled",
        "monte" to "assembled",
        "montee" to "assembled",
        "fried" to "fried",
        "frito" to "fried",
        "frita" to "fried",
        "frit" to "fried",
        "frite" to "fried",
        "baked" to "baked",
        "horneado" to "baked",
        "horneada" to "baked",
        "au four" to "baked",
        "grilled" to "grilled",
        "a la plancha" to "grilled",
        "plancha" to "grilled",
        "parrilla" to "grilled",
        "grille" to "grilled",
        "grillee" to "grilled",
        "boiled" to "boiled",
        "cocido" to "boiled",
        "cocida" to "boiled",
        "hervido" to "boiled",
        "hervida" to "boiled",
        "bouilli" to "boiled",
        "bouillie" to "boiled",
        "stewed" to "stewed",
        "guisado" to "stewed",
        "guisada" to "stewed",
        "estofado" to "stewed",
        "estofada" to "stewed",
        "mijote" to "stewed",
        "mijotee" to "stewed",
        "roasted" to "roasted",
        "asado" to "roasted",
        "asada" to "roasted",
        "roti" to "roasted",
        "rotie" to "roasted"
    )
    private val conceptAliases = componentAliases + preparationAliases + mapOf(
        "stewed" to "stew",
        "guisado" to "stew",
        "guisada" to "stew",
        "estofado" to "stew",
        "estofada" to "stew",
        "mijote" to "stew",
        "mijotee" to "stew"
    )

    fun from(language: AppLanguage, rawKey: String?): AiMenuSemanticIdentity? {
        val parts = canonicalParts(rawKey) ?: return null
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

    fun conceptSignature(rawKey: String?): AiMenuConceptSignature? {
        val rawParts = rawKey.orEmpty().split('|')
        if (rawParts.size != 3) return null
        val family = canonicalSegment(rawParts.first(), ::canonicalComponent)
            .takeIf(String::isNotBlank)
            ?: return null
        val concepts = rawParts
            .flatMap { part -> part.split('+', ',') }
            .map(::canonicalConcept)
            .filter(String::isNotBlank)
            .toSet()
            .takeIf(Set<String>::isNotEmpty)
            ?: return null
        return AiMenuConceptSignature(
            family = family,
            concepts = concepts
        )
    }

    fun similarity(firstKey: String?, secondKey: String?): Double {
        val first = conceptSignature(firstKey) ?: return 0.0
        val second = conceptSignature(secondKey) ?: return 0.0
        if (first.concepts == second.concepts) return 1.0
        if (first.family != second.family) return 0.0
        val union = first.concepts union second.concepts
        if (union.isEmpty()) return 0.0
        return (first.concepts intersect second.concepts).size.toDouble() / union.size
    }

    fun areSimilar(firstKey: String?, secondKey: String?): Boolean =
        similarity(firstKey, secondKey) >= SIMILARITY_THRESHOLD

    private fun canonicalParts(rawKey: String?): List<String>? {
        val rawParts = rawKey.orEmpty().split('|')
        if (rawParts.size != 3) return null
        return listOf(
            canonicalSegment(rawParts[0], ::canonicalComponent),
            canonicalSegment(rawParts[1], ::canonicalComponent),
            canonicalSegment(rawParts[2], ::canonicalPreparation)
        ).takeIf { parts -> parts.none(String::isBlank) }
    }

    private fun canonicalSegment(
        value: String,
        canonicalizer: (String) -> String
    ): String = value
        .split('+', ',')
        .map(canonicalizer)
        .filter(String::isNotBlank)
        .distinct()
        .sorted()
        .joinToString("+")

    private fun canonicalComponent(value: String): String {
        val normalized = value.normalized()
        return componentAliases[normalized] ?: normalized
    }

    private fun canonicalPreparation(value: String): String {
        val normalized = value.normalized()
        return preparationAliases[normalized] ?: canonicalComponent(normalized)
    }

    private fun canonicalConcept(value: String): String {
        val normalized = value.normalized()
        return conceptAliases[normalized] ?: normalized
    }

    private fun String.normalized(): String = Normalizer.normalize(
        lowercase(Locale.ROOT).replace("œ", "oe").replace("æ", "ae"),
        Normalizer.Form.NFD
    )
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9 ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
