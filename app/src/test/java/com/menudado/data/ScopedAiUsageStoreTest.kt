package com.menudado.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopedAiUsageStoreTest {
    @Test
    fun `guest and accounts keep independent daily usage`() {
        val store = SharedPreferencesScopedAiUsageStore(FakeScopedUsageContext())

        store.saveUsageState(GUEST_AI_USAGE_SCOPE, AiDailyUsageState("2026-07-28", 5))
        store.saveUsageState(accountAiUsageScope("user-a"), AiDailyUsageState("2026-07-28", 3))

        assertEquals(5, store.getUsageState(GUEST_AI_USAGE_SCOPE, "2026-07-28").usedCount)
        assertEquals(3, store.getUsageState(accountAiUsageScope("user-a"), "2026-07-28").usedCount)
        assertEquals(0, store.getUsageState(accountAiUsageScope("user-b"), "2026-07-28").usedCount)
    }

    @Test
    fun `usage resets logically for a different date and clamps invalid values`() {
        val store = SharedPreferencesScopedAiUsageStore(FakeScopedUsageContext())

        store.saveUsageState(GUEST_AI_USAGE_SCOPE, AiDailyUsageState("2026-07-27", -4))

        assertEquals(0, store.getUsageState(GUEST_AI_USAGE_SCOPE, "2026-07-27").usedCount)
        assertEquals(0, store.getUsageState(GUEST_AI_USAGE_SCOPE, "2026-07-28").usedCount)
    }

    @Test
    fun `legacy usage migrates once to active identity and provider safeguard`() {
        val context = FakeScopedUsageContext()
        val store = SharedPreferencesScopedAiUsageStore(context)
        val legacy = AiDailyUsageState("2026-07-28", 5)

        assertTrue(store.migrateLegacyUsage(GUEST_AI_USAGE_SCOPE, legacy))
        assertEquals(5, store.getUsageState(GUEST_AI_USAGE_SCOPE, "2026-07-28").usedCount)
        assertEquals(5, store.getUsageState(PROVIDER_AI_USAGE_SCOPE, "2026-07-28").usedCount)

        assertFalse(store.migrateLegacyUsage(accountAiUsageScope("user-a"), legacy.copy(usedCount = 9)))
        assertEquals(0, store.getUsageState(accountAiUsageScope("user-a"), "2026-07-28").usedCount)
        assertEquals(5, store.getUsageState(PROVIDER_AI_USAGE_SCOPE, "2026-07-28").usedCount)
    }
}

private class FakeScopedUsageContext : ContextWrapper(null) {
    private val preferenceStores = mutableMapOf<String, SharedPreferences>()

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return preferenceStores.getOrPut(name) { createScopedUsageSharedPreferences() }
    }
}

private fun createScopedUsageSharedPreferences(): SharedPreferences {
    val values = mutableMapOf<String, Any?>()
    val editor = Proxy.newProxyInstance(
        SharedPreferences.Editor::class.java.classLoader,
        arrayOf(SharedPreferences.Editor::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "putString", "putInt", "putBoolean" -> {
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
            "edit" -> editor
            "getString" -> values[args!![0] as String] as? String ?: args[1]
            "getInt" -> values[args!![0] as String] as? Int ?: args[1]
            "getBoolean" -> values[args!![0] as String] as? Boolean ?: args[1]
            else -> error("Unexpected SharedPreferences call: ${method.name}")
        }
    } as SharedPreferences
}
