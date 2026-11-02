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

package org.keycloak.tests.client.policies;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AcrCondition;
import org.keycloak.services.clientpolicy.condition.AcrConditionFactory;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientProtocolCondition;
import org.keycloak.services.clientpolicy.condition.ClientProtocolConditionFactory;
import org.keycloak.services.clientpolicy.condition.GrantTypeCondition;
import org.keycloak.services.clientpolicy.condition.GrantTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class SecureRedirectUrisLoopbackTest extends AbstractClientPoliciesTest {

    private static final String ALLOWED_REDIRECT = "https://allowed.example/callback";
    private static final List<String> LOOPBACK_HOSTS = List.of("redirect-loopback-v4.kc.test", "redirect-loopback-v6.kc.test");

    @InjectRealm(config = LoopbackRealmConfig.class)
    ManagedRealm realm;

    @InjectClient(config = AllowedClientConfig.class)
    ManagedClient allowedClient;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.client.policies")
    RunOnServerClient runOnServer;

    @ParameterizedTest
    @ValueSource(strings = {"redirect-loopback-v4.kc.test", "redirect-loopback-v6.kc.test"})
    void registrationRejectsLoopbackHostname(String hostname) throws Exception {
        enableLoopbackPolicy();
        List<String> redirects = List.of(ALLOWED_REDIRECT, "https://" + hostname + "/callback");
        String clientId = generateSuffixedName("loopback-client");
        assertPolicyError(assertThrows(ClientPolicyException.class,
                () -> createClientByAdmin(realm, clientId, OIDCLoginProtocol.LOGIN_PROTOCOL,
                        client -> client.setRedirectUris(redirects))));
        assertTrue(realm.admin().clients().findByClientId(clientId).isEmpty());

        ClientRepresentation update = allowedClient.admin().toRepresentation();
        update.setRedirectUris(redirects);
        assertPolicyError(assertThrows(BadRequestException.class, () -> allowedClient.admin().update(update)));
        assertEquals(List.of(ALLOWED_REDIRECT), allowedClient.admin().toRepresentation().getRedirectUris());
    }

    @ParameterizedTest
    @CsvSource({
            "any-client, code, openid, text/html",
            "client-type, , openid, text/html",
            "client-type, code, openid invalid-scope, application/json",
            "grant-type, code, openid, text/html",
            "acr-condition, code, openid, text/html"
    })
    void authorizationRejectsStoredLoopbackHostnameWithoutRedirecting(String condition, String responseType,
            String scope, String accept) throws Exception {
        enableLoopbackPolicy();
        useCondition(condition);
        // The realm imports this callback before the policy is enabled, as after an upgrade.
        String host = LOOPBACK_HOSTS.get(0);
        oauth.client(host, "secret").redirectUri("https://" + host + "/callback").responseType(responseType).scope(scope);
        HttpGet request = new HttpGet(oauth.loginForm().param(OAuth2Constants.ACR_VALUES, "silver").build());
        request.setHeader(HttpHeaders.ACCEPT, accept);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertForbiddenRedirect(response);
            String body = EntityUtils.toString(response.getEntity());
            assertTrue(body.contains(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI));
            if ("application/json".equals(accept)) {
                OAuth2ErrorRepresentation error = JsonSerialization.readValue(body, OAuth2ErrorRepresentation.class);
                assertEquals(OAuthErrorException.INVALID_REQUEST, error.getError());
                assertEquals(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI, error.getErrorDescription());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"par", "request-object", "default-redirect"})
    void authorizationAcceptsResolvedRedirectUri(String requestType) throws Exception {
        enableLoopbackPolicy();
        useCondition(ClientProtocolConditionFactory.PROVIDER_ID);
        oauth.client(allowedClient.getClientId(), allowedClient.getSecret()).redirectUri(ALLOWED_REDIRECT);
        HttpGet request = resolvedRedirectRequest(requestType);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 302) {
                String location = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
                assertTrue(location.startsWith(realm.getBaseUrl() + "/login-actions/authenticate"));
            } else {
                assertLoginForm(response);
            }
        }
    }

    private static void assertLoginForm(CloseableHttpResponse response) throws Exception {
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertTrue(EntityUtils.toString(response.getEntity()).contains("name=\"username\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"par", "request-object"})
    void authorizationRejectsForbiddenResolvedRedirectUri(String requestType) throws Exception {
        enableLoopbackPolicy();
        useCondition(ClientProtocolConditionFactory.PROVIDER_ID);
        String host = LOOPBACK_HOSTS.get(0);
        oauth.client(host, "secret").redirectUri("https://" + host + "/callback");
        try (CloseableHttpResponse response = httpClient.execute(resolvedRedirectRequest(requestType))) {
            assertForbiddenRedirect(response);
            assertTrue(EntityUtils.toString(response.getEntity()).contains(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI));
        }
    }

    private HttpGet resolvedRedirectRequest(String requestType) throws Exception {
        if ("par".equals(requestType)) {
            var pushed = oauth.doPushedAuthorizationRequest();
            assertEquals(201, pushed.getStatusCode());
            oauth.redirectUri(null).scope(null).responseType(null);
            return new HttpGet(oauth.loginForm().requestUri(pushed.getRequestUri()).build());
        } else if ("request-object".equals(requestType)) {
            String requestObject = new JWSBuilder().jsonContent(Map.of(
                    OAuth2Constants.CLIENT_ID, oauth.getClientId(),
                    OAuth2Constants.RESPONSE_TYPE, "code",
                    OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri(),
                    OAuth2Constants.SCOPE, "openid")).none();
            oauth.redirectUri(null);
            return new HttpGet(oauth.loginForm().request(requestObject).build());
        } else {
            assertEquals("default-redirect", requestType);
            // A fully registered redirect URI may be omitted in an OAuth 2.0 request.
            oauth.redirectUri(null).scope(null).openid(false);
            return new HttpGet(oauth.loginForm().build());
        }
    }

    private void useCondition(String condition) {
        ClientPolicyConditionConfigurationRepresentation config;
        if (ClientProtocolConditionFactory.PROVIDER_ID.equals(condition)) {
            config = new ClientProtocolCondition.Configuration(OIDCLoginProtocol.LOGIN_PROTOCOL);
        } else if (GrantTypeConditionFactory.PROVIDER_ID.equals(condition)) {
            GrantTypeCondition.Configuration grantType = new GrantTypeCondition.Configuration();
            grantType.setGrantTypes(List.of(OAuth2Constants.AUTHORIZATION_CODE));
            config = grantType;
        } else if (AcrConditionFactory.PROVIDER_ID.equals(condition)) {
            AcrCondition.Configuration acr = new AcrCondition.Configuration();
            acr.setAcrProperty("silver");
            config = acr;
        } else {
            assertEquals(AnyClientConditionFactory.PROVIDER_ID, condition);
            return;
        }
        realm.updateWithCleanup(r -> r.resetClientPolicies().clientPolicy(ClientPolicyBuilder.create()
                .name("loopback-policy")
                .profile("loopback-profile")
                .condition(condition, config)
                .build()));
    }

    @ParameterizedTest
    @CsvSource({
            "grant-type, true",
            "acr-condition, true",
            "grant-type, false",
            "acr-condition, false"
    })
    void rejectRequestRunsAfterRedirectValidation(String condition, boolean allowedRedirect) throws Exception {
        SecureRedirectUrisEnforcerExecutor.Configuration config = new SecureRedirectUrisEnforcerExecutor.Configuration();
        config.setAllowPermittedDomains(List.of("allowed\\.example"));
        realm.updateWithCleanup(r -> r.resetClientProfiles().clientProfile(ClientProfileBuilder.create()
                .name("loopback-profile")
                // Even when rejection is listed first, redirect URI validation must finish before it runs.
                .executor(RejectRequestExecutorFactory.PROVIDER_ID, null)
                .executor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID, config)
                .build()));
        useCondition(condition);

        String clientId = allowedRedirect ? allowedClient.getClientId() : LOOPBACK_HOSTS.get(0);
        String redirectUri = allowedRedirect ? ALLOWED_REDIRECT : "https://" + LOOPBACK_HOSTS.get(0) + "/callback";
        oauth.client(clientId, "secret").redirectUri(redirectUri);
        HttpGet request = new HttpGet(oauth.loginForm().param(OAuth2Constants.ACR_VALUES, "silver").build());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (allowedRedirect) {
                assertEquals(302, response.getStatusLine().getStatusCode());
                String location = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
                assertTrue(location.startsWith(ALLOWED_REDIRECT + "?"));
                assertTrue(location.contains("error=invalid_request"));
                assertTrue(new URIBuilder(location).getQueryParams().stream().anyMatch(param ->
                        "error_description".equals(param.getName()) && "Request not allowed".equals(param.getValue())));
            } else {
                assertForbiddenRedirect(response);
                assertTrue(EntityUtils.toString(response.getEntity()).contains(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI));
            }
        }
    }

    private void enableLoopbackPolicy() {
        // Check the server JVM's resolver so a missing fixture cannot hide the regression.
        assertEquals("127.0.0.1", runOnServer.fetchString(session -> InetAddress.getByName(LOOPBACK_HOSTS.get(0)).getHostAddress()));
        assertEquals("0:0:0:0:0:0:0:1", runOnServer.fetchString(session -> InetAddress.getByName(LOOPBACK_HOSTS.get(1)).getHostAddress()));

        SecureRedirectUrisEnforcerExecutor.Configuration config = new SecureRedirectUrisEnforcerExecutor.Configuration();
        config.setAllowIPv4LoopbackAddress(true);
        config.setAllowIPv6LoopbackAddress(true);
        config.setAllowPermittedDomains(List.of("allowed\\.example"));

        // Enable the policy after the default OAuth helper client has been created.
        realm.updateWithCleanup(r -> r.clientProfile(ClientProfileBuilder.create()
                .name("loopback-profile")
                .executor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID, config)
                .build())
                .clientPolicy(ClientPolicyBuilder.create()
                        .name("loopback-policy")
                        .profile("loopback-profile")
                        .condition(AnyClientConditionFactory.PROVIDER_ID, null)
                        .build()));
    }

    private static void assertForbiddenRedirect(CloseableHttpResponse response) {
        assertEquals(400, response.getStatusLine().getStatusCode());
        assertNull(response.getFirstHeader(HttpHeaders.LOCATION));
    }

    private static void assertPolicyError(ClientPolicyException error) {
        assertEquals(OAuthErrorException.INVALID_REQUEST, error.getError());
        assertEquals(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI, error.getErrorDetail());
    }

    private static void assertPolicyError(BadRequestException exception) {
        try (Response response = exception.getResponse()) {
            assertEquals(400, response.getStatus());
            OAuth2ErrorRepresentation error = response.readEntity(OAuth2ErrorRepresentation.class);
            assertEquals(OAuthErrorException.INVALID_REQUEST, error.getError());
            assertEquals(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI, error.getErrorDescription());
        }
    }

    public static class AllowedClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("allowed-client")
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .publicClient(false)
                    .secret("secret")
                    .redirectUris(ALLOWED_REDIRECT);
        }
    }

    public static class LoopbackRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // Import existing client configuration so authorization also checks URIs stored before an upgrade.
            for (String host : LOOPBACK_HOSTS) {
                realm.clients(ClientBuilder.create(host)
                        .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                        .publicClient(false)
                        .secret("secret")
                        .redirectUris("https://" + host + "/callback"));
            }
            return realm;
        }
    }
}
