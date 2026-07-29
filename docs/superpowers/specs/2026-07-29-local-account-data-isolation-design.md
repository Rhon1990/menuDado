# Local Account Data Isolation Design

## Goal

MenuDado must never show, modify or synchronize local data that belongs to a
different Firebase user. Switching from account A to account B on the same
device must immediately show only account B data. Returning to account A must
restore account A's local cache, including offline changes that have not yet
reached Firestore.

## Root Cause

The current Room database and the SharedPreferences used by backend
synchronization are global to the installation. Menus have no local owner.
After authentication, the app marks every visible local menu and dietary
profile as pending and uploads them using the new Firebase session. Therefore,
an account switch can both expose account A data to account B and copy that data
into account B's Firestore path.

## Considered Approaches

### 1. Clear local data on sign-out

This is the smallest code change, but it can destroy offline changes and forces
every returning account to download all data again. It is rejected because
sign-out must not be destructive.

### 2. Add an owner column to every Room table

Queries could filter by owner, but menu IDs are currently both Room primary
keys and Firestore document IDs. Two accounts can legitimately contain the
same numeric ID, so this approach requires new synthetic local IDs, composite
keys and remote-ID mapping across every menu relation. It has a larger and
riskier migration surface.

### 3. Separate account-bound storage by Firebase UID

This is the selected approach. Each Firebase UID receives an independent Room
database and independent account-bound preference namespace. Existing entity
and Firestore IDs remain valid because they only need to be unique inside one
user's storage.

## Architecture

Introduce a local data scope controller whose active scope is the current
Firebase UID. Anonymous Firebase users also use their UID, so registering by
linking credentials keeps the same local and remote data automatically.
A signed-out state uses an empty, non-account scope and cannot expose the last
account's cache.

Room access is provided through scoped DAO adapters. Their observable queries
switch to the active UID's database, while each repository operation captures
one database scope for its complete mutation or synchronization pass. Database
filenames use a one-way hash of the UID and never expose the UID in logs or
analytics.

Account-bound SharedPreferences use the same scope:

- dietary profiles;
- backend pending-sync flags;
- backend-synchronized AI usage and onboarding state.

Already identity-scoped stores, such as rewarded credits and AI fallback
rotation, keep their existing implementation.

Authentication success or sign-out updates the local scope before refreshing
visible application state or starting backend synchronization. A sync started
for one UID keeps that UID's local data snapshot even if authentication changes
while the operation is finishing.

## Guest-to-Account Rules

- Anonymous registration that links credentials keeps the same UID and needs
  no migration.
- Signing an anonymous user into an existing account changes the UID. Only in
  this transition are visible guest menus copied into the destination
  account.
- Imported guest menus receive new local IDs before upload so they cannot
  overwrite same-numbered documents already owned by the destination account.
- A registered account is never a migration source for another registered
  account.
- Dietary and health profile data from an anonymous UID is not copied over an
  existing account's profile; the destination account's remote profile wins.

This narrows and supersedes the broad "mark all local data pending after every
sign-in" behavior described by the earlier email-auth design.

## Existing Installations

Legacy global data has no provenance, so ownership cannot be reconstructed
reliably after accounts have already been mixed. The upgrade must not guess
that those rows belong to a newly selected account.

The legacy database remains untouched as a recoverable quarantine. A scoped
database is created for the active UID and hydrated from that UID's Firestore
path. It is claimed once only when the current session is anonymous and no
registered `last_synced_user_id` exists; this is the only legacy state without
evidence of an earlier registered-account switch. Registered-account data is
restored from Firestore.

This prevents further leakage. Data that was already copied into the wrong
Firestore account requires a separate, explicit cleanup based on confirmed
user/document ownership; the app must not delete it automatically.

## Synchronization

Every synchronization run captures the authenticated UID and matching local
scope before it reads pending data. It aborts local state changes if those
identities do not match.

The order after authentication is:

1. Resolve the resulting Firebase UID.
2. Switch the local data scope.
3. If the source was anonymous and the UID changed, import guest menus once.
4. Upload only pending data from the destination UID's scope.
5. Hydrate only from `users/{destinationUid}`.
6. Refresh the ViewModel state.

The old `last_synced_user_id` marker is no longer used to mark all visible data
pending. It is read once for the legacy quarantine decision and removed after
the new scope metadata is initialized.

## Error Handling

- A failed remote synchronization leaves pending data in the correct UID
  database and does not reveal another scope.
- A failed guest import leaves the source guest database unchanged and records
  no completed-migration marker, allowing a safe retry.
- Scope switching itself is local and must not depend on network availability.
- No user-facing message mentions Room, Firestore, UID or internal storage.

## Testing

Automated tests must prove:

- account A menus disappear immediately after sign-out;
- account B never observes or uploads account A menus;
- returning to account A restores its local cache;
- equal numeric menu IDs in two accounts do not collide;
- pending offline mutations remain attached to their original account;
- linked anonymous registration preserves data without copying;
- anonymous-to-existing-account import happens once and remaps menu IDs;
- registered-account-to-registered-account migration is impossible;
- dietary profiles and pending flags are isolated by UID;
- a synchronization finishing after a scope change cannot update the new
  scope's rows;
- legacy registered-account data is quarantined rather than assigned by guess.

Run focused unit tests first, then the complete unit-test suite, debug lint,
debug compilation and an ADB scenario covering account A -> sign-out -> account
B -> sign-out -> account A.

## Documentation Impact

`docs/project-context.md` must replace the current broad local-data merge
statement with the UID isolation and guest-only migration contract. The QA
focus must explicitly include account switching and cross-account upload
prevention.

## Non-Goals

- Automatic deletion of documents already copied to the wrong Firestore user.
- Changing Firestore security rules or Firebase project configuration.
- Synchronizing caches between different registered accounts.
- Refactoring unrelated local-only preferences.
