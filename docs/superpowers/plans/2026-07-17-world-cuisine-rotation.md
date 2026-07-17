# World Cuisine Rotation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rotate worldwide culinary inspirations locally for generated menus without additional Gemini calls, response fields, or prompt length.

**Architecture:** A pure `CuisineRotation` component owns a persisted per-meal/per-audience index and returns one typed `CuisineInspiration`. The ViewModel reads the current inspiration before the existing request and advances only after a valid result; the value is passed through the existing repository/analyzer boundary into a shorter prompt instruction.

**Tech Stack:** Kotlin, Android SharedPreferences, Firebase AI Logic, JUnit 4, kotlinx-coroutines-test, Gradle.

---

### Task 1: Typed cuisine catalog and persistent rotation

**Files:**
- Create: `app/src/main/java/com/menudado/domain/CuisineInspiration.kt`
- Create: `app/src/main/java/com/menudado/data/CuisineRotation.kt`
- Create: `app/src/test/java/com/menudado/data/CuisineRotationTest.kt`

- [x] **Step 1: Write failing catalog and rotation tests**

Add tests covering all 16 unique inspirations, a deterministic initial offset, no repetition before a full cycle, independent meal/audience keys, persistence through a second selector instance, and advance semantics:

```kotlin
@Test
fun `rotation visits every cuisine once before repeating`() {
    val state = FakeCuisineRotationStateStore()
    val rotation = CuisineRotation(state) { 3 }
    val seen = buildList {
        repeat(CuisineInspiration.entries.size) {
            add(rotation.current(MealType.LUNCH, MenuAudience.ADULT))
            rotation.advance(MealType.LUNCH, MenuAudience.ADULT)
        }
    }

    assertEquals(CuisineInspiration.entries.size, seen.toSet().size)
    assertEquals(seen.first(), rotation.current(MealType.LUNCH, MenuAudience.ADULT))
}

@Test
fun `reading current cuisine persists pending choice without advancing`() {
    val state = FakeCuisineRotationStateStore()
    val first = CuisineRotation(state) { 5 }
    val pending = first.current(MealType.DINNER, MenuAudience.CHILD)
    val restored = CuisineRotation(state) { 0 }

    assertEquals(pending, restored.current(MealType.DINNER, MenuAudience.CHILD))
}
```

- [x] **Step 2: Run the test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.data.CuisineRotationTest
```

Expected: compilation fails because the catalog and rotation types do not exist.

- [x] **Step 3: Implement the minimal catalog and rotation**

Create the typed catalog:

```kotlin
enum class CuisineInspiration(val promptName: String) {
    MEDITERRANEAN("mediterranea"),
    ITALIAN("italiana"),
    GREEK("griega"),
    SPANISH("espanola"),
    MEXICAN("mexicana"),
    ANDEAN_PERUVIAN("andina o peruana"),
    CARIBBEAN("caribena"),
    BRAZILIAN("brasilena"),
    INDIAN("india"),
    LEVANTINE("levantina"),
    MAGHREBI("magrebi"),
    WEST_AFRICAN("africana occidental"),
    JAPANESE("japonesa"),
    KOREAN("coreana"),
    SOUTHEAST_ASIAN("del sudeste asiatico"),
    NORDIC("nordica")
}
```

Create `CuisineRotationStateStore`, `SharedPreferencesCuisineRotationStateStore`, `InMemoryCuisineRotationStateStore`, and `CuisineRotation`. Use key `${mealType.name}_${audience.name}`, normalize stored indices with `floorMod`, persist the randomly supplied initial index on first read, and store `(current + 1) % entries.size` only from `advance`.

- [x] **Step 4: Run the test and verify GREEN**

Run the Task 1 command again. Expected: `BUILD SUCCESSFUL`.

### Task 2: Compact world-cuisine prompt with token guard

**Files:**
- Modify: `app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt`
- Modify: `app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt`

- [x] **Step 1: Replace the old broad-variety expectation with failing cuisine tests**

Add tests proving the prompt receives exactly one inspiration, removes the old long list, preserves the JSON schema, and uses a shorter instruction than the removed rule:

```kotlin
@Test
fun `prompt uses one compact culinary inspiration instead of generic format list`() {
    val prompt = MenuGenerationPrompt.build(
        mealType = MealType.LUNCH,
        avoidIdeas = emptyList(),
        cuisineInspiration = CuisineInspiration.INDIAN
    ).lowercase()

    assertTrue(prompt.contains("inspiracion culinaria: india."))
    assertFalse(prompt.contains("variedad saludable: cremas"))
    assertFalse(prompt.contains("bowls, salteados"))
}

@Test
fun `culinary inspiration instruction is shorter than removed variety instruction`() {
    val prompt = MenuGenerationPrompt.build(
        MealType.LUNCH,
        emptyList(),
        cuisineInspiration = CuisineInspiration.WEST_AFRICAN
    )
    val newRule = prompt.lineSequence().single { it.contains("Inspiracion culinaria:") }.trim()
    val removedRule = "- Variedad saludable: cremas, sopas, ensaladas completas, ensalada cesar saludable, bowls, salteados simples, tortillas, legumbres, wraps, tostas, pasta integral o arroz integral."

    assertTrue(newRule.length <= removedRule.length)
}
```

- [x] **Step 2: Run prompt tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ai.MenuGenerationPromptTest
```

Expected: compilation fails because `cuisineInspiration` is not accepted.

- [x] **Step 3: Implement the compact prompt instruction**

Add `cuisineInspiration: CuisineInspiration` to `build`, remove the complete `Variedad saludable` list, and insert only:

```kotlin
- Inspiracion culinaria: ${cuisineInspiration.promptName}.
```

Do not add output fields, extra examples, or another instruction block. Retain common-ingredient, safety, audience, meal-type, base-ingredient, avoidance, localization, and JSON rules unchanged.

- [x] **Step 4: Run prompt tests and verify GREEN**

Run the Task 2 command. Expected: `BUILD SUCCESSFUL`.

### Task 3: Pass and commit cuisine selection through the existing request

**Files:**
- Modify: `app/src/main/java/com/menudado/ai/HealthAnalyzer.kt`
- Modify: `app/src/main/java/com/menudado/ai/FirebaseHealthAnalyzer.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuRepository.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/test/java/com/menudado/data/MenuRepositorySyncTest.kt`

- [x] **Step 1: Write failing ViewModel tests for success and failure**

Inject a deterministic `CuisineRotation` and record `requestedCuisineInspiration` in `RecordingHealthAnalyzer`. Add:

```kotlin
@Test
fun `successful generation advances cuisine once while using one IA call`() = runTest(dispatcher) {
    viewModel.generateMenuIdea()
    advanceUntilIdle()

    assertEquals(1, analyzer.generateCalls)
    assertEquals(CuisineInspiration.GREEK, analyzer.requestedCuisineInspiration)
    assertEquals(
        CuisineInspiration.SPANISH,
        cuisineRotation.current(MealType.BREAKFAST, MenuAudience.ADULT)
    )
}

@Test
fun `failed generation keeps pending cuisine for retry`() = runTest(dispatcher) {
    analyzer.generateFailure = IllegalStateException("offline")
    viewModel.generateMenuIdea()
    advanceUntilIdle()

    assertEquals(CuisineInspiration.GREEK, analyzer.requestedCuisineInspiration)
    assertEquals(
        CuisineInspiration.GREEK,
        cuisineRotation.current(MealType.BREAKFAST, MenuAudience.ADULT)
    )
}
```

- [x] **Step 2: Run ViewModel tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest
```

Expected: compilation fails because request boundaries do not carry `CuisineInspiration` and the ViewModel has no rotation dependency.

- [x] **Step 3: Implement minimal request plumbing**

Add `cuisineInspiration: CuisineInspiration` to `HealthAnalyzer.generateMenu`, `MenuRepository.generateMenu`, and all implementations/fakes. Give the internal prompt builder a `MEDITERRANEAN` default only to keep unrelated direct prompt tests concise; production must always pass the selected value. Inject `CuisineRotation` into `MenuDadoViewModel` with an in-memory default. In `generateMenuIdea`:

```kotlin
val cuisineInspiration = cuisineRotation.current(mealType, audience)
repository.generateMenu(
    mealType = mealType,
    avoidIdeas = avoidIdeas,
    dietaryProfile = profile,
    audience = audience,
    baseIngredients = state.aiBaseIngredients.trim(),
    language = AppLanguage.fromLocale(),
    cuisineInspiration = cuisineInspiration
).onSuccess { generated ->
    cuisineRotation.advance(mealType, audience)
    // existing success flow unchanged
}
```

Do not advance in `onFailure`, timeout, quota, throttle, validation, or parsing failure paths.

- [x] **Step 4: Run ViewModel, repository, and prompt tests and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest --tests com.menudado.data.MenuRepositorySyncTest --tests com.menudado.ai.MenuGenerationPromptTest --tests com.menudado.data.CuisineRotationTest
```

Expected: `BUILD SUCCESSFUL`.

### Task 4: Wire production persistence and update project contract

**Files:**
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Modify: `app/src/main/java/com/menudado/MainActivity.kt`
- Modify: `docs/project-context.md`

- [x] **Step 1: Wire SharedPreferences-backed rotation**

Expose a lazy production instance:

```kotlin
val cuisineRotation: CuisineRotation by lazy {
    CuisineRotation(SharedPreferencesCuisineRotationStateStore(applicationContext))
}
```

Pass `app.cuisineRotation` through the existing `ViewModelProvider.Factory`.

- [x] **Step 2: Update project context**

Document that worldwide inspiration rotates locally per meal/audience, persists across restarts, advances only on valid generation, and replaces the old long format list so there is no extra Gemini call, JSON field, or prompt growth.

- [x] **Step 3: Run final verification**

Run:

```bash
git diff --check
./gradlew :app:testDebugUnitTest :app:assembleDebug --rerun-tasks
```

Expected: no diff errors and `BUILD SUCCESSFUL` with all unit tests and debug APK produced. The existing Android Gradle Plugin warning for `compileSdk = 35` may remain, but there must be no test or compilation failure.

- [x] **Step 4: Review final diff and commit**

Confirm only catalog, local rotation, prompt/request plumbing, wiring, tests, and project context changed. Commit:

```bash
git add app/src/main app/src/test docs/project-context.md docs/superpowers/plans/2026-07-17-world-cuisine-rotation.md
git commit -m "feat: rotate worldwide AI menu inspirations"
```
