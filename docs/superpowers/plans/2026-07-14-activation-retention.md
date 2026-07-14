# MenuDado Activation and Retention Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convertir el primer uso y el Home de MenuDado en un embudo claro de generar, guardar y volver, con recuperacion util para tiradas sin coincidencias y analitica medible.

**Architecture:** Mantener Compose + MVVM y reutilizar los contratos actuales. `MenuDadoUiState` tendra un estado efimero de recuperacion; `MenuDadoViewModel` orquestara acciones existentes de IA, dado y guardado; la UI solo representara estados y emitira intenciones. Room, Firestore y Firebase AI no cambian.

**Tech Stack:** Kotlin, Jetpack Compose, StateFlow, coroutines, Room, Firebase Analytics, JUnit4, kotlinx-coroutines-test, Gradle.

---

## Mapa de archivos

- `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`: estado y acciones de recuperacion, regeneracion y seleccion ampliada.
- `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: onboarding, jerarquia de Home, menu reciente, dialogos y CTAs.
- `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`: contrato del evento de recuperacion.
- `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`: serializacion segura del evento Firebase.
- `app/src/main/res/values/strings.xml`: textos en espanol.
- `app/src/main/res/values-en/strings.xml`: textos en ingles.
- `app/src/main/res/values-fr/strings.xml`: textos en frances.
- `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`: contratos de estado, acciones y analitica.
- `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: helpers puros de UI, onboarding y menu reciente.
- `docs/project-context.md`: comportamiento funcional final y eventos nuevos.

No se crean repositorios, DAOs, entidades, rutas de navegacion ni esquemas nuevos.

### Task 1: Contrato analitico de recuperacion

**Files:**
- Modify: `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`
- Modify: `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Extender el fake de pruebas y escribir una prueba fallida**

Agregar al `RecordingMenuDadoAnalytics`:

```kotlin
override fun trackDiceEmptyRecovery(action: String) {
    events += "dice_empty_recovery:$action"
}
```

Agregar una prueba que invoque temporalmente el contrato desde el ViewModel cuando se implemente la accion `dismissDiceEmptyRecovery()`:

```kotlin
@Test
fun `dice empty recovery actions use closed analytics values`() = runTest(dispatcher) {
    analytics.trackDiceEmptyRecovery("change_filters")

    assertEquals(listOf("dice_empty_recovery:change_filters"), analytics.events)
}
```

- [ ] **Step 2: Ejecutar la prueba y verificar el fallo**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuDadoViewModelTest.dice empty recovery actions use closed analytics values'
```

Expected: FAIL de compilacion porque `trackDiceEmptyRecovery` no existe en el contrato/ViewModel.

- [ ] **Step 3: Implementar el contrato minimo**

En `MenuDadoAnalytics`:

```kotlin
fun trackDiceEmptyRecovery(action: String)
```

En `NoOpMenuDadoAnalytics`:

```kotlin
override fun trackDiceEmptyRecovery(action: String) = Unit
```

En `FirebaseMenuDadoAnalytics`:

```kotlin
override fun trackDiceEmptyRecovery(action: String) {
    logEvent(EVENT_DICE_EMPTY_RECOVERY) {
        putString(PARAM_ACTION, action.sanitized())
    }
}
```

Agregar `EVENT_DICE_EMPTY_RECOVERY = "dice_empty_recovery"`. El ViewModel invocara el contrato solo con constantes cerradas; la UI no entregara texto libre.

- [ ] **Step 4: Ejecutar la prueba enfocada**

Run: el mismo comando del Step 2.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "feat: track dice empty recovery actions"
```

### Task 2: Estado y acciones de recuperacion del dado

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Escribir pruebas fallidas para ausencia real de candidatos**

Agregar pruebas que cubran:

```kotlin
@Test
fun `empty dice result exposes recovery with alternative meal for same audience`() = runTest(dispatcher) {
    dao.seed(listOf(FoodMenu(id = 1, name = "Cena", mealType = MealType.DINNER, audience = MenuAudience.ADULT, description = "Cena")))
    advanceUntilIdle()
    viewModel.setDiceFilter(MealType.BREAKFAST)
    viewModel.setDiceAudienceFilter(MenuAudience.ADULT)
    analytics.events.clear()

    viewModel.rollDice()
    advanceUntilIdle()

    assertEquals(MealType.BREAKFAST, viewModel.uiState.value.diceEmptyRecovery?.mealType)
    assertEquals(MenuAudience.ADULT, viewModel.uiState.value.diceEmptyRecovery?.audience)
    assertTrue(viewModel.uiState.value.diceEmptyRecovery?.canBroadenMealType == true)
    assertTrue(analytics.events.contains("dice_empty_recovery:shown"))
}
```

```kotlin
@Test
fun `broadened dice roll preserves audience and selects another meal type`() = runTest(dispatcher) {
    dao.seed(listOf(
        FoodMenu(id = 1, name = "Cena adulta", mealType = MealType.DINNER, audience = MenuAudience.ADULT, description = "Cena"),
        FoodMenu(id = 2, name = "Cena bebe", mealType = MealType.DINNER, audience = MenuAudience.BABY, description = "Cena")
    ))
    advanceUntilIdle()
    viewModel.setDiceFilter(MealType.BREAKFAST)
    viewModel.setDiceAudienceFilter(MenuAudience.ADULT)
    viewModel.rollDice()
    advanceUntilIdle()

    viewModel.rollDiceAcrossMealTypesForSelectedAudience()
    advanceUntilIdle()

    assertEquals("Cena adulta", viewModel.uiState.value.result?.name)
    assertEquals(MenuAudience.ADULT, viewModel.uiState.value.diceAudienceFilter)
    assertEquals(MealType.BREAKFAST, viewModel.uiState.value.diceFilter)
}
```

- [ ] **Step 2: Ejecutar ambas pruebas y verificar el fallo**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuDadoViewModelTest.*dice*recovery*' --tests 'com.menudado.ui.MenuDadoViewModelTest.broadened dice roll preserves audience and selects another meal type'
```

Expected: FAIL porque no existen `DiceEmptyRecovery`, la propiedad de estado ni la accion ampliada.

- [ ] **Step 3: Implementar el estado minimo**

En `MenuDadoViewModel.kt`:

```kotlin
data class DiceEmptyRecovery(
    val mealType: MealType,
    val audience: MenuAudience,
    val canBroadenMealType: Boolean
)
```

Agregar a `MenuDadoUiState`:

```kotlin
val diceEmptyRecovery: DiceEmptyRecovery? = null
```

Extraer el cuerpo asincrono de `rollDice()` a una funcion privada que reciba `filter: MealType?` y `audience: MenuAudience`, para que la seleccion ampliada pueda usar `filter = null` sin mutar filtros visibles.

Al no existir candidatos:

```kotlin
val recovery = DiceEmptyRecovery(
    mealType = selectedMealType,
    audience = selectedAudience,
    canBroadenMealType = currentState.menus.any {
        it.audience == selectedAudience && it.mealType != selectedMealType
    }
)
analytics.trackDiceEmptyRecovery(ACTION_SHOWN)
```

El resultado correcto debe escribir `diceEmptyRecovery = null`. El agotamiento diario con candidatos conserva el reseteo actual.

- [ ] **Step 4: Implementar acciones de recuperacion**

```kotlin
fun dismissDiceEmptyRecovery() {
    if (_uiState.value.diceEmptyRecovery != null) {
        analytics.trackDiceEmptyRecovery(ACTION_CHANGE_FILTERS)
    }
    _uiState.update { it.copy(diceEmptyRecovery = null) }
}

fun rollDiceAcrossMealTypesForSelectedAudience() {
    val recovery = _uiState.value.diceEmptyRecovery ?: return
    analytics.trackDiceEmptyRecovery(ACTION_BROADEN_MEAL_TYPE)
    _uiState.update { it.copy(diceEmptyRecovery = null) }
    startDiceRoll(filter = null, audience = recovery.audience)
}

fun generateAiFromDiceEmptyRecovery() {
    val recovery = _uiState.value.diceEmptyRecovery ?: return
    analytics.trackDiceEmptyRecovery(ACTION_GENERATE_AI)
    _uiState.update {
        it.copy(
            diceEmptyRecovery = null,
            homeMenuMode = HomeMenuMode.Ai,
            formMealType = recovery.mealType,
            formAudience = recovery.audience
        )
    }
    generateMenuIdea()
}
```

Usar constantes `shown`, `generate_ai`, `broaden_meal_type` y `change_filters`.

- [ ] **Step 5: Ejecutar todas las pruebas de ViewModel**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuDadoViewModelTest'
```

Expected: PASS sin regresiones del dado, cuotas ni analitica.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "feat: recover from empty dice results"
```

### Task 3: Onboarding de activacion en una pagina

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Cambiar la prueba a la propuesta unica**

Reemplazar la prueba de cinco pasos por:

```kotlin
@Test
fun `onboarding focuses on creating the first menu`() {
    val steps = onboardingSteps()

    assertEquals(1, steps.size)
    assertEquals(R.string.onboarding_activation_title, steps.single().titleRes)
    assertEquals(R.string.onboarding_activation_body, steps.single().bodyRes)
}
```

Eliminar la prueba de swipe, porque el componente ya no sera paginado.

- [ ] **Step 2: Ejecutar y verificar el fallo**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest.onboarding focuses on creating the first menu'
```

Expected: FAIL porque el onboarding aun tiene cinco pasos y faltan recursos.

- [ ] **Step 3: Implementar recursos y UI**

Agregar recursos equivalentes en los tres idiomas:

```xml
<string name="onboarding_activation_title">Resuelve qué comer en menos de un minuto</string>
<string name="onboarding_activation_body">Genera una idea con IA o deja que el dado elija entre tus menús guardados.</string>
<string name="onboarding_create_first_menu">Crear mi primer menú</string>
<string name="onboarding_explore">Explorar por mi cuenta</string>
<string name="onboarding_no_registration">Sin registro obligatorio</string>
```

Reducir `onboardingSteps()` a un elemento. Simplificar `OnboardingDialog` eliminando indice, indicadores, swipe y `onNext`; mantener `onFinish` y `onSkip`.

Mapear CTAs:

```kotlin
ANALYTICS_CTA_START_ONBOARDING = "create_first_menu"
ANALYTICS_CTA_SKIP_ONBOARDING = "explore_without_onboarding"
```

No cambiar `CURRENT_ONBOARDING_VERSION = 5`.

- [ ] **Step 4: Ejecutar pruebas de UI state y ViewModel onboarding**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest' --tests 'com.menudado.ui.MenuDadoViewModelTest.*onboarding*'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: focus onboarding on first menu activation"
```

### Task 4: Home IA-first y continuidad con menu reciente

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Escribir pruebas de helpers puros**

```kotlin
@Test
fun `recent menu uses greatest creation timestamp`() {
    val old = FoodMenu(id = 1, name = "Anterior", mealType = MealType.LUNCH, description = "A", createdAt = 10)
    val recent = FoodMenu(id = 2, name = "Reciente", mealType = MealType.DINNER, description = "B", createdAt = 30)

    assertEquals(2L, mostRecentMenu(listOf(recent.copy(createdAt = 20), old, recent))?.id)
    assertNull(mostRecentMenu(emptyList()))
}
```

Agregar pruebas para los labels/orden de acciones si se extraen helpers de recursos.

- [ ] **Step 2: Ejecutar y verificar el fallo**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest.recent menu uses greatest creation timestamp'
```

Expected: FAIL porque `mostRecentMenu` no existe.

- [ ] **Step 3: Implementar seleccion reciente y tarjeta**

```kotlin
internal fun mostRecentMenu(menus: List<FoodMenu>): FoodMenu? = menus.maxByOrNull(FoodMenu::createdAt)
```

Crear `RecentMenuSection(menu, onOpen)` reutilizando colores, `MenuCoverImage`, chips y formas ya presentes. Mostrarla inmediatamente despues de `TodayMenuSection` solo si existe menu.

En el callback:

```kotlin
viewModel.trackCtaTapped(ANALYTICS_SCREEN_HOME, "open_recent_menu")
viewModel.trackMenuCardOpened(menu)
selectedDetailMenuId = menu.id
```

- [ ] **Step 4: Reorganizar `TodayMenuSection` sin duplicar logica**

Mantener selectores e ingredientes. En modo IA mostrar:

1. CTA principal `Generar mi menu` -> `onGenerate`.
2. CTA secundaria `Elegir entre mis menus` -> `onRollSavedMenu`.
3. Text button `Escribir mi menu` -> `onModeChanged(HomeMenuMode.Manual)`.

En modo manual mostrar campos, `Guardar menu` y accion `Volver a generar con IA`. Eliminar el selector segmentado si deja de ser necesario visualmente, pero conservar `HomeMenuMode` como estado.

Usar CTAs cerrados:

```kotlin
generate_menu
choose_saved_menu
write_menu
open_recent_menu
```

- [ ] **Step 5: Integrar `DiceEmptyRecoveryDialog`**

El dialogo recibe el estado y callbacks:

```kotlin
DiceEmptyRecoveryDialog(
    recovery = recovery,
    onGenerateAi = viewModel::generateAiFromDiceEmptyRecovery,
    onBroadenMealType = viewModel::rollDiceAcrossMealTypesForSelectedAudience,
    onChangeFilters = viewModel::dismissDiceEmptyRecovery
)
```

Mostrar `Probar otro tipo de comida` solo con `canBroadenMealType`. Incluir `diceEmptyRecovery` en `BackHandler` y cerrarlo mediante `dismissDiceEmptyRecovery()`.

- [ ] **Step 6: Ejecutar pruebas y compilar Compose**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest' --tests 'com.menudado.ui.MenuDadoViewModelTest' :app:compileDebugKotlin
```

Expected: PASS y `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: make home activation focused"
```

### Task 5: Guardado y regeneracion desde el resultado IA

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Escribir prueba de regeneracion explicita**

```kotlin
@Test
fun `try another generated idea discards draft and starts one protected request`() = runTest(dispatcher) {
    analyzer.generatedMenu = GeneratedMenu("Idea 1", "Ingredientes", "", 400)
    viewModel.setFormMealType(MealType.LUNCH)
    viewModel.setFormAudience(MenuAudience.ADULT)
    viewModel.generateMenuIdea()
    advanceUntilIdle()
    analytics.events.clear()

    analyzer.generatedMenu = GeneratedMenu("Idea 2", "Otros ingredientes", "", 450)
    viewModel.tryAnotherGeneratedMenuIdea()
    advanceUntilIdle()

    assertEquals("Idea 2", viewModel.uiState.value.name)
    assertTrue(viewModel.uiState.value.showGeneratedMenuDetail)
    assertEquals(1, analytics.events.count { it.startsWith("ai_menu_generation_started") })
}
```

- [ ] **Step 2: Ejecutar y verificar el fallo**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuDadoViewModelTest.try another generated idea discards draft and starts one protected request'
```

Expected: FAIL porque la accion no existe.

- [ ] **Step 3: Implementar accion reutilizando protecciones**

```kotlin
fun tryAnotherGeneratedMenuIdea() {
    if (_uiState.value.isGeneratingMenu) return
    _uiState.update {
        it.copy(
            name = "",
            description = "",
            notes = "",
            calories = null,
            generatedHealthAnalysis = null,
            showGeneratedMenuDetail = false
        )
    }
    generateMenuIdea()
}
```

No limpiar `aiBaseIngredients`, `formMealType` ni `formAudience`: son la intencion que debe reutilizar la siguiente idea. No llamar directamente al repositorio. `generateMenuIdea()` debe seguir siendo la unica entrada a cuota, throttle, conflictos y timeout.

- [ ] **Step 4: Actualizar dialogo y tracking**

`GeneratedMenuDetailDialog` recibe `onTryAnother` y `isGenerating`. El CTA principal de ancho completo usa `Guardar en mis menus`; el secundario usa el conteo de IA disponible cuando aplique y llama a `tryAnotherGeneratedMenuIdea()`.

Trackear `save_generated_menu` y `try_another_generated_menu`. Cerrar/back conserva `discardGeneratedMenuIdea()`.

- [ ] **Step 5: Ejecutar pruebas de generacion y compilacion**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuDadoViewModelTest.*generated*' :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "feat: strengthen generated menu conversion"
```

### Task 6: Contexto, verificacion integral y configuracion analitica

**Files:**
- Modify: `docs/project-context.md`
- Verify: all files above

- [ ] **Step 1: Actualizar el contexto funcional**

Documentar:

- Onboarding de activacion de una pagina sin cambio de version.
- Home IA-first, acciones secundarias y menu reciente.
- Recuperacion del dado que nunca mezcla publicos.
- `dice_empty_recovery` y CTAs cerrados nuevos.
- Ausencia de cambios en Room, Firestore y Firebase AI.

- [ ] **Step 2: Revisar recursos entre idiomas**

Run:

```bash
python3 - <<'PY'
import xml.etree.ElementTree as ET
from pathlib import Path
files = [Path('app/src/main/res/values/strings.xml'), Path('app/src/main/res/values-en/strings.xml'), Path('app/src/main/res/values-fr/strings.xml')]
keys = [{node.attrib['name'] for node in ET.parse(path).getroot() if node.tag == 'string'} for path in files]
required = {name for name in keys[0] if name.startswith(('onboarding_activation', 'home_', 'dice_empty_recovery', 'generated_menu_'))}
for path, current in zip(files[1:], keys[1:]):
    missing = sorted(required - current)
    assert not missing, f'{path}: missing {missing}'
print('localized retention strings: OK')
PY
```

Expected: `localized retention strings: OK`.

- [ ] **Step 3: Ejecutar suite y verificaciones Android**

Run, en este orden:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Expected: cada comando termina con `BUILD SUCCESSFUL`.

- [ ] **Step 4: Inspeccionar calidad del diff**

Run:

```bash
git diff --check
git status --short
git diff --stat
```

Expected: sin whitespace errors; solo archivos del alcance. `.superpowers/` es un artefacto local de mockups y no se incluye en commits.

- [ ] **Step 5: Configuracion prospectiva en GA4**

En Firebase/GA4 registrar dimensiones personalizadas de evento:

- `cta` -> parametro `cta`
- `screen` -> parametro `screen`
- `action` -> parametro `action`

No afirmar que existe historico: las dimensiones empiezan a poblarse despues del alta. Si el acceso de consola no permite escribir, documentar el paso como riesgo de despliegue sin bloquear el build Android.

- [ ] **Step 6: Commit final de documentacion**

```bash
git add docs/project-context.md
git commit -m "docs: describe activation retention flow"
```

- [ ] **Step 7: Revision final del historial**

Run:

```bash
git log -7 --oneline --decorate
git status --short --branch
```

Expected: commits de diseño, plan, analitica, recuperacion, onboarding, Home, conversion IA y contexto; ningun cambio de producto sin registrar.
