# Keycloak Account UI

This project is the next generation of the Keycloak Account UI. It is written with React and [PatternFly](https://www.patternfly.org/) and uses [Vite](https://vitejs.dev/guide/).

## Features

Contains all the "pages" from the account-ui as re-usable components, all the functions to save and the side menu to use in your own build of the account-ui

## Install

```bash
npm i @keycloak/keycloak-account-ui
```

## Usage

To use these pages you'll need to add `KeycloakProvider` in your component hierarchy to setup what client, realm and url to use.

```jsx
import { KeycloakProvider } from "@keycloak/keycloak-ui-shared";

//...

<KeycloakProvider environment={{
      serverBaseUrl: "http://localhost:8080",
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
  loadPath: `http://localhost:8180/resources/master/account/{{lng}}`,
  parse: (data: string) => {
    const messages = JSON.parse(data);

    return Object.fromEntries(
      messages.map(({ key, value }) => [key, value])
    );
  },
},
```
to the `i18next` config object.

### Save functions

If you want to build your own "pages" you can still reuse the save functions:
 * deleteConsent
 * deleteCredentials
 * deleteSession
 * getApplications
 * getCredentials
 * getDevices
 * getGroups
 * getLinkedAccounts
 * getPermissionRequests
 * getPersonalInfo
 * getSupportedLocales
 * linkAccount
 * savePersonalInfo
 * unLinkAccount

Example:
```ts
import { savePersonalInfo, useEnvironment } from "@keycloak/keycloak-account-ui";

//...
function App() {
  // the save function also needs to have the context so that it knows where to POST
  // this hook gives us access to the `KeycloakProvider` context
  const context = useEnvironment();
  const submit = async (data) => {
    try {
      await savePersonalInfo(context, data);
    } catch (error) {
      // Error contains `name` and `value` of the server side errors
      // and your app will have better error handling ;)
      console.error(error);
    }
}
// ...
```
## Building

To build a library instead of an app you need to add the `LIB=true` environment variable.

```bash
LIB=true pnpm run build
```