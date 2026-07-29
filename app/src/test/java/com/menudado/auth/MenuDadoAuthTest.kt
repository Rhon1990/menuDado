package com.menudado.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuDadoAuthTest {
    @Test
    fun `fresh install starts guest mode by default instead of blocking home`() {
        assertTrue(
            shouldStartGuestModeByDefault(
                session = null,
                isGuestModeSelected = false
            )
        )
    }

    @Test
    fun `selected guest keeps current anonymous flow available`() {
        val session = MenuDadoAuthSession(
            userId = "guest-user",
            email = null,
            isAnonymous = true
        )

        assertFalse(shouldStartGuestModeByDefault(session, isGuestModeSelected = true))
        assertTrue(shouldStartMenuDadoBackendSync(session, isGuestModeSelected = true))
    }

    @Test
    fun `anonymous user from previous version is promoted to guest mode without blocking home`() {
        val session = MenuDadoAuthSession(
            userId = "legacy-anonymous",
            email = null,
            isAnonymous = true
        )

        assertTrue(
            shouldStartGuestModeByDefault(
                session = session,
                isGuestModeSelected = false
            )
        )
    }

    @Test
    fun `email session skips access screen on next app open`() {
        val session = MenuDadoAuthSession(
            userId = "registered-user",
            email = "user@example.com",
            isAnonymous = false
        )

        assertFalse(shouldStartGuestModeByDefault(session, isGuestModeSelected = false))
        assertTrue(shouldStartMenuDadoBackendSync(session, isGuestModeSelected = false))
    }

    @Test
    fun `google session stays signed in on next app open`() {
        val session = MenuDadoAuthSession(
            userId = "google-user",
            email = "google-user@gmail.com",
            isAnonymous = false
        )

        assertFalse(shouldStartGuestModeByDefault(session, isGuestModeSelected = false))
        assertTrue(shouldStartMenuDadoBackendSync(session, isGuestModeSelected = false))
    }

    @Test
    fun `guest registration links the current anonymous user instead of replacing uid`() {
        val session = MenuDadoAuthSession(
            userId = "guest-user",
            email = null,
            isAnonymous = true
        )

        assertEquals(MenuDadoRegisterStrategy.LINK_ANONYMOUS_USER, registerStrategyFor(session))
        assertEquals(MenuDadoRegisterStrategy.CREATE_EMAIL_USER, registerStrategyFor(null))
    }
}
