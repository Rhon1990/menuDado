# Rewarded AI Dice Transition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the existing animated MenuDado dice for at least 1.5 seconds after a rewarded ad closes, then reveal the generated AI menu without changing any non-ad generation flow.

**Architecture:** A small callback coordinator will remember whether Google delivered the reward and will notify the app only after the full-screen ad closes. The ViewModel will run the existing AI request and a rewarded-only minimum-presentation timer in parallel; Compose will keep the generated modal hidden while that explicit rewarded reveal state is active.

**Tech Stack:** Kotlin, Android ViewModel/StateFlow, Jetpack Compose, Google Mobile Ads Rewarded API, Kotlin coroutines test scheduler, JUnit 4.

---

## File structure

| File | Responsibility |
| --- | --- |
| `app/src/main/java/com/menudado/ads/RewardedAdCompletion.kt` | Coordinate reward, dismissal and failure callbacks without Android dependencies. |
| `app/src/main/java/com/menudado/ads/MenuDadoRewardedAd.kt` | Bridge Google Mobile Ads lifecycle callbacks to the coordinator. |
| `app/src/test/java/com/menudado/ads/RewardedAdCompletionTest.kt` | Prove reward delivery is deferred until dismissal and remains one-shot. |
| `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt` | Apply the 1.5-second minimum only to a generation started by a freshly earned ad reward. |
| `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt` | Suppress the generated modal while the rewarded reveal timer remains active. |
| `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt` | Verify fast, slow and ordinary generation timing with virtual time. |
| `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt` | Verify the modal visibility guard. |
| `docs/project-context.md` | Record the rewarded-only transition contract. |

### Task 1: Deliver the reward only after the ad closes

**Files:**
- Create: `app/src/main/java/com/menudado/ads/RewardedAdCompletion.kt`
- Create: `app/src/test/java/com/menudado/ads/RewardedAdCompletionTest.kt`
- Modify: `app/src/main/java/com/menudado/ads/MenuDadoRewardedAd.kt:41-77`

- [ ] **Step 1: Write the failing callback-order tests**

Create `RewardedAdCompletionTest.kt`:

```kotlin
package com.menudado.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardedAdCompletionTest {
    @Test
    fun `earned reward is delivered only after dismissal`() {
        val events = mutableListOf<String>()
        val completion = RewardedAdCompletion(
            onRewardEarned = { events += "earned" },
            onDismissedWithoutReward = { events += "dismissed" }
        )

        completion.recordReward()
        assertTrue(events.isEmpty())

        completion.completeAfterDismissal()

        assertEquals(listOf("earned"), events)
    }

    @Test
    fun `dismissal without reward keeps existing callback`() {
        val events = mutableListOf<String>()
        val completion = RewardedAdCompletion(
            onRewardEarned = { events += "earned" },
            onDismissedWithoutReward = { events += "dismissed" }
        )

        completion.completeAfterDismissal()

        assertEquals(listOf("dismissed"), events)
    }

    @Test
    fun `completion is consumed once and cancelled presentations stay silent`() {
        val events = mutableListOf<String>()
        val completion = RewardedAdCompletion(
            onRewardEarned = { events += "earned" },
            onDismissedWithoutReward = { events += "dismissed" }
        )

        completion.cancel()
        completion.recordReward()
        completion.completeAfterDismissal()
        completion.completeAfterDismissal()

        assertTrue(events.isEmpty())
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ads.RewardedAdCompletionTest
```

Expected: Kotlin compilation fails because `RewardedAdCompletion` does not exist.

- [ ] **Step 3: Add the minimal Android-independent coordinator**

Create `RewardedAdCompletion.kt`:

```kotlin
package com.menudado.ads

internal class RewardedAdCompletion(
    private val onRewardEarned: () -> Unit,
    private val onDismissedWithoutReward: () -> Unit
) {
    private var rewardEarned = false
    private var isCompleted = false

    fun recordReward() {
        if (!isCompleted) {
            rewardEarned = true
        }
    }

    fun completeAfterDismissal() {
        if (isCompleted) return
        isCompleted = true
        if (rewardEarned) {
            onRewardEarned()
        } else {
            onDismissedWithoutReward()
        }
    }

    fun cancel() {
        isCompleted = true
    }
}
```

- [ ] **Step 4: Wire the Google callbacks through the coordinator**

Replace the local `rewardDelivered` Boolean in `MenuDadoRewardedAd.show` with:

```kotlin
val completion = RewardedAdCompletion(
    onRewardEarned = onRewardEarned,
    onDismissedWithoutReward = onDismissedWithoutReward
)
ad.fullScreenContentCallback = object : FullScreenContentCallback() {
    override fun onAdDismissedFullScreenContent() {
        completion.completeAfterDismissal()
        loadIfNeeded()
    }

    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
        completion.cancel()
        onUnavailable()
        loadIfNeeded()
    }
}
ad.show(activity) {
    completion.recordReward()
}
```

- [ ] **Step 5: Run the focused tests and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ads.RewardedAdCompletionTest --tests com.menudado.ads.MenuDadoAdsConfigTest
```

Expected: `BUILD SUCCESSFUL`; all rewarded completion and ads configuration tests pass.

- [ ] **Step 6: Commit the callback-order change**

```bash
git add app/src/main/java/com/menudado/ads/RewardedAdCompletion.kt app/src/main/java/com/menudado/ads/MenuDadoRewardedAd.kt app/src/test/java/com/menudado/ads/RewardedAdCompletionTest.kt
git commit -m "fix: defer rewarded generation until ad closes"
```

### Task 2: Keep the rewarded dice visible for at least 1.5 seconds

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:100-150,1034-1095,1176-1286`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:5950-5975`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt:471-530`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt:1095-1110`

- [ ] **Step 1: Write failing virtual-time tests for rewarded-only timing**

Add these tests near the existing rewarded-generation tests in `MenuDadoViewModelTest.kt`:

```kotlin
@Test
fun `fast rewarded generation keeps dice active for minimum presentation`() = runTest(dispatcher) {
    scopedAiUsageStore.seed(LOCAL_ACCOUNT_AI_USAGE_SCOPE, "2026-06-10", usedCount = 10)
    viewModel.generateMenuIdea()
    assertTrue(viewModel.requestRewardedGeneration())

    viewModel.onRewardedGenerationEarned()
    runCurrent()

    assertEquals(1, analyzer.generateCalls)
    assertTrue(viewModel.uiState.value.isGeneratingMenu)
    assertTrue(viewModel.uiState.value.isRewardedMenuRevealPending)

    advanceTimeBy(REWARDED_AI_DICE_MINIMUM_PRESENTATION_MILLIS - 1L)
    runCurrent()
    assertTrue(viewModel.uiState.value.isRewardedMenuRevealPending)

    advanceTimeBy(1L)
    runCurrent()
    assertFalse(viewModel.uiState.value.isRewardedMenuRevealPending)
    assertFalse(viewModel.uiState.value.isGeneratingMenu)
    assertTrue(viewModel.uiState.value.showGeneratedMenuDetail)
}

@Test
fun `normal generation does not use rewarded minimum presentation`() = runTest(dispatcher) {
    viewModel.generateMenuIdea()
    runCurrent()

    assertEquals(1, analyzer.generateCalls)
    assertFalse(viewModel.uiState.value.isRewardedMenuRevealPending)
    assertFalse(viewModel.uiState.value.isGeneratingMenu)
    assertTrue(viewModel.uiState.value.showGeneratedMenuDetail)
}

@Test
fun `slow rewarded generation adds no wait after AI finishes`() = runTest(dispatcher) {
    scopedAiUsageStore.seed(LOCAL_ACCOUNT_AI_USAGE_SCOPE, "2026-06-10", usedCount = 10)
    analyzer.generateDelayMillis = REWARDED_AI_DICE_MINIMUM_PRESENTATION_MILLIS + 500L
    viewModel.generateMenuIdea()
    assertTrue(viewModel.requestRewardedGeneration())

    viewModel.onRewardedGenerationEarned()
    runCurrent()
    advanceTimeBy(REWARDED_AI_DICE_MINIMUM_PRESENTATION_MILLIS)
    runCurrent()

    assertTrue(viewModel.uiState.value.isGeneratingMenu)

    advanceTimeBy(500L)
    runCurrent()

    assertFalse(viewModel.uiState.value.isGeneratingMenu)
    assertFalse(viewModel.uiState.value.isRewardedMenuRevealPending)
    assertTrue(viewModel.uiState.value.showGeneratedMenuDetail)
}
```

Add this visibility test to `MenuCardUiStateTest.kt`:

```kotlin
@Test
fun `rewarded menu detail stays hidden until dice transition finishes`() {
    val pendingState = MenuDadoUiState(
        showGeneratedMenuDetail = true,
        isRewardedMenuRevealPending = true,
        formMealType = MealType.LUNCH,
        formAudience = MenuAudience.ADULT,
        name = "Idea bonificada",
        description = "Lista para mostrar"
    )

    assertNull(generatedMenuDetailPreview(pendingState))
    assertEquals(
        "Idea bonificada",
        generatedMenuDetailPreview(
            pendingState.copy(isRewardedMenuRevealPending = false)
        )?.name
    )
}
```

- [ ] **Step 2: Run the timing tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: Kotlin compilation fails because `isRewardedMenuRevealPending` and `REWARDED_AI_DICE_MINIMUM_PRESENTATION_MILLIS` do not exist.

- [ ] **Step 3: Add explicit rewarded reveal state and the minimum duration**

Add to `MenuDadoUiState`:

```kotlin
val isRewardedGenerationPending: Boolean = false,
val isRewardedMenuRevealPending: Boolean = false,
```

Add beside the existing AI timing constants:

```kotlin
internal const val REWARDED_AI_DICE_MINIMUM_PRESENTATION_MILLIS = 1_500L
```

Change the request entry points:

```kotlin
private fun startValidatedGenerationRequest(
    request: ValidatedGenerationRequest,
    minimumPresentationMillis: Long = 0L
) {
    when (currentGenerationAccess()) {
        AiGenerationAccess.FREE -> {
            startGeneratedMenuRequest(request, minimumPresentationMillis)
        }
        AiGenerationAccess.REWARDED_CREDIT -> {
            if (consumeRewardedGenerationCredit()) {
                analytics.trackAiRewardedOffer(
                    status = AI_REWARDED_STATUS_GENERATION_STARTED,
                    creditsRemaining = rewardedCreditsRemainingToday()
                )
                startGeneratedMenuRequest(request, minimumPresentationMillis)
            }
        }
        AiGenerationAccess.REWARDED_OFFER -> showRewardedGenerationOffer()
        AiGenerationAccess.HARD_LIMIT -> showAiHardLimitNotice(AI_SOURCE_GENERATE_MENU)
    }
}
```

At the end of `onRewardedGenerationEarned`, use the minimum only for this fresh ad reward:

```kotlin
startValidatedGenerationRequest(
    request = request,
    minimumPresentationMillis = REWARDED_AI_DICE_MINIMUM_PRESENTATION_MILLIS
)
```

- [ ] **Step 4: Run the AI request and minimum timer in parallel**

Change `startGeneratedMenuRequest` to accept `minimumPresentationMillis: Long`. When activating the UI, set:

```kotlin
val hasMinimumPresentation = minimumPresentationMillis > 0L
_uiState.update {
    it.copy(
        aiGenerationPhase = AiGenerationPhase.GENERATING,
        isRewardedMenuRevealPending = hasMinimumPresentation,
        message = null,
        isAiRetryNoticeVisible = false
    )
}
```

At the beginning of the existing `viewModelScope.launch`, start the timer as a sibling of the AI work:

```kotlin
val minimumPresentationJob = if (hasMinimumPresentation) {
    launch { delay(minimumPresentationMillis) }
} else {
    null
}
```

Replace the existing `finally` block with:

```kotlin
} finally {
    slowPhaseJob.cancel()
    minimumPresentationJob?.join()
    _uiState.update {
        it.copy(
            aiGenerationPhase = AiGenerationPhase.IDLE,
            isRewardedMenuRevealPending = false
        )
    }
}
```

The timer runs concurrently with Gemini and the hive fallback. A fast result waits only for the remaining part of 1.5 seconds; a slow result proceeds immediately because the timer has already completed.

- [ ] **Step 5: Gate only the rewarded modal in Compose**

Change the first guard in `generatedMenuDetailPreview`:

```kotlin
internal fun generatedMenuDetailPreview(state: MenuDadoUiState): FoodMenu? {
    if (!state.showGeneratedMenuDetail || state.isRewardedMenuRevealPending) return null
    val mealType = state.formMealType ?: return null
    val audience = state.formAudience ?: return null
    if (audience !in state.enabledAudiences) return null
    val name = state.name.trim()
    val description = state.description.trim()
    if (name.isBlank() || description.isBlank()) return null
    return FoodMenu(
        name = name,
        mealType = mealType,
        audience = audience,
        description = description,
        notes = state.notes.trim(),
        healthAnalysis = state.generatedHealthAnalysis,
        calories = state.calories,
        cuisineInspiration = state.generatedCuisineInspiration,
        shoppingProducts = state.generatedShoppingProducts,
        activeShoppingProductKeys = if (state.addGeneratedMenuToMarketList) {
            state.generatedShoppingProducts.mapTo(linkedSetOf()) { it.key }
        } else {
            emptySet()
        }
    )
}
```

The existing `AiGenerationLoadingOverlay` remains visible because `aiGenerationPhase` stays active until both the request and minimum timer finish.

- [ ] **Step 6: Run the focused tests and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: `BUILD SUCCESSFUL`; fast rewarded generation waits 1.5 seconds, slow rewarded generation adds no extra wait, and ordinary generation remains immediate.

- [ ] **Step 7: Commit the rewarded-only presentation change**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: show dice before rewarded AI result"
```

### Task 3: Document and verify the complete flow

**Files:**
- Modify: `docs/project-context.md:268-277`

- [ ] **Step 1: Update the rewarded advertising contract**

Extend the rewarded-ad bullet under `Publicidad` with:

```markdown
Tras recibir la recompensa, la generación comienza al cerrarse el anuncio y reutiliza el overlay del dado 3D durante al menos 1,5 segundos antes de revelar el menú; este mínimo visual no se aplica a generaciones gratuitas ni a créditos bonificados conservados de una sesión anterior.
```

- [ ] **Step 2: Run all affected unit tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ads.RewardedAdCompletionTest --tests com.menudado.ads.MenuDadoAdsConfigTest --tests com.menudado.ui.MenuDadoViewModelTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Compile the debug variant**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Check the patch for formatting and scope**

Run:

```bash
git diff --check
git status --short
git diff --stat HEAD~2
```

Expected: no whitespace errors; only the rewarded callback, rewarded presentation, tests and project context are changed.

- [ ] **Step 5: Record the manual QA cases**

Validate on a build where rewarded ads are enabled:

1. Exhaust the free allowance, watch the rewarded video fully, and close it.
2. Confirm the app returns to the animated 3D dice instead of an already-open menu.
3. Confirm the dice remains visible for at least 1.5 seconds and then the generated menu opens.
4. Repeat with a slow network and confirm the dice continues until the result arrives without a second fixed wait.
5. Generate with a normal free use and confirm no 1.5-second rewarded minimum is added.
6. Close a rewarded ad before earning the reward and confirm no generation or credit is granted.

- [ ] **Step 6: Commit documentation**

```bash
git add docs/project-context.md
git commit -m "docs: document rewarded dice transition"
```
