package com.menudado.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.menudado.R
import com.menudado.domain.MarketProduct
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarketClearConfirmationDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun safeActionDismissesWithoutConfirming() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            MaterialTheme {
                MarketClearConfirmationDialog(
                    action = MarketClearAction.ALL,
                    onConfirm = { confirmed += 1 },
                    onDismiss = { dismissed += 1 }
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.market_keep_shopping)).performClick()

        composeRule.runOnIdle {
            assertEquals(0, confirmed)
            assertEquals(1, dismissed)
        }
    }

    @Test
    fun destructiveActionConfirmsWithoutDismissing() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            MaterialTheme {
                MarketClearConfirmationDialog(
                    action = MarketClearAction.ALL,
                    onConfirm = { confirmed += 1 },
                    onDismiss = { dismissed += 1 }
                )
            }
        }

        composeRule.onNodeWithText(
            string(R.string.market_clear_all_confirm_action)
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(1, confirmed)
            assertEquals(0, dismissed)
        }
    }

    @Test
    fun purchasedVariantConfirmsAndBackDismisses() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            MaterialTheme {
                MarketClearConfirmationDialog(
                    action = MarketClearAction.PURCHASED,
                    onConfirm = { confirmed += 1 },
                    onDismiss = { dismissed += 1 }
                )
            }
        }

        composeRule.onNodeWithText(
            string(R.string.market_clear_purchased_confirm_action)
        ).performClick()
        pressBack()

        composeRule.runOnIdle {
            assertEquals(1, confirmed)
            assertEquals(1, dismissed)
        }
    }

    @Test
    fun hostWaitsUntilHigherPriorityModalIsGone() {
        val isAnotherModalVisible = mutableStateOf(true)
        composeRule.setContent {
            MaterialTheme {
                MarketClearConfirmationHost(
                    action = MarketClearAction.ALL,
                    isAnotherModalVisible = isAnotherModalVisible.value,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(
            string(R.string.market_clear_all_confirm_title)
        ).assertDoesNotExist()
        composeRule.runOnIdle {
            isAnotherModalVisible.value = false
        }
        composeRule.onNodeWithText(
            string(R.string.market_clear_all_confirm_title)
        ).assertExists()
    }

    @Test
    fun marketHeaderOffersManagementOnlyWhenProductsExist() {
        val products = mutableStateOf(emptyList<MarketProduct>())
        composeRule.setContent {
            MaterialTheme {
                MarketListSection(
                    products = products.value,
                    onPurchasedChanged = { _, _ -> },
                    onPurchasedVisibilityChanged = {},
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
        val purchasedVisibilityChanges = mutableListOf<Boolean>()
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
                    onPurchasedVisibilityChanged = purchasedVisibilityChanges::add,
                    onManageRequested = { manageRequests += 1 }
                )
            }
        }

        composeRule.onNodeWithText("Arroz especial").assertDoesNotExist()
        composeRule.onNodeWithText(
            string(R.string.market_purchased)
        ).performClick()
        composeRule.onNodeWithText("Arroz especial").assertExists()
        composeRule.onNodeWithText(
            string(R.string.market_purchased)
        ).performClick()
        composeRule.runOnIdle {
            assertEquals(0, manageRequests)
            assertEquals(listOf(true, false), purchasedVisibilityChanges)
        }
    }
}

private fun string(resourceId: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
