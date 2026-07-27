package com.menudado.domain

const val AI_PROVIDER_DAILY_HARD_LIMIT = 20
const val SIGNED_IN_DAILY_AI_FREE_LIMIT = 10
const val GUEST_DAILY_AI_FREE_LIMIT = 5
const val MAX_REWARDED_AI_CREDITS_PER_DAY = 10

enum class AiGenerationAccess {
    FREE,
    REWARDED_CREDIT,
    REWARDED_OFFER,
    HARD_LIMIT
}

class AiDailyUsagePolicy(
    isGuest: Boolean,
    guestAiLimitsEnabled: Boolean
) {
    private val freeDailyLimit = if (isGuest && guestAiLimitsEnabled) {
        GUEST_DAILY_AI_FREE_LIMIT
    } else {
        SIGNED_IN_DAILY_AI_FREE_LIMIT
    }

    fun generationAccess(
        usedCount: Int,
        earnedRewardedCredits: Int,
        consumedRewardedCredits: Int
    ): AiGenerationAccess {
        val normalizedUsedCount = usedCount.coerceAtLeast(0)
        val normalizedEarnedCredits = earnedRewardedCredits.coerceIn(0, MAX_REWARDED_AI_CREDITS_PER_DAY)
        val normalizedConsumedCredits = consumedRewardedCredits.coerceIn(0, normalizedEarnedCredits)

        return when {
            normalizedUsedCount < freeDailyLimit -> AiGenerationAccess.FREE
            normalizedUsedCount >= AI_PROVIDER_DAILY_HARD_LIMIT -> AiGenerationAccess.HARD_LIMIT
            normalizedEarnedCredits > normalizedConsumedCredits -> AiGenerationAccess.REWARDED_CREDIT
            normalizedEarnedCredits < MAX_REWARDED_AI_CREDITS_PER_DAY -> AiGenerationAccess.REWARDED_OFFER
            else -> AiGenerationAccess.HARD_LIMIT
        }
    }

    fun canUseAiAnalysis(usedCount: Int): Boolean {
        return usedCount.coerceAtLeast(0) < freeDailyLimit
    }
}
