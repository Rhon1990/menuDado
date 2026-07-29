package com.menudado.data

import android.content.Context
import com.menudado.domain.MenuAudience

interface FormAudienceSelectionStore {
    fun getSelectedAudience(): MenuAudience?
    fun saveSelectedAudience(audience: MenuAudience)
}

object NoOpFormAudienceSelectionStore : FormAudienceSelectionStore {
    override fun getSelectedAudience(): MenuAudience? = null
    override fun saveSelectedAudience(audience: MenuAudience) = Unit
}

class SharedPreferencesFormAudienceSelectionStore(
    context: Context
) : FormAudienceSelectionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getSelectedAudience(): MenuAudience? {
        val storedValue = preferences.getString(KEY_SELECTED_AUDIENCE, null) ?: return null
        return runCatching { MenuAudience.valueOf(storedValue) }.getOrNull()
    }

    override fun saveSelectedAudience(audience: MenuAudience) {
        preferences.edit()
            .putString(KEY_SELECTED_AUDIENCE, audience.name)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-form-audience-selection"
        const val KEY_SELECTED_AUDIENCE = "selected_audience"
    }
}
