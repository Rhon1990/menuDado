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

    @Test
    fun `opciones de privacidad no se muestran en release`() {
        assertEquals(
            false,
            shouldShowAdsPrivacyOptionsInNavigation(
                buildType = "release",
                areAdsPrivacyOptionsRequired = true
            )
        )
        assertEquals(
            false,
            shouldShowAdsPrivacyOptionsInNavigation(
                buildType = "release",
                areAdsPrivacyOptionsRequired = false
            )
        )
    }

    @Test
    fun `opciones de privacidad se pueden mostrar en builds de prueba`() {
        assertEquals(
            true,
            shouldShowAdsPrivacyOptionsInNavigation(
                buildType = "debug",
                areAdsPrivacyOptionsRequired = true
            )
        )
        assertEquals(
            true,
            shouldShowAdsPrivacyOptionsInNavigation(
                buildType = "releaseDebuggable",
                areAdsPrivacyOptionsRequired = true
            )
        )
        assertEquals(
            false,
            shouldShowAdsPrivacyOptionsInNavigation(
                buildType = "debug",
                areAdsPrivacyOptionsRequired = false
            )
        )
    }
}
