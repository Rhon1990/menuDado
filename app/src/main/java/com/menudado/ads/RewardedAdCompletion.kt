package com.menudado.ads

internal class RewardedAdCompletion(
    private val onRewardEarned: () -> Unit,
    private val onDismissedWithoutReward: () -> Unit
) {
    private var rewardEarned = false
    private var isCompleted = false

    fun recordReward() {
        if (!isCompleted) {
            rewardEarned = true
        }
    }

    fun completeAfterDismissal() {
        if (isCompleted) return
        isCompleted = true
        if (rewardEarned) {
            onRewardEarned()
        } else {
            onDismissedWithoutReward()
        }
    }

    fun cancel() {
        isCompleted = true
    }
}
