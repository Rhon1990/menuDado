package com.menudado.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthAnalysisParserTest {
    @Test
    fun `parsea una respuesta json saludable`() {
        val analysis = HealthAnalysisParser.parse(
            """
            {
              "status": "saludable",
              "reason": "Incluye proteina, grasas saludables y vegetales.",
              "suggestion": "Agrega una fruta si quieres mas fibra.",
              "calories": 520
            }
            """.trimIndent()
        )

        assertEquals(HealthStatus.HEALTHY, analysis.status)
        assertEquals("Incluye proteina, grasas saludables y vegetales.", analysis.reason)
        assertEquals("Agrega una fruta si quieres mas fibra.", analysis.suggestion)
        assertEquals(520, analysis.calories)
    }

    @Test
    fun `parsea calorias cuando vienen como texto`() {
        val analysis = HealthAnalysisParser.parse(
            """{"status":"intermedio","reason":"ok","suggestion":"ok","calories":"610 kcal aprox."}"""
        )

        assertEquals(610, analysis.calories)
    }

    @Test
    fun `parsea analisis por lote asociados por id`() {
        val results = HealthAnalysisParser.parseBatch(
            """
            {
              "results": [
                {
                  "id": 12,
                  "status": "saludable",
                  "reason": "Tiene vegetales y proteina.",
                  "suggestion": "Mantener porcion moderada.",
                  "calories": 480
                },
                {
                  "id": 18,
                  "status": "intermedio",
                  "reason": "Incluye carbohidrato refinado.",
                  "suggestion": "Cambiar por integral.",
                  "calories": "610 kcal aprox."
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(2, results.size)
        assertEquals(HealthStatus.HEALTHY, results[12L]?.status)
        assertEquals("Tiene vegetales y proteina.", results[12L]?.reason)
        assertEquals(480, results[12L]?.calories)
        assertEquals(HealthStatus.IMPROVABLE, results[18L]?.status)
        assertEquals("Cambiar por integral.", results[18L]?.suggestion)
        assertEquals(610, results[18L]?.calories)
    }

    @Test
    fun `normaliza estados mejorable y poco saludable`() {
        assertEquals(
            HealthStatus.IMPROVABLE,
            HealthAnalysisParser.parse("""{"status":"mejorable","reason":"ok","suggestion":"ok"}""").status
        )
        assertEquals(
            HealthStatus.IMPROVABLE,
            HealthAnalysisParser.parse("""{"status":"intermedio","reason":"ok","suggestion":"ok"}""").status
        )
        assertEquals(
            HealthStatus.UNHEALTHY,
            HealthAnalysisParser.parse("""{"status":"poco_saludable","reason":"ok","suggestion":"ok"}""").status
        )
    }

    @Test
    fun `usa fallback cuando la respuesta viene mal formada`() {
        val analysis = HealthAnalysisParser.parse("No puedo responder en JSON, pero parece equilibrado.")

        assertEquals(HealthStatus.UNKNOWN, analysis.status)
        assertEquals("No se pudo interpretar la respuesta de la IA.", analysis.reason)
        assertEquals("Revisa el menu manualmente o vuelve a intentar el analisis.", analysis.suggestion)
    }
}
