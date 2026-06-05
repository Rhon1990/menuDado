# Collapsible Menu Cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make saved menu cards start collapsed and expand one card at a time when tapped.

**Architecture:** Keep expansion state in `MenuDadoScreen` with `rememberSaveable`, keyed by menu id. Reuse the existing `MenuCard` composable and pass it `isExpanded` plus a click callback, without changing Room, repository, or ViewModel state.

**Tech Stack:** Android native, Kotlin, Jetpack Compose, JUnit unit tests.

---

### Task 1: Card Expansion State

**Files:**
- Modify: `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- Modify: `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- Modify: `docs/project-context.md`

- [ ] **Step 1: Write the failing tests**

Add tests for `nextExpandedMenuIdAfterMenuClick` and the collapsed analyzed chip behavior.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: FAIL because `nextExpandedMenuIdAfterMenuClick` does not exist and `menuHeaderHealthStatus` does not accept expansion state.

- [ ] **Step 3: Write minimal implementation**

Add `expandedMenuId` in `MenuDadoScreen`, pass `isExpanded` to `MenuCard`, wrap details/actions in `AnimatedVisibility`, and add the pure helper.

- [ ] **Step 4: Run focused tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.menudado.ui.MenuCardUiStateTest`

Expected: PASS.

- [ ] **Step 5: Run compile check**

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS.
