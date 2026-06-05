package com.menudado.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\n\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u0016J\u0010\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0010H\u0016R\u001c\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001a\u0010\t\u001a\u00020\nX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000e\u00a8\u0006\u0014"}, d2 = {"Lcom/menudado/ui/FakeAiDailyUsageStore;", "Lcom/menudado/data/AiDailyUsageStore;", "()V", "storedDateKey", "", "getStoredDateKey", "()Ljava/lang/String;", "setStoredDateKey", "(Ljava/lang/String;)V", "storedUsedCount", "", "getStoredUsedCount", "()I", "setStoredUsedCount", "(I)V", "getUsageState", "Lcom/menudado/data/AiDailyUsageState;", "saveUsageState", "", "state", "app_debugUnitTest"})
final class FakeAiDailyUsageStore implements com.menudado.data.AiDailyUsageStore {
    @org.jetbrains.annotations.Nullable()
    private java.lang.String storedDateKey;
    private int storedUsedCount = 0;
    
    public FakeAiDailyUsageStore() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getStoredDateKey() {
        return null;
    }
    
    public final void setStoredDateKey(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final int getStoredUsedCount() {
        return 0;
    }
    
    public final void setStoredUsedCount(int p0) {
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