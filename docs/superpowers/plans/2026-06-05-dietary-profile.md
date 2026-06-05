# Dietary Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local dietary profile from a hamburger drawer and use it in AI menu generation.

**Architecture:** Add domain/store types, inject the store into `MenuDadoViewModel`, extend AI prompt creation, and add Compose UI for a drawer and profile editor.

**Tech Stack:** Kotlin, Jetpack Compose Material3, SharedPreferences, JUnit.

---

### Task 1: Domain And Prompt

- [ ] Add `DietaryProfile` and `DietaryAllergen`.
- [ ] Extend `HealthAnalyzer.generateMenu` and repository calls with profile.
- [ ] Extend `MenuGenerationPrompt.build` to include vegan/allergy constraints.
- [ ] Add prompt tests for vegan and allergen restrictions.

### Task 2: Persistence And ViewModel

- [ ] Add `DietaryProfileStore` with SharedPreferences and no-op defaults.
- [ ] Inject store into `MenuDadoViewModel`.
- [ ] Add profile state and update methods.
- [ ] Add ViewModel tests for loading and saving profile.

### Task 3: UI

- [ ] Add hamburger icon resource.
- [ ] Wrap main screen in a `ModalNavigationDrawer`.
- [ ] Add `Perfil alimentario` drawer item.
- [ ] Add a profile editor section with toggles, allergen checkboxes, and optional avoid text.
- [ ] Keep the main menu workflow unchanged.

### Task 4: Validation

- [ ] Run focused prompt/ViewModel tests.
- [ ] Run `./gradlew :app:testDebugUnitTest`.
