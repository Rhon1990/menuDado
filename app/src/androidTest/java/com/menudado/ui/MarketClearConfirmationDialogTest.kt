package com.menudado.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.menudado.R
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
}

private fun string(resourceId: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
