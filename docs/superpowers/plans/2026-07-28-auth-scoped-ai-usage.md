# Auth-Scoped AI Usage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep free AI usage and rewarded-video credits independent between the device guest and each authenticated MenuDado account, while retaining a shared 20-request Firebase provider safeguard.

**Architecture:** Add a local store keyed by a stable usage scope (`guest` or `account:<uid>`) for product entitlements and provider attempts. Scope the existing rewarded ledger with the same key. `MenuDadoViewModel` will consume the active scope, record a provider attempt only when Firebase is called, and route generation directly to the existing hive fallback after the provider safeguard is exhausted.

**Tech Stack:** Kotlin, Android SharedPreferences, Jetpack ViewModel/Compose, coroutines, JUnit 4, Firebase AI Logic, Firestore hive fallback.

---

### Task 1: Scoped usage persistence

**Files:**
- Create: `app/src/main/java/com/menudado/data/ScopedAiUsageStore.kt`
- Create: `app/src/test/java/com/menudado/data/ScopedAiUsageStoreTest.kt`

- [ ] **Step 1: Write failing store tests**

Cover independent `guest`, `account:user-a`, `account:user-b`, date rollover,
clamping, provider scope, and one-time migration from the legacy unscoped count.

```kotlin
@Test
fun `guest and account usage remain independent`() {
    val store = SharedPreferencesScopedAiUsageStore(FakeContext())
    store.saveUsageState("guest", AiDailyUsageState("2026-07-28", 5))
    store.saveUsageState("account:user-a", AiDailyUsageState("2026-07-28", 3))

    assertEquals(5, store.getUsageState("guest", "2026-07-28").usedCount)
    assertEquals(3, store.getUsageState("account:user-a", "2026-07-28").usedCount)
    assertEquals(0, store.getUsageState("account:user-b", "2026-07-28").usedCount)
}
```

- [ ] **Step 2: Verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.data.ScopedAiUsageStoreTest
```

Expected: compilation failure because `ScopedAiUsageStore` does not exist.

- [ ] **Step 3: Implement the scoped store**

Define stable scope helpers and a storage contract:

```kotlin
const val GUEST_AI_USAGE_SCOPE = "guest"
const val PROVIDER_AI_USAGE_SCOPE = "provider"

fun accountAiUsageScope(userId: String): String = "account:$userId"

interface ScopedAiUsageStore {
    fun getUsageState(scope: String, dateKey: String): AiDailyUsageState
    fun saveUsageState(scope: String, state: AiDailyUsageState)
    fun migrateLegacyUsage(scope: String, legacyState: AiDailyUsageState?): Boolean
}
```

Persist date/count under scope-specific SharedPreferences keys. Migration copies
the legacy count into the active entitlement scope and provider scope only once.

- [ ] **Step 4: Verify GREEN**

Run the focused store test and expect all tests to pass.

### Task 2: Scope rewarded-video ledgers

**Files:**
- Modify: `app/src/main/java/com/menudado/data/RewardedAiCreditStore.kt`
- Modify: `app/src/test/java/com/menudado/data/RewardedAiCreditStoreTest.kt`

- [ ] **Step 1: Write failing rewarded-ledger tests**

```kotlin
@Test
fun `guest rewards do not reduce account rewards`() {
    val store = SharedPreferencesRewardedAiCreditStore(FakeContext())
    store.saveLedger("guest", RewardedAiCreditLedger("2026-07-28", 10, 10))

    assertEquals(0, store.getLedger("account:user-a", "2026-07-28").earnedCount)
}
```

Also verify account A/account B isolation, date rollover, clamping, and one-time
legacy ledger migration into the first resolved scope.

- [ ] **Step 2: Verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.data.RewardedAiCreditStoreTest
```

Expected: signature mismatch because the store is not scoped.

- [ ] **Step 3: Implement scoped ledger methods**

Change the contract to:

```kotlin
interface RewardedAiCreditStore {
    fun getLedger(scope: String, dateKey: String): RewardedAiCreditLedger
    fun saveLedger(scope: String, ledger: RewardedAiCreditLedger)
}
```

Use scope-specific preference keys and migrate the old unscoped keys once.

- [ ] **Step 4: Verify GREEN**

Run the focused rewarded store tests and expect all tests to pass.

### Task 3: Reproduce identity-switch regressions

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/test/java/com/menudado/domain/AiDailyUsagePolicyTest.kt`

- [ ] **Step 1: Add failing ViewModel tests**

Add tests proving:

```kotlin
@Test
fun `five guest uses leave ten free uses after sign in`() = runTest(dispatcher) {
    viewModel.updateGuestAccess(true, true, true, userId = "anonymous")
    scopedUsageStore.seed("guest", usedCount = 5)

    viewModel.updateGuestAccess(false, true, true, userId = "user-a")

    assertEquals(10, viewModel.uiState.value.aiGenerationUsesRemainingToday)
}
```

Add corresponding cases for guest rewards versus account rewards, restoring the
guest balance after logout, and keeping two authenticated UIDs independent.

- [ ] **Step 2: Add provider-safeguard tests**

Seed 20 provider attempts and a safe hive candidate. Assert that generation
consumes the active entitlement, does not call `HealthAnalyzer.generate`, and
shows the hive menu. Assert that analysis remains blocked without an analyzer
call.

- [ ] **Step 3: Verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest --tests com.menudado.domain.AiDailyUsagePolicyTest
```

Expected: failures showing the current shared free counter, shared rewarded
ledger, and hard block before hive.

### Task 4: Route ViewModel usage through the active scope

**Files:**
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Modify: `app/src/main/java/com/menudado/MainActivity.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/main/java/com/menudado/domain/AiDailyUsagePolicy.kt`

- [ ] **Step 1: Inject the scoped store and resolved initial scope**

Create `SharedPreferencesScopedAiUsageStore` in `MenuDadoApplication`. Pass it
and the initial auth-derived scope to the ViewModel factory. Extend
`updateGuestAccess` with `userId` and select:

```kotlin
activeAiUsageScope = if (isGuest) {
    GUEST_AI_USAGE_SCOPE
} else {
    accountAiUsageScope(requireNotNull(userId))
}
```

- [ ] **Step 2: Separate entitlement and provider consumption**

Replace the shared counter reads with:

```kotlin
private fun currentScopedAiUsedCount(dateKey: String): Int =
    scopedAiUsageStore.getUsageState(activeAiUsageScope, dateKey).usedCount

private fun currentProviderAiUsedCount(dateKey: String): Int =
    scopedAiUsageStore.getUsageState(PROVIDER_AI_USAGE_SCOPE, dateKey).usedCount
```

Every generation or analysis consumes the current entitlement scope. Increment
the provider scope only immediately before a real Firebase analyzer call.

- [ ] **Step 3: Scope every rewarded ledger operation**

Update offer, award, consumption, remaining-credit, and analytics paths to call
`getLedger(activeAiUsageScope, dateKey)` and
`saveLedger(activeAiUsageScope, ledger)`.

- [ ] **Step 4: Preserve the provider safeguard with hive fallback**

Extract the existing hive lookup/render block into a suspend helper. When
provider usage is below 20, call Firebase and fall back on failure. When it is
already 20, skip Firebase and call that helper directly without creating a
quota retry lock. Analysis checks both free entitlement and provider capacity.

- [ ] **Step 5: Migrate legacy state after scope resolution**

Call `migrateLegacyUsage(activeAiUsageScope, aiDailyUsageStore.getUsageState())`
once. Keep the old backend store as a signed-in compatibility mirror only;
guest actions must never write it.

- [ ] **Step 6: Verify GREEN**

Run the focused ViewModel, policy, and persistence tests and expect all to pass.

### Task 5: Context documentation and regression validation

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Update project context**

Document that guest/account free and rewarded balances are independent, keyed
by identity, while the 20-request provider safeguard remains device-wide and
generation uses the hive after exhaustion.

- [ ] **Step 2: Run targeted tests**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.data.ScopedAiUsageStoreTest --tests com.menudado.data.RewardedAiCreditStoreTest --tests com.menudado.domain.AiDailyUsagePolicyTest --tests com.menudado.ui.MenuDadoViewModelTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run complete unit suite and compilation**

```bash
./gradlew :app:testDebugUnitTest :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Inspect final diff**

Run `git diff --check`, `git status --short`, and review only the scoped quota,
reward, provider safeguard, tests, and project-context changes.
