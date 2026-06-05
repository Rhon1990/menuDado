package com.menudado.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0000\b\u0002\u0018\u00002\u00020\u0001B\u000f\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\b\u001a\u00020\u0003H\u0016J\b\u0010\t\u001a\u00020\nH\u0016R\u001a\u0010\u0002\u001a\u00020\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\u0004\u00a8\u0006\u000b"}, d2 = {"Lcom/menudado/ui/FakeOnboardingStore;", "Lcom/menudado/data/OnboardingStore;", "completed", "", "(Z)V", "getCompleted", "()Z", "setCompleted", "isOnboardingCompleted", "markOnboardingCompleted", "", "app_debugUnitTest"})
final class FakeOnboardingStore implements com.menudado.data.OnboardingStore {
    private boolean completed;
    
    public FakeOnboardingStore(boolean completed) {
        super();
    }
    
    public final boolean getCompleted() {
        return false;
    }
    
    public final void setCompleted(boolean p0) {
    }
    
    @java.lang.Override()
    public boolean isOnboardingCompleted() {
        return false;
    }
    
    @java.lang.Override()
    public void markOnboardingCompleted() {
    }
    
    public FakeOnboardingStore() {
        super();
    }
}