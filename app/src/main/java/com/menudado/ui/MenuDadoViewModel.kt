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
import com.menudado.data.FormAudienceSelectionStore
import com.menudado.data.NoOpDietaryProfileStore
import com.menudado.data.NoOpFormAudienceSelectionStore
import com.menudado.data.AiDailyUsageState
import com.menudado.data.AiDailyUsageStore
import com.menudado.data.AiQuotaRetryStore
import com.menudado.data.AiQuotaRetryState
import com.menudado.data.AiRequestThrottleStore
import com.menudado.data.AiMenuHiveGateway
import com.menudado.data.AiMenuHiveContribution
import com.menudado.data.AiMenuHiveLookupSource
import com.menudado.data.AiMenuHiveSearchRequest
import com.menudado.data.NoOpAiMenuHiveGateway
import com.menudado.data.CuisineRotation
import com.menudado.data.InMemoryCuisineRotationStateStore
import com.menudado.data.GuestDailyUsageState
import com.menudado.data.GuestUsageStore
import com.menudado.data.HiveRotationSnapshot
import com.menudado.data.HiveRotationStore
import com.menudado.data.MenuRepository
import com.menudado.data.NoOpAiDailyUsageStore
import com.menudado.data.NoOpAiQuotaRetryStore
import com.menudado.data.NoOpAiRequestThrottleStore
import com.menudado.data.NoOpGuestUsageStore
import com.menudado.data.NoOpHiveRotationStore
import com.menudado.data.NoOpOnboardingStore
import com.menudado.data.NoOpRewardedAiCreditStore
import com.menudado.data.NoOpScopedAiUsageStore
import com.menudado.data.OnboardingStore
import com.menudado.data.PROVIDER_AI_USAGE_SCOPE
import com.menudado.data.RewardedAiCreditLedger
import com.menudado.data.RewardedAiCreditStore
import com.menudado.data.ScopedAiUsageStore
import com.menudado.data.GUEST_AI_USAGE_SCOPE
import com.menudado.data.LOCAL_ACCOUNT_AI_USAGE_SCOPE
import com.menudado.data.aiUsageScope
import com.menudado.domain.AI_PROVIDER_DAILY_HARD_LIMIT
import com.menudado.domain.AiDailyUsagePolicy
import com.menudado.domain.AiGenerationAccess
import com.menudado.domain.DiceSelector
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import com.menudado.domain.CuisineInspiration
import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.MarketProduct
import com.menudado.domain.MAX_REWARDED_AI_CREDITS_PER_DAY
import com.menudado.domain.SIGNED_IN_DAILY_AI_FREE_LIMIT
import com.menudado.domain.ShoppingProduct
import com.menudado.domain.AppLanguage
import com.menudado.domain.GUEST_DAILY_AI_ANALYSIS_LIMIT
import com.menudado.domain.GUEST_DAILY_AI_FREE_LIMIT
import com.menudado.domain.GUEST_DAILY_AI_GENERATION_LIMIT
import com.menudado.domain.GUEST_DAILY_MENU_SAVE_LIMIT
import com.menudado.domain.GuestAccessPolicy
import com.menudado.domain.MenuAudience
import com.menudado.domain.MealType
import com.menudado.domain.AiQuotaLimitType
import com.menudado.domain.AiMenuHiveIdentity
import com.menudado.domain.classifyAiQuotaLimitType
import com.menudado.domain.findIngredientConflicts
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

data class MenuDadoUiState(
    val menus: List<FoodMenu> = emptyList(),
    val homeMenuMode: HomeMenuMode = HomeMenuMode.Ai,
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
    val generatedShoppingProducts: List<ShoppingProduct> = emptyList(),
    val addGeneratedMenuToMarketList: Boolean = true,
    val marketProducts: List<MarketProduct> = emptyList(),
    val generatedCuisineInspiration: CuisineInspiration? = null,
    val generatedDeduplicationKey: String? = null,
    val generatedOrigin: GeneratedMenuOrigin? = null,
    val generatedSemanticHash: String? = null,
    val isRolling: Boolean = false,
    val isAnalyzing: Boolean = false,
    val aiGenerationPhase: AiGenerationPhase = AiGenerationPhase.IDLE,
    val result: FoodMenu? = null,
    val message: String? = null,
    val menuSaveSuccessRevision: Long = 0L,
    val aiRetryAtMillis: Long? = null,
    val isAiRequestThrottlePause: Boolean = false,
    val isAiRetryNoticeVisible: Boolean = false,
    val aiUsesRemainingToday: Int = SIGNED_IN_DAILY_AI_FREE_LIMIT,
    val aiGenerationUsesRemainingToday: Int = SIGNED_IN_DAILY_AI_FREE_LIMIT,
    val aiAnalysisUsesRemainingToday: Int = SIGNED_IN_DAILY_AI_FREE_LIMIT,
    val isAiProviderAvailableToday: Boolean = true,
    val aiGenerationLimitState: AiGenerationLimitState = AiGenerationLimitState.AVAILABLE,
    val rewardedCreditsRemainingToday: Int = MAX_REWARDED_AI_CREDITS_PER_DAY,
    val isRewardedGenerationPending: Boolean = false,
    val isRewardedMenuRevealPending: Boolean = false,
    val enabledAudiences: List<MenuAudience> = MenuAudience.entries,
    val audienceAgeRanges: Map<MenuAudience, String> = MenuAudience.entries.associateWith { it.defaultAgeRange },
    val dietaryProfileAudience: MenuAudience = MenuAudience.ADULT,
    val dietaryProfile: DietaryProfile = DietaryProfile(),
    val showOnboarding: Boolean = false,
    val showGeneratedMenuDetail: Boolean = false,
    val diceEmptyRecovery: DiceEmptyRecovery? = null
) {
    val isGeneratingMenu: Boolean
        get() = aiGenerationPhase.isActive

    val canRequestRewardedGeneration: Boolean
        get() = aiGenerationLimitState == AiGenerationLimitState.REWARDED_OFFER &&
            !isRewardedGenerationPending
}

enum class AiGenerationLimitState {
    AVAILABLE,
    REWARDED_OFFER,
    HARD_LIMIT
}

enum class AiGenerationPhase {
    IDLE,
    GENERATING,
    GENERATING_SLOW,
    SEARCHING_HIVE;

    val isActive: Boolean
        get() = this != IDLE
}

enum class GeneratedMenuOrigin { LIVE_AI, HIVE_FALLBACK }

internal fun menuVisibleMenus(
    menus: List<FoodMenu>,
    enabledAudiences: List<MenuAudience>
): List<FoodMenu> = menus.filter { menu -> menu.audience in enabledAudiences }

data class DiceEmptyRecovery(
    val mealType: MealType,
    val audience: MenuAudience,
    val canBroadenMealType: Boolean
)

enum class HomeMenuMode {
    Ai,
    Manual
}

class MenuDadoViewModel(
    private val repository: MenuRepository,
    private val analytics: MenuDadoAnalytics = NoOpMenuDadoAnalytics,
    private val todayProvider: () -> String = { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
    private val clockMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val aiQuotaRetryStore: AiQuotaRetryStore = NoOpAiQuotaRetryStore,
    private val aiRequestThrottleStore: AiRequestThrottleStore = NoOpAiRequestThrottleStore,
    private val aiDailyUsageStore: AiDailyUsageStore = NoOpAiDailyUsageStore,
    private val scopedAiUsageStore: ScopedAiUsageStore = NoOpScopedAiUsageStore,
    initialAiUsageScope: String = LOCAL_ACCOUNT_AI_USAGE_SCOPE,
    private val guestUsageStore: GuestUsageStore = NoOpGuestUsageStore,
    private val rewardedAiCreditStore: RewardedAiCreditStore = NoOpRewardedAiCreditStore,
    private val dietaryProfileStore: DietaryProfileStore = NoOpDietaryProfileStore,
    private val formAudienceSelectionStore: FormAudienceSelectionStore =
        NoOpFormAudienceSelectionStore,
    private val onboardingStore: OnboardingStore = NoOpOnboardingStore,
    private val cuisineRotation: CuisineRotation = CuisineRotation(InMemoryCuisineRotationStateStore()),
    private val aiMenuHive: AiMenuHiveGateway = NoOpAiMenuHiveGateway,
    private val hiveRotationStore: HiveRotationStore = NoOpHiveRotationStore
) : ViewModel() {
    private val suggestedMealType = suggestedMealTypeForDeviceTime(clockMillisProvider())
    private val _uiState = MutableStateFlow(
        MenuDadoUiState(
            diceFilter = suggestedMealType,
            formMealType = suggestedMealType,
            formAudience = formAudienceSelectionStore.getSelectedAudience()
        )
    )
    val uiState: StateFlow<MenuDadoUiState> = _uiState.asStateFlow()
    private var aiRetryRefreshJob: Job? = null
    private var hasTrackedMenuFormStarted = false
    private var generatedIdeaDateKey: String? = null
    private val generatedIdeasToday = mutableListOf<GeneratedIdeaMemory>()
    private var guestAccessPolicy = GuestAccessPolicy(
        isGuest = false,
        areLimitsEnabled = true
    )
    private var areGuestAiLimitsEnabled = true
    private var activeAiUsageScope = initialAiUsageScope
    private var pendingRewardedGenerationRequest: ValidatedGenerationRequest? = null

    init {
        migrateLegacyAiUsageIfNeeded()
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
        viewModelScope.launch {
            repository.marketProducts.collect { products ->
                _uiState.update { it.copy(marketProducts = products) }
            }
        }
    }

    fun setDiceFilter(filter: MealType?) {
        analytics.trackDiceFilterSelected(filter, _uiState.value.menus.size)
        _uiState.update { it.copy(diceFilter = filter, diceEmptyRecovery = null) }
    }

    fun setHomeMenuMode(mode: HomeMenuMode) {
        _uiState.update { it.copy(homeMenuMode = mode) }
    }

    fun trackCtaTapped(screen: String, cta: String) {
        analytics.trackCtaTapped(screen, cta)
    }

    fun trackRewardedGenerationOfferShown() {
        if (_uiState.value.aiGenerationLimitState != AiGenerationLimitState.REWARDED_OFFER) {
            return
        }
        analytics.trackAiRewardedOffer(
            status = AI_REWARDED_STATUS_SHOWN,
            creditsRemaining = rewardedCreditsRemainingToday()
        )
    }

    fun trackMyZoneOpened() {
        analytics.trackMyZoneOpened(
            authMode = currentAuthMode(),
            menuCount = _uiState.value.menus.size
        )
    }

    fun trackAuthFlowStarted(mode: String) {
        analytics.trackAuthFlowStarted(
            mode = mode,
            authMode = currentAuthMode(),
            menuCount = _uiState.value.menus.size
        )
    }

    fun trackAuthAction(action: String, method: String) {
        analytics.trackAuthAction(
            action = action,
            method = method,
            authMode = currentAuthMode()
        )
    }

    fun updateGuestAccess(
        isGuest: Boolean,
        areLimitsEnabled: Boolean,
        areAiLimitsEnabled: Boolean = true,
        userId: String? = null
    ) {
        val updatedScope = if (
            !isGuest &&
            userId.isNullOrBlank() &&
            activeAiUsageScope.startsWith("account:")
        ) {
            activeAiUsageScope
        } else {
            aiUsageScope(isGuest = isGuest, userId = userId)
        }
        val didScopeChange = activeAiUsageScope != updatedScope
        activeAiUsageScope = updatedScope
        guestAccessPolicy = GuestAccessPolicy(
            isGuest = isGuest,
            areLimitsEnabled = areLimitsEnabled
        )
        areGuestAiLimitsEnabled = areAiLimitsEnabled
        migrateLegacyAiUsageIfNeeded()
        if (didScopeChange && aiQuotaRetryStore.getRetryState() == null) {
            aiRetryRefreshJob?.cancel()
            aiRetryRefreshJob = null
            pendingRewardedGenerationRequest = null
            _uiState.update {
                it.copy(
                    aiRetryAtMillis = null,
                    isAiRequestThrottlePause = false,
                    isAiRetryNoticeVisible = false,
                    isRewardedGenerationPending = false
                )
            }
        }
        refreshAiUsageCounters()
    }

    fun setDiceAudienceFilter(filter: MenuAudience?) {
        val state = _uiState.value
        _uiState.update {
            if (filter != null && filter !in it.enabledAudiences) {
                it
            } else {
                it.copy(diceAudienceFilter = filter, diceEmptyRecovery = null)
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
            formAudienceSelectionStore.saveSelectedAudience(audience)
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
                generatedShoppingProducts = emptyList(),
                generatedDeduplicationKey = null,
                generatedOrigin = null,
                generatedSemanticHash = null,
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
                generatedShoppingProducts = emptyList(),
                generatedDeduplicationKey = null,
                generatedOrigin = null,
                generatedSemanticHash = null,
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
                generatedShoppingProducts = emptyList(),
                generatedDeduplicationKey = null,
                generatedOrigin = null,
                generatedSemanticHash = null,
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
                generatedShoppingProducts = emptyList(),
                generatedDeduplicationKey = null,
                generatedOrigin = null,
                generatedSemanticHash = null,
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
                audienceAgeRanges = loadAudienceAgeRanges()
            ).withAudienceVisibilityState(
                enabledAudiences = enabledAudiences,
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

    fun discardGeneratedMenuIdea() {
        _uiState.update { it.withoutMenuFormDraft().copy(showGeneratedMenuDetail = false) }
    }

    fun tryAnotherGeneratedMenuIdea() {
        if (_uiState.value.isGeneratingMenu) return
        _uiState.update {
            it.copy(
                name = "",
                description = "",
                notes = "",
                calories = null,
                generatedHealthAnalysis = null,
                generatedShoppingProducts = emptyList(),
                generatedCuisineInspiration = null,
                generatedDeduplicationKey = null,
                generatedOrigin = null,
                generatedSemanticHash = null,
                showGeneratedMenuDetail = false
            )
        }
        generateMenuIdea()
    }

    fun saveGeneratedMenuIdea() {
        saveMenu(ignoreGuestMenuSaveLimit = true)
    }

    fun setAddGeneratedMenuToMarketList(isEnabled: Boolean) {
        _uiState.update { it.copy(addGeneratedMenuToMarketList = isEnabled) }
    }

    fun setMenuInMarketList(menu: FoodMenu, isEnabled: Boolean) {
        if (menu.shoppingProducts.isEmpty()) return
        viewModelScope.launch {
            repository.save(
                menu.copy(
                    activeShoppingProductKeys = if (isEnabled) {
                        menu.shoppingProducts.mapTo(linkedSetOf()) { it.key }
                    } else {
                        emptySet()
                    }
                )
            )
        }
    }

    fun setMarketProductPurchased(productKey: String, isPurchased: Boolean) {
        viewModelScope.launch {
            repository.setMarketProductPurchased(productKey, isPurchased)
        }
    }

    fun clearPurchasedMarketProducts() {
        viewModelScope.launch {
            repository.clearPurchasedMarketProducts()
        }
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

        startDiceRoll(
            filter = state.diceFilter,
            audience = state.diceAudienceFilter,
            recoveryMealType = state.diceFilter
        )
    }

    fun dismissDiceEmptyRecovery() {
        if (_uiState.value.diceEmptyRecovery != null) {
            analytics.trackDiceEmptyRecovery(DICE_EMPTY_RECOVERY_CHANGE_FILTERS)
        }
        _uiState.update { it.copy(diceEmptyRecovery = null) }
    }

    fun rollDiceAcrossMealTypesForSelectedAudience() {
        val recovery = _uiState.value.diceEmptyRecovery ?: return
        if (_uiState.value.isRolling) return
        analytics.trackDiceEmptyRecovery(DICE_EMPTY_RECOVERY_BROADEN_MEAL_TYPE)
        _uiState.update { it.copy(diceEmptyRecovery = null) }
        startDiceRoll(filter = null, audience = recovery.audience, recoveryMealType = null)
    }

    fun generateAiFromDiceEmptyRecovery() {
        val recovery = _uiState.value.diceEmptyRecovery ?: return
        analytics.trackDiceEmptyRecovery(DICE_EMPTY_RECOVERY_GENERATE_AI)
        _uiState.update {
            it.copy(
                diceEmptyRecovery = null,
                homeMenuMode = HomeMenuMode.Ai,
                formMealType = recovery.mealType,
                formAudience = recovery.audience
            )
        }
        generateMenuIdea()
    }

    private fun startDiceRoll(
        filter: MealType?,
        audience: MenuAudience,
        recoveryMealType: MealType?
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRolling = true,
                    result = null,
                    message = null,
                    isAiRetryNoticeVisible = false,
                    diceEmptyRecovery = null
                )
            }
            delay(DICE_ROLL_DURATION_MILLIS)
            val today = todayProvider()
            val currentState = _uiState.value
            if (audience !in currentState.enabledAudiences) {
                _uiState.update {
                    it.copy(
                        isRolling = false,
                        result = null,
                        diceEmptyRecovery = null
                    )
                }
                return@launch
            }
            val visibleMenus = menuVisibleMenus(currentState.menus, currentState.enabledAudiences)
            val hasCandidates = DiceSelector.hasCandidates(
                visibleMenus,
                filter,
                audience
            )
            val availableCandidateCountBeforeReset = DiceSelector.availableCandidateCount(
                menus = visibleMenus,
                filter = filter,
                audienceFilter = audience,
                today = today
            )
            var selected = DiceSelector.select(
                menus = visibleMenus,
                filter = filter,
                audienceFilter = audience,
                today = today
            )
            if (selected == null && hasCandidates) {
                val resetMenus = currentState.menus.map { menu ->
                    if (menu.matchesDiceFilters(filter, audience)) {
                        menu.copy(lastPickedDate = null)
                    } else {
                        menu
                    }
                }
                resetMenus
                    .filter { it.matchesDiceFilters(filter, audience) }
                    .forEach { repository.save(it) }
                selected = DiceSelector.select(
                    menus = resetMenus,
                    filter = filter,
                    audienceFilter = audience,
                    today = today
                )
            }
            if (selected != null) {
                repository.save(selected.copy(lastPickedDate = today))
            }
            analytics.trackDiceRolled(
                filter = filter,
                resultMealType = selected?.mealType,
                menuCount = visibleMenus.size,
                availableCandidateCount = availableCandidateCountBeforeReset
            )
            if (selected == null && !hasCandidates) {
                analytics.trackDiceEmptyResult(
                    filter = filter,
                    availableCandidateCount = availableCandidateCountBeforeReset
                )
            }
            val recovery = if (selected == null && !hasCandidates && recoveryMealType != null) {
                DiceEmptyRecovery(
                    mealType = recoveryMealType,
                    audience = audience,
                    canBroadenMealType = visibleMenus.any { menu ->
                        menu.audience == audience && menu.mealType != recoveryMealType
                    }
                ).also {
                    analytics.trackDiceEmptyRecovery(DICE_EMPTY_RECOVERY_SHOWN)
                }
            } else {
                null
            }
            _uiState.update {
                it.copy(
                    isRolling = false,
                    result = selected,
                    message = null,
                    isAiRetryNoticeVisible = false,
                    diceEmptyRecovery = recovery
                )
            }
        }
    }

    fun saveMenu() {
        saveMenu(ignoreGuestMenuSaveLimit = false)
    }

    private fun saveMenu(ignoreGuestMenuSaveLimit: Boolean) {
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

        if (!ignoreGuestMenuSaveLimit && !canGuestSaveMenuOrShowNotice()) {
            return
        }

        val hiveContribution = if (
            state.generatedOrigin == GeneratedMenuOrigin.LIVE_AI &&
            state.generatedDeduplicationKey != null &&
            state.generatedHealthAnalysis != null &&
            state.calories != null
        ) {
            AiMenuHiveContribution(
                language = AppLanguage.fromLocale(),
                mealType = mealType,
                audience = audience,
                profile = dietaryProfileStore.getProfile(audience),
                generatedMenu = GeneratedMenu(
                    name = name,
                    description = description,
                    notes = state.notes.trim(),
                    calories = state.calories,
                    healthAnalysis = state.generatedHealthAnalysis,
                    shoppingProducts = state.generatedShoppingProducts,
                    deduplicationKey = state.generatedDeduplicationKey
                ),
                cuisineInspiration = state.generatedCuisineInspiration
            )
        } else {
            null
        }

        viewModelScope.launch {
            val menu = FoodMenu(
                name = name,
                mealType = mealType,
                audience = audience,
                description = description,
                notes = state.notes.trim(),
                healthAnalysis = state.generatedHealthAnalysis,
                calories = state.calories,
                cuisineInspiration = state.generatedCuisineInspiration,
                shoppingProducts = state.generatedShoppingProducts,
                activeShoppingProductKeys = if (state.addGeneratedMenuToMarketList) {
                    state.generatedShoppingProducts.mapTo(linkedSetOf()) { it.key }
                } else {
                    emptySet()
                }
            )

            repository.save(menu)
            consumeGuestMenuSave()
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
            _uiState.update {
                it.copy(menuSaveSuccessRevision = it.menuSaveSuccessRevision + 1L)
            }
            hasTrackedMenuFormStarted = false
            hiveContribution?.let { contribution ->
                viewModelScope.launch {
                    aiMenuHive.contribute(contribution)
                }
            }
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
        if (audience !in state.enabledAudiences) {
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
                audience = audience,
                description = description,
                notes = notes,
                healthAnalysis = if (changedExistingMenu) null else existingMenu.healthAnalysis,
                calories = if (changedExistingMenu) null else existingMenu.calories,
                shoppingProducts = if (changedExistingMenu) emptyList() else existingMenu.shoppingProducts,
                activeShoppingProductKeys = if (changedExistingMenu) {
                    emptySet()
                } else {
                    existingMenu.activeShoppingProductKeys
                },
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
        val request = validatedGenerationRequestOrNull() ?: return
        startValidatedGenerationRequest(request)
    }

    private fun startValidatedGenerationRequest(
        request: ValidatedGenerationRequest,
        minimumPresentationMillis: Long = 0L
    ) {
        when (currentGenerationAccess()) {
            AiGenerationAccess.FREE -> {
                startGeneratedMenuRequest(request, minimumPresentationMillis)
            }
            AiGenerationAccess.REWARDED_CREDIT -> {
                if (consumeRewardedGenerationCredit()) {
                    analytics.trackAiRewardedOffer(
                        status = AI_REWARDED_STATUS_GENERATION_STARTED,
                        creditsRemaining = rewardedCreditsRemainingToday()
                    )
                    startGeneratedMenuRequest(request, minimumPresentationMillis)
                }
            }
            AiGenerationAccess.REWARDED_OFFER -> showRewardedGenerationOffer()
            AiGenerationAccess.HARD_LIMIT ->
                showAiHardLimitNotice(AI_SOURCE_GENERATE_MENU, request)
        }
    }

    fun requestRewardedGeneration(): Boolean {
        val request = validatedGenerationRequestOrNull()
        if (request == null || currentGenerationAccess() != AiGenerationAccess.REWARDED_OFFER) {
            refreshAiUsageCounters()
            return false
        }
        pendingRewardedGenerationRequest = request
        _uiState.update {
            it.copy(
                isRewardedGenerationPending = true,
                message = null,
                isAiRetryNoticeVisible = false
            )
        }
        return true
    }

    fun onRewardedGenerationEarned() {
        val request = pendingRewardedGenerationRequest
            ?.takeIf { _uiState.value.isRewardedGenerationPending }
            ?: return
        pendingRewardedGenerationRequest = null
        _uiState.update { it.copy(isRewardedGenerationPending = false) }
        val dateKey = currentPacificDateKey()
        val ledger = rewardedAiCreditStore.getLedger(activeAiUsageScope, dateKey)
        if (ledger.earnedCount >= MAX_REWARDED_AI_CREDITS_PER_DAY) {
            showAiHardLimitNotice(AI_SOURCE_GENERATE_MENU, request)
            return
        }
        rewardedAiCreditStore.saveLedger(
            activeAiUsageScope,
            ledger.copy(earnedCount = ledger.earnedCount + 1)
        )
        analytics.trackAiRewardedOffer(
            status = AI_REWARDED_STATUS_EARNED,
            creditsRemaining = rewardedCreditsRemainingToday()
        )
        refreshAiUsageCounters()
        startValidatedGenerationRequest(
            request = request,
            minimumPresentationMillis = REWARDED_AI_DICE_MINIMUM_PRESENTATION_MILLIS
        )
    }

    fun onRewardedGenerationDismissed() {
        if (!_uiState.value.isRewardedGenerationPending) {
            return
        }
        pendingRewardedGenerationRequest = null
        _uiState.update {
            it.copy(
                isRewardedGenerationPending = false,
                message = currentLanguage().rewardedAdDismissedMessage(),
                isAiRetryNoticeVisible = false
            )
        }
        analytics.trackAiRewardedOffer(
            status = AI_REWARDED_STATUS_DISMISSED,
            creditsRemaining = rewardedCreditsRemainingToday()
        )
    }

    fun onRewardedGenerationUnavailable() {
        if (!_uiState.value.isRewardedGenerationPending) {
            return
        }
        pendingRewardedGenerationRequest = null
        _uiState.update {
            it.copy(
                isRewardedGenerationPending = false,
                message = currentLanguage().rewardedAdUnavailableMessage(),
                isAiRetryNoticeVisible = false
            )
        }
        analytics.trackAiRewardedOffer(
            status = AI_REWARDED_STATUS_UNAVAILABLE,
            creditsRemaining = rewardedCreditsRemainingToday()
        )
    }

    private fun validatedGenerationRequestOrNull(): ValidatedGenerationRequest? {
        val state = _uiState.value
        if (state.isGeneratingMenu || state.isRewardedGenerationPending) {
            return null
        }
        val mealType = state.formMealType
        val audience = state.formAudience
        if (mealType == null) {
            _uiState.update { it.copy(message = currentLanguage().mealTypeRequiredMessage(), isAiRetryNoticeVisible = false) }
            return null
        }
        if (audience == null) {
            _uiState.update { it.copy(message = currentLanguage().audienceRequiredMessage(), isAiRetryNoticeVisible = false) }
            return null
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
            return null
        }
        val shouldCallProvider = currentProviderAiUsedCount(currentPacificDateKey()) <
            AI_PROVIDER_DAILY_HARD_LIMIT
        if (shouldCallProvider) {
            val activeRetryAtMillis = activeAiRetryAtMillis()
            if (activeRetryAtMillis != null) {
                showActiveAiRetryNotice(activeRetryAtMillis)
                return null
            }
            val activeRequestThrottleAtMillis = activeAiRequestThrottleAtMillis()
            if (activeRequestThrottleAtMillis != null) {
                showAiRequestThrottleNotice(activeRequestThrottleAtMillis)
                return null
            }
        }
        return ValidatedGenerationRequest(state, mealType, audience, profile)
    }

    private fun startGeneratedMenuRequest(
        request: ValidatedGenerationRequest,
        minimumPresentationMillis: Long
    ) {
        val state = request.state
        val mealType = request.mealType
        val audience = request.audience
        val profile = request.profile
        val hasMinimumPresentation = minimumPresentationMillis > 0L
        val avoidIdeas = state.buildAvoidIdeas(mealType, audience)
        val cuisineInspiration = cuisineRotation.current(mealType, audience)
        analytics.trackAiMenuGenerationStarted(mealType, avoidIdeas.size)
        val shouldCallProvider = currentProviderAiUsedCount(currentPacificDateKey()) <
            AI_PROVIDER_DAILY_HARD_LIMIT
        if (shouldCallProvider) {
            startAiRequestThrottle()
        }
        consumeScopedAiUse()
        if (shouldCallProvider) {
            consumeProviderAiRequest()
        }
        _uiState.update {
            it.copy(
                aiGenerationPhase = AiGenerationPhase.GENERATING,
                isRewardedMenuRevealPending = hasMinimumPresentation,
                message = null,
                isAiRetryNoticeVisible = false
            )
        }

        viewModelScope.launch {
            val minimumPresentationJob = if (hasMinimumPresentation) {
                launch { delay(minimumPresentationMillis) }
            } else {
                null
            }
            val slowPhaseJob = launch {
                delay(AI_GENERATION_SLOW_NOTICE_MILLIS)
                _uiState.update { current ->
                    if (current.aiGenerationPhase == AiGenerationPhase.GENERATING) {
                        current.copy(aiGenerationPhase = AiGenerationPhase.GENERATING_SLOW)
                    } else {
                        current
                    }
                }
            }
            try {
                if (!shouldCallProvider) {
                    searchHiveFallback(
                        request = request,
                        triggerFailureType = AI_FAILURE_QUOTA_DAILY,
                        failureNotice = null
                    )
                    return@launch
                }
                val generatedResult = withAiRequestTimeout {
                    repository.generateMenu(
                        mealType = mealType,
                        avoidIdeas = avoidIdeas,
                        dietaryProfile = profile,
                        audience = audience,
                        baseIngredients = state.aiBaseIngredients.trim(),
                        language = AppLanguage.fromLocale(),
                        cuisineInspiration = cuisineInspiration
                    )
                }
                if (generatedResult.isSuccess) {
                    val generated = generatedResult.getOrThrow()
                    aiQuotaRetryStore.clearRetryState()
                    cuisineRotation.advance(mealType, audience)
                    rememberGeneratedIdea(mealType, audience, generated.name, generated.description)
                    val identity = AiMenuHiveIdentity.from(
                        AppLanguage.fromLocale(),
                        generated.deduplicationKey
                    )

                    _uiState.update {
                        if (audience !in it.enabledAudiences || it.formAudience != audience) {
                            it
                        } else {
                            it.copy(
                                name = generated.name,
                                description = generated.description,
                                notes = generated.notes,
                                calories = generated.calories,
                                generatedHealthAnalysis = generated.healthAnalysis,
                                generatedShoppingProducts = generated.shoppingProducts,
                                addGeneratedMenuToMarketList = true,
                                generatedCuisineInspiration = cuisineInspiration,
                                generatedDeduplicationKey = generated.deduplicationKey,
                                generatedOrigin = GeneratedMenuOrigin.LIVE_AI,
                                generatedSemanticHash = identity?.semanticHash,
                                isAiRetryNoticeVisible = false,
                                showGeneratedMenuDetail = true
                            )
                        }
                    }

                    runCatching {
                        analytics.trackAiMenuGenerationFinished(
                            mealType = mealType,
                            success = true,
                            healthStatus = generated.healthAnalysis?.status,
                            failureType = null
                        )
                    }
                } else {
                    val error = requireNotNull(generatedResult.exceptionOrNull())
                    searchHiveFallback(
                        request = request,
                        triggerFailureType = error.analyticsFailureType(),
                        failureNotice = error.toAiFailureNotice(
                            clockMillisProvider(),
                            currentLanguage()
                        )
                    )
                }
            } finally {
                slowPhaseJob.cancel()
                minimumPresentationJob?.join()
                _uiState.update {
                    it.copy(
                        aiGenerationPhase = AiGenerationPhase.IDLE,
                        isRewardedMenuRevealPending = false
                    )
                }
            }
        }
    }

    private suspend fun searchHiveFallback(
        request: ValidatedGenerationRequest,
        triggerFailureType: String,
        failureNotice: AiFailureNotice?
    ) {
        val preparedFailureNotice = failureNotice?.prepareAiFailureNotice()
        preparedFailureNotice?.recordAiFailurePause()
        _uiState.update { it.copy(aiGenerationPhase = AiGenerationPhase.SEARCHING_HIVE) }
        val hiveStartedAtMillis = clockMillisProvider()
        val rotationScope = activeAiUsageScope
        val rotation = runCatching { hiveRotationStore.snapshot(rotationScope) }
            .getOrDefault(HiveRotationSnapshot())
        val hiveResult = aiMenuHive.findCompatibleMenu(
            AiMenuHiveSearchRequest(
                language = AppLanguage.fromLocale(),
                mealType = request.mealType,
                audience = request.audience,
                profile = request.profile,
                baseIngredients = request.state.aiBaseIngredients.trim(),
                recentSemanticHashes = rotation.seenHashes.toSet(),
                lastShownHash = rotation.lastShownHash
            )
        )
        val fallback = hiveResult.getOrNull()
        if (fallback == null) {
            val reason = preparedFailureNotice?.generationReason
                ?: AiGenerationFailureReason.DAILY_LIMIT
            val contextualMessage = contextualAiGenerationFailureMessage(
                language = currentLanguage(),
                reason = reason,
                mealType = request.mealType,
                audience = request.audience,
                profile = request.profile
            )
            if (preparedFailureNotice != null) {
                showPreparedAiFailureNotice(
                    preparedFailureNotice.copy(message = contextualMessage)
                )
            } else {
                _uiState.update {
                    it.copy(
                        message = contextualMessage,
                        isAiRetryNoticeVisible = false
                    )
                }
            }
        }
        fallback?.let { candidate ->
            _uiState.update {
                it.copy(
                    name = candidate.generatedMenu.name,
                    description = candidate.generatedMenu.description,
                    notes = candidate.generatedMenu.notes,
                    calories = candidate.generatedMenu.calories,
                    generatedHealthAnalysis = candidate.generatedMenu.healthAnalysis,
                    generatedShoppingProducts = candidate.generatedMenu.shoppingProducts,
                    addGeneratedMenuToMarketList = true,
                    generatedCuisineInspiration = candidate.cuisineInspiration,
                    generatedDeduplicationKey = candidate.generatedMenu.deduplicationKey,
                    generatedOrigin = GeneratedMenuOrigin.HIVE_FALLBACK,
                    generatedSemanticHash = candidate.semanticHash,
                    message = null,
                    isAiRetryNoticeVisible = false,
                    showGeneratedMenuDetail = true
                )
            }
            runCatching {
                hiveRotationStore.recordShown(
                    scope = rotationScope,
                    semanticHash = candidate.semanticHash,
                    startsNewCycle = candidate.startsNewRotationCycle
                )
            }
        }
        runCatching {
            analytics.trackAiMenuHiveFallback(
                mealType = request.mealType,
                result = when {
                    hiveResult.isFailure -> HIVE_RESULT_ERROR
                    fallback?.source == AiMenuHiveLookupSource.CACHE -> HIVE_RESULT_CACHE_HIT
                    fallback != null -> HIVE_RESULT_HIT
                    else -> HIVE_RESULT_MISS
                },
                triggerFailureType = triggerFailureType,
                durationMillis = (clockMillisProvider() - hiveStartedAtMillis).coerceAtLeast(0L)
            )
        }
        runCatching {
            analytics.trackAiMenuGenerationFinished(
                mealType = request.mealType,
                success = false,
                healthStatus = null,
                failureType = triggerFailureType
            )
        }
    }

    private fun currentAiUsagePolicy(): AiDailyUsagePolicy {
        return AiDailyUsagePolicy(
            isGuest = guestAccessPolicy.isGuest,
            guestAiLimitsEnabled = areGuestAiLimitsEnabled
        )
    }

    private fun currentRewardedLedger(): RewardedAiCreditLedger {
        val dateKey = currentPacificDateKey()
        return rewardedAiCreditStore.getLedger(activeAiUsageScope, dateKey)
    }

    private fun currentGenerationAccess(): AiGenerationAccess {
        val dateKey = currentPacificDateKey()
        val ledger = currentRewardedLedger()
        val scopedAccess = currentAiUsagePolicy().generationAccess(
            usedCount = currentScopedAiUsedCount(dateKey),
            earnedRewardedCredits = ledger.earnedCount,
            consumedRewardedCredits = ledger.consumedCount
        )
        return if (
            scopedAccess == AiGenerationAccess.REWARDED_OFFER &&
            currentProviderAiUsedCount(dateKey) >= AI_PROVIDER_DAILY_HARD_LIMIT
        ) {
            AiGenerationAccess.HARD_LIMIT
        } else {
            scopedAccess
        }
    }

    private fun consumeRewardedGenerationCredit(): Boolean {
        val dateKey = currentPacificDateKey()
        val ledger = rewardedAiCreditStore.getLedger(activeAiUsageScope, dateKey)
        if (ledger.consumedCount >= ledger.earnedCount) {
            refreshAiUsageCounters()
            return false
        }
        rewardedAiCreditStore.saveLedger(
            activeAiUsageScope,
            ledger.copy(consumedCount = ledger.consumedCount + 1)
        )
        refreshAiUsageCounters()
        return true
    }

    private fun rewardedCreditsRemainingToday(): Int {
        return (MAX_REWARDED_AI_CREDITS_PER_DAY - currentRewardedLedger().earnedCount)
            .coerceIn(0, MAX_REWARDED_AI_CREDITS_PER_DAY)
    }

    private fun showRewardedGenerationOffer() {
        refreshAiUsageCounters()
        _uiState.update {
            it.copy(
                aiGenerationLimitState = AiGenerationLimitState.REWARDED_OFFER,
                message = null,
                aiRetryAtMillis = null,
                isAiRequestThrottlePause = false,
                isAiRetryNoticeVisible = false
            )
        }
    }

    private fun canUseAiAnalysisOrShowNotice(source: String): Boolean {
        val dateKey = currentPacificDateKey()
        val scopedUsedCount = currentScopedAiUsedCount(dateKey)
        val providerUsedCount = currentProviderAiUsedCount(dateKey)
        if (
            currentAiUsagePolicy().canUseAiAnalysis(scopedUsedCount) &&
            providerUsedCount < AI_PROVIDER_DAILY_HARD_LIMIT
        ) {
            return true
        }
        if (providerUsedCount >= AI_PROVIDER_DAILY_HARD_LIMIT) {
            refreshAiUsageCounters()
            _uiState.update {
                it.copy(
                    message = currentLanguage().aiLocalDailyLimitMessage(),
                    aiAnalysisUsesRemainingToday = 0,
                    isAiRetryNoticeVisible = false
                )
            }
            analytics.trackAiDailyLimitReached(source)
        } else {
            refreshAiUsageCounters()
            _uiState.update {
                it.copy(
                    message = currentLanguage().aiLocalDailyLimitMessage(),
                    isAiRetryNoticeVisible = false
                )
            }
            analytics.trackAiDailyLimitReached(source)
        }
        return false
    }

    private fun showAiHardLimitNotice(
        source: String,
        request: ValidatedGenerationRequest? = null
    ) {
        val retryAtMillis = nextPacificMidnightMillis(clockMillisProvider())
        val message = request?.let {
            contextualAiGenerationFailureMessage(
                language = currentLanguage(),
                reason = AiGenerationFailureReason.DAILY_LIMIT,
                mealType = it.mealType,
                audience = it.audience,
                profile = it.profile
            )
        } ?: currentLanguage().aiLocalDailyLimitMessage()
        pendingRewardedGenerationRequest = null
        _uiState.update {
            it.copy(
                message = message,
                aiRetryAtMillis = retryAtMillis,
                isAiRequestThrottlePause = false,
                isAiRetryNoticeVisible = false,
                aiUsesRemainingToday = 0,
                aiGenerationUsesRemainingToday = 0,
                aiAnalysisUsesRemainingToday = 0,
                aiGenerationLimitState = AiGenerationLimitState.HARD_LIMIT,
                isRewardedGenerationPending = false
            )
        }
        scheduleAiRetryRefresh(retryAtMillis)
        analytics.trackAiDailyLimitReached(source)
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

    internal fun refreshDietaryProfile() {
        val enabledAudiences = loadEnabledAudiences()
        _uiState.update {
            it.copy(
                dietaryProfile = dietaryProfileStore.getProfile(it.dietaryProfileAudience),
                audienceAgeRanges = loadAudienceAgeRanges()
            ).withAudienceVisibilityState(
                enabledAudiences = enabledAudiences,
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
                audienceAgeRanges = loadAudienceAgeRanges()
            ).withAudienceVisibilityState(
                enabledAudiences = enabledAudiences,
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

    private fun MenuDadoUiState.withAudienceVisibilityState(
        enabledAudiences: List<MenuAudience>,
        formAudience: MenuAudience?,
        diceAudienceFilter: MenuAudience?
    ): MenuDadoUiState {
        val didFormAudienceChange = this.formAudience != formAudience
        val updated = copy(
            enabledAudiences = enabledAudiences,
            formAudience = formAudience,
            diceAudienceFilter = diceAudienceFilter,
            result = result?.takeIf { it.audience in enabledAudiences },
            diceEmptyRecovery = diceEmptyRecovery?.takeIf { it.audience in enabledAudiences }
        )
        return if (didFormAudienceChange) updated.withoutMenuFormDraft() else updated
    }

    private fun HealthAnalysis?.manualEditNotice(language: AppLanguage): String? {
        return if (this == null) null else language.generatedAnalysisManualEditMessage()
    }

    fun analyzeExisting(menu: FoodMenu) {
        val state = _uiState.value
        if (state.isAnalyzing || menu.audience !in state.enabledAudiences) {
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
        if (!canUseAiAnalysisOrShowNotice(AI_SOURCE_ANALYZE_SINGLE)) {
            return
        }
        analytics.trackAiAnalysisStarted(AI_SCOPE_SINGLE, menu.mealType, menuCount = 1)
        startAiRequestThrottle()
        consumeScopedAiUse()
        consumeProviderAiRequest()
        _uiState.update {
            it.copy(
                isAnalyzing = true,
                message = null,
                isAiRetryNoticeVisible = false
            )
        }

        viewModelScope.launch {
            withAiRequestTimeout { repository.analyze(menu, AppLanguage.fromLocale()) }
                .onSuccess { details ->
                    val analysis = details.healthAnalysis
                    repository.save(
                        menu.copy(
                            healthAnalysis = analysis,
                            calories = analysis.calories ?: menu.calories,
                            shoppingProducts = details.shoppingProducts,
                            activeShoppingProductKeys = emptySet()
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
        val pendingMenus = menuVisibleMenus(state.menus, state.enabledAudiences)
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
        if (!canUseAiAnalysisOrShowNotice(AI_SOURCE_ANALYZE_BATCH)) {
            return
        }
        analytics.trackAiAnalysisStarted(AI_SCOPE_BATCH, mealType = null, menuCount = pendingMenus.size)
        startAiRequestThrottle()
        consumeScopedAiUse()
        consumeProviderAiRequest()
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
                        analysesByMenuId[menu.id]?.let { details ->
                            val analysis = details.healthAnalysis
                            repository.save(
                                menu.copy(
                                    healthAnalysis = analysis,
                                    calories = analysis.calories ?: menu.calories,
                                    shoppingProducts = details.shoppingProducts,
                                    activeShoppingProductKeys = emptySet()
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

    fun toggleFavorite(menu: FoodMenu) {
        viewModelScope.launch {
            val isFavorite = !menu.isFavorite
            repository.save(
                menu.copy(
                    isFavorite = isFavorite,
                    favoritedAt = if (isFavorite) clockMillisProvider() else null
                )
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
            generatedShoppingProducts = emptyList(),
            addGeneratedMenuToMarketList = true,
            generatedCuisineInspiration = null,
            generatedDeduplicationKey = null,
            generatedOrigin = null,
            generatedSemanticHash = null,
            message = null,
            isAiRetryNoticeVisible = false,
            showGeneratedMenuDetail = false
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
                generatedShoppingProducts = emptyList(),
                addGeneratedMenuToMarketList = true,
                generatedCuisineInspiration = null,
                generatedDeduplicationKey = null,
                generatedOrigin = null,
                generatedSemanticHash = null,
                showGeneratedMenuDetail = false,
                formMealType = suggestedMealTypeForDeviceTime(clockMillisProvider()),
                formAudience = null.selectedOrSingleDefault(loadEnabledAudiences())
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
        refreshAiUsageCounters()
    }

    private fun refreshAiUsageCounters() {
        val freeUsesRemaining = aiDailyUsesRemaining()
        val providerAvailable = currentProviderAiUsedCount(currentPacificDateKey()) <
            AI_PROVIDER_DAILY_HARD_LIMIT
        val generationLimitState = when (currentGenerationAccess()) {
            AiGenerationAccess.FREE,
            AiGenerationAccess.REWARDED_CREDIT -> AiGenerationLimitState.AVAILABLE
            AiGenerationAccess.REWARDED_OFFER -> AiGenerationLimitState.REWARDED_OFFER
            AiGenerationAccess.HARD_LIMIT -> AiGenerationLimitState.HARD_LIMIT
        }
        _uiState.update {
            it.copy(
                aiUsesRemainingToday = freeUsesRemaining,
                aiGenerationUsesRemainingToday = freeUsesRemaining,
                aiAnalysisUsesRemainingToday = if (providerAvailable) freeUsesRemaining else 0,
                isAiProviderAvailableToday = providerAvailable,
                aiGenerationLimitState = generationLimitState,
                rewardedCreditsRemainingToday = rewardedCreditsRemainingToday()
            )
        }
    }

    private fun canGuestSaveMenuOrShowNotice(): Boolean {
        val dateKey = todayProvider()
        val usedCount = currentGuestUsageState(dateKey).savedMenuCount
        if (guestAccessPolicy.canUseAction(usedCount, GUEST_DAILY_MENU_SAVE_LIMIT)) {
            return true
        }
        analytics.trackGuestLimitReached(GUEST_LIMIT_MENU_SAVE, usedCount)
        _uiState.update {
            it.copy(
                message = currentLanguage().guestMenuSaveLimitMessage(),
                isAiRetryNoticeVisible = false
            )
        }
        return false
    }

    private fun consumeGuestMenuSave() {
        if (!guestAccessPolicy.isGuest || !guestAccessPolicy.areLimitsEnabled) {
            return
        }
        saveGuestUsageState { state ->
            state.copy(savedMenuCount = (state.savedMenuCount + 1).coerceAtMost(GUEST_DAILY_MENU_SAVE_LIMIT))
        }
    }

    private fun saveGuestUsageState(transform: (GuestDailyUsageState) -> GuestDailyUsageState) {
        val dateKey = todayProvider()
        guestUsageStore.saveUsageState(transform(currentGuestUsageState(dateKey)))
    }

    private fun currentGuestUsageState(dateKey: String): GuestDailyUsageState {
        val state = guestUsageStore.getUsageState()
        if (state?.dateKey == dateKey) {
            return GuestDailyUsageState(
                dateKey = state.dateKey,
                savedMenuCount = state.savedMenuCount.coerceIn(0, GUEST_DAILY_MENU_SAVE_LIMIT),
                generatedIdeaCount = state.generatedIdeaCount.coerceIn(0, GUEST_DAILY_AI_GENERATION_LIMIT),
                analysisCount = state.analysisCount.coerceIn(0, GUEST_DAILY_AI_ANALYSIS_LIMIT)
            )
        }
        return GuestDailyUsageState(
            dateKey = dateKey,
            savedMenuCount = 0,
            generatedIdeaCount = 0,
            analysisCount = 0
        )
    }

    private fun currentAuthMode(): String {
        return if (guestAccessPolicy.isGuest) AUTH_MODE_GUEST else AUTH_MODE_SIGNED_IN
    }

    private fun currentPacificDateKey(): String {
        return currentPacificDateKey(clockMillisProvider())
    }

    private fun migrateLegacyAiUsageIfNeeded() {
        val legacyState = aiDailyUsageStore.getUsageState()
        scopedAiUsageStore.migrateLegacyUsage(
            scope = activeAiUsageScope,
            legacyState = legacyState
        )
    }

    private fun consumeScopedAiUse() {
        val dateKey = currentPacificDateKey()
        val usedCount = currentScopedAiUsedCount(dateKey)
        val newUsedCount = (usedCount + 1).coerceAtMost(AI_PROVIDER_DAILY_HARD_LIMIT)

        val updatedState = AiDailyUsageState(
            dateKey = dateKey,
            usedCount = newUsedCount
        )
        scopedAiUsageStore.saveUsageState(activeAiUsageScope, updatedState)
        if (activeAiUsageScope != GUEST_AI_USAGE_SCOPE) {
            aiDailyUsageStore.saveUsageState(updatedState)
        }

        refreshAiUsageCounters()
    }

    private fun consumeProviderAiRequest() {
        val dateKey = currentPacificDateKey()
        val usedCount = currentProviderAiUsedCount(dateKey)
        scopedAiUsageStore.saveUsageState(
            PROVIDER_AI_USAGE_SCOPE,
            AiDailyUsageState(
                dateKey = dateKey,
                usedCount = (usedCount + 1).coerceAtMost(AI_PROVIDER_DAILY_HARD_LIMIT)
            )
        )
        refreshAiUsageCounters()
    }

    private fun aiDailyUsesRemaining(): Int {
        val freeLimit = if (guestAccessPolicy.isGuest && areGuestAiLimitsEnabled) {
            GUEST_DAILY_AI_FREE_LIMIT
        } else {
            SIGNED_IN_DAILY_AI_FREE_LIMIT
        }
        return (freeLimit - currentScopedAiUsedCount(currentPacificDateKey())).coerceIn(
            0,
            freeLimit
        )
    }

    private fun currentScopedAiUsedCount(dateKey: String): Int {
        return scopedAiUsageStore.getUsageState(activeAiUsageScope, dateKey).usedCount
            .coerceIn(0, AI_PROVIDER_DAILY_HARD_LIMIT)
    }

    private fun currentProviderAiUsedCount(dateKey: String): Int {
        return scopedAiUsageStore.getUsageState(PROVIDER_AI_USAGE_SCOPE, dateKey).usedCount
            .coerceIn(0, AI_PROVIDER_DAILY_HARD_LIMIT)
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
                isAiRetryNoticeVisible = false
            )
        }
        refreshAiUsageCounters()
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
                    isAiRetryNoticeVisible = false
                )
            }
        }
        refreshAiUsageCounters()
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
                message = null,
                aiRetryAtMillis = retryAtMillis,
                isAiRequestThrottlePause = true,
                isAiRetryNoticeVisible = false
            )
        }
        scheduleAiRetryRefresh(retryAtMillis)
    }

    private fun AiFailureNotice.prepareAiFailureNotice(): AiFailureNotice {
        return copy(retryAtMillis = retryAtMillis?.withQuotaBackoff())
    }

    private fun AiFailureNotice.recordAiFailurePause() {
        val retryAtMillis = retryAtMillis ?: return
        _uiState.update {
            it.copy(
                message = null,
                aiRetryAtMillis = retryAtMillis,
                isAiRequestThrottlePause = false,
                isAiRetryNoticeVisible = false
            )
        }
        scheduleAiRetryRefresh(retryAtMillis)
    }

    private fun showAiFailureNotice(notice: AiFailureNotice) {
        showPreparedAiFailureNotice(notice.prepareAiFailureNotice())
    }

    private fun showPreparedAiFailureNotice(notice: AiFailureNotice) {
        val retryAtMillis = notice.retryAtMillis
        val refreshAtMillis = retryAtMillis ?: _uiState.value.aiRetryAtMillis
            ?.takeIf { _uiState.value.isAiRequestThrottlePause }
        _uiState.update { current ->
            current.copy(
                message = notice.message,
                aiRetryAtMillis = retryAtMillis ?: current.aiRetryAtMillis,
                isAiRequestThrottlePause = if (retryAtMillis != null) {
                    false
                } else {
                    current.isAiRequestThrottlePause
                },
                isAiRetryNoticeVisible =
                    retryAtMillis != null &&
                        notice.generationReason != AiGenerationFailureReason.DAILY_LIMIT
            )
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
    val retryAtMillis: Long? = null,
    val generationReason: AiGenerationFailureReason
)

private fun Throwable.toAiFailureNotice(nowMillis: Long, language: AppLanguage): AiFailureNotice {
    val text = listOfNotNull(message, cause?.message).joinToString(" ").lowercase()
    return when {
        isAiQuotaExceeded() -> {
            val quotaLimitType = classifyAiQuotaLimitType(text)
            AiFailureNotice(
                message = quotaLimitType.message(language),
                retryAtMillis = text.retryAtMillis(nowMillis) ?: nextPacificMidnightMillis(nowMillis),
                generationReason = if (quotaLimitType == AiQuotaLimitType.REQUESTS_PER_DAY) {
                    AiGenerationFailureReason.DAILY_LIMIT
                } else {
                    AiGenerationFailureReason.HIGH_DEMAND
                }
            )
        }
        this is ServiceDisabledException ||
            this is APINotConfiguredException ||
            "service_disabled" in text ||
            "api_key_service_blocked" in text -> AiFailureNotice(
                message = language.aiConfigurationMessage(),
                generationReason = AiGenerationFailureReason.SERVICE_UNAVAILABLE
            )
        this is InvalidAPIKeyException ||
            "api key not valid" in text -> AiFailureNotice(
                message = language.aiInvalidApiKeyMessage(),
                generationReason = AiGenerationFailureReason.SERVICE_UNAVAILABLE
            )
        this is RequestTimeoutException ||
            this is TimeoutCancellationException ||
            "timeout" in text ||
            "timed out" in text -> AiFailureNotice(
                message = language.aiTimeoutMessage(),
                generationReason = AiGenerationFailureReason.TIMEOUT
            )
        text.isAiProviderInternalFailure() -> AiFailureNotice(
            message = language.aiTemporaryServiceMessage(),
            generationReason = AiGenerationFailureReason.SERVICE_UNAVAILABLE
        )
        else -> AiFailureNotice(
            message = language.aiGenericFailureMessage(),
            generationReason = AiGenerationFailureReason.CONNECTION
        )
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
        text.isAiProviderInternalFailure() -> AI_FAILURE_TEMPORARY
        else -> AI_FAILURE_GENERIC
    }
}

private fun String.isAiProviderInternalFailure(): Boolean {
    val normalized = trim()
    return normalized == "internal" ||
        "internal error" in normalized ||
        "status{code=internal" in normalized
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
        AppLanguage.ENGLISH -> "AI is in high demand. Try again later."
        AppLanguage.FRENCH -> "L'IA est très demandée. Réessayez plus tard."
        AppLanguage.SPANISH -> "La IA está con mucha demanda. Inténtalo nuevamente más tarde."
    }
}

private fun AppLanguage.aiRequestsPerMinuteMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI is in high demand. Try again later."
        AppLanguage.FRENCH -> "L'IA est très demandée. Réessayez plus tard."
        AppLanguage.SPANISH -> "La IA está con mucha demanda. Inténtalo nuevamente más tarde."
    }
}

private fun AppLanguage.aiTokensPerMinuteMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI is in high demand. Try again later."
        AppLanguage.FRENCH -> "L'IA est très demandée. Réessayez plus tard."
        AppLanguage.SPANISH -> "La IA está con mucha demanda. Inténtalo nuevamente más tarde."
    }
}

private fun AppLanguage.aiRequestsPerDayMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI is in high demand. Try again later."
        AppLanguage.FRENCH -> "L'IA est très demandée. Réessayez plus tard."
        AppLanguage.SPANISH -> "La IA está con mucha demanda. Inténtalo nuevamente más tarde."
    }
}

private fun AppLanguage.aiLocalDailyLimitMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "Today's free AI uses in MenuDado have run out. Your menus are still available and you can try again later."
        AppLanguage.FRENCH -> "Les usages gratuits de l'IA dans MenuDado sont épuisés pour aujourd'hui. Vos menus restent disponibles et vous pourrez réessayer plus tard."
        AppLanguage.SPANISH -> "Has usado la IA gratuita de MenuDado por hoy. Tus menús siguen disponibles y podrás intentarlo más tarde."
    }
}

private fun AppLanguage.rewardedAdDismissedMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "The video was closed before the reward. You can try again when you are ready."
        AppLanguage.FRENCH -> "La vidéo a été fermée avant la récompense. Vous pouvez réessayer quand vous le souhaitez."
        AppLanguage.SPANISH -> "El vídeo se cerró antes de la recompensa. Puedes intentarlo de nuevo cuando quieras."
    }
}

private fun AppLanguage.rewardedAdUnavailableMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "The video is not available right now. Try again in a moment."
        AppLanguage.FRENCH -> "La vidéo n'est pas disponible pour le moment. Réessayez dans un instant."
        AppLanguage.SPANISH -> "El vídeo no está disponible ahora. Inténtalo de nuevo en un momento."
    }
}

private fun AppLanguage.guestMenuSaveLimitMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "You have saved your 5 guest menus for today. Create a free account to keep saving and recover your menus later."
        AppLanguage.FRENCH -> "Vous avez enregistré vos 5 menus invités aujourd'hui. Créez un compte gratuit pour continuer et retrouver vos menus plus tard."
        AppLanguage.SPANISH -> "Has guardado tus 5 menús de invitado por hoy. Crea una cuenta gratis para seguir guardando y recuperar tus menús después."
    }
}

private fun AppLanguage.guestAiGenerationLimitMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "You have used your 5 guest AI ideas for today. Create a free account to keep generating ideas."
        AppLanguage.FRENCH -> "Vous avez utilisé vos 5 idées IA invitées aujourd'hui. Créez un compte gratuit pour continuer à générer des idées."
        AppLanguage.SPANISH -> "Has usado tus 5 ideas con IA como invitado por hoy. Crea una cuenta gratis para seguir generando ideas."
    }
}

private fun AppLanguage.guestAiAnalysisLimitMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "You have used your 5 guest AI analyses for today. Create a free account to keep analyzing your menus."
        AppLanguage.FRENCH -> "Vous avez utilisé vos 5 analyses IA invitées aujourd'hui. Créez un compte gratuit pour continuer à analyser vos menus."
        AppLanguage.SPANISH -> "Has usado tus 5 análisis con IA como invitado por hoy. Crea una cuenta gratis para seguir analizando tus menús."
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
        AppLanguage.ENGLISH -> "AI help is not available right now. You can keep using your menus and try again later."
        AppLanguage.FRENCH -> "L'aide IA n'est pas disponible pour le moment. Vous pouvez continuer à utiliser vos menus et réessayer plus tard."
        AppLanguage.SPANISH -> "La ayuda con IA no está disponible en este momento. Puedes seguir usando tus menús e intentarlo más tarde."
    }
}

private fun AppLanguage.aiInvalidApiKeyMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI help could not start right now. You can keep using your menus and try again later."
        AppLanguage.FRENCH -> "L'aide IA n'a pas pu démarrer pour le moment. Vous pouvez continuer à utiliser vos menus et réessayer plus tard."
        AppLanguage.SPANISH -> "La ayuda con IA no pudo iniciar en este momento. Puedes seguir usando tus menús e intentarlo más tarde."
    }
}

private fun AppLanguage.aiTimeoutMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "AI took too long to respond. Check your connection and try again."
        AppLanguage.FRENCH -> "L'IA a mis trop de temps à répondre. Vérifiez votre connexion et réessayez."
        AppLanguage.SPANISH -> "La IA tardó demasiado en responder. Revisa la conexión e inténtalo de nuevo."
    }
}

private fun AppLanguage.aiTemporaryServiceMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "The service had a temporary issue. Try again later."
        AppLanguage.FRENCH -> "Le service a rencontré un problème temporaire. Réessayez plus tard."
        AppLanguage.SPANISH -> "El servicio tuvo un problema temporal. Inténtalo nuevamente más tarde."
    }
}

private fun AppLanguage.aiGenericFailureMessage(): String {
    return when (this) {
        AppLanguage.ENGLISH -> "Could not connect to AI. Check your internet connection and try again."
        AppLanguage.FRENCH -> "Impossible de se connecter à l'IA. Vérifiez votre connexion internet et réessayez."
        AppLanguage.SPANISH -> "No se pudo conectar con la IA. Revisa tu conexión a internet e inténtalo de nuevo."
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

private data class GeneratedIdeaMemory(
    val mealType: MealType,
    val audience: MenuAudience,
    val name: String,
    val description: String
)

private data class ValidatedGenerationRequest(
    val state: MenuDadoUiState,
    val mealType: MealType,
    val audience: MenuAudience,
    val profile: DietaryProfile
)

private const val RETRY_GRACE_SECONDS = 2L
private const val AI_BATCH_ANALYSIS_LIMIT = 5
private const val AI_SOURCE_GENERATE_MENU = "generate_menu"
private const val AI_SOURCE_ANALYZE_SINGLE = "analyze_single"
private const val AI_SOURCE_ANALYZE_BATCH = "analyze_batch"
private const val AI_REWARDED_STATUS_SHOWN = "shown"
private const val AI_REWARDED_STATUS_UNAVAILABLE = "unavailable"
private const val AI_REWARDED_STATUS_DISMISSED = "dismissed"
private const val AI_REWARDED_STATUS_EARNED = "earned"
private const val AI_REWARDED_STATUS_GENERATION_STARTED = "generation_started"
private const val AI_SCOPE_SINGLE = "single"
private const val AI_SCOPE_BATCH = "batch"
private const val AUTH_MODE_GUEST = "guest"
private const val AUTH_MODE_SIGNED_IN = "signed_in"
private const val GUEST_LIMIT_MENU_SAVE = "menu_save"
private const val GUEST_LIMIT_AI_GENERATION = "ai_generation"
private const val GUEST_LIMIT_AI_ANALYSIS = "ai_analysis"
private const val FORM_FIELD_NAME = "name"
private const val FORM_FIELD_DESCRIPTION = "description"
private const val FORM_FIELD_NOTES = "notes"
private const val ONBOARDING_ACTION_START = "start"
private const val ONBOARDING_ACTION_SKIP = "skip"
private const val CURRENT_ONBOARDING_VERSION = 5
private const val ANALYTICS_SOURCE_DICE = "dice"
private const val DICE_EMPTY_RECOVERY_SHOWN = "shown"
private const val DICE_EMPTY_RECOVERY_GENERATE_AI = "generate_ai"
private const val DICE_EMPTY_RECOVERY_BROADEN_MEAL_TYPE = "broaden_meal_type"
private const val DICE_EMPTY_RECOVERY_CHANGE_FILTERS = "change_filters"
internal const val DICE_ROLL_DURATION_MILLIS = 850L
internal const val DICE_ROLL_SPIN_DEGREES = 720f
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
private const val AI_FAILURE_TEMPORARY = "temporary"
private const val AI_FAILURE_GENERIC = "generic"
private const val FIRST_QUOTA_BACKOFF_MILLIS = 0L
private const val SECOND_QUOTA_BACKOFF_MILLIS = 2 * 60 * 1000L
private const val MAX_QUOTA_BACKOFF_MILLIS = 30 * 60 * 1000L
private const val AI_REQUEST_THROTTLE_MILLIS = 1 * 1000L
private const val AI_REQUEST_TIMEOUT_MILLIS = 45 * 1000L
private const val AI_GENERATION_SLOW_NOTICE_MILLIS = 12 * 1000L
internal const val REWARDED_AI_DICE_MINIMUM_PRESENTATION_MILLIS = 1_500L
private const val HIVE_RESULT_HIT = "hit"
private const val HIVE_RESULT_CACHE_HIT = "cache_hit"
private const val HIVE_RESULT_MISS = "miss"
private const val HIVE_RESULT_ERROR = "error"
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
