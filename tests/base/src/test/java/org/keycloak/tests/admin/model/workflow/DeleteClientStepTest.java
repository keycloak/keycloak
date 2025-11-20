package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.client.DeleteClientStepProviderFactory;
import org.keycloak.models.workflow.client.DisableClientStepProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.CLIENT_ADDED;
import static org.keycloak.models.workflow.ResourceOperationType.CLIENT_LOGGED_IN;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class DeleteClientStepTest extends AbstractWorkflowTest {

    private static final String CLIENT_ID = "my-client";
    private static final String CLIENT_SECRET = "618268aa-51e6-4e64-93c4-3c0bc65b8171";

    @InjectOAuthClient(realmRef = DEFAULT_REALM_NAME)
    OAuthClient oAuthClient;

    @Test
    public void testStepRun() {
        var response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(CLIENT_ADDED.name())
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DeleteClientStepProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .build()
                ).build());
        assertEquals(201, response.getStatus());
        response.close();

        createClient();

        runScheduledSteps(Duration.ZERO);

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            assertNotNull(client);
            assertTrue(client.isEnabled());
        }));

        runScheduledSteps(Duration.ofDays(2));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            assertNull(client);
        }));
    }

    @Test
    public void testDisabledClientAfterInactivityPeriod() throws InterruptedException {
        WorkflowRepresentation workflowRepresentation = WorkflowRepresentation.withName("myworkflow")
                .onEvent(CLIENT_ADDED.toString(), CLIENT_LOGGED_IN.toString())
                .concurrency().cancelIfRunning() // this setting enables restarting the workflow
                .withSteps(
                        WorkflowStepRepresentation.create().of(DisableClientStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        try(Response response = managedRealm.admin().workflows().create(workflowRepresentation)) {
            assertEquals(201, response.getStatus());
        }

        // create client, the workflow will get attached
        String cid = createClient();

        // test running the scheduled steps
        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            assertTrue(client.isEnabled());
        }));

        // running the scheduled tasks now shouldn't pick up any step as none are due to run yet
        runScheduledSteps(Duration.ZERO);

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            assertTrue(client.isEnabled());
        }));

        // set offset to 4 days - disable should not run yet
        runScheduledSteps(Duration.ofDays(4));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            assertTrue(client.isEnabled());
        }));

        // Client login (at day 4)
        runOnServer.run(session -> Time.setOffset((int) Duration.ofDays(4).toSeconds()));
        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest().client(CLIENT_ID, CLIENT_SECRET).send();
        assertNotNull(response.getAccessToken());

        // setting the offset to 6 days should not run the disable step as the workflow restarted due to client login
        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            assertTrue(client.isEnabled());
        }));

        // setting the offset to 9 days (4 + 5) should disable the client
        runScheduledSteps(Duration.ofDays(9));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            assertFalse(client.isEnabled());
        }));

        // clean
        managedRealm.admin().clients().delete(cid).close();
    }

    private String createClient() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setName(CLIENT_ID);
        rep.setClientId(CLIENT_ID);
        rep.setFullScopeAllowed(true);
        rep.setSecret(CLIENT_SECRET);
        rep.setProtocol("openid-connect");
        rep.setPublicClient(false);
        rep.setStandardFlowEnabled(true);
        rep.setServiceAccountsEnabled(true);
        rep.setEnabled(true);
        try(Response response = managedRealm.admin().clients().create(rep)) {
            return ApiUtil.getCreatedId(response);
        }
    }
}
