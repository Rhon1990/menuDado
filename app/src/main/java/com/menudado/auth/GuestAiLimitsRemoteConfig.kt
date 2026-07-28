package com.menudado.auth

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.menudado.BuildConfig

class GuestAiLimitsRemoteConfig(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val onGuestAiLimitsEnabledChanged: (Boolean) -> Unit
) {
    fun fetchGuestAiLimitsEnabled() {
        val currentValue = remoteConfig.getValue(KEY_GUEST_AI_LIMITS_ENABLED)
        onGuestAiLimitsEnabledChanged(
            initialGuestAiLimitsEnabled(
                remoteValue = currentValue.asBoolean(),
                valueSource = currentValue.source
            )
        )
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

        fun initialGuestAiLimitsEnabled(remoteValue: Boolean, valueSource: Int): Boolean {
            return if (valueSource == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
                DEFAULT_GUEST_AI_LIMITS_ENABLED
            } else {
                remoteValue
            }
        }
    }
}
