/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.authz;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test for GitHub issue #41707: Authorization request for permission matching multiple
 * resources with same URI (wildcard or placeholder) and different scopes causes
 * incorrect access_denied error.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/41707">Issue #41707</a>
 */
public class MultipleResourcesSameUriTest extends AbstractAuthzTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("authz-test")
                .roles(RolesBuilder.create()
                        .realmRole(RoleBuilder.create().name("uma_authorization").build())
                        .realmRole(RoleBuilder.create().name("ROLE_READ").build())
                        .realmRole(RoleBuilder.create().name("ROLE_WRITE").build())
                )
                .user(UserBuilder.create().username("user_read").password("password")
                        .addRoles("uma_authorization", "ROLE_READ"))
                .user(UserBuilder.create().username("user_write").password("password")
                        .addRoles("uma_authorization", "ROLE_WRITE"))
                .user(UserBuilder.create().username("user_both").password("password")
                        .addRoles("uma_authorization", "ROLE_READ", "ROLE_WRITE"))
                .user(UserBuilder.create().username("user_none").password("password")
                        .addRoles("uma_authorization"))
                .client(ClientBuilder.create().clientId("resource-server-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants())
                .build());
    }

    @Before
    public void configureAuthorization() throws Exception {
        ClientResource client = getClient();
        AuthorizationResource authorization = client.authorization();

        // Create scopes
        createScope(authorization, "read");
        createScope(authorization, "write");

        // Create resources with same wildcard URI but different scopes
        createResourceWithUri(authorization, "Resource Read", "/resource/*", "read");
        createResourceWithUri(authorization, "Resource Write", "/resource/*", "write");

        // Create resources with same placeholder URI but different scopes
        createResourceWithUri(authorization, "Resource Read Placeholder", "/item/{id}", "read");
        createResourceWithUri(authorization, "Resource Write Placeholder", "/item/{id}", "write");

        // Create role policies
        createRolePolicy(authorization, "Read Policy", "ROLE_READ");
        createRolePolicy(authorization, "Write Policy", "ROLE_WRITE");

        // Create scope permissions
        createScopePermission(authorization, "Read Permission", "read", "Read Policy");
        createScopePermission(authorization, "Write Permission", "write", "Write Policy");
    }

    /**
     * Test: User with ROLE_READ requests /resource/1#read with wildcard URI
     * Expected: Access GRANTED
     */
    @Test
    public void testUserWithReadRoleCanAccessReadScopeWildcard() {
        AuthzClient authzClient = getAuthzClient();

        AuthorizationRequest request = new AuthorizationRequest();
        request.addPermission("/resource/1", "read");

        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();
        metadata.setPermissionResourceMatchingUri(true);
        metadata.setPermissionResourceFormat("uri");
        request.setMetadata(metadata);

        AuthorizationResponse response = authzClient.authorization("user_read", "password").authorize(request);
        assertNotNull(response.getToken());
    }

    /**
     * Test: User with ROLE_WRITE requests /resource/1#write with wildcard URI
     * Expected: Access GRANTED
     */
    @Test
    public void testUserWithWriteRoleCanAccessWriteScopeWildcard() {
        AuthzClient authzClient = getAuthzClient();

        AuthorizationRequest request = new AuthorizationRequest();
        request.addPermission("/resource/1", "write");

        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();
        metadata.setPermissionResourceMatchingUri(true);
        metadata.setPermissionResourceFormat("uri");
        request.setMetadata(metadata);

        AuthorizationResponse response = authzClient.authorization("user_write", "password").authorize(request);
        assertNotNull(response.getToken());
    }

    /**
     * Test: User with ROLE_READ requests /resource/1#write with wildcard URI
     * Expected: Access DENIED
     */
    @Test
    public void testUserWithReadRoleCannotAccessWriteScopeWildcard() {
        AuthzClient authzClient = getAuthzClient();

        AuthorizationRequest request = new AuthorizationRequest();
        request.addPermission("/resource/1", "write");

        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();
        metadata.setPermissionResourceMatchingUri(true);
        metadata.setPermissionResourceFormat("uri");
        request.setMetadata(metadata);

        try {
            authzClient.authorization("user_read", "password").authorize(request);
            fail("Should fail because user does not have ROLE_WRITE");
        } catch (AuthorizationDeniedException ignore) {
            // Expected
        }
    }

    /**
     * Test: User with ROLE_READ requests /item/123#read with placeholder URI
     * Expected: Access GRANTED
     */
    @Test
    public void testUserWithReadRoleCanAccessReadScopePlaceholder() {
        AuthzClient authzClient = getAuthzClient();

        AuthorizationRequest request = new AuthorizationRequest();
        request.addPermission("/item/123", "read");

        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();
        metadata.setPermissionResourceMatchingUri(true);
        metadata.setPermissionResourceFormat("uri");
        request.setMetadata(metadata);

        AuthorizationResponse response = authzClient.authorization("user_read", "password").authorize(request);
        assertNotNull(response.getToken());
    }

    /**
     * Test: User with ROLE_WRITE requests /item/123#write with placeholder URI
     * Expected: Access GRANTED
     */
    @Test
    public void testUserWithWriteRoleCanAccessWriteScopePlaceholder() {
        AuthzClient authzClient = getAuthzClient();

        AuthorizationRequest request = new AuthorizationRequest();
        request.addPermission("/item/123", "write");

        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();
        metadata.setPermissionResourceMatchingUri(true);
        metadata.setPermissionResourceFormat("uri");
        request.setMetadata(metadata);

        AuthorizationResponse response = authzClient.authorization("user_write", "password").authorize(request);
        assertNotNull(response.getToken());
    }

    /**
     * Test: User with ROLE_READ requests /item/123#write with placeholder URI
     * Expected: Access DENIED
     */
    @Test
    public void testUserWithReadRoleCannotAccessWriteScopePlaceholder() {
        AuthzClient authzClient = getAuthzClient();

        AuthorizationRequest request = new AuthorizationRequest();
        request.addPermission("/item/123", "write");

        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();
        metadata.setPermissionResourceMatchingUri(true);
        metadata.setPermissionResourceFormat("uri");
        request.setMetadata(metadata);

        try {
            authzClient.authorization("user_read", "password").authorize(request);
            fail("Should fail because user does not have ROLE_WRITE");
        } catch (AuthorizationDeniedException ignore) {
            // Expected
        }
    }

    /**
     * Test: User with both ROLE_READ and ROLE_WRITE can access both scopes
     * Expected: Access GRANTED for both
     */
    @Test
    public void testUserWithBothRolesCanAccessBothScopes() {
        AuthzClient authzClient = getAuthzClient();

        // Test read scope
        AuthorizationRequest readRequest = new AuthorizationRequest();
        readRequest.addPermission("/resource/1", "read");

        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();
        metadata.setPermissionResourceMatchingUri(true);
        metadata.setPermissionResourceFormat("uri");
        readRequest.setMetadata(metadata);

        AuthorizationResponse readResponse = authzClient.authorization("user_both", "password").authorize(readRequest);
        assertNotNull(readResponse.getToken());

        // Test write scope
        AuthorizationRequest writeRequest = new AuthorizationRequest();
        writeRequest.addPermission("/resource/1", "write");
        writeRequest.setMetadata(metadata);

        AuthorizationResponse writeResponse = authzClient.authorization("user_both", "password").authorize(writeRequest);
        assertNotNull(writeResponse.getToken());
    }

    /**
     * Test: User without any role cannot access any scope
     * Expected: Access DENIED
     */
    @Test
    public void testUserWithNoRoleCannotAccessAnyScope() {
        AuthzClient authzClient = getAuthzClient();

        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();
        metadata.setPermissionResourceMatchingUri(true);
        metadata.setPermissionResourceFormat("uri");

        // Test read scope
        AuthorizationRequest readRequest = new AuthorizationRequest();
        readRequest.addPermission("/resource/1", "read");
        readRequest.setMetadata(metadata);

        try {
            authzClient.authorization("user_none", "password").authorize(readRequest);
            fail("Should fail because user does not have ROLE_READ");
        } catch (AuthorizationDeniedException ignore) {
            // Expected
        }

        // Test write scope
        AuthorizationRequest writeRequest = new AuthorizationRequest();
        writeRequest.addPermission("/resource/1", "write");
        writeRequest.setMetadata(metadata);

        try {
            authzClient.authorization("user_none", "password").authorize(writeRequest);
            fail("Should fail because user does not have ROLE_WRITE");
        } catch (AuthorizationDeniedException ignore) {
            // Expected
        }
    }

    private void createScope(AuthorizationResource authorization, String name) {
        ScopeRepresentation scope = new ScopeRepresentation(name);
        authorization.scopes().create(scope).close();
    }

    private void createResourceWithUri(AuthorizationResource authorization, String name, String uri, String... scopes) {
        ResourceRepresentation resource = new ResourceRepresentation(name);
        Set<String> uris = new HashSet<>();
        uris.add(uri);
        resource.setUris(uris);
        resource.addScope(scopes);
        authorization.resources().create(resource).close();
    }

    private void createRolePolicy(AuthorizationResource authorization, String name, String role) {
        RolePolicyRepresentation policy = new RolePolicyRepresentation();
        policy.setName(name);
        policy.addRole(role);
        authorization.policies().role().create(policy).close();
    }

    private void createScopePermission(AuthorizationResource authorization, String name, String scope, String policy) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(name);
        permission.addScope(scope);
        permission.addPolicy(policy);
        authorization.permissions().scope().create(permission).close();
    }

    private RealmResource getRealm() {
        try {
            return getAdminClient().realm("authz-test");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create admin client");
        }
    }

    private ClientResource getClient() {
        RealmResource realm = getRealm();
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream()
                .map(representation -> clients.get(representation.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private AuthzClient getAuthzClient() {
        return AuthzClient.create(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"));
    }
}
