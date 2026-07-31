# Market Management Sheet Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the visually dominant Market delete controls with a three-dot entry point and a reusable green `Gestionar lista` action sheet, while preserving the existing confirmed clear operations.

**Architecture:** Keep Room, repository, ViewModel, and `MarketClearAction` behavior unchanged. Extract the existing green menu-action sheet shell into a reusable composable, build a Market-specific sheet on top of it, and let `MenuDadoScreen` coordinate sheet visibility, modal priority, analytics, and the already-tested confirmation flow.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android resources, JUnit 4, Compose UI instrumentation tests.

---

## File map

- Create `app/src/main/java/com/menudado/ui/MenuDadoActionSheet.kt`: reusable green bottom-sheet shell, close control, and action row.
- Create `app/src/main/java/com/menudado/ui/MarketManagementSheet.kt`: Market action availability, resources, and `Gestionar lista` sheet/host.
- Create `app/src/main/res/drawable/ic_check.xml`: reusable white-tinted check vector for `Limpiar comprados`.
- Create `app/src/androidTest/java/com/menudado/ui/MarketManagementSheetTest.kt`: real Compose behavior for the new sheet.
- Modify `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: screen state, modal priority, header overflow, purchased container, and reuse of the generic sheet.
- Modify `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: pure presentation contracts.
- Modify `app/src/androidTest/java/com/menudado/ui/MarketClearConfirmationDialogTest.kt`: replace the obsolete trash interaction with the new header/purchased behavior.
- Modify `app/src/main/res/values/strings.xml`: Spanish management copy.
- Modify `app/src/main/res/values-en/strings.xml`: English management copy.
- Modify `app/src/main/res/values-fr/strings.xml`: French management copy.
- Modify `docs/project-context.md`: document the new Market interaction.

### Task 1: Define the Market management presentation contract

**Files:**
- Create: `app/src/main/java/com/menudado/ui/MarketManagementSheet.kt`
- Create: `app/src/main/res/drawable/ic_check.xml`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Write failing unit tests for action availability and visual resources**

Add to `MenuCardUiStateTest`:

```kotlin
@Test
fun `market management exposes only actions that can change the list`() {
    assertEquals(
        listOf(MarketClearAction.ALL),
        marketManagementActions(hasPurchasedProducts = false)
    )
    assertEquals(
        listOf(MarketClearAction.PURCHASED, MarketClearAction.ALL),
        marketManagementActions(hasPurchasedProducts = true)
    )
}

@Test
fun `market management reuses MenuDado action sheet styling`() {
    assertEquals(R.drawable.ic_more_vertical, marketManagementIconRes())
    assertEquals(R.string.market_manage_list, marketManagementContentDescriptionRes())
    assertEquals(48, marketManagementTouchTargetDp())
    assertEquals(MenuDadoColors.HeaderGreen, marketManagementContainerColor())
    assertEquals(Color.White, marketManagementContentColor())
    assertEquals(MenuDadoColors.SoftSand.copy(alpha = 0.58f), marketPurchasedContainerColor())
    assertEquals("open_market_management", marketManagementOpenCta())
    assertEquals(R.drawable.ic_check, MarketClearAction.PURCHASED.managementIconRes())
    assertEquals(R.drawable.ic_delete, MarketClearAction.ALL.managementIconRes())
    assertEquals(
        R.string.market_manage_clear_purchased_description,
        MarketClearAction.PURCHASED.managementDescriptionRes()
    )
    assertEquals(
        R.string.market_manage_clear_all_label,
        MarketClearAction.ALL.managementLabelRes()
    )
}
```

- [ ] **Step 2: Run the tests and verify the expected RED state**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuCardUiStateTest \
  --no-daemon --console=plain
```

Expected: compilation fails because the management helpers and `ic_check` do not exist.

- [ ] **Step 3: Add localized resources**

Add Spanish:

```xml
<string name="market_manage_list">Gestionar lista</string>
<string name="market_manage_clear_purchased_description">Mantiene los productos que todavía tienes pendientes.</string>
<string name="market_manage_clear_all_label">Vaciar toda la lista</string>
<string name="market_manage_clear_all_description">Retira todos los productos sin borrar tus menús.</string>
```

Add English:

```xml
<string name="market_manage_list">Manage list</string>
<string name="market_manage_clear_purchased_description">Keeps the products you still have pending.</string>
<string name="market_manage_clear_all_label">Clear the entire list</string>
<string name="market_manage_clear_all_description">Removes every product without deleting your menus.</string>
```

Add French:

```xml
<string name="market_manage_list">Gérer la liste</string>
<string name="market_manage_clear_purchased_description">Conserve les produits qu’il vous reste à acheter.</string>
<string name="market_manage_clear_all_label">Vider toute la liste</string>
<string name="market_manage_clear_all_description">Retire tous les produits sans supprimer vos menus.</string>
```

- [ ] **Step 4: Add the reusable check vector**

Create `ic_check.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z" />
</vector>
```

- [ ] **Step 5: Implement the pure presentation helpers**

Start `MarketManagementSheet.kt` with:

```kotlin
package com.menudado.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.menudado.R
import com.menudado.ui.theme.MenuDadoColors

internal fun marketManagementActions(
    hasPurchasedProducts: Boolean
): List<MarketClearAction> = buildList {
    if (hasPurchasedProducts) add(MarketClearAction.PURCHASED)
    add(MarketClearAction.ALL)
}

internal fun marketManagementIconRes(): Int = R.drawable.ic_more_vertical

@StringRes
internal fun marketManagementContentDescriptionRes(): Int =
    R.string.market_manage_list

internal fun marketManagementTouchTargetDp(): Int = 48

internal fun marketManagementContainerColor(): Color =
    MenuDadoColors.HeaderGreen

internal fun marketManagementContentColor(): Color = Color.White

internal fun marketPurchasedContainerColor(): Color =
    MenuDadoColors.SoftSand.copy(alpha = 0.58f)

internal fun marketManagementOpenCta(): String = "open_market_management"

@DrawableRes
internal fun MarketClearAction.managementIconRes(): Int = when (this) {
    MarketClearAction.PURCHASED -> R.drawable.ic_check
    MarketClearAction.ALL -> R.drawable.ic_delete
}

@StringRes
internal fun MarketClearAction.managementLabelRes(): Int = when (this) {
    MarketClearAction.PURCHASED -> R.string.market_clear_purchased
    MarketClearAction.ALL -> R.string.market_manage_clear_all_label
}

@StringRes
internal fun MarketClearAction.managementDescriptionRes(): Int = when (this) {
    MarketClearAction.PURCHASED ->
        R.string.market_manage_clear_purchased_description
    MarketClearAction.ALL ->
        R.string.market_manage_clear_all_description
}
```

- [ ] **Step 6: Run the focused unit and resource checks**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuCardUiStateTest \
  :app:processDebugResources \
  --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit the contract**

```bash
git add app/src/main/java/com/menudado/ui/MarketManagementSheet.kt \
  app/src/main/res/drawable/ic_check.xml \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml \
  app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: define market management presentation"
```

### Task 2: Extract the reusable green action-sheet shell

**Files:**
- Create: `app/src/main/java/com/menudado/ui/MenuDadoActionSheet.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Add a failing contract assertion for the shared sheet**

Add:

```kotlin
@Test
fun `shared action sheet keeps the established MenuDado tokens`() {
    assertEquals(28, menuDadoActionSheetTopRadiusDp())
    assertEquals(86, menuDadoActionSheetHandleWidthDp())
    assertEquals(5, menuDadoActionSheetHandleHeightDp())
    assertEquals(42, menuDadoActionSheetIconContainerDp())
    assertEquals(48, menuSheetCloseActionButtonSizeDp())
}
```

- [ ] **Step 2: Run the test and verify RED**

Run the same focused `MenuCardUiStateTest` command.

Expected: unresolved shared sheet token helpers.

- [ ] **Step 3: Create the generic sheet composables**

Create `MenuDadoActionSheet.kt` with the shared implementation:

```kotlin
internal fun menuDadoActionSheetTopRadiusDp(): Int = 28
internal fun menuDadoActionSheetHandleWidthDp(): Int = 86
internal fun menuDadoActionSheetHandleHeightDp(): Int = 5
internal fun menuDadoActionSheetIconContainerDp(): Int = 42

@Composable
internal fun MenuDadoActionSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {}),
                colors = CardDefaults.cardColors(
                    containerColor = menuActionSheetContainerColor()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                shape = RoundedCornerShape(
                    topStart = menuDadoActionSheetTopRadiusDp().dp,
                    topEnd = menuDadoActionSheetTopRadiusDp().dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(menuSheetCloseActionButtonSizeDp().dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(menuDadoActionSheetHandleWidthDp().dp)
                                .height(menuDadoActionSheetHandleHeightDp().dp)
                                .clip(
                                    RoundedCornerShape(
                                        MenuDadoUiTokens.ControlRadius
                                    )
                                )
                                .background(
                                    menuActionSheetContentColor().copy(alpha = 0.42f)
                                )
                        )
                        MenuDadoSheetCloseButton(
                            onDismiss = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                    Text(
                        text = title,
                        color = menuActionSheetContentColor(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                menuActionSheetContentColor().copy(alpha = 0.22f)
                            )
                    )
                    content()
                }
            }
        }
    }
}

@Composable
internal fun MenuDadoActionSheetRow(
    iconRes: Int,
    title: String,
    description: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(MenuDadoUiTokens.ControlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(menuDadoActionSheetIconContainerDp().dp)
                .clip(CircleShape)
                .background(menuActionSheetContentColor().copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(23.dp),
                colorFilter = ColorFilter.tint(menuActionSheetContentColor())
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = menuActionSheetContentColor(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            description?.let {
                Text(
                    text = it,
                    color = menuActionSheetContentColor().copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun MenuDadoSheetCloseButton(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onDismiss,
        modifier = modifier.size(menuSheetCloseActionButtonSizeDp().dp)
    ) {
        Icon(
            painter = painterResource(id = menuSheetCloseActionIconRes()),
            contentDescription = stringResource(
                id = menuSheetCloseActionContentDescriptionRes()
            ),
            modifier = Modifier.size(22.dp),
            tint = menuSheetCloseActionIconTint()
        )
    }
}
```

Include the Compose imports used by this code from `MenuDadoScreen.kt`, plus `ColumnScope` and `DialogProperties`.

- [ ] **Step 4: Refactor `MenuActionsSheet` to use the generic shell**

Replace its internal `Dialog` body with:

```kotlin
MenuDadoActionSheet(
    title = menu.name,
    onDismiss = onDismiss
) {
    menuActionSheetActions().forEach { action ->
        MenuDadoActionSheetRow(
            iconRes = menuActionSheetActionIconRes(action),
            title = stringResource(id = menuActionSheetActionLabelRes(action)),
            onClick = callbacks.getValue(action)
        )
    }
}
```

Remove the old private `MenuSheetCloseButton` and `MenuActionSheetRow`. Preserve existing helper functions and semantics so the current menu sheet remains visually unchanged.

- [ ] **Step 5: Run focused unit tests and compile**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuCardUiStateTest \
  :app:compileDebugKotlin \
  --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the extraction**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoActionSheet.kt \
  app/src/main/java/com/menudado/ui/MenuDadoScreen.kt \
  app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "refactor: share MenuDado action sheet"
```

### Task 3: Build and verify `Gestionar lista`

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MarketManagementSheet.kt`
- Create: `app/src/androidTest/java/com/menudado/ui/MarketManagementSheetTest.kt`

- [ ] **Step 1: Write failing Compose tests**

Create `MarketManagementSheetTest.kt` with:

```kotlin
package com.menudado.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.menudado.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarketManagementSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun purchasedMarketShowsBothManagementActions() {
        composeRule.setContent {
            MaterialTheme {
                MarketManagementSheet(
                    hasPurchasedProducts = true,
                    onActionSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(
            string(R.string.market_clear_purchased)
        ).assertExists()
        composeRule.onNodeWithText(
            string(R.string.market_manage_clear_all_label)
        ).assertExists()
    }

    @Test
    fun pendingOnlyMarketHidesClearPurchased() {
        composeRule.setContent {
            MaterialTheme {
                MarketManagementSheet(
                    hasPurchasedProducts = false,
                    onActionSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(
            string(R.string.market_clear_purchased)
        ).assertDoesNotExist()
        composeRule.onNodeWithText(
            string(R.string.market_manage_clear_all_label)
        ).assertExists()
    }

    @Test
    fun selectingAnActionReturnsIntentWithoutClearingData() {
        val selected = mutableListOf<MarketClearAction>()
        composeRule.setContent {
            MaterialTheme {
                MarketManagementSheet(
                    hasPurchasedProducts = true,
                    onActionSelected = selected::add,
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(
            string(R.string.market_clear_purchased)
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(MarketClearAction.PURCHASED), selected)
        }
    }

    @Test
    fun backDismissesManagementSheet() {
        var dismissCount = 0
        composeRule.setContent {
            MaterialTheme {
                MarketManagementSheet(
                    hasPurchasedProducts = false,
                    onActionSelected = {},
                    onDismiss = { dismissCount += 1 }
                )
            }
        }

        pressBack()
        composeRule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    @Test
    fun managementHostWaitsForHigherPriorityModal() {
        val hasHigherPriorityModal = mutableStateOf(true)
        composeRule.setContent {
            MaterialTheme {
                MarketManagementSheetHost(
                    isRequested = true,
                    isAnotherModalVisible = hasHigherPriorityModal.value,
                    hasPurchasedProducts = true,
                    onActionSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(
            string(R.string.market_manage_list)
        ).assertDoesNotExist()
        composeRule.runOnIdle {
            hasHigherPriorityModal.value = false
        }
        composeRule.onNodeWithText(
            string(R.string.market_manage_list)
        ).assertExists()
    }
}

private fun string(resourceId: Int): String =
    InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getString(resourceId)
```

- [ ] **Step 2: Run the instrumentation test and verify RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.ui.MarketManagementSheetTest \
  --no-daemon --console=plain
```

Expected: Android-test compilation fails because the sheet and host do not exist.

- [ ] **Step 3: Implement the Market sheet**

Add:

```kotlin
@Composable
internal fun MarketManagementSheet(
    hasPurchasedProducts: Boolean,
    onActionSelected: (MarketClearAction) -> Unit,
    onDismiss: () -> Unit
) {
    MenuDadoActionSheet(
        title = stringResource(id = R.string.market_manage_list),
        onDismiss = onDismiss
    ) {
        marketManagementActions(hasPurchasedProducts).forEach { action ->
            MenuDadoActionSheetRow(
                iconRes = action.managementIconRes(),
                title = stringResource(id = action.managementLabelRes()),
                description = stringResource(id = action.managementDescriptionRes()),
                onClick = { onActionSelected(action) }
            )
        }
    }
}

@Composable
internal fun MarketManagementSheetHost(
    isRequested: Boolean,
    isAnotherModalVisible: Boolean,
    hasPurchasedProducts: Boolean,
    onActionSelected: (MarketClearAction) -> Unit,
    onDismiss: () -> Unit
) {
    if (isRequested && !isAnotherModalVisible) {
        MarketManagementSheet(
            hasPurchasedProducts = hasPurchasedProducts,
            onActionSelected = onActionSelected,
            onDismiss = onDismiss
        )
    }
}
```

- [ ] **Step 4: Run the new Compose suite**

Run the Task 3 instrumentation command.

Expected: five tests pass, zero failures.

- [ ] **Step 5: Commit the new sheet**

```bash
git add app/src/main/java/com/menudado/ui/MarketManagementSheet.kt \
  app/src/androidTest/java/com/menudado/ui/MarketManagementSheetTest.kt
git commit -m "feat: add market management sheet"
```

### Task 4: Wire the screen and simplify the Market layout

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/androidTest/java/com/menudado/ui/MarketClearConfirmationDialogTest.kt`

- [ ] **Step 1: Replace the obsolete trash test with failing interaction tests**

Replace `purchasedTrashDoesNotExpandPurchasedProducts` with:

```kotlin
@Test
fun marketHeaderOffersManagementOnlyWhenProductsExist() {
    val products = mutableStateOf(emptyList<MarketProduct>())
    composeRule.setContent {
        MaterialTheme {
            MarketListSection(
                products = products.value,
                onPurchasedChanged = { _, _ -> },
                onManageRequested = {}
            )
        }
    }

    composeRule.onNodeWithContentDescription(
        string(R.string.market_manage_list)
    ).assertDoesNotExist()
    composeRule.runOnIdle {
        products.value = listOf(
            MarketProduct(
                key = "arroz",
                displayName = "Arroz",
                sourceMenuIds = setOf(1L),
                isPurchased = false
            )
        )
    }
    composeRule.onNodeWithContentDescription(
        string(R.string.market_manage_list)
    ).assertExists()
}

@Test
fun purchasedHeaderExpandsWithoutRequestingManagement() {
    var manageRequests = 0
    composeRule.setContent {
        MaterialTheme {
            MarketListSection(
                products = listOf(
                    MarketProduct(
                        key = "arroz",
                        displayName = "Arroz especial",
                        sourceMenuIds = setOf(1L),
                        isPurchased = true
                    )
                ),
                onPurchasedChanged = { _, _ -> },
                onManageRequested = { manageRequests += 1 }
            )
        }
    }

    composeRule.onNodeWithText("Arroz especial").assertDoesNotExist()
    composeRule.onNodeWithText(
        string(R.string.market_purchased)
    ).performClick()
    composeRule.onNodeWithText("Arroz especial").assertExists()
    composeRule.runOnIdle {
        assertEquals(0, manageRequests)
    }
}
```

- [ ] **Step 2: Run the affected instrumentation tests and verify RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.ui.MarketClearConfirmationDialogTest,com.menudado.ui.MarketManagementSheetTest \
  --no-daemon --console=plain
```

Expected: compilation fails because `MarketListSection` still exposes the two legacy clear callbacks.

- [ ] **Step 3: Change `MarketListSection` to a single management callback**

Use:

```kotlin
internal fun MarketListSection(
    products: List<MarketProduct>,
    onPurchasedChanged: (String, Boolean) -> Unit,
    onManageRequested: () -> Unit
)
```

Replace the header `TextButton` with:

```kotlin
if (products.isNotEmpty()) {
    IconButton(
        onClick = onManageRequested,
        modifier = Modifier.size(marketManagementTouchTargetDp().dp)
    ) {
        Icon(
            painter = painterResource(id = marketManagementIconRes()),
            contentDescription = stringResource(
                id = marketManagementContentDescriptionRes()
            ),
            tint = MenuDadoColors.DeepGreen
        )
    }
}
```

Replace the purchased nested row/trash with a single clickable `Card` using `marketPurchasedContainerColor()`, the `Comprados` label, `purchasedProducts.size`, and the existing expand icon.

- [ ] **Step 4: Add screen state and modal priority**

Add:

```kotlin
var isMarketManagementRequested by rememberSaveable {
    mutableStateOf(false)
}
```

Rename the current boolean to `isHigherPriorityModalVisible` and keep its existing conditions. Show the management host only when requested and no higher-priority modal is visible. Pass

```kotlin
isAnotherModalVisible =
    isHigherPriorityModalVisible || isMarketManagementRequested
```

to `MarketClearConfirmationHost` so a confirmation cannot overlap the management sheet.

Include `isMarketManagementRequested` in `BackHandler`; after higher-priority messages and before unrelated menu/photo sheets, Back sets it to false.

- [ ] **Step 5: Wire analytics and action sequencing**

The header callback must:

```kotlin
viewModel.trackCtaTapped(
    ANALYTICS_SCREEN_MARKET,
    marketManagementOpenCta()
)
isMarketManagementRequested = true
```

The sheet action callback must close the sheet before setting the pending confirmation:

```kotlin
onActionSelected = { action ->
    viewModel.trackCtaTapped(
        ANALYTICS_SCREEN_MARKET,
        action.openCta()
    )
    isMarketManagementRequested = false
    pendingMarketClearAction = action
}
```

Keep confirmation analytics and calls to the two existing ViewModel methods unchanged.

- [ ] **Step 6: Run the focused interaction tests**

Run the Task 4 instrumentation command.

Expected: all Market management and confirmation tests pass.

- [ ] **Step 7: Run focused unit tests**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuCardUiStateTest \
  --tests com.menudado.ui.MenuDadoViewModelTest \
  --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit the integrated UI**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt \
  app/src/androidTest/java/com/menudado/ui/MarketClearConfirmationDialogTest.kt
git commit -m "feat: redesign market management controls"
```

### Task 5: Document and verify the integrated result

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Update the functional context**

Replace the old direct-button/paper-bin bullets with:

```markdown
- Mercado muestra un botón de tres puntos cuando existen productos. Abre la hoja verde `Gestionar lista`, que agrupa las acciones sin ejecutarlas directamente.
- `Limpiar comprados` aparece en la hoja solo si existen comprados; `Vaciar toda la lista` aparece siempre que haya productos. Cada opción cierra la hoja y abre su confirmación específica.
- `Comprados` es una cabecera cálida con contador y expansión, sin acciones destructivas anidadas.
```

Keep the tombstone, modal-priority, Firebase, and privacy guarantees already documented.

- [ ] **Step 2: Run source and resource checks**

```bash
git diff --check
./gradlew :app:processDebugResources :app:compileDebugKotlin \
  --no-daemon --console=plain
```

Expected: no diff errors and `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run a fresh full unit suite and debug build**

```bash
./gradlew :app:cleanTestDebugUnitTest \
  :app:testDebugUnitTest \
  :app:assembleDebug \
  --rerun-tasks --no-daemon --console=plain
```

Expected: all unit tests pass with zero failures and `app-debug.apk` is generated.

- [ ] **Step 4: Run the destructive-flow instrumentation suite**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.menudado.data.MarketDaoClearInstrumentedTest,com.menudado.ui.MarketClearConfirmationDialogTest,com.menudado.ui.MarketManagementSheetTest \
  --no-daemon --console=plain
```

Expected: all selected tests pass on the connected device.

- [ ] **Step 5: Review the final diff**

Verify:

```bash
git status --short
git diff --check
git diff --stat dc87026
```

Expected: only the approved Market UI, resources, tests, and documentation are changed.

- [ ] **Step 6: Commit documentation**

```bash
git add docs/project-context.md
git commit -m "docs: describe market management sheet"
```
