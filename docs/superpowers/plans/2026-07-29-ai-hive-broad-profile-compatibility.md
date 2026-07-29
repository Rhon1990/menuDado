# AI Hive Broad Profile Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Compartir cada receta IA válida entre perfiles alimentarios compatibles del mismo idioma, tipo de comida y público, manteniendo una validación local estricta y haciendo observable el resultado de la contribución.

**Architecture:** Añadir una `scopeKey` anónima basada en idioma, comida y público, incluirla en la identidad remota v3 para separar físicamente adulto, peques y bebé, y filtrar todos los candidatos mediante `DietaryProfile.accepts`. Mantener `eligibilityKeys` para compatibilidad, ampliar el mapper y las reglas con una `scopeKey` inmutable, y devolver un resultado tipado desde la contribución para registrar éxito, omisión o error sin datos sensibles.

**Tech Stack:** Kotlin, Coroutines, JUnit 4, Firebase Firestore, Firebase Analytics, Firestore Rules Emulator, Node.js, Gradle.

---

## Estructura de archivos

- `app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt`: genera la huella
  estable del ámbito y la identidad remota v3 que separa físicamente públicos.
- `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`: define el tipo de
  consulta, el resultado de contribución y la estrategia ámbito primero,
  compatibilidad heredada después.
- `app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt`: prueba la
  separación por idioma, comida y público.
- `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`: prueba
  contribución, consulta amplia, filtros alimentarios, separación por edad y
  compatibilidad heredada.
- `app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt`: consulta el
  campo correcto y serializa la `scopeKey` inmutable.
- `app/src/test/java/com/menudado/backend/AiMenuHiveDataSourceTest.kt`: prueba el
  mapper nuevo y la lectura de documentos heredados.
- `firestore.rules`: valida creaciones v3 y la inmutabilidad de `scopeKey`.
- `qa/firestore-rules.test.mjs`: cubre las reglas con el emulador.
- `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`: amplía el
  contrato de telemetría con un único resultado terminal.
- `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`: envía
  `ai_menu_hive_contribution`.
- `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`: consume el resultado
  de contribución sin afectar el guardado privado.
- `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`: prueba orden,
  cardinalidad, errores y privacidad del evento.
- `docs/project-context.md`: documenta el nuevo contrato funcional.

No se crearán tablas Room, pantallas, recursos visuales, campos de Gemini ni
llamadas adicionales a IA.

### Task 1: Identidad del ámbito y contrato de consulta

**Files:**
- Modify: `app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt`
- Modify: `app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt`
- Modify: `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`
- Modify: `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`

- [ ] **Step 1: Escribir los tests fallidos de la huella de ámbito**

Añadir a `AiMenuHiveIdentityTest`:

```kotlin
@Test
fun `scope key ignores dietary profile dimensions`() {
    val unrestricted = DietaryProfile(ageRange = "18+ años")
    val vegan = DietaryProfile(
        ageRange = "18+ años",
        isVegan = true,
        hasAllergies = true,
        allergens = setOf(DietaryAllergen.MILK)
    )

    assertNotEquals(
        AiMenuHiveIdentity.eligibilityKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT,
            unrestricted
        ),
        AiMenuHiveIdentity.eligibilityKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT,
            vegan
        )
    )
    assertEquals(
        AiMenuHiveIdentity.scopeKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT
        ),
        AiMenuHiveIdentity.scopeKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT
        )
    )
}

@Test
fun `scope key keeps audience meal and language separated`() {
    val adultLunch = AiMenuHiveIdentity.scopeKey(
        AppLanguage.SPANISH,
        MealType.LUNCH,
        MenuAudience.ADULT
    )

    assertNotEquals(
        adultLunch,
        AiMenuHiveIdentity.scopeKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.BABY
        )
    )
    assertNotEquals(
        adultLunch,
        AiMenuHiveIdentity.scopeKey(
            AppLanguage.SPANISH,
            MealType.DINNER,
            MenuAudience.ADULT
        )
    )
    assertNotEquals(
        adultLunch,
        AiMenuHiveIdentity.scopeKey(
            AppLanguage.ENGLISH,
            MealType.LUNCH,
            MenuAudience.ADULT
        )
    )
}

@Test
fun `scoped identity stores adult and baby recipes in different documents`() {
    val adult = requireNotNull(
        AiMenuHiveIdentity.scoped(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT,
            "curry|chickpeas+spinach|stewed"
        )
    )
    val baby = requireNotNull(
        AiMenuHiveIdentity.scoped(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.BABY,
            "curry|chickpeas+spinach|stewed"
        )
    )

    assertEquals(adult.canonicalKey, baby.canonicalKey)
    assertNotEquals(adult.semanticHash, baby.semanticHash)
}
```

- [ ] **Step 2: Ejecutar los tests de identidad para observar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.domain.AiMenuHiveIdentityTest \
  --console=plain
```

Expected: `FAILED` porque `scopeKey` todavía no existe.

- [ ] **Step 3: Implementar la huella de ámbito mínima**

En `AiMenuHiveIdentity` añadir:

```kotlin
fun scopeKey(
    language: AppLanguage,
    mealType: MealType,
    audience: MenuAudience
): String {
    val raw = listOf(
        "v$SCOPE_SCHEMA_VERSION",
        language.name,
        mealType.name,
        audience.name
    ).joinToString("|")
    return sha256(raw)
}

fun scoped(
    language: AppLanguage,
    mealType: MealType,
    audience: MenuAudience,
    rawKey: String?
): AiMenuSemanticIdentity? {
    val canonical = from(language, rawKey) ?: return null
    val scope = scopeKey(language, mealType, audience)
    return AiMenuSemanticIdentity(
        canonicalKey = canonical.canonicalKey,
        semanticHash = sha256(
            "v$SCOPED_IDENTITY_VERSION|$scope|${canonical.canonicalKey}"
        )
    )
}
```

Y junto a la versión del perfil:

```kotlin
private const val SCOPE_SCHEMA_VERSION = 1
private const val SCOPED_IDENTITY_VERSION = 3
```

No cambiar `eligibilityKey`; continúa siendo el índice heredado.

- [ ] **Step 4: Ejecutar los tests de identidad para observar GREEN**

Run el comando de Step 2. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Escribir los tests fallidos del repositorio**

En `AiMenuHiveRepositoryTest` cubrir estos contratos:

```kotlin
@Test
fun `broad scope query is used first with bounded limit`() = runTest {
    val dataSource = RecordingAiMenuHiveDataSource(
        response = { Result.success(listOf(sharedMenu("lentils|vegetables|stew"))) }
    )
    val repository = AiMenuHiveRepository(
        dataSource,
        AiMenuHiveFeatureToggle(true)
    ) { 0 }

    repository.findCompatibleMenu(request()).getOrThrow()

    val query = dataSource.requests.first()
    assertEquals(AiMenuHiveQueryField.SCOPE, query.queryField)
    assertEquals(
        AiMenuHiveIdentity.scopeKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.ADULT
        ),
        query.key
    )
    assertEquals(36L, query.limit)
}

@Test
fun `vegan recipe from broad adult scope is accepted by unrestricted adult`() = runTest {
    val vegan = sharedMenu(
        key = "curry|chickpeas+spinach|stewed",
        description = "Garbanzos, espinacas, tomate y especias."
    )
    val repository = AiMenuHiveRepository(
        RecordingAiMenuHiveDataSource(
            response = { Result.success(listOf(vegan)) }
        ),
        AiMenuHiveFeatureToggle(true)
    ) { 0 }

    val result = repository.findCompatibleMenu(
        request(profile = DietaryProfile(ageRange = "18+ años"))
    ).getOrThrow()

    assertEquals(vegan.semanticHash, result?.semanticHash)
}

@Test
fun `broad adult candidate is rejected for baby before legacy lookup`() = runTest {
    val adultCurry = sharedMenu(
        key = "curry|chickpeas+spinach|stewed",
        description = "Curry picante de garbanzos para adultos."
    )
    val dataSource = RecordingAiMenuHiveDataSource(
        response = { request ->
            if (request.queryField == AiMenuHiveQueryField.SCOPE) {
                Result.success(listOf(adultCurry))
            } else {
                Result.success(emptyList())
            }
        }
    )
    val repository = AiMenuHiveRepository(
        dataSource,
        AiMenuHiveFeatureToggle(true)
    ) { 0 }

    val result = repository.findCompatibleMenu(
        request(
            audience = MenuAudience.BABY,
            profile = DietaryProfile(ageRange = MenuAudience.BABY.defaultAgeRange)
        )
    ).getOrThrow()

    assertNull(result)
    assertEquals(
        AiMenuHiveIdentity.scopeKey(
            AppLanguage.SPANISH,
            MealType.LUNCH,
            MenuAudience.BABY
        ),
        dataSource.requests.first().key
    )
}

@Test
fun `legacy exact query runs only after broad scope has no safe candidate`() = runTest {
    val legacy = sharedMenu("rice|vegetables|bowl")
    val dataSource = RecordingAiMenuHiveDataSource(
        response = { request ->
            when (request.queryField) {
                AiMenuHiveQueryField.SCOPE -> Result.success(emptyList())
                AiMenuHiveQueryField.ELIGIBILITY -> Result.success(listOf(legacy))
            }
        }
    )
    val repository = AiMenuHiveRepository(
        dataSource,
        AiMenuHiveFeatureToggle(true)
    ) { 0 }

    val result = repository.findCompatibleMenu(request()).getOrThrow()

    assertEquals(legacy.semanticHash, result?.semanticHash)
    assertEquals(
        listOf(AiMenuHiveQueryField.SCOPE, AiMenuHiveQueryField.ELIGIBILITY),
        dataSource.requests.map(AiMenuHiveDataSourceRequest::queryField)
            .distinct()
    )
}
```

Actualizar el fake para resolver por petición:

```kotlin
private class RecordingAiMenuHiveDataSource(
    private val response: (AiMenuHiveDataSourceRequest) -> Result<List<SharedAiMenu>> =
        { Result.success(emptyList()) }
) : AiMenuHiveDataSource {
    val requests = mutableListOf<AiMenuHiveDataSourceRequest>()
    val upserts = mutableListOf<SharedAiMenu>()

    override suspend fun fetch(
        request: AiMenuHiveDataSourceRequest
    ): Result<List<SharedAiMenu>> {
        requests += request
        return response(request)
    }

    override suspend fun upsert(menu: SharedAiMenu): Result<Unit> {
        upserts += menu
        return Result.success(Unit)
    }
}
```

Conservar casos explícitos de servidor/caché adaptando el lambda para responder
según `request.source`.

- [ ] **Step 6: Ejecutar los tests del repositorio para observar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest \
  --console=plain
```

Expected: `FAILED` porque no existen `AiMenuHiveQueryField`, `key` ni
`scopeKey`
ni la consulta amplia.

- [ ] **Step 7: Implementar el contrato y la consulta ámbito primero**

En `AiMenuHiveRepository.kt`, sustituir la petición específica por:

```kotlin
enum class AiMenuHiveQueryField { SCOPE, ELIGIBILITY }

data class AiMenuHiveDataSourceRequest(
    val queryField: AiMenuHiveQueryField,
    val key: String,
    val source: AiMenuHiveReadSource,
    val limit: Long
)
```

Ampliar el modelo manteniendo compatibilidad con documentos existentes:

```kotlin
data class SharedAiMenu(
    val semanticHash: String,
    val semanticKey: String,
    val language: AppLanguage,
    val generatedMenu: GeneratedMenu,
    val cuisineInspiration: CuisineInspiration?,
    val eligibilityKeys: Set<String>,
    val scopeKey: String? = null
)
```

Extraer la resolución servidor/caché:

```kotlin
private suspend fun fetchWithCache(
    request: AiMenuHiveDataSourceRequest
): Result<Pair<List<SharedAiMenu>, AiMenuHiveLookupSource>> {
    val serverRequest = request.copy(source = AiMenuHiveReadSource.SERVER)
    val server = dataSource.fetch(serverRequest)
    if (server.isSuccess) {
        return Result.success(
            server.getOrThrow() to AiMenuHiveLookupSource.SERVER
        )
    }
    val cache = dataSource.fetch(
        request.copy(source = AiMenuHiveReadSource.CACHE)
    )
    return cache.map { it to AiMenuHiveLookupSource.CACHE }
}
```

Aplicar esta secuencia dentro de `findCompatibleMenu`:

```kotlin
val scopeRequest = AiMenuHiveDataSourceRequest(
    queryField = AiMenuHiveQueryField.SCOPE,
    key = AiMenuHiveIdentity.scopeKey(
        request.language,
        request.mealType,
        request.audience
    ),
    source = AiMenuHiveReadSource.SERVER,
    limit = AI_MENU_HIVE_SCOPE_QUERY_LIMIT
)
val scope = fetchWithCache(scopeRequest).getOrElse {
    return Result.failure(it)
}
selectCandidate(scope.first, request)?.let { selection ->
    return Result.success(selection.toCandidate(scope.second))
}

val legacyRequest = AiMenuHiveDataSourceRequest(
    queryField = AiMenuHiveQueryField.ELIGIBILITY,
    key = AiMenuHiveIdentity.eligibilityKey(
        request.language,
        request.mealType,
        request.audience,
        request.profile
    ),
    source = AiMenuHiveReadSource.SERVER,
    limit = AI_MENU_HIVE_LEGACY_QUERY_LIMIT
)
val legacy = fetchWithCache(legacyRequest).getOrElse {
    return Result.failure(it)
}
val selection = selectCandidate(legacy.first, request)
    ?: return Result.success(null)
return Result.success(selection.toCandidate(legacy.second))
```

Extraer la construcción repetida:

```kotlin
private fun HiveSelection.toCandidate(
    source: AiMenuHiveLookupSource
) = AiMenuHiveCandidate(
    generatedMenu = menu.generatedMenu,
    cuisineInspiration = menu.cuisineInspiration,
    semanticHash = menu.semanticHash,
    source = source,
    startsNewRotationCycle = startsNewCycle
)

internal const val AI_MENU_HIVE_SCOPE_QUERY_LIMIT = 36L
internal const val AI_MENU_HIVE_LEGACY_QUERY_LIMIT = 12L
```

Mantener `selectCandidate` y su llamada a `profile.accepts` como barrera
obligatoria para ambas consultas.

- [ ] **Step 8: Ejecutar tests de identidad y repositorio para observar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.domain.AiMenuHiveIdentityTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest \
  --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Confirmar identidad y consulta**

```bash
git add \
  app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt \
  app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt \
  app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt \
  app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt
git commit -m "feat: query hive by safe audience scope"
```

### Task 2: Persistencia v3 separada por ámbito y reglas

**Files:**
- Modify: `app/src/test/java/com/menudado/backend/AiMenuHiveDataSourceTest.kt`
- Modify: `app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt`
- Modify: `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`
- Modify: `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`
- Modify: `qa/firestore-rules.test.mjs`
- Modify: `firestore.rules`

- [ ] **Step 1: Escribir tests fallidos del mapper y contribución**

En `AiMenuHiveDataSourceTest`:

```kotlin
@Test
fun `new shared document contains immutable anonymous scope and identity v3`() {
    val menu = sampleSharedAiMenu().copy(scopeKey = "c".repeat(64))

    val document = AiMenuHiveFirestoreMapper.toDocument(menu)

    assertEquals(3, document["identityVersion"])
    assertEquals("c".repeat(64), document["scopeKey"])
    assertFalse(document.containsKey("mealType"))
    assertFalse(document.containsKey("audience"))
    assertFalse(document.containsKey("profile"))
}

@Test
fun `legacy v2 document without scope remains readable`() {
    val menu = sampleSharedAiMenu().copy(scopeKey = "c".repeat(64))
    val document = AiMenuHiveFirestoreMapper.toDocument(menu) +
        ("identityVersion" to 2) - "scopeKey"

    val parsed = AiMenuHiveFirestoreMapper.fromDocument(
        documentId = menu.semanticHash,
        document = document
    )

    assertNotNull(parsed)
    assertNull(parsed?.scopeKey)
}
```

Actualizar también `sampleSharedAiMenu()` para que sus documentos nuevos sean
válidos:

```kotlin
scopeKey = AiMenuHiveIdentity.scopeKey(
    AppLanguage.SPANISH,
    MealType.LUNCH,
    MenuAudience.ADULT
)
```

En `AiMenuHiveRepositoryTest`:

```kotlin
@Test
fun `safe contribution stores v3 identity broad scope and legacy eligibility`() =
    runTest {
        val dataSource = RecordingAiMenuHiveDataSource()
        val repository = AiMenuHiveRepository(
            dataSource,
            AiMenuHiveFeatureToggle(true)
        ) { 0 }

        repository.contribute(contribution()).getOrThrow()

        val stored = dataSource.upserts.single()
        val expected = requireNotNull(
            AiMenuHiveIdentity.scoped(
                AppLanguage.SPANISH,
                MealType.LUNCH,
                MenuAudience.ADULT,
                "pasta|tomato|sauce"
            )
        )
        assertEquals(expected.semanticHash, stored.semanticHash)
        assertEquals(
            AiMenuHiveIdentity.scopeKey(
                AppLanguage.SPANISH,
                MealType.LUNCH,
                MenuAudience.ADULT
            ),
            stored.scopeKey
        )
        assertEquals(1, stored.eligibilityKeys.size)
    }
```

- [ ] **Step 2: Ejecutar tests dirigidos para observar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.backend.AiMenuHiveDataSourceTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest \
  --console=plain
```

Expected: `FAILED`; el mapper todavía escribe identidad v2 y no existe
`scopeKey`.

- [ ] **Step 3: Implementar query, mapper y transacción**

En `FirebaseAiMenuHiveDataSource.fetch` construir la consulta según su tipo:

```kotlin
return runCatching {
    val collection = firestore.collection(COLLECTION)
    val query = when (request.queryField) {
        AiMenuHiveQueryField.SCOPE ->
            collection.whereEqualTo(FIELD_SCOPE_KEY, request.key)
        AiMenuHiveQueryField.ELIGIBILITY ->
            collection.whereArrayContains(FIELD_ELIGIBILITY_KEYS, request.key)
    }
    query.limit(request.limit)
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
```

La actualización de un documento v3 conservará su ámbito y solo ampliará la
compatibilidad heredada:

```kotlin
transaction.update(
    reference,
    mapOf(
        FIELD_IDENTITY_VERSION to 3,
        FIELD_ELIGIBILITY_KEYS to
            FieldValue.arrayUnion(menu.eligibilityKeys.single()),
        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
    )
)
```

El mapper nuevo escribirá:

```kotlin
"identityVersion" to 3,
"scopeKey" to requireNotNull(menu.scopeKey),
```

Al leer:

```kotlin
val identityVersion = (document["identityVersion"] as? Number)?.toInt()
val scopeKey = (document["scopeKey"] as? String)
    ?.takeIf { it.matches(SEMANTIC_HASH_REGEX) }
when (identityVersion) {
    null, 2 -> Unit
    3 -> if (scopeKey == null) return null
    else -> return null
}
```

Pasar `scopeKey` al `SharedAiMenu` resultante y declarar:

```kotlin
const val FIELD_SCOPE_KEY = "scopeKey"
```

En `AiMenuHiveRepository.contribute`, obtener la identidad remota y el ámbito
antes de construir el documento:

```kotlin
val identity = AiMenuHiveIdentity.scoped(
    contribution.language,
    contribution.mealType,
    contribution.audience,
    contribution.generatedMenu.deduplicationKey
) ?: return Result.success(
    AiMenuHiveContributionOutcome.SKIPPED_INVALID_IDENTITY
)
val scopeKey = AiMenuHiveIdentity.scopeKey(
    contribution.language,
    contribution.mealType,
    contribution.audience
)
```

Y usar:

```kotlin
scopeKey = scopeKey
```

La identidad v3 evita que el documento adulto y el documento bebé colisionen.
`canonicalized()` puede seguir usando la identidad culinaria v2 para deduplicar
y rotar dentro del conjunto ya limitado al mismo ámbito.

- [ ] **Step 4: Ejecutar tests Kotlin para observar GREEN**

Run el comando de Step 2. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Escribir primero las pruebas fallidas de reglas**

En `qa/firestore-rules.test.mjs` añadir:

```javascript
const SCOPE_HASH = "c".repeat(64);
```

Cambiar `validDocument()` a `identityVersion: 3`, añadir
`scopeKey: SCOPE_HASH` y cubrir:

```javascript
test("new hive document requires identity v3 and one valid scope hash", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  const { scopeKey, ...withoutScope } = validDocument();

  await assertFails(
    setDoc(doc(db, "sharedAiMenus", HASH), withoutScope)
  );
  await assertFails(
    setDoc(
      doc(db, "sharedAiMenus", HASH),
      { ...validDocument(), identityVersion: 2 }
    )
  );
  await assertFails(
    setDoc(
      doc(db, "sharedAiMenus", HASH),
      { ...validDocument(), scopeKey: "invalid" }
    )
  );
});

test("scope is immutable while eligibility can grow", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  await assertSucceeds(
    setDoc(doc(db, "sharedAiMenus", HASH), validDocument())
  );
  await assertSucceeds(
    updateDoc(doc(db, "sharedAiMenus", HASH), {
      eligibilityKeys: [ELIGIBILITY_HASH, "d".repeat(64)],
      updatedAt: Timestamp.now()
    })
  );
  await assertFails(
    updateDoc(doc(db, "sharedAiMenus", HASH), {
      scopeKey: "e".repeat(64),
      updatedAt: Timestamp.now()
    })
  );
});

test("authenticated user can still read legacy v2 document without scope", async () => {
  const { scopeKey, ...legacyDocument } = {
    ...validDocument(),
    identityVersion: 2
  };
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(
      doc(context.firestore(), "sharedAiMenus", HASH),
      legacyDocument
    );
  });
  const db = testEnv.authenticatedContext("user-a").firestore();

  await assertSucceeds(getDoc(doc(db, "sharedAiMenus", HASH)));
});
```

Adaptar el test de actualización de identidad heredada para que su fixture
elimine tanto `identityVersion` como `scopeKey`; debe seguir permitiendo
exclusivamente la actualización heredada a v2.

- [ ] **Step 6: Ejecutar reglas para observar RED**

Run:

```bash
firebase emulators:exec --only firestore \
  "npm run test:firestore-rules --prefix qa"
```

Expected: `FAILED`; `scopeKey` todavía no está permitido y las creaciones siguen
exigiendo identidad v2.

- [ ] **Step 7: Implementar las reglas mínimas**

Añadir `scopeKey` a `hasOnly` y cambiar la creación:

```text
&& request.resource.data.identityVersion == 3
&& request.resource.data.scopeKey is string
&& isHash(request.resource.data.scopeKey)
```

Mantener el campo fuera de `affectedKeys().hasOnly(...)` para que cualquier
cambio o eliminación sea rechazazado. Añadir:

```text
function isStoredV3HiveDocument() {
  return resource.data.keys().hasAll(['identityVersion', 'scopeKey'])
    && resource.data.identityVersion == 3;
}

function hasValidUpdatedIdentityVersion() {
  return isStoredV3HiveDocument()
    ? request.resource.data.identityVersion == 3
    : request.resource.data.identityVersion == 2;
}
```

Sustituir la comprobación fija de versión en `allow update` por:

```text
&& hasValidUpdatedIdentityVersion()
```

Conservar intactas las restricciones de `eligibilityKeys`, contenido de receta
y borrado. Las nuevas identidades v3 solo pueden actualizar el documento de su
propio ámbito porque el ID ya incluye `scopeKey`.

- [ ] **Step 8: Ejecutar reglas y tests Kotlin para observar GREEN**

Run:

```bash
firebase emulators:exec --only firestore \
  "npm run test:firestore-rules --prefix qa"
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.backend.AiMenuHiveDataSourceTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest \
  --console=plain
```

Expected: ambos comandos terminan correctamente.

- [ ] **Step 9: Confirmar persistencia y reglas**

```bash
git add \
  app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt \
  app/src/test/java/com/menudado/backend/AiMenuHiveDataSourceTest.kt \
  app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt \
  app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt \
  firestore.rules \
  qa/firestore-rules.test.mjs
git commit -m "feat: isolate hive recipes by audience scope"
```

### Task 3: Resultado observable de contribución

**Files:**
- Modify: `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`
- Modify: `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`
- Modify: `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`

- [ ] **Step 1: Escribir tests fallidos del resultado tipado**

En `AiMenuHiveRepositoryTest`:

```kotlin
@Test
fun `contribution distinguishes saved disabled incompatible and invalid identity`() = runTest {
    val enabledDataSource = RecordingAiMenuHiveDataSource()
    val enabled = AiMenuHiveRepository(
        enabledDataSource,
        AiMenuHiveFeatureToggle(true)
    ) { 0 }
    val disabled = AiMenuHiveRepository(
        RecordingAiMenuHiveDataSource(),
        AiMenuHiveFeatureToggle(false)
    ) { 0 }
    val incompatible = contribution().copy(
        profile = DietaryProfile(isVegan = true),
        generatedMenu = contribution().generatedMenu.copy(
            name = "Pasta con queso",
            description = "Pasta con queso y nata."
        )
    )
    val invalidIdentity = contribution().copy(
        generatedMenu = contribution().generatedMenu.copy(
            deduplicationKey = "invalid"
        )
    )

    assertEquals(
        AiMenuHiveContributionOutcome.SAVED,
        enabled.contribute(contribution()).getOrThrow()
    )
    assertEquals(
        AiMenuHiveContributionOutcome.SKIPPED_DISABLED,
        disabled.contribute(contribution()).getOrThrow()
    )
    assertEquals(
        AiMenuHiveContributionOutcome.SKIPPED_INCOMPATIBLE,
        enabled.contribute(incompatible).getOrThrow()
    )
    assertEquals(
        AiMenuHiveContributionOutcome.SKIPPED_INVALID_IDENTITY,
        enabled.contribute(invalidIdentity).getOrThrow()
    )
}

@Test
fun `remote contribution failure remains a failure result`() = runTest {
    val repository = AiMenuHiveRepository(
        RecordingAiMenuHiveDataSource(
            upsertResult = Result.failure(IllegalStateException("denied"))
        ),
        AiMenuHiveFeatureToggle(true)
    ) { 0 }

    val result = repository.contribute(contribution())

    assertTrue(result.isFailure)
}
```

Ampliar el fake con:

```kotlin
private val upsertResult: Result<Unit> = Result.success(Unit)
```

y devolver ese valor desde `upsert`.

- [ ] **Step 2: Ejecutar el test para observar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest \
  --console=plain
```

Expected: `FAILED`; `contribute` todavía devuelve únicamente `Result<Unit>`.

- [ ] **Step 3: Implementar el resultado tipado**

En `AiMenuHiveRepository.kt`:

```kotlin
enum class AiMenuHiveContributionOutcome(val analyticsValue: String) {
    SAVED("saved"),
    SKIPPED_DISABLED("skipped_disabled"),
    SKIPPED_INCOMPATIBLE("skipped_incompatible"),
    SKIPPED_INVALID_IDENTITY("skipped_invalid_identity")
}
```

Actualizar el contrato:

```kotlin
suspend fun contribute(
    contribution: AiMenuHiveContribution
): Result<AiMenuHiveContributionOutcome>
```

`NoOpAiMenuHiveGateway` devolverá:

```kotlin
Result.success(AiMenuHiveContributionOutcome.SKIPPED_DISABLED)
```

Y el repositorio, conservando la construcción actual del documento y añadiendo
el ámbito:

```kotlin
override suspend fun contribute(
    contribution: AiMenuHiveContribution
): Result<AiMenuHiveContributionOutcome> {
    if (!featureToggle.isEnabled) {
        return Result.success(AiMenuHiveContributionOutcome.SKIPPED_DISABLED)
    }
    if (
        !contribution.profile.accepts(
            contribution.generatedMenu,
            contribution.audience
        )
    ) {
        return Result.success(
            AiMenuHiveContributionOutcome.SKIPPED_INCOMPATIBLE
        )
    }
    val identity = AiMenuHiveIdentity.scoped(
        contribution.language,
        contribution.mealType,
        contribution.audience,
        contribution.generatedMenu.deduplicationKey
    ) ?: return Result.success(
        AiMenuHiveContributionOutcome.SKIPPED_INVALID_IDENTITY
    )
    val sharedMenu = SharedAiMenu(
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
        ),
        scopeKey = AiMenuHiveIdentity.scopeKey(
            contribution.language,
            contribution.mealType,
            contribution.audience
        )
    )
    return dataSource.upsert(sharedMenu).map {
        AiMenuHiveContributionOutcome.SAVED
    }
}
```

- [ ] **Step 4: Ejecutar el test del repositorio para observar GREEN**

Run el comando de Step 2. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Escribir tests fallidos de Analytics y ViewModel**

En el fake `RecordingAiMenuHiveGateway` de `MenuDadoViewModelTest` añadir:

```kotlin
var contributionResult: Result<AiMenuHiveContributionOutcome> =
    Result.success(AiMenuHiveContributionOutcome.SAVED)
```

y devolverlo desde `contribute`.

Añadir:

```kotlin
@Test
fun `successful live contribution records one anonymous terminal event`() =
    runTest(dispatcher) {
        analyzer.generatedMenu = sampleGeneratedMenu(
            deduplicationKey = "pasta|tomato|sauce"
        )

        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.saveGeneratedMenuIdea()
        advanceUntilIdle()

        assertEquals(1, dao.saved.size)
        assertEquals(
            listOf("ai_menu_hive_contribution:saved"),
            analytics.events.filter {
                it.startsWith("ai_menu_hive_contribution:")
            }
        )
    }

@Test
fun `failed live contribution keeps private save and records error`() =
    runTest(dispatcher) {
        analyzer.generatedMenu = sampleGeneratedMenu(
            deduplicationKey = "pasta|tomato|sauce"
        )
        hive.contributionResult =
            Result.failure(IllegalStateException("permission denied"))

        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.saveGeneratedMenuIdea()
        advanceUntilIdle()

        assertEquals(1, dao.saved.size)
        assertEquals(
            listOf("ai_menu_hive_contribution:error"),
            analytics.events.filter {
                it.startsWith("ai_menu_hive_contribution:")
            }
        )
        assertFalse(analytics.events.any { it.contains("permission denied") })
    }
```

En el fake de Analytics implementar:

```kotlin
override fun trackAiMenuHiveContribution(result: String) {
    events += "ai_menu_hive_contribution:$result"
}
```

- [ ] **Step 6: Ejecutar ViewModel tests para observar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuDadoViewModelTest \
  --console=plain
```

Expected: `FAILED`; el contrato Analytics y el consumo del resultado aún no
existen.

- [ ] **Step 7: Implementar Analytics y consumir el resultado**

En `MenuDadoAnalytics`:

```kotlin
fun trackAiMenuHiveContribution(result: String)
```

En `NoOpMenuDadoAnalytics`:

```kotlin
override fun trackAiMenuHiveContribution(result: String) = Unit
```

En `FirebaseMenuDadoAnalytics`:

```kotlin
override fun trackAiMenuHiveContribution(result: String) {
    logEvent(EVENT_AI_MENU_HIVE_CONTRIBUTION) {
        putString(PARAM_STATUS, result.sanitized())
    }
}
```

Y el nombre:

```kotlin
const val EVENT_AI_MENU_HIVE_CONTRIBUTION = "ai_menu_hive_contribution"
```

En el bloque de contribución de `MenuDadoViewModel`:

```kotlin
hiveContribution?.let { contribution ->
    viewModelScope.launch {
        val result = aiMenuHive.contribute(contribution)
        runCatching {
            analytics.trackAiMenuHiveContribution(
                result = result.fold(
                    onSuccess = { it.analyticsValue },
                    onFailure = { HIVE_CONTRIBUTION_ERROR }
                )
            )
        }
    }
}
```

Añadir:

```kotlin
private const val HIVE_CONTRIBUTION_ERROR = "error"
```

No mostrar `exception.message`, no cambiar el Snackbar y no esperar la
contribución antes de confirmar el guardado privado.

- [ ] **Step 8: Ejecutar repositorio y ViewModel tests para observar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest \
  --tests com.menudado.ui.MenuDadoViewModelTest \
  --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Confirmar observabilidad**

```bash
git add \
  app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt \
  app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt \
  app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "feat: track AI hive contribution outcomes"
```

### Task 4: Contexto, regresión y verificación final

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Actualizar el contrato funcional**

Sustituir en la sección del respaldo la afirmación de clave exacta por:

```markdown
Solo cuando la petición real termina con error, MenuDado consulta propuestas de
`sharedAiMenus` del mismo idioma, tipo de comida y público. Recupera un conjunto
acotado, vuelve a validar localmente edad, embarazo, veganismo, alergias,
alimentos a evitar y seguridad, y solo entonces muestra una alternativa. Los
documentos heredados sin ámbito conservan una consulta por perfil exacto como
respaldo. Nunca mezcla adulto, peques y bebé ni relaja el perfil para conseguir
un resultado.
```

Actualizar el párrafo de colmena:

```markdown
Las recetas nuevas se indexan mediante una `scopeKey` anónima de idioma, comida
y público, y su identidad remota v3 incluye ese ámbito. Una receta adulta y una
receta para bebé nunca comparten documento aunque tengan la misma identidad
culinaria. `eligibilityKeys` se conserva durante la transición y la
compatibilidad se decide siempre con el perfil vigente en el dispositivo.
```

Añadir a Analytics:

```markdown
Cada contribución iniciada registra un único resultado terminal
`ai_menu_hive_contribution`: `saved`, `skipped_disabled`,
`skipped_incompatible`, `skipped_invalid_identity` o `error`, sin perfil,
receta, UID ni texto libre.
```

- [ ] **Step 2: Ejecutar la suite unitaria completa**

Run:

```bash
./gradlew :app:testDebugUnitTest --console=plain
```

Expected: `BUILD SUCCESSFUL` y cero tests fallidos.

- [ ] **Step 3: Ejecutar las reglas completas**

Run:

```bash
firebase emulators:exec --only firestore \
  "npm run test:firestore-rules --prefix qa"
```

Expected: todos los tests Node terminan con `pass` y el emulador se detiene
correctamente.

- [ ] **Step 4: Compilar la variante debug**

Run:

```bash
./gradlew :app:assembleDebug --console=plain
```

Expected: `BUILD SUCCESSFUL` y APK actualizado bajo
`app/build/outputs/apk/debug/`.

- [ ] **Step 5: Revisar alcance y calidad**

Run:

```bash
git diff --check
git status --short
git diff --stat HEAD~3..HEAD
```

Expected:

- `git diff --check` sin salida;
- solo los archivos previstos aparecen modificados;
- no existen cambios de UI, prompt, cuota, Room ni datos privados.

- [ ] **Step 6: Confirmar documentación y validación**

```bash
git add docs/project-context.md
git commit -m "docs: describe broad hive profile compatibility"
```

- [ ] **Step 7: Prueba manual guiada en debug**

Con App Check válido y `ai_menu_hive_enabled=true`:

1. configurar `Adulto` vegano;
2. generar y guardar sin editar una receta vegana;
3. confirmar el guardado privado inmediato;
4. verificar en `menudado-debug/sharedAiMenus` que el documento contiene una
   `scopeKey`, una `eligibilityKey` y ningún UID o perfil legible;
5. cambiar `Adulto` a un perfil sin restricciones;
6. provocar un fallo de IA y confirmar que la receta puede recuperarse;
7. configurar un perfil adulto incompatible y confirmar que la receta se
   descarta;
8. activar `Bebé`, provocar el respaldo y confirmar que la receta adulta no se
   consulta ni se muestra;
9. verificar un único evento `ai_menu_hive_contribution` por cada intento.

Expected: reutilización entre perfiles adultos compatibles, separación absoluta
de bebé y ausencia de datos sensibles. Si App Check falla, registrar el riesgo
como configuración externa; no interpretar ese entorno como validación
funcional de Firestore.
