package com.menudado.data

import android.content.Context
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MenuAudience

interface DietaryProfileStore {
    fun getProfile(audience: MenuAudience = MenuAudience.ADULT): DietaryProfile
    fun saveProfile(profile: DietaryProfile, audience: MenuAudience = MenuAudience.ADULT)
}

object NoOpDietaryProfileStore : DietaryProfileStore {
    override fun getProfile(audience: MenuAudience): DietaryProfile = defaultProfile(audience)
    override fun saveProfile(profile: DietaryProfile, audience: MenuAudience) = Unit

    private fun defaultProfile(audience: MenuAudience): DietaryProfile {
        return DietaryProfile(
            isEnabled = audience == MenuAudience.ADULT,
            ageRange = audience.defaultAgeRange
        )
    }
}

class SharedPreferencesDietaryProfileStore(context: Context) : DietaryProfileStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getProfile(audience: MenuAudience): DietaryProfile {
        val allergens = preferences
            .getStringSet(KEY_ALLERGENS.forAudience(audience), emptySet())
            .orEmpty()
            .mapNotNull { value -> runCatching { DietaryAllergen.valueOf(value) }.getOrNull() }
            .toSet()

        return DietaryProfile(
            isEnabled = preferences.getBoolean(KEY_IS_ENABLED.forAudience(audience), audience == MenuAudience.ADULT),
            ageRange = preferences.getString(KEY_AGE_RANGE.forAudience(audience), null)
                ?.takeIf { it.isNotBlank() }
                ?: audience.defaultAgeRange,
            isPregnant = preferences.getBoolean(KEY_IS_PREGNANT.forAudience(audience), false),
            isVegan = preferences.getBoolean(KEY_IS_VEGAN.forAudience(audience), false),
            hasAllergies = preferences.getBoolean(KEY_HAS_ALLERGIES.forAudience(audience), false),
            allergens = allergens,
            otherAvoidances = preferences.getString(KEY_OTHER_AVOIDANCES.forAudience(audience), null).orEmpty()
        )
    }

    override fun saveProfile(profile: DietaryProfile, audience: MenuAudience) {
        preferences.edit()
            .putBoolean(KEY_IS_ENABLED.forAudience(audience), profile.isEnabled)
            .putString(KEY_AGE_RANGE.forAudience(audience), profile.ageRange.trim())
            .putBoolean(KEY_IS_PREGNANT.forAudience(audience), profile.isPregnant)
            .putBoolean(KEY_IS_VEGAN.forAudience(audience), profile.isVegan)
            .putBoolean(KEY_HAS_ALLERGIES.forAudience(audience), profile.hasAllergies)
            .putStringSet(KEY_ALLERGENS.forAudience(audience), profile.allergens.map { it.name }.toSet())
            .putString(KEY_OTHER_AVOIDANCES.forAudience(audience), profile.otherAvoidances.trim())
            .apply()
    }

    private fun String.forAudience(audience: MenuAudience): String {
        return if (audience == MenuAudience.ADULT) this else "${this}_${audience.name.lowercase()}"
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-dietary-profile"
        const val KEY_IS_ENABLED = "is_enabled"
        const val KEY_AGE_RANGE = "age_range"
        const val KEY_IS_PREGNANT = "is_pregnant"
        const val KEY_IS_VEGAN = "is_vegan"
        const val KEY_HAS_ALLERGIES = "has_allergies"
        const val KEY_ALLERGENS = "allergens"
        const val KEY_OTHER_AVOIDANCES = "other_avoidances"
    }
}
