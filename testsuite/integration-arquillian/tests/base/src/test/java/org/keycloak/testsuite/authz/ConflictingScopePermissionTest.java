/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.EntitlementResponse;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.representation.ScopeRepresentation;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConflictingScopePermissionTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("authz-test")
                .user(UserBuilder.create().username("marta").password("password"))
                .user(UserBuilder.create().username("kolo").password("password"))
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
        createResourcesAndScopes();

        RealmResource realm = getRealm();
        ClientResource client = getClient(realm);

        createPolicies(realm, client);
        createPermissions(client);
    }

    /**
     * <p>Scope Read on Resource A has two conflicting permissions. One is granting access for Marta and the other for Kolo.
     *
     * <p>Scope Read should not be granted for Marta.
     */
    @Test
    public void testMartaCanAccessResourceAWithExecuteAndWrite() {
        List<Permission> permissions = getEntitlements("marta", "password");

        for (Permission permission : new ArrayList<>(permissions)) {
            String resourceSetName = permission.getResourceSetName();

            switch (resourceSetName) {
                case "Resource A":
                    assertEquals(2, permission.getScopes().size());
                    assertTrue(permission.getScopes().contains("execute"));
                    assertTrue(permission.getScopes().contains("write"));
                    permissions.remove(permission);
                    break;
                case "Resource C":
                    assertEquals(3, permission.getScopes().size());
                    assertTrue(permission.getScopes().contains("execute"));
                    assertTrue(permission.getScopes().contains("write"));
                    assertTrue(permission.getScopes().contains("read"));
                    permissions.remove(permission);
                    break;
                default:
                    fail("Unexpected permission for resource [" + resourceSetName + "]");
            }
        }

        assertTrue(permissions.isEmpty());
    }

    private List<Permission> getEntitlements(String username, String password) {
        AuthzClient authzClient = getAuthzClient();
        EntitlementResponse response = authzClient.entitlement(authzClient.obtainAccessToken(username, password).getToken()).getAll("resource-server-test");
        AccessToken accessToken;

        try {
            accessToken = new JWSInput(response.getRpt()).readJsonContent(AccessToken.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize RPT", cause);
        }

        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull("RPT does not contain any authorization data", authorization);

        return authorization.getPermissions();
    }

    private RealmResource getRealm() throws Exception {
        return AdminClientUtil.createAdminClient().realm("authz-test");
    }

    private ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private void createPermissions(ClientResource client) throws IOException {
        createResourcePermission("Resource C Only For Marta Permission", "Resource C", Arrays.asList("Only Marta Policy"), client);
        createScopePermission("Resource A Scope Read Only For Marta Permission", "Resource A", Arrays.asList("read"), Arrays.asList("Only Marta Policy"), client);
        createScopePermission("Resource A Scope Read Only For Kolo Permission", "Resource A", Arrays.asList("read"), Arrays.asList("Only Kolo Policy"), client);
    }

    private void createPolicies(RealmResource realm, ClientResource client) throws IOException {
        createUserPolicy("Only Marta Policy", realm, client, "marta");
        createUserPolicy("Only Kolo Policy", realm, client, "kolo");
    }

    private void createResourcesAndScopes() throws IOException {
        AuthzClient authzClient = getAuthzClient();
        Set<ScopeRepresentation> scopes = new HashSet<>();

        scopes.add(new ScopeRepresentation("read"));
        scopes.add(new ScopeRepresentation("write"));
        scopes.add(new ScopeRepresentation("execute"));

        List<ResourceRepresentation> resources = new ArrayList<>();

        resources.add(new ResourceRepresentation("Resource A", scopes));
        resources.add(new ResourceRepresentation("Resource B", scopes));
        resources.add(new ResourceRepresentation("Resource C", scopes));

        resources.forEach(resource -> authzClient.protection().resource().create(resource));
    }

    private void createUserPolicy(String name, RealmResource realm, ClientResource client, String username) throws IOException {
        String userId = realm.users().search(username).stream().map(representation -> representation.getId()).findFirst().orElseThrow(() -> new RuntimeException("Expected user [userId]"));

        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setName(name);
        representation.setType("user");

        Map<String, String> config = new HashMap<>();

        config.put("users", JsonSerialization.writeValueAsString(new String[] {userId}));

        representation.setConfig(config);

        client.authorization().policies().create(representation);
    }

    private void createResourcePermission(String name, String resourceName, List<String> policies, ClientResource client) throws IOException {
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName(name);
        representation.addResource(resourceName);
        representation.addPolicy(policies.toArray(new String[policies.size()]));

        client.authorization().permissions().resource().create(representation);
    }

    private void createScopePermission(String name, String resourceName, List<String> scopes, List<String> policies, ClientResource client) throws IOException {
        AuthorizationResource authorization = client.authorization();
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName(name);

        if (resourceName != null) {
            representation.addResource(resourceName);
        }

        representation.addScope(scopes.toArray(new String[scopes.size()]));
        representation.addPolicy(scopes.toArray(new String[policies.size()]));

        authorization.permissions().scope().create(representation);
    }

    private AuthzClient getAuthzClient() {
        try {
            return AuthzClient.create(JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"), Configuration.class));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to create authz client", cause);
        }
    }
}
