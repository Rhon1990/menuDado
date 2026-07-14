# Complete UI Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the approved Calm Editorial design system to every MenuDado screen while preserving all current state, navigation, Firebase, Room and Analytics behavior.

**Architecture:** Extend the existing Material 3 theme with semantic colors, typography, shapes and spacing. Migrate existing Compose functions in place and reuse theme tokens rather than restructuring ViewModels or data flow; extract a visual primitive only when the same contract is used by multiple screens.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4, Android Gradle Plugin, existing MenuDado assets.

---

## File map

- Modify `app/src/main/java/com/menudado/ui/theme/MenuDadoTheme.kt`: semantic palette, typography, shapes and spacing tokens.
- Create `app/src/test/java/com/menudado/ui/theme/MenuDadoThemeTest.kt`: token contract tests.
- Modify `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: all app surfaces and states.
- Modify `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: helper expectations and navigation invariants.
- Modify `app/src/main/java/com/menudado/auth/MenuDadoAuthScreen.kt`: shared shapes, fields and CTA hierarchy.
- Modify localized string resources only if visible copy changes.
- Modify `docs/project-context.md`: final visual contract.

No repository, ViewModel, Firebase, Room, Analytics contract or domain file should change.

### Task 1: Define semantic design tokens with TDD

**Files:**
- Create: `app/src/test/java/com/menudado/ui/theme/MenuDadoThemeTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/theme/MenuDadoTheme.kt`

- [ ] **Step 1: Add failing token tests**

Create:

```kotlin
package com.menudado.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuDadoThemeTest {
    @Test
    fun `calm editorial palette keeps action and error roles separate`() {
        assertEquals(Color(0xFFD66548), MenuDadoColors.ActionTerracotta)
        assertEquals(Color(0xFFE7F0EB), MenuDadoColors.SelectionGreen)
        assertEquals(Color(0xFFE35D3E), MenuDadoColors.Tomato)
    }

    @Test
    fun `shape and spacing tokens follow approved scale`() {
        assertEquals(24.dp, MenuDadoUiTokens.CardRadius)
        assertEquals(16.dp, MenuDadoUiTokens.ControlRadius)
        assertEquals(20.dp, MenuDadoUiTokens.NavigationRadius)
        assertEquals(48.dp, MenuDadoUiTokens.MinimumTouchTarget)
        assertEquals(listOf(8.dp, 12.dp, 16.dp, 24.dp, 32.dp), MenuDadoUiTokens.SpacingScale)
    }
}
```

- [ ] **Step 2: Run the focused test and confirm RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.theme.MenuDadoThemeTest
```

Expected: compilation failure because the new tokens do not exist.

- [ ] **Step 3: Implement colors and dimensions**

Add to the theme while preserving every existing dice/status color:

```kotlin
val SelectionGreen = Color(0xFFE7F0EB)
val ActionTerracotta = Color(0xFFD66548)

object MenuDadoUiTokens {
    val CardRadius = 24.dp
    val ControlRadius = 16.dp
    val NavigationRadius = 20.dp
    val MinimumTouchTarget = 48.dp
    val SpacingScale = listOf(8.dp, 12.dp, 16.dp, 24.dp, 32.dp)
}
```

- [ ] **Step 4: Configure Material typography and shapes**

```kotlin
private val MenuDadoTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
)

private val MenuDadoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(MenuDadoUiTokens.ControlRadius),
    large = RoundedCornerShape(MenuDadoUiTokens.CardRadius),
    extraLarge = RoundedCornerShape(28.dp)
)
```

Pass both to `MaterialTheme`.

- [ ] **Step 5: Run GREEN and commit**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.theme.MenuDadoThemeTest
git add app/src/main/java/com/menudado/ui/theme/MenuDadoTheme.kt app/src/test/java/com/menudado/ui/theme/MenuDadoThemeTest.kt
git commit -m "feat: add calm editorial design tokens"
```

### Task 2: Modernize header and bottom navigation

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] **Step 1: Change expectations first**

Update the current navigation/header tests:

```kotlin
assertEquals(MenuDadoColors.Surface, menuDadoBottomNavigationContainerColor())
assertEquals(MenuDadoColors.BrandGreen, menuDadoBottomNavigationContentColor(true))
assertEquals(MenuDadoColors.MutedInk.copy(alpha = 0.72f), menuDadoBottomNavigationContentColor(false))
assertEquals(MenuDadoColors.Background, menuDadoNavigationBarScrimColor())
assertEquals(16, menuDadoBottomNavigationItemCornerRadiusDp())
assertEquals(24, menuDadoBottomNavigationIndicatorWidthDp(true))
assertEquals(3, menuDadoBottomNavigationIndicatorHeightDp())
assertEquals(44, menuDadoHeaderSymbolSizeDp())
```

Keep the destination assertion `HOME`, `PROFILE`, `MY_ZONE` unchanged.

- [ ] **Step 2: Run RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: failures on old colors, radii, indicator and symbol size.

- [ ] **Step 3: Implement helper values**

```kotlin
internal fun menuDadoBottomNavigationContainerColor() = MenuDadoColors.Surface
internal fun menuDadoBottomNavigationContentColor(selected: Boolean) =
    if (selected) MenuDadoColors.BrandGreen else MenuDadoColors.MutedInk.copy(alpha = 0.72f)
internal fun menuDadoBottomNavigationItemCornerRadiusDp() = 16
internal fun menuDadoBottomNavigationIndicatorWidthDp(selected: Boolean) = if (selected) 24 else 0
internal fun menuDadoBottomNavigationIndicatorHeightDp() = 3
internal fun menuDadoNavigationBarScrimColor() = MenuDadoColors.Background
internal fun menuDadoHeaderSymbolSizeDp() = 44
```

- [ ] **Step 4: Apply floating navigation and compact header**

Use a 20 dp rounded Surface navigation container, SoftSand border and 8 dp shadow, while keeping 58 dp touch height and destination callbacks. Reduce header vertical padding and symbol size, preserve the existing green header, cream wordmark, back callback and system-bar behavior.

- [ ] **Step 5: Run GREEN and commit**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: modernize app chrome"
```

### Task 3: Apply hierarchy to onboarding and Home

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Add failing semantic helper tests**

```kotlin
assertEquals(MenuDadoColors.ActionTerracotta, contextualDiceEnabledContainerColor())
assertEquals(MenuDadoColors.ActionTerracotta, onboardingPrimaryActionColor())
assertEquals(MenuDadoColors.Surface, onboardingContainerColor())
assertEquals(24, onboardingContainerCornerRadiusDp())
```

- [ ] **Step 2: Implement minimal helpers**

```kotlin
internal fun contextualDiceEnabledContainerColor() = MenuDadoColors.ActionTerracotta
internal fun onboardingPrimaryActionColor() = MenuDadoColors.ActionTerracotta
internal fun onboardingContainerColor() = MenuDadoColors.Surface
internal fun onboardingContainerCornerRadiusDp() = 24
```

- [ ] **Step 3: Modernize onboarding presentation**

Keep strings, `onFinish`, `onSkip` and Analytics callbacks. Use a 24 dp Surface card, green rounded logo area, spacing from the approved scale, a 16 dp terracotta primary button and a textual explore action. Keep both trust labels.

- [ ] **Step 4: Modernize `Qué comer hoy` and forms**

Use 24 dp Surface section cards, 16 dp controls, SoftSand borders and minimal elevation in `TodayMenuSection`, AI/manual forms and contextual dice. Keep meal type, audience, base ingredients, save/generate/roll callbacks, disabled reason and guest/quota state unchanged. Terracotta is used only for the enabled generation CTA.

- [ ] **Step 5: Verify and commit**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest --tests com.menudado.ui.MenuDadoViewModelTest
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: modernize onboarding and home"
```

### Task 4: Modernize menus, results and states

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Add failing hierarchy and touch-target tests**

```kotlin
assertEquals(MenuDadoColors.ActionTerracotta, generatedMenuPrimaryActionColor())
assertEquals(MenuDadoColors.Surface, generatedMenuContainerColor())
assertEquals(28, generatedMenuContainerCornerRadiusDp())
assertEquals(48, menuPhotoActionButtonSizeDp())
assertEquals(48, menuFavoriteActionButtonSizeDp())
assertEquals(48, menuOverflowActionButtonSizeDp())
```

- [ ] **Step 2: Implement minimal helpers**

```kotlin
internal fun generatedMenuPrimaryActionColor() = MenuDadoColors.ActionTerracotta
internal fun generatedMenuContainerColor() = MenuDadoColors.Surface
internal fun generatedMenuContainerCornerRadiusDp() = 28
internal fun menuPhotoActionButtonSizeDp() = 48
internal fun menuFavoriteActionButtonSizeDp() = 48
internal fun menuOverflowActionButtonSizeDp() = 48
```

- [ ] **Step 3: Migrate menu collection**

Apply 24 dp cards, 16 dp image corners, SoftSand borders and explicit section headers to recent menu, favorites, audience carousels and audience detail. Preserve sorting, filters, favorite/photo/overflow callbacks and `Ver más` navigation.

- [ ] **Step 4: Migrate result and detail**

Use a 28 dp Surface container, present name/context/calories first and keep health analysis semantic. `Guardar en mis menús` uses terracotta; retry/discard/edit/photo/share/delete remain unchanged. Do not alter generated-analysis reuse or model calls.

- [ ] **Step 5: Migrate loading, empty, blocked and error surfaces**

Use theme shapes and semantic colors for AI loading, empty state, dice recovery, quota/error dialogs and disabled reason. Preserve messages, throttle/quota rules and callbacks.

- [ ] **Step 6: Verify and commit**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest --tests com.menudado.ui.MenuDadoViewModelTest
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: modernize menu results and states"
```

### Task 5: Modernize profile, My zone, Auth and About

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/java/com/menudado/auth/MenuDadoAuthScreen.kt`
- Test: `app/src/test/java/com/menudado/auth/MenuDadoAuthTest.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Run behavioral baseline**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.auth.MenuDadoAuthTest --tests com.menudado.ui.MenuDadoViewModelTest
```

Expected: PASS before presentation changes.

- [ ] **Step 2: Migrate profile**

Use a 24 dp Surface card, audience pills, 48 dp touch rows, SoftSand separators and 16 dp allergen chips. Preserve active-audience constraints, pregnancy visibility, vegan/allergy state, age ranges and text callback.

- [ ] **Step 3: Migrate My zone and About**

Keep a compact green account hero, followed by warm cards and consistent action rows. Preserve guest/signed-in branches, registration/sign-in entry points, values, About, debug privacy and sign-out.

- [ ] **Step 4: Migrate authentication**

Use `MaterialTheme.shapes.medium`, Surface + SoftSand for Google, and `ActionTerracotta` for submit. Keep email/password validation, loading, password matching, mode switch and callbacks.

- [ ] **Step 5: Verify and commit**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.auth.MenuDadoAuthTest --tests com.menudado.auth.MenuDadoAuthErrorsTest --tests com.menudado.ui.MenuDadoViewModelTest
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/main/java/com/menudado/auth/MenuDadoAuthScreen.kt
git commit -m "feat: modernize profile and account screens"
```

### Task 6: Accessibility, docs and resource audit

**Files:**
- Modify: touched UI files as required by audit.
- Modify: ES/EN/FR strings only if visible copy changes.
- Modify: `docs/project-context.md`

- [ ] **Step 1: Audit action sizes and semantics**

```bash
rg -n "\.size\([0-3][0-9]\.dp\)|contentDescription = null|IconButton\(" app/src/main/java/com/menudado/ui app/src/main/java/com/menudado/auth
```

Ensure actionable parents are at least 48 dp and have localized descriptions. Decorative images may use null descriptions.

- [ ] **Step 2: Audit rigid text layout**

```bash
rg -n "\.height\([0-9]+\.dp\)|maxLines = 1" app/src/main/java/com/menudado/ui app/src/main/java/com/menudado/auth
```

Remove rigid text-container heights that truncate large/translated text; retain media ratios and minimum control heights. Mirror any new copy in ES, EN and FR.

- [ ] **Step 3: Update project context**

Record Calm Editorial, semantic terracotta versus error tomato, centralized theme, 24 dp cards, 16 dp controls and warm navigation retaining Inicio, Perfil and Mi zona. Update header wording if its final compact implementation makes the old dimensions obsolete.

- [ ] **Step 4: Verify resources and commit**

```bash
./gradlew :app:processDebugResources :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
git add app/src/main/java/com/menudado/ui app/src/main/java/com/menudado/auth docs/project-context.md
git commit -m "docs: finalize calm editorial interface"
```

Add localized resource files only when changed.

### Task 7: Full automated and manual QA

**Files:**
- Verify all modified files.

- [ ] **Step 1: Run full unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Compile and assemble**

```bash
./gradlew :app:compileDebugKotlin :app:assembleDebug
```

Expected: BUILD SUCCESSFUL and fresh APK under `app/build/outputs/apk/debug/`.

- [ ] **Step 3: Run lint and compare baseline**

```bash
./gradlew :app:lintDebug
```

Expected: no new issue attributable to modified files. Record the exact known baseline separately if lint remains non-zero.

- [ ] **Step 4: Install and smoke test when a device is available**

```bash
./gradlew :app:installDebug
```

Verify onboarding, Home IA/manual/saved paths, result actions, menus, profile, My zone/Auth/About, loading/empty/error/quota, 360 dp width, keyboard, ES/EN/FR and enlarged font. Confirm no double AI request and no navigation regression.

- [ ] **Step 5: Review scope**

```bash
git status --short
git diff --check
git log --oneline -12
```

Expected: only prompt/UI/tests/docs work from the approved specs; no Firebase, Room or Analytics contract change and no unrelated user change reverted.
