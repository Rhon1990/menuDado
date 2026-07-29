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
                "home_today_subtitle" to "No pasa nada. Dinos para quién es el menú y la IA te ayudará con una idea saludable.",
                "home_ai_dice_title" to "Encontremos algo rico",
                "form_base_ingredients" to "¿Qué tienes en casa? (Opcional)",
                "form_base_ingredients_placeholder" to "Ej. tomate, arroz o pollo",
                "dice_roll_ai_with_count" to "Ayúdame a elegir (%1\$d)",
                "dietary_other_label" to "Alimentos a evitar o indicaciones",
                "dietary_other_placeholder" to "Ej: sin picante, sin champiñones",
                "dietary_other_supporting_text" to
                    "Las condiciones de salud orientan a la IA, pero no sustituyen la revisión de un profesional sanitario. Para una exclusión estricta, escribe alimentos concretos.",
                "ai_retry_ready_title" to "¿Probamos otra vez?",
                "ai_retry_wait_title" to "Esta vez no encontramos tu idea",
                "ai_retry_ready_badge" to "Ya estamos listos",
                "ai_retry_wait_badge" to "Démonos un momento",
                "ai_retry_ready_body" to
                    "Cierra este aviso y vuelve a tocar Ayúdame a elegir. Mantendremos tus preferencias para buscar una idea que encaje contigo.",
                "ai_retry_wait_body" to
                    "Cuando termine la pausa, este aviso te lo dirá. Tus preferencias seguirán aquí para que puedas volver a intentarlo con tranquilidad."
            ),
            "values-en" to mapOf(
                "home_today_title" to "Not sure what to make today?",
                "home_today_subtitle" to "No worries. Tell us who the menu is for and AI will help with a healthy idea.",
                "home_ai_dice_title" to "Let's find something tasty",
                "form_base_ingredients" to "What do you have at home? (Optional)",
                "form_base_ingredients_placeholder" to "E.g. tomato, rice or chicken",
                "dice_roll_ai_with_count" to "Help me choose (%1\$d)",
                "dietary_other_label" to "Foods to avoid or guidance",
                "dietary_other_placeholder" to "E.g. no spicy food, no mushrooms",
                "dietary_other_supporting_text" to
                    "Health conditions guide the AI but do not replace review by a health professional. For strict exclusion, enter specific foods.",
                "ai_retry_ready_title" to "Shall we try again?",
                "ai_retry_wait_title" to "We didn’t find your idea this time",
                "ai_retry_ready_badge" to "We’re ready",
                "ai_retry_wait_badge" to "Let’s give it a moment",
                "ai_retry_ready_body" to
                    "Close this notice and tap Help me choose again. We’ll keep your preferences to look for an idea that fits you.",
                "ai_retry_wait_body" to
                    "When the pause is over, this notice will let you know. Your preferences will still be here so you can try again with peace of mind."
            ),
            "values-fr" to mapOf(
                "home_today_title" to "Vous ne savez pas quoi préparer aujourd’hui ?",
                "home_today_subtitle" to "Pas de souci. Dites-nous à qui s’adresse le menu et l’IA vous proposera une idée équilibrée.",
                "home_ai_dice_title" to "Trouvons quelque chose de bon",
                "form_base_ingredients" to "Qu’avez-vous à la maison ? (Facultatif)",
                "form_base_ingredients_placeholder" to "Ex. tomate, riz ou poulet",
                "dice_roll_ai_with_count" to "Aidez-moi à choisir (%1\$d)",
                "dietary_other_label" to "Aliments à éviter ou indications",
                "dietary_other_placeholder" to "Ex. sans épices, sans champignons",
                "dietary_other_supporting_text" to
                    "Les conditions de santé orientent l’IA mais ne remplacent pas l’avis d’un professionnel de santé. Pour une exclusion stricte, indiquez des aliments précis.",
                "ai_retry_ready_title" to "On réessaie ?",
                "ai_retry_wait_title" to "Cette fois, nous n’avons pas trouvé votre idée",
                "ai_retry_ready_badge" to "Nous sommes prêts",
                "ai_retry_wait_badge" to "Donnons-lui un instant",
                "ai_retry_ready_body" to
                    "Fermez cet avis et touchez de nouveau Aidez-moi à choisir. Nous conserverons vos préférences pour chercher une idée qui vous corresponde.",
                "ai_retry_wait_body" to
                    "Lorsque la pause sera terminée, cet avis vous l’indiquera. Vos préférences resteront disponibles afin que vous puissiez réessayer sereinement."
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
