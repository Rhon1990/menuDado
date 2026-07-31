package com.menudado.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menudado.ui.theme.MenuDadoColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MenuCatalogSearchBarStyleTest {
    @Test
    fun filterButtonUsesHeaderIntegratedColors() {
        val style = menuCatalogFilterButtonVisualStyle()

        assertEquals(MenuDadoColors.Cream.copy(alpha = 0.12f), style.containerColor)
        assertEquals(MenuDadoColors.Cream.copy(alpha = 0.82f), style.borderColor)
        assertEquals(MenuDadoColors.Cream, style.iconColor)
        assertEquals(1.dp, style.borderWidth)
    }

    @Test
    fun activeFilterBadgeFloatsOutsideTheButtonCorner() {
        val style = menuCatalogFilterButtonVisualStyle()

        assertEquals(20.dp, style.badgeSize)
        assertEquals(4.dp, style.badgeHorizontalOffset)
        assertEquals((-4).dp, style.badgeVerticalOffset)
    }

    @Test
    fun activeFilterBadgeUsesCenteredTypographyMetrics() {
        val style = menuCatalogFilterButtonVisualStyle()

        assertEquals(11.sp, style.badgeFontSize)
        assertEquals(11.sp, style.badgeLineHeight)
        assertFalse(style.includeFontPadding)
    }
}
