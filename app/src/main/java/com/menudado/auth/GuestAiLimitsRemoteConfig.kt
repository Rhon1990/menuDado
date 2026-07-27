package com.menudado.auth

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.menudado.BuildConfig

class GuestAiLimitsRemoteConfig(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val onGuestAiLimitsEnabledChanged: (Boolean) -> Unit
) {
    fun fetchGuestAiLimitsEnabled() {
        onGuestAiLimitsEnabledChanged(remoteConfig.getBoolean(KEY_GUEST_AI_LIMITS_ENABLED))
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds(BuildConfig.DEBUG))
                .build()
        ).addOnCompleteListener {
            remoteConfig.setDefaultsAsync(
                mapOf(KEY_GUEST_AI_LIMITS_ENABLED to DEFAULT_GUEST_AI_LIMITS_ENABLED)
            ).addOnCompleteListener {
                remoteConfig.fetchAndActivate().addOnCompleteListener {
                    onGuestAiLimitsEnabledChanged(remoteConfig.getBoolean(KEY_GUEST_AI_LIMITS_ENABLED))
                }
            }
        }
    }

    companion object {
        const val KEY_GUEST_AI_LIMITS_ENABLED = "guest_ai_limits_enabled"
        const val DEFAULT_GUEST_AI_LIMITS_ENABLED = true
        private const val DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS = 0L
        private const val MINIMUM_FETCH_INTERVAL_SECONDS = 3_600L

        fun fetchIntervalSeconds(isDebugBuild: Boolean): Long {
            return if (isDebugBuild) DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS else MINIMUM_FETCH_INTERVAL_SECONDS
        }
    }
}
