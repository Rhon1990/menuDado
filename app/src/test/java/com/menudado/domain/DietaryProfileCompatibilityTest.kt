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

    @Test
    fun `baby profile rejects honey added salt and whole choking hazards`() {
        val profile = DietaryProfile(ageRange = MenuAudience.BABY.defaultAgeRange)

        listOf(
            generated("Yogur con miel"),
            generated("Arroz con una pizca de sal añadida"),
            generated("Uvas enteras con frutos secos enteros")
        ).forEach { menu ->
            assertFalse(profile.accepts(menu, MenuAudience.BABY))
        }
    }

    @Test
    fun `pregnancy profile rejects raw unpasteurized alcohol and high mercury fish`() {
        val profile = DietaryProfile(isPregnant = true)

        listOf(
            generated("Sushi de atún rojo"),
            generated("Queso de leche no pasteurizada"),
            generated("Salsa con vino sin cocinar")
        ).forEach { menu ->
            assertFalse(profile.accepts(menu, MenuAudience.ADULT))
        }
    }

    @Test
    fun `explicit compatible substitutions do not trigger ambiguous allergen terms`() {
        val profile = DietaryProfile(
            hasAllergies = true,
            allergens = setOf(DietaryAllergen.GLUTEN, DietaryAllergen.DAIRY)
        )

        assertTrue(
            profile.accepts(
                generated("Pasta sin gluten con crema vegetal"),
                MenuAudience.ADULT
            )
        )
    }

    @Test
    fun `clinical condition names are not treated as food ingredients`() {
        val profile = DietaryProfile(otherAvoidances = "diabético, hipertenso")

        assertTrue(
            profile.accepts(
                generated("Lentejas con verduras"),
                MenuAudience.ADULT
            )
        )
    }

    @Test
    fun `prefixed concrete avoidance is enforced`() {
        val profile = DietaryProfile(otherAvoidances = "sin picante, sin champiñones")

        assertFalse(
            profile.accepts(
                generated("Arroz con champiñones"),
                MenuAudience.ADULT
            )
        )
    }

    private fun generated(description: String) = GeneratedMenu(
        name = "Idea",
        description = description,
        notes = "",
        calories = 300
    )
}
