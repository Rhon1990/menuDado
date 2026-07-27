# Rewarded AI Credits Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give accounts 10 and guests 5 free daily Gemini requests, then let an opted-in Rewarded ad unlock one normal menu-generation attempt at a time, up to ten rewarded attempts per day and never beyond 20 Gemini requests.

**Architecture:** Keep the existing `AiDailyUsageStore` as the counter of real provider requests and add a small, local reward-credit ledger for earned and consumed ad rewards. The ViewModel owns eligibility and resumes the existing `generateMenuIdea` flow after a verified `onUserEarnedReward`; the Activity-owned AdMob controller only loads/shows ads and reports their lifecycle. A provider failure follows the existing Gemini-to-hive fallback unchanged.

**Tech Stack:** Kotlin, Compose, ViewModel/StateFlow, SharedPreferences, Firebase Remote Config, Google Mobile Ads SDK 24.7.0, UMP, JUnit, Firebase CLI, AdMob.

---

## File structure

| File | Responsibility |
| --- | --- |
| `app/src/main/java/com/menudado/domain/AiDailyUsagePolicy.kt` | Pure limits and eligibility for free/rewarded generation and analysis. |
| `app/src/main/java/com/menudado/data/RewardedAiCreditStore.kt` | Persist daily earned/consumed rewarded credits locally. |
| `app/src/main/java/com/menudado/ads/MenuDadoRewardedAd.kt` | Load, expose readiness and show a single Rewarded ad after consent. |
| `app/src/main/java/com/menudado/ads/RewardedAiRemoteConfig.kt` | Fetch the kill switch for rewarded generation. |
| `app/src/main/java/com/menudado/auth/GuestAiLimitsRemoteConfig.kt` | Fetch the guest-only AI limit switch without changing guest menu-saving limits. |
| `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt` | Own pending rewarded generation, consume a credit and preserve Gemini-to-hive behavior. |
| `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt` | Render the active rewarded CTA in the existing dice button and bridge its request to the Activity. |
| `app/src/main/java/com/menudado/MainActivity.kt` | Wire Remote Config, ad readiness and reward callbacks to the ViewModel. |

### Task 1: Model the limits and reward-credit ledger

**Files:**
- Create: `app/src/main/java/com/menudado/domain/AiDailyUsagePolicy.kt`
- Create: `app/src/main/java/com/menudado/data/RewardedAiCreditStore.kt`
- Test: `app/src/test/java/com/menudado/domain/AiDailyUsagePolicyTest.kt`
- Test: `app/src/test/java/com/menudado/data/RewardedAiCreditStoreTest.kt`

- [ ] **Step 1: Write failing policy tests for every entitlement boundary.**

```kotlin
@Test
fun account_has_ten_free_requests_then_can_use_earned_generation_credit() {
    val access = aiGenerationAccess(
        isGuest = false,
        guestAiLimitsEnabled = true,
        totalRequests = 10,
        rewardedCreditsEarned = 1,
        rewardedCreditsConsumed = 0
    )

    assertEquals(AiGenerationAccess.REWARDED_CREDIT, access)
}

@Test
fun guest_stops_after_five_free_requests_without_a_reward() {
    assertEquals(
        AiGenerationAccess.REWARDED_OFFER,
        aiGenerationAccess(
            isGuest = true,
            guestAiLimitsEnabled = true,
            totalRequests = 5,
            rewardedCreditsEarned = 0,
            rewardedCreditsConsumed = 0
        )
    )
}

@Test
fun analysis_never_uses_a_rewarded_credit() {
    assertFalse(canUseAiAnalysis(totalRequests = 10, isGuest = false, guestAiLimitsEnabled = true))
}
```

- [ ] **Step 2: Run the policy tests and verify they fail because the policy does not exist.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.domain.AiDailyUsagePolicyTest`

Expected: compilation failure mentioning `AiGenerationAccess` or `aiGenerationAccess`.

- [ ] **Step 3: Add the pure policy with fixed, named limits.**

```kotlin
internal const val AI_DAILY_HARD_REQUEST_LIMIT = 20
internal const val AI_SIGNED_IN_FREE_REQUEST_LIMIT = 10
internal const val AI_GUEST_FREE_REQUEST_LIMIT = 5
internal const val MAX_DAILY_REWARDED_AI_GENERATIONS = 10

internal enum class AiGenerationAccess { FREE, REWARDED_CREDIT, REWARDED_OFFER, HARD_LIMIT }

internal fun aiGenerationAccess(
    isGuest: Boolean,
    guestAiLimitsEnabled: Boolean,
    totalRequests: Int,
    rewardedCreditsEarned: Int,
    rewardedCreditsConsumed: Int
): AiGenerationAccess {
    val freeLimit = if (isGuest && guestAiLimitsEnabled) {
        AI_GUEST_FREE_REQUEST_LIMIT
    } else {
        AI_SIGNED_IN_FREE_REQUEST_LIMIT
    }
    if (totalRequests < freeLimit) return AiGenerationAccess.FREE
    if (totalRequests >= AI_DAILY_HARD_REQUEST_LIMIT) return AiGenerationAccess.HARD_LIMIT
    if (rewardedCreditsConsumed < rewardedCreditsEarned) return AiGenerationAccess.REWARDED_CREDIT
    return if (rewardedCreditsEarned < MAX_DAILY_REWARDED_AI_GENERATIONS) {
        AiGenerationAccess.REWARDED_OFFER
    } else {
        AiGenerationAccess.HARD_LIMIT
    }
}
```

Add `RewardedAiCreditState(dateKey, earnedCount, consumedCount)`, its store interface, no-op implementation and `SharedPreferencesRewardedAiCreditStore`. Clamp both counts to `0..10`, reset logically whenever the date key differs, and use a dedicated preferences file named `menu-dado-rewarded-ai-credits`.

- [ ] **Step 4: Write store tests for persistence, date rollover and clamping.**

```kotlin
@Test
fun state_from_another_day_is_not_reused() {
    val state = RewardedAiCreditState("2026-07-27", earnedCount = 10, consumedCount = 10)
    assertEquals(RewardedAiCreditState("2026-07-28", 0, 0), state.forDate("2026-07-28"))
}
```

- [ ] **Step 5: Run both test classes and commit.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.domain.AiDailyUsagePolicyTest --tests com.menudado.data.RewardedAiCreditStoreTest`

Expected: `BUILD SUCCESSFUL`.

Commit: `feat: model rewarded AI daily credits`

### Task 2: Add remote switches and inject the ledger

**Files:**
- Create: `app/src/main/java/com/menudado/ads/RewardedAiRemoteConfig.kt`
- Create: `app/src/main/java/com/menudado/auth/GuestAiLimitsRemoteConfig.kt`
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Modify: `app/src/main/java/com/menudado/MainActivity.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Test: `app/src/test/java/com/menudado/ads/MenuDadoAdsConfigTest.kt`

- [ ] **Step 1: Add failing tests for defaults and fetch intervals.**

```kotlin
@Test
fun rewarded_ai_uses_zero_second_fetch_in_debug_and_hourly_fetch_in_release() {
    assertEquals(0L, RewardedAiRemoteConfig.fetchIntervalSeconds(isDebugBuild = true))
    assertEquals(3_600L, RewardedAiRemoteConfig.fetchIntervalSeconds(isDebugBuild = false))
}

@Test
fun rewarded_ai_default_is_disabled_until_remote_config_activates_it() {
    assertFalse(RewardedAiRemoteConfig.DEFAULT_REWARDED_AI_ENABLED)
}
```

- [ ] **Step 2: Run the test and verify it fails.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ads.MenuDadoAdsConfigTest`

Expected: compilation failure mentioning `RewardedAiRemoteConfig`.

- [ ] **Step 3: Implement the two narrowly scoped Remote Config readers.**

`RewardedAiRemoteConfig` mirrors `MenuDadoAdsRemoteConfig`, exposes `KEY_REWARDED_AI_ENABLED = "rewarded_ai_enabled"`, defaults to `false` and reports a Boolean callback after `fetchAndActivate`.

`GuestAiLimitsRemoteConfig` mirrors `MenuDadoGuestLimitsRemoteConfig`, exposes `KEY_GUEST_AI_LIMITS_ENABLED = "guest_ai_limits_enabled"`, defaults to `true`, and never reads or writes `guest_limits_enabled`.

- [ ] **Step 4: Inject runtime state without coupling it to guest menu-saving limits.**

In `MenuDadoApplication`, add `rewardedAiCreditStore` backed by `SharedPreferencesRewardedAiCreditStore`.

In `MainActivity`, pass the store into `MenuDadoViewModel`, hold `isRewardedAiEnabled` and `areGuestAiLimitsEnabled` as Compose state, fetch both new Remote Config readers with the existing lifecycle effects, and pass those values through `MenuDadoScreen`.

In `MenuDadoScreen`, change the existing `LaunchedEffect(authSession?.userId, authSession?.isAnonymous, areGuestLimitsEnabled)` to pass both guest policy values to the ViewModel. Preserve `areGuestLimitsEnabled` for menu-saving behavior.

- [ ] **Step 5: Run focused Remote Config tests and commit.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ads.MenuDadoAdsConfigTest`

Expected: `BUILD SUCCESSFUL`.

Commit: `feat: configure rewarded AI limits remotely`

### Task 3: Create the Rewarded ad controller and build configuration

**Files:**
- Create: `app/src/main/java/com/menudado/ads/MenuDadoRewardedAd.kt`
- Modify: `app/src/main/java/com/menudado/ads/MenuDadoAdsConfig.kt`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/com/menudado/ads/MenuDadoAdsConfigTest.kt`

- [ ] **Step 1: Add failing configuration tests.**

```kotlin
@Test
fun rewarded_ad_is_requested_only_when_ads_are_ready_and_remote_offer_is_enabled() {
    assertTrue(shouldOfferRewardedAi(remoteEnabled = true, adsReady = true))
    assertFalse(shouldOfferRewardedAi(remoteEnabled = false, adsReady = true))
    assertFalse(shouldOfferRewardedAi(remoteEnabled = true, adsReady = false))
}
```

- [ ] **Step 2: Run the test and verify it fails.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ads.MenuDadoAdsConfigTest`

Expected: compilation failure mentioning `shouldOfferRewardedAi`.

- [ ] **Step 3: Add safe ad-unit configuration.**

Add `REWARDED_AI_AD_UNIT_ID` in every build type:

```kotlin
// debug and releaseDebuggable only
"ca-app-pub-3940256099942544/5224354917"

// release: replace only after Task 6 creates MenuDado's real rewarded unit
providers.optionalConfig(
    gradlePropertyName = "menudadoRewardedAiAdUnitId",
    environmentVariableName = "MENUDADO_REWARDED_AI_AD_UNIT_ID"
).asAndroidStringValue()
```

Fail closed in `MenuDadoAdsConfig` when the release value is blank: do not load or offer a rewarded ad. This prevents accidental use of a test ID in production and keeps the unit ID outside source control until AdMob supplies it.

- [ ] **Step 4: Implement the controller with exactly-once reward delivery.**

```kotlin
class MenuDadoRewardedAd(
    private val activity: Activity,
    private val adUnitId: String,
    private val requestFactory: () -> AdRequest
) {
    fun preload(onReadyChanged: (Boolean) -> Unit, onLoadFailure: () -> Unit)
    fun show(
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onUnavailable: () -> Unit
    )
}
```

Use `RewardedAd.load`, retain at most one loaded ad, set it to null before `show`, and preload the next one in every terminal callback. Attach a `FullScreenContentCallback`; guard the reward lambda with a local Boolean so a dismissal can never grant a second credit. Build the request with the same non-personalized extras currently used by `MenuDadoBannerAd`.

- [ ] **Step 5: Run ads tests and commit.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ads.MenuDadoAdsConfigTest`

Expected: `BUILD SUCCESSFUL`.

Commit: `feat: add rewarded ad controller`

### Task 4: Make the ViewModel consume earned credits and resume the existing flow

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`
- Modify: `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Add failing ViewModel tests for free, rewarded and hard-limit paths.**

```kotlin
@Test
fun generation_limit_requests_rewarded_ad_instead_of_disabling_dice() {
    val viewModel = viewModelWithAiUsage(totalRequests = 10)
    viewModel.updateRewardedAiAvailability(enabled = true, adsReady = true)

    viewModel.generateMenuIdea()

    assertEquals(AiGenerationLimitState.REWARDED_OFFER, viewModel.uiState.value.aiGenerationLimitState)
    assertTrue(viewModel.uiState.value.canRequestRewardedGeneration)
}

@Test
fun earned_reward_resumes_gemini_then_keeps_existing_hive_fallback() = runTest {
    val repository = FakeMenuRepository(generateResult = Result.failure(IOException()))
    val hive = FakeAiMenuHiveGateway(result = compatibleHiveMenu)
    val viewModel = viewModelWithAiUsage(totalRequests = 10, repository = repository, hive = hive)

    viewModel.onRewardedGenerationEarned()
    advanceUntilIdle()

    assertEquals(1, repository.generateCalls)
    assertEquals(1, hive.findCalls)
    assertEquals(GeneratedMenuOrigin.HIVE_FALLBACK, viewModel.uiState.value.generatedOrigin)
}
```

- [ ] **Step 2: Run these tests and verify they fail.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest`

Expected: compilation failure mentioning `AiGenerationLimitState` or reward methods.

- [ ] **Step 3: Add an explicit state machine rather than overloading retry state.**

Add `AiGenerationLimitState { AVAILABLE, REWARDED_OFFER, HARD_LIMIT }` to `MenuDadoUiState`, together with `rewardedCreditsRemainingToday` and `canRequestRewardedGeneration`.

Refactor `canUseAiDailyOrShowNotice(source)` into source-aware checks:

```kotlin
private fun generationAccess(): AiGenerationAccess = aiGenerationAccess(
    isGuest = guestAccessPolicy.isGuest,
    guestAiLimitsEnabled = guestAiLimitsEnabled,
    totalRequests = currentAiDailyUsedCount(currentPacificDateKey()),
    rewardedCreditsEarned = currentRewardedCredits().earnedCount,
    rewardedCreditsConsumed = currentRewardedCredits().consumedCount
)
```

Generation returns `REWARDED_OFFER` without setting `aiRetryAtMillis`; analysis keeps using only the free limit. `requestRewardedGeneration()` revalidates meal, audience, profile, throttle and offer eligibility, then emits a one-shot event for the Activity to show the ad. `onRewardedGenerationEarned()` records one earned credit, consumes it immediately before calling the same private generation routine, and therefore preserves the existing Gemini-first/hive-second body without duplication.

- [ ] **Step 4: Add privacy-safe analytics.**

Add `trackAiRewardedOffer(status: String, creditsRemaining: Int)` to the interface, no-op and Firebase implementation. Valid statuses are constants: `shown`, `unavailable`, `dismissed`, `earned`, `generation_started`. Do not attach menu text, ingredients, profile data, user identifiers or the ad response.

- [ ] **Step 5: Run the ViewModel suite and commit.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest`

Expected: `BUILD SUCCESSFUL`.

Commit: `feat: resume AI generation after rewarded ad`

### Task 5: Wire the active dice CTA to the Activity-owned ad controller

**Files:**
- Modify: `app/src/main/java/com/menudado/MainActivity.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Add failing UI-state tests for the approved button copy.**

```kotlin
@Test
fun reward_offer_keeps_dice_actionable_with_clear_reward_copy() {
    assertEquals(R.string.dice_ai_rewarded_primary, aiDicePrimaryTextRes(AiGenerationLimitState.REWARDED_OFFER))
    assertEquals(R.string.dice_ai_rewarded_secondary, aiDiceSecondaryTextRes(AiGenerationLimitState.REWARDED_OFFER))
}

@Test
fun hard_limit_has_no_rewarded_cta() {
    assertFalse(aiDiceShouldOfferRewardedGeneration(AiGenerationLimitState.HARD_LIMIT))
}
```

- [ ] **Step 2: Run the UI-state test and verify it fails.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: compilation failure mentioning the new reward helper.

- [ ] **Step 3: Wire the controller in `MainActivity`.**

Create and remember `MenuDadoRewardedAd` only when the existing ads controller reports consent/initialization ready and `rewarded_ai_enabled` is true. Preload after readiness. Pass `onRequestRewardedGeneration` to `MenuDadoScreen`; when invoked, call the rewarded controller's `show` method.

Map callbacks exactly:

```kotlin
onRewardEarned = viewModel::onRewardedGenerationEarned
onDismissedWithoutReward = viewModel::onRewardedGenerationDismissed
onUnavailable = viewModel::onRewardedGenerationUnavailable
```

The Activity must not call Gemini directly and must not grant a credit from an ad-load or ad-dismiss callback.

- [ ] **Step 4: Render option B in the existing `ContextualDiceButton`.**

When `AiGenerationLimitState.REWARDED_OFFER`, keep `enabled = canUseFormActions && !state.isGeneratingMenu`, replace the primary/secondary strings with:

```xml
<string name="dice_ai_rewarded_primary">Ver anuncio · desbloquea 1 idea IA</string>
<string name="dice_ai_rewarded_secondary">Límite gratuito de hoy alcanzado</string>
```

Use equivalent English and French translations. When the ad is loading, show `Preparando vídeo…`; when unavailable, keep the CTA retryable and show a concise notice. When `HARD_LIMIT`, retain the ordinary inactive dice styling and show a localized maximum-daily notice with the existing retry time.

- [ ] **Step 5: Run UI tests and commit.**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest --tests com.menudado.ui.MenuDadoViewModelTest`

Expected: `BUILD SUCCESSFUL`.

Commit: `feat: show rewarded AI action in dice`

### Task 6: Configure AdMob and Remote Config safely

**Files:**
- Modify: `app/build.gradle.kts` after obtaining the real Rewarded ad-unit ID
- Modify: `docs/privacy-policy.md`
- Modify: `docs/project-context.md`
- Modify: `firebase.json` only if the Remote Config schema requires a new local default

- [ ] **Step 1: Create the production Rewarded unit in the MenuDado AdMob application.**

In AdMob: `Apps` → `MenuDado` → `Ad units` → `Add ad unit` → `Rewarded`.

Use:

| Setting | Value |
| --- | --- |
| Ad unit name | `ai_daily_limit_rewarded` |
| Reward amount | `1` |
| Reward item | `Idea IA` |
| Frequency cap | `10` per user per day |
| Ad types | Video, interactive and display enabled |
| Server-side verification | Disabled for this no-server implementation |

Copy the generated production ad-unit ID into the secure Gradle property `menudadoRewardedAiAdUnitId` for the Release build. Do not put it in a screenshot, analytics event or debug variant.

- [ ] **Step 2: Add the Remote Config parameters to Debug and Production templates.**

```json
"rewarded_ai_enabled": { "defaultValue": { "value": "true" } },
"guest_ai_limits_enabled": { "defaultValue": { "value": "true" } }
```

Preserve existing `ads_enabled`, `guest_limits_enabled` and `ai_menu_hive_enabled` values. Deploy rules only if the final Firestore diff requires it; this feature does not require a shared-collection rule change.

- [ ] **Step 3: Update public documentation.**

Add a concise privacy-policy sentence stating that users may optionally view rewarded ads to obtain another AI menu-generation attempt and that the request remains subject to daily limits. Update `docs/project-context.md` with the two-tier limits, reward cap, current flow and Debug test-ID requirement.

- [ ] **Step 4: Verify live templates after deployment.**

Run:

```bash
firebase remoteconfig:get --project menudado-debug --output /private/tmp/menudado-debug-rewarded.json
firebase remoteconfig:get --project menudado-6a2da --output /private/tmp/menudado-prod-rewarded.json
jq '{rewarded: .parameters.rewarded_ai_enabled.defaultValue.value, guestAi: .parameters.guest_ai_limits_enabled.defaultValue.value}' /private/tmp/menudado-debug-rewarded.json
```

Expected: both feature flags report `"true"` and existing parameters remain present.

- [ ] **Step 5: Commit documentation/configuration changes.**

Commit: `docs: document rewarded AI limits`

### Task 7: Full validation and device QA

**Files:**
- Modify only if failures identify an implementation defect.

- [ ] **Step 1: Run all automated Android checks.**

Run: `./gradlew :app:clean :app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:bundleRelease`

Expected: `BUILD SUCCESSFUL`, all unit tests pass, and both debug APK and signed Release AAB exist.

- [ ] **Step 2: Install Debug on a registered test device and use only the Google test Rewarded unit.**

Run: `./gradlew :app:installDebug`

Validate in this sequence:

1. Select meal and audience; spend the free limit through controlled test state or a dedicated test double.
2. Confirm the dice shows the option-B rewarded CTA rather than a disabled daily-limit button.
3. Complete the Google test Rewarded ad and confirm the dice starts its normal animation.
4. Confirm a successful Gemini result opens the existing generated-menu dialog.
5. Force the existing provider-failure path and confirm a compatible hive menu opens instead.
6. Close a test ad early and confirm no credit and no Gemini request occur.
7. Repeat until ten earned rewards, then confirm the hard-limit notice has no ad CTA.

- [ ] **Step 3: Verify Ad Inspector and release identity.**

Confirm that Debug displays the Google test unit, ReleaseDebuggable also uses the test unit, and only Release resolves the property-backed production unit. Verify the release artifact reports `applicationId=com.menudado`, version `1.2.1 (13)` unless the release version is intentionally advanced before publishing.

- [ ] **Step 4: Record evidence and request review.**

Report test counts, lint/build result, actual ad load/reward behavior, Remote Config values, residual limits (ad availability, provider quota and local reward-ledger integrity), and the paths to APK/AAB artifacts.

