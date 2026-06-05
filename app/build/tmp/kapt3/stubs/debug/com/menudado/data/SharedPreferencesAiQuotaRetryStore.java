package com.menudado.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u0000 \u000e2\u00020\u0001:\u0001\u000eB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\b\u001a\u00020\tH\u0016J\n\u0010\n\u001a\u0004\u0018\u00010\u000bH\u0016J\u0010\u0010\f\u001a\u00020\t2\u0006\u0010\r\u001a\u00020\u000bH\u0016R\u0016\u0010\u0005\u001a\n \u0007*\u0004\u0018\u00010\u00060\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/menudado/data/SharedPreferencesAiQuotaRetryStore;", "Lcom/menudado/data/AiQuotaRetryStore;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "preferences", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "clearRetryState", "", "getRetryState", "Lcom/menudado/data/AiQuotaRetryState;", "saveRetryState", "state", "Companion", "app_debug"})
public final class SharedPreferencesAiQuotaRetryStore implements com.menudado.data.AiQuotaRetryStore {
    private final android.content.SharedPreferences preferences = null;
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PREFERENCES_NAME = "menu-dado-ai-quota";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_RETRY_AT_MILLIS = "retry_at_millis";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_CONSECUTIVE_FAILURES = "consecutive_failures";
    @org.jetbrains.annotations.NotNull()
    private static final com.menudado.data.SharedPreferencesAiQuotaRetryStore.Companion Companion = null;
    
    public SharedPreferencesAiQuotaRetryStore(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public com.menudado.data.AiQuotaRetryState getRetryState() {
        return null;
    }
    
    @java.lang.Override()
    public void saveRetryState(@org.jetbrains.annotations.NotNull()
    com.menudado.data.AiQuotaRetryState state) {
    }
    
    @java.lang.Override()
    public void clearRetryState() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/menudado/data/SharedPreferencesAiQuotaRetryStore$Companion;", "", "()V", "KEY_CONSECUTIVE_FAILURES", "", "KEY_RETRY_AT_MILLIS", "PREFERENCES_NAME", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
}