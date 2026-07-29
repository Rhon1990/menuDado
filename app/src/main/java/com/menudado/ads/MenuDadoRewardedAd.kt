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
    private var isEnabled = false
    private var loadGeneration = 0
    private var onReadyChanged: (Boolean) -> Unit = {}
    private var onLoadFailure: () -> Unit = {}

    fun preload(
        onReadyChanged: (Boolean) -> Unit,
        onLoadFailure: () -> Unit = {}
    ) {
        isEnabled = true
        this.onReadyChanged = onReadyChanged
        this.onLoadFailure = onLoadFailure
        loadIfNeeded()
    }

    fun disable() {
        isEnabled = false
        loadGeneration += 1
        loadedAd = null
        isLoading = false
        onReadyChanged(false)
    }

    fun show(
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        if (!isEnabled) {
            onUnavailable()
            return
        }
        val ad = loadedAd ?: run {
            onUnavailable()
            loadIfNeeded()
            return
        }
        loadedAd = null
        onReadyChanged(false)

        val completion = RewardedAdCompletion(
            onRewardEarned = onRewardEarned,
            onDismissedWithoutReward = onDismissedWithoutReward
        )
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                completion.completeAfterDismissal()
                loadIfNeeded()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                completion.cancel()
                onUnavailable()
                loadIfNeeded()
            }
        }
        ad.show(activity) {
            completion.recordReward()
        }
    }

    private fun loadIfNeeded() {
        if (!isEnabled) {
            onReadyChanged(false)
            return
        }
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
        val requestGeneration = loadGeneration
        onReadyChanged(false)
        RewardedAd.load(
            activity,
            adUnitId,
            requestFactory(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    if (requestGeneration != loadGeneration) {
                        return
                    }
                    isLoading = false
                    if (!isEnabled) {
                        loadedAd = null
                        onReadyChanged(false)
                        return
                    }
                    loadedAd = rewardedAd
                    onReadyChanged(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (requestGeneration != loadGeneration) {
                        return
                    }
                    isLoading = false
                    loadedAd = null
                    onReadyChanged(false)
                    if (isEnabled) {
                        onLoadFailure()
                    }
                }
            }
        )
    }
}
