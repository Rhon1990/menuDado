package com.menudado.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketListAggregatorTest {
    @Test
    fun `consolida productos repetidos de menus activos sin cantidades`() {
        val tomato = requireNotNull(ShoppingProduct.fromAi("Tomate"))
        val firstMenu = sampleMenu(1L, listOf(tomato), isActive = true)
        val secondMenu = sampleMenu(
            2L,
            listOf(requireNotNull(ShoppingProduct.fromAi("tomáte"))),
            isActive = true
        )

        val result = aggregateMarketProducts(listOf(firstMenu, secondMenu), emptySet())

        assertEquals(1, result.size)
        assertEquals("Tomate", result.single().displayName)
        assertEquals(setOf(1L, 2L), result.single().sourceMenuIds)
    }

    @Test
    fun `ignora productos de menus que no estan en la lista`() {
        val result = aggregateMarketProducts(
            menus = listOf(
                sampleMenu(
                    id = 1L,
                    products = listOf(requireNotNull(ShoppingProduct.fromAi("Pollo"))),
                    isActive = false
                )
            ),
            purchasedProductKeys = emptySet()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `separa pendientes y comprados conservando el producto`() {
        val tomato = requireNotNull(ShoppingProduct.fromAi("Tomate"))
        val result = aggregateMarketProducts(
            menus = listOf(sampleMenu(1L, listOf(tomato), isActive = true)),
            purchasedProductKeys = setOf(tomato.key)
        )

        assertTrue(result.single().isPurchased)
        assertFalse(result.single().displayName.isBlank())
    }

    @Test
    fun `permite desactivar un producto sin ocultar los demas del mismo menu`() {
        val tomato = requireNotNull(ShoppingProduct.fromAi("Tomate"))
        val chicken = requireNotNull(ShoppingProduct.fromAi("Pollo"))
        val menu = FoodMenu(
            id = 1L,
            name = "Pollo con tomate",
            mealType = MealType.LUNCH,
            description = "Descripción",
            shoppingProducts = listOf(tomato, chicken),
            activeShoppingProductKeys = setOf(chicken.key)
        )

        val result = aggregateMarketProducts(listOf(menu), emptySet())

        assertEquals(listOf("Pollo"), result.map { it.displayName })
    }

    private fun sampleMenu(
        id: Long,
        products: List<ShoppingProduct>,
        isActive: Boolean
    ) = FoodMenu(
        id = id,
        name = "Menu $id",
        mealType = MealType.LUNCH,
        description = "Descripcion",
        shoppingProducts = products,
        activeShoppingProductKeys = if (isActive) {
            products.mapTo(linkedSetOf()) { it.key }
        } else {
            emptySet()
        }
    )
}
