package com.menudado.ads

import com.menudado.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuDadoAdsConfigTest {
    @Test
    fun `debug home banner uses Google demo ad unit to avoid invalid traffic while testing`() {
        assertTrue(MenuDadoAdsConfig.isEnabled)
        assertEquals("home_inline_banner", MenuDadoAdsConfig.HOME_INLINE_BANNER_PLACEMENT)
        assertEquals(
            "ca-app-pub-3940256099942544/9214589741",
            MenuDadoAdsConfig.HOME_INLINE_BANNER_AD_UNIT_ID
        )
    }

    @Test
    fun `release home banner ad unit uses MenuDado production placement`() {
        assertEquals(
            "ca-app-pub-2347852335093406/4295829613",
            MenuDadoAdsConfig.RELEASE_HOME_INLINE_BANNER_AD_UNIT_ID
        )
    }

    @Test
    fun `current variant ad unit comes from build config`() {
        assertEquals(BuildConfig.HOME_INLINE_BANNER_AD_UNIT_ID, MenuDadoAdsConfig.HOME_INLINE_BANNER_AD_UNIT_ID)
    }

    @Test
    fun `debuggable variants use demo banner unless production release is selected`() {
        if (BuildConfig.DEBUG) {
            assertEquals(
                MenuDadoAdsConfig.DEBUG_HOME_INLINE_BANNER_AD_UNIT_ID,
                MenuDadoAdsConfig.HOME_INLINE_BANNER_AD_UNIT_ID
            )
        }
    }

    @Test
    fun `home banner width subtracts horizontal padding so ad is not clipped`() {
        assertEquals(320, MenuDadoAdsConfig.homeInlineBannerWidthDp(screenWidthDp = 360))
    }

    @Test
    fun `home banner requests non personalized ads while advertising id is disabled`() {
        assertTrue(MenuDadoAdsConfig.requestNonPersonalizedAds)
        assertEquals("npa", MenuDadoAdsConfig.NON_PERSONALIZED_ADS_PARAM_KEY)
        assertEquals("1", MenuDadoAdsConfig.NON_PERSONALIZED_ADS_PARAM_VALUE)
    }

    @Test
    fun `privacy options fallback is shown only when UMP returns a form error`() {
        assertTrue(MenuDadoAdsController.shouldNotifyPrivacyOptionsUnavailable(hasFormError = true))
        assertFalse(MenuDadoAdsController.shouldNotifyPrivacyOptionsUnavailable(hasFormError = false))
    }
}
