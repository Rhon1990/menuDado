# Email Auth With Guest Migration Design

## Goal

MenuDado must allow users to create an account or sign in with email and password while keeping the current guest flow unchanged. A registered user must keep menus and profile data after reinstalling the app and signing in again.

## Approved Approach

Use Firebase Authentication email/password alongside the existing anonymous authentication. The guest path remains the current anonymous Firebase user. When a guest creates an account, MenuDado links the anonymous user with the email credential using Firebase Auth so the Firebase `uid` does not change.

Because Firestore data already lives under `users/{uid}`, linking preserves existing guest menus, profile, onboarding, AI usage and pending backend sync data without copying documents between users.

## UX

On a fresh install, MenuDado opens Home immediately and starts the current guest flow by default. Login and registration live inside a bottom navigation destination called `Mi zona`.

`Mi zona` shows a guest greeting, a benefits band, account advantages and actions to create an account or sign in. The create-account and sign-in forms are simple dialogs styled with MenuDado colors and do not block access to the app.

If Firebase already has an email/password user, `Mi zona` shows an account view with avatar initial, email and synced-account state. The app does not ask the user to sign in again.

## Data Flow

- Guest: anonymous Firebase Auth user, local Room first, Firestore under `users/{anonymousUid}`.
- Register while guest: `linkWithCredential`, same `uid`, existing local and remote data remains attached.
- Register without guest session: create email/password user.
- Sign in: `signInWithEmailAndPassword`, then run the existing backend sync pipeline.
- Local data present before sign-in is marked pending and synced into the signed-in user to avoid silent loss.

## Error Handling

Authentication errors are converted into simple user-facing messages. Common cases include invalid email, short password, existing account, wrong password, and network/service failures.

## Testing

Unit tests cover:

- Whether the access screen should be shown.
- Guest account linking keeps the same user identity.
- Existing email users skip login on next app open.
- Guest mode remains available.
- Sync is requested after login or registration.

## Non-Goals

- Social login providers.
- Password reset.
- Account deletion UI.
- A profile/account tab in bottom navigation.
