# MenuDado UX Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve the main MenuDado experience with a cleaner palette, a visible dice animation, and a simpler single-save form flow.

**Architecture:** Keep the existing single Compose screen and MVVM flow. Limit behavior changes to replacing the create form's two save buttons with one local save; IA analysis remains available from each saved menu card.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android Gradle, existing ViewModel and repository.

---

### Task 1: Single Save Behavior

**Files:**
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] Add a ViewModel unit test proving form save stores a menu without invoking IA analysis.
- [ ] Remove the secondary IA form action from the screen and keep a single `Guardar menú` CTA.
- [ ] Leave existing `Analizar IA` action on saved menu cards.

### Task 2: Visual Refresh

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/theme/MenuDadoTheme.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `docs/project-context.md`

- [ ] Update the palette to a warmer, cleaner food-app look with better contrast and fewer muddy beige surfaces.
- [ ] Improve layout hierarchy: compact header, calmer sections, stronger spacing, readable cards.
- [ ] Update project context palette to match the actual code.

### Task 3: Dice Animation

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] Replace the static `D` circle with a dice face made from Compose UI.
- [ ] During rolling, animate rotation, scale, and vertical bounce so the interaction is clearly visible.
- [ ] Keep the existing ViewModel result timing and double-tap guard.

### Task 4: Verification

**Files:**
- Existing project files only.

- [ ] Run `./gradlew :app:testDebugUnitTest`.
- [ ] Run `./gradlew :app:compileDebugKotlin`.
- [ ] Run `rg` for old naming and removed form text.
