package com.menudado.analytics

import com.menudado.domain.MealType
import com.menudado.domain.HealthStatus
import com.menudado.domain.MenuAudience

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

    fun trackCtaTapped(screen: String, cta: String)

    fun trackMenuDeleted(mealType: MealType, hadAiAnalysis: Boolean)

    fun trackFirstMenuCreated(mealType: MealType)

    fun trackMenuInventoryChanged(menuCount: Int, analyzedMenuCount: Int, pendingAnalysisCount: Int)

    fun trackMenuFormStarted(firstEditedField: String, mealType: MealType)

    fun trackMealTypeSelected(mealType: MealType, formHasContent: Boolean)

    fun trackAudienceFilterSelected(source: String, audience: MenuAudience?, menuCount: Int)

    fun trackMenuSaveBlocked(reason: String, hasName: Boolean, hasDescription: Boolean)

    fun trackMenuEditStarted(
        mealType: MealType,
        audience: MenuAudience,
        hasAiAnalysis: Boolean,
        hasPhoto: Boolean,
        menuCount: Int
    )

    fun trackMenuEditSaved(
        mealType: MealType,
        audience: MenuAudience,
        changedRecipe: Boolean,
        hasAiAnalysis: Boolean,
        hasPhoto: Boolean,
        menuCount: Int
    )

    fun trackMenuPhotoUpdated(mealType: MealType, audience: MenuAudience, hasPhoto: Boolean, menuCount: Int)

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

    fun trackDietaryProfileOpened(activeAudienceCount: Int)

    fun trackDietaryProfileAudienceSelected(audience: MenuAudience)

    fun trackDietaryProfileUpdated(audience: MenuAudience, fieldGroup: String, activeAudienceCount: Int)

    fun trackMenuListViewMoreOpened(audience: MenuAudience, menuCount: Int)

    fun trackBackendSyncRetried(source: String, pendingMenuCount: Int)

    fun trackBackendSyncFinished(source: String, status: String, pendingMenuCount: Int)

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

    override fun trackCtaTapped(screen: String, cta: String) = Unit

    override fun trackMenuDeleted(mealType: MealType, hadAiAnalysis: Boolean) = Unit

    override fun trackFirstMenuCreated(mealType: MealType) = Unit

    override fun trackMenuInventoryChanged(menuCount: Int, analyzedMenuCount: Int, pendingAnalysisCount: Int) = Unit

    override fun trackMenuFormStarted(firstEditedField: String, mealType: MealType) = Unit

    override fun trackMealTypeSelected(mealType: MealType, formHasContent: Boolean) = Unit

    override fun trackAudienceFilterSelected(source: String, audience: MenuAudience?, menuCount: Int) = Unit

    override fun trackMenuSaveBlocked(reason: String, hasName: Boolean, hasDescription: Boolean) = Unit

    override fun trackMenuEditStarted(
        mealType: MealType,
        audience: MenuAudience,
        hasAiAnalysis: Boolean,
        hasPhoto: Boolean,
        menuCount: Int
    ) = Unit

    override fun trackMenuEditSaved(
        mealType: MealType,
        audience: MenuAudience,
        changedRecipe: Boolean,
        hasAiAnalysis: Boolean,
        hasPhoto: Boolean,
        menuCount: Int
    ) = Unit

    override fun trackMenuPhotoUpdated(mealType: MealType, audience: MenuAudience, hasPhoto: Boolean, menuCount: Int) = Unit

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

    override fun trackDietaryProfileOpened(activeAudienceCount: Int) = Unit

    override fun trackDietaryProfileAudienceSelected(audience: MenuAudience) = Unit

    override fun trackDietaryProfileUpdated(audience: MenuAudience, fieldGroup: String, activeAudienceCount: Int) = Unit

    override fun trackMenuListViewMoreOpened(audience: MenuAudience, menuCount: Int) = Unit

    override fun trackBackendSyncRetried(source: String, pendingMenuCount: Int) = Unit

    override fun trackBackendSyncFinished(source: String, status: String, pendingMenuCount: Int) = Unit

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
