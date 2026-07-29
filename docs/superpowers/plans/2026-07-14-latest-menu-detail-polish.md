# Latest Menu Detail Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Renombrar el acceso rápido al menú más reciente y eliminar la esquina blanca del detalle mediante un radio compartido.

**Architecture:** Mantener el flujo y los datos existentes. Exponer un contrato puro de radio para ambos modales, usarlo en el contenedor y la cabecera, y actualizar únicamente los recursos localizados y el contexto funcional.

**Tech Stack:** Kotlin, Jetpack Compose, recursos Android XML, JUnit 4, Gradle.

---

### Task 1: Fijar el contrato visual con TDD

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] **Step 1: Escribir el test rojo**

Añadir una prueba que exija `menuDetailContainerCornerRadiusDp() == 28` y que `generatedMenuContainerCornerRadiusDp()` reutilice el mismo valor.

- [ ] **Step 2: Verificar el fallo esperado**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: FAIL porque `menuDetailContainerCornerRadiusDp` todavía no existe.

- [ ] **Step 3: Implementar el contrato mínimo**

Crear `menuDetailContainerCornerRadiusDp(): Int = 28`, delegar el radio generado en él y usarlo tanto en la tarjeta exterior como en las esquinas superiores de `MenuDetailDialog`.

- [ ] **Step 4: Verificar verde**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: BUILD SUCCESSFUL.

### Task 2: Corregir el texto visible y el contexto

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Modify: `docs/project-context.md`

- [ ] **Step 1: Actualizar las traducciones**

Cambiar `home_recent_title` a `Tu último menú`, `Your latest menu` y `Votre dernier menu`.

- [ ] **Step 2: Actualizar la fuente de contexto**

Documentar que Inicio muestra `Tu último menú`, seleccionado por el mayor `createdAt`.

- [ ] **Step 3: Comprobar consistencia**

Run: `rg -n "home_recent_title|Tu último menú|Your latest menu|Votre dernier menu" app/src/main/res docs/project-context.md`

Expected: las tres traducciones y el contexto coinciden.

### Task 3: QA integral

**Files:**
- Verify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Verify: `app/src/main/res/values*/strings.xml`

- [ ] **Step 1: Ejecutar suite y build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Revisar el diff**

Run: `git diff --check && git status --short`

Expected: sin errores de whitespace y solo archivos del alcance.

- [ ] **Step 3: Probar visualmente**

Instalar `app-debug.apk`, abrir el detalle de un menú guardado y confirmar que la cabecera cubre completamente ambas esquinas superiores.

