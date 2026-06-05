package com.menudado.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType

@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mealType: String,
    val description: String,
    val notes: String,
    val healthStatus: String?,
    val healthReason: String?,
    val healthSuggestion: String?,
    val calories: Int?,
    val lastPickedDate: String?,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): FoodMenu {
        return FoodMenu(
            id = id,
            name = name,
            mealType = MealType.valueOf(mealType),
            description = description,
            notes = notes,
            healthAnalysis = healthStatus?.let {
                HealthAnalysis(
                    status = HealthStatus.valueOf(it),
                    reason = healthReason.orEmpty(),
                    suggestion = healthSuggestion.orEmpty(),
                    calories = calories
                )
            },
            calories = calories,
            lastPickedDate = lastPickedDate,
            createdAt = createdAt
        )
    }
}

fun FoodMenu.toEntity(): MenuEntity {
    return MenuEntity(
        id = id,
        name = name,
        mealType = mealType.name,
        description = description,
        notes = notes,
        healthStatus = healthAnalysis?.status?.name,
        healthReason = healthAnalysis?.reason,
        healthSuggestion = healthAnalysis?.suggestion,
        calories = calories,
        lastPickedDate = lastPickedDate,
        createdAt = createdAt
    )
}
