package com.menudado

import androidx.compose.ui.graphics.toArgb
import com.menudado.ui.theme.MenuDadoColors
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivitySystemBarsTest {
    @Test
    fun `barras del sistema usan el verde de cabecera de MenuDado`() {
        val headerGreen = MenuDadoColors.HeaderGreen.toArgb()

        assertEquals(headerGreen, menuDadoStatusBarColor())
        assertEquals(headerGreen, menuDadoNavigationBarColor())
    }
}
