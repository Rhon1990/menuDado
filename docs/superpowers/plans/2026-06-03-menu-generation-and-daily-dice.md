# Menu Generation And Daily Dice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add IA menu generation with estimated calories and prevent the dice from repeating menus within the same day.

**Architecture:** Extend the existing domain model and Room entity with optional calories and last-picked date. Reuse Firebase AI through the existing analyzer dependency, adding a generated-menu parser and ViewModel action that fills the form before saving.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Firebase AI Logic, JUnit.

---

### Task 1: Domain Tests

**Files:**
- Modify: `app/src/test/java/com/menudado/domain/DiceSelectorTest.kt`
- Create: `app/src/test/java/com/menudado/domain/GeneratedMenuParserTest.kt`

- [ ] Add a failing dice test that excludes menus whose `lastPickedDate` equals today's date.
- [ ] Add a failing parser test that reads generated menu JSON with name, description, notes, and calories.

### Task 2: Domain And Persistence

**Files:**
- Modify: `app/src/main/java/com/menudado/domain/MenuModels.kt`
- Create: `app/src/main/java/com/menudado/domain/GeneratedMenuParser.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuEntity.kt`
- Modify: `app/src/main/java/com/menudado/data/MenuDadoDatabase.kt`
- Modify: `app/src/main/java/com/menudado/MenuDadoApplication.kt`

- [ ] Add optional `calories`, `lastPickedDate`, and `createdAt` to `FoodMenu`.
- [ ] Add Room migration 1 to 2 for `calories` and `lastPickedDate`.
- [ ] Preserve `createdAt` on updates.

### Task 3: IA Generation

**Files:**
- Modify: `app/src/main/java/com/menudado/ai/HealthAnalyzer.kt`
- Modify: `app/src/main/java/com/menudado/ai/FirebaseHealthAnalyzer.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Modify: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] Add `generateMenu(mealType)` to the IA interface.
- [ ] Prompt Gemini for common-market healthy menu JSON with calories.
- [ ] Add ViewModel action that fills the form from generated content.

### Task 4: UI

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`

- [ ] Add `Generar idea con IA` inside the add-menu form.
- [ ] Show estimated calories in form after generation, menu cards, and result modal.

### Task 5: Verification

**Files:**
- Modify: `docs/project-context.md`

- [ ] Update project context.
- [ ] Run `./gradlew :app:testDebugUnitTest`.
- [ ] Run `./gradlew :app:compileDebugKotlin`.
