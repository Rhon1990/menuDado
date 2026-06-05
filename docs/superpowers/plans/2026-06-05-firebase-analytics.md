# Firebase Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add anonymous Firebase Analytics and product events for MenuDado usage statistics.

**Architecture:** Add a narrow analytics interface, a Firebase adapter, and ViewModel/Application instrumentation. Tests use a recording fake to verify event emission without Android or Firebase SDK calls.

**Tech Stack:** Android Kotlin, Firebase Analytics, Jetpack ViewModel, JUnit, kotlinx-coroutines-test.

---

### Task 1: Analytics Contract And Tests

**Files:**
- Create: `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] Write failing tests that assert events for menu saving, dice rolling, AI generation, AI analysis, batch analysis, deletion, and daily AI limit.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuDadoViewModelTest` and verify failures are due to missing analytics support.
- [ ] Add the analytics interface and no-op implementation.
- [ ] Inject analytics into `MenuDadoViewModel` and emit events from existing actions without changing visible behavior.
- [ ] Re-run the focused ViewModel tests and verify they pass.

### Task 2: Firebase Adapter

**Files:**
- Create: `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Modify: `app/src/main/java/com/menudado/MainActivity.kt`

- [ ] Add Firebase dependencies with explicit versions: `firebase-ai:17.12.1` and `firebase-analytics:22.1.2`, avoiding the Analytics 23.2.0 Kotlin 2.2 metadata incompatibility with this project.
- [ ] Implement `FirebaseMenuDadoAnalytics` with safe event names and non-sensitive parameters.
- [ ] Include device manufacturer, device model, Android version, locale country, and time zone on the app-open event and matching user properties.
- [ ] Expose the Firebase analytics instance from `MenuDadoApplication`.
- [ ] Track app opens from `MainActivity`.
- [ ] Compile with `./gradlew :app:compileDebugKotlin`.

### Task 3: Project Context And QA

**Files:**
- Modify: `docs/project-context.md`

- [ ] Document Firebase Analytics under technical direction and validation focus.
- [ ] Run the focused unit tests.
- [ ] Run Kotlin compilation.
- [ ] Report validation, risks, and the fact that Firebase Console will show device/user metrics after real app sessions are received.
