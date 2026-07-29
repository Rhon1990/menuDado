package com.menudado.ui

import com.menudado.domain.AppLanguage
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import com.menudado.domain.localizedLabel

internal enum class AiGenerationFailureReason {
    DAILY_LIMIT,
    HIGH_DEMAND,
    TIMEOUT,
    CONNECTION,
    SERVICE_UNAVAILABLE
}

internal fun contextualAiGenerationFailureMessage(
    language: AppLanguage,
    reason: AiGenerationFailureReason,
    mealType: MealType,
    audience: MenuAudience,
    profile: DietaryProfile
): String {
    val ageRange = profile.ageRange.trim().ifBlank { audience.defaultAgeRange }
    val context = "${mealType.localizedLabel(language)} · " +
        "${audience.localizedLabel(language)} ($ageRange)"
    val profileSuffix = if (profile.hasRestrictions) {
        language.currentProfileSuffix()
    } else {
        ""
    }

    return when (language) {
        AppLanguage.SPANISH -> reason.spanishMessage(context, profileSuffix)
        AppLanguage.ENGLISH -> reason.englishMessage(context, profileSuffix)
        AppLanguage.FRENCH -> reason.frenchMessage(context, profileSuffix)
    }
}

private fun AppLanguage.currentProfileSuffix(): String = when (this) {
    AppLanguage.SPANISH -> " con tu perfil actual"
    AppLanguage.ENGLISH -> " with your current profile"
    AppLanguage.FRENCH -> " avec votre profil actuel"
}

private fun AiGenerationFailureReason.spanishMessage(
    context: String,
    profileSuffix: String
): String = when (this) {
    AiGenerationFailureReason.DAILY_LIMIT ->
        "Hoy no podemos preparar más ideas para $context. Vuelve a intentarlo mañana."
    AiGenerationFailureReason.HIGH_DEMAND ->
        "La IA está con mucha demanda y no pudo preparar una idea para " +
            "$context$profileSuffix. Inténtalo más tarde."
    AiGenerationFailureReason.TIMEOUT ->
        "La IA tardó demasiado y no pudo preparar una idea para " +
            "$context$profileSuffix. Revisa tu conexión e inténtalo de nuevo."
    AiGenerationFailureReason.CONNECTION ->
        "No pudimos preparar una idea para $context$profileSuffix. " +
            "Revisa tu conexión e inténtalo de nuevo."
    AiGenerationFailureReason.SERVICE_UNAVAILABLE ->
        "No pudimos preparar una idea para $context$profileSuffix en este momento. " +
            "Inténtalo nuevamente más tarde."
}

private fun AiGenerationFailureReason.englishMessage(
    context: String,
    profileSuffix: String
): String = when (this) {
    AiGenerationFailureReason.DAILY_LIMIT ->
        "We can't prepare more ideas today for $context. Try again tomorrow."
    AiGenerationFailureReason.HIGH_DEMAND ->
        "AI is in high demand and couldn't prepare an idea for " +
            "$context$profileSuffix. Try again later."
    AiGenerationFailureReason.TIMEOUT ->
        "AI took too long and couldn't prepare an idea for " +
            "$context$profileSuffix. Check your connection and try again."
    AiGenerationFailureReason.CONNECTION ->
        "We couldn't prepare an idea for $context$profileSuffix. " +
            "Check your connection and try again."
    AiGenerationFailureReason.SERVICE_UNAVAILABLE ->
        "We couldn't prepare an idea for $context$profileSuffix right now. " +
            "Try again later."
}

private fun AiGenerationFailureReason.frenchMessage(
    context: String,
    profileSuffix: String
): String = when (this) {
    AiGenerationFailureReason.DAILY_LIMIT ->
        "Nous ne pouvons pas préparer plus d'idées aujourd'hui pour $context. " +
            "Réessayez demain."
    AiGenerationFailureReason.HIGH_DEMAND ->
        "L'IA est très demandée et n'a pas pu préparer d'idée pour " +
            "$context$profileSuffix. Réessayez plus tard."
    AiGenerationFailureReason.TIMEOUT ->
        "L'IA a mis trop de temps et n'a pas pu préparer d'idée pour " +
            "$context$profileSuffix. Vérifiez votre connexion et réessayez."
    AiGenerationFailureReason.CONNECTION ->
        "Nous n'avons pas pu préparer d'idée pour $context$profileSuffix. " +
            "Vérifiez votre connexion et réessayez."
    AiGenerationFailureReason.SERVICE_UNAVAILABLE ->
        "Nous n'avons pas pu préparer d'idée pour $context$profileSuffix pour le moment. " +
            "Réessayez plus tard."
}
