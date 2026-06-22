package com.menudado.domain

import java.util.Locale

enum class AppLanguage(
    val promptLanguageName: String
) {
    SPANISH("Spanish"),
    ENGLISH("English"),
    FRENCH("French");

    companion object {
        fun fromLocale(locale: Locale = Locale.getDefault()): AppLanguage {
            return when (locale.language.lowercase(Locale.ROOT)) {
                "en" -> ENGLISH
                "fr" -> FRENCH
                else -> SPANISH
            }
        }
    }
}

fun MealType.localizedLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SPANISH -> label
        AppLanguage.ENGLISH -> when (this) {
            MealType.BREAKFAST -> "Breakfast"
            MealType.LUNCH -> "Lunch"
            MealType.DINNER -> "Dinner"
        }
        AppLanguage.FRENCH -> when (this) {
            MealType.BREAKFAST -> "Petit-déjeuner"
            MealType.LUNCH -> "Déjeuner"
            MealType.DINNER -> "Dîner"
        }
    }
}

fun MenuAudience.localizedLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SPANISH -> when (this) {
            MenuAudience.ADULT -> "Adulto"
            MenuAudience.CHILD -> "Peques"
            MenuAudience.BABY -> "Bebé"
        }
        AppLanguage.ENGLISH -> when (this) {
            MenuAudience.ADULT -> "Adult"
            MenuAudience.CHILD -> "Kids"
            MenuAudience.BABY -> "Baby"
        }
        AppLanguage.FRENCH -> when (this) {
            MenuAudience.ADULT -> "Adulte"
            MenuAudience.CHILD -> "Enfants"
            MenuAudience.BABY -> "Bébé"
        }
    }
}

fun HealthStatus.localizedLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SPANISH -> label
        AppLanguage.ENGLISH -> when (this) {
            HealthStatus.HEALTHY -> "Healthy"
            HealthStatus.IMPROVABLE -> "Moderate"
            HealthStatus.UNHEALTHY -> "Not healthy"
            HealthStatus.UNKNOWN -> "Not analyzed"
        }
        AppLanguage.FRENCH -> when (this) {
            HealthStatus.HEALTHY -> "Sain"
            HealthStatus.IMPROVABLE -> "Intermédiaire"
            HealthStatus.UNHEALTHY -> "Peu sain"
            HealthStatus.UNKNOWN -> "Non analysé"
        }
    }
}
