# Clarify Menu Recipient Microcopy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Evitar que el subtítulo de Inicio sugiera que el usuario necesariamente cocina para otra persona.

**Architecture:** Mantener intactos UI y comportamiento, cambiando solo el valor de `home_today_subtitle` en los tres recursos localizados. La prueba JVM existente fija el contrato de cada idioma y `docs/project-context.md` conserva sincronizada la descripción funcional.

**Tech Stack:** Android string resources, Kotlin, JUnit 4, Gradle.

---

### Task 1: Actualizar el contrato localizado con TDD

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Cambiar únicamente las expectativas de `home_today_subtitle`**

Usar estos valores:

```kotlin
"values" to "No pasa nada. Dinos para quién es el menú y la IA te ayudará con una idea saludable."
"values-en" to "No worries. Tell us who the menu is for and AI will help with a healthy idea."
"values-fr" to "Pas de souci. Dites-nous à qui s’adresse le menu et l’IA vous proposera une idée équilibrée."
```

- [ ] **Step 2: Ejecutar el test y confirmar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.AiCreationMicrocopyTest'
```

Expected: `FAIL` en `home_today_subtitle` porque los recursos todavía contienen la redacción anterior.

- [ ] **Step 3: Cambiar la clave en los tres recursos**

```xml
<!-- values -->
<string name="home_today_subtitle">No pasa nada. Dinos para quién es el menú y la IA te ayudará con una idea saludable.</string>

<!-- values-en -->
<string name="home_today_subtitle">No worries. Tell us who the menu is for and AI will help with a healthy idea.</string>

<!-- values-fr -->
<string name="home_today_subtitle">Pas de souci. Dites-nous à qui s’adresse le menu et l’IA vous proposera une idée équilibrée.</string>
```

- [ ] **Step 4: Ejecutar el test y confirmar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.AiCreationMicrocopyTest'
```

Expected: `BUILD SUCCESSFUL`, 1 test y 0 fallos.

### Task 2: Sincronizar documentación y verificar

**Files:**
- Modify: `docs/project-context.md`
- Verify: `app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt`
- Verify: `app/src/main/res/values/strings.xml`
- Verify: `app/src/main/res/values-en/strings.xml`
- Verify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Actualizar la descripción funcional**

En `docs/project-context.md`, sustituir solamente el subtítulo antiguo:

```markdown
`No pasa nada. Dinos para quién es el menú y la IA te ayudará con una idea saludable.`
```

- [ ] **Step 2: Ejecutar la suite completa**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL` y 0 fallos.

- [ ] **Step 3: Verificar recursos release**

Run:

```bash
./gradlew :app:lintRelease :app:bundleRelease
```

Expected: `BUILD SUCCESSFUL`, lint sin errores y `app/build/outputs/bundle/release/app-release.aab`.

- [ ] **Step 4: Verificar alcance**

Run:

```bash
git diff --check
rg -n 'Elige para quién cocinas|Choose who you.re cooking for|Choisissez pour qui vous cuisinez' \
  app/src/main/res app/src/test docs/project-context.md
git status --short
```

Expected: diff sin errores, ninguna coincidencia de la redacción anterior y solo los seis archivos previstos: plan, prueba, tres recursos y contexto.

- [ ] **Step 5: Crear commit local**

```bash
git add docs/superpowers/plans/2026-07-27-clarify-menu-recipient.md \
  app/src/test/java/com/menudado/ui/AiCreationMicrocopyTest.kt \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml \
  docs/project-context.md
git commit -m "fix: aclarar para quien es el menu"
```
