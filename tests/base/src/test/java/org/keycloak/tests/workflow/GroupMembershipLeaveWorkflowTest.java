package org.keycloak.tests.workflow;

import java.time.Duration;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class GroupMembershipLeaveWorkflowTest extends AbstractWorkflowTest {

    private static final String GROUP_NAME = "generic-group";

    @Test
    public void testEventsOnGroupMembershipLeave() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);
        String groupId;

        // create a test group
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create().name(GROUP_NAME).build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_GROUP_MEMBERSHIP_REMOVED.name() + "(" + GROUP_NAME + ")")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        // create the workflow that activates on group membership removal
        WorkflowsResource workflows = managedRealm.admin().workflows();
        try (Response response = workflows.create(expectedWorkflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        // now create a user and add them to the group
        String userId;
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);
        userResource.joinGroup(groupId);

        // set offset to 6 days - no steps should run as the workflow shouldn't have activated yet
        runScheduledSteps(Duration.ofDays(6));
        UserRepresentation rep = userResource.toRepresentation();
        assertNull(rep.getAttributes());

        // now remove the user from the group - this should trigger the workflow
        userResource.leaveGroup(groupId);
        // set offset to 6 days - set attribute step should run now
        runScheduledSteps(Duration.ofDays(6));
        rep = userResource.toRepresentation();
        assertNotNull(rep.getAttributes());
        assertThat(rep.getAttributes().get("attribute").get(0), is("attr1"));
    }

}
