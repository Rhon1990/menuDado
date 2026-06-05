package com.menudado.domain

enum class MealType(val label: String) {
    BREAKFAST("Desayuno"),
    LUNCH("Almuerzo"),
    DINNER("Cena")
}

enum class HealthStatus(val label: String) {
    HEALTHY("Saludable"),
    IMPROVABLE("Intermedio"),
    UNHEALTHY("No saludable"),
    UNKNOWN("Sin analizar")
}

data class HealthAnalysis(
    val status: HealthStatus,
    val reason: String,
    val suggestion: String,
    val calories: Int? = null
)

data class FoodMenu(
    val id: Long = 0,
    val name: String,
    val mealType: MealType,
    val description: String,
    val notes: String = "",
    val healthAnalysis: HealthAnalysis? = null,
    val calories: Int? = null,
    val lastPickedDate: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class GeneratedMenu(
    val name: String,
    val description: String,
    val notes: String,
    val calories: Int,
    val healthAnalysis: HealthAnalysis? = null
)

enum class DietaryAllergen(val label: String, val promptName: String) {
    GLUTEN("Gluten", "gluten"),
    DAIRY("Lactosa / lacteos", "lactosa y lacteos"),
    EGG("Huevo", "huevo"),
    TREE_NUTS("Frutos secos", "frutos secos"),
    PEANUT("Cacahuete", "cacahuete"),
    SOY("Soja", "soja"),
    FISH("Pescado", "pescado"),
    SHELLFISH("Marisco", "marisco"),
    SESAME("Sesamo", "sesamo")
}

data class DietaryProfile(
    val isVegan: Boolean = false,
    val hasAllergies: Boolean = false,
    val allergens: Set<DietaryAllergen> = emptySet(),
    val otherAvoidances: String = ""
) {
    val hasRestrictions: Boolean
        get() = isVegan || hasAllergies && allergens.isNotEmpty() || otherAvoidances.isNotBlank()
}
