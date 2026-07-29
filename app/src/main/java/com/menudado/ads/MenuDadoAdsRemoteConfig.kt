package com.menudado.ads

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.menudado.BuildConfig

class MenuDadoAdsRemoteConfig(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val onAdsEnabledChanged: (Boolean) -> Unit
) {
    fun fetchAdsEnabled() {
        onAdsEnabledChanged(remoteConfig.getBoolean(KEY_ADS_ENABLED))
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds(BuildConfig.DEBUG))
                .build()
        ).addOnCompleteListener {
            remoteConfig.setDefaultsAsync(mapOf(KEY_ADS_ENABLED to DEFAULT_ADS_ENABLED))
                .addOnCompleteListener {
                    remoteConfig.fetchAndActivate().addOnCompleteListener {
                        onAdsEnabledChanged(remoteConfig.getBoolean(KEY_ADS_ENABLED))
                    }
                }
        }
    }

    companion object {
        const val KEY_ADS_ENABLED = "ads_enabled"
        private const val DEFAULT_ADS_ENABLED = false
        private const val DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS = 0L
        private const val MINIMUM_FETCH_INTERVAL_SECONDS = 3_600L

        fun fetchIntervalSeconds(isDebugBuild: Boolean): Long {
            return if (isDebugBuild) DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS else MINIMUM_FETCH_INTERVAL_SECONDS
        }

        fun shouldShowAds(remoteAdsEnabled: Boolean, areAdsReady: Boolean): Boolean {
            return remoteAdsEnabled && areAdsReady
        }
    }
}
