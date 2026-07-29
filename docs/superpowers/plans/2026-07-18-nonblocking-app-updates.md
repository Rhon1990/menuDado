# Nonblocking App Updates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convertir la actualización opcional actual en un recordatorio persistente y flexible que mejore adopción sin impedir usar MenuDado.

**Architecture:** `MainActivity` conserva la integración con Google Play y traduce sus callbacks a un estado pequeño de presentación. Un archivo nuevo en `update/` contiene el modelo puro y otro contiene la UI reusable de modal y recordatorio; `MenuDadoScreen` solo recibe el estado y callbacks, sin depender de `AppUpdateManager`. El flujo usa exclusivamente `FLEXIBLE`; si no está disponible, abre Play Store.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Google Play In-App Updates 2.1.0, JUnit 4, Gradle Android.

---

## File map

- Create `app/src/main/java/com/menudado/update/MenuDadoAppUpdateState.kt`: estado de presentación y reglas puras derivadas de Play.
- Create `app/src/main/java/com/menudado/update/MenuDadoAppUpdateUi.kt`: modal no bloqueante y recordatorio compacto reutilizable.
- Create `app/src/test/java/com/menudado/update/MenuDadoAppUpdateStateTest.kt`: contratos de transición y visibilidad.
- Create `app/src/test/java/com/menudado/update/MenuDadoAppUpdateUiTest.kt`: contrato de recursos para cada estado visible.
- Modify `app/src/main/java/com/menudado/MainActivity.kt`: consulta Play, listener de descarga, lanzamiento flexible y callbacks.
- Modify `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`: inserta el recordatorio únicamente en Inicio.
- Modify `app/src/main/res/values/strings.xml`: textos españoles.
- Modify `app/src/main/res/values-en/strings.xml`: textos ingleses.
- Modify `app/src/main/res/values-fr/strings.xml`: textos franceses.
- Modify `app/src/test/java/com/menudado/MainActivitySystemBarsTest.kt`: preferencia exclusiva por flexible y fallback de tienda.
- Modify `docs/project-context.md`: contrato funcional y QA actualizado.

### Task 1: Modelar el estado no bloqueante con TDD

**Files:**
- Create: `app/src/main/java/com/menudado/update/MenuDadoAppUpdateState.kt`
- Create: `app/src/test/java/com/menudado/update/MenuDadoAppUpdateStateTest.kt`

- [x] **Step 1: Write failing state translation tests**

Create tests that require the missing model:

```kotlin
package com.menudado.update

import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuDadoAppUpdateStateTest {
    @Test
    fun `Play availability maps to presentation status`() {
        assertEquals(
            MenuDadoAppUpdateStatus.AVAILABLE,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                installStatus = InstallStatus.UNKNOWN
            )
        )
        assertEquals(
            MenuDadoAppUpdateStatus.DOWNLOADING,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                installStatus = InstallStatus.DOWNLOADING
            )
        )
        assertEquals(
            MenuDadoAppUpdateStatus.DOWNLOADED,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                installStatus = InstallStatus.DOWNLOADED
            )
        )
        assertEquals(
            MenuDadoAppUpdateStatus.NOT_AVAILABLE,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.UPDATE_NOT_AVAILABLE,
                installStatus = InstallStatus.UNKNOWN
            )
        )
    }

    @Test
    fun `developer triggered update remains visible as downloading`() {
        assertEquals(
            MenuDadoAppUpdateStatus.DOWNLOADING,
            menuDadoAppUpdateStatus(
                updateAvailability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                installStatus = InstallStatus.UNKNOWN
            )
        )
    }

    @Test
    fun `dismissed prompt keeps persistent reminder`() {
        val state = MenuDadoAppUpdatePresentationState(
            status = MenuDadoAppUpdateStatus.AVAILABLE,
            isAvailablePromptDismissed = true
        )

        assertFalse(state.shouldShowAvailablePrompt)
        assertTrue(state.shouldShowReminder)
    }

    @Test
    fun `downloaded transition re-enables install prompt`() {
        val state = MenuDadoAppUpdatePresentationState(
            status = MenuDadoAppUpdateStatus.AVAILABLE,
            isAvailablePromptDismissed = true,
            isDownloadedPromptDismissed = true
        ).withStatus(MenuDadoAppUpdateStatus.DOWNLOADED)

        assertTrue(state.shouldShowDownloadedPrompt)
        assertTrue(state.shouldShowReminder)
    }
}
```

- [x] **Step 2: Run RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.update.MenuDadoAppUpdateStateTest
```

Expected: compilation fails because the update presentation types and functions do not exist.

- [x] **Step 3: Implement the minimal state model**

Create:

```kotlin
package com.menudado.update

import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

internal enum class MenuDadoAppUpdateStatus {
    NOT_AVAILABLE,
    AVAILABLE,
    DOWNLOADING,
    DOWNLOADED
}

internal data class MenuDadoAppUpdatePresentationState(
    val status: MenuDadoAppUpdateStatus = MenuDadoAppUpdateStatus.NOT_AVAILABLE,
    val isAvailablePromptDismissed: Boolean = false,
    val isDownloadedPromptDismissed: Boolean = false
) {
    val shouldShowAvailablePrompt: Boolean
        get() = status == MenuDadoAppUpdateStatus.AVAILABLE && !isAvailablePromptDismissed

    val shouldShowDownloadedPrompt: Boolean
        get() = status == MenuDadoAppUpdateStatus.DOWNLOADED && !isDownloadedPromptDismissed

    val shouldShowReminder: Boolean
        get() = status != MenuDadoAppUpdateStatus.NOT_AVAILABLE

    fun withStatus(nextStatus: MenuDadoAppUpdateStatus): MenuDadoAppUpdatePresentationState {
        if (nextStatus == status) return this
        return copy(
            status = nextStatus,
            isAvailablePromptDismissed = if (nextStatus == MenuDadoAppUpdateStatus.NOT_AVAILABLE) {
                false
            } else {
                isAvailablePromptDismissed
            },
            isDownloadedPromptDismissed = if (nextStatus == MenuDadoAppUpdateStatus.DOWNLOADED) {
                false
            } else if (nextStatus == MenuDadoAppUpdateStatus.NOT_AVAILABLE) {
                false
            } else {
                isDownloadedPromptDismissed
            }
        )
    }
}

internal fun menuDadoAppUpdateStatus(
    updateAvailability: Int,
    installStatus: Int
): MenuDadoAppUpdateStatus {
    return when {
        installStatus == InstallStatus.DOWNLOADED -> MenuDadoAppUpdateStatus.DOWNLOADED
        installStatus == InstallStatus.PENDING ||
            installStatus == InstallStatus.DOWNLOADING ||
            installStatus == InstallStatus.INSTALLING -> MenuDadoAppUpdateStatus.DOWNLOADING
        updateAvailability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
            MenuDadoAppUpdateStatus.DOWNLOADING
        updateAvailability == UpdateAvailability.UPDATE_AVAILABLE ->
            MenuDadoAppUpdateStatus.AVAILABLE
        else -> MenuDadoAppUpdateStatus.NOT_AVAILABLE
    }
}
```

- [x] **Step 4: Run GREEN**

Run the Task 1 command. Expected: `BUILD SUCCESSFUL` and 4 tests pass.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/menudado/update/MenuDadoAppUpdateState.kt app/src/test/java/com/menudado/update/MenuDadoAppUpdateStateTest.kt
git commit -m "feat: model nonblocking app update state"
```

### Task 2: Integrar descarga flexible y progreso de Play

**Files:**
- Modify: `app/src/main/java/com/menudado/MainActivity.kt`
- Modify: `app/src/test/java/com/menudado/MainActivitySystemBarsTest.kt`

- [x] **Step 1: Write the failing flexible-only contract test**

Add:

```kotlin
@Test
fun `actualizacion usa flexible sin convertir immediate en bloqueo`() {
    assertEquals(AppUpdateType.FLEXIBLE, preferredMenuDadoAppUpdateType(isFlexibleAllowed = true))
    assertEquals(null, preferredMenuDadoAppUpdateType(isFlexibleAllowed = false))
}
```

- [x] **Step 2: Run RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.MainActivitySystemBarsTest
```

Expected: compilation fails because `preferredMenuDadoAppUpdateType` does not exist.

- [x] **Step 3: Add the flexible-only selector**

Replace the current flexible/immediate fallback with:

```kotlin
internal fun preferredMenuDadoAppUpdateType(isFlexibleAllowed: Boolean): Int? {
    return AppUpdateType.FLEXIBLE.takeIf { isFlexibleAllowed }
}

private fun appUpdateOptionsFor(appUpdateInfo: AppUpdateInfo): AppUpdateOptions? {
    val type = preferredMenuDadoAppUpdateType(
        isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
    ) ?: return null
    return AppUpdateOptions.newBuilder(type).build()
}
```

- [x] **Step 4: Replace booleans with presentation state**

In `setContent`, keep `pendingAppUpdateInfo` and replace `isAppUpdateDialogDismissed` / `isAppUpdateDownloaded` with:

```kotlin
var appUpdatePresentation by remember {
    mutableStateOf(MenuDadoAppUpdatePresentationState())
}
```

Update `refreshPlayStoreUpdateState` so success passes both the `AppUpdateInfo` and `menuDadoAppUpdateStatus(...)`. A failed Play query must leave the current presentation unchanged; a confirmed `NOT_AVAILABLE` clears the pending info and resets presentation.

- [x] **Step 5: Register Play install-state listener**

Create one listener with `remember`:

```kotlin
val appUpdateInstallStateListener = remember {
    InstallStateUpdatedListener { installState ->
        val nextStatus = menuDadoAppUpdateStatus(
            updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
            installStatus = installState.installStatus()
        )
        appUpdatePresentation = appUpdatePresentation.withStatus(nextStatus)
    }
}
```

Register it inside the existing lifecycle `DisposableEffect` and unregister it in `onDispose`. Refresh on startup, `ON_RESUME`, and launcher completion.

- [x] **Step 6: Keep the app usable during update**

In `startMenuDadoUpdate()`:

- track `update`;
- dismiss only the available prompt;
- retain `pendingAppUpdateInfo` and reminder state;
- start `FLEXIBLE` when allowed;
- call `openMenuDadoPlayStore()` when info is missing, flexible is unavailable, or the launcher fails.

For install, track `install` and call `appUpdateManager.completeUpdate()`. Dismissing either prompt tracks `later` and only flips its session flag; it must never hide the reminder.

- [x] **Step 7: Run GREEN**

Run the Task 2 command. Expected: `BUILD SUCCESSFUL` and the existing MainActivity tests plus the new flexible-only contract pass.

- [x] **Step 8: Commit**

```bash
git add app/src/main/java/com/menudado/MainActivity.kt app/src/test/java/com/menudado/MainActivitySystemBarsTest.kt
git commit -m "feat: run app updates in flexible mode"
```

### Task 3: Añadir modal y recordatorio persistente en Inicio

**Files:**
- Create: `app/src/main/java/com/menudado/update/MenuDadoAppUpdateUi.kt`
- Create: `app/src/test/java/com/menudado/update/MenuDadoAppUpdateUiTest.kt`
- Modify: `app/src/main/java/com/menudado/MainActivity.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [x] **Step 1: Write failing UI content contract tests**

Create tests that expect a resource contract:

```kotlin
package com.menudado.update

import com.menudado.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuDadoAppUpdateUiTest {
    @Test
    fun `available reminder offers update`() {
        assertEquals(
            MenuDadoAppUpdateContent(
                titleRes = R.string.app_update_title,
                bodyRes = R.string.app_update_reminder_body,
                actionRes = R.string.app_update_action
            ),
            menuDadoAppUpdateContent(MenuDadoAppUpdateStatus.AVAILABLE)
        )
    }

    @Test
    fun `downloading reminder has no repeated action`() {
        assertNull(menuDadoAppUpdateContent(MenuDadoAppUpdateStatus.DOWNLOADING)?.actionRes)
    }

    @Test
    fun `downloaded reminder offers install`() {
        assertEquals(
            R.string.app_update_install_action,
            menuDadoAppUpdateContent(MenuDadoAppUpdateStatus.DOWNLOADED)?.actionRes
        )
    }

    @Test
    fun `no update has no reminder content`() {
        assertNull(menuDadoAppUpdateContent(MenuDadoAppUpdateStatus.NOT_AVAILABLE))
    }
}
```

- [x] **Step 2: Run RED**

```bash
./gradlew :app:testDebugUnitTest --tests com.menudado.update.MenuDadoAppUpdateUiTest
```

Expected: compilation fails because the content mapper and new resource names do not exist.

- [x] **Step 3: Add localized resources**

Add in all three locales:

```xml
<string name="app_update_later">Ahora no</string>
<string name="app_update_reminder_body">Puedes actualizar en segundo plano y seguir usando MenuDado.</string>
<string name="app_update_downloading_title">Actualizando MenuDado</string>
<string name="app_update_downloading_body">La nueva versión se está descargando en segundo plano.</string>
<string name="app_update_downloaded_reminder_body">La nueva versión está lista. Instálala cuando quieras reiniciar MenuDado.</string>
```

Use equivalent natural English and French translations. Keep `app_update_action` and `app_update_install_action` as the action labels.

- [x] **Step 4: Implement reusable update UI**

Create `MenuDadoAppUpdateContent` and `menuDadoAppUpdateContent(status)`. Add:

- `MenuDadoAppUpdateAvailableDialog(onUpdate, onDismiss)`;
- `MenuDadoAppUpdateDownloadedDialog(onInstall, onDismiss)`;
- `MenuDadoAppUpdateReminder(status, onUpdate, onInstall)`.

The reminder uses a Material 3 `Card`, `MenuDadoColors.SelectionGreen`, `MenuDadoColors.Ink`, and a compact green action. For `DOWNLOADING`, show `CircularProgressIndicator` and no button.

- [x] **Step 5: Insert the reminder without competing with the main CTA**

Extend `MenuDadoScreen` with defaults:

```kotlin
appUpdateStatus: MenuDadoAppUpdateStatus = MenuDadoAppUpdateStatus.NOT_AVAILABLE,
onAppUpdateClick: () -> Unit = {},
onAppUpdateInstallClick: () -> Unit = {},
```

Inside the root Home branch, after the header and before `TodayMenuSection`, add one `item` only when `appUpdateStatus != NOT_AVAILABLE`. Do not render it in Profile, My Zone, About, favorites detail, or audience detail.

- [x] **Step 6: Wire dialogs and reminder from MainActivity**

Replace the private update dialogs in `MainActivity` with the new update UI. Pass `appUpdatePresentation.status` and callbacks to `MenuDadoScreen`. Available and downloaded dialogs depend on `shouldShowAvailablePrompt` and `shouldShowDownloadedPrompt`; dismissing them never changes `status`.

- [x] **Step 7: Run GREEN**

Run the Task 3 command. Expected: `BUILD SUCCESSFUL` and 4 UI contract tests pass.

- [x] **Step 8: Commit**

```bash
git add app/src/main/java/com/menudado/update/MenuDadoAppUpdateUi.kt app/src/test/java/com/menudado/update/MenuDadoAppUpdateUiTest.kt app/src/main/java/com/menudado/MainActivity.kt app/src/main/java/com/menudado/ui/MenuDadoScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/main/res/values-fr/strings.xml
git commit -m "feat: keep update reminder visible on home"
```

### Task 4: Documentar y verificar la rama actual

**Files:**
- Modify: `docs/project-context.md`
- Modify: `docs/superpowers/plans/2026-07-18-nonblocking-app-updates.md`

- [x] **Step 1: Update project context**

Replace the current publication update paragraph with the new contract: flexible-only, initial prompt once per session, persistent Home reminder, downloaded install reminder, Play Store fallback, and no app blocking.

- [x] **Step 2: Check regression boundaries**

Confirm with `git diff` that the change does not touch Room, Firestore, AI prompts, authentication, versionCode, versionName, or navigation state.

- [x] **Step 3: Run complete verification from scratch**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleReleaseDebuggable --rerun-tasks
```

Expected: `BUILD SUCCESSFUL` with zero test failures and both APKs generated.

- [x] **Step 4: Verify Android resources and diff**

```bash
./gradlew :app:lintDebug
git diff --check
git status --short --branch
```

Expected: lint/build succeeds, no whitespace errors, and only the completed plan/context are pending.

Actual: tests and both APK builds succeeded with 90 tasks executed. `git diff --check` passed. `lintDebug` executed and reported only two pre-existing errors outside this feature (`MenuDadoApplication.kt` missing-permission inference and `styles.xml` API-level placement); the new update files and changed UI contain no lint errors.

- [x] **Step 5: Commit documentation and completed checklist**

```bash
git add docs/project-context.md docs/superpowers/plans/2026-07-18-nonblocking-app-updates.md
git commit -m "docs: record nonblocking update flow"
```

- [x] **Step 6: Validate current-branch artifact**

Install `app/build/outputs/apk/releaseDebuggable/app-releaseDebuggable.apk` on the selected emulator. Confirm normal MenuDado use remains available when Play reports no update. Google Play update availability cannot be forced reliably for an ADB-installed package; use the automated state tests as evidence for available/downloading/downloaded states and document this limitation rather than simulating production data.

Actual: `releaseDebuggable` installed successfully on `S24plus` (`emulator-5556`), opened `com.menudado/.MainActivity`, dismissed onboarding, and rendered usable Home content without a false update prompt or reminder.
