package org.keycloak.tests.workflow.condition;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.RestartWorkflowStepProviderFactory;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.models.workflow.conditions.RoleWorkflowConditionFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowScheduleRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class RoleWorkflowConditionTest extends AbstractWorkflowTest {

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

    @Test
    public void testActivateWorkflowForEligibleResources() {
        RoleRepresentation role = createRoleIfNotExists("testRole");

        // create some users associated with the role
        for (int i = 0; i < 10; i++) {
            try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create().username("user-with-role-" + i).build())) {
                assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
                managedRealm.admin().users().get(ApiUtil.getCreatedId(response)).roles().realmLevel().add(List.of(role));
            }

        }

        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("test-role-workflow")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").build())
                .onCondition(RoleWorkflowConditionFactory.ID + "(testRole)")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));

        Awaitility.await()
                .timeout(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    runOnServer.run((RunOnServer) session -> {
                        // check the same users are now scheduled to run the second step.
                        WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
                        List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
                        assertThat(registeredWorkflows, hasSize(1));
                        Workflow workflow = registeredWorkflows.get(0);
                        // check workflow was correctly assigned to the users
                        WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
                        List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow).toList();
                        assertThat(scheduledSteps, hasSize(10));
                    });
                });
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

        // set offset to 6 days - notify step should run now
        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
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
        createWorkflow(List.of(expectedValues));
    }

    private void createWorkflow(List<String> expectedValues) {

        for (String roleName : expectedValues) {
            createRoleIfNotExists(roleName);
        }
        String roleCondition = expectedValues.stream()
                .map(role -> RoleWorkflowConditionFactory.ID + "(" + role + ")")
                .reduce((a, b) -> a + " AND " + b).orElse(null);

        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_ROLE_GRANTED.name())
                .onCondition(roleCondition)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("notified", "true")
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create()
                                .of(RestartWorkflowStepProviderFactory.ID)
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        try (Response response = workflows.create(expectedWorkflow)) {
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
}
