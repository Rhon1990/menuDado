package com.menudado.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MenuDadoRewardedAd(
    private val activity: Activity,
    private val adUnitId: String,
    private val requestFactory: () -> AdRequest = MenuDadoAdsConfig::createAdRequest
) {
    private var loadedAd: RewardedAd? = null
    private var isLoading = false
    private var onReadyChanged: (Boolean) -> Unit = {}
    private var onLoadFailure: () -> Unit = {}

    fun preload(
        onReadyChanged: (Boolean) -> Unit,
        onLoadFailure: () -> Unit = {}
    ) {
        this.onReadyChanged = onReadyChanged
        this.onLoadFailure = onLoadFailure
        loadIfNeeded()
    }

    fun show(
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        val ad = loadedAd ?: run {
            onUnavailable()
            loadIfNeeded()
            return
        }
        loadedAd = null
        onReadyChanged(false)

        var rewardDelivered = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (!rewardDelivered) {
                    onDismissedWithoutReward()
                }
                loadIfNeeded()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                onUnavailable()
                loadIfNeeded()
            }
        }
        ad.show(activity) {
            if (!rewardDelivered) {
                rewardDelivered = true
                onRewardEarned()
            }
        }
    }

    private fun loadIfNeeded() {
        if (adUnitId.isBlank()) {
            loadedAd = null
            isLoading = false
            onReadyChanged(false)
            onLoadFailure()
            return
        }
        if (loadedAd != null) {
            onReadyChanged(true)
            return
        }
        if (isLoading) {
            return
        }
        isLoading = true
        onReadyChanged(false)
        RewardedAd.load(
            activity,
            adUnitId,
            requestFactory(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    isLoading = false
                    loadedAd = rewardedAd
                    onReadyChanged(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading = false
                    loadedAd = null
                    onReadyChanged(false)
                    onLoadFailure()
                }
            }
        )
    }
}
