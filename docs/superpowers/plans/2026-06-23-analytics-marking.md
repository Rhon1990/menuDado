# Analytics Marking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete safe Firebase Analytics marking across MenuDado.

**Architecture:** Extend the existing `MenuDadoAnalytics` contract, keep Firebase mapping centralized in `FirebaseMenuDadoAnalytics`, emit events from ViewModel/application action boundaries, and validate with unit tests.

**Tech Stack:** Kotlin, Firebase Analytics, Jetpack ViewModel tests, JUnit.

---

## Tasks

- [ ] Add failing tests in `MenuDadoViewModelTest` for profile updates, edit start/save, photo update, audience filter, and blocked missing audience.
- [ ] Extend `MenuDadoAnalytics` and `NoOpMenuDadoAnalytics` with the approved events and safe parameters.
- [ ] Implement event logging in `FirebaseMenuDadoAnalytics` with closed enum/string constants.
- [ ] Instrument `MenuDadoViewModel` and `MenuDadoApplication` with the new events.
- [ ] Update the recording analytics fake and expected tests.
- [ ] Update `docs/project-context.md` with the expanded analytics schema.
- [ ] Run `./gradlew :app:testDebugUnitTest :app:compileDebugKotlin :app:processDebugManifest`.
