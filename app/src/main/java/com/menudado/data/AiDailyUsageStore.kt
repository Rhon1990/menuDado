package com.menudado.data

import android.content.Context

data class AiDailyUsageState(
    val dateKey: String,
    val usedCount: Int
)

interface AiDailyUsageStore {
    fun getUsageState(): AiDailyUsageState?
    fun saveUsageState(state: AiDailyUsageState)
}

object NoOpAiDailyUsageStore : AiDailyUsageStore {
    override fun getUsageState(): AiDailyUsageState? = null
    override fun saveUsageState(state: AiDailyUsageState) = Unit
}

class SharedPreferencesAiDailyUsageStore(context: Context) : AiDailyUsageStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getUsageState(): AiDailyUsageState? {
        val dateKey = preferences.getString(KEY_DATE, null) ?: return null
        return AiDailyUsageState(
            dateKey = dateKey,
            usedCount = preferences.getInt(KEY_USED_COUNT, 0).coerceAtLeast(0)
        )
    }

    override fun saveUsageState(state: AiDailyUsageState) {
        preferences.edit()
            .putString(KEY_DATE, state.dateKey)
            .putInt(KEY_USED_COUNT, state.usedCount.coerceAtLeast(0))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-ai-daily-usage"
        const val KEY_DATE = "date"
        const val KEY_USED_COUNT = "used_count"
    }
}
