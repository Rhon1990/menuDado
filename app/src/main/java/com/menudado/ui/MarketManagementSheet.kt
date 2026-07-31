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
