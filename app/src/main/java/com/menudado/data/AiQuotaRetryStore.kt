package com.menudado.data

import android.content.Context

data class AiQuotaRetryState(
    val retryAtMillis: Long,
    val consecutiveFailures: Int
)

interface AiQuotaRetryStore {
    fun getRetryState(): AiQuotaRetryState?
    fun saveRetryState(state: AiQuotaRetryState)
    fun clearRetryState()
}

object NoOpAiQuotaRetryStore : AiQuotaRetryStore {
    override fun getRetryState(): AiQuotaRetryState? = null
    override fun saveRetryState(state: AiQuotaRetryState) = Unit
    override fun clearRetryState() = Unit
}

class SharedPreferencesAiQuotaRetryStore(context: Context) : AiQuotaRetryStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getRetryState(): AiQuotaRetryState? {
        val retryAtMillis = preferences.getLong(KEY_RETRY_AT_MILLIS, 0L).takeIf { it > 0L }
            ?: return null
        return AiQuotaRetryState(
            retryAtMillis = retryAtMillis,
            consecutiveFailures = preferences.getInt(KEY_CONSECUTIVE_FAILURES, 1).coerceAtLeast(1)
        )
    }

    override fun saveRetryState(state: AiQuotaRetryState) {
        preferences.edit()
            .putLong(KEY_RETRY_AT_MILLIS, state.retryAtMillis)
            .putInt(KEY_CONSECUTIVE_FAILURES, state.consecutiveFailures)
            .apply()
    }

    override fun clearRetryState() {
        preferences.edit()
            .remove(KEY_RETRY_AT_MILLIS)
            .remove(KEY_CONSECUTIVE_FAILURES)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-ai-quota"
        const val KEY_RETRY_AT_MILLIS = "retry_at_millis"
        const val KEY_CONSECUTIVE_FAILURES = "consecutive_failures"
    }
}
