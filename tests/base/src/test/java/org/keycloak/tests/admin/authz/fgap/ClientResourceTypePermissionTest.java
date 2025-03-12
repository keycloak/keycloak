package org.keycloak.tests.admin.authz.fgap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;

@KeycloakIntegrationTest(config = KeycloakAdminPermissionsServerConfig.class)
public class ClientResourceTypePermissionTest extends AbstractPermissionTest {

    @InjectClient(ref = "testClient")
    ManagedClient testClient;

    @InjectClient(ref = "testClient2", lifecycle = LifeCycle.METHOD)
    ManagedClient testClient2;

    @BeforeEach
    public void onBefore() {
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("User Policy");
        client.admin().authorization().policies().user().create(policy).close();
    }

    @AfterEach
    public void onAfter() {
        ScopePermissionsResource permissions = getScopePermissionsResource(client);

        for (ScopePermissionRepresentation permission : permissions.findAll(null, null, null, -1, -1)) {
            permissions.findById(permission.getId()).remove();
        }
    }

    @Test
    public void testRemoveClient() {
        //create client policies
        createClientPolicy(realm, client, "Only testClient or testClient2 Client Policy", testClient.getId(), testClient2.getId());
        createClientPolicy(realm, client, "Only testClient2 Client Policy", testClient2.getId());

        //create client permissions
        createClientPermission(testClient, testClient2);
        createClientPermission(testClient2);

        List<PolicyRepresentation> policies = getPolicies().policies(null, "Only", "client", null, null, null, null, null, null, null);
        assertThat(policies, hasSize(2));
        assertThat(policies.get(0).getConfig().get("clients"), containsString(testClient2.getId()));
        assertThat(policies.get(1).getConfig().get("clients"), containsString(testClient2.getId()));

        List<ScopePermissionRepresentation> permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(2));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(testClient2.getId()));
        assertThat(getPolicies().policy(permissions.get(1).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(testClient2.getId()));

        //remove client
        realm.admin().clients().get(testClient2.getId()).remove();

        //check the resource was removed from policies
        ClientPolicyRepresentation clientPolicy = getPolicies().client().findByName("Only testClient or testClient2 Client Policy");
        assertThat(clientPolicy, notNullValue());
        assertThat(clientPolicy.getClients(), not(contains(testClient2.getId())));

        ClientPolicyRepresentation clientPolicy1 = getPolicies().client().findByName("Only testClient2 Client Policy");
        assertThat(clientPolicy1, notNullValue());
        assertThat(clientPolicy1.getClients(), empty());

        //there should be 1 permission left
        permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(1));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), not(hasItem(testClient2.getId())));
    }

    private ScopePermissionRepresentation createClientPermission(ManagedClient... clients) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.CLIENTS.getType())
                .resources(Arrays.stream(clients).map(ManagedClient::getClientId).collect(Collectors.toSet()))
                .scopes(AdminPermissionsSchema.CLIENTS.getScopes())
                .addPolicies(List.of("User Policy"))
                .build();

        createPermission(client, permission);

        return permission;
    }

}
