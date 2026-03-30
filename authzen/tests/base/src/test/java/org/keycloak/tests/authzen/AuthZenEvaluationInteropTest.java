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
package org.keycloak.tests.authzen;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.authorization.authzen.AuthZen;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.authzen.client.AuthZenClient;
import org.keycloak.testframework.authzen.client.AuthZenClient.EvaluationResult;
import org.keycloak.testframework.authzen.client.annotations.InjectAuthZenClient;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs the AuthZen interop evaluation test suite defined in the OpenID AuthZen interop project.
 * Test scenarios are loaded from the decisions-authorization-api-1_0-01.json resource file.
 * Each entry in the JSON defines an AuthZen evaluation request and its expected decision.
 * <p>
 * All required Realm configuration including users, roles, client, and authorization settings is loaded from interop-realm.json.
 */
@KeycloakIntegrationTest(config = AuthZenServerConfig.class)
public class AuthZenEvaluationInteropTest {

    // Interop project subject IDs (base64-encoded, too long for Keycloak's VARCHAR(36) ID column)
    private static final String INTEROP_RICK_ID = "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs";
    private static final String INTEROP_MORTY_ID = "CiRmZDE2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs";
    private static final String INTEROP_SUMMER_ID = "CiRmZDI2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs";
    private static final String INTEROP_BETH_ID = "CiRmZDM2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs";
    private static final String INTEROP_JERRY_ID = "CiRmZDQ2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs";

    // Keycloak-compatible UUIDs for each user
    private static final String RICK_ID = "fd0614d3-c39a-4781-b7bd-8b96f5a5100d";
    private static final String MORTY_ID = "fd1614d3-c39a-4781-b7bd-8b96f5a5100d";
    private static final String SUMMER_ID = "fd2614d3-c39a-4781-b7bd-8b96f5a5100d";
    private static final String BETH_ID = "fd3614d3-c39a-4781-b7bd-8b96f5a5100d";
    private static final String JERRY_ID = "fd4614d3-c39a-4781-b7bd-8b96f5a5100d";

    // Maps interop subject IDs to Keycloak UUIDs
    private static final Map<String, String> INTEROP_TO_KC_ID = Map.of(
          INTEROP_RICK_ID, RICK_ID,
          INTEROP_MORTY_ID, MORTY_ID,
          INTEROP_SUMMER_ID, SUMMER_ID,
          INTEROP_BETH_ID, BETH_ID,
          INTEROP_JERRY_ID, JERRY_ID
    );

    private static final Map<String, String> USER_NAMES = Map.of(
          RICK_ID, "rick",
          MORTY_ID, "morty",
          SUMMER_ID, "summer",
          BETH_ID, "beth",
          JERRY_ID, "jerry"
    );

    private static final String PDP_CLIENT_ID = "authzen-pdp";
    private static final String PDP_CLIENT_SECRET = "authzen-pdp-secret";

    @InjectRealm(fromJson = "authzen-interop-realm.json")
    ManagedRealm realm;

    @InjectClient(attachTo = PDP_CLIENT_ID)
    ManagedClient pdpClient;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectAuthZenClient
    AuthZenClient authZenClient;

    @TestFactory
    Stream<DynamicTest> interopEvaluationTests() throws IOException {
        List<InteropDecision> decisions = loadDecisions();

        AccessTokenResponse tokenResponse = oauth
              .client(PDP_CLIENT_ID, PDP_CLIENT_SECRET)
              .doClientCredentialsGrantAccessTokenRequest();
        AuthZenClient.Authenticated client = authZenClient.withAccessToken(tokenResponse.getAccessToken());

        return decisions.stream().map(decision -> {
            String testName = buildTestName(decision);
            return DynamicTest.dynamicTest(testName, () -> {
                EvaluationResult result = client.evaluate(decision.request());
                assertEquals(200, result.statusCode(), "Expected 200 OK for: " + testName);
                assertEquals(decision.expected(), result.decision(), "Expected decision=" + decision.expected() + " for: " + testName);
            });
        });
    }

    private static String buildTestName(InteropDecision decision) {
        AuthZen.EvaluationRequest req = decision.request();
        String userName = USER_NAMES.getOrDefault(req.subject().id(), req.subject().id());
        String action = req.action().name();
        String resourceType = req.resource().type();
        String resourceId = req.resource().id();
        String expected = decision.expected() ? "ALLOW" : "DENY";
        return String.format("%s | %s %s:%s => %s", userName, action, resourceType, resourceId, expected);
    }

    private static List<InteropDecision> loadDecisions() throws IOException {
        try (InputStream is = AuthZenEvaluationInteropTest.class.getResourceAsStream(
              "decisions-authorization-api-1_0-01.json")) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = JsonSerialization.mapper;
            mapper = mapper.copy().configure(
                  com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            InteropDecisions decisions = mapper.readValue(is, InteropDecisions.class);
            return decisions.evaluation().stream()
                  .map(AuthZenEvaluationInteropTest::remapSubjectId)
                  .toList();
        }
    }

    private static InteropDecision remapSubjectId(InteropDecision decision) {
        AuthZen.EvaluationRequest req = decision.request();
        String kcId = INTEROP_TO_KC_ID.get(req.subject().id());
        if (kcId == null) {
            return decision;
        }
        AuthZen.Subject remapped = new AuthZen.Subject(req.subject().type(), kcId, req.subject().properties());
        return new InteropDecision(new AuthZen.EvaluationRequest(remapped, req.resource(), req.action(), req.context()), decision.expected());
    }

    public record InteropDecisions(List<InteropDecision> evaluation) {
    }

    public record InteropDecision(AuthZen.EvaluationRequest request, boolean expected) {
    }
}
