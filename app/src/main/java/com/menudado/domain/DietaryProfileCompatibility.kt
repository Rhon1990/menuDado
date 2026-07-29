package com.menudado.domain

import java.text.Normalizer
import java.util.Locale

fun DietaryProfile.findIngredientConflicts(input: String): List<String> {
    if (input.isBlank() || !hasRestrictions) {
        return emptyList()
    }

    val ingredients = input.split(',', ';', '\n')
        .map(String::trim)
        .filter(String::isNotBlank)

    return ingredients
        .filter { ingredient -> ingredient.conflictsWith(this) }
        .distinctBy(String::normalizedForFoodMatch)
}

fun DietaryProfile.accepts(menu: GeneratedMenu): Boolean {
    val candidateText = buildList {
        add(menu.name)
        add(menu.description)
        add(menu.notes)
        addAll(menu.shoppingProducts.map(ShoppingProduct::displayName))
    }.joinToString(separator = ",")
    return findIngredientConflicts(candidateText).isEmpty()
}

private fun String.conflictsWith(profile: DietaryProfile): Boolean {
    val normalized = normalizedForFoodMatch()
    if (profile.isVegan && normalized.containsAnyFoodTerm(VEGAN_EXCLUDED_TERMS)) {
        return true
    }
    if (profile.hasAllergies && profile.allergens.any { allergen ->
            normalized.containsAnyFoodTerm(allergen.excludedTerms())
        }
    ) {
        return true
    }

    return profile.otherAvoidances
        .split(',', ';', '\n')
        .map(String::normalizedForFoodMatch)
        .filter(String::isNotBlank)
        .any(normalized::containsFoodTerm)
}

private fun String.normalizedForFoodMatch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()

private fun String.containsAnyFoodTerm(terms: Set<String>): Boolean =
    terms.any(::containsFoodTerm)

private fun String.containsFoodTerm(term: String): Boolean {
    if (term.isBlank()) return false
    val normalizedTerm = term.normalizedForFoodMatch()
    if (normalizedTerm.length <= 3) {
        return Regex("""(^|[^a-z0-9])${Regex.escape(normalizedTerm)}([^a-z0-9]|$)""")
            .containsMatchIn(this)
    }
    return contains(normalizedTerm)
}

private fun DietaryAllergen.excludedTerms(): Set<String> = when (this) {
    DietaryAllergen.GLUTEN -> setOf("gluten", "trigo", "cebada", "centeno", "harina", "pan", "pasta")
    DietaryAllergen.DAIRY -> DAIRY_TERMS
    DietaryAllergen.EGG -> EGG_TERMS
    DietaryAllergen.TREE_NUTS -> setOf("frutos secos", "almendra", "nuez", "nueces", "avellana", "pistacho", "anacardo")
    DietaryAllergen.PEANUT -> setOf("cacahuete", "mani")
    DietaryAllergen.SOY -> setOf("soja", "soya", "tofu", "edamame", "tamari")
    DietaryAllergen.FISH -> FISH_TERMS
    DietaryAllergen.SHELLFISH -> SHELLFISH_TERMS
    DietaryAllergen.SESAME -> setOf("sesamo", "tahini")
}

private val DAIRY_TERMS = setOf("lactosa", "lacteo", "lacteos", "leche", "queso", "yogur", "yogurt", "mantequilla", "nata", "crema")
private val EGG_TERMS = setOf("huevo", "huevos", "tortilla francesa")
private val FISH_TERMS = setOf("pescado", "atun", "salmon", "merluza", "bacalao", "sardina", "anchoa")
private val SHELLFISH_TERMS = setOf("marisco", "gamba", "gambas", "langostino", "cangrejo", "mejillon", "almeja")
private val MEAT_TERMS = setOf("pollo", "ternera", "cerdo", "jamon", "pavo", "carne", "chorizo", "panceta", "bacon")
private val VEGAN_EXCLUDED_TERMS =
    DAIRY_TERMS + EGG_TERMS + FISH_TERMS + SHELLFISH_TERMS + MEAT_TERMS + setOf("miel")
