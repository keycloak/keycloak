## Keycloak Admin Client

## Features

- TypeScript supported
- Latest Keycloak version supported
- [Complete resource definitions](./src/defs)
- [Well-tested for supported APIs](./test)

## Install

```sh
npm install @keycloak/keycloak-admin-client
```

## Usage

```js
import KcAdminClient from '@keycloak/keycloak-admin-client';

// To configure the client, pass an object to override any of these  options:
// {
//   baseUrl: 'http://127.0.0.1:8080',
//   realmName: 'master',
//   requestOptions: {
//     /* Fetch request options https://developer.mozilla.org/en-US/docs/Web/API/fetch#options */
//   },
// }
const kcAdminClient = new KcAdminClient();

// Authorize with username / password
await kcAdminClient.auth({
  username: 'admin',
  password: 'admin',
  grantType: 'password',
  clientId: 'admin-cli',
  totp: '123456', // optional Time-based One-time Password if OTP is required in authentication flow
});

// List first page of users
const users = await kcAdminClient.users.find({ first: 0, max: 10 });

// find users by attributes
const users = await kcAdminClient.users.find({ q: "phone:123" });

// Override client configuration for all further requests:
kcAdminClient.setConfig({
  realmName: 'another-realm',
});

// This operation will now be performed in 'another-realm' if the user has access.
const groups = await kcAdminClient.groups.find();

// Set a `realm` property to override the realm for only a single operation.
// For example, creating a user in another realm:
await kcAdminClient.users.create({
  realm: 'a-third-realm',
  username: 'username',
  email: 'user@example.com',
});
```

To refresh the access token provided by Keycloak, an OpenID client like [panva/node-openid-client](https://github.com/panva/node-openid-client) can be used like this:

```js
import {Issuer} from 'openid-client';

const keycloakIssuer = await Issuer.discover(
  'http://localhost:8080/realms/master',
);

const client = new keycloakIssuer.Client({
  client_id: 'admin-cli', // Same as `clientId` passed to client.auth()
  token_endpoint_auth_method: 'none', // to send only client_id in the header
});

// Use the grant type 'password'
let tokenSet = await client.grant({
  grant_type: 'password',
  username: 'admin',
  password: 'admin',
});

// Periodically using refresh_token grant flow to get new access token here
setInterval(async () => {
  const refreshToken = tokenSet.refresh_token;
  tokenSet = await client.refresh(refreshToken);
  kcAdminClient.setAccessToken(tokenSet.access_token);
}, 58 * 1000); // 58 seconds
```

In cases where you don't have a refresh token, eg. in a client credentials flow, you can simply call `kcAdminClient.auth` to get a new access token, like this:

```js
const credentials = {
  grantType: 'client_credentials',
  clientId: 'clientId',
  clientSecret: 'some-client-secret-uuid',
};
await kcAdminClient.auth(credentials);

setInterval(() => kcAdminClient.auth(credentials), 58 * 1000); // 58 seconds
```

## Building and running the tests

To build the source do a build:

```bash
pnpm build
```

Start the Keycloak server:

```bash
pnpm server:start
```

If you started your container manually make sure there is an admin user named 'admin' with password 'admin'.
Then start the tests with:

```bash
pnpm test
```

## Supported APIs

### [Realm admin](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_realms_admin_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/realms.spec.ts

- Import a realm from a full representation of that realm (`POST /`)
- Get the top-level representation of the realm (`GET /{realm}`)
- Update the top-level information of the realm (`PUT /{realm}`)
- Delete the realm (`DELETE /{realm}`)
- Partial export of existing realm into a JSON file (`POST /{realm}/partial-export`)
- Get users management permissions (`GET /{realm}/users-management-permissions`)
- Enable users management permissions (`PUT /{realm}/users-management-permissions`)
- Get events (`GET /{realm}/events`)
- Get admin events (`GET /{realm}/admin-events`)
- Remove all user sessions (`POST /{realm}/logout-all`)
- Remove a specific user session (`DELETE /{realm}/sessions/{session}`)
- Get client policies policies (`GET /{realm}/client-policies/policies`)
- Update client policies policies (`PUT /{realm}/client-policies/policies`)
- Get client policies profiles (`GET /{realm}/client-policies/profiles`)
- Update client policies profiles (`PUT /{realm}/client-policies/profiles`)
- Get a group by path (`GET /{realm}/group-by-path/{path}`)
### [Role](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_roles_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/roles.spec.ts

- Create a new role for the realm (`POST /{realm}/roles`)
- Get all roles for the realm (`GET /{realm}/roles`)
- Get a role by name (`GET /{realm}/roles/{role-name}`)
- Update a role by name (`PUT /{realm}/roles/{role-name}`)
- Delete a role by name (`DELETE /{realm}/roles/{role-name}`)
- Get all users in a role by name for the realm (`GET /{realm}/roles/{role-name}/users`)

### [Roles (by ID)](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_roles_by_id_resource)

- Get a specific role (`GET /{realm}/roles-by-id/{role-id}`)
- Update the role (`PUT /{realm}/roles-by-id/{role-id}`)
- Delete the role (`DELETE /{realm}/roles-by-id/{role-id}`)
- Make the role a composite role by associating some child roles(`POST /{realm}/roles-by-id/{role-id}/composites`)
- Get role’s children Returns a set of role’s children provided the role is a composite. (`GET /{realm}/roles-by-id/{role-id}/composites`)
- Remove a set of roles from the role’s composite (`DELETE /{realm}/roles-by-id/{role-id}/composites`)
- Get client-level roles for the client that are in the role’s composite (`GET /{realm}/roles-by-id/{role-id}/composites/clients/{client}`)
- Get realm-level roles that are in the role’s composite (`GET /{realm}/roles-by-id/{role-id}/composites/realm`)

### [User](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_users_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/users.spec.ts

- Create a new user (`POST /{realm}/users`)
- Get users Returns a list of users, filtered according to query parameters (`GET /{realm}/users`)
- Get representation of the user (`GET /{realm}/users/{id}`)
- Update the user (`PUT /{realm}/users/{id}`)
- Delete the user (`DELETE /{realm}/users/{id}`)
- Count users (`GET /{realm}/users/count`)
- Send a update account email to the user An email contains a link the user can click to perform a set of required actions. (`PUT /{realm}/users/{id}/execute-actions-email`)
- Get user groups (`GET /{realm}/users/{id}/groups`)
- Add user to group (`PUT /{realm}/users/{id}/groups/{groupId}`)
- Delete user from group (`DELETE /{realm}/users/{id}/groups/{groupId}`)
- Remove TOTP from the user (`PUT /{realm}/users/{id}/remove-totp`)
- Set up a temporary password for the user User will have to reset the temporary password next time they log in. (`PUT /{realm}/users/{id}/reset-password`)
- Send an email-verification email to the user An email contains a link the user can click to verify their email address. (`PUT /{realm}/users/{id}/send-verify-email`)
- Update a credential label for a user (`PUT /{realm}/users/{id}/credentials/{credentialId}/userLabel`)
- Move a credential to a position behind another credential (`POST /{realm}/users/{id}/credentials/{credentialId}/moveAfter/{newPreviousCredentialId}`)
- Move a credential to a first position in the credentials list of the user (`PUT /{realm}/users/{id}/credentials/{credentialId}/moveToFirst`)

### User group-mapping

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/users.spec.ts#L178

- Add user to group (`PUT /{id}/groups/{groupId}`)
- List all user groups (`GET /{id}/groups`)
- Count user groups (`GET /{id}/groups/count`)
- Remove user from group (`DELETE /{id}/groups/{groupId}`)

### User role-mapping

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/users.spec.ts#L352

- Get user role-mappings (`GET /{realm}/users/{id}/role-mappings`)
- Add realm-level role mappings to the user (`POST /{realm}/users/{id}/role-mappings/realm`)
- Get realm-level role mappings (`GET /{realm}/users/{id}/role-mappings/realm`)
- Delete realm-level role mappings (`DELETE /{realm}/users/{id}/role-mappings/realm`)
- Get realm-level roles that can be mapped (`GET /{realm}/users/{id}/role-mappings/realm/available`)
- Get effective realm-level role mappings This will recurse all composite roles to get the result. (`GET /{realm}/users/{id}/role-mappings/realm/composite`)

### [Group](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_groups_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/groups.spec.ts

- Create (`POST /{realm}/groups`)
- List (`GET /{realm}/groups`)
- Get one (`GET /{realm}/groups/{id}`)
- Update (`PUT /{realm}/groups/{id}`)
- Delete (`DELETE /{realm}/groups/{id}`)
- Count (`GET /{realm}/groups/count`)
- List members (`GET /{realm}/groups/{id}/members`)
- Set or create child (`POST /{realm}/groups/{id}/children`)
- Get children (`GET /{realm}/groups/{id}/children`)

### Group role-mapping

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/groups.spec.ts#L103

- Get group role-mappings (`GET /{realm}/groups/{id}/role-mappings`)
- Add realm-level role mappings to the group (`POST /{realm}/groups/{id}/role-mappings/realm`)
- Get realm-level role mappings (`GET /{realm}/groups/{id}/role-mappings/realm`)
- Delete realm-level role mappings (`DELETE /{realm}/groups/{id}/role-mappings/realm`)
- Get realm-level roles that can be mapped (`GET /{realm}/groups/{id}/role-mappings/realm/available`)
- Get effective realm-level role mappings This will recurse all composite roles to get the result. (`GET /{realm}/groups/{id}/role-mappings/realm/composite`)

### [Client](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_clients_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clients.spec.ts

- Create a new client (`POST /{realm}/clients`)
- Get clients belonging to the realm (`GET /{realm}/clients`)
- Get representation of the client (`GET /{realm}/clients/{id}`)
- Update the client (`PUT /{realm}/clients/{id}`)
- Delete the client (`DELETE /{realm}/clients/{id}`)

### [Client roles](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_roles_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clients.spec.ts

- Create a new role for the client (`POST /{realm}/clients/{id}/roles`)
- Get all roles for the client (`GET /{realm}/clients/{id}/roles`)
- Get a role by name (`GET /{realm}/clients/{id}/roles/{role-name}`)
- Update a role by name (`PUT /{realm}/clients/{id}/roles/{role-name}`)
- Delete a role by name (`DELETE /{realm}/clients/{id}/roles/{role-name}`)

### [Client role-mapping for group](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_client_role_mappings_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/groups.spec.ts#L192

- Add client-level roles to the group role mapping (`POST /{realm}/groups/{id}/role-mappings/clients/{client}`)
- Get client-level role mappings for the group (`GET /{realm}/groups/{id}/role-mappings/clients/{client}`)
- Delete client-level roles from group role mapping (`DELETE /{realm}/groups/{id}/role-mappings/clients/{client}`)
- Get available client-level roles that can be mapped to the group (`GET /{realm}/groups/{id}/role-mappings/clients/{client}/available`)
- Get effective client-level role mappings This will recurse all composite roles to get the result. (`GET /{realm}/groups/{id}/role-mappings/clients/{client}/composite`)

### [Client role-mapping for user](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_client_role_mappings_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/users.spec.ts#L352

- Add client-level roles to the user role mapping (`POST /{realm}/users/{id}/role-mappings/clients/{client}`)
- Get client-level role mappings for the user (`GET /{realm}/users/{id}/role-mappings/clients/{client}`)
- Delete client-level roles from user role mapping (`DELETE /{realm}/users/{id}/role-mappings/clients/{client}`)
- Get available client-level roles that can be mapped to the user (`GET /{realm}/users/{id}/role-mappings/clients/{client}/available`)
- Get effective client-level role mappings This will recurse all composite roles to get the result. (`GET /{realm}/users/{id}/role-mappings/clients/{client}/composite`)

### [Client Attribute Certificate](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_client_attribute_certificate_resource)

- Get key info (`GET /{realm}/clients/{id}/certificates/{attr}`)
- Get a keystore file for the client, containing private key and public certificate (`POST /{realm}/clients/{id}/certificates/{attr}/download`)
- Generate a new certificate with new key pair (`POST /{realm}/clients/{id}/certificates/{attr}/generate`)
- Generate a new keypair and certificate, and get the private key file Generates a keypair and certificate and serves the private key in a specified keystore format. (`POST /{realm}/clients/{id}/certificates/{attr}/generate-and-download`)
- Upload certificate and eventually private key (`POST /{realm}/clients/{id}/certificates/{attr}/upload`)
- Upload only certificate, not private key (`POST /{realm}/clients/{id}/certificates/{attr}/upload-certificate`)

### [Identity Providers](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_identity_providers_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/idp.spec.ts

- Create a new identity provider (`POST /{realm}/identity-provider/instances`)
- Get identity providers (`GET /{realm}/identity-provider/instances`)
- Get the identity provider (`GET /{realm}/identity-provider/instances/{alias}`)
- Update the identity provider (`PUT /{realm}/identity-provider/instances/{alias}`)
- Delete the identity provider (`DELETE /{realm}/identity-provider/instances/{alias}`)
- Find identity provider factory (`GET /{realm}/identity-provider/providers/{providerId}`)
- Create a new identity provider mapper (`POST /{realm}/identity-provider/instances/{alias}/mappers`)
- Get identity provider mappers (`GET /{realm}/identity-provider/instances/{alias}/mappers`)
- Get the identity provider mapper (`GET /{realm}/identity-provider/instances/{alias}/mappers/{id}`)
- Update the identity provider mapper (`PUT /{realm}/identity-provider/instances/{alias}/mappers/{id}`)
- Delete the identity provider mapper (`DELETE /{realm}/identity-provider/instances/{alias}/mappers/{id}`)
- Find the identity provider mapper types (`GET /{realm}/identity-provider/instances/{alias}/mapper-types`)

### [Client Scopes](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_client_scopes_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clientScopes.spec.ts

- Create a new client scope (`POST /{realm}/client-scopes`)
- Get client scopes belonging to the realm (`GET /{realm}/client-scopes`)
- Get representation of the client scope (`GET /{realm}/client-scopes/{id}`)
- Update the client scope (`PUT /{realm}/client-scopes/{id}`)
- Delete the client scope (`DELETE /{realm}/client-scopes/{id}`)

### [Client Scopes for realm](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_client_scopes_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clientScopes.spec.ts

- Get realm default client scopes (`GET /{realm}/default-default-client-scopes`)
- Add realm default client scope (`PUT /{realm}/default-default-client-scopes/{id}`)
- Delete realm default client scope (`DELETE /{realm}/default-default-client-scopes/{id}`)
- Get realm optional client scopes (`GET /{realm}/default-optional-client-scopes`)
- Add realm optional client scope (`PUT /{realm}/default-optional-client-scopes/{id}`)
- Delete realm optional client scope (`DELETE /{realm}/default-optional-client-scopes/{id}`)

### [Client Scopes for client](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_client_scopes_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clientScopes.spec.ts

- Get default client scopes (`GET /{realm}/clients/{id}/default-client-scopes`)
- Add default client scope (`PUT /{realm}/clients/{id}/default-client-scopes/{clientScopeId}`)
- Delete default client scope (`DELETE /{realm}/clients/{id}/default-client-scopes/{clientScopeId}`)
- Get optional client scopes (`GET /{realm}/clients/{id}/optional-client-scopes`)
- Add optional client scope (`PUT /{realm}/clients/{id}/optional-client-scopes/{clientScopeId}`)
- Delete optional client scope (`DELETE /{realm}/clients/{id}/optional-client-scopes/{clientScopeId}`)

### [Scope Mappings for client scopes](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_scope_mappings_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clientScopes.spec.ts

- Get all scope mappings for the client (`GET /{realm}/client-scopes/{id}/scope-mappings`)
- Add client-level roles to the client’s scope (`POST /{realm}/client-scopes/{id}/scope-mappings/clients/{client}`)
- Get the roles associated with a client’s scope (`GET /{realm}/client-scopes/{id}/scope-mappings/clients/{client}`)
- The available client-level roles (`GET /{realm}/client-scopes/{id}/scope-mappings/clients/{client}/available`)
- Get effective client roles (`GET /{realm}/client-scopes/{id}/scope-mappings/clients/{client}/composite`)
- Remove client-level roles from the client’s scope. (`DELETE /{realm}/client-scopes/{id}/scope-mappings/clients/{client}`)
- Add a set of realm-level roles to the client’s scope (`POST /{realm}/client-scopes/{id}/scope-mappings/realm`)
- Get realm-level roles associated with the client’s scope (`GET /{realm}/client-scopes/{id}/scope-mappings/realm`)
- Remove a set of realm-level roles from the client’s scope (`DELETE /{realm}/client-scopes/{id}/scope-mappings/realm`)
- Get realm-level roles that are available to attach to this client’s scope (`GET /{realm}/client-scopes/{id}/scope-mappings/realm/available`)
- Get effective realm-level roles associated with the client’s scope (`GET /{realm}/client-scopes/{id}/scope-mappings/realm/composite`)

### [Scope Mappings for clients](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_scope_mappings_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clientScopes.spec.ts

- Get all scope mappings for the client (`GET /{realm}/clients/{id}/scope-mappings`)
- Add client-level roles to the client’s scope (`POST /{realm}/clients/{id}/scope-mappings/clients/{client}`)
- Get the roles associated with a client’s scope (`GET /{realm}/clients/{id}/scope-mappings/clients/{client}`)
- Remove client-level roles from the client’s scope. (`DELETE /{realm}/clients/{id}/scope-mappings/clients/{client}`)
- The available client-level roles (`GET /{realm}/clients/{id}/scope-mappings/clients/{client}/available`)
- Get effective client roles (`GET /{realm}/clients/{id}/scope-mappings/clients/{client}/composite`)
- Add a set of realm-level roles to the client’s scope (`POST /{realm}/clients/{id}/scope-mappings/realm`)
- Get realm-level roles associated with the client’s scope (`GET /{realm}/clients/{id}/scope-mappings/realm`)
- Remove a set of realm-level roles from the client’s scope (`DELETE /{realm}/clients/{id}/scope-mappings/realm`)
- Get realm-level roles that are available to attach to this client’s scope (`GET /{realm}/clients/{id}/scope-mappings/realm/available`)
- Get effective realm-level roles associated with the client’s scope (`GET /{realm}/clients/{id}/scope-mappings/realm/composite`)

### [Protocol Mappers for client scopes](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_protocol_mappers_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clientScopes.spec.ts

- Create multiple mappers (`POST /{realm}/client-scopes/{id}/protocol-mappers/add-models`)
- Create a mapper (`POST /{realm}/client-scopes/{id}/protocol-mappers/models`)
- Get mappers (`GET /{realm}/client-scopes/{id}/protocol-mappers/models`)
- Get mapper by id (`GET /{realm}/client-scopes/{id}/protocol-mappers/models/{mapperId}`)
- Update the mapper (`PUT /{realm}/client-scopes/{id}/protocol-mappers/models/{mapperId}`)
- Delete the mapper (`DELETE /{realm}/client-scopes/{id}/protocol-mappers/models/{mapperId}`)
- Get mappers by name for a specific protocol (`GET /{realm}/client-scopes/{id}/protocol-mappers/protocol/{protocol}`)

### [Protocol Mappers for clients](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_protocol_mappers_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clients.spec.ts

- Create multiple mappers (`POST /{realm}/clients/{id}/protocol-mappers/add-models`)
- Create a mapper (`POST /{realm}/clients/{id}/protocol-mappers/models`)
- Get mappers (`GET /{realm}/clients/{id}/protocol-mappers/models`)
- Get mapper by id (`GET /{realm}/clients/{id}/protocol-mappers/models/{mapperId}`)
- Update the mapper (`PUT /{realm}/clients/{id}/protocol-mappers/models/{mapperId}`)
- Delete the mapper (`DELETE /{realm}/clients/{id}/protocol-mappers/models/{mapperId}`)
- Get mappers by name for a specific protocol (`GET /{realm}/clients/{id}/protocol-mappers/protocol/{protocol}`)

### [Component]()

Supported for [user federation](https://www.keycloak.org/docs/latest/server_admin/index.html#_user-storage-federation). Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/components.spec.ts

- Create (`POST /{realm}/components`)
- List (`GET /{realm}/components`)
- Get (`GET /{realm}/components/{id}`)
- Update (`PUT /{realm}/components/{id}`)
- Delete (`DELETE /{realm}/components/{id}`)

### [Sessions for clients]()

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clients.spec.ts

- List user sessions for a specific client (`GET /{realm}/clients/{id}/user-sessions`)
- List offline sessions for a specific client (`GET /{realm}/clients/{id}/offline-sessions`)
- Get user session count for a specific client (`GET /{realm}/clients/{id}/session-count`)
- List offline session count for a specific client (`GET /{realm}/clients/{id}/offline-session-count`)

### [Authentication Management: Required actions](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_authentication_management_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/authenticationManagement.spec.ts

- Register a new required action (`POST /{realm}/authentication/register-required-action`)
- Get required actions. Returns a list of required actions. (`GET /{realm}/authentication/required-actions`)
- Get required action for alias (`GET /{realm}/authentication/required-actions/{alias}`)
- Update required action (`PUT /{realm}/authentication/required-actions/{alias}`)
- Delete required action (`DELETE /{realm}/authentication/required-actions/{alias}`)
- Lower required action’s priority (`POST /{realm}/authentication/required-actions/{alias}/lower-priority`)
- Raise required action’s priority (`POST /{realm}/authentication/required-actions/{alias}/raise-priority`)
- Get unregistered required actions Returns a list of unregistered required actions. (`GET /{realm}/authentication/unregistered-required-actions`)

### [Authorization: Permission](https://www.keycloak.org/docs/8.0/authorization_services/#_overview)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clients.spec.ts

- Create permission (`POST /{realm}/clients/{id}/authz/resource-server/permission/{type}`)
- Get permission (`GET /{realm}/clients/{id}/authz/resource-server/permission/{type}/{permissionId}`)
- Update permission (`PUT /{realm}/clients/{id}/authz/resource-server/permission/{type}/{permissionId}`)
- Delete permission (`DELETE /{realm}/clients/{id}/authz/resource-server/permission/{type}/{permissionId}`)

### [Authorization: Policy](https://www.keycloak.org/docs/8.0/authorization_services/#_overview)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/clients.spec.ts

- Create policy (`POST /{realm}/clients/{id}/authz/resource-server/policy/{type}`)
- Get policy (`GET /{realm}/clients/{id}/authz/resource-server/policy/{type}/{policyId}`)
- Get policy by name (`GET /{realm}/clients/{id}/authz/resource-server/policy/search`)
- Update policy (`PUT /{realm}/clients/{id}/authz/resource-server/policy/{type}/{policyId}`)
- Delete policy (`DELETE /{realm}/clients/{id}/authz/resource-server/policy/{policyId}`)

### [Attack Detection](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_attack_detection_resource)

Demo code: https://github.com/keycloak/keycloak/blob/main/js/libs/keycloak-admin-client/test/attackDetection.spec.ts

- Clear any user login failures for all users This can release temporary disabled users (`DELETE /{realm}/attack-detection/brute-force/users`)
- Get status of a username in brute force detection (`GET /{realm}/attack-detection/brute-force/users/{userId}`)
- Clear any user login failures for the user This can release temporary disabled user (`DELETE /{realm}/attack-detection/brute-force/users/{userId}`)

## Not yet supported

- [Authentication Management](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_authentication_management_resource)
- [Client Initial Access](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_client_initial_access_resource)
- [Client Registration Policy](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_client_registration_policy_resource)
- [Key](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_key_resource)
- [User Storage Provider](https://www.keycloak.org/docs-api/20.0.2/rest-api/index.html#_user_storage_provider_resource)

## Maintainers

This repo is originally developed by [Canner](https://www.cannercms.com) and [InfuseAI](https://infuseai.io) before being transferred under keycloak organization.
