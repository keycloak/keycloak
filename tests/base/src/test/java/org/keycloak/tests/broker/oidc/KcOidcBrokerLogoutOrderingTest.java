package org.keycloak.tests.broker.oidc;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.config.RequestConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerLogoutOrderingTest extends AbstractKcOidcBrokerTest {

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectHttpServer
    HttpServer httpServer;

    @Test
    public void notifyDownstreamBeforeBrowserVisitsUpstream() throws Exception {
        logoutWithoutInitialUpstreamVisit(200);
    }

    @Test
    public void failedDownstreamNotificationStillAllowsUpstreamLogout() throws Exception {
        logoutWithoutInitialUpstreamVisit(503);
    }

    private void logoutWithoutInitialUpstreamVisit(int receiverStatus) throws Exception {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put("backchannelSupported", "false");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);

        // Exercise the browser callback independently of upstream-initiated backchannel logout.
        ClientResource providerClient = providerRealm.admin().clients().get(
                providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0).getId());
        var providerRepresentation = providerClient.toRepresentation();
        providerRepresentation.setAdminUrl("");
        providerRepresentation.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "");
        providerClient.update(providerRepresentation);
        assertNull(providerClient.toRepresentation().getAttributes().get(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL));

        String path = "/logout-ordering-receiver";
        AtomicInteger requests = new AtomicInteger();
        httpServer.createContext(path, exchange -> {
            exchange.getRequestBody().readAllBytes();
            requests.incrementAndGet();
            HttpServerUtil.sendResponse(exchange, receiverStatus, null);
        });
        try {
            ClientResource client = consumerRealm.admin().clients().get(
                    consumerRealm.admin().clients().findByClientId(CONSUMER_BROKER_APP_CLIENT_ID).get(0).getId());
            client.update(ClientBuilder.update(client.toRepresentation())
                    .frontchannelLogout(false)
                    .attribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL,
                            "http://127.0.0.1:" + httpServer.getAddress().getPort() + path)
                    .attribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true")
                    .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
                    .build());

            loginUser();
            AccessTokenResponse tokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
            assertEquals(200, tokens.getStatusCode());
            String consumerCookies = cookies(consumerRealm.getBaseUrl());
            String providerCookies = cookies(providerRealm.getBaseUrl());
            SimpleHttp noRedirects = simpleHttp.withRequestConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
            String upstream;
            try (SimpleHttpResponse response = noRedirects.doGet(oauth.logoutForm()
                    .idTokenHint(tokens.getIdToken()).postLogoutRedirectUri(oauth.getRedirectUri())
                    .state("ordering-state").build()).header("Cookie", consumerCookies).asResponse()) {
                assertEquals(302, response.getStatus());
                upstream = response.getFirstHeader("Location");
                assertNotNull(upstream);
                assertTrue(upstream.startsWith(providerRealm.getBaseUrl() + "/protocol/openid-connect/logout?"));
            }

            assertEquals(1, requests.get(), "The client must be notified even if the browser stops at this redirect");

            String callback;
            try (SimpleHttpResponse response = noRedirects.doGet(upstream).header("Cookie", providerCookies).asResponse()) {
                assertEquals(302, response.getStatus());
                callback = response.getFirstHeader("Location");
                assertNotNull(callback);
                assertTrue(callback.startsWith(consumerRealm.getBaseUrl() + "/broker/" + getIdpAlias() + "/endpoint/logout_response?"));
            }
            try (SimpleHttpResponse response = noRedirects.doGet(callback).header("Cookie", consumerCookies).asResponse()) {
                assertEquals(302, response.getStatus());
                assertEquals(oauth.getRedirectUri() + "?state=ordering-state", response.getFirstHeader("Location"));
            }
            assertEquals(1, requests.get(), "Finishing browser logout must not repeat the earlier delivery");
        } finally {
            httpServer.removeContext(path);
        }
    }

    private String cookies(String realmBaseUrl) {
        webDriver.open(realmBaseUrl + "/.well-known/openid-configuration");
        return webDriver.driver().manage().getCookies().stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
    }
}
