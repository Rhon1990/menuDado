package com.menudado.ui

import com.menudado.domain.CuisineInspiration
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import com.menudado.domain.ShoppingProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuCatalogFiltersTest {
    private val adultMenu = FoodMenu(
        id = 1,
        name = "Tostada mediterránea",
        mealType = MealType.BREAKFAST,
        audience = MenuAudience.ADULT,
        description = "Pan, tomate y queso",
        notes = "Servir templada",
        isFavorite = true,
        favoritedAt = 10,
        createdAt = 10,
        cuisineInspiration = CuisineInspiration.MEDITERRANEAN,
        shoppingProducts = listOf(requireNotNull(ShoppingProduct.fromAi("Aguacate")))
    )
    private val childMenu = FoodMenu(
        id = 2,
        name = "Arroz suave",
        mealType = MealType.LUNCH,
        audience = MenuAudience.CHILD,
        description = "Arroz con verduras",
        isFavorite = true,
        favoritedAt = 20,
        createdAt = 20
    )
    private val babyMenu = FoodMenu(
        id = 3,
        name = "Puré de calabaza",
        mealType = MealType.DINNER,
        audience = MenuAudience.BABY,
        description = "Calabaza y arroz",
        createdAt = 30
    )

    @Test
    fun `audience scope never expands`() {
        val result = menuCatalogFilteredMenus(
            menus = listOf(adultMenu, childMenu),
            scope = MenuCatalogScope.Audience(MenuAudience.ADULT),
            filters = MenuCatalogFilters(query = childMenu.name),
            dietaryProfiles = profiles(),
            cuisineLabels = emptyMap()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `favorites scope never includes non favorites`() {
        val result = menuCatalogFilteredMenus(
            menus = listOf(adultMenu.copy(isFavorite = false), babyMenu),
            scope = MenuCatalogScope.Favorites,
            filters = MenuCatalogFilters(favoriteAudience = MenuAudience.BABY),
            dietaryProfiles = profiles(),
            cuisineLabels = emptyMap()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `defaults reset every transient filter`() {
        assertEquals(MenuCatalogFilters(), defaultMenuCatalogFilters())
        assertEquals(0, menuCatalogActiveFilterCount(defaultMenuCatalogFilters()))
    }

    @Test
    fun `active filter count excludes text query`() {
        val filters = MenuCatalogFilters(
            query = "arroz",
            dietaryNeed = MenuCatalogDietaryNeed.VEGAN,
            favoritesOnly = true,
            healthyOnly = true
        )

        assertEquals(3, menuCatalogActiveFilterCount(filters))
    }

    @Test
    fun `search is accent insensitive across every supported saved field`() {
        val labels = mapOf(CuisineInspiration.MEDITERRANEAN to "Cocina mediterránea")

        listOf("tostada", "mediterranea", "templada", "aguacate").forEach { query ->
            val result = menuCatalogFilteredMenus(
                menus = listOf(adultMenu),
                scope = MenuCatalogScope.Audience(MenuAudience.ADULT),
                filters = MenuCatalogFilters(query = query.uppercase()),
                dietaryProfiles = profiles(),
                cuisineLabels = labels
            )

            assertEquals(query, listOf(adultMenu), result)
        }
    }

    @Test
    fun `dietary needs exclude only detected conflicts`() {
        val unsafe = adultMenu.copy(
            description = "Queso no pasteurizado con sésamo",
            notes = "Terminar con mantequilla"
        )
        val profiles = profiles().toMutableMap().apply {
            this[MenuAudience.ADULT] = DietaryProfile(
                isEnabled = true,
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.SESAME)
            )
        }

        assertEquals(
            listOf(unsafe),
            filterAdult(unsafe, MenuCatalogDietaryNeed.NONE, profiles)
        )
        listOf(
            MenuCatalogDietaryNeed.PREGNANCY,
            MenuCatalogDietaryNeed.VEGAN,
            MenuCatalogDietaryNeed.ALLERGIES,
            MenuCatalogDietaryNeed.FULL_PROFILE
        ).forEach { need ->
            assertTrue(need.name, filterAdult(unsafe, need, profiles).isEmpty())
        }
    }

    @Test
    fun `favorite and healthy filters intersect and preserve favorite ordering`() {
        val olderHealthy = adultMenu.copy(
            id = 4,
            favoritedAt = 30,
            healthAnalysis = healthyAnalysis()
        )
        val newerHealthy = childMenu.copy(
            id = 5,
            favoritedAt = 50,
            healthAnalysis = healthyAnalysis()
        )
        val unhealthy = babyMenu.copy(
            id = 6,
            isFavorite = true,
            favoritedAt = 70,
            healthAnalysis = HealthAnalysis(HealthStatus.UNHEALTHY, "", "")
        )

        val result = menuCatalogFilteredMenus(
            menus = listOf(olderHealthy, unhealthy, newerHealthy),
            scope = MenuCatalogScope.Favorites,
            filters = MenuCatalogFilters(healthyOnly = true),
            dietaryProfiles = profiles(),
            cuisineLabels = emptyMap()
        )

        assertEquals(listOf(newerHealthy, olderHealthy), result)
    }

    @Test
    fun `allergy need without configured allergens is unavailable and matches nothing`() {
        val profiles = profiles()

        assertFalse(
            menuCatalogDietaryNeedEnabled(
                need = MenuCatalogDietaryNeed.ALLERGIES,
                scope = MenuCatalogScope.Audience(MenuAudience.ADULT),
                favoriteAudience = null,
                profiles = profiles
            )
        )
        assertTrue(
            filterAdult(adultMenu, MenuCatalogDietaryNeed.ALLERGIES, profiles).isEmpty()
        )
    }

    @Test
    fun `allergy need ignores configured allergens from disabled audiences`() {
        val profiles = profiles().toMutableMap().apply {
            this[MenuAudience.CHILD] = DietaryProfile(
                isEnabled = false,
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.EGG)
            )
        }

        assertFalse(
            menuCatalogDietaryNeedEnabled(
                need = MenuCatalogDietaryNeed.ALLERGIES,
                scope = MenuCatalogScope.Favorites,
                favoriteAudience = null,
                profiles = profiles
            )
        )
    }

    @Test
    fun `favorites audience change clears pregnancy outside adult`() {
        val current = MenuCatalogFilters(
            favoriteAudience = MenuAudience.ADULT,
            dietaryNeed = MenuCatalogDietaryNeed.PREGNANCY
        )

        val changed = menuCatalogFiltersAfterFavoriteAudienceSelected(
            current,
            MenuAudience.CHILD
        )

        assertEquals(MenuCatalogDietaryNeed.NONE, changed.dietaryNeed)
        assertEquals(MenuAudience.CHILD, changed.favoriteAudience)
    }

    @Test
    fun `pregnancy is available only for an adult fixed or selected scope`() {
        val profiles = profiles()

        assertTrue(
            menuCatalogDietaryNeedEnabled(
                MenuCatalogDietaryNeed.PREGNANCY,
                MenuCatalogScope.Audience(MenuAudience.ADULT),
                null,
                profiles
            )
        )
        assertFalse(
            menuCatalogDietaryNeedEnabled(
                MenuCatalogDietaryNeed.PREGNANCY,
                MenuCatalogScope.Audience(MenuAudience.CHILD),
                null,
                profiles
            )
        )
        assertTrue(
            menuCatalogDietaryNeedEnabled(
                MenuCatalogDietaryNeed.PREGNANCY,
                MenuCatalogScope.Favorites,
                MenuAudience.ADULT,
                profiles
            )
        )
        assertFalse(
            menuCatalogDietaryNeedEnabled(
                MenuCatalogDietaryNeed.PREGNANCY,
                MenuCatalogScope.Favorites,
                null,
                profiles
            )
        )
    }

    private fun filterAdult(
        menu: FoodMenu,
        need: MenuCatalogDietaryNeed,
        profiles: Map<MenuAudience, DietaryProfile>
    ): List<FoodMenu> = menuCatalogFilteredMenus(
        menus = listOf(menu),
        scope = MenuCatalogScope.Audience(MenuAudience.ADULT),
        filters = MenuCatalogFilters(dietaryNeed = need),
        dietaryProfiles = profiles,
        cuisineLabels = emptyMap()
    )

    private fun profiles(): Map<MenuAudience, DietaryProfile> =
        MenuAudience.entries.associateWith { DietaryProfile(isEnabled = true) }

    private fun healthyAnalysis() = HealthAnalysis(
        status = HealthStatus.HEALTHY,
        reason = "Equilibrado",
        suggestion = ""
    )
}
