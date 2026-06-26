package com.menudado.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServiceDisabledException
import com.menudado.analytics.MenuDadoAnalytics
import com.menudado.analytics.NoOpMenuDadoAnalytics
import com.menudado.data.DietaryProfileStore
import com.menudado.data.NoOpDietaryProfileStore
import com.menudado.data.AiDailyUsageState
import com.menudado.data.AiDailyUsageStore
import com.menudado.data.AiQuotaRetryStore
import com.menudado.data.AiQuotaRetryState
import com.menudado.data.AiRequestThrottleStore
import com.menudado.data.MenuRepository
import com.menudado.data.NoOpAiDailyUsageStore
import com.menudado.data.NoOpAiQuotaRetryStore
import com.menudado.data.NoOpAiRequestThrottleStore
import com.menudado.data.NoOpOnboardingStore
import com.menudado.data.OnboardingStore
import com.menudado.domain.DiceSelector
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.AppLanguage
import com.menudado.domain.MenuAudience
import com.menudado.domain.MealType
import com.menudado.domain.AiQuotaLimitType
import com.menudado.domain.classifyAiQuotaLimitType
import com.menudado.domain.isAiQuotaExceeded
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

data class MenuDadoUiState(
    val menus: List<FoodMenu> = emptyList(),
    val diceFilter: MealType? = null,
    val diceAudienceFilter: MenuAudience? = null,
    val editingMenuId: Long? = null,
    val editMealType: MealType? = null,
    val editAudience: MenuAudience? = null,
    val editName: String = "",
    val editDescription: String = "",
    val editNotes: String = "",
    val editImageUri: String? = null,
    val formMealType: MealType? = null,
    val formAudience: MenuAudience? = null,
    val name: String = "",
    val description: String = "",
    val notes: String = "",
    val aiBaseIngredients: String = "",
    val calories: Int? = null,
    val generatedHealthAnalysis: HealthAnalysis? = null,
    val isRolling: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isGeneratingMenu: Boolean = false,
    val result: FoodMenu? = null,
    val message: String? = null,
    val aiRetryAtMillis: Long? = null,
    val isAiRequestThrottlePause: Boolean = false,
    val isAiRetryNoticeVisible: Boolean = false,
    val aiUsesRemainingToday: Int = AI_DAILY_FREE_REQUEST_LIMIT,
    val enabledAudiences: List<MenuAudience> = MenuAudience.entries,
    val audienceAgeRanges: Map<MenuAudience, String> = MenuAudience.entries.associateWith { it.defaultAgeRange },
    val dietaryProfileAudience: MenuAudience = MenuAudience.ADULT,
    val dietaryProfile: DietaryProfile = DietaryProfile(),
    val showOnboarding: Boolean = false
)

class MenuDadoViewModel(
    private val repository: MenuRepository,
    private val analytics: MenuDadoAnalytics = NoOpMenuDadoAnalytics,
    private val todayProvider: () -> String = { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
    private val clockMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val aiQuotaRetryStore: AiQuotaRetryStore = NoOpAiQuotaRetryStore,
    private val aiRequestThrottleStore: AiRequestThrottleStore = NoOpAiRequestThrottleStore,
    private val aiDailyUsageStore: AiDailyUsageStore = NoOpAiDailyUsageStore,
    private val dietaryProfileStore: DietaryProfileStore = NoOpDietaryProfileStore,
    private val onboardingStore: OnboardingStore = NoOpOnboardingStore
) : ViewModel() {
    private val suggestedMealType = suggestedMealTypeForDeviceTime(clockMillisProvider())
    private val _uiState = MutableStateFlow(
        MenuDadoUiState(
            diceFilter = suggestedMealType,
            formMealType = suggestedMealType
        )
    )
    val uiState: StateFlow<MenuDadoUiState> = _uiState.asStateFlow()
    private var aiRetryRefreshJob: Job? = null
    private var hasTrackedMenuFormStarted = false
    private var generatedIdeaDateKey: String? = null
    private val generatedIdeasToday = mutableListOf<GeneratedIdeaMemory>()

    init {
        refreshOnboarding()
        refreshDietaryProfile()
        refreshStoredAiRetry()
        refreshStoredAiRequestThrottle()
        refreshAiDailyUsage()
        viewModelScope.launch {
            repository.menus.collect { menus ->
                _uiState.update { it.copy(menus = menus) }
            }
        }
    }

    fun setDiceFilter(filter: MealType?) {
        analytics.trackDiceFilterSelected(filter, _uiState.value.menus.size)
        _uiState.update { it.copy(diceFilter = filter) }
    }

    fun trackCtaTapped(screen: String, cta: String) {
        analytics.trackCtaTapped(screen, cta)
    }

    fun setDiceAudienceFilter(filter: MenuAudience?) {
        val state = _uiState.value
        _uiState.update {
            if (filter != null && filter !in it.enabledAudiences) {
                it
            } else {
                it.copy(diceAudienceFilter = filter)
            }
        }
        if (filter == null || filter in state.enabledAudiences) {
            analytics.trackAudienceFilterSelected(ANALYTICS_SOURCE_DICE, filter, state.menus.size)
        }
    }

    fun completeOnboarding() {
        completeOnboarding(ONBOARDING_ACTION_START)
    }

    fun skipOnboarding() {
        completeOnboarding(ONBOARDING_ACTION_SKIP)
    }

    fun trackAboutAppOpened() {
        analytics.trackAboutAppOpened()
    }

    fun trackDietaryProfileOpened() {
        analytics.trackDietaryProfileOpened(_uiState.value.enabledAudiences.size)
    }

    fun trackMenuListViewMoreOpened(audience: MenuAudience) {
        val menuCount = _uiState.value.menus.count { it.audience == audience }
        analytics.trackMenuListViewMoreOpened(audience, menuCount)
    }

    private fun completeOnboarding(action: String) {
        onboardingStore.markOnboardingCompleted(CURRENT_ONBOARDING_VERSION)
        analytics.trackOnboardingCompleted(action)
        _uiState.update { it.copy(showOnboarding = false) }
    }

    fun setFormMealType(mealType: MealType) {
        analytics.trackMealTypeSelected(mealType, formHasContent = _uiState.value.formHasContent())
        _uiState.update {
            if (it.formMealType == mealType) {
                return@update it
            }
            it.copy(
                formMealType = mealType,
            ).withoutMenuFormDraft()
        }
    }

    fun setFormAudience(audience: MenuAudience) {
        val state = _uiState.value
        _uiState.update {
            if (audience !in it.enabledAudiences) {
                return@update it
            }
            if (it.formAudience == audience) {
                return@update it
            }
            it.copy(
                formAudience = audience,
            ).withoutMenuFormDraft()
        }
        if (audience in state.enabledAudiences) {
            analytics.trackAudienceFilterSelected(ANALYTICS_SOURCE_FORM, audience, state.menus.size)
        }
    }

    fun updateName(value: String) {
        trackMenuFormStartedIfNeeded(FORM_FIELD_NAME, value)
        _uiState.update {
            val notice = it.generatedHealthAnalysis.manualEditNotice(currentLanguage())
            it.copy(
                name = value,
                generatedHealthAnalysis = null,
                message = notice ?: it.message,
                isAiRetryNoticeVisible = if (notice != null) false else it.isAiRetryNoticeVisible
            )
        }
    }

    fun updateDescription(value: String) {
        trackMenuFormStartedIfNeeded(FORM_FIELD_DESCRIPTION, value)
        _uiState.update {
            val notice = it.generatedHealthAnalysis.manualEditNotice(currentLanguage())
            it.copy(
                description = value,
                generatedHealthAnalysis = null,
                message = notice ?: it.message,
                isAiRetryNoticeVisible = if (notice != null) false else it.isAiRetryNoticeVisible
            )
        }
    }

    fun updateNotes(value: String) {
        trackMenuFormStartedIfNeeded(FORM_FIELD_NOTES, value)
        _uiState.update {
            val notice = it.generatedHealthAnalysis.manualEditNotice(currentLanguage())
            it.copy(
                notes = value,
                generatedHealthAnalysis = null,
                message = notice ?: it.message,
                isAiRetryNoticeVisible = if (notice != null) false else it.isAiRetryNoticeVisible
            )
        }
    }

    fun updateAiBaseIngredients(value: String) {
        _uiState.update {
            val notice = it.generatedHealthAnalysis.manualEditNotice(currentLanguage())
            it.copy(
                aiBaseIngredients = value,
                generatedHealthAnalysis = null,
                message = notice ?: it.message,
                isAiRetryNoticeVisible = if (notice != null) false else it.isAiRetryNoticeVisible
            )
        }
    }

    fun updateEditImageUri(imageUri: String?) {
        _uiState.update { it.copy(editImageUri = imageUri) }
    }

    fun updateMenuImageUri(menuId: Long, imageUri: String) {
        val menu = _uiState.value.menus.firstOrNull { it.id == menuId } ?: return
        viewModelScope.launch {
            repository.save(menu.copy(imageUri = imageUri))
            analytics.trackMenuPhotoUpdated(
                mealType = menu.mealType,
                audience = menu.audience,
                hasPhoto = true,
                menuCount = _uiState.value.menus.size
            )
        }
    }

    fun setDietaryProfileVegan(isVegan: Boolean) {
        updateActiveDietaryProfile(ANALYTICS_PROFILE_FIELD_VEGAN) { it.copy(isVegan = isVegan) }
    }

    fun setDietaryProfileAudience(audience: MenuAudience) {
        _uiState.update {
            it.copy(
                dietaryProfileAudience = audience,
                dietaryProfile = dietaryProfileStore.getProfile(audience),
                enabledAudiences = loadEnabledAudiences(),
                audienceAgeRanges = loadAudienceAgeRanges()
            )
        }
        analytics.trackDietaryProfileAudienceSelected(audience)
    }

    fun setDietaryProfileAudienceEnabled(isEnabled: Boolean) {
        val state = _uiState.value
        if (!isEnabled && state.dietaryProfile.isEnabled && state.enabledAudiences.size <= 1) {
            return
        }
        val updated = state.dietaryProfile.copy(isEnabled = isEnabled)
        dietaryProfileStore.saveProfile(updated, state.dietaryProfileAudience)
        val enabledAudiences = loadEnabledAudiences()
        val defaultAudience = enabledAudiences.singleOrNull()
        _uiState.update {
            it.copy(
                dietaryProfile = updated,
                enabledAudiences = enabledAudiences,
                audienceAgeRanges = loadAudienceAgeRanges(),
                formAudience = defaultAudience,
                diceAudienceFilter = defaultAudience
            )
        }
        analytics.trackDietaryProfileUpdated(
            audience = state.dietaryProfileAudience,
            fieldGroup = ANALYTICS_PROFILE_FIELD_ENABLED,
            activeAudienceCount = enabledAudiences.size
        )
    }

    fun updateDietaryProfileAgeRange(ageRange: String) {
        updateDietaryProfile(ANALYTICS_PROFILE_FIELD_AGE_RANGE) { it.copy(ageRange = ageRange) }
    }

    fun setDietaryProfilePregnant(isPregnant: Boolean) {
        updateActiveDietaryProfile(ANALYTICS_PROFILE_FIELD_PREGNANT) { it.copy(isPregnant = isPregnant) }
    }

    fun setDietaryProfileHasAllergies(hasAllergies: Boolean) {
        updateActiveDietaryProfile(ANALYTICS_PROFILE_FIELD_ALLERGIES) {
            it.copy(
                hasAllergies = hasAllergies,
                allergens = if (hasAllergies) it.allergens else emptySet()
            )
        }
    }

    fun toggleDietaryAllergen(allergen: DietaryAllergen) {
        updateActiveDietaryProfile(ANALYTICS_PROFILE_FIELD_ALLERGENS) { profile ->
            val allergens = if (allergen in profile.allergens) {
                profile.allergens - allergen
            } else {
                profile.allergens + allergen
            }
            profile.copy(hasAllergies = true, allergens = allergens)
        }
    }

    fun updateDietaryProfileOtherAvoidances(value: String) {
        updateActiveDietaryProfile(ANALYTICS_PROFILE_FIELD_OTHER_AVOIDANCES) { it.copy(otherAvoidances = value) }
    }

    fun clearMessage() {
        clearExpiredAiRetryStateIfNeeded()
        _uiState.update { it.copy(message = null, isAiRetryNoticeVisible = false) }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null) }
    }

    fun trackMenuCardOpened(menu: FoodMenu) {
        analytics.trackMenuCardOpened(
            mealType = menu.mealType,
            hasAiAnalysis = menu.healthAnalysis != null,
            menuCount = _uiState.value.menus.size
        )
    }

    fun startEditingMenu(menu: FoodMenu) {
        analytics.trackMenuEditStarted(
            mealType = menu.mealType,
            audience = menu.audience,
            hasAiAnalysis = menu.healthAnalysis != null,
            hasPhoto = menu.imageUri != null,
            menuCount = _uiState.value.menus.size
        )
        _uiState.update {
            it.copy(
                editingMenuId = menu.id,
                editMealType = menu.mealType,
                editAudience = menu.audience,
                editName = menu.name,
                editDescription = menu.description,
                editNotes = menu.notes,
                editImageUri = menu.imageUri,
                message = null,
                isAiRetryNoticeVisible = false
            )
        }
    }

    fun cancelEditingMenu() {
        resetEditForm()
    }

    fun setEditMealType(mealType: MealType) {
        _uiState.update { it.copy(editMealType = mealType) }
    }

    fun setEditAudience(audience: MenuAudience) {
        _uiState.update { it.copy(editAudience = audience) }
    }

    fun updateEditName(value: String) {
        _uiState.update { it.copy(editName = value) }
    }

    fun updateEditDescription(value: String) {
        _uiState.update { it.copy(editDescription = value) }
    }

    fun updateEditNotes(value: String) {
        _uiState.update { it.copy(editNotes = value) }
    }

    fun rollDice() {
        val state = _uiState.value
        if (state.isRolling) return
        if (state.diceFilter == null) {
            _uiState.update { it.copy(message = currentLanguage().diceFilterRequiredMessage(), isAiRetryNoticeVisible = false) }
            return
        }
        if (state.diceAudienceFilter == null) {
            _uiState.update { it.copy(message = currentLanguage().audienceRequiredMessage(), isAiRetryNoticeVisible = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRolling = true, result = null, message = null, isAiRetryNoticeVisible = false) }
            delay(850)
            val today = todayProvider()
            val currentState = _uiState.value
            val hasCandidates = DiceSelector.hasCandidates(
                currentState.menus,
                currentState.diceFilter,
                currentState.diceAudienceFilter
            )
            val availableCandidateCountBeforeReset = DiceSelector.availableCandidateCount(
                menus = currentState.menus,
                filter = currentState.diceFilter,
                audienceFilter = currentState.diceAudienceFilter,
                today = today
            )
            var selected = DiceSelector.select(
                menus = currentState.menus,
                filter = currentState.diceFilter,
                audienceFilter = currentState.diceAudienceFilter,
                today = today
            )
            if (selected == null && hasCandidates) {
                val resetMenus = currentState.menus.map { menu ->
                    if (menu.matchesDiceFilters(currentState.diceFilter, currentState.diceAudienceFilter)) {
                        menu.copy(lastPickedDate = null)
                    } else {
                        menu
                    }
                }
                resetMenus
                    .filter { it.matchesDiceFilters(currentState.diceFilter, currentState.diceAudienceFilter) }
                    .forEach { repository.save(it) }
                selected = DiceSelector.select(
                    menus = resetMenus,
                    filter = currentState.diceFilter,
                    audienceFilter = currentState.diceAudienceFilter,
                    today = today
                )
            }
            if (selected != null) {
                repository.save(selected.copy(lastPickedDate = today))
            }
            analytics.trackDiceRolled(
                filter = currentState.diceFilter,
                resultMealType = selected?.mealType,
                menuCount = currentState.menus.size,
                availableCandidateCount = availableCandidateCountBeforeReset
            )
            if (selected == null && !hasCandidates) {
                analytics.trackDiceEmptyResult(
                    filter = currentState.diceFilter,
                    availableCandidateCount = availableCandidateCountBeforeReset
                )
            }
            _uiState.update {
                it.copy(
                    isRolling = false,
                    result = selected,
                    message = if (selected == null && !hasCandidates) currentLanguage().noMenusForFilterMessage() else null,
                    isAiRetryNoticeVisible = false
                )
            }
        }
    }

    fun saveMenu() {
        val state = _uiState.value
        val name = state.name.trim()
        val description = state.description.trim()
        val mealType = state.formMealType
        val audience = state.formAudience

        if (mealType == null) {
            analytics.trackMenuSaveBlocked(
                reason = MENU_SAVE_BLOCKED_MISSING_MEAL_TYPE,
                hasName = name.isNotBlank(),
                hasDescription = description.isNotBlank()
            )
            _uiState.update { it.copy(message = currentLanguage().mealTypeRequiredMessage(), isAiRetryNoticeVisible = false) }
            return
        }

        if (audience == null) {
            analytics.trackMenuSaveBlocked(
                reason = MENU_SAVE_BLOCKED_MISSING_AUDIENCE,
                hasName = name.isNotBlank(),
                hasDescription = description.isNotBlank()
            )
            _uiState.update { it.copy(message = currentLanguage().audienceRequiredMessage(), isAiRetryNoticeVisible = false) }
            return
        }

        if (name.isBlank() || description.isBlank()) {
            analytics.trackMenuSaveBlocked(
                reason = MENU_SAVE_BLOCKED_MISSING_REQUIRED_FIELDS,
                hasName = name.isNotBlank(),
                hasDescription = description.isNotBlank()
            )
            _uiState.update {
                it.copy(
                    message = currentLanguage().missingRequiredFieldsMessage(),
                    isAiRetryNoticeVisible = false
                )
            }
            return
        }

        viewModelScope.launch {
            val menu = FoodMenu(
                name = name,
                mealType = mealType,
                audience = audience,
                description = description,
                notes = state.notes.trim(),
                healthAnalysis = state.generatedHealthAnalysis,
                calories = state.calories
            )

            repository.save(menu)
            if (state.menus.isEmpty()) {
                analytics.trackFirstMenuCreated(menu.mealType)
            }
            analytics.trackMenuSaved(
                mealType = menu.mealType,
                hasAiAnalysis = menu.healthAnalysis != null,
                hasCalories = menu.calories != null,
                menuCount = state.menus.size + 1
            )
            analytics.trackMenuInventoryChanged(
                menuCount = state.menus.size + 1,
                analyzedMenuCount = state.menus.countAnalyzed() + if (menu.healthAnalysis != null) 1 else 0,
                pendingAnalysisCount = state.menus.countPendingAnalysis() + if (menu.healthAnalysis == null) 1 else 0
            )
            resetForm()
            hasTrackedMenuFormStarted = false
        }
    }

    fun saveEditedMenu() {
        val state = _uiState.value
        val editingMenuId = state.editingMenuId ?: return
        val existingMenu = state.menus.firstOrNull { it.id == editingMenuId } ?: run {
            resetEditForm()
            return
        }
        val name = state.editName.trim()
        val description = state.editDescription.trim()
        val notes = state.editNotes.trim()
        val mealType = state.editMealType
        val audience = state.editAudience

        if (mealType == null) {
            _uiState.update { it.copy(message = currentLanguage().mealTypeRequiredMessage(), isAiRetryNoticeVisible = false) }
            return
        }

        if (audience == null) {
            _uiState.update { it.copy(message = currentLanguage().audienceRequiredMessage(), isAiRetryNoticeVisible = false) }
            return
        }

        if (name.isBlank() || description.isBlank()) {
            _uiState.update {
                it.copy(
                    message = currentLanguage().missingRequiredFieldsMessage(),
                    isAiRetryNoticeVisible = false
                )
            }
            return
        }

        viewModelScope.launch {
            val changedExistingMenu = existingMenu.hasEditableChanges(
                name = name,
                mealType = mealType,
                audience = audience,
                description = description,
                notes = notes
            )
            val menu = existingMenu.copy(
                name = name,
                mealType = mealType,
                description = description,
                notes = notes,
                healthAnalysis = if (changedExistingMenu) null else existingMenu.healthAnalysis,
                calories = if (changedExistingMenu) null else existingMenu.calories,
                imageUri = state.editImageUri
            )

            repository.save(menu)
            analytics.trackMenuEditSaved(
                mealType = menu.mealType,
                audience = menu.audience,
                changedRecipe = changedExistingMenu,
                hasAiAnalysis = menu.healthAnalysis != null,
                hasPhoto = menu.imageUri != null,
                menuCount = state.menus.size
            )
            analytics.trackMenuSaved(
                mealType = menu.mealType,
                hasAiAnalysis = menu.healthAnalysis != null,
                hasCalories = menu.calories != null,
                menuCount = state.menus.size
            )
            val inventoryMenus = state.menus.map { if (it.id == menu.id) menu else it }
            analytics.trackMenuInventoryChanged(
                menuCount = inventoryMenus.size,
                analyzedMenuCount = inventoryMenus.countAnalyzed(),
                pendingAnalysisCount = inventoryMenus.countPendingAnalysis()
            )
            resetEditForm()
        }
    }

    fun generateMenuIdea() {
        val state = _uiState.value
        if (state.isGeneratingMenu) {
            return
        }
        val mealType = state.formMealType
        val audience = state.formAudience
        if (mealType == null) {
            _uiState.update { it.copy(message = currentLanguage().mealTypeRequiredMessage(), isAiRetryNoticeVisible = false) }
            return
        }
        if (audience == null) {
            _uiState.update { it.copy(message = currentLanguage().audienceRequiredMessage(), isAiRetryNoticeVisible = false) }
            return
        }
        val profile = dietaryProfileStore.getProfile(audience)
        val ingredientConflicts = profile.findIngredientConflicts(state.aiBaseIngredients)
        if (ingredientConflicts.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    message = ingredientConflicts.toIngredientConflictMessage(currentLanguage()),
                    isAiRetryNoticeVisible = false
                )
            }
            return
        }
        val activeRetryAtMillis = activeAiRetryAtMillis()
        if (activeRetryAtMillis != null) {
            showActiveAiRetryNotice(activeRetryAtMillis)
            return
        }
        val activeRequestThrottleAtMillis = activeAiRequestThrottleAtMillis()
        if (activeRequestThrottleAtMillis != null) {
            showAiRequestThrottleNotice(activeRequestThrottleAtMillis)
            return
        }
        if (!consumeAiDailyUseOrShowNotice(AI_SOURCE_GENERATE_MENU)) {
            return
        }
        val avoidIdeas = state.buildAvoidIdeas(mealType, audience)
        analytics.trackAiMenuGenerationStarted(mealType, avoidIdeas.size)
        startAiRequestThrottle()
        _uiState.update {
            it.copy(
                isGeneratingMenu = true,
                message = null,
                isAiRetryNoticeVisible = false
            )
        }

        viewModelScope.launch {
            withAiRequestTimeout {
                repository.generateMenu(
                    mealType = mealType,
                    avoidIdeas = avoidIdeas,
                    dietaryProfile = profile,
                    audience = audience,
                    baseIngredients = state.aiBaseIngredients.trim(),
                    language = AppLanguage.fromLocale()
                )
            }
                .onSuccess { generated ->
                    aiQuotaRetryStore.clearRetryState()
                    rememberGeneratedIdea(mealType, audience, generated.name, generated.description)
                    _uiState.update {
                        it.copy(
                            name = generated.name,
                            description = generated.description,
                            notes = generated.notes,
                            calories = generated.calories,
                            generatedHealthAnalysis = generated.healthAnalysis,
                            isAiRetryNoticeVisible = false
                        )
                    }
                    analytics.trackAiMenuGenerationFinished(
                        mealType = mealType,
                        success = true,
                        healthStatus = generated.healthAnalysis?.status,
                        failureType = null
                    )
                }
                .onFailure { error ->
                    val notice = error.toAiFailureNotice(clockMillisProvider(), currentLanguage())
                    showAiFailureNotice(notice)
                    analytics.trackAiMenuGenerationFinished(
                        mealType = mealType,
                        success = false,
                        healthStatus = null,
                        failureType = error.analyticsFailureType()
                    )
                }
            _uiState.update { it.copy(isGeneratingMenu = false) }
        }
    }

    private fun MenuDadoUiState.buildAvoidIdeas(mealType: MealType, audience: MenuAudience): List<String> {
        val savedIdeas = menus
            .filter { it.mealType == mealType && it.audience == audience }
            .map { "${it.name}: ${it.description}" }

        val generatedIdeas = generatedIdeasForToday(mealType, audience)
        val currentName = name.trim()
        val currentDescription = description.trim()
        val currentIdea = if (currentName.isNotBlank() || currentDescription.isNotBlank()) {
            listOf("${currentName.ifBlank { "Idea actual" }}: $currentDescription".trim())
        } else {
            emptyList()
        }

        return (savedIdeas + generatedIdeas + currentIdea)
            .distinct()
            .takeLast(8)
    }

    private fun generatedIdeasForToday(mealType: MealType, audience: MenuAudience): List<String> {
        resetGeneratedIdeaMemoryIfNeeded()
        return generatedIdeasToday
            .filter { it.mealType == mealType && it.audience == audience }
            .map { "${it.name}: ${it.description}" }
    }

    private fun rememberGeneratedIdea(mealType: MealType, audience: MenuAudience, name: String, description: String) {
        resetGeneratedIdeaMemoryIfNeeded()
        generatedIdeasToday += GeneratedIdeaMemory(mealType, audience, name.trim(), description.trim())
        val uniqueIdeas = generatedIdeasToday.distinctBy { listOf(it.mealType.name, it.audience.name, it.name, it.description) }
        generatedIdeasToday.clear()
        generatedIdeasToday.addAll(uniqueIdeas.takeLast(8))
    }

    private fun resetGeneratedIdeaMemoryIfNeeded() {
        val today = todayProvider()
        if (generatedIdeaDateKey != today) {
            generatedIdeaDateKey = today
            generatedIdeasToday.clear()
        }
    }

    private fun refreshDietaryProfile() {
        val enabledAudiences = loadEnabledAudiences()
        _uiState.update {
            it.copy(
                dietaryProfile = dietaryProfileStore.getProfile(it.dietaryProfileAudience),
                enabledAudiences = enabledAudiences,
                audienceAgeRanges = loadAudienceAgeRanges(),
                formAudience = it.formAudience.selectedOrSingleDefault(enabledAudiences),
                diceAudienceFilter = it.diceAudienceFilter.selectedOrSingleDefault(enabledAudiences)
            )
        }
    }

    private fun refreshOnboarding() {
        val shouldShowOnboarding = !onboardingStore.isOnboardingCompleted(CURRENT_ONBOARDING_VERSION)
        _uiState.update { it.copy(showOnboarding = shouldShowOnboarding) }
        if (shouldShowOnboarding) {
            analytics.trackOnboardingShown()
        }
    }

    private fun updateDietaryProfile(fieldGroup: String, transform: (DietaryProfile) -> DietaryProfile) {
        val state = _uiState.value
        val updated = transform(state.dietaryProfile)
        dietaryProfileStore.saveProfile(updated, state.dietaryProfileAudience)
        val enabledAudiences = loadEnabledAudiences()
        _uiState.update {
            it.copy(
                dietaryProfile = updated,
                enabledAudiences = enabledAudiences,
                audienceAgeRanges = loadAudienceAgeRanges(),
                formAudience = it.formAudience.selectedOrSingleDefault(enabledAudiences),
                diceAudienceFilter = it.diceAudienceFilter.selectedOrSingleDefault(enabledAudiences)
            )
        }
        analytics.trackDietaryProfileUpdated(
            audience = state.dietaryProfileAudience,
            fieldGroup = fieldGroup,
            activeAudienceCount = enabledAudiences.size
        )
    }

    private fun updateActiveDietaryProfile(fieldGroup: String, transform: (DietaryProfile) -> DietaryProfile) {
        if (!_uiState.value.dietaryProfile.isEnabled) {
            return
        }
        updateDietaryProfile(fieldGroup, transform)
    }

    private fun loadEnabledAudiences(): List<MenuAudience> {
        return MenuAudience.entries.filter { audience ->
            dietaryProfileStore.getProfile(audience).isEnabled
        }
    }

    private fun loadAudienceAgeRanges(): Map<MenuAudience, String> {
        return MenuAudience.entries.associateWith { audience ->
            dietaryProfileStore.getProfile(audience).ageRange.ifBlank { audience.defaultAgeRange }
        }
    }

    private fun MenuAudience?.selectedOrSingleDefault(enabledAudiences: List<MenuAudience>): MenuAudience? {
        return takeIf { it in enabledAudiences } ?: enabledAudiences.singleOrNull()
    }

    private fun HealthAnalysis?.manualEditNotice(language: AppLanguage): String? {
        return if (this == null) null else language.generatedAnalysisManualEditMessage()
    }

    fun analyzeExisting(menu: FoodMenu) {
        val state = _uiState.value
        if (state.isAnalyzing) {
            return
        }
        val activeRetryAtMillis = activeAiRetryAtMillis()
        if (activeRetryAtMillis != null) {
            showActiveAiRetryNotice(activeRetryAtMillis)
            return
        }
        val activeRequestThrottleAtMillis = activeAiRequestThrottleAtMillis()
        if (activeRequestThrottleAtMillis != null) {
            showAiRequestThrottleNotice(activeRequestThrottleAtMillis)
            return
        }
        if (!consumeAiDailyUseOrShowNotice(AI_SOURCE_ANALYZE_SINGLE)) {
            return
        }
        analytics.trackAiAnalysisStarted(AI_SCOPE_SINGLE, menu.mealType, menuCount = 1)
        startAiRequestThrottle()
        _uiState.update {
            it.copy(
                isAnalyzing = true,
                message = null,
                isAiRetryNoticeVisible = false
            )
        }

        viewModelScope.launch {
            withAiRequestTimeout { repository.analyze(menu, AppLanguage.fromLocale()) }
                .onSuccess { analysis ->
                    repository.save(
                        menu.copy(
                            healthAnalysis = analysis,
                            calories = analysis.calories ?: menu.calories
                        )
                    )
                    aiQuotaRetryStore.clearRetryState()
                    _uiState.update { it.copy(isAiRetryNoticeVisible = false) }
                    analytics.trackAiAnalysisFinished(
                        AI_SCOPE_SINGLE,
                        menu.mealType,
                        success = true,
                        analyzedCount = 1,
                        healthStatus = analysis.status,
                        failureType = null
                    )
                }
                .onFailure { error ->
                    val notice = error.toAiFailureNotice(clockMillisProvider(), currentLanguage())
                    showAiFailureNotice(notice)
                    analytics.trackAiAnalysisFinished(
                        AI_SCOPE_SINGLE,
                        menu.mealType,
                        success = false,
                        analyzedCount = 0,
                        healthStatus = null,
                        failureType = error.analyticsFailureType()
                    )
                }
            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }

    fun analyzePendingMenus() {
        val state = _uiState.value
        if (state.isAnalyzing) {
            return
        }
        val pendingMenus = _uiState.value.menus
            .filter { it.healthAnalysis == null }
            .take(AI_BATCH_ANALYSIS_LIMIT)

        if (pendingMenus.isEmpty()) {
            _uiState.update {
                it.copy(
                    message = currentLanguage().noPendingMenusMessage(),
                    isAiRetryNoticeVisible = false
                )
            }
            return
        }

        val activeRetryAtMillis = activeAiRetryAtMillis()
        if (activeRetryAtMillis != null) {
            showActiveAiRetryNotice(activeRetryAtMillis)
            return
        }
        val activeRequestThrottleAtMillis = activeAiRequestThrottleAtMillis()
        if (activeRequestThrottleAtMillis != null) {
            showAiRequestThrottleNotice(activeRequestThrottleAtMillis)
            return
        }
        if (!consumeAiDailyUseOrShowNotice(AI_SOURCE_ANALYZE_BATCH)) {
            return
        }
        analytics.trackAiAnalysisStarted(AI_SCOPE_BATCH, mealType = null, menuCount = pendingMenus.size)
        startAiRequestThrottle()
        _uiState.update {
            it.copy(
                isAnalyzing = true,
                message = null,
                isAiRetryNoticeVisible = false
            )
        }

        viewModelScope.launch {
            withAiRequestTimeout { repository.analyzeBatch(pendingMenus, AppLanguage.fromLocale()) }
                .onSuccess { analysesByMenuId ->
                    pendingMenus.forEach { menu ->
                        analysesByMenuId[menu.id]?.let { analysis ->
                            repository.save(
                                menu.copy(
                                    healthAnalysis = analysis,
                                    calories = analysis.calories ?: menu.calories
                                )
                            )
                        }
                    }
                    aiQuotaRetryStore.clearRetryState()
                    _uiState.update {
                        it.copy(
                            isAiRetryNoticeVisible = false,
                            message = if (analysesByMenuId.isEmpty()) {
                                currentLanguage().emptyAiBatchAnalysisMessage()
                            } else {
                                null
                            }
                        )
                    }
                    analytics.trackAiAnalysisFinished(
                        AI_SCOPE_BATCH,
                        mealType = null,
                        success = true,
                        analyzedCount = analysesByMenuId.size,
                        healthStatus = null,
                        failureType = null
                    )
                }
                .onFailure { error ->
                    val notice = error.toAiFailureNotice(clockMillisProvider(), currentLanguage())
                    showAiFailureNotice(notice)
                    analytics.trackAiAnalysisFinished(
                        AI_SCOPE_BATCH,
                        mealType = null,
                        success = false,
                        analyzedCount = 0,
                        healthStatus = null,
                        failureType = error.analyticsFailureType()
                    )
                }
            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }

    fun deleteMenu(menu: FoodMenu) {
        viewModelScope.launch {
            repository.delete(menu)
            analytics.trackMenuDeleted(menu.mealType, hadAiAnalysis = menu.healthAnalysis != null)
            val remainingMenus = _uiState.value.menus.filterNot { it.id == menu.id }
            analytics.trackMenuInventoryChanged(
                menuCount = remainingMenus.size,
                analyzedMenuCount = remainingMenus.countAnalyzed(),
                pendingAnalysisCount = remainingMenus.countPendingAnalysis()
            )
        }
    }

    private fun trackMenuFormStartedIfNeeded(firstEditedField: String, value: String) {
        val mealType = _uiState.value.formMealType
        if (!hasTrackedMenuFormStarted && value.isNotBlank() && mealType != null) {
            hasTrackedMenuFormStarted = true
            analytics.trackMenuFormStarted(firstEditedField, mealType)
        }
    }

    private fun MenuDadoUiState.formHasContent(): Boolean {
        return name.isNotBlank() || description.isNotBlank() || notes.isNotBlank()
    }

    private fun MenuDadoUiState.withoutMenuFormDraft(): MenuDadoUiState {
        return copy(
            name = "",
            description = "",
            notes = "",
            aiBaseIngredients = "",
            calories = null,
            generatedHealthAnalysis = null,
            message = null,
            isAiRetryNoticeVisible = false
        )
    }

    private fun FoodMenu.hasEditableChanges(
        name: String,
        mealType: MealType,
        audience: MenuAudience,
        description: String,
        notes: String
    ): Boolean {
        return this.name != name ||
            this.mealType != mealType ||
            this.audience != audience ||
            this.description != description ||
            this.notes != notes
    }

    private fun resetForm() {
        _uiState.update {
            it.copy(
                name = "",
                description = "",
                notes = "",
                aiBaseIngredients = "",
                calories = null,
                generatedHealthAnalysis = null,
                formMealType = suggestedMealTypeForDeviceTime(clockMillisProvider()),
                formAudience = null
            )
        }
    }

    private fun resetEditForm() {
        _uiState.update {
            it.copy(
                editingMenuId = null,
                editMealType = null,
                editAudience = null,
                editName = "",
                editDescription = "",
                editNotes = "",
                editImageUri = null
            )
        }
    }

    private fun FoodMenu.matchesDiceFilters(mealTypeFilter: MealType?, audienceFilter: MenuAudience?): Boolean {
        return (mealTypeFilter == null || mealType == mealTypeFilter) &&
            (audienceFilter == null || audience == audienceFilter)
    }

    private fun refreshStoredAiRetry() {
        val state = aiQuotaRetryStore.getRetryState()
        if (state == null) {
            return
        }
        if (state.retryAtMillis > clockMillisProvider()) {
            _uiState.update {
                it.copy(
                    aiRetryAtMillis = state.retryAtMillis,
                    isAiRequestThrottlePause = false
                )
            }
            scheduleAiRetryRefresh(state.retryAtMillis)
        }
    }

    private fun refreshStoredAiRequestThrottle() {
        aiRequestThrottleStore.clearLastRequest()
    }

    private fun refreshAiDailyUsage() {
        _uiState.update {
            it.copy(aiUsesRemainingToday = aiDailyUsesRemaining())
        }
    }

    private fun currentPacificDateKey(): String {
        return currentPacificDateKey(clockMillisProvider())
    }

    private fun consumeAiDailyUseOrShowNotice(source: String): Boolean {
        val dateKey = currentPacificDateKey()
        val usedCount = currentAiDailyUsedCount(dateKey)
        if (usedCount >= AI_DAILY_FREE_REQUEST_LIMIT) {
            val retryAtMillis = nextPacificMidnightMillis(clockMillisProvider())
            _uiState.update {
                it.copy(
                    message = currentLanguage().aiRequestsPerDayMessage(),
                    aiRetryAtMillis = retryAtMillis,
                    isAiRequestThrottlePause = false,
                    isAiRetryNoticeVisible = true,
                    aiUsesRemainingToday = 0
                )
            }
            scheduleAiRetryRefresh(retryAtMillis)
            analytics.trackAiDailyLimitReached(source)
            return false
        }

        val newUsedCount = usedCount + 1
        aiDailyUsageStore.saveUsageState(
            AiDailyUsageState(
                dateKey = dateKey,
                usedCount = newUsedCount
            )
        )
        _uiState.update {
            it.copy(aiUsesRemainingToday = (AI_DAILY_FREE_REQUEST_LIMIT - newUsedCount).coerceAtLeast(0))
        }
        return true
    }

    private fun aiDailyUsesRemaining(): Int {
        return (AI_DAILY_FREE_REQUEST_LIMIT - currentAiDailyUsedCount(currentPacificDateKey())).coerceIn(
            0,
            AI_DAILY_FREE_REQUEST_LIMIT
        )
    }

    private fun currentAiDailyUsedCount(dateKey: String): Int {
        val state = aiDailyUsageStore.getUsageState()
        if (state?.dateKey != dateKey) {
            return 0
        }
        return state.usedCount.coerceIn(0, AI_DAILY_FREE_REQUEST_LIMIT)
    }

    private fun activeAiRetryAtMillis(): Long? {
        val state = _uiState.value
        val retryAtMillis = state.aiRetryAtMillis.takeUnless { state.isAiRequestThrottlePause }
            ?: aiQuotaRetryStore.getRetryState()?.retryAtMillis
        if (retryAtMillis == null) {
            return null
        }
        if (retryAtMillis > clockMillisProvider()) {
            _uiState.update {
                it.copy(
                    aiRetryAtMillis = retryAtMillis,
                    isAiRequestThrottlePause = false
                )
            }
            scheduleAiRetryRefresh(retryAtMillis)
            return retryAtMillis
        }

        clearExpiredAiRetryStateIfNeeded()
        return null
    }

    private fun activeAiRequestThrottleAtMillis(): Long? {
        val lastRequestAtMillis = aiRequestThrottleStore.getLastRequestAtMillis() ?: return null
        val retryAtMillis = lastRequestAtMillis + AI_REQUEST_THROTTLE_MILLIS
        if (retryAtMillis > clockMillisProvider()) {
            _uiState.update {
                it.copy(
                    aiRetryAtMillis = retryAtMillis,
                    isAiRequestThrottlePause = true
                )
            }
            scheduleAiRetryRefresh(retryAtMillis)
            return retryAtMillis
        }
        aiRequestThrottleStore.clearLastRequest()
        clearExpiredAiRequestThrottleStateIfNeeded()
        return null
    }

    private fun clearExpiredAiRetryStateIfNeeded(): Boolean {
        val state = _uiState.value
        if (state.isAiRequestThrottlePause) {
            return clearExpiredAiRequestThrottleStateIfNeeded()
        }
        val retryAtMillis = state.aiRetryAtMillis ?: aiQuotaRetryStore.getRetryState()?.retryAtMillis
        if (retryAtMillis == null || retryAtMillis > clockMillisProvider()) {
            return false
        }

        _uiState.update {
            it.copy(
                aiRetryAtMillis = null,
                isAiRequestThrottlePause = false,
                isAiRetryNoticeVisible = false,
                aiUsesRemainingToday = aiDailyUsesRemaining()
            )
        }
        scheduleAiRetryRefresh(null)
        return true
    }

    private fun clearExpiredAiRequestThrottleStateIfNeeded(): Boolean {
        val state = _uiState.value
        val retryAtMillis = state.aiRetryAtMillis.takeIf { state.isAiRequestThrottlePause }
            ?: aiRequestThrottleStore.getLastRequestAtMillis()?.plus(AI_REQUEST_THROTTLE_MILLIS)
            ?: return false
        if (retryAtMillis > clockMillisProvider()) {
            return false
        }

        aiRequestThrottleStore.clearLastRequest()
        _uiState.update {
            if (!it.isAiRequestThrottlePause) {
                it
            } else {
                it.copy(
                    aiRetryAtMillis = null,
                    isAiRequestThrottlePause = false,
                    isAiRetryNoticeVisible = false,
                    aiUsesRemainingToday = aiDailyUsesRemaining()
                )
            }
        }
        scheduleAiRetryRefresh(null)
        return true
    }

    private fun showActiveAiRetryNotice(retryAtMillis: Long) {
        _uiState.update {
            it.copy(
                message = currentLanguage().aiRetryMessage(),
                aiRetryAtMillis = retryAtMillis,
                isAiRequestThrottlePause = false,
                isAiRetryNoticeVisible = true
            )
        }
        scheduleAiRetryRefresh(retryAtMillis)
    }

    private fun showAiRequestThrottleNotice(retryAtMillis: Long) {
        _uiState.update {
            it.copy(
                message = currentLanguage().aiRequestsPerMinuteMessage(),
                aiRetryAtMillis = retryAtMillis,
                isAiRequestThrottlePause = true,
                isAiRetryNoticeVisible = true
            )
        }
        scheduleAiRetryRefresh(retryAtMillis)
    }

    private fun showAiFailureNotice(notice: AiFailureNotice) {
        val retryAtMillis = notice.retryAtMillis?.withQuotaBackoff()
        val refreshAtMillis = retryAtMillis ?: _uiState.value.aiRetryAtMillis
            ?.takeIf { _uiState.value.isAiRequestThrottlePause }
        _uiState.update { current ->
            if (retryAtMillis == null) {
                current.copy(
                    message = notice.message,
                    isAiRetryNoticeVisible = false
                )
            } else {
                current.copy(
                    message = notice.message,
                    aiRetryAtMillis = retryAtMillis,
                    isAiRequestThrottlePause = false,
                    isAiRetryNoticeVisible = true
                )
            }
        }
        scheduleAiRetryRefresh(refreshAtMillis)
    }

    private fun startAiRequestThrottle() {
        val requestAtMillis = clockMillisProvider()
        aiRequestThrottleStore.saveLastRequestAtMillis(requestAtMillis)
    }

    private suspend fun <T> withAiRequestTimeout(block: suspend () -> Result<T>): Result<T> {
        return runCatching {
            withTimeout(AI_REQUEST_TIMEOUT_MILLIS) {
                block()
            }
        }.getOrElse { error ->
            Result.failure(error)
        }
    }

    private fun scheduleAiRetryRefresh(retryAtMillis: Long?) {
        aiRetryRefreshJob?.cancel()
        if (retryAtMillis == null) {
            aiRetryRefreshJob = null
            return
        }

        aiRetryRefreshJob = viewModelScope.launch {
            delay((retryAtMillis - clockMillisProvider()).coerceAtLeast(0L))
            clearExpiredAiRetryStateIfNeeded()
        }
    }

    private fun Long.withQuotaBackoff(): Long {
        val consecutiveFailures = (aiQuotaRetryStore.getRetryState()?.consecutiveFailures ?: 0) + 1
        val backedOffRetryAtMillis = maxOf(
            this,
            clockMillisProvider() + quotaBackoffMillis(consecutiveFailures)
        )
        aiQuotaRetryStore.saveRetryState(
            AiQuotaRetryState(
                retryAtMillis = backedOffRetryAtMillis,
                consecutiveFailures = consecutiveFailures
            )
        )
        return backedOffRetryAtMillis
    }
}

private data class AiFailureNotice(
    val message: String,
    val retryAtMillis: Long? = null
)

private fun Throwable.toAiFailureNotice(nowMillis: Long, language: AppLanguage): AiFailureNotice {
    val text = listOfNotNull(message, cause?.message).joinToString(" ").lowercase()
    return when {
        isAiQuotaExceeded() -> {
            val quotaLimitType = classifyAiQuotaLimitType(text)
            AiFailureNotice(
                message = quotaLimitType.message(language),
                retryAtMillis = text.retryAtMillis(nowMillis) ?: nextPacificMidnightMillis(nowMillis)
            )
        }
        this is ServiceDisabledException ||
            this is APINotConfiguredException ||
            "service_disabled" in text ||
            "api_key_service_blocked" in text -> AiFailureNotice(
                language.aiConfigurationMessage()
            )
        this is InvalidAPIKeyException ||
            "api key not valid" in text -> AiFailureNotice(
                language.aiInvalidApiKeyMessage()
            )
        this is RequestTimeoutException ||
            this is TimeoutCancellationException ||
            "timeout" in text ||
            "timed out" in text -> AiFailureNotice(
                language.aiTimeoutMessage()
            )
        else -> AiFailureNotice(language.aiGenericFailureMessage())
    }
}

private fun Throwable.analyticsFailureType(): String {
    val text = listOfNotNull(message, cause?.message).joinToString(" ").lowercase()
    return when {
        isAiQuotaExceeded() -> when (classifyAiQuotaLimitType(text)) {
            AiQuotaLimitType.REQUESTS_PER_MINUTE -> AI_FAILURE_QUOTA_REQUESTS
            AiQuotaLimitType.TOKENS_PER_MINUTE -> AI_FAILURE_QUOTA_TOKENS
            AiQuotaLimitType.REQUESTS_PER_DAY -> AI_FAILURE_QUOTA_DAILY
            AiQuotaLimitType.UNKNOWN -> AI_FAILURE_QUOTA
        }
        this is ServiceDisabledException ||
            this is APINotConfiguredException ||
            "service_disabled" in text ||
            "api_key_service_blocked" in text -> AI_FAILURE_CONFIGURATION
        this is InvalidAPIKeyException ||
            "api key not valid" in text -> AI_FAILURE_CONFIGURATION
        this is RequestTimeoutException ||
            this is TimeoutCancellationException ||
            "timeout" in text ||
            "timed out" in text -> AI_FAILURE_TIMEOUT
        else -> AI_FAILURE_GENERIC
    }
}

private fun List<FoodMenu>.countAnalyzed(): Int = count { it.healthAnalysis != null }

private fun List<FoodMenu>.countPendingAnalysis(): Int = count { it.healthAnalysis == null }

private fun String.retryAtMillis(nowMillis: Long): Long? {
    val seconds = Regex("""retry in ([0-9]+(?:\.[0-9]+)?)s""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toDoubleOrNull()
        ?: return null

    return nowMillis + ((ceil(seconds).toLong() + RETRY_GRACE_SECONDS) * 1000)
}

private fun DietaryProfile.findIngredientConflicts(input: String): List<String> {
    if (input.isBlank() || !hasRestrictions) {
        return emptyList()
    }

    val ingredients = input.split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return ingredients
        .filter { ingredient -> ingredient.conflictsWith(this) }
        .distinctBy { it.normalizedForFoodMatch() }
}

private fun String.conflictsWith(profile: DietaryProfile): Boolean {
    val normalized = normalizedForFoodMatch()
    if (profile.isVegan && normalized.containsAnyFoodTerm(VEGAN_EXCLUDED_TERMS)) {
        return true
    }
    if (profile.hasAllergies && profile.allergens.any { allergen ->
            normalized.containsAnyFoodTerm(allergen.excludedTerms())
        }
    ) {
        return true
    }

    return profile.otherAvoidances
        .split(',', ';', '\n')
        .map { it.normalizedForFoodMatch() }
        .filter { it.isNotBlank() }
        .any { avoidance -> normalized.containsFoodTerm(avoidance) }
}

private fun currentLanguage(): AppLanguage = AppLanguage.fromLocale()

private fun AppLanguage.noMenusForFilterMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "There are no menus for that filter."
        AppLanguage.FRENCH -> "Aucun menu ne correspond à ce filtre."
        AppLanguage.SPANISH -> "No hay menús para ese filtro."
    }
}

private fun AppLanguage.mealTypeRequiredMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "Choose breakfast, lunch or dinner."
        AppLanguage.FRENCH -> "Choisissez petit-déjeuner, déjeuner ou dîner."
        AppLanguage.SPANISH -> "Selecciona si es desayuno, almuerzo o cena."
    }
}

private fun AppLanguage.diceFilterRequiredMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "First choose breakfast, lunch or dinner."
        AppLanguage.FRENCH -> "Choisissez d'abord petit-déjeuner, déjeuner ou dîner."
        AppLanguage.SPANISH -> "Primero escoge desayuno, almuerzo o cena."
    }
}

private fun AppLanguage.audienceRequiredMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "Choose whether this menu is for an adult, kids or a baby."
        AppLanguage.FRENCH -> "Choisissez si ce menu est pour un adulte, des enfants ou un bébé."
        AppLanguage.SPANISH -> "Selecciona si el menú es para persona adulta, peques o bebé."
    }
}

private fun AppLanguage.missingRequiredFieldsMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "Add a name and ingredients to save the menu."
        AppLanguage.FRENCH -> "Ajoutez un nom et des ingrédients pour enregistrer le menu."
        AppLanguage.SPANISH -> "Agrega nombre e ingredientes para guardar el menú."
    }
}

private fun AppLanguage.noPendingMenusMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "You do not have pending menus to analyze."
        AppLanguage.FRENCH -> "Vous n'avez aucun menu en attente d'analyse."
        AppLanguage.SPANISH -> "No tienes menús pendientes por analizar."
    }
}

private fun AppLanguage.emptyAiBatchAnalysisMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI did not return valid analyses. You can try again later."
        AppLanguage.FRENCH -> "L'IA n'a pas renvoyé d'analyses valides. Vous pouvez réessayer plus tard."
        AppLanguage.SPANISH -> "La IA no devolvió análisis válidos. Puedes intentar de nuevo más tarde."
    }
}

private fun AppLanguage.aiRetryMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI is paused to avoid repeated attempts. Your menus are still available."
        AppLanguage.FRENCH -> "L'IA est en pause pour éviter les tentatives répétées. Vos menus restent disponibles."
        AppLanguage.SPANISH -> "La IA está en pausa para evitar intentos repetidos. Tus menús siguen disponibles."
    }
}

private fun AppLanguage.aiRequestsPerMinuteMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI received several requests close together. Wait a moment before trying again."
        AppLanguage.FRENCH -> "L'IA a reçu plusieurs demandes très rapprochées. Attendez un moment avant de réessayer."
        AppLanguage.SPANISH -> "La IA recibió varias peticiones muy seguidas. Espera un momento antes de volver a intentarlo."
    }
}

private fun AppLanguage.aiTokensPerMinuteMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "This idea needs a short break before being processed again. You can keep using your menus."
        AppLanguage.FRENCH -> "Cette idée a besoin d'une pause avant d'être traitée de nouveau. Vous pouvez continuer à utiliser vos menus."
        AppLanguage.SPANISH -> "La idea necesita una pausa antes de procesarse de nuevo. Puedes seguir usando tus menús."
    }
}

private fun AppLanguage.aiRequestsPerDayMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "Today's free AI help has run out. Your menus are still available and you can try again later."
        AppLanguage.FRENCH -> "L'aide IA gratuite du jour est épuisée. Vos menus restent disponibles et vous pourrez réessayer plus tard."
        AppLanguage.SPANISH -> "La ayuda gratuita con IA de hoy se agotó. Tus menús siguen disponibles y podrás volver a probar más adelante."
    }
}

private fun AppLanguage.generatedAnalysisManualEditMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "You changed the generated recipe. To see it as analyzed, save the menu and tap Analyze with AI."
        AppLanguage.FRENCH -> "Vous avez modifié la recette générée. Pour la voir comme analysée, enregistrez le menu et touchez Analyser avec l'IA."
        AppLanguage.SPANISH -> "Modificaste la receta generada. Para verla como analizada, guarda el menú y toca Analizar IA."
    }
}

private fun AppLanguage.aiConfigurationMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI is not enabled for this project or API key. Enable Firebase AI Logic / Generative Language API in Firebase or Google Cloud."
        AppLanguage.FRENCH -> "L'IA n'est pas activée pour ce projet ou cette clé API. Activez Firebase AI Logic / Generative Language API dans Firebase ou Google Cloud."
        AppLanguage.SPANISH -> "La IA no está habilitada para este proyecto o API key. Activa Firebase AI Logic / Generative Language API en Firebase o Google Cloud."
    }
}

private fun AppLanguage.aiInvalidApiKeyMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "The Firebase API key is not valid for AI. Check google-services.json or the key restrictions."
        AppLanguage.FRENCH -> "La clé API Firebase n'est pas valide pour l'IA. Vérifiez google-services.json ou les restrictions de la clé."
        AppLanguage.SPANISH -> "La API key de Firebase no es válida para IA. Revisa el archivo google-services.json o las restricciones de la clave."
    }
}

private fun AppLanguage.aiTimeoutMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI took too long to respond. Check your connection and try again."
        AppLanguage.FRENCH -> "L'IA a mis trop de temps à répondre. Vérifiez votre connexion et réessayez."
        AppLanguage.SPANISH -> "La IA tardó demasiado en responder. Revisa la conexión e inténtalo de nuevo."
    }
}

private fun AppLanguage.aiGenericFailureMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "Could not connect to AI. Check your internet connection or Firebase configuration."
        AppLanguage.FRENCH -> "Impossible de se connecter à l'IA. Vérifiez internet ou la configuration Firebase."
        AppLanguage.SPANISH -> "No se pudo conectar con la IA. Revisa internet o la configuración de Firebase."
    }
}

private fun List<String>.toIngredientConflictMessage(language: AppLanguage): String {
    val ingredients = joinToString(", ")
    return when (language) {
        AppLanguage.ENGLISH -> "Review the ingredients: $ingredients does not fit your food profile."
        AppLanguage.FRENCH -> "Vérifiez les ingrédients : $ingredients ne correspond pas à votre profil alimentaire."
        AppLanguage.SPANISH -> {
            val verb = if (size == 1) "no encaja" else "no encajan"
            "Revisa los ingredientes: $ingredients $verb con tu perfil alimentario."
        }
    }
}

private fun String.normalizedForFoodMatch(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
}

private fun String.containsAnyFoodTerm(terms: Set<String>): Boolean {
    return terms.any { containsFoodTerm(it) }
}

private fun String.containsFoodTerm(term: String): Boolean {
    if (term.isBlank()) return false
    val normalizedTerm = term.normalizedForFoodMatch()
    if (normalizedTerm.length <= 3) {
        return Regex("""(^|[^a-z0-9])${Regex.escape(normalizedTerm)}([^a-z0-9]|$)""")
            .containsMatchIn(this)
    }
    return contains(normalizedTerm)
}

private data class GeneratedIdeaMemory(
    val mealType: MealType,
    val audience: MenuAudience,
    val name: String,
    val description: String
)

private fun DietaryAllergen.excludedTerms(): Set<String> {
    return when (this) {
        DietaryAllergen.GLUTEN -> setOf("gluten", "trigo", "cebada", "centeno", "harina", "pan", "pasta")
        DietaryAllergen.DAIRY -> DAIRY_TERMS
        DietaryAllergen.EGG -> EGG_TERMS
        DietaryAllergen.TREE_NUTS -> setOf("frutos secos", "almendra", "nuez", "nueces", "avellana", "pistacho", "anacardo")
        DietaryAllergen.PEANUT -> setOf("cacahuete", "mani")
        DietaryAllergen.SOY -> setOf("soja", "soya", "tofu", "edamame", "tamari")
        DietaryAllergen.FISH -> FISH_TERMS
        DietaryAllergen.SHELLFISH -> SHELLFISH_TERMS
        DietaryAllergen.SESAME -> setOf("sesamo", "tahini")
    }
}

private val DAIRY_TERMS = setOf("lactosa", "lacteo", "lacteos", "leche", "queso", "yogur", "yogurt", "mantequilla", "nata", "crema")
private val EGG_TERMS = setOf("huevo", "huevos", "tortilla francesa")
private val FISH_TERMS = setOf("pescado", "atun", "salmon", "merluza", "bacalao", "sardina", "anchoa")
private val SHELLFISH_TERMS = setOf("marisco", "gamba", "gambas", "langostino", "cangrejo", "mejillon", "almeja")
private val MEAT_TERMS = setOf("pollo", "ternera", "cerdo", "jamon", "pavo", "carne", "chorizo", "panceta", "bacon")
private val VEGAN_EXCLUDED_TERMS = DAIRY_TERMS + EGG_TERMS + FISH_TERMS + SHELLFISH_TERMS + MEAT_TERMS + setOf("miel")

private const val RETRY_GRACE_SECONDS = 2L
private const val AI_DAILY_FREE_REQUEST_LIMIT = 20
private const val AI_BATCH_ANALYSIS_LIMIT = 5
private const val AI_SOURCE_GENERATE_MENU = "generate_menu"
private const val AI_SOURCE_ANALYZE_SINGLE = "analyze_single"
private const val AI_SOURCE_ANALYZE_BATCH = "analyze_batch"
private const val AI_SCOPE_SINGLE = "single"
private const val AI_SCOPE_BATCH = "batch"
private const val FORM_FIELD_NAME = "name"
private const val FORM_FIELD_DESCRIPTION = "description"
private const val FORM_FIELD_NOTES = "notes"
private const val ONBOARDING_ACTION_START = "start"
private const val ONBOARDING_ACTION_SKIP = "skip"
private const val CURRENT_ONBOARDING_VERSION = 2
private const val ANALYTICS_SOURCE_DICE = "dice"
private const val ANALYTICS_SOURCE_FORM = "form"
private const val ANALYTICS_PROFILE_FIELD_ENABLED = "enabled"
private const val ANALYTICS_PROFILE_FIELD_AGE_RANGE = "age_range"
private const val ANALYTICS_PROFILE_FIELD_PREGNANT = "pregnant"
private const val ANALYTICS_PROFILE_FIELD_VEGAN = "vegan"
private const val ANALYTICS_PROFILE_FIELD_ALLERGIES = "allergies"
private const val ANALYTICS_PROFILE_FIELD_ALLERGENS = "allergens"
private const val ANALYTICS_PROFILE_FIELD_OTHER_AVOIDANCES = "other_avoidances"
private const val MENU_SAVE_BLOCKED_MISSING_MEAL_TYPE = "missing_meal_type"
private const val MENU_SAVE_BLOCKED_MISSING_AUDIENCE = "missing_audience"
private const val MENU_SAVE_BLOCKED_MISSING_REQUIRED_FIELDS = "missing_required_fields"
private const val AI_FAILURE_QUOTA_REQUESTS = "quota_requests"
private const val AI_FAILURE_QUOTA_TOKENS = "quota_tokens"
private const val AI_FAILURE_QUOTA_DAILY = "quota_daily"
private const val AI_FAILURE_QUOTA = "quota"
private const val AI_FAILURE_CONFIGURATION = "configuration"
private const val AI_FAILURE_TIMEOUT = "timeout"
private const val AI_FAILURE_GENERIC = "generic"
private const val FIRST_QUOTA_BACKOFF_MILLIS = 0L
private const val SECOND_QUOTA_BACKOFF_MILLIS = 2 * 60 * 1000L
private const val MAX_QUOTA_BACKOFF_MILLIS = 30 * 60 * 1000L
private const val AI_REQUEST_THROTTLE_MILLIS = 4 * 1000L
private const val AI_REQUEST_TIMEOUT_MILLIS = 25 * 1000L
private fun AiQuotaLimitType.message(language: AppLanguage): String {
    return when (this) {
        AiQuotaLimitType.REQUESTS_PER_MINUTE -> language.aiRequestsPerMinuteMessage()
        AiQuotaLimitType.TOKENS_PER_MINUTE -> language.aiTokensPerMinuteMessage()
        AiQuotaLimitType.REQUESTS_PER_DAY -> language.aiRequestsPerDayMessage()
        AiQuotaLimitType.UNKNOWN -> language.aiRetryMessage()
    }
}

private fun quotaBackoffMillis(consecutiveFailures: Int): Long {
    if (consecutiveFailures <= 1) {
        return FIRST_QUOTA_BACKOFF_MILLIS
    }
    val multiplier = 1L shl (consecutiveFailures - 2).coerceAtMost(10)
    return (SECOND_QUOTA_BACKOFF_MILLIS * multiplier).coerceAtMost(MAX_QUOTA_BACKOFF_MILLIS)
}

private fun suggestedMealTypeForDeviceTime(nowMillis: Long): MealType {
    val hour = Calendar.getInstance().apply {
        timeInMillis = nowMillis
    }.get(Calendar.HOUR_OF_DAY)

    return when (hour) {
        in 5..10 -> MealType.BREAKFAST
        in 11..16 -> MealType.LUNCH
        else -> MealType.DINNER
    }
}

private fun nextPacificMidnightMillis(nowMillis: Long): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles")).apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private fun currentPacificDateKey(nowMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("America/Los_Angeles")
    }.format(Date(nowMillis))
}
