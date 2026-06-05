package com.menudado.domain;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J*\u0010\u0003\u001a\u00020\u00042\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\b\u0010\b\u001a\u0004\u0018\u00010\t2\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000bJ\u001e\u0010\f\u001a\u00020\r2\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\b\u0010\b\u001a\u0004\u0018\u00010\tJQ\u0010\u000e\u001a\u0004\u0018\u00010\u00072\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\b\u0010\b\u001a\u0004\u0018\u00010\t2\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000b2#\b\u0002\u0010\u000f\u001a\u001d\u0012\u0013\u0012\u00110\u0004\u00a2\u0006\f\b\u0011\u0012\b\b\u0012\u0012\u0004\b\b(\u0013\u0012\u0004\u0012\u00020\u00040\u0010\u00a8\u0006\u0014"}, d2 = {"Lcom/menudado/domain/DiceSelector;", "", "()V", "availableCandidateCount", "", "menus", "", "Lcom/menudado/domain/FoodMenu;", "filter", "Lcom/menudado/domain/MealType;", "today", "", "hasCandidates", "", "select", "nextIndex", "Lkotlin/Function1;", "Lkotlin/ParameterName;", "name", "bound", "app_debug"})
public final class DiceSelector {
    @org.jetbrains.annotations.NotNull()
    public static final com.menudado.domain.DiceSelector INSTANCE = null;
    
    private DiceSelector() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.menudado.domain.FoodMenu select(@org.jetbrains.annotations.NotNull()
    java.util.List<com.menudado.domain.FoodMenu> menus, @org.jetbrains.annotations.Nullable()
    com.menudado.domain.MealType filter, @org.jetbrains.annotations.Nullable()
    java.lang.String today, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, java.lang.Integer> nextIndex) {
        return null;
    }
    
    public final boolean hasCandidates(@org.jetbrains.annotations.NotNull()
    java.util.List<com.menudado.domain.FoodMenu> menus, @org.jetbrains.annotations.Nullable()
    com.menudado.domain.MealType filter) {
        return false;
    }
    
    public final int availableCandidateCount(@org.jetbrains.annotations.NotNull()
    java.util.List<com.menudado.domain.FoodMenu> menus, @org.jetbrains.annotations.Nullable()
    com.menudado.domain.MealType filter, @org.jetbrains.annotations.Nullable()
    java.lang.String today) {
        return 0;
    }
}