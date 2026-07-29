package com.menudado.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestAccessPolicyTest {
    @Test
    fun `guest action is blocked at the daily limit`() {
        val policy = GuestAccessPolicy(
            isGuest = true,
            areLimitsEnabled = true
        )

        assertFalse(policy.canUseAction(usedCount = GUEST_DAILY_MENU_SAVE_LIMIT, limit = GUEST_DAILY_MENU_SAVE_LIMIT))
    }

    @Test
    fun `guest action is unlimited when limits are disabled`() {
        val policy = GuestAccessPolicy(
            isGuest = true,
            areLimitsEnabled = false
        )

        assertTrue(policy.canUseAction(usedCount = GUEST_DAILY_MENU_SAVE_LIMIT, limit = GUEST_DAILY_MENU_SAVE_LIMIT))
    }

    @Test
    fun `signed in users are not limited by guest policy`() {
        val policy = GuestAccessPolicy(
            isGuest = false,
            areLimitsEnabled = true
        )

        assertTrue(policy.canUseAction(usedCount = GUEST_DAILY_MENU_SAVE_LIMIT, limit = GUEST_DAILY_MENU_SAVE_LIMIT))
    }
}
