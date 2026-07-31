package com.menudado.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.menudado.R
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MenuAudience

private enum class MenuCatalogFilterPage {
    ROOT,
    AUDIENCE,
    NEED
}

@Composable
internal fun MenuCatalogFilterSheet(
    scope: MenuCatalogScope,
    filters: MenuCatalogFilters,
    dietaryProfiles: Map<MenuAudience, DietaryProfile>,
    onFiltersChanged: (MenuCatalogFilters) -> Unit,
    onInteraction: (MenuCatalogFilterInteraction) -> Unit,
    onDismiss: () -> Unit
) {
    var page by remember { mutableStateOf(MenuCatalogFilterPage.ROOT) }
    val title = when (page) {
        MenuCatalogFilterPage.ROOT -> stringResource(R.string.menu_catalog_filters_title)
        MenuCatalogFilterPage.AUDIENCE -> stringResource(R.string.menu_catalog_filter_audience)
        MenuCatalogFilterPage.NEED -> stringResource(R.string.menu_catalog_filter_need)
    }

    MenuDadoActionSheet(
        title = title,
        onDismiss = onDismiss,
        onBack = if (page == MenuCatalogFilterPage.ROOT) {
            null
        } else {
            { page = MenuCatalogFilterPage.ROOT }
        }
    ) {
        when (page) {
            MenuCatalogFilterPage.ROOT -> MenuCatalogFilterRoot(
                scope = scope,
                filters = filters,
                onOpenAudience = { page = MenuCatalogFilterPage.AUDIENCE },
                onOpenNeed = { page = MenuCatalogFilterPage.NEED },
                onFiltersChanged = onFiltersChanged,
                onInteraction = onInteraction
            )
            MenuCatalogFilterPage.AUDIENCE -> MenuCatalogAudienceOptions(
                selected = filters.favoriteAudience,
                onSelected = { audience ->
                    onInteraction(MenuCatalogFilterInteraction.AUDIENCE_SELECTED)
                    onFiltersChanged(
                        menuCatalogFiltersAfterFavoriteAudienceSelected(filters, audience)
                    )
                    page = MenuCatalogFilterPage.ROOT
                }
            )
            MenuCatalogFilterPage.NEED -> MenuCatalogNeedOptions(
                scope = scope,
                filters = filters,
                dietaryProfiles = dietaryProfiles,
                onSelected = { need ->
                    onInteraction(MenuCatalogFilterInteraction.DIETARY_NEED_SELECTED)
                    onFiltersChanged(filters.copy(dietaryNeed = need))
                    page = MenuCatalogFilterPage.ROOT
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.MenuCatalogFilterRoot(
    scope: MenuCatalogScope,
    filters: MenuCatalogFilters,
    onOpenAudience: () -> Unit,
    onOpenNeed: () -> Unit,
    onFiltersChanged: (MenuCatalogFilters) -> Unit,
    onInteraction: (MenuCatalogFilterInteraction) -> Unit
) {
    if (scope == MenuCatalogScope.Favorites) {
        MenuDadoActionSheetRow(
            iconRes = R.drawable.ic_nav_profile,
            title = stringResource(R.string.menu_catalog_filter_audience),
            description = filters.favoriteAudience?.let {
                stringResource(menuAudienceLabelRes(it))
            } ?: stringResource(R.string.menu_catalog_audience_all),
            onClick = onOpenAudience
        )
    }
    MenuDadoActionSheetRow(
        iconRes = R.drawable.ic_filter_list,
        title = stringResource(R.string.menu_catalog_filter_need),
        description = stringResource(filters.dietaryNeed.labelRes()),
        onClick = onOpenNeed
    )
    if (scope is MenuCatalogScope.Audience) {
        MenuDadoActionSheetRow(
            iconRes = R.drawable.ic_favorite_border,
            title = stringResource(R.string.menu_catalog_filter_favorites),
            onClick = {
                onInteraction(MenuCatalogFilterInteraction.FAVORITES_ONLY_TOGGLED)
                onFiltersChanged(filters.copy(favoritesOnly = !filters.favoritesOnly))
            },
            trailingContent = {
                FilterSwitch(checked = filters.favoritesOnly)
            }
        )
    }
    MenuDadoActionSheetRow(
        iconRes = R.drawable.ic_check,
        title = stringResource(R.string.menu_catalog_filter_healthy),
        onClick = {
            onInteraction(MenuCatalogFilterInteraction.HEALTHY_ONLY_TOGGLED)
            onFiltersChanged(filters.copy(healthyOnly = !filters.healthyOnly))
        },
        trailingContent = {
            FilterSwitch(checked = filters.healthyOnly)
        }
    )
}

@Composable
private fun ColumnScope.MenuCatalogAudienceOptions(
    selected: MenuAudience?,
    onSelected: (MenuAudience?) -> Unit
) {
    MenuDadoActionSheetRow(
        iconRes = R.drawable.ic_nav_profile,
        title = stringResource(R.string.menu_catalog_audience_all),
        onClick = { onSelected(null) },
        trailingContent = { FilterRadioButton(selected = selected == null) }
    )
    MenuAudience.entries.forEach { audience ->
        MenuDadoActionSheetRow(
            iconRes = R.drawable.ic_nav_profile,
            title = stringResource(menuAudienceLabelRes(audience)),
            onClick = { onSelected(audience) },
            trailingContent = { FilterRadioButton(selected = selected == audience) }
        )
    }
}

@Composable
private fun ColumnScope.MenuCatalogNeedOptions(
    scope: MenuCatalogScope,
    filters: MenuCatalogFilters,
    dietaryProfiles: Map<MenuAudience, DietaryProfile>,
    onSelected: (MenuCatalogDietaryNeed) -> Unit
) {
    MenuCatalogDietaryNeed.entries.forEach { need ->
        val enabled = menuCatalogDietaryNeedEnabled(
            need = need,
            scope = scope,
            favoriteAudience = filters.favoriteAudience,
            profiles = dietaryProfiles
        )
        MenuDadoActionSheetRow(
            iconRes = R.drawable.ic_filter_list,
            title = stringResource(need.labelRes()),
            onClick = { onSelected(need) },
            enabled = enabled,
            trailingContent = {
                FilterRadioButton(selected = filters.dietaryNeed == need)
            }
        )
    }
}

@Composable
private fun FilterSwitch(checked: Boolean) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        modifier = Modifier.clearAndSetSemantics {},
        colors = SwitchDefaults.colors(
            checkedThumbColor = menuActionSheetContainerColor(),
            checkedTrackColor = menuActionSheetContentColor(),
            uncheckedThumbColor = menuActionSheetContentColor(),
            uncheckedTrackColor = menuActionSheetContentColor().copy(alpha = 0.2f)
        )
    )
}

@Composable
private fun FilterRadioButton(selected: Boolean) {
    RadioButton(
        selected = selected,
        onClick = null,
        modifier = Modifier.clearAndSetSemantics {},
        colors = RadioButtonDefaults.colors(
            selectedColor = menuActionSheetContentColor(),
            unselectedColor = menuActionSheetContentColor().copy(alpha = 0.62f)
        )
    )
}

private fun MenuCatalogDietaryNeed.labelRes(): Int = when (this) {
    MenuCatalogDietaryNeed.NONE -> R.string.menu_catalog_need_none
    MenuCatalogDietaryNeed.PREGNANCY -> R.string.menu_catalog_need_pregnancy
    MenuCatalogDietaryNeed.VEGAN -> R.string.menu_catalog_need_vegan
    MenuCatalogDietaryNeed.ALLERGIES -> R.string.menu_catalog_need_allergies
    MenuCatalogDietaryNeed.FULL_PROFILE -> R.string.menu_catalog_need_full_profile
}

@Composable
internal fun MenuCatalogFilterSheetHost(
    isRequested: Boolean,
    isAnotherModalVisible: Boolean,
    scope: MenuCatalogScope?,
    filters: MenuCatalogFilters,
    dietaryProfiles: Map<MenuAudience, DietaryProfile>,
    onFiltersChanged: (MenuCatalogFilters) -> Unit,
    onInteraction: (MenuCatalogFilterInteraction) -> Unit,
    onDismiss: () -> Unit
) {
    if (isRequested && !isAnotherModalVisible && scope != null) {
        MenuCatalogFilterSheet(
            scope = scope,
            filters = filters,
            dietaryProfiles = dietaryProfiles,
            onFiltersChanged = onFiltersChanged,
            onInteraction = onInteraction,
            onDismiss = onDismiss
        )
    }
}
