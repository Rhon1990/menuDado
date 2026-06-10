package com.menudado.ai

import com.menudado.domain.MealType
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuGenerationPromptTest {
    @Test
    fun `dinner prompt asks for dinner and rejects lunch ideas`() {
        val prompt = MenuGenerationPrompt.build(MealType.DINNER, emptyList()).lowercase()

        assertTrue(prompt.contains("genera una cena saludable"))
        assertTrue(prompt.contains("no generes almuerzos"))
        assertTrue(prompt.contains("no uses la palabra almuerzo"))
        assertFalse(prompt.contains("genera una idea de comida para cena"))
    }

    @Test
    fun `dinner prompt asks for quick low effort night meals`() {
        val prompt = MenuGenerationPrompt.build(MealType.DINNER, emptyList()).lowercase()

        assertTrue(prompt.contains("maximo 10 minutos"))
        assertTrue(prompt.contains("maximo 5 ingredientes principales"))
        assertTrue(prompt.contains("similar de sencilla que un desayuno"))
        assertTrue(prompt.contains("evita horno"))
        assertTrue(prompt.contains("preparaciones con varios pasos"))
    }

    @Test
    fun `prompt includes vegan and allergy restrictions from dietary profile`() {
        val prompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList(),
            dietaryProfile = DietaryProfile(
                isVegan = true,
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.GLUTEN, DietaryAllergen.PEANUT),
                otherAvoidances = "picante"
            )
        ).lowercase()

        assertTrue(prompt.contains("perfil alimentario"))
        assertTrue(prompt.contains("vegana"))
        assertTrue(prompt.contains("sin ingredientes de origen animal"))
        assertTrue(prompt.contains("gluten"))
        assertTrue(prompt.contains("cacahuete"))
        assertTrue(prompt.contains("picante"))
        assertTrue(prompt.contains("no incluyas"))
    }

    @Test
    fun `prompt asks to use base ingredients when provided`() {
        val prompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList(),
            dietaryProfile = DietaryProfile(),
            baseIngredients = "berenjena, tomate"
        ).lowercase()

        assertTrue(prompt.contains("ingredientes base"))
        assertTrue(prompt.contains("berenjena, tomate"))
        assertTrue(prompt.contains("debe usar esos ingredientes"))
    }
}
