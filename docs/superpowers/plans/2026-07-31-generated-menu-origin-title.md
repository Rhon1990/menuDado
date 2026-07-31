# Generated Menu Origin Title Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrar `Idea generada con IA` para resultados de Gemini y `Idea generada` para resultados de la colmena, con equivalentes localizados en inglés y francés.

**Architecture:** Reutilizar `MenuDadoUiState.generatedOrigin` y pasarlo al modal generado. Una función pura seleccionará el recurso de título; el origen nulo caerá al texto neutral para no atribuir falsamente una receta a Gemini.

**Tech Stack:** Kotlin, Jetpack Compose, Android string resources, JUnit 4, Gradle.

---

## Estructura de archivos

- `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: pasar el origen al modal y resolver el recurso del título.
- `app/src/main/res/values/strings.xml`: añadir el título neutral en español.
- `app/src/main/res/values-en/strings.xml`: añadir el título neutral en inglés.
- `app/src/main/res/values-fr/strings.xml`: añadir el título neutral en francés.
- `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: probar la selección del recurso para Gemini, colmena y origen nulo.
- `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`: fijar los dos títulos aprobados en los tres idiomas.
- `docs/project-context.md`: documentar la distinción discreta del título.

### Task 1: Crear las pruebas de regresión

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`

- [ ] **Step 1: Añadir la prueba fallida del selector de título**

Añadir a `MenuCardUiStateTest`:

```kotlin
@Test
fun `generated detail title distinguishes Gemini from hive without exposing fallback`() {
    assertEquals(
        R.string.generated_menu_detail_title,
        generatedMenuDetailTitleRes(GeneratedMenuOrigin.LIVE_AI)
    )
    assertEquals(
        R.string.generated_menu_detail_title_neutral,
        generatedMenuDetailTitleRes(GeneratedMenuOrigin.HIVE_FALLBACK)
    )
    assertEquals(
        R.string.generated_menu_detail_title_neutral,
        generatedMenuDetailTitleRes(null)
    )
}
```

- [ ] **Step 2: Fijar las traducciones aprobadas**

Añadir estas entradas a cada mapa de `AiCreationMicrocopyTest.aiCreationMicrocopyMatchesApprovedCopyInEverySupportedLocale`:

```kotlin
// values
"generated_menu_detail_title" to "Idea generada con IA",
"generated_menu_detail_title_neutral" to "Idea generada",

// values-en
"generated_menu_detail_title" to "AI-generated idea",
"generated_menu_detail_title_neutral" to "Generated idea",

// values-fr
"generated_menu_detail_title" to "Idée générée par l’IA",
"generated_menu_detail_title_neutral" to "Idée générée",
```

- [ ] **Step 3: Ejecutar las pruebas para comprobar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuCardUiStateTest \
  --tests com.menudado.ui.AiCreationMicrocopyTest \
  --console=plain
```

Expected: FAIL de compilación porque `generatedMenuDetailTitleRes` y `generated_menu_detail_title_neutral` todavía no existen.

### Task 2: Implementar el título según el origen

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:887-901`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:6000-6074`
- Modify: `app/src/main/res/values/strings.xml:226`
- Modify: `app/src/main/res/values-en/strings.xml:224`
- Modify: `app/src/main/res/values-fr/strings.xml:224`
- Modify: `docs/project-context.md:61-63`

- [ ] **Step 1: Pasar el origen al modal**

En la llamada a `GeneratedMenuDetailDialog`, añadir:

```kotlin
origin = state.generatedOrigin,
```

En su firma, añadir:

```kotlin
origin: GeneratedMenuOrigin?,
```

- [ ] **Step 2: Añadir el selector puro y usarlo en el título**

Sustituir el recurso fijo del `Text` por:

```kotlin
text = stringResource(id = generatedMenuDetailTitleRes(origin)),
```

Añadir junto a los helpers de detalle generado:

```kotlin
internal fun generatedMenuDetailTitleRes(origin: GeneratedMenuOrigin?): Int =
    if (origin == GeneratedMenuOrigin.LIVE_AI) {
        R.string.generated_menu_detail_title
    } else {
        R.string.generated_menu_detail_title_neutral
    }
```

- [ ] **Step 3: Añadir los recursos localizados**

Después de `generated_menu_detail_title`, añadir respectivamente:

```xml
<!-- values/strings.xml -->
<string name="generated_menu_detail_title_neutral">Idea generada</string>

<!-- values-en/strings.xml -->
<string name="generated_menu_detail_title_neutral">Generated idea</string>

<!-- values-fr/strings.xml -->
<string name="generated_menu_detail_title_neutral">Idée générée</string>
```

- [ ] **Step 4: Actualizar el contexto funcional**

En la descripción del modal generado de `docs/project-context.md`, documentar explícitamente:

```markdown
El título del detalle diferencia discretamente el origen: una respuesta en vivo muestra `Idea generada con IA`; una recuperación desde la colmena muestra `Idea generada`, con equivalentes localizados en inglés y francés. No se añaden iconos, colores ni menciones visibles a la colmena.
```

- [ ] **Step 5: Ejecutar las pruebas enfocadas para comprobar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuCardUiStateTest \
  --tests com.menudado.ui.AiCreationMicrocopyTest \
  --console=plain
```

Expected: PASS en ambas clases, sin fallos.

- [ ] **Step 6: Ejecutar la verificación completa**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:compileReleaseKotlin --rerun-tasks --console=plain
git diff --check
```

Expected: `BUILD SUCCESSFUL`, todas las pruebas con cero fallos y `git diff --check` sin salida.

- [ ] **Step 7: Crear el commit de implementación**

```bash
git add \
  app/src/main/java/com/menudado/ui/MenuDadoScreen.kt \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml \
  app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt \
  app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt \
  docs/project-context.md
git commit -m "feat: distinguish generated menu origin title"
```
