package com.menudado;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002R\u001b\u0010\u0003\u001a\u00020\u00048FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0007\u0010\b\u001a\u0004\b\u0005\u0010\u0006R\u001b\u0010\t\u001a\u00020\n8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\b\u001a\u0004\b\u000b\u0010\fR\u001b\u0010\u000e\u001a\u00020\u000f8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0012\u0010\b\u001a\u0004\b\u0010\u0010\u0011R\u001b\u0010\u0013\u001a\u00020\u00148BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0017\u0010\b\u001a\u0004\b\u0015\u0010\u0016R\u001b\u0010\u0018\u001a\u00020\u00198FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001c\u0010\b\u001a\u0004\b\u001a\u0010\u001bR\u000e\u0010\u001d\u001a\u00020\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u001f\u001a\u00020 8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b#\u0010\b\u001a\u0004\b!\u0010\"R\u001b\u0010$\u001a\u00020%8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b(\u0010\b\u001a\u0004\b&\u0010\'\u00a8\u0006)"}, d2 = {"Lcom/menudado/MenuDadoApplication;", "Landroid/app/Application;", "()V", "aiDailyUsageStore", "Lcom/menudado/data/AiDailyUsageStore;", "getAiDailyUsageStore", "()Lcom/menudado/data/AiDailyUsageStore;", "aiDailyUsageStore$delegate", "Lkotlin/Lazy;", "aiQuotaRetryStore", "Lcom/menudado/data/AiQuotaRetryStore;", "getAiQuotaRetryStore", "()Lcom/menudado/data/AiQuotaRetryStore;", "aiQuotaRetryStore$delegate", "analytics", "Lcom/menudado/analytics/MenuDadoAnalytics;", "getAnalytics", "()Lcom/menudado/analytics/MenuDadoAnalytics;", "analytics$delegate", "database", "Lcom/menudado/data/MenuDadoDatabase;", "getDatabase", "()Lcom/menudado/data/MenuDadoDatabase;", "database$delegate", "dietaryProfileStore", "Lcom/menudado/data/DietaryProfileStore;", "getDietaryProfileStore", "()Lcom/menudado/data/DietaryProfileStore;", "dietaryProfileStore$delegate", "migration1To2", "Landroidx/room/migration/Migration;", "onboardingStore", "Lcom/menudado/data/OnboardingStore;", "getOnboardingStore", "()Lcom/menudado/data/OnboardingStore;", "onboardingStore$delegate", "repository", "Lcom/menudado/data/MenuRepository;", "getRepository", "()Lcom/menudado/data/MenuRepository;", "repository$delegate", "app_debug"})
public final class MenuDadoApplication extends android.app.Application {
    @org.jetbrains.annotations.NotNull()
    private final androidx.room.migration.Migration migration1To2 = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy database$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy repository$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy aiQuotaRetryStore$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy aiDailyUsageStore$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy dietaryProfileStore$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy onboardingStore$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy analytics$delegate = null;
    
    public MenuDadoApplication() {
        super();
    }
    
    private final com.menudado.data.MenuDadoDatabase getDatabase() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.menudado.data.MenuRepository getRepository() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.menudado.data.AiQuotaRetryStore getAiQuotaRetryStore() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.menudado.data.AiDailyUsageStore getAiDailyUsageStore() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.menudado.data.DietaryProfileStore getDietaryProfileStore() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.menudado.data.OnboardingStore getOnboardingStore() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.menudado.analytics.MenuDadoAnalytics getAnalytics() {
        return null;
    }
}