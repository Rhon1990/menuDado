package com.menudado.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\t\u001a\u00020\u0004H\u0016J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0004H\u0016R\u001a\u0010\u0003\u001a\u00020\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\b\u00a8\u0006\r"}, d2 = {"Lcom/menudado/ui/FakeDietaryProfileStore;", "Lcom/menudado/data/DietaryProfileStore;", "()V", "storedProfile", "Lcom/menudado/domain/DietaryProfile;", "getStoredProfile", "()Lcom/menudado/domain/DietaryProfile;", "setStoredProfile", "(Lcom/menudado/domain/DietaryProfile;)V", "getProfile", "saveProfile", "", "profile", "app_debugUnitTest"})
final class FakeDietaryProfileStore implements com.menudado.data.DietaryProfileStore {
    @org.jetbrains.annotations.NotNull()
    private com.menudado.domain.DietaryProfile storedProfile;
    
    public FakeDietaryProfileStore() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.menudado.domain.DietaryProfile getStoredProfile() {
        return null;
    }
    
    public final void setStoredProfile(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.DietaryProfile p0) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.menudado.domain.DietaryProfile getProfile() {
        return null;
    }
    
    @java.lang.Override()
    public void saveProfile(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.DietaryProfile profile) {
    }
}