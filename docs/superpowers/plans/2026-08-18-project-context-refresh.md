# Project Context Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Actualizar `docs/project-context.md` con el estado real de Android 16/API 36, eliminar contradicciones activas y separar evidencia actual, histórica y pendiente.

**Architecture:** El cambio se limita al documento existente y conserva su estructura funcional. `app/build.gradle.kts` y las constantes activas del código serán las fuentes de verdad para configuración; los resultados de QA conservarán su fecha y no se presentarán como verificación nueva.

**Tech Stack:** Markdown, Android Gradle Kotlin DSL, Git y búsquedas estáticas con `rg`.

---

### Task 0: Restaurar la línea base verde aprobada

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt:1578`
- Modify: `docs/superpowers/plans/2026-08-18-project-context-refresh.md`

- [x] **Step 1: Confirmar el fallo RED existente**

Ejecutar el test `acerca de la app muestra motivo creador y version` y confirmar que falla porque `BuildConfig.VERSION_CODE` esperado es `14` pero el valor actual es `15`.

- [x] **Step 2: Aplicar la expansión de alcance aprobada**

Cambiar únicamente la expectativa obsoleta de `BuildConfig.VERSION_CODE` de `14` a `15`. No modificar producción, Gradle, Firebase, recursos ni `docs/project-context.md`.

- [x] **Step 3: Confirmar GREEN dirigido**

Ejecutar de nuevo el test dirigido y confirmar que pasa.

- [x] **Step 4: Confirmar GREEN de la suite completa**

Ejecutar `./gradlew :app:testDebugUnitTest` y confirmar que toda la suite de tests unitarios debug pasa.

### Task 1: Confirmar las fuentes de verdad

**Files:**
- Read: `app/build.gradle.kts:36-44`
- Read: `app/src/main/java/com/menudado/about/MenuDadoAboutRemoteConfig.kt:1-80`
- Read: `docs/project-context.md:223-371`

- [x] **Step 1: Confirmar versión y SDK declarados**

Run:

```bash
rg -n "compileSdk|targetSdk|minSdk|versionCode|versionName" app/build.gradle.kts
```

Expected: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 23`, `versionCode = 15` y `versionName = "1.3.0"`.

- [x] **Step 2: Confirmar las claves activas de Acerca de la app**

Run:

```bash
rg -n "about_description|about_created_by|about_contact" app/src/main/java/com/menudado/about app/src/main/java/com/menudado/MainActivity.kt app/src/main/res
```

Expected: el contrato activo usa `about_description_v3`, `about_created_by` y `about_contact`; cualquier clave anterior solo puede documentarse como histórica.

- [x] **Step 3: Localizar contradicciones documentales**

Run:

```bash
rg -n "versionCode|Versión 1\.3\.0|about_description_v2|about_description_v3|Android 15/API 35|Android 16|targetSdk" docs/project-context.md
```

Expected: aparecen las referencias que deben clasificarse o corregirse sin modificar la especificación funcional no relacionada.

### Task 2: Actualizar el estado técnico y de Google Play

**Files:**
- Modify: `docs/project-context.md:223-312`

- [x] **Step 1: Añadir la fuente de verdad y configuración vigente**

Añadir en `Dirección Técnica` un bloque que indique:

```markdown
- Estado Android verificado estáticamente el 18 de agosto de 2026: `compileSdk=36`, `targetSdk=36`, `minSdk=23`, `versionName=1.3.0` y `versionCode=15`, según `app/build.gradle.kts`.
- Fuente de verdad: para configuración Android prevalecen `app/build.gradle.kts` y el manifiesto final del artefacto sobre datos históricos de este documento.
```

- [x] **Step 2: Documentar correctamente el estado de la política API 36**

Añadir dentro de `Publicación Play Store`:

```markdown
- Política de nivel de API de destino: la rama actual ya declara Android 16/API 36. Esto confirma el cumplimiento en la configuración local, pero Google Play solo retirará el aviso después de publicar y procesar en producción un AAB que conserve `targetSdk=36` o superior. La captura de Play Console no demuestra que el código actual siga orientado a API 35; puede corresponder al último artefacto productivo procesado.
```

- [x] **Step 3: Corregir la referencia limitada a Android 15**

Reformular la nota de edge-to-edge para indicar que se introdujo por Android 15/API 35 y debe seguir validándose al orientar la aplicación a Android 16/API 36, sin afirmar que la aplicación continúa orientada a API 35.

### Task 3: Reconciliar configuración remota y evidencia QA

**Files:**
- Modify: `docs/project-context.md:236-371`

- [x] **Step 1: Normalizar las claves activas de Acerca de la app**

Mantener `about_description_v3`, `about_created_by` y `about_contact` como contrato vigente. En la nota histórica, identificar `about_description_v2` como el fallback observado por un artefacto anterior, sin presentarlo como clave activa actual.

- [x] **Step 2: Separar la auditoría en estados verificables**

Renombrar la sección final como:

```markdown
## Estado técnico y auditoría QA
```

Organizarla con estos subtítulos:

```markdown
### Verificación estática actual (2026-08-18)
### Evidencia histórica de QA
### Validaciones pendientes antes de publicación
```

- [x] **Step 3: Clasificar la evidencia de versión 14**

Indicar que `Versión 1.3.0 (14)` fue observada en un artefacto instalado anteriormente y no contradice la configuración actual `versionCode=15`. No atribuir ese resultado a una build generada durante esta actualización.

- [x] **Step 4: Conservar los gates externos pendientes**

Mantener como pendientes la firma final, el AAB productivo, la publicación/procesamiento en Google Play y la regresión manual con Firebase producción, Auth, Firestore, Remote Config, anuncios y App Check.

### Task 4: Validar la actualización documental

**Files:**
- Verify: `docs/project-context.md`

- [x] **Step 1: Buscar contradicciones activas**

Run:

```bash
rg -n "versionCode|Versión 1\.3\.0|about_description_v2|about_description_v3|targetSdk|API 3[56]" docs/project-context.md
```

Expected: todas las referencias antiguas están marcadas como históricas y la configuración vigente es inequívoca.

- [x] **Step 2: Verificar coherencia Markdown y whitespace**

Run:

```bash
git diff --check 0f48e72..HEAD
```

Expected: salida vacía y código de salida 0.

- [x] **Step 3: Revisar el diff completo**

Run:

```bash
git diff 0f48e72..HEAD -- app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt docs/project-context.md docs/superpowers/plans/2026-08-18-project-context-refresh.md
```

Expected: cambios pequeños limitados a estado técnico, política de Google Play, configuración remota y clasificación de QA.

- [x] **Step 4: Confirmar el alcance del worktree**

Run:

```bash
git status --short
```

Expected: solo el plan y `docs/project-context.md` aparecen modificados o añadidos; no hay cambios en código, Gradle, Firebase ni recursos.

- [x] **Step 5: Crear un commit documental separado**

Run:

```bash
git add docs/superpowers/plans/2026-08-18-project-context-refresh.md docs/project-context.md
git commit -m "docs: refresh MenuDado project context"
```

Expected: commit documental con el plan y la actualización del contexto, sin archivos de implementación.
