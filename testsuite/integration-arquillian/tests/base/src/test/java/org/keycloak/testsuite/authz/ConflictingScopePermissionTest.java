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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConflictingScopePermissionTest extends AbstractAuthzTest {

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
        RealmResource realm = getRealm();
        ClientResource client = getClient(realm);

        if (client.authorization().resources().findByName("Resource A").isEmpty()) {
            createResourcesAndScopes();
            createPolicies(realm, client);
            createPermissions(client);
        }
    }

    /**
     * <p>Scope Read on Resource A has two conflicting permissions. One is granting access for Marta and the other for Kolo.
     *
     * <p>Scope Read should not be granted for Marta.
     */
    @Test
    public void testMartaCanAccessResourceAWithExecuteAndWrite() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();
        ResourceServerRepresentation settings = authorization.getSettings();

        settings.setPolicyEnforcementMode(PolicyEnforcementMode.ENFORCING);
        settings.setDecisionStrategy(DecisionStrategy.UNANIMOUS);

        authorization.update(settings);

        Collection<Permission> permissions = getEntitlements("marta", "password");

        assertEquals(1, permissions.size());

        for (Permission permission : new ArrayList<>(permissions)) {
            String resourceSetName = permission.getResourceName();

            switch (resourceSetName) {
                case "Resource A":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write"));
                    permissions.remove(permission);
                    break;
                case "Resource C":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write", "read"));
                    permissions.remove(permission);
                    break;
                default:
                    fail("Unexpected permission for resource [" + resourceSetName + "]");
            }
        }

        assertTrue(permissions.isEmpty());
    }

    /**
     * <p>Scope Read on Resource A has two conflicting permissions. One is granting access for Marta and the other for Kolo.
     *
     * <p>Scope Read should not be granted for Marta.
     */
    @Test
    public void testMartaCanAccessResourceA() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();
        ResourceServerRepresentation settings = authorization.getSettings();

        settings.setPolicyEnforcementMode(PolicyEnforcementMode.ENFORCING);
        settings.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        authorization.update(settings);

        Collection<Permission> permissions = getEntitlements("marta", "password");

        assertEquals(1, permissions.size());

        for (Permission permission : new ArrayList<>(permissions)) {
            String resourceSetName = permission.getResourceName();

            switch (resourceSetName) {
                case "Resource A":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write", "read"));
                    permissions.remove(permission);
                    break;
                case "Resource C":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write", "read"));
                    permissions.remove(permission);
                    break;
                default:
                    fail("Unexpected permission for resource [" + resourceSetName + "]");
            }
        }

        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testWithPermissiveMode() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();
        ResourceServerRepresentation settings = authorization.getSettings();

        settings.setPolicyEnforcementMode(PolicyEnforcementMode.PERMISSIVE);
        settings.setDecisionStrategy(DecisionStrategy.UNANIMOUS);

        authorization.update(settings);

        Collection<Permission> permissions = getEntitlements("marta", "password");

        assertEquals(3, permissions.size());

        for (Permission permission : new ArrayList<>(permissions)) {
            String resourceSetName = permission.getResourceName();

            switch (resourceSetName) {
                case "Resource A":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write"));
                    permissions.remove(permission);
                    break;
                case "Resource C":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write", "read"));
                    permissions.remove(permission);
                    break;
                case "Resource B":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write", "read"));
                    permissions.remove(permission);
                    break;
                default:
                    fail("Unexpected permission for resource [" + resourceSetName + "]");
            }
        }

        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testWithDisabledMode() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();
        ResourceServerRepresentation settings = authorization.getSettings();

        settings.setPolicyEnforcementMode(PolicyEnforcementMode.DISABLED);
        settings.setDecisionStrategy(DecisionStrategy.UNANIMOUS);

        authorization.update(settings);

        Collection<Permission> permissions = getEntitlements("marta", "password");

        assertEquals(3, permissions.size());

        for (Permission permission : new ArrayList<>(permissions)) {
            String resourceSetName = permission.getResourceName();

            switch (resourceSetName) {
                case "Resource A":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write", "read"));
                    permissions.remove(permission);
                    break;
                case "Resource C":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write", "read"));
                    permissions.remove(permission);
                    break;
                case "Resource B":
                    assertThat(permission.getScopes(), containsInAnyOrder("execute", "write", "read"));
                    permissions.remove(permission);
                    break;
                default:
                    fail("Unexpected permission for resource [" + resourceSetName + "]");
            }
        }

        assertTrue(permissions.isEmpty());
    }

    private Collection<Permission> getEntitlements(String username, String password) {
        AuthzClient authzClient = getAuthzClient();
        AuthorizationResponse response = authzClient.authorization(username, password).authorize();
        AccessToken accessToken;

        try {
            accessToken = new JWSInput(response.getToken()).readJsonContent(AccessToken.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize RPT", cause);
        }

        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull("RPT does not contain any authorization data", authorization);

        return authorization.getPermissions();
    }

    private RealmResource getRealm() throws Exception {
        return adminClient.realm("authz-test");
    }

    private ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private void createPermissions(ClientResource client) throws IOException {
        createResourcePermission("Resource A Only For Marta Permission", "Resource A", Arrays.asList("Only Marta Policy"), client);
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

        client.authorization().policies().create(representation).close();
    }

    private void createResourcePermission(String name, String resourceName, List<String> policies, ClientResource client) throws IOException {
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName(name);
        representation.addResource(resourceName);
        representation.addPolicy(policies.toArray(new String[policies.size()]));

        client.authorization().permissions().resource().create(representation).close();
    }

    private void createScopePermission(String name, String resourceName, List<String> scopes, List<String> policies, ClientResource client) throws IOException {
        AuthorizationResource authorization = client.authorization();
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName(name);

        if (resourceName != null) {
            representation.addResource(resourceName);
        }

        representation.addScope(scopes.toArray(new String[scopes.size()]));
        representation.addPolicy(policies.toArray(new String[policies.size()]));

        authorization.permissions().scope().create(representation).close();
    }

    private AuthzClient getAuthzClient() {
        return AuthzClient.create(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"));
    }
}
