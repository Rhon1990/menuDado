package com.menudado.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestAiLimitsRemoteConfigTest {
    @Test
    fun `guest AI limits use their own remotely configurable default`() {
        assertEquals("guest_ai_limits_enabled", GuestAiLimitsRemoteConfig.KEY_GUEST_AI_LIMITS_ENABLED)
        assertNotEquals(
            MenuDadoGuestLimitsRemoteConfig.KEY_GUEST_LIMITS_ENABLED,
            GuestAiLimitsRemoteConfig.KEY_GUEST_AI_LIMITS_ENABLED
        )
        assertTrue(GuestAiLimitsRemoteConfig.DEFAULT_GUEST_AI_LIMITS_ENABLED)
        assertEquals(0L, GuestAiLimitsRemoteConfig.fetchIntervalSeconds(isDebugBuild = true))
        assertEquals(3_600L, GuestAiLimitsRemoteConfig.fetchIntervalSeconds(isDebugBuild = false))
    }
}
