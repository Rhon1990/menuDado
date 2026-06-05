package com.menudado.ui

import com.menudado.R
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuCardUiStateTest {
    @Test
    fun `muestra chip de analisis en cabecera cuando la tarjeta esta colapsada`() {
        val menuWithoutAnalysis = FoodMenu(
            name = "Tostada",
            mealType = MealType.BREAKFAST,
            description = "Pan con tomate"
        )
        val menuWithAnalysis = menuWithoutAnalysis.copy(
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.IMPROVABLE,
                reason = "Tiene buena base, pero puede mejorar",
                suggestion = "Agrega fruta"
            )
        )

        assertEquals(HealthStatus.UNKNOWN, menuHeaderHealthStatus(menuWithoutAnalysis, isExpanded = false))
        assertEquals(HealthStatus.UNKNOWN, menuHeaderHealthStatus(menuWithoutAnalysis, isExpanded = true))
        assertEquals(HealthStatus.IMPROVABLE, menuHeaderHealthStatus(menuWithAnalysis, isExpanded = false))
        assertNull(menuHeaderHealthStatus(menuWithAnalysis, isExpanded = true))
    }

    @Test
    fun `muestra calorias solo despues del analisis IA`() {
        val menuWithoutAnalysis = FoodMenu(
            name = "Porridge",
            mealType = MealType.BREAKFAST,
            description = "Avena con fruta",
            calories = 480
        )
        val menuWithAnalysis = menuWithoutAnalysis.copy(
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Tiene fibra y fruta.",
                suggestion = "Mantener variedad."
            )
        )

        assertNull(menuVisibleCalories(menuWithoutAnalysis))
        assertEquals(480, menuVisibleCalories(menuWithAnalysis))
    }

    @Test
    fun `tocar un menu cerrado lo convierte en el unico expandido`() {
        assertEquals(7L, nextExpandedMenuIdAfterMenuClick(currentExpandedMenuId = null, clickedMenuId = 7L))
        assertEquals(9L, nextExpandedMenuIdAfterMenuClick(currentExpandedMenuId = 7L, clickedMenuId = 9L))
    }

    @Test
    fun `tocar el menu expandido lo colapsa`() {
        assertNull(nextExpandedMenuIdAfterMenuClick(currentExpandedMenuId = 7L, clickedMenuId = 7L))
    }

    @Test
    fun `usa iconos distintos para ver y ocultar menu`() {
        assertEquals(R.drawable.ic_expand_more, menuExpandToggleIconRes(isExpanded = false))
        assertEquals(R.drawable.ic_expand_less, menuExpandToggleIconRes(isExpanded = true))
    }
}
