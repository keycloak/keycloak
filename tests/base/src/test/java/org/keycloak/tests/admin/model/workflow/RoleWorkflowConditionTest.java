package org.keycloak.tests.admin.model.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.models.workflow.conditions.RoleWorkflowConditionFactory.EXPECTED_ROLES;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.EventBasedWorkflowProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.WorkflowsManager;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.conditions.RoleWorkflowConditionFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.workflows.WorkflowSetRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.representations.workflows.WorkflowConditionRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;

@KeycloakIntegrationTest(config = WorkflowsServerConfig.class)
public class RoleWorkflowConditionTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @BeforeEach
    public void onBefore() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);
    }

    @Test
    public void testConditionForSingleRole() {
        String expected = "realm-role-1";
        createWorkflow(expected);
        assertUserRoles("user-1", false);
        assertUserRoles("user-2", false, "not-valid-role");
        assertUserRoles("user-3", true, expected);
    }

    @Test
    public void testConditionForMultipleRole() {
        List<String> expected = List.of("realm-role-1", "realm-role-2", "client-a/client-role-1");
        createWorkflow(expected);
        assertUserRoles("user-1", false, List.of("realm-role-1", "realm-role-2"));
        assertUserRoles("user-2", false, List.of("realm-role-1", "realm-role-2", "client-b/client-role-1"));
        assertUserRoles("user-3", true, expected);
    }

    private void assertUserRoles(String username, boolean shouldExist, String... roles) {
        assertUserRoles(username, shouldExist, List.of(roles));
    }

    private void assertUserRoles(String username, boolean shouldExist, List<String> roles) {
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                .username(username)
                .email(username + "@example.com")
                .build())) {
            String id = ApiUtil.getCreatedId(response);

            for (String roleName : roles) {
                RoleRepresentation role = createRoleIfNotExists(roleName);

                if (role.getClientRole()) {
                    managedRealm.admin().users().get(id).roles().clientLevel(role.getContainerId()).add(List.of(role));
                } else {
                    managedRealm.admin().users().get(id).roles().realmLevel().add(List.of(role));
                }
            }
        }

        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);

            try {
                // set offset to 7 days - notify step should run now
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                new WorkflowsManager(session).runScheduledSteps();
            } finally {
                Time.setOffset(0);
            }

            UserModel user = session.users().getUserByUsername(realm, username);
            assertNotNull(user);

            if (shouldExist) {
                assertTrue(user.getAttributes().containsKey("notified"));
            } else {
                assertFalse(user.getAttributes().containsKey("notified"));
            }
        }));
    }

    private void createWorkflow(String... expectedValues) {
        createWorkflow(Map.of(EXPECTED_ROLES, List.of(expectedValues)));
    }

    private void createWorkflow(List<String> expectedValues) {
        createWorkflow(Map.of(EXPECTED_ROLES, expectedValues));
    }

    private void createWorkflow(Map<String, List<String>> attributes) {
        for (String roleName : attributes.getOrDefault(EXPECTED_ROLES, List.of())) {
            createRoleIfNotExists(roleName);
        }

        WorkflowSetRepresentation expectedWorkflows = WorkflowRepresentation.create()
                .of(EventBasedWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.USER_ROLE_ADD.name())
                .recurring()
                .onConditions(WorkflowConditionRepresentation.create()
                        .of(RoleWorkflowConditionFactory.ID)
                        .withConfig(attributes)
                        .build())
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("notified", "true")
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        try (Response response = workflows.create(expectedWorkflows)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }
    }

    private RoleRepresentation createRoleIfNotExists(String roleName) {
        if (roleName.indexOf('/') != -1) {
            String[] parts = roleName.split("/");
            String clientId = parts[0];
            String clientRoleName = parts[1];
            List<ClientRepresentation> clients = managedRealm.admin().clients().findByClientId(clientId);

            if (clients.isEmpty()) {
                ClientRepresentation client = new ClientRepresentation();
                client.setClientId(clientId);
                client.setName(clientId);
                client.setProtocol("openid-connect");
                managedRealm.admin().clients().create(client).close();
                clients = managedRealm.admin().clients().findByClientId(clientId);
            }

            assertThat(clients.isEmpty(), is(false));

            RolesResource roles = managedRealm.admin().clients().get(clients.get(0).getId()).roles();

            if (roles.list(clientRoleName, -1, -1).isEmpty()) {
                roles.create(RoleConfigBuilder.create()
                        .name(clientRoleName)
                        .build());
            }

            return roles.get(clientRoleName).toRepresentation();
        } else {
            RolesResource roles = managedRealm.admin().roles();

            if (roles.list(roleName, -1, -1).isEmpty()) {
                roles.create(RoleConfigBuilder.create()
                        .name(roleName)
                        .build());
            }

            return roles.get(roleName).toRepresentation();
        }
    }

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
    }
}
