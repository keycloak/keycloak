![Keycloak IGA Header](https://github.com/user-attachments/assets/bb1b7336-4566-49bb-85d3-4fbaad5fdf0e)

---

## About this Fork: Keycloak IGA

This is a fork of Open Source Identity and Access Management Keycloak, enhanced with **Identity Governance and Administration (IGA)** features. It includes:

- Custom SPI providers for realms, users, roles, and clients
- Easy dev-mode startup with preconfigured extensions

> ⚠️ This fork is intended for testing and evaluation of IGA capabilities. It may eventually be released or contributed upstream.

## Rational

Please see [Community Discussion: Introduce foundational Identity Governance & Administration (IGA) capabilities into Keycloak](https://github.com/keycloak/keycloak/discussions/41350)


### Getting Started (Keycloak IGA)

> **Supported branches:**, `26.2.5-IGA`
>
> The latest supported release is `26.2.5-IGA`. Future version tags may be supported as well.

#### 1. Clone the repository

```bash
git clone --branch 26.2.5-IGA --single-branch https://github.com/tide-foundation/keycloak-IGA.git
cd keycloak-IGA
```

#### 2. Run the build and setup script

```bash
./build-kc-with-iga.sh
```

This script will:

- Build all necessary IGA JARs
- Build Keycloak with the required Quarkus modules
- Inject the JARs into the correct `providers/` folder
- Generate a `conf/keycloak.conf` file for IGA SPI configuration
- Display the path to the final `kc.sh` script to run

#### 3. Start Keycloak in development mode

After the script finishes, it will print something like:

```bash
./tmp_kc/keycloak-<version>/bin/kc.sh start-dev
```

Simply copy and run the printed command to launch Keycloak with your IGA extensions active.

---

## Keycloak IGA Guide

This fork introduces a set of [Keycloak SPI](https://www.keycloak.org/docs/latest/server_development/#_providers) extensions to support IGA (Identity Governance and Administration) functionality.

### Features

- **Custom User Provider** (`tide-User-Provider`)
- **Custom Realm Provider** (`tideRealmProvider`)
- **Client and Role providers** for advanced governance

### Configuration

The following is automatically set in `conf/keycloak.conf`:

```properties
spi-user-provider=tide-User-Provider
spi-realm-provider=tideRealmProvider
spi-client-provider=tideClientProvider
spi-role-provider=tide-role-provider
```

The build script ensures these JARs are installed into `providers/` of the distribution.

---

## Walkthrough / demo  

[![Native Keycloak IGA Walkthrough](http://img.youtube.com/vi/BrTBgFM7Lq0/0.jpg)](https://www.youtube.com/watch?v=BrTBgFM7Lq0 "Native Keycloak IGA Walkthrough")

---

## IGA Role & Access Governance Model

This fork introduces a custom JPA provider to manage identity lifecycle and governance for:

- **Users**
- **Roles**
- **Composite roles**
- **Role mappings**
- **Clients**

Unlike default Keycloak behavior where access updates take effect immediately, this fork introduces a **drafting and approval workflow** to improve security and oversight.

### JPA Structure Enhancements

The schema mirrors standard Keycloak tables with added governance fields such as:

- `draftStatus` (`pending`, `approved`, `rejected`, `active`)


### Role Assignment Rules

- Only **active** roles are included in the user's access token.
- You can:
  - Assign roles to users
  - Remove roles from users
  - Enable/disable full scope on a client
  - Add composite role relationships

### Drafting Triggers

Changes requiring approval include:

- Assigning/removing active roles from users
- Deleting roles already assigned to users
- Enabling/disabling full-scope on clients if users exist in that realm

These go through an approval process instead of immediate execution.

### Authority Delegation & Quorum

- Initially, the **master admin** has approval rights.
- The master admin can delegate realm control by assigning the `realm-admin` role.
- Once delegated, the master admin relinquishes approval rights.
- Realm-admins can assign other realm-admins.

#### 🗳 Quorum Approval

- A configurable quorum is enforced (currently: 70%).
- If 3 realm-admins exist, any change requires approval from at least 2.
- Once quorum is met, any realm-admin can **commit** the change.

## License

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
