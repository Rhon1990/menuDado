# Localized Card Text Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hacer que el catálogo encuentre cada menú por todos los textos visibles de su tarjeta en el idioma activo, conservando los campos buscables actuales.

**Architecture:** `MenuDadoScreen` resolverá los recursos localizados que ya usa la tarjeta y los agrupará en `MenuCatalogSearchLabels`. La función pura `menuCatalogFilteredMenus` consumirá ese contrato para formar el índice por menú, manteniendo ámbito, orden, normalización y combinación con filtros sin cambios.

**Tech Stack:** Kotlin, Jetpack Compose, recursos Android localizados, JUnit 4, Gradle.

---

## File map

- Modify: `app/src/main/java/com/menudado/ui/MenuCatalogFilters.kt` — contrato de etiquetas y composición del texto buscable.
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt` — resolución de los mismos recursos visibles en la tarjeta.
- Modify: `app/src/test/java/com/menudado/ui/MenuCatalogFiltersTest.kt` — cobertura de todos los textos, idiomas y reglas de visibilidad.
- Modify: `docs/project-context.md` — contrato funcional actualizado del buscador.

### Task 1: Especificar la búsqueda visible multidioma con pruebas rojas

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCatalogFiltersTest.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuCatalogFiltersTest.kt`

- [ ] **Step 1: Add a helper that models the exact localized card labels**

```kotlin
private fun searchLabels(
    cuisine: String = "Cocina mediterránea",
    mealType: String = "Desayuno",
    audience: String = "Adulto",
    healthStatus: String = "Saludable",
    unknownStatus: String = "Sin analizar",
    calories: String = "450 kcal"
) = MenuCatalogSearchLabels(
    cuisineLabels = mapOf(CuisineInspiration.MEDITERRANEAN to cuisine),
    mealTypeLabels = mapOf(MealType.BREAKFAST to mealType),
    audienceLabels = mapOf(MenuAudience.ADULT to audience),
    healthStatusLabels = mapOf(
        HealthStatus.HEALTHY to healthStatus,
        HealthStatus.UNKNOWN to unknownStatus
    ),
    calorieLabels = mapOf(450 to calories)
)
```

- [ ] **Step 2: Add failing cases for Spanish, English and French visible labels**

```kotlin
@Test
fun `search matches every localized text visible on analyzed cards`() {
    val analyzed = adultMenu.copy(
        healthAnalysis = healthyAnalysis(),
        calories = 450
    )
    val localizedCases = listOf(
        searchLabels() to listOf("cocina", "desayuno", "saludable", "450", "kcal"),
        searchLabels(
            cuisine = "Mediterranean cuisine",
            mealType = "Breakfast",
            audience = "Adult",
            healthStatus = "Healthy",
            unknownStatus = "Not analyzed"
        ) to listOf("cuisine", "breakfast", "healthy", "450"),
        searchLabels(
            cuisine = "Cuisine méditerranéenne",
            mealType = "Petit-déjeuner",
            audience = "Adulte",
            healthStatus = "Sain",
            unknownStatus = "Non analysé"
        ) to listOf("cuisine", "petit-dejeuner", "sain", "450")
    )

    localizedCases.forEach { (labels, queries) ->
        queries.forEach { query ->
            assertEquals(
                "$query with $labels",
                listOf(analyzed),
                filterByQuery(analyzed, query, labels)
            )
        }
    }
}
```

- [ ] **Step 3: Add failing visibility-boundary cases**

```kotlin
@Test
fun `search matches unknown status shown on unanalyzed card but not hidden calories`() {
    val unanalyzed = adultMenu.copy(calories = 450)
    val labels = searchLabels()

    assertEquals(listOf(unanalyzed), filterByQuery(unanalyzed, "sin analizar", labels))
    assertTrue(filterByQuery(unanalyzed, "450", labels).isEmpty())
}

@Test
fun `audience label is searchable only where favorites cards show it`() {
    val labels = searchLabels()

    assertEquals(
        listOf(adultMenu),
        filterByQuery(adultMenu, "adulto", labels, MenuCatalogScope.Favorites)
    )
    assertTrue(filterByQuery(adultMenu, "adulto", labels).isEmpty())
}
```

Add the focused helper used above:

```kotlin
private fun filterByQuery(
    menu: FoodMenu,
    query: String,
    labels: MenuCatalogSearchLabels,
    scope: MenuCatalogScope = MenuCatalogScope.Audience(MenuAudience.ADULT)
): List<FoodMenu> = menuCatalogFilteredMenus(
    menus = listOf(menu),
    scope = scope,
    filters = MenuCatalogFilters(query = query),
    dietaryProfiles = profiles(),
    searchLabels = labels
)
```

- [ ] **Step 4: Run the focused test and confirm RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCatalogFiltersTest
```

Expected: compilation fails because `MenuCatalogSearchLabels` and `searchLabels` do not exist in production yet.

### Task 2: Construir el índice con los mismos textos que la tarjeta

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuCatalogFilters.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuCatalogFiltersTest.kt`

- [ ] **Step 1: Add the localized search-label contract**

Add `import com.menudado.domain.MealType` to `MenuCatalogFilters.kt`, then define:

```kotlin
internal data class MenuCatalogSearchLabels(
    val cuisineLabels: Map<CuisineInspiration, String> = emptyMap(),
    val mealTypeLabels: Map<MealType, String> = emptyMap(),
    val audienceLabels: Map<MenuAudience, String> = emptyMap(),
    val healthStatusLabels: Map<HealthStatus, String> = emptyMap(),
    val calorieLabels: Map<Int, String> = emptyMap()
)
```

Change `menuCatalogFilteredMenus` to accept `searchLabels: MenuCatalogSearchLabels` and pass `includeAudience = scope is MenuCatalogScope.Favorites` into `catalogSearchText`.

- [ ] **Step 2: Extend the searchable text without changing filter semantics**

```kotlin
private fun FoodMenu.catalogSearchText(
    labels: MenuCatalogSearchLabels,
    includeAudience: Boolean
): String = buildList {
    add(name)
    add(description)
    add(notes)
    cuisineInspiration?.let { add(labels.cuisineLabels[it].orEmpty()) }
    add(labels.mealTypeLabels[mealType].orEmpty())
    if (includeAudience) add(labels.audienceLabels[audience].orEmpty())
    add(
        labels.healthStatusLabels[
            healthAnalysis?.status ?: HealthStatus.UNKNOWN
        ].orEmpty()
    )
    if (healthAnalysis != null) {
        calories?.let { add(labels.calorieLabels[it].orEmpty()) }
    }
    addAll(shoppingProducts.map { it.displayName })
}.joinToString(separator = " ").normalizedCatalogText()
```

- [ ] **Step 3: Resolve exact localized card strings in Compose**

Replace `cuisineSearchLabels` with:

```kotlin
val menuCatalogSearchLabels = MenuCatalogSearchLabels(
    cuisineLabels = CuisineInspiration.entries.associateWith { inspiration ->
        stringResource(
            id = generatedCuisineBadgeFormatRes(),
            stringResource(id = cuisineInspirationLabelRes(inspiration))
        )
    },
    mealTypeLabels = MealType.entries.associateWith { mealType ->
        stringResource(id = mealTypeLabelRes(mealType))
    },
    audienceLabels = MenuAudience.entries.associateWith { audience ->
        stringResource(id = menuAudienceLabelRes(audience))
    },
    healthStatusLabels = HealthStatus.entries.associateWith { status ->
        stringResource(id = healthStatusLabelRes(status))
    },
    calorieLabels = visibleMenus.mapNotNull(::menuVisibleCalories)
        .distinct()
        .associateWith { calories ->
            stringResource(id = R.string.calories_short, calories)
        }
)
```

Pass this object as `searchLabels` to `menuCatalogFilteredMenus`. Update existing unit-test calls to use `MenuCatalogSearchLabels()` or the localized helper.

- [ ] **Step 4: Run the focused test and confirm GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCatalogFiltersTest
```

Expected: `BUILD SUCCESSFUL`; existing scope/filter tests and new localized search tests pass.

- [ ] **Step 5: Commit the implementation**

```bash
git add app/src/main/java/com/menudado/ui/MenuCatalogFilters.kt app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCatalogFiltersTest.kt
git commit -m "feat: search every localized menu card label"
```

### Task 3: Documentar y verificar la integración

**Files:**
- Modify: `docs/project-context.md`
- Verify: `app/src/main/java/com/menudado/ui/MenuCatalogFilters.kt`
- Verify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] **Step 1: Update the functional catalog contract**

Extend the existing catalog-search bullet with:

```markdown
La búsqueda conserva nombre, descripción, notas e ingredientes y también coincide con todos los textos visibles de la tarjeta en el idioma activo: cocina completa, tipo de comida, estado saludable, calorías visibles y público cuando la tarjeta de Favoritos lo muestra. Sigue ignorando mayúsculas y acentos.
```

- [ ] **Step 2: Run full unit tests and build**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`; no unit-test failures and debug APK generated.

- [ ] **Step 3: Verify repository hygiene**

Run:

```bash
git diff --check
git status --short
```

Expected: no whitespace errors and only the intentional documentation change remains.

- [ ] **Step 4: Commit the functional documentation**

```bash
git add docs/project-context.md
git commit -m "docs: describe localized catalog search"
```
