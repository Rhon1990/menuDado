package com.menudado.ui

import com.menudado.analytics.MenuDadoAnalytics
import com.menudado.analytics.DeviceInfo
import com.menudado.ai.HealthAnalyzer
import com.menudado.data.DietaryProfileStore
import com.menudado.data.AiDailyUsageState
import com.menudado.data.AiDailyUsageStore
import com.menudado.data.AiQuotaRetryStore
import com.menudado.data.AiQuotaRetryState
import com.menudado.data.AiRequestThrottleStore
import com.menudado.data.CuisineRotation
import com.menudado.data.CuisineRotationStateStore
import com.menudado.data.GuestDailyUsageState
import com.menudado.data.GuestUsageStore
import com.menudado.data.MenuDao
import com.menudado.data.MenuEntity
import com.menudado.data.MenuRepository
import com.menudado.data.OnboardingStore
import com.menudado.data.RemoteSyncState
import com.menudado.data.toEntity
import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import com.menudado.domain.MenuAiDetails
import com.menudado.domain.AppLanguage
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import com.menudado.domain.CuisineInspiration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MenuDadoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeMenuDao
    private lateinit var analyzer: RecordingHealthAnalyzer
    private lateinit var aiQuotaRetryStore: FakeAiQuotaRetryStore
    private lateinit var aiRequestThrottleStore: FakeAiRequestThrottleStore
    private lateinit var aiDailyUsageStore: FakeAiDailyUsageStore
    private lateinit var guestUsageStore: FakeGuestUsageStore
    private lateinit var dietaryProfileStore: FakeDietaryProfileStore
    private lateinit var onboardingStore: FakeOnboardingStore
    private lateinit var cuisineRotationStateStore: FakeCuisineRotationStateStore
    private lateinit var cuisineRotation: CuisineRotation
    private lateinit var analytics: RecordingMenuDadoAnalytics
    private lateinit var viewModel: MenuDadoViewModel
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale("es"))
        Dispatchers.setMain(dispatcher)
        dao = FakeMenuDao()
        analyzer = RecordingHealthAnalyzer()
        aiQuotaRetryStore = FakeAiQuotaRetryStore()
        aiRequestThrottleStore = FakeAiRequestThrottleStore()
        aiDailyUsageStore = FakeAiDailyUsageStore()
        guestUsageStore = FakeGuestUsageStore()
        dietaryProfileStore = FakeDietaryProfileStore()
        onboardingStore = FakeOnboardingStore(completed = true)
        cuisineRotationStateStore = FakeCuisineRotationStateStore()
        cuisineRotation = CuisineRotation(cuisineRotationStateStore) { 2 }
        analytics = RecordingMenuDadoAnalytics()
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            todayProvider = { "2026-06-11" },
            clockMillisProvider = { localMillisAtHour(8) },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore,
            guestUsageStore = guestUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore,
            cuisineRotation = cuisineRotation
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        viewModel.setDiceAudienceFilter(MenuAudience.ADULT)
        analytics.events.clear()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        Dispatchers.resetMain()
    }

    private fun localMillisAtHour(hourOfDay: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 11)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `meal selectors start with meal type based on local hour and remain editable`() = runTest(dispatcher) {
        val freshViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { localMillisAtHour(8) },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )

        assertEquals(MealType.BREAKFAST, freshViewModel.uiState.value.formMealType)
        assertEquals(MealType.BREAKFAST, freshViewModel.uiState.value.diceFilter)
        assertEquals(MenuAudience.ADULT, freshViewModel.uiState.value.formAudience)
        assertEquals(MenuAudience.ADULT, freshViewModel.uiState.value.diceAudienceFilter)

        freshViewModel.setFormMealType(MealType.DINNER)
        freshViewModel.setDiceFilter(MealType.LUNCH)

        assertEquals(MealType.DINNER, freshViewModel.uiState.value.formMealType)
        assertEquals(MealType.LUNCH, freshViewModel.uiState.value.diceFilter)
    }

    @Test
    fun `meal selectors use lunch at midday and dinner at night`() = runTest(dispatcher) {
        val lunchViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { localMillisAtHour(13) },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        val dinnerViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { localMillisAtHour(21) },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )

        assertEquals(MealType.LUNCH, lunchViewModel.uiState.value.formMealType)
        assertEquals(MealType.LUNCH, lunchViewModel.uiState.value.diceFilter)
        assertEquals(MealType.DINNER, dinnerViewModel.uiState.value.formMealType)
        assertEquals(MealType.DINNER, dinnerViewModel.uiState.value.diceFilter)
    }

    @Test
    fun `generate menu sends current device language to AI`() = runTest(dispatcher) {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.FRENCH)
        try {
            viewModel.generateMenuIdea()
            advanceUntilIdle()

            assertEquals(AppLanguage.FRENCH, analyzer.requestedLanguage)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `track cta tapped delegates safe screen and cta names to analytics`() {
        viewModel.trackCtaTapped(screen = "home", cta = "generate_ai_menu")

        assertEquals(listOf("cta_tapped:home:generate_ai_menu"), analytics.events)
    }

    @Test
    fun `dice empty recovery actions use closed analytics values`() {
        analytics.trackDiceEmptyRecovery("change_filters")

        assertEquals(listOf("dice_empty_recovery:change_filters"), analytics.events)
    }

    @Test
    fun `account and my zone analytics use closed values without personal data`() {
        viewModel.updateGuestAccess(isGuest = true, areLimitsEnabled = true)

        viewModel.trackMyZoneOpened()
        viewModel.trackAuthFlowStarted("register")
        viewModel.trackAuthAction("register", "email")
        viewModel.updateGuestAccess(isGuest = false, areLimitsEnabled = true)
        viewModel.trackAuthAction("sign_out", "account")

        assertEquals(
            listOf(
                "my_zone_opened:guest:0",
                "auth_flow_started:register:guest:0",
                "auth_action:register:email:guest",
                "auth_action:sign_out:account:signed_in"
            ),
            analytics.events
        )
    }

    @Test
    fun `save menu requires selecting an audience`() = runTest(dispatcher) {
        val freshViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        freshViewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        freshViewModel.setDietaryProfileAudienceEnabled(true)
        freshViewModel.setFormMealType(MealType.BREAKFAST)
        freshViewModel.updateName("Tostadas")
        freshViewModel.updateDescription("Pan, tomate y aguacate")

        freshViewModel.saveMenu()
        advanceUntilIdle()

        assertEquals(emptyList<FoodMenu>(), dao.saved.map { it.toDomain() })
        assertEquals("Selecciona si el menú es para persona adulta, peques o bebé.", freshViewModel.uiState.value.message)
        assertEquals(0L, freshViewModel.uiState.value.menuSaveSuccessRevision)
    }

    @Test
    fun `generate menu idea requires selecting an audience before using IA`() = runTest(dispatcher) {
        val freshViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        freshViewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        freshViewModel.setDietaryProfileAudienceEnabled(true)
        freshViewModel.setFormMealType(MealType.BREAKFAST)

        freshViewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals("Selecciona si el menú es para persona adulta, peques o bebé.", freshViewModel.uiState.value.message)
    }

    @Test
    fun `save menu stores local menu without analyzing it`() = runTest(dispatcher) {
        viewModel.updateName("Tostadas")
        viewModel.updateDescription("Pan, tomate y aguacate")
        viewModel.setFormMealType(MealType.BREAKFAST)
        analytics.events.clear()

        viewModel.saveMenu()
        advanceUntilIdle()

        val saved = dao.saved.single().toDomain()
        assertEquals("Tostadas", saved.name)
        assertEquals(MealType.BREAKFAST, saved.mealType)
        assertEquals(MenuAudience.ADULT, saved.audience)
        assertEquals("Pan, tomate y aguacate", saved.description)
        assertNull(saved.healthAnalysis)
        assertNull(saved.cuisineInspiration)
        assertFalse(analyzer.wasCalled)
        assertEquals(1L, viewModel.uiState.value.menuSaveSuccessRevision)
        assertEquals(MealType.BREAKFAST, viewModel.uiState.value.formMealType)
        assertEquals(MenuAudience.ADULT, viewModel.uiState.value.formAudience)
        assertEquals(
            listOf(
                "first_menu_created:BREAKFAST",
                "menu_saved:BREAKFAST:false:false:1",
                "menu_inventory_changed:1:0:1"
            ),
            analytics.events
        )
    }

    @Test
    fun `guest cannot save more than five menus per day when limits are enabled`() = runTest(dispatcher) {
        guestUsageStore.state = GuestDailyUsageState(
            dateKey = "2026-06-11",
            savedMenuCount = 5,
            generatedIdeaCount = 0,
            analysisCount = 0
        )
        viewModel.updateGuestAccess(isGuest = true, areLimitsEnabled = true)
        viewModel.updateName("Tostadas")
        viewModel.updateDescription("Pan, tomate y aguacate")

        viewModel.saveMenu()
        advanceUntilIdle()

        assertEquals(emptyList<FoodMenu>(), dao.saved.map { it.toDomain() })
        assertTrue(viewModel.uiState.value.message.orEmpty().contains("5 menús"))
        assertTrue(analytics.events.contains("guest_limit_reached:menu_save:5"))
    }

    @Test
    fun `guest save limit can be disabled remotely`() = runTest(dispatcher) {
        guestUsageStore.state = GuestDailyUsageState(
            dateKey = "2026-06-11",
            savedMenuCount = 5,
            generatedIdeaCount = 0,
            analysisCount = 0
        )
        viewModel.updateGuestAccess(isGuest = true, areLimitsEnabled = false)
        viewModel.updateName("Tostadas")
        viewModel.updateDescription("Pan, tomate y aguacate")

        viewModel.saveMenu()
        advanceUntilIdle()

        assertEquals("Tostadas", dao.saved.single().name)
    }

    @Test
    fun `guest cannot generate more than five AI ideas per day when limits are enabled`() = runTest(dispatcher) {
        guestUsageStore.state = GuestDailyUsageState(
            dateKey = "2026-06-11",
            savedMenuCount = 0,
            generatedIdeaCount = 5,
            analysisCount = 0
        )
        viewModel.updateGuestAccess(isGuest = true, areLimitsEnabled = true)

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertTrue(viewModel.uiState.value.message.orEmpty().contains("5 ideas"))
    }

    @Test
    fun `guest sees guest AI counters instead of project daily counter when limits are enabled`() = runTest(dispatcher) {
        guestUsageStore.state = GuestDailyUsageState(
            dateKey = "2026-06-11",
            savedMenuCount = 0,
            generatedIdeaCount = 0,
            analysisCount = 2
        )

        viewModel.updateGuestAccess(isGuest = true, areLimitsEnabled = true)

        assertEquals(20, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals(5, viewModel.uiState.value.aiGenerationUsesRemainingToday)
        assertEquals(3, viewModel.uiState.value.aiAnalysisUsesRemainingToday)
    }

    @Test
    fun `guest sees project AI counter when guest limits are disabled`() = runTest(dispatcher) {
        guestUsageStore.state = GuestDailyUsageState(
            dateKey = "2026-06-11",
            savedMenuCount = 0,
            generatedIdeaCount = 5,
            analysisCount = 5
        )

        viewModel.updateGuestAccess(isGuest = true, areLimitsEnabled = false)

        assertEquals(20, viewModel.uiState.value.aiGenerationUsesRemainingToday)
        assertEquals(20, viewModel.uiState.value.aiAnalysisUsesRemainingToday)
    }

    @Test
    fun `guest cannot analyze more than five menus per day when limits are enabled`() = runTest(dispatcher) {
        guestUsageStore.state = GuestDailyUsageState(
            dateKey = "2026-06-11",
            savedMenuCount = 0,
            generatedIdeaCount = 0,
            analysisCount = 5
        )
        viewModel.updateGuestAccess(isGuest = true, areLimitsEnabled = true)

        viewModel.analyzeExisting(
            FoodMenu(
                id = 1,
                name = "Tostadas",
                mealType = MealType.BREAKFAST,
                audience = MenuAudience.ADULT,
                description = "Pan, tomate y aguacate"
            )
        )
        advanceUntilIdle()

        assertEquals(0, analyzer.analysisCalls)
        assertTrue(viewModel.uiState.value.message.orEmpty().contains("5 análisis"))
    }

    @Test
    fun `guest successful actions consume separate daily counters`() = runTest(dispatcher) {
        guestUsageStore.state = GuestDailyUsageState(
            dateKey = "2026-06-11",
            savedMenuCount = 4,
            generatedIdeaCount = 4,
            analysisCount = 4
        )
        viewModel.updateGuestAccess(isGuest = true, areLimitsEnabled = true)
        viewModel.updateName("Tostadas")
        viewModel.updateDescription("Pan, tomate y aguacate")

        viewModel.saveMenu()
        advanceUntilIdle()
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        aiRequestThrottleStore.clearLastRequest()
        viewModel.analyzeExisting(
            FoodMenu(
                id = 1,
                name = "Tostadas",
                mealType = MealType.BREAKFAST,
                audience = MenuAudience.ADULT,
                description = "Pan, tomate y aguacate"
            )
        )
        advanceUntilIdle()

        assertEquals(
            GuestDailyUsageState(
                dateKey = "2026-06-11",
                savedMenuCount = 5,
                generatedIdeaCount = 5,
                analysisCount = 5
            ),
            guestUsageStore.state
        )
    }

    @Test
    fun `editing saved menu updates same menu and clears stale IA analysis`() = runTest(dispatcher) {
        val savedMenu = FoodMenu(
            id = 7,
            name = "Tostada",
            mealType = MealType.BREAKFAST,
            description = "Pan y aguacate",
            notes = "Con semillas",
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Equilibrado.",
                suggestion = "Mantener."
            ),
            calories = 420,
            lastPickedDate = "2026-06-10",
            createdAt = 123L
        )
        dao.seed(listOf(savedMenu))
        advanceUntilIdle()

        viewModel.startEditingMenu(savedMenu)
        assertEquals("", viewModel.uiState.value.name)
        assertEquals("Tostada", viewModel.uiState.value.editName)

        viewModel.updateEditName("Tostada con huevo")
        viewModel.updateEditDescription("Pan, aguacate y huevo")
        viewModel.saveEditedMenu()
        advanceUntilIdle()

        val updated = dao.saved.single().toDomain()
        assertEquals(7L, updated.id)
        assertEquals("Tostada con huevo", updated.name)
        assertEquals("Pan, aguacate y huevo", updated.description)
        assertNull(updated.healthAnalysis)
        assertNull(updated.calories)
        assertEquals("2026-06-10", updated.lastPickedDate)
        assertEquals(123L, updated.createdAt)
        assertNull(viewModel.uiState.value.editingMenuId)
        assertEquals("", viewModel.uiState.value.editName)
        assertEquals("", viewModel.uiState.value.name)
        assertEquals(
            listOf(
                "menu_edit_started:BREAKFAST:ADULT:true:false:1",
                "menu_edit_saved:BREAKFAST:ADULT:true:false:false:1",
                "menu_saved:BREAKFAST:false:false:1",
                "menu_inventory_changed:1:0:1"
            ),
            analytics.events
        )
    }

    @Test
    fun `editing saved menu can update optional user photo without clearing IA analysis`() = runTest(dispatcher) {
        val savedMenu = FoodMenu(
            id = 9,
            name = "Pasta",
            mealType = MealType.LUNCH,
            description = "Pasta con tomate",
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Equilibrado.",
                suggestion = "Mantener."
            ),
            calories = 480
        )
        dao.seed(listOf(savedMenu))
        advanceUntilIdle()

        viewModel.startEditingMenu(savedMenu)
        viewModel.updateEditImageUri("content://menu/photo/1")
        assertEquals("content://menu/photo/1", viewModel.uiState.value.editImageUri)

        viewModel.saveEditedMenu()
        advanceUntilIdle()

        val saved = dao.saved.single().toDomain()
        assertEquals("Pasta", saved.name)
        assertEquals("content://menu/photo/1", saved.imageUri)
        assertEquals(HealthStatus.HEALTHY, saved.healthAnalysis?.status)
        assertEquals(480, saved.calories)
    }

    @Test
    fun `toggle favorite updates saved menu without clearing analysis or photo`() = runTest(dispatcher) {
        val savedMenu = FoodMenu(
            id = 8L,
            name = "Pasta",
            mealType = MealType.LUNCH,
            description = "Pasta con tomate",
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Equilibrado.",
                suggestion = "Mantener verduras.",
                calories = 480
            ),
            calories = 480,
            imageUri = "content://menu/photo/1"
        )
        dao.seed(listOf(savedMenu))
        advanceUntilIdle()

        viewModel.toggleFavorite(savedMenu)
        advanceUntilIdle()

        val saved = dao.saved.single().toDomain()
        assertTrue(saved.isFavorite)
        assertEquals(localMillisAtHour(8), saved.favoritedAt)
        assertEquals(HealthStatus.HEALTHY, saved.healthAnalysis?.status)
        assertEquals(480, saved.calories)
        assertEquals("content://menu/photo/1", saved.imageUri)

        viewModel.toggleFavorite(saved)
        advanceUntilIdle()

        val unmarked = dao.saved.single().toDomain()
        assertFalse(unmarked.isFavorite)
        assertNull(unmarked.favoritedAt)
    }

    @Test
    fun `shows onboarding when it has not been completed and tracks it once`() = runTest(dispatcher) {
        analytics.events.clear()
        val firstRunStore = FakeOnboardingStore(completed = false)
        val firstRunViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = firstRunStore
        )

        assertEquals(true, firstRunViewModel.uiState.value.showOnboarding)
        assertEquals(listOf("onboarding_shown"), analytics.events)
    }

    @Test
    fun `does not show onboarding after it was completed previously and does not track shown`() = runTest(dispatcher) {
        analytics.events.clear()

        val completedViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = FakeOnboardingStore(completed = true)
        )

        assertEquals(false, completedViewModel.uiState.value.showOnboarding)
        assertEquals(emptyList<String>(), analytics.events)
    }

    @Test
    fun `shows onboarding again when stored completion is from older content version`() = runTest(dispatcher) {
        analytics.events.clear()
        val previousContentStore = FakeOnboardingStore(completed = true, completedVersion = 3)
        val updatedViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = previousContentStore
        )

        assertEquals(true, updatedViewModel.uiState.value.showOnboarding)
        assertEquals(listOf("onboarding_shown"), analytics.events)

        updatedViewModel.completeOnboarding()

        assertEquals(false, updatedViewModel.uiState.value.showOnboarding)
        assertEquals(5, previousContentStore.completedVersion)
    }

    @Test
    fun `complete onboarding hides it stores completion and tracks start action`() = runTest(dispatcher) {
        analytics.events.clear()
        onboardingStore = FakeOnboardingStore(completed = false)
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        analytics.events.clear()

        viewModel.completeOnboarding()

        assertEquals(false, viewModel.uiState.value.showOnboarding)
        assertEquals(true, onboardingStore.completed)
        assertEquals(listOf("onboarding_completed:start"), analytics.events)
    }

    @Test
    fun `skip onboarding hides it stores completion and tracks skip action`() = runTest(dispatcher) {
        analytics.events.clear()
        onboardingStore = FakeOnboardingStore(completed = false)
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        analytics.events.clear()

        viewModel.skipOnboarding()

        assertEquals(false, viewModel.uiState.value.showOnboarding)
        assertEquals(true, onboardingStore.completed)
        assertEquals(listOf("onboarding_completed:skip"), analytics.events)
    }

    @Test
    fun `opening about app tracks navigation without personal contact data`() = runTest(dispatcher) {
        viewModel.trackAboutAppOpened()

        assertEquals(listOf("about_app_opened"), analytics.events)
    }

    @Test
    fun `starting form and changing meal type track creation intent without menu text`() = runTest(dispatcher) {
        viewModel.setFormMealType(MealType.DINNER)
        viewModel.updateName("Tostadas")
        viewModel.updateDescription("Pan")

        assertEquals(
            listOf(
                "meal_type_selected:DINNER:false",
                "menu_form_started:name:DINNER"
            ),
            analytics.events
        )
    }

    @Test
    fun `blocked menu save tracks missing required fields`() = runTest(dispatcher) {
        viewModel.updateName("Solo nombre")
        analytics.events.clear()

        viewModel.saveMenu()
        advanceUntilIdle()

        assertEquals(
            listOf("menu_save_blocked:missing_required_fields:true:false"),
            analytics.events
        )
    }

    @Test
    fun `generate menu idea fills form using selected meal type`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Bowl de pollo y arroz",
            description = "Pollo, arroz, tomate, lechuga y aguacate.",
            notes = "Ingredientes faciles de supermercado.",
            calories = 560
        )
        viewModel.setFormMealType(MealType.LUNCH)
        analytics.events.clear()

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(MealType.LUNCH, analyzer.requestedMealType)
        assertEquals("Bowl de pollo y arroz", viewModel.uiState.value.name)
        assertEquals("Pollo, arroz, tomate, lechuga y aguacate.", viewModel.uiState.value.description)
        assertEquals("Ingredientes faciles de supermercado.", viewModel.uiState.value.notes)
        assertEquals(560, viewModel.uiState.value.calories)
        assertEquals(
            listOf(
                "ai_menu_generation_started:LUNCH:0",
                "ai_menu_generation_finished:LUNCH:true:unknown:none"
            ),
            analytics.events
        )
    }

    @Test
    fun `home starts in IA mode so the dice creates a new idea by default`() = runTest(dispatcher) {
        assertEquals(HomeMenuMode.Ai, viewModel.uiState.value.homeMenuMode)
    }

    @Test
    fun `generated IA idea is marked as pending detail before saving`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Bowl de lentejas",
            description = "Lentejas, arroz integral, tomate y aguacate.",
            notes = "Puedes usar lentejas cocidas.",
            calories = 540,
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Aporta fibra y proteina vegetal.",
                suggestion = "Ajusta la sal.",
                calories = 540
            )
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showGeneratedMenuDetail)
        assertEquals("Bowl de lentejas", viewModel.uiState.value.name)
        assertEquals(CuisineInspiration.MEXICAN, viewModel.uiState.value.generatedCuisineInspiration)
        assertEquals(emptyList<FoodMenu>(), dao.saved.map { it.toDomain() })
    }

    @Test
    fun `discard generated IA idea clears pending detail and draft`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Bowl de lentejas",
            description = "Lentejas, arroz integral, tomate y aguacate.",
            notes = "Puedes usar lentejas cocidas.",
            calories = 540
        )
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        viewModel.discardGeneratedMenuIdea()

        val state = viewModel.uiState.value
        assertFalse(state.showGeneratedMenuDetail)
        assertEquals("", state.name)
        assertEquals("", state.description)
        assertEquals("", state.notes)
        assertNull(state.calories)
        assertNull(state.generatedHealthAnalysis)
        assertNull(state.generatedCuisineInspiration)
    }

    @Test
    fun `try another generated idea preserves intent and starts one protected request`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Idea inicial",
            description = "Calabacín y arroz",
            notes = "",
            calories = 420
        )
        viewModel.setFormMealType(MealType.LUNCH)
        viewModel.setFormAudience(MenuAudience.ADULT)
        viewModel.updateAiBaseIngredients("calabacín")
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        aiRequestThrottleStore.clearLastRequest()
        analytics.events.clear()
        analyzer.generatedMenu = GeneratedMenu(
            name = "Idea alternativa",
            description = "Calabacín y quinoa",
            notes = "",
            calories = 450
        )

        viewModel.tryAnotherGeneratedMenuIdea()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Idea alternativa", state.name)
        assertEquals("calabacín", state.aiBaseIngredients)
        assertEquals(MealType.LUNCH, state.formMealType)
        assertEquals(MenuAudience.ADULT, state.formAudience)
        assertTrue(state.showGeneratedMenuDetail)
        assertEquals(2, analyzer.generateCalls)
        assertEquals(1, analytics.events.count { it.startsWith("ai_menu_generation_started") })
    }

    @Test
    fun `failed alternative clears cuisine from previous generated idea`() = runTest(dispatcher) {
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        assertEquals(CuisineInspiration.MEXICAN, viewModel.uiState.value.generatedCuisineInspiration)
        aiRequestThrottleStore.clearLastRequest()
        analyzer.generateFailure = IllegalStateException("offline")

        viewModel.tryAnotherGeneratedMenuIdea()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.generatedCuisineInspiration)
        assertFalse(viewModel.uiState.value.showGeneratedMenuDetail)
    }

    @Test
    fun `save generated IA idea from pending detail stores menu and closes detail`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Bowl de lentejas",
            description = "Lentejas, arroz integral, tomate y aguacate.",
            notes = "Puedes usar lentejas cocidas.",
            calories = 540,
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Aporta fibra y proteina vegetal.",
                suggestion = "Ajusta la sal.",
                calories = 540
            )
        )
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        viewModel.saveGeneratedMenuIdea()
        advanceUntilIdle()

        val saved = dao.saved.single().toDomain()
        val state = viewModel.uiState.value
        assertFalse(state.showGeneratedMenuDetail)
        assertNull(state.generatedCuisineInspiration)
        assertEquals("Bowl de lentejas", saved.name)
        assertEquals(CuisineInspiration.MEXICAN, saved.cuisineInspiration)
        assertEquals(HealthStatus.HEALTHY, saved.healthAnalysis?.status)
        assertEquals(540, saved.calories)
    }

    @Test
    fun `generated IA idea exposes market products enabled by default`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Tacos de pollo",
            description = "Pollo, tomate y tortillas.",
            notes = "Cena rápida.",
            calories = 480,
            shoppingProducts = listOf(
                requireNotNull(com.menudado.domain.ShoppingProduct.fromAi("Pollo")),
                requireNotNull(com.menudado.domain.ShoppingProduct.fromAi("Tomate"))
            )
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            listOf("Pollo", "Tomate"),
            viewModel.uiState.value.generatedShoppingProducts.map { it.displayName }
        )
        assertTrue(viewModel.uiState.value.addGeneratedMenuToMarketList)

        viewModel.setAddGeneratedMenuToMarketList(false)

        assertFalse(viewModel.uiState.value.addGeneratedMenuToMarketList)
    }

    @Test
    fun `save generated menu stores included IA analysis without analyzing again`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Bowl de lentejas",
            description = "Lentejas, arroz integral, tomate y aguacate.",
            notes = "Puedes usar lentejas cocidas.",
            calories = 540,
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Aporta fibra y proteina vegetal.",
                suggestion = "Ajusta la sal.",
                calories = 540
            )
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.saveMenu()
        advanceUntilIdle()

        val saved = dao.saved.single().toDomain()
        assertEquals(1, analyzer.generateCalls)
        assertFalse(analyzer.wasCalled)
        assertEquals(HealthStatus.HEALTHY, saved.healthAnalysis?.status)
        assertEquals("Aporta fibra y proteina vegetal.", saved.healthAnalysis?.reason)
        assertEquals(540, saved.calories)
        assertEquals(540, menuVisibleCalories(saved))
    }

    @Test
    fun `saved generated menu appears first in matching carousel`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(
                menuDao = dao,
                healthAnalyzer = analyzer,
                clockMillisProvider = { 2_000L }
            ),
            analytics = analytics,
            clockMillisProvider = { localMillisAtHour(12) },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        dao.seed(
            listOf(
                FoodMenu(
                    id = 1L,
                    name = "Menu antiguo",
                    mealType = MealType.LUNCH,
                    audience = MenuAudience.ADULT,
                    description = "Arroz con pollo.",
                    createdAt = 1_000L
                )
            )
        )
        advanceUntilIdle()
        analyzer.generatedMenu = GeneratedMenu(
            name = "Bowl mediterraneo",
            description = "Quinoa, pollo, tomate y pepino.",
            notes = "Servir fresco.",
            calories = 520,
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Incluye proteina y vegetales.",
                suggestion = "Mantener."
            )
        )
        viewModel.setFormMealType(MealType.LUNCH)
        viewModel.setFormAudience(MenuAudience.ADULT)

        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.saveMenu()
        advanceUntilIdle()

        val visibleMenus = menuCarouselVisibleMenus(
            menus = viewModel.uiState.value.menus,
            audience = MenuAudience.ADULT,
            isExpanded = false
        )
        assertEquals("Bowl mediterraneo", visibleMenus.first().name)
        assertEquals(MealType.LUNCH, visibleMenus.first().mealType)
        assertEquals(MenuAudience.ADULT, visibleMenus.first().audience)
    }

    @Test
    fun `manual menu is created without photo from add form`() = runTest(dispatcher) {
        viewModel.updateName("Crema de calabaza")
        viewModel.updateDescription("Calabaza, patata y aceite de oliva.")

        viewModel.saveMenu()
        advanceUntilIdle()

        val saved = dao.saved.single().toDomain()
        assertEquals("Crema de calabaza", saved.name)
        assertNull(saved.imageUri)
    }

    @Test
    fun `manual edit after generated IA idea clears included analysis and shows notice`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Bowl de lentejas",
            description = "Lentejas, arroz integral, tomate y aguacate.",
            notes = "Puedes usar lentejas cocidas.",
            calories = 540,
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Aporta fibra y proteina vegetal.",
                suggestion = "Ajusta la sal.",
                calories = 540
            )
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.updateDescription("Lentejas, arroz integral, tomate, aguacate y queso")
        viewModel.saveMenu()
        advanceUntilIdle()

        val saved = dao.saved.single().toDomain()
        assertNull(saved.healthAnalysis)
        assertEquals(
            "Modificaste la receta generada. Para verla como analizada, guarda el menú y toca Analizar IA.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun `changing form meal type resets menu form fields`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Bowl de lentejas",
            description = "Lentejas, arroz integral y tomate.",
            notes = "Puedes usar lentejas cocidas.",
            calories = 540,
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Aporta fibra.",
                suggestion = "Mantener variedad.",
                calories = 540
            )
        )
        viewModel.updateAiBaseIngredients("lentejas")
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        viewModel.setFormMealType(MealType.DINNER)

        val state = viewModel.uiState.value
        assertEquals(MealType.DINNER, state.formMealType)
        assertEquals("", state.name)
        assertEquals("", state.description)
        assertEquals("", state.notes)
        assertEquals("", state.aiBaseIngredients)
        assertNull(state.calories)
        assertNull(state.generatedHealthAnalysis)
    }

    @Test
    fun `changing form audience resets menu form fields`() = runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setDietaryProfileAudienceEnabled(true)
        viewModel.updateName("Pasta suave")
        viewModel.updateDescription("Pasta, tomate y queso")
        viewModel.updateNotes("Cortar pequeno")
        viewModel.updateAiBaseIngredients("pasta")

        viewModel.setFormAudience(MenuAudience.CHILD)

        val state = viewModel.uiState.value
        assertEquals(MenuAudience.CHILD, state.formAudience)
        assertEquals("", state.name)
        assertEquals("", state.description)
        assertEquals("", state.notes)
        assertEquals("", state.aiBaseIngredients)
        assertNull(state.calories)
        assertNull(state.generatedHealthAnalysis)
    }

    @Test
    fun `editing form text keeps the other existing text fields`() = runTest(dispatcher) {
        viewModel.updateName("Pasta")
        viewModel.updateDescription("Pasta, tomate y queso")
        viewModel.updateNotes("Sin picante")
        viewModel.updateAiBaseIngredients("tomate")

        viewModel.updateName("Pasta cremosa")

        val state = viewModel.uiState.value
        assertEquals("Pasta cremosa", state.name)
        assertEquals("Pasta, tomate y queso", state.description)
        assertEquals("Sin picante", state.notes)
        assertEquals("tomate", state.aiBaseIngredients)
    }

    @Test
    fun `starts with daily IA uses available`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore
        )

        assertEquals(20, viewModel.uiState.value.aiUsesRemainingToday)
    }

    @Test
    fun `generate menu idea decreases daily IA uses`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(19, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals(1, aiDailyUsageStore.storedUsedCount)
    }

    @Test
    fun `generate menu idea keeps local IA pause hidden after real request`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.aiRetryAtMillis)
        assertFalse(viewModel.uiState.value.isAiRequestThrottlePause)
        assertFalse(viewModel.uiState.value.isAiRetryNoticeVisible)
        assertNull(viewModel.uiState.value.message)
        assertEquals(100_000L, aiRequestThrottleStore.storedLastRequestAtMillis)
    }

    @Test
    fun `typing base ingredients does not expose hidden local IA pause`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        viewModel.updateAiBaseIngredients("berenjera")

        assertEquals("berenjera", viewModel.uiState.value.aiBaseIngredients)
        assertNull(viewModel.uiState.value.aiRetryAtMillis)
        assertFalse(viewModel.uiState.value.isAiRequestThrottlePause)
        assertFalse(viewModel.uiState.value.isAiRetryNoticeVisible)
    }

    @Test
    fun `restored local request pause is cleared on startup and does not block generation`() = runTest(dispatcher) {
        aiRequestThrottleStore.storedLastRequestAtMillis = 99_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(1, analyzer.generateCalls)
        assertNull(viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.aiRetryAtMillis)
        assertFalse(viewModel.uiState.value.isAiRequestThrottlePause)
    }

    @Test
    fun `generate menu idea stops loading and shows timeout message when IA takes too long`() = runTest(dispatcher) {
        analyzer.generateDelayMillis = 46_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { currentTime },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)

        viewModel.generateMenuIdea()
        runCurrent()
        advanceTimeBy(45_001L)
        runCurrent()

        assertFalse(viewModel.uiState.value.isGeneratingMenu)
        assertEquals("La IA tardó demasiado en responder. Revisa la conexión e inténtalo de nuevo.", viewModel.uiState.value.message)
        assertEquals(1, analyzer.generateCalls)
    }

    @Test
    fun `generate menu idea stops loading even if post success tracking fails`() = runTest(dispatcher) {
        analytics.throwOnAiMenuGenerationFinished = true

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals("Tostada", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.isGeneratingMenu)
    }

    @Test
    fun `generate menu idea does not call IA when daily local uses are exhausted`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore.apply {
                storedDateKey = "1969-12-31"
                storedUsedCount = 20
            }
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analytics.events.clear()

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals(0, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals("Has usado la IA gratuita de MenuDado por hoy. Tus menús siguen disponibles y podrás intentarlo más tarde.", viewModel.uiState.value.message)
        assertEquals(listOf("ai_daily_limit_reached:generate_menu"), analytics.events)
    }

    @Test
    fun `generate menu idea asks IA to avoid saved menus and current form idea`() = runTest(dispatcher) {
        dao.seed(
            listOf(
                FoodMenu(id = 1, name = "Tostada de aguacate", mealType = MealType.BREAKFAST, description = "Pan integral con aguacate y huevo"),
                FoodMenu(id = 2, name = "Pasta con pollo", mealType = MealType.LUNCH, description = "Pasta, pollo y tomate")
            )
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.updateName("Yogur con fruta")
        viewModel.updateDescription("Yogur griego, fresas y avena")
        advanceUntilIdle()

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "Tostada de aguacate: Pan integral con aguacate y huevo",
                "Yogur con fruta: Yogur griego, fresas y avena"
            ),
            analyzer.avoidIdeas
        )
    }

    @Test
    fun `world cuisine generation keeps the existing limit of eight avoided ideas`() = runTest(dispatcher) {
        dao.seed(
            (1L..10L).map { id ->
                FoodMenu(
                    id = id,
                    name = "Menu $id",
                    mealType = MealType.BREAKFAST,
                    audience = MenuAudience.ADULT,
                    description = "Descripcion $id"
                )
            }
        )
        advanceUntilIdle()

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(8, analyzer.avoidIdeas.size)
        assertEquals("Menu 3: Descripcion 3", analyzer.avoidIdeas.first())
        assertEquals("Menu 10: Descripcion 10", analyzer.avoidIdeas.last())
    }

    @Test
    fun `successful generation advances cuisine once while using one IA call`() = runTest(dispatcher) {
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(1, analyzer.generateCalls)
        assertEquals(CuisineInspiration.MEXICAN, analyzer.requestedCuisineInspiration)
        assertEquals(CuisineInspiration.MEXICAN, viewModel.uiState.value.generatedCuisineInspiration)
        assertEquals(
            CuisineInspiration.JAPANESE,
            cuisineRotation.current(MealType.BREAKFAST, MenuAudience.ADULT)
        )
    }

    @Test
    fun `failed generation keeps pending cuisine for retry`() = runTest(dispatcher) {
        analyzer.generateFailure = IllegalStateException("offline")

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(1, analyzer.generateCalls)
        assertEquals(CuisineInspiration.MEXICAN, analyzer.requestedCuisineInspiration)
        assertNull(viewModel.uiState.value.generatedCuisineInspiration)
        assertEquals(
            CuisineInspiration.MEXICAN,
            cuisineRotation.current(MealType.BREAKFAST, MenuAudience.ADULT)
        )
    }

    @Test
    fun `generate menu idea ignores repeated taps while previous generation is running`() = runTest(dispatcher) {
        analyzer.generateDelayMillis = 1_000L

        viewModel.generateMenuIdea()
        runCurrent()
        viewModel.generateMenuIdea()
        runCurrent()

        assertEquals(1, analyzer.generateCalls)
        advanceTimeBy(1_000L)
        advanceUntilIdle()
    }

    @Test
    fun `generate menu idea ignores immediate repeated taps before generation coroutine starts`() = runTest(dispatcher) {
        analyzer.generateDelayMillis = 1_000L

        viewModel.generateMenuIdea()
        viewModel.generateMenuIdea()
        runCurrent()

        assertEquals(1, analyzer.generateCalls)
        assertEquals(19, viewModel.uiState.value.aiUsesRemainingToday)
        advanceTimeBy(1_000L)
        advanceUntilIdle()
    }

    @Test
    fun `generate menu idea waits locally before another real IA request`() = runTest(dispatcher) {
        var nowMillis = 100_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { nowMillis },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        aiRequestThrottleStore.storedLastRequestAtMillis = 99_500L

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals(20, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals(100_500L, viewModel.uiState.value.aiRetryAtMillis)
        assertNull(viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isAiRetryNoticeVisible)

        nowMillis = 100_500L
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(1, analyzer.generateCalls)
        assertEquals(19, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals(100_500L, aiRequestThrottleStore.storedLastRequestAtMillis)
    }

    @Test
    fun `generate menu idea can be requested again after a completed response exceeds local anti double tap pause`() = runTest(dispatcher) {
        analyzer.generateDelayMillis = 1_500L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { 100_000L + currentTime },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)

        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.discardGeneratedMenuIdea()
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(2, analyzer.generateCalls)
        assertEquals(18, viewModel.uiState.value.aiUsesRemainingToday)
        assertNull(viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.aiRetryAtMillis)
        assertFalse(viewModel.uiState.value.isAiRetryNoticeVisible)
    }

    @Test
    fun `generate menu idea avoids other generated ideas from the same day`() = runTest(dispatcher) {
        var nowMillis = 100_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { nowMillis },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generatedMenu = GeneratedMenu(
            name = "Crema de calabacin",
            description = "Calabacin, yogur natural y semillas.",
            notes = "Lista en pocos minutos.",
            calories = 310
        )
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        nowMillis += 30_000L
        analyzer.generatedMenu = GeneratedMenu(
            name = "Ensalada Cesar saludable",
            description = "Lechuga, pollo a la plancha, yogur y pan integral tostado.",
            notes = "Usa salsa ligera.",
            calories = 430
        )
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        nowMillis += 30_000L
        analyzer.generatedMenu = GeneratedMenu(
            name = "Bowl de garbanzos",
            description = "Garbanzos, tomate, pepino y aguacate.",
            notes = "Servir fresco.",
            calories = 480
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "Crema de calabacin: Calabacin, yogur natural y semillas.",
                "Ensalada Cesar saludable: Lechuga, pollo a la plancha, yogur y pan integral tostado."
            ),
            analyzer.avoidIdeas
        )
    }

    @Test
    fun `loads and saves dietary profile locally`() = runTest(dispatcher) {
        dietaryProfileStore.storedProfile = DietaryProfile(
            isVegan = true,
            hasAllergies = true,
            allergens = setOf(DietaryAllergen.GLUTEN),
            otherAvoidances = "setas"
        )
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            dietaryProfileStore = dietaryProfileStore
        )

        assertEquals(true, viewModel.uiState.value.dietaryProfile.isVegan)
        assertEquals(setOf(DietaryAllergen.GLUTEN), viewModel.uiState.value.dietaryProfile.allergens)

        viewModel.setDietaryProfileVegan(false)
        viewModel.setDietaryProfileHasAllergies(true)
        viewModel.toggleDietaryAllergen(DietaryAllergen.PEANUT)
        viewModel.updateDietaryProfileOtherAvoidances("picante")

        assertEquals(
            DietaryProfile(
                isVegan = false,
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.GLUTEN, DietaryAllergen.PEANUT),
                otherAvoidances = "picante"
            ),
            dietaryProfileStore.storedProfile
        )
    }

    @Test
    fun `only adult audience is enabled by default`() = runTest(dispatcher) {
        assertEquals(
            listOf(MenuAudience.ADULT),
            viewModel.uiState.value.enabledAudiences
        )
        assertEquals(true, viewModel.uiState.value.dietaryProfile.isEnabled)
    }

    @Test
    fun `single active audience is selected by default in selectors`() = runTest(dispatcher) {
        assertEquals(MenuAudience.ADULT, viewModel.uiState.value.formAudience)
        assertEquals(MenuAudience.ADULT, viewModel.uiState.value.diceAudienceFilter)
    }

    @Test
    fun `remote profile refresh selects the only active audience`() = runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setDietaryProfileAudienceEnabled(true)
        assertNull(viewModel.uiState.value.formAudience)

        dietaryProfileStore.saveProfile(
            DietaryProfile(
                isEnabled = false,
                ageRange = MenuAudience.ADULT.defaultAgeRange
            ),
            MenuAudience.ADULT
        )

        viewModel.refreshDietaryProfile()

        assertEquals(listOf(MenuAudience.CHILD), viewModel.uiState.value.enabledAudiences)
        assertEquals(MenuAudience.CHILD, viewModel.uiState.value.formAudience)
        assertEquals(MenuAudience.CHILD, viewModel.uiState.value.diceAudienceFilter)
    }

    @Test
    fun `dietary profile is stored independently per audience`() = runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setDietaryProfileAudienceEnabled(true)
        viewModel.setDietaryProfileVegan(true)

        viewModel.setDietaryProfileAudience(MenuAudience.BABY)
        viewModel.setDietaryProfileAudienceEnabled(true)
        assertEquals(false, viewModel.uiState.value.dietaryProfile.isVegan)

        viewModel.setDietaryProfileHasAllergies(true)
        viewModel.toggleDietaryAllergen(DietaryAllergen.EGG)

        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        assertEquals(true, viewModel.uiState.value.dietaryProfile.isVegan)
        assertEquals(emptySet<DietaryAllergen>(), viewModel.uiState.value.dietaryProfile.allergens)

        viewModel.setDietaryProfileAudience(MenuAudience.BABY)
        assertEquals(false, viewModel.uiState.value.dietaryProfile.isVegan)
        assertEquals(setOf(DietaryAllergen.EGG), viewModel.uiState.value.dietaryProfile.allergens)
    }

    @Test
    fun `audience can be disabled from dietary profile and hidden from selectors`() = runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setDietaryProfileAudienceEnabled(true)

        viewModel.setDietaryProfileAudienceEnabled(false)

        assertEquals(
            listOf(MenuAudience.ADULT),
            viewModel.uiState.value.enabledAudiences
        )
    }

    @Test
    fun `last active audience cannot be disabled`() = runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.ADULT)

        viewModel.setDietaryProfileAudienceEnabled(false)

        assertEquals(true, viewModel.uiState.value.dietaryProfile.isEnabled)
        assertEquals(
            listOf(MenuAudience.ADULT),
            viewModel.uiState.value.enabledAudiences
        )
    }

    @Test
    fun `adding a second active audience clears default audience selections`() = runTest(dispatcher) {
        assertEquals(MenuAudience.ADULT, viewModel.uiState.value.formAudience)
        assertEquals(MenuAudience.ADULT, viewModel.uiState.value.diceAudienceFilter)

        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setDietaryProfileAudienceEnabled(true)

        assertNull(viewModel.uiState.value.formAudience)
        assertNull(viewModel.uiState.value.diceAudienceFilter)
    }

    @Test
    fun `disabling selected audience falls back to remaining single active audience`() = runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setDietaryProfileAudienceEnabled(true)
        viewModel.setFormAudience(MenuAudience.CHILD)
        viewModel.setDiceAudienceFilter(MenuAudience.CHILD)

        viewModel.setDietaryProfileAudienceEnabled(false)

        assertEquals(MenuAudience.ADULT, viewModel.uiState.value.formAudience)
        assertEquals(MenuAudience.ADULT, viewModel.uiState.value.diceAudienceFilter)
    }

    @Test
    fun `dice discards result if its audience is disabled while rolling`() = runTest(dispatcher) {
        dietaryProfileStore.saveProfile(
            DietaryProfile(isEnabled = true, ageRange = MenuAudience.CHILD.defaultAgeRange),
            MenuAudience.CHILD
        )
        viewModel.refreshDietaryProfile()
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setFormAudience(MenuAudience.CHILD)
        viewModel.setDiceAudienceFilter(MenuAudience.CHILD)
        viewModel.setDiceFilter(MealType.BREAKFAST)
        dao.seed(
            listOf(
                FoodMenu(
                    id = 1,
                    name = "Desayuno peques",
                    mealType = MealType.BREAKFAST,
                    audience = MenuAudience.CHILD,
                    description = "Oculto"
                )
            )
        )
        advanceUntilIdle()

        viewModel.rollDice()
        runCurrent()
        viewModel.setDietaryProfileAudienceEnabled(false)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.result)
        assertNull(dao.saved.single().toDomain().lastPickedDate)
    }

    @Test
    fun `generated idea is discarded if its audience is disabled during request`() = runTest(dispatcher) {
        dietaryProfileStore.saveProfile(
            DietaryProfile(isEnabled = true, ageRange = MenuAudience.CHILD.defaultAgeRange),
            MenuAudience.CHILD
        )
        viewModel.refreshDietaryProfile()
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setFormAudience(MenuAudience.CHILD)
        analyzer.generateDelayMillis = 100L
        analyzer.generatedMenu = GeneratedMenu(
            name = "Idea para peques",
            description = "Contenido infantil",
            notes = "",
            calories = 420
        )

        viewModel.generateMenuIdea()
        runCurrent()
        viewModel.setDietaryProfileAudienceEnabled(false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showGeneratedMenuDetail)
        assertEquals("", state.name)
        assertEquals(MenuAudience.ADULT, state.formAudience)
    }

    @Test
    fun `inactive audience ignores dietary restriction changes`() = runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)

        viewModel.setDietaryProfileVegan(true)
        viewModel.setDietaryProfileHasAllergies(true)
        viewModel.toggleDietaryAllergen(DietaryAllergen.EGG)
        viewModel.updateDietaryProfileOtherAvoidances("picante")

        assertEquals(
            DietaryProfile(
                isEnabled = false,
                ageRange = MenuAudience.CHILD.defaultAgeRange
            ),
            viewModel.uiState.value.dietaryProfile
        )
    }

    @Test
    fun `dietary profile stores age range per audience`() = runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.BABY)

        viewModel.updateDietaryProfileAgeRange("8-10 meses")
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)

        assertEquals("2-12 años", viewModel.uiState.value.dietaryProfile.ageRange)

        viewModel.setDietaryProfileAudience(MenuAudience.BABY)
        assertEquals("8-10 meses", viewModel.uiState.value.dietaryProfile.ageRange)
    }

    @Test
    fun `adult dietary profile stores pregnancy setting and sends it to IA`() = runTest(dispatcher) {
        viewModel.setDietaryProfilePregnant(true)

        assertEquals(true, dietaryProfileStore.storedProfile.isPregnant)

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(true, analyzer.requestedDietaryProfile?.isPregnant)
    }

    @Test
    fun `generate menu idea sends dietary profile to IA`() = runTest(dispatcher) {
        viewModel.setDietaryProfileVegan(true)
        viewModel.setDietaryProfileHasAllergies(true)
        viewModel.toggleDietaryAllergen(DietaryAllergen.SOY)
        viewModel.updateDietaryProfileOtherAvoidances("picante")

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            DietaryProfile(
                ageRange = MenuAudience.ADULT.defaultAgeRange,
                isVegan = true,
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.SOY),
                otherAvoidances = "picante"
            ),
            analyzer.requestedDietaryProfile
        )
    }

    @Test
    fun `generate menu idea uses the selected audience and its dietary profile`() = runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setDietaryProfileAudienceEnabled(true)
        viewModel.setDietaryProfileVegan(true)
        viewModel.setFormAudience(MenuAudience.CHILD)

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(MenuAudience.CHILD, analyzer.requestedAudience)
        assertEquals(
            DietaryProfile(
                ageRange = MenuAudience.CHILD.defaultAgeRange,
                isVegan = true
            ),
            analyzer.requestedDietaryProfile
        )
    }

    @Test
    fun `generate menu idea blocks ingredients that do not match dietary profile`() = runTest(dispatcher) {
        viewModel.setDietaryProfileHasAllergies(true)
        viewModel.toggleDietaryAllergen(DietaryAllergen.DAIRY)
        viewModel.updateAiBaseIngredients("berenjena, crema")

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals(20, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals(0, aiDailyUsageStore.storedUsedCount)
        assertEquals(
            "Revisa los ingredientes: crema no encaja con tu perfil alimentario.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun `generate menu idea sends base ingredients to IA when they match profile`() = runTest(dispatcher) {
        viewModel.setDietaryProfileHasAllergies(true)
        viewModel.toggleDietaryAllergen(DietaryAllergen.DAIRY)
        viewModel.updateAiBaseIngredients("berenjena")

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(1, analyzer.generateCalls)
        assertEquals("berenjena", analyzer.requestedBaseIngredients)
    }

    @Test
    fun `generate menu idea shows quota message when IA quota is exhausted`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 57s.")

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            "La IA está con mucha demanda. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
        assertEquals(159_000L, viewModel.uiState.value.aiRetryAtMillis)
        assertEquals(159_000L, aiQuotaRetryStore.storedRetryAtMillis)
    }

    @Test
    fun `generate menu idea shows request rate limit message when request quota is exhausted`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException(
            "Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 20. Please retry in 21s."
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            "La IA está con mucha demanda. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun `generate menu idea shows token rate limit message when token quota is exhausted`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException(
            "RESOURCE_EXHAUSTED quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_input_tokens. Please retry in 21s."
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            "La IA está con mucha demanda. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun `generate menu idea shows daily limit message when daily quota is exhausted`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException(
            "RESOURCE_EXHAUSTED quota exceeded for metric: Requests per day, limit: 20."
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            "La IA está con mucha demanda. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun `generate menu idea shows temporary AI message when provider returns internal error`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException("INTERNAL")

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            "El servicio tuvo un problema temporal. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
        assertNull(viewModel.uiState.value.aiRetryAtMillis)
    }

    @Test
    fun `generate menu idea does not call IA again while quota retry is active`() = runTest(dispatcher) {
        var nowMillis = 100_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { nowMillis },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 57s.")

        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.clearMessage()
        nowMillis = 120_000L
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(1, analyzer.generateCalls)
        assertEquals(
            "La IA está con mucha demanda. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
        assertEquals(159_000L, viewModel.uiState.value.aiRetryAtMillis)
    }

    @Test
    fun `generate menu idea does not call IA while persisted quota retry is active`() = runTest(dispatcher) {
        var nowMillis = 120_000L
        aiQuotaRetryStore.storedRetryAtMillis = 159_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { nowMillis },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals(
            "La IA está con mucha demanda. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
        assertEquals(159_000L, viewModel.uiState.value.aiRetryAtMillis)
    }

    @Test
    fun `persisted quota retry takes precedence over local request pause`() = runTest(dispatcher) {
        var nowMillis = 120_000L
        aiQuotaRetryStore.storedRetryAtMillis = 159_000L
        aiRequestThrottleStore.storedLastRequestAtMillis = 110_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { nowMillis },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)

        assertEquals(159_000L, viewModel.uiState.value.aiRetryAtMillis)
        assertFalse(viewModel.uiState.value.isAiRequestThrottlePause)

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals(159_000L, viewModel.uiState.value.aiRetryAtMillis)
        assertEquals(
            "La IA está con mucha demanda. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun `generate menu idea calls IA again after quota retry expires`() = runTest(dispatcher) {
        var nowMillis = 100_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { nowMillis },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 57s.")
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.clearMessage()

        nowMillis = 160_000L
        analyzer.generateFailure = null
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(2, analyzer.generateCalls)
        assertNull(viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.aiRetryAtMillis)
        assertFalse(viewModel.uiState.value.isAiRetryNoticeVisible)
        assertNull(aiQuotaRetryStore.storedRetryAtMillis)
        assertEquals("Tostada", viewModel.uiState.value.name)
    }

    @Test
    fun `clear message removes expired quota retry from ui state`() = runTest(dispatcher) {
        var nowMillis = 120_000L
        aiQuotaRetryStore.storedRetryAtMillis = 159_000L
        aiQuotaRetryStore.storedConsecutiveFailures = 1
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { nowMillis },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)

        viewModel.generateMenuIdea()
        advanceUntilIdle()
        nowMillis = 160_000L
        viewModel.clearMessage()

        assertNull(viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.aiRetryAtMillis)
        assertEquals(159_000L, aiQuotaRetryStore.storedRetryAtMillis)
    }

    @Test
    fun `active quota retry refreshes when Pacific daily reset arrives without user interaction`() = runTest(dispatcher) {
        val resetAtMillis = 1_780_642_800_000L
        aiDailyUsageStore.storedDateKey = "2026-06-04"
        aiDailyUsageStore.storedUsedCount = 20
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 1_780_642_799_000L + currentTime },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        viewModel.generateMenuIdea()

        assertEquals(resetAtMillis, viewModel.uiState.value.aiRetryAtMillis)
        assertEquals(0, viewModel.uiState.value.aiUsesRemainingToday)

        advanceTimeBy(1_000L)
        runCurrent()

        assertNull(viewModel.uiState.value.aiRetryAtMillis)
        assertEquals(20, viewModel.uiState.value.aiUsesRemainingToday)
    }

    @Test
    fun `generate menu idea increases local backoff when quota fails repeatedly`() = runTest(dispatcher) {
        var nowMillis = 100_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { nowMillis },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 10s.")
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        assertEquals(112_000L, viewModel.uiState.value.aiRetryAtMillis)

        viewModel.clearMessage()
        nowMillis = 113_000L
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 10s.")
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(2, analyzer.generateCalls)
        assertEquals(233_000L, viewModel.uiState.value.aiRetryAtMillis)
        assertEquals(233_000L, aiQuotaRetryStore.storedRetryAtMillis)
        assertEquals(2, aiQuotaRetryStore.storedConsecutiveFailures)
    }

    @Test
    fun `analyze existing shows quota message when IA quota is exhausted`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 250_000L },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        analyzer.analysisFailure = IllegalStateException("RESOURCE_EXHAUSTED quota exceeded. Please retry in 42.5s.")

        viewModel.analyzeExisting(
            FoodMenu(
                id = 1,
                name = "Tostadas",
                mealType = MealType.BREAKFAST,
                description = "Pan y aguacate"
            )
        )
        advanceUntilIdle()

        assertEquals(
            "La IA está con mucha demanda. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
        assertEquals(295_000L, viewModel.uiState.value.aiRetryAtMillis)
    }

    @Test
    fun `analyze existing shows temporary AI message when provider returns internal error`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 250_000L },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        analyzer.analysisFailure = IllegalStateException("INTERNAL")

        viewModel.analyzeExisting(
            FoodMenu(
                id = 1,
                name = "Tostadas",
                mealType = MealType.BREAKFAST,
                description = "Pan y aguacate"
            )
        )
        advanceUntilIdle()

        assertEquals(
            "El servicio tuvo un problema temporal. Inténtalo nuevamente más tarde.",
            viewModel.uiState.value.message
        )
        assertNull(viewModel.uiState.value.aiRetryAtMillis)
    }

    @Test
    fun `analyze existing saves estimated calories from IA analysis`() = runTest(dispatcher) {
        val menu = FoodMenu(
            id = 1,
            name = "Arroz",
            mealType = MealType.LUNCH,
            description = "Arroz con atun"
        )
        dao.seed(listOf(menu))
        analyzer.analysisCalories = 640

        viewModel.analyzeExisting(menu)
        advanceUntilIdle()

        val saved = dao.saved.single().toDomain()
        assertEquals(640, saved.calories)
        assertEquals(640, menuVisibleCalories(saved))
    }

    @Test
    fun `analyze existing ignores immediate repeated taps before analysis coroutine starts`() = runTest(dispatcher) {
        val menu = FoodMenu(
            id = 1,
            name = "Arroz",
            mealType = MealType.LUNCH,
            description = "Arroz con atun"
        )
        analyzer.analysisDelayMillis = 1_000L

        viewModel.analyzeExisting(menu)
        viewModel.analyzeExisting(menu)
        runCurrent()

        assertEquals(1, analyzer.analysisCalls)
        assertEquals(19, viewModel.uiState.value.aiUsesRemainingToday)
        advanceTimeBy(1_000L)
        advanceUntilIdle()
    }

    @Test
    fun `analyze existing shares local wait with recent IA generation`() = runTest(dispatcher) {
        var nowMillis = 200_000L
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { nowMillis },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        nowMillis += 500L
        viewModel.analyzeExisting(
            FoodMenu(
                id = 1,
                name = "Arroz",
                mealType = MealType.LUNCH,
                description = "Arroz con atun"
            )
        )
        advanceUntilIdle()

        assertEquals(1, analyzer.generateCalls)
        assertEquals(0, analyzer.analysisCalls)
        assertEquals(19, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals(201_000L, viewModel.uiState.value.aiRetryAtMillis)
    }

    @Test
    fun `analyze pending menus uses one IA call and saves valid results`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiRequestThrottleStore = aiRequestThrottleStore,
            aiDailyUsageStore = aiDailyUsageStore
        )
        val analyzed = FoodMenu(
            id = 1,
            name = "Tostada",
            mealType = MealType.BREAKFAST,
            description = "Pan y aguacate",
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Ya analizado.",
                suggestion = "Mantener.",
                calories = 430
            ),
            calories = 430
        )
        val pendingOne = FoodMenu(
            id = 2,
            name = "Arroz",
            mealType = MealType.LUNCH,
            description = "Arroz con atun"
        )
        val pendingTwo = FoodMenu(
            id = 3,
            name = "Pasta",
            mealType = MealType.DINNER,
            description = "Pasta con tomate"
        )
        dao.seed(listOf(analyzed, pendingOne, pendingTwo))
        analyzer.batchAnalyses = mapOf(
            2L to HealthAnalysis(
                status = HealthStatus.IMPROVABLE,
                reason = "Tiene proteina, pero poca verdura.",
                suggestion = "Agrega ensalada.",
                calories = 620
            ),
            3L to HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Es equilibrado.",
                suggestion = "Mantener porcion.",
                calories = 510
            )
        )
        advanceUntilIdle()

        viewModel.analyzePendingMenus()
        advanceUntilIdle()

        val savedById = dao.saved.map { it.toDomain() }.associateBy { it.id }
        assertEquals(1, analyzer.batchAnalyzeCalls)
        assertEquals(listOf(2L, 3L), analyzer.batchAnalyzeMenuIds)
        assertEquals(19, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals(1, aiDailyUsageStore.storedUsedCount)
        assertEquals(HealthStatus.IMPROVABLE, savedById.getValue(2L).healthAnalysis?.status)
        assertEquals(620, savedById.getValue(2L).calories)
        assertEquals(HealthStatus.HEALTHY, savedById.getValue(3L).healthAnalysis?.status)
        assertEquals(510, savedById.getValue(3L).calories)
        assertEquals(HealthStatus.HEALTHY, savedById.getValue(1L).healthAnalysis?.status)
        assertEquals(
            listOf(
                "ai_analysis_started:batch:UNKNOWN:2",
                "ai_analysis_finished:batch:UNKNOWN:true:2:unknown:none"
            ),
            analytics.events
        )
    }

    @Test
    fun `analyze pending menus ignores inactive profiles`() = runTest(dispatcher) {
        val adultMenu = FoodMenu(
            id = 1,
            name = "Adulto",
            mealType = MealType.LUNCH,
            audience = MenuAudience.ADULT,
            description = "Visible"
        )
        val childMenu = FoodMenu(
            id = 2,
            name = "Peques",
            mealType = MealType.LUNCH,
            audience = MenuAudience.CHILD,
            description = "Oculto"
        )
        dao.seed(listOf(adultMenu, childMenu))
        analyzer.batchAnalyses = mapOf(
            1L to HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Correcto.",
                suggestion = "Mantener."
            )
        )
        advanceUntilIdle()

        viewModel.analyzePendingMenus()
        advanceUntilIdle()

        assertEquals(listOf(1L), analyzer.batchAnalyzeMenuIds)
        assertNull(dao.saved.single { it.id == 2L }.toDomain().healthAnalysis)
    }

    @Test
    fun `analyze existing tracks started and successful analysis events`() = runTest(dispatcher) {
        val menu = FoodMenu(
            id = 1,
            name = "Tostadas",
            mealType = MealType.BREAKFAST,
            description = "Pan y aguacate"
        )
        dao.seed(listOf(menu))

        viewModel.analyzeExisting(menu)
        advanceUntilIdle()

        assertEquals(
            listOf(
                "ai_analysis_started:single:BREAKFAST:1",
                "ai_analysis_finished:single:BREAKFAST:true:1:healthy:none"
            ),
            analytics.events
        )
    }

    @Test
    fun `ai menu generation failure tracks failed event`() = runTest(dispatcher) {
        analyzer.generateFailure = IllegalStateException("No internet")

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "ai_menu_generation_started:BREAKFAST:0",
                "ai_menu_generation_finished:BREAKFAST:false:unknown:generic"
            ),
            analytics.events
        )
    }

    @Test
    fun `non IA validation messages clear IA retry notice marker`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 57s.")
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isAiRetryNoticeVisible)

        viewModel.saveMenu()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Agrega nombre e ingredientes para guardar el menú.", state.message)
        assertEquals(159_000L, state.aiRetryAtMillis)
        assertFalse(state.isAiRetryNoticeVisible)
    }

    @Test
    fun `dice required filter message is not shown as IA retry notice after quota pause`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.setFormAudience(MenuAudience.ADULT)
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 57s.")
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        assertEquals(159_000L, viewModel.uiState.value.aiRetryAtMillis)
        assertEquals(true, viewModel.uiState.value.isAiRetryNoticeVisible)

        viewModel.clearMessage()
        dao.seed(
            listOf(
                FoodMenu(id = 1, name = "Tostada", mealType = MealType.BREAKFAST, description = "Pan")
            )
        )
        advanceUntilIdle()
        viewModel.setDiceFilter(null)
        viewModel.rollDice()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Primero escoge desayuno, almuerzo o cena.", state.message)
        assertEquals(159_000L, state.aiRetryAtMillis)
        assertFalse(state.isAiRetryNoticeVisible)
    }

    @Test
    fun `roll dice requires selecting a meal type filter`() = runTest(dispatcher) {
        dao.seed(
            listOf(
                FoodMenu(id = 1, name = "Tostada", mealType = MealType.BREAKFAST, description = "Pan")
            )
        )
        advanceUntilIdle()
        viewModel.setDiceFilter(null)
        analytics.events.clear()

        viewModel.rollDice()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRolling)
        assertNull(state.result)
        assertEquals("Primero escoge desayuno, almuerzo o cena.", state.message)
        assertEquals(emptyList<String>(), analytics.events)
    }

    @Test
    fun `roll dice requires selecting an audience filter`() = runTest(dispatcher) {
        dao.seed(
            listOf(
                FoodMenu(id = 1, name = "Tostada", mealType = MealType.BREAKFAST, description = "Pan")
            )
        )
        advanceUntilIdle()
        viewModel.setDiceFilter(MealType.BREAKFAST)
        viewModel.setDiceAudienceFilter(null)
        analytics.events.clear()

        viewModel.rollDice()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRolling)
        assertNull(state.result)
        assertEquals("Selecciona si el menú es para persona adulta, peques o bebé.", state.message)
        assertEquals(emptyList<String>(), analytics.events)
    }

    @Test
    fun `missing audience while saving menu is tracked without menu content`() = runTest(dispatcher) {
        dietaryProfileStore.saveProfile(
            DietaryProfile(isEnabled = true, ageRange = MenuAudience.CHILD.defaultAgeRange),
            MenuAudience.CHILD
        )
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        viewModel.updateName("Tostada secreta")
        viewModel.updateDescription("Ingredientes privados")
        analytics.events.clear()

        viewModel.saveMenu()
        advanceUntilIdle()

        assertEquals(
            listOf("menu_save_blocked:missing_audience:true:true"),
            analytics.events
        )
    }

    @Test
    fun `dice audience filter selection is tracked`() = runTest(dispatcher) {
        dao.seed(listOf(FoodMenu(id = 1, name = "Tostada", mealType = MealType.BREAKFAST, description = "Pan")))
        advanceUntilIdle()
        analytics.events.clear()

        viewModel.setDiceAudienceFilter(MenuAudience.ADULT)

        assertEquals(listOf("audience_filter_selected:dice:ADULT:1"), analytics.events)
    }

    @Test
    fun `dietary profile updates are tracked by audience and field group only`() = runTest(dispatcher) {
        analytics.events.clear()

        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setDietaryProfileAudienceEnabled(true)
        viewModel.setDietaryProfileVegan(true)
        viewModel.updateDietaryProfileOtherAvoidances("texto privado")

        assertEquals(
            listOf(
                "dietary_profile_audience_selected:CHILD",
                "dietary_profile_updated:CHILD:enabled:2",
                "dietary_profile_updated:CHILD:vegan:2",
                "dietary_profile_updated:CHILD:other_avoidances:2"
            ),
            analytics.events
        )
    }

    @Test
    fun `menu edit and photo updates are tracked without menu ids or names`() = runTest(dispatcher) {
        val menu = FoodMenu(
            id = 1,
            name = "Nombre privado",
            mealType = MealType.LUNCH,
            audience = MenuAudience.ADULT,
            description = "Receta privada"
        )
        dao.seed(listOf(menu))
        advanceUntilIdle()
        analytics.events.clear()

        viewModel.startEditingMenu(menu)
        viewModel.updateEditName("Nombre nuevo privado")
        viewModel.saveEditedMenu()
        advanceUntilIdle()
        viewModel.updateMenuImageUri(menu.id, "content://private")
        advanceUntilIdle()

        assertEquals(
            listOf(
                "menu_edit_started:LUNCH:ADULT:false:false:1",
                "menu_edit_saved:LUNCH:ADULT:true:false:false:1",
                "menu_saved:LUNCH:false:false:1",
                "menu_inventory_changed:1:0:1",
                "menu_photo_updated:LUNCH:ADULT:true:1"
            ),
            analytics.events
        )
    }

    @Test
    fun `roll dice tracks filter result and menu count`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            todayProvider = { "2026-06-05" }
        )
        dao.seed(
            listOf(
                FoodMenu(id = 1, name = "Tostada", mealType = MealType.BREAKFAST, description = "Pan"),
                FoodMenu(id = 2, name = "Pasta", mealType = MealType.DINNER, description = "Pasta")
            )
        )
        advanceUntilIdle()
        viewModel.setDiceFilter(MealType.DINNER)
        viewModel.setDiceAudienceFilter(MenuAudience.ADULT)
        analytics.events.clear()

        viewModel.rollDice()
        advanceUntilIdle()

        assertEquals(
            listOf("dice_rolled:DINNER:true:DINNER:2:1"),
            analytics.events
        )
    }

    @Test
    fun `roll dice keeps rolling for the fixed animation duration`() = runTest(dispatcher) {
        dao.seed(
            listOf(
                FoodMenu(
                    id = 1,
                    name = "Tostada",
                    mealType = MealType.BREAKFAST,
                    audience = MenuAudience.ADULT,
                    description = "Pan"
                )
            )
        )
        advanceUntilIdle()
        viewModel.setDiceFilter(MealType.BREAKFAST)
        viewModel.setDiceAudienceFilter(MenuAudience.ADULT)

        viewModel.rollDice()
        runCurrent()

        assertTrue(viewModel.uiState.value.isRolling)
        assertNull(viewModel.uiState.value.result)

        advanceTimeBy(DICE_ROLL_DURATION_MILLIS - 1)
        runCurrent()

        assertTrue(viewModel.uiState.value.isRolling)
        assertNull(viewModel.uiState.value.result)

        advanceTimeBy(1)
        runCurrent()

        assertFalse(viewModel.uiState.value.isRolling)
        assertEquals("Tostada", viewModel.uiState.value.result?.name)
    }

    @Test
    fun `dice filter selection and empty result are tracked`() = runTest(dispatcher) {
        viewModel.setDiceFilter(MealType.DINNER)
        viewModel.rollDice()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "dice_filter_selected:DINNER:0",
                "dice_rolled:DINNER:false:NONE:0:0",
                "dice_empty_result:DINNER:0",
                "dice_empty_recovery:shown"
            ),
            analytics.events
        )
    }

    @Test
    fun `empty dice result exposes recovery with alternative meal for same audience`() = runTest(dispatcher) {
        dao.seed(
            listOf(
                FoodMenu(
                    id = 1,
                    name = "Cena adulta",
                    mealType = MealType.DINNER,
                    audience = MenuAudience.ADULT,
                    description = "Cena"
                )
            )
        )
        advanceUntilIdle()
        viewModel.setDiceFilter(MealType.BREAKFAST)
        viewModel.setDiceAudienceFilter(MenuAudience.ADULT)
        analytics.events.clear()

        viewModel.rollDice()
        advanceUntilIdle()

        val recovery = viewModel.uiState.value.diceEmptyRecovery
        assertEquals(MealType.BREAKFAST, recovery?.mealType)
        assertEquals(MenuAudience.ADULT, recovery?.audience)
        assertTrue(recovery?.canBroadenMealType == true)
        assertTrue(analytics.events.contains("dice_empty_recovery:shown"))
    }

    @Test
    fun `broadened dice roll preserves audience and selects another meal type`() = runTest(dispatcher) {
        dao.seed(
            listOf(
                FoodMenu(
                    id = 1,
                    name = "Cena adulta",
                    mealType = MealType.DINNER,
                    audience = MenuAudience.ADULT,
                    description = "Cena"
                ),
                FoodMenu(
                    id = 2,
                    name = "Cena bebé",
                    mealType = MealType.DINNER,
                    audience = MenuAudience.BABY,
                    description = "Cena"
                )
            )
        )
        advanceUntilIdle()
        viewModel.setDiceFilter(MealType.BREAKFAST)
        viewModel.setDiceAudienceFilter(MenuAudience.ADULT)
        viewModel.rollDice()
        advanceUntilIdle()

        viewModel.rollDiceAcrossMealTypesForSelectedAudience()
        advanceUntilIdle()

        assertEquals("Cena adulta", viewModel.uiState.value.result?.name)
        assertEquals(MenuAudience.ADULT, viewModel.uiState.value.diceAudienceFilter)
        assertEquals(MealType.BREAKFAST, viewModel.uiState.value.diceFilter)
    }

    @Test
    fun `changing filters dismisses empty recovery and tracks the action`() = runTest(dispatcher) {
        viewModel.setDiceFilter(MealType.DINNER)
        viewModel.rollDice()
        advanceUntilIdle()
        analytics.events.clear()

        viewModel.dismissDiceEmptyRecovery()

        assertNull(viewModel.uiState.value.diceEmptyRecovery)
        assertEquals(listOf("dice_empty_recovery:change_filters"), analytics.events)
    }

    @Test
    fun `empty recovery can generate ai with the same meal and audience`() = runTest(dispatcher) {
        analyzer.generatedMenu = GeneratedMenu(
            name = "Desayuno nuevo",
            description = "Avena y fruta",
            notes = "",
            calories = 380
        )
        viewModel.setDiceFilter(MealType.BREAKFAST)
        viewModel.setDiceAudienceFilter(MenuAudience.ADULT)
        viewModel.rollDice()
        advanceUntilIdle()
        analytics.events.clear()

        viewModel.generateAiFromDiceEmptyRecovery()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.diceEmptyRecovery)
        assertEquals(MealType.BREAKFAST, state.formMealType)
        assertEquals(MenuAudience.ADULT, state.formAudience)
        assertEquals("Desayuno nuevo", state.name)
        assertTrue(state.showGeneratedMenuDetail)
        assertTrue(analytics.events.contains("dice_empty_recovery:generate_ai"))
        assertEquals(1, analytics.events.count { it.startsWith("ai_menu_generation_started") })
    }

    @Test
    fun `opening menu card tracks meal type analysis state and inventory count`() = runTest(dispatcher) {
        dao.seed(
            listOf(
                FoodMenu(
                    id = 1,
                    name = "Tostada",
                    mealType = MealType.BREAKFAST,
                    description = "Pan",
                    healthAnalysis = HealthAnalysis(
                        status = HealthStatus.HEALTHY,
                        reason = "Equilibrado.",
                        suggestion = "Mantener."
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.trackMenuCardOpened(dao.saved.single().toDomain())

        assertEquals(
            listOf("menu_card_opened:BREAKFAST:true:1"),
            analytics.events
        )
    }

    @Test
    fun `delete menu tracks deleted menu event`() = runTest(dispatcher) {
        val menu = FoodMenu(
            id = 1,
            name = "Tostada",
            mealType = MealType.BREAKFAST,
            description = "Pan",
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Equilibrado.",
                suggestion = "Mantener."
            )
        )
        dao.seed(listOf(menu))

        viewModel.deleteMenu(menu)
        advanceUntilIdle()

        assertEquals(
            listOf(
                "menu_deleted:BREAKFAST:true",
                "menu_inventory_changed:0:0:0"
            ),
            analytics.events
        )
    }

    @Test
    fun `roll dice restarts exhausted filter instead of showing exhausted message`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            todayProvider = { "2026-06-03" }
        )
        dao.seed(
            listOf(
                FoodMenu(
                    id = 1,
                    name = "Tostada",
                    mealType = MealType.BREAKFAST,
                    description = "Pan",
                    lastPickedDate = "2026-06-03"
                ),
                FoodMenu(
                    id = 2,
                    name = "Yogur",
                    mealType = MealType.BREAKFAST,
                    description = "Yogur",
                    lastPickedDate = "2026-06-03"
                ),
                FoodMenu(
                    id = 3,
                    name = "Pasta",
                    mealType = MealType.DINNER,
                    description = "Pasta",
                    lastPickedDate = "2026-06-03"
                )
            )
        )
        advanceUntilIdle()
        viewModel.setDiceFilter(MealType.BREAKFAST)
        viewModel.setDiceAudienceFilter(MenuAudience.ADULT)

        viewModel.rollDice()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.message)
        assertEquals(MealType.BREAKFAST, state.result?.mealType)
        assertEquals(
            1,
            dao.saved
                .map { it.toDomain() }
                .count { it.mealType == MealType.BREAKFAST && it.lastPickedDate == "2026-06-03" }
        )
        assertEquals(
            "2026-06-03",
            dao.saved
                .map { it.toDomain() }
                .single { it.mealType == MealType.DINNER }
                .lastPickedDate
        )
    }
}

private class FakeMenuDao : MenuDao {
    private val menus = MutableStateFlow<List<MenuEntity>>(emptyList())
    val saved: List<MenuEntity>
        get() = menus.value

    fun seed(foodMenus: List<FoodMenu>) {
        menus.value = foodMenus.map { it.toEntity() }
    }

    override fun observeMenus(): Flow<List<MenuEntity>> = menus

    override suspend fun getPendingSyncMenus(): List<MenuEntity> {
        return menus.value.filter { it.remoteSyncState != RemoteSyncState.SYNCED.name }
    }

    override suspend fun countPendingSyncMenus(): Int {
        return getPendingSyncMenus().size
    }

    override suspend fun markUpsertSynced(id: Long, remoteSyncToken: String): Int {
        var updatedCount = 0
        menus.value = menus.value.map { existing ->
            if (
                existing.id == id &&
                existing.remoteSyncToken == remoteSyncToken &&
                existing.remoteSyncState == RemoteSyncState.PENDING_UPSERT.name
            ) {
                updatedCount += 1
                existing.copy(
                    remoteSyncState = RemoteSyncState.SYNCED.name,
                    remoteSyncToken = null
                )
            } else {
                existing
            }
        }
        return updatedCount
    }

    override suspend fun markVisibleMenusPendingUpsert(updatedAt: Long): Int {
        var updatedCount = 0
        menus.value = menus.value.map { existing ->
            if (existing.deletedAt == null) {
                updatedCount += 1
                existing.copy(
                    remoteSyncState = RemoteSyncState.PENDING_UPSERT.name,
                    updatedAt = updatedAt,
                    remoteSyncToken = null
                )
            } else {
                existing
            }
        }
        return updatedCount
    }

    override suspend fun deletePendingTombstone(id: Long, deletedAt: Long): Int {
        val initialSize = menus.value.size
        menus.value = menus.value.filterNot { entity ->
            entity.id == id &&
                entity.remoteSyncState == RemoteSyncState.PENDING_DELETE.name &&
                entity.deletedAt == deletedAt
        }
        return initialSize - menus.value.size
    }

    override suspend fun insert(menu: MenuEntity): Long {
        val nextId = (menus.value.maxOfOrNull { it.id } ?: 0L) + 1L
        menus.value = listOf(menu.copy(id = nextId)) + menus.value
        return nextId
    }

    override suspend fun update(menu: MenuEntity) {
        menus.value = menus.value.map { existing ->
            if (existing.id == menu.id) menu else existing
        }
    }

    override suspend fun delete(menu: MenuEntity) {
        menus.value = menus.value.filterNot { it.id == menu.id }
    }
}

private class RecordingHealthAnalyzer : HealthAnalyzer {
    var wasCalled = false
    var analysisCalls = 0
    var generateCalls = 0
    var batchAnalyzeCalls = 0
    var requestedMealType: MealType? = null
    var requestedDietaryProfile: DietaryProfile? = null
    var requestedBaseIngredients: String = ""
    var avoidIdeas: List<String> = emptyList()
    var analysisFailure: Throwable? = null
    var analysisCalories: Int? = null
    var batchAnalysisFailure: Throwable? = null
    var batchAnalyses: Map<Long, HealthAnalysis> = emptyMap()
    var batchAnalyzeMenuIds: List<Long> = emptyList()
    var generateFailure: Throwable? = null
    var requestedAudience: MenuAudience? = null
    var requestedLanguage: AppLanguage? = null
    var requestedCuisineInspiration: CuisineInspiration? = null
    var analysisDelayMillis: Long = 0L
    var generateDelayMillis: Long = 0L
    var generatedMenu = GeneratedMenu(
        name = "Tostada",
        description = "Pan integral y huevo.",
        notes = "Simple.",
        calories = 350
    )

    override suspend fun analyze(menu: FoodMenu, language: AppLanguage): Result<MenuAiDetails> {
        wasCalled = true
        analysisCalls += 1
        requestedLanguage = language
        if (analysisDelayMillis > 0L) {
            delay(analysisDelayMillis)
        }
        analysisFailure?.let { return Result.failure(it) }
        return Result.success(
            MenuAiDetails(
                healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Equilibrado.",
                suggestion = "Mantener variedad.",
                calories = analysisCalories
                ),
                shoppingProducts = emptyList()
            )
        )
    }

    override suspend fun analyzeBatch(menus: List<FoodMenu>, language: AppLanguage): Result<Map<Long, MenuAiDetails>> {
        batchAnalyzeCalls += 1
        requestedLanguage = language
        batchAnalyzeMenuIds = menus.map { it.id }
        batchAnalysisFailure?.let { return Result.failure(it) }
        return Result.success(
            batchAnalyses.mapValues { (_, analysis) ->
                MenuAiDetails(
                    healthAnalysis = analysis,
                    shoppingProducts = emptyList()
                )
            }
        )
    }

    override suspend fun generateMenu(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile,
        audience: MenuAudience,
        baseIngredients: String,
        language: AppLanguage,
        cuisineInspiration: CuisineInspiration
    ): Result<GeneratedMenu> {
        generateCalls += 1
        requestedMealType = mealType
        requestedDietaryProfile = dietaryProfile
        requestedAudience = audience
        requestedBaseIngredients = baseIngredients
        requestedLanguage = language
        requestedCuisineInspiration = cuisineInspiration
        this.avoidIdeas = avoidIdeas
        if (generateDelayMillis > 0L) {
            delay(generateDelayMillis)
        }
        generateFailure?.let { return Result.failure(it) }
        return Result.success(generatedMenu)
    }
}

private class FakeCuisineRotationStateStore : CuisineRotationStateStore {
    private val indices = mutableMapOf<String, Int>()

    override fun readIndex(key: String): Int? = indices[key]

    override fun writeIndex(key: String, index: Int) {
        indices[key] = index
    }
}

private class FakeDietaryProfileStore : DietaryProfileStore {
    private val storedProfiles = mutableMapOf(MenuAudience.ADULT to defaultProfile(MenuAudience.ADULT))
    var storedProfile: DietaryProfile
        get() = getProfile(MenuAudience.ADULT)
        set(value) {
            storedProfiles[MenuAudience.ADULT] = value
        }

    override fun getProfile(audience: MenuAudience): DietaryProfile {
        return storedProfiles[audience] ?: defaultProfile(audience)
    }

    override fun saveProfile(profile: DietaryProfile, audience: MenuAudience) {
        storedProfiles[audience] = profile
    }

    private fun defaultProfile(audience: MenuAudience): DietaryProfile {
        return DietaryProfile(
            isEnabled = audience == MenuAudience.ADULT,
            ageRange = audience.defaultAgeRange
        )
    }
}

private class FakeOnboardingStore(
    var completed: Boolean = false,
    var completedVersion: Int = if (completed) 5 else 0
) : OnboardingStore {
    override fun isOnboardingCompleted(requiredVersion: Int): Boolean =
        completed && completedVersion >= requiredVersion

    override fun markOnboardingCompleted(version: Int) {
        completed = true
        completedVersion = version
    }
}

private class FakeAiQuotaRetryStore : AiQuotaRetryStore {
    var storedRetryAtMillis: Long? = null
    var storedConsecutiveFailures = 0

    override fun getRetryState() = storedRetryAtMillis?.let { retryAtMillis ->
        AiQuotaRetryState(
            retryAtMillis = retryAtMillis,
            consecutiveFailures = storedConsecutiveFailures
        )
    }

    override fun saveRetryState(state: AiQuotaRetryState) {
        storedRetryAtMillis = state.retryAtMillis
        storedConsecutiveFailures = state.consecutiveFailures
    }

    override fun clearRetryState() {
        storedRetryAtMillis = null
        storedConsecutiveFailures = 0
    }
}

private class FakeAiRequestThrottleStore : AiRequestThrottleStore {
    var storedLastRequestAtMillis: Long? = null

    override fun getLastRequestAtMillis(): Long? = storedLastRequestAtMillis

    override fun saveLastRequestAtMillis(requestAtMillis: Long) {
        storedLastRequestAtMillis = requestAtMillis
    }

    override fun clearLastRequest() {
        storedLastRequestAtMillis = null
    }
}

private class FakeAiDailyUsageStore : AiDailyUsageStore {
    var storedDateKey: String? = null
    var storedUsedCount = 0

    override fun getUsageState(): AiDailyUsageState? {
        return storedDateKey?.let { dateKey ->
            AiDailyUsageState(
                dateKey = dateKey,
                usedCount = storedUsedCount
            )
        }
    }

    override fun saveUsageState(state: AiDailyUsageState) {
        storedDateKey = state.dateKey
        storedUsedCount = state.usedCount
    }
}

private class FakeGuestUsageStore : GuestUsageStore {
    var state: GuestDailyUsageState? = null

    override fun getUsageState(): GuestDailyUsageState? = state

    override fun saveUsageState(state: GuestDailyUsageState) {
        this.state = state
    }
}

private class RecordingMenuDadoAnalytics : MenuDadoAnalytics {
    val events = mutableListOf<String>()
    var throwOnAiMenuGenerationFinished = false

    override fun trackAppOpened(deviceInfo: DeviceInfo?) {
        events += "app_opened"
    }

    override fun trackMenuSaved(
        mealType: MealType,
        hasAiAnalysis: Boolean,
        hasCalories: Boolean,
        menuCount: Int
    ) {
        events += "menu_saved:${mealType.name}:$hasAiAnalysis:$hasCalories:$menuCount"
    }

    override fun trackCtaTapped(screen: String, cta: String) {
        events += "cta_tapped:$screen:$cta"
    }

    override fun trackMyZoneOpened(authMode: String, menuCount: Int) {
        events += "my_zone_opened:$authMode:$menuCount"
    }

    override fun trackAuthFlowStarted(mode: String, authMode: String, menuCount: Int) {
        events += "auth_flow_started:$mode:$authMode:$menuCount"
    }

    override fun trackAuthAction(action: String, method: String, authMode: String) {
        events += "auth_action:$action:$method:$authMode"
    }

    override fun trackGuestLimitReached(limitType: String, usedCount: Int) {
        events += "guest_limit_reached:$limitType:$usedCount"
    }

    override fun trackMenuDeleted(mealType: MealType, hadAiAnalysis: Boolean) {
        events += "menu_deleted:${mealType.name}:$hadAiAnalysis"
    }

    override fun trackFirstMenuCreated(mealType: MealType) {
        events += "first_menu_created:${mealType.name}"
    }

    override fun trackMenuInventoryChanged(menuCount: Int, analyzedMenuCount: Int, pendingAnalysisCount: Int) {
        events += "menu_inventory_changed:$menuCount:$analyzedMenuCount:$pendingAnalysisCount"
    }

    override fun trackMenuFormStarted(firstEditedField: String, mealType: MealType) {
        events += "menu_form_started:$firstEditedField:${mealType.name}"
    }

    override fun trackMealTypeSelected(mealType: MealType, formHasContent: Boolean) {
        events += "meal_type_selected:${mealType.name}:$formHasContent"
    }

    override fun trackAudienceFilterSelected(source: String, audience: MenuAudience?, menuCount: Int) {
        events += "audience_filter_selected:$source:${audience?.name ?: "ALL"}:$menuCount"
    }

    override fun trackMenuSaveBlocked(reason: String, hasName: Boolean, hasDescription: Boolean) {
        events += "menu_save_blocked:$reason:$hasName:$hasDescription"
    }

    override fun trackMenuEditStarted(
        mealType: MealType,
        audience: MenuAudience,
        hasAiAnalysis: Boolean,
        hasPhoto: Boolean,
        menuCount: Int
    ) {
        events += "menu_edit_started:${mealType.name}:${audience.name}:$hasAiAnalysis:$hasPhoto:$menuCount"
    }

    override fun trackMenuEditSaved(
        mealType: MealType,
        audience: MenuAudience,
        changedRecipe: Boolean,
        hasAiAnalysis: Boolean,
        hasPhoto: Boolean,
        menuCount: Int
    ) {
        events += "menu_edit_saved:${mealType.name}:${audience.name}:$changedRecipe:$hasAiAnalysis:$hasPhoto:$menuCount"
    }

    override fun trackMenuPhotoUpdated(mealType: MealType, audience: MenuAudience, hasPhoto: Boolean, menuCount: Int) {
        events += "menu_photo_updated:${mealType.name}:${audience.name}:$hasPhoto:$menuCount"
    }

    override fun trackDiceRolled(
        filter: MealType?,
        resultMealType: MealType?,
        menuCount: Int,
        availableCandidateCount: Int
    ) {
        events += "dice_rolled:${filter?.name ?: "ALL"}:${resultMealType != null}:${resultMealType?.name ?: "NONE"}:$menuCount:$availableCandidateCount"
    }

    override fun trackDiceFilterSelected(filter: MealType?, menuCount: Int) {
        events += "dice_filter_selected:${filter?.name ?: "ALL"}:$menuCount"
    }

    override fun trackDiceEmptyResult(filter: MealType?, availableCandidateCount: Int) {
        events += "dice_empty_result:${filter?.name ?: "ALL"}:$availableCandidateCount"
    }

    override fun trackDiceEmptyRecovery(action: String) {
        events += "dice_empty_recovery:$action"
    }

    override fun trackMenuCardOpened(mealType: MealType, hasAiAnalysis: Boolean, menuCount: Int) {
        events += "menu_card_opened:${mealType.name}:$hasAiAnalysis:$menuCount"
    }

    override fun trackOnboardingShown() {
        events += "onboarding_shown"
    }

    override fun trackOnboardingCompleted(action: String) {
        events += "onboarding_completed:$action"
    }

    override fun trackAppUpdatePrompt(action: String) {
        events += "app_update_prompt:$action"
    }

    override fun trackAboutAppOpened() {
        events += "about_app_opened"
    }

    override fun trackDietaryProfileOpened(activeAudienceCount: Int) {
        events += "dietary_profile_opened:$activeAudienceCount"
    }

    override fun trackDietaryProfileAudienceSelected(audience: MenuAudience) {
        events += "dietary_profile_audience_selected:${audience.name}"
    }

    override fun trackDietaryProfileUpdated(audience: MenuAudience, fieldGroup: String, activeAudienceCount: Int) {
        events += "dietary_profile_updated:${audience.name}:$fieldGroup:$activeAudienceCount"
    }

    override fun trackMenuListViewMoreOpened(audience: MenuAudience, menuCount: Int) {
        events += "menu_list_view_more_opened:${audience.name}:$menuCount"
    }

    override fun trackBackendSyncRetried(source: String, pendingMenuCount: Int) {
        events += "backend_sync_retried:$source:$pendingMenuCount"
    }

    override fun trackBackendSyncFinished(source: String, status: String, pendingMenuCount: Int) {
        events += "backend_sync_finished:$source:$status:$pendingMenuCount"
    }

    override fun trackAiMenuGenerationStarted(mealType: MealType, avoidIdeaCount: Int) {
        events += "ai_menu_generation_started:${mealType.name}:$avoidIdeaCount"
    }

    override fun trackAiMenuGenerationFinished(
        mealType: MealType,
        success: Boolean,
        healthStatus: HealthStatus?,
        failureType: String?
    ) {
        if (throwOnAiMenuGenerationFinished) {
            throw IllegalStateException("tracking failed")
        }
        events += "ai_menu_generation_finished:${mealType.name}:$success:${healthStatus?.name?.lowercase() ?: "unknown"}:${failureType ?: "none"}"
    }

    override fun trackAiAnalysisStarted(scope: String, mealType: MealType?, menuCount: Int) {
        events += "ai_analysis_started:$scope:${mealType?.name ?: "UNKNOWN"}:$menuCount"
    }

    override fun trackAiAnalysisFinished(
        scope: String,
        mealType: MealType?,
        success: Boolean,
        analyzedCount: Int,
        healthStatus: HealthStatus?,
        failureType: String?
    ) {
        events += "ai_analysis_finished:$scope:${mealType?.name ?: "UNKNOWN"}:$success:$analyzedCount:${healthStatus?.name?.lowercase() ?: "unknown"}:${failureType ?: "none"}"
    }

    override fun trackAiDailyLimitReached(source: String) {
        events += "ai_daily_limit_reached:$source"
    }
}
