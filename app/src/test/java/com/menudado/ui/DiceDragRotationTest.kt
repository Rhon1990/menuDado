package com.menudado.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DiceDragRotationTest {
    @Test
    fun `actualiza la rotacion del dado segun el arrastre del dedo`() {
        val rotation = DiceDragRotation(x = 10f, y = 20f)

        val updated = rotation.afterDrag(dragX = 12f, dragY = -8f)

        assertEquals(12f, updated.x, 0.001f)
        assertEquals(23f, updated.y, 0.001f)
    }

    @Test
    fun `normaliza angulos para evitar valores acumulados enormes`() {
        val rotation = DiceDragRotation(x = 178f, y = -179f)

        val updated = rotation.afterDrag(dragX = -16f, dragY = -16f)

        assertEquals(-178f, updated.x, 0.001f)
        assertEquals(177f, updated.y, 0.001f)
    }
}
