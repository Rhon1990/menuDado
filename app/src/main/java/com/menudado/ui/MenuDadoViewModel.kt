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
import com.menudado.data.MenuRepository
import com.menudado.data.NoOpAiDailyUsageStore
import com.menudado.data.NoOpAiQuotaRetryStore
import com.menudado.data.NoOpOnboardingStore
import com.menudado.data.OnboardingStore
import com.menudado.domain.DiceSelector
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.MenuAudience
import com.menudado.domain.MealType
import com.menudado.domain.AiQuotaLimitType
import com.menudado.domain.classifyAiQuotaLimitType
import com.menudado.domain.isAiQuotaExceeded
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    init {
        refreshOnboarding()
        refreshDietaryProfile()
        refreshStoredAiRetry()
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

    fun setDiceAudienceFilter(filter: MenuAudience?) {
        _uiState.update {
            if (filter != null && filter !in it.enabledAudiences) {
                it
            } else {
                it.copy(diceAudienceFilter = filter)
            }
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
    }

    fun updateName(value: String) {
        trackMenuFormStartedIfNeeded(FORM_FIELD_NAME, value)
        _uiState.update {
            val notice = it.generatedHealthAnalysis.manualEditNotice()
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
            val notice = it.generatedHealthAnalysis.manualEditNotice()
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
            val notice = it.generatedHealthAnalysis.manualEditNotice()
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
            val notice = it.generatedHealthAnalysis.manualEditNotice()
            it.copy(
                aiBaseIngredients = value,
                generatedHealthAnalysis = null,
                message = notice ?: it.message,
                isAiRetryNoticeVisible = if (notice != null) false else it.isAiRetryNoticeVisible
            )
        }
    }

    fun setDietaryProfileVegan(isVegan: Boolean) {
        updateActiveDietaryProfile { it.copy(isVegan = isVegan) }
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
    }

    fun updateDietaryProfileAgeRange(ageRange: String) {
        updateDietaryProfile { it.copy(ageRange = ageRange) }
    }

    fun setDietaryProfilePregnant(isPregnant: Boolean) {
        updateActiveDietaryProfile { it.copy(isPregnant = isPregnant) }
    }

    fun setDietaryProfileHasAllergies(hasAllergies: Boolean) {
        updateActiveDietaryProfile {
            it.copy(
                hasAllergies = hasAllergies,
                allergens = if (hasAllergies) it.allergens else emptySet()
            )
        }
    }

    fun toggleDietaryAllergen(allergen: DietaryAllergen) {
        updateActiveDietaryProfile { profile ->
            val allergens = if (allergen in profile.allergens) {
                profile.allergens - allergen
            } else {
                profile.allergens + allergen
            }
            profile.copy(hasAllergies = true, allergens = allergens)
        }
    }

    fun updateDietaryProfileOtherAvoidances(value: String) {
        updateActiveDietaryProfile { it.copy(otherAvoidances = value) }
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
        _uiState.update {
            it.copy(
                editingMenuId = menu.id,
                editMealType = menu.mealType,
                editAudience = menu.audience,
                editName = menu.name,
                editDescription = menu.description,
                editNotes = menu.notes,
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
            _uiState.update { it.copy(message = DICE_FILTER_REQUIRED_MESSAGE, isAiRetryNoticeVisible = false) }
            return
        }
        if (state.diceAudienceFilter == null) {
            _uiState.update { it.copy(message = AUDIENCE_REQUIRED_MESSAGE, isAiRetryNoticeVisible = false) }
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
                    message = if (selected == null && !hasCandidates) "No hay menus para ese filtro." else null,
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
            _uiState.update { it.copy(message = MEAL_TYPE_REQUIRED_MESSAGE, isAiRetryNoticeVisible = false) }
            return
        }

        if (audience == null) {
            _uiState.update { it.copy(message = AUDIENCE_REQUIRED_MESSAGE, isAiRetryNoticeVisible = false) }
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
                    message = "Agrega nombre e ingredientes para guardar el menu.",
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
            _uiState.update { it.copy(message = MEAL_TYPE_REQUIRED_MESSAGE, isAiRetryNoticeVisible = false) }
            return
        }

        if (audience == null) {
            _uiState.update { it.copy(message = AUDIENCE_REQUIRED_MESSAGE, isAiRetryNoticeVisible = false) }
            return
        }

        if (name.isBlank() || description.isBlank()) {
            _uiState.update {
                it.copy(
                    message = "Agrega nombre e ingredientes para guardar el menu.",
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
                calories = if (changedExistingMenu) null else existingMenu.calories
            )

            repository.save(menu)
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
        val mealType = state.formMealType
        val audience = state.formAudience
        if (mealType == null) {
            _uiState.update { it.copy(message = MEAL_TYPE_REQUIRED_MESSAGE, isAiRetryNoticeVisible = false) }
            return
        }
        if (audience == null) {
            _uiState.update { it.copy(message = AUDIENCE_REQUIRED_MESSAGE, isAiRetryNoticeVisible = false) }
            return
        }
        val profile = dietaryProfileStore.getProfile(audience)
        val ingredientConflicts = profile.findIngredientConflicts(state.aiBaseIngredients)
        if (ingredientConflicts.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    message = ingredientConflicts.toIngredientConflictMessage(),
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
        if (!consumeAiDailyUseOrShowNotice(AI_SOURCE_GENERATE_MENU)) {
            return
        }
        val avoidIdeas = state.buildAvoidIdeas(mealType, audience)
        analytics.trackAiMenuGenerationStarted(mealType, avoidIdeas.size)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingMenu = true,
                    message = null,
                    aiRetryAtMillis = null,
                    isAiRetryNoticeVisible = false
                )
            }
            repository.generateMenu(
                mealType = mealType,
                avoidIdeas = avoidIdeas,
                dietaryProfile = profile,
                audience = audience,
                baseIngredients = state.aiBaseIngredients.trim()
            )
                .onSuccess { generated ->
                    aiQuotaRetryStore.clearRetryState()
                    _uiState.update {
                        it.copy(
                            name = generated.name,
                            description = generated.description,
                            notes = generated.notes,
                            calories = generated.calories,
                            generatedHealthAnalysis = generated.healthAnalysis,
                            aiRetryAtMillis = null,
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
                    val notice = error.toAiFailureNotice(clockMillisProvider())
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

        val currentName = name.trim()
        val currentDescription = description.trim()
        val currentIdea = if (currentName.isNotBlank() || currentDescription.isNotBlank()) {
            listOf("${currentName.ifBlank { "Idea actual" }}: $currentDescription".trim())
        } else {
            emptyList()
        }

        return (savedIdeas + currentIdea)
            .distinct()
            .takeLast(8)
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

    private fun updateDietaryProfile(transform: (DietaryProfile) -> DietaryProfile) {
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
    }

    private fun updateActiveDietaryProfile(transform: (DietaryProfile) -> DietaryProfile) {
        if (!_uiState.value.dietaryProfile.isEnabled) {
            return
        }
        updateDietaryProfile(transform)
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

    private fun HealthAnalysis?.manualEditNotice(): String? {
        return if (this == null) null else GENERATED_ANALYSIS_MANUAL_EDIT_MESSAGE
    }

    fun analyzeExisting(menu: FoodMenu) {
        val activeRetryAtMillis = activeAiRetryAtMillis()
        if (activeRetryAtMillis != null) {
            showActiveAiRetryNotice(activeRetryAtMillis)
            return
        }
        if (!consumeAiDailyUseOrShowNotice(AI_SOURCE_ANALYZE_SINGLE)) {
            return
        }
        analytics.trackAiAnalysisStarted(AI_SCOPE_SINGLE, menu.mealType, menuCount = 1)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    message = null,
                    aiRetryAtMillis = null,
                    isAiRetryNoticeVisible = false
                )
            }
            repository.analyze(menu)
                .onSuccess { analysis ->
                    repository.save(
                        menu.copy(
                            healthAnalysis = analysis,
                            calories = analysis.calories ?: menu.calories
                        )
                    )
                    aiQuotaRetryStore.clearRetryState()
                    _uiState.update { it.copy(aiRetryAtMillis = null, isAiRetryNoticeVisible = false) }
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
                    val notice = error.toAiFailureNotice(clockMillisProvider())
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
        val pendingMenus = _uiState.value.menus
            .filter { it.healthAnalysis == null }
            .take(AI_BATCH_ANALYSIS_LIMIT)

        if (pendingMenus.isEmpty()) {
            _uiState.update {
                it.copy(
                    message = "No tienes menus pendientes por analizar.",
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
        if (!consumeAiDailyUseOrShowNotice(AI_SOURCE_ANALYZE_BATCH)) {
            return
        }
        analytics.trackAiAnalysisStarted(AI_SCOPE_BATCH, mealType = null, menuCount = pendingMenus.size)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    message = null,
                    aiRetryAtMillis = null,
                    isAiRetryNoticeVisible = false
                )
            }
            repository.analyzeBatch(pendingMenus)
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
                            aiRetryAtMillis = null,
                            isAiRetryNoticeVisible = false,
                            message = if (analysesByMenuId.isEmpty()) {
                                "La IA no devolvio analisis validos. Puedes intentar de nuevo mas tarde."
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
                    val notice = error.toAiFailureNotice(clockMillisProvider())
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
                editNotes = ""
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
            _uiState.update { it.copy(aiRetryAtMillis = state.retryAtMillis) }
            scheduleAiRetryRefresh(state.retryAtMillis)
        }
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
                    message = AI_REQUESTS_PER_DAY_MESSAGE,
                    aiRetryAtMillis = retryAtMillis,
                    isAiRetryNoticeVisible = true,
                    aiUsesRemainingToday = 0
                )
            }
            analytics.trackAiDailyLimitReached(source)
            scheduleAiRetryRefresh(retryAtMillis)
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
        val retryAtMillis = _uiState.value.aiRetryAtMillis ?: aiQuotaRetryStore.getRetryState()?.retryAtMillis
        if (retryAtMillis == null) {
            return null
        }
        if (retryAtMillis > clockMillisProvider()) {
            _uiState.update { it.copy(aiRetryAtMillis = retryAtMillis) }
            scheduleAiRetryRefresh(retryAtMillis)
            return retryAtMillis
        }

        clearExpiredAiRetryStateIfNeeded()
        return null
    }

    private fun clearExpiredAiRetryStateIfNeeded(): Boolean {
        val retryAtMillis = _uiState.value.aiRetryAtMillis ?: aiQuotaRetryStore.getRetryState()?.retryAtMillis
        if (retryAtMillis == null || retryAtMillis > clockMillisProvider()) {
            return false
        }

        _uiState.update {
            it.copy(
                aiRetryAtMillis = null,
                isAiRetryNoticeVisible = false,
                aiUsesRemainingToday = aiDailyUsesRemaining()
            )
        }
        scheduleAiRetryRefresh(null)
        return true
    }

    private fun showActiveAiRetryNotice(retryAtMillis: Long) {
        _uiState.update {
            it.copy(
                message = AI_RETRY_MESSAGE,
                aiRetryAtMillis = retryAtMillis,
                isAiRetryNoticeVisible = true
            )
        }
        scheduleAiRetryRefresh(retryAtMillis)
    }

    private fun showAiFailureNotice(notice: AiFailureNotice) {
        val retryAtMillis = notice.retryAtMillis?.withQuotaBackoff()
        _uiState.update { current ->
            current.copy(
                message = notice.message,
                aiRetryAtMillis = retryAtMillis,
                isAiRetryNoticeVisible = retryAtMillis != null
            )
        }
        scheduleAiRetryRefresh(retryAtMillis)
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

private fun Throwable.toAiFailureNotice(nowMillis: Long): AiFailureNotice {
    val text = listOfNotNull(message, cause?.message).joinToString(" ").lowercase()
    return when {
        isAiQuotaExceeded() -> {
            val quotaLimitType = classifyAiQuotaLimitType(text)
            AiFailureNotice(
                message = quotaLimitType.message(),
                retryAtMillis = text.retryAtMillis(nowMillis) ?: nextPacificMidnightMillis(nowMillis)
            )
        }
        this is ServiceDisabledException ||
            this is APINotConfiguredException ||
            "service_disabled" in text ||
            "api_key_service_blocked" in text -> AiFailureNotice(
                "La IA no esta habilitada para este proyecto o API key. Activa Firebase AI Logic / Generative Language API en Firebase o Google Cloud."
            )
        this is InvalidAPIKeyException ||
            "api key not valid" in text -> AiFailureNotice(
                "La API key de Firebase no es valida para IA. Revisa el archivo google-services.json o las restricciones de la clave."
            )
        this is RequestTimeoutException ||
            "timeout" in text -> AiFailureNotice(
                "La IA tardo demasiado en responder. Revisa la conexion e intentalo de nuevo."
            )
        else -> AiFailureNotice("No se pudo conectar con la IA. Revisa internet o la configuracion de Firebase.")
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
            "timeout" in text -> AI_FAILURE_TIMEOUT
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

private fun List<String>.toIngredientConflictMessage(): String {
    val ingredients = joinToString(", ")
    val verb = if (size == 1) "no encaja" else "no encajan"
    return "Revisa los ingredientes: $ingredients $verb con tu perfil alimentario."
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
private const val MENU_SAVE_BLOCKED_MISSING_MEAL_TYPE = "missing_meal_type"
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
private const val AI_RETRY_MESSAGE = "La IA esta tomando una pausa para evitar intentos fallidos. Tus menus siguen disponibles."
private const val AI_REQUESTS_PER_MINUTE_MESSAGE = "La IA recibio varias peticiones seguidas. Espera un momento antes de volver a intentarlo."
private const val AI_TOKENS_PER_MINUTE_MESSAGE = "La idea necesita un descanso antes de procesarse de nuevo. Puedes seguir usando tus menus."
private const val AI_REQUESTS_PER_DAY_MESSAGE = "La ayuda con IA gratuita de hoy se agoto. Tus menus siguen disponibles y podras volver a probar mas adelante."
private const val GENERATED_ANALYSIS_MANUAL_EDIT_MESSAGE = "Modificaste la receta generada. Para verla como analizada, guarda el menu y toca Analizar IA."
private const val MEAL_TYPE_REQUIRED_MESSAGE = "Selecciona si es desayuno, almuerzo o cena."
private const val DICE_FILTER_REQUIRED_MESSAGE = "Primero escoge desayuno, almuerzo o cena."
private const val AUDIENCE_REQUIRED_MESSAGE = "Selecciona si el menu es para adulto, niño o bebe."

private fun AiQuotaLimitType.message(): String {
    return when (this) {
        AiQuotaLimitType.REQUESTS_PER_MINUTE -> AI_REQUESTS_PER_MINUTE_MESSAGE
        AiQuotaLimitType.TOKENS_PER_MINUTE -> AI_TOKENS_PER_MINUTE_MESSAGE
        AiQuotaLimitType.REQUESTS_PER_DAY -> AI_REQUESTS_PER_DAY_MESSAGE
        AiQuotaLimitType.UNKNOWN -> AI_RETRY_MESSAGE
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
