package com.menudado.ads

import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.menudado.BuildConfig

object MenuDadoAdsConfig {
    const val HOME_INLINE_BANNER_PLACEMENT = "home_inline_banner"
    const val DEBUG_HOME_INLINE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val RELEASE_HOME_INLINE_BANNER_AD_UNIT_ID = "ca-app-pub-2347852335093406/2270906270"
    val HOME_INLINE_BANNER_AD_UNIT_ID: String = BuildConfig.HOME_INLINE_BANNER_AD_UNIT_ID
    const val DEBUG_REWARDED_AI_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    val REWARDED_AI_AD_UNIT_ID: String = BuildConfig.REWARDED_AI_AD_UNIT_ID
    const val HOME_INLINE_BANNER_HORIZONTAL_PADDING_DP = 20
    const val NON_PERSONALIZED_ADS_PARAM_KEY = "npa"
    const val NON_PERSONALIZED_ADS_PARAM_VALUE = "1"
    const val requestNonPersonalizedAds = true

    fun homeInlineBannerWidthDp(screenWidthDp: Int): Int {
        return (screenWidthDp - HOME_INLINE_BANNER_HORIZONTAL_PADDING_DP * 2).coerceAtLeast(1)
    }

    fun shouldOfferRewardedAi(
        remoteEnabled: Boolean,
        adsReady: Boolean,
        adUnitId: String = REWARDED_AI_AD_UNIT_ID
    ): Boolean {
        return remoteEnabled && adsReady && adUnitId.isNotBlank()
    }

    fun createAdRequest(): AdRequest {
        return AdRequest.Builder().apply {
            if (requestNonPersonalizedAds) {
                addNetworkExtrasBundle(
                    AdMobAdapter::class.java,
                    Bundle().apply {
                        putString(NON_PERSONALIZED_ADS_PARAM_KEY, NON_PERSONALIZED_ADS_PARAM_VALUE)
                    }
                )
            }
        }.build()
    }
}
