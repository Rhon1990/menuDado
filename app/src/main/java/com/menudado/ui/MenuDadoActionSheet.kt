package com.menudado.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.menudado.ui.theme.MenuDadoUiTokens

internal fun menuDadoActionSheetTopRadiusDp(): Int = 28

internal fun menuDadoActionSheetHandleWidthDp(): Int = 86

internal fun menuDadoActionSheetHandleHeightDp(): Int = 5

internal fun menuDadoActionSheetIconContainerDp(): Int = 42

@Composable
internal fun MenuDadoActionSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {}),
                colors = CardDefaults.cardColors(
                    containerColor = menuActionSheetContainerColor()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                shape = RoundedCornerShape(
                    topStart = menuDadoActionSheetTopRadiusDp().dp,
                    topEnd = menuDadoActionSheetTopRadiusDp().dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 18.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(menuSheetCloseActionButtonSizeDp().dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(menuDadoActionSheetHandleWidthDp().dp)
                                .height(menuDadoActionSheetHandleHeightDp().dp)
                                .clip(
                                    RoundedCornerShape(
                                        MenuDadoUiTokens.ControlRadius
                                    )
                                )
                                .background(
                                    menuActionSheetContentColor()
                                        .copy(alpha = 0.42f)
                                )
                        )
                        MenuDadoSheetCloseButton(
                            onDismiss = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                    Text(
                        text = title,
                        color = menuActionSheetContentColor(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                menuActionSheetContentColor()
                                    .copy(alpha = 0.22f)
                            )
                    )
                    content()
                }
            }
        }
    }
}

@Composable
internal fun MenuDadoActionSheetRow(
    iconRes: Int,
    title: String,
    description: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(MenuDadoUiTokens.ControlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(menuDadoActionSheetIconContainerDp().dp)
                .clip(CircleShape)
                .background(
                    menuActionSheetContentColor().copy(alpha = 0.14f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(23.dp),
                colorFilter = ColorFilter.tint(menuActionSheetContentColor())
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = menuActionSheetContentColor(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            description?.let {
                Text(
                    text = it,
                    color = menuActionSheetContentColor()
                        .copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun MenuDadoSheetCloseButton(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onDismiss,
        modifier = modifier.size(menuSheetCloseActionButtonSizeDp().dp)
    ) {
        Icon(
            painter = painterResource(id = menuSheetCloseActionIconRes()),
            contentDescription = stringResource(
                id = menuSheetCloseActionContentDescriptionRes()
            ),
            modifier = Modifier.size(22.dp),
            tint = menuSheetCloseActionIconTint()
        )
    }
}
