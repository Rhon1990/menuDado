# Favorite Card Content Hierarchy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simplificar la colección de favoritos y priorizar el nombre de cada menú sin perder acciones directas ni accesibilidad.

**Architecture:** Se mantiene el componente específico del carrusel y se ajustan únicamente sus tokens y composición. Helpers internos fijan la jerarquía esperada para que `MenuCardUiStateTest` proteja el diseño sin introducir cambios en datos, navegación o ViewModel.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Gradle Android.

---

### Task 1: Fijar la jerarquía visual con una prueba fallida

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt:443`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:5421`

- [ ] **Step 1: Actualizar la prueba de presentación**

Cambiar el test `carrusel favorito usa tarjeta horizontal compacta y acento de marca` para esperar una tarjeta de `280 x 132 dp`, portada de `96 dp`, título de tres líneas, cabecera sin icono decorativo y tarjeta sin etiqueta repetida:

```kotlin
assertEquals(280, favoriteCarouselCardWidthDp())
assertEquals(132, favoriteCarouselCardMinHeightDp())
assertEquals(96, favoriteCarouselCoverSizeDp())
assertEquals(3, favoriteCarouselTitleMaxLines())
assertFalse(favoriteCarouselShowsHeaderIcon())
assertFalse(favoriteCarouselShowsSupportingLabel())
```

- [ ] **Step 2: Ejecutar el test y comprobar RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: `FAIL` porque los helpers nuevos aún no existen o los tokens conservan los valores anteriores.

- [ ] **Step 3: Añadir los helpers mínimos**

En `MenuDadoScreen.kt`, actualizar los tokens y añadir:

```kotlin
internal fun favoriteCarouselCardWidthDp(): Int = 280
internal fun favoriteCarouselCardMinHeightDp(): Int = 132
internal fun favoriteCarouselCoverSizeDp(): Int = 96
internal fun favoriteCarouselTitleMaxLines(): Int = 3
internal fun favoriteCarouselShowsHeaderIcon(): Boolean = false
internal fun favoriteCarouselShowsSupportingLabel(): Boolean = false
```

- [ ] **Step 4: Ejecutar el test y comprobar GREEN**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: `BUILD SUCCESSFUL` y todos los tests de la clase pasan.

### Task 2: Aplicar la nueva composición de Favoritos

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:3860`

- [ ] **Step 1: Simplificar la cabecera**

Eliminar el `Box` con `ic_favorite_filled` de `FavoriteMenuCarouselSection`, manteniendo título, contador y `Ver más` en la misma fila.

- [ ] **Step 2: Liberar la columna del título**

En `FavoriteMenuCarouselItem`, superponer `MenuOverflowActionButton` en la esquina superior derecha de `MenuCoverImage`; retirar el botón de la fila del título.

- [ ] **Step 3: Reforzar el título y eliminar la repetición**

Usar `bodyLarge`, `lineHeight = 20.sp` y `maxLines = favoriteCarouselTitleMaxLines()` para `menu.name`. Eliminar el `Text` con `R.string.favorite_menus` de la fila inferior y alinear `FavoriteMenuIconButton` al final.

- [ ] **Step 4: Mantener accesibilidad y espaciado**

Conservar las descripciones semánticas existentes y aplicar los tokens de `280 x 132 dp` y portada `96 dp`, sin reducir los objetivos táctiles.

- [ ] **Step 5: Ejecutar la prueba focalizada**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Actualizar contexto y verificar regresiones

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Actualizar el índice funcional**

En la descripción de la colección `Favoritos`, indicar que la cabecera evita iconografía redundante y que las tarjetas priorizan el nombre hasta tres líneas, conservando un único corazón accionable.

- [ ] **Step 2: Verificar la suite unitaria**

Run: `./gradlew :app:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL` sin tests fallidos.

- [ ] **Step 3: Verificar compilación de la app**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL` y APK debug generado.

- [ ] **Step 4: Revisar calidad del diff**

Run: `git diff --check`

Expected: salida vacía y código de retorno `0`.

- [ ] **Step 5: Validar visualmente**

Instalar/abrir la variante debug en emulador, navegar a Inicio con favoritos y comprobar: cabecera sin corazón decorativo, ausencia de la etiqueta `Favoritos` en tarjetas, nombre largo visible hasta tres líneas, menú de tres puntos sobre la portada y corazón operativo.
