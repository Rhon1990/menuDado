# Catalog, Market, About, and Onboarding V7 Design

## Goal

Measure the new catalog and Market interactions with stable, privacy-safe CTA
markers, while updating About and onboarding so they describe search, filters,
and the Market list accurately in Spanish, English, and French.

The change must preserve current navigation, filtering, saved-menu, favorite,
and Market behavior. Analytics must describe only the action taken and must
never contain user-entered text or dietary-profile data.

## Current State

- Saved-menu audience and favorites lists provide localized card-text search
  and profile-oriented filters.
- The catalog search field, clear-search icon, filter button, filter choices,
  and empty-state reset action do not yet have CTA markers.
- `cta_tapped` is the existing generic interaction event and accepts closed
  `screen` and `cta` values.
- Menu opening, favorites, `Ver más`, Market navigation, product
  checked/restored state, management actions, and clear confirmations are
  already tracked.
- The Market purchased-products expander and the management-sheet dismissal
  are not tracked.
- About uses the versioned Remote Config key `about_description_v2` with a
  localized fallback.
- Onboarding is a single activation dialog at content version `6`.

## Approaches Considered

### 1. Minimal CTA coverage

Track only filter opening and the empty-state reset. This is low cost but does
not reveal whether search and individual filter controls are useful.

### 2. Balanced CTA coverage

Reuse `cta_tapped` for each meaningful new interaction, with stable closed
identifiers and without attaching selected values. Keep existing specialized
events and CTA markers unchanged to avoid duplicated counts.

This is the selected approach because it provides actionable usage data with
the smallest analytics and privacy footprint.

### 3. Dedicated catalog events with filter parameters

Create new events carrying query length, catalog scope, or selected filter
types. This adds reporting complexity and risks exposing sensitive intent
without improving the decisions required for this release.

## Analytics Design

All new catalog markers use `cta_tapped` with `screen=menu_catalog`.

| Interaction | `cta` value | Emission rule |
| --- | --- | --- |
| Start a search | `search_started` | When the query changes from blank to nonblank |
| Clear-search icon | `clear_search` | Only from the visible clear icon, not from keyboard deletion |
| Open filter sheet | `open_filters` | On the filter-button tap |
| Select audience | `select_audience_filter` | After choosing any audience option, including All |
| Select dietary need | `select_dietary_filter` | After choosing any enabled dietary option |
| Toggle favorites-only | `toggle_favorites_only` | On each toggle |
| Toggle healthy-only | `toggle_healthy_only` | On each toggle |
| Reset an empty result | `clear_search_and_filters` | On the empty-state action |

The search marker represents a search intent rather than every character. If a
query returns to blank and the user starts another query, a new
`search_started` marker is valid. The query itself is never sent.

Market keeps `screen=market`. The following missing markers are added:

| Interaction | `cta` value |
| --- | --- |
| Expand purchased products | `expand_purchased_products` |
| Collapse purchased products | `collapse_purchased_products` |
| Dismiss Market management | `close_market_management` |

Existing Market CTA values remain unchanged, including navigation,
`open_market_management`, product checked/restored state, opening clear
confirmations, confirming or cancelling clear actions, and menu inclusion.
Existing favorite, menu-open, and `menu_list_view_more_opened` measurements are
also not duplicated.

## Component Boundaries and Data Flow

- `MenuCatalogSearchBar` exposes distinct callbacks for query editing and the
  clear icon so analytics can distinguish an explicit CTA from text editing.
- `MenuCatalogFilterSheet` exposes action callbacks using closed UI action
  identifiers. It continues to return `MenuCatalogFilters` as its state
  contract; analytics does not become part of the component.
- `MenuDadoScreen` remains the composition boundary that maps UI callbacks to
  `MenuDadoViewModel.trackCtaTapped` and then applies the existing state change.
- The empty-state reset callbacks and Market purchased-section callback follow
  the same pattern: mark the CTA first, then perform the current local action.
- `MenuDadoAnalytics` and `FirebaseMenuDadoAnalytics` require no new event
  method because the existing generic CTA contract is sufficient.

Analytics failures remain non-blocking through the current Firebase adapter.
No UI state or user action depends on successful event delivery.

## Privacy Contract

New events contain only the closed `screen` and `cta` identifiers documented
above. They must not include:

- the search query or any portion, length, token, or normalized representation
  of it;
- selected audience, pregnancy state, vegan state, allergies, avoided foods,
  health conditions, or free-text profile values;
- recipe names, ingredients, notes, menu IDs, user IDs, email addresses, or
  image references;
- Market product names or keys.

This restriction applies to production logging, debug logging, tests, and any
future custom dimensions derived from these markers.

## About Update

The localized `about_reason` fallback is updated to mention that saved menus
can be found with search and filters while retaining AI, dice, Market, guest,
and account value.

Spanish:

> MenuDado nació para resolver una pregunta cotidiana: ¿qué preparo hoy?
> Genera ideas con IA adaptadas a tu perfil, guarda tus menús, encuéntralos
> mediante búsqueda y filtros, deja que el dado te ayude a elegir y reúne los
> productos en tu lista de mercado. Puedes empezar sin registrarte y crear una
> cuenta gratis para conservar tus datos.

English:

> MenuDado was created to answer an everyday question: what should I make
> today? It generates AI ideas tailored to your profile, saves your menus,
> helps you find them with search and filters, lets the dice help you choose,
> and gathers the items in your shopping list. You can start without
> registering and create a free account to keep your data.

French:

> MenuDado a été créé pour répondre à une question du quotidien : que préparer
> aujourd’hui ? L’app génère des idées avec l’IA adaptées à votre profil,
> enregistre vos menus, vous aide à les retrouver grâce à la recherche et aux
> filtres, laisse le dé vous aider à choisir et regroupe les produits dans votre
> liste de courses. Vous pouvez commencer sans vous inscrire et créer
> gratuitement un compte pour conserver vos données.

The Remote Config description key changes from `about_description_v2` to
`about_description_v3`. A missing or blank `v3` value uses the new localized
fallback. The creator and contact keys remain unchanged. This change does not
publish a Remote Config template.

## Onboarding Version 7

The onboarding stays as one responsive dialog with the current logo, colors,
trust messages, primary action, and secondary action. Only its supporting copy
and content version change.

Title and actions remain stable:

- title: `Decide qué preparar hoy`;
- primary action: `Ayúdame a elegir`;
- secondary action: `Explorar la app`;
- trust messages: `Sin registro` and `Tus datos, bajo control`.

Updated supporting copy:

- Spanish: `Recibe una idea saludable con IA, encuentra tus menús guardados con búsqueda y filtros y prepara tu lista de mercado.`
- English: `Get a healthy AI-assisted idea, find your saved menus with search and filters, and prepare your shopping list.`
- French: `Obtenez une idée saine avec l’IA, retrouvez vos menus enregistrés grâce à la recherche et aux filtres, puis préparez votre liste de courses.`

`CURRENT_ONBOARDING_VERSION` changes from `6` to `7`. Version `7` is shown
once to new users and once to users whose stored completion version is older.
Completion or skipping persists version `7` through the existing local-first
store and synchronization flow. No Firestore collection, migration, or
security-rule change is required.

Existing onboarding event and CTA identifiers remain stable:

- `onboarding_shown` and `onboarding_completed` report version `v7` and the
  existing `new_install` or `upgrade` exposure;
- the primary CTA remains `create_first_menu`;
- the secondary CTA remains `explore_without_onboarding`;
- `first_menu_created` remains the activation key event.

## Accessibility and UX

- Existing accessible content descriptions for search clearing and filter
  opening are preserved.
- CTA tracking does not add visual controls, delays, loading states, or
  announcements.
- The onboarding copy remains short enough for the existing scrollable dialog
  and supported mobile widths.
- The approved preview changes content only; implementation continues to use
  the real MenuDado symbol and established Compose components.

## QA Strategy

### Unit and component tests

- Blank-to-nonblank search transition emits `search_started`; subsequent
  characters do not.
- The clear icon emits `clear_search`, while keyboard deletion to blank does
  not.
- Opening filters and each filter interaction emit the documented closed CTA.
- Resetting an empty result emits `clear_search_and_filters` and restores the
  existing default filters.
- No event receives query text, filter selection values, profile data, menu
  content, or Market product data.
- Expanding, collapsing, and dismissing Market management emit their expected
  CTA without changing current behavior.
- Existing Market, favorite, menu-open, and view-more analytics remain emitted
  exactly once.
- About uses `about_description_v3` and falls back for absent or blank remote
  content.
- Onboarding version `7` is required after version `6`, shown only once, and
  reports `v7` with the correct exposure type.
- Spanish, English, and French resources contain equivalent updated meaning.

### Build and regression validation

- Run focused analytics, catalog-filter, Market, About, onboarding, and
  localization unit tests.
- Run the app unit-test suite and compile the affected Android variant.
- Review the onboarding at a narrow mobile width and confirm that content,
  buttons, and scrolling remain usable.
- Confirm that entering each catalog scope still resets filters and preserves
  the scope-specific Adult, child, baby, or favorites constraint.

## Out of Scope

- Sending free text or dietary selections to Firebase Analytics.
- Creating new GA4 events, custom dimensions, or key events.
- Publishing Remote Config changes.
- Redesigning onboarding into multiple pages.
- Changing search, filter, favorite, Market, or menu-selection behavior.
