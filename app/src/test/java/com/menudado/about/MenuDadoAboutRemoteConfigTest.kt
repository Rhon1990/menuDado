package com.menudado.about

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuDadoAboutRemoteConfigTest {
    private val fallback = MenuDadoAboutContent(
        description = "Descripcion local",
        createdBy = "Autor local",
        contact = "contacto@local.test"
    )

    @Test
    fun `remote config reemplaza descripcion creador y contacto de acerca`() {
        val content = MenuDadoAboutRemoteConfig.aboutContentFromRemoteValues(
            description = "Descripcion desde Firebase",
            createdBy = "Equipo MenuDado",
            contact = "hola@menudado.app",
            fallback = fallback
        )

        assertEquals("Descripcion desde Firebase", content.description)
        assertEquals("Equipo MenuDado", content.createdBy)
        assertEquals("hola@menudado.app", content.contact)
    }

    @Test
    fun `remote config vacio conserva los textos locales de acerca`() {
        val content = MenuDadoAboutRemoteConfig.aboutContentFromRemoteValues(
            description = "   ",
            createdBy = "",
            contact = null,
            fallback = fallback
        )

        assertEquals(fallback, content)
    }

    @Test
    fun `debug refresca acerca inmediatamente y release conserva cache`() {
        assertEquals(0L, MenuDadoAboutRemoteConfig.fetchIntervalSeconds(isDebugBuild = true))
        assertEquals(3_600L, MenuDadoAboutRemoteConfig.fetchIntervalSeconds(isDebugBuild = false))
    }

    @Test
    fun `descripcion renovada usa una clave remota versionada`() {
        assertEquals(
            "about_description_v3",
            MenuDadoAboutRemoteConfig.KEY_ABOUT_DESCRIPTION
        )
    }
}
