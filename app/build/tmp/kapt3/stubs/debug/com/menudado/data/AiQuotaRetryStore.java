package com.menudado.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\n\u0010\u0004\u001a\u0004\u0018\u00010\u0005H&J\u0010\u0010\u0006\u001a\u00020\u00032\u0006\u0010\u0007\u001a\u00020\u0005H&\u00a8\u0006\b"}, d2 = {"Lcom/menudado/data/AiQuotaRetryStore;", "", "clearRetryState", "", "getRetryState", "Lcom/menudado/data/AiQuotaRetryState;", "saveRetryState", "state", "app_debug"})
public abstract interface AiQuotaRetryStore {
    
    @org.jetbrains.annotations.Nullable()
    public abstract com.menudado.data.AiQuotaRetryState getRetryState();
    
    public abstract void saveRetryState(@org.jetbrains.annotations.NotNull()
    com.menudado.data.AiQuotaRetryState state);
    
    public abstract void clearRetryState();
}