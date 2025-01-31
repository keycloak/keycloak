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

package org.keycloak.tests.admin.authz.fgap;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.authorization.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.AdminPermissionsSchema.VIEW;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RegexPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;

@KeycloakIntegrationTest(config = KeycloakAdminPermissionsServerConfig.class)
public class PermissionClientTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @Test
    public void testUnsupportedPolicyTypes() {
        assertSupportForPolicyType("resource", () -> getPermissionsResource().resource().create(new ResourcePermissionRepresentation()), false);
    }

    @Test
    public void testSupportedPolicyTypes() {
        assertSupportForPolicyType("scope", () -> getPermissionsResource().scope().create(PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .scopes(AdminPermissionsSchema.USERS.getScopes())
                .build()), true);
        assertSupportForPolicyType("user", () -> getPolicies().user().create(new UserPolicyRepresentation()), true);
        assertSupportForPolicyType("client", () -> getPolicies().client().create(new ClientPolicyRepresentation()), true);
        assertSupportForPolicyType("group", () -> getPolicies().group().create(new GroupPolicyRepresentation()), true);
        assertSupportForPolicyType("role", () -> getPolicies().role().create(new RolePolicyRepresentation()), true);
        assertSupportForPolicyType("aggregate", () -> getPolicies().aggregate().create(new AggregatePolicyRepresentation()), true);
        assertSupportForPolicyType("client-scope", () -> getPolicies().clientScope().create(new ClientScopePolicyRepresentation()), true);
        assertSupportForPolicyType("js", () -> getPolicies().js().create(new JSPolicyRepresentation()), true);
        assertSupportForPolicyType("regex", () -> getPolicies().regex().create(new RegexPolicyRepresentation()), true);
        assertSupportForPolicyType("time", () -> getPolicies().time().create(new TimePolicyRepresentation()), true);
    }

    private void assertSupportForPolicyType(String type, Supplier<Response> operation, boolean supported) {
        try (Response response = operation.get()) {
            assertPolicyEndpointResponse(type, supported, response);
        }

        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setType(type);

        try (Response response = getPolicies().create(representation)) {
            assertPolicyEndpointResponse(type, supported, response);
        }
    }

    private void assertPolicyEndpointResponse(String type, boolean supported, Response response) {
        assertThat("Policy type [" + type + "] should be " + (supported ? "supported" : "unsupported"), Status.BAD_REQUEST.equals(Status.fromStatusCode(response.getStatus())), not(supported));
        assertThat("Policy type [" + type + "] should be " + (supported ? "supported" : "unsupported"), response.readEntity(String.class).contains("Policy type not supported by feature"), not(supported));
    }

    @Test
    public void testClientPolicy() {
        ClientPolicyRepresentation policy = new ClientPolicyRepresentation();
        policy.setName("Only My Client Client Policy");
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        policy.addClient(myclient.getId());
        policy.addResource(myclient.getId());
        policy.setResourceType(AdminPermissionsSchema.CLIENTS.getType());
        client.admin().authorization().policies().client().create(policy).close();

        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .name(AdminPermissionsSchema.CLIENTS.getType())
                .resourceType(AdminPermissionsSchema.CLIENTS.getType())
                .scopes(Set.of(VIEW, MANAGE))
                .addPolicies(List.of(policy.getName()))
                .build();

        createPermission(permission);

        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertFalse(search.isEmpty());

        permission = client.admin().authorization().permissions().scope().findByName(permission.getName());
        permission.setPolicies(Set.of());
        client.admin().authorization().permissions().scope().findById(permission.getId()).update(permission);
        search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertTrue(search.isEmpty());
    }

    @Test
    public void testManageOnlyOneClient() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);

        UserPolicyRepresentation userPolicy = new UserPolicyRepresentation();
        userPolicy.setName("Only My Admin User Policy");
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        userPolicy.addUser(myadmin.getId());
        userPolicy.addResource(myclient.getId());
        userPolicy.setResourceType(AdminPermissionsSchema.CLIENTS.getType());
        client.admin().authorization().policies().user().create(userPolicy).close();

        PolicyRepresentation onlyMyAdminUserPolicy = client.admin().authorization().policies().findByName("Only My Admin User Policy");

        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .name(AdminPermissionsSchema.CLIENTS.getType())
                .resourceType(AdminPermissionsSchema.CLIENTS.getType())
                .scopes(Set.of(MANAGE))
                .addPolicies(List.of(onlyMyAdminUserPolicy.getName()))
                .build();
        createPermission(permission);

        myclient.setName("somethingNew");
        realmAdminClient.realm(realm.getName()).clients().get(myclient.getId()).update(myclient);

        ClientRepresentation adminCliClient = realm.admin().clients().findByClientId("admin-cli").get(0);
        adminCliClient.setName("somethingNew");
        try {
            realmAdminClient.realm(realm.getName()).clients().get(adminCliClient.getId()).update(adminCliClient);
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

    }

    private void createClientPolicy(Policy policy, PolicyStore policyStore, String client, String owner) {
        ClientPolicyRepresentation rep = new ClientPolicyRepresentation();

        rep.setName(KeycloakModelUtils.generateId());
        rep.addClient(client);

        Policy associatedPolicy = policyStore.create(policy.getResourceServer(), rep);

        associatedPolicy.setOwner(owner);

        policy.addAssociatedPolicy(associatedPolicy);
    }
}
