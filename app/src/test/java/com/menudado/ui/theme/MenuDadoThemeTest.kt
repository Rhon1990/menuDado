package com.menudado.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuDadoThemeTest {
    @Test
    fun `calm editorial palette keeps action and error roles separate`() {
        assertEquals(Color(0xFFD66548), MenuDadoColors.ActionTerracotta)
        assertEquals(Color(0xFFE7F0EB), MenuDadoColors.SelectionGreen)
        assertEquals(Color(0xFFE35D3E), MenuDadoColors.Tomato)
        assertEquals(Color(0xFFFFF9EC), MenuDadoColors.Background)
        assertEquals(Color(0xFFFFFCF4), MenuDadoColors.Surface)
    }

    @Test
    fun `shape and spacing tokens follow approved scale`() {
        assertEquals(24.dp, MenuDadoUiTokens.CardRadius)
        assertEquals(16.dp, MenuDadoUiTokens.ControlRadius)
        assertEquals(20.dp, MenuDadoUiTokens.NavigationRadius)
        assertEquals(48.dp, MenuDadoUiTokens.MinimumTouchTarget)
        assertEquals(
            listOf(8.dp, 12.dp, 16.dp, 24.dp, 32.dp),
            MenuDadoUiTokens.SpacingScale
        )
    }
}
