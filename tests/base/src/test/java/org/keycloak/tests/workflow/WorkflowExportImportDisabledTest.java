package org.keycloak.tests.workflow;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.exportimport.ExportOptions;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.models.jpa.entities.ComponentConfigEntity;
import org.keycloak.models.jpa.entities.ComponentEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = WorkflowExportImportDisabledTest.ServerConfig.class)
public class WorkflowExportImportDisabledTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.hamcrest"})
    RunOnServerClient runOnServer;

    @Test
    public void testExportWithoutWorkflowsFeature() {
        assertNull(realm.admin().partialExport(false, false).getWorkflows());
    }

    @Test
    public void testStoredDefinitionsRemainInOfflineBackupOnly() {
        runOnServer.run(session -> {
            var model = session.getContext().getRealm();
            // Represent a definition persisted before the workflows feature was disabled.
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            ComponentEntity workflow = new ComponentEntity();
            workflow.setId(UUID.randomUUID().toString());
            workflow.setRealm(em.getReference(RealmEntity.class, model.getId()));
            workflow.setParentId(model.getId());
            workflow.setProviderType(WorkflowProvider.class.getName());
            workflow.setProviderId("default");
            Map.of("name", "stored-before-disabling-feature", "enabled", "false").forEach((name, value) -> {
                ComponentConfigEntity config = new ComponentConfigEntity();
                config.setId(UUID.randomUUID().toString());
                config.setComponent(workflow);
                config.setName(name);
                config.setValue(value);
                workflow.getComponentConfigs().add(config);
            });
            em.persist(workflow);
        });
        realm.admin().clearRealmCache();
        runOnServer.run(session -> {
            var model = session.getContext().getRealm();
            RealmRepresentation full = ExportUtils.exportRealm(session, model, false, false);
            assertThat(full.getComponents().containsKey(WorkflowProvider.class.getName()), is(true));
            ExportOptions options = new ExportOptions();
            options.setWorkflowsIncluded(false);
            assertThat(ExportUtils.exportRealm(session, model, options, false).getComponents()
                    .containsKey(WorkflowProvider.class.getName()), is(false));
        });
        RealmRepresentation exported = realm.admin().partialExport(false, false);
        assertNull(exported.getWorkflows());
        assertThat(exported.getComponents().containsKey(WorkflowProvider.class.getName()), is(false));
    }

    @Test
    public void testCannotSilentlyDiscardWorkflowDefinitions() {
        RealmRepresentation imported = new RealmRepresentation();
        imported.setRealm("disabled-workflow-import");
        imported.setWorkflows(List.of(WorkflowRepresentation.withName("reminder").build()));
        assertThrows(WebApplicationException.class, () -> adminClient.realms().create(imported));
        assertThrows(NotFoundException.class, () -> adminClient.realm(imported.getRealm()).toRepresentation());
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.featuresDisabled(Profile.Feature.WORKFLOWS);
        }
    }
}
