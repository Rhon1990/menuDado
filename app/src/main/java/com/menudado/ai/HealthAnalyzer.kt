package com.menudado.ai

import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.MealType
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MenuAudience
import com.menudado.domain.AppLanguage

interface HealthAnalyzer {
    suspend fun analyze(menu: FoodMenu, language: AppLanguage): Result<HealthAnalysis>
    suspend fun analyzeBatch(menus: List<FoodMenu>, language: AppLanguage): Result<Map<Long, HealthAnalysis>>
    suspend fun generateMenu(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile,
        audience: MenuAudience,
        baseIngredients: String,
        language: AppLanguage
    ): Result<GeneratedMenu>
}
