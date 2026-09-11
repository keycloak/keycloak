/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.workflow.activation;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.models.workflow.client.DisableClientStepProviderFactory;
import org.keycloak.models.workflow.conditions.ClientAttributeWorkflowConditionFactory;
import org.keycloak.models.workflow.events.ClientCreatedWorkflowEventFactory;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowScheduleRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests activation of workflows based on client creation events (Admin REST API, DCR, and Partial Import)
 * and scheduled scans with client-scoped condition evaluation.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class ClientCreationWorkflowTest extends AbstractWorkflowTest {

    @Test
    public void testActivateWorkflowOnAdminClientCreationWithCondition() {
        // create workflow with a client attribute condition
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("admin-client-created-workflow")
                .onEvent(ClientCreatedWorkflowEventFactory.ID)
                .onCondition(ClientAttributeWorkflowConditionFactory.ID + "(custom-attr:admin-val)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableClientStepProviderFactory.ID)
                                .build()
                ).build();
        managedRealm.admin().workflows().create(workflow).close();

        // 1. Create client matching the condition -> should be disabled by workflow
        ClientRepresentation matchingClient = new ClientRepresentation();
        matchingClient.setClientId("test-admin-matching-client");
        matchingClient.setEnabled(true);
        matchingClient.setAttributes(Map.of("custom-attr", "admin-val"));

        String matchingId;
        try (Response response = managedRealm.admin().clients().create(matchingClient)) {
            matchingId = ApiUtil.getCreatedId(response);
        }

        ClientResource matchingResource = managedRealm.admin().clients().get(matchingId);
        ClientRepresentation matchingRep = matchingResource.toRepresentation();
        assertThat(matchingRep, notNullValue());
        assertThat(matchingRep.getAttributes(), notNullValue());
        assertThat(matchingRep.getAttributes().get("custom-attr"), is("admin-val"));
        assertThat(matchingRep.isEnabled(), is(false));

        // 2. Create client not matching the condition -> should remain enabled
        ClientRepresentation nonMatchingClient = new ClientRepresentation();
        nonMatchingClient.setClientId("test-admin-non-matching-client");
        nonMatchingClient.setEnabled(true);
        nonMatchingClient.setAttributes(Map.of("custom-attr", "other-val"));

        String nonMatchingId;
        try (Response response = managedRealm.admin().clients().create(nonMatchingClient)) {
            nonMatchingId = ApiUtil.getCreatedId(response);
        }

        ClientResource nonMatchingResource = managedRealm.admin().clients().get(nonMatchingId);
        ClientRepresentation nonMatchingRep = nonMatchingResource.toRepresentation();
        assertThat(nonMatchingRep, notNullValue());
        assertThat(nonMatchingRep.isEnabled(), is(true));
    }

    @Test
    public void testActivateWorkflowOnDynamicClientRegistrationWithCondition() throws Exception {
        // create workflow with a client attribute condition for DCR
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("dcr-client-created-workflow")
                .onEvent(ClientCreatedWorkflowEventFactory.ID)
                .onCondition(ClientAttributeWorkflowConditionFactory.ID + "(custom-attr:dcr-val)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableClientStepProviderFactory.ID)
                                .build()
                ).build();
        managedRealm.admin().workflows().create(workflow).close();

        // configure initial access token for dynamic client registration
        ClientInitialAccessPresentation token = managedRealm.admin().clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0, 1));
        ClientRegistration reg = oauth.clientRegistration();
        reg.auth(Auth.token(token));

        // 1. Create DCR client matching the condition -> should be disabled by workflow
        ClientRepresentation dcrClient = new ClientRepresentation();
        dcrClient.setClientId("test-dcr-matching-client");
        dcrClient.setEnabled(true);
        dcrClient.setAttributes(Map.of("custom-attr", "dcr-val"));

        ClientRepresentation created = reg.create(dcrClient);
        assertThat(created, notNullValue());

        ClientResource clientResource = managedRealm.admin().clients().get(created.getId());
        ClientRepresentation representation = clientResource.toRepresentation();
        assertThat(representation, notNullValue());
        assertThat(representation.getAttributes(), notNullValue());
        assertThat(representation.getAttributes().get("custom-attr"), is("dcr-val"));
        assertThat(representation.isEnabled(), is(false));
    }

    @Test
    public void testActivateWorkflowOnOIDCDynamicClientRegistrationWithCondition() throws Exception {
        // create workflow triggering on OIDC DCR creation
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("oidc-dcr-client-created-workflow")
                .onEvent(ClientCreatedWorkflowEventFactory.ID)
                .onCondition(ClientAttributeWorkflowConditionFactory.ID + "(client.secret.creation.time:)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableClientStepProviderFactory.ID)
                                .build()
                ).build();
        managedRealm.admin().workflows().create(workflow).close();

        ClientInitialAccessPresentation token = managedRealm.admin().clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0, 1));
        ClientRegistration reg = oauth.clientRegistration();
        reg.auth(Auth.token(token));

        OIDCClientRepresentation oidcClient = new OIDCClientRepresentation();
        oidcClient.setClientName("test-oidc-dcr-matching-client");
        oidcClient.setRedirectUris(List.of("http://localhost:8080/app/*"));

        OIDCClientRepresentation created = reg.oidc().create(oidcClient);
        assertThat(created, notNullValue());

        List<ClientRepresentation> found = managedRealm.admin().clients().findByClientId(created.getClientId());
        assertThat(found.isEmpty(), is(false));
        ClientResource clientResource = managedRealm.admin().clients().get(found.get(0).getId());
        ClientRepresentation representation = clientResource.toRepresentation();
        assertThat(representation, notNullValue());
        assertThat(representation.isEnabled(), is(false));
    }

    @Test
    public void testActivateWorkflowOnPartialImportClientCreationWithCondition() {
        // create workflow with a client attribute condition for partial import
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("partial-import-client-created-workflow")
                .onEvent(ClientCreatedWorkflowEventFactory.ID)
                .onCondition(ClientAttributeWorkflowConditionFactory.ID + "(custom-attr:pi-val)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableClientStepProviderFactory.ID)
                                .build()
                ).build();
        managedRealm.admin().workflows().create(workflow).close();

        // 1. Partial import client matching condition -> should be disabled by workflow
        ClientRepresentation matchingClient = new ClientRepresentation();
        matchingClient.setClientId("test-pi-matching-client");
        matchingClient.setEnabled(true);
        matchingClient.setAttributes(Map.of("custom-attr", "pi-val"));

        PartialImportRepresentation pi = new PartialImportRepresentation();
        pi.setIfResourceExists(PartialImportRepresentation.Policy.FAIL.toString());
        pi.setClients(List.of(matchingClient));

        try (Response response = managedRealm.admin().partialImport(pi)) {
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

        List<ClientRepresentation> found = managedRealm.admin().clients().findByClientId("test-pi-matching-client");
        assertThat(found.isEmpty(), is(false));
        ClientResource matchingResource = managedRealm.admin().clients().get(found.get(0).getId());
        ClientRepresentation matchingRep = matchingResource.toRepresentation();
        assertThat(matchingRep, notNullValue());
        assertThat(matchingRep.getAttributes(), notNullValue());
        assertThat(matchingRep.getAttributes().get("custom-attr"), is("pi-val"));
        assertThat(matchingRep.isEnabled(), is(false));
    }

    @Test
    @DatabaseTest
    public void testScheduledWorkflowWithClientAttributeCondition() {
        // 1. Create pre-existing client with matching attribute
        ClientRepresentation matchingClient = new ClientRepresentation();
        matchingClient.setClientId("test-scheduled-matching-client");
        matchingClient.setEnabled(true);
        matchingClient.setAttributes(Map.of("custom-attr", "scheduled-val"));

        String matchingId;
        try (Response response = managedRealm.admin().clients().create(matchingClient)) {
            matchingId = ApiUtil.getCreatedId(response);
        }

        // 2. Create pre-existing client with non-matching attribute
        ClientRepresentation nonMatchingClient = new ClientRepresentation();
        nonMatchingClient.setClientId("test-scheduled-non-matching-client");
        nonMatchingClient.setEnabled(true);
        nonMatchingClient.setAttributes(Map.of("custom-attr", "other-val"));

        String nonMatchingId;
        try (Response response = managedRealm.admin().clients().create(nonMatchingClient)) {
            nonMatchingId = ApiUtil.getCreatedId(response);
        }

        // 3. Create scheduled workflow with has-client-attribute condition
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("scheduled-client-workflow")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").batchSize(10).build())
                .onCondition(ClientAttributeWorkflowConditionFactory.ID + "(custom-attr:scheduled-val)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableClientStepProviderFactory.ID)
                                .build()
                ).build();
        managedRealm.admin().workflows().create(workflow).close();

        // 4. Await scheduled workflow execution (evaluates toPredicate)
        Awaitility.await()
                .timeout(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    ClientResource matchingResource = managedRealm.admin().clients().get(matchingId);
                    assertThat(matchingResource.toRepresentation().isEnabled(), is(false));

                    ClientResource nonMatchingResource = managedRealm.admin().clients().get(nonMatchingId);
                    assertThat(nonMatchingResource.toRepresentation().isEnabled(), is(true));
                });
    }
}
