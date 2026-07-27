# AI Creation Microcopy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sustituir seis textos técnicos del bloque de creación con IA por la microcopy conversacional aprobada en español, inglés y francés.

**Architecture:** El cambio se limita a recursos Android localizados; no modifica Compose, ViewModel, navegación ni lógica. Un test JVM leerá los tres `strings.xml` y fijará los seis contratos visibles y el placeholder `%1$d`.

**Tech Stack:** Android string resources, Kotlin, JUnit 4, XML DOM, Gradle.

---

### Task 1: Fijar los contratos de microcopy localizada

**Files:**
- Create: `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`
- Modify: `app/src/main/res/values/strings.xml:153-164`
- Modify: `app/src/main/res/values-en/strings.xml:151-162`
- Modify: `app/src/main/res/values-fr/strings.xml:151-162`

- [ ] **Step 1: Escribir el test que todavía falla**

Crear `AiCreationMicrocopyTest.kt`:

```kotlin
package com.menudado.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

class AiCreationMicrocopyTest {
    @Test
    fun `creation block uses approved conversational copy in every locale`() {
        expectedCopy.forEach { (directory, expectedStrings) ->
            val actualStrings = readStrings(findResourceFile(directory))

            expectedStrings.forEach { (key, expectedValue) ->
                assertEquals(
                    "Unexpected copy for $directory:$key",
                    expectedValue,
                    actualStrings.getValue(key)
                )
            }
        }
    }

    private fun findResourceFile(directory: String): Path {
        val start = Paths.get("").toAbsolutePath().normalize()
        return generateSequence(start) { current -> current.parent }
            .map { root -> root.resolve("app/src/main/res/$directory/strings.xml") }
            .firstOrNull(Files::isRegularFile)
            ?: error("Could not locate $directory/strings.xml from $start")
    }

    private fun readStrings(file: Path): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val nodes = factory.newDocumentBuilder()
            .parse(file.toFile())
            .getElementsByTagName("string")

        return buildMap {
            repeat(nodes.length) { index ->
                val element = nodes.item(index) as Element
                put(
                    element.getAttribute("name"),
                    element.textContent.replace("\\'", "'")
                )
            }
        }
    }

    private companion object {
        val expectedCopy = mapOf(
            "values" to mapOf(
                "home_today_title" to "¿No sabes qué preparar hoy?",
                "home_today_subtitle" to
                    "No pasa nada. Elige para quién cocinas y la IA te ayudará con una idea saludable.",
                "home_ai_dice_title" to "Encontremos algo rico",
                "form_base_ingredients" to "¿Qué tienes en casa? (Opcional)",
                "form_base_ingredients_placeholder" to "Ej. tomate, arroz o pollo",
                "dice_roll_ai_with_count" to "Ayúdame a elegir (%1\$d)"
            ),
            "values-en" to mapOf(
                "home_today_title" to "Not sure what to make today?",
                "home_today_subtitle" to
                    "No worries. Choose who you're cooking for and AI will help with a healthy idea.",
                "home_ai_dice_title" to "Let's find something tasty",
                "form_base_ingredients" to "What do you have at home? (Optional)",
                "form_base_ingredients_placeholder" to "E.g. tomato, rice or chicken",
                "dice_roll_ai_with_count" to "Help me choose (%1\$d)"
            ),
            "values-fr" to mapOf(
                "home_today_title" to "Vous ne savez pas quoi préparer aujourd’hui ?",
                "home_today_subtitle" to
                    "Pas de souci. Choisissez pour qui vous cuisinez et l’IA vous proposera une idée équilibrée.",
                "home_ai_dice_title" to "Trouvons quelque chose de bon",
                "form_base_ingredients" to "Qu’avez-vous à la maison ? (Facultatif)",
                "form_base_ingredients_placeholder" to "Ex. tomate, riz ou poulet",
                "dice_roll_ai_with_count" to "Aidez-moi à choisir (%1\$d)"
            )
        )
    }
}
```

- [ ] **Step 2: Ejecutar el test y confirmar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.AiCreationMicrocopyTest'
```

Expected: `FAIL` porque `home_today_title` todavía contiene `Qué comer hoy`.

- [ ] **Step 3: Cambiar únicamente las seis claves en español**

En `app/src/main/res/values/strings.xml`:

```xml
<string name="dice_roll_ai_with_count">Ayúdame a elegir (%1$d)</string>
<string name="home_today_title">¿No sabes qué preparar hoy?</string>
<string name="home_today_subtitle">No pasa nada. Elige para quién cocinas y la IA te ayudará con una idea saludable.</string>
<string name="home_ai_dice_title">Encontremos algo rico</string>
<string name="form_base_ingredients">¿Qué tienes en casa? (Opcional)</string>
<string name="form_base_ingredients_placeholder">Ej. tomate, arroz o pollo</string>
```

- [ ] **Step 4: Aplicar la adaptación inglesa**

En `app/src/main/res/values-en/strings.xml`:

```xml
<string name="dice_roll_ai_with_count">Help me choose (%1$d)</string>
<string name="home_today_title">Not sure what to make today?</string>
<string name="home_today_subtitle">No worries. Choose who you\'re cooking for and AI will help with a healthy idea.</string>
<string name="home_ai_dice_title">Let\'s find something tasty</string>
<string name="form_base_ingredients">What do you have at home? (Optional)</string>
<string name="form_base_ingredients_placeholder">E.g. tomato, rice or chicken</string>
```

- [ ] **Step 5: Aplicar la adaptación francesa**

En `app/src/main/res/values-fr/strings.xml`:

```xml
<string name="dice_roll_ai_with_count">Aidez-moi à choisir (%1$d)</string>
<string name="home_today_title">Vous ne savez pas quoi préparer aujourd’hui ?</string>
<string name="home_today_subtitle">Pas de souci. Choisissez pour qui vous cuisinez et l’IA vous proposera une idée équilibrée.</string>
<string name="home_ai_dice_title">Trouvons quelque chose de bon</string>
<string name="form_base_ingredients">Qu’avez-vous à la maison ? (Facultatif)</string>
<string name="form_base_ingredients_placeholder">Ex. tomate, riz ou poulet</string>
```

- [ ] **Step 6: Ejecutar el test y confirmar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.AiCreationMicrocopyTest'
```

Expected: `BUILD SUCCESSFUL`, 1 test y 0 fallos.

- [ ] **Step 7: Confirmar que el diff no toca otras claves**

Run:

```bash
git diff -- app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml
```

Expected: solo seis reemplazos por archivo.

- [ ] **Step 8: Commit**

```bash
git add app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml
git commit -m "feat: acercar textos de creacion con ia"
```

### Task 2: Sincronizar contexto y verificar producción

**Files:**
- Modify: `docs/project-context.md`
- Verify: `app/src/main/res/values/strings.xml`
- Verify: `app/src/main/res/values-en/strings.xml`
- Verify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Actualizar el contrato visible de Inicio**

En `docs/project-context.md`, sustituir las referencias visibles del bloque principal:

```markdown
- El bloque principal de Inicio abre con `¿No sabes qué preparar hoy?`, explica que la IA ayudará con una idea saludable y mantiene la generación con IA como estado inicial.
- La acción principal de generación se muestra como `Ayúdame a elegir`; conserva el contador de usos y abre el mismo flujo de creación con IA.
- El campo opcional se presenta como `¿Qué tienes en casa? (Opcional)` y permite indicar ingredientes que la IA puede incorporar.
```

Buscar después todas las menciones documentales de `Qué comer hoy`, `Generar con IA`,
`Ingredientes base opcionales` y `Lanzar con IA`. Actualizar únicamente las que describan
la etiqueta visible de una de las seis claves aprobadas. No modificar contratos internos,
nombres de eventos, prompts ni referencias técnicas que no sean texto visible.

- [ ] **Step 2: Ejecutar la suite completa**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL` y 0 fallos.

- [ ] **Step 3: Ejecutar lint y bundle release**

Run:

```bash
./gradlew :app:lintRelease :app:bundleRelease
```

Expected: `BUILD SUCCESSFUL`, lint sin errores y `app/build/outputs/bundle/release/app-release.aab`.

- [ ] **Step 4: Verificar alcance y formato**

Run:

```bash
git diff --check
git status --short
rg -n '¿No sabes qué preparar hoy\\?|Ayúdame a elegir|¿Qué tienes en casa\\?' \
  app/src/main/res/values/strings.xml docs/project-context.md
```

Expected: diff sin errores, solo los archivos previstos y coincidencias en recursos/documentación.

- [ ] **Step 5: QA visual si existe dispositivo**

Run:

```bash
adb devices -l
```

Si hay un dispositivo conectado, instalar debug y revisar el bloque en español, inglés y francés con fuente normal y ampliada. Si no hay dispositivo, registrar el riesgo residual sin afirmar QA visual.

- [ ] **Step 6: Commit**

```bash
git add docs/project-context.md
git commit -m "docs: actualizar microcopy visible de inicio"
```
