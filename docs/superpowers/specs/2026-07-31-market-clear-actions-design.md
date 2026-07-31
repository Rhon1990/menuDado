# Market Clear Actions Design

## Goal

Let users clear the entire Market list without first moving products through `Comprados`, while keeping destructive actions easy to find, visually calm, and protected by confirmation.

## Product behavior

MenuDado will expose two distinct actions:

1. `Vaciar lista` removes every pending and purchased product currently visible in Market.
2. `Limpiar comprados` removes only purchased products and keeps pending products.

Neither action deletes menus or their generated shopping products. Clearing only deactivates their current contributions to Market. Users can add those products again later by enabling `Incluir este menú en Mercado` from a saved menu.

The actions are shown only when they have something to remove:

- `Vaciar lista` appears whenever Market contains at least one product.
- The compact clear-purchased action appears whenever at least one purchased product exists, even when the purchased section is collapsed.

## Visual design and interaction

### Clear all

The Market header will place its title on the left and a compact `Vaciar lista` action with a trash icon on the right. The label prevents the top-level destructive action from being mistaken for clearing only purchased products. It uses MenuDado's tomato color as a destructive accent, preserves a minimum 48 dp touch target, and remains visually secondary to the shopping list itself.

Tapping it opens a confirmation dialog:

- Title: `¿Vaciar la lista?`
- Body: `Quitaremos todos los productos de Mercado. Tus menús seguirán guardados y podrás volver a incluirlos cuando quieras.`
- Safe primary action: `Seguir comprando`
- Destructive secondary action: `Sí, vaciar lista`

The safe action uses the brand green. The destructive action uses tomato red and does not receive stronger visual weight than the safe path.

### Clear purchased

The `Comprados` header will place a compact trash icon immediately beside its label. The expand/collapse control remains available independently. The existing `Limpiar comprados` text button at the bottom of the expanded section is removed.

The icon has a localized content description equivalent to `Limpiar comprados` and a 48 dp touch target. Tapping it opens a separate confirmation dialog:

- Title: `¿Limpiar los comprados?`
- Body: `Los productos pendientes seguirán en tu lista.`
- Safe primary action: `Seguir comprando`
- Destructive secondary action: `Sí, limpiar`

Both dialogs dismiss without mutation on outside tap, back press, or the safe action. After confirmation, the dialog closes and Room's observable data updates the list immediately.

## Architecture and data flow

The existing Compose screen will own only transient dialog visibility. Confirming delegates to `MenuDadoViewModel`, which calls a focused repository operation.

`clearPurchasedMarketProducts()` keeps its current meaning but gains the confirmation UI before invocation. A new `clearAllMarketProducts()` operation will perform the all-products behavior.

The repository will use one Room transaction to:

1. Read the active Market contributions and affected menu IDs.
2. Mark all active `menu_shopping_products` contributions inactive.
3. Reset purchased states so re-adding a product later does not restore it as already purchased.
4. Mark affected menus and remote purchased-state resets pending when Firebase synchronization is configured.

After the local transaction, the repository invokes the existing pending-sync path. Remote failure does not restore products in the UI; pending local mutations remain available for retry. No Room schema, migration, Firebase collection, or security-rule change is required.

The operation preserves:

- Menu rows and their analysis.
- Each menu's complete `shoppingProducts`.
- Favorites, photos, audience, meal type, and all other menu metadata.

It only empties `activeShoppingProductKeys` through the existing product-contribution representation.

## Analytics and privacy

The existing closed `cta_tapped` event will record categorical actions for:

- opening each clear confirmation;
- confirming each clear action;
- cancelling each clear action.

Events include only fixed screen and CTA names. They never include product names, product keys, menu IDs, counts, recipe data, or profile information.

## Localization and accessibility

All new visible text and content descriptions will be added to Spanish, English, and French resources. Icon-only actions will expose localized descriptions to TalkBack. Touch targets remain at least 48 dp, destructive color is not the only signal, and every destructive action requires explicit confirmation.

## Error handling

The local transaction is the source of immediate UI truth. If it fails, the list remains unchanged and the ViewModel exposes the existing user-message mechanism with a localized retry message: `No pudimos actualizar tu lista de Mercado. Inténtalo de nuevo.` Firebase failures are handled as pending synchronization and do not block local clearing.

Repeated confirmation taps are prevented by closing the dialog before launching the ViewModel action. The repository transaction is idempotent when the list is already empty.

## Testing strategy

### Unit and data tests

- Clearing all deactivates pending and purchased contributions across multiple menus.
- Menus and their full shopping-product data remain stored.
- Purchased states are reset so restored products return as pending.
- A local-only repository leaves no obsolete purchased state.
- A repository with Firebase marks affected menus and purchased resets pending for retry.
- Calling clear-all on an empty list is a safe no-op.
- Existing clear-purchased behavior continues to keep pending products.

### ViewModel and UI-contract tests

- `clearAllMarketProducts()` delegates once to the repository.
- `Vaciar lista` visibility depends on the complete list.
- The clear-purchased icon is visible with purchased products even while collapsed.
- Both actions use the correct labels, icons, content descriptions, colors, and confirmation copy.
- Cancelling either dialog does not mutate data.

### Regression verification

- Run focused Market, repository, and ViewModel tests.
- Run the complete debug unit-test suite.
- Build the debug APK.
- Manually verify both dialogs with pending-only, purchased-only, and mixed lists, including large font and TalkBack focus order when a device is available.

## Out of scope

- Undo snackbars or a trash history.
- Deleting saved menus or their generated shopping products.
- Manual product entry.
- Database migrations or new Firebase documents.
- Sending product-level analytics.
