/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.tests.oauth.tokenexchange;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.clientpolicy.condition.ClientScopesCondition;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.GrantTypeCondition;
import org.keycloak.services.clientpolicy.condition.GrantTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.DownscopeAssertionGrantEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.JWTClaimEnforcerExecutor;
import org.keycloak.services.clientpolicy.executor.JWTClaimEnforcerExecutorFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.providers.client.policies.executor.TestRaiseExceptionExecutor;
import org.keycloak.tests.providers.client.policies.executor.TestRaiseExceptionExecutorFactory;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


@KeycloakIntegrationTest(config = StandardBaseTokenExchangeClientPoliciesV2Test.ServerConfig.class)
public class StandardBaseTokenExchangeClientPoliciesV2Test extends AbstractBaseTokenExchangeTest {

    private static final String PROFILE_NAME = "MyProfile";
    private static final String POLICY_NAME = "MyPolicy";

    @Test
    void testClientPolicies() throws Exception {
        // Create executor configuration
        TestRaiseExceptionExecutor.Configuration executorConfig = new TestRaiseExceptionExecutor.Configuration();
        executorConfig.setEvents(List.of(ClientPolicyEvent.TOKEN_EXCHANGE_REQUEST));

        // Create and update client profile
        realm.updateClientProfile(List.of(
                ClientProfileBuilder.create()
                        .name(PROFILE_NAME)
                        .description("Profilo")
                        .executor(TestRaiseExceptionExecutorFactory.PROVIDER_ID, executorConfig)
                        .build()
        ));

        // Create client scopes condition configuration
        ClientScopesCondition.Configuration scopesConfig = new ClientScopesCondition.Configuration();
        scopesConfig.setType(ClientScopesConditionFactory.ANY);
        scopesConfig.setScopes(List.of("optional-scope2"));

        // Create grant type condition configuration
        GrantTypeCondition.Configuration grantTypeConfig = ClientPolicyBuilder.grantTypeConditionConfiguration(
                false, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE
        );

        // Create and update client policy
        realm.updateClientPolicy(List.of(
                ClientPolicyBuilder.create()
                        .name(POLICY_NAME)
                        .description("Client Scope Policy")
                        .condition(ClientScopesConditionFactory.PROVIDER_ID, scopesConfig)
                        .condition(GrantTypeConditionFactory.PROVIDER_ID, grantTypeConfig)
                        .profile(PROFILE_NAME)
                        .build()
        ));

        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret").getAccessToken();

        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), null);
        assertAudiencesAndScopes(response, john, List.of("target-client1"), List.of("default-scope1"));

        //block token exchange request if optional-scope2 is requested
        oauth.scope("optional-scope2");
        response  = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client2"), null);
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        Assertions.assertEquals(ClientPolicyEvent.TOKEN_EXCHANGE_REQUEST.toString(), response.getError());
        Assertions.assertEquals("Exception thrown intentionally", response.getErrorDescription());
    }

    @Test
    public void testDownscopeClientPolicies() throws Exception {
        // Create and update client profile
        realm.updateClientProfile(List.of(
                ClientProfileBuilder.create()
                        .name(PROFILE_NAME)
                        .description("Profile")
                        .executor(DownscopeAssertionGrantEnforcerExecutorFactory.PROVIDER_ID, null)
                        .build()
        ));

        // Create grant type condition configuration
        GrantTypeCondition.Configuration grantTypeConfig = ClientPolicyBuilder.grantTypeConditionConfiguration(
                false, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE
        );

        // Create and update client policy
        realm.updateClientPolicy(List.of(
                ClientPolicyBuilder.create()
                        .name(POLICY_NAME)
                        .description("Client Scope Policy")
                        .condition(GrantTypeConditionFactory.PROVIDER_ID, grantTypeConfig)
                        .profile(PROFILE_NAME)
                        .build()
        ));

        // request initial token with optional scope optional-scope2
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret", "optional-scope2").getAccessToken();
        AccessToken token = TokenVerifier.create(accessToken, AccessToken.class).parse().getToken();
        assertScopes(token, List.of("email", "profile", "optional-scope2"));

        // request with the all the scopes allowed in the initial token, all are optional in requester-client
        // only those should be there, even default-scope1 is supressed
        oauth.scope("email profile optional-scope2");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertAudiencesAndScopes(response, john, List.of("target-client2"), List.of("email", "profile", "optional-scope2"));

        // exchange with downscope to only optional-scope2
        oauth.scope("optional-scope2");
        response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertAudiencesAndScopes(response, john, List.of("target-client2"), List.of("optional-scope2"));

        // exchange for a invisible scope returns error although it is added by default
        oauth.scope("basic optional-scope2");
        response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
        assertEquals("Scopes [basic] not present in the initial access token [optional-scope2, profile, email]",
                response.getErrorDescription());

        // exchange for another optional that is not in the token
        oauth.scope("optional-requester-scope");
        response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
        assertEquals("Scopes [optional-requester-scope] not present in the initial access token [optional-scope2, profile, email]",
                response.getErrorDescription());

        // exchange for a optional that is not in initial token
        oauth.scope("default-scope1");
        response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
        assertEquals("Scopes [default-scope1] not present in the initial access token [optional-scope2, profile, email]",
                response.getErrorDescription());
    }

    @Test
    public void testJWTClaimClientPolicies() throws Exception {
        testJWTClaimClientPolicies("username", "testuser", "testuser", true, null);
        testJWTClaimClientPolicies("username", "puppa", "testuser", false, "Value for claim 'username' not allowed");
        testJWTClaimClientPolicies("username", "admin", "^(admin|service|test-[0-9]+)$", true, null);
        testJWTClaimClientPolicies("username", "test-12345", "^(admin|service|test-[0-9]+)$", true, null);
        testJWTClaimClientPolicies("username", "unknown-username", "^(admin|service|test-[0-9]+)$", false, "Value for claim 'username' not allowed");
        testJWTClaimClientPolicies("username", "testuser", null, true, "Value for claim 'username' not allowed");
        testJWTClaimClientPolicies("username", null, null, false, "Required claim 'username' is missing from the token");
    }

    private void testJWTClaimClientPolicies(String claimName, String claimValue, String executorRegex, boolean success, String errorMessage) throws Exception {
        // Add protocol mapper to subject-client
        Response createMapperResponse = subjectClient.admin().getProtocolMappers().createMapper(
                ModelToRepresentation.toRepresentation(
                        HardcodedClaim.create(claimName, claimName, claimValue, "String", true, true, true)
                )
        );
        String mapperId = ApiUtil.getCreatedId(createMapperResponse);
        createMapperResponse.close();

        try {
            // Create executor configuration
            JWTClaimEnforcerExecutor.Configuration claimsConfig = new JWTClaimEnforcerExecutor.Configuration();
            claimsConfig.setClaimName(claimName);
            claimsConfig.setAllowedValue(executorRegex);

            // Create and update client profile
            realm.updateClientProfile(List.of(
                    ClientProfileBuilder.create()
                            .name(PROFILE_NAME)
                            .description("Profile")
                            .executor(JWTClaimEnforcerExecutorFactory.PROVIDER_ID, claimsConfig)
                            .build()
            ));

            // Create grant type condition configuration
            GrantTypeCondition.Configuration grantTypeConfig = ClientPolicyBuilder.grantTypeConditionConfiguration(
                    false, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE
            );

            // Create and update client policy
            realm.updateClientPolicy(List.of(
                    ClientPolicyBuilder.create()
                            .name(POLICY_NAME)
                            .description("Client Scope Policy")
                            .condition(GrantTypeConditionFactory.PROVIDER_ID, grantTypeConfig)
                            .profile(PROFILE_NAME)
                            .build()
            ));

            String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret").getAccessToken();
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);

            if (success) {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
            } else {
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
                assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
                assertEquals(errorMessage, response.getErrorDescription());
            }
        } finally {
            // Remove protocol mapper
            subjectClient.admin().getProtocolMappers().delete(mapperId);
        }
    }

    protected static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
