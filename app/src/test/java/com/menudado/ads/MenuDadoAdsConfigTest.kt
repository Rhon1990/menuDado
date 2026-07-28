package com.menudado.ads

import com.menudado.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuDadoAdsConfigTest {
    @Test
    fun `debug home banner uses Google demo ad unit to avoid invalid traffic while testing`() {
        assertEquals("home_inline_banner", MenuDadoAdsConfig.HOME_INLINE_BANNER_PLACEMENT)
        assertEquals(
            "ca-app-pub-3940256099942544/9214589741",
            MenuDadoAdsConfig.HOME_INLINE_BANNER_AD_UNIT_ID
        )
    }

    @Test
    fun `release home banner ad unit uses MenuDado production placement`() {
        assertEquals(
            "ca-app-pub-2347852335093406/2270906270",
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
    fun `home banner visibility requires remote config enabled and ads ready`() {
        assertTrue(MenuDadoAdsRemoteConfig.shouldShowAds(remoteAdsEnabled = true, areAdsReady = true))
        assertFalse(MenuDadoAdsRemoteConfig.shouldShowAds(remoteAdsEnabled = false, areAdsReady = true))
        assertFalse(MenuDadoAdsRemoteConfig.shouldShowAds(remoteAdsEnabled = true, areAdsReady = false))
    }

    @Test
    fun `ads readiness requires both active consent and initialized SDK`() {
        assertTrue(MenuDadoAdsController.shouldReportReady(canRequestAds = true, hasInitializedMobileAds = true))
        assertFalse(MenuDadoAdsController.shouldReportReady(canRequestAds = true, hasInitializedMobileAds = false))
        assertFalse(MenuDadoAdsController.shouldReportReady(canRequestAds = false, hasInitializedMobileAds = true))
        assertFalse(MenuDadoAdsController.shouldReportReady(canRequestAds = false, hasInitializedMobileAds = false))
    }

    @Test
    fun `debug remote config fetches ads flag immediately while release keeps cache interval`() {
        assertEquals(0L, MenuDadoAdsRemoteConfig.fetchIntervalSeconds(isDebugBuild = true))
        assertEquals(3_600L, MenuDadoAdsRemoteConfig.fetchIntervalSeconds(isDebugBuild = false))
    }

    @Test
    fun `rewarded AI remote config is disabled by default`() {
        assertFalse(RewardedAiRemoteConfig.DEFAULT_REWARDED_AI_ENABLED)
    }

    @Test
    fun `rewarded AI remote config fetches immediately in debug and hourly in release`() {
        assertEquals(0L, RewardedAiRemoteConfig.fetchIntervalSeconds(isDebugBuild = true))
        assertEquals(3_600L, RewardedAiRemoteConfig.fetchIntervalSeconds(isDebugBuild = false))
    }

    @Test
    fun `rewarded AI is offered only when remote config ads and a real ad unit are ready`() {
        assertTrue(
            MenuDadoAdsConfig.shouldOfferRewardedAi(
                remoteEnabled = true,
                adsReady = true,
                adUnitId = "ca-app-pub-example/rewarded"
            )
        )
        assertFalse(
            MenuDadoAdsConfig.shouldOfferRewardedAi(
                remoteEnabled = false,
                adsReady = true,
                adUnitId = "ca-app-pub-example/rewarded"
            )
        )
        assertFalse(
            MenuDadoAdsConfig.shouldOfferRewardedAi(
                remoteEnabled = true,
                adsReady = false,
                adUnitId = "ca-app-pub-example/rewarded"
            )
        )
        assertFalse(
            MenuDadoAdsConfig.shouldOfferRewardedAi(
                remoteEnabled = true,
                adsReady = true,
                adUnitId = ""
            )
        )
    }

    @Test
    fun `privacy options fallback is shown only when UMP returns a form error`() {
        assertTrue(MenuDadoAdsController.shouldNotifyPrivacyOptionsUnavailable(hasFormError = true))
        assertFalse(MenuDadoAdsController.shouldNotifyPrivacyOptionsUnavailable(hasFormError = false))
    }
}
