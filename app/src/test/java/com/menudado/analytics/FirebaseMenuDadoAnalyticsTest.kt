package com.menudado.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseMenuDadoAnalyticsTest {

    @Test
    fun `onboarding version uses an alphanumeric app dimension value`() {
        assertEquals("v6", onboardingVersionDimensionValue(6))
    }
}
