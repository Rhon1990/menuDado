package com.menudado.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

class LocalizedMicrocopyTest {
    @Test
    fun `AppLanguage visible messages do not use old AI analysis CTAs`() {
        val source = findProjectFile(
            "app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt"
        ).toFile().readText()

        legacyHardcodedAnalysisCtas.forEach { legacyCta ->
            assertFalse(
                "Legacy hardcoded CTA remains: $legacyCta",
                source.contains(legacyCta)
            )
        }
        technicalUserFacingCopy.forEach { technicalCopy ->
            assertFalse(
                "Technical user-facing copy remains: $technicalCopy",
                source.contains(technicalCopy)
            )
        }
    }

    @Test
    fun `placeholder signature normalizes Android formatting and allows reordering`() {
        val source = placeholderSignature("%1${'$'}-12.2S · %2${'$'}+08d %% %n")
        val reordered = placeholderSignature("%2${'$'}d%n%1${'$'}s%%")

        assertEquals(source, reordered)
    }

    @Test
    fun `placeholder signature assigns implicit indexes and supports date time`() {
        assertEquals(
            listOf("arg:1:s", "arg:2:f", "arg:3:tY", "literal:%", "literal:n"),
            placeholderSignature("%s · %08.2f · %tY %% %n")
        )
        assertEquals(
            listOf("arg:1:s", "arg:1:s"),
            placeholderSignature("%s %<S")
        )
    }

    @Test
    fun `localized copy keeps human voice key parity and placeholder contracts`() {
        val resources = resourceDirectories.mapValues { (_, directory) ->
            readResources(findResourceFile(directory))
        }
        val spanishStrings = resources.getValue("es").strings
        val spanishKeys = spanishStrings.filterValues { it.translatable }.keys

        resources.forEach { (language, localizedResources) ->
            val translatableStrings = localizedResources.strings.filterValues { it.translatable }
            assertEquals("Translatable keys differ for $language", spanishKeys, translatableStrings.keys)
            spanishKeys.forEach { key ->
                assertEquals(
                    "Placeholder signature differs for $language:$key",
                    spanishStrings.getValue(key).placeholderSignature,
                    translatableStrings.getValue(key).placeholderSignature
                )
            }
        }

        val spanishPlurals = resources.getValue("es").plurals
            .filterValues { it.translatable }
        assertTrue(spanishPlurals.containsKey("favorite_menu_count"))
        resources.forEach { (language, localizedResources) ->
            val translatablePlurals = localizedResources.plurals.filterValues { it.translatable }
            assertEquals(
                "Plural keys differ for $language",
                spanishPlurals.keys,
                translatablePlurals.keys
            )
            translatablePlurals.forEach { (key, localizedPlural) ->
                assertTrue(
                    "Required plural quantities missing for $language:$key",
                    localizedPlural.quantities.keys.containsAll(
                        requiredPluralQuantities.getValue(language)
                    )
                )
            }
        }
        val languages = resources.keys.toList()
        languages.forEachIndexed { index, leftLanguage ->
            languages.drop(index + 1).forEach { rightLanguage ->
                val leftPlurals = resources.getValue(leftLanguage).plurals
                    .filterValues { it.translatable }
                val rightPlurals = resources.getValue(rightLanguage).plurals
                    .filterValues { it.translatable }
                leftPlurals.forEach { (key, leftPlural) ->
                    val rightPlural = rightPlurals.getValue(key)
                    val commonQuantities = leftPlural.quantities.keys
                        .intersect(rightPlural.quantities.keys)
                    commonQuantities.forEach { quantity ->
                        assertEquals(
                            "Plural placeholder signature differs for " +
                                "$leftLanguage/$rightLanguage:$key:$quantity",
                            leftPlural.quantities.getValue(quantity),
                            rightPlural.quantities.getValue(quantity)
                        )
                    }
                }
            }
        }

        expectedHumanCopy.forEach { (language, anchors) ->
            anchors.forEach { (key, expected) ->
                assertEquals(
                    "Unexpected human microcopy for $language:$key",
                    expected,
                    resources.getValue(language).strings.getValue(key).value
                )
            }
        }

        resources.values
            .flatMap { localizedResources -> localizedResources.strings.values }
            .forEach { localizedString ->
                val normalizedValue = localizedString.value.replace('’', '\'')
                legacyCopy.forEach { legacy ->
                    assertFalse(
                        "Legacy copy remains: $legacy",
                        normalizedValue.contains(legacy)
                    )
                }
            }
    }

    private fun findResourceFile(directory: String): Path {
        return findProjectFile("app/src/main/res/$directory/strings.xml")
    }

    private fun findProjectFile(relativePath: String): Path {
        val start = Paths.get("").toAbsolutePath().normalize()
        val roots = generateSequence(start) { current -> current.parent }.toList()
        val candidates = roots.map { root -> root.resolve(relativePath) }
        return candidates.firstOrNull(Files::isRegularFile)
            ?: error("Could not locate $relativePath from $start")
    }

    private fun readResources(file: Path): LocalizedResources {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(file.toFile())
        val stringNodes = document.getElementsByTagName("string")
        val pluralNodes = document.getElementsByTagName("plurals")

        val strings = buildMap {
            repeat(stringNodes.length) { index ->
                val element = stringNodes.item(index) as Element
                val name = element.getAttribute("name")
                val value = element.textContent
                put(
                    name,
                    LocalizedString(
                        value = value,
                        translatable = element.getAttribute("translatable") != "false",
                        placeholderSignature = placeholderSignature(value)
                    )
                )
            }
        }
        val plurals = buildMap {
            repeat(pluralNodes.length) { index ->
                val element = pluralNodes.item(index) as Element
                val itemNodes = element.getElementsByTagName("item")
                val quantities = buildMap {
                    repeat(itemNodes.length) { itemIndex ->
                        val item = itemNodes.item(itemIndex) as Element
                        put(item.getAttribute("quantity"), placeholderSignature(item.textContent))
                    }
                }
                put(
                    element.getAttribute("name"),
                    LocalizedPlural(
                        translatable = element.getAttribute("translatable") != "false",
                        quantities = quantities
                    )
                )
            }
        }
        return LocalizedResources(strings = strings, plurals = plurals)
    }

    private fun placeholderSignature(value: String): List<String> {
        var nextImplicitIndex = 1
        var previousArgumentIndex: Int? = null

        return placeholderRegex.findAll(value)
            .map { match ->
                val explicitIndex = match.groupValues[1].toIntOrNull()
                val flags = match.groupValues[2]
                val dateTimePrefix = match.groupValues[5]
                val conversion = match.groupValues[6]

                when {
                    dateTimePrefix.isEmpty() && conversion == "%" -> "literal:%"
                    dateTimePrefix.isEmpty() && conversion == "n" -> "literal:n"
                    else -> {
                        val argumentIndex = explicitIndex
                            ?: if ('<' in flags) {
                                requireNotNull(previousArgumentIndex) {
                                    "Relative placeholder has no previous argument in: $value"
                                }
                            } else {
                                nextImplicitIndex++
                            }
                        previousArgumentIndex = argumentIndex
                        val type = if (dateTimePrefix.isEmpty()) {
                            conversion.lowercase()
                        } else {
                            "t$conversion"
                        }
                        "arg:$argumentIndex:$type"
                    }
                }
            }
            .sorted()
            .toList()
    }

    private data class LocalizedResources(
        val strings: Map<String, LocalizedString>,
        val plurals: Map<String, LocalizedPlural>
    )

    private data class LocalizedString(
        val value: String,
        val translatable: Boolean,
        val placeholderSignature: List<String>
    )

    private data class LocalizedPlural(
        val translatable: Boolean,
        val quantities: Map<String, List<String>>
    )

    private companion object {
        val resourceDirectories = mapOf(
            "es" to "values",
            "en" to "values-en",
            "fr" to "values-fr"
        )
        val requiredPluralQuantities = mapOf(
            "es" to setOf("one", "other"),
            "en" to setOf("one", "other"),
            "fr" to setOf("one", "other")
        )
        val placeholderRegex = Regex(
            "%(?:(\\d+)\\$)?([-#+ 0,(<]*)(\\d+)?(?:\\.(\\d+))?([tT])?([a-zA-Z%])"
        )
        val legacyCopy = setOf(
            "Lanzar con IA",
            "Launch with AI",
            "Lancer avec l'IA"
        )
        val legacyHardcodedAnalysisCtas = setOf(
            "Analizar IA",
            "Analyze with AI",
            "Analyser avec l'IA"
        )
        val technicalUserFacingCopy = setOf(
            "setup issue",
            "problème de configuration",
            "problema de configuración"
        )
        val expectedHumanCopy = mapOf(
            "es" to mapOf(
                "home_today_title" to "¿No sabes qué cocinar?",
                "home_ai_dice_title" to "Lanza el dado",
                "dice_ai_action" to "Idea saludable con IA",
                "dice_ai_blocked_ai_paused" to
                    "La IA necesita un momento. Espera un poco o escribe tu menú.",
                "ai_generation_loading_title" to "Buscando algo rico para ti",
                "generated_menu_detail_title" to "Una idea pensada para ti",
                "analysis_ai" to "Lo que la IA ve en tu menú"
            ),
            "en" to mapOf(
                "home_today_title" to "Not sure what to cook?",
                "home_ai_dice_title" to "Roll the die",
                "dice_ai_action" to "Healthy idea with AI",
                "dice_ai_blocked_ai_paused" to
                    "AI needs a moment. Wait a little or write your own menu.",
                "ai_generation_loading_title" to "Finding something tasty for you",
                "generated_menu_detail_title" to "An idea picked for you",
                "analysis_ai" to "What AI sees in your menu"
            ),
            "fr" to mapOf(
                "home_today_title" to "Vous ne savez pas quoi cuisiner ?",
                "home_ai_dice_title" to "Lancez le dé",
                "dice_ai_action" to "Idée équilibrée avec l’IA",
                "dice_ai_blocked_ai_paused" to
                    "L’IA a besoin d’un moment. Patientez un peu ou écrivez votre menu.",
                "ai_generation_loading_title" to "Nous cherchons quelque chose de bon pour vous",
                "generated_menu_detail_title" to "Une idée pensée pour vous",
                "analysis_ai" to "Ce que l’IA observe dans votre menu"
            )
        )
    }
}
