package com.menudado.data

import android.content.Context

interface OnboardingStore {
    fun isOnboardingCompleted(requiredVersion: Int): Boolean
    fun markOnboardingCompleted(version: Int)
}

object NoOpOnboardingStore : OnboardingStore {
    override fun isOnboardingCompleted(requiredVersion: Int): Boolean = true
    override fun markOnboardingCompleted(version: Int) = Unit
}

class SharedPreferencesOnboardingStore(context: Context) : OnboardingStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun isOnboardingCompleted(requiredVersion: Int): Boolean {
        val completed = preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        val completedVersion = preferences.getInt(
            KEY_ONBOARDING_COMPLETED_VERSION,
            if (completed) LEGACY_ONBOARDING_VERSION else 0
        )
        return completed && completedVersion >= requiredVersion
    }

    override fun markOnboardingCompleted(version: Int) {
        preferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putInt(KEY_ONBOARDING_COMPLETED_VERSION, version)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-onboarding"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_ONBOARDING_COMPLETED_VERSION = "onboarding_completed_version"
        const val LEGACY_ONBOARDING_VERSION = 1
    }
}
