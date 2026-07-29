package com.menudado.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AiDailyUsagePolicyTest {
    @Test
    fun `signed in users use free generation allowance before rewarded credits`() {
        val policy = AiDailyUsagePolicy(
            isGuest = false,
            guestAiLimitsEnabled = true
        )

        assertEquals(
            AiGenerationAccess.FREE,
            policy.generationAccess(
                usedCount = 9,
                earnedRewardedCredits = 1,
                consumedRewardedCredits = 0
            )
        )
    }

    @Test
    fun `guest free generation allowance is five when guest limits are enabled`() {
        val policy = AiDailyUsagePolicy(
            isGuest = true,
            guestAiLimitsEnabled = true
        )

        assertEquals(
            AiGenerationAccess.REWARDED_OFFER,
            policy.generationAccess(
                usedCount = 5,
                earnedRewardedCredits = 0,
                consumedRewardedCredits = 0
            )
        )
    }

    @Test
    fun `guest has the signed in free allowance when guest limits are disabled`() {
        val policy = AiDailyUsagePolicy(
            isGuest = true,
            guestAiLimitsEnabled = false
        )

        assertEquals(
            AiGenerationAccess.FREE,
            policy.generationAccess(
                usedCount = 9,
                earnedRewardedCredits = 0,
                consumedRewardedCredits = 0
            )
        )
    }

    @Test
    fun `provider hard cap takes precedence over unused rewarded credits`() {
        val policy = AiDailyUsagePolicy(
            isGuest = false,
            guestAiLimitsEnabled = true
        )

        assertEquals(
            AiGenerationAccess.HARD_LIMIT,
            policy.generationAccess(
                usedCount = 20,
                earnedRewardedCredits = 1,
                consumedRewardedCredits = 0
            )
        )
    }

    @Test
    fun `unused rewarded credit grants generation after free allowance`() {
        val policy = AiDailyUsagePolicy(
            isGuest = false,
            guestAiLimitsEnabled = true
        )

        assertEquals(
            AiGenerationAccess.REWARDED_CREDIT,
            policy.generationAccess(
                usedCount = 10,
                earnedRewardedCredits = 1,
                consumedRewardedCredits = 0
            )
        )
    }

    @Test
    fun `rewarded offer remains available until ten credits have been earned`() {
        val policy = AiDailyUsagePolicy(
            isGuest = false,
            guestAiLimitsEnabled = true
        )

        assertEquals(
            AiGenerationAccess.REWARDED_OFFER,
            policy.generationAccess(
                usedCount = 10,
                earnedRewardedCredits = 9,
                consumedRewardedCredits = 9
            )
        )
    }

    @Test
    fun `generation is hard limited after ten rewarded credits are spent`() {
        val policy = AiDailyUsagePolicy(
            isGuest = false,
            guestAiLimitsEnabled = true
        )

        assertEquals(
            AiGenerationAccess.HARD_LIMIT,
            policy.generationAccess(
                usedCount = 10,
                earnedRewardedCredits = 10,
                consumedRewardedCredits = 10
            )
        )
    }

    @Test
    fun `analysis only uses free daily allowance`() {
        val policy = AiDailyUsagePolicy(
            isGuest = false,
            guestAiLimitsEnabled = true
        )

        assertEquals(true, policy.canUseAiAnalysis(usedCount = 9))
        assertEquals(false, policy.canUseAiAnalysis(usedCount = 10))
    }
}
