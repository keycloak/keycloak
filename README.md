![Keycloak](https://github.com/keycloak/keycloak-misc/blob/main/logo/logo.svg)

![GitHub Release](https://img.shields.io/github/v/tag/tide-foundation/keycloak-IGA?label=latest%20release)
[![OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/projects/6818/badge)](https://bestpractices.coreinfrastructure.org/projects/6818)
[![CLOMonitor](https://img.shields.io/endpoint?url=https://clomonitor.io/api/projects/cncf/keycloak/badge)](https://clomonitor.io/projects/cncf/keycloak)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/keycloak/keycloak/badge)](https://securityscorecards.dev/viewer/?uri=github.com/keycloak/keycloak)
[![Artifact Hub](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/keycloak-operator)](https://artifacthub.io/packages/olm/community-operators/keycloak-operator)
![GitHub Repo stars](https://img.shields.io/github/stars/keycloak/keycloak?style=flat)
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/keycloak/keycloak)
[![Translation status](https://hosted.weblate.org/widget/keycloak/svg-badge.svg)](docs/translation.md)

# Open Source Identity and Access Management

Add authentication to applications and secure services with minimum effort. No need to deal with storing users or authenticating users.

Keycloak provides user federation, strong authentication, user management, fine-grained authorization, and more.

---

## About this Fork: Keycloak IGA

This is a fork of Keycloak enhanced with **Identity Governance and Administration (IGA)** features. It includes:

- Custom SPI providers for realms, users, roles, and clients
- Easy dev-mode startup with preconfigured extensions

> ⚠️ This fork is intended for testing and evaluation of IGA capabilities. It may eventually be released or contributed upstream.

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

---

## Help and Documentation

* [Documentation](https://www.keycloak.org/documentation.html)
* [User Mailing List](https://groups.google.com/d/forum/keycloak-user) - Mailing list for help and general questions about Keycloak
* Join [#keycloak](https://cloud-native.slack.com/archives/C056HC17KK9) for general questions, or [#keycloak-dev](https://cloud-native.slack.com/archives/C056XU905S6) on Slack for design and development discussions, by creating an account at [https://slack.cncf.io/](https://slack.cncf.io/).


## Reporting Security Vulnerabilities

If you have found a security vulnerability, please look at the [instructions on how to properly report it](https://github.com/keycloak/keycloak/security/policy).


## Reporting an issue

If you believe you have discovered a defect in Keycloak, please open [an issue](https://github.com/keycloak/keycloak/issues).
Please remember to provide a good summary, description as well as steps to reproduce the issue.


## Getting started

To run Keycloak, download the distribution from our [website](https://www.keycloak.org/downloads.html). Unzip and run:

    bin/kc.[sh|bat] start-dev

Alternatively, you can use the Docker image by running:

    docker run quay.io/keycloak/keycloak start-dev
    
For more details refer to the [Keycloak Documentation](https://www.keycloak.org/documentation.html).


## Building from Source

To build from source, refer to the [building and working with the code base](docs/building.md) guide.


### Testing

To run tests, refer to the [running tests](docs/tests.md) guide.


### Writing Tests

To write tests, refer to the [writing tests](docs/tests-development.md) guide.


## Contributing

Before contributing to Keycloak, please read our [contributing guidelines](CONTRIBUTING.md). Participation in the Keycloak project is governed by the [CNCF Code of Conduct](https://github.com/cncf/foundation/blob/main/code-of-conduct.md).

Joining a [community meeting](https://www.keycloak.org/community) is a great way to get involved and help shape the future of Keycloak.

## Other Keycloak Projects

* [Keycloak](https://github.com/keycloak/keycloak) - Keycloak Server and Java adapters
* [Keycloak QuickStarts](https://github.com/keycloak/keycloak-quickstarts) - QuickStarts for getting started with Keycloak
* [Keycloak Node.js Connect](https://github.com/keycloak/keycloak-nodejs-connect) - Node.js adapter for Keycloak


## License

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
