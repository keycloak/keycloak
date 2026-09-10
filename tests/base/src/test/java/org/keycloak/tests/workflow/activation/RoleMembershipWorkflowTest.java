package org.keycloak.tests.workflow.activation;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.events.UserRoleGrantedWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserRoleRevokedWorkflowEventFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.RoleBuilder;
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
 * Tests activation of workflows based on user role membership events.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class RoleMembershipWorkflowTest extends AbstractWorkflowTest {

    private static final String ROLE_NAME = "myrole";

    @Test
    public void testActivateWorkflowOnRoleGrant() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        // create a test realm role
        managedRealm.admin().roles().create(RoleBuilder.create().name(ROLE_NAME).build());
        RoleRepresentation roleRep = managedRealm.admin().roles().get(ROLE_NAME).toRepresentation();

        // create the workflow that triggers on role grant
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserRoleGrantedWorkflowEventFactory.ID + "(" + ROLE_NAME + ")")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();
        try (Response response = workflows.create(expectedWorkflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        // create a test user and then grant them the role to trigger the workflow
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);
        userResource.roles().realmLevel().add(List.of(roleRep));

        // verify that the workflow step executed and set the user attribute
        UserRepresentation userRepresentation = userResource.toRepresentation();
        assertThat(userRepresentation.getAttributes().get("attribute"), notNullValue());
        assertThat(userRepresentation.getAttributes().get("attribute").get(0), is("attr1"));
    }

    @Test
    public void testActivateWorkflowOnRoleRevoke() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        // create a test realm role
        managedRealm.admin().roles().create(RoleBuilder.create().name(ROLE_NAME).build());
        RoleRepresentation roleRep = managedRealm.admin().roles().get(ROLE_NAME).toRepresentation();

        // create the workflow that triggers on role revoke
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserRoleRevokedWorkflowEventFactory.ID + "(" + ROLE_NAME + ")")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();
        try (Response response = workflows.create(expectedWorkflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        // create a test user and then grant them the role - workflow should not trigger right now
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);
        userResource.roles().realmLevel().add(List.of(roleRep));

        UserRepresentation userRepresentation = userResource.toRepresentation();
        assertThat(userRepresentation.getAttributes(), nullValue());

        // now revoke the role to trigger the workflow
        userResource.roles().realmLevel().remove(List.of(roleRep));

        // verify that the workflow step executed and set the user attribute
        userRepresentation = userResource.toRepresentation();
        assertThat(userRepresentation.getAttributes().get("attribute"), notNullValue());
        assertThat(userRepresentation.getAttributes().get("attribute").get(0), is("attr1"));
    }

    /**
     * Test that a workflow triggered by a bare role name does not fire when a client role with
     * the same name is granted. A bare name should match only realm roles.
     */
    @Test
    public void testRoleGrantDoesNotConfuseClientRoleWithRealmRole() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        String collisionRoleName = "approver";

        // create a realm role
        managedRealm.admin().roles().create(RoleBuilder.create().name(collisionRoleName).build());
        RoleRepresentation realmRole = managedRealm.admin().roles().get(collisionRoleName).toRepresentation();

        // create a client with a role of the same name
        String clientId = "role-collision-client";
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientId);
        clientRep.setProtocol("openid-connect");
        managedRealm.admin().clients().create(clientRep).close();
        String clientUuid = managedRealm.admin().clients().findByClientId(clientId).get(0).getId();
        managedRealm.admin().clients().get(clientUuid).roles().create(RoleBuilder.create().name(collisionRoleName).build());
        RoleRepresentation clientRole = managedRealm.admin().clients().get(clientUuid).roles()
                .get(collisionRoleName).toRepresentation();

        // create a workflow triggered by the bare role name (should match realm role only)
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("role-collision-workflow")
                .onEvent(UserRoleGrantedWorkflowEventFactory.ID + "(" + collisionRoleName + ")")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "grant-triggered")
                                .build()
                ).build();

        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        // create a test user
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("collision-user").email("collision-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);

        // grant the CLIENT role - should NOT trigger the workflow
        userResource.roles().clientLevel(clientUuid).add(List.of(clientRole));
        UserRepresentation rep = userResource.toRepresentation();
        assertThat("Workflow should not fire for client role grant", rep.getAttributes(), nullValue());

        // grant the REALM role - should trigger the workflow
        userResource.roles().realmLevel().add(List.of(realmRole));
        rep = userResource.toRepresentation();
        assertThat("Workflow should fire for realm role grant", rep.getAttributes(), notNullValue());
        assertThat(rep.getAttributes().get("attribute").get(0), is("grant-triggered"));
    }

    /**
     * Test that a workflow triggered by a bare role name does not fire when a client role with
     * the same name is revoked. A bare name should match only realm roles.
     */
    @Test
    public void testRoleRevokeDoesNotConfuseClientRoleWithRealmRole() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        String collisionRoleName = "approver";

        // create a realm role
        managedRealm.admin().roles().create(RoleBuilder.create().name(collisionRoleName).build());
        RoleRepresentation realmRole = managedRealm.admin().roles().get(collisionRoleName).toRepresentation();

        // create a client with a role of the same name
        String clientId = "role-collision-client";
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientId);
        clientRep.setProtocol("openid-connect");
        managedRealm.admin().clients().create(clientRep).close();
        String clientUuid = managedRealm.admin().clients().findByClientId(clientId).get(0).getId();
        managedRealm.admin().clients().get(clientUuid).roles().create(RoleBuilder.create().name(collisionRoleName).build());
        RoleRepresentation clientRole = managedRealm.admin().clients().get(clientUuid).roles()
                .get(collisionRoleName).toRepresentation();

        // create a workflow triggered by the bare role name on revoke
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("role-collision-revoke-workflow")
                .onEvent(UserRoleRevokedWorkflowEventFactory.ID + "(" + collisionRoleName + ")")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "revoke-triggered")
                                .build()
                ).build();

        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        // create a test user and grant both roles
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("collision-user").email("collision-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);
        userResource.roles().clientLevel(clientUuid).add(List.of(clientRole));
        userResource.roles().realmLevel().add(List.of(realmRole));

        // revoke the CLIENT role - should NOT trigger the workflow
        userResource.roles().clientLevel(clientUuid).remove(List.of(clientRole));
        UserRepresentation rep = userResource.toRepresentation();
        assertThat("Workflow should not fire for client role revoke", rep.getAttributes(), nullValue());

        // revoke the REALM role - should trigger the workflow
        userResource.roles().realmLevel().remove(List.of(realmRole));
        rep = userResource.toRepresentation();
        assertThat("Workflow should fire for realm role revoke", rep.getAttributes(), notNullValue());
        assertThat(rep.getAttributes().get("attribute").get(0), is("revoke-triggered"));
    }

    /**
     * Test that a workflow can target a specific client role using the clientId/roleName format.
     */
    @Test
    public void testRoleGrantWithQualifiedClientRole() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        String roleName = "editor";
        String clientId = "qualified-client";

        // create a realm role with the same name
        managedRealm.admin().roles().create(RoleBuilder.create().name(roleName).build());
        RoleRepresentation realmRole = managedRealm.admin().roles().get(roleName).toRepresentation();

        // create a client with a role of the same name
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientId);
        clientRep.setProtocol("openid-connect");
        managedRealm.admin().clients().create(clientRep).close();
        String clientUuid = managedRealm.admin().clients().findByClientId(clientId).get(0).getId();
        managedRealm.admin().clients().get(clientUuid).roles().create(RoleBuilder.create().name(roleName).build());
        RoleRepresentation clientRole = managedRealm.admin().clients().get(clientUuid).roles()
                .get(roleName).toRepresentation();

        // create a workflow using the qualified clientId/roleName format
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("qualified-role-workflow")
                .onEvent(UserRoleGrantedWorkflowEventFactory.ID + "(" + clientId + "/" + roleName + ")")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "client-role-granted")
                                .build()
                ).build();

        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        // create a test user
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("qualified-user").email("qualified-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);

        // grant the REALM role - should NOT trigger the workflow (workflow targets client role)
        userResource.roles().realmLevel().add(List.of(realmRole));
        UserRepresentation rep = userResource.toRepresentation();
        assertThat("Workflow should not fire for realm role", rep.getAttributes(), nullValue());

        // grant the CLIENT role - should trigger the workflow
        userResource.roles().clientLevel(clientUuid).add(List.of(clientRole));
        rep = userResource.toRepresentation();
        assertThat("Workflow should fire for qualified client role", rep.getAttributes(), notNullValue());
        assertThat(rep.getAttributes().get("attribute").get(0), is("client-role-granted"));
    }

    /**
     * Test that a workflow can target a specific client role revocation using the clientId/roleName format.
     */
    @Test
    public void testRoleRevokeWithQualifiedClientRole() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        String roleName = "editor";
        String clientId = "qualified-client";

        // create a realm role with the same name
        managedRealm.admin().roles().create(RoleBuilder.create().name(roleName).build());
        RoleRepresentation realmRole = managedRealm.admin().roles().get(roleName).toRepresentation();

        // create a client with a role of the same name
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientId);
        clientRep.setProtocol("openid-connect");
        managedRealm.admin().clients().create(clientRep).close();
        String clientUuid = managedRealm.admin().clients().findByClientId(clientId).get(0).getId();
        managedRealm.admin().clients().get(clientUuid).roles().create(RoleBuilder.create().name(roleName).build());
        RoleRepresentation clientRole = managedRealm.admin().clients().get(clientUuid).roles()
                .get(roleName).toRepresentation();

        // create a workflow using the qualified clientId/roleName format for revoke
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("qualified-revoke-workflow")
                .onEvent(UserRoleRevokedWorkflowEventFactory.ID + "(" + clientId + "/" + roleName + ")")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "client-role-revoked")
                                .build()
                ).build();

        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        // create a test user and grant both roles
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("qualified-user").email("qualified-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);
        userResource.roles().realmLevel().add(List.of(realmRole));
        userResource.roles().clientLevel(clientUuid).add(List.of(clientRole));

        // revoke the REALM role - should NOT trigger the workflow (workflow targets client role)
        userResource.roles().realmLevel().remove(List.of(realmRole));
        UserRepresentation rep = userResource.toRepresentation();
        assertThat("Workflow should not fire for realm role revoke", rep.getAttributes(), nullValue());

        // revoke the CLIENT role - should trigger the workflow
        userResource.roles().clientLevel(clientUuid).remove(List.of(clientRole));
        rep = userResource.toRepresentation();
        assertThat("Workflow should fire for qualified client role revoke", rep.getAttributes(), notNullValue());
        assertThat(rep.getAttributes().get("attribute").get(0), is("client-role-revoked"));
    }
}
