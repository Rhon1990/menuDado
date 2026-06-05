package com.menudado.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\t\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0010\u001a\u00020\u0011H\u0016J\n\u0010\u0012\u001a\u0004\u0018\u00010\u0013H\u0016J\u0010\u0010\u0014\u001a\u00020\u00112\u0006\u0010\u0015\u001a\u00020\u0013H\u0016R\u001a\u0010\u0003\u001a\u00020\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001e\u0010\t\u001a\u0004\u0018\u00010\nX\u0086\u000e\u00a2\u0006\u0010\n\u0002\u0010\u000f\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000e\u00a8\u0006\u0016"}, d2 = {"Lcom/menudado/ui/FakeAiQuotaRetryStore;", "Lcom/menudado/data/AiQuotaRetryStore;", "()V", "storedConsecutiveFailures", "", "getStoredConsecutiveFailures", "()I", "setStoredConsecutiveFailures", "(I)V", "storedRetryAtMillis", "", "getStoredRetryAtMillis", "()Ljava/lang/Long;", "setStoredRetryAtMillis", "(Ljava/lang/Long;)V", "Ljava/lang/Long;", "clearRetryState", "", "getRetryState", "Lcom/menudado/data/AiQuotaRetryState;", "saveRetryState", "state", "app_debugUnitTest"})
final class FakeAiQuotaRetryStore implements com.menudado.data.AiQuotaRetryStore {
    @org.jetbrains.annotations.Nullable()
    private java.lang.Long storedRetryAtMillis;
    private int storedConsecutiveFailures = 0;
    
    public FakeAiQuotaRetryStore() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getStoredRetryAtMillis() {
        return null;
    }
    
    public final void setStoredRetryAtMillis(@org.jetbrains.annotations.Nullable()
    java.lang.Long p0) {
    }
    
    public final int getStoredConsecutiveFailures() {
        return 0;
    }
    
    public final void setStoredConsecutiveFailures(int p0) {
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
}