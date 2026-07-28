package com.menudado.data

import android.content.Context
import com.menudado.domain.MAX_REWARDED_AI_CREDITS_PER_DAY

data class RewardedAiCreditLedger(
    val dateKey: String,
    val earnedCount: Int,
    val consumedCount: Int
)

interface RewardedAiCreditStore {
    fun getLedger(scope: String, dateKey: String): RewardedAiCreditLedger
    fun saveLedger(scope: String, ledger: RewardedAiCreditLedger)

    fun getLedger(dateKey: String): RewardedAiCreditLedger {
        return getLedger(GUEST_AI_USAGE_SCOPE, dateKey)
    }

    fun saveLedger(ledger: RewardedAiCreditLedger) {
        saveLedger(GUEST_AI_USAGE_SCOPE, ledger)
    }
}

object NoOpRewardedAiCreditStore : RewardedAiCreditStore {
    override fun getLedger(scope: String, dateKey: String): RewardedAiCreditLedger {
        return RewardedAiCreditLedger(
            dateKey = dateKey,
            earnedCount = 0,
            consumedCount = 0
        )
    }

    override fun saveLedger(scope: String, ledger: RewardedAiCreditLedger) = Unit
}

class SharedPreferencesRewardedAiCreditStore(context: Context) : RewardedAiCreditStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getLedger(scope: String, dateKey: String): RewardedAiCreditLedger {
        migrateLegacyLedgerIfNeeded(scope)
        val storedDateKey = preferences.getString(preferenceKey(scope, KEY_DATE), null)
        if (storedDateKey != dateKey) {
            return RewardedAiCreditLedger(
                dateKey = dateKey,
                earnedCount = 0,
                consumedCount = 0
            )
        }

        val earnedCount = preferences.getInt(preferenceKey(scope, KEY_EARNED_COUNT), 0)
            .coerceIn(0, MAX_REWARDED_AI_CREDITS_PER_DAY)
        return RewardedAiCreditLedger(
            dateKey = dateKey,
            earnedCount = earnedCount,
            consumedCount = preferences.getInt(preferenceKey(scope, KEY_CONSUMED_COUNT), 0)
                .coerceIn(0, earnedCount)
        )
    }

    override fun saveLedger(scope: String, ledger: RewardedAiCreditLedger) {
        migrateLegacyLedgerIfNeeded(scope)
        val earnedCount = ledger.earnedCount.coerceIn(0, MAX_REWARDED_AI_CREDITS_PER_DAY)
        preferences.edit()
            .putString(preferenceKey(scope, KEY_DATE), ledger.dateKey)
            .putInt(preferenceKey(scope, KEY_EARNED_COUNT), earnedCount)
            .putInt(
                preferenceKey(scope, KEY_CONSUMED_COUNT),
                ledger.consumedCount.coerceIn(0, earnedCount)
            )
            .apply()
    }

    private fun migrateLegacyLedgerIfNeeded(scope: String) {
        if (preferences.getString(KEY_LEGACY_MIGRATION_SCOPE, null) != null) {
            return
        }
        val legacyDateKey = preferences.getString(KEY_DATE, null)
        val editor = preferences.edit()
        if (legacyDateKey != null) {
            val earnedCount = preferences.getInt(KEY_EARNED_COUNT, 0)
                .coerceIn(0, MAX_REWARDED_AI_CREDITS_PER_DAY)
            editor
                .putString(preferenceKey(scope, KEY_DATE), legacyDateKey)
                .putInt(preferenceKey(scope, KEY_EARNED_COUNT), earnedCount)
                .putInt(
                    preferenceKey(scope, KEY_CONSUMED_COUNT),
                    preferences.getInt(KEY_CONSUMED_COUNT, 0).coerceIn(0, earnedCount)
                )
        }
        editor
            .putString(KEY_LEGACY_MIGRATION_SCOPE, scope)
            .apply()
    }

    private fun preferenceKey(scope: String, key: String): String = "$scope.$key"

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-rewarded-ai-credits"
        const val KEY_DATE = "date"
        const val KEY_EARNED_COUNT = "earned_count"
        const val KEY_CONSUMED_COUNT = "consumed_count"
        const val KEY_LEGACY_MIGRATION_SCOPE = "legacy_migration_scope"
    }
}
