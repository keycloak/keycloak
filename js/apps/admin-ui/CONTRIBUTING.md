# Keycloak Admin UI

This project is the next generation of the Keycloak Administration UI. It is written with React and [PatternFly 4](https://www.patternfly.org/v4/) and uses [Vite](https://vitejs.dev/guide/) and [Playwright](hhttps://playwright.dev/).

## Development

### Prerequisites

Make sure that you have Node.js version 24 (or later) [installed on your system](https://nodejs.org/en/download).

You can find out which version of Node.js you are using by running the following command:

```bash
node --version
```

In order to run the Keycloak server you will also have to install the Java Development Kit (JDK). We recommend that you use the same version of the JDK as [required by the Keycloak server](https://github.com/keycloak/keycloak/blob/main/docs/building.md#building-from-source).

### Running the Keycloak server

See the instructions in the [Keycloak server app](../keycloak-server/README.md).

### Running the development server

Now that the Keycloak sever is running it's time to run the development server for the Admin UI. This server is used to build the Admin UI in a manner that it can be iterated on quickly in a browser, using features such as [Hot Module Replacement (HMR)](https://vitejs.dev/guide/features.html#hot-module-replacement) and [Fast Refresh](https://www.npmjs.com/package/react-refresh).

To start the development server run the following command:

```bash
pnpm dev
```

Once the process of optimization is done your browser will automatically open your local host on port `8080`. From here you will be redirected to the Keycloak server to authenticate, which you can do with the default credentials (`admin`/`admin`).

You can now start making changes to the source code, and they will be reflected in your browser.

In this mode the messages for translation will come directly from the messages.properties and the realm overrides will be ignored, this makes testing adding messages easier.
But when you want to see the realm overrides instead, you can add the environment variable `VITE_REALM_OVERRIDES=true` to disable this behavior.

## Building as a Keycloak theme

If you want to build the application using Maven and produce a JAR that can be installed directly into Keycloak, check out the [Keycloak theme documentation](../../keycloak-theme/README.md).

## Linting

Every time you create a commit it should be automatically linted and formatted for you. It is also possible to trigger the linting manually:

```bash
pnpm lint
```

## Integration testing with Playwright

This repository contains integration tests developed with the [Playwright framework](https://playwright.dev/).

### Prerequisites

Ensure the Keycloak and development server are running as [outlined previously](#running-the-keycloak-server) in this document.

### Running the tests

You can run the tests using the interactive graphical user [Visual Studio Code plugin](https://marketplace.visualstudio.com/items?itemName=ms-playwright.playwright)

Alternatively the tests can also run headless as follows:

```
pnpm test:integration
```
