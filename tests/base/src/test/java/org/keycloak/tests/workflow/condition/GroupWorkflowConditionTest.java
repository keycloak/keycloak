package org.keycloak.tests.workflow.condition;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.conditions.GroupMembershipWorkflowConditionFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowScheduleRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class GroupWorkflowConditionTest extends AbstractWorkflowTest {

    private static final String GROUP_NAME = "generic-group";
    private static final String GROUP_CONDITION = GroupMembershipWorkflowConditionFactory.ID + "(" + GROUP_NAME + ")";

    @Test
    public void testActivateWorkflowForUsersInGroup() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        // create test group and associate a few test users with it
        managedRealm.admin().groups().add(GroupConfigBuilder.create().name(GROUP_NAME).build()).close();

        // create workflow that activates on user creation with a group membership condition
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("group-membership-workflow")
                .onEvent(ResourceOperationType.USER_CREATED.name())
                .onCondition(GROUP_CONDITION)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .build()
                ).build()).close();

        // create test user not associated with the target group - workflow should not trigger
        String userId;
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                .username("no-group-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        // check the workflow hasn't run for this user
        UserRepresentation userRepresentation = managedRealm.admin().users().get(userId).toRepresentation();
        assertThat(userRepresentation.getAttributes(), nullValue());

        // create another user associated with the target group - this time the workflow should trigger
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                .username("group-user").groups(GROUP_NAME).build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        // verify that the workflow step executed and set the user attribute
        userRepresentation = managedRealm.admin().users().get(userId).toRepresentation();
        assertThat(userRepresentation.getAttributes().get("attribute"), notNullValue());
        assertThat(userRepresentation.getAttributes().get("attribute").get(0), is("attr1"));
    }

    @Test
    public void testActivateWorkflowForEligibleResources() {
        managedRealm.admin().groups().add(GroupConfigBuilder.create().name(GROUP_NAME).build()).close();

        // create test users - some associated with the target group, some not
        for (int i = 0; i < 20; i++) {
            UserConfigBuilder builder = UserConfigBuilder.create().username("group-member-" + i);
            if (i % 2 == 0) {
                builder.groups(GROUP_NAME);
            }
            managedRealm.admin().users().create(builder.build()).close();
        }

        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("group-membership-workflow")
                .onCondition(GROUP_CONDITION)
                .schedule(WorkflowScheduleRepresentation.create().after("1s").build())
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
                        RealmModel realm = session.getContext().getRealm();
                        List<String> scheduledUsers = stateProvider.getScheduledStepsByWorkflow(workflow)
                                .map(step -> session.users().getUserById(realm, step.resourceId()).getUsername()).toList();
                        assertThat(scheduledUsers, hasSize(10));

                        List<String> expectedUsers = session.users().searchForUserStream(realm, Map.of())
                                .map(UserModel::getUsername)
                                .filter(username -> username.startsWith("group-member-"))
                                .filter(username -> Integer.parseInt(username.substring("group-member-".length())) % 2 == 0)
                                .toList();
                        assertThat(scheduledUsers, containsInAnyOrder(expectedUsers.toArray()));
                    });
                });
    }

}
