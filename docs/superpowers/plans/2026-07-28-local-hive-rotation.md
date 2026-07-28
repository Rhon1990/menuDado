# Local Hive Rotation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persistir en el teléfono una rotación pequeña por identidad para que la colmena recorra sus candidatos compatibles antes de repetir.

**Architecture:** Un `HiveRotationStore` local conserva como máximo 24 `semanticHash` ordenados y el último mostrado para cada scope `guest` o `account:<uid>`. El repositorio informa cuándo una selección inicia un nuevo ciclo y el ViewModel lee/registra el snapshot alrededor del fallback, sin añadir operaciones remotas.

**Tech Stack:** Kotlin, Android SharedPreferences, MVVM, coroutines, JUnit 4, Firebase Firestore existente.

---

## File Map

- Create `app/src/main/java/com/menudado/data/HiveRotationStore.kt`: contrato, snapshot, implementación local y límite de almacenamiento.
- Create `app/src/test/java/com/menudado/data/HiveRotationStoreTest.kt`: persistencia, scopes, reinicio y límite.
- Modify `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`: selección sin repetición y señal de nuevo ciclo.
- Modify `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`: orden de rotación, reinicio y candidato único.
- Modify `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`: usar el snapshot de la identidad activa y registrar el candidato mostrado.
- Modify `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`: verificar scopes y registro del fallback.
- Modify `app/src/main/java/com/menudado/MenuDadoApplication.kt`: crear el store de producción.
- Modify `app/src/main/java/com/menudado/MainActivity.kt`: inyectar store y scope inicial.
- Modify `app/src/main/res/xml/backup_rules.xml`: excluir el historial local.
- Modify `app/src/main/res/xml/data_extraction_rules.xml`: excluirlo de backup y transferencia.
- Modify `app/src/test/java/com/menudado/data/AiUsageBackupRulesTest.kt`: cubrir la nueva preferencia local.
- Modify `docs/project-context.md`: documentar la rotación acotada.

### Task 1: Store local acotado por identidad

**Files:**
- Create: `app/src/test/java/com/menudado/data/HiveRotationStoreTest.kt`
- Create: `app/src/main/java/com/menudado/data/HiveRotationStore.kt`

- [ ] **Step 1: Write the failing store tests**

```kotlin
class HiveRotationStoreTest {
    @Test
    fun `guest and accounts keep independent rotation histories`() {
        val context = FakeHiveRotationContext()
        val store = SharedPreferencesHiveRotationStore(context)

        store.recordShown("guest", "guest-hash", startsNewCycle = false)
        store.recordShown("account:a", "account-hash", startsNewCycle = false)

        assertEquals(listOf("guest-hash"), store.snapshot("guest").seenHashes)
        assertEquals(listOf("account-hash"), store.snapshot("account:a").seenHashes)
    }

    @Test
    fun `new cycle clears prior hashes and keeps selected candidate`() {
        val store = SharedPreferencesHiveRotationStore(FakeHiveRotationContext())
        store.recordShown("guest", "first", startsNewCycle = false)
        store.recordShown("guest", "second", startsNewCycle = true)

        assertEquals(listOf("second"), store.snapshot("guest").seenHashes)
        assertEquals("second", store.snapshot("guest").lastShownHash)
    }

    @Test
    fun `history keeps only latest twenty four hashes`() {
        val store = SharedPreferencesHiveRotationStore(FakeHiveRotationContext())
        repeat(30) { store.recordShown("guest", "hash-$it", startsNewCycle = false) }

        assertEquals((6 until 30).map { "hash-$it" }, store.snapshot("guest").seenHashes)
    }
}

private class FakeHiveRotationContext : ContextWrapper(null) {
    private val preferences = createHiveRotationPreferences()

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences
}

private fun createHiveRotationPreferences(): SharedPreferences {
    val values = mutableMapOf<String, String?>()
    val editor = Proxy.newProxyInstance(
        SharedPreferences.Editor::class.java.classLoader,
        arrayOf(SharedPreferences.Editor::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "putString" -> {
                values[args!![0] as String] = args[1] as String?
                proxy
            }
            "apply" -> Unit
            else -> error("Unexpected editor call: ${method.name}")
        }
    } as SharedPreferences.Editor
    return Proxy.newProxyInstance(
        SharedPreferences::class.java.classLoader,
        arrayOf(SharedPreferences::class.java)
    ) { _, method, args ->
        when (method.name) {
            "getString" -> values[args!![0] as String] ?: args[1]
            "edit" -> editor
            else -> error("Unexpected preferences call: ${method.name}")
        }
    } as SharedPreferences
}
```

- [ ] **Step 2: Run the store test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.data.HiveRotationStoreTest
```

Expected: compilation fails because `HiveRotationStore` does not exist.

- [ ] **Step 3: Implement the minimal store**

```kotlin
data class HiveRotationSnapshot(
    val seenHashes: List<String> = emptyList(),
    val lastShownHash: String? = null
)

interface HiveRotationStore {
    fun snapshot(scope: String): HiveRotationSnapshot
    fun recordShown(scope: String, semanticHash: String, startsNewCycle: Boolean)
}

object NoOpHiveRotationStore : HiveRotationStore {
    override fun snapshot(scope: String) = HiveRotationSnapshot()
    override fun recordShown(scope: String, semanticHash: String, startsNewCycle: Boolean) = Unit
}

class SharedPreferencesHiveRotationStore(context: Context) : HiveRotationStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun snapshot(scope: String): HiveRotationSnapshot {
        val hashes = preferences.getString(hashesKey(scope), null)
            ?.split('|')
            ?.filter(String::isNotBlank)
            .orEmpty()
        return HiveRotationSnapshot(hashes, preferences.getString(lastKey(scope), null))
    }

    override fun recordShown(scope: String, semanticHash: String, startsNewCycle: Boolean) {
        val current = if (startsNewCycle) emptyList() else snapshot(scope).seenHashes
        val hashes = (current - semanticHash + semanticHash).takeLast(MAX_HIVE_ROTATION_HASHES)
        preferences.edit()
            .putString(hashesKey(scope), hashes.joinToString("|"))
            .putString(lastKey(scope), semanticHash)
            .apply()
    }
}

internal const val MAX_HIVE_ROTATION_HASHES = 24
```

Use private keys based on the scope and `PREFERENCES_NAME = "menu-dado-hive-rotation"`. Reuse the fake SharedPreferences context pattern from `RewardedAiCreditStoreTest`.

- [ ] **Step 4: Run the store tests and verify GREEN**

Run the focused command from Step 2.

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/data/HiveRotationStore.kt app/src/test/java/com/menudado/data/HiveRotationStoreTest.kt
git commit -m "feat: persist bounded hive rotation"
```

### Task 2: Selección por ciclo en el repositorio

**Files:**
- Modify: `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`
- Modify: `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`

- [ ] **Step 1: Write failing repository tests**

Add `lastShownHash` to `AiMenuHiveSearchRequest` and `startsNewRotationCycle` to `AiMenuHiveCandidate`, then cover:

```kotlin
@Test
fun `all unseen candidates are selected before rotation restarts`() = runTest {
    val first = sharedMenu("rice|vegetables|bowl")
    val second = sharedMenu("lentils|vegetables|stew")
    val dataSource = RecordingAiMenuHiveDataSource(server = Result.success(listOf(first, second)))
    val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

    val unseen = repository.findCompatibleMenu(
        request(recentSemanticHashes = setOf(first.semanticHash), lastShownHash = first.semanticHash)
    ).getOrThrow()

    assertEquals(second.semanticHash, unseen?.semanticHash)
    assertFalse(requireNotNull(unseen).startsNewRotationCycle)
}

@Test
fun `completed rotation starts new cycle without immediate repeat`() = runTest {
    val first = sharedMenu("rice|vegetables|bowl")
    val second = sharedMenu("lentils|vegetables|stew")
    val dataSource = RecordingAiMenuHiveDataSource(server = Result.success(listOf(first, second)))
    val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

    val restarted = repository.findCompatibleMenu(
        request(
            recentSemanticHashes = setOf(first.semanticHash, second.semanticHash),
            lastShownHash = first.semanticHash
        )
    ).getOrThrow()

    assertEquals(second.semanticHash, restarted?.semanticHash)
    assertTrue(requireNotNull(restarted).startsNewRotationCycle)
}

@Test
fun `single compatible candidate can repeat after its cycle`() = runTest {
    val only = sharedMenu("rice|vegetables|bowl")
    val dataSource = RecordingAiMenuHiveDataSource(server = Result.success(listOf(only)))
    val repository = AiMenuHiveRepository(dataSource, AiMenuHiveFeatureToggle(true)) { 0 }

    val restarted = repository.findCompatibleMenu(
        request(recentSemanticHashes = setOf(only.semanticHash), lastShownHash = only.semanticHash)
    ).getOrThrow()

    assertEquals(only.semanticHash, restarted?.semanticHash)
    assertTrue(requireNotNull(restarted).startsNewRotationCycle)
}
```

- [ ] **Step 2: Run repository tests and verify RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.data.AiMenuHiveRepositoryTest
```

Expected: compilation failures for the new request/candidate properties.

- [ ] **Step 3: Implement cycle-aware selection**

Introduce an internal result:

```kotlin
private data class HiveSelection(
    val menu: SharedAiMenu,
    val startsNewCycle: Boolean
)
```

Selection logic:

```kotlin
val unseen = safe.filterNot { it.semanticHash in request.recentSemanticHashes }
val startsNewCycle = unseen.isEmpty()
val cycleCandidates = if (startsNewCycle && safe.size > 1) {
    safe.filterNot { it.semanticHash == request.lastShownHash }.ifEmpty { safe }
} else {
    unseen.ifEmpty { safe }
}
val preferred = cycleCandidates.filter { candidate ->
    candidate.generatedMenu.searchableText()
        .containsAnyRequestedIngredient(request.baseIngredients)
}.ifEmpty { cycleCandidates }
return HiveSelection(
    menu = preferred[pickIndexProvider(preferred.size).coerceIn(preferred.indices)],
    startsNewCycle = startsNewCycle
)
```

Map `HiveSelection.startsNewCycle` into `AiMenuHiveCandidate.startsNewRotationCycle`.

- [ ] **Step 4: Run repository tests and verify GREEN**

Run the command from Step 2.

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt
git commit -m "feat: rotate hive candidates before repeating"
```

### Task 3: Integración por identidad en ViewModel y aplicación

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Modify: `app/src/main/java/com/menudado/MainActivity.kt`

- [ ] **Step 1: Write failing ViewModel tests**

Extend the recording gateway to queue candidates and add a fake store. Cover:

```kotlin
@Test
fun `hive fallback reads and records active account rotation`() = runTest(dispatcher) {
    scopedAiUsageStore.seed(PROVIDER_AI_USAGE_SCOPE, "2026-06-10", 20)
    hive.searchResult = Result.success(sampleHiveCandidate())
    viewModel.updateGuestAccess(false, true, true, userId = "user-a")

    viewModel.generateMenuIdea()
    advanceUntilIdle()

    assertEquals(accountAiUsageScope("user-a"), hiveRotationStore.snapshotScopes.single())
    assertEquals(
        Triple(accountAiUsageScope("user-a"), sampleHiveCandidate().semanticHash, false),
        hiveRotationStore.records.single()
    )
}

@Test
fun `guest and account pass independent hive histories`() = runTest(dispatcher) {
    scopedAiUsageStore.seed(PROVIDER_AI_USAGE_SCOPE, "2026-06-10", 20)
    hive.searchResult = Result.success(sampleHiveCandidate())
    hiveRotationStore.seed(GUEST_AI_USAGE_SCOPE, listOf("guest-hash"), "guest-hash")
    hiveRotationStore.seed(accountAiUsageScope("user-a"), listOf("account-hash"), "account-hash")

    viewModel.updateGuestAccess(true, true, true, userId = "anonymous")
    viewModel.generateMenuIdea()
    advanceUntilIdle()
    viewModel.discardGeneratedMenuIdea()

    viewModel.updateGuestAccess(false, true, true, userId = "user-a")
    viewModel.generateMenuIdea()
    advanceUntilIdle()

    assertEquals(setOf("guest-hash"), hive.searches[0].recentSemanticHashes)
    assertEquals(setOf("account-hash"), hive.searches[1].recentSemanticHashes)
}

private class FakeHiveRotationStore : HiveRotationStore {
    private val snapshots = mutableMapOf<String, HiveRotationSnapshot>()
    val snapshotScopes = mutableListOf<String>()
    val records = mutableListOf<Triple<String, String, Boolean>>()

    fun seed(scope: String, hashes: List<String>, lastShownHash: String?) {
        snapshots[scope] = HiveRotationSnapshot(hashes, lastShownHash)
    }

    override fun snapshot(scope: String): HiveRotationSnapshot {
        snapshotScopes += scope
        return snapshots[scope] ?: HiveRotationSnapshot()
    }

    override fun recordShown(scope: String, semanticHash: String, startsNewCycle: Boolean) {
        records += Triple(scope, semanticHash, startsNewCycle)
    }
}
```

- [ ] **Step 2: Run ViewModel tests and verify RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest
```

Expected: compilation failures for the missing `HiveRotationStore` dependency and candidate fields.

- [ ] **Step 3: Wire the store**

Add to `MenuDadoViewModel`:

```kotlin
private val hiveRotationStore: HiveRotationStore = NoOpHiveRotationStore
```

Before `findCompatibleMenu`:

```kotlin
val rotation = hiveRotationStore.snapshot(activeAiUsageScope)
AiMenuHiveSearchRequest(
    // existing fields
    recentSemanticHashes = rotation.seenHashes.toSet(),
    lastShownHash = rotation.lastShownHash
)
```

After a candidate is accepted:

```kotlin
hiveRotationStore.recordShown(
    scope = activeAiUsageScope,
    semanticHash = candidate.semanticHash,
    startsNewCycle = candidate.startsNewRotationCycle
)
```

Remove `recentHiveSemanticHashes`, `rememberHiveSemanticHash` and `MAX_RECENT_HIVE_HASHES`.

Expose in `MenuDadoApplication`:

```kotlin
val hiveRotationStore by lazy { SharedPreferencesHiveRotationStore(this) }
```

Pass it from `MainActivity` to the ViewModel. The existing `initialAiUsageScope` and `updateGuestAccess` continue controlling `activeAiUsageScope`.

- [ ] **Step 4: Run ViewModel and store/repository tests**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.ui.MenuDadoViewModelTest \
  --tests com.menudado.data.HiveRotationStoreTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt app/src/main/java/com/menudado/MenuDadoApplication.kt app/src/main/java/com/menudado/MainActivity.kt
git commit -m "feat: scope hive rotation by identity"
```

### Task 4: Backup, contexto y validación final

**Files:**
- Modify: `app/src/main/res/xml/backup_rules.xml`
- Modify: `app/src/main/res/xml/data_extraction_rules.xml`
- Modify: `app/src/test/java/com/menudado/data/AiUsageBackupRulesTest.kt`
- Modify: `docs/project-context.md`

- [ ] **Step 1: Add a failing backup assertion**

Add `"menu-dado-hive-rotation.xml"` to the expected local-only preferences in `AiUsageBackupRulesTest`.

- [ ] **Step 2: Run the backup test and verify RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.data.AiUsageBackupRulesTest
```

Expected: assertion failure because the preference is not excluded.

- [ ] **Step 3: Exclude the preference and update project context**

Add this entry to legacy backup, cloud backup and device transfer:

```xml
<exclude domain="sharedpref" path="menu-dado-hive-rotation.xml" />
```

Document in `docs/project-context.md`:

- local history contains at most 24 hashes per identity;
- it is not synchronized or backed up;
- the current 12-result Firestore query is unchanged;
- a new cycle avoids an immediate repeat when alternatives exist.

- [ ] **Step 4: Run focused and full verification**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.data.HiveRotationStoreTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest \
  --tests com.menudado.data.AiUsageBackupRulesTest \
  --tests com.menudado.ui.MenuDadoViewModelTest
```

Then:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:compileReleaseDebuggableKotlin
git diff --check
git status --short
```

Expected: all Gradle tasks report `BUILD SUCCESSFUL`, diff check is empty, and status contains only intended files before commit.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/xml/backup_rules.xml app/src/main/res/xml/data_extraction_rules.xml app/src/test/java/com/menudado/data/AiUsageBackupRulesTest.kt docs/project-context.md
git commit -m "docs: document bounded hive rotation"
```
