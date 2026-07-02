# Email Auth Guest Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add email/password login and registration while preserving the current guest flow and guest data when registering.

**Architecture:** Introduce a small auth boundary around Firebase Auth, expose auth UI state from `MainActivity`, and reuse the existing backend sync pipeline after auth changes. The app opens Home by default as guest; login and registration are reachable from `Mi zona`. Guest registration uses Firebase Auth credential linking so Firestore keeps using the same `users/{uid}` path.

**Tech Stack:** Kotlin, Jetpack Compose, Firebase Auth, Firestore, Room, coroutines, JUnit.

---

### Task 1: Auth Domain Boundary

**Files:**
- Create: `app/src/main/java/com/menudado/auth/MenuDadoAuth.kt`
- Test: `app/src/test/java/com/menudado/auth/MenuDadoAuthTest.kt`

- [ ] Write tests for access screen visibility, guest state, persisted email session and registration strategy.
- [ ] Implement pure auth state helpers and Firebase-backed auth service methods.
- [ ] Verify with `./gradlew :app:testDebugUnitTest --tests 'com.menudado.auth.MenuDadoAuthTest'`.

### Task 2: Backend Sync Trigger

**Files:**
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Test: `app/src/test/java/com/menudado/auth/MenuDadoAuthTest.kt`

- [ ] Extract app-start backend sync into a callable `syncBackendNow(source: String)`.
- [ ] Call it on app start and after login/register/guest selection.
- [ ] Mark local menus pending before signed-in sync when needed.

### Task 3: Compose Access Screen

**Files:**
- Create: `app/src/main/java/com/menudado/auth/MenuDadoAuthScreen.kt`
- Modify: `app/src/main/java/com/menudado/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] Add email/password fields and actions for login, register and guest.
- [ ] Add `Mi zona` to bottom navigation.
- [ ] Show guest benefits and account CTAs inside `Mi zona`.
- [ ] Show create-account/sign-in dialogs from `Mi zona` without blocking Home.

### Task 4: Validation

**Files:**
- Modify: `docs/project-context.md`

- [ ] Update project context to document email auth and guest behavior.
- [ ] Run targeted unit tests.
- [ ] Compile `debug`, `release`, and `releaseDebuggable`.
- [ ] Report Firebase console requirement: Email/Password provider must be enabled in debug and production projects.
