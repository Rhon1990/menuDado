package com.menudado.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0019\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J>\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u00052\b\u0010\u000b\u001a\u0004\u0018\u00010\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0005H\u0016J\"\u0010\u0014\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u00052\b\u0010\u000b\u001a\u0004\u0018\u00010\f2\u0006\u0010\u0015\u001a\u00020\u0010H\u0016J\u0010\u0010\u0016\u001a\u00020\t2\u0006\u0010\u0017\u001a\u00020\u0005H\u0016J,\u0010\u0018\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0005H\u0016J\u0018\u0010\u0019\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\u001a\u001a\u00020\u0010H\u0016J\u0012\u0010\u001b\u001a\u00020\t2\b\u0010\u001c\u001a\u0004\u0018\u00010\u001dH\u0016J\u001a\u0010\u001e\u001a\u00020\t2\b\u0010\u001f\u001a\u0004\u0018\u00010\f2\u0006\u0010 \u001a\u00020\u0010H\u0016J\u001a\u0010!\u001a\u00020\t2\b\u0010\u001f\u001a\u0004\u0018\u00010\f2\u0006\u0010\u0015\u001a\u00020\u0010H\u0016J,\u0010\"\u001a\u00020\t2\b\u0010\u001f\u001a\u0004\u0018\u00010\f2\b\u0010#\u001a\u0004\u0018\u00010\f2\u0006\u0010\u0015\u001a\u00020\u00102\u0006\u0010 \u001a\u00020\u0010H\u0016J\u0010\u0010$\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\fH\u0016J\u0018\u0010%\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010&\u001a\u00020\u000eH\u0016J \u0010\'\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010(\u001a\u00020\u000e2\u0006\u0010\u0015\u001a\u00020\u0010H\u0016J\u0018\u0010)\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010*\u001a\u00020\u000eH\u0016J\u0018\u0010+\u001a\u00020\t2\u0006\u0010,\u001a\u00020\u00052\u0006\u0010\u000b\u001a\u00020\fH\u0016J \u0010-\u001a\u00020\t2\u0006\u0010\u0015\u001a\u00020\u00102\u0006\u0010.\u001a\u00020\u00102\u0006\u0010/\u001a\u00020\u0010H\u0016J \u00100\u001a\u00020\t2\u0006\u00101\u001a\u00020\u00052\u0006\u00102\u001a\u00020\u000e2\u0006\u00103\u001a\u00020\u000eH\u0016J(\u00104\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010(\u001a\u00020\u000e2\u0006\u00105\u001a\u00020\u000e2\u0006\u0010\u0015\u001a\u00020\u0010H\u0016R\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u00066"}, d2 = {"Lcom/menudado/ui/RecordingMenuDadoAnalytics;", "Lcom/menudado/analytics/MenuDadoAnalytics;", "()V", "events", "", "", "getEvents", "()Ljava/util/List;", "trackAiAnalysisFinished", "", "scope", "mealType", "Lcom/menudado/domain/MealType;", "success", "", "analyzedCount", "", "healthStatus", "Lcom/menudado/domain/HealthStatus;", "failureType", "trackAiAnalysisStarted", "menuCount", "trackAiDailyLimitReached", "source", "trackAiMenuGenerationFinished", "trackAiMenuGenerationStarted", "avoidIdeaCount", "trackAppOpened", "deviceInfo", "Lcom/menudado/analytics/DeviceInfo;", "trackDiceEmptyResult", "filter", "availableCandidateCount", "trackDiceFilterSelected", "trackDiceRolled", "resultMealType", "trackFirstMenuCreated", "trackMealTypeSelected", "formHasContent", "trackMenuCardOpened", "hasAiAnalysis", "trackMenuDeleted", "hadAiAnalysis", "trackMenuFormStarted", "firstEditedField", "trackMenuInventoryChanged", "analyzedMenuCount", "pendingAnalysisCount", "trackMenuSaveBlocked", "reason", "hasName", "hasDescription", "trackMenuSaved", "hasCalories", "app_debugUnitTest"})
final class RecordingMenuDadoAnalytics implements com.menudado.analytics.MenuDadoAnalytics {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> events = null;
    
    public RecordingMenuDadoAnalytics() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getEvents() {
        return null;
    }
    
    @java.lang.Override()
    public void trackAppOpened(@org.jetbrains.annotations.Nullable()
    com.menudado.analytics.DeviceInfo deviceInfo) {
    }
    
    @java.lang.Override()
    public void trackMenuSaved(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.MealType mealType, boolean hasAiAnalysis, boolean hasCalories, int menuCount) {
    }
    
    @java.lang.Override()
    public void trackMenuDeleted(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.MealType mealType, boolean hadAiAnalysis) {
    }
    
    @java.lang.Override()
    public void trackFirstMenuCreated(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.MealType mealType) {
    }
    
    @java.lang.Override()
    public void trackMenuInventoryChanged(int menuCount, int analyzedMenuCount, int pendingAnalysisCount) {
    }
    
    @java.lang.Override()
    public void trackMenuFormStarted(@org.jetbrains.annotations.NotNull()
    java.lang.String firstEditedField, @org.jetbrains.annotations.NotNull()
    com.menudado.domain.MealType mealType) {
    }
    
    @java.lang.Override()
    public void trackMealTypeSelected(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.MealType mealType, boolean formHasContent) {
    }
    
    @java.lang.Override()
    public void trackMenuSaveBlocked(@org.jetbrains.annotations.NotNull()
    java.lang.String reason, boolean hasName, boolean hasDescription) {
    }
    
    @java.lang.Override()
    public void trackDiceRolled(@org.jetbrains.annotations.Nullable()
    com.menudado.domain.MealType filter, @org.jetbrains.annotations.Nullable()
    com.menudado.domain.MealType resultMealType, int menuCount, int availableCandidateCount) {
    }
    
    @java.lang.Override()
    public void trackDiceFilterSelected(@org.jetbrains.annotations.Nullable()
    com.menudado.domain.MealType filter, int menuCount) {
    }
    
    @java.lang.Override()
    public void trackDiceEmptyResult(@org.jetbrains.annotations.Nullable()
    com.menudado.domain.MealType filter, int availableCandidateCount) {
    }
    
    @java.lang.Override()
    public void trackMenuCardOpened(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.MealType mealType, boolean hasAiAnalysis, int menuCount) {
    }
    
    @java.lang.Override()
    public void trackAiMenuGenerationStarted(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.MealType mealType, int avoidIdeaCount) {
    }
    
    @java.lang.Override()
    public void trackAiMenuGenerationFinished(@org.jetbrains.annotations.NotNull()
    com.menudado.domain.MealType mealType, boolean success, @org.jetbrains.annotations.Nullable()
    com.menudado.domain.HealthStatus healthStatus, @org.jetbrains.annotations.Nullable()
    java.lang.String failureType) {
    }
    
    @java.lang.Override()
    public void trackAiAnalysisStarted(@org.jetbrains.annotations.NotNull()
    java.lang.String scope, @org.jetbrains.annotations.Nullable()
    com.menudado.domain.MealType mealType, int menuCount) {
    }
    
    @java.lang.Override()
    public void trackAiAnalysisFinished(@org.jetbrains.annotations.NotNull()
    java.lang.String scope, @org.jetbrains.annotations.Nullable()
    com.menudado.domain.MealType mealType, boolean success, int analyzedCount, @org.jetbrains.annotations.Nullable()
    com.menudado.domain.HealthStatus healthStatus, @org.jetbrains.annotations.Nullable()
    java.lang.String failureType) {
    }
    
    @java.lang.Override()
    public void trackAiDailyLimitReached(@org.jetbrains.annotations.NotNull()
    java.lang.String source) {
    }
}