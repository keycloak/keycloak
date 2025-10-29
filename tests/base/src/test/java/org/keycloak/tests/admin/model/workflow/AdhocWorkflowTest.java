package org.keycloak.tests.admin.model.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

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
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .withSteps(WorkflowStepRepresentation.create()
                        .of(SetUserAttributeStepProviderFactory.ID)
                        .withConfig("message", "message")
                        .build())
                .build()).close();

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));
        WorkflowRepresentation workflow = workflows.get(0);

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
            String id = ApiUtil.getCreatedId(response);
            try {
                managedRealm.admin().workflows().workflow(workflow.getId()).bind(ResourceType.USERS.name(), id, Duration.ofDays(5).toMillis());
            } catch (Exception e) {
                assertThat(e, instanceOf(BadRequestException.class));
            }
        }
    }

    @Test
    public void testRunAdHocScheduledWorkflow() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .withSteps(WorkflowStepRepresentation.create()
                        .of(SetUserAttributeStepProviderFactory.ID)
                        .after(Duration.ofDays(5))
                        .withConfig("message", "message")
                        .build())
                .build()).close();

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));
        WorkflowRepresentation workflow = workflows.get(0);

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
            String id = ApiUtil.getCreatedId(response);
            managedRealm.admin().workflows().workflow(workflow.getId()).bind(ResourceType.USERS.name(), id);
        }
    }

    @Test
    public void testRunAdHocImmediateWorkflow() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .withSteps(WorkflowStepRepresentation.create()
                        .of(SetUserAttributeStepProviderFactory.ID)
                        .withConfig("message", "message")
                        .build())
                .build()).close();

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));
        WorkflowRepresentation workflow = workflows.get(0);

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
            String id = ApiUtil.getCreatedId(response);
            managedRealm.admin().workflows().workflow(workflow.getId()).bind(ResourceType.USERS.name(), id);
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
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .withSteps(WorkflowStepRepresentation.create()
                        .of(SetUserAttributeStepProviderFactory.ID)
                        .withConfig("message", "message")
                        .build())
                .build()).close();

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));
        WorkflowRepresentation workflow = workflows.get(0);
        String id;

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
            id = ApiUtil.getCreatedId(response);
            managedRealm.admin().workflows().workflow(workflow.getId()).bind(ResourceType.USERS.name(), id, Duration.ofDays(5).toMillis());
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

        managedRealm.admin().workflows().workflow(workflow.getId()).bind(ResourceType.USERS.name(), id, Duration.ofDays(10).toMillis());

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
