package org.keycloak.tests.workflow;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.ScheduledWorkflowRunner;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStepProvider;
import org.keycloak.models.workflow.events.UserAuthenticatedWorkflowEventFactory;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowScheduleRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.timer.TimerProvider;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class WorkflowExportImportTest {

    @InjectRealm(config = ExportPermissionsRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRealm(ref = "other", lifecycle = LifeCycle.METHOD)
    ManagedRealm other;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.hamcrest"})
    RunOnServerClient runOnServer;

    @Test
    public void testManageRealmExportOmitsWorkflows() {
        createWorkflow();
        try (Keycloak client = adminClientFactory.create().realm(realm.getName())
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).username("realm-manager").password("password").build()) {
            RealmRepresentation exported = client.realm(realm.getName()).partialExport(false, false);
            assertThat(exported.getRealm(), is(realm.getName()));
            assertNull(exported.getWorkflows());
            assertThat(exported.getComponents().containsKey(WorkflowProvider.class.getName()), is(false));
        }
        assertThat(realm.admin().partialExport(false, false).getWorkflows(), hasSize(1));
    }

    public static class ExportPermissionsRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create("realm-manager").password("password")
                    .email("realm-manager@localhost").firstName("Realm").lastName("Manager")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_REALM));
        }
    }

    @Test
    public void testPartialExportPreservesWorkflowConfiguration() throws IOException {
        createWorkflow();

        RealmRepresentation exported = realm.admin().partialExport(false, false);
        JsonNode json = JsonSerialization.mapper.readTree(JsonSerialization.writeValueAsBytes(exported));
        assertThat(json.toString(), containsString("Track inactive users"));
        assertThat(json.path("workflows").size(), is(1));
        assertThat(json.path("workflows").get(0).path("name").asText(), is("Track inactive users"));
        assertThat(json.path("workflows").get(0).path("steps").size(), is(1));
        assertThat(exported.getWorkflows().get(0).getSteps().get(0).getAfter(), is("2592000"));
        assertThat(exported.getComponents().containsKey(WorkflowProvider.class.getName()), is(false));
    }

    @Test
    public void testFullExportImport() throws IOException {
        createWorkflow();
        RealmRepresentation exported = runOnServer.fetch(session ->
                ExportUtils.exportRealm(session, session.getContext().getRealm(), false, false), RealmRepresentation.class);
        WorkflowRepresentation expected = realm.admin().workflows().list().get(0);
        assertThat(exported.getWorkflows(), hasSize(1));
        realm.admin().remove();
        adminClient.realms().create(exported);
        WorkflowRepresentation actual = realm.admin().workflows().list().get(0);
        assertDefinitionEquals(expected, actual);
    }

    @Test
    public void testExportSeveralRealmsInOneSession() {
        createWorkflow();
        String otherName = other.getName();
        runOnServer.run(session -> {
            var original = session.getContext().getRealm();
            session.getProvider(WorkflowProvider.class);
            assertThat(ExportUtils.exportRealm(session, original, false, false).getWorkflows(), hasSize(1));
            var otherRealm = session.realms().getRealmByName(otherName);
            assertThat(ExportUtils.exportRealm(session, otherRealm, false, false).getWorkflows(), hasSize(0));
            assertThat(session.getContext().getRealm().getId(), is(original.getId()));
        });
    }

    @Test
    public void testLegacyWorkflowComponentsImport() throws IOException {
        createWorkflow();
        WorkflowRepresentation expected = realm.admin().workflows().list().get(0);
        ComponentExportRepresentation legacy = new ComponentExportRepresentation();
        legacy.setId(expected.getId());
        legacy.setProviderId("default");
        legacy.setConfig(expected.getConfig());
        legacy.setSubComponents(new MultivaluedHashMap<>());
        for (WorkflowStepRepresentation step : expected.getSteps()) {
            ComponentExportRepresentation component = new ComponentExportRepresentation();
            component.setId(step.getId());
            component.setProviderId(step.getUses());
            component.setConfig(step.getConfig());
            legacy.getSubComponents().add(WorkflowStepProvider.class.getName(), component);
        }
        RealmRepresentation imported = new RealmRepresentation();
        imported.setRealm(realm.getName());
        imported.setComponents(new MultivaluedHashMap<>());
        imported.getComponents().add(WorkflowProvider.class.getName(), legacy);
        realm.admin().remove();
        adminClient.realms().create(imported);
        assertDefinitionEquals(expected, realm.admin().workflows().list().get(0));
    }

    private void createWorkflow() {
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("Track inactive users")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .withConfig("enabled", "false")
                .schedule(WorkflowScheduleRepresentation.create().after("30d").batchSize(10).build())
                .concurrency().cancelInProgress(UserAuthenticatedWorkflowEventFactory.ID)
                .withSteps(WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                        .after(Duration.ofDays(30)).build())
                .build();
        try (Response response = realm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

    }

    @Test
    public void testImportUsesWorkflowValidation() {
        RealmRepresentation imported = new RealmRepresentation();
        imported.setRealm("invalid-workflow-import");
        imported.setWorkflows(List.of(WorkflowRepresentation.withName("").build()));
        assertThrows(WebApplicationException.class, () -> adminClient.realms().create(imported));
        assertThrows(NotFoundException.class, () -> adminClient.realm(imported.getRealm()).toRepresentation());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s", "2147483648s"})
    public void testUnsupportedScheduleDoesNotPersist(String interval) {
        RealmRepresentation imported = new RealmRepresentation();
        imported.setRealm("unsupported-workflow-schedule");
        imported.setWorkflows(List.of(WorkflowRepresentation.withName("scheduled")
                .schedule(WorkflowScheduleRepresentation.create().after(interval).build())
                .withSteps(WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                        .after(Duration.ofDays(30)).build()).build()));
        assertThrows(WebApplicationException.class, () -> adminClient.realms().create(imported));
        assertThrows(NotFoundException.class, () -> adminClient.realm(imported.getRealm()).toRepresentation());
    }

    @Test
    public void testRolledBackDefinitionDoesNotRegisterTimer() {
        runOnServer.run(session -> {
            WorkflowRepresentation definition = WorkflowRepresentation.withName("rolled-back")
                    .schedule(WorkflowScheduleRepresentation.create().after("30d").build())
                    .withSteps(WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                            .after(Duration.ofDays(30)).build())
                    .build();
            definition.setId("rolled-back-workflow");
            session.getProvider(WorkflowProvider.class).toModel(definition);
            String task = new ScheduledWorkflowRunner(definition.getId(), session.getContext().getRealm().getId(), 2592000).getTaskName();
            assertThat(session.getProvider(TimerProvider.class).getTasks().containsKey(task), is(false));
            session.getTransactionManager().setRollbackOnly();
        });
        runOnServer.run(session -> {
            String task = new ScheduledWorkflowRunner("rolled-back-workflow", session.getContext().getRealm().getId(), 2592000).getTaskName();
            assertThat(session.getProvider(TimerProvider.class).getTasks().containsKey(task), is(false));
            assertThat(session.getProvider(WorkflowProvider.class).getWorkflows().count(), is(0L));
        });
    }

    @Test
    public void testImportedScheduleIsRegisteredAfterCommit() {
        RealmRepresentation imported = new RealmRepresentation();
        imported.setRealm(realm.getName());
        WorkflowRepresentation definition = WorkflowRepresentation.withName("scheduled-import")
                .schedule(WorkflowScheduleRepresentation.create().after("30d").build())
                .withSteps(WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                        .after(Duration.ofDays(30)).build()).build();
        definition.setId("scheduled-import-workflow");
        imported.setWorkflows(List.of(definition));
        realm.admin().remove();
        adminClient.realms().create(imported);
        runOnServer.run(session -> {
            String task = new ScheduledWorkflowRunner("scheduled-import-workflow", session.getContext().getRealm().getId(), 2592000).getTaskName();
            assertThat(session.getProvider(TimerProvider.class).getTasks().containsKey(task), is(true));
        });
    }

    private static void assertDefinitionEquals(WorkflowRepresentation expected, WorkflowRepresentation actual) throws IOException {
        ObjectNode expectedJson = (ObjectNode) JsonSerialization.mapper.readTree(JsonSerialization.writeValueAsBytes(expected));
        ObjectNode actualJson = (ObjectNode) JsonSerialization.mapper.readTree(JsonSerialization.writeValueAsBytes(actual));
        // Provider creation generates step IDs; this export restores definitions, not running executions.
        for (ObjectNode json : List.of(expectedJson, actualJson)) {
            json.path("steps").forEach(step -> ((ObjectNode) step).remove("id"));
        }
        assertThat(actualJson, is(expectedJson));
    }
}
