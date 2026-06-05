package com.menudado.domain;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0006\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/menudado/domain/AiQuotaLimitType;", "", "(Ljava/lang/String;I)V", "REQUESTS_PER_MINUTE", "TOKENS_PER_MINUTE", "REQUESTS_PER_DAY", "UNKNOWN", "app_debug"})
public enum AiQuotaLimitType {
    /*public static final*/ REQUESTS_PER_MINUTE /* = new REQUESTS_PER_MINUTE() */,
    /*public static final*/ TOKENS_PER_MINUTE /* = new TOKENS_PER_MINUTE() */,
    /*public static final*/ REQUESTS_PER_DAY /* = new REQUESTS_PER_DAY() */,
    /*public static final*/ UNKNOWN /* = new UNKNOWN() */;
    
    AiQuotaLimitType() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.menudado.domain.AiQuotaLimitType> getEntries() {
        return null;
    }
}