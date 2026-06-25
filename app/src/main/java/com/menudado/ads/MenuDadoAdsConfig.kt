package com.menudado.ads

import com.menudado.BuildConfig

object MenuDadoAdsConfig {
    const val HOME_INLINE_BANNER_PLACEMENT = "home_inline_banner"
    const val DEBUG_HOME_INLINE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val RELEASE_HOME_INLINE_BANNER_AD_UNIT_ID = "ca-app-pub-2347852335093406/4295829613"
    val HOME_INLINE_BANNER_AD_UNIT_ID: String = BuildConfig.HOME_INLINE_BANNER_AD_UNIT_ID
    const val HOME_INLINE_BANNER_HORIZONTAL_PADDING_DP = 20
    const val NON_PERSONALIZED_ADS_PARAM_KEY = "npa"
    const val NON_PERSONALIZED_ADS_PARAM_VALUE = "1"
    const val isEnabled = true
    const val requestNonPersonalizedAds = true

    fun homeInlineBannerWidthDp(screenWidthDp: Int): Int {
        return (screenWidthDp - HOME_INLINE_BANNER_HORIZONTAL_PADDING_DP * 2).coerceAtLeast(1)
    }
}
