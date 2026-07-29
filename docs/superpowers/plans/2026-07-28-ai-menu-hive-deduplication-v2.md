# AI Menu Hive Deduplication v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Canonicalize equivalent AI menu identities before hashing, prevent old duplicates from rotating as different recipes, and provide a safe Firebase consolidation tool without increasing AI calls or prompt size.

**Architecture:** Keep SHA-256 document IDs and the existing transaction, but feed them a position-aware canonical identity. Canonicalize again on reads for backward compatibility, mark new documents with `identityVersion: 2`, and use a dry-run-first authenticated Firestore tool to merge old documents.

**Tech Stack:** Kotlin/JUnit, Android Gradle, Firebase Firestore rules emulator, Node.js test runner, Google Cloud Firestore client.

---

### Task 1: Canonical identity v2

**Files:**
- Create: `app/src/test/resources/ai-menu-hive-identity-v2-fixtures.tsv`
- Modify: `app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt`
- Modify: `app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt`

- [ ] **Step 1: Write failing equivalence and negative tests**

Create one shared UTF-8 TSV fixture:

```text
SPANISH	salad|lentils+vegetables|mix	salad|lentils+vegetables|mixed
SPANISH	salad|lentils+vegetable|mixed	salad|lentils+vegetables|mixed
SPANISH	salad|lentils+vegetables|tossed	salad|lentils+vegetables|mixed
```

Load it from the Kotlin test classpath and assert each raw key produces the
expected canonical key. Also assert the three identities are equal:

```kotlin
val identities = listOf(
    "salad|lentils+vegetables|mix",
    "salad|lentils+vegetable|mixed",
    "salad|lentils+vegetables|tossed"
).map { requireNotNull(AiMenuHiveIdentity.from(AppLanguage.SPANISH, it)) }

assertEquals(1, identities.distinct().size)
assertEquals("salad|lentils+vegetables|mixed", identities.first().canonicalKey)
```

Also assert that `stew|lentils+vegetables|stewed` and
`salad|chickpeas+vegetables|mixed` remain different.

- [ ] **Step 2: Verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.domain.AiMenuHiveIdentityTest
```

Expected: FAIL because `vegetable`, `mix`, and `tossed` are not canonicalized.

- [ ] **Step 3: Implement position-aware aliases**

Replace the shared alias lookup with dedicated maps:

```kotlin
private val componentAliases = mapOf(
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
    "lentil" to "lentils",
    "vegetable" to "vegetables",
    "verdura" to "vegetables",
    "verduras" to "vegetables",
    "salsa" to "sauce",
    "salsa de tomate" to "sauce"
)

private val preparationAliases = mapOf(
    "mix" to "mixed",
    "mixing" to "mixed",
    "toss" to "mixed",
    "tossed" to "mixed",
    "mezcla" to "mixed",
    "mezclado" to "mixed",
    "mezclada" to "mixed"
)
```

Use `canonicalPreparation` only for the third key component. Retain explicit
aliases rather than generic stemming.

- [ ] **Step 4: Verify GREEN**

Run the directed test command from Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt app/src/test/resources/ai-menu-hive-identity-v2-fixtures.tsv
git commit -m "fix: canonicalize equivalent AI menu identities"
```

### Task 2: Defensive deduplication on reads

**Files:**
- Modify: `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`
- Modify: `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`

- [ ] **Step 1: Write a failing legacy-duplicate test**

Return two `SharedAiMenu` instances with different stored hashes and semantic
keys `salad|lentils+vegetable|mixed` and
`salad|lentils+vegetables|mix`. Capture the argument passed to
`pickIndexProvider` and assert:

```kotlin
assertEquals(1, selectableCount)
assertEquals(
    AiMenuHiveIdentity.from(
        AppLanguage.SPANISH,
        "salad|lentils+vegetables|mixed"
    )?.semanticHash,
    result?.semanticHash
)
```

- [ ] **Step 2: Verify RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.data.AiMenuHiveRepositoryTest
```

Expected: FAIL because both stored hashes currently remain selectable.

- [ ] **Step 3: Canonicalize fetched candidates**

Before dietary and rotation filtering, transform candidates with:

```kotlin
private fun SharedAiMenu.canonicalized(): SharedAiMenu? {
    val identity = AiMenuHiveIdentity.from(language, semanticKey) ?: return null
    return copy(
        semanticHash = identity.semanticHash,
        semanticKey = identity.canonicalKey,
        generatedMenu = generatedMenu.copy(deduplicationKey = identity.canonicalKey)
    )
}
```

Then build the safe list with:

```kotlin
val safe = candidates
    .mapNotNull(SharedAiMenu::canonicalized)
    .filter { request.profile.accepts(it.generatedMenu) }
    .distinctBy(SharedAiMenu::semanticHash)
```

Keep all existing rotation and ingredient-priority behavior after this list.

- [ ] **Step 4: Verify GREEN**

Run the directed repository tests. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt
git commit -m "fix: collapse legacy hive duplicates on read"
```

### Task 3: Version new Firestore identities

**Files:**
- Modify: `app/src/test/java/com/menudado/backend/AiMenuHiveDataSourceTest.kt`
- Modify: `app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt`
- Modify: `qa/firestore-rules.test.mjs`
- Modify: `firestore.rules`

- [ ] **Step 1: Write failing mapper and rules tests**

Assert mapper output contains:

```kotlin
assertEquals(2, document["identityVersion"])
```

Add an emulator test that deletes `identityVersion` from `validDocument()` and
expects create to fail. Update `validDocument()` to include
`identityVersion: 2`.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.backend.AiMenuHiveDataSourceTest
firebase emulators:exec --only firestore "cd qa && npm run test:firestore-rules"
```

Expected: mapper assertion fails and rules reject the new valid fixture because
the field is not yet allowed. Firebase CLI requires JDK 21 or newer.

- [ ] **Step 3: Write v2 identity metadata**

Add `"identityVersion" to 2` to `AiMenuHiveFirestoreMapper.toDocument`. Keep
`schemaVersion` at `1`; `fromDocument` continues accepting legacy documents
without the new field and rejects numeric values other than `2` when present.

- [ ] **Step 4: Require v2 on new creates**

Add `identityVersion` to `hasOnly` and require:

```text
request.resource.data.identityVersion == 2
```

Do not change the update whitelist, read authorization, or delete denial.

- [ ] **Step 5: Verify GREEN**

Run both commands from Step 2. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt app/src/test/java/com/menudado/backend/AiMenuHiveDataSourceTest.kt firestore.rules qa/firestore-rules.test.mjs
git commit -m "feat: require v2 hive identities"
```

### Task 4: Token-neutral prompt reinforcement

**Files:**
- Modify: `app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt`
- Modify: `app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt`

- [ ] **Step 1: Write a failing compactness test**

Extract the new canonical instruction and compare it against the four current
deduplication instructions kept as a test-only legacy string:

```kotlin
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
```

Existing JSON-schema tests must remain unchanged.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ai.MenuGenerationPromptTest
```

Expected: FAIL because the current prompt has four lines and no compact v2 rule.

- [ ] **Step 3: Replace, do not append, prompt text**

Replace the four existing lines with one shorter line:

```text
- deduplication_key: English, exactly dish family|alphabetically sorted main ingredients|preparation; use canonical ingredients (vegetables, lentils, tomato), canonical preparation forms (mixed, baked, grilled, boiled, stewed), pasta for spaghetti or macaroni, and no profile data.
```

Do not change the JSON example, generated fields, repository call, quota, or
timeout.

- [ ] **Step 4: Verify GREEN**

Run the prompt tests. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt app/src/test/java/com/menudado/ai/MenuGenerationPromptTest.kt
git commit -m "fix: constrain hive identity without prompt growth"
```

### Task 5: Dry-run-first Firebase consolidation utility

**Files:**
- Create: `qa/ai-menu-hive-identity-v2.mjs`
- Create: `qa/ai-menu-hive-identity-v2.test.mjs`
- Create: `qa/migrate-ai-menu-hive-v2.mjs`
- Read: `app/src/test/resources/ai-menu-hive-identity-v2-fixtures.tsv`
- Modify: `qa/package.json`
- Modify: `qa/package-lock.json`

- [ ] **Step 1: Write failing Node identity and migration-plan tests**

Load the shared TSV used by Kotlin. Test that every row produces its declared
canonical key, the three reported variants produce one hash, and a group with
three documents yields one write, merged `eligibilityKeys`, and two deletes.
Also test that dry-run planning never mutates the input.

- [ ] **Step 2: Verify RED**

```bash
cd qa && node --test ai-menu-hive-identity-v2.test.mjs
```

Expected: FAIL because the modules do not exist.

- [ ] **Step 3: Implement the pure identity and planner module**

Export:

```javascript
export function canonicalIdentity(language, rawKey) {
  const rawParts = String(rawKey ?? "").split("|");
  if (rawParts.length !== 3) return null;
  const parts = [
    canonicalComponent(rawParts[0]),
    canonicalIngredients(rawParts[1]),
    canonicalPreparation(rawParts[2])
  ];
  if (parts.some((part) => part.length === 0)) return null;
  const canonicalKey = parts.join("|");
  return {
    canonicalKey,
    semanticHash: createHash("sha256")
      .update(`${language}|${canonicalKey}`, "utf8")
      .digest("hex")
  };
}

export function buildMigrationPlan(documents) {
  const groups = new Map();
  for (const document of documents) {
    const identity = canonicalIdentity(
      document.data.language,
      document.data.semanticKey
    );
    if (!identity) continue;
    const group = groups.get(identity.semanticHash) ?? [];
    group.push({ ...document, identity });
    groups.set(identity.semanticHash, group);
  }
  return [...groups.entries()].map(([semanticHash, members]) => {
    const ordered = members.toSorted(compareCreatedAtThenId);
    const representative = ordered[0];
    const eligibilityKeys = [...new Set(
      ordered.flatMap(({ data }) => data.eligibilityKeys ?? [])
    )].sort();
    return {
      write: {
        id: semanticHash,
        data: {
          ...representative.data,
          identityVersion: 2,
          semanticHash,
          semanticKey: representative.identity.canonicalKey,
          eligibilityKeys
        }
      },
      deletes: ordered
        .map(({ id }) => id)
        .filter((id) => id !== semanticHash)
    };
  });
}
```

Use the same explicit aliases as Kotlin, preserve the oldest valid recipe,
merge and sort eligibility hashes, and return immutable `writes` and `deletes`
arrays.

- [ ] **Step 4: Implement the guarded Firestore CLI**

Require `--project=<exact-id>`. Default to dry-run. Require both `--apply` and
`--confirm-project=<same-id>` before writes. Before applying:

```javascript
await writeFile(backupPath, JSON.stringify(serializableDocuments, null, 2));
```

For each plan group, write the canonical document with
`identityVersion: 2`, reread and validate it, then delete only the duplicate
IDs. Never store credentials or backup output in Git.

- [ ] **Step 5: Add dependency and scripts**

Add `@google-cloud/firestore` and:

```json
"test:hive-migration": "node --test ai-menu-hive-identity-v2.test.mjs",
"migrate:hive-v2": "node migrate-ai-menu-hive-v2.mjs"
```

Run `npm install` in `qa` to update the lockfile.

- [ ] **Step 6: Verify GREEN**

```bash
cd qa && npm run test:hive-migration
```

Expected: PASS without accessing Firebase.

- [ ] **Step 7: Commit**

```bash
git add qa/package.json qa/package-lock.json qa/ai-menu-hive-identity-v2.mjs qa/ai-menu-hive-identity-v2.test.mjs qa/migrate-ai-menu-hive-v2.mjs
git commit -m "feat: add guarded hive deduplication migration"
```

### Task 6: Context and full verification

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Update project context**

Document that equivalence uses position-aware identity v2, old candidates are
collapsed on read, new shared documents carry `identityVersion: 2`, and the
prompt replacement does not add calls, fields, or prompt length.

- [ ] **Step 2: Run all local verification**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
cd qa && npm run test:hive-migration
firebase emulators:exec --only firestore "cd qa && npm run test:firestore-rules"
git diff --check
```

Expected: all commands pass and `git diff --check` is silent.

- [ ] **Step 3: Inspect scope**

```bash
git status --short
git diff --stat HEAD~5
git log --oneline -6
```

Expected: only the identity, read policy, mapper/rules, prompt, QA migration,
tests, and project context are changed.

- [ ] **Step 4: Commit documentation**

```bash
git add docs/project-context.md
git commit -m "docs: document hive identity v2"
```

- [ ] **Step 5: Run final clean verification**

Repeat the commands in Step 2 after the final commit. Record exact results for
handoff. Do not run the production migration or deploy Firestore rules without
separate live-environment authorization and credentials.
