package com.menudado.data

import android.content.Context
import com.menudado.domain.MAX_REWARDED_AI_CREDITS_PER_DAY

data class RewardedAiCreditLedger(
    val dateKey: String,
    val earnedCount: Int,
    val consumedCount: Int
)

interface RewardedAiCreditStore {
    fun getLedger(dateKey: String): RewardedAiCreditLedger
    fun saveLedger(ledger: RewardedAiCreditLedger)
}

object NoOpRewardedAiCreditStore : RewardedAiCreditStore {
    override fun getLedger(dateKey: String): RewardedAiCreditLedger {
        return RewardedAiCreditLedger(
            dateKey = dateKey,
            earnedCount = 0,
            consumedCount = 0
        )
    }

    override fun saveLedger(ledger: RewardedAiCreditLedger) = Unit
}

class SharedPreferencesRewardedAiCreditStore(context: Context) : RewardedAiCreditStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getLedger(dateKey: String): RewardedAiCreditLedger {
        val storedDateKey = preferences.getString(KEY_DATE, null)
        if (storedDateKey != dateKey) {
            return RewardedAiCreditLedger(
                dateKey = dateKey,
                earnedCount = 0,
                consumedCount = 0
            )
        }

        return RewardedAiCreditLedger(
            dateKey = dateKey,
            earnedCount = preferences.getInt(KEY_EARNED_COUNT, 0)
                .coerceIn(0, MAX_REWARDED_AI_CREDITS_PER_DAY),
            consumedCount = preferences.getInt(KEY_CONSUMED_COUNT, 0)
                .coerceIn(0, MAX_REWARDED_AI_CREDITS_PER_DAY)
        )
    }

    override fun saveLedger(ledger: RewardedAiCreditLedger) {
        preferences.edit()
            .putString(KEY_DATE, ledger.dateKey)
            .putInt(
                KEY_EARNED_COUNT,
                ledger.earnedCount.coerceIn(0, MAX_REWARDED_AI_CREDITS_PER_DAY)
            )
            .putInt(
                KEY_CONSUMED_COUNT,
                ledger.consumedCount.coerceIn(0, MAX_REWARDED_AI_CREDITS_PER_DAY)
            )
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-rewarded-ai-credits"
        const val KEY_DATE = "date"
        const val KEY_EARNED_COUNT = "earned_count"
        const val KEY_CONSUMED_COUNT = "consumed_count"
    }
}
