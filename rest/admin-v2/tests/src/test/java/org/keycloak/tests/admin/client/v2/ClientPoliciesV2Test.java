/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.client.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.api.AdminApi;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutorFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.client.policies.TrackEventsClientPolicyExecutor;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that client policies are properly executed when creating/updating clients via the Admin API v2.
 * <p>
 * These tests verify that the client policy framework correctly intercepts REGISTER, UPDATE, REGISTERED,
 * and UPDATED events when clients are managed through the v2 API endpoints.
 * <p>
 * Note: Currently the v2 API creates clients in two phases:
 * 1. Create a minimal client with just clientId and protocol
 * 2. Update the client model with the full representation
 * <p>
 * This means client policies are triggered on the minimal representation during CREATE, 
 * which doesn't include the client authenticator type. The policy is then triggered again
 * on UPDATE with the full model.
 * 
 * @author <a href="mailto:erik.dewit@gmail.com">Erik de Wit</a>
 */
@KeycloakIntegrationTest(config = ClientPoliciesV2Test.AdminV2Config.class)
public class ClientPoliciesV2Test extends AbstractClientApiV2Test {
    private static final String PROFILE_NAME = "TestProfile";
    private static final String POLICY_NAME = "TestPolicy";
    
    @InjectHttpClient
    CloseableHttpClient client;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @AfterEach
    public void cleanup() {
        // Clean up any test clients
        cleanupClient("test-policy-client");
        cleanupClient("test-auto-config-client");
        cleanupClient("test-put-update-client");
        cleanupClient("test-patch-update-client");
        
        // Revert to builtin profiles/policies
        revertToBuiltinProfiles();
        revertToBuiltinPolicies();

        cleanupTrackEventsClientPolicyExecutor();
    }

    /**
     * Test that creating a client via POST with an unacceptable client authenticator fails
     * when a policy requires specific authenticators.
     */
    @Test
    public void createClientWithUnacceptableAuthType() throws Exception {
        // Setup policy that only allows JWT-based authenticators
        setupPolicyClientIdAndSecretNotAcceptable();

        // Try to create a client with client-secret authenticator (which should be rejected)
        HttpPost request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-policy-client");
        rep.setEnabled(true);
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        auth.setSecret("secret");
        rep.setAuth(auth);

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            // Should fail with 400 Bad Request due to policy violation
            assertEquals(400, response.getStatusLine().getStatusCode());
            String body = EntityUtils.toString(response.getEntity());
            assertThat(body, containsString("invalid_client_metadata"));
        }
    }

    /**
     * Test that creating a client via POST with an acceptable client authenticator succeeds
     * when a policy requires specific authenticators AND has a default authenticator configured.
     * 
     * Note: Due to how the v2 API creates clients (minimal representation first, then update),
     * the policy must have a default authenticator configured for creation to succeed.
     */
    @Test
    public void createClientWithAcceptableAuthType() throws Exception {
        // Setup policy that allows JWT-based authenticators AND sets a default
        setupPolicyWithAutoConfiguration();

        // Create a confidential client with an auth method - policy should allow it
        HttpPost request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-policy-client");
        rep.setEnabled(true);
        // Set auth method to one of the allowed types
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(JWTClientSecretAuthenticator.PROVIDER_ID);
        auth.setSecret("secret");
        rep.setAuth(auth);
        // Add a login flow to ensure it's treated as a confidential client
        rep.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD));

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            assertEquals(201, statusCode, "Expected 201 but got " + statusCode + ": " + body);
            OIDCClientRepresentation created = mapper.readValue(body, OIDCClientRepresentation.class);
            assertEquals("test-policy-client", created.getClientId());
            // Auth should be present for confidential clients
            assertThat(created.getAuth(), is(notNullValue()));
            assertThat(created.getAuth().getMethod(), is(JWTClientSecretAuthenticator.PROVIDER_ID));
        }
    }

    /**
     * Test that a client created without auth is treated as a public client.
     * When no auth is specified in the v2 representation, the client is created as public,
     * and public clients don't have auth information returned.
     */
    @Test
    public void publicClientWithoutAuth() throws Exception {
        // Create a client without specifying auth - should be created as public client
        HttpPost request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-auto-config-client");
        rep.setEnabled(true);
        rep.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD));

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            assertEquals(201, statusCode, "Expected 201 but got " + statusCode + ": " + body);
            OIDCClientRepresentation created = mapper.readValue(body, OIDCClientRepresentation.class);
            assertEquals("test-auto-config-client", created.getClientId());
            // Public clients don't have auth configuration
            // The auth field is null for public clients in the v2 API
        }
    }

    private OIDCClientRepresentation getPutUpdateClientRep() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-put-update-client");
        rep.setEnabled(true);
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(JWTClientSecretAuthenticator.PROVIDER_ID);
        auth.setSecret("secret");
        rep.setAuth(auth);
        return rep;
    }

    /**
     * Test that updating a client via PUT with an unacceptable client authenticator fails.
     */
    @Test
    public void updateClientViaPutWithUnacceptableAuthType() throws Exception {
        // First create a client with acceptable auth type before policy is set
        HttpPut createRequest = new HttpPut(getClientsApiUrl() + "/test-put-update-client");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = getPutUpdateClientRep();
        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            EntityUtils.consumeQuietly(response.getEntity());
        }

        // Now setup policy
        setupPolicyClientIdAndSecretNotAcceptable();

        // Try to update the client to use an unacceptable auth type
        HttpPut updateRequest = new HttpPut(getClientsApiUrl() + "/test-put-update-client");
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        rep.setAuth(new OIDCClientRepresentation.Auth());
        rep.getAuth().setMethod(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        rep.getAuth().setSecret("newsecret");

        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            // Should fail with 400 Bad Request due to policy violation
            assertEquals(400, response.getStatusLine().getStatusCode());
            String body = EntityUtils.toString(response.getEntity());
            assertThat(body, containsString("invalid_client_metadata"));
        }
    }

    /**
     * Test that updating a client via PUT with an acceptable client authenticator succeeds.
     * Creates the client before setting up the policy to avoid the initial creation issue.
     */
    @Test
    public void updateClientViaPutWithAcceptableAuthType() throws Exception {
        // First create a client BEFORE the policy is set
        HttpPut createRequest = new HttpPut(getClientsApiUrl() + "/test-put-update-client");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = getPutUpdateClientRep();

        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            EntityUtils.consumeQuietly(response.getEntity());
        }

        // Now setup policy
        setupPolicyClientIdAndSecretNotAcceptable();

        // Update the client to use another acceptable auth type
        HttpPut updateRequest = new HttpPut(getClientsApiUrl() + "/test-put-update-client");
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        rep.getAuth().setMethod(JWTClientAuthenticator.PROVIDER_ID);
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            assertEquals(200, statusCode, "Expected 200 but got " + statusCode + ": " + body);
            OIDCClientRepresentation updated = mapper.readValue(body, OIDCClientRepresentation.class);
            assertThat(updated.getAuth().getMethod(), is(JWTClientAuthenticator.PROVIDER_ID));
        }
    }

    /**
     * Test that updating a client via PATCH (merge patch) with an unacceptable client authenticator fails.
     */
    @Test
    public void updateClientViaPatchWithUnacceptableAuthType() throws Exception {
        // First create a client with acceptable auth type
        HttpPut createRequest = new HttpPut(getClientsApiUrl() + "/test-patch-update-client");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-patch-update-client");
        rep.setEnabled(true);
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(JWTClientSecretAuthenticator.PROVIDER_ID);
        auth.setSecret("secret");
        rep.setAuth(auth);

        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            EntityUtils.consumeQuietly(response.getEntity());
        }

        // Now setup policy
        setupPolicyClientIdAndSecretNotAcceptable();

        // Try to patch the client to use an unacceptable auth type
        HttpPatch patchRequest = new HttpPatch(getClientsApiUrl() + "/test-patch-update-client");
        setAuthHeader(patchRequest);
        patchRequest.setHeader(HttpHeaders.CONTENT_TYPE, AdminApi.CONTENT_TYPE_MERGE_PATCH);

        OIDCClientRepresentation patch = new OIDCClientRepresentation();
        var patchAuth = new OIDCClientRepresentation.Auth();
        patchAuth.setMethod(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        patch.setAuth(patchAuth);

        patchRequest.setEntity(new StringEntity(mapper.writeValueAsString(patch)));

        try (var response = client.execute(patchRequest)) {
            // Should fail with 400 Bad Request due to policy violation
            assertEquals(400, response.getStatusLine().getStatusCode());
            String body = EntityUtils.toString(response.getEntity());
            assertThat(body, containsString("invalid_client_metadata"));
        }
    }


    /**
     * Test that policy is applied during client update even when not changing the auth type.
     * Creates the client before setting up the policy to avoid the initial creation issue.
     */
    @Test
    public void policyAppliedOnUpdateWithoutAuthTypeChange() throws Exception {
        // Create a client BEFORE the policy is set
        HttpPut createRequest = new HttpPut(getClientsApiUrl() + "/test-put-update-client");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = getPutUpdateClientRep();

        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            EntityUtils.consumeQuietly(response.getEntity());
        }

        // Now setup policy
        setupPolicyClientIdAndSecretNotAcceptable();

        // Update client without changing auth type (just change description)
        HttpPut updateRequest = new HttpPut(getClientsApiUrl() + "/test-put-update-client");
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        rep.setDescription("Updated description");
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            // Should succeed since auth type is still acceptable
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            assertEquals(200, statusCode, "Expected 200 but got " + statusCode + ": " + body);
            OIDCClientRepresentation updated = mapper.readValue(body, OIDCClientRepresentation.class);
            assertThat(updated.getDescription(), is("Updated description"));
            assertThat(updated.getAuth().getMethod(), is(JWTClientSecretAuthenticator.PROVIDER_ID));
        }
    }

    /**
     * GET /clients/{client}
     * Policy Events: ClientPolicyEvent.VIEW
     */
    @Test
    public void getClientViewEvent() throws Exception {
        setupAlwaysAppliedTestPolicy();

        HttpGet getClient = new HttpGet(getClientsApiUrl() + "/account");
        setAuthHeader(getClient);
        try (var response = client.execute(getClient)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }

        assertClientPolicyEventIsEmitted(ClientPolicyEvent.VIEW);
    }

    /**
     * POST /clients
     * Policy Events: ClientPolicyEvent.REGISTER + ClientPolicyEvent.REGISTERED
     */
    @Test
    public void createClientRegisterEvent() throws Exception {
        setupAlwaysAppliedTestPolicy();

        HttpPost request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("client-123");
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(201));
            EntityUtils.consumeQuietly(response.getEntity());
        }

        assertClientPolicyEventIsEmitted(ClientPolicyEvent.REGISTER, ClientPolicyEvent.REGISTERED);
    }

    /**
     * PUT /clients/{client}
     * Policy Events:
     * - a) Create a new client via PUT: ClientPolicyEvent.REGISTER, ClientPolicyEvent.REGISTERED
     * - b) Update the client: ClientPolicyEvent.UPDATE, ClientPolicyEvent.UPDATED
     */
    @Test
    public void updateClientUpdateEvent() throws Exception {
        setupAlwaysAppliedTestPolicy();

        HttpPut createRequest = new HttpPut(getClientsApiUrl() + "/test-put-update-client");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = getPutUpdateClientRep();
        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            EntityUtils.consumeQuietly(response.getEntity());
        }

        assertClientPolicyEventIsEmitted(ClientPolicyEvent.REGISTER, ClientPolicyEvent.REGISTERED);

        HttpPut updateRequest = new HttpPut(getClientsApiUrl() + "/test-put-update-client");
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        rep = getPutUpdateClientRep();
        rep.setDescription("Updated");
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            EntityUtils.consumeQuietly(response.getEntity());
        }

        // for now, the VIEW is also present, but it is not required for update
        assertClientPolicyEventIsEmitted(ClientPolicyEvent.VIEW, ClientPolicyEvent.UPDATE, ClientPolicyEvent.UPDATED);
    }

    /**
     * DELETE /clients/{client}
     * Policy Events: ClientPolicyEvent.UNREGISTER
     */
    @Test
    public void deleteClientUnregisterEvent() throws Exception {
        HttpPut createRequest = new HttpPut(getClientsApiUrl() + "/test-put-update-client");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = getPutUpdateClientRep();

        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            EntityUtils.consumeQuietly(response.getEntity());
        }

        setupAlwaysAppliedTestPolicy();
        cleanupClient(rep.getClientId());

        assertClientPolicyEventIsEmitted(ClientPolicyEvent.UNREGISTER);
    }

    private void assertClientPolicyEventIsEmitted(ClientPolicyEvent... events) {
        runOnServer.run(session -> {
            TrackEventsClientPolicyExecutor executor = (TrackEventsClientPolicyExecutor) session.getProvider(ClientPolicyExecutorProvider.class, TrackEventsClientPolicyExecutor.PROVIDER_ID);
            assertNotNull(executor);
            try {
                var foundEvents = executor.getEvents();
                assertNotNull(foundEvents);
                assertThat(foundEvents, contains(events));
                assertThat(foundEvents, hasSize(events.length));
            } finally {
                executor.clearEventResult();
            }
        });
    }

    private void cleanupTrackEventsClientPolicyExecutor() {
        runOnServer.run(session -> {
            TrackEventsClientPolicyExecutor executor = (TrackEventsClientPolicyExecutor) session.getProvider(ClientPolicyExecutorProvider.class, TrackEventsClientPolicyExecutor.PROVIDER_ID);
            assertNotNull(executor);
            executor.clearEventResult();
        });
    }

    /**
     * Sets up a policy that does NOT allow client_id and secret authenticator.
     * Only JWT-based authenticators are allowed.
     */
    private void setupPolicyClientIdAndSecretNotAcceptable() throws Exception {
        setupSecureClientAuthenticatorPolicy("Test Profile/Policy that restricts client authenticators");
    }

    /**
     * Sets up a policy with auto-configuration that defaults to X509 authenticator.
     */
    private void setupPolicyWithAutoConfiguration() throws Exception {
        setupSecureClientAuthenticatorPolicy("Test Profile/Policy with auto-configuration - defaults to X509",
                config -> config.setDefaultClientAuthenticator(X509ClientAuthenticator.PROVIDER_ID));
    }

    private void setupAlwaysAppliedTestPolicy() throws Exception {
        ClientPolicyExecutorRepresentation executorRep = new ClientPolicyExecutorRepresentation();
        executorRep.setExecutorProviderId(TrackEventsClientPolicyExecutor.PROVIDER_ID);

        TrackEventsClientPolicyExecutor.Configuration config = new TrackEventsClientPolicyExecutor.Configuration();
        JsonNode configNode = JsonSerialization.mapper.readValue(
                JsonSerialization.mapper.writeValueAsBytes(config), JsonNode.class);
        executorRep.setConfiguration(configNode);

        ClientPolicyConditionRepresentation conditionRep = new ClientPolicyConditionRepresentation();
        conditionRep.setConditionProviderId(AnyClientConditionFactory.PROVIDER_ID);

        ClientPolicyConditionConfigurationRepresentation conditionConfig = new ClientPolicyConditionConfigurationRepresentation();
        JsonNode conditionConfigNode = JsonSerialization.mapper.readValue(
                JsonSerialization.mapper.writeValueAsBytes(conditionConfig), JsonNode.class);
        conditionRep.setConfiguration(conditionConfigNode);

        setupPolicy("Test Profile/Policy that handles the TrackEventsClientPolicyExecutor and verifies types", PROFILE_NAME, POLICY_NAME, executorRep, conditionRep);
    }

    private void setupSecureClientAuthenticatorPolicy(String description) throws Exception {
        setupSecureClientAuthenticatorPolicy(description, (config) -> {
        });
    }

    /**
     * Setup secure client authenticator executor
     */
    private void setupSecureClientAuthenticatorPolicy(String description, Consumer<SecureClientAuthenticatorExecutor.Configuration> configuration) throws Exception {
        ClientPolicyExecutorRepresentation executorRep = new ClientPolicyExecutorRepresentation();
        executorRep.setExecutorProviderId(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID);

        SecureClientAuthenticatorExecutor.Configuration config = new SecureClientAuthenticatorExecutor.Configuration();
        config.setAllowedClientAuthenticators(Arrays.asList(
                JWTClientAuthenticator.PROVIDER_ID,
                JWTClientSecretAuthenticator.PROVIDER_ID,
                X509ClientAuthenticator.PROVIDER_ID
        ));
        configuration.accept(config);

        // Use JsonSerialization mapper to properly serialize with @JsonProperty annotations
        JsonNode configNode = JsonSerialization.mapper.readValue(
                JsonSerialization.mapper.writeValueAsBytes(config), JsonNode.class);
        executorRep.setConfiguration(configNode);

        // Add condition for authenticated user context
        ClientPolicyConditionRepresentation conditionRep = new ClientPolicyConditionRepresentation();
        conditionRep.setConditionProviderId(ClientUpdaterContextConditionFactory.PROVIDER_ID);

        ClientPolicyConditionConfigurationRepresentation conditionConfig = new ClientPolicyConditionConfigurationRepresentation();
        conditionConfig.setConfigAsMap(
                ClientUpdaterContextConditionFactory.UPDATE_CLIENT_SOURCE,
                List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER)
        );
        JsonNode conditionConfigNode = JsonSerialization.mapper.readValue(
                JsonSerialization.mapper.writeValueAsBytes(conditionConfig), JsonNode.class);
        conditionRep.setConfiguration(conditionConfigNode);

        setupPolicy(description, PROFILE_NAME, POLICY_NAME, executorRep, conditionRep);
    }

    private void setupPolicy(String description, String profileName, String policyName, ClientPolicyExecutorRepresentation executor, ClientPolicyConditionRepresentation condition) {
        // Create profile
        ClientProfileRepresentation profileRep = new ClientProfileRepresentation();
        profileRep.setName(profileName);
        profileRep.setDescription(description);
        profileRep.setExecutors(new ArrayList<>());

        profileRep.getExecutors().add(executor);

        ClientProfilesRepresentation profilesRep = new ClientProfilesRepresentation();
        profilesRep.setProfiles(List.of(profileRep));

        adminClient.realm("master").clientPoliciesProfilesResource().updateProfiles(profilesRep);

        // Create policy
        ClientPolicyRepresentation policyRep = new ClientPolicyRepresentation();
        policyRep.setName(policyName);
        policyRep.setDescription(description);
        policyRep.setEnabled(true);
        policyRep.setProfiles(List.of(profileName));
        policyRep.setConditions(new ArrayList<>());

        policyRep.getConditions().add(condition);

        ClientPoliciesRepresentation policiesRep = new ClientPoliciesRepresentation();
        policiesRep.setPolicies(List.of(policyRep));

        adminClient.realm("master").clientPoliciesPoliciesResource().updatePolicies(policiesRep);
    }

    private void revertToBuiltinProfiles() {
        try {
            ClientProfilesRepresentation emptyProfiles = new ClientProfilesRepresentation();
            emptyProfiles.setProfiles(List.of());
            adminClient.realm("master").clientPoliciesProfilesResource().updateProfiles(emptyProfiles);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private void revertToBuiltinPolicies() {
        try {
            ClientPoliciesRepresentation emptyPolicies = new ClientPoliciesRepresentation();
            emptyPolicies.setPolicies(List.of());
            adminClient.realm("master").clientPoliciesPoliciesResource().updatePolicies(emptyPolicies);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private void cleanupClient(String clientId) {
        try {
            HttpDelete deleteRequest = new HttpDelete(getClientsApiUrl() + "/" + clientId);
            setAuthHeader(deleteRequest);
            try (var response = client.execute(deleteRequest)) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private void setAuthHeader(HttpMessage request) {
        String token = adminClient.tokenManager().getAccessTokenString();
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    public static class AdminV2Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2)
                    .dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
