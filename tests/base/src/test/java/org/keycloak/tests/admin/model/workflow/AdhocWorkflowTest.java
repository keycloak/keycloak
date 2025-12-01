package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_ADDED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class AdhocWorkflowTest extends AbstractWorkflowTest {

    @Test
    public void testCreate() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .withSteps(WorkflowStepRepresentation.create()
                        .of(SetUserAttributeStepProviderFactory.ID)
                        .withConfig("message", "message")
                        .after(Duration.ofDays(1))
                        .build())
                .build()).close();

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));
        WorkflowRepresentation workflow = workflows.get(0);
        assertThat(workflow.getSteps(), hasSize(1));
        WorkflowStepRepresentation aggregatedStep = workflow.getSteps().get(0);
        assertThat(aggregatedStep.getUses(), is(SetUserAttributeStepProviderFactory.ID));
    }

    @Test
    public void testBindAdHocScheduledWithImmediateWorkflow() {
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

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
            String id = ApiUtil.getCreatedId(response);
            try {
                managedRealm.admin().workflows().workflow(workflowId).activate(ResourceType.USERS.name(), id, "5D");
            } catch (Exception e) {
                assertThat(e, instanceOf(BadRequestException.class));
            }
        }
    }

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

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
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

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
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

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
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
                .onEvent(USER_ADDED.name())
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
                .onEvent(USER_ADDED.name())
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
        String id = ApiUtil.getCreatedId(managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org")));

        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();

            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertThat(user.getAttributes().keySet(), hasItems("workflowOne", "workflowTwo"));

            // Verify that the steps are scheduled for the user
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            List<WorkflowStateProvider.ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByResource(user.getId());
            assertNotNull(scheduledSteps, "Two steps should have been scheduled for the user " + user.getUsername());
            assertThat(scheduledSteps, hasSize(2));
        });

        //deactivate workflow One
        managedRealm.admin().workflows().workflow(workflowOneId).deactivate(ResourceType.USERS.name(), id);

        runOnServer.run(session -> {
            // Verify that there is single step scheduled for the user
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            List<WorkflowStateProvider.ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByResource(id);
            assertThat(scheduledSteps, hasSize(1));
        });
    }

    private UserRepresentation getUserRepresentation(String username, String firstName, String lastName, String email) {
        UserRepresentation representation = new UserRepresentation();
        representation.setUsername(username);
        representation.setFirstName(firstName);
        representation.setLastName(lastName);
        representation.setEmail(email);
        representation.setEnabled(true);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        representation.setCredentials(List.of(credential));
        return representation;
    }
}
