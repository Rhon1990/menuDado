# MenuDado Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the MenuDado native Android Kotlin app with local menus, optional filtered dice selection, and Firebase AI Logic health analysis.

**Architecture:** Android app module with Compose UI, Room local persistence, MVVM state, and tested domain logic. AI is behind a repository interface so Firebase can be configured without leaking provider details into UI.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose, Room, coroutines, Flow, Firebase AI Logic, JUnit.

---

### Task 1: Project Scaffold

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`

- [x] Create Android Studio project files with Kotlin, Compose, Room, and Firebase AI Logic dependencies.

### Task 2: Tested Domain

**Files:**
- Test: `app/src/test/java/com/menudado/domain/DiceSelectorTest.kt`
- Test: `app/src/test/java/com/menudado/domain/HealthAnalysisParserTest.kt`
- Create: `app/src/main/java/com/menudado/domain/MenuModels.kt`
- Create: `app/src/main/java/com/menudado/domain/DiceSelector.kt`
- Create: `app/src/main/java/com/menudado/domain/HealthAnalysisParser.kt`

- [x] Write failing tests for optional filter, empty candidates, deterministic random selection, valid IA JSON, and malformed IA text.
- [x] Implement minimal domain code.

### Task 3: Data And AI

**Files:**
- Create: `app/src/main/java/com/menudado/data/MenuEntity.kt`
- Create: `app/src/main/java/com/menudado/data/MenuDao.kt`
- Create: `app/src/main/java/com/menudado/data/MenuDadoDatabase.kt`
- Create: `app/src/main/java/com/menudado/data/MenuRepository.kt`
- Create: `app/src/main/java/com/menudado/ai/HealthAnalyzer.kt`
- Create: `app/src/main/java/com/menudado/ai/FirebaseHealthAnalyzer.kt`

- [x] Add Room persistence and Firebase analyzer abstraction.

### Task 4: Compose App

**Files:**
- Create: `app/src/main/java/com/menudado/MenuDadoApplication.kt`
- Create: `app/src/main/java/com/menudado/MainActivity.kt`
- Create: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Create: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Create: `app/src/main/java/com/menudado/ui/theme/MenuDadoTheme.kt`

- [x] Build the one-screen app with logo-aware colors, form, filters, animated dice button, result dialog, and menu list.

### Task 5: Assets And Verification

**Files:**
- Create: `app/src/main/res/drawable/menu_dado_logo.png`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/values/strings.xml`

- [x] Copy logo into app resources.
- [x] Run unit tests and compile.
