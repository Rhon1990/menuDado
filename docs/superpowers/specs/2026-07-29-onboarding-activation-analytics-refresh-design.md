# Onboarding Activation and Analytics Refresh Design

## Goal

Refresh MenuDado's onboarding so it communicates the current product value,
drives users toward a useful first result, and produces privacy-safe Firebase
Analytics data that can measure acquisition, activation, and retention.

The primary success signal is not closing onboarding. It is creating the first
menu.

## Current State

- Onboarding is a single dialog with one value proposition.
- Its primary CTA is `Crear mi primer menú`; Home currently uses
  `Ayúdame a elegir` for AI-assisted menu creation.
- Onboarding content version `5` is stored locally and synchronized through the
  existing Firestore onboarding document.
- Firebase Analytics already records `onboarding_shown`,
  `onboarding_completed`, `cta_tapped`, `first_menu_created`, generation events,
  form events, saved-menu actions, favorites, and market-list CTAs.
- `cta_tapped` uses closed `screen` and `cta` values. Product analytics does not
  send menu text, profile details, user identifiers, or document identifiers.

## Approaches Considered

### 1. Copy-only update

Change onboarding strings and keep version `5` and the current event payloads.
This is the smallest implementation, but existing users would not see the
updated value proposition and reports could not distinguish a fresh install
from an upgrade exposure.

### 2. Focused activation refresh

Keep one onboarding screen, align its primary CTA with Home, bump the content
version once, and enrich the existing onboarding events with version and
exposure parameters. This preserves a short path to value and gives the minimum
data required to compare new and returning users.

This is the selected approach.

### 3. Multi-step onboarding and expanded telemetry

Create separate screens for AI, saved menus, market list, and account benefits,
with one event per step. This provides more granular data but increases friction,
implementation cost, and the risk of abandonment before the first useful
action.

## UX Design

Onboarding remains one responsive dialog using the existing MenuDado logo,
colors, typography, rounded card, trust messages, and accessible actions.

The Spanish content communicates three current benefits:

- decide what to prepare with a healthy AI-assisted idea;
- reuse saved menus when the user wants a quick choice;
- prepare the market list from saved menu products.

The visible hierarchy is:

1. Title: `Decide qué preparar hoy`.
2. A concise supporting message covering AI help, saved-menu choice, and the
   market list.
3. Existing trust messages: no registration required and user data under
   control.
4. Primary CTA: `Ayúdame a elegir`.
5. Secondary action: `Explorar la app`.

English and French resources express the same meaning naturally rather than
translating word for word.

The primary action closes onboarding and leaves Home in its existing default AI
mode. It does not launch an AI request automatically because meal type,
audience, profile compatibility, quota, rewarded-ad state, and optional
ingredients must remain under explicit user control.

The secondary action closes onboarding without changing Home state.

## Versioning and Exposure

`CURRENT_ONBOARDING_VERSION` changes from `5` to `6`.

- A user with no completed onboarding version receives exposure type
  `new_install`.
- A user who completed an earlier version receives exposure type `upgrade`.
- Version `6` is shown only once per user state.
- Completing or skipping stores version `6` through the existing
  `OnboardingStore`.
- The current remote synchronization path uploads the same version to the
  existing Firestore onboarding document.

No new Firestore collection, document type, field contract, migration, or
security-rule change is required.

## Components and Responsibilities

### UI resources

Localized string resources own all visible copy in Spanish, English, and French.
The existing onboarding composable owns layout and interaction. No new screen or
navigation route is added.

### Onboarding state

`MenuDadoViewModel` determines whether version `6` must be shown. Before
presentation, it derives the exposure type from the existing version state and
keeps that closed value for the corresponding completion event.

The derivation reuses `OnboardingStore.isOnboardingCompleted`:

- completion of version `1` or newer means `upgrade`;
- no prior completion means `new_install`.

This avoids widening the persistence interface solely for analytics.

### Analytics contract

The existing `MenuDadoAnalytics` abstraction remains the only entry point.
Onboarding methods accept the content version and exposure type explicitly so
the Firebase adapter and test fake cannot silently disagree.

The Firebase implementation sanitizes closed strings with the existing helper
and writes the content version as the alphanumeric categorical value `v6` so
GA4 can expose it as an event-scoped custom dimension for an Android app.

## Analytics Schema

### Existing events with enriched parameters

`onboarding_shown`:

- `onboarding_version`: `v6`
- `exposure_type`: `new_install` or `upgrade`

`onboarding_completed`:

- `action`: `start` or `skip`
- `onboarding_version`: `v6`
- `exposure_type`: `new_install` or `upgrade`

`cta_tapped` keeps:

- `screen=onboarding`
- `cta=create_first_menu` for the primary action
- `cta=explore_without_onboarding` for the secondary action

The stable CTA identifiers are intentionally retained even though the visible
labels change. This preserves historical comparisons because the underlying
actions have not changed.

### Events intentionally not added

No duplicate favorite or market-list events are introduced. Existing closed CTA
values already distinguish favorite toggles, menu inclusion, product checked or
restored, and purchased-item clearing. Duplicating these interactions would
inflate event volume without creating a new decision signal.

No event includes recipe content, ingredients, notes, email, Firebase UID,
local or remote menu IDs, photo URI, allergies, conditions, pregnancy state, or
free profile text.

## Firebase and GA4 Configuration

Create or verify event-scoped custom dimensions:

- `screen` mapped to event parameter `screen`;
- `cta` mapped to event parameter `cta`;
- `action` mapped to event parameter `action`;
- `onboarding_version` mapped to event parameter `onboarding_version`;
- `exposure_type` mapped to event parameter `exposure_type`.

Mark `first_menu_created` as the primary key event. It represents actual
activation and can be used to evaluate acquisition sources.

Do not mark `onboarding_completed` as a key event. It remains a funnel step
because closing onboarding does not prove that the user received product value.

Custom dimensions and key-event configuration are prospective. They do not
backfill historical parameter values.

The target funnel is:

`onboarding_shown` -> `onboarding_completed` with `action=start` ->
`ai_menu_gen_started` or `menu_form_started` -> `first_menu_created`.

Existing events such as `app_opened`, `menu_saved`, `menu_card_opened`,
`menu_inventory_changed`, favorite CTAs, and market-list CTAs remain available
for retention analysis without changing their schema.

## Error Handling and Safety

- Dismissing the Android dialog retains the existing safe `skip` behavior.
- Analytics failures remain non-blocking through the Firebase adapter's
  `runCatching` boundary.
- The local onboarding completion is written before remote synchronization, so
  a slow or unavailable network does not reopen the dialog in the same local
  state.
- Pending Firestore synchronization continues through the existing retry path.
- The CTA does not start generation implicitly and therefore cannot consume
  quota, rewarded credits, tokens, or an advertisement without another explicit
  user action.

## QA Strategy

### Unit tests

- Version `6` is required after version `5` was completed.
- No prior completion produces `new_install`.
- A prior completed version produces `upgrade`.
- `onboarding_shown` emits once with version and exposure type when visible.
- Primary completion emits `action=start`, version, and exposure type.
- Secondary completion and dialog dismissal emit `action=skip`, version, and
  exposure type.
- Completing version `6` prevents subsequent presentation.
- Existing CTA IDs remain `create_first_menu` and
  `explore_without_onboarding`.
- The onboarding definition remains a single page.

### Resource and UI checks

- Spanish, English, and French contain the updated title, body, and actions.
- Large font scale does not clip either action or supporting text.
- Primary and secondary actions retain at least a 48 dp touch target.
- TalkBack reads the title, explanation, trust messages, and both actions in a
  meaningful order.
- The primary action lands on Home with AI mode selected but does not issue a
  request.
- The secondary action lands on Home without changing filters or drafts.

### Build and static verification

- Run focused onboarding, analytics, ViewModel, and UI-state unit tests.
- Run the full debug unit-test suite.
- Compile the debug Kotlin variant.
- Run `git diff --check`.
- Verify that only intended resources, onboarding state, analytics, tests, and
  project documentation changed.

### Firebase validation

- Use a debug build with Analytics DebugView to verify exact event names and
  parameter values.
- Confirm no personal or free-text values appear.
- Verify or create the five custom dimensions.
- Mark `first_menu_created` as a key event.
- Record Firebase console configuration separately from Android build results.

## Risks and Mitigations

- **Existing-user interruption:** version `6` appears once only and remains one
  concise screen.
- **Historical metric discontinuity:** stable event and CTA names are preserved;
  new parameters segment data prospectively.
- **Overstating onboarding success:** only `first_menu_created` is treated as
  the primary key event.
- **Remote synchronization delay:** current local-first completion and pending
  sync behavior remain unchanged.
- **Telemetry duplication:** no redundant favorite or market-list events are
  added.
- **Localization growth:** the existing responsive dialog is retained and must
  be checked with large fonts in all three languages.

## Out of Scope

- Multi-step onboarding, experiments, or remote onboarding copy.
- Automatic AI generation from the onboarding CTA.
- New Firestore documents, collections, rules, or indexes.
- Changes to Firebase AI Logic, Gemini prompts, quotas, Remote Config, or ads.
- Sending personal content or identifiers to Analytics.
- Retrofitting historical Analytics data.
