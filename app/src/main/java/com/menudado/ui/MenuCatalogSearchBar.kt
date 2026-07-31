package com.menudado.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menudado.R
import com.menudado.ui.theme.MenuDadoColors
import com.menudado.ui.theme.MenuDadoUiTokens

internal const val MENU_CATALOG_SEARCH_FIELD_TEST_TAG = "menu_catalog_search_field"

internal data class MenuCatalogFilterButtonVisualStyle(
    val containerColor: Color,
    val borderColor: Color,
    val iconColor: Color,
    val borderWidth: Dp,
    val badgeSize: Dp,
    val badgeHorizontalOffset: Dp,
    val badgeVerticalOffset: Dp,
    val badgeFontSize: TextUnit,
    val badgeLineHeight: TextUnit,
    val includeFontPadding: Boolean
)

internal fun menuCatalogFilterButtonVisualStyle() = MenuCatalogFilterButtonVisualStyle(
    containerColor = MenuDadoColors.Cream.copy(alpha = 0.12f),
    borderColor = MenuDadoColors.Cream.copy(alpha = 0.82f),
    iconColor = MenuDadoColors.Cream,
    borderWidth = 1.dp,
    badgeSize = 20.dp,
    badgeHorizontalOffset = 4.dp,
    badgeVerticalOffset = (-4).dp,
    badgeFontSize = 11.sp,
    badgeLineHeight = 11.sp,
    includeFontPadding = false
)

@Composable
internal fun MenuCatalogSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    activeFilterCount: Int,
    onOpenFilters: () -> Unit
) {
    val filterStyle = menuCatalogFilterButtonVisualStyle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(MenuDadoUiTokens.ControlRadius))
                .background(MenuDadoColors.Cream)
                .testTag(MENU_CATALOG_SEARCH_FIELD_TEST_TAG),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MenuDadoColors.Ink
            ),
            cursorBrush = SolidColor(MenuDadoColors.HeaderGreen),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(start = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = MenuDadoColors.HeaderGreen
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.menu_catalog_search_hint),
                                color = MenuDadoColors.MutedInk,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(
                                    R.string.menu_catalog_clear_search
                                ),
                                modifier = Modifier.size(19.dp),
                                tint = MenuDadoColors.HeaderGreen
                            )
                        }
                    }
                }
            }
        )
        Box(modifier = Modifier.size(48.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(MenuDadoUiTokens.ControlRadius))
                    .border(
                        width = filterStyle.borderWidth,
                        color = filterStyle.borderColor,
                        shape = RoundedCornerShape(MenuDadoUiTokens.ControlRadius)
                    )
                    .background(filterStyle.containerColor)
                    .clickable(onClick = onOpenFilters),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_filter_list),
                    contentDescription = if (activeFilterCount > 0) {
                        stringResource(
                            R.string.menu_catalog_open_filters_active,
                            activeFilterCount
                        )
                    } else {
                        stringResource(R.string.menu_catalog_open_filters)
                    },
                    modifier = Modifier.size(24.dp),
                    tint = filterStyle.iconColor
                )
            }
            if (activeFilterCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = filterStyle.badgeHorizontalOffset,
                            y = filterStyle.badgeVerticalOffset
                        )
                        .size(filterStyle.badgeSize)
                        .clip(CircleShape)
                        .background(MenuDadoColors.ActionTerracotta),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeFilterCount.toString(),
                        style = TextStyle(
                            color = MenuDadoColors.Cream,
                            fontSize = filterStyle.badgeFontSize,
                            lineHeight = filterStyle.badgeLineHeight,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = filterStyle.includeFontPadding
                            )
                        )
                    )
                }
            }
        }
    }
}
