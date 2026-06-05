package com.menudado.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiFailureClassifierTest {
    @Test
    fun `detects quota exceeded messages from Gemini`() {
        assertTrue(
            isAiQuotaExceededText(
                "Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests"
            )
        )
        assertTrue(
            isAiQuotaExceededText("RESOURCE_EXHAUSTED. Please retry in 45.297396107s.")
        )
    }

    @Test
    fun `does not classify generic network failures as quota exceeded`() {
        assertFalse(isAiQuotaExceededText("timeout while connecting to Firebase"))
        assertFalse(isAiQuotaExceededText("api key not valid"))
        assertFalse(isAiQuotaExceededText("quota project configuration loaded"))
    }

    @Test
    fun `classifies quota limit type from Gemini messages`() {
        assertEquals(
            AiQuotaLimitType.REQUESTS_PER_MINUTE,
            classifyAiQuotaLimitType(
                "Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 20. Please retry in 21s."
            )
        )
        assertEquals(
            AiQuotaLimitType.TOKENS_PER_MINUTE,
            classifyAiQuotaLimitType(
                "RESOURCE_EXHAUSTED quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_input_tokens"
            )
        )
        assertEquals(
            AiQuotaLimitType.REQUESTS_PER_DAY,
            classifyAiQuotaLimitType("Quota exceeded for metric: Requests per day, limit: 20")
        )
    }
}
