package com.menudado.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuDadoAdsConfigTest {
    @Test
    fun `home banner uses Google demo ad unit until production ids are configured`() {
        assertTrue(MenuDadoAdsConfig.isEnabled)
        assertEquals("home_inline_banner", MenuDadoAdsConfig.HOME_INLINE_BANNER_PLACEMENT)
        assertEquals(
            "ca-app-pub-3940256099942544/9214589741",
            MenuDadoAdsConfig.HOME_INLINE_BANNER_AD_UNIT_ID
        )
    }

    @Test
    fun `home banner width subtracts horizontal padding so ad is not clipped`() {
        assertEquals(320, MenuDadoAdsConfig.homeInlineBannerWidthDp(screenWidthDp = 360))
    }
}
