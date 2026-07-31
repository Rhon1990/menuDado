package com.menudado.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseMenuDadoAnalyticsTest {

    @Test
    fun `onboarding version uses an alphanumeric app dimension value`() {
        assertEquals("v7", onboardingVersionDimensionValue(7))
    }
}
