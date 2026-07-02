package com.menudado

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
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
                areAdsEnabled = true,
                buildType = "release",
                areAdsPrivacyOptionsRequired = true
            )
        )
        assertEquals(
            false,
            shouldShowAdsPrivacyOptionsInNavigation(
                areAdsEnabled = true,
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
                areAdsEnabled = true,
                buildType = "debug",
                areAdsPrivacyOptionsRequired = true
            )
        )
        assertEquals(
            true,
            shouldShowAdsPrivacyOptionsInNavigation(
                areAdsEnabled = true,
                buildType = "releaseDebuggable",
                areAdsPrivacyOptionsRequired = true
            )
        )
        assertEquals(
            false,
            shouldShowAdsPrivacyOptionsInNavigation(
                areAdsEnabled = true,
                buildType = "debug",
                areAdsPrivacyOptionsRequired = false
            )
        )
        assertEquals(
            false,
            shouldShowAdsPrivacyOptionsInNavigation(
                areAdsEnabled = false,
                buildType = "debug",
                areAdsPrivacyOptionsRequired = true
            )
        )
    }

    @Test
    fun `remote config de anuncios se refresca al volver la app a primer plano`() {
        assertEquals(true, shouldRefreshAdsRemoteConfigOnLifecycleEvent(Lifecycle.Event.ON_RESUME))
        assertEquals(false, shouldRefreshAdsRemoteConfigOnLifecycleEvent(Lifecycle.Event.ON_PAUSE))
        assertEquals(false, shouldRefreshAdsRemoteConfigOnLifecycleEvent(Lifecycle.Event.ON_STOP))
    }
}
