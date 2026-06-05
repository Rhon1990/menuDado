# Firebase Analytics Design

## Goal

Add anonymous Firebase Analytics to MenuDado so Firebase can report active devices/users and the app can record product statistics that explain how core features are used.

## Scope

- Enable Firebase Analytics through the existing Firebase project configuration.
- Keep tracking anonymous: do not send menu names, ingredients, notes, generated text, or other user-authored content.
- Track only coarse product events and metadata:
  - app opened, including device manufacturer, device model, Android version, locale country, and time zone.
  - menu saved and deleted.
  - first menu created and local menu inventory counts.
  - form start, meal type selection, and blocked save attempts.
  - menu cards opened.
  - dice filter selected, dice rolled, available candidate counts, and empty dice results.
  - AI menu generation started, completed, or failed, including health status or coarse failure type when available.
  - individual and batch AI analysis started, completed, or failed, including health status for single-result flows and coarse failure type when available.
  - local daily AI limit reached.

## Architecture

Create a small `MenuDadoAnalytics` interface in an analytics package. The ViewModel depends on the interface, using a no-op default for tests and previews. The Android application wires a `FirebaseMenuDadoAnalytics` implementation backed by Firebase Analytics.

Firebase Analytics automatically provides device/user metrics such as first opens, sessions, active users, device models, OS versions, app versions, and aggregated geographic reporting once the SDK is present and initialized by `google-services.json`. MenuDado also sends coarse app-open metadata as custom event parameters and user properties for easier filtering.

## Error Handling

Analytics calls must never affect app behavior. The Firebase adapter catches SDK failures and the no-op implementation remains the fallback-friendly contract. The app does not request GPS/location permission; "where" is represented by Firebase's aggregated geo reporting plus locale country and time zone.

## Testing

Unit tests use a recording analytics implementation to assert that the ViewModel emits the intended events for successful and failed flows. Verification includes the focused ViewModel test suite and Kotlin compilation.
