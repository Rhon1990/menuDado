package com.menudado.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test
    fun `detecta idioma soportado desde locale del dispositivo`() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLocale(Locale("es", "ES")))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLocale(Locale("en", "US")))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromLocale(Locale("fr", "FR")))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLocale(Locale("de", "DE")))
    }

    @Test
    fun `usa etiquetas inclusivas para publico en cada idioma`() {
        assertEquals("Persona adulta", MenuAudience.ADULT.localizedLabel(AppLanguage.SPANISH))
        assertEquals("Peques", MenuAudience.CHILD.localizedLabel(AppLanguage.SPANISH))
        assertEquals("Bebé", MenuAudience.BABY.localizedLabel(AppLanguage.SPANISH))

        assertEquals("Adult", MenuAudience.ADULT.localizedLabel(AppLanguage.ENGLISH))
        assertEquals("Kids", MenuAudience.CHILD.localizedLabel(AppLanguage.ENGLISH))
        assertEquals("Baby", MenuAudience.BABY.localizedLabel(AppLanguage.ENGLISH))

        assertEquals("Adulte", MenuAudience.ADULT.localizedLabel(AppLanguage.FRENCH))
        assertEquals("Enfants", MenuAudience.CHILD.localizedLabel(AppLanguage.FRENCH))
        assertEquals("Bébé", MenuAudience.BABY.localizedLabel(AppLanguage.FRENCH))
    }
}
