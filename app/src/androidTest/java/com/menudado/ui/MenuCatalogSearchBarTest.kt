package com.menudado.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.menudado.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuCatalogSearchBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchCanBeClearedAndActiveFilterButtonCanBeOpened() {
        var query by mutableStateOf("")
        var clearClicks = 0
        var filterClicks = 0
        composeRule.setContent {
            MaterialTheme {
                MenuCatalogSearchBar(
                    query = query,
                    onQueryChanged = { query = it },
                    onClearSearch = {
                        clearClicks += 1
                        query = ""
                    },
                    activeFilterCount = 2,
                    onOpenFilters = { filterClicks += 1 }
                )
            }
        }

        composeRule.onNodeWithTag(MENU_CATALOG_SEARCH_FIELD_TEST_TAG)
            .performTextInput("arroz")
        composeRule.onNodeWithContentDescription(
            string(R.string.menu_catalog_clear_search)
        ).performClick()
        composeRule.onNodeWithContentDescription(
            string(R.string.menu_catalog_open_filters_active, 2)
        ).performClick()

        composeRule.runOnIdle {
            assertEquals("", query)
            assertEquals(1, clearClicks)
            assertEquals(1, filterClicks)
        }
    }

    @Test
    fun zeroActiveFiltersDoesNotShowBadge() {
        composeRule.setContent {
            MaterialTheme {
                MenuCatalogSearchBar(
                    query = "",
                    onQueryChanged = {},
                    onClearSearch = {},
                    activeFilterCount = 0,
                    onOpenFilters = {}
                )
            }
        }

        composeRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun activeFiltersShowCountBadge() {
        composeRule.setContent {
            MaterialTheme {
                MenuCatalogSearchBar(
                    query = "",
                    onQueryChanged = {},
                    onClearSearch = {},
                    activeFilterCount = 2,
                    onOpenFilters = {}
                )
            }
        }

        composeRule.onNodeWithText("2").assertExists()
    }
}

private fun string(resourceId: Int, vararg formatArgs: Any): String =
    InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getString(resourceId, *formatArgs)
