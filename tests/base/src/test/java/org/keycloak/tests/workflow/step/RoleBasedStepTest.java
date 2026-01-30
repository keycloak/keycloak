package org.keycloak.tests.workflow.step;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.workflow.GrantRoleStepProvider;
import org.keycloak.models.workflow.GrantRoleStepProviderFactory;
import org.keycloak.models.workflow.RevokeRoleStepProvider;
import org.keycloak.models.workflow.RevokeRoleStepProviderFactory;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserRoleRevokedWorkflowEventFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

/**
 * Tests the execution of the 'grant-role' and 'revoke-role' workflow steps.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class RoleBasedStepTest extends AbstractWorkflowTest {

    @BeforeEach
    public void setupRoles() {
        RealmResource admin = managedRealm.admin();
        RolesResource realmRoles = admin.roles();
        List.of("a", "b", "c").forEach(name -> realmRoles.create(RoleConfigBuilder.create().name("realm-role-" + name).build()));
        ClientsResource clients = admin.clients();
        clients.create(ClientConfigBuilder.create().clientId("myclient").build()).close();
        ClientRepresentation client = clients.findByClientId("myclient").get(0);
        RolesResource clientRoles = clients.get(client.getId()).roles();
        List.of("a", "b", "c").forEach(name -> clientRoles.create(RoleConfigBuilder.create().name("client-role-" + name).build()));
    }

    @Test
    public void testGrantRole() {
        List<String> expectedRealmRoles = List.of("realm-role-a", "realm-role-b");
        List<String> expectedClientRoles = List.of("myclient/client-role-a", "myclient/client-role-c");
        List<String> expectedRoles = Stream.concat(expectedRealmRoles.stream(), expectedClientRoles.stream()).toList();

        create(WorkflowRepresentation.withName("grant-roles")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(GrantRoleStepProviderFactory.ID)
                                .withConfig(GrantRoleStepProvider.CONFIG_ROLE, expectedRoles.toArray(new String[0]))
                                .build()
                ).build());

        UserResource user = getUserResource(UserConfigBuilder.create().username("myuser").build());

        Awaitility.await()
                .timeout(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    var actualRealmRoles = user.roles().getAll().getRealmMappings().stream()
                            .map(RoleRepresentation::getName).toList();
                    assertThat(actualRealmRoles, hasItems(expectedRealmRoles.toArray(new String[0])));
                    var actualClientRoles = user.roles().getAll().getClientMappings().get("myclient").getMappings().stream()
                            .map((r) -> "myclient/" + r.getName()).toList();
                    assertThat(actualClientRoles, hasItems(expectedClientRoles.toArray(new String[0])));
                });
    }

    @Test
    public void testRevokeRole() {
        UserResource user = getUserResource(UserConfigBuilder.create()
                .username("myuser")
                .build());
        grantRole(user, "realm-role-a", "realm-role-b", "realm-role-c", "myclient/client-role-a", "myclient/client-role-c");

        create(WorkflowRepresentation.withName("revoke-roles")
                .onEvent(UserRoleRevokedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(RevokeRoleStepProviderFactory.ID)
                                .withConfig(RevokeRoleStepProvider.CONFIG_ROLE, "realm-role-a", "myclient/client-role-c")
                                .build()
                ).build());

        user.roles().realmLevel().remove(List.of(
            managedRealm.admin().roles().get("realm-role-b").toRepresentation()
        ));

        Awaitility.await()
                .timeout(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    var actualRealmRoles = user.roles().getAll().getRealmMappings().stream()
                            .map(RoleRepresentation::getName).toList();
                    assertThat(actualRealmRoles, not(hasItems(List.of("realm-role-a", "realm-role-b").toArray(new String[0]))));
                    assertThat(actualRealmRoles, hasItems(List.of("realm-role-c").toArray(new String[0])));
                    var actualClientRoles = user.roles().getAll().getClientMappings().get("myclient").getMappings().stream()
                            .map((r) -> "myclient/" + r.getName()).toList();
                    assertThat(actualClientRoles, containsInAnyOrder(List.of("myclient/client-role-a").toArray(new String[0])));
                });
    }

    private UserResource getUserResource(UserRepresentation user) {
        UsersResource users = managedRealm.admin().users();

        try (Response response = users.create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        return users.get(user.getId());
    }

    private void grantRole(UserResource user, String... roles) {
        RealmResource admin = managedRealm.admin();

        for (String name : roles) {
            String[] parts = name.split("/");

            if (parts.length > 1) {
                ClientsResource clients = admin.clients();
                ClientRepresentation client = clients.findByClientId(parts[0]).get(0);
                RoleRepresentation clientRole = clients.get(client.getId()).roles().get(parts[1]).toRepresentation();
                user.roles().clientLevel(client.getId()).add(List.of(clientRole));
            } else {
                RoleRepresentation realmRole = admin.roles().get(name).toRepresentation();
                user.roles().realmLevel().add(List.of(realmRole));
            }
        }
    }
}
