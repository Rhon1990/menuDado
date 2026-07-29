package com.menudado.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardedAdCompletionTest {
    @Test
    fun `earned reward is delivered only after dismissal`() {
        val events = mutableListOf<String>()
        val completion = RewardedAdCompletion(
            onRewardEarned = { events += "earned" },
            onDismissedWithoutReward = { events += "dismissed" }
        )

        completion.recordReward()
        assertTrue(events.isEmpty())

        completion.completeAfterDismissal()

        assertEquals(listOf("earned"), events)
    }

    @Test
    fun `dismissal without reward keeps existing callback`() {
        val events = mutableListOf<String>()
        val completion = RewardedAdCompletion(
            onRewardEarned = { events += "earned" },
            onDismissedWithoutReward = { events += "dismissed" }
        )

        completion.completeAfterDismissal()

        assertEquals(listOf("dismissed"), events)
    }

    @Test
    fun `completion is consumed once and cancelled presentations stay silent`() {
        val events = mutableListOf<String>()
        val completion = RewardedAdCompletion(
            onRewardEarned = { events += "earned" },
            onDismissedWithoutReward = { events += "dismissed" }
        )

        completion.cancel()
        completion.recordReward()
        completion.completeAfterDismissal()
        completion.completeAfterDismissal()

        assertTrue(events.isEmpty())
    }
}
