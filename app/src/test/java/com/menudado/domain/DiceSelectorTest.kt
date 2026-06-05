package com.menudado.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiceSelectorTest {
    private val menus = listOf(
        FoodMenu(id = 1, name = "Tostada", mealType = MealType.BREAKFAST, description = "Pan, huevo y aguacate"),
        FoodMenu(id = 2, name = "Ensalada", mealType = MealType.LUNCH, description = "Pollo, lechuga y tomate"),
        FoodMenu(id = 3, name = "Pasta", mealType = MealType.DINNER, description = "Pasta con tomate")
    )

    @Test
    fun `sin filtro usa todos los menus`() {
        val selected = DiceSelector.select(
            menus = menus,
            filter = null,
            nextIndex = { bound ->
                assertEquals(3, bound)
                2
            }
        )

        assertEquals("Pasta", selected?.name)
    }

    @Test
    fun `con filtro solo usa menus del tipo elegido`() {
        val selected = DiceSelector.select(
            menus = menus,
            filter = MealType.LUNCH,
            nextIndex = { bound ->
                assertEquals(1, bound)
                0
            }
        )

        assertEquals("Ensalada", selected?.name)
    }

    @Test
    fun `devuelve null cuando no hay candidatos para el filtro`() {
        val selected = DiceSelector.select(
            menus = menus.filterNot { it.mealType == MealType.DINNER },
            filter = MealType.DINNER,
            nextIndex = { error("No debe pedir azar sin candidatos") }
        )

        assertNull(selected)
    }

    @Test
    fun `excluye menus que ya salieron hoy`() {
        val selected = DiceSelector.select(
            menus = listOf(
                FoodMenu(id = 1, name = "Tostada", mealType = MealType.BREAKFAST, description = "Pan", lastPickedDate = "2026-06-03"),
                FoodMenu(id = 2, name = "Yogur", mealType = MealType.BREAKFAST, description = "Yogur y fruta")
            ),
            filter = MealType.BREAKFAST,
            today = "2026-06-03",
            nextIndex = { bound ->
                assertEquals(1, bound)
                0
            }
        )

        assertEquals("Yogur", selected?.name)
    }
}
