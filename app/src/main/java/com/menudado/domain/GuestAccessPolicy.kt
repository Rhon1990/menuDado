package com.menudado.domain

const val GUEST_DAILY_MENU_SAVE_LIMIT = 5
const val GUEST_DAILY_AI_GENERATION_LIMIT = 5
const val GUEST_DAILY_AI_ANALYSIS_LIMIT = 5

data class GuestAccessPolicy(
    val isGuest: Boolean,
    val areLimitsEnabled: Boolean
) {
    fun canUseAction(usedCount: Int, limit: Int): Boolean {
        return !isGuest || !areLimitsEnabled || usedCount < limit
    }
}
