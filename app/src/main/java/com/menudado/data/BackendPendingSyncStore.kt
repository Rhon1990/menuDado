package com.menudado.data

import android.content.Context
import com.menudado.domain.MenuAudience

interface BackendPendingSyncStore {
    fun markDietaryProfilePending(audience: MenuAudience)
    fun clearDietaryProfilePending(audience: MenuAudience)
    fun getPendingDietaryProfileAudiences(): Set<MenuAudience>
    fun markAiUsagePending()
    fun clearAiUsagePending()
    fun isAiUsagePending(): Boolean
    fun markOnboardingPending(version: Int)
    fun clearOnboardingPending()
    fun getPendingOnboardingVersion(): Int?
}

class SharedPreferencesBackendPendingSyncStore(context: Context) : BackendPendingSyncStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun markDietaryProfilePending(audience: MenuAudience) {
        val current = getPendingDietaryProfileAudiences().map { it.name }.toSet()
        preferences.edit()
            .putStringSet(KEY_PENDING_PROFILES, current + audience.name)
            .apply()
    }

    override fun clearDietaryProfilePending(audience: MenuAudience) {
        val remaining = getPendingDietaryProfileAudiences()
            .filterNot { it == audience }
            .map { it.name }
            .toSet()
        preferences.edit()
            .putStringSet(KEY_PENDING_PROFILES, remaining)
            .apply()
    }

    override fun getPendingDietaryProfileAudiences(): Set<MenuAudience> {
        return preferences.getStringSet(KEY_PENDING_PROFILES, emptySet())
            .orEmpty()
            .mapNotNull { value -> runCatching { MenuAudience.valueOf(value) }.getOrNull() }
            .toSet()
    }

    override fun markAiUsagePending() {
        preferences.edit().putBoolean(KEY_PENDING_AI_USAGE, true).apply()
    }

    override fun clearAiUsagePending() {
        preferences.edit().putBoolean(KEY_PENDING_AI_USAGE, false).apply()
    }

    override fun isAiUsagePending(): Boolean {
        return preferences.getBoolean(KEY_PENDING_AI_USAGE, false)
    }

    override fun markOnboardingPending(version: Int) {
        preferences.edit().putInt(KEY_PENDING_ONBOARDING_VERSION, version).apply()
    }

    override fun clearOnboardingPending() {
        preferences.edit().remove(KEY_PENDING_ONBOARDING_VERSION).apply()
    }

    override fun getPendingOnboardingVersion(): Int? {
        val version = preferences.getInt(KEY_PENDING_ONBOARDING_VERSION, NO_PENDING_VERSION)
        return version.takeIf { it != NO_PENDING_VERSION }
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-backend-pending-sync"
        const val KEY_PENDING_PROFILES = "pending_profiles"
        const val KEY_PENDING_AI_USAGE = "pending_ai_usage"
        const val KEY_PENDING_ONBOARDING_VERSION = "pending_onboarding_version"
        const val NO_PENDING_VERSION = -1
    }
}
