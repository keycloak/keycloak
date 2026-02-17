package org.keycloak.tests.workflow.step;

import java.time.Duration;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.AddRequiredActionStepProvider;
import org.keycloak.models.workflow.AddRequiredActionStepProviderFactory;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Tests the execution of the 'add-required-action' workflow step.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class AddRequiredActionTest extends AbstractWorkflowTest {

    @Test
    public void testStepRun() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(AddRequiredActionStepProviderFactory.ID)
                                .withConfig(AddRequiredActionStepProvider.REQUIRED_ACTION_KEY, "update-password")
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("myuser").build()).close();

        Awaitility.await()
                .timeout(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    var users = managedRealm.admin().users().search("myuser");
                    assertThat(users, hasSize(1));
                    var userRepresentation = users.get(0);
                    Assertions.assertTrue(userRepresentation.getRequiredActions() != null && !userRepresentation.getRequiredActions().isEmpty());
                    assertThat(userRepresentation.getRequiredActions(), hasSize(1));
                    assertThat(userRepresentation.getRequiredActions().get(0), is(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
                });
    }

    @Test
    @DisplayName("Test that a disabled required action is not added to the user")
    public void testDisabledRequiredActionIsNotAdded() {
        // create a workflow that adds 'terms-and-conditions' required action - it is disabled by default in the realm, so step should not add it to the user
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(AddRequiredActionStepProviderFactory.ID)
                                .withConfig(AddRequiredActionStepProvider.REQUIRED_ACTION_KEY, "terms-and-conditions")
                                .build()
                ).build()).close();

        // create a user to trigger the workflow
        String userId;
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create().username("myuser").build())) {
            assertThat(201, is(response.getStatus()));
            userId = ApiUtil.getCreatedId(response);
        }

        // check the user does not have the required action added
        UserRepresentation userRepresentation = managedRealm.admin().users().get(userId).toRepresentation();
        assertThat(userRepresentation.getRequiredActions(), hasSize(0));
    }
}
