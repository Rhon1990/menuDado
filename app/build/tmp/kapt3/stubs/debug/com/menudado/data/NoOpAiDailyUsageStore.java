package com.menudado.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\n\u0010\u0003\u001a\u0004\u0018\u00010\u0004H\u0016J\u0010\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0004H\u0016\u00a8\u0006\b"}, d2 = {"Lcom/menudado/data/NoOpAiDailyUsageStore;", "Lcom/menudado/data/AiDailyUsageStore;", "()V", "getUsageState", "Lcom/menudado/data/AiDailyUsageState;", "saveUsageState", "", "state", "app_debug"})
public final class NoOpAiDailyUsageStore implements com.menudado.data.AiDailyUsageStore {
    @org.jetbrains.annotations.NotNull()
    public static final com.menudado.data.NoOpAiDailyUsageStore INSTANCE = null;
    
    private NoOpAiDailyUsageStore() {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public com.menudado.data.AiDailyUsageState getUsageState() {
        return null;
    }
    
    @java.lang.Override()
    public void saveUsageState(@org.jetbrains.annotations.NotNull()
    com.menudado.data.AiDailyUsageState state) {
    }
}