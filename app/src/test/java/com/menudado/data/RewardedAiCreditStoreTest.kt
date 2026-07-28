package com.menudado.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.menudado.domain.MAX_REWARDED_AI_CREDITS_PER_DAY
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardedAiCreditStoreTest {
    @Test
    fun `store returns an empty ledger for a date without saved credits`() {
        val store = SharedPreferencesRewardedAiCreditStore(FakeContext())

        assertEquals(
            RewardedAiCreditLedger(
                dateKey = "2026-07-27",
                earnedCount = 0,
                consumedCount = 0
            ),
            store.getLedger("2026-07-27")
        )
    }

    @Test
    fun `store persists a clamped ledger in its dedicated preferences`() {
        val context = FakeContext()
        val store = SharedPreferencesRewardedAiCreditStore(context)

        store.saveLedger(
            RewardedAiCreditLedger(
                dateKey = "2026-07-27",
                earnedCount = -1,
                consumedCount = MAX_REWARDED_AI_CREDITS_PER_DAY + 1
            )
        )

        assertEquals(listOf("menu-dado-rewarded-ai-credits"), context.requestedPreferencesNames)
        assertEquals(
            RewardedAiCreditLedger(
                dateKey = "2026-07-27",
                earnedCount = 0,
                consumedCount = 0
            ),
            SharedPreferencesRewardedAiCreditStore(context).getLedger("2026-07-27")
        )
    }

    @Test
    fun `store resets credits logically when the requested date changes`() {
        val store = SharedPreferencesRewardedAiCreditStore(FakeContext())
        store.saveLedger(
            RewardedAiCreditLedger(
                dateKey = "2026-07-26",
                earnedCount = 4,
                consumedCount = 2
            )
        )

        assertEquals(
            RewardedAiCreditLedger(
                dateKey = "2026-07-27",
                earnedCount = 0,
                consumedCount = 0
            ),
            store.getLedger("2026-07-27")
        )
    }

    @Test
    fun `no op store always returns an empty ledger for the requested date`() {
        NoOpRewardedAiCreditStore.saveLedger(
            RewardedAiCreditLedger(
                dateKey = "2026-07-27",
                earnedCount = 3,
                consumedCount = 1
            )
        )

        assertEquals(
            RewardedAiCreditLedger(
                dateKey = "2026-07-27",
                earnedCount = 0,
                consumedCount = 0
            ),
            NoOpRewardedAiCreditStore.getLedger("2026-07-27")
        )
    }
}

private class FakeContext : ContextWrapper(null) {
    val requestedPreferencesNames = mutableListOf<String>()
    private val preferenceStores = mutableMapOf<String, SharedPreferences>()

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        requestedPreferencesNames += name
        return preferenceStores.getOrPut(name) { createInMemorySharedPreferences() }
    }
}

private fun createInMemorySharedPreferences(): SharedPreferences {
    val values = mutableMapOf<String, Any?>()
    val editor = Proxy.newProxyInstance(
        SharedPreferences.Editor::class.java.classLoader,
        arrayOf(SharedPreferences.Editor::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "putString", "putInt" -> {
                values[args!![0] as String] = args[1]
                proxy
            }

            "apply" -> Unit
            else -> error("Unexpected SharedPreferences.Editor call: ${method.name}")
        }
    } as SharedPreferences.Editor

    return Proxy.newProxyInstance(
        SharedPreferences::class.java.classLoader,
        arrayOf(SharedPreferences::class.java)
    ) { _, method, args ->
        when (method.name) {
            "getString" -> values[args!![0] as String] as? String ?: args[1]
            "getInt" -> values[args!![0] as String] as? Int ?: args[1]
            "edit" -> editor
            else -> error("Unexpected SharedPreferences call: ${method.name}")
        }
    } as SharedPreferences
}
