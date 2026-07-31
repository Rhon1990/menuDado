package com.menudado.ui

import com.menudado.domain.CuisineInspiration
import com.menudado.domain.DietaryProfile
import com.menudado.domain.DietaryProfileViolation
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import com.menudado.domain.compatibilityWith
import java.text.Normalizer
import java.util.Locale

internal sealed interface MenuCatalogScope {
    data class Audience(val audience: MenuAudience) : MenuCatalogScope
    data object Favorites : MenuCatalogScope
}

internal enum class MenuCatalogDietaryNeed {
    NONE,
    PREGNANCY,
    VEGAN,
    ALLERGIES,
    FULL_PROFILE
}

internal data class MenuCatalogFilters(
    val query: String = "",
    val favoriteAudience: MenuAudience? = null,
    val dietaryNeed: MenuCatalogDietaryNeed = MenuCatalogDietaryNeed.NONE,
    val favoritesOnly: Boolean = false,
    val healthyOnly: Boolean = false
)

internal data class MenuCatalogSearchLabels(
    val cuisineLabels: Map<CuisineInspiration, String> = emptyMap(),
    val mealTypeLabels: Map<MealType, String> = emptyMap(),
    val audienceLabels: Map<MenuAudience, String> = emptyMap(),
    val healthStatusLabels: Map<HealthStatus, String> = emptyMap(),
    val calorieLabels: Map<Int, String> = emptyMap()
)

internal fun defaultMenuCatalogFilters(): MenuCatalogFilters = MenuCatalogFilters()

internal fun menuCatalogActiveFilterCount(filters: MenuCatalogFilters): Int =
    listOf(
        filters.favoriteAudience != null,
        filters.dietaryNeed != MenuCatalogDietaryNeed.NONE,
        filters.favoritesOnly,
        filters.healthyOnly
    ).count { it }

internal fun menuCatalogFiltersAfterFavoriteAudienceSelected(
    filters: MenuCatalogFilters,
    audience: MenuAudience?
): MenuCatalogFilters = filters.copy(
    favoriteAudience = audience,
    dietaryNeed = if (
        filters.dietaryNeed == MenuCatalogDietaryNeed.PREGNANCY &&
        audience != MenuAudience.ADULT
    ) {
        MenuCatalogDietaryNeed.NONE
    } else {
        filters.dietaryNeed
    }
)

internal fun menuCatalogFilteredMenus(
    menus: List<FoodMenu>,
    scope: MenuCatalogScope,
    filters: MenuCatalogFilters,
    dietaryProfiles: Map<MenuAudience, DietaryProfile>,
    searchLabels: MenuCatalogSearchLabels
): List<FoodMenu> {
    val scopedMenus = when (scope) {
        is MenuCatalogScope.Audience -> menus
            .filter { it.audience == scope.audience }
            .sortedByDescending(FoodMenu::createdAt)
        MenuCatalogScope.Favorites -> menus
            .filter(FoodMenu::isFavorite)
            .sortedWith(
                compareByDescending<FoodMenu> { it.favoritedAt ?: it.createdAt }
                    .thenByDescending(FoodMenu::createdAt)
            )
    }
    val normalizedQuery = filters.query.normalizedCatalogText()

    return scopedMenus.filter { menu ->
        (normalizedQuery.isBlank() ||
            menu.catalogSearchText(
                labels = searchLabels,
                includeAudience = scope is MenuCatalogScope.Favorites
            ).contains(normalizedQuery)) &&
            (scope !is MenuCatalogScope.Favorites ||
                filters.favoriteAudience == null ||
                menu.audience == filters.favoriteAudience) &&
            menu.matchesDietaryNeed(filters.dietaryNeed, dietaryProfiles) &&
            (!filters.favoritesOnly || menu.isFavorite) &&
            (!filters.healthyOnly || menu.healthAnalysis?.status == HealthStatus.HEALTHY)
    }
}

internal fun menuCatalogDietaryNeedEnabled(
    need: MenuCatalogDietaryNeed,
    scope: MenuCatalogScope,
    favoriteAudience: MenuAudience?,
    profiles: Map<MenuAudience, DietaryProfile>
): Boolean {
    val audiences = when (scope) {
        is MenuCatalogScope.Audience -> listOf(scope.audience)
        MenuCatalogScope.Favorites -> favoriteAudience?.let(::listOf) ?: profiles.keys.toList()
    }

    return when (need) {
        MenuCatalogDietaryNeed.NONE,
        MenuCatalogDietaryNeed.VEGAN -> true
        MenuCatalogDietaryNeed.PREGNANCY -> audiences == listOf(MenuAudience.ADULT)
        MenuCatalogDietaryNeed.ALLERGIES -> audiences.any { audience ->
            profiles[audience]?.let { profile ->
                profile.isEnabled && profile.hasAllergies && profile.allergens.isNotEmpty()
            } == true
        }
        MenuCatalogDietaryNeed.FULL_PROFILE -> audiences.any { audience ->
            profiles[audience]?.let { profile ->
                profile.isEnabled && profile.hasRestrictions
            } == true
        }
    }
}

private fun FoodMenu.catalogSearchText(
    labels: MenuCatalogSearchLabels,
    includeAudience: Boolean
): String = buildList {
    add(name)
    add(description)
    add(notes)
    cuisineInspiration?.let { add(labels.cuisineLabels[it].orEmpty()) }
    add(labels.mealTypeLabels[mealType].orEmpty())
    if (includeAudience) add(labels.audienceLabels[audience].orEmpty())
    add(
        labels.healthStatusLabels[
            healthAnalysis?.status ?: HealthStatus.UNKNOWN
        ].orEmpty()
    )
    if (healthAnalysis != null) {
        calories?.let { add(labels.calorieLabels[it].orEmpty()) }
    }
    addAll(shoppingProducts.map { it.displayName })
}.joinToString(separator = " ").normalizedCatalogText()

private fun String.normalizedCatalogText(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .lowercase(Locale.ROOT)
        .trim()

private fun FoodMenu.matchesDietaryNeed(
    need: MenuCatalogDietaryNeed,
    profiles: Map<MenuAudience, DietaryProfile>
): Boolean {
    val profile = profiles[audience] ?: DietaryProfile(isEnabled = false)
    return when (need) {
        MenuCatalogDietaryNeed.NONE -> true
        MenuCatalogDietaryNeed.PREGNANCY ->
            DietaryProfile(isPregnant = true)
                .compatibilityWith(this, MenuAudience.ADULT)
                .violations
                .contains(DietaryProfileViolation.PREGNANCY_SAFETY)
                .not()
        MenuCatalogDietaryNeed.VEGAN ->
            DietaryProfile(isVegan = true)
                .compatibilityWith(this, audience)
                .violations
                .contains(DietaryProfileViolation.VEGAN)
                .not()
        MenuCatalogDietaryNeed.ALLERGIES ->
            profile.hasAllergies &&
                profile.allergens.isNotEmpty() &&
                DietaryProfile(
                    hasAllergies = true,
                    allergens = profile.allergens
                ).compatibilityWith(this, audience)
                    .violations
                    .contains(DietaryProfileViolation.ALLERGEN)
                    .not()
        MenuCatalogDietaryNeed.FULL_PROFILE ->
            profile.isEnabled && profile.compatibilityWith(this, audience).isCompatible
    }
}
