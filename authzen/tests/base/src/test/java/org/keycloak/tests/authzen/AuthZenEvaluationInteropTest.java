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
import java.util.stream.StreamSupport;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.authzen.client.AuthZenClient;
import org.keycloak.testframework.authzen.client.AuthZenClient.EvaluationResult;
import org.keycloak.testframework.authzen.client.annotations.InjectAuthZenClient;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
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

    // Maps interop subject IDs (base64-encoded, used as usernames) to display names for test output
    private static final Map<String, String> USER_NAMES = Map.of(
          "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs", "rick",
          "CiRmZDE2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs", "morty",
          "CiRmZDI2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs", "summer",
          "CiRmZDM2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs", "beth",
          "CiRmZDQ2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs", "jerry"
    );

    private static final String PDP_CLIENT_ID = "authzen-pdp";
    private static final String PDP_CLIENT_SECRET = "authzen-pdp-secret";

    @InjectRealm(fromJson = "authzen-interop-realm.json")
    ManagedRealm realm;

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
        JsonNode req = decision.request();
        String subjectId = req.path("subject").path("id").asText();
        String userName = USER_NAMES.getOrDefault(subjectId, subjectId);
        String action = req.path("action").path("name").asText();
        String resourceType = req.path("resource").path("type").asText();
        String resourceId = req.path("resource").path("id").asText();
        String expected = decision.expected() ? "ALLOW" : "DENY";
        return String.format("%s | %s %s:%s => %s", userName, action, resourceType, resourceId, expected);
    }

    private static List<InteropDecision> loadDecisions() throws IOException {
        try (InputStream is = AuthZenEvaluationInteropTest.class.getResourceAsStream(
              "decisions-authorization-api-1_0-01.json")) {
            JsonNode root = JsonSerialization.mapper.readTree(is);
            return StreamSupport.stream(root.path("evaluation").spliterator(), false)
                  .map(node -> new InteropDecision(node.get("request"), node.get("expected").asBoolean()))
                  .toList();
        }
    }

    public record InteropDecision(JsonNode request, boolean expected) {
    }
}
