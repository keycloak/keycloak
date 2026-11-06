package org.keycloak.tests.broker.oidc;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.config.RequestConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerLogoutTest extends AbstractKcOidcBrokerTest {

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectHttpServer
    HttpServer httpServer;

    @Test
    public void consumerInitiatedLogoutWithUpstreamBackchannelLogout() {
        configureBrowserLogoutWithUpstreamBackchannel();

        loginUser();
        String consumerUserId = AccountHelper.getUserRepresentation(consumerRealm.admin(), getUserLogin()).getId();
        String providerUserId = AccountHelper.getUserRepresentation(providerRealm.admin(), getUserLogin()).getId();
        assertEquals(1, consumerRealm.admin().users().get(consumerUserId).getUserSessions().size());
        assertEquals(1, providerRealm.admin().users().get(providerUserId).getUserSessions().size());

        AccessTokenResponse tokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(200, tokens.getStatusCode());

        String state = "broker-logout-state";
        oauth.logoutForm()
                .idTokenHint(tokens.getIdToken())
                .postLogoutRedirectUri(oauth.getRedirectUri())
                .state(state)
                .open();

        // Session removal alone is insufficient: the upstream notification must not break the
        // consumer's pending browser logout callback and lose the application's redirect/state.
        assertTrue(consumerRealm.admin().users().get(consumerUserId).getUserSessions().isEmpty());
        assertTrue(providerRealm.admin().users().get(providerUserId).getUserSessions().isEmpty());
        assertEquals(oauth.getRedirectUri() + "?state=" + state, webDriver.getCurrentUrl());
    }

    // Simulate the browser closing after upstream logout, before the consumer callback. Refresh
    // tokens must already be unusable and downstream clients notified without browser completion.
    @Test
    public void upstreamBackchannelLogoutWithoutBrowserCallback() throws Exception {
        configureBrowserLogoutWithUpstreamBackchannel();

        String logoutPath = "/broker-app-backchannel-logout";
        AtomicReference<String> logoutNotification = new AtomicReference<>();
        httpServer.createContext(logoutPath, exchange -> {
            logoutNotification.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            HttpServerUtil.sendResponse(exchange, 200, null);
        });
        try {
            ClientResource consumerClient = consumerRealm.admin().clients().get(
                    consumerRealm.admin().clients().findByClientId(CONSUMER_BROKER_APP_CLIENT_ID).get(0).getId());
            consumerClient.update(ClientBuilder.update(consumerClient.toRepresentation())
                    .frontchannelLogout(false)
                    .attribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL,
                            "http://127.0.0.1:" + httpServer.getAddress().getPort() + logoutPath)
                    .attribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true")
                    .build());

            loginUser();
            AccessTokenResponse tokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
            assertEquals(200, tokens.getStatusCode());
            String providerUserId = AccountHelper.getUserRepresentation(providerRealm.admin(), getUserLogin()).getId();

            // Carry the browser's realm-scoped cookies into the HTTP requests. An ID token alone
            // takes the non-browser logout path and would not reproduce the interrupted callback.
            String consumerCookies = browserCookies(consumerRealm.getBaseUrl());
            String providerCookies = browserCookies(providerRealm.getBaseUrl());
            SimpleHttp noRedirects = simpleHttp.withRequestConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
            String upstreamLogout;
            try (SimpleHttpResponse response = noRedirects.doGet(oauth.logoutForm()
                    .idTokenHint(tokens.getIdToken())
                    .postLogoutRedirectUri(oauth.getRedirectUri())
                    .build()).header("Cookie", consumerCookies).asResponse()) {
                assertEquals(302, response.getStatus());
                upstreamLogout = response.getFirstHeader("Location");
                assertNotNull(upstreamLogout);
                assertTrue(upstreamLogout.startsWith(providerRealm.getBaseUrl() + "/protocol/openid-connect/logout?"));
            }

            // The upstream request sends the backchannel notification synchronously. Do not follow
            // its redirect to the consumer callback, simulating the browser closing at this point.
            try (SimpleHttpResponse response = noRedirects.doGet(upstreamLogout).header("Cookie", providerCookies).asResponse()) {
                assertEquals(302, response.getStatus());
                String callback = response.getFirstHeader("Location");
                assertNotNull(callback);
                assertTrue(callback.startsWith(consumerRealm.getBaseUrl() + "/broker/" + getIdpAlias() + "/endpoint/logout_response?"));
            }
            assertTrue(providerRealm.admin().users().get(providerUserId).getUserSessions().isEmpty());

            // Capture this before attempting refresh: refresh rejection must not be what triggers
            // the downstream notification.
            String notificationBeforeRefresh = logoutNotification.get();
            AccessTokenResponse refresh = oauth.doRefreshTokenRequest(tokens.getRefreshToken());
            assertAll("Logout must take effect without the browser callback",
                    () -> assertEquals(400, refresh.getStatusCode(), "Refresh must be rejected"),
                    () -> assertEquals("invalid_grant", refresh.getError()),
                    () -> {
                        assertNotNull(notificationBeforeRefresh, "Downstream backchannel client must be notified");
                        assertTrue(notificationBeforeRefresh.contains("logout_token="));
                    });
        } finally {
            httpServer.removeContext(logoutPath);
        }
    }

    private String browserCookies(String realmBaseUrl) {
        webDriver.open(realmBaseUrl + "/.well-known/openid-configuration");
        return webDriver.driver().manage().getCookies().stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
    }

    private void configureBrowserLogoutWithUpstreamBackchannel() {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put("backchannelSupported", "false");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);

        ClientRepresentation providerClient = providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0);
        providerClient.setFrontchannelLogout(false);
        providerClient.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true");
        providerRealm.admin().clients().get(providerClient.getId()).update(providerClient);

        // Use the application callback for the final redirect, after both realms complete logout.
        ClientResource consumerClient = consumerRealm.admin().clients().get(
                consumerRealm.admin().clients().findByClientId(CONSUMER_BROKER_APP_CLIENT_ID).get(0).getId());
        consumerClient.update(ClientBuilder.update(consumerClient.toRepresentation())
                .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
                .build());
    }
}
