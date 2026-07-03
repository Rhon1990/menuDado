package com.menudado.data

import android.content.Context

data class GuestDailyUsageState(
    val dateKey: String,
    val savedMenuCount: Int,
    val generatedIdeaCount: Int,
    val analysisCount: Int
)

interface GuestUsageStore {
    fun getUsageState(): GuestDailyUsageState?
    fun saveUsageState(state: GuestDailyUsageState)
}

object NoOpGuestUsageStore : GuestUsageStore {
    override fun getUsageState(): GuestDailyUsageState? = null
    override fun saveUsageState(state: GuestDailyUsageState) = Unit
}

class SharedPreferencesGuestUsageStore(context: Context) : GuestUsageStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getUsageState(): GuestDailyUsageState? {
        val dateKey = preferences.getString(KEY_DATE, null) ?: return null
        return GuestDailyUsageState(
            dateKey = dateKey,
            savedMenuCount = preferences.getInt(KEY_SAVED_MENU_COUNT, 0).coerceAtLeast(0),
            generatedIdeaCount = preferences.getInt(KEY_GENERATED_IDEA_COUNT, 0).coerceAtLeast(0),
            analysisCount = preferences.getInt(KEY_ANALYSIS_COUNT, 0).coerceAtLeast(0)
        )
    }

    override fun saveUsageState(state: GuestDailyUsageState) {
        preferences.edit()
            .putString(KEY_DATE, state.dateKey)
            .putInt(KEY_SAVED_MENU_COUNT, state.savedMenuCount.coerceAtLeast(0))
            .putInt(KEY_GENERATED_IDEA_COUNT, state.generatedIdeaCount.coerceAtLeast(0))
            .putInt(KEY_ANALYSIS_COUNT, state.analysisCount.coerceAtLeast(0))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-guest-daily-usage"
        const val KEY_DATE = "date"
        const val KEY_SAVED_MENU_COUNT = "saved_menu_count"
        const val KEY_GENERATED_IDEA_COUNT = "generated_idea_count"
        const val KEY_ANALYSIS_COUNT = "analysis_count"
    }
}
