package com.menudado.ui

import com.menudado.analytics.MenuDadoAnalytics
import com.menudado.analytics.DeviceInfo
import com.menudado.ai.HealthAnalyzer
import com.menudado.data.DietaryProfileStore
import com.menudado.data.AiDailyUsageState
import com.menudado.data.AiDailyUsageStore
import com.menudado.data.AiQuotaRetryStore
import com.menudado.data.AiQuotaRetryState
import com.menudado.data.MenuDao
import com.menudado.data.MenuEntity
import com.menudado.data.MenuRepository
import com.menudado.data.OnboardingStore
import com.menudado.data.toEntity
import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MenuDadoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeMenuDao
    private lateinit var analyzer: RecordingHealthAnalyzer
    private lateinit var aiQuotaRetryStore: FakeAiQuotaRetryStore
    private lateinit var aiDailyUsageStore: FakeAiDailyUsageStore
    private lateinit var dietaryProfileStore: FakeDietaryProfileStore
    private lateinit var onboardingStore: FakeOnboardingStore
    private lateinit var analytics: RecordingMenuDadoAnalytics
    private lateinit var viewModel: MenuDadoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeMenuDao()
        analyzer = RecordingHealthAnalyzer()
        aiQuotaRetryStore = FakeAiQuotaRetryStore()
        aiDailyUsageStore = FakeAiDailyUsageStore()
        dietaryProfileStore = FakeDietaryProfileStore()
        onboardingStore = FakeOnboardingStore(completed = true)
        analytics = RecordingMenuDadoAnalytics()
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        viewModel.setFormMealType(MealType.BREAKFAST)
        analytics.events.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `form starts without a selected meal type`() = runTest(dispatcher) {
        val freshViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )

        assertNull(freshViewModel.uiState.value.formMealType)
    }

    @Test
    fun `save menu requires selecting a meal type`() = runTest(dispatcher) {
        val freshViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        freshViewModel.updateName("Tostadas")
        freshViewModel.updateDescription("Pan, tomate y aguacate")
        analytics.events.clear()

        freshViewModel.saveMenu()
        advanceUntilIdle()

        assertEquals(emptyList<FoodMenu>(), dao.saved.map { it.toDomain() })
        assertEquals("Selecciona si es desayuno, almuerzo o cena.", freshViewModel.uiState.value.message)
        assertEquals(
            listOf("menu_save_blocked:missing_meal_type:true:true"),
            analytics.events
        )
    }

    @Test
    fun `generate menu idea requires selecting a meal type before using IA`() = runTest(dispatcher) {
        val freshViewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            aiQuotaRetryStore = aiQuotaRetryStore,
            aiDailyUsageStore = aiDailyUsageStore,
            dietaryProfileStore = dietaryProfileStore,
            onboardingStore = onboardingStore
        )
        freshViewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals(20, freshViewModel.uiState.value.aiUsesRemainingToday)
        assertEquals(0, aiDailyUsageStore.storedUsedCount)
        assertEquals("Selecciona si es desayuno, almuerzo o cena.", freshViewModel.uiState.value.message)
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
        assertEquals("Pan, tomate y aguacate", saved.description)
        assertNull(saved.healthAnalysis)
        assertFalse(analyzer.wasCalled)
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
            "Modificaste la receta generada. Para verla como analizada, guarda el menu y toca Analizar IA.",
            viewModel.uiState.value.message
        )
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

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(19, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals(1, aiDailyUsageStore.storedUsedCount)
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
        analytics.events.clear()

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals(0, viewModel.uiState.value.aiUsesRemainingToday)
        assertEquals("La ayuda con IA gratuita de hoy se agoto. Tus menus siguen disponibles y podras volver a probar mas adelante.", viewModel.uiState.value.message)
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
    fun `generate menu idea sends dietary profile to IA`() = runTest(dispatcher) {
        viewModel.setDietaryProfileVegan(true)
        viewModel.setDietaryProfileHasAllergies(true)
        viewModel.toggleDietaryAllergen(DietaryAllergen.SOY)
        viewModel.updateDietaryProfileOtherAvoidances("picante")

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            DietaryProfile(
                isVegan = true,
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.SOY),
                otherAvoidances = "picante"
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
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 57s.")

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            "La IA esta tomando una pausa para evitar intentos fallidos. Tus menus siguen disponibles.",
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
        analyzer.generateFailure = IllegalStateException(
            "Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 20. Please retry in 21s."
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            "La IA recibio varias peticiones seguidas. Espera un momento antes de volver a intentarlo.",
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
        analyzer.generateFailure = IllegalStateException(
            "RESOURCE_EXHAUSTED quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_input_tokens. Please retry in 21s."
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            "La idea necesita un descanso antes de procesarse de nuevo. Puedes seguir usando tus menus.",
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
        analyzer.generateFailure = IllegalStateException(
            "RESOURCE_EXHAUSTED quota exceeded for metric: Requests per day, limit: 20."
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(
            "La ayuda con IA gratuita de hoy se agoto. Tus menus siguen disponibles y podras volver a probar mas adelante.",
            viewModel.uiState.value.message
        )
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
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 57s.")

        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.clearMessage()
        nowMillis = 120_000L
        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(1, analyzer.generateCalls)
        assertEquals(
            "La IA esta tomando una pausa para evitar intentos fallidos. Tus menus siguen disponibles.",
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

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals(
            "La IA esta tomando una pausa para evitar intentos fallidos. Tus menus siguen disponibles.",
            viewModel.uiState.value.message
        )
        assertEquals(159_000L, viewModel.uiState.value.aiRetryAtMillis)
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
            "La IA esta tomando una pausa para evitar intentos fallidos. Tus menus siguen disponibles.",
            viewModel.uiState.value.message
        )
        assertEquals(295_000L, viewModel.uiState.value.aiRetryAtMillis)
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
    fun `analyze pending menus uses one IA call and saves valid results`() = runTest(dispatcher) {
        viewModel = MenuDadoViewModel(
            repository = MenuRepository(dao, analyzer),
            analytics = analytics,
            clockMillisProvider = { 100_000L },
            aiQuotaRetryStore = aiQuotaRetryStore,
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
        analyzer.generateFailure = IllegalStateException("Quota exceeded. Please retry in 57s.")
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isAiRetryNoticeVisible)

        viewModel.saveMenu()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Agrega nombre e ingredientes para guardar el menu.", state.message)
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

        viewModel.rollDice()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRolling)
        assertNull(state.result)
        assertEquals("Primero escoge desayuno, almuerzo o cena.", state.message)
        assertEquals(emptyList<String>(), analytics.events)
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
        analytics.events.clear()

        viewModel.rollDice()
        advanceUntilIdle()

        assertEquals(
            listOf("dice_rolled:DINNER:true:DINNER:2:1"),
            analytics.events
        )
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
                "dice_empty_result:DINNER:0"
            ),
            analytics.events
        )
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
    var generatedMenu = GeneratedMenu(
        name = "Tostada",
        description = "Pan integral y huevo.",
        notes = "Simple.",
        calories = 350
    )

    override suspend fun analyze(menu: FoodMenu): Result<HealthAnalysis> {
        wasCalled = true
        analysisFailure?.let { return Result.failure(it) }
        return Result.success(
            HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Equilibrado.",
                suggestion = "Mantener variedad.",
                calories = analysisCalories
            )
        )
    }

    override suspend fun analyzeBatch(menus: List<FoodMenu>): Result<Map<Long, HealthAnalysis>> {
        batchAnalyzeCalls += 1
        batchAnalyzeMenuIds = menus.map { it.id }
        batchAnalysisFailure?.let { return Result.failure(it) }
        return Result.success(batchAnalyses)
    }

    override suspend fun generateMenu(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile,
        baseIngredients: String
    ): Result<GeneratedMenu> {
        generateCalls += 1
        requestedMealType = mealType
        requestedDietaryProfile = dietaryProfile
        requestedBaseIngredients = baseIngredients
        this.avoidIdeas = avoidIdeas
        generateFailure?.let { return Result.failure(it) }
        return Result.success(generatedMenu)
    }
}

private class FakeDietaryProfileStore : DietaryProfileStore {
    var storedProfile = DietaryProfile()

    override fun getProfile(): DietaryProfile = storedProfile

    override fun saveProfile(profile: DietaryProfile) {
        this.storedProfile = profile
    }
}

private class FakeOnboardingStore(
    var completed: Boolean = false
) : OnboardingStore {
    override fun isOnboardingCompleted(): Boolean = completed

    override fun markOnboardingCompleted() {
        completed = true
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

private class RecordingMenuDadoAnalytics : MenuDadoAnalytics {
    val events = mutableListOf<String>()

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

    override fun trackMenuSaveBlocked(reason: String, hasName: Boolean, hasDescription: Boolean) {
        events += "menu_save_blocked:$reason:$hasName:$hasDescription"
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

    override fun trackMenuCardOpened(mealType: MealType, hasAiAnalysis: Boolean, menuCount: Int) {
        events += "menu_card_opened:${mealType.name}:$hasAiAnalysis:$menuCount"
    }

    override fun trackOnboardingShown() {
        events += "onboarding_shown"
    }

    override fun trackOnboardingCompleted(action: String) {
        events += "onboarding_completed:$action"
    }

    override fun trackAboutAppOpened() {
        events += "about_app_opened"
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
