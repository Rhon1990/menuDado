package com.menudado.domain

enum class AiQuotaLimitType {
    REQUESTS_PER_MINUTE,
    TOKENS_PER_MINUTE,
    REQUESTS_PER_DAY,
    UNKNOWN
}

fun Throwable.isAiQuotaExceeded(): Boolean {
    return this::class.simpleName == "QuotaExceededException" ||
        isAiQuotaExceededText(listOfNotNull(message, cause?.message).joinToString(" "))
}

fun isAiQuotaExceededText(text: String): Boolean {
    val normalized = text.lowercase()
    return "quota exceeded" in normalized ||
        "exceeded your current quota" in normalized ||
        "resource_exhausted" in normalized
}

fun classifyAiQuotaLimitType(text: String): AiQuotaLimitType {
    val normalized = text.lowercase()
    return when {
        normalized.contains("request") &&
            (normalized.contains("per day") || normalized.contains("rpd") || normalized.contains("daily")) -> AiQuotaLimitType.REQUESTS_PER_DAY
        normalized.contains("token") ||
            normalized.contains("tpm") ||
            normalized.contains("input_token") -> AiQuotaLimitType.TOKENS_PER_MINUTE
        normalized.contains("request") ||
            normalized.contains("free_tier_requests") -> AiQuotaLimitType.REQUESTS_PER_MINUTE
        else -> AiQuotaLimitType.UNKNOWN
    }
}
