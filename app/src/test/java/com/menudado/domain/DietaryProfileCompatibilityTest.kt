package com.menudado.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietaryProfileCompatibilityTest {
    @Test
    fun `vegan profile rejects dairy in generated candidate`() {
        val menu = GeneratedMenu(
            name = "Pasta cremosa",
            description = "Pasta, tomate, queso y nata.",
            notes = "",
            calories = 500
        )

        assertFalse(DietaryProfile(isVegan = true).accepts(menu))
    }

    @Test
    fun `allergy and free avoidance are checked across recipe and products`() {
        val menu = GeneratedMenu(
            name = "Bowl suave",
            description = "Arroz y verduras.",
            notes = "Termina con tahini.",
            calories = 410,
            shoppingProducts = listOf(requireNotNull(ShoppingProduct.fromAi("Sésamo")))
        )
        val profile = DietaryProfile(
            hasAllergies = true,
            allergens = setOf(DietaryAllergen.SESAME),
            otherAvoidances = "picante"
        )

        assertFalse(profile.accepts(menu))
        assertTrue(profile.accepts(menu.copy(notes = "", shoppingProducts = emptyList())))
    }
}
