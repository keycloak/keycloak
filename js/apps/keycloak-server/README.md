# Keycloak Server

This app allows you to run a local development version of the Keycloak server.

### Running the Keycloak server

First, ensure that all dependencies are installed locally using PNPM by running:

```bash
pnpm install
```

After the dependencies are installed we can start the Keycloak server by running the following command:

```bash
pnpm start
```

This will download the [Nightly version](https://github.com/keycloak/keycloak/releases/tag/nightly) of the Keycloak server and run it locally on port `8180`. If a previously downloaded version was found in the `server/` directory then that one will be used instead. If you want to download the latest Nightly version you can remove the server directory before running the command to start the server.

If you want to run with a local Quarkus distribution of Keycloak for development purposes, you can do so by running this command instead: 

```bash
pnpm start -- --local
```

**All other arguments will be passed through to the underlying Keycloak server.**

In order for the development version of the Admin UI to work you will have to import a custom client to the Keycloak server. This is only required during development as the development server for the Admin UI runs on a different port. This client will be imported automatically under the name `security-admin-console-v2` when the Keycloak server starts.

This client only allows redirects from/to "localhost:8080" so be sure either modify the client json in `./scripts` or only attempt to authenticate and redirect from that address

