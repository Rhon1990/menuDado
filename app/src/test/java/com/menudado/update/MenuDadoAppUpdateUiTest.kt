package com.menudado.update

import com.menudado.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuDadoAppUpdateUiTest {
    @Test
    fun `available reminder offers update`() {
        assertEquals(
            MenuDadoAppUpdateContent(
                titleRes = R.string.app_update_title,
                bodyRes = R.string.app_update_reminder_body,
                actionRes = R.string.app_update_action
            ),
            menuDadoAppUpdateContent(MenuDadoAppUpdateStatus.AVAILABLE)
        )
    }

    @Test
    fun `downloading reminder has no repeated action`() {
        assertNull(menuDadoAppUpdateContent(MenuDadoAppUpdateStatus.DOWNLOADING)?.actionRes)
    }

    @Test
    fun `downloaded reminder offers install`() {
        assertEquals(
            R.string.app_update_install_action,
            menuDadoAppUpdateContent(MenuDadoAppUpdateStatus.DOWNLOADED)?.actionRes
        )
    }

    @Test
    fun `no update has no reminder content`() {
        assertNull(menuDadoAppUpdateContent(MenuDadoAppUpdateStatus.NOT_AVAILABLE))
    }
}
