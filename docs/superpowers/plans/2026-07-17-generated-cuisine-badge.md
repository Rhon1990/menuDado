# Generated Cuisine Badge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the locally selected world-cuisine inspiration in the generated-menu detail metadata without changing Gemini requests or persisted menus.

**Architecture:** `MenuDadoUiState` temporarily carries the `CuisineInspiration` associated with the valid generated draft. The dialog maps that value to Android string resources and renders it as the first item of a responsive metadata `FlowRow` before health and calories.

**Tech Stack:** Kotlin, Jetpack Compose Foundation/Material 3, Android resources, JUnit 4, Gradle.

---

### Task 1: Generated cuisine lifecycle in UI state

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [x] **Step 1: Write failing state lifecycle tests**

Extend generation tests with:

```kotlin
assertEquals(CuisineInspiration.MEXICAN, viewModel.uiState.value.generatedCuisineInspiration)
```

Add error and discard assertions:

```kotlin
assertNull(viewModel.uiState.value.generatedCuisineInspiration)

viewModel.discardGeneratedMenuIdea()
assertNull(viewModel.uiState.value.generatedCuisineInspiration)
```

- [x] **Step 2: Run RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest
```

Expected: compilation fails because `generatedCuisineInspiration` does not exist.

- [x] **Step 3: Implement minimal state propagation**

Add `generatedCuisineInspiration: CuisineInspiration? = null` to `MenuDadoUiState`. Set it in the valid generation `onSuccess` state update:

```kotlin
generatedCuisineInspiration = cuisineInspiration
```

Clear it in `withoutMenuFormDraft()` together with analysis and calories. Do not set it before or during a failed request.

- [x] **Step 4: Run GREEN**

Run the Task 1 command. Expected: `BUILD SUCCESSFUL`.

### Task 2: Localized cuisine labels

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [x] **Step 1: Write failing resource mapping test**

```kotlin
@Test
fun `all cuisine inspirations have unique visible labels`() {
    val labels = CuisineInspiration.entries.map(::cuisineInspirationLabelRes)

    assertEquals(CuisineInspiration.entries.size, labels.toSet().size)
    assertEquals(R.string.cuisine_mexican, cuisineInspirationLabelRes(CuisineInspiration.MEXICAN))
    assertEquals(R.string.generated_cuisine_badge, generatedCuisineBadgeFormatRes())
}
```

- [x] **Step 2: Run RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: compilation fails because mapping helpers/resources do not exist.

- [x] **Step 3: Add resources and exhaustive mapping**

Add `generated_cuisine_badge` with localized word order and one label for each of the 16 enum values in all three resource files. Add an exhaustive `when` returning the corresponding `@StringRes` and a format-resource helper.

- [x] **Step 4: Run GREEN**

Run the Task 2 command. Expected: `BUILD SUCCESSFUL`.

### Task 3: Responsive metadata row

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `docs/project-context.md`

- [x] **Step 1: Add failing style contract assertions**

```kotlin
assertEquals(MenuDadoColors.SelectionGreen, generatedCuisineBadgeContainerColor())
assertEquals(MenuDadoColors.DeepGreen, generatedCuisineBadgeContentColor())
```

- [x] **Step 2: Run RED**

Run `MenuCardUiStateTest`. Expected: compilation fails because the style helpers do not exist.

- [x] **Step 3: Implement the badge and responsive row**

Pass `state.generatedCuisineInspiration` into `GeneratedMenuDetailDialog`. Keep the title in its own `Text`, replace the existing health/calorie `Row` with `FlowRow`, and render this first when non-null:

```kotlin
CuisineInspirationBadge(cuisineInspiration)
HealthChip(...)
menuVisibleCalories(menu)?.let { CaloriesPill(it) }
```

Use `Arrangement.spacedBy(8.dp)` for horizontal and vertical spacing. The badge uses `SelectionGreen`, `DeepGreen`, the existing control radius, `labelLarge`, bold weight, and the same padding as the other pills. Update project context with the visible metadata behavior and zero-token guarantee.

- [x] **Step 4: Run complete verification and commit locally**

```bash
git diff --check
./gradlew :app:testDebugUnitTest :app:assembleDebug --rerun-tasks
git add app/src/main app/src/test docs/project-context.md docs/superpowers/plans/2026-07-17-generated-cuisine-badge.md
git commit -m "feat: show cuisine on generated menu"
```

Expected: `49` or more Gradle tasks complete with `BUILD SUCCESSFUL`; commit remains local with no push.
