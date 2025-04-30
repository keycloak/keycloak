# Keycloak Admin UI

This project is the next generation of the Keycloak Admin UI. It is written with React and [PatternFly 4](https://www.patternfly.org/v4/) and uses [Vite](https://vitejs.dev/guide/).

## Features

Contains all the "pages" from the admin-ui as re-usable components, all the functions to save and the side menu to use in your own build of the admin-ui

## Install

```bash
npm i @keycloak/keycloak-admin-ui
```

## Usage

To use these pages you'll need to add `KeycloakProvider` in your component hierarchy to setup what client, realm and url to use.

```jsx
import { KeycloakProvider } from "@keycloak/keycloak-ui-shared";

//...

<KeycloakProvider environment={{
      authServerUrl: "http://localhost:8080",
      realm: "master",
      clientId: "security-admin-console"
  }}>
  {/* rest of you application */}
</KeycloakProvider>
```

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