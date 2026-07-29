# MenuDado 1.3.0 Account Benefits and About Copy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish the current feature branch as `1.3.0 (14)` with accurate, conversion-focused account benefits and a complete localized About description.

**Architecture:** Keep the existing Compose and resource architecture unchanged. Update the version contract in Gradle, reduce `myZoneAccountBenefitRes()` to four localized resources, replace the localized About fallback, and synchronize tests and `docs/project-context.md`; Remote Config remains an external override.

**Tech Stack:** Android, Kotlin, Jetpack Compose, Android string resources, JUnit 4, Gradle.

---

## File map

- `app/build.gradle.kts`: authoritative `versionName` and `versionCode`.
- `app/src/main/res/values/strings.xml`: Spanish/default account and About copy.
- `app/src/main/res/values-en/strings.xml`: English account and About copy.
- `app/src/main/res/values-fr/strings.xml`: French account and About copy.
- `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: ordered account-benefit resource list.
- `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`: version and account-benefit contracts.
- `docs/project-context.md`: current product behavior, limits, copy intent, and release evidence.

### Task 1: Advance the branch to version 1.3.0

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt:1362-1368`
- Modify: `app/build.gradle.kts:43-44`

- [ ] **Step 1: Update the version expectations first**

Replace the three version assertions with:

```kotlin
assertEquals("1.3.0", BuildConfig.VERSION_NAME)
assertEquals(14, BuildConfig.VERSION_CODE)
assertEquals("1.3.0 (14)", aboutVersionLabel(versionName = "1.3.0", versionCode = 14))
```

- [ ] **Step 2: Run the focused test and verify the red state**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: FAIL because `BuildConfig.VERSION_NAME` is still `1.2.1` and
`BuildConfig.VERSION_CODE` is still `13`.

- [ ] **Step 3: Update the authoritative Gradle version**

Set:

```kotlin
versionCode = 14
versionName = "1.3.0"
```

- [ ] **Step 4: Re-run the focused test**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: PASS.

- [ ] **Step 5: Commit the version change**

```bash
git add app/build.gradle.kts app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "build: bump MenuDado to 1.3.0"
```

### Task 2: Replace the account-benefit contract

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt:324-338`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt:2141-2148`
- Modify: `app/src/main/res/values/strings.xml:127-137`
- Modify: `app/src/main/res/values-en/strings.xml:126-135`
- Modify: `app/src/main/res/values-fr/strings.xml:126-135`

- [ ] **Step 1: Write the new ordered-list expectation**

Replace the expected list with:

```kotlin
listOf(
    R.string.my_zone_benefit_ai_uses,
    R.string.my_zone_benefit_reinstall,
    R.string.my_zone_benefit_profile,
    R.string.my_zone_benefit_saved_content
)
```

- [ ] **Step 2: Run the focused test and verify the red state**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest
```

Expected: FAIL at Kotlin/resource compilation because
`my_zone_benefit_ai_uses` and `my_zone_benefit_saved_content` do not exist yet.

- [ ] **Step 3: Add the four Spanish/default benefits**

Use exactly:

```xml
<string name="my_zone_guest_body">Crea tu cuenta gratis para proteger lo que guardas y disponer de 10 usos gratuitos de IA al día.</string>
<string name="my_zone_benefits_title">Tu cuenta te da más</string>
<string name="my_zone_guest_prompt">Crea una cuenta para recuperar tus datos si reinstalas la app</string>
<string name="my_zone_benefit_ai_uses">Con tu cuenta tienes 10 usos gratuitos de IA al día.</string>
<string name="my_zone_benefit_reinstall">Recupera tus menús y favoritos si reinstalas la app.</string>
<string name="my_zone_benefit_profile">Conserva tu perfil alimentario y preferencias en tu cuenta.</string>
<string name="my_zone_benefit_saved_content">Mantén guardados tus menús analizados y los productos de su lista de mercado.</string>
```

Remove the obsolete `my_zone_benefit_personalization`,
`my_zone_benefit_favorites`, `my_zone_benefit_devices`, and
`my_zone_benefit_ai_context` resources.

- [ ] **Step 4: Add the equivalent English benefits**

Use exactly:

```xml
<string name="my_zone_guest_body">Create your free account to protect what you save and get 10 free AI uses per day.</string>
<string name="my_zone_benefits_title">Your account gives you more</string>
<string name="my_zone_guest_prompt">Create an account to recover your data if you reinstall the app</string>
<string name="my_zone_benefit_ai_uses">Your account includes 10 free AI uses per day.</string>
<string name="my_zone_benefit_reinstall">Recover your menus and favorites if you reinstall the app.</string>
<string name="my_zone_benefit_profile">Keep your food profile and preferences in your account.</string>
<string name="my_zone_benefit_saved_content">Keep your analyzed menus and their shopping list items saved to your account.</string>
```

- [ ] **Step 5: Add the equivalent French benefits**

Use exactly:

```xml
<string name="my_zone_guest_body">Créez gratuitement votre compte pour protéger ce que vous enregistrez et obtenir 10 utilisations gratuites de l’IA par jour.</string>
<string name="my_zone_benefits_title">Votre compte vous offre plus</string>
<string name="my_zone_guest_prompt">Créez un compte pour récupérer vos données si vous réinstallez l’app</string>
<string name="my_zone_benefit_ai_uses">Votre compte comprend 10 utilisations gratuites de l’IA par jour.</string>
<string name="my_zone_benefit_reinstall">Retrouvez vos menus et favoris si vous réinstallez l’app.</string>
<string name="my_zone_benefit_profile">Conservez votre profil alimentaire et vos préférences dans votre compte.</string>
<string name="my_zone_benefit_saved_content">Gardez dans votre compte vos menus analysés et les produits de leur liste de courses.</string>
```

- [ ] **Step 6: Return only the four current resources from the UI helper**

Set:

```kotlin
internal fun myZoneAccountBenefitRes(): List<Int> = listOf(
    R.string.my_zone_benefit_ai_uses,
    R.string.my_zone_benefit_reinstall,
    R.string.my_zone_benefit_profile,
    R.string.my_zone_benefit_saved_content
)
```

Apply `myZoneBenefitsContentModifier(rememberScrollState())` to the dialog
column and define the helper as `Modifier.verticalScroll(scrollState)`.

- [ ] **Step 7: Run the focused test and resource compilation**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest :app:processDebugResources
```

Expected: PASS and all three locale resource sets compile.

- [ ] **Step 8: Verify locale key parity**

Run:

```bash
for key in my_zone_benefits_title my_zone_guest_body my_zone_benefit_ai_uses my_zone_benefit_reinstall my_zone_benefit_profile my_zone_benefit_saved_content; do
  test "$(rg -l "name=\"$key\"" app/src/main/res/values{,-en,-fr}/strings.xml | wc -l | tr -d ' ')" = "3"
done
```

Expected: exit code 0.

- [ ] **Step 9: Commit the account copy**

```bash
git add app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt
git commit -m "feat: clarify registered account benefits"
```

### Task 3: Refresh the localized About fallback

**Files:**
- Modify: `app/src/main/res/values/strings.xml:119`
- Modify: `app/src/main/res/values-en/strings.xml:119`
- Modify: `app/src/main/res/values-fr/strings.xml:119`

- [ ] **Step 1: Replace the Spanish/default description**

```xml
<string name="about_reason">MenuDado nació para resolver una pregunta cotidiana: ¿qué preparo hoy? Genera ideas con IA adaptadas a tu perfil, guarda tus menús, deja que el dado te ayude a elegir y reúne los productos en tu lista de mercado. Puedes empezar sin registrarte y crear una cuenta gratis para conservar tus datos.</string>
```

- [ ] **Step 2: Replace the English description**

```xml
<string name="about_reason">MenuDado was created to answer an everyday question: what should I make today? It generates AI ideas tailored to your profile, saves your menus, lets the dice help you choose, and gathers the items in your shopping list. You can start without registering and create a free account to keep your data.</string>
```

- [ ] **Step 3: Replace the French description**

```xml
<string name="about_reason">MenuDado a été créé pour répondre à une question du quotidien : que préparer aujourd’hui ? L’app génère des idées avec l’IA adaptées à votre profil, enregistre vos menus, laisse le dé vous aider à choisir et regroupe les produits dans votre liste de courses. Vous pouvez commencer sans vous inscrire et créer gratuitement un compte pour conserver vos données.</string>
```

- [ ] **Step 4: Compile localized resources**

Run:

```bash
./gradlew :app:processDebugResources
```

Expected: PASS without malformed XML or missing-resource errors.

- [ ] **Step 5: Confirm Remote Config precedence remains unchanged**

Run:

```bash
rg -n 'KEY_ABOUT_DESCRIPTION|remoteTextOrFallback' app/src/main/java/com/menudado/about/MenuDadoAboutRemoteConfig.kt
```

Expected: `about_description_v2` uses the local description as fallback and
retires the stale `about_description` value.

- [ ] **Step 6: Commit the About copy**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml
git commit -m "copy: refresh About MenuDado description"
```

### Task 4: Synchronize project context

**Files:**
- Modify: `docs/project-context.md:170-182`
- Modify: `docs/project-context.md:208-214`
- Modify: `docs/project-context.md:237-243`
- Modify: `docs/project-context.md:340-347`

- [ ] **Step 1: Document the account proposition accurately**

Add that the account-benefits modal highlights:

```markdown
- 10 daily free AI uses for a registered account versus 5 for a guest when `guest_ai_limits_enabled=true`;
- recovery of menus and favorites after reinstalling;
- preservation of the dietary profile and preferences;
- analyzed menus and shopping-list products associated with the account.
```

Do not describe another phone or unlimited menu saving as a benefit.

- [ ] **Step 2: Update the About fallback description**

Document that the local fallback now explains AI ideas, saved menus, dice
selection, the shopping list, guest entry, and optional account creation.
Keep the Remote Config precedence statement.

- [ ] **Step 3: Update the target version**

Replace:

```markdown
Versión objetivo actual: `1.2.1` (`versionCode` 13)
```

with:

```markdown
Versión objetivo actual: `1.3.0` (`versionCode` 14)
```

Move the previous 1.2.1 AAB evidence into an explicitly historical sentence;
do not relabel it as fresh 1.3.0 evidence.

- [ ] **Step 4: Check documentation consistency**

Run:

```bash
rg -n '1\\.2\\.1|versionCode.?13|otro móvil|sin límite' docs/project-context.md
```

Expected: any remaining `1.2.1 (13)` mention is explicitly historical; no
account-benefit promise says another phone or unlimited saving.

- [ ] **Step 5: Commit the documentation**

```bash
git add docs/project-context.md
git commit -m "docs: describe MenuDado 1.3.0 account value"
```

### Task 5: Run release-proportional verification

**Files:**
- Verify: all files changed in Tasks 1-4
- Inspect: `app/build/outputs/apk/debug/output-metadata.json`

- [ ] **Step 1: Run whitespace and stale-resource checks**

Run:

```bash
git diff --check HEAD~4..HEAD
rg -n 'my_zone_benefit_personalization|my_zone_benefit_favorites|my_zone_benefit_devices|my_zone_benefit_ai_context' app/src/main app/src/test
```

Expected: no whitespace errors and no obsolete benefit-resource references.

- [ ] **Step 2: Run the complete debug unit suite**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Build the debug APK**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Inspect generated version metadata**

Run:

```bash
rg -n '"versionCode": 14|"versionName": "1.3.0"|"applicationId": "com.menudado.debug"' app/build/outputs/apk/debug/output-metadata.json
```

Expected: all three values are present.

- [ ] **Step 5: Perform manual screen checks when a device is available**

Verify:

1. `Mi zona` shows `Tu cuenta te da más`.
2. The modal contains exactly four benefits and remains scrollable/readable.
3. No benefit promises unlimited use or another phone.
4. `Acerca de la app` shows the new fallback when `about_description_v2` is empty.
5. The footer shows `Versión 1.3.0 (14)`.
6. English and French follow the selected device locale without clipping.

- [ ] **Step 6: Record current evidence in project context**

Only after Steps 2-4 pass, add the exact fresh commands and artifact metadata
under the current 1.3.0 audit. Keep manual and Firebase Remote Config checks
listed separately if they were not completed.

- [ ] **Step 7: Commit the verified evidence if documentation changed**

```bash
git add docs/project-context.md
git commit -m "docs: record MenuDado 1.3.0 verification"
```
