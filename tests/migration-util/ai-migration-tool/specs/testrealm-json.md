# testrealm.json Summary

**Path**: `tests/base/src/test/resources/org/keycloak/tests/testrealm.json`
**Usage**: `@InjectRealm(fromJson = "/org/keycloak/tests/testrealm.json")`

Prefer **minimal `RealmConfig`** over loading this file. Only use `fromJson` when the test genuinely needs many of these resources. See `rules/core.md` Realm Configuration rule.

---

## Users

| Username | Password | OTP | Realm Roles | Groups | Notes |
|---|---|---|---|---|---|
| `test-user@localhost` | `password` | — | `user`, `offline_access` | — | Primary test user |
| `john-doh@localhost` | `password` | — | `user` | — | |
| `keycloak-user@localhost` | `password` | — | `user` | — | |
| `topGroupUser` | `password` | — | — | `/topGroup` | |
| `level2GroupUser` | `password` | — | — | `/topGroup/level2group` | |
| `roleRichUser` | `password` | — | — | `/roleRichGroup/level2group` | |
| `non-duplicate-email-user` | `password` | — | `user`, `offline_access` | — | |
| `user-with-one-configured-otp` | `password` | 1 OTP (secret: `DJmQfC73VGFhw7D4QJ8A`) | — | — | |
| `user-with-two-configured-otp` | `password` | 2 OTPs (secrets: `DJmQfC73VG...`, `ABCQfC73VG...`) | `user` | — | First OTP labeled "first", second unnamed |
| `special>>character` | `<password>` | — | `user`, `offline_access` | — | Special chars in username |

---

## Clients

| ClientId | Secret | Public | Redirect URIs | Notes |
|---|---|---|---|---|
| `test-app` | `password` | No | `localhost:8180/auth/realms/*/app/auth/*` | **Conflicts with default `@InjectOAuthClient`** — use `ref` |
| `root-url-client` | `password` | No | `localhost:8180/foo/bar/*` | |
| `test-app-scope` | `password` | No | `localhost:8180/auth/realms/master/app/*` | Has scope-specific roles |
| `third-party` | `password` | No | `localhost:8180/auth/realms/master/app/*` | |
| `test-app-authz` | `secret` | No | `/test-app-authz/*` | Authorization services |
| `named-test-app` | `password` | No | `localhost:8180/namedapp/base/*` | |
| `var-named-test-app` | `password` | No | `localhost:8180/varnamedapp/base/*` | |
| `direct-grant` | `password` | No | — | No redirects |
| `custom-audience` | `password` | No | — | No redirects |

---

## Realm Roles

`user`, `admin`, `customer-user-premium`, `sample-realm-role`, `attribute-role`, `realm-composite-role`

## Client Roles

- **test-app**: `manage-account`, `customer-user`, `customer-admin`, `sample-client-role`, `customer-admin-composite-role`
- **test-app-scope**: `test-app-allowed-by-scope`, `test-app-disallowed-by-scope`

## Groups

- `topGroup` → subgroups: `level2group`, `level2group2`
- `roleRichGroup` → subgroups: `level2group`, `level2group2`
- `sample-realm-group`

---

## When to use fromJson vs minimal RealmConfig

| Test needs | Approach |
|---|---|
| Just a user with password | Minimal `RealmConfig` with `addUser()` |
| User with OTP credentials | `fromJson` (OTP credential setup is complex) |
| Multiple users with roles | Minimal `RealmConfig` with `realm.addUser().roles()` (no `.build()` needed) |
| `test-app` client specifically | `fromJson` (but use `@InjectOAuthClient(ref = "xxx")` to avoid conflict) |
| Group hierarchy | `fromJson` or build with `addGroup()` |
| Just need admin API access to a realm | Minimal `RealmConfig` — don't load this file |
