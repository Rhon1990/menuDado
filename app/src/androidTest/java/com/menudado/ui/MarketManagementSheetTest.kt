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
                    onActionSelected = { action: MarketClearAction ->
                        selected.add(action)
                    },
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
