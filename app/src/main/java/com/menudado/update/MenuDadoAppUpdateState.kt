package com.menudado.update

import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

enum class MenuDadoAppUpdateStatus {
    NOT_AVAILABLE,
    AVAILABLE,
    DOWNLOADING,
    DOWNLOADED
}

internal data class MenuDadoAppUpdatePresentationState(
    val status: MenuDadoAppUpdateStatus = MenuDadoAppUpdateStatus.NOT_AVAILABLE,
    val isAvailablePromptDismissed: Boolean = false,
    val isDownloadedPromptDismissed: Boolean = false
) {
    val shouldShowAvailablePrompt: Boolean
        get() = status == MenuDadoAppUpdateStatus.AVAILABLE && !isAvailablePromptDismissed

    val shouldShowDownloadedPrompt: Boolean
        get() = status == MenuDadoAppUpdateStatus.DOWNLOADED && !isDownloadedPromptDismissed

    val shouldShowReminder: Boolean
        get() = status != MenuDadoAppUpdateStatus.NOT_AVAILABLE

    fun withStatus(nextStatus: MenuDadoAppUpdateStatus): MenuDadoAppUpdatePresentationState {
        if (nextStatus == status) return this
        return copy(
            status = nextStatus,
            isAvailablePromptDismissed = if (nextStatus == MenuDadoAppUpdateStatus.NOT_AVAILABLE) {
                false
            } else {
                isAvailablePromptDismissed
            },
            isDownloadedPromptDismissed = when (nextStatus) {
                MenuDadoAppUpdateStatus.DOWNLOADED,
                MenuDadoAppUpdateStatus.NOT_AVAILABLE -> false
                else -> isDownloadedPromptDismissed
            }
        )
    }
}

internal fun menuDadoAppUpdateStatus(
    updateAvailability: Int,
    installStatus: Int
): MenuDadoAppUpdateStatus {
    return when {
        installStatus == InstallStatus.DOWNLOADED -> MenuDadoAppUpdateStatus.DOWNLOADED
        installStatus == InstallStatus.PENDING ||
            installStatus == InstallStatus.DOWNLOADING ||
            installStatus == InstallStatus.INSTALLING -> MenuDadoAppUpdateStatus.DOWNLOADING
        updateAvailability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
            MenuDadoAppUpdateStatus.DOWNLOADING
        updateAvailability == UpdateAvailability.UPDATE_AVAILABLE ->
            MenuDadoAppUpdateStatus.AVAILABLE
        else -> MenuDadoAppUpdateStatus.NOT_AVAILABLE
    }
}
