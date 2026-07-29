package com.menudado

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import com.google.android.play.core.install.model.AppUpdateType
import com.menudado.ui.theme.MenuDadoColors
import android.view.WindowManager
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
    fun `modo cutout usa el valor moderno compatible con Android 15`() {
        assertEquals(
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            menuDadoDisplayCutoutMode()
        )
    }

    @Test
    fun `edge to edge dibuja contenido detras de barras del sistema`() {
        assertEquals(false, menuDadoDecorFitsSystemWindows())
    }

    @Test
    fun `opciones de privacidad no se muestran en la barra de navegacion`() {
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
                buildType = "debug",
                areAdsPrivacyOptionsRequired = true
            )
        )
        assertEquals(
            false,
            shouldShowAdsPrivacyOptionsInNavigation(
                areAdsEnabled = true,
                buildType = "releaseDebuggable",
                areAdsPrivacyOptionsRequired = true
            )
        )
        assertEquals(
            false,
            shouldShowAdsPrivacyOptionsInNavigation(
                areAdsEnabled = false,
                buildType = "debug",
                areAdsPrivacyOptionsRequired = false
            )
        )
    }

    @Test
    fun `remote config de anuncios se refresca al volver la app a primer plano`() {
        assertEquals(true, shouldRefreshAdsRemoteConfigOnLifecycleEvent(Lifecycle.Event.ON_RESUME))
        assertEquals(false, shouldRefreshAdsRemoteConfigOnLifecycleEvent(Lifecycle.Event.ON_PAUSE))
        assertEquals(false, shouldRefreshAdsRemoteConfigOnLifecycleEvent(Lifecycle.Event.ON_STOP))
    }

    @Test
    fun `actualizacion usa flexible sin convertir immediate en bloqueo`() {
        assertEquals(
            AppUpdateType.FLEXIBLE,
            preferredMenuDadoAppUpdateType(isFlexibleAllowed = true)
        )
        assertEquals(null, preferredMenuDadoAppUpdateType(isFlexibleAllowed = false))
    }

    @Test
    fun `analitica de actualizacion usa acciones cerradas sin datos personales`() {
        assertEquals("app_update_prompt", appUpdateAnalyticsScreen())
        assertEquals("shown", appUpdateAnalyticsActionShown())
        assertEquals("update", appUpdateAnalyticsActionUpdate())
        assertEquals("later", appUpdateAnalyticsActionLater())
        assertEquals("install", appUpdateAnalyticsActionInstall())
    }

    @Test
    fun `fallback de actualizacion apunta a la ficha productiva de MenuDado en Play Store`() {
        assertEquals("com.menudado", menuDadoPlayStorePackageName())
        assertEquals(
            "market://details?id=com.menudado",
            menuDadoPlayStoreMarketUri()
        )
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.menudado",
            menuDadoPlayStoreWebUrl()
        )
    }
}
