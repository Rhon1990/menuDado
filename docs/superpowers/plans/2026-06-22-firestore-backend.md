# Firestore Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a backend for MenuDado where user-saved app data is stored in Firebase Firestore without showing a login screen, while keeping the current local-first/offline behavior.

**Architecture:** Use Firebase Anonymous Auth as the invisible identity layer, Room as the local source of truth/cache, and Firestore as the remote per-user backend under `users/{uid}`. Remote writes are best-effort but explicit; failed writes are kept pending locally for retry.

**Tech Stack:** Kotlin, Jetpack Compose, MVVM, Room, Coroutines/Flow, Firebase Auth anonymous, Firebase Firestore, existing Firebase Analytics/AI setup.

---

## Data Model

- `users/{uid}/metadata/current`
  - `createdAt`, `lastSeenAt`
  - `country`: from `Locale.getDefault().country`, not GPS
  - `timeZone`: from `TimeZone.getDefault().id`
  - `deviceManufacturer`, `deviceModel`, `androidVersion`
  - `appVersionName`: `BuildConfig.VERSION_NAME`
  - `appVersionCode`: `BuildConfig.VERSION_CODE`
  - `authMode`: `anonymous`
- `users/{uid}/menus/{menuId}`
  - `name`, `mealType`, `audience`, `description`, `notes`
  - `healthStatus`, `healthReason`, `healthSuggestion`, `calories`
  - `imageUri`: local URI string only for now; no image upload in this phase
  - `lastPickedDate`, `createdAt`, `updatedAt`, `deletedAt`
- `users/{uid}/dietaryProfiles/{audience}`
  - `isEnabled`, `ageRange`, `isPregnant`, `isVegan`, `hasAllergies`, `allergens`, `otherAvoidances`, `updatedAt`
- `users/{uid}/aiUsage/{dateKey}`
  - `dateKey`, `usedCount`, `updatedAt`
- `users/{uid}/onboarding/current`
  - `completed`, `contentVersion`, `updatedAt`

## Security Rules Contract

- Require Firebase Auth.
- Allow each user to read/write only `users/{request.auth.uid}/**`.
- Do not expose global menu documents.
- Country/device/app version are metadata for analytics/product/ad-readiness, not precise location.

## Implementation Steps

- [ ] Add Firebase backend dependencies in `app/build.gradle.kts`.
  - Add Firebase Auth and Firestore Android libraries compatible with the current Firebase setup.
  - Keep existing Firebase AI and Analytics dependencies intact.
  - Run `./gradlew :app:dependencies --configuration debugRuntimeClasspath` if dependency resolution is unclear.

- [ ] Add invisible backend identity.
  - Create `app/src/main/java/com/menudado/backend/MenuDadoBackendSession.kt`.
  - Define an interface with `suspend fun userId(): String?`.
  - Implement `FirebaseMenuDadoBackendSession` using anonymous Firebase Auth.
  - Add a `NoOpMenuDadoBackendSession` for tests or Firebase-disabled contexts.
  - Expected behavior: first backend write signs in anonymously; no login UI is shown.

- [ ] Add app/device metadata provider.
  - Reuse `AndroidDeviceInfoProvider.current()`.
  - Create `BackendAppMetadata` with country, timezone, device, Android version, `BuildConfig.VERSION_NAME`, `BuildConfig.VERSION_CODE`.
  - Avoid GPS, contacts, ad ID, or extra permissions.

- [ ] Add Firestore remote data source.
  - Create `app/src/main/java/com/menudado/backend/MenuDadoRemoteDataSource.kt`.
  - Define methods:
    - `upsertMetadata(metadata: BackendAppMetadata)`
    - `upsertMenu(menu: FoodMenu)`
    - `deleteMenu(menu: FoodMenu)`
    - `upsertDietaryProfile(audience: MenuAudience, profile: DietaryProfile)`
    - `upsertAiUsage(state: AiDailyUsageState)`
    - `upsertOnboardingCompleted(contentVersion: Int)`
  - Implement `FirebaseMenuDadoRemoteDataSource` using `FirebaseFirestore`.
  - Use per-user paths through the anonymous UID.
  - Convert enums to `name` strings and timestamps to millisecond values or Firestore server timestamps where useful.

- [ ] Track pending remote sync for menus.
  - Add lightweight sync fields to `MenuEntity` via a Room migration:
    - `remoteSyncState TEXT NOT NULL DEFAULT 'SYNCED'`
    - `updatedAt INTEGER NOT NULL DEFAULT 0`
    - `deletedAt INTEGER`
  - Update `MenuDadoDatabase` version and migrations.
  - On local save, mark `PENDING_UPSERT`.
  - On local delete, either delete immediately after successful remote delete or use a tombstone if remote delete fails.
  - Keep UI behavior unchanged.

- [ ] Wire repository sync.
  - Update `MenuRepository` constructor to accept `MenuDadoRemoteDataSource`.
  - On insert, use returned Room ID to build the saved `FoodMenu` before remote upsert.
  - On update, persist locally then remote upsert.
  - On delete, remove locally and attempt remote delete; if tombstones are added, hide them from `observeMenus()`.
  - Add a small retry method for pending menu sync on app start.

- [ ] Sync profile and app stores.
  - Wrap or extend `SharedPreferencesDietaryProfileStore` so `saveProfile()` writes locally first and triggers `upsertDietaryProfile()`.
  - Wrap or extend `SharedPreferencesAiDailyUsageStore` so `saveUsageState()` writes locally first and triggers `upsertAiUsage()`.
  - Wrap or extend onboarding store so completion is mirrored to Firestore.
  - Keep reads local to preserve startup speed and offline behavior.

- [ ] Initialize backend metadata on app start.
  - In `MenuDadoApplication`, create the backend session and remote data source.
  - After app launch, call metadata sync and pending menu sync from an application coroutine scope.
  - Store `lastSeenAt` on each app start.

- [ ] Add Firestore rules and documentation.
  - Add `firestore.rules` at repo root if missing.
  - Add a concise schema note to `docs/project-context.md`.
  - Update the project context from “local only” to “local-first with Firestore backend sync”.
  - Document that country comes from device locale and app version comes from Android build config.

## Tests

- [ ] Unit-test menu insert/update/delete sync with a fake remote data source.
  - New tests should verify local save still works if remote write fails.
  - Verify new menus are synced with the generated Room ID.
  - Verify delete calls remote delete with the same menu ID.

- [ ] Unit-test store wrappers with fake remotes.
  - Dietary profile save writes local data and calls remote with the selected audience.
  - AI usage save writes local data and calls remote with the current date key.

- [ ] Unit-test metadata mapping.
  - Verify country, timezone, device fields, `appVersionName`, and `appVersionCode` are present.
  - Verify no GPS/location permission fields are introduced.

## Validation Commands

- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:compileDebugKotlin`
- [ ] If Firebase dependency resolution changes significantly: `./gradlew :app:dependencies --configuration debugRuntimeClasspath`

## Manual QA

- [ ] Fresh install: app opens without login UI and creates an anonymous backend identity only when needed.
- [ ] Create a menu online: menu appears locally and in `users/{uid}/menus`.
- [ ] Edit a menu online: Firestore document updates and local UI remains unchanged.
- [ ] Delete a menu: confirmation still appears, local card disappears, Firestore delete/tombstone is reflected.
- [ ] Change dietary profile: disabled audiences disappear from home/dice and the profile document updates.
- [ ] Generate an AI idea: usage counter still works and `aiUsage/{dateKey}` updates.
- [ ] Launch app again: `metadata/current.lastSeenAt`, country, device, and app version are updated.
- [ ] Offline save: app remains usable; sync retries after returning online or next launch.

## Risks And Constraints

- Anonymous Auth without account linking means uninstalling/reinstalling can create a new backend user.
- Local image URIs cannot be used by other devices; image upload/storage is out of scope for this phase.
- Dietary profile can contain sensitive health/allergy information, so Firestore rules must stay per-user and ad segmentation must not use health data without explicit consent.
- Country is approximate from locale, not physical location.
