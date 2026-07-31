package com.menudado.ui

internal const val MENU_CATALOG_ANALYTICS_SCREEN = "menu_catalog"

internal enum class MenuCatalogAnalyticsAction(val cta: String) {
    SEARCH_STARTED("search_started"),
    CLEAR_SEARCH("clear_search"),
    OPEN_FILTERS("open_filters"),
    SELECT_AUDIENCE_FILTER("select_audience_filter"),
    SELECT_DIETARY_FILTER("select_dietary_filter"),
    TOGGLE_FAVORITES_ONLY("toggle_favorites_only"),
    TOGGLE_HEALTHY_ONLY("toggle_healthy_only"),
    CLEAR_SEARCH_AND_FILTERS("clear_search_and_filters"),
}

internal enum class MenuCatalogFilterInteraction(
    val analyticsAction: MenuCatalogAnalyticsAction,
) {
    AUDIENCE_SELECTED(MenuCatalogAnalyticsAction.SELECT_AUDIENCE_FILTER),
    DIETARY_NEED_SELECTED(MenuCatalogAnalyticsAction.SELECT_DIETARY_FILTER),
    FAVORITES_ONLY_TOGGLED(MenuCatalogAnalyticsAction.TOGGLE_FAVORITES_ONLY),
    HEALTHY_ONLY_TOGGLED(MenuCatalogAnalyticsAction.TOGGLE_HEALTHY_ONLY),
}

internal fun menuCatalogSearchTransitionCta(previousQuery: String, newQuery: String): String? =
    MenuCatalogAnalyticsAction.SEARCH_STARTED.cta.takeIf {
        previousQuery.isBlank() && newQuery.isNotBlank()
    }
