package com.menudado.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietaryProfileCompatibilityTest {
    @Test
    fun `vegan profile rejects dairy in generated candidate`() {
        val menu = GeneratedMenu(
            name = "Pasta cremosa",
            description = "Pasta, tomate, queso y nata.",
            notes = "",
            calories = 500
        )

        assertFalse(DietaryProfile(isVegan = true).accepts(menu))
    }

    @Test
    fun `vegan and dairy profiles reject common plural food terms`() {
        val menu = generated("Macarrones cuatro quesos con gelatinas de postre")
        val explicitlyExcludedMenu = generated("Macarrones sin quesos")

        assertFalse(DietaryProfile(isVegan = true).accepts(menu))
        assertFalse(
            DietaryProfile(
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.DAIRY)
            ).accepts(menu)
        )
        assertTrue(DietaryProfile(isVegan = true).accepts(explicitlyExcludedMenu))
    }

    @Test
    fun `allergy and free avoidance are checked across recipe and products`() {
        val menu = GeneratedMenu(
            name = "Bowl suave",
            description = "Arroz y verduras.",
            notes = "Termina con tahini.",
            calories = 410,
            shoppingProducts = listOf(requireNotNull(ShoppingProduct.fromAi("Sésamo")))
        )
        val profile = DietaryProfile(
            hasAllergies = true,
            allergens = setOf(DietaryAllergen.SESAME),
            otherAvoidances = "picante"
        )

        assertFalse(profile.accepts(menu))
        assertTrue(profile.accepts(menu.copy(notes = "", shoppingProducts = emptyList())))
    }

    @Test
    fun `baby profile rejects honey added salt and whole choking hazards`() {
        val profile = DietaryProfile(ageRange = MenuAudience.BABY.defaultAgeRange)

        listOf(
            generated("Yogur con miel"),
            generated("Arroz con una pizca de sal añadida"),
            generated("Uvas enteras con frutos secos enteros")
        ).forEach { menu ->
            assertFalse(profile.accepts(menu, MenuAudience.BABY))
        }
    }

    @Test
    fun `pregnancy profile rejects raw unpasteurized alcohol and high mercury fish`() {
        val profile = DietaryProfile(isPregnant = true)

        listOf(
            generated("Sushi de atún rojo"),
            generated("Queso de leche no pasteurizada"),
            generated("Salsa con vino sin cocinar"),
            generated("Huevo poco cocinado")
        ).forEach { menu ->
            assertFalse(profile.accepts(menu, MenuAudience.ADULT))
        }
    }

    @Test
    fun `baby and young child profiles reject salty condiments and sliced sausages`() {
        val profile = DietaryProfile()

        assertFalse(
            profile.accepts(
                generated("Arroz tierno con una cucharadita de salsa de soja"),
                MenuAudience.BABY
            )
        )
        assertFalse(
            profile.accepts(
                generated("Salchicha en rodajas con verduras"),
                MenuAudience.CHILD
            )
        )
    }

    @Test
    fun `allergen derivatives are rejected across every supported family`() {
        val examples = mapOf(
            DietaryAllergen.GLUTEN to "Avena cocida",
            DietaryAllergen.DAIRY to "Proteína de suero",
            DietaryAllergen.EGG to "Albúmina",
            DietaryAllergen.TREE_NUTS to "Macadamia",
            DietaryAllergen.PEANUT to "Mantequilla de cacahuete",
            DietaryAllergen.SOY to "Miso",
            DietaryAllergen.FISH to "Salsa de anchoas",
            DietaryAllergen.SHELLFISH to "Calamar",
            DietaryAllergen.SESAME to "Aceite de sésamo"
        )

        examples.forEach { (allergen, description) ->
            val profile = DietaryProfile(
                hasAllergies = true,
                allergens = setOf(allergen)
            )

            assertFalse("$allergen should reject $description", profile.accepts(generated(description)))
        }
    }

    @Test
    fun `explicitly absent safety hazards do not reject a compatible recipe`() {
        val profile = DietaryProfile(isPregnant = true)

        assertTrue(
            profile.accepts(
                generated("Salsa sin alcohol con queso pasteurizado"),
                MenuAudience.ADULT
            )
        )
    }

    @Test
    fun `explicit compatible substitutions do not trigger ambiguous allergen terms`() {
        val profile = DietaryProfile(
            hasAllergies = true,
            allergens = setOf(DietaryAllergen.GLUTEN, DietaryAllergen.DAIRY)
        )

        assertTrue(
            profile.accepts(
                generated("Pasta sin gluten con crema vegetal"),
                MenuAudience.ADULT
            )
        )
    }

    @Test
    fun `compatible allergen contexts never override explicit food avoidances`() {
        assertFalse(
            DietaryProfile(otherAvoidances = "garbanzo").accepts(
                generated("Harina de garbanzo")
            )
        )
        assertFalse(
            DietaryProfile(otherAvoidances = "avena").accepts(
                generated("Avena sin gluten")
            )
        )
    }

    @Test
    fun `plant substitutions are accepted only for the allergen they replace`() {
        val plantMenu = generated(
            "Leche de coco, mantequilla de cacahuete, yogur de soja y mayonesa vegana"
        )
        val dairyProfile = DietaryProfile(
            hasAllergies = true,
            allergens = setOf(DietaryAllergen.DAIRY, DietaryAllergen.EGG)
        )

        assertTrue(DietaryProfile(isVegan = true).accepts(plantMenu))
        assertTrue(dairyProfile.accepts(plantMenu))
        assertFalse(
            DietaryProfile(
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.PEANUT)
            ).accepts(plantMenu)
        )
        assertFalse(
            DietaryProfile(
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.SOY)
            ).accepts(plantMenu)
        )
    }

    @Test
    fun `clinical condition names are not treated as food ingredients`() {
        val profile = DietaryProfile(otherAvoidances = "diabético, hipertenso")

        assertTrue(
            profile.accepts(
                generated("Lentejas con verduras"),
                MenuAudience.ADULT
            )
        )
    }

    @Test
    fun `food avoidance following a clinical condition remains strict`() {
        val profile = DietaryProfile(otherAvoidances = "diabetes sin azúcar")

        assertFalse(profile.accepts(generated("Yogur con azúcar")))
    }

    @Test
    fun `common named allergen sources and alcoholic drinks are rejected`() {
        val cases = listOf(
            DietaryProfile(
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.GLUTEN)
            ) to listOf("Pan de espelta", "Ensalada de bulgur", "Salsa de malta"),
            DietaryProfile(
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.DAIRY)
            ) to listOf("Mozzarella", "Parmesano", "Kéfir"),
            DietaryProfile(
                hasAllergies = true,
                allergens = setOf(DietaryAllergen.FISH)
            ) to listOf("Trucha", "Dorada", "Sea bream", "Truite")
        )

        cases.forEach { (profile, descriptions) ->
            descriptions.forEach { description ->
                assertFalse(description, profile.accepts(generated(description)))
            }
        }
        listOf("Cerveza", "Beer", "Bière").forEach { description ->
            assertFalse(
                description,
                DietaryProfile(isPregnant = true).accepts(generated(description))
            )
            assertFalse(
                description,
                DietaryProfile().accepts(generated(description), MenuAudience.CHILD)
            )
        }
    }

    @Test
    fun `prefixed concrete avoidance is enforced`() {
        val profile = DietaryProfile(otherAvoidances = "sin picante, sin champiñones")

        assertFalse(
            profile.accepts(
                generated("Arroz con champiñones"),
                MenuAudience.ADULT
            )
        )
    }

    private fun generated(description: String) = GeneratedMenu(
        name = "Idea",
        description = description,
        notes = "",
        calories = 300
    )
}
