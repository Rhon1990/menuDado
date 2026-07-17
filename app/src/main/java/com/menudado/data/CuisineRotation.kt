package com.menudado.data

import android.content.Context
import com.menudado.domain.CuisineInspiration
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import kotlin.random.Random

interface CuisineRotationStateStore {
    fun readIndex(key: String): Int?
    fun writeIndex(key: String, index: Int)
}

class InMemoryCuisineRotationStateStore : CuisineRotationStateStore {
    private val indices = mutableMapOf<String, Int>()

    override fun readIndex(key: String): Int? = indices[key]

    override fun writeIndex(key: String, index: Int) {
        indices[key] = index
    }
}

class SharedPreferencesCuisineRotationStateStore(context: Context) : CuisineRotationStateStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun readIndex(key: String): Int? {
        return if (preferences.contains(key)) preferences.getInt(key, 0) else null
    }

    override fun writeIndex(key: String, index: Int) {
        preferences.edit().putInt(key, index).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-cuisine-rotation"
    }
}

class CuisineRotation(
    private val stateStore: CuisineRotationStateStore,
    private val initialIndexProvider: (String) -> Int = {
        Random.nextInt(CuisineInspiration.entries.size)
    }
) {
    fun current(mealType: MealType, audience: MenuAudience): CuisineInspiration {
        return CuisineInspiration.entries[currentIndex(rotationKey(mealType, audience))]
    }

    fun advance(mealType: MealType, audience: MenuAudience) {
        val key = rotationKey(mealType, audience)
        stateStore.writeIndex(key, normalize(currentIndex(key) + 1))
    }

    private fun currentIndex(key: String): Int {
        stateStore.readIndex(key)?.let { return normalize(it) }
        return normalize(initialIndexProvider(key)).also { stateStore.writeIndex(key, it) }
    }

    private fun normalize(index: Int): Int {
        return Math.floorMod(index, CuisineInspiration.entries.size)
    }

    private fun rotationKey(mealType: MealType, audience: MenuAudience): String {
        return "${mealType.name}_${audience.name}"
    }
}
