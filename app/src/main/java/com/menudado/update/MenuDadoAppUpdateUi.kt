package com.menudado.update

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.menudado.R
import com.menudado.ui.theme.MenuDadoColors
import com.menudado.ui.theme.MenuDadoUiTokens

internal data class MenuDadoAppUpdateContent(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    @StringRes val actionRes: Int?
)

internal fun menuDadoAppUpdateContent(
    status: MenuDadoAppUpdateStatus
): MenuDadoAppUpdateContent? {
    return when (status) {
        MenuDadoAppUpdateStatus.NOT_AVAILABLE -> null
        MenuDadoAppUpdateStatus.AVAILABLE -> MenuDadoAppUpdateContent(
            titleRes = R.string.app_update_title,
            bodyRes = R.string.app_update_reminder_body,
            actionRes = R.string.app_update_action
        )
        MenuDadoAppUpdateStatus.DOWNLOADING -> MenuDadoAppUpdateContent(
            titleRes = R.string.app_update_downloading_title,
            bodyRes = R.string.app_update_downloading_body,
            actionRes = null
        )
        MenuDadoAppUpdateStatus.DOWNLOADED -> MenuDadoAppUpdateContent(
            titleRes = R.string.app_update_downloaded_title,
            bodyRes = R.string.app_update_downloaded_reminder_body,
            actionRes = R.string.app_update_install_action
        )
    }
}

@Composable
internal fun MenuDadoAppUpdateAvailableDialog(
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onUpdate,
                colors = ButtonDefaults.buttonColors(containerColor = MenuDadoColors.BrandGreen)
            ) {
                Text(text = stringResource(id = R.string.app_update_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.app_update_later))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.app_update_title))
        },
        text = {
            Text(text = stringResource(id = R.string.app_update_body))
        }
    )
}

@Composable
internal fun MenuDadoAppUpdateDownloadedDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = MenuDadoColors.BrandGreen)
            ) {
                Text(text = stringResource(id = R.string.app_update_install_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.app_update_later))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.app_update_downloaded_title))
        },
        text = {
            Text(text = stringResource(id = R.string.app_update_downloaded_body))
        }
    )
}

@Composable
internal fun MenuDadoAppUpdateReminder(
    status: MenuDadoAppUpdateStatus,
    onUpdate: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content = menuDadoAppUpdateContent(status) ?: return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MenuDadoColors.SelectionGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MenuDadoColors.BrandGreen.copy(alpha = 0.24f)),
        shape = RoundedCornerShape(MenuDadoUiTokens.ControlRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = content.titleRes),
                    color = MenuDadoColors.Ink,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(id = content.bodyRes),
                    color = MenuDadoColors.MutedInk,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (status == MenuDadoAppUpdateStatus.DOWNLOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MenuDadoColors.BrandGreen,
                    strokeWidth = 2.dp
                )
            } else {
                content.actionRes?.let { actionRes ->
                    TextButton(
                        onClick = if (status == MenuDadoAppUpdateStatus.DOWNLOADED) {
                            onInstall
                        } else {
                            onUpdate
                        }
                    ) {
                        Text(
                            text = stringResource(id = actionRes),
                            color = MenuDadoColors.BrandGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
