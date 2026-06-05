package com.menudado.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\b\u0007\u0018\u0000 \r2\u00020\u0001:\u0001\rB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\b\u001a\u00020\tH\u0016J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\tH\u0016R\u0016\u0010\u0005\u001a\n \u0007*\u0004\u0018\u00010\u00060\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lcom/menudado/data/SharedPreferencesDietaryProfileStore;", "Lcom/menudado/data/DietaryProfileStore;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "preferences", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "getProfile", "Lcom/menudado/domain/DietaryProfile;", "saveProfile", "", "profile", "Companion", "app_debug"})
public final class SharedPreferencesDietaryProfileStore implements com.menudado.data.DietaryProfileStore {
    private final android.content.SharedPreferences preferences = null;
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PREFERENCES_NAME = "menu-dado-dietary-profile";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_IS_VEGAN = "is_vegan";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_HAS_ALLERGIES = "has_allergies";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_ALLERGENS = "allergens";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_OTHER_AVOIDANCES = "other_avoidances";
    @org.jetbrains.annotations.NotNull()
    private static final com.menudado.data.SharedPreferencesDietaryProfileStore.Companion Companion = null;
    
    public SharedPreferencesDietaryProfileStore(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/menudado/data/SharedPreferencesDietaryProfileStore$Companion;", "", "()V", "KEY_ALLERGENS", "", "KEY_HAS_ALLERGIES", "KEY_IS_VEGAN", "KEY_OTHER_AVOIDANCES", "PREFERENCES_NAME", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
}