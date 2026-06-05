package com.menudado.data

import android.content.Context

interface OnboardingStore {
    fun isOnboardingCompleted(): Boolean
    fun markOnboardingCompleted()
}

object NoOpOnboardingStore : OnboardingStore {
    override fun isOnboardingCompleted(): Boolean = true
    override fun markOnboardingCompleted() = Unit
}

class SharedPreferencesOnboardingStore(context: Context) : OnboardingStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun isOnboardingCompleted(): Boolean =
        preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    override fun markOnboardingCompleted() {
        preferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-onboarding"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
