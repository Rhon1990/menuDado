package com.menudado.ai

import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import com.menudado.domain.AppLanguage
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
    fun `prompt includes health conditions written in dietary profile`() {
        val prompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList(),
            dietaryProfile = DietaryProfile(otherAvoidances = "diabetico, hipertenso")
        ).lowercase()

        assertTrue(prompt.contains("condiciones de salud"))
        assertTrue(prompt.contains("diabetico, hipertenso"))
        assertTrue(prompt.contains("ajusta el menu"))
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

    @Test
    fun `prompt includes selected audience age range`() {
        val prompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList(),
            dietaryProfile = DietaryProfile(ageRange = "8-10 meses"),
            audience = MenuAudience.BABY
        ).lowercase()

        assertTrue(prompt.contains("8-10 meses"))
        assertTrue(prompt.contains("alimentacion complementaria"))
    }

    @Test
    fun `prompt includes pregnancy restriction for adult profile`() {
        val prompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList(),
            dietaryProfile = DietaryProfile(isPregnant = true),
            audience = MenuAudience.ADULT
        ).lowercase()

        assertTrue(prompt.contains("embarazada"))
        assertTrue(prompt.contains("seguridad alimentaria"))
    }

    @Test
    fun `prompt asks for brief non judgmental health analysis`() {
        val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

        assertTrue(prompt.contains("evaluacion saludable"))
        assertTrue(prompt.contains("breve"))
        assertTrue(prompt.contains("sin tono de juicio"))
    }

    @Test
    fun `prompt asks for broader healthy variety`() {
        val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

        assertTrue(prompt.contains("cremas"))
        assertTrue(prompt.contains("sopas"))
        assertTrue(prompt.contains("ensaladas completas"))
        assertTrue(prompt.contains("ensalada cesar saludable"))
        assertTrue(prompt.contains("bowls"))
    }

    @Test
    fun `prompt asks AI to generate content in selected app language`() {
        val englishPrompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList(),
            language = AppLanguage.ENGLISH
        ).lowercase()
        val frenchPrompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList(),
            language = AppLanguage.FRENCH
        ).lowercase()

        assertTrue(englishPrompt.contains("write name, description, notes, reason and suggestion in english"))
        assertTrue(frenchPrompt.contains("write name, description, notes, reason and suggestion in french"))
    }
}
