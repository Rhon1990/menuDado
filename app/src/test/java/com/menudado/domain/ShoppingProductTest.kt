package com.menudado.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingProductTest {
    @Test
    fun `normaliza mayusculas tildes y espacios con una clave estable`() {
        val first = requireNotNull(ShoppingProduct.fromAi("  Tomáte   cherry  "))
        val second = requireNotNull(ShoppingProduct.fromAi("tomate cherry"))

        assertEquals("tomate cherry", first.normalizedName)
        assertEquals(first.key, second.key)
        assertEquals("Tomáte cherry", first.displayName)
    }

    @Test
    fun `productos distintos tienen claves distintas`() {
        val tomato = requireNotNull(ShoppingProduct.fromAi("Tomate"))
        val chicken = requireNotNull(ShoppingProduct.fromAi("Pollo"))

        assertNotEquals(tomato.key, chicken.key)
    }

    @Test
    fun `descarta productos vacios con saltos o demasiado largos`() {
        assertNull(ShoppingProduct.fromAi(" "))
        assertNull(ShoppingProduct.fromAi("Tomate\nPollo"))
        assertNull(ShoppingProduct.fromAi("a".repeat(81)))
    }
}
