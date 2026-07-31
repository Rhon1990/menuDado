# Menu Catalog Search and Filters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Add compact local search and green list-based filters to Adult, Kids, Baby, and Favorites collections while preserving each entry's fixed scope and resetting transient state on every entry.

**Architecture:** A focused pure filter module owns scope, query normalization, dietary selection, ordering, active-count, and reset transitions. Compose only renders this state. Existing detail routes remain the immutable scope boundary; MenuDadoActionSheet gains optional backward-compatible navigation/trailing slots; MenuDadoUiState exposes every current audience profile. No Room, Firebase, AI, or navigation redesign is required.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, StateFlow/ViewModel, JUnit 4, AndroidX Compose UI tests, Gradle.

---

## File map

- Create app/src/main/java/com/menudado/ui/MenuCatalogFilters.kt: pure scope and filtering rules.
- Create app/src/main/java/com/menudado/ui/MenuCatalogFilterSheet.kt: green root list and selection sublists.
- Create app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt: compact connected search UI.
- Modify app/src/main/java/com/menudado/domain/DietaryProfileCompatibility.kt: evaluate FoodMenu.
- Modify app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt: expose profiles by audience.
- Modify app/src/main/java/com/menudado/ui/MenuDadoActionSheet.kt: optional back and trailing content.
- Modify app/src/main/java/com/menudado/ui/MenuDadoScreen.kt: route-keyed state, header integration, modal priority, results and empty state.
- Create app/src/main/res/drawable/ic_search.xml and ic_filter_list.xml.
- Modify Spanish, English, and French strings.xml.
- Add or modify unit tests in DietaryProfileCompatibilityTest.kt, MenuCatalogFiltersTest.kt, MenuDadoViewModelTest.kt, and MenuCardUiStateTest.kt.
- Add Compose tests MenuDadoActionSheetTest.kt, MenuCatalogFilterSheetTest.kt, and MenuCatalogSearchBarTest.kt.
- Modify docs/project-context.md after verification.

## Task 1: Evaluate saved menus with the existing compatibility engine

**Files:**
- Modify: app/src/main/java/com/menudado/domain/DietaryProfileCompatibility.kt
- Test: app/src/test/java/com/menudado/domain/DietaryProfileCompatibilityTest.kt

- [ ] **Step 1: Write failing tests**

~~~kotlin
@Test
fun savedMenuCompatibilityChecksRecipeAndProducts() {
    val profile = DietaryProfile(
        isVegan = true,
        hasAllergies = true,
        allergens = setOf(DietaryAllergen.SESAME)
    )
    val menu = FoodMenu(
        name = "Bowl vegetal",
        mealType = MealType.LUNCH,
        description = "Arroz y verduras",
        notes = "Terminar con queso",
        shoppingProducts = listOf(
            requireNotNull(ShoppingProduct.fromAi("Sésamo"))
        )
    )

    assertFalse(profile.accepts(menu))
}

@Test
fun savedMenuCompatibilityUsesSavedAudienceByDefault() {
    val menu = FoodMenu(
        name = "Yogur con miel",
        mealType = MealType.BREAKFAST,
        audience = MenuAudience.BABY,
        description = "Miel y yogur"
    )

    assertFalse(DietaryProfile().accepts(menu))
}
~~~

- [ ] **Step 2: Verify RED**

~~~bash
./gradlew :app:testDebugUnitTest   --tests com.menudado.domain.DietaryProfileCompatibilityTest
~~~

Expected: compilation fails because DietaryProfile.accepts(FoodMenu) is missing.

- [ ] **Step 3: Add minimal overloads**

~~~kotlin
fun DietaryProfile.compatibilityWith(
    menu: FoodMenu,
    audience: MenuAudience = menu.audience
): DietaryProfileCompatibility = compatibilityWith(
    menu = GeneratedMenu(
        name = menu.name,
        description = menu.description,
        notes = menu.notes,
        calories = menu.healthAnalysis?.calories ?: menu.calories ?: 0,
        shoppingProducts = menu.shoppingProducts
    ),
    audience = audience
)

fun DietaryProfile.accepts(
    menu: FoodMenu,
    audience: MenuAudience = menu.audience
): Boolean = compatibilityWith(menu, audience).isCompatible
~~~

Do not import UI helpers into the domain package.

- [ ] **Step 4: Verify GREEN with the Step 2 command.**

- [ ] **Step 5: Commit**

~~~bash
git add app/src/main/java/com/menudado/domain/DietaryProfileCompatibility.kt   app/src/test/java/com/menudado/domain/DietaryProfileCompatibilityTest.kt
git commit -m "feat: evaluate saved menu profile compatibility"
~~~

## Task 2: Expose all audience profiles in UI state

**Files:**
- Modify: app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt
- Test: app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt

- [ ] **Step 1: Write failing tests**

~~~kotlin
@Test
fun uiStateExposesProfilesIndependently() = runTest(dispatcher) {
    dietaryProfileStore.saveProfile(
        DietaryProfile(isEnabled = true, isVegan = true),
        MenuAudience.CHILD
    )

    viewModel.refreshDietaryProfile()

    assertTrue(
        viewModel.uiState.value.dietaryProfiles
            .getValue(MenuAudience.CHILD).isVegan
    )
    assertFalse(
        viewModel.uiState.value.dietaryProfiles
            .getValue(MenuAudience.ADULT).isVegan
    )
}

@Test
fun editingProfileRefreshesProfileMap() = runTest(dispatcher) {
    viewModel.setDietaryProfileAudience(MenuAudience.ADULT)
    viewModel.setDietaryProfilePregnant(true)

    assertTrue(
        viewModel.uiState.value.dietaryProfiles
            .getValue(MenuAudience.ADULT).isPregnant
    )
}
~~~

- [ ] **Step 2: Verify RED**

~~~bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest
~~~

- [ ] **Step 3: Add the map and one loader**

Add to MenuDadoUiState:

~~~kotlin
val dietaryProfiles: Map<MenuAudience, DietaryProfile> =
    MenuAudience.entries.associateWith { DietaryProfile() },
~~~

Add to the ViewModel:

~~~kotlin
private fun loadDietaryProfiles(): Map<MenuAudience, DietaryProfile> =
    MenuAudience.entries.associateWith(dietaryProfileStore::getProfile)
~~~

Add dietaryProfiles = loadDietaryProfiles() to the copy calls in setDietaryProfileAudience, setDietaryProfileAudienceEnabled, refreshDietaryProfile, and updateDietaryProfile. Retain dietaryProfile for the selected Profile tab.

- [ ] **Step 4: Verify GREEN with the Step 2 command.**

- [ ] **Step 5: Commit**

~~~bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt   app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "feat: expose menu audience profiles to ui"
~~~

## Task 3: Implement pure fixed-scope filtering

**Files:**
- Create: app/src/main/java/com/menudado/ui/MenuCatalogFilters.kt
- Create: app/src/test/java/com/menudado/ui/MenuCatalogFiltersTest.kt

- [ ] **Step 1: Write failing scope and reset tests**

Define these fixtures at the top of MenuCatalogFiltersTest:

~~~kotlin
private val adultMenu = FoodMenu(
    id = 1,
    name = "Tostada mediterránea",
    mealType = MealType.BREAKFAST,
    audience = MenuAudience.ADULT,
    description = "Pan, tomate y queso",
    isFavorite = true,
    favoritedAt = 10,
    createdAt = 10,
    cuisineInspiration = CuisineInspiration.MEDITERRANEAN
)
private val childMenu = FoodMenu(
    id = 2,
    name = "Arroz suave",
    mealType = MealType.LUNCH,
    audience = MenuAudience.CHILD,
    description = "Arroz con verduras",
    isFavorite = true,
    favoritedAt = 20,
    createdAt = 20
)
private val babyMenu = FoodMenu(
    id = 3,
    name = "Puré de calabaza",
    mealType = MealType.DINNER,
    audience = MenuAudience.BABY,
    description = "Calabaza y arroz",
    createdAt = 30
)
private fun profiles(): Map<MenuAudience, DietaryProfile> =
    MenuAudience.entries.associateWith {
        DietaryProfile(isEnabled = true)
    }
~~~

~~~kotlin
@Test
fun audienceScopeNeverExpands() {
    val result = menuCatalogFilteredMenus(
        menus = listOf(adultMenu, childMenu),
        scope = MenuCatalogScope.Audience(MenuAudience.ADULT),
        filters = MenuCatalogFilters(query = childMenu.name),
        dietaryProfiles = profiles(),
        cuisineLabels = emptyMap()
    )

    assertTrue(result.isEmpty())
}

@Test
fun favoritesScopeNeverIncludesNonFavorites() {
    val result = menuCatalogFilteredMenus(
        menus = listOf(adultMenu, babyMenu.copy(isFavorite = false)),
        scope = MenuCatalogScope.Favorites,
        filters = MenuCatalogFilters(favoriteAudience = MenuAudience.BABY),
        dietaryProfiles = profiles(),
        cuisineLabels = emptyMap()
    )

    assertTrue(result.isEmpty())
}

@Test
fun defaultsResetEveryTransientFilter() {
    assertEquals(MenuCatalogFilters(), defaultMenuCatalogFilters())
    assertEquals(0, menuCatalogActiveFilterCount(defaultMenuCatalogFilters()))
}
~~~

- [ ] **Step 2: Add failing search and dietary tests**

Add explicit cases for case/accent-insensitive name, description, notes, localized cuisine label and shopping-product searches. Use a dairy menu with profiles for Pregnancy, Vegan and Dairy allergy; assert each selected need excludes it, while NONE includes it. Add a favorite/healthy intersection case that expects only the healthy favorite and preserves favoritedAt ordering. Add a profile without allergens and assert ALLERGIES produces no matches and is unavailable.

Example:

~~~kotlin
@Test
fun favoritesAudienceChangeClearsPregnancyOutsideAdult() {
    val current = MenuCatalogFilters(
        favoriteAudience = MenuAudience.ADULT,
        dietaryNeed = MenuCatalogDietaryNeed.PREGNANCY
    )

    val changed = menuCatalogFiltersAfterFavoriteAudienceSelected(
        current,
        MenuAudience.CHILD
    )

    assertEquals(MenuCatalogDietaryNeed.NONE, changed.dietaryNeed)
}
~~~

- [ ] **Step 3: Verify RED**

~~~bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCatalogFiltersTest
~~~

- [ ] **Step 4: Implement contracts**

Create MenuCatalogFilters.kt with:

~~~kotlin
internal sealed interface MenuCatalogScope {
    data class Audience(val audience: MenuAudience) : MenuCatalogScope
    data object Favorites : MenuCatalogScope
}

internal enum class MenuCatalogDietaryNeed {
    NONE, PREGNANCY, VEGAN, ALLERGIES, FULL_PROFILE
}

internal data class MenuCatalogFilters(
    val query: String = "",
    val favoriteAudience: MenuAudience? = null,
    val dietaryNeed: MenuCatalogDietaryNeed = MenuCatalogDietaryNeed.NONE,
    val favoritesOnly: Boolean = false,
    val healthyOnly: Boolean = false
)

internal fun defaultMenuCatalogFilters() = MenuCatalogFilters()

internal fun menuCatalogActiveFilterCount(filters: MenuCatalogFilters): Int =
    listOf(
        filters.favoriteAudience != null,
        filters.dietaryNeed != MenuCatalogDietaryNeed.NONE,
        filters.favoritesOnly,
        filters.healthyOnly
    ).count { it }

internal fun menuCatalogFiltersAfterFavoriteAudienceSelected(
    filters: MenuCatalogFilters,
    audience: MenuAudience?
): MenuCatalogFilters = filters.copy(
    favoriteAudience = audience,
    dietaryNeed = if (
        filters.dietaryNeed == MenuCatalogDietaryNeed.PREGNANCY &&
        audience != MenuAudience.ADULT
    ) MenuCatalogDietaryNeed.NONE else filters.dietaryNeed
)
~~~

- [ ] **Step 5: Implement filtering and normalized search**

~~~kotlin
internal fun menuCatalogFilteredMenus(
    menus: List<FoodMenu>,
    scope: MenuCatalogScope,
    filters: MenuCatalogFilters,
    dietaryProfiles: Map<MenuAudience, DietaryProfile>,
    cuisineLabels: Map<CuisineInspiration, String>
): List<FoodMenu> {
    val scoped = when (scope) {
        is MenuCatalogScope.Audience -> menus
            .filter { it.audience == scope.audience }
            .sortedByDescending(FoodMenu::createdAt)
        MenuCatalogScope.Favorites -> menus
            .filter(FoodMenu::isFavorite)
            .sortedWith(
                compareByDescending<FoodMenu> { it.favoritedAt ?: it.createdAt }
                    .thenByDescending(FoodMenu::createdAt)
            )
    }
    val query = filters.query.normalizedCatalogText()

    return scoped.filter { menu ->
        (query.isBlank() || menu.catalogSearchText(cuisineLabels).contains(query)) &&
            (scope !is MenuCatalogScope.Favorites ||
                filters.favoriteAudience == null ||
                menu.audience == filters.favoriteAudience) &&
            menu.matchesDietaryNeed(filters.dietaryNeed, dietaryProfiles) &&
            (!filters.favoritesOnly || menu.isFavorite) &&
            (!filters.healthyOnly ||
                menu.healthAnalysis?.status == HealthStatus.HEALTHY)
    }
}

private fun FoodMenu.catalogSearchText(
    cuisineLabels: Map<CuisineInspiration, String>
): String = buildList {
    add(name)
    add(description)
    add(notes)
    cuisineInspiration?.let { add(cuisineLabels[it].orEmpty()) }
    addAll(shoppingProducts.map { it.displayName })
}.joinToString(" ").normalizedCatalogText()

private fun String.normalizedCatalogText(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
~~~

Implement matchesDietaryNeed by inspecting only the relevant violation for Pregnancy, Vegan, or Allergies:

~~~kotlin
private fun FoodMenu.matchesDietaryNeed(
    need: MenuCatalogDietaryNeed,
    profiles: Map<MenuAudience, DietaryProfile>
): Boolean {
    val profile = profiles[audience] ?: DietaryProfile(isEnabled = false)
    return when (need) {
        MenuCatalogDietaryNeed.NONE -> true
        MenuCatalogDietaryNeed.PREGNANCY ->
            DietaryProfile(isPregnant = true)
                .compatibilityWith(this, MenuAudience.ADULT)
                .violations
                .contains(DietaryProfileViolation.PREGNANCY_SAFETY)
                .not()
        MenuCatalogDietaryNeed.VEGAN ->
            DietaryProfile(isVegan = true)
                .compatibilityWith(this, audience)
                .violations
                .contains(DietaryProfileViolation.VEGAN)
                .not()
        MenuCatalogDietaryNeed.ALLERGIES ->
            profile.hasAllergies && profile.allergens.isNotEmpty() &&
                DietaryProfile(
                    hasAllergies = true,
                    allergens = profile.allergens
                ).compatibilityWith(this, audience)
                    .violations
                    .contains(DietaryProfileViolation.ALLERGEN)
                    .not()
        MenuCatalogDietaryNeed.FULL_PROFILE ->
            profile.isEnabled &&
                profile.compatibilityWith(this, audience).isCompatible
    }
}
~~~

- [ ] **Step 6: Implement availability**

~~~kotlin
internal fun menuCatalogDietaryNeedEnabled(
    need: MenuCatalogDietaryNeed,
    scope: MenuCatalogScope,
    favoriteAudience: MenuAudience?,
    profiles: Map<MenuAudience, DietaryProfile>
): Boolean {
    val audiences = when (scope) {
        is MenuCatalogScope.Audience -> listOf(scope.audience)
        MenuCatalogScope.Favorites ->
            favoriteAudience?.let(::listOf) ?: profiles.keys.toList()
    }
    return when (need) {
        MenuCatalogDietaryNeed.NONE,
        MenuCatalogDietaryNeed.VEGAN -> true
        MenuCatalogDietaryNeed.PREGNANCY ->
            audiences == listOf(MenuAudience.ADULT)
        MenuCatalogDietaryNeed.ALLERGIES -> audiences.any { audience ->
            profiles[audience]?.let {
                it.hasAllergies && it.allergens.isNotEmpty()
            } == true
        }
        MenuCatalogDietaryNeed.FULL_PROFILE -> audiences.any { audience ->
            profiles[audience]?.let {
                it.isEnabled && it.hasRestrictions
            } == true
        }
    }
}
~~~

- [ ] **Step 7: Verify GREEN with the Step 3 command.**

- [ ] **Step 8: Commit**

~~~bash
git add app/src/main/java/com/menudado/ui/MenuCatalogFilters.kt   app/src/test/java/com/menudado/ui/MenuCatalogFiltersTest.kt
git commit -m "feat: add fixed scope menu catalog filters"
~~~

## Task 4: Extend the shared action sheet safely

**Files:**
- Modify: app/src/main/java/com/menudado/ui/MenuDadoActionSheet.kt
- Create: app/src/androidTest/java/com/menudado/ui/MenuDadoActionSheetTest.kt
- Regression: app/src/androidTest/java/com/menudado/ui/MarketManagementSheetTest.kt

- [ ] **Step 1: Write a failing Compose test**

Add this test, using the same resource helper as MarketManagementSheetTest:

~~~kotlin
@Test
fun nestedSheetExposesBackAndClose() {
    var backs = 0
    var dismissals = 0
    composeRule.setContent {
        MaterialTheme {
            MenuDadoActionSheet(
                title = "Para quién",
                onBack = { backs += 1 },
                onDismiss = { dismissals += 1 }
            ) {
                MenuDadoActionSheetRow(
                    iconRes = R.drawable.ic_nav_profile,
                    title = "Adulto",
                    onClick = {}
                )
            }
        }
    }

    composeRule.onNodeWithContentDescription(string(R.string.common_back))
        .performClick()
    composeRule.onNodeWithContentDescription(string(R.string.common_close))
        .performClick()
    composeRule.runOnIdle {
        assertEquals(1, backs)
        assertEquals(1, dismissals)
    }
}
~~~

Existing MarketManagementSheetTest must continue reporting only its useful click actions.

- [ ] **Step 2: Verify RED**

~~~bash
./gradlew :app:compileDebugAndroidTestSources
~~~

- [ ] **Step 3: Add optional navigation and trailing slots**

~~~kotlin
@Composable
internal fun MenuDadoActionSheet(
    title: String,
    onDismiss: () -> Unit,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
)
~~~

Place an optional 48 dp back IconButton at TopStart using ic_arrow_back and common_back. Keep the handle centered and the existing close button unchanged.

Extend the row:

~~~kotlin
@Composable
internal fun MenuDadoActionSheetRow(
    iconRes: Int,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
)
~~~

Use clickable(enabled = enabled), alpha 0.42 when disabled, and invoke trailingContent after the text column. Defaults must preserve MenuActionsSheet and MarketManagementSheet.

- [ ] **Step 4: Compile and run unit regressions**

~~~bash
./gradlew :app:compileDebugAndroidTestSources   :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
~~~

- [ ] **Step 5: Commit**

~~~bash
git add app/src/main/java/com/menudado/ui/MenuDadoActionSheet.kt   app/src/androidTest/java/com/menudado/ui/MenuDadoActionSheetTest.kt
git commit -m "refactor: support nested menu action sheets"
~~~

## Task 5: Add icons and localized copy

**Files:**
- Create: app/src/main/res/drawable/ic_search.xml
- Create: app/src/main/res/drawable/ic_filter_list.xml
- Modify: app/src/main/res/values/strings.xml
- Modify: app/src/main/res/values-en/strings.xml
- Modify: app/src/main/res/values-fr/strings.xml

- [ ] **Step 1: Create Material-style 24 dp vector icons**

Use these pathData values:

~~~xml
<!-- ic_search.xml -->
<path android:fillColor="#FFFFFFFF"
    android:pathData="M9.5,3a6.5,6.5 0,1 0,0 13a6.46,6.46 0,0 0,4.04,-1.41L18.95,20L20,18.95l-5.41,-5.41A6.5,6.5 0,0 0,9.5,3M9.5,4.5a5,5 0,1 1,0,10a5,5 0,0 1,0,-10" />

<!-- ic_filter_list.xml -->
<path android:fillColor="#FFFFFFFF"
    android:pathData="M3,6h18v2H3zM6,11h12v2H6zM10,16h4v2h-4z" />
~~~

Wrap each in a 24 dp vector matching existing drawable conventions.

- [ ] **Step 2: Add all localized keys**

Add identical keys in all three locales:

~~~text
menu_catalog_search_hint
menu_catalog_clear_search
menu_catalog_open_filters
menu_catalog_open_filters_active
menu_catalog_filters_title
menu_catalog_filter_audience
menu_catalog_filter_need
menu_catalog_filter_favorites
menu_catalog_filter_healthy
menu_catalog_audience_all
menu_catalog_need_none
menu_catalog_need_pregnancy
menu_catalog_need_vegan
menu_catalog_need_allergies
menu_catalog_need_full_profile
menu_catalog_no_results_title
menu_catalog_no_results_body
menu_catalog_clear_filters
~~~

Spanish values follow the approved labels. English uses “Search dish or ingredient”, “Who it is for”, “Dietary need”, “My allergies”, and “My full profile”. French uses “Rechercher un plat ou un ingrédient”, “Pour qui”, “Besoin alimentaire”, “Mes allergies”, and “Tout mon profil”. Translate accessibility and empty-state strings too; do not leave Spanish fallbacks.

- [ ] **Step 3: Verify resources**

~~~bash
./gradlew :app:processDebugResources
~~~

- [ ] **Step 4: Commit**

~~~bash
git add app/src/main/res/drawable/ic_search.xml   app/src/main/res/drawable/ic_filter_list.xml   app/src/main/res/values/strings.xml   app/src/main/res/values-en/strings.xml   app/src/main/res/values-fr/strings.xml
git commit -m "feat: add menu catalog filter resources"
~~~

## Task 6: Build the green list-based filter sheet

**Files:**
- Create: app/src/main/java/com/menudado/ui/MenuCatalogFilterSheet.kt
- Create: app/src/androidTest/java/com/menudado/ui/MenuCatalogFilterSheetTest.kt

- [ ] **Step 1: Write failing fixed-scope tests**

For Audience(ADULT), assert the root shows Need, Favorites, and Healthy but not Audience. For Favorites, assert it shows Audience, Need, and Healthy but not Favorites. Use performClick on the Audience row, assert Todos/Adulto/Peques/Bebé appear, select Adulto, and assert onFiltersChanged receives favoriteAudience = ADULT. In a separate test open Need for Audience(CHILD), assert Pregnancy has no click action, and assert Allergies has no click action when the child profile has no configured allergens. Reuse the close and Back assertions from MenuDadoActionSheetTest.

- [ ] **Step 2: Verify RED**

~~~bash
./gradlew :app:compileDebugAndroidTestSources
~~~

- [ ] **Step 3: Implement page-driven sheet**

~~~kotlin
private enum class MenuCatalogFilterPage { ROOT, AUDIENCE, NEED }

@Composable
internal fun MenuCatalogFilterSheet(
    scope: MenuCatalogScope,
    filters: MenuCatalogFilters,
    dietaryProfiles: Map<MenuAudience, DietaryProfile>,
    onFiltersChanged: (MenuCatalogFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var page by remember { mutableStateOf(MenuCatalogFilterPage.ROOT) }
    val title = when (page) {
        MenuCatalogFilterPage.ROOT ->
            stringResource(R.string.menu_catalog_filters_title)
        MenuCatalogFilterPage.AUDIENCE ->
            stringResource(R.string.menu_catalog_filter_audience)
        MenuCatalogFilterPage.NEED ->
            stringResource(R.string.menu_catalog_filter_need)
    }

    MenuDadoActionSheet(
        title = title,
        onBack = if (page == MenuCatalogFilterPage.ROOT) null else {
            { page = MenuCatalogFilterPage.ROOT }
        },
        onDismiss = onDismiss
    ) {
        when (page) {
            MenuCatalogFilterPage.ROOT -> MenuCatalogFilterRoot(
                scope = scope,
                filters = filters,
                onOpenAudience = { page = MenuCatalogFilterPage.AUDIENCE },
                onOpenNeed = { page = MenuCatalogFilterPage.NEED },
                onFiltersChanged = onFiltersChanged
            )
            MenuCatalogFilterPage.AUDIENCE -> MenuCatalogAudienceOptions(
                selected = filters.favoriteAudience,
                onSelected = { audience ->
                    onFiltersChanged(
                        menuCatalogFiltersAfterFavoriteAudienceSelected(
                            filters,
                            audience
                        )
                    )
                    page = MenuCatalogFilterPage.ROOT
                }
            )
            MenuCatalogFilterPage.NEED -> MenuCatalogNeedOptions(
                scope = scope,
                filters = filters,
                dietaryProfiles = dietaryProfiles,
                onSelected = { need ->
                    onFiltersChanged(filters.copy(dietaryNeed = need))
                    page = MenuCatalogFilterPage.ROOT
                }
            )
        }
    }
}
~~~

Implement the three private composables referenced above with the exact callback signatures shown. Root rows use ic_nav_profile, ic_filter_list, ic_favorite_border, and ic_check. A selected sublist row updates filters and returns to ROOT. Visual radio/switch trailing content must clear its own semantics so the whole row is the only click target.

~~~kotlin
@Composable
private fun ColumnScope.MenuCatalogFilterRoot(
    scope: MenuCatalogScope,
    filters: MenuCatalogFilters,
    onOpenAudience: () -> Unit,
    onOpenNeed: () -> Unit,
    onFiltersChanged: (MenuCatalogFilters) -> Unit
)

@Composable
private fun ColumnScope.MenuCatalogAudienceOptions(
    selected: MenuAudience?,
    onSelected: (MenuAudience?) -> Unit
)

@Composable
private fun ColumnScope.MenuCatalogNeedOptions(
    scope: MenuCatalogScope,
    filters: MenuCatalogFilters,
    dietaryProfiles: Map<MenuAudience, DietaryProfile>,
    onSelected: (MenuCatalogDietaryNeed) -> Unit
)
~~~

- [ ] **Step 4: Add priority-aware host**

~~~kotlin
@Composable
internal fun MenuCatalogFilterSheetHost(
    isRequested: Boolean,
    isAnotherModalVisible: Boolean,
    scope: MenuCatalogScope?,
    filters: MenuCatalogFilters,
    dietaryProfiles: Map<MenuAudience, DietaryProfile>,
    onFiltersChanged: (MenuCatalogFilters) -> Unit,
    onDismiss: () -> Unit
) {
    if (isRequested && !isAnotherModalVisible && scope != null) {
        MenuCatalogFilterSheet(
            scope,
            filters,
            dietaryProfiles,
            onFiltersChanged,
            onDismiss
        )
    }
}
~~~

- [ ] **Step 5: Compile and run when a device exists**

~~~bash
./gradlew :app:compileDebugAndroidTestSources
./gradlew :app:connectedDebugAndroidTest   -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.ui.MenuCatalogFilterSheetTest
~~~

- [ ] **Step 6: Commit**

~~~bash
git add app/src/main/java/com/menudado/ui/MenuCatalogFilterSheet.kt   app/src/androidTest/java/com/menudado/ui/MenuCatalogFilterSheetTest.kt
git commit -m "feat: add green menu catalog filter sheet"
~~~

## Task 7: Build the compact connected search bar

**Files:**
- Create: app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt
- Create: app/src/androidTest/java/com/menudado/ui/MenuCatalogSearchBarTest.kt
- Modify: app/src/main/java/com/menudado/ui/MenuDadoScreen.kt (Header only)

- [ ] **Step 1: Write failing Compose tests**

Add one stateful test that types “arroz”, clicks the localized clear action, clicks “Open filters, 2 active”, and asserts query is empty and the filter callback ran once. Add two rendering tests: activeFilterCount = 0 does not show a badge, while activeFilterCount = 2 shows “2”.

- [ ] **Step 2: Verify RED**

~~~bash
./gradlew :app:compileDebugAndroidTestSources
~~~

- [ ] **Step 3: Implement MenuCatalogSearchBar**

Use a single-line BasicTextField inside a 48 dp minimum-height cream rounded surface. Add leading ic_search, conditional clear action, a separate 48 dp SelectionGreen filter button with ic_filter_list, and a terracotta badge. Use 16 dp horizontal padding, 14 dp bottom padding, and 8 dp gap. Search and filter icons use HeaderGreen tint. Do not send text to analytics.

- [ ] **Step 4: Make Header accept optional lower content**

Add bottomContent: (@Composable () -> Unit)? = null to Header. Wrap the current Row in a Column whose fillMaxWidth/background modifiers are the current green container modifiers. Keep statusBarsPadding and the current paddings on the unchanged Row. Invoke bottomContent immediately after that Row. This is a mechanical containment change: do not duplicate or restyle the back button, symbol, wordmark, or subtitle.

- [ ] **Step 5: Compile and run focused UI tests when possible**

~~~bash
./gradlew :app:compileDebugAndroidTestSources
./gradlew :app:connectedDebugAndroidTest   -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.ui.MenuCatalogSearchBarTest
~~~

- [ ] **Step 6: Commit**

~~~bash
git add app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt   app/src/main/java/com/menudado/ui/MenuDadoScreen.kt   app/src/androidTest/java/com/menudado/ui/MenuCatalogSearchBarTest.kt
git commit -m "feat: add compact menu catalog search bar"
~~~

## Task 8: Integrate fixed scopes, reset, filtering, and empty state

**Files:**
- Modify: app/src/main/java/com/menudado/ui/MenuDadoScreen.kt
- Modify: app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt

- [ ] **Step 1: Write failing route/reset tests**

~~~kotlin
@Test
fun detailRoutesMapToImmutableCatalogScopes() {
    assertEquals(
        MenuCatalogScope.Audience(MenuAudience.ADULT),
        menuCatalogScope(MenuAudience.ADULT.name)
    )
    assertEquals(
        MenuCatalogScope.Favorites,
        menuCatalogScope(menuFavoritesDetailRouteAfterViewMore())
    )
    assertNull(menuCatalogScope(null))
}

@Test
fun everyRouteEntryResetsTransientFilters() {
    val previous = MenuCatalogFilters(
        query = "arroz",
        dietaryNeed = MenuCatalogDietaryNeed.VEGAN,
        favoritesOnly = true,
        healthyOnly = true
    )

    assertEquals(
        MenuCatalogFilters(),
        menuCatalogFiltersForRouteEntry(MenuAudience.ADULT.name, previous)
    )
    assertEquals(
        MenuCatalogFilters(),
        menuCatalogFiltersForRouteEntry(
            menuFavoritesDetailRouteAfterViewMore(),
            previous
        )
    )
}
~~~

- [ ] **Step 2: Verify RED**

~~~bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
~~~

- [ ] **Step 3: Add route helpers**

~~~kotlin
internal fun menuCatalogScope(route: String?): MenuCatalogScope? = when {
    menuIsFavoritesDetailRoute(route) -> MenuCatalogScope.Favorites
    else -> menuAudienceFromDetailRoute(route)?.let(MenuCatalogScope::Audience)
}

internal fun menuCatalogFiltersForRouteEntry(
    route: String?,
    previous: MenuCatalogFilters = MenuCatalogFilters()
): MenuCatalogFilters =
    if (menuCatalogScope(route) == null) previous
    else defaultMenuCatalogFilters()
~~~

- [ ] **Step 4: Add route-keyed local state**

~~~kotlin
var menuCatalogFilters by remember(audienceDetailRoute) {
    mutableStateOf(menuCatalogFiltersForRouteEntry(audienceDetailRoute))
}
var isMenuCatalogFilterRequested by remember(audienceDetailRoute) {
    mutableStateOf(false)
}
val currentCatalogScope = menuCatalogScope(audienceDetailRoute)
val cuisineSearchLabels = CuisineInspiration.entries.associateWith { inspiration ->
    stringResource(id = cuisineInspirationLabelRes(inspiration))
}
val filteredCatalogMenus = currentCatalogScope?.let { scope ->
    menuCatalogFilteredMenus(
        visibleMenus,
        scope,
        menuCatalogFilters,
        state.dietaryProfiles,
        cuisineSearchLabels
    )
}.orEmpty()
~~~

The remember(audienceDetailRoute) key is mandatory. Do not use rememberSaveable for query or filters.

- [ ] **Step 5: Connect modal priority and Back**

Add the filter request before route navigation in BackHandler. Include it in modal-exclusion logic. Render MenuCatalogFilterSheetHost with current scope and filters. Opening another higher-priority modal must never overlap it.

- [ ] **Step 6: Render header search only on collection routes**

Pass Header.bottomContent only when currentCatalogScope is non-null:

~~~kotlin
MenuCatalogSearchBar(
    query = menuCatalogFilters.query,
    activeFilterCount = menuCatalogActiveFilterCount(menuCatalogFilters),
    onQueryChanged = { query ->
        menuCatalogFilters = menuCatalogFilters.copy(query = query)
    },
    onClearQuery = {
        menuCatalogFilters = menuCatalogFilters.copy(query = "")
    },
    onOpenFilters = {
        focusManager.clearFocus()
        keyboardController?.hide()
        isMenuCatalogFilterRequested = true
    }
)
~~~

- [ ] **Step 7: Preserve immutable base scopes**

Pass filteredCatalogMenus to MenuAudienceDetailScreen or FavoriteMenusDetailScreen. Keep menuShouldLeaveFavoritesDetail based on the unfiltered underlying favorite set, so a zero-result filter shows an empty state instead of navigating away. Do not change Home carousel contents or counts.

- [ ] **Step 8: Add empty-state recovery**

Create MenuCatalogEmptyState using existing Surface, SelectionGreen, Ink, and button patterns. Show localized title/body and a “Clear search and filters” action that sets defaultMenuCatalogFilters(). Preserve the current fixed screen (Adult, Kids, Baby, or Favorites).

- [ ] **Step 9: Run focused verification**

~~~bash
./gradlew :app:testDebugUnitTest   --tests com.menudado.ui.MenuCatalogFiltersTest   --tests com.menudado.ui.MenuCardUiStateTest   --tests com.menudado.ui.MenuDadoViewModelTest   --tests com.menudado.domain.DietaryProfileCompatibilityTest
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestSources
~~~

Expected: focused tests pass; production and Android-test sources compile.

- [ ] **Step 10: Commit**

~~~bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt   app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: integrate menu catalog search and filters"
~~~

## Task 9: Document and verify the integrated result

**Files:**
- Modify: docs/project-context.md
- Verify all implementation files

- [ ] **Step 1: Update project context**

Add this confirmed behavior after implementation:

~~~markdown
- Las colecciones completas de Adulto, Peques, Bebé y Favoritos muestran una barra compacta de búsqueda conectada a la cabecera y una hoja verde de filtros en listas. Cada entrada conserva un ámbito fijo: un filtro nunca incorpora menús de otro público ni menús no favoritos. La consulta y filtros se reinician al abandonar y volver a entrar. Embarazo, Vegano, Mis alergias y Todo mi perfil evalúan conflictos deterministas en el texto guardado; no representan una certificación clínica ni una instantánea histórica del perfil.
~~~

- [ ] **Step 2: Run static checks**

~~~bash
git diff --check
rg -n "menu_catalog_" app/src/main/res/values*/strings.xml
~~~

Expected: no whitespace errors and every key exists in Spanish, English, and French.

- [ ] **Step 3: Run the full local gate**

~~~bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --rerun-tasks
~~~

Expected: all unit tests pass and app/build/outputs/apk/debug/app-debug.apk exists.

- [ ] **Step 4: Run instrumented tests when a device exists**

~~~bash
/Users/rdelgpad/Library/Android/sdk/platform-tools/adb devices
./gradlew :app:connectedDebugAndroidTest   -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.ui.MenuDadoActionSheetTest,com.menudado.ui.MenuCatalogFilterSheetTest,com.menudado.ui.MenuCatalogSearchBarTest,com.menudado.ui.MarketManagementSheetTest
~~~

If no device is in state device, record compileDebugAndroidTestSources as evidence and report the device gap.

- [ ] **Step 5: Perform manual QA**

Verify Adult only shows Adult, Kids only Kids, Baby only Baby, and Favorites never shows non-favorites. Exit and re-enter each screen to prove reset. Test search fields, dietary options, empty recovery, nested Back, X/outside dismissal, Spanish/English/French, narrow screen, large text, TalkBack, and regression appearance of Menu actions and Market management.

- [ ] **Step 6: Commit documentation**

~~~bash
git add docs/project-context.md
git commit -m "docs: document menu catalog search filters"
~~~

- [ ] **Step 7: Inspect final state**

~~~bash
git status --short
git log --oneline -8
~~~

Expected: clean worktree with focused commits. Do not push, deploy Firebase, create a release, or publish unless explicitly requested.
