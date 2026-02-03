package org.keycloak.tests.workflow.activation;

import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.events.UserFedIdentityAddedWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserFedIdentityRemovedWorkflowEventFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;
import org.keycloak.testsuite.util.FederatedIdentityBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests activation of workflows based on IdP linking events.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class IdpLinkingWorkflowTest extends AbstractWorkflowTest {

    private static final String IDP_ALIAS = "my-idp";

    @Test
    public void testActivateWorkflowOnIdpLink() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        // create the workflow that triggers on IdP linking
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserFedIdentityAddedWorkflowEventFactory.ID + "(" + IDP_ALIAS + ")")
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

        // create a test user then add a federated identity (idp-linking)
        String userId;
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);

        // add federated identity to trigger the workflow
        userResource.addFederatedIdentity(IDP_ALIAS, FederatedIdentityBuilder.create().identityProvider(IDP_ALIAS)
                .userId(UUID.randomUUID().toString()).userName("federated-username").build()).close();

        // verify that the workflow step executed and set the user attribute
        UserRepresentation userRepresentation = userResource.toRepresentation();
        assertThat(userRepresentation.getAttributes().get("attribute"), notNullValue());
        assertThat(userRepresentation.getAttributes().get("attribute").get(0), is("attr1"));
    }

    @Test
    public void testActivateWorkflowOnIdpUnlink() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        // create the workflow that triggers on IdP unlinking
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserFedIdentityRemovedWorkflowEventFactory.ID + "(" + IDP_ALIAS + ")")
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

        // create a test user then add a federated identity (idp-linking) - workflow should not trigger yet
        String userId;
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource userResource = managedRealm.admin().users().get(userId);
        userResource.addFederatedIdentity(IDP_ALIAS, FederatedIdentityBuilder.create().identityProvider(IDP_ALIAS)
                .userId(UUID.randomUUID().toString()).userName("federated-username").build()).close();

        UserRepresentation userRepresentation = userResource.toRepresentation();
        assertThat(userRepresentation.getAttributes(), nullValue());

        // remove federated identity to trigger the workflow (idp-unlinking)
        userResource.removeFederatedIdentity(IDP_ALIAS);

        // verify that the workflow step executed and set the user attribute
        userRepresentation = userResource.toRepresentation();
        assertThat(userRepresentation.getAttributes().get("attribute"), notNullValue());
        assertThat(userRepresentation.getAttributes().get("attribute").get(0), is("attr1"));
    }
}
