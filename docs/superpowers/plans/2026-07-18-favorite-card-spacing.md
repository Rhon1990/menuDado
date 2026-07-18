# Favorite Card Spacing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dar más espacio al nombre del favorito y eliminar el hueco entre inspiración culinaria y metadatos sin alterar las acciones ni el comportamiento del carrusel.

**Architecture:** Se mantiene `FavoriteMenuCarouselItem` como componente específico y se cambia únicamente su composición interna. Un `Box` desacopla las acciones superior e inferior del flujo de textos, mientras helpers internos fijan las medidas mediante pruebas unitarias.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4, Gradle.

---

## Mapa de archivos

- `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: composición y tokens visuales de la tarjeta de Favoritos.
- `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: regresión de las medidas que garantizan ancho útil y espaciado continuo.
- `docs/project-context.md`: descripción funcional vigente de la tarjeta destacada.

### Task 1: Proteger la geometría corregida con una prueba

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt:582-595`

- [ ] **Step 1: Escribir la prueba que exige la nueva geometría**

Actualizar `carrusel favorito prioriza el nombre sin repetir la coleccion` con estas aserciones:

```kotlin
assertEquals(300, favoriteCarouselCardWidthDp())
assertEquals(148, favoriteCarouselCardMinHeightDp())
assertEquals(92, favoriteCarouselCoverSizeDp())
assertEquals(18, favoriteCarouselCoverCornerRadiusDp())
assertEquals(3, favoriteCarouselTitleMaxLines())
assertEquals(12, favoriteCarouselContentVerticalPaddingDp())
assertEquals(2, favoriteCarouselMetadataSpacingDp())
assertEquals(52, favoriteCarouselActionReserveWidthDp())
```

Mantener las aserciones existentes de colores y acciones.

- [ ] **Step 2: Ejecutar la prueba y confirmar el estado RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: `FAILED`; el ancho y la portada conservan 280/96 y los nuevos helpers aún no existen.

### Task 2: Redistribuir contenido y acciones

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:4120-4215`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:5653-5663`

- [ ] **Step 1: Añadir los tokens mínimos de presentación**

```kotlin
internal fun favoriteCarouselCardWidthDp(): Int = 300

internal fun favoriteCarouselCardMinHeightDp(): Int = 148

internal fun favoriteCarouselCoverSizeDp(): Int = 92

internal fun favoriteCarouselCoverCornerRadiusDp(): Int = 18

internal fun favoriteCarouselTitleMaxLines(): Int = 3

internal fun favoriteCarouselContentVerticalPaddingDp(): Int = 12

internal fun favoriteCarouselMetadataSpacingDp(): Int = 2

internal fun favoriteCarouselActionReserveWidthDp(): Int = 52
```

- [ ] **Step 2: Sustituir la fila de contenido por una composición desacoplada**

Dentro de la fila que sigue al acento terracota, usar un `Box` de altura fija. Centrar la portada a la izquierda, reservar su ancho más 10 dp para la columna, reservar 52 dp al final para las acciones y anclar estas de forma independiente:

```kotlin
Box(
    modifier = Modifier
        .weight(1f)
        .height(favoriteCarouselCardMinHeightDp().dp)
        .padding(start = 8.dp, end = 4.dp)
) {
    MenuCoverImage(
        menu = menu,
        modifier = Modifier
            .align(Alignment.CenterStart)
            .size(favoriteCarouselCoverSizeDp().dp)
            .clip(RoundedCornerShape(favoriteCarouselCoverCornerRadiusDp().dp))
            .border(
                1.dp,
                MenuDadoColors.SoftSand,
                RoundedCornerShape(favoriteCarouselCoverCornerRadiusDp().dp)
            ),
        showMealTypeLabel = false
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = (favoriteCarouselCoverSizeDp() + 10).dp,
                top = favoriteCarouselContentVerticalPaddingDp().dp,
                end = favoriteCarouselActionReserveWidthDp().dp,
                bottom = favoriteCarouselContentVerticalPaddingDp().dp
            ),
        verticalArrangement = Arrangement.spacedBy(favoriteCarouselMetadataSpacingDp().dp)
    ) {
        Text(
            text = menu.name,
            color = MenuDadoColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            lineHeight = 20.sp,
            maxLines = favoriteCarouselTitleMaxLines(),
            overflow = TextOverflow.Ellipsis
        )
        menu.cuisineInspiration?.let { inspiration ->
            MenuCuisineMetadata(inspiration = inspiration)
        }
        Text(
            text = "${stringResource(id = mealTypeLabelRes(menu.mealType))} · " +
                stringResource(id = menuAudienceLabelRes(menu.audience)),
            color = MenuDadoColors.MutedInk,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    MenuOverflowActionButton(
        onOpenActions = onOpenActions,
        modifier = Modifier.align(Alignment.TopEnd),
        containerColor = favoriteCarouselOverflowActionBackgroundColor(),
        iconTint = favoriteCarouselOverflowActionIconTint()
    )
    FavoriteMenuIconButton(
        isFavorite = true,
        onToggleFavorite = onToggleFavorite,
        modifier = Modifier.align(Alignment.BottomEnd)
    )
}
```

- [ ] **Step 3: Ejecutar la prueba y confirmar el estado GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: `BUILD SUCCESSFUL` y cero pruebas fallidas.

### Task 3: Alinear documentación y ejecutar regresión completa

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Actualizar el contexto funcional**

En la descripción de `Favoritos`, mantener el texto existente y añadir que las acciones están ancladas fuera del flujo textual y que inspiración, tipo y público forman un bloque compacto sin huecos artificiales.

- [ ] **Step 2: Ejecutar pruebas unitarias de debug**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL` y cero pruebas fallidas.

- [ ] **Step 3: Compilar el APK debug**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL` y APK generado en `app/build/outputs/apk/debug/`.

- [ ] **Step 4: Revisar el diff final**

Run:

```bash
git diff --check
git diff -- app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt docs/project-context.md
```

Expected: sin errores de espacios; el diff solo contiene la tarjeta, sus tokens, su prueba y el contexto funcional.

- [ ] **Step 5: Commit de implementación**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt docs/project-context.md docs/superpowers/plans/2026-07-18-favorite-card-spacing.md
git commit -m "fix: balance favorite card content"
```
