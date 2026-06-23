package com.menudado.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience

class FirebaseMenuDadoAnalytics(
    private val firebaseAnalytics: FirebaseAnalytics
) : MenuDadoAnalytics {
    override fun trackAppOpened(deviceInfo: DeviceInfo?) {
        logEvent(EVENT_APP_OPENED) {
            deviceInfo?.appendTo(this)
        }
        deviceInfo?.setAsUserProperties()
    }

    override fun trackMenuSaved(
        mealType: MealType,
        hasAiAnalysis: Boolean,
        hasCalories: Boolean,
        menuCount: Int
    ) {
        logEvent(EVENT_MENU_SAVED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
            putBooleanAsLong(PARAM_HAS_AI_ANALYSIS, hasAiAnalysis)
            putBooleanAsLong(PARAM_HAS_CALORIES, hasCalories)
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
        }
    }

    override fun trackCtaTapped(screen: String, cta: String) {
        logEvent(EVENT_CTA_TAPPED) {
            putString(PARAM_SCREEN, screen.sanitized())
            putString(PARAM_CTA, cta.sanitized())
        }
    }

    override fun trackMenuDeleted(mealType: MealType, hadAiAnalysis: Boolean) {
        logEvent(EVENT_MENU_DELETED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
            putBooleanAsLong(PARAM_HAS_AI_ANALYSIS, hadAiAnalysis)
        }
    }

    override fun trackFirstMenuCreated(mealType: MealType) {
        logEvent(EVENT_FIRST_MENU_CREATED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
        }
    }

    override fun trackMenuInventoryChanged(menuCount: Int, analyzedMenuCount: Int, pendingAnalysisCount: Int) {
        logEvent(EVENT_MENU_INVENTORY_CHANGED) {
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
            putLong(PARAM_ANALYZED_MENU_COUNT, analyzedMenuCount.toLong())
            putLong(PARAM_PENDING_ANALYSIS_COUNT, pendingAnalysisCount.toLong())
        }
    }

    override fun trackMenuFormStarted(firstEditedField: String, mealType: MealType) {
        logEvent(EVENT_MENU_FORM_STARTED) {
            putString(PARAM_FIELD, firstEditedField.sanitized())
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
        }
    }

    override fun trackMealTypeSelected(mealType: MealType, formHasContent: Boolean) {
        logEvent(EVENT_MEAL_TYPE_SELECTED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
            putBooleanAsLong(PARAM_FORM_HAS_CONTENT, formHasContent)
        }
    }

    override fun trackAudienceFilterSelected(source: String, audience: MenuAudience?, menuCount: Int) {
        logEvent(EVENT_AUDIENCE_FILTER_SELECTED) {
            putString(PARAM_SOURCE, source.sanitized())
            putString(PARAM_AUDIENCE, audience?.analyticsName() ?: VALUE_ALL)
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
        }
    }

    override fun trackMenuSaveBlocked(reason: String, hasName: Boolean, hasDescription: Boolean) {
        logEvent(EVENT_MENU_SAVE_BLOCKED) {
            putString(PARAM_REASON, reason.sanitized())
            putBooleanAsLong(PARAM_HAS_NAME, hasName)
            putBooleanAsLong(PARAM_HAS_DESCRIPTION, hasDescription)
        }
    }

    override fun trackMenuEditStarted(
        mealType: MealType,
        audience: MenuAudience,
        hasAiAnalysis: Boolean,
        hasPhoto: Boolean,
        menuCount: Int
    ) {
        logEvent(EVENT_MENU_EDIT_STARTED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
            putString(PARAM_AUDIENCE, audience.analyticsName())
            putBooleanAsLong(PARAM_HAS_AI_ANALYSIS, hasAiAnalysis)
            putBooleanAsLong(PARAM_HAS_PHOTO, hasPhoto)
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
        }
    }

    override fun trackMenuEditSaved(
        mealType: MealType,
        audience: MenuAudience,
        changedRecipe: Boolean,
        hasAiAnalysis: Boolean,
        hasPhoto: Boolean,
        menuCount: Int
    ) {
        logEvent(EVENT_MENU_EDIT_SAVED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
            putString(PARAM_AUDIENCE, audience.analyticsName())
            putBooleanAsLong(PARAM_CHANGED_RECIPE, changedRecipe)
            putBooleanAsLong(PARAM_HAS_AI_ANALYSIS, hasAiAnalysis)
            putBooleanAsLong(PARAM_HAS_PHOTO, hasPhoto)
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
        }
    }

    override fun trackMenuPhotoUpdated(mealType: MealType, audience: MenuAudience, hasPhoto: Boolean, menuCount: Int) {
        logEvent(EVENT_MENU_PHOTO_UPDATED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
            putString(PARAM_AUDIENCE, audience.analyticsName())
            putBooleanAsLong(PARAM_HAS_PHOTO, hasPhoto)
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
        }
    }

    override fun trackDiceRolled(
        filter: MealType?,
        resultMealType: MealType?,
        menuCount: Int,
        availableCandidateCount: Int
    ) {
        logEvent(EVENT_DICE_ROLLED) {
            putString(PARAM_FILTER_TYPE, filter?.analyticsName() ?: VALUE_ALL)
            putBooleanAsLong(PARAM_RESULT_FOUND, resultMealType != null)
            putString(PARAM_RESULT_MEAL_TYPE, resultMealType?.analyticsName() ?: VALUE_NONE)
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
            putLong(PARAM_AVAILABLE_CANDIDATE_COUNT, availableCandidateCount.toLong())
        }
    }

    override fun trackDiceFilterSelected(filter: MealType?, menuCount: Int) {
        logEvent(EVENT_DICE_FILTER_SELECTED) {
            putString(PARAM_FILTER_TYPE, filter?.analyticsName() ?: VALUE_ALL)
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
        }
    }

    override fun trackDiceEmptyResult(filter: MealType?, availableCandidateCount: Int) {
        logEvent(EVENT_DICE_EMPTY_RESULT) {
            putString(PARAM_FILTER_TYPE, filter?.analyticsName() ?: VALUE_ALL)
            putLong(PARAM_AVAILABLE_CANDIDATE_COUNT, availableCandidateCount.toLong())
        }
    }

    override fun trackMenuCardOpened(mealType: MealType, hasAiAnalysis: Boolean, menuCount: Int) {
        logEvent(EVENT_MENU_CARD_OPENED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
            putBooleanAsLong(PARAM_HAS_AI_ANALYSIS, hasAiAnalysis)
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
        }
    }

    override fun trackOnboardingShown() {
        logEvent(EVENT_ONBOARDING_SHOWN)
    }

    override fun trackOnboardingCompleted(action: String) {
        logEvent(EVENT_ONBOARDING_COMPLETED) {
            putString(PARAM_ACTION, action.sanitized())
        }
    }

    override fun trackAboutAppOpened() {
        logEvent(EVENT_ABOUT_APP_OPENED)
    }

    override fun trackDietaryProfileOpened(activeAudienceCount: Int) {
        logEvent(EVENT_DIETARY_PROFILE_OPENED) {
            putLong(PARAM_ACTIVE_AUDIENCE_COUNT, activeAudienceCount.toLong())
        }
    }

    override fun trackDietaryProfileAudienceSelected(audience: MenuAudience) {
        logEvent(EVENT_DIETARY_PROFILE_AUDIENCE_SELECTED) {
            putString(PARAM_AUDIENCE, audience.analyticsName())
        }
    }

    override fun trackDietaryProfileUpdated(audience: MenuAudience, fieldGroup: String, activeAudienceCount: Int) {
        logEvent(EVENT_DIETARY_PROFILE_UPDATED) {
            putString(PARAM_AUDIENCE, audience.analyticsName())
            putString(PARAM_FIELD_GROUP, fieldGroup.sanitized())
            putLong(PARAM_ACTIVE_AUDIENCE_COUNT, activeAudienceCount.toLong())
        }
    }

    override fun trackMenuListViewMoreOpened(audience: MenuAudience, menuCount: Int) {
        logEvent(EVENT_MENU_LIST_VIEW_MORE_OPENED) {
            putString(PARAM_AUDIENCE, audience.analyticsName())
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
        }
    }

    override fun trackBackendSyncRetried(source: String, pendingMenuCount: Int) {
        logEvent(EVENT_BACKEND_SYNC_RETRIED) {
            putString(PARAM_SOURCE, source.sanitized())
            putLong(PARAM_PENDING_MENU_COUNT, pendingMenuCount.toLong())
        }
    }

    override fun trackBackendSyncFinished(source: String, status: String, pendingMenuCount: Int) {
        logEvent(EVENT_BACKEND_SYNC_FINISHED) {
            putString(PARAM_SOURCE, source.sanitized())
            putString(PARAM_STATUS, status.sanitized())
            putLong(PARAM_PENDING_MENU_COUNT, pendingMenuCount.toLong())
        }
    }

    override fun trackAiMenuGenerationStarted(mealType: MealType, avoidIdeaCount: Int) {
        logEvent(EVENT_AI_MENU_GENERATION_STARTED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
            putLong(PARAM_AVOID_IDEA_COUNT, avoidIdeaCount.toLong())
        }
    }

    override fun trackAiMenuGenerationFinished(
        mealType: MealType,
        success: Boolean,
        healthStatus: HealthStatus?,
        failureType: String?
    ) {
        logEvent(EVENT_AI_MENU_GENERATION_FINISHED) {
            putString(PARAM_MEAL_TYPE, mealType.analyticsName())
            putString(PARAM_STATUS, success.analyticsStatus())
            putString(PARAM_HEALTH_STATUS, healthStatus?.analyticsName() ?: VALUE_UNKNOWN)
            putString(PARAM_FAILURE_TYPE, failureType ?: VALUE_NONE)
        }
    }

    override fun trackAiAnalysisStarted(scope: String, mealType: MealType?, menuCount: Int) {
        logEvent(EVENT_AI_ANALYSIS_STARTED) {
            putString(PARAM_SCOPE, scope)
            putString(PARAM_MEAL_TYPE, mealType?.analyticsName() ?: VALUE_UNKNOWN)
            putLong(PARAM_MENU_COUNT, menuCount.toLong())
        }
    }

    override fun trackAiAnalysisFinished(
        scope: String,
        mealType: MealType?,
        success: Boolean,
        analyzedCount: Int,
        healthStatus: HealthStatus?,
        failureType: String?
    ) {
        logEvent(EVENT_AI_ANALYSIS_FINISHED) {
            putString(PARAM_SCOPE, scope)
            putString(PARAM_MEAL_TYPE, mealType?.analyticsName() ?: VALUE_UNKNOWN)
            putString(PARAM_STATUS, success.analyticsStatus())
            putLong(PARAM_ANALYZED_COUNT, analyzedCount.toLong())
            putString(PARAM_HEALTH_STATUS, healthStatus?.analyticsName() ?: VALUE_UNKNOWN)
            putString(PARAM_FAILURE_TYPE, failureType ?: VALUE_NONE)
        }
    }

    override fun trackAiDailyLimitReached(source: String) {
        logEvent(EVENT_AI_DAILY_LIMIT_REACHED) {
            putString(PARAM_SOURCE, source)
        }
    }

    private fun logEvent(name: String, buildParams: Bundle.() -> Unit = {}) {
        runCatching {
            firebaseAnalytics.logEvent(name, Bundle().apply(buildParams))
        }
    }

    private fun DeviceInfo.appendTo(bundle: Bundle) {
        bundle.putString(PARAM_DEVICE_MANUFACTURER, manufacturer.sanitized())
        bundle.putString(PARAM_DEVICE_MODEL, model.sanitized())
        bundle.putString(PARAM_ANDROID_VERSION, androidVersion.sanitized())
        bundle.putString(PARAM_LOCALE_COUNTRY, localeCountry.sanitized())
        bundle.putString(PARAM_TIME_ZONE, timeZone.sanitized())
    }

    private fun DeviceInfo.setAsUserProperties() {
        runCatching {
            firebaseAnalytics.setUserProperty(PARAM_DEVICE_MANUFACTURER, manufacturer.sanitized())
            firebaseAnalytics.setUserProperty(PARAM_DEVICE_MODEL, model.sanitized())
            firebaseAnalytics.setUserProperty(PARAM_ANDROID_VERSION, androidVersion.sanitized())
            firebaseAnalytics.setUserProperty(PARAM_LOCALE_COUNTRY, localeCountry.sanitized())
            firebaseAnalytics.setUserProperty(PARAM_TIME_ZONE, timeZone.sanitized())
        }
    }

    private fun Bundle.putBooleanAsLong(key: String, value: Boolean) {
        putLong(key, if (value) 1L else 0L)
    }

    private fun Boolean.analyticsStatus(): String = if (this) VALUE_SUCCESS else VALUE_FAILURE

    private fun MealType.analyticsName(): String = name.lowercase()

    private fun HealthStatus.analyticsName(): String = name.lowercase()

    private fun MenuAudience.analyticsName(): String = name.lowercase()

    private fun String.sanitized(): String = take(MAX_PARAM_LENGTH).ifBlank { VALUE_UNKNOWN }

    private companion object {
        const val MAX_PARAM_LENGTH = 100

        const val EVENT_APP_OPENED = "app_opened"
        const val EVENT_MENU_SAVED = "menu_saved"
        const val EVENT_CTA_TAPPED = "cta_tapped"
        const val EVENT_MENU_DELETED = "menu_deleted"
        const val EVENT_FIRST_MENU_CREATED = "first_menu_created"
        const val EVENT_MENU_INVENTORY_CHANGED = "menu_inventory_changed"
        const val EVENT_MENU_FORM_STARTED = "menu_form_started"
        const val EVENT_MEAL_TYPE_SELECTED = "meal_type_selected"
        const val EVENT_AUDIENCE_FILTER_SELECTED = "audience_filter_selected"
        const val EVENT_MENU_SAVE_BLOCKED = "menu_save_blocked"
        const val EVENT_MENU_EDIT_STARTED = "menu_edit_started"
        const val EVENT_MENU_EDIT_SAVED = "menu_edit_saved"
        const val EVENT_MENU_PHOTO_UPDATED = "menu_photo_updated"
        const val EVENT_DICE_ROLLED = "dice_rolled"
        const val EVENT_DICE_FILTER_SELECTED = "dice_filter_selected"
        const val EVENT_DICE_EMPTY_RESULT = "dice_empty_result"
        const val EVENT_MENU_CARD_OPENED = "menu_card_opened"
        const val EVENT_ONBOARDING_SHOWN = "onboarding_shown"
        const val EVENT_ONBOARDING_COMPLETED = "onboarding_completed"
        const val EVENT_ABOUT_APP_OPENED = "about_app_opened"
        const val EVENT_DIETARY_PROFILE_OPENED = "dietary_profile_opened"
        const val EVENT_DIETARY_PROFILE_AUDIENCE_SELECTED = "dietary_profile_audience_selected"
        const val EVENT_DIETARY_PROFILE_UPDATED = "dietary_profile_updated"
        const val EVENT_MENU_LIST_VIEW_MORE_OPENED = "menu_list_view_more_opened"
        const val EVENT_BACKEND_SYNC_RETRIED = "backend_sync_retried"
        const val EVENT_BACKEND_SYNC_FINISHED = "backend_sync_finished"
        const val EVENT_AI_MENU_GENERATION_STARTED = "ai_menu_gen_started"
        const val EVENT_AI_MENU_GENERATION_FINISHED = "ai_menu_gen_finished"
        const val EVENT_AI_ANALYSIS_STARTED = "ai_analysis_started"
        const val EVENT_AI_ANALYSIS_FINISHED = "ai_analysis_finished"
        const val EVENT_AI_DAILY_LIMIT_REACHED = "ai_daily_limit_reached"

        const val PARAM_DEVICE_MANUFACTURER = "device_manufacturer"
        const val PARAM_DEVICE_MODEL = "device_model"
        const val PARAM_ANDROID_VERSION = "android_version"
        const val PARAM_LOCALE_COUNTRY = "locale_country"
        const val PARAM_TIME_ZONE = "time_zone"
        const val PARAM_SCREEN = "screen"
        const val PARAM_CTA = "cta"
        const val PARAM_MEAL_TYPE = "meal_type"
        const val PARAM_AUDIENCE = "audience"
        const val PARAM_HAS_AI_ANALYSIS = "has_ai_analysis"
        const val PARAM_HAS_PHOTO = "has_photo"
        const val PARAM_CHANGED_RECIPE = "changed_recipe"
        const val PARAM_HAS_CALORIES = "has_calories"
        const val PARAM_MENU_COUNT = "menu_count"
        const val PARAM_ANALYZED_MENU_COUNT = "analyzed_menu_count"
        const val PARAM_PENDING_ANALYSIS_COUNT = "pending_analysis_count"
        const val PARAM_FIELD = "field"
        const val PARAM_FORM_HAS_CONTENT = "form_has_content"
        const val PARAM_REASON = "reason"
        const val PARAM_HAS_NAME = "has_name"
        const val PARAM_HAS_DESCRIPTION = "has_description"
        const val PARAM_FILTER_TYPE = "filter_type"
        const val PARAM_RESULT_FOUND = "result_found"
        const val PARAM_RESULT_MEAL_TYPE = "result_meal_type"
        const val PARAM_AVAILABLE_CANDIDATE_COUNT = "available_candidate_count"
        const val PARAM_AVOID_IDEA_COUNT = "avoid_idea_count"
        const val PARAM_STATUS = "status"
        const val PARAM_SCOPE = "scope"
        const val PARAM_ANALYZED_COUNT = "analyzed_count"
        const val PARAM_SOURCE = "source"
        const val PARAM_HEALTH_STATUS = "health_status"
        const val PARAM_FAILURE_TYPE = "failure_type"
        const val PARAM_ACTION = "action"
        const val PARAM_FIELD_GROUP = "field_group"
        const val PARAM_ACTIVE_AUDIENCE_COUNT = "active_audience_count"
        const val PARAM_PENDING_MENU_COUNT = "pending_menu_count"

        const val VALUE_ALL = "all"
        const val VALUE_NONE = "none"
        const val VALUE_UNKNOWN = "unknown"
        const val VALUE_SUCCESS = "success"
        const val VALUE_FAILURE = "failure"
    }
}
