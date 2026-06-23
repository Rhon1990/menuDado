package com.menudado.ads

object MenuDadoAdsConfig {
    const val HOME_INLINE_BANNER_PLACEMENT = "home_inline_banner"
    const val HOME_INLINE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val HOME_INLINE_BANNER_HORIZONTAL_PADDING_DP = 20
    const val isEnabled = true

    fun homeInlineBannerWidthDp(screenWidthDp: Int): Int {
        return (screenWidthDp - HOME_INLINE_BANNER_HORIZONTAL_PADDING_DP * 2).coerceAtLeast(1)
    }
}
