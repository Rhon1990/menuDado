package com.menudado.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \f2\u00020\u0001:\u0001\fB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\b\u001a\u00020\tH\u0016J\b\u0010\n\u001a\u00020\u000bH\u0016R\u0016\u0010\u0005\u001a\n \u0007*\u0004\u0018\u00010\u00060\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/menudado/data/SharedPreferencesOnboardingStore;", "Lcom/menudado/data/OnboardingStore;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "preferences", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "isOnboardingCompleted", "", "markOnboardingCompleted", "", "Companion", "app_debug"})
public final class SharedPreferencesOnboardingStore implements com.menudado.data.OnboardingStore {
    private final android.content.SharedPreferences preferences = null;
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PREFERENCES_NAME = "menu-dado-onboarding";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    @org.jetbrains.annotations.NotNull()
    private static final com.menudado.data.SharedPreferencesOnboardingStore.Companion Companion = null;
    
    public SharedPreferencesOnboardingStore(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @java.lang.Override()
    public boolean isOnboardingCompleted() {
        return false;
    }
    
    @java.lang.Override()
    public void markOnboardingCompleted() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/menudado/data/SharedPreferencesOnboardingStore$Companion;", "", "()V", "KEY_ONBOARDING_COMPLETED", "", "PREFERENCES_NAME", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
}