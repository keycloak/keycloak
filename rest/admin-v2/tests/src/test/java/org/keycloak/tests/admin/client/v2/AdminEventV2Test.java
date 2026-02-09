/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.events.admin.OperationType;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Admin API v2 events.
 * Verifies that v2 admin events are fired with the apiVersion=v2 detail.
 */
@KeycloakIntegrationTest(config = AdminEventV2Test.AdminV2EventConfig.class)
public class AdminEventV2Test {

    public static final String HOSTNAME_LOCAL_ADMIN = "http://localhost:8080/admin/api/master/clients/v2";
    private static ObjectMapper mapper;

    @InjectHttpClient
    CloseableHttpClient client;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRealm(attachTo = "master", ref = "master")
    ManagedRealm masterRealm;

    @BeforeAll
    public static void setupMapper() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    public void setupAndClearEvents() {
        // Enable admin events on master realm
        RealmEventsConfigRepresentation eventsConfig = masterRealm.admin().getRealmEventsConfig();
        eventsConfig.setAdminEventsEnabled(true);
        eventsConfig.setAdminEventsDetailsEnabled(true);
        masterRealm.admin().updateRealmEventsConfig(eventsConfig);
        
        // Clear any existing events
        masterRealm.admin().clearAdminEvents();
    }

    @Test
    public void createClientFiresV2Event() throws Exception {
        // Create a client via v2 API
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("v2-event-test-client");
        rep.setDescription("Client to test v2 events");

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // Verify v2 events were fired
        List<AdminEventRepresentation> events = masterRealm.admin().getAdminEvents();
        
        // Should have at least 1 event
        assertThat("Should have at least 1 event", events.size(), greaterThanOrEqualTo(1));

        // Find the v2 event (has apiVersion=v2 in details)
        AdminEventRepresentation v2Event = events.stream()
                .filter(e -> e.getDetails() != null && "v2".equals(e.getDetails().get("apiVersion")))
                .findFirst()
                .orElse(null);

        assertThat("V2 event should be present with apiVersion=v2 detail", v2Event, notNullValue());
        assertThat("V2 event should have CREATE operation", v2Event.getOperationType(), is(OperationType.CREATE.toString()));

        // Cleanup
        HttpDelete deleteRequest = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/v2-event-test-client");
        setAuthHeader(deleteRequest);
        try (var response = client.execute(deleteRequest)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void updateClientFiresV2Event() throws Exception {
        // First create a client
        HttpPut createRequest = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/v2-update-test");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("v2-update-test");
        rep.setEnabled(true);
        rep.setDescription("Original description");

        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // Clear events from creation
        masterRealm.admin().clearAdminEvents();

        // Now update the client
        rep.setDescription("Updated description");
        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // Verify v2 UPDATE event was fired
        List<AdminEventRepresentation> events = masterRealm.admin().getAdminEvents();

        // Find the v2 event (has apiVersion=v2 in details)
        AdminEventRepresentation v2Event = events.stream()
                .filter(e -> e.getDetails() != null && "v2".equals(e.getDetails().get("apiVersion")))
                .findFirst()
                .orElse(null);

        assertThat("V2 event should be present for update with apiVersion=v2 detail", v2Event, notNullValue());
        assertThat("V2 event should have UPDATE operation", v2Event.getOperationType(), is(OperationType.UPDATE.toString()));

        // Cleanup
        HttpDelete deleteRequest = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/v2-update-test");
        setAuthHeader(deleteRequest);
        try (var response = client.execute(deleteRequest)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void v2EventContainsV2Representation() throws Exception {
        // Create a client via v2 API (representation details already enabled in @BeforeEach)
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("v2-rep-test-client");
        rep.setDescription("Client to test v2 representation");

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // Verify v2 event contains the v2 representation format
        List<AdminEventRepresentation> events = masterRealm.admin().getAdminEvents();

        // Find the v2 event
        AdminEventRepresentation v2Event = events.stream()
                .filter(e -> e.getDetails() != null && "v2".equals(e.getDetails().get("apiVersion")))
                .findFirst()
                .orElse(null);

        assertThat("V2 event should be present", v2Event, notNullValue());
        assertThat("V2 event should have representation", v2Event.getRepresentation(), notNullValue());
        
        // The v2 representation should contain the "protocol" field (part of v2 format)
        assertTrue(v2Event.getRepresentation().contains("\"protocol\""), 
                "V2 event representation should contain protocol field");
        assertTrue(v2Event.getRepresentation().contains("\"clientId\""), 
                "V2 event representation should contain clientId field");

        // Cleanup
        HttpDelete deleteRequest = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/v2-rep-test-client");
        setAuthHeader(deleteRequest);
        try (var response = client.execute(deleteRequest)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }
    }

    private void setAuthHeader(HttpMessage request) {
        String token = adminClient.tokenManager().getAccessTokenString();
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    public static class AdminV2EventConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
