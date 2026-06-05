package com.menudado.ai;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010$\n\u0002\u0010\t\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u0003\n\u0002\b\u0005\b\u0007\u0018\u0000 #2\u00020\u0001:\u0001#B\u0005\u00a2\u0006\u0002\u0010\u0002J$\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0006\u001a\u00020\u0007H\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\b\u0010\tJ6\u0010\n\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\u00050\u000b0\u00042\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00070\u000eH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u000f\u0010\u0010J:\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\u00042\u0006\u0010\u0013\u001a\u00020\u00142\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\u000e2\u0006\u0010\u0017\u001a\u00020\u0018H\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0019\u0010\u001aJ\u0018\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u00162\u0006\u0010\u001e\u001a\u00020\u001fH\u0002J\f\u0010 \u001a\u00020\u0016*\u00020\u0016H\u0002J\u0012\u0010!\u001a\u00020\u0016*\b\u0012\u0004\u0012\u00020\u00070\u000eH\u0002J\f\u0010\"\u001a\u00020\u0016*\u00020\u0007H\u0002\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006$"}, d2 = {"Lcom/menudado/ai/FirebaseHealthAnalyzer;", "Lcom/menudado/ai/HealthAnalyzer;", "()V", "analyze", "Lkotlin/Result;", "Lcom/menudado/domain/HealthAnalysis;", "menu", "Lcom/menudado/domain/FoodMenu;", "analyze-gIAlu-s", "(Lcom/menudado/domain/FoodMenu;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "analyzeBatch", "", "", "menus", "", "analyzeBatch-gIAlu-s", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "generateMenu", "Lcom/menudado/domain/GeneratedMenu;", "mealType", "Lcom/menudado/domain/MealType;", "avoidIdeas", "", "dietaryProfile", "Lcom/menudado/domain/DietaryProfile;", "generateMenu-BWLJW6A", "(Lcom/menudado/domain/MealType;Ljava/util/List;Lcom/menudado/domain/DietaryProfile;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "logAiFailure", "", "operation", "error", "", "promptSafe", "toBatchPrompt", "toPrompt", "Companion", "app_debug"})
public final class FirebaseHealthAnalyzer implements com.menudado.ai.HealthAnalyzer {
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String TAG = "FirebaseHealthAnalyzer";
    @org.jetbrains.annotations.NotNull()
    private static final com.menudado.ai.FirebaseHealthAnalyzer.Companion Companion = null;
    
    public FirebaseHealthAnalyzer() {
        super();
    }
    
    private final void logAiFailure(java.lang.String operation, java.lang.Throwable error) {
    }
    
    private final java.lang.String toPrompt(com.menudado.domain.FoodMenu $this$toPrompt) {
        return null;
    }
    
    private final java.lang.String toBatchPrompt(java.util.List<com.menudado.domain.FoodMenu> $this$toBatchPrompt) {
        return null;
    }
    
    private final java.lang.String promptSafe(java.lang.String $this$promptSafe) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/menudado/ai/FirebaseHealthAnalyzer$Companion;", "", "()V", "TAG", "", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
}