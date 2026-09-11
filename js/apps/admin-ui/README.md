# Keycloak Admin UI

This project is the next generation of the Keycloak Admin UI. It is written with React and [PatternFly 4](https://www.patternfly.org/v4/) and uses [Vite](https://vitejs.dev/guide/).

## Features

Contains all the "pages" from the admin-ui as re-usable components, all the functions to save and the side menu to use in your own build of the admin-ui

## Install

```bash
npm i @keycloak/keycloak-admin-ui
```

## Usage

Wrap your app with `KeycloakProvider` from `@keycloak/keycloak-ui-shared` so the realm, client, and server URLs are available. Any subtree that calls `useAdminClient()` (or otherwise needs the Keycloak Admin Client) must also be wrapped with `AdminClientProvider` from this package, **inside** `KeycloakProvider`. `AdminClientProvider` initializes the admin client from the authenticated Keycloak instance and exposes it through `AdminClientContext`.

```jsx
import { KeycloakProvider } from "@keycloak/keycloak-ui-shared";
import { AdminClientProvider } from "@keycloak/keycloak-admin-ui";

//...

<KeycloakProvider environment={environment}>
  <AdminClientProvider>
    {/* components that use useAdminClient() */}
  </AdminClientProvider>
</KeycloakProvider>
```

The `environment` object must include the admin console fields expected by this library (see `AdminEnvironment` in the package), not only `BaseEnvironment`.

### Translation

For the translation we use `react-i18next` you can [set it up](https://react.i18next.com/) as described on their website.
If you want to use the translations that are provided then you need to add `i18next-fetch-backend` to your project and add:

```ts

backend: {
  loadPath: `http://localhost:8180/resources/master/admin/{{lng}}`,
  parse: (data: string) => {
    const messages = JSON.parse(data);

    return Object.fromEntries(
      messages.map(({ key, value }) => [key, value])
    );
  },
},
```
to the `i18next` config object.

## Playwright Test Environment

The Playwright tests under `js/apps/admin-ui/test` accept these environment variables:

- `KEYCLOAK_SERVER_URL` (default: `http://localhost:8080`)
- `KEYCLOAK_ADMIN_USER` and `KEYCLOAK_ADMIN_PASSWORD` (default: `admin` / `admin`)
- `KEYCLOAK_REQUIRE_OID4VCI=true` to fail OID4VCI tests when the server feature is missing instead of skipping them

These test-only variables are different from server bootstrap variables such as `KC_BOOTSTRAP_ADMIN_USERNAME` and `KC_BOOTSTRAP_ADMIN_PASSWORD`.

## Playwright loading conventions

Async UI in the admin console exposes two distinct loading signals for E2E tests:

- `data-testid="table-loading-overlay"` — visible while a `LoadingOverlay` table fetch is in progress (skeleton overlay). The container also sets `aria-busy="true"`.
- `data-testid="page-loading-spinner"` — visible while a full-page `KeycloakSpinner` placeholder is shown before a section mounts (for example the Users list before `UserDataTable`).

Prefer waiting for the **element you interact with** (row link, toolbar button, empty-state action) rather than polling spinners globally.

### Helpers

```ts
import {
  waitForLoadingComplete,
  waitForLoadingCycle,
  waitForPageLoadingComplete,
  waitForTableIdle,
} from "./utils/loading.ts";
```

| Helper | Use when |
|--------|----------|
| `waitForLoadingComplete` | After an action that triggers a `KeycloakDataTable` reload; waits for overlay absence |
| `waitForLoadingCycle` | After search Enter or refresh; tolerates a short delay before the overlay appears |
| `waitForPageLoadingComplete` | A page still shows `KeycloakSpinner` before its main content |
| `waitForTableIdle` | Both overlay and page spinner must clear (e.g. navigating to Users) |

```ts
await page.keyboard.press("Enter");
await waitForLoadingCycle(page);
await page.locator("table tbody").getByRole("link", { name: itemName }).click();
```

`clickTableRowItem` and `assertRowExists` wait directly on row/link locators. `clickTableToolbarItem` waits for the toolbar to appear (it is not rendered until the first table fetch completes on empty tables).

### Required test fallbacks (CI)

Some PatternFly and browser interactions still need fallbacks despite UI stability work:

- **Switches** — use `clickSwitch` / `switchOn` / `switchOff` from `form.ts` (label force-click), not raw `.click()` on the hidden input
- **Authentication flow drag** — `dragExecutionAboveExecution` tries pointer, `dragTo`, and keyboard paths; waits for `flow-order-stable` before and after a move
- **Table row names** — `clickTableRowItem` matches substring link text (disabled badges), exact names, and `provider-name-link` for draggable provider tables

## Building

To build a library instead of an app you need to add the `LIB=true` environment variable.

```bash
LIB=true pnpm run build
```