/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.openshift;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.events.Details;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.openshift.OpenshiftClientStorageProviderFactory;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.OAuthClient;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.common.Profile.Feature.OPENSHIFT_INTEGRATION;
import static org.keycloak.testsuite.ProfileAssume.assumeFeatureEnabled;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;

/**
 * Test that clients can override auth flows
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@EnableFeature(OPENSHIFT_INTEGRATION)
public final class OpenshiftClientStorageTest extends AbstractTestRealmKeycloakTest {

    private static Undertow OPENSHIFT_API_SERVER;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    private LoginPage loginPage;

    @Page
    private AppPage appPage;

    @Page
    private ConsentPage consentPage;

    @Page
    private ErrorPage errorPage;

    private String userId;
    private String clientStorageId;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @BeforeClass
    public static void onBeforeClass() {
        OPENSHIFT_API_SERVER = Undertow.builder().addHttpListener(8880, "localhost", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                String uri = exchange.getRequestURI();

                if (uri.endsWith("/version/openshift") || uri.endsWith("/version")) {
                    writeResponse("openshift-version.json", exchange);
                } else if (uri.endsWith("/oapi")) {
                    writeResponse("oapi-response.json", exchange);
                } else if (uri.endsWith("/apis")) {
                    writeResponse("apis-response.json", exchange);
                } else if (uri.endsWith("/api")) {
                    writeResponse("api.json", exchange);
                } else if (uri.endsWith("/api/v1")) {
                    writeResponse("api-v1.json", exchange);
                } else if (uri.endsWith("/oapi/v1")) {
                    writeResponse("oapi-v1.json", exchange);
                } else if (uri.contains("/apis/route.openshift.io/v1")) {
                    writeResponse("apis-route-v1.json", exchange);
                } else if (uri.endsWith("/api/v1/namespaces/default")) {
                    writeResponse("namespace-default.json", exchange);
                } else if (uri.endsWith("/oapi/v1/namespaces/default/routes/proxy")) {
                    writeResponse("route-response.json", exchange);
                } else if (uri.contains("/serviceaccounts/system")) {
                    writeResponse("sa-system.json", exchange);
                } else if (uri.contains("/serviceaccounts/")) {
                    writeResponse(uri.substring(uri.lastIndexOf('/') + 1) + ".json", exchange);
                }
            }

            private void writeResponse(String file, HttpServerExchange exchange) throws IOException {
                exchange.getResponseSender().send(StreamUtil.readString(getClass().getResourceAsStream("/openshift/client-storage/" + file)));
            }
        }).build();

        OPENSHIFT_API_SERVER.start();
    }

    @AfterClass
    public static void onAfterClass() {
        if (OPENSHIFT_API_SERVER != null) {
            OPENSHIFT_API_SERVER.stop();
        }
    }

    @Before
    public void onBefore() {
        assumeFeatureEnabled(OPENSHIFT_INTEGRATION);
        ComponentRepresentation provider = new ComponentRepresentation();

        provider.setName("openshift-client-storage");
        provider.setProviderId(OpenshiftClientStorageProviderFactory.PROVIDER_ID);
        provider.setProviderType(ClientStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_OPENSHIFT_URI, "http://localhost:8880");
        provider.getConfig().putSingle(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_ACCESS_TOKEN, "token");
        provider.getConfig().putSingle(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_DEFAULT_NAMESPACE, "default");
        provider.getConfig().putSingle(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_REQUIRE_USER_CONSENT, "true");

        Response resp = adminClient.realm("test").components().add(provider);
        resp.close();
        clientStorageId = ApiUtil.getCreatedId(resp);
        getCleanup().addComponentId(clientStorageId);
    }

    @Before
    public void clientConfiguration() {
        userId = findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId();
    }

    @Test
    public void testCodeGrantFlowWithServiceAccountUsingOAuthRedirectReference() {
        String clientId = "system:serviceaccount:default:sa-oauth-redirect-reference";
        testCodeGrantFlow(clientId, "https://myapp.org/callback", () -> assertSuccessfulResponseWithoutConsent(clientId));
    }

    @Test
    public void failCodeGrantFlowWithServiceAccountUsingOAuthRedirectReference() throws Exception {
        testCodeGrantFlow("system:serviceaccount:default:sa-oauth-redirect-reference", "http://myapp.org/callback", () -> assertEquals(OAuthErrorException.INVALID_REDIRECT_URI, events.poll().getError()));
    }

    @Test
    public void testCodeGrantFlowWithServiceAccountUsingOAuthRedirectUri() {
        String clientId = "system:serviceaccount:default:sa-oauth-redirect-uri";
        testCodeGrantFlow(clientId, "http://localhost:8180/auth/realms/master/app/auth", () -> assertSuccessfulResponseWithoutConsent(clientId));
        testCodeGrantFlow(clientId, "http://localhost:8180/auth/realms/master/app/auth/second", () -> assertSuccessfulResponseWithoutConsent(clientId));
        testCodeGrantFlow(clientId, "http://localhost:8180/auth/realms/master/app/auth/third", () -> assertSuccessfulResponseWithoutConsent(clientId));
    }

    @Test
    public void testCodeGrantFlowWithUserConsent() {
        String clientId = "system:serviceaccount:default:sa-oauth-redirect-uri";
        testCodeGrantFlow(clientId, "http://localhost:8180/auth/realms/master/app/auth", () -> assertSuccessfulResponseWithConsent(clientId), "user:info user:check-access");

        ComponentResource component = testRealm().components().component(clientStorageId);
        ComponentRepresentation representation = component.toRepresentation();

        representation.getConfig().put(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_REQUIRE_USER_CONSENT, Arrays.asList("false"));
        component.update(representation);

        testCodeGrantFlow(clientId, "http://localhost:8180/auth/realms/master/app/auth", () -> assertSuccessfulResponseWithoutConsent(clientId), "user:info user:check-access");

        representation.getConfig().put(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_REQUIRE_USER_CONSENT, Arrays.asList("true"));
        component.update(representation);

        testCodeGrantFlow(clientId, "http://localhost:8180/auth/realms/master/app/auth", () -> assertSuccessfulResponseWithoutConsent(clientId, Details.CONSENT_VALUE_PERSISTED_CONSENT), "user:info user:check-access");

        testRealm().users().get(userId).revokeConsent(clientId);

        testCodeGrantFlow(clientId, "http://localhost:8180/auth/realms/master/app/auth", () -> assertSuccessfulResponseWithConsent(clientId), "user:info user:check-access");
    }

    @Test
    public void failCodeGrantFlowWithServiceAccountUsingOAuthRedirectUri() throws Exception {
        testCodeGrantFlow("system:serviceaccount:default:sa-oauth-redirect-uri", "http://myapp.org/callback", () -> assertEquals(OAuthErrorException.INVALID_REDIRECT_URI, events.poll().getError()));
    }

    private void testCodeGrantFlow(String clientId, String expectedRedirectUri, Runnable assertThat) {
        testCodeGrantFlow(clientId, expectedRedirectUri, assertThat, null);
    }

    private void testCodeGrantFlow(String clientId, String expectedRedirectUri, Runnable assertThat, String scope) {
        if (scope != null) {
            oauth.scope(scope);
        }
        oauth.clientId(clientId);
        oauth.redirectUri(expectedRedirectUri);
        driver.navigate().to(oauth.getLoginFormUrl());
        loginPage.assertCurrent();

        try {
            // Fill username+password. I am successfully authenticated
            oauth.fillLoginForm("test-user@localhost", "password");
        } catch (Exception ignore) {

        }

        assertThat.run();
    }

    private void assertSuccessfulResponseWithoutConsent(String clientId) {
        assertSuccessfulResponseWithoutConsent(clientId, null);
    }

    private void assertSuccessfulResponseWithoutConsent(String clientId, String consentDetail) {
        AssertEvents.ExpectedEvent expectedEvent = events.expectLogin().client(clientId).detail(Details.REDIRECT_URI, oauth.getRedirectUri()).detail(Details.USERNAME, "test-user@localhost");

        if (consentDetail != null) {
            expectedEvent.detail(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT);
        }

        expectedEvent.assertEvent();
        assertSuccessfulRedirect();
    }

    private void assertSuccessfulResponseWithConsent(String clientId) {
        consentPage.assertCurrent();
        driver.getPageSource().contains("user:info");
        driver.getPageSource().contains("user:check-access");
        consentPage.confirm();
        events.expectLogin().client(clientId).detail(Details.REDIRECT_URI, oauth.getRedirectUri()).detail(Details.USERNAME, "test-user@localhost").detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED).assertEvent();
        assertSuccessfulRedirect("user:info", "user:check-access");
    }

    private void assertSuccessfulRedirect(String... expectedScopes) {
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, null);
        String accessToken = tokenResponse.getAccessToken();
        Assert.assertNotNull(accessToken);

        try {
            AccessToken token = new JWSInput(accessToken).readJsonContent(AccessToken.class);

            for (String expectedScope : expectedScopes) {
                token.getScope().contains(expectedScope);
            }
        } catch (Exception e) {
            fail("Failed to parse access token");
            e.printStackTrace();
        }

        Assert.assertNotNull(tokenResponse.getRefreshToken());
        oauth.doLogout(tokenResponse.getRefreshToken(), null);
        events.clear();
    }
}
