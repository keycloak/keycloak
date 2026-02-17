package org.keycloak.tests.workflow.execution;

import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that a workflow with no time conditions runs all steps immediately when scheduled.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class ImmediateWorkflowExecutionTest extends AbstractWorkflowTest {

    @Test
    public void testRunImmediateWorkflow() {
        // create a test workflow with no time conditions - should run immediately when scheduled
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("message", "message")
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .build()
                ).build()).close();

        // create a new user - should be bound to the new workflow and all steps should run right away
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").build()).close();

        // check the user has the attribute set and is disabled
        runOnServer.run(session -> {
            UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), "testuser");
            assertThat(user, notNullValue());
            assertThat(user.getAttributes(), notNullValue());
            assertThat(user.getAttributes().get("message"), notNullValue());
            assertThat(user.getAttributes().get("message").get(0), is("message"));
            assertFalse(user.isEnabled());
        });
    }

}
