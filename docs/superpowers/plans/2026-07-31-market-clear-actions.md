# Market Clear Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add safe, direct actions to clear all Market products or only purchased products without deleting saved menus.

**Architecture:** Keep dialog state in Compose, delegate confirmed mutations through `MenuDadoViewModel`, and perform clear-all as one Room transaction in `MarketDao`. Reuse the existing local-first pending-sync path so Firebase failures never roll back the immediate local result and require no schema changes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, ViewModel/StateFlow, Room, Firebase synchronization, JUnit 4, kotlinx-coroutines-test, Gradle

---

## Task 1: Add the atomic clear-all data operation

**Files:**
- Create: `app/src/test/java/com/menudado/data/MarketDaoClearTest.kt`
- Modify: `app/src/main/java/com/menudado/data/MarketDao.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuRepository.kt`

- [ ] **Step 1: Write failing transaction tests**

Create `MarketDaoClearTest.kt` with a recording `MarketDao` and these cases:

```kotlin
@Test
fun `clear all locally deactivates every contribution and removes purchased state`() = runTest {
    val dao = RecordingMarketDao(
        activeMenuIds = listOf(4L, 9L),
        activeProductCount = 3,
        purchasedStates = listOf(purchasedState("tomate"))
    )

    val clearedCount = dao.clearAllMarketProductsLocally(
        updatedAt = 500L,
        shouldSyncRemote = false,
        remoteSyncToken = null
    )

    assertEquals(4, clearedCount)
    assertEquals(1, dao.deactivateAllCalls)
    assertTrue(dao.productStates.isEmpty())
    assertTrue(dao.markedMenuBatches.isEmpty())
}

@Test
fun `clear all with remote keeps reset tombstones and marks affected menus pending`() = runTest {
    val dao = RecordingMarketDao(
        activeMenuIds = listOf(4L, 9L),
        activeProductCount = 2,
        purchasedStates = listOf(
            purchasedState("tomate"),
            purchasedState("arroz")
        )
    )

    val clearedCount = dao.clearAllMarketProductsLocally(
        updatedAt = 700L,
        shouldSyncRemote = true,
        remoteSyncToken = "clear-token"
    )

    assertEquals(4, clearedCount)
    assertEquals(listOf(listOf(4L, 9L) to 700L), dao.markedMenuBatches)
    assertTrue(dao.productStates.all { !it.isPurchased })
    assertTrue(dao.productStates.all { it.remoteSyncState == RemoteSyncState.PENDING_UPSERT.name })
    assertTrue(dao.productStates.all { it.remoteSyncToken == "clear-token" })
}

@Test
fun `clear all is an idempotent no-op when market is empty`() = runTest {
    val dao = RecordingMarketDao()

    val clearedCount = dao.clearAllMarketProductsLocally(
        updatedAt = 800L,
        shouldSyncRemote = true,
        remoteSyncToken = "unused-token"
    )

    assertEquals(0, clearedCount)
    assertEquals(1, dao.deactivateAllCalls)
    assertTrue(dao.markedMenuBatches.isEmpty())
}
```

Use this test fixture in the same file so the default DAO transaction is exercised directly:

```kotlin
private fun purchasedState(key: String) = MarketProductStateEntity(
    productKey = key,
    isPurchased = true,
    updatedAt = 100L
)

internal class RecordingMarketDao(
    private val activeMenuIds: List<Long> = emptyList(),
    private var activeProductCount: Int = 0,
    purchasedStates: List<MarketProductStateEntity> = emptyList(),
    var throwOnDeactivateAll: Boolean = false
) : MarketDao {
    private val menuProducts = MutableStateFlow<List<MenuShoppingProductEntity>>(emptyList())
    private val purchasedProducts =
        MutableStateFlow(purchasedStates.filter(MarketProductStateEntity::isPurchased))
    private val states = purchasedStates.associateByTo(linkedMapOf()) { it.productKey }

    var deactivateAllCalls = 0
    val markedMenuBatches = mutableListOf<Pair<List<Long>, Long>>()
    val productStates: List<MarketProductStateEntity>
        get() = states.values.toList()

    override suspend fun insertMenu(menu: MenuEntity): Long = menu.id
    override fun observeMenuProducts(): Flow<List<MenuShoppingProductEntity>> = menuProducts
    override suspend fun getMenuProducts(menuId: Long): List<MenuShoppingProductEntity> =
        menuProducts.value.filter { it.menuId == menuId }
    override fun observePurchasedProductStates(): Flow<List<MarketProductStateEntity>> =
        purchasedProducts
    override suspend fun getPurchasedProductStates(): List<MarketProductStateEntity> =
        states.values.filter(MarketProductStateEntity::isPurchased)
    override suspend fun getPendingProductStates(): List<MarketProductStateEntity> =
        states.values.filter { it.remoteSyncState != RemoteSyncState.SYNCED.name }
    override suspend fun getProductState(productKey: String): MarketProductStateEntity? =
        states[productKey]
    override suspend fun getActiveMenuIdsForProducts(productKeys: List<String>): List<Long> =
        activeMenuIds
    override suspend fun deactivateProducts(productKeys: List<String>): Int =
        minOf(activeProductCount, productKeys.size)
    override suspend fun getActiveMarketMenuIds(): List<Long> = activeMenuIds
    override suspend fun deactivateAllMarketProducts(): Int {
        deactivateAllCalls += 1
        if (throwOnDeactivateAll) error("market clear failed")
        return activeProductCount.also { activeProductCount = 0 }
    }
    override suspend fun markMenusPendingUpsert(menuIds: List<Long>, updatedAt: Long): Int {
        markedMenuBatches += menuIds to updatedAt
        return menuIds.size
    }
    override suspend fun deleteMenuProducts(menuId: Long) = Unit
    override suspend fun insertMenuProducts(products: List<MenuShoppingProductEntity>) = Unit
    override suspend fun deleteMenuById(menuId: Long) = Unit
    override suspend fun deletePendingMenuTombstone(menuId: Long, deletedAt: Long): Int = 0
    override suspend fun upsertProductState(state: MarketProductStateEntity) {
        states[state.productKey] = state
        publishPurchasedStates()
    }
    override suspend fun deleteProductStateNow(productKey: String) {
        states.remove(productKey)
        publishPurchasedStates()
    }
    override suspend fun deleteProductStateIfCurrent(
        productKey: String,
        remoteSyncToken: String
    ): Int {
        val current = states[productKey]
        if (current?.remoteSyncToken != remoteSyncToken) return 0
        states.remove(productKey)
        publishPurchasedStates()
        return 1
    }
    override suspend fun clearProductStates() {
        states.clear()
        publishPurchasedStates()
    }
    override suspend fun markPurchasedProductStatesPendingUpsert(
        updatedAt: Long,
        remoteSyncToken: String
    ): Int {
        val purchased = getPurchasedProductStates()
        purchased.forEach { state ->
            states[state.productKey] = state.copy(
                updatedAt = updatedAt,
                remoteSyncState = RemoteSyncState.PENDING_UPSERT.name,
                remoteSyncToken = remoteSyncToken
            )
        }
        publishPurchasedStates()
        return purchased.size
    }

    private fun publishPurchasedStates() {
        purchasedProducts.value = states.values.filter(MarketProductStateEntity::isPurchased)
    }
}
```

- [ ] **Step 2: Run the new tests and confirm RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.data.MarketDaoClearTest' \
  --no-daemon --console=plain
```

Expected: compilation fails because `clearAllMarketProductsLocally()`, `getActiveMarketMenuIds()`, and `deactivateAllMarketProducts()` do not exist.

- [ ] **Step 3: Implement the Room transaction**

Add these DAO operations:

```kotlin
@Query("SELECT DISTINCT menuId FROM menu_shopping_products WHERE isActive = 1")
suspend fun getActiveMarketMenuIds(): List<Long>

@Query("UPDATE menu_shopping_products SET isActive = 0 WHERE isActive = 1")
suspend fun deactivateAllMarketProducts(): Int
```

Add the transaction:

```kotlin
@Transaction
suspend fun clearAllMarketProductsLocally(
    updatedAt: Long,
    shouldSyncRemote: Boolean,
    remoteSyncToken: String?
): Int {
    val affectedMenuIds = getActiveMarketMenuIds()
    val deactivatedCount = deactivateAllMarketProducts()
    val purchasedStates = getPurchasedProductStates()
    if (shouldSyncRemote) {
        val token = requireNotNull(remoteSyncToken)
        purchasedStates.forEach { state ->
            upsertProductState(
                state.copy(
                    isPurchased = false,
                    updatedAt = updatedAt,
                    remoteSyncState = RemoteSyncState.PENDING_UPSERT.name,
                    remoteSyncToken = token
                )
            )
        }
        if (affectedMenuIds.isNotEmpty()) {
            markMenusPendingUpsert(affectedMenuIds, updatedAt)
        }
    } else {
        clearProductStates()
    }
    return deactivatedCount + purchasedStates.size
}
```

This updates only `isActive`; it never deletes `menu_shopping_products` or menu rows.

- [ ] **Step 4: Add the repository entry point**

Add:

```kotlin
suspend fun clearAllMarketProducts() {
    val dao = marketDao ?: return
    val updatedAt = maxOf(
        clockMillisProvider(),
        dao.getPurchasedProductStates()
            .maxOfOrNull(MarketProductStateEntity::updatedAt)
            ?.plus(1L)
            ?: Long.MIN_VALUE
    )
    val clearedCount = dao.clearAllMarketProductsLocally(
        updatedAt = updatedAt,
        shouldSyncRemote = remoteDataSource != null,
        remoteSyncToken = remoteDataSource?.let { syncTokenProvider() }
    )
    if (clearedCount > 0 && remoteDataSource != null) {
        syncPendingMenus()
    }
}
```

- [ ] **Step 5: Run the focused tests and confirm GREEN**

Run the Task 1 command again. Expected: PASS.

- [ ] **Step 6: Commit the data layer**

```bash
git add app/src/main/java/com/menudado/data/MarketDao.kt \
  app/src/main/java/com/menudado/data/MenuRepository.kt \
  app/src/test/java/com/menudado/data/MarketDaoClearTest.kt
git commit -m "feat: clear all market products atomically"
```

## Task 2: Expose safe ViewModel actions

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Add failing localization and behavior tests**

Add:

```kotlin
@Test
fun `market update failure message is localized`() {
    assertEquals(
        "No pudimos actualizar tu lista de Mercado. Inténtalo de nuevo.",
        AppLanguage.SPANISH.marketUpdateFailureMessage()
    )
    assertEquals(
        "We couldn't update your Market list. Try again.",
        AppLanguage.ENGLISH.marketUpdateFailureMessage()
    )
    assertEquals(
        "Nous n’avons pas pu mettre à jour votre liste de courses. Réessayez.",
        AppLanguage.FRENCH.marketUpdateFailureMessage()
    )
}
```

Import the Task 1 `RecordingMarketDao` and add ViewModel delegation and failure cases:

```kotlin
@Test
fun `clear all market products delegates once to repository data operation`() =
    runTest(dispatcher) {
        val marketDao = RecordingMarketDao(
            activeMenuIds = listOf(1L),
            activeProductCount = 2
        )
        val freshViewModel = MenuDadoViewModel(
            repository = MenuRepository(
                menuDao = dao,
                healthAnalyzer = analyzer,
                marketDao = marketDao
            )
        )

        freshViewModel.clearAllMarketProducts()
        advanceUntilIdle()

        assertEquals(1, marketDao.deactivateAllCalls)
    }

@Test
fun `clear all market failure exposes localized retry`() =
    runTest(dispatcher) {
        val marketDao = RecordingMarketDao(
            activeMenuIds = listOf(1L),
            activeProductCount = 1,
            throwOnDeactivateAll = true
        )
        val freshViewModel = MenuDadoViewModel(
            repository = MenuRepository(
                menuDao = dao,
                healthAnalyzer = analyzer,
                marketDao = marketDao
            )
        )

        freshViewModel.clearAllMarketProducts()
        advanceUntilIdle()

        assertEquals(
            "No pudimos actualizar tu lista de Mercado. Inténtalo de nuevo.",
            freshViewModel.uiState.value.message
        )
    }
```

- [ ] **Step 2: Run the focused ViewModel test and confirm RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuDadoViewModelTest.clear all market products delegates once to repository data operation' \
  --no-daemon --console=plain
```

Expected: compilation fails because `clearAllMarketProducts()` and `marketUpdateFailureMessage()` do not exist.

- [ ] **Step 3: Implement the ViewModel methods**

Add:

```kotlin
fun clearAllMarketProducts() {
    viewModelScope.launch {
        runCatching { repository.clearAllMarketProducts() }
            .onFailure {
                _uiState.update { state ->
                    state.copy(message = currentLanguage().marketUpdateFailureMessage())
                }
            }
    }
}
```

Wrap the existing purchased operation the same way:

```kotlin
fun clearPurchasedMarketProducts() {
    viewModelScope.launch {
        runCatching { repository.clearPurchasedMarketProducts() }
            .onFailure {
                _uiState.update { state ->
                    state.copy(message = currentLanguage().marketUpdateFailureMessage())
                }
            }
    }
}
```

Add:

```kotlin
internal fun AppLanguage.marketUpdateFailureMessage(): String {
    return when (this) {
        AppLanguage.SPANISH ->
            "No pudimos actualizar tu lista de Mercado. Inténtalo de nuevo."
        AppLanguage.ENGLISH ->
            "We couldn't update your Market list. Try again."
        AppLanguage.FRENCH ->
            "Nous n’avons pas pu mettre à jour votre liste de courses. Réessayez."
    }
}
```

- [ ] **Step 4: Run the focused test and complete ViewModel class**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuDadoViewModelTest' \
  --no-daemon --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit ViewModel behavior**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "feat: expose market clear actions"
```

## Task 3: Add the header actions and confirmation dialogs

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Add failing UI-contract tests**

Add:

```kotlin
@Test
fun `market clear actions are visible only when they can remove products`() {
    val pending = MarketProduct(
        key = "tomate",
        displayName = "Tomate",
        sourceMenuIds = setOf(1L),
        isPurchased = false
    )
    val purchased = MarketProduct(
        key = "arroz",
        displayName = "Arroz",
        sourceMenuIds = setOf(2L),
        isPurchased = true
    )

    assertFalse(shouldShowMarketClearAll(emptyList()))
    assertTrue(shouldShowMarketClearAll(listOf(pending)))
    assertFalse(shouldShowMarketClearPurchased(listOf(pending)))
    assertTrue(shouldShowMarketClearPurchased(listOf(purchased)))
}

@Test
fun `market clear dialogs use safe copy and accessible delete actions`() {
    assertEquals(R.drawable.ic_delete, marketClearActionIconRes())
    assertEquals(R.string.market_clear_all, marketClearAllLabelRes())
    assertEquals(R.string.market_clear_purchased, marketClearPurchasedContentDescriptionRes())
    assertEquals(R.string.market_clear_all_confirm_title, MarketClearAction.ALL.titleRes())
    assertEquals(R.string.market_clear_purchased_confirm_title, MarketClearAction.PURCHASED.titleRes())
    assertEquals(MenuDadoColors.Tomato, marketClearDestructiveColor())
    assertEquals(MenuDadoColors.BrandGreen, marketClearSafeActionColor())
    assertEquals(48, marketClearIconTouchTargetDp())
}
```

- [ ] **Step 2: Run the UI-contract tests and confirm RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuCardUiStateTest' \
  --no-daemon --console=plain
```

Expected: compilation fails because the Market clear UI contract does not exist.

- [ ] **Step 3: Add localized resources**

Add the following Spanish resources:

```xml
<string name="market_clear_all">Vaciar lista</string>
<string name="market_clear_all_confirm_title">¿Vaciar la lista?</string>
<string name="market_clear_all_confirm_body">Quitaremos todos los productos de Mercado. Tus menús seguirán guardados y podrás volver a incluirlos cuando quieras.</string>
<string name="market_clear_all_confirm_action">Sí, vaciar lista</string>
<string name="market_clear_purchased_confirm_title">¿Limpiar los comprados?</string>
<string name="market_clear_purchased_confirm_body">Los productos pendientes seguirán en tu lista.</string>
<string name="market_clear_purchased_confirm_action">Sí, limpiar</string>
<string name="market_keep_shopping">Seguir comprando</string>
```

Add equivalent English copy:

```xml
<string name="market_clear_all">Clear list</string>
<string name="market_clear_all_confirm_title">Clear the list?</string>
<string name="market_clear_all_confirm_body">We’ll remove every product from Market. Your menus will stay saved, and you can include them again whenever you want.</string>
<string name="market_clear_all_confirm_action">Yes, clear list</string>
<string name="market_clear_purchased_confirm_title">Clear purchased products?</string>
<string name="market_clear_purchased_confirm_body">Pending products will stay on your list.</string>
<string name="market_clear_purchased_confirm_action">Yes, clear</string>
<string name="market_keep_shopping">Keep shopping</string>
```

Add equivalent French copy:

```xml
<string name="market_clear_all">Vider la liste</string>
<string name="market_clear_all_confirm_title">Vider la liste ?</string>
<string name="market_clear_all_confirm_body">Nous retirerons tous les produits de la liste de courses. Vos menus resteront enregistrés et vous pourrez les ajouter à nouveau quand vous le souhaitez.</string>
<string name="market_clear_all_confirm_action">Oui, vider la liste</string>
<string name="market_clear_purchased_confirm_title">Effacer les produits achetés ?</string>
<string name="market_clear_purchased_confirm_body">Les produits restants seront conservés dans votre liste.</string>
<string name="market_clear_purchased_confirm_action">Oui, effacer</string>
<string name="market_keep_shopping">Continuer mes courses</string>
```

- [ ] **Step 4: Implement the UI contract and dialog**

Add:

```kotlin
internal enum class MarketClearAction {
    ALL,
    PURCHASED
}

internal fun shouldShowMarketClearAll(products: List<MarketProduct>): Boolean =
    products.isNotEmpty()

internal fun shouldShowMarketClearPurchased(products: List<MarketProduct>): Boolean =
    products.any(MarketProduct::isPurchased)

internal fun marketClearActionIconRes(): Int = R.drawable.ic_delete

@StringRes
internal fun marketClearAllLabelRes(): Int = R.string.market_clear_all

@StringRes
internal fun marketClearPurchasedContentDescriptionRes(): Int =
    R.string.market_clear_purchased

internal fun marketClearDestructiveColor(): Color = MenuDadoColors.Tomato

internal fun marketClearSafeActionColor(): Color = MenuDadoColors.BrandGreen

internal fun marketClearIconTouchTargetDp(): Int = 48

@StringRes
internal fun MarketClearAction.titleRes(): Int = when (this) {
    MarketClearAction.ALL -> R.string.market_clear_all_confirm_title
    MarketClearAction.PURCHASED -> R.string.market_clear_purchased_confirm_title
}

@StringRes
internal fun MarketClearAction.bodyRes(): Int = when (this) {
    MarketClearAction.ALL -> R.string.market_clear_all_confirm_body
    MarketClearAction.PURCHASED -> R.string.market_clear_purchased_confirm_body
}

@StringRes
internal fun MarketClearAction.confirmRes(): Int = when (this) {
    MarketClearAction.ALL -> R.string.market_clear_all_confirm_action
    MarketClearAction.PURCHASED -> R.string.market_clear_purchased_confirm_action
}
```

Create the dialog:

```kotlin
@Composable
private fun MarketClearConfirmationDialog(
    action: MarketClearAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = marketClearSafeActionColor()
                )
            ) {
                Text(
                    text = stringResource(id = R.string.market_keep_shopping),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = action.confirmRes()),
                    color = marketClearDestructiveColor(),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Text(
                text = stringResource(id = action.titleRes()),
                color = MenuDadoColors.Ink,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Text(
                text = stringResource(id = action.bodyRes()),
                color = MenuDadoColors.Ink
            )
        },
        containerColor = MenuDadoColors.Surface
    )
}
```

- [ ] **Step 5: Wire state, analytics, and positioning**

At `MenuDadoScreen` level, add:

```kotlin
var pendingMarketClearAction by rememberSaveable {
    mutableStateOf<MarketClearAction?>(null)
}
```

Add fixed CTA mappings:

```kotlin
internal fun MarketClearAction.openCta(): String = when (this) {
    MarketClearAction.ALL -> "open_market_clear_all_confirmation"
    MarketClearAction.PURCHASED -> "open_market_clear_purchased_confirmation"
}

internal fun MarketClearAction.confirmCta(): String = when (this) {
    MarketClearAction.ALL -> "confirm_market_clear_all"
    MarketClearAction.PURCHASED -> "market_purchased_cleared"
}

internal fun MarketClearAction.cancelCta(): String = when (this) {
    MarketClearAction.ALL -> "cancel_market_clear_all"
    MarketClearAction.PURCHASED -> "cancel_market_clear_purchased"
}
```

Pass two request callbacks to `MarketListSection`. Opening calls `trackCtaTapped(ANALYTICS_SCREEN_MARKET, action.openCta())` and stores the action. Render the pending confirmation as:

```kotlin
pendingMarketClearAction?.let { action ->
    MarketClearConfirmationDialog(
        action = action,
        onConfirm = {
            viewModel.trackCtaTapped(ANALYTICS_SCREEN_MARKET, action.confirmCta())
            pendingMarketClearAction = null
            when (action) {
                MarketClearAction.ALL -> viewModel.clearAllMarketProducts()
                MarketClearAction.PURCHASED -> viewModel.clearPurchasedMarketProducts()
            }
        },
        onDismiss = {
            viewModel.trackCtaTapped(ANALYTICS_SCREEN_MARKET, action.cancelCta())
            pendingMarketClearAction = null
        }
    )
}
```

Confirming closes the dialog before invoking the ViewModel, preventing repeated taps.

Change the Market title block to:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = stringResource(id = R.string.market_title),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = MenuDadoColors.DeepGreen
    )
    if (shouldShowMarketClearAll(products)) {
        TextButton(onClick = onClearAllRequested) {
            Icon(
                painter = painterResource(id = marketClearActionIconRes()),
                contentDescription = null,
                tint = marketClearDestructiveColor()
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(id = marketClearAllLabelRes()),
                color = marketClearDestructiveColor(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

Change the purchased title area to keep expand/collapse independent:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(MenuDadoUiTokens.ControlRadius))
            .clickable { showPurchased = !showPurchased }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.market_purchased),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MenuDadoColors.DeepGreen
        )
        IconButton(
            onClick = onClearPurchasedRequested,
            modifier = Modifier.size(marketClearIconTouchTargetDp().dp)
        ) {
            Icon(
                painter = painterResource(id = marketClearActionIconRes()),
                contentDescription = stringResource(
                    id = marketClearPurchasedContentDescriptionRes()
                ),
                tint = marketClearDestructiveColor()
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(
                id = if (showPurchased) R.drawable.ic_expand_less
                else R.drawable.ic_expand_more
            ),
            contentDescription = null,
            tint = MenuDadoColors.DeepGreen
        )
    }
}
```

Update the `MarketListSection` signature with `onClearAllRequested` and `onClearPurchasedRequested`, wire both from the destination call, and remove the bottom `Limpiar comprados` text button.

- [ ] **Step 6: Run UI and resource verification**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.ui.MenuCardUiStateTest' \
  --no-daemon --console=plain
./gradlew :app:processDebugResources --no-daemon --console=plain
```

Expected: both commands pass.

- [ ] **Step 7: Commit UI**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt \
  app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml
git commit -m "feat: add safe market clear controls"
```

## Task 4: Update project context and verify the integrated feature

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Document the final Market contract**

Add under `5. Lista de mercado`:

```markdown
- Mercado muestra `Vaciar lista` en la cabecera cuando existen productos. La acción requiere confirmación, elimina pendientes y comprados de la lista activa, pero conserva los menús y sus productos para poder volver a incluirlos después.
- `Comprados` mantiene su sección plegable y muestra una papelera accesible junto al título cuando contiene productos. `Limpiar comprados` requiere confirmación y no afecta a los productos pendientes.
```

- [ ] **Step 2: Run focused Market tests**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.data.MarketDaoClearTest' \
  --tests 'com.menudado.domain.MarketListAggregatorTest' \
  --tests 'com.menudado.ui.MenuCardUiStateTest' \
  --tests 'com.menudado.ui.MenuDadoViewModelTest' \
  --no-daemon --console=plain
```

Expected: PASS.

- [ ] **Step 3: Run the complete unit suite freshly**

Run:

```bash
./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest \
  --rerun-tasks --no-daemon --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Build the debug APK**

Run:

```bash
./gradlew :app:assembleDebug --rerun-tasks --no-daemon --console=plain
```

Expected: BUILD SUCCESSFUL and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 5: Check hygiene and commit documentation**

```bash
git diff --check
git status --short
git add docs/project-context.md
git commit -m "docs: describe market clear controls"
```

- [ ] **Step 6: Manual QA when a device is available**

Verify:

1. Pending-only list: `Vaciar lista` appears; purchased trash does not.
2. Purchased-only list: both actions appear; purchased trash works while collapsed.
3. Mixed list: clearing purchased preserves pending; clearing all empties the list.
4. Cancelling or dismissing either dialog changes nothing.
5. Re-including a saved menu restores its products as pending.
6. Menus, favorites, analysis, and photos remain intact.
7. Spanish, English, and French copy fits with large font.
8. TalkBack announces both trash actions distinctly.
