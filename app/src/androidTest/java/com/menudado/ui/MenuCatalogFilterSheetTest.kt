package com.menudado.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.menudado.R
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MenuAudience
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuCatalogFilterSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun audienceScopeKeepsAudienceFixedAndShowsApplicableRootFilters() {
        composeRule.setContent {
            MaterialTheme {
                MenuCatalogFilterSheet(
                    scope = MenuCatalogScope.Audience(MenuAudience.ADULT),
                    filters = MenuCatalogFilters(),
                    dietaryProfiles = profiles(),
                    onFiltersChanged = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.menu_catalog_filter_audience))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.menu_catalog_filter_need)).assertExists()
        composeRule.onNodeWithText(string(R.string.menu_catalog_filter_favorites)).assertExists()
        composeRule.onNodeWithText(string(R.string.menu_catalog_filter_healthy)).assertExists()
    }

    @Test
    fun favoritesScopeCanSelectAudienceWithoutOfferingFavoriteAgain() {
        var changed: MenuCatalogFilters? = null
        composeRule.setContent {
            MaterialTheme {
                MenuCatalogFilterSheet(
                    scope = MenuCatalogScope.Favorites,
                    filters = MenuCatalogFilters(),
                    dietaryProfiles = profiles(),
                    onFiltersChanged = { changed = it },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.menu_catalog_filter_favorites))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.menu_catalog_filter_audience))
            .performClick()
        composeRule.onNodeWithText(string(R.string.menu_catalog_audience_all)).assertExists()
        composeRule.onNodeWithText(string(R.string.audience_child)).assertExists()
        composeRule.onNodeWithText(string(R.string.audience_baby)).assertExists()
        composeRule.onNodeWithText(string(R.string.audience_adult)).performClick()

        composeRule.runOnIdle {
            assertEquals(MenuAudience.ADULT, changed?.favoriteAudience)
        }
    }

    @Test
    fun unavailableChildNeedsAreVisibleButDisabled() {
        composeRule.setContent {
            MaterialTheme {
                MenuCatalogFilterSheet(
                    scope = MenuCatalogScope.Audience(MenuAudience.CHILD),
                    filters = MenuCatalogFilters(),
                    dietaryProfiles = profiles(),
                    onFiltersChanged = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.menu_catalog_filter_need)).performClick()
        composeRule.onNodeWithText(string(R.string.menu_catalog_need_pregnancy))
            .assert(hasNoClickAction())
        composeRule.onNodeWithText(string(R.string.menu_catalog_need_allergies))
            .assert(hasNoClickAction())
    }

    private fun profiles(): Map<MenuAudience, DietaryProfile> =
        MenuAudience.entries.associateWith { DietaryProfile(isEnabled = true) }
}

private fun string(resourceId: Int): String =
    InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getString(resourceId)
