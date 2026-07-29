package com.menudado.domain

import java.text.Normalizer
import java.util.Locale

enum class DietaryProfileViolation {
    VEGAN,
    ALLERGEN,
    EXPLICIT_AVOIDANCE,
    BABY_SAFETY,
    CHILD_SAFETY,
    PREGNANCY_SAFETY
}

data class DietaryProfileCompatibility(
    val violations: Set<DietaryProfileViolation>
) {
    val isCompatible: Boolean
        get() = violations.isEmpty()
}

fun DietaryProfile.findIngredientConflicts(input: String): List<String> {
    if (input.isBlank() || !hasRestrictions) {
        return emptyList()
    }

    return input.split(',', ';', '\n')
        .map(String::trim)
        .filter(String::isNotBlank)
        .filter { ingredient -> ingredient.conflictsWith(this) }
        .distinctBy(String::normalizedForFoodMatch)
}

fun DietaryProfile.compatibilityWith(
    menu: GeneratedMenu,
    audience: MenuAudience = MenuAudience.ADULT
): DietaryProfileCompatibility {
    val candidateText = menu.searchableText().normalizedForFoodMatch()
    val violations = buildSet {
        if (isVegan && candidateText.containsAnyProhibitedFoodTerm(VEGAN_EXCLUDED_TERMS)) {
            add(DietaryProfileViolation.VEGAN)
        }
        if (
            hasAllergies &&
            allergens.any { allergen ->
                candidateText.containsAnyProhibitedFoodTerm(allergen.excludedTerms())
            }
        ) {
            add(DietaryProfileViolation.ALLERGEN)
        }
        if (
            concreteAvoidanceTerms().any { avoided ->
                candidateText.containsProhibitedFoodTerm(avoided)
            }
        ) {
            add(DietaryProfileViolation.EXPLICIT_AVOIDANCE)
        }
        if (
            audience == MenuAudience.BABY &&
            candidateText.containsAnyFoodTerm(BABY_UNSAFE_PHRASES)
        ) {
            add(DietaryProfileViolation.BABY_SAFETY)
        }
        if (
            audience == MenuAudience.CHILD &&
            candidateText.containsAnyFoodTerm(CHILD_UNSAFE_PHRASES)
        ) {
            add(DietaryProfileViolation.CHILD_SAFETY)
        }
        if (
            isPregnant &&
            candidateText.containsAnyFoodTerm(PREGNANCY_UNSAFE_PHRASES)
        ) {
            add(DietaryProfileViolation.PREGNANCY_SAFETY)
        }
    }
    return DietaryProfileCompatibility(violations)
}

fun DietaryProfile.accepts(
    menu: GeneratedMenu,
    audience: MenuAudience = MenuAudience.ADULT
): Boolean = compatibilityWith(menu, audience).isCompatible

private fun GeneratedMenu.searchableText(): String = buildList {
    add(name)
    add(description)
    add(notes)
    addAll(shoppingProducts.map(ShoppingProduct::displayName))
}.joinToString(separator = ",")

private fun String.conflictsWith(profile: DietaryProfile): Boolean {
    val normalized = normalizedForFoodMatch()
    if (profile.isVegan && normalized.containsAnyProhibitedFoodTerm(VEGAN_EXCLUDED_TERMS)) {
        return true
    }
    if (
        profile.hasAllergies &&
        profile.allergens.any { allergen ->
            normalized.containsAnyProhibitedFoodTerm(allergen.excludedTerms())
        }
    ) {
        return true
    }
    return profile.concreteAvoidanceTerms()
        .any(normalized::containsProhibitedFoodTerm)
}

private fun DietaryProfile.concreteAvoidanceTerms(): List<String> {
    return otherAvoidances
        .split(',', ';', '\n')
        .map(String::normalizedForFoodMatch)
        .map(String::withoutAvoidancePrefix)
        .filter(String::isNotBlank)
        .filterNot(String::isClinicalCondition)
        .distinct()
}

private fun String.withoutAvoidancePrefix(): String {
    return AVOIDANCE_PREFIXES.fold(this) { current, prefix ->
        current.removePrefix(prefix)
    }.trim()
}

private fun String.isClinicalCondition(): Boolean {
    return CLINICAL_CONDITION_TERMS.any(::containsFoodTerm)
}

private fun String.normalizedForFoodMatch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()

private fun String.containsAnyFoodTerm(terms: Set<String>): Boolean =
    terms.any(::containsFoodTerm)

private fun String.containsAnyProhibitedFoodTerm(terms: Set<String>): Boolean =
    terms.any(::containsProhibitedFoodTerm)

private fun String.containsProhibitedFoodTerm(term: String): Boolean {
    val normalizedTerm = term.normalizedForFoodMatch()
    if (normalizedTerm.isBlank()) return false
    val textWithoutCompatibleContexts = COMPATIBLE_FOOD_CONTEXTS.fold(this) { text, context ->
        text.replace(context, " ")
    }
    val textWithoutExplicitExclusion = EXCLUSION_TEMPLATES.fold(textWithoutCompatibleContexts) { text, template ->
        text.replace(template.format(Regex.escape(normalizedTerm)).toRegex(), " ")
    }
    return textWithoutExplicitExclusion.containsFoodTerm(normalizedTerm)
}

private fun String.containsFoodTerm(term: String): Boolean {
    if (term.isBlank()) return false
    val normalizedTerm = term.normalizedForFoodMatch()
    return Regex("""(^|[^a-z0-9])${Regex.escape(normalizedTerm)}([^a-z0-9]|$)""")
        .containsMatchIn(this)
}

private fun DietaryAllergen.excludedTerms(): Set<String> = when (this) {
    DietaryAllergen.GLUTEN -> setOf(
        "gluten", "trigo", "wheat", "ble", "cebada", "barley", "orge",
        "centeno", "rye", "seigle", "semola", "semolina", "semoule",
        "cuscus", "couscous", "seitan", "harina", "flour", "farine",
        "pan", "bread", "pain", "pasta", "pates"
    )
    DietaryAllergen.DAIRY -> DAIRY_TERMS
    DietaryAllergen.EGG -> EGG_TERMS
    DietaryAllergen.TREE_NUTS -> TREE_NUT_TERMS
    DietaryAllergen.PEANUT -> setOf(
        "cacahuete", "cacahuetes", "mani", "peanut", "peanuts",
        "arachide", "arachides"
    )
    DietaryAllergen.SOY -> SOY_TERMS
    DietaryAllergen.FISH -> FISH_TERMS
    DietaryAllergen.SHELLFISH -> SHELLFISH_TERMS
    DietaryAllergen.SESAME -> setOf(
        "sesamo", "sesame", "tahini", "aceite de sesamo", "sesame oil",
        "huile de sesame"
    )
}

private val AVOIDANCE_PREFIXES = listOf(
    "sin ",
    "no ",
    "evitar ",
    "evita ",
    "without ",
    "avoid ",
    "sans ",
    "eviter "
)

private val EXCLUSION_TEMPLATES = listOf(
    """\bsin\s+%s\b""",
    """\bno\s+%s\b""",
    """\bsans\s+%s\b""",
    """\bwithout\s+%s\b""",
    """\b%s[\s-]+free\b"""
)

private val COMPATIBLE_FOOD_CONTEXTS = setOf(
    "pasta sin gluten",
    "pasta gluten-free",
    "gluten-free pasta",
    "pates sans gluten",
    "pan sin gluten",
    "gluten-free bread",
    "pain sans gluten",
    "harina de garbanzo",
    "chickpea flour",
    "farine de pois chiche",
    "harina de arroz",
    "rice flour",
    "farine de riz",
    "harina de maiz",
    "corn flour",
    "farine de mais",
    "crema vegetal",
    "plant cream",
    "creme vegetale",
    "queso vegano",
    "vegan cheese",
    "fromage vegetal",
    "leche vegetal",
    "plant milk",
    "lait vegetal"
)

private val CLINICAL_CONDITION_TERMS = setOf(
    "diabetes",
    "diabetico",
    "diabetica",
    "diabetic",
    "diabete",
    "diabetique",
    "hipertension",
    "hipertenso",
    "hipertensa",
    "hypertension",
    "high blood pressure",
    "hypertension arterielle"
)

private val DAIRY_TERMS = setOf(
    "lactosa", "lactose", "lacteo", "lacteos", "dairy", "laitier",
    "leche", "milk", "lait", "queso", "cheese", "fromage",
    "yogur", "yogurt", "yaourt", "mantequilla", "butter", "beurre",
    "nata", "crema", "cream", "creme", "caseina", "casein", "caseine",
    "suero de leche", "whey", "lactoserum"
)

private val EGG_TERMS = setOf(
    "huevo", "huevos", "egg", "eggs", "oeuf", "oeufs",
    "tortilla francesa", "mayonesa", "mayonnaise"
)

private val TREE_NUT_TERMS = setOf(
    "frutos secos", "almendra", "almendras", "nuez", "nueces",
    "avellana", "avellanas", "pistacho", "pistachos", "anacardo", "anacardos",
    "pecana", "pecanas", "macadamia", "nuez de brasil",
    "tree nuts", "almond", "almonds", "walnut", "walnuts", "hazelnut",
    "hazelnuts", "pistachio", "pistachios", "cashew", "cashews", "pecan",
    "pecans", "brazil nut", "noix", "amande", "amandes", "noisette",
    "noisettes", "pistache", "pistaches", "noix de cajou"
)

private val SOY_TERMS = setOf(
    "soja", "soya", "soy", "tofu", "edamame", "tamari", "miso", "tempeh",
    "lecitina de soja", "soy lecithin", "lecithine de soja"
)

private val FISH_TERMS = setOf(
    "pescado", "fish", "poisson", "atun", "tuna", "thon",
    "salmon", "saumon", "merluza", "hake", "merlu", "bacalao", "cod", "morue",
    "sardina", "sardinas", "sardine", "sardines", "anchoa", "anchoas",
    "anchovy", "anchovies", "anchois"
)

private val SHELLFISH_TERMS = setOf(
    "marisco", "mariscos", "shellfish", "fruits de mer",
    "gamba", "gambas", "shrimp", "prawn", "crevette", "crevettes",
    "langostino", "langostinos", "cangrejo", "crab", "crabe",
    "mejillon", "mejillones", "mussel", "mussels", "moule", "moules",
    "almeja", "almejas", "clam", "clams", "palourde", "palourdes",
    "langosta", "lobster", "homard"
)

private val MEAT_TERMS = setOf(
    "pollo", "chicken", "poulet", "ternera", "beef", "boeuf",
    "cerdo", "pork", "porc", "jamon", "ham", "jambon",
    "pavo", "turkey", "dinde", "carne", "meat", "viande",
    "cordero", "lamb", "agneau", "conejo", "rabbit", "lapin",
    "pato", "duck", "canard", "chorizo", "panceta", "bacon",
    "manteca", "lard", "saindoux", "gelatina", "gelatin", "gelatine"
)

private val VEGAN_EXCLUDED_TERMS =
    DAIRY_TERMS + EGG_TERMS + FISH_TERMS + SHELLFISH_TERMS + MEAT_TERMS +
        setOf("miel", "honey")

private val BABY_UNSAFE_PHRASES = setOf(
    "miel", "honey",
    "sal anadida", "added salt", "sel ajoute",
    "azucar anadida", "added sugar", "sucre ajoute",
    "edulcorante", "sweetener", "edulcorant",
    "alcohol", "vino sin cocinar", "uncooked wine", "vin non cuit",
    "frutos secos enteros", "whole nuts", "noix entieres",
    "palomitas", "popcorn",
    "uvas enteras", "whole grapes", "raisins entiers",
    "tomates cherry enteros", "whole cherry tomatoes", "tomates cerises entieres",
    "salchicha en rodajas", "sliced sausage", "saucisse en rondelles",
    "trozos grandes de carne", "large chunks of meat", "gros morceaux de viande",
    "trozos grandes de queso", "large chunks of cheese", "gros morceaux de fromage",
    "huesos", "bones", "aretes",
    "huevo crudo", "raw egg", "oeuf cru",
    "carne cruda", "raw meat", "viande crue",
    "pescado crudo", "raw fish", "poisson cru"
)

private val CHILD_UNSAFE_PHRASES = setOf(
    "alcohol", "vino sin cocinar", "uncooked wine", "vin non cuit",
    "frutos secos enteros", "whole nuts", "noix entieres",
    "uvas enteras", "whole grapes", "raisins entiers",
    "palomitas", "popcorn"
)

private val PREGNANCY_UNSAFE_PHRASES = setOf(
    "alcohol", "vino sin cocinar", "uncooked wine", "vin non cuit",
    "leche no pasteurizada", "unpasteurized milk", "lait non pasteurise",
    "queso no pasteurizado", "unpasteurized cheese", "fromage non pasteurise",
    "huevo crudo", "raw egg", "oeuf cru",
    "carne cruda", "raw meat", "viande crue",
    "pescado crudo", "raw fish", "poisson cru",
    "sushi", "sashimi", "ceviche", "carpaccio", "steak tartar",
    "pez espada", "emperador", "atun rojo", "tiburon", "lucio",
    "swordfish", "bluefin tuna", "shark", "pike",
    "espadon", "thon rouge", "requin", "brochet"
)
