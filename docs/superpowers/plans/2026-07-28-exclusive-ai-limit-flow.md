# Exclusive AI Limit Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Garantizar que el flujo bonificado muestre una sola superficie cada vez y que un fallback exitoso llegue directamente al menú sin un modal transitorio de pausa.

**Architecture:** El `ViewModel` preparará y persistirá la pausa de cuota antes del fallback, pero diferirá su mensaje visible hasta conocer que no existe alternativa. Compose derivará una superficie exclusiva con prioridad carga, menú y mensaje; el detalle impedirá perder el menú actual intentando otra idea durante una pausa o un límite absoluto.

**Tech Stack:** Kotlin, Jetpack Compose, StateFlow, coroutines test, JUnit 4, Gradle.

---

## File map

- `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`: separar la preparación interna del fallo de su presentación y diferir el aviso.
- `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: seleccionar una única superficie IA y proteger `Probar otra idea`.
- `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`: regresiones de cuota con hit y miss de colmena.
- `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: prioridad de superficies y estado del CTA del detalle.
- `docs/project-context.md`: reflejar el contrato definitivo de límites y exclusividad modal.

### Task 1: Diferir el aviso de cuota hasta terminar el fallback

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:1318-1398`
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Escribir las pruebas fallidas de hit y miss**

Añadir dos pruebas:

```kotlin
@Test
fun `quota fallback success keeps retry internal and reveals only generated menu`() =
    runTest(dispatcher) {
        analyzer.generateFailure =
            IllegalStateException("Quota exceeded. Please retry in 57s.")
        hive.searchResult = Result.success(sampleHiveCandidate())
        hive.searchDelayMillis = 1_000L

        viewModel.generateMenuIdea()
        runCurrent()

        assertEquals(AiGenerationPhase.SEARCHING_HIVE, viewModel.uiState.value.aiGenerationPhase)
        assertNull(viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isAiRetryNoticeVisible)

        advanceTimeBy(1_000L)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(GeneratedMenuOrigin.HIVE_FALLBACK, state.generatedOrigin)
        assertTrue(state.showGeneratedMenuDetail)
        assertNull(state.message)
        assertFalse(state.isAiRetryNoticeVisible)
        assertTrue(state.aiRetryAtMillis != null)
    }

@Test
fun `quota fallback miss reveals one retry notice after loading`() = runTest(dispatcher) {
    analyzer.generateFailure =
        IllegalStateException("Quota exceeded. Please retry in 57s.")
    hive.searchResult = Result.success(null)

    viewModel.generateMenuIdea()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isGeneratingMenu)
    assertFalse(state.showGeneratedMenuDetail)
    assertEquals(
        "La IA está con mucha demanda. Inténtalo nuevamente más tarde.",
        state.message
    )
    assertTrue(state.isAiRetryNoticeVisible)
    assertTrue(state.aiRetryAtMillis != null)
}
```

Ampliar el fake existente para poder observar el estado intermedio:

```kotlin
private class RecordingAiMenuHiveGateway : AiMenuHiveGateway {
    var searchResult: Result<AiMenuHiveCandidate?> = Result.success(null)
    var searchDelayMillis: Long = 0L
    val searches = mutableListOf<AiMenuHiveSearchRequest>()
    val contributions = mutableListOf<AiMenuHiveContribution>()

    override suspend fun findCompatibleMenu(
        request: AiMenuHiveSearchRequest
    ): Result<AiMenuHiveCandidate?> {
        searches += request
        delay(searchDelayMillis)
        return searchResult
    }

    override suspend fun contribute(contribution: AiMenuHiveContribution): Result<Unit> {
        contributions += contribution
        return Result.success(Unit)
    }
}
```

- [ ] **Step 2: Ejecutar las pruebas y verificar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuDadoViewModelTest.quota fallback success keeps retry internal and reveals only generated menu' \
  --tests 'com.menudado.ui.MenuDadoViewModelTest.quota fallback miss reveals one retry notice after loading'
```

Expected: la primera prueba falla porque `showAiFailureNotice()` publica el mensaje antes de completar la búsqueda.

- [ ] **Step 3: Preparar el fallo una sola vez y diferir su visibilidad**

En `searchHiveFallback()`, sustituir:

```kotlin
failureNotice?.let(::showAiFailureNotice)
```

por:

```kotlin
val preparedFailureNotice = failureNotice?.prepareAiFailureNotice()
preparedFailureNotice?.recordAiFailurePause()
```

Sustituir:

```kotlin
if (fallback == null && failureNotice == null) {
    _uiState.update {
        it.copy(
            message = currentLanguage().aiLocalDailyLimitMessage(),
            isAiRetryNoticeVisible = false
        )
    }
}
```

por:

```kotlin
if (fallback == null) {
    if (preparedFailureNotice != null) {
        showPreparedAiFailureNotice(preparedFailureNotice)
    } else {
        _uiState.update {
            it.copy(
                message = currentLanguage().aiLocalDailyLimitMessage(),
                isAiRetryNoticeVisible = false
            )
        }
    }
}
```

Reemplazar `showAiFailureNotice()` por:

```kotlin

private fun AiFailureNotice.prepareAiFailureNotice(): AiFailureNotice {
    return copy(retryAtMillis = retryAtMillis?.withQuotaBackoff())
}

private fun AiFailureNotice.recordAiFailurePause() {
    val retryAtMillis = retryAtMillis ?: return
    _uiState.update {
        it.copy(
            message = null,
            aiRetryAtMillis = retryAtMillis,
            isAiRequestThrottlePause = false,
            isAiRetryNoticeVisible = false
        )
    }
    scheduleAiRetryRefresh(retryAtMillis)
}

private fun showPreparedAiFailureNotice(notice: AiFailureNotice) {
    val retryAtMillis = notice.retryAtMillis
    val refreshAtMillis = retryAtMillis ?: _uiState.value.aiRetryAtMillis
        ?.takeIf { _uiState.value.isAiRequestThrottlePause }
    _uiState.update { current ->
        current.copy(
            message = notice.message,
            aiRetryAtMillis = retryAtMillis ?: current.aiRetryAtMillis,
            isAiRequestThrottlePause = if (retryAtMillis != null) false else current.isAiRequestThrottlePause,
            isAiRetryNoticeVisible = retryAtMillis != null
        )
    }
    scheduleAiRetryRefresh(refreshAtMillis)
}
```

- [ ] **Step 4: Ejecutar pruebas focalizadas y verificar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuDadoViewModelTest'
```

Expected: `BUILD SUCCESSFUL` y todas las pruebas de `MenuDadoViewModelTest` pasan.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "fix: defer AI quota notice until fallback completes"
```

### Task 2: Hacer mutuamente excluyentes carga, menú y aviso

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Escribir las pruebas fallidas de prioridad**

Añadir:

```kotlin
@Test
fun `flujo IA prioriza carga luego menu y finalmente aviso`() {
    assertEquals(
        AiFlowSurface.LOADING,
        aiFlowSurface(isGenerating = true, hasGeneratedMenu = true, hasMessage = true)
    )
    assertEquals(
        AiFlowSurface.GENERATED_MENU,
        aiFlowSurface(isGenerating = false, hasGeneratedMenu = true, hasMessage = true)
    )
    assertEquals(
        AiFlowSurface.MESSAGE,
        aiFlowSurface(isGenerating = false, hasGeneratedMenu = false, hasMessage = true)
    )
    assertEquals(
        AiFlowSurface.NONE,
        aiFlowSurface(isGenerating = false, hasGeneratedMenu = false, hasMessage = false)
    )
}

@Test
fun `probar otra idea se bloquea durante pausa o maximo diario`() {
    assertTrue(
        generatedMenuCanTryAnother(
            isGenerating = false,
            isAiPaused = false,
            limitState = AiGenerationLimitState.AVAILABLE
        )
    )
    assertFalse(
        generatedMenuCanTryAnother(
            isGenerating = false,
            isAiPaused = true,
            limitState = AiGenerationLimitState.AVAILABLE
        )
    )
    assertFalse(
        generatedMenuCanTryAnother(
            isGenerating = false,
            isAiPaused = false,
            limitState = AiGenerationLimitState.HARD_LIMIT
        )
    )
}
```

- [ ] **Step 2: Ejecutar y verificar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest'
```

Expected: error de compilación porque los helpers todavía no existen.

- [ ] **Step 3: Implementar la selección pura y aplicarla en Compose**

Añadir:

```kotlin
internal enum class AiFlowSurface {
    LOADING,
    GENERATED_MENU,
    MESSAGE,
    NONE
}

internal fun aiFlowSurface(
    isGenerating: Boolean,
    hasGeneratedMenu: Boolean,
    hasMessage: Boolean
): AiFlowSurface = when {
    isGenerating -> AiFlowSurface.LOADING
    hasGeneratedMenu -> AiFlowSurface.GENERATED_MENU
    hasMessage -> AiFlowSurface.MESSAGE
    else -> AiFlowSurface.NONE
}

internal fun generatedMenuCanTryAnother(
    isGenerating: Boolean,
    isAiPaused: Boolean,
    limitState: AiGenerationLimitState
): Boolean {
    return !isGenerating &&
        !isAiPaused &&
        limitState != AiGenerationLimitState.HARD_LIMIT
}

@StringRes
internal fun generatedMenuTryAnotherTextRes(
    isAiPaused: Boolean,
    limitState: AiGenerationLimitState
): Int = when {
    isAiPaused -> R.string.ai_resting
    limitState == AiGenerationLimitState.HARD_LIMIT -> R.string.dice_ai_daily_maximum
    else -> R.string.generated_menu_try_another
}
```

Derivar una vez:

```kotlin
val aiSurface = aiFlowSurface(
    isGenerating = state.isGeneratingMenu,
    hasGeneratedMenu = generatedDetailMenu != null,
    hasMessage = message != null
)
```

Renderizar el diálogo de mensaje solo con `aiSurface == AiFlowSurface.MESSAGE`,
el detalle solo con `aiSurface == AiFlowSurface.GENERATED_MENU` y el overlay solo
con `aiSurface == AiFlowSurface.LOADING`.

Pasar al detalle:

```kotlin
val canTryAnother = generatedMenuCanTryAnother(
    isGenerating = state.isGeneratingMenu,
    isAiPaused = state.aiRetryAtMillis != null,
    limitState = state.aiGenerationLimitState
)
```

Usar `canTryAnother` como `enabled` del botón. Resolver el texto con
`generatedMenuTryAnotherTextRes()` y aplicar el contador únicamente cuando el
recurso sea `R.string.generated_menu_try_another`.

- [ ] **Step 4: Ejecutar pruebas focalizadas y verificar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest'
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt \
  app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "fix: make AI flow surfaces mutually exclusive"
```

### Task 3: Documentar y verificar la versión

**Files:**
- Modify: `docs/project-context.md:63-83`

- [ ] **Step 1: Actualizar el contexto**

Añadir al contrato de Inicio:

```markdown
- Carga, detalle generado y avisos de IA son superficies mutuamente excluyentes.
  Ante un fallo del proveedor, el aviso se difiere hasta terminar la búsqueda
  alternativa: un hit abre directamente el menú y un miss muestra un único
  aviso. Durante pausa o máximo diario, el detalle conserva el menú actual y
  bloquea `Probar otra idea` sin cerrar el modal.
```

- [ ] **Step 2: Ejecutar toda la verificación**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, cero tests fallidos y APK debug generado.

- [ ] **Step 3: Revisar diff y whitespace**

Run:

```bash
git diff --check
git status --short
git diff --stat
```

Expected: sin errores de whitespace y únicamente los archivos previstos.

- [ ] **Step 4: Commit**

```bash
git add docs/project-context.md
git commit -m "docs: document exclusive AI limit presentation"
```

## QA manual residual

En `releaseDebuggable` con anuncios demo:

1. Agotar usos gratuitos.
2. Completar el anuncio y confirmar `anuncio -> dado -> menú`.
3. Cerrar un anuncio sin recompensa y confirmar que no genera.
4. Simular cuota del proveedor con hit de colmena y confirmar que no aparece
   “IA descansando” antes del menú.
5. Simular miss y confirmar un único aviso.
6. Alcanzar el máximo recompensado y confirmar que Inicio permite escribir o
   elegir un menú guardado.
