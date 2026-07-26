# Market List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar una lista de mercado local-first, generada únicamente por IA, consolidada sin cantidades y sincronizada por usuario.

**Architecture:** La IA devuelve `ShoppingProduct` junto con la generación o análisis existente. Room persiste la relación por menú y el estado global; el repositorio expone una lista consolidada. Firestore amplía los documentos de menú y añade estados de mercado por producto. La UI incorpora un cuarto destino `Mercado`.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Kotlin Coroutines/Flow, Firebase AI Logic, Firebase Firestore, JUnit 4.

---

### Task 1: Contratos y parseo de productos IA

**Files:**
- Create: `app/src/main/java/com/menudado/domain/ShoppingModels.kt`
- Modify: `app/src/main/java/com/menudado/domain/MenuModels.kt`
- Modify: `app/src/main/java/com/menudado/domain/GeneratedMenuParser.kt`
- Modify: `app/src/main/java/com/menudado/domain/HealthAnalysisParser.kt`
- Modify: `app/src/main/java/com/menudado/ai/HealthAnalyzer.kt`
- Modify: `app/src/main/java/com/menudado/ai/FirebaseHealthAnalyzer.kt`
- Modify: `app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt`
- Test: `app/src/test/java/com/menudado/domain/ShoppingProductTest.kt`
- Test: `app/src/test/java/com/menudado/domain/GeneratedMenuParserTest.kt`
- Test: `app/src/test/java/com/menudado/domain/HealthAnalysisParserTest.kt`
- Test: `app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt`

- [ ] **Step 1: Añadir tests RED**

```kotlin
@Test fun `normaliza nombre y crea clave estable`() {
    val first = ShoppingProduct.fromAi("  Tomáte  ")
    val second = ShoppingProduct.fromAi("tomate")
    assertEquals(first.key, second.key)
    assertEquals("tomate", first.normalizedName)
}

@Test fun `lista ausente no invalida menu generado`() {
    val generated = GeneratedMenuParser.parse(validGeneratedMenuJsonWithoutProducts)
    assertTrue(generated.shoppingProducts.isEmpty())
}

@Test fun `analisis devuelve salud y productos`() {
    val details = HealthAnalysisParser.parse(validAnalysisJsonWithProducts)
    assertEquals(listOf("Tomate"), details.shoppingProducts.map { it.displayName })
}
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.domain.ShoppingProductTest' --tests 'com.menudado.domain.GeneratedMenuParserTest' --tests 'com.menudado.domain.HealthAnalysisParserTest'
```

Expected: FAIL porque `ShoppingProduct`, `shoppingProducts` y `MenuAiDetails` todavía no existen.

- [ ] **Step 3: Implementar contratos mínimos**

```kotlin
data class ShoppingProduct(
    val key: String,
    val normalizedName: String,
    val displayName: String
) {
    companion object {
        fun fromAi(value: String): ShoppingProduct?
    }
}

data class MenuAiDetails(
    val healthAnalysis: HealthAnalysis,
    val shoppingProducts: List<ShoppingProduct>
)
```

Actualizar `GeneratedMenu` y `FoodMenu` con `shoppingProducts: List<ShoppingProduct> = emptyList()`. Cambiar `HealthAnalyzer.analyze` y `analyzeBatch` para devolver `MenuAiDetails`.

- [ ] **Step 4: Ampliar prompts y parsers**

Añadir `"shopping_products": ["producto"]` a generación, análisis individual y análisis por lote. Parsear el array de forma independiente, descartando vacíos, entradas de más de 80 caracteres y duplicados por clave.

- [ ] **Step 5: Ejecutar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.domain.*' --tests 'com.menudado.ai.MenuGenerationPromptTest'
```

Expected: BUILD SUCCESSFUL.

### Task 2: Room y repositorio de mercado

**Files:**
- Create: `app/src/main/java/com/menudado/data/MarketEntities.kt`
- Create: `app/src/main/java/com/menudado/data/MarketDao.kt`
- Create: `app/src/main/java/com/menudado/data/MarketRepository.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuDadoDatabase.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuDadoMigrations.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuRepository.kt`
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Test: `app/src/test/java/com/menudado/data/MenuDadoMigrationsTest.kt`
- Test: `app/src/test/java/com/menudado/data/MarketRepositoryTest.kt`

- [ ] **Step 1: Añadir tests RED de migración y consolidación**

```kotlin
@Test fun `migration 10 to 11 creates market tables`() {
    assertEquals(11, MENU_DADO_DATABASE_VERSION)
    assertTrue(MIGRATION_10_TO_11_SQL.any { it.contains("menu_shopping_products") })
    assertTrue(MIGRATION_10_TO_11_SQL.any { it.contains("market_product_states") })
}

@Test fun `dos menus con tomate producen un solo pendiente`() = runTest {
    repository.replaceMenuProducts(1, listOf(tomato), activate = true)
    repository.replaceMenuProducts(2, listOf(tomato), activate = true)
    assertEquals(1, repository.currentItems().size)
}
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.data.MenuDadoMigrationsTest' --tests 'com.menudado.data.MarketRepositoryTest'
```

Expected: FAIL por versión 10 y clases ausentes.

- [ ] **Step 3: Crear entidades y DAO**

```kotlin
@Entity(primaryKeys = ["menuId", "productKey"], tableName = "menu_shopping_products")
data class MenuShoppingProductEntity(
    val menuId: Long,
    val productKey: String,
    val normalizedName: String,
    val displayName: String,
    val isActive: Boolean
)

@Entity(tableName = "market_product_states")
data class MarketProductStateEntity(
    @PrimaryKey val productKey: String,
    val displayName: String,
    val isPurchased: Boolean,
    val purchasedAt: Long?,
    val updatedAt: Long,
    val remoteSyncState: String,
    val deletedAt: Long?,
    val remoteSyncToken: String?
)
```

El DAO debe observar productos por menú, relaciones activas y estados; reemplazar relaciones en transacción; activar/desactivar por menú; marcar/restaurar/limpiar comprados; y exponer pendientes de sincronización.

- [ ] **Step 4: Migrar e integrar**

Subir Room a 11, crear ambas tablas e índices por `productKey`, `menuId` e `isActive`, registrar `MIGRATION_10_TO_11` y exponer `marketDao()`.

- [ ] **Step 5: Implementar `MarketRepository`**

Combinar relaciones activas y estados en:

```kotlin
data class MarketListItem(
    val product: ShoppingProduct,
    val isPurchased: Boolean
)
```

Todas las mutaciones deben escribir primero en Room. Activar un producto comprado lo devuelve a pendiente.

- [ ] **Step 6: Ejecutar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.data.MenuDadoMigrationsTest' --tests 'com.menudado.data.MarketRepositoryTest'
```

Expected: BUILD SUCCESSFUL.

### Task 3: Persistencia remota

**Files:**
- Modify: `app/src/main/java/com/menudado/backend/MenuDadoRemoteDataSource.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuRepository.kt`
- Modify: `app/src/main/java/com/menudado/data/MarketRepository.kt`
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Test: `app/src/test/java/com/menudado/backend/MenuDadoRemoteDataSourceTest.kt`
- Test: `app/src/test/java/com/menudado/data/MenuRepositorySyncTest.kt`
- Test: `app/src/test/java/com/menudado/data/MarketRepositoryTest.kt`

- [ ] **Step 1: Añadir tests RED de mapeo**

```kotlin
@Test fun `menu document round trip preserves shopping products`() {
    val restored = BackendFirestoreMapper.menuFromDocument("7", BackendFirestoreMapper.menuDocument(menu))
    assertEquals(menu.shoppingProducts, restored?.shoppingProducts)
}
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.backend.MenuDadoRemoteDataSourceTest' --tests 'com.menudado.data.MenuRepositorySyncTest'
```

Expected: FAIL porque Firestore no mapea productos.

- [ ] **Step 3: Ampliar mapeo de menú**

Persistir `shoppingProducts` con `key`, `normalizedName`, `displayName` e `isActive`; aceptar documentos antiguos sin ese campo.

- [ ] **Step 4: Añadir contratos remotos de mercado**

```kotlin
suspend fun fetchMarketProductStates(): List<MarketProductState>
suspend fun upsertMarketProductState(state: MarketProductState)
suspend fun deleteMarketProductState(state: MarketProductState)
```

Usar `users/{uid}/marketProducts/{productKey}`, timestamps de servidor, tokens y tombstones. No hidratar remoto sobre mutaciones locales pendientes.

- [ ] **Step 5: Ejecutar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.backend.MenuDadoRemoteDataSourceTest' --tests 'com.menudado.data.*'
```

Expected: BUILD SUCCESSFUL.

### Task 4: Estado y acciones de ViewModel

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Añadir tests RED**

```kotlin
@Test fun `generated menu defaults to add products`() { /* assert market opt-in true */ }
@Test fun `manual menu gets products only after analyze`() { /* assert empty then populated */ }
@Test fun `checking product moves it to purchased`() { /* repository action and state */ }
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuDadoViewModelTest'
```

Expected: FAIL por estado y acciones ausentes.

- [ ] **Step 3: Ampliar `MenuDadoUiState`**

```kotlin
val generatedShoppingProducts: List<ShoppingProduct> = emptyList()
val addGeneratedProductsToMarket: Boolean = true
val marketItems: List<MarketListItem> = emptyList()
val isMarketMutationInProgress: Boolean = false
```

- [ ] **Step 4: Implementar acciones**

Añadir toggle de guardado, activar/desactivar productos de menú, marcar/restaurar producto y limpiar comprados. Guardar menú y productos de forma coordinada. El análisis individual y por lote persiste `MenuAiDetails`.

- [ ] **Step 5: Ejecutar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuDadoViewModelTest'
```

Expected: BUILD SUCCESSFUL.

### Task 5: Navegación y UI Compose

**Files:**
- Create: `app/src/main/res/drawable/ic_nav_market.xml`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Añadir tests RED de navegación**

```kotlin
@Test fun `bottom navigation includes market as fourth destination`() {
    assertEquals(
        listOf(HOME, PROFILE, MARKET, MY_ZONE),
        menuDadoBottomNavigationDestinations(false)
    )
}
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest'
```

Expected: FAIL porque `MARKET` no existe.

- [ ] **Step 3: Añadir cuarto destino**

Crear `MenuDadoDestination.MARKET`, icono, etiqueta localizada, cabecera y owner de lista.

- [ ] **Step 4: Implementar componentes**

Crear `GeneratedShoppingProductsCard`, `MarketOptInRow`, `MarketScreen`, `MarketProductRow`, sección plegada de comprados, estado vacío y acciones por menú. Las filas no muestran cantidades y mantienen 48 dp.

- [ ] **Step 5: Ejecutar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest'
```

Expected: BUILD SUCCESSFUL.

### Task 6: Analítica, documentación y regresión

**Files:**
- Modify: `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`
- Modify: `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`
- Modify: `docs/project-context.md`
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Añadir eventos sin contenido**

Implementar apertura, alta/baja de menú, check/restauración, limpieza y fallo parcial. Los parámetros solo contienen conteos, booleanos, tipo y público; nunca nombres ni IDs.

- [ ] **Step 2: Actualizar contexto**

Documentar flujo IA, navegación, Room 11, Firestore y casos QA.

- [ ] **Step 3: Ejecutar validación final**

Run:

```bash
./gradlew :app:clean :app:testDebugUnitTest :app:lintRelease :app:bundleRelease
git diff --check
```

Expected: BUILD SUCCESSFUL, lint sin errores y diff sin whitespace inválido.

- [ ] **Step 4: Inspección manual guiada**

Validar en emulador o dispositivo: generación IA, checkbox activado, menú manual sin productos, `Analizar IA`, consolidación, comprado/restaurado/limpieza, eliminación de menú, reinicio sin red y posterior sincronización.
