# AI Generation Prompt Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve the specificity, practicality and safety of generated menu responses while preserving the current JSON parser and exactly one model call per generation action.

**Architecture:** Keep the existing `MenuDadoViewModel -> repository -> FirebaseHealthAnalyzer -> generateContent -> GeneratedMenuParser` flow unchanged. Strengthen only the prompt contract in `MenuGenerationPrompt`, with tests that make response quality requirements and instruction priority explicit.

**Tech Stack:** Kotlin, JUnit 4, Firebase AI Logic through the existing analyzer, Gradle.

---

## File map

- Modify `app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt`: organize prompt priorities and add a compact usefulness/output contract.
- Modify `app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt`: lock the quality, priority, variety and strict JSON requirements.
- Verify `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`: retain existing one-generation-call assertions; no production change expected.
- Modify `docs/project-context.md`: document the final prompt contract after implementation.

### Task 1: Lock the prompt quality contract with failing tests

**Files:**
- Modify: `app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt`
- Test: `app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt`

- [ ] **Step 1: Add tests for an executable recipe**

Add these tests inside `MenuGenerationPromptTest`:

```kotlin
@Test
fun `prompt requires a specific executable recipe`() {
    val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

    assertTrue(prompt.contains("nombre concreto"))
    assertTrue(prompt.contains("cantidades aproximadas"))
    assertTrue(prompt.contains("una racion"))
    assertTrue(prompt.contains("2 a 4 indicaciones"))
    assertTrue(prompt.contains("tiempo total de preparacion"))
    assertTrue(prompt.contains("sustitucion, conservacion o servicio"))
}

@Test
fun `prompt separates description notes and health fields`() {
    val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

    assertTrue(prompt.contains("notes no debe repetir description"))
    assertTrue(prompt.contains("estimacion realista para una racion"))
    assertTrue(prompt.contains("breves, utiles y sin tono alarmista"))
}
```

- [ ] **Step 2: Add tests for priorities and genuine variety**

```kotlin
@Test
fun `prompt prioritizes safety before variety`() {
    val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

    val safety = prompt.indexOf("1. seguridad alimentaria")
    val audience = prompt.indexOf("2. publico y edad")
    val mealType = prompt.indexOf("3. tipo de comida")
    val variety = prompt.indexOf("5. variedad y atractivo")

    assertTrue(safety >= 0)
    assertTrue(safety < audience)
    assertTrue(audience < mealType)
    assertTrue(mealType < variety)
}

@Test
fun `prompt requires genuine differentiation from prior ideas`() {
    val prompt = MenuGenerationPrompt.build(
        mealType = MealType.LUNCH,
        avoidIdeas = listOf("Ensalada de pollo", "Bowl de garbanzos")
    ).lowercase()

    assertTrue(prompt.contains("no basta con cambiar el nombre"))
    assertTrue(prompt.contains("base, proteina, tecnica o estilo"))
    assertTrue(prompt.contains("ensalada de pollo"))
    assertTrue(prompt.contains("bowl de garbanzos"))
}
```

- [ ] **Step 3: Add a test for strict unchanged output**

```kotlin
@Test
fun `prompt requires only the existing json schema`() {
    val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

    assertTrue(prompt.contains("solo un objeto json valido"))
    assertTrue(prompt.contains("sin markdown"))
    assertTrue(prompt.contains("sin texto adicional"))
    assertTrue(prompt.contains("sin campos nuevos"))
    assertTrue(prompt.contains("\"name\""))
    assertTrue(prompt.contains("\"description\""))
    assertTrue(prompt.contains("\"notes\""))
    assertTrue(prompt.contains("\"calories\""))
    assertTrue(prompt.contains("\"health_status\""))
    assertTrue(prompt.contains("\"health_reason\""))
    assertTrue(prompt.contains("\"health_suggestion\""))
}
```

- [ ] **Step 4: Run the focused test and confirm RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ai.MenuGenerationPromptTest
```

Expected: FAIL because the new contract phrases are not present in the current prompt.

- [ ] **Step 5: Commit the failing contract tests**

```bash
git add app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt
git commit -m "test: define generated menu quality contract"
```

### Task 2: Implement the compact one-call prompt contract

**Files:**
- Modify: `app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt`
- Test: `app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt`

- [ ] **Step 1: Replace repeated generic rules with explicit priority and quality blocks**

Inside the returned multiline prompt, keep the existing meal, audience, dietary, ingredients and avoid blocks, then use this structure before the JSON schema:

```kotlin
Prioridad obligatoria si dos instrucciones entran en tension:
1. Seguridad alimentaria, alergias y restricciones dieteticas.
2. Publico y edad, incluido embarazo o alimentacion infantil.
3. Tipo de comida solicitado.
4. Uso razonable de ingredientes base.
5. Variedad y atractivo de la propuesta.

Contrato de calidad:
- name debe ser un nombre concreto y reconocible, no una categoria generica ni una lista de ingredientes.
- description debe incluir ingredientes comunes con cantidades aproximadas para una racion adecuada al publico y una preparacion accionable de 2 a 4 indicaciones breves.
- notes debe incluir el tiempo total de preparacion y un consejo util de sustitucion, conservacion o servicio. notes no debe repetir description.
- calories debe ser una estimacion realista para una racion adecuada al publico.
- health_reason y health_suggestion deben ser breves, utiles y sin tono alarmista o de juicio. No des consejo medico.

Variedad real:
- Debe ser distinta de los platos previos; no basta con cambiar el nombre.
- Cambia al menos una dimension relevante: base, proteina, tecnica o estilo, sin romper las prioridades anteriores.
```

Retain the current healthy variety examples once, not in multiple sections.

- [ ] **Step 2: Tighten the JSON instruction without changing fields**

Replace the current output introduction with:

```kotlin
Responde con solo un objeto JSON valido, sin markdown, sin texto adicional y sin campos nuevos:
{
  "name": "nombre breve",
  "description": "ingredientes con cantidades y preparacion breve",
  "notes": "tiempo total y consejo practico",
  "calories": 520,
  "health_status": "saludable",
  "health_reason": "motivo breve",
  "health_suggestion": "sugerencia practica"
}
```

Keep `health_status` restricted to `saludable`, `intermedio` or `no_saludable`, and keep the existing language instruction.

- [ ] **Step 3: Run the focused tests and confirm GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ai.MenuGenerationPromptTest
```

Expected: PASS for old restrictions and the new quality contract.

- [ ] **Step 4: Verify one action still produces one generation call**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest
```

Expected: PASS, including existing assertions around fake analyzer generation call counts. No new `generateContent`, retry or validation path should appear in the diff.

- [ ] **Step 5: Commit the prompt implementation**

```bash
git add app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt
git commit -m "feat: improve generated menu usefulness"
```

### Task 3: Document and verify the prompt change

**Files:**
- Modify: `docs/project-context.md`
- Verify: `app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt`
- Verify: `app/src/main/java/com/menudado/ai/FirebaseHealthAnalyzer.kt`

- [ ] **Step 1: Update the generation contract in project context**

Add a concise bullet to the AI generation section:

```markdown
- El prompt de generación prioriza seguridad, público, tipo de comida, ingredientes base y variedad en ese orden; exige nombre específico, cantidades aproximadas para una ración, preparación accionable, tiempo total y un consejo útil dentro del JSON existente, sin añadir llamadas ni campos.
```

- [ ] **Step 2: Confirm the provider path still contains one call**

Run:

```bash
rg -n "generateContent\(|MenuGenerationPrompt\.build" app/src/main/java/com/menudado/ai app/src/main/java/com/menudado/ui
```

Expected: the existing analyzer generation path contains one `generateContent(MenuGenerationPrompt.build(...))` operation and no new model call.

- [ ] **Step 3: Run prompt regression and compile**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ai.MenuGenerationPromptTest :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit documentation**

```bash
git add docs/project-context.md
git commit -m "docs: record generated menu quality contract"
```
