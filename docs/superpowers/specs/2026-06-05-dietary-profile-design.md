# Dietary Profile Design

## Goal

Add a local dietary profile reachable from a hamburger menu so MenuDado can generate AI meal ideas that respect vegan preference and allergy restrictions.

## Scope

- Add a hamburger entry point in the main header.
- Open a drawer with a `Perfil alimentario` option.
- Show a local profile editor with:
  - vegan toggle.
  - allergy toggle.
  - common allergen checkboxes.
  - short optional free-text foods to avoid.
- Persist the profile locally with SharedPreferences.
- Include the profile in AI generation prompts only when configured.

## Privacy And Safety

The profile stays local. It is sent only to Firebase AI Logic as generation constraints when the user asks for an AI idea. It is not sent to Firebase Analytics.

## Architecture

Use a small domain model `DietaryProfile`, a `DietaryProfileStore` matching existing SharedPreferences stores, and inject it into `MenuDadoViewModel`. Keep Room unchanged because this is app preference state, not menu content.

## Testing

Unit tests cover prompt constraints and ViewModel profile persistence. Existing AI generation tests ensure the profile flows into the analyzer request.
