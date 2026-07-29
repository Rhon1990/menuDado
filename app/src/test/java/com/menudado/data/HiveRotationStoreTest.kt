package com.menudado.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy

class HiveRotationStoreTest {

    @Test
    fun `guest and accounts keep independent rotation histories`() {
        val store = SharedPreferencesHiveRotationStore(FakeHiveRotationContext())

        store.recordShown(GUEST_AI_USAGE_SCOPE, "guest-hash", startsNewCycle = false)
        store.recordShown(accountAiUsageScope("a"), "account-hash", startsNewCycle = false)

        assertEquals(
            listOf("guest-hash"),
            store.snapshot(GUEST_AI_USAGE_SCOPE).seenHashes
        )
        assertEquals(
            listOf("account-hash"),
            store.snapshot(accountAiUsageScope("a")).seenHashes
        )
    }

    @Test
    fun `new cycle clears prior hashes and keeps selected candidate`() {
        val store = SharedPreferencesHiveRotationStore(FakeHiveRotationContext())
        store.recordShown(GUEST_AI_USAGE_SCOPE, "first", startsNewCycle = false)

        store.recordShown(GUEST_AI_USAGE_SCOPE, "second", startsNewCycle = true)

        assertEquals(
            HiveRotationSnapshot(
                seenHashes = listOf("second"),
                lastShownHash = "second"
            ),
            store.snapshot(GUEST_AI_USAGE_SCOPE)
        )
    }

    @Test
    fun `history keeps only latest twenty four hashes`() {
        val store = SharedPreferencesHiveRotationStore(FakeHiveRotationContext())

        repeat(30) { index ->
            store.recordShown(GUEST_AI_USAGE_SCOPE, "hash-$index", startsNewCycle = false)
        }

        assertEquals(
            (6 until 30).map { "hash-$it" },
            store.snapshot(GUEST_AI_USAGE_SCOPE).seenHashes
        )
    }

    @Test
    fun `history survives store recreation on the same phone`() {
        val context = FakeHiveRotationContext()
        SharedPreferencesHiveRotationStore(context)
            .recordShown(accountAiUsageScope("a"), "persisted-hash", startsNewCycle = false)

        val restored = SharedPreferencesHiveRotationStore(context)
            .snapshot(accountAiUsageScope("a"))

        assertEquals(
            HiveRotationSnapshot(
                seenHashes = listOf("persisted-hash"),
                lastShownHash = "persisted-hash"
            ),
            restored
        )
    }
}

private class FakeHiveRotationContext : ContextWrapper(null) {
    private val preferences = createHiveRotationPreferences()

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences
}

private fun createHiveRotationPreferences(): SharedPreferences {
    val values = mutableMapOf<String, String?>()
    val editor = Proxy.newProxyInstance(
        SharedPreferences.Editor::class.java.classLoader,
        arrayOf(SharedPreferences.Editor::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "putString" -> {
                values[args!![0] as String] = args[1] as String?
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
            "getString" -> values[args!![0] as String] ?: args[1]
            "edit" -> editor
            else -> error("Unexpected SharedPreferences call: ${method.name}")
        }
    } as SharedPreferences
}
