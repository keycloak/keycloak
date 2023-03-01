# Keycloak JavaScript

This directory contains an [NPM workspace](https://docs.npmjs.com/cli/v9/using-npm/workspaces) with JavaScript (and TypeScript) code related to the UIs and libraries of the Keycloak project.

## Repository structure

    ├── apps
    │   ├── account-ui                 # Account UI for account management i.e controlling password and account access, tracking and managing permissions
    │   └── admin-ui                   # Admin UI for handling login, registration, administration, and account management
    ├── libs
    │   ├── keycloak-admin-client      # Keycloak Admin Client library for Keycloak REST API
    │   ├── keycloak-js                # Keycloak JS library for securing HTML5/JavaScript applications
    │   └── keycloak-masthead          # Keycloak Masthead library for an easy way to bring applications into the Keycloak ecosystem, allow users to access
    │                                  # and manage security for those applications and manage authorization of resources
