package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.conditions.GroupMembershipWorkflowConditionFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStateRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_ADDED;
import static org.keycloak.models.workflow.ResourceOperationType.USER_LOGGED_IN;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class GroupMembershipJoinWorkflowTest extends AbstractWorkflowTest {

    private static final String GROUP_NAME = "generic-group";
    private static final String GROUP_CONDITION = GroupMembershipWorkflowConditionFactory.ID + "(" + GROUP_NAME + ")";

    @Test
    public void testEventsOnGroupMembershipJoin() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);
        String groupId;

        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("generic-group").build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_GROUP_MEMBERSHIP_ADDED.name())
                .onCondition(GROUP_CONDITION)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        try (Response response = workflows.create(expectedWorkflow)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        String userId;

        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        UserResource userResource = managedRealm.admin().users().get(userId);

        userResource.joinGroup(groupId);

        // set offset to 6 days - notify step should run now
        runScheduledSteps(Duration.ofDays(6));

        UserRepresentation rep = userResource.toRepresentation();
        assertNotNull(rep.getAttributes().get("attribute"));
    }

    @Test
    public void testRemoveAssociatedGroup() {
        String groupId;
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("generic-group").build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_ADDED.toString(), USER_LOGGED_IN.toString())
                .onCondition(GROUP_CONDITION)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .build())
                .build())) {
            workflowId = ApiUtil.getCreatedId(response);
        }

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));

        WorkflowRepresentation workflowRep = managedRealm.admin().workflows().workflow(workflowId).toRepresentation();
        assertThat(workflowRep.getConfig().getFirst("enabled"), nullValue());

        // remove group
        managedRealm.admin().groups().group(groupId).remove();

        // create new user - it will trigger an activation event and therefore should disable the workflow
        managedRealm.admin().users().create(UserConfigBuilder.create().username("test").build()).close();

        Awaitility.await()
                .timeout(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    var rep = managedRealm.admin().workflows().workflow(workflowId).toRepresentation();
                    assertThat(rep.getEnabled(), allOf(notNullValue(), is(false)));
                    WorkflowStateRepresentation status = rep.getState();
                    assertThat(status, notNullValue());
                    assertThat(status.getErrors(), hasSize(1));
                    assertThat(status.getErrors().get(0), containsString("Group with name %s does not exist.".formatted("generic-group")));
                });
    }
}
