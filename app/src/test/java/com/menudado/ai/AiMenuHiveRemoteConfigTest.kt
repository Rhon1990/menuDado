package com.menudado.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMenuHiveRemoteConfigTest {
    @Test
    fun `hive remote config uses free shared refresh policy`() {
        assertEquals("ai_menu_hive_enabled", AiMenuHiveRemoteConfig.KEY_AI_MENU_HIVE_ENABLED)
        assertEquals(0L, AiMenuHiveRemoteConfig.fetchIntervalSeconds(isDebugBuild = true))
        assertEquals(3_600L, AiMenuHiveRemoteConfig.fetchIntervalSeconds(isDebugBuild = false))
        assertTrue(AiMenuHiveRemoteConfig.DEFAULT_AI_MENU_HIVE_ENABLED)
    }
}
