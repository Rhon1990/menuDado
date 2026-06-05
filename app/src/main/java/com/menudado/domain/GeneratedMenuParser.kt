package com.menudado.domain

object GeneratedMenuParser {
    private val quotedField = """"%s"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
    private val intField = """"%s"\s*:\s*(\d+)""".toRegex()

    fun parse(rawText: String): GeneratedMenu {
        val jsonLikeText = rawText.jsonLikeText()
        val name = jsonLikeText.quoted("name")
        val description = jsonLikeText.quoted("description")
        val notes = jsonLikeText.quoted("notes").orEmpty()
        val calories = jsonLikeText.int("calories") ?: jsonLikeText.quoted("calories")?.firstNumber()
        val healthAnalysis = jsonLikeText.healthAnalysis(calories)

        if (name.isNullOrBlank() || description.isNullOrBlank() || calories == null) {
            throw IllegalArgumentException("No se pudo interpretar el menu generado por IA.")
        }

        return GeneratedMenu(
            name = name,
            description = description,
            notes = notes,
            calories = calories,
            healthAnalysis = healthAnalysis
        )
    }

    private fun String.healthAnalysis(calories: Int?): HealthAnalysis? {
        val status = quoted("health_status")?.toHealthStatus()
        val reason = quoted("health_reason")
        val suggestion = quoted("health_suggestion")

        if (status == null || reason.isNullOrBlank() || suggestion.isNullOrBlank()) {
            return null
        }

        return HealthAnalysis(
            status = status,
            reason = reason,
            suggestion = suggestion,
            calories = calories
        )
    }

    private fun String.quoted(name: String): String? {
        return Regex(quotedField.pattern.format(name), RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.unescapeJsonString()
    }

    private fun String.int(name: String): Int? {
        return Regex(intField.pattern.format(name), RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private fun String.jsonLikeText(): String {
        val firstBrace = indexOf('{')
        val lastBrace = lastIndexOf('}')
        return if (firstBrace >= 0 && lastBrace > firstBrace) {
            substring(firstBrace, lastBrace + 1)
        } else {
            this
        }
    }

    private fun String.firstNumber(): Int? {
        return Regex("""\d+""")
            .find(this)
            ?.value
            ?.toIntOrNull()
    }

    private fun String.unescapeJsonString(): String {
        val decoded = StringBuilder(length)
        var index = 0
        while (index < length) {
            val char = this[index]
            if (char == '\\' && index + 1 < length) {
                val escaped = this[index + 1]
                decoded.append(
                    when (escaped) {
                        '"' -> '"'
                        '\\' -> '\\'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        else -> escaped
                    }
                )
                index += 2
            } else {
                decoded.append(char)
                index += 1
            }
        }
        return decoded.toString().trim()
    }

    private fun String.toHealthStatus(): HealthStatus? {
        return lowercase()
            .replace("-", "_")
            .replace(" ", "_")
            .let { normalized ->
                when (normalized) {
                    "saludable", "healthy" -> HealthStatus.HEALTHY
                    "intermedio", "mejorable", "improvable" -> HealthStatus.IMPROVABLE
                    "no_saludable", "poco_saludable", "poco__saludable", "unhealthy" -> HealthStatus.UNHEALTHY
                    else -> null
                }
            }
    }
}
