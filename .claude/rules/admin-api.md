---
paths:
  - "services/src/main/java/org/keycloak/services/resources/admin/***"
---

## Purpose

These are the rules to check when analyzing code related to the `Admin API` of Keycloak.

## Rules

- Make sure endpoints are enforcing access to operations on a realm resource type using the appropriate admin role and FGAP permissions.
- Inform any sensitive data exposed in response payloads without an access control check or with an insufficient access control check.