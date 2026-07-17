package com.menudado.data

import com.menudado.domain.CuisineInspiration
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CuisineRotationTest {
    @Test
    fun `catalog contains sixteen unique worldwide inspirations`() {
        assertEquals(16, CuisineInspiration.entries.size)
        assertEquals(16, CuisineInspiration.entries.map { it.promptName }.toSet().size)
    }

    @Test
    fun `catalog order interleaves culinary regions for consecutive requests`() {
        assertEquals(
            listOf(
                CuisineInspiration.MEDITERRANEAN,
                CuisineInspiration.INDIAN,
                CuisineInspiration.MEXICAN,
                CuisineInspiration.JAPANESE,
                CuisineInspiration.GREEK,
                CuisineInspiration.WEST_AFRICAN,
                CuisineInspiration.ITALIAN,
                CuisineInspiration.KOREAN,
                CuisineInspiration.CARIBBEAN,
                CuisineInspiration.LEVANTINE,
                CuisineInspiration.ANDEAN_PERUVIAN,
                CuisineInspiration.NORDIC,
                CuisineInspiration.SPANISH,
                CuisineInspiration.SOUTHEAST_ASIAN,
                CuisineInspiration.BRAZILIAN,
                CuisineInspiration.MAGHREBI
            ),
            CuisineInspiration.entries
        )
    }

    @Test
    fun `rotation visits every cuisine once before repeating`() {
        val state = FakeCuisineRotationStateStore()
        val rotation = CuisineRotation(state) { 3 }
        val seen = buildList {
            repeat(CuisineInspiration.entries.size) {
                add(rotation.current(MealType.LUNCH, MenuAudience.ADULT))
                rotation.advance(MealType.LUNCH, MenuAudience.ADULT)
            }
        }

        assertEquals(CuisineInspiration.entries.size, seen.toSet().size)
        assertEquals(seen.first(), rotation.current(MealType.LUNCH, MenuAudience.ADULT))
    }

    @Test
    fun `reading current cuisine persists pending choice without advancing`() {
        val state = FakeCuisineRotationStateStore()
        val first = CuisineRotation(state) { 5 }
        val pending = first.current(MealType.DINNER, MenuAudience.CHILD)
        val restored = CuisineRotation(state) { 0 }

        assertEquals(pending, restored.current(MealType.DINNER, MenuAudience.CHILD))
    }

    @Test
    fun `meal and audience combinations rotate independently`() {
        val state = FakeCuisineRotationStateStore()
        val rotation = CuisineRotation(state) { 2 }
        val adultLunch = rotation.current(MealType.LUNCH, MenuAudience.ADULT)
        val childLunch = rotation.current(MealType.LUNCH, MenuAudience.CHILD)

        rotation.advance(MealType.LUNCH, MenuAudience.ADULT)

        assertNotEquals(adultLunch, rotation.current(MealType.LUNCH, MenuAudience.ADULT))
        assertEquals(childLunch, rotation.current(MealType.LUNCH, MenuAudience.CHILD))
        assertEquals(
            CuisineInspiration.entries[2],
            rotation.current(MealType.DINNER, MenuAudience.ADULT)
        )
    }

    @Test
    fun `initial offset is normalized before it is persisted`() {
        val state = FakeCuisineRotationStateStore()
        val rotation = CuisineRotation(state) { CuisineInspiration.entries.size + 1 }

        assertEquals(
            CuisineInspiration.entries[1],
            rotation.current(MealType.BREAKFAST, MenuAudience.BABY)
        )
    }
}

private class FakeCuisineRotationStateStore : CuisineRotationStateStore {
    private val indices = mutableMapOf<String, Int>()

    override fun readIndex(key: String): Int? = indices[key]

    override fun writeIndex(key: String, index: Int) {
        indices[key] = index
    }
}
