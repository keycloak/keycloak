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
package org.keycloak.testsuite.admin.client.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPolicyManagementTest extends AbstractKeycloakTest {

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(createTestRealm().build());
    }

    protected RealmBuilder createTestRealm() {
        return RealmBuilder.create().name("authz-test")
                .user(UserBuilder.create().username("marta").password("password"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId("resource-server-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants());
    }

    @Before
    public void configureAuthorization() throws Exception {
        createResourcesAndScopes();
        RealmResource realm = getRealm();
        createPolicies(realm, getClient(realm));
    }

    protected void assertRepresentation(AbstractPolicyRepresentation expected, AbstractPolicyRepresentation actual,
                                        Supplier<List<ResourceRepresentation>> resources,
                                        Supplier<List<ScopeRepresentation>> scopes,
                                        Supplier<List<PolicyRepresentation>> policies) {
        assertNotNull(actual);
        assertNotNull(actual.getId());

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getDecisionStrategy(), actual.getDecisionStrategy());
        assertEquals(expected.getLogic(), actual.getLogic());
        assertNull(actual.getResources());
        assertNull(actual.getPolicies());
        assertNull(actual.getScopes());

        List<PolicyRepresentation> associatedPolicies = policies.get();

        if (expected.getPolicies() != null) {
            assertEquals(expected.getPolicies().size(), associatedPolicies.size());
            assertEquals(0, associatedPolicies.stream().map(representation1 -> representation1.getName()).filter(policyName -> !expected.getPolicies().contains(policyName)).count());
        } else {
            assertTrue(associatedPolicies.isEmpty());
        }

        List<ResourceRepresentation> associatedResources = resources.get();

        if (expected.getResources() != null) {
            assertEquals(expected.getResources().size(), associatedResources.size());
            assertEquals(0, associatedResources.stream().map(representation1 -> representation1.getName()).filter(resourceName -> !expected.getResources().contains(resourceName)).count());
        } else {
            assertTrue(associatedResources.isEmpty());
        }

        List<ScopeRepresentation> associatedScopes = scopes.get();

        if (expected.getScopes() != null) {
            assertEquals(expected.getScopes().size(), associatedScopes.size());
            assertEquals(0, associatedScopes.stream().map(representation1 -> representation1.getName()).filter(scopeName -> !expected.getScopes().contains(scopeName)).count());
        } else {
            assertTrue(associatedScopes.isEmpty());
        }

        expected.setId(actual.getId());
    }

    private void createResourcesAndScopes() throws IOException {
        Set<ScopeRepresentation> scopes = new HashSet<>();

        scopes.add(new ScopeRepresentation("read"));
        scopes.add(new ScopeRepresentation("write"));
        scopes.add(new ScopeRepresentation("execute"));

        List<ResourceRepresentation> resources = new ArrayList<>();

        resources.add(new ResourceRepresentation("Resource A", scopes));
        resources.add(new ResourceRepresentation("Resource B", scopes));
        resources.add(new ResourceRepresentation("Resource C", scopes));

        resources.forEach(resource -> {
            Response response = getClient().authorization().resources().create(resource);
            response.close();
        });
    }

    private void createPolicies(RealmResource realm, ClientResource client) throws IOException {
        createUserPolicy("Only Marta Policy", realm, client, "marta");
        createUserPolicy("Only Kolo Policy", realm, client, "kolo");
    }

    private void createUserPolicy(String name, RealmResource realm, ClientResource client, String username) throws IOException {
        String userId = realm.users().search(username).stream().map(representation -> representation.getId()).findFirst().orElseThrow(() -> new RuntimeException("Expected user [userId]"));

        UserPolicyRepresentation representation = new UserPolicyRepresentation();

        representation.setName(name);
        representation.addUser(userId);

        Response response = client.authorization().policies().user().create(representation);
        response.close();
    }

    protected ClientResource getClient() {
        return getClient(getRealm());
    }

    protected ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    protected RealmResource getRealm() {
        try {
            return adminClient.realm("authz-test");
        } catch (Exception cause) {
            throw new RuntimeException("Failed to create admin client", cause);
        }
    }
}
