package com.menudado.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GeneratedMenuParserTest {
    @Test
    fun `parsea menu generado con calorias`() {
        val generated = GeneratedMenuParser.parse(
            """
            {
              "name": "Tostada de aguacate y huevo",
              "description": "Pan integral, aguacate, huevo cocido y tomate.",
              "notes": "Usa pan integral y tomate de mercado.",
              "calories": 420
            }
            """.trimIndent()
        )

        assertEquals("Tostada de aguacate y huevo", generated.name)
        assertEquals("Pan integral, aguacate, huevo cocido y tomate.", generated.description)
        assertEquals("Usa pan integral y tomate de mercado.", generated.notes)
        assertEquals(420, generated.calories)
    }

    @Test
    fun `parsea menu generado con analisis saludable incluido`() {
        val generated = GeneratedMenuParser.parse(
            """
            {
              "name": "Bowl de lentejas",
              "description": "Lentejas, arroz integral, tomate y aguacate.",
              "notes": "Puedes usar lentejas cocidas.",
              "calories": 540,
              "health_status": "saludable",
              "health_reason": "Aporta fibra, proteina vegetal y grasas saludables.",
              "health_suggestion": "Ajusta la sal y agrega limon."
            }
            """.trimIndent()
        )

        assertEquals(HealthStatus.HEALTHY, generated.healthAnalysis?.status)
        assertEquals("Aporta fibra, proteina vegetal y grasas saludables.", generated.healthAnalysis?.reason)
        assertEquals("Ajusta la sal y agrega limon.", generated.healthAnalysis?.suggestion)
        assertEquals(540, generated.healthAnalysis?.calories)
    }

    @Test
    fun `parsea respuesta envuelta en markdown y calorias como texto`() {
        val generated = GeneratedMenuParser.parse(
            """
            Claro, aqui tienes una idea distinta:
            ```json
            {
              "name": "Ensalada tibia de lentejas",
              "description": "Lentejas, zanahoria, tomate, huevo cocido y aceite de oliva.",
              "notes": "Puedes usar lentejas cocidas de bote.",
              "calories": "510 kcal aprox."
            }
            ```
            """.trimIndent()
        )

        assertEquals("Ensalada tibia de lentejas", generated.name)
        assertEquals("Lentejas, zanahoria, tomate, huevo cocido y aceite de oliva.", generated.description)
        assertEquals("Puedes usar lentejas cocidas de bote.", generated.notes)
        assertEquals(510, generated.calories)
    }

    @Test
    fun `parsea valores con comillas escapadas`() {
        val generated = GeneratedMenuParser.parse(
            """
            {
              "name": "Bowl \"verde\" de pollo",
              "description": "Pollo, arroz integral y verduras.",
              "notes": "",
              "calories": 540
            }
            """.trimIndent()
        )

        assertEquals("Bowl \"verde\" de pollo", generated.name)
        assertEquals(540, generated.calories)
    }
}
