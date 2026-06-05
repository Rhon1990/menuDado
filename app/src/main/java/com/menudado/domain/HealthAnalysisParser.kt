package com.menudado.domain

object HealthAnalysisParser {
    private val quotedField = """"%s"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
    private val intField = """"%s"\s*:\s*(\d+)""".toRegex()
    private val longField = """"%s"\s*:\s*(\d+)""".toRegex()

    fun parse(rawText: String): HealthAnalysis {
        val jsonLikeText = rawText.jsonLikeText()
        val status = jsonLikeText.field("status")?.toHealthStatus()
        val reason = jsonLikeText.field("reason")
        val suggestion = jsonLikeText.field("suggestion")
        val calories = jsonLikeText.int("calories") ?: jsonLikeText.field("calories")?.firstNumber()

        if (status == null || reason.isNullOrBlank() || suggestion.isNullOrBlank()) {
            return HealthAnalysis(
                status = HealthStatus.UNKNOWN,
                reason = "No se pudo interpretar la respuesta de la IA.",
                suggestion = "Revisa el menu manualmente o vuelve a intentar el analisis."
            )
        }

        return HealthAnalysis(
            status = status,
            reason = reason,
            suggestion = suggestion,
            calories = calories
        )
    }

    fun parseBatch(rawText: String): Map<Long, HealthAnalysis> {
        return rawText.jsonObjects()
            .mapNotNull { jsonObject ->
                val id = jsonObject.long("id") ?: return@mapNotNull null
                val analysis = parse(jsonObject)
                if (analysis.status == HealthStatus.UNKNOWN) {
                    null
                } else {
                    id to analysis
                }
            }
            .toMap()
    }

    private fun String.field(name: String): String? {
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

    private fun String.long(name: String): Long? {
        return Regex(longField.pattern.format(name), RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
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

    private fun String.jsonObjects(): List<String> {
        val source = resultsArrayText()
        val objects = mutableListOf<String>()
        var startIndex = -1
        var depth = 0
        var inString = false
        var escaped = false

        source.forEachIndexed { index, char ->
            when {
                escaped -> escaped = false
                char == '\\' && inString -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> {
                    if (depth == 0) startIndex = index
                    depth += 1
                }
                !inString && char == '}' -> {
                    depth -= 1
                    if (depth == 0 && startIndex >= 0) {
                        objects += source.substring(startIndex, index + 1)
                        startIndex = -1
                    }
                }
            }
        }

        return objects.filter { it.long("id") != null }
    }

    private fun String.resultsArrayText(): String {
        val resultsIndex = indexOf("\"results\"", ignoreCase = true)
        val arrayStart = if (resultsIndex >= 0) indexOf('[', startIndex = resultsIndex) else indexOf('[')
        if (arrayStart < 0) return this

        var depth = 0
        var inString = false
        var escaped = false
        for (index in arrayStart until length) {
            val char = this[index]
            when {
                escaped -> escaped = false
                char == '\\' && inString -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '[' -> depth += 1
                !inString && char == ']' -> {
                    depth -= 1
                    if (depth == 0) {
                        return substring(arrayStart + 1, index)
                    }
                }
            }
        }

        return this
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
