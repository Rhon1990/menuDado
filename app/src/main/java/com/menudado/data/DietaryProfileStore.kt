package com.menudado.data

import android.content.Context
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile

interface DietaryProfileStore {
    fun getProfile(): DietaryProfile
    fun saveProfile(profile: DietaryProfile)
}

object NoOpDietaryProfileStore : DietaryProfileStore {
    override fun getProfile(): DietaryProfile = DietaryProfile()
    override fun saveProfile(profile: DietaryProfile) = Unit
}

class SharedPreferencesDietaryProfileStore(context: Context) : DietaryProfileStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getProfile(): DietaryProfile {
        val allergens = preferences
            .getStringSet(KEY_ALLERGENS, emptySet())
            .orEmpty()
            .mapNotNull { value -> runCatching { DietaryAllergen.valueOf(value) }.getOrNull() }
            .toSet()

        return DietaryProfile(
            isVegan = preferences.getBoolean(KEY_IS_VEGAN, false),
            hasAllergies = preferences.getBoolean(KEY_HAS_ALLERGIES, false),
            allergens = allergens,
            otherAvoidances = preferences.getString(KEY_OTHER_AVOIDANCES, null).orEmpty()
        )
    }

    override fun saveProfile(profile: DietaryProfile) {
        preferences.edit()
            .putBoolean(KEY_IS_VEGAN, profile.isVegan)
            .putBoolean(KEY_HAS_ALLERGIES, profile.hasAllergies)
            .putStringSet(KEY_ALLERGENS, profile.allergens.map { it.name }.toSet())
            .putString(KEY_OTHER_AVOIDANCES, profile.otherAvoidances.trim())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-dietary-profile"
        const val KEY_IS_VEGAN = "is_vegan"
        const val KEY_HAS_ALLERGIES = "has_allergies"
        const val KEY_ALLERGENS = "allergens"
        const val KEY_OTHER_AVOIDANCES = "other_avoidances"
    }
}
