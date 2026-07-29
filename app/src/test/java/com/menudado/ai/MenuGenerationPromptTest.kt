package com.menudado.ai

import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import com.menudado.domain.AppLanguage
import com.menudado.domain.CuisineInspiration
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import org.junit.Assert.assertEquals
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
        assertTrue(prompt.contains("debe incluir esos ingredientes"))
        assertFalse(prompt.contains("ingredientes como base principal"))
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
    fun `prompt uses one compact culinary inspiration instead of generic format list`() {
        val prompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList(),
            cuisineInspiration = CuisineInspiration.INDIAN
        ).lowercase()

        assertTrue(prompt.contains("inspiracion culinaria: india."))
        assertEquals(1, prompt.split("inspiracion culinaria:").size - 1)
        assertFalse(prompt.contains("variedad saludable: cremas"))
        assertFalse(prompt.contains("bowls, salteados"))
    }

    @Test
    fun `world cuisine prompt stays shorter than equivalent legacy prompt`() {
        val newPrompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList(),
            cuisineInspiration = CuisineInspiration.WEST_AFRICAN
        )
        val newRule = newPrompt.lineSequence()
            .single { it.contains("Inspiracion culinaria:") }
            .trim()
        val removedRule = "- Variedad saludable: cremas, sopas, ensaladas completas, ensalada cesar saludable, bowls, salteados simples, tortillas, legumbres, wraps, tostas, pasta integral o arroz integral."
        val equivalentLegacyPrompt = newPrompt.replace(newRule, removedRule)

        assertTrue(newPrompt.length < equivalentLegacyPrompt.length)
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

    @Test
    fun `prompt requires a specific executable recipe`() {
        val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

        assertTrue(prompt.contains("nombre concreto"))
        assertTrue(prompt.contains("cantidades aproximadas"))
        assertTrue(prompt.contains("una racion"))
        assertTrue(prompt.contains("2 a 4 indicaciones"))
        assertTrue(prompt.contains("tiempo total de preparacion"))
        assertTrue(prompt.contains("sustitucion, conservacion o servicio"))
    }

    @Test
    fun `prompt separates description notes and health fields`() {
        val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

        assertTrue(prompt.contains("notes no debe repetir description"))
        assertTrue(prompt.contains("estimacion realista para una racion"))
        assertTrue(prompt.contains("breves, utiles y sin tono alarmista"))
    }

    @Test
    fun `prompt prioritizes safety before variety`() {
        val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

        val safety = prompt.indexOf("1. seguridad alimentaria")
        val audience = prompt.indexOf("2. publico y edad")
        val mealType = prompt.indexOf("3. tipo de comida")
        val variety = prompt.indexOf("5. variedad y atractivo")

        assertTrue(safety >= 0)
        assertTrue(safety < audience)
        assertTrue(audience < mealType)
        assertTrue(mealType < variety)
    }

    @Test
    fun `prompt requires genuine differentiation from prior ideas`() {
        val prompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = listOf("Ensalada de pollo", "Bowl de garbanzos")
        ).lowercase()

        assertTrue(prompt.contains("no basta con cambiar el nombre"))
        assertTrue(prompt.contains("base, proteina, tecnica o estilo"))
        assertTrue(prompt.contains("ensalada de pollo"))
        assertTrue(prompt.contains("bowl de garbanzos"))
    }

    @Test
    fun `prompt requires only the closed json schema`() {
        val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

        assertTrue(prompt.contains("solo un objeto json valido"))
        assertTrue(prompt.contains("sin markdown"))
        assertTrue(prompt.contains("sin texto adicional"))
        assertTrue(prompt.contains("solo con estos campos"))
        assertTrue(prompt.contains("\"name\""))
        assertTrue(prompt.contains("\"description\""))
        assertTrue(prompt.contains("\"notes\""))
        assertTrue(prompt.contains("\"calories\""))
        assertTrue(prompt.contains("\"health_status\""))
        assertTrue(prompt.contains("\"health_reason\""))
        assertTrue(prompt.contains("\"health_suggestion\""))
        assertTrue(prompt.contains("\"deduplication_key\""))
    }

    @Test
    fun `prompt asks for market products without quantities`() {
        val prompt = MenuGenerationPrompt.build(MealType.LUNCH, emptyList()).lowercase()

        assertTrue(prompt.contains("\"shopping_products\""))
        assertTrue(prompt.contains("sin cantidades"))
        assertTrue(prompt.contains("productos reales de supermercado"))
    }

    @Test
    fun `generation prompt requests one canonical deduplication key in existing json`() {
        val prompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList()
        )

        assertTrue(prompt.contains("\"deduplication_key\""))
        assertTrue(prompt.contains("dish family|main ingredients|preparation"))
        assertTrue(prompt.contains("spaghetti, macaroni and similar shapes as pasta"))
        assertFalse(prompt.contains("make another request", ignoreCase = true))
    }

    @Test
    fun `canonical identity guidance replaces legacy rules with shorter text`() {
        val prompt = MenuGenerationPrompt.build(
            mealType = MealType.LUNCH,
            avoidIdeas = emptyList()
        )
        val canonicalRule = prompt.lineSequence()
            .single { it.contains("deduplication_key:") }
            .trim()
        val previousRules = """
            - deduplication_key debe estar en ingles y usar exactamente: dish family|main ingredients|preparation.
            - Si hay varios ingredientes principales, separalos con + y ordenalos alfabeticamente.
            - Normaliza variantes equivalentes: classify spaghetti, macaroni and similar shapes as pasta; use canonical ingredient names such as tomato.
            - La clave es tecnica, breve y no debe contener texto del perfil del usuario.
        """.trimIndent()

        assertTrue(canonicalRule.length < previousRules.length)
        assertTrue(canonicalRule.contains("vegetables, lentils, tomato"))
        assertTrue(canonicalRule.contains("mixed"))
        assertEquals(1, prompt.split("deduplication_key:").size - 1)
    }
}
