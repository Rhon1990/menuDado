# Onboarding Activation and Analytics Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refresh onboarding version 6, align its CTA with Home, and measure new-install versus upgrade activation safely in Firebase Analytics.

**Architecture:** Keep the existing single Compose dialog, local-first `OnboardingStore`, Firestore synchronization path, and stable CTA identifiers. Extend the analytics boundary with explicit version and exposure parameters, derive exposure from existing onboarding completion state, and configure only the prospective GA4 definitions needed to analyze the activation funnel.

**Tech Stack:** Kotlin, Jetpack Compose, Android resources, StateFlow/ViewModel, Firebase Analytics, Firestore synchronization, JUnit 4, kotlinx-coroutines-test, Gradle.

---

## File Structure

- `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`: typed onboarding analytics contract and no-op implementation.
- `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`: Firebase event parameter serialization.
- `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`: onboarding version, exposure derivation, visibility, completion, and event emission.
- `app/src/main/res/values/strings.xml`: Spanish onboarding copy.
- `app/src/main/res/values-en/strings.xml`: English onboarding copy.
- `app/src/main/res/values-fr/strings.xml`: French onboarding copy.
- `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`: onboarding state and analytics regression coverage.
- `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: single-screen presentation and stable CTA contract coverage.
- `docs/project-context.md`: current onboarding and Analytics behavior.

No new production file, Firestore document, collection, rule, index, Room
migration, navigation route, or AI request path is needed.

### Task 1: Define the enriched onboarding analytics contract

**Files:**
- Modify: `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt:80-86`
- Modify: `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt:200-206`
- Modify: `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt:207-215`
- Modify: `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt:370-455`
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt:4279-4285`

- [ ] **Step 1: Update the recording fake first so callers require the new payload**

Replace the two onboarding methods in `RecordingMenuDadoAnalytics` with:

```kotlin
override fun trackOnboardingShown(contentVersion: Int, exposureType: String) {
    events += "onboarding_shown:$contentVersion:$exposureType"
}

override fun trackOnboardingCompleted(
    action: String,
    contentVersion: Int,
    exposureType: String
) {
    events += "onboarding_completed:$action:$contentVersion:$exposureType"
}
```

- [ ] **Step 2: Run the focused test compilation and verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuDadoViewModelTest'
```

Expected: compilation fails because `MenuDadoAnalytics` still declares the
parameterless `trackOnboardingShown` and one-parameter
`trackOnboardingCompleted`.

- [ ] **Step 3: Change the production analytics interface**

Use these exact signatures in `MenuDadoAnalytics`:

```kotlin
fun trackOnboardingShown(contentVersion: Int, exposureType: String)

fun trackOnboardingCompleted(
    action: String,
    contentVersion: Int,
    exposureType: String
)
```

Use matching no-op methods:

```kotlin
override fun trackOnboardingShown(contentVersion: Int, exposureType: String) = Unit

override fun trackOnboardingCompleted(
    action: String,
    contentVersion: Int,
    exposureType: String
) = Unit
```

- [ ] **Step 4: Serialize the new parameters in Firebase**

Replace the Firebase adapter methods with:

```kotlin
override fun trackOnboardingShown(contentVersion: Int, exposureType: String) {
    logEvent(EVENT_ONBOARDING_SHOWN) {
        putString(PARAM_ONBOARDING_VERSION, onboardingVersionDimensionValue(contentVersion))
        putString(PARAM_EXPOSURE_TYPE, exposureType.sanitized())
    }
}

override fun trackOnboardingCompleted(
    action: String,
    contentVersion: Int,
    exposureType: String
) {
    logEvent(EVENT_ONBOARDING_COMPLETED) {
        putString(PARAM_ACTION, action.sanitized())
        putString(PARAM_ONBOARDING_VERSION, onboardingVersionDimensionValue(contentVersion))
        putString(PARAM_EXPOSURE_TYPE, exposureType.sanitized())
    }
}

internal fun onboardingVersionDimensionValue(contentVersion: Int): String =
    "v$contentVersion"
```

Add these parameter constants next to the existing onboarding/action constants:

```kotlin
const val PARAM_ONBOARDING_VERSION = "onboarding_version"
const val PARAM_EXPOSURE_TYPE = "exposure_type"
```

- [ ] **Step 5: Run Kotlin compilation to expose the unmodified ViewModel calls**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: compilation fails only at the old onboarding calls in
`MenuDadoViewModel`.

### Task 2: Implement version 6 and exposure-aware tracking

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:215-245`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:344-370`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:1607-1613`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:2645-2660`
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt:1380-1480`

- [ ] **Step 1: Change onboarding expectations to version and exposure**

Update the existing onboarding tests so their expected event strings are:

```kotlin
"onboarding_shown:6:new_install"
"onboarding_shown:6:upgrade"
"onboarding_completed:start:6:new_install"
"onboarding_completed:skip:6:new_install"
```

Update the old-version test to start with version `5` and assert:

```kotlin
assertEquals(6, previousContentStore.completedVersion)
```

Add this regression test:

```kotlin
@Test
fun `completed onboarding version six is not shown or tracked`() = runTest(dispatcher) {
    analytics.events.clear()

    val completedViewModel = MenuDadoViewModel(
        repository = MenuRepository(dao, analyzer),
        analytics = analytics,
        aiQuotaRetryStore = aiQuotaRetryStore,
        aiDailyUsageStore = aiDailyUsageStore,
        scopedAiUsageStore = scopedAiUsageStore,
        dietaryProfileStore = dietaryProfileStore,
        onboardingStore = FakeOnboardingStore(completed = true, completedVersion = 6)
    )

    assertEquals(false, completedViewModel.uiState.value.showOnboarding)
    assertEquals(emptyList<String>(), analytics.events)
}
```

- [ ] **Step 2: Run the four onboarding tests and verify failure**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuDadoViewModelTest.*onboarding*'
```

Expected: failures show version `5`, missing exposure values, or old event
strings.

- [ ] **Step 3: Add a closed exposure value owned by the ViewModel**

Add a private field before `init`:

```kotlin
private var onboardingExposureType = ONBOARDING_EXPOSURE_NEW_INSTALL
```

Replace `refreshOnboarding` with:

```kotlin
private fun refreshOnboarding() {
    val hadPreviousOnboarding = onboardingStore.isOnboardingCompleted(
        LEGACY_ONBOARDING_BASELINE_VERSION
    )
    val shouldShowOnboarding = !onboardingStore.isOnboardingCompleted(
        CURRENT_ONBOARDING_VERSION
    )
    onboardingExposureType = if (hadPreviousOnboarding) {
        ONBOARDING_EXPOSURE_UPGRADE
    } else {
        ONBOARDING_EXPOSURE_NEW_INSTALL
    }
    _uiState.update { it.copy(showOnboarding = shouldShowOnboarding) }
    if (shouldShowOnboarding) {
        analytics.trackOnboardingShown(
            contentVersion = CURRENT_ONBOARDING_VERSION,
            exposureType = onboardingExposureType
        )
    }
}
```

Replace the analytics call in `completeOnboarding` with:

```kotlin
analytics.trackOnboardingCompleted(
    action = action,
    contentVersion = CURRENT_ONBOARDING_VERSION,
    exposureType = onboardingExposureType
)
```

Use these constants:

```kotlin
private const val LEGACY_ONBOARDING_BASELINE_VERSION = 1
private const val CURRENT_ONBOARDING_VERSION = 6
private const val ONBOARDING_EXPOSURE_NEW_INSTALL = "new_install"
private const val ONBOARDING_EXPOSURE_UPGRADE = "upgrade"
```

- [ ] **Step 4: Run focused tests and verify they pass**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuDadoViewModelTest.*onboarding*'
```

Expected: all onboarding tests pass.

- [ ] **Step 5: Run the complete ViewModel test class**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuDadoViewModelTest'
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the version and tracking behavior**

```bash
git add app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "feat: track onboarding v6 exposure"
```

### Task 3: Refresh localized onboarding copy and preserve CTA semantics

**Files:**
- Modify: `app/src/main/res/values/strings.xml:248-253`
- Modify: `app/src/main/res/values-en/strings.xml:246-251`
- Modify: `app/src/main/res/values-fr/strings.xml:246-251`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt:1230-1242`
- Verify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:560-570`
- Verify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:7060-7088`
- Verify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:7145-7150`

- [ ] **Step 1: Strengthen the UI-state test before changing resources**

Rename the existing onboarding test to:

```kotlin
@Test
fun `onboarding v6 remains one activation page with branded hierarchy`() {
    val steps = onboardingSteps()

    assertEquals(1, steps.size)
    assertEquals(R.string.onboarding_activation_title, steps.single().titleRes)
    assertEquals(R.string.onboarding_activation_body, steps.single().bodyRes)
    assertEquals(MenuDadoColors.ActionTerracotta, onboardingPrimaryActionColor())
    assertEquals(MenuDadoColors.Surface, onboardingContainerColor())
    assertEquals(24, onboardingContainerCornerRadiusDp())
}
```

Add CTA helper assertions that verify:

```kotlin
assertEquals("create_first_menu", onboardingStartCta())
assertEquals("explore_without_onboarding", onboardingSkipCta())
```

- [ ] **Step 2: Run the UI-state test and verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuCardUiStateTest.*onboarding*'
```

Expected: test compilation fails because `onboardingStartCta` and
`onboardingSkipCta` do not exist.

- [ ] **Step 3: Expose stable CTA values through pure helpers**

Add these helpers in `MenuDadoScreen.kt`:

```kotlin
internal fun onboardingStartCta(): String = "create_first_menu"

internal fun onboardingSkipCta(): String = "explore_without_onboarding"
```

Make both onboarding click handlers pass these helpers to
`viewModel.trackCtaTapped` and remove the two private onboarding CTA constants.

Use a scrollable content modifier so larger fonts and short windows retain
access to both actions:

```kotlin
internal fun onboardingContentModifier(scrollState: ScrollState): Modifier =
    Modifier
        .padding(24.dp)
        .verticalScroll(scrollState)
```

- [ ] **Step 4: Run the UI-state test and verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuCardUiStateTest.*onboarding*'
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Replace Spanish resources**

Use:

```xml
<string name="onboarding_activation_title">Decide qué preparar hoy</string>
<string name="onboarding_activation_body">Recibe una idea saludable con IA, elige entre tus menús guardados y prepara tu lista de mercado.</string>
<string name="onboarding_create_first_menu">Ayúdame a elegir</string>
<string name="onboarding_explore">Explorar la app</string>
```

- [ ] **Step 6: Replace English resources**

Use:

```xml
<string name="onboarding_activation_title">Decide what to make today</string>
<string name="onboarding_activation_body">Get a healthy AI-assisted idea, choose from your saved menus, and prepare your shopping list.</string>
<string name="onboarding_create_first_menu">Help me choose</string>
<string name="onboarding_explore">Explore the app</string>
```

- [ ] **Step 7: Replace French resources**

Use:

```xml
<string name="onboarding_activation_title">Décidez quoi préparer aujourd’hui</string>
<string name="onboarding_activation_body">Obtenez une idée saine avec l’IA, choisissez parmi vos menus enregistrés et préparez votre liste de courses.</string>
<string name="onboarding_create_first_menu">Aidez-moi à choisir</string>
<string name="onboarding_explore">Explorer l’application</string>
```

- [ ] **Step 8: Verify resources, tests, and compilation**

Run:

```bash
rg -n 'onboarding_(activation_title|activation_body|create_first_menu|explore)' \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuCardUiStateTest.*onboarding*'
./gradlew :app:compileDebugKotlin
```

Expected: twelve localized resource lines are present and both Gradle commands
finish with `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit localized UX**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml \
  app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: refresh onboarding activation copy"
```

### Task 4: Update project context and run regression validation

**Files:**
- Modify: `docs/project-context.md:189-193`
- Modify: `docs/project-context.md:252-265`
- Modify: `docs/project-context.md:328-330`

- [ ] **Step 1: Update the functional onboarding contract**

Document:

- version `6`;
- one-screen explanation of AI help, saved-menu choice, and market list;
- primary CTA `Ayúdame a elegir`;
- secondary action `Explorar la app`;
- one-time exposure for both new installations and users who completed an older
  content version;
- no automatic AI request from the CTA.

- [ ] **Step 2: Update the analytics contract**

Document:

- `onboarding_version` and `exposure_type`;
- `new_install` and `upgrade` as the only exposure values;
- stable CTA IDs despite visible-copy changes;
- `first_menu_created` as the primary key event;
- no duplicate favorite or market-list events.

- [ ] **Step 3: Run full relevant automated verification**

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
git diff --check
```

Expected: both Gradle commands report `BUILD SUCCESSFUL`; diff check produces no
output.

- [ ] **Step 4: Inspect scope and privacy**

Run:

```bash
git status --short
git diff --stat
git diff -- \
  app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/main/java/com/menudado/ui/MenuDadoScreen.kt \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt \
  app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt \
  docs/project-context.md
```

Expected: no menu text, profile detail, UID, document ID, email, image URI, or
new Firestore schema appears in Analytics calls.

- [ ] **Step 5: Commit documentation**

```bash
git add docs/project-context.md
git commit -m "docs: describe onboarding v6 funnel"
```

### Task 5: Verify prospective Firebase Analytics configuration

**External scope:**
- Firebase project: confirm exact production project before changing state.
- GA4 property: use the property linked to the confirmed MenuDado production
  Firebase project.

- [ ] **Step 1: Confirm Firebase CLI identity and project mapping read-only**

Run:

```bash
firebase use
firebase projects:list
```

Expected: the production alias resolves to `menudado-6a2da`. Stop without
changing Analytics if the mapping differs.

- [ ] **Step 2: Inspect existing custom definitions in the linked GA4 property**

Using the authenticated Firebase/Google Analytics console, verify event-scoped
custom dimensions for:

```text
screen -> screen
cta -> cta
action -> action
onboarding_version -> onboarding_version
exposure_type -> exposure_type
```

Expected: existing definitions are reused exactly; only missing definitions are
created. No user-scoped definition is used.

- [ ] **Step 3: Verify the primary key event**

In the linked GA4 property, mark `first_menu_created` as a key event if it is not
already marked.

Expected: `onboarding_completed` remains a regular event and
`first_menu_created` is the activation key event.

- [ ] **Step 4: Validate runtime telemetry when a debug-capable build is available**

In Analytics DebugView, complete these two paths:

```text
new install -> onboarding shown -> start -> first menu created
version 5 state -> onboarding shown -> skip
```

Expected:

```text
onboarding_shown: onboarding_version=v6, exposure_type=new_install|upgrade
onboarding_completed: action=start|skip, onboarding_version=v6,
  exposure_type=new_install|upgrade
cta_tapped: screen=onboarding,
  cta=create_first_menu|explore_without_onboarding
```

If no emulator or device is connected, report DebugView as pending manual QA;
do not infer runtime delivery from a successful build.

- [ ] **Step 5: Record final evidence**

Report separately:

- Android code/tests/build;
- Firestore schema and rules unchanged;
- GA4 custom dimensions created, already present, or blocked by access;
- `first_menu_created` key-event status;
- DebugView/manual device validation status.

No Firebase deployment is required because the Android event schema ships with
the app and this change does not modify Firestore rules, indexes, Remote Config,
Functions, Hosting, Auth, or AI Logic.

### Task 6: Final branch verification

**Files:** no new modifications expected.

- [ ] **Step 1: Run completion checks**

```bash
git diff --check
git status --short --branch
git log -6 --oneline --decorate
```

Expected: no unstaged implementation changes, the branch contains the design,
plan, analytics, UX, and context commits, and unrelated user changes were not
reverted.

- [ ] **Step 2: Summarize residual QA**

State explicitly whether:

- an emulator/device was available for large-font and TalkBack checks;
- Analytics DebugView was observed;
- GA4 changes were confirmed in production;
- the branch was pushed.

Do not report any unverified item as complete.
