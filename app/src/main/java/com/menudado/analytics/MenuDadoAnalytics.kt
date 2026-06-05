package com.menudado.analytics

import com.menudado.domain.MealType
import com.menudado.domain.HealthStatus

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val localeCountry: String,
    val timeZone: String
)

interface MenuDadoAnalytics {
    fun trackAppOpened(deviceInfo: DeviceInfo? = null)

    fun trackMenuSaved(
        mealType: MealType,
        hasAiAnalysis: Boolean,
        hasCalories: Boolean,
        menuCount: Int
    )

    fun trackMenuDeleted(mealType: MealType, hadAiAnalysis: Boolean)

    fun trackFirstMenuCreated(mealType: MealType)

    fun trackMenuInventoryChanged(menuCount: Int, analyzedMenuCount: Int, pendingAnalysisCount: Int)

    fun trackMenuFormStarted(firstEditedField: String, mealType: MealType)

    fun trackMealTypeSelected(mealType: MealType, formHasContent: Boolean)

    fun trackMenuSaveBlocked(reason: String, hasName: Boolean, hasDescription: Boolean)

    fun trackDiceRolled(
        filter: MealType?,
        resultMealType: MealType?,
        menuCount: Int,
        availableCandidateCount: Int
    )

    fun trackDiceFilterSelected(filter: MealType?, menuCount: Int)

    fun trackDiceEmptyResult(filter: MealType?, availableCandidateCount: Int)

    fun trackMenuCardOpened(mealType: MealType, hasAiAnalysis: Boolean, menuCount: Int)

    fun trackOnboardingShown()

    fun trackOnboardingCompleted(action: String)

    fun trackAboutAppOpened()

    fun trackAiMenuGenerationStarted(mealType: MealType, avoidIdeaCount: Int)

    fun trackAiMenuGenerationFinished(
        mealType: MealType,
        success: Boolean,
        healthStatus: HealthStatus?,
        failureType: String?
    )

    fun trackAiAnalysisStarted(scope: String, mealType: MealType?, menuCount: Int)

    fun trackAiAnalysisFinished(
        scope: String,
        mealType: MealType?,
        success: Boolean,
        analyzedCount: Int,
        healthStatus: HealthStatus?,
        failureType: String?
    )

    fun trackAiDailyLimitReached(source: String)
}

object NoOpMenuDadoAnalytics : MenuDadoAnalytics {
    override fun trackAppOpened(deviceInfo: DeviceInfo?) = Unit

    override fun trackMenuSaved(
        mealType: MealType,
        hasAiAnalysis: Boolean,
        hasCalories: Boolean,
        menuCount: Int
    ) = Unit

    override fun trackMenuDeleted(mealType: MealType, hadAiAnalysis: Boolean) = Unit

    override fun trackFirstMenuCreated(mealType: MealType) = Unit

    override fun trackMenuInventoryChanged(menuCount: Int, analyzedMenuCount: Int, pendingAnalysisCount: Int) = Unit

    override fun trackMenuFormStarted(firstEditedField: String, mealType: MealType) = Unit

    override fun trackMealTypeSelected(mealType: MealType, formHasContent: Boolean) = Unit

    override fun trackMenuSaveBlocked(reason: String, hasName: Boolean, hasDescription: Boolean) = Unit

    override fun trackDiceRolled(
        filter: MealType?,
        resultMealType: MealType?,
        menuCount: Int,
        availableCandidateCount: Int
    ) = Unit

    override fun trackDiceFilterSelected(filter: MealType?, menuCount: Int) = Unit

    override fun trackDiceEmptyResult(filter: MealType?, availableCandidateCount: Int) = Unit

    override fun trackMenuCardOpened(mealType: MealType, hasAiAnalysis: Boolean, menuCount: Int) = Unit

    override fun trackOnboardingShown() = Unit

    override fun trackOnboardingCompleted(action: String) = Unit

    override fun trackAboutAppOpened() = Unit

    override fun trackAiMenuGenerationStarted(mealType: MealType, avoidIdeaCount: Int) = Unit

    override fun trackAiMenuGenerationFinished(
        mealType: MealType,
        success: Boolean,
        healthStatus: HealthStatus?,
        failureType: String?
    ) = Unit

    override fun trackAiAnalysisStarted(scope: String, mealType: MealType?, menuCount: Int) = Unit

    override fun trackAiAnalysisFinished(
        scope: String,
        mealType: MealType?,
        success: Boolean,
        analyzedCount: Int,
        healthStatus: HealthStatus?,
        failureType: String?
    ) = Unit

    override fun trackAiDailyLimitReached(source: String) = Unit
}
