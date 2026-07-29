# General AI Menu Similarity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent multilingual or minimally varied versions of the same AI recipe from being presented as different Firestore hive alternatives.

**Architecture:** Extend the deterministic v2 identity with a reusable multilingual culinary concept vocabulary and a role-independent concept signature. Keep canonical document IDs exact, then collapse highly similar candidates locally before rotation; mirror exact canonicalization and review-only similarity detection in the Node migration.

**Tech Stack:** Kotlin, JUnit 4, Kotlin coroutines test, Node.js test runner, Firebase Firestore migration tooling, Gradle.

---

### Task 1: Capture multilingual identity regressions

**Files:**
- Modify: `app/src/test/resources/ai-menu-hive-identity-v2-fixtures.tsv`
- Modify: `app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt`
- Modify: `qa/ai-menu-hive-identity-v2.test.mjs`

- [ ] **Step 1: Add shared fixtures for the reported recipe**

Append fixtures carrying a group identifier so Kotlin and Node can assert
equivalence within each group:

```text
lentil-salad	SPANISH	salad|lentils+vegetables|mix	salad|lentils+vegetables|mixed
toast-avocado-egg	SPANISH	toast|avocado|egg	toast|avocado|egg
toast-avocado-egg	SPANISH	tostada|aguacate|huevo	toast|avocado|egg
toast-avocado-egg	SPANISH	tartine|avocat|oeuf	toast|avocado|egg
```

Update the existing three rows to begin with `lentil-salad`.

- [ ] **Step 2: Write failing Kotlin identity tests**

Parse `(group, language, rawKey, canonicalKey)` and assert each group produces
one identity. Add:

```kotlin
@Test
fun `reported toast variants share canonical identity`() {
    val identities = identityFixtures()
        .filter { it.group == "toast-avocado-egg" }
        .map { requireNotNull(AiMenuHiveIdentity.from(it.language, it.rawKey)) }

    assertEquals(1, identities.distinct().size)
    assertEquals("toast|avocado|egg", identities.first().canonicalKey)
}
```

- [ ] **Step 3: Write failing Node fixture assertions**

Group fixtures by `group` and assert that every group has exactly one canonical
key and one semantic hash.

- [ ] **Step 4: Run tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.domain.AiMenuHiveIdentityTest'
cd qa && node --test ai-menu-hive-identity-v2.test.mjs
```

Expected: both suites fail because `tostada`, `tartine`, `aguacate`, `avocat`,
`huevo` and `oeuf` are not canonicalized.

### Task 2: Implement reusable conceptual identity

**Files:**
- Modify: `app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt`
- Modify: `app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt`

- [ ] **Step 1: Add failing similarity tests**

Add tests for role-independent equality, a minor garnish, and negative cases:

```kotlin
@Test
fun `same concepts remain equivalent when model changes their position`() {
    assertTrue(
        AiMenuHiveIdentity.areSimilar(
            "toast|avocado+egg|assembled",
            "toast|avocado|egg+assembled"
        )
    )
}

@Test
fun `minor garnish is similar but ingredient or technique changes are not`() {
    assertTrue(
        AiMenuHiveIdentity.areSimilar(
            "toast|avocado+egg|assembled",
            "tostada|aguacate+huevo+cilantro|montada"
        )
    )
    assertFalse(
        AiMenuHiveIdentity.areSimilar(
            "salad|chicken+tomato|mixed",
            "salad|chicken+avocado|mixed"
        )
    )
    assertFalse(
        AiMenuHiveIdentity.areSimilar(
            "potato|potato|fried",
            "potato|potato|baked"
        )
    )
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.domain.AiMenuHiveIdentityTest'
```

Expected: compilation fails because `areSimilar` does not exist.

- [ ] **Step 3: Implement canonical vocabulary and signatures**

Keep a data-driven alias map for common culinary families, ingredients and
preparations across English, Spanish and French. Add:

```kotlin
data class AiMenuConceptSignature(
    val family: String,
    val concepts: Set<String>
)

fun conceptSignature(rawKey: String?): AiMenuConceptSignature? {
    val parts = canonicalParts(rawKey) ?: return null
    return AiMenuConceptSignature(
        family = parts.first().split('+').first(),
        concepts = parts.flatMap { it.split('+') }.toSet()
    )
}

fun areSimilar(firstKey: String?, secondKey: String?): Boolean {
    val first = conceptSignature(firstKey) ?: return false
    val second = conceptSignature(secondKey) ?: return false
    if (first.concepts == second.concepts) return true
    if (first.family != second.family) return false
    val intersection = first.concepts.intersect(second.concepts).size.toDouble()
    val union = first.concepts.union(second.concepts).size.toDouble()
    return union > 0 && intersection / union >= 0.80
}
```

Use one `canonicalParts` function from both `from` and `conceptSignature`.
Every position must preserve `+`/`,` subcomponents so a concept accidentally
placed in the preparation segment is not lost.

Keep all existing aliases and add this conservative multilingual vocabulary:

```text
toast/tostada/tartine -> toast
salad/ensalada/salade -> salad
soup/sopa/soupe -> soup
stew/guiso/estofado/ragout -> stew
bowl/bol -> bowl
sandwich/bocadillo -> sandwich
omelette/omelet/tortilla francesa -> omelette
avocado/aguacate/avocat -> avocado
egg/huevo/oeuf -> egg
chicken/pollo/poulet -> chicken
rice/arroz/riz -> rice
chickpea/garbanzo/pois chiche -> chickpeas
potato/patata/pomme de terre -> potato
assembled/mounted/montada/montee -> assembled
fried/frito/frit/frite -> fried
baked/horneado/au four -> baked
grilled/plancha/parrilla/grille -> grilled
boiled/cocido/hervido/bouilli -> boiled
stewed/guisado/estofado/mijote -> stewed
roasted/asado/roti -> roasted
```

Include explicit singular/plural and grammatical-gender forms for these terms;
do not remove suffixes generically.

- [ ] **Step 4: Run identity tests and verify GREEN**

Run the focused Gradle command from Step 2.

Expected: all `AiMenuHiveIdentityTest` tests pass.

### Task 3: Collapse similar candidates before rotation

**Files:**
- Modify: `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`
- Modify: `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`

- [ ] **Step 1: Write failing repository regressions**

Create two legacy `SharedAiMenu` entries using the exact reported keys and
assert `pickIndexProvider` receives one candidate. Add another pair with
`toast|avocado+egg|assembled` and
`tostada|aguacate+huevo+cilantro|montada`; assert it also receives one.

Add negative coverage using fried and baked potato and assert the selectable
count remains two.

- [ ] **Step 2: Run the repository tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.data.AiMenuHiveRepositoryTest'
```

Expected: the near-similar pair is counted as two candidates.

- [ ] **Step 3: Implement deterministic grouping**

After canonicalization and dietary validation:

```kotlin
val safe = candidates
    .mapNotNull(SharedAiMenu::canonicalized)
    .filter { request.profile.accepts(it.generatedMenu) }
    .sortedBy(SharedAiMenu::semanticHash)
    .fold(mutableListOf<SharedAiMenu>()) { unique, candidate ->
        if (unique.none { existing ->
                AiMenuHiveIdentity.areSimilar(
                    existing.semanticKey,
                    candidate.semanticKey
                )
            }) {
            unique += candidate
        }
        unique
    }
```

The sorted hash makes the representative stable for rotation history.

- [ ] **Step 4: Run repository and identity tests and verify GREEN**

Run both focused Gradle test classes.

Expected: all tests pass, including the current rotation tests.

### Task 4: Mirror canonicalization and safe migration reporting

**Files:**
- Modify: `qa/ai-menu-hive-identity-v2.mjs`
- Modify: `qa/ai-menu-hive-identity-v2.test.mjs`
- Modify: `qa/migrate-ai-menu-hive-v2.mjs`

- [ ] **Step 1: Write failing Node tests**

Assert:

- the three reported language variants produce one identity;
- `areSimilarIdentities` accepts role changes and the `0.80` garnish case;
- fried/baked and changed main ingredient cases remain distinct;
- `buildMigrationPlan` merges exact canonical duplicates;
- near-only matches appear in `reviewGroups` and not in `deletes`.

- [ ] **Step 2: Run Node tests and verify RED**

Run:

```bash
cd qa && node --test ai-menu-hive-identity-v2.test.mjs
```

Expected: failure because aliases, similarity and `reviewGroups` are missing.

- [ ] **Step 3: Implement the mirrored algorithm**

Export `conceptSignature` and `areSimilarIdentities` using the same aliases,
exact concept equality, same-family guard and `0.80` Jaccard threshold.

After exact migration writes are built, compare canonical writes in stable ID
order and return:

```js
{
  writes,
  deletes,
  skipped,
  reviewGroups: [
    { ids: ["first-hash", "second-hash"], similarity: 0.8 }
  ]
}
```

`reviewGroups` must never add IDs to `deletes`.

- [ ] **Step 4: Print review-only groups**

Update the CLI summary with:

```text
Similarity review groups: <count>
- REVIEW ONLY <id>, <id> (similarity 0.80; no automatic delete)
```

- [ ] **Step 5: Run Node tests and verify GREEN**

Run the Node command from Step 2.

Expected: all tests pass.

### Task 5: Update functional context and verify the complete change

**Files:**
- Modify: `docs/project-context.md`

- [ ] **Step 1: Update the project context**

Document that v2 now uses multilingual conceptual normalization, collapses
high-confidence similar candidates locally at `0.80`, and only deletes exact
canonical duplicates during migration.

- [ ] **Step 2: Run formatting and focused verification**

Run:

```bash
git diff --check
./gradlew :app:testDebugUnitTest \
  --tests 'com.menudado.domain.AiMenuHiveIdentityTest' \
  --tests 'com.menudado.data.AiMenuHiveRepositoryTest'
cd qa && node --test ai-menu-hive-identity-v2.test.mjs
```

Expected: zero whitespace errors and all focused tests pass.

- [ ] **Step 3: Run regression suite and compile**

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL` for both commands.

- [ ] **Step 4: Inspect the final diff**

Verify only the identity, hive repository, tests, migration tooling and project
context changed. Confirm no UI, quota, provider, Firestore query or private-menu
files changed.

- [ ] **Step 5: Commit the implementation**

```bash
git add app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt \
  app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt \
  app/src/test/java/com/menudado/domain/AiMenuHiveIdentityTest.kt \
  app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt \
  app/src/test/resources/ai-menu-hive-identity-v2-fixtures.tsv \
  qa/ai-menu-hive-identity-v2.mjs \
  qa/ai-menu-hive-identity-v2.test.mjs \
  qa/migrate-ai-menu-hive-v2.mjs \
  docs/project-context.md \
  docs/superpowers/plans/2026-07-29-general-ai-menu-similarity.md
git commit -m "fix: deduplicate similar AI menu recipes"
```
