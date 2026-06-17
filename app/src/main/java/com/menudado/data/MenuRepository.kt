package com.menudado.data

import com.menudado.ai.HealthAnalyzer
import com.menudado.domain.FoodMenu
import com.menudado.domain.GeneratedMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.MealType
import com.menudado.domain.DietaryProfile
import com.menudado.domain.MenuAudience
import com.menudado.domain.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MenuRepository(
    private val menuDao: MenuDao,
    private val healthAnalyzer: HealthAnalyzer
) {
    val menus: Flow<List<FoodMenu>> = menuDao.observeMenus()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun save(menu: FoodMenu) {
        if (menu.id == 0L) {
            menuDao.insert(menu.toEntity())
        } else {
            menuDao.update(menu.toEntity())
        }
    }

    suspend fun delete(menu: FoodMenu) {
        menuDao.delete(menu.toEntity())
    }

    suspend fun analyze(menu: FoodMenu, language: AppLanguage): Result<HealthAnalysis> {
        return healthAnalyzer.analyze(menu, language)
    }

    suspend fun analyzeBatch(menus: List<FoodMenu>, language: AppLanguage): Result<Map<Long, HealthAnalysis>> {
        return healthAnalyzer.analyzeBatch(menus, language)
    }

    suspend fun generateMenu(
        mealType: MealType,
        avoidIdeas: List<String>,
        dietaryProfile: DietaryProfile,
        audience: MenuAudience,
        baseIngredients: String,
        language: AppLanguage
    ): Result<GeneratedMenu> {
        return healthAnalyzer.generateMenu(mealType, avoidIdeas, dietaryProfile, audience, baseIngredients, language)
    }
}
