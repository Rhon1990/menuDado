package com.menudado.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.menudado.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuDadoActionSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nestedSheetExposesBackAndClose() {
        var backs = 0
        var dismissals = 0
        composeRule.setContent {
            MaterialTheme {
                MenuDadoActionSheet(
                    title = "Para quién",
                    onBack = { backs += 1 },
                    onDismiss = { dismissals += 1 }
                ) {
                    MenuDadoActionSheetRow(
                        iconRes = R.drawable.ic_nav_profile,
                        title = "Adulto",
                        onClick = {}
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(string(R.string.common_back))
            .performClick()
        composeRule.onNodeWithContentDescription(string(R.string.common_close))
            .performClick()
        composeRule.runOnIdle {
            assertEquals(1, backs)
            assertEquals(1, dismissals)
        }
    }
}

private fun string(resourceId: Int): String =
    InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getString(resourceId)
