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
                candidateText.containsProhibitedFoodTerm(
                    term = avoided,
                    allowCompatibleContexts = false
                )
            }
        ) {
            add(DietaryProfileViolation.EXPLICIT_AVOIDANCE)
        }
        if (
            audience == MenuAudience.BABY &&
            candidateText.containsAnyProhibitedFoodTerm(
                terms = BABY_UNSAFE_PHRASES,
                allowCompatibleContexts = false
            )
        ) {
            add(DietaryProfileViolation.BABY_SAFETY)
        }
        if (
            audience == MenuAudience.CHILD &&
            candidateText.containsAnyProhibitedFoodTerm(
                terms = CHILD_UNSAFE_PHRASES,
                allowCompatibleContexts = false
            )
        ) {
            add(DietaryProfileViolation.CHILD_SAFETY)
        }
        if (
            isPregnant &&
            candidateText.containsAnyProhibitedFoodTerm(
                terms = PREGNANCY_UNSAFE_PHRASES,
                allowCompatibleContexts = false
            )
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
        .any { avoided ->
            normalized.containsProhibitedFoodTerm(
                term = avoided,
                allowCompatibleContexts = false
            )
        }
}

private fun DietaryProfile.concreteAvoidanceTerms(): List<String> {
    return otherAvoidances
        .split(',', ';', '\n')
        .map(String::normalizedForFoodMatch)
        .map(String::withoutClinicalConditionPrefix)
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

private fun String.withoutClinicalConditionPrefix(): String {
    CLINICAL_CONDITION_TERMS.forEach { condition ->
        INLINE_AVOIDANCE_PREFIXES.forEach { prefix ->
            val combinedPrefix = "$condition $prefix"
            if (startsWith(combinedPrefix)) {
                return removePrefix(combinedPrefix).trim()
            }
        }
    }
    return this
}

private fun String.isClinicalCondition(): Boolean {
    return this in CLINICAL_CONDITION_TERMS
}

private fun String.normalizedForFoodMatch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()

private fun String.containsAnyFoodTerm(terms: Set<String>): Boolean =
    terms.any(::containsFoodTerm)

private fun String.containsAnyProhibitedFoodTerm(
    terms: Set<String>,
    allowCompatibleContexts: Boolean = true
): Boolean = terms.any { term ->
    containsProhibitedFoodTerm(
        term = term,
        allowCompatibleContexts = allowCompatibleContexts
    )
}

private fun String.containsProhibitedFoodTerm(
    term: String,
    allowCompatibleContexts: Boolean = true
): Boolean {
    val normalizedTerm = term.normalizedForFoodMatch()
    if (normalizedTerm.isBlank()) return false
    val termPattern = normalizedTerm.foodTermPattern()
    val compatibleContexts = if (allowCompatibleContexts) {
        COMPATIBLE_FOOD_CONTEXTS_BY_TERM[normalizedTerm].orEmpty()
    } else {
        emptySet()
    }
    val textWithoutCompatibleContexts = compatibleContexts.fold(this) { text, context ->
        text.replace(context, " ")
    }
    val textWithoutExplicitExclusion = EXCLUSION_TEMPLATES.fold(textWithoutCompatibleContexts) { text, template ->
        text.replace(template.format(termPattern).toRegex(), " ")
    }
    return textWithoutExplicitExclusion.containsFoodTerm(normalizedTerm)
}

private fun String.containsFoodTerm(term: String): Boolean {
    if (term.isBlank()) return false
    val normalizedTerm = term.normalizedForFoodMatch()
    return Regex("""(^|[^a-z0-9])${normalizedTerm.foodTermPattern()}([^a-z0-9]|$)""")
        .containsMatchIn(this)
}

private fun String.foodTermPattern(): String {
    val pluralSuffix = if (endsWith("s")) "" else "(?:s|es)?"
    return Regex.escape(this) + pluralSuffix
}

private fun DietaryAllergen.excludedTerms(): Set<String> = when (this) {
    DietaryAllergen.GLUTEN -> setOf(
        "gluten", "trigo", "wheat", "ble", "cebada", "barley", "orge",
        "centeno", "rye", "seigle", "semola", "semolina", "semoule",
        "cuscus", "couscous", "seitan", "avena", "oats", "avoine",
        "espelta", "spelt", "epeautre", "bulgur", "boulgour",
        "malta", "malt",
        "harina", "flour", "farine",
        "pan", "bread", "pain", "pasta", "pates",
        "cerveza", "beer", "biere"
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

private val INLINE_AVOIDANCE_PREFIXES = listOf(
    "sin ",
    "without ",
    "sans "
)

private val EXCLUSION_TEMPLATES = listOf(
    """\bsin\s+%s\b""",
    """\bno\s+%s\b""",
    """\bsans\s+%s\b""",
    """\bwithout\s+%s\b""",
    """\b%s[\s-]+free\b"""
)

private val COMPATIBLE_FOOD_CONTEXTS_BY_TERM = mapOf(
    "pasta" to setOf("pasta sin gluten", "pasta gluten-free", "gluten-free pasta"),
    "pates" to setOf("pates sans gluten"),
    "avena" to setOf("avena sin gluten"),
    "oats" to setOf("gluten-free oats"),
    "avoine" to setOf("avoine sans gluten"),
    "pan" to setOf("pan sin gluten"),
    "bread" to setOf("gluten-free bread"),
    "pain" to setOf("pain sans gluten"),
    "cerveza" to setOf("cerveza sin gluten"),
    "beer" to setOf("gluten-free beer"),
    "biere" to setOf("biere sans gluten"),
    "harina" to setOf("harina de garbanzo", "harina de arroz", "harina de maiz"),
    "flour" to setOf("chickpea flour", "rice flour", "corn flour"),
    "farine" to setOf("farine de pois chiche", "farine de riz", "farine de mais"),
    "crema" to setOf("crema vegetal", "crema de coco"),
    "cream" to setOf("plant cream", "coconut cream"),
    "creme" to setOf("creme vegetale", "creme de coco"),
    "queso" to setOf("queso vegano"),
    "cheese" to setOf("vegan cheese"),
    "fromage" to setOf("fromage vegetal"),
    "leche" to setOf(
        "leche vegetal", "leche de coco", "leche de almendra",
        "leche de avena", "leche de soja"
    ),
    "milk" to setOf(
        "plant milk", "coconut milk", "almond milk", "oat milk", "soy milk"
    ),
    "lait" to setOf(
        "lait vegetal", "lait de coco", "lait d'amande",
        "lait d'avoine", "lait de soja"
    ),
    "mantequilla" to setOf(
        "mantequilla de cacahuete", "mantequilla de almendra",
        "mantequilla de avellana", "mantequilla de coco", "mantequilla de cacao"
    ),
    "butter" to setOf(
        "peanut butter", "almond butter", "hazelnut butter",
        "coconut butter", "cocoa butter"
    ),
    "beurre" to setOf(
        "beurre de cacahuete", "beurre d'amande", "beurre de noisette",
        "beurre de coco", "beurre de cacao"
    ),
    "manteca" to setOf("manteca de cacao"),
    "yogur" to setOf("yogur de soja", "yogur de coco", "yogur vegetal"),
    "yogurt" to setOf("soy yogurt", "coconut yogurt", "plant yogurt"),
    "yaourt" to setOf("yaourt de soja", "yaourt de coco", "yaourt vegetal"),
    "mayonesa" to setOf("mayonesa vegana", "mayonesa sin huevo"),
    "mayonnaise" to setOf(
        "vegan mayonnaise", "egg-free mayonnaise",
        "mayonnaise vegetale", "mayonnaise sans oeuf"
    )
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
    "suero de leche", "proteina de suero", "whey", "whey protein", "lactoserum",
    "mozzarella", "parmesano", "parmesan", "kefir"
)

private val EGG_TERMS = setOf(
    "huevo", "huevos", "egg", "eggs", "oeuf", "oeufs",
    "tortilla francesa", "mayonesa", "mayonnaise",
    "albumina", "albumen"
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
    "anchovy", "anchovies", "anchois", "trucha", "trout", "truite",
    "dorada", "sea bream", "daurade"
)

private val SHELLFISH_TERMS = setOf(
    "marisco", "mariscos", "shellfish", "fruits de mer",
    "gamba", "gambas", "shrimp", "prawn", "crevette", "crevettes",
    "langostino", "langostinos", "cangrejo", "crab", "crabe",
    "mejillon", "mejillones", "mussel", "mussels", "moule", "moules",
    "almeja", "almejas", "clam", "clams", "palourde", "palourdes",
    "langosta", "lobster", "homard", "calamar", "calamares", "squid", "calmar",
    "pulpo", "octopus", "poulpe", "ostra", "ostras", "oyster", "oysters",
    "huitre", "huitres", "vieira", "vieiras", "scallop", "scallops"
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
    "sal", "salt", "sel", "sal anadida", "added salt", "sel ajoute",
    "azucar", "sugar", "sucre", "azucar anadida", "added sugar", "sucre ajoute",
    "edulcorante", "sweetener", "edulcorant",
    "salsa de soja", "soy sauce", "sauce soja", "tamari",
    "cubito de caldo", "bouillon cube", "cube de bouillon",
    "alcohol", "vino sin cocinar", "uncooked wine", "vin non cuit",
    "cerveza", "beer", "biere",
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
    "cerveza", "beer", "biere",
    "frutos secos enteros", "whole nuts", "noix entieres",
    "uvas enteras", "whole grapes", "raisins entiers",
    "palomitas", "popcorn",
    "salchicha en rodajas", "sliced sausage", "saucisse en rondelles"
)

private val PREGNANCY_UNSAFE_PHRASES = setOf(
    "alcohol", "vino sin cocinar", "uncooked wine", "vin non cuit",
    "cerveza", "beer", "biere",
    "leche no pasteurizada", "unpasteurized milk", "lait non pasteurise",
    "queso no pasteurizado", "unpasteurized cheese", "fromage non pasteurise",
    "huevo crudo", "raw egg", "oeuf cru",
    "huevo poco cocinado", "huevo poco hecho", "undercooked egg", "oeuf peu cuit",
    "carne cruda", "raw meat", "viande crue",
    "carne poco cocinada", "carne poco hecha", "undercooked meat", "viande peu cuite",
    "pescado crudo", "raw fish", "poisson cru",
    "pescado poco cocinado", "undercooked fish", "poisson peu cuit",
    "sushi", "sashimi", "ceviche", "carpaccio", "steak tartar",
    "pez espada", "emperador", "atun rojo", "tiburon", "lucio",
    "swordfish", "bluefin tuna", "shark", "pike",
    "espadon", "thon rouge", "requin", "brochet"
)
