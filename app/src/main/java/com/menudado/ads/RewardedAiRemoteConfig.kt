package com.menudado.ads

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.menudado.BuildConfig

class RewardedAiRemoteConfig(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val onRewardedAiEnabledChanged: (Boolean) -> Unit
) {
    fun fetchRewardedAiEnabled() {
        onRewardedAiEnabledChanged(remoteConfig.getBoolean(KEY_REWARDED_AI_ENABLED))
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds(BuildConfig.DEBUG))
                .build()
        ).addOnCompleteListener {
            remoteConfig.setDefaultsAsync(
                mapOf(KEY_REWARDED_AI_ENABLED to DEFAULT_REWARDED_AI_ENABLED)
            ).addOnCompleteListener {
                remoteConfig.fetchAndActivate().addOnCompleteListener {
                    onRewardedAiEnabledChanged(remoteConfig.getBoolean(KEY_REWARDED_AI_ENABLED))
                }
            }
        }
    }

    companion object {
        const val KEY_REWARDED_AI_ENABLED = "rewarded_ai_enabled"
        const val DEFAULT_REWARDED_AI_ENABLED = false
        private const val DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS = 0L
        private const val MINIMUM_FETCH_INTERVAL_SECONDS = 3_600L

        fun fetchIntervalSeconds(isDebugBuild: Boolean): Long {
            return if (isDebugBuild) DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS else MINIMUM_FETCH_INTERVAL_SECONDS
        }
    }
}
