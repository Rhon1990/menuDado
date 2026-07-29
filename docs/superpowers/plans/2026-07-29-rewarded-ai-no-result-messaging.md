# Rewarded AI No-Result Messaging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent a new rewarded ad when the provider is already at its technical daily cap and show private, cause-specific generation errors personalized for every meal, audience and age profile.

**Architecture:** Keep the existing per-scope access policy and apply provider availability only when it would produce a new rewarded offer. Add a small pure Kotlin formatter for generation-only failure copy, carry a presentation reason through `AiFailureNotice`, and render contextual copy only after generation ends without a live or internal fallback result. Existing analysis messages, retry scheduling, analytics and Firestore behavior remain unchanged.

**Tech Stack:** Kotlin, Android ViewModel, coroutines/StateFlow, JUnit 4, Gradle.

---

## File map

- Create `app/src/main/java/com/menudado/ui/AiGenerationFailureMessage.kt`: pure localized formatter with no Android dependency.
- Create `app/src/test/java/com/menudado/ui/AiGenerationFailureMessageTest.kt`: audience, profile, locale and privacy coverage for the formatter.
- Modify `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:1039-1107,1190-1354,1417-1424,1492-1509,2279-2314`: rewarded-offer gate, failure reason propagation and contextual presentation.
- Modify `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt:248-290,432-487,548-566,701-712`: integration and regression coverage.
- Modify `docs/project-context.md:74-80,149-155,273`: document the user-visible contract without changing the internal architecture.

### Task 1: Block new rewarded offers at the provider cap

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`

- [ ] **Step 1: Write the failing ViewModel tests**

Add these tests beside the existing rewarded-offer and provider-safeguard tests:

```kotlin
@Test
fun `provider cap blocks a new rewarded offer after free uses are exhausted`() =
    runTest(dispatcher) {
        scopedAiUsageStore.seed(
            LOCAL_ACCOUNT_AI_USAGE_SCOPE,
            "2026-06-10",
            usedCount = 10
        )
        scopedAiUsageStore.seed(
            PROVIDER_AI_USAGE_SCOPE,
            "2026-06-10",
            usedCount = AI_PROVIDER_DAILY_HARD_LIMIT
        )
        viewModel.updateGuestAccess(
            isGuest = false,
            areLimitsEnabled = true,
            areAiLimitsEnabled = true
        )

        viewModel.generateMenuIdea()

        assertEquals(AiGenerationLimitState.HARD_LIMIT, viewModel.uiState.value.aiGenerationLimitState)
        assertFalse(viewModel.uiState.value.canRequestRewardedGeneration)
        assertFalse(viewModel.requestRewardedGeneration())
        assertEquals(0, analyzer.generateCalls)
        assertTrue(hive.searches.isEmpty())
        assertNull(rewardedAiCreditStore.ledger)
    }

@Test
fun `earned rewarded credit still uses hive when provider cap is reached`() =
    runTest(dispatcher) {
        scopedAiUsageStore.seed(
            LOCAL_ACCOUNT_AI_USAGE_SCOPE,
            "2026-06-10",
            usedCount = 10
        )
        scopedAiUsageStore.seed(
            PROVIDER_AI_USAGE_SCOPE,
            "2026-06-10",
            usedCount = AI_PROVIDER_DAILY_HARD_LIMIT
        )
        rewardedAiCreditStore.seed(
            LOCAL_ACCOUNT_AI_USAGE_SCOPE,
            RewardedAiCreditLedger(
                dateKey = "2026-06-10",
                earnedCount = 1,
                consumedCount = 0
            )
        )
        hive.searchResult = Result.success(sampleHiveCandidate())
        viewModel.updateGuestAccess(
            isGuest = false,
            areLimitsEnabled = true,
            areAiLimitsEnabled = true
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertEquals(0, analyzer.generateCalls)
        assertEquals(1, hive.searches.size)
        assertEquals(GeneratedMenuOrigin.HIVE_FALLBACK, viewModel.uiState.value.generatedOrigin)
        assertEquals(1, rewardedAiCreditStore.ledger?.consumedCount)
    }
```

Import `AI_PROVIDER_DAILY_HARD_LIMIT` from `com.menudado.domain`.

- [ ] **Step 2: Run the two tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest.provider cap blocks a new rewarded offer after free uses are exhausted" \
  --tests "com.menudado.ui.MenuDadoViewModelTest.earned rewarded credit still uses hive when provider cap is reached"
```

Expected: the first test fails because `currentGenerationAccess()` still returns
`REWARDED_OFFER`; the second test should already pass and protects the existing
authorization contract.

- [ ] **Step 3: Apply provider availability only to a new offer**

Replace `currentGenerationAccess()` with:

```kotlin
private fun currentGenerationAccess(): AiGenerationAccess {
    val dateKey = currentPacificDateKey()
    val ledger = currentRewardedLedger()
    val scopedAccess = currentAiUsagePolicy().generationAccess(
        usedCount = currentScopedAiUsedCount(dateKey),
        earnedRewardedCredits = ledger.earnedCount,
        consumedRewardedCredits = ledger.consumedCount
    )
    return if (
        scopedAccess == AiGenerationAccess.REWARDED_OFFER &&
        currentProviderAiUsedCount(dateKey) >= AI_PROVIDER_DAILY_HARD_LIMIT
    ) {
        AiGenerationAccess.HARD_LIMIT
    } else {
        scopedAccess
    }
}
```

Do not move the provider counter into `AiDailyUsagePolicy`: it is deliberately
separate from account/guest product balances, and `FREE` plus
`REWARDED_CREDIT` must remain authorized for the internal fallback.

- [ ] **Step 4: Run focused and policy tests and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest" \
  --tests "com.menudado.domain.AiDailyUsagePolicyTest"
```

Expected: all selected tests pass.

- [ ] **Step 5: Commit the access fix**

```bash
git add \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "fix: avoid rewarded offer at provider cap"
```

### Task 2: Add a private contextual failure-message formatter

**Files:**
- Create: `app/src/test/java/com/menudado/ui/AiGenerationFailureMessageTest.kt`
- Create: `app/src/main/java/com/menudado/ui/AiGenerationFailureMessage.kt`

- [ ] **Step 1: Write the failing formatter tests**

Create `AiGenerationFailureMessageTest.kt`:

```kotlin
package com.menudado.ui

import com.menudado.domain.AppLanguage
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiGenerationFailureMessageTest {
    @Test
    fun `daily limit includes meal audience and default baby age`() {
        val message = contextualAiGenerationFailureMessage(
            language = AppLanguage.SPANISH,
            reason = AiGenerationFailureReason.DAILY_LIMIT,
            mealType = MealType.LUNCH,
            audience = MenuAudience.BABY,
            profile = DietaryProfile(ageRange = "")
        )

        assertEquals(
            "Hoy no podemos preparar más ideas para Almuerzo · Bebé (6-24 meses). " +
                "Vuelve a intentarlo mañana.",
            message
        )
    }

    @Test
    fun `restricted child profile uses custom age without exposing restrictions`() {
        val message = contextualAiGenerationFailureMessage(
            language = AppLanguage.SPANISH,
            reason = AiGenerationFailureReason.HIGH_DEMAND,
            mealType = MealType.DINNER,
            audience = MenuAudience.CHILD,
            profile = DietaryProfile(
                ageRange = "4-8 años",
                isVegan = true,
                otherAvoidances = "texto privado"
            )
        )

        assertEquals(
            "La IA está con mucha demanda y no pudo preparar una idea para " +
                "Cena · Peques (4-8 años) con tu perfil actual. Inténtalo más tarde.",
            message
        )
        assertFalse(message.contains("veg", ignoreCase = true))
        assertFalse(message.contains("texto privado", ignoreCase = true))
    }

    @Test
    fun `every audience uses its localized label and effective age`() {
        val cases = listOf(
            MenuAudience.ADULT to "Persona adulta (18+ años)",
            MenuAudience.CHILD to "Peques (2-12 años)",
            MenuAudience.BABY to "Bebé (6-24 meses)"
        )

        cases.forEach { (audience, expectedContext) ->
            val message = contextualAiGenerationFailureMessage(
                language = AppLanguage.SPANISH,
                reason = AiGenerationFailureReason.CONNECTION,
                mealType = MealType.BREAKFAST,
                audience = audience,
                profile = DietaryProfile(ageRange = "")
            )

            assertTrue(message.contains("Desayuno · $expectedContext"))
        }
    }

    @Test
    fun `every failure reason gives the expected recovery action`() {
        val expectedActions = mapOf(
            AiGenerationFailureReason.DAILY_LIMIT to "mañana",
            AiGenerationFailureReason.HIGH_DEMAND to "más tarde",
            AiGenerationFailureReason.TIMEOUT to "Revisa tu conexión",
            AiGenerationFailureReason.CONNECTION to "Revisa tu conexión",
            AiGenerationFailureReason.SERVICE_UNAVAILABLE to "más tarde"
        )

        expectedActions.forEach { (reason, expectedAction) ->
            val message = contextualAiGenerationFailureMessage(
                language = AppLanguage.SPANISH,
                reason = reason,
                mealType = MealType.BREAKFAST,
                audience = MenuAudience.ADULT,
                profile = DietaryProfile(ageRange = "18+ años")
            )

            assertTrue("$reason omitted $expectedAction", message.contains(expectedAction))
        }
    }

    @Test
    fun `all localized messages hide internal fallback details`() {
        val forbidden = listOf(
            "colmena",
            "hive",
            "firestore",
            "shared",
            "compartid",
            "partagé",
            "fallback"
        )

        AppLanguage.entries.forEach { language ->
            AiGenerationFailureReason.entries.forEach { reason ->
                val message = contextualAiGenerationFailureMessage(
                    language = language,
                    reason = reason,
                    mealType = MealType.BREAKFAST,
                    audience = MenuAudience.ADULT,
                    profile = DietaryProfile(ageRange = "18+ años")
                )

                forbidden.forEach { term ->
                    assertFalse("$language/$reason leaked $term", message.contains(term, ignoreCase = true))
                }
                assertTrue(message.isNotBlank())
            }
        }
    }
}
```

- [ ] **Step 2: Run the formatter tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.AiGenerationFailureMessageTest"
```

Expected: compilation fails because `AiGenerationFailureReason` and
`contextualAiGenerationFailureMessage` do not exist.

- [ ] **Step 3: Implement the pure formatter**

Create `AiGenerationFailureMessage.kt`:

```kotlin
package com.menudado.ui

import com.menudado.domain.AppLanguage
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import com.menudado.domain.localizedLabel

internal enum class AiGenerationFailureReason {
    DAILY_LIMIT,
    HIGH_DEMAND,
    TIMEOUT,
    CONNECTION,
    SERVICE_UNAVAILABLE
}

internal fun contextualAiGenerationFailureMessage(
    language: AppLanguage,
    reason: AiGenerationFailureReason,
    mealType: MealType,
    audience: MenuAudience,
    profile: DietaryProfile
): String {
    val ageRange = profile.ageRange.trim().ifBlank { audience.defaultAgeRange }
    val context = "${mealType.localizedLabel(language)} · " +
        "${audience.localizedLabel(language)} ($ageRange)"
    val profileSuffix = if (profile.hasRestrictions) {
        when (language) {
            AppLanguage.SPANISH -> " con tu perfil actual"
            AppLanguage.ENGLISH -> " with your current profile"
            AppLanguage.FRENCH -> " avec votre profil actuel"
        }
    } else {
        ""
    }

    return when (language) {
        AppLanguage.SPANISH -> when (reason) {
            AiGenerationFailureReason.DAILY_LIMIT ->
                "Hoy no podemos preparar más ideas para $context. Vuelve a intentarlo mañana."
            AiGenerationFailureReason.HIGH_DEMAND ->
                "La IA está con mucha demanda y no pudo preparar una idea para " +
                    "$context$profileSuffix. Inténtalo más tarde."
            AiGenerationFailureReason.TIMEOUT ->
                "La IA tardó demasiado y no pudo preparar una idea para " +
                    "$context$profileSuffix. Revisa tu conexión e inténtalo de nuevo."
            AiGenerationFailureReason.CONNECTION ->
                "No pudimos preparar una idea para $context$profileSuffix. " +
                    "Revisa tu conexión e inténtalo de nuevo."
            AiGenerationFailureReason.SERVICE_UNAVAILABLE ->
                "No pudimos preparar una idea para $context$profileSuffix en este momento. " +
                    "Inténtalo nuevamente más tarde."
        }
        AppLanguage.ENGLISH -> when (reason) {
            AiGenerationFailureReason.DAILY_LIMIT ->
                "We can't prepare more ideas today for $context. Try again tomorrow."
            AiGenerationFailureReason.HIGH_DEMAND ->
                "AI is in high demand and couldn't prepare an idea for " +
                    "$context$profileSuffix. Try again later."
            AiGenerationFailureReason.TIMEOUT ->
                "AI took too long and couldn't prepare an idea for " +
                    "$context$profileSuffix. Check your connection and try again."
            AiGenerationFailureReason.CONNECTION ->
                "We couldn't prepare an idea for $context$profileSuffix. " +
                    "Check your connection and try again."
            AiGenerationFailureReason.SERVICE_UNAVAILABLE ->
                "We couldn't prepare an idea for $context$profileSuffix right now. " +
                    "Try again later."
        }
        AppLanguage.FRENCH -> when (reason) {
            AiGenerationFailureReason.DAILY_LIMIT ->
                "Nous ne pouvons pas préparer plus d'idées aujourd'hui pour $context. " +
                    "Réessayez demain."
            AiGenerationFailureReason.HIGH_DEMAND ->
                "L'IA est très demandée et n'a pas pu préparer d'idée pour " +
                    "$context$profileSuffix. Réessayez plus tard."
            AiGenerationFailureReason.TIMEOUT ->
                "L'IA a mis trop de temps et n'a pas pu préparer d'idée pour " +
                    "$context$profileSuffix. Vérifiez votre connexion et réessayez."
            AiGenerationFailureReason.CONNECTION ->
                "Nous n'avons pas pu préparer d'idée pour $context$profileSuffix. " +
                    "Vérifiez votre connexion et réessayez."
            AiGenerationFailureReason.SERVICE_UNAVAILABLE ->
                "Nous n'avons pas pu préparer d'idée pour $context$profileSuffix pour le moment. " +
                    "Réessayez plus tard."
        }
    }
}
```

- [ ] **Step 4: Run the formatter tests and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.AiGenerationFailureMessageTest"
```

Expected: all formatter tests pass.

- [ ] **Step 5: Commit the formatter**

```bash
git add \
  app/src/main/java/com/menudado/ui/AiGenerationFailureMessage.kt \
  app/src/test/java/com/menudado/ui/AiGenerationFailureMessageTest.kt
git commit -m "feat: add contextual AI failure messages"
```

### Task 3: Present contextual copy only when generation has no result

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`

- [ ] **Step 1: Replace the old fallback-miss assertion with failing contextual tests**

Replace `hive miss preserves original localized failure` and add the daily-cap
case:

```kotlin
@Test
fun `provider failure and hive miss show requested baby context`() = runTest(dispatcher) {
    analyzer.generateFailure = IllegalStateException("internal")
    hive.searchResult = Result.success(null)
    viewModel.setDietaryProfileAudience(MenuAudience.BABY)
    viewModel.setDietaryProfileAudienceEnabled(true)
    viewModel.updateDietaryProfileAgeRange("8-10 meses")
    viewModel.setFormMealType(MealType.LUNCH)
    viewModel.setFormAudience(MenuAudience.BABY)

    viewModel.generateMenuIdea()
    advanceUntilIdle()

    assertEquals(
        "No pudimos preparar una idea para Almuerzo · Bebé (8-10 meses) " +
            "en este momento. Inténtalo nuevamente más tarde.",
        viewModel.uiState.value.message
    )
}

@Test
fun `provider cap hard limit shows context without offering an ad`() = runTest(dispatcher) {
    scopedAiUsageStore.seed(LOCAL_ACCOUNT_AI_USAGE_SCOPE, "2026-06-10", usedCount = 10)
    scopedAiUsageStore.seed(
        PROVIDER_AI_USAGE_SCOPE,
        "2026-06-10",
        usedCount = AI_PROVIDER_DAILY_HARD_LIMIT
    )
    viewModel.setDietaryProfileAudience(MenuAudience.CHILD)
    viewModel.setDietaryProfileAudienceEnabled(true)
    viewModel.setFormMealType(MealType.DINNER)
    viewModel.setFormAudience(MenuAudience.CHILD)
    viewModel.updateGuestAccess(false, true, true)

    viewModel.generateMenuIdea()

    assertEquals(
        "Hoy no podemos preparar más ideas para Cena · Peques (2-12 años). " +
            "Vuelve a intentarlo mañana.",
        viewModel.uiState.value.message
    )
    assertFalse(viewModel.uiState.value.canRequestRewardedGeneration)
}
```

- [ ] **Step 2: Run the two tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest.provider failure and hive miss show requested baby context" \
  --tests "com.menudado.ui.MenuDadoViewModelTest.provider cap hard limit shows context without offering an ad"
```

Expected: both tests fail with the old generic messages.

- [ ] **Step 3: Carry a generation presentation reason through failures**

Extend `AiFailureNotice`:

```kotlin
private data class AiFailureNotice(
    val message: String,
    val retryAtMillis: Long? = null,
    val generationReason: AiGenerationFailureReason
)
```

Replace `toAiFailureNotice()` with:

```kotlin
private fun Throwable.toAiFailureNotice(
    nowMillis: Long,
    language: AppLanguage
): AiFailureNotice {
    val text = listOfNotNull(message, cause?.message).joinToString(" ").lowercase()
    return when {
        isAiQuotaExceeded() -> {
            val quotaLimitType = classifyAiQuotaLimitType(text)
            AiFailureNotice(
                message = quotaLimitType.message(language),
                retryAtMillis = text.retryAtMillis(nowMillis)
                    ?: nextPacificMidnightMillis(nowMillis),
                generationReason = AiGenerationFailureReason.HIGH_DEMAND
            )
        }
        this is ServiceDisabledException ||
            this is APINotConfiguredException ||
            "service_disabled" in text ||
            "api_key_service_blocked" in text -> AiFailureNotice(
                message = language.aiConfigurationMessage(),
                generationReason = AiGenerationFailureReason.SERVICE_UNAVAILABLE
            )
        this is InvalidAPIKeyException ||
            "api key not valid" in text -> AiFailureNotice(
                message = language.aiInvalidApiKeyMessage(),
                generationReason = AiGenerationFailureReason.SERVICE_UNAVAILABLE
            )
        this is RequestTimeoutException ||
            this is TimeoutCancellationException ||
            "timeout" in text ||
            "timed out" in text -> AiFailureNotice(
                message = language.aiTimeoutMessage(),
                generationReason = AiGenerationFailureReason.TIMEOUT
            )
        text.isAiProviderInternalFailure() -> AiFailureNotice(
            message = language.aiTemporaryServiceMessage(),
            generationReason = AiGenerationFailureReason.SERVICE_UNAVAILABLE
        )
        else -> AiFailureNotice(
            message = language.aiGenericFailureMessage(),
            generationReason = AiGenerationFailureReason.CONNECTION
        )
    }
}
```

Retain each existing `message` value because analysis paths still use the
current generic copy.

- [ ] **Step 4: Render request context after an internal fallback miss**

In `searchHiveFallback()`, replace the `fallback == null` branch with:

```kotlin
if (fallback == null) {
    val reason = preparedFailureNotice?.generationReason
        ?: AiGenerationFailureReason.DAILY_LIMIT
    val contextualMessage = contextualAiGenerationFailureMessage(
        language = currentLanguage(),
        reason = reason,
        mealType = request.mealType,
        audience = request.audience,
        profile = request.profile
    )
    if (preparedFailureNotice != null) {
        showPreparedAiFailureNotice(
            preparedFailureNotice.copy(message = contextualMessage)
        )
    } else {
        _uiState.update {
            it.copy(
                message = contextualMessage,
                isAiRetryNoticeVisible = false
            )
        }
    }
}
```

Do not modify the success branch: a valid result must continue clearing the
message and opening the detail.

- [ ] **Step 5: Make direct hard-limit notices generation-aware**

Change the helper signature:

```kotlin
private fun showAiHardLimitNotice(
    source: String,
    request: ValidatedGenerationRequest? = null
) {
    val retryAtMillis = nextPacificMidnightMillis(clockMillisProvider())
    val message = request?.let {
        contextualAiGenerationFailureMessage(
            language = currentLanguage(),
            reason = AiGenerationFailureReason.DAILY_LIMIT,
            mealType = it.mealType,
            audience = it.audience,
            profile = it.profile
        )
    } ?: currentLanguage().aiLocalDailyLimitMessage()
    pendingRewardedGenerationRequest = null
    _uiState.update {
        it.copy(
            message = message,
            aiRetryAtMillis = retryAtMillis,
            isAiRequestThrottlePause = false,
            isAiRetryNoticeVisible = true,
            aiUsesRemainingToday = 0,
            aiGenerationUsesRemainingToday = 0,
            aiAnalysisUsesRemainingToday = 0,
            aiGenerationLimitState = AiGenerationLimitState.HARD_LIMIT,
            isRewardedGenerationPending = false
        )
    }
    scheduleAiRetryRefresh(retryAtMillis)
    analytics.trackAiDailyLimitReached(source)
}
```

Pass `request` from both generation call sites:

```kotlin
AiGenerationAccess.HARD_LIMIT ->
    showAiHardLimitNotice(AI_SOURCE_GENERATE_MENU, request)
```

and:

```kotlin
if (ledger.earnedCount >= MAX_REWARDED_AI_CREDITS_PER_DAY) {
    showAiHardLimitNotice(AI_SOURCE_GENERATE_MENU, request)
    return
}
```

- [ ] **Step 6: Run ViewModel and formatter tests and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest" \
  --tests "com.menudado.ui.AiGenerationFailureMessageTest"
```

Expected: all selected tests pass, including fallback-hit, retry scheduling,
analytics and rewarded-credit regression tests.

- [ ] **Step 7: Commit the integration**

```bash
git add \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "fix: contextualize AI generation failures"
```

### Task 4: Update project context and run final verification

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Update the functional contract**

Update the AI fallback and rewarded-ad sections to state:

```markdown
- Si la generación no produce un resultado, el aviso diferencia límite diario,
  alta demanda, timeout, conectividad o indisponibilidad temporal y muestra
  únicamente tipo de comida, público y rango de edad. Cuando existen
  restricciones añade `con tu perfil actual`, sin enumerarlas. La UI nunca
  menciona Firestore, contenido compartido, fallback ni la existencia o ausencia
  de candidatos internos.
- Al alcanzar el máximo técnico de 20 llamadas, no se ofrecen nuevos anuncios
  bonificados. Los usos gratuitos y créditos obtenidos previamente conservan su
  autorización y pueden resolverse sin otra llamada al proveedor.
```

Keep the internal architecture paragraphs about `sharedAiMenus`; they are
developer documentation, not user-visible copy.

- [ ] **Step 2: Run formatting and focused unit tests**

Run:

```bash
git diff --check
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.AiGenerationFailureMessageTest" \
  --tests "com.menudado.ui.MenuDadoViewModelTest" \
  --tests "com.menudado.domain.AiDailyUsagePolicyTest"
```

Expected: no whitespace errors and all selected tests pass.

- [ ] **Step 3: Run the complete debug unit suite**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Compile the debug app**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL` and a fresh APK under
`app/build/outputs/apk/debug/`.

- [ ] **Step 5: Inspect the final diff and privacy contract**

Run:

```bash
git diff --check
git status --short
rg -n "colmena|hive|Firestore|shared|fallback" \
  app/src/main/java/com/menudado/ui/AiGenerationFailureMessage.kt
```

Expected: the source formatter search returns no matches; status contains only
the intended implementation and context changes.

- [ ] **Step 6: Commit documentation**

```bash
git add docs/project-context.md
git commit -m "docs: document private AI failure messaging"
```

- [ ] **Step 7: Perform guided device QA**

On `com.menudado.debug`:

1. Set the provider daily counter to 20 in a controlled debug state.
2. Exhaust the active scope's free balance with no earned credit.
3. Select each audience once and confirm the CTA does not offer a rewarded ad.
4. Confirm the daily notice contains meal, localized audience and effective age.
5. With provider capacity restored, force timeout, internal failure and generic
   connectivity failures with an empty internal fallback.
6. Confirm each notice gives the correct action and never exposes internal
   fallback terminology or profile details.
7. Force a valid fallback hit and confirm it opens the generated-menu detail
   without displaying a failure notice.

Record manual QA separately from automated build/test evidence. If fault
injection is unavailable in the installed build, report those cases as pending
rather than inferring success from unit tests.
