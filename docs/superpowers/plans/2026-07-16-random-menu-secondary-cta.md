# Random Menu Secondary CTA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convertir `Elegir un menú al azar` en la segunda acción protagonista mediante un fondo verde de marca y textos blancos.

**Architecture:** Se mantiene el componente `SavedMenuRandomButton` y toda su lógica. Solo se exponen dos funciones puras de color para probar el contrato visual y se cambia el contenedor Material 3 de delineado a sólido.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4.

---

### Task 1: Contrato visual del botón aleatorio

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `seleccion aleatoria usa fondo verde y texto blanco`() {
    assertEquals(MenuDadoColors.BrandGreen, savedMenuRandomContainerColor())
    assertEquals(Color.White, savedMenuRandomContentColor())
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: FAIL porque `savedMenuRandomContainerColor` y `savedMenuRandomContentColor` todavía no existen.

- [ ] **Step 3: Write minimal implementation**

```kotlin
internal fun savedMenuRandomContainerColor(): Color = MenuDadoColors.BrandGreen

internal fun savedMenuRandomContentColor(): Color = Color.White
```

Cambiar `SavedMenuRandomButton` a `Button` con `ButtonDefaults.buttonColors`, usando verde para el contenedor activo, blanco para título e indicador, y blanco con opacidad leve para el texto de apoyo. El estado deshabilitado conservará menor opacidad.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run complete verification**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: `BUILD SUCCESSFUL` y APK en `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 6: Validate visually**

Instalar el APK conservando datos y confirmar que `Lanzar con IA` sigue siendo primario, `Elegir un menú al azar` aparece sólido en verde con ambos textos blancos y `Escribir mi menú` continúa como acción terciaria.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt docs/project-context.md docs/superpowers/plans/2026-07-16-random-menu-secondary-cta.md
git commit -m "feat: promote random menu action"
```
