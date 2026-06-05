package com.menudado.ai

import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.MealType
import com.menudado.domain.DietaryProfile

interface HealthAnalyzer {
    suspend fun analyze(menu: FoodMenu): Result<HealthAnalysis>
    suspend fun analyzeBatch(menus: List<FoodMenu>): Result<Map<Long, HealthAnalysis>>
    suspend fun generateMenu(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile
    ): Result<GeneratedMenu>
}
