# Keycloak JavaScript

This directory contains the UIs and related libraries of the Keycloak project written in JavaScript (and TypeScript).

## Directory structure

    ├── apps
    │   ├── account-ui                 # Account UI for account management i.e controlling password and account access, tracking and managing permissions
    │   ├── admin-ui                   # Admin UI for handling login, registration, administration, and account management
    │   └── keycloak-server            # Keycloak server for local development of UIs
    ├── libs
    │   └── keycloak-admin-client      # Keycloak Admin Client library for Keycloak REST API
    ├── ...

## Data processing

Red Hat may process information including business contact information and code contributions as part of its participation in the project, data is processed in accordance with [Red Hat Privacy Statement](https://www.redhat.com/en/about/privacy-policy).

To speed up the build process, the following build flag can be used to disable the processing of these modules:

    -Dskip.npm
