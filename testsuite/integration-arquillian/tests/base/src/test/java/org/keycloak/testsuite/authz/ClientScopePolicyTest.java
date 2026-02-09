/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.util.List;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class ClientScopePolicyTest extends AbstractAuthzTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms
            .add(RealmBuilder.create().name("authz-test").user(UserBuilder.create().username("marta").password("password"))
                .clientScope(ClientScopeBuilder.create().name("foo").protocol("openid-connect"))
                .clientScope(ClientScopeBuilder.create().name("bar").protocol("openid-connect"))
                .clientScope(ClientScopeBuilder.create().name("baz").protocol("openid-connect"))
                .clientScope(ClientScopeBuilder.create().name("to-remove-a").protocol("openid-connect"))
                .clientScope(ClientScopeBuilder.create().name("to-remove-b").protocol("openid-connect"))
                .client(ClientBuilder.create().clientId("resource-server-test").secret("secret")
                    .authorizationServicesEnabled(true).redirectUris("http://localhost/resource-server-test")
                    .addOptionalClientScopes("foo", "bar", "baz").directAccessGrants())
                .build());
    }

    @Before
    public void configureAuthorization() throws Exception {
        createResource("Resource A");
        createResource("Resource B");

        createClientScopePolicy("Client Scope foo Policy", "foo", "bar");
        createClientScopePolicyAndLastOneRequired("Client Scope bar Policy", "foo", "bar");

        createResourcePermission("Resource A Permission", "Resource A", "Client Scope foo Policy");
        createResourcePermission("Resource B Permission", "Resource B", "Client Scope bar Policy");
    }

    private void createResource(String name) {
        AuthorizationResource authorization = getClient().authorization();
        ResourceRepresentation resource = new ResourceRepresentation(name);

        authorization.resources().create(resource).close();
    }

    private void createClientScopePolicy(String name, String... clientScopes) {
        ClientScopePolicyRepresentation policy = new ClientScopePolicyRepresentation();

        policy.setName(name);

        for (String clientScope : clientScopes) {
            policy.addClientScope(clientScope);
        }

        getClient().authorization().policies().clientScope().create(policy).close();
    }

    private void createClientScopePolicyAndLastOneRequired(String name, String... clientScopes) {
        ClientScopePolicyRepresentation policy = new ClientScopePolicyRepresentation();

        policy.setName(name);

        for (int i = 0; i < clientScopes.length - 1; i++) {
            policy.addClientScope(clientScopes[i]);
        }

        policy.addClientScope(clientScopes[clientScopes.length - 1], true);

        getClient().authorization().policies().clientScope().create(policy).close();
    }

    private void createResourcePermission(String name, String resource, String... policies) {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(name);
        permission.addResource(resource);
        permission.addPolicy(policies);

        getClient().authorization().permissions().resource().create(permission).close();
    }

    private ClientResource getClient() {
        return getClient(getRealm());
    }

    private ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream()
            .map(representation -> clients.get(representation.getId())).findFirst()
            .orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private RealmResource getRealm() {
        try {
            return getAdminClient().realm("authz-test");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create admin client");
        }
    }

    @Test
    public void testWithExpectedClientScope() {
        // Access Resource A with client scope foo.
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource A");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        AuthorizationResponse response = authzClient.authorization("marta", "password", "foo")
            .authorize(new AuthorizationRequest(ticket));
        assertNotNull(response.getToken());

        // Access Resource A with client scope bar.
        request = new PermissionRequest("Resource A");
        ticket = authzClient.protection().permission().create(request).getTicket();
        response = authzClient.authorization("marta", "password", "bar").authorize(new AuthorizationRequest(ticket));
        assertNotNull(response.getToken());

        // Access Resource B with client scope bar.
        request = new PermissionRequest("Resource B");
        ticket = authzClient.protection().permission().create(request).getTicket();
        response = authzClient.authorization("marta", "password", "bar").authorize(new AuthorizationRequest(ticket));
        assertNotNull(response.getToken());
    }

    @Test
    public void testWithoutExpectedClientScope() {
        // Access Resource A with client scope baz.
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource A");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        try {
            authzClient.authorization("marta", "password", "baz").authorize(new AuthorizationRequest(ticket));
            fail("Should fail.");
        } catch (AuthorizationDeniedException ignore) {

        }

        // Access Resource B with client scope foo.
        request = new PermissionRequest("Resource B");
        ticket = authzClient.protection().permission().create(request).getTicket();
        try {
            authzClient.authorization("marta", "password", "foo").authorize(new AuthorizationRequest(ticket));
            fail("Should fail.");
        } catch (AuthorizationDeniedException ignore) {

        }
    }

    @Test
    public void testRemovePolicyWhenRemovingScope() {
        createClientScopePolicy("Client Scope To Remove Policy", "to-remove-a", "to-remove-b");
        ClientScopesResource clientScopes = getRealm().clientScopes();
        ClientScopeRepresentation scopeRep = clientScopes.findAll().stream().filter(r -> r.getName().equals("to-remove-a"))
            .findAny().get();

        getClient().removeDefaultClientScope(scopeRep.getId());
        getRealm().clientScopes().get(scopeRep.getId()).remove();

        ClientScopePolicyRepresentation policyRep = getClient().authorization().policies().clientScope()
            .findByName("Client Scope To Remove Policy");
        final String id = scopeRep.getId();

        assertFalse(policyRep.getClientScopes().stream().anyMatch(def -> def.getId().equals(id)));

        scopeRep = clientScopes.findAll().stream().filter(r -> r.getName().equals("to-remove-b")).findAny().get();
        getClient().removeDefaultClientScope(scopeRep.getId());
        getRealm().clientScopes().get(scopeRep.getId()).remove();

        assertNull(getClient().authorization().policies().clientScope().findByName("Client Scope To Remove Policy"));
    }

    private AuthzClient getAuthzClient() {
        return AuthzClient.create(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"));
    }
}
