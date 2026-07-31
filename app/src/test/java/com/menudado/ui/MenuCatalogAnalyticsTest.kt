package com.menudado.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuCatalogAnalyticsTest {

    @Test
    fun `catalog analytics uses closed screen and CTA identifiers`() {
        assertEquals("menu_catalog", MENU_CATALOG_ANALYTICS_SCREEN)
        assertEquals("search_started", MenuCatalogAnalyticsAction.SEARCH_STARTED.cta)
        assertEquals("clear_search", MenuCatalogAnalyticsAction.CLEAR_SEARCH.cta)
        assertEquals("open_filters", MenuCatalogAnalyticsAction.OPEN_FILTERS.cta)
        assertEquals(
            "clear_search_and_filters",
            MenuCatalogAnalyticsAction.CLEAR_SEARCH_AND_FILTERS.cta,
        )
    }

    @Test
    fun `filter interactions map to their closed analytics actions`() {
        assertEquals(
            "select_audience_filter",
            MenuCatalogFilterInteraction.AUDIENCE_SELECTED.analyticsAction.cta,
        )
        assertEquals(
            "select_dietary_filter",
            MenuCatalogFilterInteraction.DIETARY_NEED_SELECTED.analyticsAction.cta,
        )
        assertEquals(
            "toggle_favorites_only",
            MenuCatalogFilterInteraction.FAVORITES_ONLY_TOGGLED.analyticsAction.cta,
        )
        assertEquals(
            "toggle_healthy_only",
            MenuCatalogFilterInteraction.HEALTHY_ONLY_TOGGLED.analyticsAction.cta,
        )
    }

    @Test
    fun `search transition reports only blank to nonblank queries`() {
        assertEquals(
            MenuCatalogAnalyticsAction.SEARCH_STARTED.cta,
            menuCatalogSearchTransitionCta(previousQuery = "", newQuery = "ensalada"),
        )
        assertEquals(
            MenuCatalogAnalyticsAction.SEARCH_STARTED.cta,
            menuCatalogSearchTransitionCta(previousQuery = "   ", newQuery = "tomate"),
        )
        assertNull(menuCatalogSearchTransitionCta(previousQuery = "sopa", newQuery = "ensalada"))
        assertNull(menuCatalogSearchTransitionCta(previousQuery = "sopa", newQuery = ""))
        assertNull(menuCatalogSearchTransitionCta(previousQuery = "", newQuery = ""))
        assertNull(menuCatalogSearchTransitionCta(previousQuery = "", newQuery = "   "))
    }
}
