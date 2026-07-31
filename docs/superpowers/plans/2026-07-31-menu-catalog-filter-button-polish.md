# Catalog Filter Button Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrar el botón de filtros en la cabecera verde y centrar visualmente el contador activo sin modificar el comportamiento del catálogo.

**Architecture:** Mantener `MenuCatalogSearchBar` como único componente de UI afectado y extraer sus decisiones visuales a un contrato `internal` pequeño que la UI consuma directamente. Una prueba JVM fijará los tokens aprobados y las pruebas Compose existentes seguirán cubriendo visibilidad, accesibilidad e interacción.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4, AndroidX Compose UI Test, Gradle.

---

## File map

- Modify: `app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt` — contrato de estilo y representación del botón/badge.
- Create: `app/src/test/java/com/menudado/ui/MenuCatalogSearchBarStyleTest.kt` — regresión JVM de color, geometría y métricas tipográficas.
- Existing verification: `app/src/androidTest/java/com/menudado/ui/MenuCatalogSearchBarTest.kt` — comportamiento y accesibilidad ya cubiertos; no requiere cambios.

### Task 1: Fijar el contrato visual con una prueba roja

**Files:**
- Create: `app/src/test/java/com/menudado/ui/MenuCatalogSearchBarStyleTest.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuCatalogSearchBarStyleTest.kt`

- [ ] **Step 1: Write the failing visual contract test**

```kotlin
package com.menudado.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menudado.ui.theme.MenuDadoColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MenuCatalogSearchBarStyleTest {
    @Test
    fun filterButtonUsesHeaderIntegratedStyleAndCenteredBadgeMetrics() {
        val style = menuCatalogFilterButtonVisualStyle()

        assertEquals(MenuDadoColors.Cream.copy(alpha = 0.12f), style.containerColor)
        assertEquals(MenuDadoColors.Cream.copy(alpha = 0.82f), style.borderColor)
        assertEquals(MenuDadoColors.Cream, style.iconColor)
        assertEquals(1.dp, style.borderWidth)
        assertEquals(20.dp, style.badgeSize)
        assertEquals(4.dp, style.badgeHorizontalOffset)
        assertEquals((-4).dp, style.badgeVerticalOffset)
        assertEquals(11.sp, style.badgeFontSize)
        assertEquals(11.sp, style.badgeLineHeight)
        assertFalse(style.includeFontPadding)
    }
}
```

- [ ] **Step 2: Run the focused test and confirm RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCatalogSearchBarStyleTest
```

Expected: compilation fails because `menuCatalogFilterButtonVisualStyle` does not exist yet.

### Task 2: Implementar la opción B y poner la prueba en verde

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuCatalogSearchBarStyleTest.kt`

- [ ] **Step 1: Add the reusable visual style contract**

Add the needed `Color`, `Dp`, `TextUnit` and `dp`/`sp` imports, then define:

```kotlin
internal data class MenuCatalogFilterButtonVisualStyle(
    val containerColor: Color,
    val borderColor: Color,
    val iconColor: Color,
    val borderWidth: Dp,
    val badgeSize: Dp,
    val badgeHorizontalOffset: Dp,
    val badgeVerticalOffset: Dp,
    val badgeFontSize: TextUnit,
    val badgeLineHeight: TextUnit,
    val includeFontPadding: Boolean
)

internal fun menuCatalogFilterButtonVisualStyle() = MenuCatalogFilterButtonVisualStyle(
    containerColor = MenuDadoColors.Cream.copy(alpha = 0.12f),
    borderColor = MenuDadoColors.Cream.copy(alpha = 0.82f),
    iconColor = MenuDadoColors.Cream,
    borderWidth = 1.dp,
    badgeSize = 20.dp,
    badgeHorizontalOffset = 4.dp,
    badgeVerticalOffset = (-4).dp,
    badgeFontSize = 11.sp,
    badgeLineHeight = 11.sp,
    includeFontPadding = false
)
```

- [ ] **Step 2: Apply the approved container, icon and badge styling**

Inside `MenuCatalogSearchBar`, obtain `val filterStyle = menuCatalogFilterButtonVisualStyle()` and update the filter control:

```kotlin
.border(
    width = filterStyle.borderWidth,
    color = filterStyle.borderColor,
    shape = RoundedCornerShape(MenuDadoUiTokens.ControlRadius)
)
.background(filterStyle.containerColor)
```

Use `filterStyle.iconColor` for the filter icon. Update the badge modifier and text:

```kotlin
.align(Alignment.TopEnd)
.offset(
    x = filterStyle.badgeHorizontalOffset,
    y = filterStyle.badgeVerticalOffset
)
.size(filterStyle.badgeSize)
```

```kotlin
style = TextStyle(
    color = MenuDadoColors.Cream,
    fontSize = filterStyle.badgeFontSize,
    lineHeight = filterStyle.badgeLineHeight,
    fontWeight = FontWeight.Black,
    textAlign = TextAlign.Center,
    platformStyle = PlatformTextStyle(
        includeFontPadding = filterStyle.includeFontPadding
    )
)
```

- [ ] **Step 3: Run the focused test and confirm GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCatalogSearchBarStyleTest
```

Expected: `BUILD SUCCESSFUL` and one passing test class.

- [ ] **Step 4: Commit the functional change**

```bash
git add app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt app/src/test/java/com/menudado/ui/MenuCatalogSearchBarStyleTest.kt
git commit -m "fix: integrate catalog filter button with header"
```

### Task 3: Verificar comportamiento y presentación integrada

**Files:**
- Verify: `app/src/androidTest/java/com/menudado/ui/MenuCatalogSearchBarTest.kt`
- Verify: `app/src/main/java/com/menudado/ui/MenuCatalogSearchBar.kt`

- [ ] **Step 1: Run unit tests and build the debug APK**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`; no unit-test failures and a debug APK produced.

- [ ] **Step 2: Run the existing Compose component tests on a connected device**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.ui.MenuCatalogSearchBarTest
```

Expected: three passing tests covering clear/search interaction, filter action, hidden zero badge and visible active count.

- [ ] **Step 3: Perform visual QA**

Install/open the debug build and inspect one catalog screen with zero filters and one with at least one active filter. Confirm:

- the button reads as part of the green header;
- cream border/icon have clear contrast;
- the terracotta badge does not cover the filter glyph;
- the numeral is perceptually centered;
- the touch area and search spacing remain unchanged.

- [ ] **Step 4: Run repository hygiene checks**

Run:

```bash
git diff --check
git status --short
```

Expected: no whitespace errors and only intentional committed changes.
