package com.menudado.data

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class AiUsageBackupRulesTest {

    @Test
    fun `volatile identity usage and rewards are excluded from every backup mode`() {
        val volatilePreferences = setOf(
            "menu-dado-scoped-ai-usage.xml",
            "menu-dado-rewarded-ai-credits.xml"
        )

        val legacyBackupExclusions = excludedSharedPreferences("backup_rules.xml")
        val modernBackupExclusions = excludedSharedPreferences("data_extraction_rules.xml")

        volatilePreferences.forEach { preferencesFile ->
            assertTrue(
                "$preferencesFile must be excluded from legacy backups",
                preferencesFile in legacyBackupExclusions
            )
            assertTrue(
                "$preferencesFile must be excluded from cloud backup and device transfer",
                modernBackupExclusions.count { it == preferencesFile } == 2
            )
        }
    }

    private fun excludedSharedPreferences(fileName: String): List<String> {
        val rulesFile = findProjectRoot()
            .resolve("app/src/main/res/xml")
            .resolve(fileName)
        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = documentBuilderFactory.newDocumentBuilder().parse(rulesFile.toFile())

        return (0 until document.getElementsByTagName("exclude").length)
            .map { document.getElementsByTagName("exclude").item(it) as Element }
            .filter { it.getAttribute("domain") == "sharedpref" }
            .map { it.getAttribute("path") }
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .first { Files.isRegularFile(it.resolve("app/src/main/res/xml/backup_rules.xml")) }
}
