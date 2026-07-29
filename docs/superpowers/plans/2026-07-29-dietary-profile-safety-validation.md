# Dietary Profile Safety Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent any detectably profile-incompatible AI menu from being shown, saved, shared, or contributed to the shared hive.

**Architecture:** Extend the existing domain compatibility module into the single deterministic authority for audience and dietary-profile safety. Call it at the three trust boundaries: live AI result, hive candidate selection, and generated-menu save. Keep clinical conditions advisory and make the UI contract explicit instead of inventing medical thresholds.

**Tech Stack:** Kotlin, Android MVVM, Jetpack Compose, JUnit 4, kotlinx-coroutines-test, Gradle.

---

### Task 1: Build the audience-aware safety validator

**Files:**
- Modify: `app/src/test/java/com/menudado/domain/DietaryProfileCompatibilityTest.kt`
- Modify: `app/src/main/java/com/menudado/domain/DietaryProfileCompatibility.kt`

- [ ] **Step 1: Write failing tests for the public compatibility contract**

Add tests that exercise the wished-for API:

```kotlin
@Test
fun `baby profile rejects honey added salt and whole choking hazards`() {
    val profile = DietaryProfile(ageRange = "6-24 meses")

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
        generated("Salsa con vino sin cocinar")
    ).forEach { menu ->
        assertFalse(profile.accepts(menu, MenuAudience.ADULT))
    }
}

@Test
fun `explicit compatible substitutions do not trigger ambiguous allergen terms`() {
    assertTrue(
        DietaryProfile(
            hasAllergies = true,
            allergens = setOf(DietaryAllergen.GLUTEN, DietaryAllergen.DAIRY)
        ).accepts(
            generated("Pasta sin gluten con crema vegetal"),
            MenuAudience.ADULT
        )
    )
}

@Test
fun `clinical condition names are not treated as food ingredients`() {
    val profile = DietaryProfile(otherAvoidances = "diabético, hipertenso")

    assertTrue(profile.accepts(generated("Lentejas con verduras"), MenuAudience.ADULT))
}

@Test
fun `prefixed concrete avoidance is enforced`() {
    val profile = DietaryProfile(otherAvoidances = "sin picante, sin champiñones")

    assertFalse(profile.accepts(generated("Arroz con champiñones"), MenuAudience.ADULT))
}
```

Use this helper inside the test class:

```kotlin
private fun generated(description: String) = GeneratedMenu(
    name = "Idea",
    description = description,
    notes = "",
    calories = 300
)
```

- [ ] **Step 2: Run the domain tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.domain.DietaryProfileCompatibilityTest
```

Expected: compilation fails because `accepts(GeneratedMenu, MenuAudience)` does not exist.

- [ ] **Step 3: Implement the result model and common validator**

Replace the single boolean-only path with:

```kotlin
enum class DietaryProfileViolation {
    VEGAN,
    ALLERGEN,
    EXPLICIT_AVOIDANCE,
    BABY_SAFETY,
    CHILD_SAFETY,
    PREGNANCY_SAFETY
}

data class DietaryProfileCompatibility(
    val violations: Set<DietaryProfileViolation>
) {
    val isCompatible: Boolean
        get() = violations.isEmpty()
}

fun DietaryProfile.compatibilityWith(
    menu: GeneratedMenu,
    audience: MenuAudience = MenuAudience.ADULT
): DietaryProfileCompatibility

fun DietaryProfile.accepts(
    menu: GeneratedMenu,
    audience: MenuAudience = MenuAudience.ADULT
): Boolean = compatibilityWith(menu, audience).isCompatible
```

Build one normalized searchable string from name, description, notes and shopping
products. Preserve `findIngredientConflicts()` for preflight input validation, but
route vegan/allergen/explicit-avoidance matching through shared helpers.

Implement the following closed rule groups:

```kotlin
private val CLINICAL_CONDITION_TERMS = setOf(
    "diabetes", "diabetico", "diabetica",
    "hypertension", "hipertension", "hipertenso", "hipertensa",
    "diabete", "diabetique", "hypertension arterielle"
)

private val BABY_UNSAFE_PHRASES = setOf(
    "miel", "honey", "miel ajoute",
    "sal anadida", "added salt", "sel ajoute",
    "azucar anadida", "added sugar", "sucre ajoute",
    "edulcorante", "sweetener", "edulcorant",
    "frutos secos enteros", "whole nuts", "noix entieres",
    "palomitas", "popcorn",
    "uvas enteras", "whole grapes", "raisins entiers",
    "tomates cherry enteros", "whole cherry tomatoes",
    "salchicha en rodajas", "sliced sausage",
    "trozos grandes de carne", "large chunks of meat",
    "huesos", "bones", "aretes"
)

private val CHILD_UNSAFE_PHRASES = setOf(
    "alcohol", "vino sin cocinar", "uncooked wine", "vin non cuit",
    "frutos secos enteros", "whole nuts", "noix entieres",
    "uvas enteras", "whole grapes", "raisins entiers",
    "palomitas", "popcorn"
)

private val PREGNANCY_UNSAFE_PHRASES = setOf(
    "alcohol", "vino sin cocinar", "uncooked wine", "vin non cuit",
    "leche no pasteurizada", "unpasteurized milk", "lait non pasteurise",
    "queso no pasteurizado", "unpasteurized cheese", "fromage non pasteurise",
    "huevo crudo", "raw egg", "oeuf cru",
    "carne cruda", "raw meat", "viande crue",
    "pescado crudo", "raw fish", "poisson cru",
    "sushi", "sashimi", "ceviche", "carpaccio", "steak tartar",
    "pez espada", "emperador", "atun rojo", "tiburon", "lucio",
    "swordfish", "bluefin tuna", "shark", "pike",
    "espadon", "thon rouge", "requin", "brochet"
)
```

Expand the existing vegan and allergen families with high-confidence derivatives:
gelatin, lard, whey, casein, semolina, couscous, seitan, miso, tempeh, sesame oil,
and common nut/shellfish names in the three supported languages.

Strip a leading `sin`, `no`, `without`, `avoid`, `sans` or `eviter` from concrete
avoidance entries. Ignore entries that normalize exactly to a clinical-condition
term. Treat explicit `sin gluten`, `gluten-free`, `sans gluten`, `crema vegetal`,
`plant cream`, `creme vegetale`, `harina de garbanzo`, `chickpea flour` and
`farine de pois chiche` as compatible contexts before applying ambiguous terms.

- [ ] **Step 4: Run the domain tests and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.domain.DietaryProfileCompatibilityTest
```

Expected: all compatibility tests pass.

- [ ] **Step 5: Commit the domain validator**

```bash
git add \
  app/src/main/java/com/menudado/domain/DietaryProfileCompatibility.kt \
  app/src/test/java/com/menudado/domain/DietaryProfileCompatibilityTest.kt
git commit -m "feat: validate generated menus against dietary profile"
```

### Task 2: Close every generated-menu trust boundary

**Files:**
- Modify: `app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`

- [ ] **Step 1: Write failing hive and ViewModel tests**

Add a hive test proving audience rules are content-validated:

```kotlin
@Test
fun `baby search rejects honey candidate even with matching eligibility`() = runTest {
    val unsafe = sharedMenu(
        key = "yogurt|fruit|mixed",
        name = "Yogur con miel"
    )
    val repository = AiMenuHiveRepository(
        RecordingAiMenuHiveDataSource(server = Result.success(listOf(unsafe))),
        AiMenuHiveFeatureToggle(true)
    ) { 0 }

    val result = repository.findCompatibleMenu(
        request(
            audience = MenuAudience.BABY,
            profile = DietaryProfile(ageRange = MenuAudience.BABY.defaultAgeRange)
        )
    ).getOrThrow()

    assertNull(result)
}
```

Extend the test request helper with an `audience` argument if it does not already
have one.

Add ViewModel tests:

```kotlin
@Test
fun `unsafe live result is hidden and searches hive without another AI call`() =
    runTest(dispatcher) {
        dietaryProfileStore.storedProfile = DietaryProfile(isVegan = true)
        analyzer.generatedMenu = sampleGeneratedMenu(
            name = "Pasta cuatro quesos",
            description = "Pasta con queso y nata"
        )
        hive.searchResult = Result.success(null)

        viewModel.generateMenuIdea()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showGeneratedMenuDetail)
        assertEquals(1, analyzer.generateCalls)
        assertEquals(1, hive.searches.size)
        assertEquals(
            "La idea no cumplía tu perfil alimentario y no la mostramos. " +
                "No encontramos una alternativa segura ahora; inténtalo más tarde.",
            viewModel.uiState.value.message
        )
    }

@Test
fun `profile change before generated save blocks persistence and hive contribution`() =
    runTest(dispatcher) {
        analyzer.generatedMenu = sampleGeneratedMenu(
            name = "Pasta con queso",
            description = "Pasta, tomate y queso"
        )
        viewModel.generateMenuIdea()
        advanceUntilIdle()
        viewModel.setDietaryProfileVegan(true)

        viewModel.saveGeneratedMenuIdea()
        advanceUntilIdle()

        assertTrue(dao.saved.isEmpty())
        assertTrue(hive.contributions.isEmpty())
    }
```

- [ ] **Step 2: Run focused tests and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest \
  --tests com.menudado.ui.MenuDadoViewModelTest
```

Expected: the hive returns the baby-unsafe candidate and the ViewModel opens or
saves the incompatible live result.

- [ ] **Step 3: Apply the validator to hive candidates**

Change the safety filter to include audience:

```kotlin
.filter { shared ->
    request.profile.accepts(
        menu = shared.generatedMenu,
        audience = request.audience
    )
}
```

- [ ] **Step 4: Reject unsafe live results before mutating visible state**

In the `generatedResult.isSuccess` branch, evaluate compatibility before clearing
retry state, advancing cuisine rotation, remembering the idea or copying it into
UI state:

```kotlin
if (!profile.accepts(generated, audience)) {
    searchHiveFallback(
        request = request,
        triggerFailureType = AI_FAILURE_PROFILE_MISMATCH,
        failureNotice = null,
        fallbackMissMessage = currentLanguage().profileMismatchMessage()
    )
} else {
    // Existing success path unchanged.
}
```

Add `fallbackMissMessage: String? = null` to `searchHiveFallback()` and prefer it
over quota/provider contextual messages only when no candidate exists.

Add the closed analytics value:

```kotlin
private const val AI_FAILURE_PROFILE_MISMATCH = "profile_mismatch"
```

Do not attach ingredient, allergen, pregnancy or condition values.

- [ ] **Step 5: Revalidate generated content when saving**

Change:

```kotlin
fun saveGeneratedMenuIdea() {
    saveMenu(ignoreGuestMenuSaveLimit = true)
}
```

to:

```kotlin
fun saveGeneratedMenuIdea() {
    saveMenu(
        ignoreGuestMenuSaveLimit = true,
        validateGeneratedProfile = true
    )
}
```

Add `validateGeneratedProfile: Boolean = false` to the private `saveMenu()`.
After required-field validation and before persistence, build a transient
`GeneratedMenu` from the visible fields and reject it when:

```kotlin
validateGeneratedProfile &&
    !dietaryProfileStore.getProfile(audience).accepts(
        menu = GeneratedMenu(
            name = name,
            description = description,
            notes = state.notes.trim(),
            calories = state.calories ?: 0,
            healthAnalysis = state.generatedHealthAnalysis,
            shoppingProducts = state.generatedShoppingProducts,
            deduplicationKey = state.generatedDeduplicationKey
        ),
        audience = audience
    )
```

Set the same generic localized message and return before Room or hive work.
Manual `saveMenu()` must continue with `validateGeneratedProfile = false`.

- [ ] **Step 6: Add localized private messages**

Add:

```kotlin
private fun AppLanguage.profileMismatchMessage(): String = when (this) {
    AppLanguage.SPANISH ->
        "La idea no cumplía tu perfil alimentario y no la mostramos. " +
            "No encontramos una alternativa segura ahora; inténtalo más tarde."
    AppLanguage.ENGLISH ->
        "The idea did not match your dietary profile, so we did not show it. " +
            "We could not find a safe alternative right now; please try again later."
    AppLanguage.FRENCH ->
        "L'idée ne respectait pas votre profil alimentaire, nous ne l'avons donc pas affichée. " +
            "Aucune alternative sûre n'est disponible pour le moment; réessayez plus tard."
}
```

- [ ] **Step 7: Run focused tests and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.menudado.data.AiMenuHiveRepositoryTest \
  --tests com.menudado.ui.MenuDadoViewModelTest
```

Expected: all focused tests pass and the fake analyzer records exactly one call.

- [ ] **Step 8: Commit the trust-boundary changes**

```bash
git add \
  app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt \
  app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt \
  app/src/test/java/com/menudado/data/AiMenuHiveRepositoryTest.kt \
  app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt
git commit -m "fix: block AI menus that violate dietary profiles"
```

### Task 3: Make the health-condition contract truthful

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `docs/project-context.md`

- [ ] **Step 1: Write a failing copy contract test**

Add a resource-source test following the existing resource contract style:

```kotlin
@Test
fun `dietary profile explains that health conditions need professional review`() {
    val spanish = resourceFile("values/strings.xml")
    val english = resourceFile("values-en/strings.xml")
    val french = resourceFile("values-fr/strings.xml")

    assertTrue(spanish.contains("dietary_other_supporting_text"))
    assertTrue(spanish.contains("profesional sanitario"))
    assertTrue(english.contains("health professional"))
    assertTrue(french.contains("professionnel de santé"))
}
```

If `MenuCardUiStateTest` has no resource helper, add the test to the existing
resource-oriented test class and reuse its helper instead of duplicating IO.

- [ ] **Step 2: Run the copy test and verify RED**

Run the exact owning test class. Expected: failure because
`dietary_other_supporting_text` does not exist.

- [ ] **Step 3: Update profile strings in all locales**

Use:

```xml
<!-- Spanish -->
<string name="dietary_other_label">Alimentos a evitar o indicaciones</string>
<string name="dietary_other_placeholder">Ej: sin picante, sin champiñones</string>
<string name="dietary_other_supporting_text">Las condiciones de salud orientan a la IA, pero no sustituyen la revisión de un profesional sanitario. Para una exclusión estricta, escribe alimentos concretos.</string>

<!-- English -->
<string name="dietary_other_label">Foods to avoid or guidance</string>
<string name="dietary_other_placeholder">E.g. no spicy food, no mushrooms</string>
<string name="dietary_other_supporting_text">Health conditions guide the AI but do not replace review by a health professional. For strict exclusion, enter specific foods.</string>

<!-- French -->
<string name="dietary_other_label">Aliments à éviter ou indications</string>
<string name="dietary_other_placeholder">Ex. sans épices, sans champignons</string>
<string name="dietary_other_supporting_text">Les conditions de santé orientent l’IA mais ne remplacent pas l’avis d’un professionnel de santé. Pour une exclusion stricte, indiquez des aliments précis.</string>
```

- [ ] **Step 4: Render the supporting text in the existing profile card**

Add immediately below the `OutlinedTextField`:

```kotlin
Text(
    text = stringResource(id = R.string.dietary_other_supporting_text),
    style = MaterialTheme.typography.bodySmall,
    color = MenuDadoColors.MutedInk
)
```

Do not introduce a new component or alter the surrounding layout.

- [ ] **Step 5: Update project context**

Document that:

- live AI and hive candidates use one local audience/profile validator;
- generated ideas are revalidated at save;
- detectable mismatches fail closed without another AI call;
- free-text clinical conditions remain advisory and concrete foods are required
  for strict local exclusion.

- [ ] **Step 6: Run the copy test and verify GREEN**

Run the exact owning test class. Expected: pass.

- [ ] **Step 7: Commit UI copy and documentation**

```bash
git add \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-en/strings.xml \
  app/src/main/res/values-fr/strings.xml \
  app/src/main/java/com/menudado/ui/MenuDadoScreen.kt \
  app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt \
  docs/project-context.md
git commit -m "copy: clarify dietary condition safety limits"
```

### Task 4: Full verification and handoff

**Files:**
- Verify all modified files

- [ ] **Step 1: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, zero failed tests.

- [ ] **Step 2: Compile debug Kotlin**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run Android lint for debug**

```bash
./gradlew :app:lintDebug
```

Expected: `BUILD SUCCESSFUL` and no newly introduced lint errors.

- [ ] **Step 4: Inspect scope and whitespace**

```bash
git status --short
git diff --check HEAD~3..HEAD
git diff --stat HEAD~3..HEAD
```

Expected: only validator, tests, ViewModel/hive wiring, localized profile copy,
project context, spec and plan are present; no Firebase, Remote Config, Room,
Gradle or model configuration is changed.

- [ ] **Step 5: Perform a targeted manual QA checklist**

On a debug build:

1. Activate `Bebé`, generate an idea, and confirm compatible content opens.
2. Configure vegan and an allergen, provide a conflicting base ingredient, and
   confirm no AI call is started.
3. Confirm an incompatible fake/test response never opens the detail.
4. Change profile while generated detail is open and confirm save fails closed.
5. Open Perfil in Spanish, English and French and confirm supporting copy fits
   without clipping.

If device/emulator validation is unavailable, report these five cases as residual
manual QA instead of claiming them complete.
