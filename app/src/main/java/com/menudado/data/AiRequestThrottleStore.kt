package com.menudado.data

import android.content.Context

interface AiRequestThrottleStore {
    fun getLastRequestAtMillis(): Long?
    fun saveLastRequestAtMillis(requestAtMillis: Long)
    fun clearLastRequest()
}

object NoOpAiRequestThrottleStore : AiRequestThrottleStore {
    override fun getLastRequestAtMillis(): Long? = null
    override fun saveLastRequestAtMillis(requestAtMillis: Long) = Unit
    override fun clearLastRequest() = Unit
}

class SharedPreferencesAiRequestThrottleStore(context: Context) : AiRequestThrottleStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getLastRequestAtMillis(): Long? {
        return preferences.getLong(KEY_LAST_REQUEST_AT_MILLIS, 0L).takeIf { it > 0L }
    }

    override fun saveLastRequestAtMillis(requestAtMillis: Long) {
        preferences.edit()
            .putLong(KEY_LAST_REQUEST_AT_MILLIS, requestAtMillis)
            .apply()
    }

    override fun clearLastRequest() {
        preferences.edit()
            .remove(KEY_LAST_REQUEST_AT_MILLIS)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-ai-request-throttle"
        const val KEY_LAST_REQUEST_AT_MILLIS = "last_request_at_millis"
    }
}
