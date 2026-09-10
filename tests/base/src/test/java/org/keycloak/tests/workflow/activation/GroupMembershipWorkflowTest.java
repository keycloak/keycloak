package org.keycloak.tests.workflow.activation;

import java.time.Duration;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.events.UserGroupMembershipAddedWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserGroupMembershipRemovedWorkflowEventFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests activation of workflows based on user group membership events.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class GroupMembershipWorkflowTest extends AbstractWorkflowTest {

    private static final String GROUP_NAME = "generic-group";

    /**
     * Test that a workflow activating on user group membership addition is correctly triggered when a user joins the correct group.
     */
    @Test
    public void testActivateWorkflowOnGroupMembershipJoin() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);
        String groupId;

        try (Response response = managedRealm.admin().groups().add(GroupBuilder.create()
                .name("generic-group").build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserGroupMembershipAddedWorkflowEventFactory.ID + "(" + GROUP_NAME + ")")
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
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        UserResource userResource = managedRealm.admin().users().get(userId);
        userResource.joinGroup(groupId);

        // set offset to 6 days - notify step should run now
        runScheduledSteps(Duration.ofDays(6));

        UserRepresentation rep = userResource.toRepresentation();
        assertThat(rep.getAttributes().get("attribute"), notNullValue());
        assertThat(rep.getAttributes().get("attribute").get(0), is("attr1"));
    }

    /**
     * Test that a workflow activating on user group membership removal is correctly triggered when a user leaves a group.
     */
    @Test
    public void testActivateWorkflowOnGroupMembershipLeave() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);
        String groupId;

        // create a test group
        try (Response response = managedRealm.admin().groups().add(GroupBuilder.create().name(GROUP_NAME).build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserGroupMembershipRemovedWorkflowEventFactory.ID + "(" + GROUP_NAME + ")")
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
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);
        userResource.joinGroup(groupId);

        // set offset to 6 days - no steps should run as the workflow shouldn't have activated yet
        runScheduledSteps(Duration.ofDays(6));
        UserRepresentation rep = userResource.toRepresentation();
        assertThat(rep.getAttributes(), nullValue());

        // now remove the user from the group - this should trigger the workflow
        userResource.leaveGroup(groupId);
        // set offset to 6 days - set attribute step should run now
        runScheduledSteps(Duration.ofDays(6));
        rep = userResource.toRepresentation();
        assertThat(rep.getAttributes(), notNullValue());
        assertThat(rep.getAttributes().get("attribute").get(0), is("attr1"));
    }

    /**
     * Test that a workflow targeting a nested group path does not fire when a user joins a top-level group
     * whose name contains a slash that produces the same path string.
     */
    @Test
    public void testGroupMembershipJoinDoesNotConfuseSlashNamedGroupWithNestedPath() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        GroupsResource groups = managedRealm.admin().groups();

        // create a nested group structure: /Parent/Child
        String parentGroupId;
        try (Response response = groups.add(GroupBuilder.create().name("Parent").build())) {
            parentGroupId = ApiUtil.getCreatedId(response);
        }
        String nestedChildGroupId;
        try (Response response = groups.group(parentGroupId).subGroup(GroupBuilder.create().name("Child").build())) {
            nestedChildGroupId = ApiUtil.getCreatedId(response);
        }

        // create a top-level group with a slash in its name: "Parent/Child" - produces the same path /Parent/Child
        String slashNamedGroupId;
        try (Response response = groups.add(GroupBuilder.create().name("Parent/Child").build())) {
            slashNamedGroupId = ApiUtil.getCreatedId(response);
        }

        // verify the path collision exists - both groups produce the same path string
        GroupResource nestedChildResource = groups.group(nestedChildGroupId);
        GroupResource slashNamedResource = groups.group(slashNamedGroupId);
        assertThat("Nested group path", nestedChildResource.toRepresentation().getPath(), is("/Parent/Child"));
        assertThat("Slash-named group path must collide", slashNamedResource.toRepresentation().getPath(), is("/Parent/Child"));

        // create a workflow that triggers on user-group-membership-added(/Parent/Child)
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("nested-group-workflow")
                .onEvent(UserGroupMembershipAddedWorkflowEventFactory.ID + "(/Parent/Child)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "workflow-triggered")
                                .after(Duration.ofDays(1))
                                .build()
                ).build();

        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // create a test user
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("test-user").email("test-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);

        // add the user to the top-level slash-named group - this should NOT trigger the workflow
        userResource.joinGroup(slashNamedGroupId);
        runScheduledSteps(Duration.ofDays(2));
        UserRepresentation rep = userResource.toRepresentation();
        assertThat("Workflow should not fire for slash-named group", rep.getAttributes(), nullValue());

        // now add the user to the actual nested group - this SHOULD trigger the workflow
        userResource.joinGroup(nestedChildGroupId);
        runScheduledSteps(Duration.ofDays(2));
        rep = userResource.toRepresentation();
        assertThat("Workflow should fire for nested group", rep.getAttributes(), notNullValue());
        assertThat(rep.getAttributes().get("attribute").get(0), is("workflow-triggered"));
    }

    /**
     * Test that a workflow targeting a nested group path does not fire when a user leaves a top-level group
     * whose name contains a slash that produces the same path string.
     */
    @Test
    public void testGroupMembershipLeaveDoesNotConfuseSlashNamedGroupWithNestedPath() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        GroupsResource groups = managedRealm.admin().groups();

        // create a nested group structure: /Parent/Child
        String parentGroupId;
        try (Response response = groups.add(GroupBuilder.create().name("Parent").build())) {
            parentGroupId = ApiUtil.getCreatedId(response);
        }
        String nestedChildGroupId;
        try (Response response = groups.group(parentGroupId).subGroup(GroupBuilder.create().name("Child").build())) {
            nestedChildGroupId = ApiUtil.getCreatedId(response);
        }

        // create a top-level group with a slash in its name
        String slashNamedGroupId;
        try (Response response = groups.add(GroupBuilder.create().name("Parent/Child").build())) {
            slashNamedGroupId = ApiUtil.getCreatedId(response);
        }

        // verify the path collision exists
        assertThat("Slash-named group path must collide",
                groups.group(slashNamedGroupId).toRepresentation().getPath(), is("/Parent/Child"));

        // create a workflow that triggers on user-group-membership-removed(/Parent/Child)
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("nested-group-leave-workflow")
                .onEvent(UserGroupMembershipRemovedWorkflowEventFactory.ID + "(/Parent/Child)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "leave-triggered")
                                .after(Duration.ofDays(1))
                                .build()
                ).build();

        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // create a test user and add them to both groups
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("test-user").email("test-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);
        userResource.joinGroup(slashNamedGroupId);
        userResource.joinGroup(nestedChildGroupId);

        // remove the user from the slash-named group - this should NOT trigger the workflow
        userResource.leaveGroup(slashNamedGroupId);
        runScheduledSteps(Duration.ofDays(2));
        UserRepresentation rep = userResource.toRepresentation();
        assertThat("Workflow should not fire for slash-named group removal", rep.getAttributes(), nullValue());

        // remove the user from the nested group - this SHOULD trigger the workflow
        userResource.leaveGroup(nestedChildGroupId);
        runScheduledSteps(Duration.ofDays(2));
        rep = userResource.toRepresentation();
        assertThat("Workflow should fire for nested group removal", rep.getAttributes(), notNullValue());
        assertThat(rep.getAttributes().get("attribute").get(0), is("leave-triggered"));
    }

}
