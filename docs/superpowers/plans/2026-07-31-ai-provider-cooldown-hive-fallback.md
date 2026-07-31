# AI Provider Cooldown Hive Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mantener disponible la generación de ideas mediante la colmena cuando Gemini está en recuperación, sin mostrar `IA descansando` ni repetir llamadas al proveedor antes de tiempo.

**Architecture:** `MenuDadoViewModel` seguirá conservando la recuperación del proveedor para proteger los análisis IA, pero la generación dejará de tratarla como un bloqueo. Al iniciar una generación resolverá si puede llamar a Gemini; con recuperación activa saltará directamente al fallback existente. La UI de generación dejará de interpretar el retry compartido como estado deshabilitado.

**Tech Stack:** Kotlin, Android ViewModel, StateFlow, Jetpack Compose, coroutines-test y JUnit.

---

### Task 1: Cubrir la regresión en el ViewModel

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Añadir pruebas que establezcan `aiQuotaRetryStore.storedRetryAtMillis = Long.MAX_VALUE`, ejecuten `generateMenuIdea()` y exijan cero llamadas al analizador, una búsqueda en colmena y un resultado `HIVE_FALLBACK`. Añadir otra prueba con reloj fijo que complete una generación rápida, descarte el resultado y exija que la segunda generación llame de nuevo al proveedor pese al throttle local almacenado.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest --console=plain`

Expected: FAIL porque el retry y el throttle actuales detienen la generación antes de consultar Gemini o la colmena.

- [ ] **Step 3: Commit the red tests together with the implementation after GREEN**

Los tests rojos no se publicarán aislados; se incluirán en el commit funcional después de verificar GREEN.

### Task 2: Enrutar la generación durante la recuperación

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`

- [ ] **Step 1: Stop treating retry and throttle as generation validation failures**

En `validatedGenerationRequestOrNull()`, conservar validaciones de ejecución activa, campos, público, perfil e ingredientes, pero retirar las salidas tempranas basadas en `activeAiRetryAtMillis()` y `activeAiRequestThrottleAtMillis()`.

- [ ] **Step 2: Resolve provider availability when the request starts**

En `startGeneratedMenuRequest()`, calcular por separado la capacidad diaria y la recuperación activa:

```kotlin
val hasProviderDailyCapacity = currentProviderAiUsedCount(currentPacificDateKey()) <
    AI_PROVIDER_DAILY_HARD_LIMIT
val providerRetryAtMillis = if (hasProviderDailyCapacity) activeAiRetryAtMillis() else null
val shouldCallProvider = hasProviderDailyCapacity && providerRetryAtMillis == null
```

Con `shouldCallProvider == false`, reutilizar `searchHiveFallback()`; usar `quota` como causa durante recuperación y `quota_daily` cuando se alcanzó el límite diario local del proveedor.

- [ ] **Step 3: Verify the focused ViewModel tests pass**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest --console=plain`

Expected: PASS.

### Task 3: Mantener habilitada la UI de generación

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Write the failing UI-state test**

Actualizar la prueba de recuperación del proveedor para exigir que una recuperación activa no pause el dado de generación, ya que existe fallback de colmena.

- [ ] **Step 2: Run the UI-state test to verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest --console=plain`

Expected: FAIL porque `aiGenerationIsPaused()` todavía devuelve `true` con retry activo.

- [ ] **Step 3: Remove retry-based disabling from generation surfaces**

En Inicio y en `GeneratedMenuDetailDialog`, no pasar el retry compartido como pausa de generación. El estado seguirá aplicándose a análisis individual y por lote.

- [ ] **Step 4: Run the UI-state test to verify GREEN**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest --console=plain`

Expected: PASS.

### Task 4: Actualizar contexto y verificar integralmente

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Document the final behavior**

Actualizar la sección de generación para indicar que una recuperación conocida del proveedor no bloquea el dado: las solicitudes usan la colmena hasta el vencimiento y después vuelven a Gemini.

- [ ] **Step 2: Run focused and full verification**

Run: `./gradlew :app:testDebugUnitTest --console=plain`

Run: `./gradlew :app:compileReleaseKotlin --console=plain`

Expected: ambos comandos terminan con `BUILD SUCCESSFUL`.

- [ ] **Step 3: Inspect the diff**

Run: `git diff --check`

Run: `git status --short`

Expected: sin errores de whitespace y solo los archivos previstos.

- [ ] **Step 4: Commit the implementation**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt docs/project-context.md docs/superpowers/plans/2026-07-31-ai-provider-cooldown-hive-fallback.md
git commit -m "fix: keep hive generation available during AI recovery"
```
