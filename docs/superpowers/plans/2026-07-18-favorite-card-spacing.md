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

- [x] **Step 1: Escribir la prueba que exige la nueva geometría**

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
assertEquals(Alignment.CenterStart, favoriteCarouselContentAlignment())
assertEquals(
    Modifier.defaultMinSize(minHeight = 148.dp),
    Modifier.favoriteCarouselCardMinHeight()
)
```

Mantener las aserciones existentes de colores y acciones.

- [x] **Step 2: Ejecutar la prueba y confirmar el estado RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: `FAILED`; el ancho y la portada conservan 280/96 y los nuevos helpers aún no existen.

### Task 2: Redistribuir contenido y acciones

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:4120-4215`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:5653-5663`

- [x] **Step 1: Añadir los tokens mínimos de presentación**

```kotlin
internal fun favoriteCarouselCardWidthDp(): Int = 300

internal fun favoriteCarouselCardMinHeightDp(): Int = 148

internal fun Modifier.favoriteCarouselCardMinHeight(): Modifier =
    defaultMinSize(minHeight = favoriteCarouselCardMinHeightDp().dp)

internal fun favoriteCarouselCoverSizeDp(): Int = 92

internal fun favoriteCarouselCoverCornerRadiusDp(): Int = 18

internal fun favoriteCarouselTitleMaxLines(): Int = 3

internal fun favoriteCarouselContentVerticalPaddingDp(): Int = 12

internal fun favoriteCarouselMetadataSpacingDp(): Int = 2

internal fun favoriteCarouselActionReserveWidthDp(): Int = 52

internal fun favoriteCarouselContentAlignment(): Alignment = Alignment.CenterStart
```

- [x] **Step 2: Sustituir la fila de contenido por una composición desacoplada**

Usar un `Box` con altura mínima flexible. Dibujar el acento terracota sobre toda la altura resultante, centrar la portada a la izquierda, reservar su ancho más 10 dp para la columna, reservar 52 dp al final para las acciones y anclar estas de forma independiente:

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .favoriteCarouselCardMinHeight()
        .drawBehind {
            drawRect(
                color = favoriteCarouselAccentColor(),
                size = size.copy(width = 4.dp.toPx())
            )
        }
        .padding(start = 12.dp, end = 4.dp)
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
            .align(favoriteCarouselContentAlignment())
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

- [x] **Step 3: Ejecutar la prueba y confirmar el estado GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: `BUILD SUCCESSFUL` y cero pruebas fallidas.

### Task 3: Alinear documentación y ejecutar regresión completa

**Files:**
- Modify: `docs/project-context.md`

- [x] **Step 1: Actualizar el contexto funcional**

En la descripción de `Favoritos`, mantener el texto existente y añadir que las acciones están ancladas fuera del flujo textual y que inspiración, tipo y público forman un bloque compacto sin huecos artificiales.

- [x] **Step 2: Ejecutar pruebas unitarias de debug**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL` y cero pruebas fallidas.

- [x] **Step 3: Compilar el APK debug**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL` y APK generado en `app/build/outputs/apk/debug/`.

- [x] **Step 4: Revisar el diff final**

Run:

```bash
git diff --check
git diff -- app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt docs/project-context.md
```

Expected: sin errores de espacios; el diff solo contiene la tarjeta, sus tokens, su prueba y el contexto funcional.

- [x] **Step 5: Commit de implementación**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt docs/project-context.md docs/superpowers/plans/2026-07-18-favorite-card-spacing.md
git commit -m "fix: balance favorite card content"
```
