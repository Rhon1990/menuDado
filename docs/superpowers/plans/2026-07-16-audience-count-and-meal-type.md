# Audience Count And Meal Type Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrar el total de menús en cada cabecera de perfil y el tipo de comida en todas sus tarjetas.

**Architecture:** Se extrae el badge de conteo ya usado por Favoritos para reutilizarlo en las cabeceras de público. Helpers puros calculan el total por público y exponen el recurso localizado del tipo de comida; Compose solo presenta esos resultados.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Android resources, Gradle.

---

### Task 1: Definir contratos con pruebas fallidas

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] Añadir una prueba que valide `menuCarouselAudienceMenuCount` con más de 10 menús y públicos mezclados:

```kotlin
val menus = (1L..12L).map { id ->
    FoodMenu(id = id, name = "Adulto $id", mealType = MealType.LUNCH, audience = MenuAudience.ADULT, description = "A")
} + FoodMenu(id = 20L, name = "Peques", mealType = MealType.DINNER, audience = MenuAudience.CHILD, description = "P")

assertEquals(12, menuCarouselAudienceMenuCount(menus, MenuAudience.ADULT))
assertEquals(1, menuCarouselAudienceMenuCount(menus, MenuAudience.CHILD))
```

- [ ] Añadir una prueba que valide `menuCarouselItemMealTypeRes` para desayuno, almuerzo y cena:

```kotlin
assertEquals(R.string.meal_breakfast, menuCarouselItemMealTypeRes(MealType.BREAKFAST))
assertEquals(R.string.meal_lunch, menuCarouselItemMealTypeRes(MealType.LUNCH))
assertEquals(R.string.meal_dinner, menuCarouselItemMealTypeRes(MealType.DINNER))
```
- [ ] Ejecutar `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest` y comprobar RED por helpers inexistentes.
- [ ] Implementar los helpers mínimos y repetir el comando hasta GREEN:

```kotlin
internal fun menuCarouselAudienceMenuCount(menus: List<FoodMenu>, audience: MenuAudience): Int =
    menus.count { it.audience == audience }

@StringRes
internal fun menuCarouselItemMealTypeRes(mealType: MealType): Int = mealTypeLabelRes(mealType)
```

### Task 2: Reutilizar contador y mostrar tipo

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] Extraer `MenuCountBadge(count)` desde la cabecera de Favoritos y usar `pluralStringResource(favoriteMenuCountRes(), count, count)`.
- [ ] Pasar a `MenuCarouselSection` `menuCount = menuCarouselAudienceMenuCount(menus, audience)` calculado sobre la lista completa.
- [ ] Mostrar `MenuCountBadge(menuCount)` junto al título de cada perfil dentro de una fila con `Modifier.weight(1f)`.
- [ ] Hacer que `MenuCarouselItem` muestre siempre el tipo con `menuCarouselItemMealTypeRes(menu.mealType)`; concatenar `menuAudienceLabelRes(menu.audience)` solo cuando `showAudienceLabel` sea verdadero.

### Task 3: Contexto y verificación

**Files:**
- Modify: `docs/project-context.md`

- [ ] Documentar contador por perfil y tipo visible en tarjetas.
- [ ] Ejecutar `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- [ ] Ejecutar `git diff --check`.
- [ ] Instalar el APK debug y comprobar visualmente contador, singular/plural y tipos de comida.
