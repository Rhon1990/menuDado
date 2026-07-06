# Home Dice IA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Home dice generate an IA menu idea by default while preserving saved-menu random selection in manual mode.

**Architecture:** Keep the change inside the existing MVVM surface. Add explicit Home mode and pending generated idea state to `MenuDadoUiState`, reuse `generateMenuIdea()` and `saveMenu()`, and adapt Compose to render one contextual `Que comer hoy` block.

**Tech Stack:** Kotlin, Jetpack Compose, Android resources, coroutine ViewModel tests.

---

### Task 1: ViewModel State And Tests

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- Test: `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`

- [ ] Add failing tests for default IA mode, pending generated result, discard, and save.
- [ ] Add `HomeMenuMode`, `homeMenuMode`, and `showGeneratedMenuDetail`.
- [ ] Update `generateMenuIdea()` success to set `showGeneratedMenuDetail = true`.
- [ ] Add `setHomeMenuMode`, `discardGeneratedMenuIdea`, and `saveGeneratedMenuIdea`.
- [ ] Verify targeted tests pass.

### Task 2: Compose Home Restructure

**Files:**
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`

- [ ] Replace separate `DiceSection` then `MenuForm` ordering with one `TodayMenuSection`.
- [ ] In IA mode, show selectors, optional base ingredients, dice CTA, and generated idea modal.
- [ ] In manual mode, show selectors, manual fields, save button, and saved-menu dice CTA.
- [ ] Keep the existing menu detail modal for saved menus.
- [ ] Add strings for contextual labels and modal actions.

### Task 3: Docs And Verification

**Files:**
- Modify: `docs/project-context.md`

- [ ] Update project context for the new Home behavior.
- [ ] Run unit tests.
- [ ] Run compile/build check if feasible.
