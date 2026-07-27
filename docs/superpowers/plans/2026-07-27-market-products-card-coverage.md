# Market Products Card Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrar el bloque existente `Productos para este menú` en el carrusel de Favoritos y en todas las tarjetas de `Ver más`, sin alterar los carruseles normales por público.

**Architecture:** `MenuDadoScreen.kt` seguirá siendo la única capa de composición afectada. Una función pura seleccionará los nombres visibles según el contexto de la tarjeta y `ShoppingProductsPreview` continuará siendo el único componente que renderiza el bloque, evitando variantes y lógica duplicada.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4.

---

### Task 1: Ampliar la cobertura del bloque de productos

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Escribir la prueba RED**

Añadir un test que construya un menú con dos `ShoppingProduct` y compruebe:

```kotlin
@Test
fun `productos se muestran en favoritos y ver mas pero no en carrusel normal`() {
    val menu = FoodMenu(
        name = "Ensalada",
        mealType = MealType.LUNCH,
        description = "Tomate y aguacate",
        shoppingProducts = listOf(
            requireNotNull(ShoppingProduct.fromAi("Tomate")),
            requireNotNull(ShoppingProduct.fromAi("Aguacate"))
        )
    )

    assertEquals(
        listOf("Tomate", "Aguacate"),
        menuCardShoppingProductNames(menu, showShoppingProducts = true)
    )
    assertTrue(menuCardShoppingProductNames(menu, showShoppingProducts = false).isEmpty())
    assertTrue(
        menuCardShoppingProductNames(
            menu.copy(shoppingProducts = emptyList()),
            showShoppingProducts = true
        ).isEmpty()
    )
}
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDevelopUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest.productos se muestran en favoritos y ver mas pero no en carrusel normal'
```

Expected: FAIL porque `menuCardShoppingProductNames` no existe.

- [ ] **Step 3: Implementar la selección mínima**

Añadir en `MenuDadoScreen.kt`:

```kotlin
internal fun menuCardShoppingProductNames(
    menu: FoodMenu,
    showShoppingProducts: Boolean
): List<String> {
    return if (showShoppingProducts) {
        menu.shoppingProducts.map { product -> product.displayName }
    } else {
        emptyList()
    }
}
```

- [ ] **Step 4: Reutilizar el bloque en las tarjetas solicitadas**

En `FavoriteMenuCarouselItem`, obtener los nombres con `showShoppingProducts = true` y añadir `ShoppingProductsPreview` solo cuando la lista no esté vacía.

En `MenuCarouselItem`, añadir `showShoppingProducts: Boolean = false`, obtener los nombres mediante la misma función y renderizar el bloque solo cuando no esté vacío.

Pasar `showShoppingProducts = true` desde:

- `FavoriteMenusDetailScreen`.
- `MenuAudienceMealGroupSection`.

No pasar el parámetro desde `MenuCarouselSection`, de modo que los carruseles normales por público mantengan su composición actual.

- [ ] **Step 5: Ejecutar GREEN y regresión**

Run:

```bash
./gradlew :app:testDevelopUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest'
./gradlew :app:compileDevelopKotlin
```

Expected: ambos comandos terminan con `BUILD SUCCESSFUL`.

- [ ] **Step 6: Revisar el diff sin mezclar trabajo previo**

Run:

```bash
git diff --check
git diff -- app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
```

Expected: sin errores de whitespace y con cambios limitados a la función pura, sus pruebas y los tres puntos de renderizado. El commit funcional se pospone porque ambos archivos ya contienen la implementación de lista de mercado todavía no consolidada.
