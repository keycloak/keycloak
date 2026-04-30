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

The `environment` object must include the admin console fields expected by this library (see `Environment` in the package), not only `BaseEnvironment`.

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

## Building

To build a library instead of an app you need to add the `LIB=true` environment variable.

```bash
LIB=true pnpm run build
```