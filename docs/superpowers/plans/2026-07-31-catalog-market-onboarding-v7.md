# Catalog, Market, About, and Onboarding V7 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add privacy-safe CTA markers for the saved-menu catalog and missing Market interactions, then update About and the one-time onboarding to version 7 in all supported languages.

**Architecture:** Keep Firebase Analytics behind the existing `MenuDadoViewModel.trackCtaTapped` entry point. Add a small catalog analytics contract containing only closed identifiers, let reusable Compose components report typed UI interactions to `MenuDadoScreen`, and keep all user/profile values out of event calls. Update localized resource fallbacks and the existing onboarding version gate without changing navigation, filtering, Market data, or Firestore schemas.

**Tech Stack:** Kotlin, Jetpack Compose, Firebase Analytics, Firebase Remote Config, JUnit 4, AndroidX Compose UI tests, Gradle.

---

## File Map

- Create `app/src/main/java/com/menudado/ui/MenuCatalogAnalytics.kt`: closed catalog screen/CTA identifiers and blank-to-nonblank search transition logic.
- Create `app/src/test/java/com/menudado/ui/MenuCatalogAnalyticsTest.kt`: unit contract for identifier stability and search-event throttling.
- Modify `app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt`: separate explicit clear-button callback from text editing.
- Modify `app/src/main/java/com/menudado/ui/MenuCatalogFilterSheet.kt`: report typed filter interactions without knowing Firebase.
- Modify `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: map catalog and Market callbacks to `trackCtaTapped` and preserve existing state mutations.
- Modify `app/src/main/java/com/menudado/ui/MarketManagementSheet.kt`: closed CTA helpers for missing Market interactions.
- Modify `app/src/androidTest/java/com/menudado/ui/MenuCatalogSearchBarTest.kt`: verify clear and filter callbacks remain distinct.
- Modify `app/src/androidTest/java/com/menudado/ui/MenuCatalogFilterSheetTest.kt`: verify filter choices report typed interactions.
- Modify `app/src/androidTest/java/com/menudado/ui/MarketClearConfirmationDialogTest.kt`: verify expanded/collapsed purchased-section callbacks.
- Modify `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: verify stable Market and onboarding UI identifiers.
- Modify `app/src/main/java/com/menudado/about/MenuDadoAboutRemoteConfig.kt`: advance the versioned About description key to `v3`.
- Modify `app/src/test/java/com/menudado/about/MenuDadoAboutRemoteConfigTest.kt`: lock the `v3` key and fallback behavior.
- Modify `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`: advance onboarding content version to `7`.
- Modify `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`: verify version-7 new-install and upgrade behavior.
- Modify `app/src/test/java/com/menudado/analytics/FirebaseMenuDadoAnalyticsTest.kt`: lock the `v7` analytics dimension.
- Modify `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`: lock approved About/onboarding copy in every locale.
- Modify `app/src/main/res/values/strings.xml`: Spanish About and onboarding copy.
- Modify `app/src/main/res/values-en/strings.xml`: English About and onboarding copy.
- Modify `app/src/main/res/values-fr/strings.xml`: French About and onboarding copy.
- Modify `docs/project-context.md`: document CTA coverage, About `v3`, and onboarding version `7`.

### Task 1: Define the privacy-safe catalog analytics contract

**Files:**
- Create: `app/src/test/java/com/menudado/ui/MenuCatalogAnalyticsTest.kt`
- Create: `app/src/main/java/com/menudado/ui/MenuCatalogAnalytics.kt`

- [ ] **Step 1: Write the failing catalog analytics tests**

```kotlin
package com.menudado.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuCatalogAnalyticsTest {
    @Test
    fun `catalog CTA values stay closed and stable`() {
        assertEquals("menu_catalog", MENU_CATALOG_ANALYTICS_SCREEN)
        assertEquals("search_started", MenuCatalogAnalyticsAction.SEARCH_STARTED.cta)
        assertEquals("clear_search", MenuCatalogAnalyticsAction.CLEAR_SEARCH.cta)
        assertEquals("open_filters", MenuCatalogAnalyticsAction.OPEN_FILTERS.cta)
        assertEquals("clear_search_and_filters", MenuCatalogAnalyticsAction.CLEAR_SEARCH_AND_FILTERS.cta)
        assertEquals(
            "select_audience_filter",
            MenuCatalogFilterInteraction.AUDIENCE_SELECTED.analyticsAction.cta
        )
        assertEquals(
            "select_dietary_filter",
            MenuCatalogFilterInteraction.DIETARY_NEED_SELECTED.analyticsAction.cta
        )
        assertEquals(
            "toggle_favorites_only",
            MenuCatalogFilterInteraction.FAVORITES_ONLY_TOGGLED.analyticsAction.cta
        )
        assertEquals(
            "toggle_healthy_only",
            MenuCatalogFilterInteraction.HEALTHY_ONLY_TOGGLED.analyticsAction.cta
        )
    }

    @Test
    fun `search emits only when a blank query becomes nonblank`() {
        assertEquals("search_started", menuCatalogSearchTransitionCta("", "arroz"))
        assertEquals("search_started", menuCatalogSearchTransitionCta("   ", "tomate"))
        assertNull(menuCatalogSearchTransitionCta("arroz", "arroz con pollo"))
        assertNull(menuCatalogSearchTransitionCta("arroz", ""))
        assertNull(menuCatalogSearchTransitionCta("", "   "))
    }
}
```

- [ ] **Step 2: Run the test and confirm the contract does not exist yet**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCatalogAnalyticsTest
```

Expected: FAIL at Kotlin compilation because `MenuCatalogAnalyticsAction`,
`MenuCatalogFilterInteraction`, and `menuCatalogSearchTransitionCta` are not
defined.

- [ ] **Step 3: Add the minimal closed analytics contract**

```kotlin
package com.menudado.ui

internal const val MENU_CATALOG_ANALYTICS_SCREEN = "menu_catalog"

internal enum class MenuCatalogAnalyticsAction(val cta: String) {
    SEARCH_STARTED("search_started"),
    CLEAR_SEARCH("clear_search"),
    OPEN_FILTERS("open_filters"),
    SELECT_AUDIENCE_FILTER("select_audience_filter"),
    SELECT_DIETARY_FILTER("select_dietary_filter"),
    TOGGLE_FAVORITES_ONLY("toggle_favorites_only"),
    TOGGLE_HEALTHY_ONLY("toggle_healthy_only"),
    CLEAR_SEARCH_AND_FILTERS("clear_search_and_filters")
}

internal enum class MenuCatalogFilterInteraction(
    val analyticsAction: MenuCatalogAnalyticsAction
) {
    AUDIENCE_SELECTED(MenuCatalogAnalyticsAction.SELECT_AUDIENCE_FILTER),
    DIETARY_NEED_SELECTED(MenuCatalogAnalyticsAction.SELECT_DIETARY_FILTER),
    FAVORITES_ONLY_TOGGLED(MenuCatalogAnalyticsAction.TOGGLE_FAVORITES_ONLY),
    HEALTHY_ONLY_TOGGLED(MenuCatalogAnalyticsAction.TOGGLE_HEALTHY_ONLY)
}

internal fun menuCatalogSearchTransitionCta(
    previousQuery: String,
    newQuery: String
): String? = MenuCatalogAnalyticsAction.SEARCH_STARTED.cta.takeIf {
    previousQuery.isBlank() && newQuery.isNotBlank()
}
```

- [ ] **Step 4: Run the focused test**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCatalogAnalyticsTest
```

Expected: PASS, with two tests and no query value in any returned CTA.

- [ ] **Step 5: Commit the contract**

```bash
git add app/src/main/java/com/menudado/ui/MenuCatalogAnalytics.kt app/src/test/java/com/menudado/ui/MenuCatalogAnalyticsTest.kt
git commit -m "test: define privacy-safe catalog CTA contract"
```

### Task 2: Wire catalog search, filters, and reset CTAs

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuCatalogFilterSheet.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/androidTest/java/com/menudado/ui/MenuCatalogSearchBarTest.kt`
- Modify: `app/src/androidTest/java/com/menudado/ui/MenuCatalogFilterSheetTest.kt`

- [ ] **Step 1: Make the search-bar UI test require a distinct clear callback**

Update the first `MenuCatalogSearchBarTest` case to collect clear taps
separately:

```kotlin
var query by mutableStateOf("")
var clearClicks = 0
var filterClicks = 0
composeRule.setContent {
    MaterialTheme {
        MenuCatalogSearchBar(
            query = query,
            onQueryChanged = { query = it },
            onClearSearch = {
                clearClicks += 1
                query = ""
            },
            activeFilterCount = 2,
            onOpenFilters = { filterClicks += 1 }
        )
    }
}
```

Extend its final assertion:

```kotlin
assertEquals("", query)
assertEquals(1, clearClicks)
assertEquals(1, filterClicks)
```

Add `onClearSearch = {}` to the other two test constructors.

- [ ] **Step 2: Make the filter-sheet UI test require typed interaction callbacks**

Add `onInteraction = {}` to existing `MenuCatalogFilterSheet` calls. In
`favoritesScopeCanSelectAudienceWithoutOfferingFavoriteAgain`, collect the
audience interaction:

```kotlin
val interactions = mutableListOf<MenuCatalogFilterInteraction>()
```

Pass `onInteraction = interactions::add` and extend its final assertion:

```kotlin
assertEquals(MenuAudience.ADULT, changed?.favoriteAudience)
assertEquals(
    listOf(MenuCatalogFilterInteraction.AUDIENCE_SELECTED),
    interactions
)
```

Then add this test for the remaining applicable filter actions:

```kotlin
@Test
fun filterChoicesReportClosedInteractions() {
    val interactions = mutableListOf<MenuCatalogFilterInteraction>()
    var filters = MenuCatalogFilters()
    composeRule.setContent {
        MaterialTheme {
            MenuCatalogFilterSheet(
                scope = MenuCatalogScope.Audience(MenuAudience.ADULT),
                filters = filters,
                dietaryProfiles = profiles(),
                onFiltersChanged = { filters = it },
                onInteraction = interactions::add,
                onDismiss = {}
            )
        }
    }

    composeRule.onNodeWithText(
        string(R.string.menu_catalog_filter_favorites)
    ).performClick()
    composeRule.onNodeWithText(
        string(R.string.menu_catalog_filter_healthy)
    ).performClick()
    composeRule.onNodeWithText(
        string(R.string.menu_catalog_filter_need)
    ).performClick()
    composeRule.onNodeWithText(
        string(R.string.menu_catalog_need_none)
    ).performClick()

    composeRule.runOnIdle {
        assertEquals(
            listOf(
                MenuCatalogFilterInteraction.FAVORITES_ONLY_TOGGLED,
                MenuCatalogFilterInteraction.HEALTHY_ONLY_TOGGLED,
                MenuCatalogFilterInteraction.DIETARY_NEED_SELECTED
            ),
            interactions
        )
    }
}
```

- [ ] **Step 3: Compile Android tests and confirm the new callback contracts fail**

Run:

```bash
./gradlew :app:compileDebugAndroidTestSources
```

Expected: FAIL because `onClearSearch` and `onInteraction` do not exist.

- [ ] **Step 4: Add explicit component callbacks**

Change `MenuCatalogSearchBar` to accept:

```kotlin
onQueryChanged: (String) -> Unit,
onClearSearch: () -> Unit,
activeFilterCount: Int,
onOpenFilters: () -> Unit
```

and replace the clear icon handler with:

```kotlin
IconButton(
    onClick = onClearSearch,
    modifier = Modifier.size(48.dp)
)
```

Change `MenuCatalogFilterSheet` and `MenuCatalogFilterSheetHost` to accept and
forward:

```kotlin
onInteraction: (MenuCatalogFilterInteraction) -> Unit
```

Emit the typed callback immediately before the existing state change:

```kotlin
onInteraction(MenuCatalogFilterInteraction.AUDIENCE_SELECTED)
onFiltersChanged(menuCatalogFiltersAfterFavoriteAudienceSelected(filters, audience))
```

```kotlin
onInteraction(MenuCatalogFilterInteraction.DIETARY_NEED_SELECTED)
onFiltersChanged(filters.copy(dietaryNeed = need))
```

```kotlin
onInteraction(MenuCatalogFilterInteraction.FAVORITES_ONLY_TOGGLED)
onFiltersChanged(filters.copy(favoritesOnly = !filters.favoritesOnly))
```

```kotlin
onInteraction(MenuCatalogFilterInteraction.HEALTHY_ONLY_TOGGLED)
onFiltersChanged(filters.copy(healthyOnly = !filters.healthyOnly))
```

- [ ] **Step 5: Map catalog callbacks to the existing analytics entry point**

In `MenuDadoScreen`, update query editing without passing either query to
Analytics:

```kotlin
onQueryChanged = { query ->
    menuCatalogSearchTransitionCta(
        previousQuery = menuCatalogFilters.query,
        newQuery = query
    )?.let { cta ->
        viewModel.trackCtaTapped(MENU_CATALOG_ANALYTICS_SCREEN, cta)
    }
    menuCatalogFilters = menuCatalogFilters.copy(query = query)
},
onClearSearch = {
    viewModel.trackCtaTapped(
        MENU_CATALOG_ANALYTICS_SCREEN,
        MenuCatalogAnalyticsAction.CLEAR_SEARCH.cta
    )
    menuCatalogFilters = menuCatalogFilters.copy(query = "")
},
```

Track filter opening before the existing keyboard dismissal:

```kotlin
viewModel.trackCtaTapped(
    MENU_CATALOG_ANALYTICS_SCREEN,
    MenuCatalogAnalyticsAction.OPEN_FILTERS.cta
)
```

Pass this callback to `MenuCatalogFilterSheetHost`:

```kotlin
onInteraction = { interaction ->
    viewModel.trackCtaTapped(
        MENU_CATALOG_ANALYTICS_SCREEN,
        interaction.analyticsAction.cta
    )
},
```

In both favorites-detail and audience-detail empty-state reset callbacks, log
the closed action before restoring defaults:

```kotlin
viewModel.trackCtaTapped(
    MENU_CATALOG_ANALYTICS_SCREEN,
    MenuCatalogAnalyticsAction.CLEAR_SEARCH_AND_FILTERS.cta
)
menuCatalogFilters = defaultMenuCatalogFilters()
```

- [ ] **Step 6: Run catalog tests and compilation**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCatalogAnalyticsTest
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestSources
```

Expected: PASS. No production call passes `query`, `audience`, `need`, or a
profile value to `trackCtaTapped`.

If an emulator is available, run:

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.ui.MenuCatalogSearchBarTest,com.menudado.ui.MenuCatalogFilterSheetTest
```

Expected: PASS for the search and filter-sheet UI tests.

- [ ] **Step 7: Commit catalog instrumentation**

```bash
git add app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt app/src/main/java/com/menudado/ui/MenuCatalogFilterSheet.kt app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/androidTest/java/com/menudado/ui/MenuCatalogSearchBarTest.kt app/src/androidTest/java/com/menudado/ui/MenuCatalogFilterSheetTest.kt
git commit -m "feat: track saved-menu catalog actions"
```

### Task 3: Add the missing Market CTA markers

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MarketManagementSheet.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/androidTest/java/com/menudado/ui/MarketClearConfirmationDialogTest.kt`

- [ ] **Step 1: Write failing stable-ID and purchased-section callback tests**

Add to `MenuCardUiStateTest`:

```kotlin
@Test
fun `market secondary CTA identifiers stay stable`() {
    assertEquals("expand_purchased_products", marketPurchasedProductsVisibilityCta(true))
    assertEquals("collapse_purchased_products", marketPurchasedProductsVisibilityCta(false))
    assertEquals("close_market_management", marketManagementCloseCta())
}
```

Replace the current purchased-header Android test with:

```kotlin
@Test
fun purchasedHeaderReportsExpandAndCollapseWithoutRequestingManagement() {
    var manageRequests = 0
    val visibilityChanges = mutableListOf<Boolean>()
    composeRule.setContent {
        MaterialTheme {
            MarketListSection(
                products = listOf(
                    MarketProduct(
                        key = "arroz",
                        displayName = "Arroz especial",
                        sourceMenuIds = setOf(1L),
                        isPurchased = true
                    )
                ),
                onPurchasedChanged = { _, _ -> },
                onManageRequested = { manageRequests += 1 },
                onPurchasedVisibilityChanged = visibilityChanges::add
            )
        }
    }

    composeRule.onNodeWithText(string(R.string.market_purchased)).performClick()
    composeRule.onNodeWithText("Arroz especial").assertExists()
    composeRule.onNodeWithText(string(R.string.market_purchased)).performClick()
    composeRule.onNodeWithText("Arroz especial").assertDoesNotExist()
    composeRule.runOnIdle {
        assertEquals(listOf(true, false), visibilityChanges)
        assertEquals(0, manageRequests)
    }
}
```

Add `onPurchasedVisibilityChanged = {}` to the other `MarketListSection`
constructor in that test file.

- [ ] **Step 2: Run tests and confirm the Market callbacks are missing**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
./gradlew :app:compileDebugAndroidTestSources
```

Expected: FAIL because the two CTA helpers and
`onPurchasedVisibilityChanged` are not defined.

- [ ] **Step 3: Implement Market closed identifiers and component callback**

Add to `MarketManagementSheet.kt`:

```kotlin
internal fun marketPurchasedProductsVisibilityCta(isExpanded: Boolean): String =
    if (isExpanded) "expand_purchased_products" else "collapse_purchased_products"

internal fun marketManagementCloseCta(): String = "close_market_management"
```

Extend `MarketListSection`:

```kotlin
internal fun MarketListSection(
    products: List<MarketProduct>,
    onPurchasedChanged: (String, Boolean) -> Unit,
    onManageRequested: () -> Unit,
    onPurchasedVisibilityChanged: (Boolean) -> Unit
)
```

Replace the purchased-card click handler with:

```kotlin
.clickable {
    val isExpanded = !showPurchased
    onPurchasedVisibilityChanged(isExpanded)
    showPurchased = isExpanded
}
```

- [ ] **Step 4: Wire Market markers in `MenuDadoScreen`**

Track sheet dismissal before hiding it:

```kotlin
onDismiss = {
    viewModel.trackCtaTapped(
        ANALYTICS_SCREEN_MARKET,
        marketManagementCloseCta()
    )
    isMarketManagementRequested = false
}
```

Pass the purchased visibility callback:

```kotlin
onPurchasedVisibilityChanged = { isExpanded ->
    viewModel.trackCtaTapped(
        ANALYTICS_SCREEN_MARKET,
        marketPurchasedProductsVisibilityCta(isExpanded)
    )
}
```

Do not alter existing Market callbacks for navigation, product state, menu
inclusion, management opening, or clear confirmations.

- [ ] **Step 5: Run focused Market tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestSources
```

Expected: PASS.

If an emulator is available, run:

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.ui.MarketClearConfirmationDialogTest,com.menudado.ui.MarketManagementSheetTest
```

Expected: PASS, including close, outside-tap, and purchased-section behavior.

- [ ] **Step 6: Commit Market instrumentation**

```bash
git add app/src/main/java/com/menudado/ui/MarketManagementSheet.kt app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt app/src/androidTest/java/com/menudado/ui/MarketClearConfirmationDialogTest.kt
git commit -m "feat: track remaining Market actions"
```

### Task 4: Version and localize the About description

**Files:**
- Modify: `app/src/test/java/com/menudado/about/MenuDadoAboutRemoteConfigTest.kt`
- Modify: `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`
- Modify: `app/src/main/java/com/menudado/about/MenuDadoAboutRemoteConfig.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Update tests to require `about_description_v3` and approved copy**

Change the versioned-key assertion to:

```kotlin
assertEquals(
    "about_description_v3",
    MenuDadoAboutRemoteConfig.KEY_ABOUT_DESCRIPTION
)
```

Add these `about_reason` entries to the three locale maps in
`AiCreationMicrocopyTest`:

```kotlin
"about_reason" to
    "MenuDado nació para resolver una pregunta cotidiana: ¿qué preparo hoy? Genera ideas con IA adaptadas a tu perfil, guarda tus menús, encuéntralos mediante búsqueda y filtros, deja que el dado te ayude a elegir y reúne los productos en tu lista de mercado. Puedes empezar sin registrarte y crear una cuenta gratis para conservar tus datos."
```

```kotlin
"about_reason" to
    "MenuDado was created to answer an everyday question: what should I make today? It generates AI ideas tailored to your profile, saves your menus, helps you find them with search and filters, lets the dice help you choose, and gathers the items in your shopping list. You can start without registering and create a free account to keep your data."
```

```kotlin
"about_reason" to
    "MenuDado a été créé pour répondre à une question du quotidien : que préparer aujourd’hui ? L’app génère des idées avec l’IA adaptées à votre profil, enregistre vos menus, vous aide à les retrouver grâce à la recherche et aux filtres, laisse le dé vous aider à choisir et regroupe les produits dans votre liste de courses. Vous pouvez commencer sans vous inscrire et créer gratuitement un compte pour conserver vos données."
```

- [ ] **Step 2: Run the two tests and confirm old key/copy failures**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.about.MenuDadoAboutRemoteConfigTest --tests com.menudado.ui.AiCreationMicrocopyTest
```

Expected: FAIL because production still uses `v2` and the locale resources
still contain the previous descriptions.

- [ ] **Step 3: Apply the versioned fallback and localized descriptions**

Change only the About description key:

```kotlin
const val KEY_ABOUT_DESCRIPTION = "about_description_v3"
```

Replace `about_reason` in `values`, `values-en`, and `values-fr` with the exact
approved strings from Step 1. Keep `about_created_by` and `about_contact`
unchanged. Do not publish Remote Config.

- [ ] **Step 4: Run About and localization tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.about.MenuDadoAboutRemoteConfigTest --tests com.menudado.ui.AiCreationMicrocopyTest
```

Expected: PASS, including the absent/blank remote-value fallback tests.

- [ ] **Step 5: Commit the About update**

```bash
git add app/src/main/java/com/menudado/about/MenuDadoAboutRemoteConfig.kt app/src/test/java/com/menudado/about/MenuDadoAboutRemoteConfigTest.kt app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml
git commit -m "feat: describe catalog search in About"
```

### Task 5: Advance onboarding to version 7 with approved copy

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/test/java/com/menudado/analytics/FirebaseMenuDadoAnalyticsTest.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Update onboarding tests from version 6 to version 7**

In `FirebaseMenuDadoAnalyticsTest` require:

```kotlin
assertEquals("v7", onboardingVersionDimensionValue(7))
```

In `MenuDadoViewModelTest` make the expectations explicit:

```kotlin
assertEquals(listOf("onboarding_shown:7:new_install"), analytics.events)
```

Rename `completed onboarding version six is not shown or tracked` to version
seven and construct `FakeOnboardingStore(completed = true, completedVersion = 7)`.

For the upgrade test, use:

```kotlin
val previousContentStore = FakeOnboardingStore(completed = true, completedVersion = 6)
```

and require:

```kotlin
assertEquals(listOf("onboarding_shown:7:upgrade"), analytics.events)
assertEquals(7, previousContentStore.completedVersion)
assertEquals(
    listOf(
        "onboarding_shown:7:upgrade",
        "onboarding_completed:start:7:upgrade"
    ),
    analytics.events
)
```

Update the remaining completion expectations to
`onboarding_completed:start:7:new_install` and
`onboarding_completed:skip:7:new_install`.

Rename the `MenuCardUiStateTest` case to `onboarding v7 remains one activation
page with branded hierarchy`; keep its one-page and stable-CTA assertions.

Add these `onboarding_activation_body` entries to the three locale maps in
`AiCreationMicrocopyTest`:

```kotlin
"onboarding_activation_body" to
    "Recibe una idea saludable con IA, encuentra tus menús guardados con búsqueda y filtros y prepara tu lista de mercado."
```

```kotlin
"onboarding_activation_body" to
    "Get a healthy AI-assisted idea, find your saved menus with search and filters, and prepare your shopping list."
```

```kotlin
"onboarding_activation_body" to
    "Obtenez une idée saine avec l’IA, retrouvez vos menus enregistrés grâce à la recherche et aux filtres, puis préparez votre liste de courses."
```

- [ ] **Step 2: Run onboarding tests and confirm version/copy failures**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest --tests com.menudado.analytics.FirebaseMenuDadoAnalyticsTest --tests com.menudado.ui.MenuCardUiStateTest --tests com.menudado.ui.AiCreationMicrocopyTest
```

Expected: FAIL because production still emits/stores version `6` and resources
still contain the old supporting copy.

- [ ] **Step 3: Bump the version and update all locale resources**

Change in `MenuDadoViewModel.kt`:

```kotlin
private const val CURRENT_ONBOARDING_VERSION = 7
```

Replace `onboarding_activation_body` in `values`, `values-en`, and `values-fr`
with the exact approved strings from Step 1. Keep the title, trust messages,
primary CTA label, secondary CTA label, layout, and CTA identifiers unchanged.

- [ ] **Step 4: Run focused onboarding and content tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest --tests com.menudado.analytics.FirebaseMenuDadoAnalyticsTest --tests com.menudado.ui.MenuCardUiStateTest --tests com.menudado.ui.AiCreationMicrocopyTest
```

Expected: PASS. A stored version `6` produces one upgrade exposure, completing
or skipping stores `7`, and stored version `7` stays hidden.

- [ ] **Step 5: Commit onboarding version 7**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt app/src/test/java/com/menudado/analytics/FirebaseMenuDadoAnalyticsTest.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml
git commit -m "feat: refresh onboarding for catalog discovery"
```

### Task 6: Synchronize project context and verify the integrated change

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Update the functional context**

Make these factual documentation changes:

```markdown
- About reads `about_description_v3`; an absent or blank remote value uses the
  localized fallback describing AI ideas, saved-menu search and filters, dice,
  Market, guest access, and account persistence.
- Onboarding content version `7` is shown once to new installs and once as an
  upgrade after older completed versions. It remains a single dialog and keeps
  CTA IDs `create_first_menu` and `explore_without_onboarding`.
- Catalog CTA markers use `screen=menu_catalog` with the eight closed values
  defined in `MenuCatalogAnalyticsAction`. They never include queries or
  dietary selections.
- Market adds only `expand_purchased_products`,
  `collapse_purchased_products`, and `close_market_management`; existing
  Market events remain unchanged and are not duplicated.
```

Replace obsolete references to `about_description_v2` and onboarding version
`6` where they describe current behavior. Preserve dated historical notes by
marking them as historical rather than rewriting what happened at that date.

- [ ] **Step 2: Run focused JVM verification**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuCatalogAnalyticsTest \
  --tests com.menudado.ui.MenuCardUiStateTest \
  --tests com.menudado.ui.MenuDadoViewModelTest \
  --tests com.menudado.analytics.FirebaseMenuDadoAnalyticsTest \
  --tests com.menudado.about.MenuDadoAboutRemoteConfigTest \
  --tests com.menudado.ui.AiCreationMicrocopyTest
```

Expected: `BUILD SUCCESSFUL` and all focused tests pass.

- [ ] **Step 3: Compile production and Android-test sources**

Run:

```bash
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestSources
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run the full unit suite and debug build**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --rerun-tasks
```

Expected: `BUILD SUCCESSFUL` with no failing unit tests and a generated debug
APK.

- [ ] **Step 5: Run focused Compose tests when an emulator is available**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.ui.MenuCatalogSearchBarTest,com.menudado.ui.MenuCatalogFilterSheetTest,com.menudado.ui.MarketClearConfirmationDialogTest,com.menudado.ui.MarketManagementSheetTest
```

Expected: `BUILD SUCCESSFUL`. If no emulator is available, record this as a
residual manual-validation requirement rather than claiming it passed.

- [ ] **Step 6: Perform privacy and duplication checks**

Run:

```bash
rg -n "trackCtaTapped" app/src/main/java/com/menudado/ui/MenuDadoScreen.kt
rg -n "search_started|clear_search|open_filters|select_audience_filter|select_dietary_filter|toggle_favorites_only|toggle_healthy_only|clear_search_and_filters|expand_purchased_products|collapse_purchased_products|close_market_management" app/src/main app/src/test app/src/androidTest docs/project-context.md
git diff --check
```

Expected: catalog and Market calls contain only closed constants/helpers; no
query, profile, menu, or product value is passed to Analytics; each existing
favorite/Market action remains logged once; `git diff --check` prints nothing.

- [ ] **Step 7: Manually review the onboarding and catalog entry behavior**

On a narrow Android device or emulator:

1. Seed onboarding completion version `6`, launch the app, and verify version
   `7` appears once with the approved layout and complete text.
2. Choose either onboarding action, relaunch, and verify it does not reopen.
3. Open Adult, child, baby, and favorites catalog routes; confirm each entry
   resets search and optional filters while preserving its route scope.
4. Search, clear, open filters, change each available filter, and reset an empty
   result; confirm UI behavior is unchanged.
5. In Market, expand/collapse purchased products and open/close management;
   confirm no data is modified by those navigation-only actions.

Expected: no clipped onboarding content, unchanged filter/Market behavior, and
one-time version-7 exposure.

- [ ] **Step 8: Commit documentation and verification-ready state**

```bash
git add docs/project-context.md
git commit -m "docs: describe catalog CTA and onboarding v7 behavior"
```

- [ ] **Step 9: Confirm final repository state**

Run:

```bash
git status --short --branch
git log --oneline -7
```

Expected: clean `release/Version_1.3.0` worktree with the implementation commits
ahead of its remote until the user explicitly requests a push.
