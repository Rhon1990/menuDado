# Saved Menu Cuisine Metadata Design

**Date:** 2026-07-18

## Goal

Show the known world-cuisine inspiration consistently across saved-menu surfaces while keeping the menu name as the primary content and without increasing Gemini calls, prompt tokens, or response fields.

## Product decision

The app displays cuisine only when it has a trustworthy value captured during AI generation. It does not guess cuisine from a menu name or description and does not assign a generic cuisine to legacy or manually created menus.

This favors user trust over superficial completeness. Existing menus without stored cuisine remain visually unchanged; newly generated and saved menus retain their exact local rotation value.

## Visual hierarchy

Cuisine is secondary metadata, never a competing title or large badge on compact cards.

- **Recent menu:** menu name first, then a single green `Cocina …` line, then `Meal · Audience`.
- **Audience carousel and View More grids:** fixed two-line menu name first, then a compact green `Cocina …` line, then meal type, followed by health/calorie metadata.
- **Favorites carousel:** menu name keeps up to three lines; the cuisine line appears below it and before `Meal · Audience` and the favorite action.
- **Favorites View More:** reuses the standard grid-card presentation.
- **Saved-menu detail:** cuisine is the first metadata pill, followed by health and calories in the existing responsive `FlowRow`, matching the generated-menu detail.

Cuisine text uses the existing localized format and labels in Spanish, English, and French. Compact cards use bold `labelSmall` text in deep green with one-line ellipsis. The detail keeps the existing soft-green badge treatment.

## Data model and persistence

Add nullable `CuisineInspiration` metadata to `FoodMenu` and a nullable string column to `MenuEntity`.

- AI generation continues selecting cuisine locally through `CuisineRotation`.
- Saving an untouched generated draft copies `generatedCuisineInspiration` into `FoodMenu.cuisineInspiration`.
- Manual menus use `null`.
- Editing, favoriting, adding photos, analyzing, and other `copy` operations preserve the existing value.
- Deleting and restoring visibility through audience activation do not alter cuisine metadata.

Room moves from schema version 9 to 10 with a non-destructive migration adding nullable `cuisineInspiration TEXT`. Existing rows receive `NULL` and are not deleted or rewritten.

Firestore adds the optional `cuisineInspiration` enum name to menu documents. Reading uses safe enum parsing and falls back to `null` for missing or unknown values, preserving compatibility with existing cloud documents and future enum changes.

## AI and cost boundaries

The feature does not change:

- the Gemini request count;
- the generation prompt;
- the response JSON contract;
- token usage;
- cuisine rotation order or advancement.

It only persists and renders the local cuisine value already selected for the existing generation call.

## Compatibility and failure handling

- Legacy Room rows and Firestore documents remain valid with `null` cuisine.
- Unknown remote enum values are ignored instead of failing synchronization.
- A generated menu whose cuisine state is unexpectedly absent can still be saved normally; it simply omits the label.
- UI components render cuisine conditionally, so manual and legacy menus preserve their current spacing rather than showing empty placeholders.

## Verification

Automated coverage must verify:

- generated cuisine is copied into a saved menu;
- manual menus keep cuisine `null`;
- Room entity/domain conversion preserves known and null cuisine values;
- migration 9 to 10 is registered and non-destructive;
- Firestore writes, reads, safely ignores unknown values, and supports missing fields;
- recent, favorite, carousel/grid, and saved-detail presentation contracts use the localized cuisine label when present;
- existing carousel ordering, favorite ordering, active-audience filtering, scroll-reset behavior, image persistence, health analysis, and calories remain unchanged.

Final validation runs unit tests, debug build, release-debuggable build, and manual inspection on a narrow Android viewport.
