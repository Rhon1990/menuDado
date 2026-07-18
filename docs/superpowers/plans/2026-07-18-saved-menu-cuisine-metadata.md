# Saved Menu Cuisine Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist the locally selected cuisine inspiration on AI-generated menus and show it consistently on recent, favorite, carousel/grid, and saved-detail surfaces.

**Architecture:** `FoodMenu` owns an optional `CuisineInspiration`; Room stores its enum name through a non-destructive version 9 to 10 migration, and Firestore mirrors the optional field with safe parsing. Generated drafts copy the already selected local cuisine into the saved menu. Shared Compose helpers render localized cuisine metadata without changing Gemini calls, prompts, tokens, or response JSON.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Firebase Firestore mapper, JUnit 4, Gradle.

---

### Task 1: Domain and Room persistence

**Files:**
- Modify: `app/src/main/java/com/menudado/domain/MenuModels.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuEntity.kt`
- Create: `app/src/main/java/com/menudado/data/MenuDadoMigrations.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuDadoDatabase.kt`
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Create: `app/src/test/java/com/menudado/data/MenuDadoMigrationsTest.kt`

- [ ] **Step 1: Write failing conversion and migration-contract tests**

Extend the entity round-trip test with a known cuisine and add a null compatibility assertion:

```kotlin
val menu = FoodMenu(
    id = 8L,
    name = "Pasta",
    mealType = MealType.LUNCH,
    description = "Pasta con tomate",
    cuisineInspiration = CuisineInspiration.ITALIAN
)

assertEquals(CuisineInspiration.ITALIAN, menu.toEntity().toDomain().cuisineInspiration)
assertNull(menu.copy(cuisineInspiration = null).toEntity().toDomain().cuisineInspiration)
```

Create `MenuDadoMigrationsTest`:

```kotlin
class MenuDadoMigrationsTest {
    @Test
    fun `database version ten adds nullable cuisine without rewriting rows`() {
        assertEquals(10, MENU_DADO_DATABASE_VERSION)
        assertEquals(9, MIGRATION_9_TO_10.startVersion)
        assertEquals(10, MIGRATION_9_TO_10.endVersion)
        assertEquals(
            "ALTER TABLE menus ADD COLUMN cuisineInspiration TEXT",
            MIGRATION_9_TO_10_SQL
        )
    }
}
```

- [ ] **Step 2: Run RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest --tests com.menudado.data.MenuDadoMigrationsTest
```

Expected: compilation fails because `FoodMenu.cuisineInspiration`, migration constants, and the Room column do not exist.

- [ ] **Step 3: Implement nullable domain/entity storage and migration 9 to 10**

Add to `FoodMenu`:

```kotlin
val cuisineInspiration: CuisineInspiration? = null,
```

Add to `MenuEntity`:

```kotlin
val cuisineInspiration: String? = null,
```

Map safely in `toDomain()`:

```kotlin
cuisineInspiration = cuisineInspiration?.let { stored ->
    runCatching { CuisineInspiration.valueOf(stored) }.getOrNull()
},
```

Map in `toEntity()`:

```kotlin
cuisineInspiration = cuisineInspiration?.name,
```

Create `MenuDadoMigrations.kt`:

```kotlin
internal const val MENU_DADO_DATABASE_VERSION = 10
internal const val MIGRATION_9_TO_10_SQL =
    "ALTER TABLE menus ADD COLUMN cuisineInspiration TEXT"

internal val MIGRATION_9_TO_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(MIGRATION_9_TO_10_SQL)
    }
}
```

Use `MENU_DADO_DATABASE_VERSION` in `@Database` and register `MIGRATION_9_TO_10` after the existing migration 8 to 9 in `MenuDadoApplication`.

- [ ] **Step 4: Run GREEN**

Run the Task 1 command. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/domain/MenuModels.kt app/src/main/java/com/menudado/data/MenuEntity.kt app/src/main/java/com/menudado/data/MenuDadoMigrations.kt app/src/main/java/com/menudado/data/MenuDadoDatabase.kt app/src/main/java/com/menudado/MenuDadoApplication.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt app/src/test/java/com/menudado/data/MenuDadoMigrationsTest.kt
git commit -m "feat: persist menu cuisine locally"
```

### Task 2: Firestore compatibility

**Files:**
- Modify: `app/src/main/java/com/menudado/backend/MenuDadoRemoteDataSource.kt`
- Test: `app/src/test/java/com/menudado/backend/MenuDadoRemoteDataSourceTest.kt`

- [ ] **Step 1: Write failing mapper tests**

Add assertions that `menuDocument` writes the enum name:

```kotlin
val menu = FoodMenu(
    id = 42L,
    name = "Tacos",
    mealType = MealType.LUNCH,
    description = "Tacos de verduras",
    cuisineInspiration = CuisineInspiration.MEXICAN
)

assertEquals("MEXICAN", BackendFirestoreMapper.menuDocument(menu)["cuisineInspiration"])
```

Add read compatibility checks:

```kotlin
assertEquals(
    CuisineInspiration.GREEK,
    BackendFirestoreMapper.menuFromDocument("42", document + ("cuisineInspiration" to "GREEK"))
        ?.cuisineInspiration
)
assertNull(BackendFirestoreMapper.menuFromDocument("42", document)?.cuisineInspiration)
assertNull(
    BackendFirestoreMapper.menuFromDocument(
        "42",
        document + ("cuisineInspiration" to "FUTURE_VALUE")
    )?.cuisineInspiration
)
```

- [ ] **Step 2: Run RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.backend.MenuDadoRemoteDataSourceTest
```

Expected: the write assertion returns `null` and parsed menus do not expose cuisine.

- [ ] **Step 3: Add optional Firestore mapping**

Write:

```kotlin
"cuisineInspiration" to menu.cuisineInspiration?.name,
```

Read with:

```kotlin
cuisineInspiration = (document["cuisineInspiration"] as? String)?.let { stored ->
    runCatching { CuisineInspiration.valueOf(stored) }.getOrNull()
},
```

- [ ] **Step 4: Run GREEN**

Run the Task 2 command. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/backend/MenuDadoRemoteDataSource.kt app/src/test/java/com/menudado/backend/MenuDadoRemoteDataSourceTest.kt
git commit -m "feat: sync menu cuisine metadata"
```

### Task 3: Save generated cuisine without changing AI requests

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Write failing save lifecycle tests**

Extend the generated save test:

```kotlin
assertEquals(CuisineInspiration.MEXICAN, saved.cuisineInspiration)
```

Add a manual-save assertion:

```kotlin
viewModel.setHomeMenuMode(HomeMenuMode.Manual)
viewModel.updateName("Ensalada casera")
viewModel.updateDescription("Tomate y lechuga")
viewModel.saveMenu()
advanceUntilIdle()

assertNull(dao.saved.single().toDomain().cuisineInspiration)
```

- [ ] **Step 2: Run RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest
```

Expected: the generated saved menu has `null` cuisine.

- [ ] **Step 3: Copy the generated draft cuisine into the saved menu**

In the `FoodMenu` created by `saveMenu()` add:

```kotlin
cuisineInspiration = state.generatedCuisineInspiration,
```

Keep manual mode and legacy paths nullable. Do not alter `generateMenu`, `CuisineRotation`, `MenuGenerationPrompt`, or the Gemini JSON parser.

- [ ] **Step 4: Run GREEN**

Run the Task 3 command. Expected: `BUILD SUCCESSFUL` and the existing one-call cuisine rotation tests still pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "feat: retain cuisine on generated menus"
```

### Task 4: Render cuisine across saved-menu surfaces

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `docs/project-context.md`

- [ ] **Step 1: Write failing presentation-contract tests**

Add helpers to the expected UI contract:

```kotlin
assertTrue(menuCuisineMetadataShouldShow(CuisineInspiration.MEXICAN))
assertFalse(menuCuisineMetadataShouldShow(null))
assertEquals(MenuDadoColors.DeepGreen, menuCuisineMetadataColor())
assertEquals(148, favoriteCarouselCardMinHeightDp())
val previewState = MenuDadoUiState(
    showGeneratedMenuDetail = true,
    formMealType = MealType.LUNCH,
    formAudience = MenuAudience.ADULT,
    enabledAudiences = listOf(MenuAudience.ADULT),
    name = "Ensalada mediterránea",
    description = "Tomate, pepino y aceite de oliva",
    generatedCuisineInspiration = CuisineInspiration.MEDITERRANEAN
)
assertEquals(
    CuisineInspiration.MEDITERRANEAN,
    generatedMenuDetailPreview(previewState)?.cuisineInspiration
)
```

- [ ] **Step 2: Run RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: presentation helpers are unresolved, favorite height remains 132, and preview cuisine is `null`.

- [ ] **Step 3: Add reusable compact cuisine text**

Add:

```kotlin
internal fun menuCuisineMetadataShouldShow(inspiration: CuisineInspiration?): Boolean =
    inspiration != null

internal fun menuCuisineMetadataColor(): Color = MenuDadoColors.DeepGreen

@Composable
private fun MenuCuisineMetadata(inspiration: CuisineInspiration) {
    Text(
        text = stringResource(
            id = generatedCuisineBadgeFormatRes(),
            stringResource(id = cuisineInspirationLabelRes(inspiration))
        ),
        color = menuCuisineMetadataColor(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
```

- [ ] **Step 4: Place cuisine without competing with names**

Render `MenuCuisineMetadata` conditionally:

- in `RecentMenuSection`, after the name and before `Meal · Audience`;
- in `FavoriteMenuCarouselItem`, after the title row and before its bottom metadata row;
- in `MenuCarouselItem`, after the fixed-height title and before meal type;
- in `MenuDetailDialog`, as the first item of a `FlowRow` before health and calories.

Pass `state.generatedCuisineInspiration` into `generatedMenuDetailPreview()` so the preview domain object matches the visible generated draft. Increase `favoriteCarouselCardMinHeightDp()` from 132 to 148 so three-line names, cuisine, and actions do not collide.

- [ ] **Step 5: Update project context**

Document that known cuisine is persisted and shown after the title across recent, favorites, audience carousels, View More grids, and details; legacy/manual menus without a trustworthy cuisine omit it; no Gemini calls or tokens change.

- [ ] **Step 6: Run GREEN**

Run the Task 4 command. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt docs/project-context.md
git commit -m "feat: show cuisine across saved menus"
```

### Task 5: Full verification and active-project integration

**Files:**
- Modify: `docs/superpowers/plans/2026-07-18-saved-menu-cuisine-metadata.md` (mark completed steps)

- [ ] **Step 1: Check the complete diff**

```bash
git diff --check release/Version_1.2.0...HEAD
git status --short --branch
```

Expected: no whitespace errors and only the plan completion update remains uncommitted.

- [ ] **Step 2: Run complete verification from scratch**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleReleaseDebuggable --rerun-tasks
```

Expected: `BUILD SUCCESSFUL` with no test failures.

- [ ] **Step 3: Review regression risks**

Inspect the final diff for Room migration registration, null/unknown Firestore compatibility, unchanged Gemini request contract, cuisine preservation through `FoodMenu.copy`, conditional legacy UI, title priority, and all saved-menu surfaces.

- [ ] **Step 4: Commit the completed plan locally**

```bash
git add docs/superpowers/plans/2026-07-18-saved-menu-cuisine-metadata.md
git commit -m "docs: record saved cuisine implementation"
```

- [ ] **Step 5: Integrate locally and verify the checkout Android Studio uses**

Fast-forward `release/Version_1.2.0` to the feature branch without pushing, then rerun:

```bash
./gradlew :app:testDebugUnitTest :app:assembleReleaseDebuggable
```

Install `app/build/outputs/apk/releaseDebuggable/app-releaseDebuggable.apk` on the connected target with `adb install -r`, open `com.menudado/.MainActivity`, generate and save one new AI menu, and verify cuisine appears in generated detail, recent menu, favorites/audience carousel after favoriting, View More grid, and saved detail. Do not consume an additional AI request beyond that single end-to-end validation generation.
