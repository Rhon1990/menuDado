package com.menudado.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0016J\b\u0010\u0005\u001a\u00020\u0006H\u0016\u00a8\u0006\u0007"}, d2 = {"Lcom/menudado/data/NoOpOnboardingStore;", "Lcom/menudado/data/OnboardingStore;", "()V", "isOnboardingCompleted", "", "markOnboardingCompleted", "", "app_debug"})
public final class NoOpOnboardingStore implements com.menudado.data.OnboardingStore {
    @org.jetbrains.annotations.NotNull()
    public static final com.menudado.data.NoOpOnboardingStore INSTANCE = null;
    
    private NoOpOnboardingStore() {
        super();
    }
    
    @java.lang.Override()
    public boolean isOnboardingCompleted() {
        return false;
    }
    
    @java.lang.Override()
    public void markOnboardingCompleted() {
    }
}