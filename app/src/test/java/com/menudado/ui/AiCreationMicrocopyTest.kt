package com.menudado.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class AiCreationMicrocopyTest {

    @Test
    fun aiCreationMicrocopyMatchesApprovedCopyInEverySupportedLocale() {
        val expectedCopy = mapOf(
            "values" to mapOf(
                "home_today_title" to "¿No sabes qué preparar hoy?",
                "home_today_subtitle" to "No pasa nada. Elige para quién cocinas y la IA te ayudará con una idea saludable.",
                "home_ai_dice_title" to "Encontremos algo rico",
                "form_base_ingredients" to "¿Qué tienes en casa? (Opcional)",
                "form_base_ingredients_placeholder" to "Ej. tomate, arroz o pollo",
                "dice_roll_ai_with_count" to "Ayúdame a elegir (%1\$d)"
            ),
            "values-en" to mapOf(
                "home_today_title" to "Not sure what to make today?",
                "home_today_subtitle" to "No worries. Choose who you're cooking for and AI will help with a healthy idea.",
                "home_ai_dice_title" to "Let's find something tasty",
                "form_base_ingredients" to "What do you have at home? (Optional)",
                "form_base_ingredients_placeholder" to "E.g. tomato, rice or chicken",
                "dice_roll_ai_with_count" to "Help me choose (%1\$d)"
            ),
            "values-fr" to mapOf(
                "home_today_title" to "Vous ne savez pas quoi préparer aujourd’hui ?",
                "home_today_subtitle" to "Pas de souci. Choisissez pour qui vous cuisinez et l’IA vous proposera une idée équilibrée.",
                "home_ai_dice_title" to "Trouvons quelque chose de bon",
                "form_base_ingredients" to "Qu’avez-vous à la maison ? (Facultatif)",
                "form_base_ingredients_placeholder" to "Ex. tomate, riz ou poulet",
                "dice_roll_ai_with_count" to "Aidez-moi à choisir (%1\$d)"
            )
        )

        expectedCopy.forEach { (resourceDirectory, expectedStrings) ->
            assertEquals(
                expectedStrings,
                readStrings(resourceDirectory).filterKeys(expectedStrings::containsKey)
            )
        }
    }

    private fun readStrings(resourceDirectory: String): Map<String, String> {
        val stringsFile = findProjectRoot()
            .resolve("app/src/main/res")
            .resolve(resourceDirectory)
            .resolve("strings.xml")
        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = documentBuilderFactory.newDocumentBuilder().parse(stringsFile.toFile())

        return (0 until document.getElementsByTagName("string").length).associate { index ->
            val element = document.getElementsByTagName("string").item(index) as Element
            element.getAttribute("name") to element.textContent.replace("\\'", "'")
        }
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .first { Files.isRegularFile(it.resolve("app/src/main/res/values/strings.xml")) }
}
