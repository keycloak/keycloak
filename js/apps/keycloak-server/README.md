# Keycloak Server

This app allows you to run a local development version of the Keycloak server.

### Running the Keycloak server

First, ensure that all dependencies are installed locally using PNPM by running:

```sh
pnpm install
```

After the dependencies are installed we can start the Keycloak server by running the following command:

```sh
pnpm start
```

If you want to run the server against a local development Vite server, you'll have to pass the `--admin-dev` or `--account-dev` flag:

```sh
pnpm start --admin-dev
pnpm start --account-dev
```

The above commands will download the [Nightly version](https://github.com/keycloak/keycloak/releases/tag/nightly) of the Keycloak server and run it locally on port `8080`. If a previously downloaded version was found in the `server/` directory then that one will be used instead. If you want to download the latest Nightly version you can remove the server directory before running the command to start the server:

```sh
pnpm delete-server
```

Or if you just want to clear the data so you can start fresh without downloading the server again:

```sh
pnpm delete-data
```

If you want to run with a local Quarkus distribution of Keycloak for development purposes, you can do so by running this command instead:

```sh
pnpm start --local
```

If you want to run Keycloak standalone (without the script) against the Vite development server, you can set the following environment variables to achieve the same result:

```sh
KC_ACCOUNT_VITE_URL=http://localhost:5173
KC_ADMIN_VITE_URL=http://localhost:5174
KC_FEATURES=login:v2,account:v3,admin-fine-grained-authz,transient-users,oid4vc-vci
```

**All other arguments will be passed through to the underlying Keycloak server.**


