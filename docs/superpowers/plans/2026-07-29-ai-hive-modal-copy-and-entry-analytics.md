# AI Hive Modal Copy and Entry Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Humanizar el modal de pausa de IA y registrar cada entrada al respaldo antes de su resultado terminal.

**Architecture:** Mantener el modal y el flujo actuales, cambiando solo recursos localizados y el mensaje contextual de alta demanda. Ampliar el contrato existente de Analytics con un evento de inicio llamado desde `searchHiveFallback`, sin datos del perfil y protegido para no afectar el flujo funcional.

**Tech Stack:** Kotlin, Android string resources, Coroutines, JUnit 4, Firebase Analytics, Gradle.

---

## Estructura de archivos

- `app/src/main/java/com/menudado/ui/AiGenerationFailureMessage.kt`: genera el
  mensaje contextual de alta demanda.
- `app/src/main/res/values/strings.xml`: copy español del modal.
- `app/src/main/res/values-en/strings.xml`: copy inglés del modal.
- `app/src/main/res/values-fr/strings.xml`: copy francés del modal.
- `app/src/test/java/com/menudado/ui/AiGenerationFailureMessageTest.kt`: contrato
  del mensaje contextual y privacidad.
- `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`: contrato exacto
  de recursos localizados.
- `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`: interfaz y
  implementación no-op del evento de entrada.
- `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`: envío
  del evento a Firebase Analytics.
- `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`: punto único de entrada
  al respaldo.
- `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`: orden, cardinalidad,
  privacidad y tolerancia a fallos de la telemetría.
- `docs/project-context.md`: contrato funcional actualizado.

No se crearán clases de producción nuevas ni se cambiarán UI, navegación,
Firestore, Remote Config o esquemas persistidos.

### Task 1: Copy cercano y localizado

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`
- Modify: `app/src/test/java/com/menudado/ui/AiGenerationFailureMessageTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/AiGenerationFailureMessage.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Escribir primero el contrato fallido de recursos**

Añadir estas entradas a cada mapa de `AiCreationMicrocopyTest`:

```kotlin
"values" to mapOf(
    // entradas existentes
    "ai_retry_ready_title" to "¿Probamos otra vez?",
    "ai_retry_wait_title" to "Esta vez no encontramos tu idea",
    "ai_retry_ready_badge" to "Ya estamos listos",
    "ai_retry_wait_badge" to "Démonos un momento",
    "ai_retry_ready_body" to
        "Cierra este aviso y vuelve a tocar Ayúdame a elegir. Mantendremos tus preferencias para buscar una idea que encaje contigo.",
    "ai_retry_wait_body" to
        "Cuando termine la pausa, este aviso te lo dirá. Tus preferencias seguirán aquí para que puedas volver a intentarlo con tranquilidad."
)
```

```kotlin
"values-en" to mapOf(
    // entradas existentes
    "ai_retry_ready_title" to "Shall we try again?",
    "ai_retry_wait_title" to "We didn’t find your idea this time",
    "ai_retry_ready_badge" to "We’re ready",
    "ai_retry_wait_badge" to "Let’s give it a moment",
    "ai_retry_ready_body" to
        "Close this notice and tap Help me choose again. We’ll keep your preferences to look for an idea that fits you.",
    "ai_retry_wait_body" to
        "When the pause is over, this notice will let you know. Your preferences will still be here so you can try again with peace of mind."
)
```

```kotlin
"values-fr" to mapOf(
    // entradas existentes
    "ai_retry_ready_title" to "On réessaie ?",
    "ai_retry_wait_title" to "Cette fois, nous n’avons pas trouvé votre idée",
    "ai_retry_ready_badge" to "Nous sommes prêts",
    "ai_retry_wait_badge" to "Donnons-lui un instant",
    "ai_retry_ready_body" to
        "Fermez cet avis et touchez de nouveau Aidez-moi à choisir. Nous conserverons vos préférences pour chercher une idée qui vous corresponde.",
    "ai_retry_wait_body" to
        "Lorsque la pause sera terminée, cet avis vous l’indiquera. Vos préférences resteront disponibles afin que vous puissiez réessayer sereinement."
)
```

- [ ] **Step 2: Escribir primero el contrato fallido de alta demanda**

Actualizar el valor esperado del test restringido y añadir un caso por idioma:

```kotlin
@Test
fun `high demand copy is human and localized without exposing restrictions`() {
    val profile = DietaryProfile(
        ageRange = MenuAudience.ADULT.defaultAgeRange,
        isVegan = true,
        otherAvoidances = "texto privado"
    )
    val expected = mapOf(
        AppLanguage.SPANISH to
            "Queremos proponerte algo que encaje de verdad contigo, pero ahora mismo " +
                "la IA necesita un pequeño respiro para Almuerzo · Persona adulta " +
                "(18+ años) con tu perfil actual.",
        AppLanguage.ENGLISH to
            "We want to suggest something that truly fits you, but AI needs a short " +
                "break before preparing an idea for Lunch · Adult (18+ years) with " +
                "your current profile.",
        AppLanguage.FRENCH to
            "Nous voulons vous proposer quelque chose qui vous corresponde vraiment, " +
                "mais l’IA a besoin d’une courte pause avant de préparer une idée pour " +
                "Déjeuner · Adulte (18 ans et plus) avec votre profil actuel."
    )

    expected.forEach { (language, copy) ->
        val message = contextualAiGenerationFailureMessage(
            language = language,
            reason = AiGenerationFailureReason.HIGH_DEMAND,
            mealType = MealType.LUNCH,
            audience = MenuAudience.ADULT,
            profile = profile
        )

        assertEquals(copy, message)
        assertFalse(message.contains("veg", ignoreCase = true))
        assertFalse(message.contains("texto privado", ignoreCase = true))
    }
}
```

En `every failure reason gives the expected recovery action`, cambiar la
expectativa española de `HIGH_DEMAND` de `"más tarde"` a `"pequeño respiro"`.

- [ ] **Step 3: Ejecutar los tests para observar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.AiCreationMicrocopyTest \
  --tests com.menudado.ui.AiGenerationFailureMessageTest \
  --console=plain
```

Expected: `FAILED`; los recursos y el mensaje de alta demanda aún contienen el
copy anterior.

- [ ] **Step 4: Implementar el copy mínimo de recursos**

Reemplazar los seis recursos `ai_retry_*` en cada `strings.xml` por los valores
exactos definidos en Step 1. No cambiar IDs ni estructura del modal.

- [ ] **Step 5: Implementar el mensaje localizado de alta demanda**

Cambiar únicamente `HIGH_DEMAND` en las tres funciones de
`AiGenerationFailureMessage.kt`:

```kotlin
AiGenerationFailureReason.HIGH_DEMAND ->
    "Queremos proponerte algo que encaje de verdad contigo, pero ahora mismo " +
        "la IA necesita un pequeño respiro para $context$profileSuffix."
```

```kotlin
AiGenerationFailureReason.HIGH_DEMAND ->
    "We want to suggest something that truly fits you, but AI needs a short " +
        "break before preparing an idea for $context$profileSuffix."
```

```kotlin
AiGenerationFailureReason.HIGH_DEMAND ->
    "Nous voulons vous proposer quelque chose qui vous corresponde vraiment, " +
        "mais l’IA a besoin d’une courte pause avant de préparer une idée pour " +
        "$context$profileSuffix."
```

- [ ] **Step 6: Ejecutar los tests para observar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.AiCreationMicrocopyTest \
  --tests com.menudado.ui.AiGenerationFailureMessageTest \
  --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Confirmar el cambio de copy**

```bash
git add \
  app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt \
  app/src/test/java/com/menudado/ui/AiGenerationFailureMessageTest.kt \
  app/src/main/java/com/menudado/ui/AiGenerationFailureMessage.kt \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml
git commit -m "copy: humanize AI retry notice"
```

### Task 2: Evento de entrada a la colmena

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`
- Modify: `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`

- [ ] **Step 1: Escribir primero los tests del evento y su orden**

Actualizar `ai menu generation failure tracks failed event` para exigir:

```kotlin
assertEquals(
    listOf(
        "ai_menu_generation_started:BREAKFAST:0",
        "ai_menu_hive_fallback_started:BREAKFAST:generic",
        "ai_menu_hive_fallback:BREAKFAST:miss:generic:0",
        "ai_menu_generation_finished:BREAKFAST:false:unknown:generic"
    ),
    analytics.events
)
```

Ampliar `provider safeguard routes an entitled generation directly to hive`:

```kotlin
assertEquals(
    1,
    analytics.events.count {
        it == "ai_menu_hive_fallback_started:BREAKFAST:quota_daily"
    }
)
```

Ampliar `hive fallback analytics contains outcome and failure type only`:

```kotlin
assertEquals(
    1,
    analytics.events.count {
        it == "ai_menu_hive_fallback_started:BREAKFAST:temporary"
    }
)
assertTrue(analytics.events.none { "Nombre privado" in it })
```

Añadir la tolerancia a fallos:

```kotlin
@Test
fun `hive entry analytics failure never blocks a compatible fallback`() =
    runTest(dispatcher) {
        analyzer.generateFailure = IllegalStateException("internal")
        hive.searchResult = Result.success(sampleHiveCandidate())
        analytics.throwOnAiMenuHiveFallbackStarted = true

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showGeneratedMenuDetail)
        assertEquals(GeneratedMenuOrigin.HIVE_FALLBACK, viewModel.uiState.value.generatedOrigin)
        assertEquals(1, hive.searches.size)
    }
```

- [ ] **Step 2: Ejecutar el ViewModel test para observar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuDadoViewModelTest \
  --console=plain
```

Expected: `FAILED`; falta `ai_menu_hive_fallback_started`.

- [ ] **Step 3: Ampliar el contrato de Analytics**

En `MenuDadoAnalytics` añadir:

```kotlin
fun trackAiMenuHiveFallbackStarted(
    mealType: MealType,
    triggerFailureType: String
)
```

En `NoOpMenuDadoAnalytics` añadir:

```kotlin
override fun trackAiMenuHiveFallbackStarted(
    mealType: MealType,
    triggerFailureType: String
) = Unit
```

- [ ] **Step 4: Implementar el evento Firebase**

En `FirebaseMenuDadoAnalytics` añadir:

```kotlin
override fun trackAiMenuHiveFallbackStarted(
    mealType: MealType,
    triggerFailureType: String
) {
    logEvent(EVENT_AI_MENU_HIVE_FALLBACK_STARTED) {
        putString(PARAM_MEAL_TYPE, mealType.analyticsName())
        putString(PARAM_FAILURE_TYPE, triggerFailureType.sanitized())
    }
}
```

Y declarar:

```kotlin
const val EVENT_AI_MENU_HIVE_FALLBACK_STARTED = "ai_menu_hive_fallback_started"
```

- [ ] **Step 5: Marcar el punto único de entrada**

En `searchHiveFallback`, después de cambiar la fase y antes de leer rotación o
consultar la fuente:

```kotlin
_uiState.update { it.copy(aiGenerationPhase = AiGenerationPhase.SEARCHING_HIVE) }
runCatching {
    analytics.trackAiMenuHiveFallbackStarted(
        mealType = request.mealType,
        triggerFailureType = triggerFailureType
    )
}
val hiveStartedAtMillis = clockMillisProvider()
```

- [ ] **Step 6: Actualizar el fake de pruebas**

En `FakeAnalytics` añadir:

```kotlin
var throwOnAiMenuHiveFallbackStarted = false
```

```kotlin
override fun trackAiMenuHiveFallbackStarted(
    mealType: MealType,
    triggerFailureType: String
) {
    if (throwOnAiMenuHiveFallbackStarted) {
        throw IllegalStateException("tracking failed")
    }
    events += "ai_menu_hive_fallback_started:${mealType.name}:$triggerFailureType"
}
```

- [ ] **Step 7: Ejecutar el ViewModel test para observar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuDadoViewModelTest \
  --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Confirmar el evento de entrada**

```bash
git add \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt \
  app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt
git commit -m "feat: track AI hive fallback entry"
```

### Task 3: Contexto y verificación integral

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Actualizar el contrato funcional**

En la sección de generación IA, documentar explícitamente:

```markdown
El modal de pausa evita lenguaje técnico y usa un tono cercano: explica que esta
vez no se encontró la idea, conserva las preferencias y avisa cuando se puede
volver a intentar. La causa contextual sigue diferenciando alta demanda, timeout,
conectividad, indisponibilidad y límite diario.
```

Junto al párrafo de Remote Config/colmena, añadir:

```markdown
Cada entrada al respaldo registra `ai_menu_hive_fallback_started` con tipo de
comida y causa técnica. El evento terminal `ai_menu_hive_fallback` conserva
`hit`, `cache_hit`, `miss` o `error`. Ninguno incluye perfil, ingredientes ni
texto libre.
```

- [ ] **Step 2: Ejecutar todos los tests unitarios de app**

Run:

```bash
./gradlew :app:testDebugUnitTest --console=plain
```

Expected: `BUILD SUCCESSFUL`, 0 tests fallidos.

- [ ] **Step 3: Compilar Kotlin debug**

Run:

```bash
./gradlew :app:compileDebugKotlin --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Verificar higiene del diff**

Run:

```bash
git diff --check
git status --short
```

Expected: sin errores de whitespace; solo `docs/project-context.md` pendiente
desde el último commit.

- [ ] **Step 5: Confirmar la documentación**

```bash
git add docs/project-context.md
git commit -m "docs: describe hive entry analytics"
```

- [ ] **Step 6: Verificación final posterior a commits**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:compileDebugKotlin --console=plain
git diff --check
git status --short --branch
git log -4 --oneline --decorate
```

Expected: `BUILD SUCCESSFUL`; worktree limpio en `release/Version_1.3.0`; los
commits de especificación, plan, copy, analítica y contexto están presentes.

## QA manual residual

Con un dispositivo conectado y una cuenta de Firebase Analytics en DebugView:

1. Forzar alta demanda y ausencia de candidato compatible.
2. Confirmar el nuevo copy en español y que el contexto no enumera restricciones.
3. Esperar el fin de la pausa y confirmar el estado `¿Probamos otra vez?`.
4. Repetir con inglés y francés.
5. Confirmar un único `ai_menu_hive_fallback_started` seguido de un único evento
   terminal.
6. Probar `hit`, `miss` y desconexión; el contenido funcional no debe cambiar.

Esta comprobación manual no se sustituye por compilación ni tests JVM y debe
reportarse como pendiente si no hay dispositivo o DebugView disponible.
