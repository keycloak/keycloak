package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;

import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.AddRequiredActionStepProvider;
import org.keycloak.models.workflow.AddRequiredActionStepProviderFactory;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_ADDED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class AddRequiredActionTest extends AbstractWorkflowTest {

    @Test
    public void testStepRun() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_ADDED.name())
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(AddRequiredActionStepProviderFactory.ID)
                                .withConfig(AddRequiredActionStepProvider.REQUIRED_ACTION_KEY, "UPDATE_PASSWORD")
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
}
