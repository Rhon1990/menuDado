package com.menudado.data

import android.content.Context

data class HiveRotationSnapshot(
    val seenHashes: List<String> = emptyList(),
    val lastShownHash: String? = null
)

interface HiveRotationStore {
    fun snapshot(scope: String): HiveRotationSnapshot
    fun recordShown(scope: String, semanticHash: String, startsNewCycle: Boolean)
}

object NoOpHiveRotationStore : HiveRotationStore {
    override fun snapshot(scope: String): HiveRotationSnapshot = HiveRotationSnapshot()

    override fun recordShown(
        scope: String,
        semanticHash: String,
        startsNewCycle: Boolean
    ) = Unit
}

class SharedPreferencesHiveRotationStore(context: Context) : HiveRotationStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun snapshot(scope: String): HiveRotationSnapshot {
        return HiveRotationSnapshot(
            seenHashes = preferences.getString(hashesKey(scope), null)
                ?.split(HASH_SEPARATOR)
                ?.filter(String::isNotBlank)
                .orEmpty(),
            lastShownHash = preferences.getString(lastHashKey(scope), null)
        )
    }

    override fun recordShown(
        scope: String,
        semanticHash: String,
        startsNewCycle: Boolean
    ) {
        val currentHashes = if (startsNewCycle) {
            emptyList()
        } else {
            snapshot(scope).seenHashes
        }
        val updatedHashes = (currentHashes - semanticHash + semanticHash)
            .takeLast(MAX_HIVE_ROTATION_HASHES)

        preferences.edit()
            .putString(hashesKey(scope), updatedHashes.joinToString(HASH_SEPARATOR))
            .putString(lastHashKey(scope), semanticHash)
            .apply()
    }

    private fun hashesKey(scope: String): String = "$scope.$KEY_HASHES"

    private fun lastHashKey(scope: String): String = "$scope.$KEY_LAST_HASH"

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-hive-rotation"
        const val HASH_SEPARATOR = "|"
        const val KEY_HASHES = "hashes"
        const val KEY_LAST_HASH = "last_hash"
    }
}

internal const val MAX_HIVE_ROTATION_HASHES = 24
