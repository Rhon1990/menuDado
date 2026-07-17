# Favorite Selection Order Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep profile carousels ordered by menu creation while showing the most recently selected favorite first in the Favorites collection.

**Architecture:** Add an optional `favoritedAt` timestamp to the existing local-first menu aggregate. Profile carousels continue to sort by `createdAt`; the Favorites projection sorts by `favoritedAt` with `createdAt` as a backward-compatible fallback. Room and Firestore persist the timestamp, and the ViewModel owns assigning or clearing it when the user toggles the heart.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Firebase Firestore mapper, JUnit, kotlinx-coroutines-test, Gradle.

---

### Task 1: Lock both ordering rules with failing tests

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/test/java/com/menudado/backend/MenuDadoRemoteDataSourceTest.kt`

- [ ] **Step 1: Write the Favorites ordering test**

Create favorite fixtures whose creation and selection times disagree, then assert the selection order:

```kotlin
val menus = listOf(
    FoodMenu(id = 2L, name = "Creado antes", mealType = MealType.LUNCH, description = "B", createdAt = 1L, isFavorite = true, favoritedAt = 30L),
    FoodMenu(id = 3L, name = "Creado después", mealType = MealType.DINNER, description = "C", createdAt = 20L, isFavorite = true, favoritedAt = 10L)
)
assertEquals(listOf(2L, 3L), menuFavoriteMenus(menus).map { it.id })
```

- [ ] **Step 2: Extend toggle and mapping tests**

Assert that activating a favorite sets `favoritedAt` to the injected clock, deactivating it clears the timestamp, and the Room/domain plus Firestore mappings preserve it.

```kotlin
assertTrue(saved.isFavorite)
assertEquals(localMillisAtHour(8), saved.favoritedAt)
assertFalse(unmarked.isFavorite)
assertNull(unmarked.favoritedAt)
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest --tests com.menudado.ui.MenuDadoViewModelTest --tests com.menudado.backend.MenuDadoRemoteDataSourceTest
```

Expected: compilation fails because `favoritedAt` does not exist yet.

### Task 2: Persist the favorite-selection timestamp

**Files:**
- Modify: `app/src/main/java/com/menudado/domain/MenuModels.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuEntity.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuDadoDatabase.kt`
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Modify: `app/src/main/java/com/menudado/backend/MenuDadoRemoteDataSource.kt`

- [ ] **Step 1: Add the optional field to domain and Room models**

Add the field next to `isFavorite` and map it in both directions:

```kotlin
val isFavorite: Boolean = false,
val favoritedAt: Long? = null,
```

- [ ] **Step 2: Add the Room 8-to-9 migration**

Bump the database version to 9 and register:

```kotlin
private val migration8To9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE menus ADD COLUMN favoritedAt INTEGER")
        db.execSQL("UPDATE menus SET favoritedAt = createdAt WHERE isFavorite = 1")
    }
}
```

- [ ] **Step 3: Add Firestore serialization and backward-compatible deserialization**

Map the nullable field without requiring it on old documents:

```kotlin
"favoritedAt" to menu.favoritedAt,
favoritedAt = (document["favoritedAt"] as? Number)?.toLong(),
```

- [ ] **Step 4: Run mapper tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest --tests com.menudado.backend.MenuDadoRemoteDataSourceTest
```

Expected: mapping tests pass; favorite ordering still fails until Task 3.

### Task 3: Apply independent ordering behavior

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] **Step 1: Timestamp heart activation and clear heart deactivation**

Use the existing injected clock:

```kotlin
val isFavorite = !menu.isFavorite
repository.save(
    menu.copy(
        isFavorite = isFavorite,
        favoritedAt = if (isFavorite) clockMillisProvider() else null
    )
)
```

- [ ] **Step 2: Keep profile ordering creation-based**

Keep the shared profile ordering independent from favorite state:

```kotlin
private fun List<FoodMenu>.menuSortedForDisplay(): List<FoodMenu> =
    sortedByDescending { it.createdAt }
```

- [ ] **Step 3: Sort Favorites by last selection**

Use a deterministic fallback and tie-breaker:

```kotlin
.sortedWith(
    compareByDescending<FoodMenu> { it.favoritedAt ?: it.createdAt }
        .thenByDescending { it.createdAt }
)
```

- [ ] **Step 4: Reset the affected carousel when its first menu changes**

Use the first visible menu ID as a `LaunchedEffect` key in both carousel components and call `scrollToItem(0)`. A new profile menu or latest favorite changes that key, while toggling a heart leaves the profile key unchanged.

- [ ] **Step 5: Confirm successful menu creation**

Return from `repository.save` after the Room write while its cancelable Firestore child continues, increment a consumed-once success revision, and show a localized Snackbar above bottom navigation. Do not emit it for validation or guest-limit failures.

- [ ] **Step 6: Run focused tests and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest --tests com.menudado.ui.MenuDadoViewModelTest --tests com.menudado.backend.MenuDadoRemoteDataSourceTest
```

Expected: all selected tests pass.

### Task 4: Document and verify the complete change

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Update project context**

Document Room version 9, `favoritedAt`, the stable creation order for profile carousels, and latest-selection order for Favorites.

- [ ] **Step 2: Run static and full verification**

Run:

```bash
git diff --check
./gradlew :app:testDebugUnitTest :app:assembleDebug --rerun-tasks
```

Expected: no whitespace errors and `BUILD SUCCESSFUL`.

- [ ] **Step 3: Perform physical QA**

Install the debug APK, mark a later profile card, and verify it stays in place while appearing first in Favorites. Unmark it and confirm the device returns to its original state.

- [ ] **Step 4: Commit the implementation**

```bash
git add app/src/main app/src/test docs/project-context.md docs/superpowers/plans/2026-07-17-favorite-selection-order.md
git commit -m "fix: prioritize latest favorite selection"
```
