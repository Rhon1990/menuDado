package com.menudado.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMenuHiveIdentityTest {
    @Test
    fun `reported lentil salad variants share canonical identity`() {
        val identities = identityFixtures().map { fixture ->
            val identity = requireNotNull(
                AiMenuHiveIdentity.from(fixture.language, fixture.rawKey)
            )
            assertEquals(fixture.canonicalKey, identity.canonicalKey)
            identity
        }

        assertEquals(1, identities.distinct().size)
    }

    @Test
    fun `different dish families ingredients and preparations do not collide`() {
        val salad = AiMenuHiveIdentity.from(
            AppLanguage.SPANISH,
            "salad|lentils+vegetables|mixed"
        )
        val stew = AiMenuHiveIdentity.from(
            AppLanguage.SPANISH,
            "stew|lentils+vegetables|stewed"
        )
        val chickpeas = AiMenuHiveIdentity.from(
            AppLanguage.SPANISH,
            "salad|chickpeas+vegetables|mixed"
        )

        assertNotEquals(salad, stew)
        assertNotEquals(salad, chickpeas)
    }

    @Test
    fun `equivalent pasta names share semantic hash`() {
        val pasta = AiMenuHiveIdentity.from(
            language = AppLanguage.SPANISH,
            rawKey = "pasta|tomato|sauce"
        )
        val spaghetti = AiMenuHiveIdentity.from(
            language = AppLanguage.SPANISH,
            rawKey = "spaghetti|tomate|salsa"
        )

        assertEquals(pasta, spaghetti)
        assertEquals(64, requireNotNull(pasta).semanticHash.length)
    }

    @Test
    fun `different preparations or languages do not collide`() {
        val sauce = AiMenuHiveIdentity.from(AppLanguage.SPANISH, "pasta|tomato|sauce")
        val salad = AiMenuHiveIdentity.from(AppLanguage.SPANISH, "pasta|tomato|salad")
        val english = AiMenuHiveIdentity.from(AppLanguage.ENGLISH, "pasta|tomato|sauce")

        assertNotEquals(sauce, salad)
        assertNotEquals(sauce, english)
    }

    @Test
    fun `main ingredient order does not change identity`() {
        val first = AiMenuHiveIdentity.from(
            AppLanguage.SPANISH,
            "bowl|tomato+lentils|mixed"
        )
        val reordered = AiMenuHiveIdentity.from(
            AppLanguage.SPANISH,
            "bowl|lentejas+tomate|mixed"
        )

        assertEquals(first, reordered)
    }

    @Test
    fun `invalid semantic key is rejected`() {
        assertNull(AiMenuHiveIdentity.from(AppLanguage.SPANISH, "pasta con tomate"))
        assertNull(AiMenuHiveIdentity.from(AppLanguage.SPANISH, "||"))
    }

    @Test
    fun `profile key is stable and changes for safety restrictions`() {
        val base = DietaryProfile(ageRange = "18+ años")
        val vegan = base.copy(isVegan = true)

        val baseKey = AiMenuHiveIdentity.eligibilityKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT,
            base
        )
        val veganKey = AiMenuHiveIdentity.eligibilityKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT,
            vegan
        )

        assertEquals(
            baseKey,
            AiMenuHiveIdentity.eligibilityKey(
                AppLanguage.SPANISH,
                MealType.LUNCH,
                MenuAudience.ADULT,
                base
            )
        )
        assertNotEquals(baseKey, veganKey)
        assertTrue(baseKey.matches(Regex("[a-f0-9]{64}")))
    }

    private fun identityFixtures(): List<IdentityFixture> {
        val resource = requireNotNull(
            javaClass.classLoader?.getResourceAsStream(
                "ai-menu-hive-identity-v2-fixtures.tsv"
            )
        )
        return resource.bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { line ->
                val (language, rawKey, canonicalKey) = line.split('\t')
                IdentityFixture(
                    language = AppLanguage.valueOf(language),
                    rawKey = rawKey,
                    canonicalKey = canonicalKey
                )
            }.toList()
        }
    }
}

private data class IdentityFixture(
    val language: AppLanguage,
    val rawKey: String,
    val canonicalKey: String
)
