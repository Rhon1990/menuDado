package com.menudado.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000`\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010$\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J$\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000e2\u0006\u0010\u0010\u001a\u00020\nH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0011\u0010\u0012J6\u0010\u0013\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u000f0\u00140\u000e2\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\n0\tH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0016\u0010\u0017J\u0016\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u0010\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u0012J:\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u001b0\u000e2\u0006\u0010\u001c\u001a\u00020\u001d2\f\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u001f0\t2\u0006\u0010 \u001a\u00020!H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\"\u0010#J\u0016\u0010$\u001a\u00020\u00192\u0006\u0010\u0010\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u0012R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\f\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006%"}, d2 = {"Lcom/menudado/data/MenuRepository;", "", "menuDao", "Lcom/menudado/data/MenuDao;", "healthAnalyzer", "Lcom/menudado/ai/HealthAnalyzer;", "(Lcom/menudado/data/MenuDao;Lcom/menudado/ai/HealthAnalyzer;)V", "menus", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/menudado/domain/FoodMenu;", "getMenus", "()Lkotlinx/coroutines/flow/Flow;", "analyze", "Lkotlin/Result;", "Lcom/menudado/domain/HealthAnalysis;", "menu", "analyze-gIAlu-s", "(Lcom/menudado/domain/FoodMenu;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "analyzeBatch", "", "", "analyzeBatch-gIAlu-s", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "delete", "", "generateMenu", "Lcom/menudado/domain/GeneratedMenu;", "mealType", "Lcom/menudado/domain/MealType;", "avoidIdeas", "", "dietaryProfile", "Lcom/menudado/domain/DietaryProfile;", "generateMenu-BWLJW6A", "(Lcom/menudado/domain/MealType;Ljava/util/List;Lcom/menudado/domain/DietaryProfile;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "save", "app_debug"})
public final class MenuRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.menudado.data.MenuDao menuDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.menudado.ai.HealthAnalyzer healthAnalyzer = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.util.List<com.menudado.domain.FoodMenu>> menus = null;
    
    public MenuRepository(@org.jetbrains.annotations.NotNull()
    com.menudado.data.MenuDao menuDao, @org.jetbrains.annotations.NotNull()
    com.menudado.ai.HealthAnalyzer healthAnalyzer) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.menudado.domain.FoodMenu>> getMenus() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object save(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.FoodMenu menu, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object delete(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.FoodMenu menu, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}