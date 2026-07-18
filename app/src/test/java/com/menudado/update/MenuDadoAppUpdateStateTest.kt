package com.menudado.update

import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuDadoAppUpdateStateTest {
    @Test
    fun `Play availability maps to presentation status`() {
        assertEquals(
            MenuDadoAppUpdateStatus.AVAILABLE,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                installStatus = InstallStatus.UNKNOWN
            )
        )
        assertEquals(
            MenuDadoAppUpdateStatus.DOWNLOADING,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                installStatus = InstallStatus.DOWNLOADING
            )
        )
        assertEquals(
            MenuDadoAppUpdateStatus.DOWNLOADED,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                installStatus = InstallStatus.DOWNLOADED
            )
        )
        assertEquals(
            MenuDadoAppUpdateStatus.NOT_AVAILABLE,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.UPDATE_NOT_AVAILABLE,
                installStatus = InstallStatus.UNKNOWN
            )
        )
    }

    @Test
    fun `developer triggered update remains visible as downloading`() {
        assertEquals(
            MenuDadoAppUpdateStatus.DOWNLOADING,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                installStatus = InstallStatus.UNKNOWN
            )
        )
    }

    @Test
    fun `dismissed prompt keeps persistent reminder`() {
        val state = MenuDadoAppUpdatePresentationState(
            status = MenuDadoAppUpdateStatus.AVAILABLE,
            isAvailablePromptDismissed = true
        )

        assertFalse(state.shouldShowAvailablePrompt)
        assertTrue(state.shouldShowReminder)
    }

    @Test
    fun `downloaded transition re-enables install prompt`() {
        val state = MenuDadoAppUpdatePresentationState(
            status = MenuDadoAppUpdateStatus.AVAILABLE,
            isAvailablePromptDismissed = true,
            isDownloadedPromptDismissed = true
        ).withStatus(MenuDadoAppUpdateStatus.DOWNLOADED)

        assertTrue(state.shouldShowDownloadedPrompt)
        assertTrue(state.shouldShowReminder)
    }
}
