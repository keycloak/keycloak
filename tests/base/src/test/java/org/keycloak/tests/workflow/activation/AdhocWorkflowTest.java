package org.keycloak.tests.workflow.activation;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests activation of workflows with no pre-defined event triggers (ad-hoc workflows). The activation has to be performed
 * using the API.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class AdhocWorkflowTest extends AbstractWorkflowTest {

    @Test
    public void testRunAdHocScheduledWorkflow() {
        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .withSteps(WorkflowStepRepresentation.create()
                        .of(SetUserAttributeStepProviderFactory.ID)
                        .after(Duration.ofDays(5))
                        .withConfig("message", "message")
                        .build())
                .build())) {
            workflowId = ApiUtil.getCreatedId(response);
        }

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));

        try (Response response = managedRealm.admin().users().create(getUserRepresentation())) {
            String id = ApiUtil.getCreatedId(response);
            managedRealm.admin().workflows().workflow(workflowId).activate(ResourceType.USERS.name(), id);
        }
    }

    @Test
    public void testRunAdHocImmediateWorkflow() {
        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .withSteps(WorkflowStepRepresentation.create()
                        .of(SetUserAttributeStepProviderFactory.ID)
                        .withConfig("message", "message")
                        .build())
                .build())) {
            workflowId = ApiUtil.getCreatedId(response);
        }

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));

        try (Response response = managedRealm.admin().users().create(getUserRepresentation())) {
            String id = ApiUtil.getCreatedId(response);
            managedRealm.admin().workflows().workflow(workflowId).activate(ResourceType.USERS.name(), id);
        }

        runScheduledSteps(Duration.ZERO);

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertNotNull(user.getAttributes().get("message"));
        }));
    }

    @Test
    public void testRunAdHocTimedWorkflow() {
        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .withSteps(WorkflowStepRepresentation.create()
                        .of(SetUserAttributeStepProviderFactory.ID)
                        .withConfig("message", "message")
                        .build())
                .build())) {
            workflowId = ApiUtil.getCreatedId(response);
        }

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));
        String resourceId;

        try (Response response = managedRealm.admin().users().create(getUserRepresentation())) {
            resourceId = ApiUtil.getCreatedId(response);
            managedRealm.admin().workflows().workflow(workflowId).activate(ResourceType.USERS.name(), resourceId, "5D");
        }

        runScheduledSteps(Duration.ZERO);

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertNull(user.getAttributes().get("message"));
        }));

        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            try {
                assertNotNull(user.getAttributes().get("message"));
            } finally {
                user.removeAttribute("message");
            }
        }));

        // using seconds as the notBefore parameter just to check if this format is also working properly
        managedRealm.admin().workflows().workflow(workflowId).activate(ResourceType.USERS.name(), resourceId, String.valueOf(Duration.ofDays(10).toSeconds()));

        runScheduledSteps(Duration.ZERO);

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertNull(user.getAttributes().get("message"));
        }));

        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertNull(user.getAttributes().get("message"));
        }));

        runScheduledSteps(Duration.ofDays(11));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertNotNull(user.getAttributes().get("message"));
        }));
    }

    @Test
    public void testDeactivateWorkflowForResource() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("One")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                            .of(SetUserAttributeStepProviderFactory.ID)
                            .withConfig("workflowOne", "first")
                            .after(Duration.ofDays(5))
                            .build(),
                        WorkflowStepRepresentation.create()
                            .of(NotifyUserStepProviderFactory.ID)
                            .after(Duration.ofDays(5))
                            .build()
                )
                .build()).close();
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("Two")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("workflowTwo", "second")
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create()
                                .of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                )
                .build()).close();

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(2));
        String workflowOneId = workflows.stream().filter(w -> w.getName().equals("One")).findFirst().orElseThrow(IllegalStateException::new).getId();

        // create a new user - should bind the user to the workflow and set up the first step in both workflows
        String id = ApiUtil.getCreatedId(managedRealm.admin().users().create(getUserRepresentation()));

        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();

            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertThat(user.getAttributes().keySet(), hasItems("workflowOne", "workflowTwo"));

            // Verify that the steps are scheduled for the user
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            List<WorkflowRepresentation> scheduledWorkflows = provider.getScheduledWorkflowsByResource(user.getId()).toList();
            assertNotNull(scheduledWorkflows, "Two workflow steps should have been scheduled for the user " + user.getUsername());
            assertThat(scheduledWorkflows, hasSize(2));
        });

        //deactivate workflow One
        managedRealm.admin().workflows().workflow(workflowOneId).deactivate(ResourceType.USERS.name(), id);

        runOnServer.run(session -> {
            // Verify that there is single step scheduled for the user
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            List<WorkflowRepresentation> scheduledWorkflows = provider.getScheduledWorkflowsByResource(id).toList();
            assertThat(scheduledWorkflows, hasSize(1));
        });
    }

    private UserRepresentation getUserRepresentation() {
        return UserConfigBuilder.create().username("alice")
                .email("alice@wonderland.org")
                .firstName("Alice")
                .lastName("Wonderland")
                .enabled(true)
                .password("alice")
                .build();
    }
}
