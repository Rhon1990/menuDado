# Saved Menu Random Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separar la selección aleatoria local del dado IA, explicar claramente su comportamiento, permitir elegir otro menú desde el detalle y publicar la entrega como `1.2.0 (11)`.

**Architecture:** Mantener intacto `MenuDadoViewModel.rollDice()` y resolver la separación en Compose. `MenuDadoScreen` distinguirá el origen del detalle, `TodayMenuSection` tendrá feedback local y `MenuDetailDialog` expondrá una repetición opcional; Gradle, pruebas y contexto compartirán la misma versión.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, StateFlow, JUnit 4 y Gradle Android.

---

## File map

- `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: animación, origen del detalle, botón local y repetición.
- `app/src/main/res/values*/strings.xml`: textos en español, inglés y francés.
- `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: contratos visuales.
- `app/build.gradle.kts`: versión Android.
- `docs/project-context.md`: comportamiento y versión vigentes.

### Task 1: Escribir los contratos en rojo

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Añadir la prueba de separación del dado IA**

```kotlin
@Test
fun `seleccion local no anima el dado IA`() {
    assertFalse(contextualDiceShouldAnimate(isGeneratingMenu = false, isSelectingSavedMenu = true))
    assertTrue(contextualDiceShouldAnimate(isGeneratingMenu = true, isSelectingSavedMenu = false))
}
```

- [ ] **Step 2: Añadir la prueba de contenido y repetición**

```kotlin
@Test
fun `seleccion aleatoria explica origen progreso y repeticion`() {
    assertEquals(R.string.home_choose_saved_action, savedMenuRandomPrimaryTextRes(isRolling = false))
    assertEquals(R.string.home_choose_saved_loading, savedMenuRandomPrimaryTextRes(isRolling = true))
    assertEquals(R.string.home_choose_saved_supporting, savedMenuRandomSupportingTextRes())
    assertEquals(R.string.menu_detail_choose_another_saved, menuDetailChooseAnotherSavedTextRes())
    assertTrue(shouldShowChooseAnotherSavedMenu(openedFromRandomSelection = true))
    assertFalse(shouldShowChooseAnotherSavedMenu(openedFromRandomSelection = false))
}
```

- [ ] **Step 3: Ejecutar y confirmar el fallo esperado**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest'
```

Expected: `compileDebugUnitTestKotlin FAILED` por helpers aún inexistentes.

### Task 2: Separar la animación y dar feedback local

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Implementar los helpers probados**

```kotlin
internal fun contextualDiceShouldAnimate(
    isGeneratingMenu: Boolean,
    isSelectingSavedMenu: Boolean
): Boolean = isGeneratingMenu && !isSelectingSavedMenu

internal fun savedMenuRandomPrimaryTextRes(isRolling: Boolean): Int = if (isRolling) {
    R.string.home_choose_saved_loading
} else {
    R.string.home_choose_saved_action
}

internal fun savedMenuRandomSupportingTextRes(): Int = R.string.home_choose_saved_supporting
internal fun menuDetailChooseAnotherSavedTextRes(): Int = R.string.menu_detail_choose_another_saved
internal fun shouldShowChooseAnotherSavedMenu(openedFromRandomSelection: Boolean): Boolean =
    openedFromRandomSelection
```

- [ ] **Step 2: Hacer que pose y progreso observen solo IA**

```kotlin
val isContextualDiceAnimating = contextualDiceShouldAnimate(
    isGeneratingMenu = state.isGeneratingMenu,
    isSelectingSavedMenu = state.isRolling
)
var previousContextualDiceAnimating by remember { mutableStateOf(isContextualDiceAnimating) }

LaunchedEffect(isContextualDiceAnimating) {
    if (previousContextualDiceAnimating && !isContextualDiceAnimating) {
        diceFaceIndex = (diceFaceIndex + 1) % DiceRestPoses.size
    }
    previousContextualDiceAnimating = isContextualDiceAnimating
}
```

El segundo `LaunchedEffect` también observará `isContextualDiceAnimating` en lugar de `state.isRolling`.

- [ ] **Step 3: Añadir textos localizados**

```xml
<!-- values -->
<string name="home_choose_saved_action">Elegir un menú al azar</string>
<string name="home_choose_saved_supporting">Entre tus menús guardados</string>
<string name="home_choose_saved_loading">Buscando entre tus menús…</string>
<string name="menu_detail_choose_another_saved">Elegir otro menú</string>

<!-- values-en -->
<string name="home_choose_saved_action">Choose a random menu</string>
<string name="home_choose_saved_supporting">From your saved menus</string>
<string name="home_choose_saved_loading">Looking through your menus…</string>
<string name="menu_detail_choose_another_saved">Choose another menu</string>

<!-- values-fr -->
<string name="home_choose_saved_action">Choisir un menu au hasard</string>
<string name="home_choose_saved_supporting">Parmi vos menus enregistrés</string>
<string name="home_choose_saved_loading">Recherche dans vos menus…</string>
<string name="menu_detail_choose_another_saved">Choisir un autre menu</string>
```

- [ ] **Step 4: Sustituir el botón por feedback propio**

```kotlin
@Composable
private fun SavedMenuRandomButton(isRolling: Boolean, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MenuDadoUiTokens.ControlRadius),
        border = BorderStroke(1.dp, MenuDadoColors.BrandGreen)
    ) {
        if (isRolling) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MenuDadoColors.BrandGreen,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = savedMenuRandomPrimaryTextRes(isRolling)),
                color = MenuDadoColors.DeepGreen,
                fontWeight = FontWeight.Bold
            )
            if (!isRolling) {
                Text(
                    text = stringResource(id = savedMenuRandomSupportingTextRes()),
                    color = MenuDadoColors.MutedInk,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

- [ ] **Step 5: Ejecutar verde y registrar**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest'
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "fix: separate saved menu selection from AI dice"
```

Expected: `BUILD SUCCESSFUL` antes del commit.

### Task 3: Elegir otro desde el detalle

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Conservar el origen del detalle**

```kotlin
var openedFromRandomSelection by rememberSaveable { mutableStateOf(false) }

LaunchedEffect(result?.id) {
    if (menuShouldOpenDetailFromDiceResult(result)) {
        openedFromRandomSelection = true
        selectedDetailMenuId = menuDetailMenuIdAfterDiceResult(selectedDetailMenuId, result)
        viewModel.clearResult()
    }
}
```

Las aperturas desde tarjeta, `Tu último menú` o lista establecerán el origen en `false`; cierre, borrado y navegación atrás también lo limpiarán.

- [ ] **Step 2: Añadir callback opcional al detalle**

```kotlin
private fun MenuDetailDialog(
    menu: FoodMenu,
    isAnalyzing: Boolean,
    aiUsesRemainingToday: Int,
    isAiPaused: Boolean,
    onAnalyze: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenActions: () -> Unit,
    onChooseAnotherSavedMenu: (() -> Unit)?,
    onDismiss: () -> Unit
)
```

Antes de `Cerrar`, un `OutlinedButton` no nulo usará `menuDetailChooseAnotherSavedTextRes()`.

- [ ] **Step 3: Repetir con los mismos filtros**

```kotlin
onChooseAnotherSavedMenu = if (shouldShowChooseAnotherSavedMenu(openedFromRandomSelection)) {
    {
        viewModel.trackCtaTapped(ANALYTICS_SCREEN_MENU_DETAIL, ANALYTICS_CTA_CHOOSE_ANOTHER_SAVED_MENU)
        selectedDetailMenuId = null
        openedFromRandomSelection = false
        viewModel.rollDice()
    }
} else null
```

Añadir el CTA cerrado `choose_another_saved_menu`, sin contenido del menú.

- [ ] **Step 4: Ejecutar contratos de UI y selección**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest' --tests 'com.menudado.ui.MenuDadoViewModelTest'
```

Expected: `BUILD SUCCESSFUL`, incluidos filtros, duración, no repetición y recuperación.

- [ ] **Step 5: Registrar el resultado**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: let users choose another saved menu"
```

### Task 4: Actualizar la entrega a 1.2.0

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `docs/project-context.md`

- [ ] **Step 1: Cambiar el contrato de versión**

```kotlin
assertEquals("1.2.0", BuildConfig.VERSION_NAME)
assertEquals(11, BuildConfig.VERSION_CODE)
assertEquals("1.2.0 (11)", aboutVersionLabel(versionName = "1.2.0", versionCode = 11))
```

Ejecutar la clase antes de modificar Gradle y confirmar que falla porque el build todavía expone `1.1.0 (10)`.

- [ ] **Step 2: Actualizar Gradle y contexto**

```kotlin
versionCode = 11
versionName = "1.2.0"
```

Documentar en `docs/project-context.md` el botón aleatorio, su progreso, la repetición desde detalle, la separación de IA y la versión `1.2.0 (11)`.

- [ ] **Step 3: Verificar metadata y registrar**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
rg -n 'versionCode = 11|versionName = "1.2.0"|"versionCode": 11|"versionName": "1.2.0"' app/build.gradle.kts app/build/outputs/apk/debug/output-metadata.json
git add app/build.gradle.kts app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt docs/project-context.md
git commit -m "chore: prepare version 1.2.0"
```

Expected: tests y APK correctos; Gradle y metadata muestran `1.2.0 (11)`.

### Task 5: QA completa y entrega

**Files:**
- Verify: all changed files

- [ ] **Step 1: Ejecutar verificación automatizada fresca**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
git diff --check
```

Expected: `BUILD SUCCESSFUL` y ningún error de whitespace.

- [ ] **Step 2: Instalar conservando datos**

```bash
/Users/rdelgpad/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected: `Success`.

- [ ] **Step 3: Validar manualmente**

1. El botón explica azar y origen guardado.
2. Solo el botón local muestra progreso.
3. El dado IA no gira ni cambia de pose.
4. El resultado aleatorio ofrece `Elegir otro menú`.
5. La repetición conserva filtros y evita candidatos ya usados cuando quedan nuevos.
6. Los detalles abiertos desde tarjetas no muestran la repetición.
7. Logcat no muestra errores `AndroidRuntime`.

- [ ] **Step 4: Confirmar árbol limpio**

```bash
git status --short --branch
git log -5 --oneline
```

Expected: árbol limpio y commits de implementación presentes.
