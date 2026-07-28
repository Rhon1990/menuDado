package com.menudado.data

import android.content.Context

const val GUEST_AI_USAGE_SCOPE = "guest"
const val PROVIDER_AI_USAGE_SCOPE = "provider"

fun accountAiUsageScope(userId: String): String = "account:$userId"

fun aiUsageScope(isGuest: Boolean, userId: String?): String {
    return if (isGuest || userId.isNullOrBlank()) {
        GUEST_AI_USAGE_SCOPE
    } else {
        accountAiUsageScope(userId)
    }
}

interface ScopedAiUsageStore {
    fun getUsageState(scope: String, dateKey: String): AiDailyUsageState
    fun saveUsageState(scope: String, state: AiDailyUsageState)
    fun migrateLegacyUsage(scope: String, legacyState: AiDailyUsageState?): Boolean
}

object NoOpScopedAiUsageStore : ScopedAiUsageStore {
    override fun getUsageState(scope: String, dateKey: String): AiDailyUsageState {
        return AiDailyUsageState(dateKey = dateKey, usedCount = 0)
    }

    override fun saveUsageState(scope: String, state: AiDailyUsageState) = Unit

    override fun migrateLegacyUsage(scope: String, legacyState: AiDailyUsageState?): Boolean = false
}

class SharedPreferencesScopedAiUsageStore(context: Context) : ScopedAiUsageStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getUsageState(scope: String, dateKey: String): AiDailyUsageState {
        val storedDateKey = preferences.getString(dateKeyPreference(scope), null)
        if (storedDateKey != dateKey) {
            return AiDailyUsageState(dateKey = dateKey, usedCount = 0)
        }
        return AiDailyUsageState(
            dateKey = dateKey,
            usedCount = preferences.getInt(usedCountPreference(scope), 0).coerceAtLeast(0)
        )
    }

    override fun saveUsageState(scope: String, state: AiDailyUsageState) {
        preferences.edit()
            .putString(dateKeyPreference(scope), state.dateKey)
            .putInt(usedCountPreference(scope), state.usedCount.coerceAtLeast(0))
            .apply()
    }

    override fun migrateLegacyUsage(scope: String, legacyState: AiDailyUsageState?): Boolean {
        if (preferences.getBoolean(KEY_LEGACY_MIGRATION_COMPLETE, false)) {
            return false
        }

        val editor = preferences.edit()
        legacyState?.let { state ->
            val usedCount = state.usedCount.coerceAtLeast(0)
            editor
                .putString(dateKeyPreference(scope), state.dateKey)
                .putInt(usedCountPreference(scope), usedCount)
                .putString(dateKeyPreference(PROVIDER_AI_USAGE_SCOPE), state.dateKey)
                .putInt(usedCountPreference(PROVIDER_AI_USAGE_SCOPE), usedCount)
        }
        editor
            .putBoolean(KEY_LEGACY_MIGRATION_COMPLETE, true)
            .apply()
        return true
    }

    private fun dateKeyPreference(scope: String): String = "$scope.$KEY_DATE"

    private fun usedCountPreference(scope: String): String = "$scope.$KEY_USED_COUNT"

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-scoped-ai-usage"
        const val KEY_DATE = "date"
        const val KEY_USED_COUNT = "used_count"
        const val KEY_LEGACY_MIGRATION_COMPLETE = "legacy_migration_complete"
    }
}
