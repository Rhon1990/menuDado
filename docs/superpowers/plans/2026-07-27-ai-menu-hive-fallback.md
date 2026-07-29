# AI Menu Hive Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar una idea IA compatible desde una colmena anónima de Firestore cuando la generación real falla, manteniendo el dado animado, evitando duplicados semánticos y sin añadir servicios de pago ni llamadas extra a Gemini.

**Architecture:** Mantener `MenuDadoViewModel -> MenuRepository -> HealthAnalyzer` como única ruta de generación en vivo y añadir un `AiMenuHiveGateway` independiente para contribución y respaldo. Una clave canónica corta incluida en la respuesta Gemini actual produce un hash semántico determinista; una huella no reversible del perfil restringe las consultas. Firestore usa una colección pública autenticada separada de `users/{uid}`, con máximo 12 candidatos, caché local, reglas cerradas y un interruptor de Remote Config.

**Tech Stack:** Kotlin, coroutines/StateFlow, Jetpack Compose, Firebase AI Logic, Cloud Firestore Android SDK, Firebase Remote Config, Firebase Auth/App Check, JUnit 4, kotlinx-coroutines-test, Firebase Local Emulator Suite y Node test runner.

---

## Mapa de archivos y responsabilidades

### Nuevos

- `app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt`: normalización de clave semántica, SHA-256 y huella anónima del perfil.
- `app/src/main/java/com/menudado/domain/DietaryProfileCompatibility.kt`: detector reutilizable de conflictos alimentarios para entrada y candidatos.
- `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`: contrato de colmena, política de selección, caché y contribución.
- `app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt`: contrato Firestore, implementación Android y mapper saneado.
- `app/src/main/java/com/menudado/ai/AiMenuHiveRemoteConfig.kt`: interruptor gratuito `ai_menu_hive_enabled`.
- `app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt`: equivalencia semántica y huella de perfil.
- `app/src/test/java/com/menudado/domain/DietaryProfileCompatibilityTest.kt`: seguridad alimentaria local.
- `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`: selección, caché, coste máximo y contribución.
- `app/src/test/java/com/menudado/backend/AiMenuHiveDataSourceTest.kt`: serialización anónima y lectura defensiva.
- `app/src/test/java/com/menudado/ai/AiMenuHiveRemoteConfigTest.kt`: defaults e intervalo.
- `qa/package.json`, `qa/package-lock.json`, `qa/firestore-rules.test.mjs`: pruebas locales de reglas, sin tocar producción.

### Modificados

- `app/src/main/java/com/menudado/domain/MenuModels.kt`: clave de deduplicación opcional en `GeneratedMenu`.
- `app/src/main/java/com/menudado/domain/GeneratedMenuParser.kt`: parseo tolerante de `deduplication_key`.
- `app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt`: clave canónica dentro de la llamada existente.
- `app/src/test/java/com/menudado/domain/GeneratedMenuParserTest.kt`: contrato del campo opcional.
- `app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt`: una sola respuesta, sin llamada adicional.
- `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`: fases, fallback y contribución posterior al guardado.
- `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`: orquestación completa con reloj virtual.
- `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: copy por fase sin detener el dado.
- `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: recursos de loading por fase.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml`, `values-fr/strings.xml`: copy localizado.
- `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`: evento agregado de fallback.
- `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`: serialización sin contenido de menú.
- `app/src/main/java/com/menudado/MenuDadoApplication.kt`: construcción de dependencias.
- `app/src/main/java/com/menudado/MainActivity.kt`: inyección y refresh del interruptor.
- `firestore.rules`: colección compartida sin abrir datos privados.
- `firebase.json`: emulador local para pruebas de reglas.
- `docs/project-context.md`: fuente funcional actualizada.
- `docs/privacy-policy.md`: reutilización anónima explicada sin aviso en el guardado.

### Deliberadamente no modificados

- Room, `MenuEntity` y `MenuDadoDatabase`: la clave vive en el borrador hasta el guardado y la contribución es de mejor esfuerzo.
- `users/{uid}/menus`: conserva su contrato privado y local-first.
- límites locales de invitado/IA: no se eluden mediante el respaldo.

---

### Task 1: Añadir identidad semántica a la respuesta IA existente

**Files:**
- Modify: `app/src/main/java/com/menudado/domain/MenuModels.kt:54-62`
- Modify: `app/src/main/java/com/menudado/domain/GeneratedMenuParser.kt:7-30`
- Modify: `app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt:69-108`
- Test: `app/src/test/java/com/menudado/domain/GeneratedMenuParserTest.kt`
- Test: `app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt`

- [ ] **Step 1: Escribir las pruebas fallidas del parser**

Añadir:

```kotlin
@Test
fun `parsea clave semantica opcional sin hacerla requisito del menu`() {
    val withKey = GeneratedMenuParser.parse(
        """
        {
          "name": "Espaguetis en salsa de tomate",
          "description": "Pasta con tomate.",
          "notes": "Lista en 10 minutos.",
          "calories": 430,
          "deduplication_key": "pasta|tomato|sauce"
        }
        """.trimIndent()
    )
    val legacy = GeneratedMenuParser.parse(
        """{"name":"Sopa","description":"Verduras.","notes":"","calories":320}"""
    )

    assertEquals("pasta|tomato|sauce", withKey.deduplicationKey)
    assertNull(legacy.deduplicationKey)
}
```

Importar `assertNull`.

- [ ] **Step 2: Escribir la prueba fallida del prompt**

Añadir:

```kotlin
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
```

- [ ] **Step 3: Ejecutar las pruebas y comprobar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.domain.GeneratedMenuParserTest" \
  --tests "com.menudado.ai.MenuGenerationPromptTest"
```

Expected: FAIL porque `GeneratedMenu.deduplicationKey` y el campo del prompt aún no existen.

- [ ] **Step 4: Implementar el cambio mínimo de modelo y parser**

Cambiar `GeneratedMenu` a:

```kotlin
data class GeneratedMenu(
    val name: String,
    val description: String,
    val notes: String,
    val calories: Int,
    val healthAnalysis: HealthAnalysis? = null,
    val shoppingProducts: List<ShoppingProduct> = emptyList(),
    val deduplicationKey: String? = null
)
```

En `GeneratedMenuParser.parse`, leer:

```kotlin
val deduplicationKey = jsonLikeText.quoted("deduplication_key")
    ?.takeIf { it.isNotBlank() }
```

Y entregarlo al constructor:

```kotlin
deduplicationKey = deduplicationKey
```

- [ ] **Step 5: Ampliar el JSON del prompt sin otra petición**

Reemplazar `sin campos nuevos` por una lista cerrada que incluya:

```text
- deduplication_key debe estar en inglés y usar exactamente:
  dish family|main ingredients|preparation.
- Si hay varios ingredientes principales, sepáralos con + y ordénalos
  alfabéticamente.
- Normaliza variantes equivalentes: classify spaghetti, macaroni and similar
  shapes as pasta; use canonical ingredient names such as tomato.
- La clave es técnica, breve y no debe contener texto del perfil del usuario.
```

Y añadir al objeto JSON:

```json
"deduplication_key": "pasta|tomato|sauce"
```

No cambiar `FirebaseHealthAnalyzer.generateMenu`: seguirá ejecutando un único
`model.generateContent(...)`.

- [ ] **Step 6: Ejecutar GREEN y regresión del analizador**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.domain.GeneratedMenuParserTest" \
  --tests "com.menudado.ai.MenuGenerationPromptTest"
```

Expected: PASS. `FirebaseHealthAnalyzer` se cubrirá mediante compilación en
Tasks 4, 7 y 10 porque no tiene una clase unitaria dedicada actualmente.

- [ ] **Step 7: Commit**

```bash
git add \
  app/src/main/java/com/menudado/domain/MenuModels.kt \
  app/src/main/java/com/menudado/domain/GeneratedMenuParser.kt \
  app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt \
  app/src/test/java/com/menudado/domain/GeneratedMenuParserTest.kt \
  app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt
git commit -m "feat: add semantic identity to AI menus"
```

---

### Task 2: Crear huellas deterministas y validación alimentaria reutilizable

**Files:**
- Create: `app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt`
- Create: `app/src/main/java/com/menudado/domain/DietaryProfileCompatibility.kt`
- Create: `app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt`
- Create: `app/src/test/java/com/menudado/domain/DietaryProfileCompatibilityTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:1893-2180`

- [ ] **Step 1: Escribir pruebas fallidas de identidad**

Crear:

```kotlin
package com.menudado.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMenuHiveIdentityTest {
    @Test
    fun `equivalent pasta names share semantic hash`() {
        val pasta = AiMenuHiveIdentity.from(
            language = AppLanguage.SPANISH,
            rawKey = "pasta|tomato|sauce"
        )
        val spaghetti = AiMenuHiveIdentity.from(
            language = AppLanguage.SPANISH,
            rawKey = "spaghetti|tomate|salsa"
        )

        assertEquals(pasta, spaghetti)
        assertEquals(64, requireNotNull(pasta).semanticHash.length)
    }

    @Test
    fun `different preparations or languages do not collide`() {
        val sauce = AiMenuHiveIdentity.from(AppLanguage.SPANISH, "pasta|tomato|sauce")
        val salad = AiMenuHiveIdentity.from(AppLanguage.SPANISH, "pasta|tomato|salad")
        val english = AiMenuHiveIdentity.from(AppLanguage.ENGLISH, "pasta|tomato|sauce")

        assertNotEquals(sauce, salad)
        assertNotEquals(sauce, english)
    }

    @Test
    fun `main ingredient order does not change identity`() {
        val first = AiMenuHiveIdentity.from(
            AppLanguage.SPANISH,
            "bowl|tomato+lentils|mixed"
        )
        val reordered = AiMenuHiveIdentity.from(
            AppLanguage.SPANISH,
            "bowl|lentejas+tomate|mixed"
        )

        assertEquals(first, reordered)
    }

    @Test
    fun `invalid semantic key is rejected`() {
        assertNull(AiMenuHiveIdentity.from(AppLanguage.SPANISH, "pasta con tomate"))
        assertNull(AiMenuHiveIdentity.from(AppLanguage.SPANISH, "||"))
    }

    @Test
    fun `profile key is stable and changes for safety restrictions`() {
        val base = DietaryProfile(ageRange = "18+ años")
        val vegan = base.copy(isVegan = true)

        val baseKey = AiMenuHiveIdentity.eligibilityKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT,
            base
        )
        val veganKey = AiMenuHiveIdentity.eligibilityKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT,
            vegan
        )

        assertEquals(
            baseKey,
            AiMenuHiveIdentity.eligibilityKey(
                AppLanguage.SPANISH,
                MealType.LUNCH,
                MenuAudience.ADULT,
                base
            )
        )
        assertNotEquals(baseKey, veganKey)
        assertTrue(baseKey.matches(Regex("[a-f0-9]{64}")))
    }
}
```

- [ ] **Step 2: Escribir pruebas fallidas de seguridad**

Crear:

```kotlin
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
}
```

- [ ] **Step 3: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.domain.AiMenuHiveIdentityTest" \
  --tests "com.menudado.domain.DietaryProfileCompatibilityTest"
```

Expected: FAIL por archivos y APIs inexistentes.

- [ ] **Step 4: Implementar identidad y SHA-256**

Crear `AiMenuHiveIdentity.kt` con:

```kotlin
package com.menudado.domain

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

data class AiMenuSemanticIdentity(
    val canonicalKey: String,
    val semanticHash: String
)

object AiMenuHiveIdentity {
    private const val PROFILE_SCHEMA_VERSION = 1
    private val aliases = mapOf(
        "spaghetti" to "pasta",
        "espagueti" to "pasta",
        "espaguetis" to "pasta",
        "macaroni" to "pasta",
        "macarron" to "pasta",
        "macarrones" to "pasta",
        "tomate" to "tomato",
        "tomates" to "tomato",
        "lenteja" to "lentils",
        "lentejas" to "lentils",
        "salsa" to "sauce",
        "salsa de tomate" to "sauce"
    )

    fun from(language: AppLanguage, rawKey: String?): AiMenuSemanticIdentity? {
        val rawParts = rawKey.orEmpty().split('|')
        if (rawParts.size != 3) return null
        val parts = listOf(
            canonicalComponent(rawParts[0]),
            canonicalIngredients(rawParts[1]),
            canonicalComponent(rawParts[2])
        )
        if (parts.any(String::isBlank)) return null
        val canonicalKey = parts.joinToString("|")
        return AiMenuSemanticIdentity(
            canonicalKey = canonicalKey,
            semanticHash = sha256("${language.name}|$canonicalKey")
        )
    }

    fun eligibilityKey(
        language: AppLanguage,
        mealType: MealType,
        audience: MenuAudience,
        profile: DietaryProfile
    ): String {
        val allergens = if (profile.hasAllergies) {
            profile.allergens.map(DietaryAllergen::name).sorted().joinToString(",")
        } else {
            ""
        }
        val raw = listOf(
            "v$PROFILE_SCHEMA_VERSION",
            language.name,
            mealType.name,
            audience.name,
            profile.ageRange.normalized(),
            profile.isPregnant.toString(),
            profile.isVegan.toString(),
            allergens,
            profile.otherAvoidances.normalized()
        ).joinToString("|")
        return sha256(raw)
    }

    private fun canonicalComponent(value: String): String {
        val normalized = value.normalized()
        return aliases[normalized] ?: normalized
    }

    private fun canonicalIngredients(value: String): String = value
        .split('+', ',')
        .map(::canonicalComponent)
        .filter(String::isNotBlank)
        .distinct()
        .sorted()
        .joinToString("+")

    private fun String.normalized(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9 ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
```

- [ ] **Step 5: Extraer el detector alimentario**

Mover desde `MenuDadoViewModel.kt` las funciones `findIngredientConflicts`,
`conflictsWith`, normalización y tablas de términos a
`DietaryProfileCompatibility.kt`. Mantener la firma usada por el ViewModel y
añadir:

```kotlin
fun DietaryProfile.accepts(menu: GeneratedMenu): Boolean {
    val candidateText = buildList {
        add(menu.name)
        add(menu.description)
        add(menu.notes)
        addAll(menu.shoppingProducts.map(ShoppingProduct::displayName))
    }.joinToString(separator = ",")
    return findIngredientConflicts(candidateText).isEmpty()
}
```

Importar la extensión desde el ViewModel. No cambiar mensajes visibles.

- [ ] **Step 6: Ejecutar GREEN y regresión del ViewModel**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.domain.AiMenuHiveIdentityTest" \
  --tests "com.menudado.domain.DietaryProfileCompatibilityTest" \
  --tests "com.menudado.ui.MenuDadoViewModelTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add \
  app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt \
  app/src/main/java/com/menudado/domain/DietaryProfileCompatibility.kt \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt \
  app/src/test/java/com/menudado/domain/DietaryProfileCompatibilityTest.kt
git commit -m "feat: add safe semantic menu identity"
```

---

### Task 3: Implementar política de colmena sin Firebase

**Files:**
- Create: `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`
- Create: `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`

- [ ] **Step 1: Escribir fakes y pruebas fallidas**

Crear pruebas que cubran:

```kotlin
@Test
fun `live server candidate is preferred and query is capped at twelve`() = runTest {
    val dataSource = RecordingAiMenuHiveDataSource(
        server = Result.success(listOf(sharedMenu("pasta|tomato|sauce")))
    )
    val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }
    val request = request()

    val result = repository.findCompatibleMenu(request).getOrThrow()

    assertEquals("pasta|tomato|sauce", result?.generatedMenu?.deduplicationKey)
    assertEquals(AiMenuHiveLookupSource.SERVER, result?.source)
    assertEquals(12L, dataSource.requests.single().limit)
}

@Test
fun `cache is used only when server fails`() = runTest {
    val cached = sharedMenu("pasta|tomato|sauce")
    val dataSource = RecordingAiMenuHiveDataSource(
        server = Result.failure(IllegalStateException("offline")),
        cache = Result.success(listOf(cached))
    )
    val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

    val result = repository.findCompatibleMenu(request()).getOrThrow()

    assertEquals(AiMenuHiveLookupSource.CACHE, result?.source)
    assertEquals(
        listOf(AiMenuHiveReadSource.SERVER, AiMenuHiveReadSource.CACHE),
        dataSource.requests.map(AiMenuHiveDataSourceRequest::source)
    )
}

@Test
fun `unsafe and recently shown candidates lose priority to unseen safe menu`() = runTest {
    val unsafe = sharedMenu(
        "pasta|tomato|sauce",
        description = "Pasta con queso"
    )
    val repeated = sharedMenu("rice|vegetables|bowl")
    val fresh = sharedMenu("lentils|vegetables|stew")
    val dataSource = RecordingAiMenuHiveDataSource(
        server = Result.success(listOf(unsafe, repeated, fresh))
    )
    val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

    val result = repository.findCompatibleMenu(
        request(
            profile = DietaryProfile(isVegan = true),
            recentSemanticHashes = setOf(repeated.semanticHash)
        )
    ).getOrThrow()

    assertEquals(fresh.semanticHash, result?.semanticHash)
}

@Test
fun `all safe menus already seen reuses one instead of returning unsafe failure`() = runTest {
    val repeated = sharedMenu("rice|vegetables|bowl")
    val dataSource = RecordingAiMenuHiveDataSource(
        server = Result.success(listOf(repeated))
    )
    val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

    val result = repository.findCompatibleMenu(
        request(recentSemanticHashes = setOf(repeated.semanticHash))
    ).getOrThrow()

    assertEquals(repeated.semanticHash, result?.semanticHash)
}

@Test
fun `disabled hive performs no read or contribution`() = runTest {
    val dataSource = RecordingAiMenuHiveDataSource()
    val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(false)) { 0 }

    assertNull(repository.findCompatibleMenu(request()).getOrThrow())
    repository.contribute(contribution()).getOrThrow()

    assertTrue(dataSource.requests.isEmpty())
    assertTrue(dataSource.upserts.isEmpty())
}
```

Incluir estos builders y fake en el mismo test:

```kotlin
private fun request(
    profile: DietaryProfile = DietaryProfile(ageRange = "18+ años"),
    recentSemanticHashes: Set<String> = emptySet()
) = AiMenuHiveSearchRequest(
    language = AppLanguage.SPANISH,
    mealType = MealType.LUNCH,
    audience = MenuAudience.ADULT,
    profile = profile,
    baseIngredients = "",
    recentSemanticHashes = recentSemanticHashes
)

private fun contribution() = AiMenuHiveContribution(
    language = AppLanguage.SPANISH,
    mealType = MealType.LUNCH,
    audience = MenuAudience.ADULT,
    profile = DietaryProfile(ageRange = "18+ años"),
    generatedMenu = GeneratedMenu(
        name = "Pasta con tomate",
        description = "Pasta integral con salsa de tomate.",
        notes = "Lista en 10 minutos.",
        calories = 430,
        deduplicationKey = "pasta|tomato|sauce"
    ),
    cuisineInspiration = CuisineInspiration.ITALIAN
)

private fun sharedMenu(
    key: String,
    description: String = "Pasta integral con salsa de tomate."
): SharedAiMenu {
    val identity = requireNotNull(
        AiMenuHiveIdentity.from(AppLanguage.SPANISH, key)
    )
    return SharedAiMenu(
        semanticHash = identity.semanticHash,
        semanticKey = identity.canonicalKey,
        language = AppLanguage.SPANISH,
        generatedMenu = GeneratedMenu(
            name = "Idea recuperada",
            description = description,
            notes = "Lista en 10 minutos.",
            calories = 430,
            deduplicationKey = identity.canonicalKey
        ),
        cuisineInspiration = CuisineInspiration.ITALIAN,
        eligibilityKeys = setOf(
            AiMenuHiveIdentity.eligibilityKey(
                AppLanguage.SPANISH,
                MealType.LUNCH,
                MenuAudience.ADULT,
                DietaryProfile(ageRange = "18+ años")
            )
        )
    )
}

private class RecordingAiMenuHiveDataSource(
    var server: Result<List<SharedAiMenu>> = Result.success(emptyList()),
    var cache: Result<List<SharedAiMenu>> = Result.success(emptyList())
) : AiMenuHiveDataSource {
    val requests = mutableListOf<AiMenuHiveDataSourceRequest>()
    val upserts = mutableListOf<SharedAiMenu>()

    override suspend fun fetch(
        request: AiMenuHiveDataSourceRequest
    ): Result<List<SharedAiMenu>> {
        requests += request
        return when (request.source) {
            AiMenuHiveReadSource.SERVER -> server
            AiMenuHiveReadSource.CACHE -> cache
        }
    }

    override suspend fun upsert(menu: SharedAiMenu): Result<Unit> {
        upserts += menu
        return Result.success(Unit)
    }
}
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.data.AiMenuHiveRepositoryTest"
```

Expected: FAIL porque el contrato no existe.

- [ ] **Step 3: Definir modelos y contratos**

Crear:

```kotlin
enum class AiMenuHiveReadSource { SERVER, CACHE }
enum class AiMenuHiveLookupSource { SERVER, CACHE }

data class SharedAiMenu(
    val semanticHash: String,
    val semanticKey: String,
    val language: AppLanguage,
    val generatedMenu: GeneratedMenu,
    val cuisineInspiration: CuisineInspiration?,
    val eligibilityKeys: Set<String>
)

data class AiMenuHiveDataSourceRequest(
    val eligibilityKey: String,
    val source: AiMenuHiveReadSource,
    val limit: Long = AI_MENU_HIVE_QUERY_LIMIT
)

interface AiMenuHiveDataSource {
    suspend fun fetch(request: AiMenuHiveDataSourceRequest): Result<List<SharedAiMenu>>
    suspend fun upsert(menu: SharedAiMenu): Result<Unit>
}

data class AiMenuHiveSearchRequest(
    val language: AppLanguage,
    val mealType: MealType,
    val audience: MenuAudience,
    val profile: DietaryProfile,
    val baseIngredients: String,
    val recentSemanticHashes: Set<String>
)

data class AiMenuHiveContribution(
    val language: AppLanguage,
    val mealType: MealType,
    val audience: MenuAudience,
    val profile: DietaryProfile,
    val generatedMenu: GeneratedMenu,
    val cuisineInspiration: CuisineInspiration?
)

data class AiMenuHiveCandidate(
    val generatedMenu: GeneratedMenu,
    val cuisineInspiration: CuisineInspiration?,
    val semanticHash: String,
    val source: AiMenuHiveLookupSource
)

interface AiMenuHiveGateway {
    suspend fun findCompatibleMenu(request: AiMenuHiveSearchRequest): Result<AiMenuHiveCandidate?>
    suspend fun contribute(contribution: AiMenuHiveContribution): Result<Unit>
}

object NoOpAiMenuHiveGateway : AiMenuHiveGateway {
    override suspend fun findCompatibleMenu(
        request: AiMenuHiveSearchRequest
    ): Result<AiMenuHiveCandidate?> = Result.success(null)

    override suspend fun contribute(
        contribution: AiMenuHiveContribution
    ): Result<Unit> = Result.success(Unit)
}

class AiMenuHiveFeatureToggle(@Volatile var isEnabled: Boolean)

internal const val AI_MENU_HIVE_QUERY_LIMIT = 12L
```

- [ ] **Step 4: Implementar selección y contribución mínimas**

`AiMenuHiveRepository` debe:

1. devolver `null` sin IO si el toggle está apagado;
2. calcular `eligibilityKey` con idioma, tipo, público y perfil;
3. intentar `SERVER` y usar `CACHE` solo si el servidor falla;
4. filtrar por hash reciente y `profile.accepts(generatedMenu)`;
5. priorizar candidatos cuyo texto contenga algún ingrediente base;
6. usar `pickIndexProvider(size)` inyectado para seleccionar;
7. rechazar contribución sin identidad válida;
8. construir `SharedAiMenu` sin UID ni entrada base.

Usar esta firma y flujo:

```kotlin
class AiMenuHiveRepository(
    private val dataSource: AiMenuHiveDataSource,
    private val featureToggle: AiMenuHiveFeatureToggle,
    private val pickIndexProvider: (Int) -> Int = { size ->
        kotlin.random.Random.nextInt(size)
    }
) : AiMenuHiveGateway {
    override suspend fun findCompatibleMenu(
        request: AiMenuHiveSearchRequest
    ): Result<AiMenuHiveCandidate?> {
        if (!featureToggle.isEnabled) return Result.success(null)
        val eligibilityKey = AiMenuHiveIdentity.eligibilityKey(
            request.language,
            request.mealType,
            request.audience,
            request.profile
        )
        val baseRequest = AiMenuHiveDataSourceRequest(
            eligibilityKey = eligibilityKey,
            source = AiMenuHiveReadSource.SERVER
        )
        val server = dataSource.fetch(baseRequest)
        val resolved = if (server.isSuccess) {
            server.getOrThrow() to AiMenuHiveLookupSource.SERVER
        } else {
            val cache = dataSource.fetch(
                baseRequest.copy(source = AiMenuHiveReadSource.CACHE)
            )
            if (cache.isFailure) {
                return Result.failure(requireNotNull(cache.exceptionOrNull()))
            }
            cache.getOrThrow() to AiMenuHiveLookupSource.CACHE
        }
        val selected = selectCandidate(resolved.first, request)
            ?: return Result.success(null)
        return Result.success(
            AiMenuHiveCandidate(
                generatedMenu = selected.generatedMenu,
                cuisineInspiration = selected.cuisineInspiration,
                semanticHash = selected.semanticHash,
                source = resolved.second
            )
        )
    }

    override suspend fun contribute(
        contribution: AiMenuHiveContribution
    ): Result<Unit> {
        if (!featureToggle.isEnabled) return Result.success(Unit)
        val identity = AiMenuHiveIdentity.from(
            contribution.language,
            contribution.generatedMenu.deduplicationKey
        ) ?: return Result.success(Unit)
        return dataSource.upsert(
            SharedAiMenu(
                semanticHash = identity.semanticHash,
                semanticKey = identity.canonicalKey,
                language = contribution.language,
                generatedMenu = contribution.generatedMenu.copy(
                    deduplicationKey = identity.canonicalKey
                ),
                cuisineInspiration = contribution.cuisineInspiration,
                eligibilityKeys = setOf(
                    AiMenuHiveIdentity.eligibilityKey(
                        contribution.language,
                        contribution.mealType,
                        contribution.audience,
                        contribution.profile
                    )
                )
            )
        )
    }
}
```

Usar este selector:

```kotlin
private fun selectCandidate(
    candidates: List<SharedAiMenu>,
    request: AiMenuHiveSearchRequest
): SharedAiMenu? {
    val safe = candidates.filter { candidate ->
        request.profile.accepts(candidate.generatedMenu)
    }
    if (safe.isEmpty()) return null
    val unseen = safe.filterNot { it.semanticHash in request.recentSemanticHashes }
    val eligible = unseen.ifEmpty { safe }
    val preferred = eligible.filter { candidate ->
        candidate.generatedMenu.searchableText()
            .containsAnyRequestedIngredient(request.baseIngredients)
    }.ifEmpty { eligible }
    return preferred[pickIndexProvider(preferred.size).coerceIn(preferred.indices)]
}

private fun GeneratedMenu.searchableText(): String = buildList {
    add(name)
    add(description)
    add(notes)
    addAll(shoppingProducts.map(ShoppingProduct::displayName))
}.joinToString(" ").lowercase()

private fun String.containsAnyRequestedIngredient(input: String): Boolean {
    val requested = input.split(',', ';', '\n')
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
    return requested.isNotEmpty() && requested.any(::contains)
}
```

Cuando `baseIngredients` esté vacío,
`containsAnyRequestedIngredient` devolverá `false`, de modo que `ifEmpty`
restaura todos los seguros.

- [ ] **Step 5: Ejecutar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.data.AiMenuHiveRepositoryTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add \
  app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt \
  app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt
git commit -m "feat: add AI menu hive selection policy"
```

---

### Task 4: Conectar Firestore y Remote Config

**Files:**
- Create: `app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt`
- Create: `app/src/main/java/com/menudado/ai/AiMenuHiveRemoteConfig.kt`
- Create: `app/src/test/java/com/menudado/backend/AiMenuHiveDataSourceTest.kt`
- Create: `app/src/test/java/com/menudado/ai/AiMenuHiveRemoteConfigTest.kt`

- [ ] **Step 1: Escribir pruebas fallidas del mapper anónimo**

Crear:

```kotlin
@Test
fun `shared document excludes identity profile and local-only fields`() {
    val menu = sampleSharedAiMenu()

    val document = AiMenuHiveFirestoreMapper.toDocument(menu)

    assertEquals(menu.semanticHash, document["semanticHash"])
    assertEquals("SPANISH", document["language"])
    assertEquals(1, (document["eligibilityKeys"] as List<*>).size)
    assertFalse(document.containsKey("uid"))
    assertFalse(document.containsKey("email"))
    assertFalse(document.containsKey("profile"))
    assertFalse(document.containsKey("baseIngredients"))
    assertFalse(document.containsKey("imageUri"))
    assertFalse(document.containsKey("mealType"))
    assertFalse(document.containsKey("audience"))
}

@Test
fun `malformed or incomplete shared document is ignored`() {
    assertNull(
        AiMenuHiveFirestoreMapper.fromDocument(
            documentId = "bad",
            document = mapOf("name" to "Sin contrato")
        )
    )
}
```

Añadir este builder al test:

```kotlin
private fun sampleSharedAiMenu(): SharedAiMenu {
    val profile = DietaryProfile(ageRange = "18+ años")
    val identity = requireNotNull(
        AiMenuHiveIdentity.from(
            AppLanguage.SPANISH,
            "pasta|tomato|sauce"
        )
    )
    return SharedAiMenu(
        semanticHash = identity.semanticHash,
        semanticKey = identity.canonicalKey,
        language = AppLanguage.SPANISH,
        generatedMenu = GeneratedMenu(
            name = "Pasta con tomate",
            description = "Pasta integral con salsa de tomate.",
            notes = "Lista en 10 minutos.",
            calories = 430,
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Incluye cereal y tomate.",
                suggestion = "Acompaña con verduras.",
                calories = 430
            ),
            shoppingProducts = listOf(
                requireNotNull(ShoppingProduct.fromAi("Pasta integral")),
                requireNotNull(ShoppingProduct.fromAi("Tomate"))
            ),
            deduplicationKey = identity.canonicalKey
        ),
        cuisineInspiration = CuisineInspiration.ITALIAN,
        eligibilityKeys = setOf(
            AiMenuHiveIdentity.eligibilityKey(
                AppLanguage.SPANISH,
                MealType.LUNCH,
                MenuAudience.ADULT,
                profile
            )
        )
    )
}
```

- [ ] **Step 2: Escribir pruebas fallidas del interruptor**

Crear:

```kotlin
@Test
fun `hive remote config uses free shared refresh policy`() {
    assertEquals("ai_menu_hive_enabled", AiMenuHiveRemoteConfig.KEY_AI_MENU_HIVE_ENABLED)
    assertEquals(0L, AiMenuHiveRemoteConfig.fetchIntervalSeconds(isDebugBuild = true))
    assertEquals(3_600L, AiMenuHiveRemoteConfig.fetchIntervalSeconds(isDebugBuild = false))
    assertTrue(AiMenuHiveRemoteConfig.DEFAULT_AI_MENU_HIVE_ENABLED)
}
```

- [ ] **Step 3: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.backend.AiMenuHiveDataSourceTest" \
  --tests "com.menudado.ai.AiMenuHiveRemoteConfigTest"
```

Expected: FAIL por clases inexistentes.

- [ ] **Step 4: Implementar mapper y consultas**

En `FirebaseAiMenuHiveDataSource`:

```kotlin
class FirebaseAiMenuHiveDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AiMenuHiveDataSource {
override suspend fun fetch(
    request: AiMenuHiveDataSourceRequest
): Result<List<SharedAiMenu>> = runCatching {
    firestore.collection(COLLECTION)
        .whereArrayContains("eligibilityKeys", request.eligibilityKey)
        .limit(request.limit)
        .get(
            when (request.source) {
                AiMenuHiveReadSource.SERVER -> Source.SERVER
                AiMenuHiveReadSource.CACHE -> Source.CACHE
            }
        )
        .awaitBackendTask()
        .documents
        .mapNotNull { snapshot ->
            AiMenuHiveFirestoreMapper.fromDocument(
                documentId = snapshot.id,
                document = snapshot.data.orEmpty()
            )
        }
}

private companion object {
    const val COLLECTION = "sharedAiMenus"
}
}
```

La transacción de `upsert` debe crear con `FieldValue.serverTimestamp()` y, si
ya existe, actualizar exclusivamente:

```kotlin
mapOf(
    "eligibilityKeys" to FieldValue.arrayUnion(menu.eligibilityKeys.single()),
    "updatedAt" to FieldValue.serverTimestamp()
)
```

El mapper debe validar hash hexadecimal de 64 caracteres, enums, strings no
vacíos, calorías positivas, máximo 20 productos y `deduplicationKey`.

- [ ] **Step 5: Implementar Remote Config siguiendo el patrón existente**

Crear `AiMenuHiveRemoteConfig` con `fetchEnabled()` igual al flujo de
`MenuDadoGuestLimitsRemoteConfig`, usando:

```kotlin
class AiMenuHiveRemoteConfig(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val onEnabledChanged: (Boolean) -> Unit
) {
    fun fetchEnabled() {
        onEnabledChanged(remoteConfig.getBoolean(KEY_AI_MENU_HIVE_ENABLED))
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(
                    fetchIntervalSeconds(BuildConfig.DEBUG)
                )
                .build()
        ).addOnCompleteListener {
            remoteConfig.setDefaultsAsync(
                mapOf(KEY_AI_MENU_HIVE_ENABLED to DEFAULT_AI_MENU_HIVE_ENABLED)
            ).addOnCompleteListener {
                remoteConfig.fetchAndActivate().addOnCompleteListener {
                    onEnabledChanged(
                        remoteConfig.getBoolean(KEY_AI_MENU_HIVE_ENABLED)
                    )
                }
            }
        }
    }

    companion object {
const val KEY_AI_MENU_HIVE_ENABLED = "ai_menu_hive_enabled"
const val DEFAULT_AI_MENU_HIVE_ENABLED = true
        private const val DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS = 0L
        private const val MINIMUM_FETCH_INTERVAL_SECONDS = 3_600L

        fun fetchIntervalSeconds(isDebugBuild: Boolean): Long =
            if (isDebugBuild) {
                DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS
            } else {
                MINIMUM_FETCH_INTERVAL_SECONDS
            }
    }
}
```

El callback solo actualiza `AiMenuHiveFeatureToggle.isEnabled`.

- [ ] **Step 6: Ejecutar GREEN y compile**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.backend.AiMenuHiveDataSourceTest" \
  --tests "com.menudado.ai.AiMenuHiveRemoteConfigTest"
./gradlew :app:compileDebugKotlin
```

Expected: PASS y `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add \
  app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt \
  app/src/main/java/com/menudado/ai/AiMenuHiveRemoteConfig.kt \
  app/src/test/java/com/menudado/backend/AiMenuHiveDataSourceTest.kt \
  app/src/test/java/com/menudado/ai/AiMenuHiveRemoteConfigTest.kt
git commit -m "feat: connect AI menu hive to Firebase"
```

---

### Task 5: Cerrar reglas y probarlas sin producción

**Files:**
- Modify: `firestore.rules`
- Modify: `firebase.json`
- Create: `qa/package.json`
- Create: `qa/package-lock.json`
- Create: `qa/firestore-rules.test.mjs`

- [ ] **Step 1: Crear el harness local de reglas**

`qa/package.json`:

```json
{
  "name": "menudado-firestore-rules-tests",
  "private": true,
  "type": "module",
  "scripts": {
    "test:firestore-rules": "node --test firestore-rules.test.mjs"
  },
  "devDependencies": {
    "@firebase/rules-unit-testing": "5.0.1",
    "firebase": "12.16.0"
  }
}
```

Generar lockfile:

```bash
npm install --package-lock-only --prefix qa
```

Expected: `qa/package-lock.json` creado sin dependencias globales.

- [ ] **Step 2: Escribir las pruebas de reglas antes de abrir la colección**

Iniciar `qa/firestore-rules.test.mjs` con:

```javascript
import { readFileSync } from "node:fs";
import { after, afterEach, before, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from "@firebase/rules-unit-testing";
import {
  Timestamp,
  deleteDoc,
  doc,
  getDoc,
  setDoc,
  updateDoc
} from "firebase/firestore";

const PROJECT_ID = "menudado-rules-test";
const HASH = "a".repeat(64);
const ELIGIBILITY_HASH = "b".repeat(64);
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8")
    }
  });
});

afterEach(async () => testEnv.clearFirestore());
after(async () => testEnv.cleanup());

function validDocument() {
  const now = Timestamp.now();
  return {
    schemaVersion: 1,
    semanticHash: HASH,
    semanticKey: "pasta|tomato|sauce",
    language: "SPANISH",
    name: "Pasta con tomate",
    description: "Pasta integral con salsa de tomate.",
    notes: "Lista en 10 minutos.",
    calories: 430,
    healthStatus: "HEALTHY",
    healthReason: "Incluye cereal y tomate.",
    healthSuggestion: "Acompaña con verduras.",
    cuisineInspiration: "ITALIAN",
    shoppingProducts: [
      {
        key: "pasta-integral",
        normalizedName: "pasta integral",
        displayName: "Pasta integral"
      }
    ],
    eligibilityKeys: [ELIGIBILITY_HASH],
    createdAt: now,
    updatedAt: now
  };
}
```

Después cubrir exactamente:

```javascript
test("authenticated user can create and read sanitized hive menu", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  await assertSucceeds(setDoc(doc(db, "sharedAiMenus", HASH), validDocument()));
  await assertSucceeds(getDoc(doc(db, "sharedAiMenus", HASH)));
});

test("unauthenticated user cannot read hive", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "sharedAiMenus", HASH)));
});

test("private menus remain isolated by uid", async () => {
  const ownerDb = testEnv.authenticatedContext("owner").firestore();
  const otherDb = testEnv.authenticatedContext("other").firestore();
  await assertSucceeds(setDoc(doc(ownerDb, "users/owner/menus/1"), { name: "Privado" }));
  await assertFails(getDoc(doc(otherDb, "users/owner/menus/1")));
});

test("identity and unknown fields are rejected", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  await assertFails(
    setDoc(doc(db, "sharedAiMenus", HASH), { ...validDocument(), uid: "user-a" })
  );
});

test("recipe overwrite and delete are rejected", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  await assertSucceeds(setDoc(doc(db, "sharedAiMenus", HASH), validDocument()));
  await assertFails(updateDoc(doc(db, "sharedAiMenus", HASH), { name: "Alterado" }));
  await assertFails(deleteDoc(doc(db, "sharedAiMenus", HASH)));
});

test("invalid eligibility hash is rejected on update", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  await assertSucceeds(setDoc(doc(db, "sharedAiMenus", HASH), validDocument()));
  await assertFails(
    updateDoc(doc(db, "sharedAiMenus", HASH), {
      eligibilityKeys: [ELIGIBILITY_HASH, "invalid"]
    })
  );
});
```

`validDocument()` debe contener todos los campos del diseño y timestamps del
cliente iguales a `Timestamp.now()` para el emulador.

- [ ] **Step 3: Ejecutar RED con las reglas actuales**

Run:

```bash
npm ci --prefix qa
firebase emulators:exec --only firestore \
  "npm run test:firestore-rules --prefix qa"
```

Expected: FAIL porque las reglas actuales no permiten `sharedAiMenus`.

- [ ] **Step 4: Implementar reglas cerradas**

Conservar el bloque privado y añadir funciones:

```text
function isSignedIn() {
  return request.auth != null;
}

function isHash(value) {
  return value is string && value.matches('^[a-f0-9]{64}$');
}

function hasValidHiveShape(id) {
  return request.resource.data.keys().hasOnly([
      'schemaVersion', 'semanticHash', 'semanticKey', 'language',
      'name', 'description', 'notes',
      'calories', 'healthStatus', 'healthReason', 'healthSuggestion',
      'cuisineInspiration', 'shoppingProducts', 'eligibilityKeys',
      'createdAt', 'updatedAt'
    ])
    && request.resource.data.schemaVersion == 1
    && request.resource.data.semanticHash == id
    && isHash(request.resource.data.semanticHash)
    && request.resource.data.semanticKey is string
    && request.resource.data.semanticKey.size() <= 120
    && request.resource.data.name is string
    && request.resource.data.name.size() <= 120
    && request.resource.data.description is string
    && request.resource.data.description.size() <= 3000
    && request.resource.data.notes is string
    && request.resource.data.notes.size() <= 1000
    && request.resource.data.language in ['SPANISH', 'ENGLISH', 'FRENCH']
    && request.resource.data.calories is int
    && request.resource.data.calories > 0
    && request.resource.data.calories <= 5000
    && request.resource.data.healthStatus in [
      'HEALTHY', 'IMPROVABLE', 'UNHEALTHY'
    ]
    && request.resource.data.healthReason is string
    && request.resource.data.healthReason.size() <= 1000
    && request.resource.data.healthSuggestion is string
    && request.resource.data.healthSuggestion.size() <= 1000
    && request.resource.data.cuisineInspiration is string
    && request.resource.data.shoppingProducts is list
    && request.resource.data.shoppingProducts.size() <= 20
    && request.resource.data.eligibilityKeys is list
    && request.resource.data.eligibilityKeys.size() == 1
    && isHash(request.resource.data.eligibilityKeys[0])
    && request.resource.data.createdAt is timestamp
    && request.resource.data.updatedAt is timestamp;
}
```

Match:

```text
match /sharedAiMenus/{semanticHash} {
  allow read: if isSignedIn();
  allow create: if isSignedIn() && hasValidHiveShape(semanticHash);
  allow update: if isSignedIn()
    && request.resource.data.diff(resource.data).affectedKeys()
      .hasOnly(['eligibilityKeys', 'updatedAt'])
    && request.resource.data.eligibilityKeys is list
    && request.resource.data.eligibilityKeys.size()
      >= resource.data.eligibilityKeys.size()
    && request.resource.data.eligibilityKeys.size()
      <= resource.data.eligibilityKeys.size() + 1
    && request.resource.data.eligibilityKeys.size() <= 100
    && isHash(
      request.resource.data.eligibilityKeys[
        request.resource.data.eligibilityKeys.size() - 1
      ]
    )
    && resource.data.eligibilityKeys.toSet()
      .difference(request.resource.data.eligibilityKeys.toSet()).size() == 0
    && request.resource.data.eligibilityKeys.toSet()
      .difference(resource.data.eligibilityKeys.toSet()).size() <= 1;
  allow delete: if false;
}
```

- [ ] **Step 5: Registrar solo el emulador**

La consulta usa un único `array-contains("eligibilityKeys", ...)`, cubierto por
el índice automático de Firestore. No crear `firestore.indexes.json`.

Actualizar `firebase.json` sin cambiar el path de reglas:

```json
{
  "firestore": {
    "rules": "firestore.rules"
  },
  "emulators": {
    "firestore": {
      "port": 8080
    }
  }
}
```

- [ ] **Step 6: Ejecutar GREEN**

Run:

```bash
firebase emulators:exec --only firestore \
  "npm run test:firestore-rules --prefix qa"
```

Expected: todas las pruebas Node PASS y el emulador se detiene automáticamente.

- [ ] **Step 7: Commit**

```bash
git add \
  firestore.rules \
  firebase.json \
  qa/package.json \
  qa/package-lock.json \
  qa/firestore-rules.test.mjs
git commit -m "test: secure shared AI menu collection"
```

---

### Task 6: Orquestar espera lenta y fallback en el ViewModel

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:61-105,135-175,881-1000`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] **Step 1: Añadir fake de gateway a los tests**

El fake debe registrar búsquedas y contribuciones:

```kotlin
private class RecordingAiMenuHiveGateway : AiMenuHiveGateway {
    var searchResult: Result<AiMenuHiveCandidate?> = Result.success(null)
    val searches = mutableListOf<AiMenuHiveSearchRequest>()
    val contributions = mutableListOf<AiMenuHiveContribution>()

    override suspend fun findCompatibleMenu(
        request: AiMenuHiveSearchRequest
    ): Result<AiMenuHiveCandidate?> {
        searches += request
        return searchResult
    }

    override suspend fun contribute(
        contribution: AiMenuHiveContribution
    ): Result<Unit> {
        contributions += contribution
        return Result.success(Unit)
    }
}
```

Inicializarlo en `setUp` y entregarlo al ViewModel.

Añadir builders compartidos por los tests de Tasks 6, 7 y 9:

```kotlin
private fun sampleGeneratedMenu(
    name: String = "Idea recuperada",
    deduplicationKey: String = "pasta|tomato|sauce"
) = GeneratedMenu(
    name = name,
    description = "Pasta integral con salsa de tomate.",
    notes = "Lista en 10 minutos.",
    calories = 430,
    healthAnalysis = HealthAnalysis(
        status = HealthStatus.HEALTHY,
        reason = "Incluye cereal y tomate.",
        suggestion = "Acompaña con verduras.",
        calories = 430
    ),
    shoppingProducts = listOf(
        requireNotNull(ShoppingProduct.fromAi("Pasta integral")),
        requireNotNull(ShoppingProduct.fromAi("Tomate"))
    ),
    deduplicationKey = deduplicationKey
)

private fun sampleHiveCandidate(
    name: String = "Idea recuperada"
): AiMenuHiveCandidate {
    val generated = sampleGeneratedMenu(name = name)
    val identity = requireNotNull(
        AiMenuHiveIdentity.from(
            AppLanguage.SPANISH,
            generated.deduplicationKey
        )
    )
    return AiMenuHiveCandidate(
        generatedMenu = generated,
        cuisineInspiration = CuisineInspiration.ITALIAN,
        semanticHash = identity.semanticHash,
        source = AiMenuHiveLookupSource.SERVER
    )
}
```

- [ ] **Step 2: Escribir pruebas fallidas de fases**

```kotlin
@Test
fun `slow live request changes copy phase while dice remains active`() = runTest(dispatcher) {
    analyzer.generateDelayMillis = 13_000L

    viewModel.generateMenuIdea()
    runCurrent()
    assertEquals(AiGenerationPhase.GENERATING, viewModel.uiState.value.aiGenerationPhase)

    advanceTimeBy(12_000L)
    runCurrent()

    assertEquals(AiGenerationPhase.GENERATING_SLOW, viewModel.uiState.value.aiGenerationPhase)
    assertTrue(viewModel.uiState.value.isGeneratingMenu)

    advanceTimeBy(1_000L)
    advanceUntilIdle()
    assertEquals(AiGenerationPhase.IDLE, viewModel.uiState.value.aiGenerationPhase)
}
```

- [ ] **Step 3: Escribir pruebas fallidas de fallback**

Cubrir éxito, miss y ausencia en validaciones:

```kotlin
@Test
fun `provider failure keeps dice active and opens compatible hive result`() = runTest(dispatcher) {
    analyzer.generateFailure = IllegalStateException("internal")
    hive.searchResult = Result.success(sampleHiveCandidate())

    viewModel.generateMenuIdea()
    advanceUntilIdle()

    assertEquals(1, hive.searches.size)
    assertTrue(viewModel.uiState.value.showGeneratedMenuDetail)
    assertEquals("Idea recuperada", viewModel.uiState.value.name)
    assertEquals(AiGenerationPhase.IDLE, viewModel.uiState.value.aiGenerationPhase)
    assertFalse(viewModel.uiState.value.isGeneratingMenu)
}

@Test
fun `hive miss preserves original localized failure`() = runTest(dispatcher) {
    analyzer.generateFailure = IllegalStateException("internal")
    hive.searchResult = Result.success(null)

    viewModel.generateMenuIdea()
    advanceUntilIdle()

    assertEquals("El servicio tuvo un problema temporal. Inténtalo nuevamente más tarde.",
        viewModel.uiState.value.message)
}

@Test
fun `local validation and throttle never query hive`() = runTest(dispatcher) {
    dietaryProfileStore.storedProfile = DietaryProfile(
        ageRange = "18+ años",
        isVegan = true
    )
    viewModel.updateAiBaseIngredients("queso")
    viewModel.generateMenuIdea()
    advanceUntilIdle()
    assertTrue(hive.searches.isEmpty())
}
```

- [ ] **Step 4: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest"
```

Expected: FAIL por fase y dependencia inexistentes.

- [ ] **Step 5: Introducir fase explícita sin duplicar estado**

Añadir:

```kotlin
enum class AiGenerationPhase {
    IDLE,
    GENERATING,
    GENERATING_SLOW,
    SEARCHING_HIVE;

    val isActive: Boolean
        get() = this != IDLE
}
```

En `MenuDadoUiState`, sustituir el constructor booleano por:

```kotlin
val aiGenerationPhase: AiGenerationPhase = AiGenerationPhase.IDLE,
val generatedDeduplicationKey: String? = null,
val generatedOrigin: GeneratedMenuOrigin? = null,
val generatedSemanticHash: String? = null
) {
    val isGeneratingMenu: Boolean
        get() = aiGenerationPhase.isActive
}

enum class GeneratedMenuOrigin { LIVE_AI, HIVE_FALLBACK }
```

Añadir al constructor:

```kotlin
private val aiMenuHive: AiMenuHiveGateway = NoOpAiMenuHiveGateway
```

- [ ] **Step 6: Implementar transición lenta y búsqueda**

Al comenzar:

```kotlin
_uiState.update {
    it.copy(
        aiGenerationPhase = AiGenerationPhase.GENERATING,
        message = null,
        isAiRetryNoticeVisible = false
    )
}
```

Dentro de la corrutina:

```kotlin
val slowPhaseJob = launch {
    delay(AI_GENERATION_SLOW_NOTICE_MILLIS)
    _uiState.update { current ->
        if (current.aiGenerationPhase == AiGenerationPhase.GENERATING) {
            current.copy(aiGenerationPhase = AiGenerationPhase.GENERATING_SLOW)
        } else {
            current
        }
    }
}
```

En éxito, guardar clave/origen. En fallo:

1. calcular y registrar `AiFailureNotice`;
2. cambiar a `SEARCHING_HIVE`;
3. buscar con idioma, tipo, público, perfil, ingredientes base y
   `recentHiveSemanticHashes.toSet()`;
4. si hay candidato, limpiar el mensaje y poblar el modal;
5. si no hay candidato o hay error de colmena, conservar el aviso original.

El éxito live debe añadir a su `copy` actual:

```kotlin
generatedDeduplicationKey = generated.deduplicationKey,
generatedOrigin = GeneratedMenuOrigin.LIVE_AI,
generatedSemanticHash = AiMenuHiveIdentity.from(
    AppLanguage.fromLocale(),
    generated.deduplicationKey
)?.semanticHash
```

La rama de fallo debe usar:

```kotlin
val notice = error.toAiFailureNotice(
    clockMillisProvider(),
    currentLanguage()
)
showAiFailureNotice(notice)
_uiState.update {
    it.copy(aiGenerationPhase = AiGenerationPhase.SEARCHING_HIVE)
}
val fallback = aiMenuHive.findCompatibleMenu(
    AiMenuHiveSearchRequest(
        language = AppLanguage.fromLocale(),
        mealType = mealType,
        audience = audience,
        profile = profile,
        baseIngredients = state.aiBaseIngredients.trim(),
        recentSemanticHashes = recentHiveSemanticHashes.toSet()
    )
).getOrNull()

fallback?.let { candidate ->
    rememberHiveSemanticHash(candidate.semanticHash)
    _uiState.update {
        it.copy(
            name = candidate.generatedMenu.name,
            description = candidate.generatedMenu.description,
            notes = candidate.generatedMenu.notes,
            calories = candidate.generatedMenu.calories,
            generatedHealthAnalysis = candidate.generatedMenu.healthAnalysis,
            generatedShoppingProducts = candidate.generatedMenu.shoppingProducts,
            generatedCuisineInspiration = candidate.cuisineInspiration,
            generatedDeduplicationKey = candidate.generatedMenu.deduplicationKey,
            generatedOrigin = GeneratedMenuOrigin.HIVE_FALLBACK,
            generatedSemanticHash = candidate.semanticHash,
            message = null,
            isAiRetryNoticeVisible = false,
            showGeneratedMenuDetail = true
        )
    }
}
```

Conservar el `trackAiMenuGenerationFinished` live existente. Task 9 añadirá el
evento agregado del resultado de colmena.

Mantener una ventana pequeña en memoria:

```kotlin
private val recentHiveSemanticHashes = ArrayDeque<String>()

private fun rememberHiveSemanticHash(hash: String) {
    recentHiveSemanticHashes.remove(hash)
    recentHiveSemanticHashes.addLast(hash)
    while (recentHiveSemanticHashes.size > MAX_RECENT_HIVE_HASHES) {
        recentHiveSemanticHashes.removeFirst()
    }
}
```

Llamarla solo después de aceptar un candidato.

En `finally`:

```kotlin
slowPhaseJob.cancel()
_uiState.update { it.copy(aiGenerationPhase = AiGenerationPhase.IDLE) }
```

Añadir:

```kotlin
private const val AI_GENERATION_SLOW_NOTICE_MILLIS = 12 * 1000L
private const val MAX_RECENT_HIVE_HASHES = 8
```

- [ ] **Step 7: Ejecutar GREEN y pruebas de timeout existentes**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest"
```

Expected: PASS, incluidos los tests existentes del timeout de 45 segundos.

- [ ] **Step 8: Commit**

```bash
git add \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "feat: fallback to compatible shared AI menu"
```

---

### Task 7: Contribuir solo ideas IA guardadas y sin editar

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt:288-340,443-470,720-790,1360-1420`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt:145-170`
- Modify: `app/src/main/java/com/menudado/MainActivity.kt:88-108,280-330`

- [ ] **Step 1: Escribir pruebas fallidas de contribución**

```kotlin
@Test
fun `saving untouched live AI idea contributes after local save`() = runTest(dispatcher) {
    analyzer.generatedMenu = sampleGeneratedMenu(
        name = "Pasta con tomate",
        deduplicationKey = "pasta|tomato|sauce"
    )

    viewModel.generateMenuIdea()
    advanceUntilIdle()
    viewModel.saveGeneratedMenuIdea()
    advanceUntilIdle()

    assertEquals(1, dao.saved.size)
    assertEquals(1, hive.contributions.size)
    assertEquals("pasta|tomato|sauce",
        hive.contributions.single().generatedMenu.deduplicationKey)
}

@Test
fun `editing generated recipe clears identity and never contributes`() = runTest(dispatcher) {
    analyzer.generatedMenu = sampleGeneratedMenu(
        deduplicationKey = "pasta|tomato|sauce"
    )

    viewModel.generateMenuIdea()
    advanceUntilIdle()
    viewModel.updateDescription("Texto cambiado por el usuario")
    viewModel.saveGeneratedMenuIdea()
    advanceUntilIdle()

    assertTrue(hive.contributions.isEmpty())
}

@Test
fun `saving hive fallback does not write it again`() = runTest(dispatcher) {
    analyzer.generateFailure = IllegalStateException("internal")
    hive.searchResult = Result.success(sampleHiveCandidate())

    viewModel.generateMenuIdea()
    advanceUntilIdle()
    viewModel.saveGeneratedMenuIdea()
    advanceUntilIdle()

    assertTrue(hive.contributions.isEmpty())
}
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest"
```

Expected: FAIL porque guardar aún no contribuye ni limpia identidad.

- [ ] **Step 3: Limpiar metadatos en toda edición o descarte**

En `updateName`, `updateDescription`, `updateNotes`,
`updateAiBaseIngredients`, `tryAnotherGeneratedMenuIdea`,
`withoutMenuFormDraft` y `resetForm`, incluir:

```kotlin
generatedDeduplicationKey = null,
generatedOrigin = null,
generatedSemanticHash = null
```

Cambiar tipo o público ya limpia el borrador mediante las rutas existentes y
debe limpiar también esos tres campos.

- [ ] **Step 4: Contribuir después del feedback local-first**

En `saveMenu`, capturar antes de `resetForm`:

```kotlin
val hiveContribution = if (
    state.generatedOrigin == GeneratedMenuOrigin.LIVE_AI &&
    state.generatedDeduplicationKey != null &&
    state.generatedHealthAnalysis != null
) {
    AiMenuHiveContribution(
        language = AppLanguage.fromLocale(),
        mealType = mealType,
        audience = audience,
        profile = dietaryProfileStore.getProfile(audience),
        generatedMenu = GeneratedMenu(
            name = name,
            description = description,
            notes = state.notes.trim(),
            calories = requireNotNull(state.calories),
            healthAnalysis = state.generatedHealthAnalysis,
            shoppingProducts = state.generatedShoppingProducts,
            deduplicationKey = state.generatedDeduplicationKey
        ),
        cuisineInspiration = state.generatedCuisineInspiration
    )
} else {
    null
}
```

Después de incrementar `menuSaveSuccessRevision`, lanzar sin bloquear:

```kotlin
hiveContribution?.let { contribution ->
    viewModelScope.launch {
        aiMenuHive.contribute(contribution)
    }
}
```

No mostrar error si falla.

- [ ] **Step 5: Inyectar Firebase y el toggle**

En `MenuDadoApplication`:

```kotlin
val aiMenuHiveFeatureToggle by lazy { AiMenuHiveFeatureToggle(isEnabled = true) }

val aiMenuHiveGateway: AiMenuHiveGateway by lazy {
    AiMenuHiveRepository(
        dataSource = FirebaseAiMenuHiveDataSource(),
        featureToggle = aiMenuHiveFeatureToggle
    )
}
```

Entregar `aiMenuHiveGateway` al ViewModel.

En `MainActivity`, crear `AiMenuHiveRemoteConfig` con callback al toggle,
añadirlo a los `LaunchedEffect` y refrescarlo junto a los otros Remote Config.

- [ ] **Step 6: Ejecutar GREEN y compile**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest"
./gradlew :app:compileDebugKotlin
```

Expected: PASS y `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt \
  app/src/main/java/com/menudado/MenuDadoApplication.kt \
  app/src/main/java/com/menudado/MainActivity.kt
git commit -m "feat: contribute saved AI menus anonymously"
```

---

### Task 8: Mantener el dado animado y localizar el copy por fase

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:1140-1215,1640-1660`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt:860-875`
- Modify: `app/src/main/res/values/strings.xml:204-205`
- Modify: `app/src/main/res/values-en/strings.xml:202-203`
- Modify: `app/src/main/res/values-fr/strings.xml:202-203`

- [ ] **Step 1: Escribir prueba fallida de recursos por fase**

Reemplazar las expectativas sin parámetro por:

```kotlin
@Test
fun `generacion IA mantiene overlay y cambia copy por fase`() {
    assertTrue(aiGenerationLoadingUsesMenuDadoDiceCube())
    assertEquals(
        R.string.ai_generation_loading_message,
        aiGenerationLoadingMessageRes(AiGenerationPhase.GENERATING)
    )
    assertEquals(
        R.string.ai_generation_slow_message,
        aiGenerationLoadingMessageRes(AiGenerationPhase.GENERATING_SLOW)
    )
    assertEquals(
        R.string.ai_generation_hive_message,
        aiGenerationLoadingMessageRes(AiGenerationPhase.SEARCHING_HIVE)
    )
    assertTrue(aiGenerationLoadingBlocksTouches())
}
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuCardUiStateTest"
```

Expected: FAIL por firma y recursos inexistentes.

- [ ] **Step 3: Añadir recursos ES/EN/FR**

Español:

```xml
<string name="ai_generation_slow_title">Seguimos preparando tu idea</string>
<string name="ai_generation_slow_message">La IA está tardando más de lo habitual. Seguimos buscando una idea para ti.</string>
<string name="ai_generation_hive_title">Buscando una idea compatible</string>
<string name="ai_generation_hive_message">Estamos buscando otra idea que encaje con tu perfil.</string>
```

Inglés:

```xml
<string name="ai_generation_slow_title">We’re still preparing your idea</string>
<string name="ai_generation_slow_message">AI is taking longer than usual. We’re still looking for an idea for you.</string>
<string name="ai_generation_hive_title">Finding a compatible idea</string>
<string name="ai_generation_hive_message">We’re looking for another idea that fits your profile.</string>
```

Francés:

```xml
<string name="ai_generation_slow_title">Nous préparons toujours votre idée</string>
<string name="ai_generation_slow_message">L’IA prend plus de temps que prévu. Nous continuons à chercher une idée pour vous.</string>
<string name="ai_generation_hive_title">Recherche d’une idée compatible</string>
<string name="ai_generation_hive_message">Nous cherchons une autre idée adaptée à votre profil.</string>
```

- [ ] **Step 4: Representar fase sin detener el dado**

Pasar `state.aiGenerationPhase` a `AiGenerationLoadingOverlay`. Cambiar helpers:

```kotlin
@StringRes
internal fun aiGenerationLoadingTitleRes(phase: AiGenerationPhase): Int = when (phase) {
    AiGenerationPhase.GENERATING -> R.string.ai_generation_loading_title
    AiGenerationPhase.GENERATING_SLOW -> R.string.ai_generation_slow_title
    AiGenerationPhase.SEARCHING_HIVE -> R.string.ai_generation_hive_title
    AiGenerationPhase.IDLE -> R.string.ai_generation_loading_title
}

@StringRes
internal fun aiGenerationLoadingMessageRes(phase: AiGenerationPhase): Int = when (phase) {
    AiGenerationPhase.GENERATING -> R.string.ai_generation_loading_message
    AiGenerationPhase.GENERATING_SLOW -> R.string.ai_generation_slow_message
    AiGenerationPhase.SEARCHING_HIVE -> R.string.ai_generation_hive_message
    AiGenerationPhase.IDLE -> R.string.ai_generation_loading_message
}
```

No cambiar `AiGenerationLoadingDice`, ciclos, progreso ni bloqueo táctil.

- [ ] **Step 5: Ejecutar GREEN y comprobar paridad**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuCardUiStateTest"
python3 - <<'PY'
import xml.etree.ElementTree as ET
from pathlib import Path
files = [
    Path("app/src/main/res/values/strings.xml"),
    Path("app/src/main/res/values-en/strings.xml"),
    Path("app/src/main/res/values-fr/strings.xml"),
]
keys = [{e.attrib["name"] for e in ET.parse(f).getroot() if e.tag == "string"} for f in files]
assert keys[0] == keys[1] == keys[2], [len(k) for k in keys]
print(f"{len(keys[0])} localized string keys aligned")
PY
```

Expected: test PASS y mensaje de claves alineadas.

- [ ] **Step 6: Commit**

```bash
git add \
  app/src/main/java/com/menudado/ui/MenuDadoScreen.kt \
  app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml
git commit -m "feat: show progressive AI fallback loading"
```

---

### Task 9: Añadir telemetría agregada y documentación

**Files:**
- Modify: `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt:100-115,210-225`
- Modify: `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt:269-295,360-440`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `docs/project-context.md`
- Modify: `docs/privacy-policy.md`

- [ ] **Step 1: Escribir prueba fallida de telemetría sin contenido**

En el fake analítico y test:

```kotlin
@Test
fun `hive fallback analytics contains outcome and failure type only`() = runTest(dispatcher) {
    analyzer.generateFailure = IllegalStateException("internal")
    hive.searchResult = Result.success(sampleHiveCandidate(name = "Nombre privado"))

    viewModel.generateMenuIdea()
    advanceUntilIdle()

    assertTrue(
        analytics.events.any {
            it == "ai_menu_hive_fallback:LUNCH:hit:temporary:0"
        }
    )
    assertTrue(analytics.events.none { "Nombre privado" in it })
}
```

En `RecordingMenuDadoAnalytics` añadir:

```kotlin
override fun trackAiMenuHiveFallback(
    mealType: MealType,
    result: String,
    triggerFailureType: String,
    durationMillis: Long
) {
    events += "ai_menu_hive_fallback:${mealType.name}:$result:" +
        "$triggerFailureType:$durationMillis"
}
```

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest"
```

Expected: FAIL por evento inexistente.

- [ ] **Step 3: Añadir contrato agregado**

En `MenuDadoAnalytics`:

```kotlin
fun trackAiMenuHiveFallback(
    mealType: MealType,
    result: String,
    triggerFailureType: String,
    durationMillis: Long
)
```

Implementar NoOp y Firebase con evento `ai_menu_hive_fallback` y parámetros:
`meal_type`, `status`, `failure_type` y `duration_ms`. Valores permitidos para
`result`: `hit`, `cache_hit`, `miss`, `error`. No pasar hash, nombre,
ingredientes, restricciones ni ID.

Llamar una sola vez por fallo real después de resolver la búsqueda. Medir con
`clockMillisProvider()` y proteger el resultado con `coerceAtLeast(0L)`.

- [ ] **Step 4: Actualizar contexto funcional**

En `docs/project-context.md`, añadir al flujo IA:

- cambio de copy a los 12 s;
- fallback solo tras fallo real;
- coincidencia exacta de perfil y validación local;
- colmena anónima separada y máximo 12 lecturas;
- deduplicación semántica;
- contribución solo de idea IA guardada sin edición;
- Remote Config `ai_menu_hive_enabled`;
- límite de garantía y ausencia de relajación alimentaria;
- reglas `sharedAiMenus` y pruebas de emulador;
- ausencia de Room/Functions/embeddings/segunda llamada.

- [ ] **Step 5: Actualizar privacidad**

Añadir a `docs/privacy-policy.md`, en lenguaje general:

```text
Cuando guardas sin modificar una propuesta creada por la IA, MenuDado puede
reutilizar de forma anónima el contenido de esa propuesta para ofrecer una
alternativa compatible si la IA no responde a otra persona. No se comparte tu
identificador, correo, foto, ingredientes escritos inicialmente ni el contenido
legible de tu perfil alimentario.
```

Actualizar `Ultima actualizacion` a `27 de julio de 2026`.

No afirmar anonimato de Analytics más allá de lo implementado ni ocultar el uso
compartido.

- [ ] **Step 6: Ejecutar GREEN y revisión documental**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.menudado.ui.MenuDadoViewModelTest"
rg -n "sharedAiMenus|ai_menu_hive_enabled|12 segundos|anónima|anonima" \
  docs/project-context.md docs/privacy-policy.md
git diff --check
```

Expected: test PASS, coincidencias en ambos documentos y diff limpio.

- [ ] **Step 7: Commit**

```bash
git add \
  app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt \
  docs/project-context.md \
  docs/privacy-policy.md
git commit -m "docs: document anonymous AI menu fallback"
```

---

### Task 10: Verificación integral y preparación de QA manual

**Files:**
- Verify only: all files touched above

- [ ] **Step 1: Ejecutar suite unitaria limpia**

Run:

```bash
./gradlew :app:clean :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`; el total será mayor que los 305 tests documentados
antes de esta función.

- [ ] **Step 2: Ejecutar reglas en emulador**

Run:

```bash
firebase emulators:exec --only firestore \
  "npm run test:firestore-rules --prefix qa"
```

Expected: todas las pruebas PASS, sin conexión a Firestore producción.

- [ ] **Step 3: Ejecutar compile, lint y artefacto**

Run:

```bash
./gradlew :app:compileDebugKotlin :app:lintRelease :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, lint sin errores bloqueantes y APK debug generado.

- [ ] **Step 4: Verificar ausencia de infraestructura con coste nuevo**

Run:

```bash
test ! -d functions
rg -n "generateContent\\(" app/src/main/java/com/menudado/ai/FirebaseHealthAnalyzer.kt
rg -n "Cloud Functions|embedding|vector" \
  app/src/main app/build.gradle.kts firebase.json || true
```

Expected:

- no directorio `functions`;
- las llamadas `generateContent` siguen siendo las existentes de generación y
  análisis, sin una segunda llamada para deduplicación o fallback;
- ninguna dependencia de embeddings o búsqueda vectorial.

- [ ] **Step 5: Verificar coste acotado y privacidad por búsqueda estática**

Run:

```bash
rg -n "AI_MENU_HIVE_QUERY_LIMIT = 12|\\.limit\\(request\\.limit\\)" \
  app/src/main/java/com/menudado
rg -n "uid|email|imageUri|baseIngredients|profile" \
  app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt
```

Expected: límite 12 presente; cualquier coincidencia sensible aparece solo en
validación negativa/test o no aparece en el mapa Firestore.

- [ ] **Step 6: Ejecutar checks de repositorio**

Run:

```bash
git diff --check
git status --short
```

Expected: diff limpio y únicamente cambios previstos si aún no se hizo commit.

- [ ] **Step 7: QA manual guiada**

En `debug` con emulador o Firebase debug:

1. Generación <12 s: copy actual, cero consultas de colmena, modal normal.
2. Generación >12 s: mismo dado girando y copy de demora.
3. Forzar timeout/internal/red: fase de búsqueda y modal de candidato.
4. Cortar red con candidato cacheado: `cache_hit`.
5. Perfil incompatible: candidato rechazado y error original.
6. Guardar idea live sin editar: una contribución.
7. Editar cualquier campo y guardar: ninguna contribución.
8. Guardar fallback: ninguna escritura redundante.
9. Guardar “pasta con tomate” y “espaguetis en salsa de tomate”: un hash.
10. Desactivar `ai_menu_hive_enabled`: cero lecturas/escrituras.
11. Revisar Usage de Firestore: cada fallo lee como máximo 12 documentos.

Registrar como riesgo residual cualquier caso sin candidato; no afirmar garantía
absoluta ni despliegue productivo sin evidencia.

- [ ] **Step 8: Cerrar sin despliegue externo**

No desplegar `firestore.rules`, índices ni Remote Config automáticamente. Revisar
que `git status --short` esté limpio después de los commits anteriores. Cualquier
corrección encontrada en Tasks 1-9 debe volver a su tarea, repetir su comando de
verificación y usar el commit específico de esa tarea.

El despliegue externo requiere revisión del diff, confirmación del proyecto
Firebase objetivo y aprobación explícita.
