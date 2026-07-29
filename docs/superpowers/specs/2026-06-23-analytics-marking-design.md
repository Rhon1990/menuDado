# MenuDado Analytics Marking Design

## Goal

Complete and standardize Firebase Analytics marking across MenuDado without sending personal menu content, free-text health data, creator/contact data, Firestore document IDs, or user IDs.

## Scope

Add product events for missing user actions:

- Dietary profile: opened, audience selected, updated.
- Menu editing: edit started, edit saved, photo updated.
- Menu browsing: list filter selected, audience filter selected, view-more opened.
- Backend sync: sync retried, sync finished.

Keep existing events for app open, menu creation/deletion, inventory, dice, onboarding, about, and AI, but extend them where needed with safe audience/source/status parameters.

## Safe Parameters

Allowed values are closed enums or counts:

- `meal_type`: `breakfast`, `lunch`, `dinner`, `unknown`, `all`
- `audience`: `adult`, `child`, `baby`, `unknown`, `all`
- `source`, `status`, `scope`, `action`, `reason`: controlled constants only
- Counts and booleans: `menu_count`, `active_audience_count`, `pending_menu_count`, `has_ai_analysis`, `has_photo`, `changed_recipe`

## Forbidden Data

Analytics must not send:

- Menu names, descriptions, ingredients, notes, or recipes.
- Allergens selected, pregnancy state, health conditions, foods to avoid, or any user-written dietary text.
- Contact email, creator name, Firestore document IDs, Firebase UID, local menu IDs, image URIs, or advertising IDs.

## Integration

The public contract remains `MenuDadoAnalytics`. `FirebaseMenuDadoAnalytics` owns event names and sanitized parameter mapping. `MenuDadoViewModel` emits events only at clear user-action boundaries. Backend sync events can be emitted from `MenuDadoApplication` because sync happens there.

## Validation

Unit tests should verify key new ViewModel events and ensure the recording fake receives no sensitive values. Build verification must include `:app:testDebugUnitTest`, `:app:compileDebugKotlin`, and manifest permission checks.
