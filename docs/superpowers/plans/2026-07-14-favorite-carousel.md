# Favorite Carousel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Diferenciar visualmente el carrusel de Favoritos y añadir un flujo `Ver más` que muestre todos los favoritos.

**Architecture:** Mantener el estado local existente y extender la navegación interna de listas con una ruta cerrada para Favoritos. Crear una presentación circular exclusiva para Inicio y reutilizar `MenuCarouselItem` en la pantalla completa para preservar acciones y reducir regresiones.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Gradle Android.

---

### Task 1: Contrato de navegación y selección de Favoritos

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] **Step 1: Escribir pruebas fallidas para ruta, orden y tokens visuales**

Añadir pruebas que validen `menuFavoritesDetailRouteAfterViewMore()`, `menuIsFavoritesDetailRoute(route)`, `menuFavoriteDetailMenus(menus)`, `favoriteCarouselCoverSizeDp()` y `favoriteCarouselBackgroundColor()`.

- [ ] **Step 2: Ejecutar las pruebas y comprobar que fallan**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: `FAIL` porque los nuevos helpers todavía no existen.

- [ ] **Step 3: Implementar el contrato mínimo**

Añadir en `MenuDadoScreen.kt`:

```kotlin
private const val FAVORITES_DETAIL_ROUTE = "FAVORITES"

internal fun menuFavoritesDetailRouteAfterViewMore(): String = FAVORITES_DETAIL_ROUTE

internal fun menuIsFavoritesDetailRoute(route: String?): Boolean = route == FAVORITES_DETAIL_ROUTE

internal fun menuFavoriteDetailMenus(menus: List<FoodMenu>): List<FoodMenu> = menuFavoriteMenus(menus)

internal fun favoriteCarouselCoverSizeDp(): Int = 108

internal fun favoriteCarouselBackgroundColor(): Color = MenuDadoColors.SelectionGreen
```

- [ ] **Step 4: Ejecutar las pruebas focalizadas**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "test: define favorite collection navigation"
```

### Task 2: Carrusel circular y acción Ver más

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Conectar la acción específica de Favoritos**

Añadir `onViewMoreFavorites: () -> Unit` a `MenuCarouselSections` y `FavoriteMenuCarouselSection`. En Home, registrar `cta_tapped` con `view_more_favorites` y asignar `audienceDetailRoute = menuFavoritesDetailRouteAfterViewMore()`.

- [ ] **Step 2: Crear el encabezado destacado**

Envolver la sección en una superficie `favoriteCarouselBackgroundColor()`, con corazón, título, `menu_count` y `TextButton` usando `R.string.view_more`. Mostrar la acción siempre que la sección exista.

- [ ] **Step 3: Crear la tarjeta circular reutilizando acciones existentes**

Crear `FavoriteMenuCarouselItem` con portada circular de `favoriteCarouselCoverSizeDp()`, borde `SoftSand`, `FavoriteMenuIconButton`, `MenuOverflowActionButton`, nombre a dos líneas y tipo de comida. Mantener `onOpenMenu`, `onOpenActions` y `onToggleFavorite`.

- [ ] **Step 4: Añadir texto de apoyo localizado**

Añadir `favorite_menus_supporting`:

```xml
<string name="favorite_menus_supporting">Tus elecciones guardadas</string>
```

con equivalentes `Your saved picks` y `Vos choix enregistrés`.

- [ ] **Step 5: Compilar y ejecutar pruebas focalizadas**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/main/res/values*/strings.xml
git commit -m "feat: redesign favorite menu carousel"
```

### Task 3: Pantalla completa de Favoritos

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Escribir prueba del cierre al quedar vacío**

Añadir una prueba de `menuShouldLeaveFavoritesDetail(route, menus)` que devuelva `true` solo para la ruta de Favoritos sin favoritos.

- [ ] **Step 2: Ejecutar la prueba y comprobar que falla**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: `FAIL` porque el helper no existe.

- [ ] **Step 3: Implementar pantalla y salida segura**

Crear `FavoriteMenusDetailScreen` con título, contador y grilla de dos columnas reutilizando `MenuCarouselItem`. Dar prioridad a esta ruta antes de `MenuAudienceDetailScreen` y volver a Home mediante `LaunchedEffect` cuando `menuShouldLeaveFavoritesDetail(...)` sea verdadero.

- [ ] **Step 4: Ejecutar pruebas focalizadas**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: add full favorite menu collection"
```

### Task 4: Contexto y verificación completa

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Actualizar el contrato funcional**

Documentar que Favoritos usa una colección circular destacada y que `Ver más` abre todos los favoritos en una grilla interna.

- [ ] **Step 2: Ejecutar validación automatizada**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`.

Run: `git diff --check`

Expected: salida vacía.

- [ ] **Step 3: Ejecutar QA manual en emulador**

Instalar `app/build/outputs/apk/debug/app-debug.apk` y validar: diferenciación circular, encabezado/contador, `Ver más`, retroceso, detalle, menú contextual, quitar favorito y salida al quedar vacío.

- [ ] **Step 4: Commit**

```bash
git add docs/project-context.md
git commit -m "docs: describe favorite collection experience"
```
