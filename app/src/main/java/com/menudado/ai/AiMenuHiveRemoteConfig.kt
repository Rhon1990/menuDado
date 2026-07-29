package com.menudado.ai

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.menudado.BuildConfig

class AiMenuHiveRemoteConfig(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val onEnabledChanged: (Boolean) -> Unit
) {
    fun fetchEnabled() {
        onEnabledChanged(remoteConfig.getBoolean(KEY_AI_MENU_HIVE_ENABLED))
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds(BuildConfig.DEBUG))
                .build()
        ).addOnCompleteListener {
            remoteConfig.setDefaultsAsync(
                mapOf(KEY_AI_MENU_HIVE_ENABLED to DEFAULT_AI_MENU_HIVE_ENABLED)
            ).addOnCompleteListener {
                remoteConfig.fetchAndActivate().addOnCompleteListener {
                    onEnabledChanged(remoteConfig.getBoolean(KEY_AI_MENU_HIVE_ENABLED))
                }
            }
        }
    }

    companion object {
        const val KEY_AI_MENU_HIVE_ENABLED = "ai_menu_hive_enabled"
        const val DEFAULT_AI_MENU_HIVE_ENABLED = true
        private const val DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS = 0L
        private const val MINIMUM_FETCH_INTERVAL_SECONDS = 3_600L

        fun fetchIntervalSeconds(isDebugBuild: Boolean): Long =
            if (isDebugBuild) DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS
            else MINIMUM_FETCH_INTERVAL_SECONDS
    }
}
