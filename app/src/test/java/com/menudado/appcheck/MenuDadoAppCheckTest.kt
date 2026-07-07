package com.menudado.appcheck

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuDadoAppCheckTest {
    @Test
    fun `debug provider is selected only when build config asks for debug app check`() {
        assertEquals(
            MenuDadoAppCheckProviderKind.DEBUG,
            menuDadoAppCheckProviderKind("debug")
        )
        assertEquals(
            MenuDadoAppCheckProviderKind.PLAY_INTEGRITY,
            menuDadoAppCheckProviderKind("play_integrity")
        )
    }
}
