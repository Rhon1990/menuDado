package com.menudado.analytics;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b&\b\u0007\u0018\u0000 G2\u00020\u0001:\u0001GB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J+\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0019\b\u0002\u0010\t\u001a\u0013\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00060\n\u00a2\u0006\u0002\b\fH\u0002J\b\u0010\r\u001a\u00020\u0006H\u0016J>\u0010\u000e\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\b2\b\u0010\u0010\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\bH\u0016J\"\u0010\u0019\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\b2\b\u0010\u0010\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u001a\u001a\u00020\u0015H\u0016J\u0010\u0010\u001b\u001a\u00020\u00062\u0006\u0010\u001c\u001a\u00020\bH\u0016J,\u0010\u001d\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\b\u0010\u0016\u001a\u0004\u0018\u00010\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\bH\u0016J\u0018\u0010\u001e\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u001f\u001a\u00020\u0015H\u0016J\u0012\u0010 \u001a\u00020\u00062\b\u0010!\u001a\u0004\u0018\u00010\"H\u0016J\u001a\u0010#\u001a\u00020\u00062\b\u0010$\u001a\u0004\u0018\u00010\u00112\u0006\u0010%\u001a\u00020\u0015H\u0016J\u001a\u0010&\u001a\u00020\u00062\b\u0010$\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u001a\u001a\u00020\u0015H\u0016J,\u0010\'\u001a\u00020\u00062\b\u0010$\u001a\u0004\u0018\u00010\u00112\b\u0010(\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u001a\u001a\u00020\u00152\u0006\u0010%\u001a\u00020\u0015H\u0016J\u0010\u0010)\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u0011H\u0016J\u0018\u0010*\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010+\u001a\u00020\u0013H\u0016J \u0010,\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010-\u001a\u00020\u00132\u0006\u0010\u001a\u001a\u00020\u0015H\u0016J\u0018\u0010.\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010/\u001a\u00020\u0013H\u0016J\u0018\u00100\u001a\u00020\u00062\u0006\u00101\u001a\u00020\b2\u0006\u0010\u0010\u001a\u00020\u0011H\u0016J \u00102\u001a\u00020\u00062\u0006\u0010\u001a\u001a\u00020\u00152\u0006\u00103\u001a\u00020\u00152\u0006\u00104\u001a\u00020\u0015H\u0016J \u00105\u001a\u00020\u00062\u0006\u00106\u001a\u00020\b2\u0006\u00107\u001a\u00020\u00132\u0006\u00108\u001a\u00020\u0013H\u0016J(\u00109\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010-\u001a\u00020\u00132\u0006\u0010:\u001a\u00020\u00132\u0006\u0010\u001a\u001a\u00020\u0015H\u0016J\u0010\u0010;\u001a\u00020\u00062\u0006\u0010<\u001a\u00020\bH\u0016J\b\u0010=\u001a\u00020\u0006H\u0016J\f\u0010>\u001a\u00020\b*\u00020\u0017H\u0002J\f\u0010>\u001a\u00020\b*\u00020\u0011H\u0002J\f\u0010?\u001a\u00020\b*\u00020\u0013H\u0002J\u0014\u0010@\u001a\u00020\u0006*\u00020\"2\u0006\u0010A\u001a\u00020\u000bH\u0002J\u001c\u0010B\u001a\u00020\u0006*\u00020\u000b2\u0006\u0010C\u001a\u00020\b2\u0006\u0010D\u001a\u00020\u0013H\u0002J\f\u0010E\u001a\u00020\b*\u00020\bH\u0002J\f\u0010F\u001a\u00020\u0006*\u00020\"H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006H"}, d2 = {"Lcom/menudado/analytics/FirebaseMenuDadoAnalytics;", "Lcom/menudado/analytics/MenuDadoAnalytics;", "firebaseAnalytics", "Lcom/google/firebase/analytics/FirebaseAnalytics;", "(Lcom/google/firebase/analytics/FirebaseAnalytics;)V", "logEvent", "", "name", "", "buildParams", "Lkotlin/Function1;", "Landroid/os/Bundle;", "Lkotlin/ExtensionFunctionType;", "trackAboutAppOpened", "trackAiAnalysisFinished", "scope", "mealType", "Lcom/menudado/domain/MealType;", "success", "", "analyzedCount", "", "healthStatus", "Lcom/menudado/domain/HealthStatus;", "failureType", "trackAiAnalysisStarted", "menuCount", "trackAiDailyLimitReached", "source", "trackAiMenuGenerationFinished", "trackAiMenuGenerationStarted", "avoidIdeaCount", "trackAppOpened", "deviceInfo", "Lcom/menudado/analytics/DeviceInfo;", "trackDiceEmptyResult", "filter", "availableCandidateCount", "trackDiceFilterSelected", "trackDiceRolled", "resultMealType", "trackFirstMenuCreated", "trackMealTypeSelected", "formHasContent", "trackMenuCardOpened", "hasAiAnalysis", "trackMenuDeleted", "hadAiAnalysis", "trackMenuFormStarted", "firstEditedField", "trackMenuInventoryChanged", "analyzedMenuCount", "pendingAnalysisCount", "trackMenuSaveBlocked", "reason", "hasName", "hasDescription", "trackMenuSaved", "hasCalories", "trackOnboardingCompleted", "action", "trackOnboardingShown", "analyticsName", "analyticsStatus", "appendTo", "bundle", "putBooleanAsLong", "key", "value", "sanitized", "setAsUserProperties", "Companion", "app_debug"})
public final class FirebaseMenuDadoAnalytics implements com.menudado.analytics.MenuDadoAnalytics {
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.analytics.FirebaseAnalytics firebaseAnalytics = null;
    @java.lang.Deprecated()
    public static final int MAX_PARAM_LENGTH = 100;
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_APP_OPENED = "app_opened";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_MENU_SAVED = "menu_saved";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_MENU_DELETED = "menu_deleted";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_FIRST_MENU_CREATED = "first_menu_created";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_MENU_INVENTORY_CHANGED = "menu_inventory_changed";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_MENU_FORM_STARTED = "menu_form_started";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_MEAL_TYPE_SELECTED = "meal_type_selected";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_MENU_SAVE_BLOCKED = "menu_save_blocked";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_DICE_ROLLED = "dice_rolled";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_DICE_FILTER_SELECTED = "dice_filter_selected";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_DICE_EMPTY_RESULT = "dice_empty_result";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_MENU_CARD_OPENED = "menu_card_opened";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_ONBOARDING_SHOWN = "onboarding_shown";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_ONBOARDING_COMPLETED = "onboarding_completed";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_ABOUT_APP_OPENED = "about_app_opened";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_AI_MENU_GENERATION_STARTED = "ai_menu_gen_started";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_AI_MENU_GENERATION_FINISHED = "ai_menu_gen_finished";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_AI_ANALYSIS_STARTED = "ai_analysis_started";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_AI_ANALYSIS_FINISHED = "ai_analysis_finished";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String EVENT_AI_DAILY_LIMIT_REACHED = "ai_daily_limit_reached";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_DEVICE_MANUFACTURER = "device_manufacturer";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_DEVICE_MODEL = "device_model";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_ANDROID_VERSION = "android_version";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_LOCALE_COUNTRY = "locale_country";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_TIME_ZONE = "time_zone";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_MEAL_TYPE = "meal_type";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_HAS_AI_ANALYSIS = "has_ai_analysis";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_HAS_CALORIES = "has_calories";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_MENU_COUNT = "menu_count";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_ANALYZED_MENU_COUNT = "analyzed_menu_count";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_PENDING_ANALYSIS_COUNT = "pending_analysis_count";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_FIELD = "field";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_FORM_HAS_CONTENT = "form_has_content";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_REASON = "reason";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_HAS_NAME = "has_name";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_HAS_DESCRIPTION = "has_description";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_FILTER_TYPE = "filter_type";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_RESULT_FOUND = "result_found";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_RESULT_MEAL_TYPE = "result_meal_type";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_AVAILABLE_CANDIDATE_COUNT = "available_candidate_count";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_AVOID_IDEA_COUNT = "avoid_idea_count";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_STATUS = "status";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_SCOPE = "scope";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_ANALYZED_COUNT = "analyzed_count";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_SOURCE = "source";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_HEALTH_STATUS = "health_status";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_FAILURE_TYPE = "failure_type";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PARAM_ACTION = "action";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String VALUE_ALL = "all";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String VALUE_NONE = "none";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String VALUE_UNKNOWN = "unknown";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String VALUE_SUCCESS = "success";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String VALUE_FAILURE = "failure";
    @org.jetbrains.annotations.NotNull()
    private static final com.menudado.analytics.FirebaseMenuDadoAnalytics.Companion Companion = null;
    
    public FirebaseMenuDadoAnalytics(@org.jetbrains.annotations.NotNull()
    com.google.firebase.analytics.FirebaseAnalytics firebaseAnalytics) {
        super();
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
    public void trackOnboardingShown() {
    }
    
    @java.lang.Override()
    public void trackOnboardingCompleted(@org.jetbrains.annotations.NotNull()
    java.lang.String action) {
    }
    
    @java.lang.Override()
    public void trackAboutAppOpened() {
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
    
    private final void logEvent(java.lang.String name, kotlin.jvm.functions.Function1<? super android.os.Bundle, kotlin.Unit> buildParams) {
    }
    
    private final void appendTo(com.menudado.analytics.DeviceInfo $this$appendTo, android.os.Bundle bundle) {
    }
    
    private final void setAsUserProperties(com.menudado.analytics.DeviceInfo $this$setAsUserProperties) {
    }
    
    private final void putBooleanAsLong(android.os.Bundle $this$putBooleanAsLong, java.lang.String key, boolean value) {
    }
    
    private final java.lang.String analyticsStatus(boolean $this$analyticsStatus) {
        return null;
    }
    
    private final java.lang.String analyticsName(com.menudado.domain.MealType $this$analyticsName) {
        return null;
    }
    
    private final java.lang.String analyticsName(com.menudado.domain.HealthStatus $this$analyticsName) {
        return null;
    }
    
    private final java.lang.String sanitized(java.lang.String $this$sanitized) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0014\n\u0002\u0010\b\n\u0002\b\"\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0019X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001c\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001e\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010 \u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010#\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010$\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010%\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010&\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\'\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010(\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010)\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010*\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010+\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010,\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010-\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010.\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010/\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00100\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00101\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00102\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00103\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00104\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00105\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00106\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00107\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00108\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u00109\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010:\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006;"}, d2 = {"Lcom/menudado/analytics/FirebaseMenuDadoAnalytics$Companion;", "", "()V", "EVENT_ABOUT_APP_OPENED", "", "EVENT_AI_ANALYSIS_FINISHED", "EVENT_AI_ANALYSIS_STARTED", "EVENT_AI_DAILY_LIMIT_REACHED", "EVENT_AI_MENU_GENERATION_FINISHED", "EVENT_AI_MENU_GENERATION_STARTED", "EVENT_APP_OPENED", "EVENT_DICE_EMPTY_RESULT", "EVENT_DICE_FILTER_SELECTED", "EVENT_DICE_ROLLED", "EVENT_FIRST_MENU_CREATED", "EVENT_MEAL_TYPE_SELECTED", "EVENT_MENU_CARD_OPENED", "EVENT_MENU_DELETED", "EVENT_MENU_FORM_STARTED", "EVENT_MENU_INVENTORY_CHANGED", "EVENT_MENU_SAVED", "EVENT_MENU_SAVE_BLOCKED", "EVENT_ONBOARDING_COMPLETED", "EVENT_ONBOARDING_SHOWN", "MAX_PARAM_LENGTH", "", "PARAM_ACTION", "PARAM_ANALYZED_COUNT", "PARAM_ANALYZED_MENU_COUNT", "PARAM_ANDROID_VERSION", "PARAM_AVAILABLE_CANDIDATE_COUNT", "PARAM_AVOID_IDEA_COUNT", "PARAM_DEVICE_MANUFACTURER", "PARAM_DEVICE_MODEL", "PARAM_FAILURE_TYPE", "PARAM_FIELD", "PARAM_FILTER_TYPE", "PARAM_FORM_HAS_CONTENT", "PARAM_HAS_AI_ANALYSIS", "PARAM_HAS_CALORIES", "PARAM_HAS_DESCRIPTION", "PARAM_HAS_NAME", "PARAM_HEALTH_STATUS", "PARAM_LOCALE_COUNTRY", "PARAM_MEAL_TYPE", "PARAM_MENU_COUNT", "PARAM_PENDING_ANALYSIS_COUNT", "PARAM_REASON", "PARAM_RESULT_FOUND", "PARAM_RESULT_MEAL_TYPE", "PARAM_SCOPE", "PARAM_SOURCE", "PARAM_STATUS", "PARAM_TIME_ZONE", "VALUE_ALL", "VALUE_FAILURE", "VALUE_NONE", "VALUE_SUCCESS", "VALUE_UNKNOWN", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
}