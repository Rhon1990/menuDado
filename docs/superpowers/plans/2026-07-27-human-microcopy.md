# Human Microcopy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reescribir y auditar todos los textos visibles de MenuDado para conectar con la necesidad cotidiana de la persona antes de presentar la acción y el valor de la IA.

**Architecture:** Los cambios se concentran en recursos Android localizados y mantienen intactos navegación, lógica, analítica, Firebase e IA. Un test de contrato leerá los XML reales para fijar la voz principal, paridad de claves y placeholders entre español, inglés y francés; los dos únicos textos accesibles hardcodeados se moverán a recursos.

**Tech Stack:** Android string resources, Kotlin, JUnit 4, XML DOM, Jetpack Compose.

---

### Task 1: Contrato automatizado de microcopy

**Files:**
- Create: `app/src/test/java/com/menudado/ui/LocalizedMicrocopyTest.kt`

- [ ] **Step 1: Crear el lector de recursos y el test RED**

Crear un test que:

1. Localice `values`, `values-en` y `values-fr` desde la raíz del repositorio o del módulo `app`.
2. Lea todos los elementos `<string>` mediante `DocumentBuilderFactory`.
3. Ignore en paridad las claves con `translatable="false"`.
4. Compare claves traducibles y firmas de placeholders.
5. Fije estas anclas de voz:

```kotlin
private val expectedHumanCopy = mapOf(
    "es" to mapOf(
        "home_today_title" to "¿No sabes qué cocinar?",
        "home_ai_dice_title" to "Lanza el dado",
        "dice_ai_action" to "Idea saludable creada con IA",
        "ai_generation_loading_title" to "Buscando algo rico para ti",
        "generated_menu_detail_title" to "Una idea pensada para ti",
        "analysis_ai" to "Lo que la IA ve en tu menú"
    ),
    "en" to mapOf(
        "home_today_title" to "Not sure what to cook?",
        "home_ai_dice_title" to "Roll the die",
        "dice_ai_action" to "Healthy idea created with AI",
        "ai_generation_loading_title" to "Finding something tasty for you",
        "generated_menu_detail_title" to "An idea picked for you",
        "analysis_ai" to "What AI sees in your menu"
    ),
    "fr" to mapOf(
        "home_today_title" to "Vous ne savez pas quoi cuisiner ?",
        "home_ai_dice_title" to "Lancez le dé",
        "dice_ai_action" to "Une idée équilibrée créée avec l’IA",
        "ai_generation_loading_title" to "Nous cherchons quelque chose de bon pour vous",
        "generated_menu_detail_title" to "Une idée pensée pour vous",
        "analysis_ai" to "Ce que l’IA observe dans votre menu"
    )
)
```

El test debe rechazar los textos antiguos `Lanzar con IA`, `Launch with AI` y `Lancer avec l'IA`.

- [ ] **Step 2: Ejecutar RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.LocalizedMicrocopyTest'
```

Expected: FAIL porque los textos ancla todavía conservan el tono anterior.

- [ ] **Step 3: Confirmar que el fallo es de intención**

Verificar que la salida muestra diferencias de valor en las claves ancla, no errores de ruta ni de parseo XML. Corregir el lector si falla antes de comparar contenido.

### Task 2: Reescribir y auditar español

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Aplicar la jerarquía necesidad, acción y valor**

Usar estas anclas exactas:

```xml
<string name="home_today_title">¿No sabes qué cocinar?</string>
<string name="home_today_subtitle">Elige la comida y para quién es. La IA te dará una idea saludable.</string>
<string name="home_ai_dice_title">Lanza el dado</string>
<string name="home_ai_dice_body">MenuDado usa IA para crear una idea saludable según tu perfil.</string>
<string name="dice_roll_ai_with_count">Lanza el dado (%1$d)</string>
<string name="dice_ai_action">Idea saludable creada con IA</string>
<string name="ai_generation_loading_title">Buscando algo rico para ti</string>
<string name="ai_generation_loading_message">La IA está creando una idea saludable según lo que elegiste.</string>
<string name="generated_menu_detail_title">Una idea pensada para ti</string>
<string name="analysis_ai">Lo que la IA ve en tu menú</string>
<string name="ai_analyze_with_count">Revisar con IA (%1$d)</string>
<string name="ai_analyze_pending_with_count">Revisar menús pendientes (%1$d)</string>
```

- [ ] **Step 2: Humanizar el resto de flujos**

Auditar todas las claves y cambiar las que no cumplan la voz aprobada. Cubrir explícitamente:

- Comunes, éxito, favoritos, compartir y eliminación.
- Cuenta, errores de acceso y modo invitado.
- Mercado y sus estados vacíos.
- Perfil alimentario y ayudas.
- Actualización, acerca, salud y privacidad.
- `Mi zona`, beneficios y sesión iniciada.
- Dado, selección aleatoria, bloqueos y recuperación sin resultados.
- Formulario, edición, fotos y campos.
- Generación, resultado, reintento, cuota y espera de IA.
- Onboarding, resultados, calorías y accesibilidad.

Mantener sin cambios las etiquetas que ya sean la formulación más clara, por ejemplo nombres de comidas, alérgenos, cocinas, navegación y acciones breves como `Guardar`, `Editar` o `Eliminar`.

- [ ] **Step 3: Validar español**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.LocalizedMicrocopyTest'
```

Expected: todavía puede fallar por inglés y francés, pero todas las anclas españolas deben pasar.

### Task 3: Transcrear inglés y francés

**Files:**
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Aplicar las anclas inglesas**

```xml
<string name="home_today_title">Not sure what to cook?</string>
<string name="home_today_subtitle">Choose the meal and who it’s for. AI will give you a healthy idea.</string>
<string name="home_ai_dice_title">Roll the die</string>
<string name="home_ai_dice_body">MenuDado uses AI to create a healthy idea based on your profile.</string>
<string name="dice_roll_ai_with_count">Roll the die (%1$d)</string>
<string name="dice_ai_action">Healthy idea created with AI</string>
<string name="ai_generation_loading_title">Finding something tasty for you</string>
<string name="ai_generation_loading_message">AI is creating a healthy idea from your choices.</string>
<string name="generated_menu_detail_title">An idea picked for you</string>
<string name="analysis_ai">What AI sees in your menu</string>
<string name="ai_analyze_with_count">Review with AI (%1$d)</string>
<string name="ai_analyze_pending_with_count">Review pending menus (%1$d)</string>
```

- [ ] **Step 2: Aplicar las anclas francesas**

```xml
<string name="home_today_title">Vous ne savez pas quoi cuisiner ?</string>
<string name="home_today_subtitle">Choisissez le repas et pour qui il est prévu. L’IA vous proposera une idée équilibrée.</string>
<string name="home_ai_dice_title">Lancez le dé</string>
<string name="home_ai_dice_body">MenuDado utilise l’IA pour créer une idée équilibrée adaptée à votre profil.</string>
<string name="dice_roll_ai_with_count">Lancez le dé (%1$d)</string>
<string name="dice_ai_action">Une idée équilibrée créée avec l’IA</string>
<string name="ai_generation_loading_title">Nous cherchons quelque chose de bon pour vous</string>
<string name="ai_generation_loading_message">L’IA crée une idée équilibrée à partir de vos choix.</string>
<string name="generated_menu_detail_title">Une idée pensée pour vous</string>
<string name="analysis_ai">Ce que l’IA observe dans votre menu</string>
<string name="ai_analyze_with_count">Vérifier avec l’IA (%1$d)</string>
<string name="ai_analyze_pending_with_count">Vérifier les menus en attente (%1$d)</string>
```

- [ ] **Step 3: Transcrear el resto de claves auditadas**

Reproducir la intención del español en inglés y francés sin traducción literal. Conservar exactamente:

- Nombres de claves.
- `%1$d`, `%1$s`, `%2$s` y plurales.
- Significado legal, sanitario y destructivo.
- Referencias visibles a AI/IA en generación, análisis y personalización.

- [ ] **Step 4: Ejecutar GREEN del contrato**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.LocalizedMicrocopyTest'
```

Expected: BUILD SUCCESSFUL.

### Task 4: Accesibilidad y contexto funcional

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Modify: `docs/project-context.md`
- Test: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`

- [ ] **Step 1: Añadir test RED para descripciones localizadas**

Añadir helpers de recursos y pruebas:

```kotlin
@StringRes
internal fun menuExpandDescriptionRes(isExpanded: Boolean): Int =
    if (isExpanded) R.string.menu_collapse_description else R.string.menu_expand_description

@Test
fun `expandir y contraer menu usan descripciones localizadas`() {
    assertEquals(R.string.menu_expand_description, menuExpandDescriptionRes(false))
    assertEquals(R.string.menu_collapse_description, menuExpandDescriptionRes(true))
}
```

Ejecutar el test y confirmar RED porque los recursos y el helper no existen.

- [ ] **Step 2: Mover los textos hardcodeados**

Añadir recursos localizados:

```xml
<!-- es -->
<string name="menu_expand_description">Ver detalles de %1$s</string>
<string name="menu_collapse_description">Ocultar detalles de %1$s</string>

<!-- en -->
<string name="menu_expand_description">View details for %1$s</string>
<string name="menu_collapse_description">Hide details for %1$s</string>

<!-- fr -->
<string name="menu_expand_description">Afficher les détails de %1$s</string>
<string name="menu_collapse_description">Masquer les détails de %1$s</string>
```

Reemplazar `Contraer menu` y `Expandir menu` en Compose por `stringResource(menuExpandDescriptionRes(isExpanded), menu.name)`.

- [ ] **Step 3: Actualizar el contexto**

Documentar en `docs/project-context.md`:

- Voz cercana, breve y cotidiana.
- Jerarquía necesidad, acción y valor de IA.
- Inicio usa `¿No sabes qué cocinar?` y `Lanza el dado`.
- IA visible como creadora, analista y personalizadora.
- Errores con explicación y siguiente paso.

- [ ] **Step 4: Ejecutar GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.menudado.ui.MenuCardUiStateTest' --tests 'com.menudado.ui.LocalizedMicrocopyTest'
```

Expected: BUILD SUCCESSFUL.

### Task 5: Validación integral local

**Files:**
- Verify all modified files.

- [ ] **Step 1: Escanear textos hardcodeados**

Run:

```bash
rg -n 'text = "|contentDescription = "|Contraer menu|Expandir menu|Lanzar con IA|Launch with AI|Lancer avec' app/src/main/java/com/menudado/ui -g '*.kt'
```

Expected: solo símbolos visuales o contenido dinámico justificado; ninguna frase visible pendiente.

- [ ] **Step 2: Ejecutar suite y builds**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:lintRelease :app:bundleRelease
```

Expected: BUILD SUCCESSFUL y cero fallos.

- [ ] **Step 3: QA visual**

Instalar Debug y revisar Inicio, generación, detalle, Mercado, Perfil, `Mi zona`, onboarding y errores en español. Cambiar el idioma del dispositivo a inglés y francés y revisar las pantallas principales. Repetir Inicio con tamaño de fuente ampliado.

- [ ] **Step 4: Revisar el diff y conservarlo local**

Run:

```bash
git diff --check
git status --short
```

Expected: solo recursos, tests, Compose, contexto y documentos del plan. No ejecutar `git push`, no crear PR y no modificar servicios remotos.
