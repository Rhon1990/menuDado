# Preserve Form Audience Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the last valid `Para` audience selected after saving or discarding when multiple dietary profiles are active.

**Architecture:** Preserve the current form audience during the existing `resetForm()` state copy and reuse `selectedOrSingleDefault()` to validate it against the currently enabled audiences. The persisted selection store and the discard path already retain the selection, so no new storage, UI, analytics, or Firebase behavior is needed.

**Tech Stack:** Kotlin, Android ViewModel/StateFlow, JUnit 4, kotlinx-coroutines-test, Gradle

---

## Task 1: Preserve the selected audience after saving

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Verify: `docs/project-context.md`

- [ ] **Step 1: Add the regression test**

Add this test near the existing menu-save tests:

```kotlin
@Test
fun `saving menu keeps last selected audience when several are active`() =
    runTest(dispatcher) {
        viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
        viewModel.setDietaryProfileAudienceEnabled(true)
        viewModel.setFormAudience(MenuAudience.CHILD)
        viewModel.updateName("Tostadas")
        viewModel.updateDescription("Pan, tomate y aguacate")

        viewModel.saveMenu()
        advanceUntilIdle()

        assertEquals(MenuAudience.CHILD, viewModel.uiState.value.formAudience)
        assertEquals(MenuAudience.CHILD, formAudienceSelectionStore.storedAudience)
    }
```

- [ ] **Step 2: Run the focused test and confirm RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuDadoViewModelTest.saving menu keeps last selected audience when several are active' \
  --no-daemon --console=plain
```

Expected: the test fails because `resetForm()` clears `formAudience` when more than one audience is enabled.

- [ ] **Step 3: Implement the minimal state fix**

In `resetForm()`, replace:

```kotlin
formAudience = null.selectedOrSingleDefault(loadEnabledAudiences())
```

with:

```kotlin
formAudience = it.formAudience.selectedOrSingleDefault(loadEnabledAudiences())
```

This keeps the current selection only while it remains enabled. If it becomes invalid, the existing helper selects the only enabled audience or returns `null` when the choice is ambiguous.

- [ ] **Step 4: Run the focused test and confirm GREEN**

Run the same focused Gradle command. Expected: PASS.

- [ ] **Step 5: Run the complete ViewModel test class**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuDadoViewModelTest' \
  --no-daemon --console=plain
```

Expected: PASS, including existing single-profile, invalid-profile, save, and discard behavior.

- [ ] **Step 6: Verify project context remains accurate**

Confirm `docs/project-context.md` already states that the `Para` selector retains the last valid selection. Do not edit it unless the implementation changes that documented contract.

- [ ] **Step 7: Commit the focused implementation**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "fix: preserve selected form audience after save"
```

## Task 2: Validate the integrated change

- [ ] **Step 1: Run the full unit test suite from a clean test result state**

```bash
./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest \
  --rerun-tasks --no-daemon --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Build the debug APK**

```bash
./gradlew :app:assembleDebug --rerun-tasks --no-daemon --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Check repository hygiene**

```bash
git diff --check
git status --short
```

Expected: no whitespace errors and no unrelated changes.

