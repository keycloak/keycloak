package org.keycloak.tests.url;

import java.io.IOException;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.server.KeycloakUrls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

final class HostnameV2Assertions {

    static final String REALM_FRONTEND_NAME = "frontendUrlRealm";
    static final String REALM_FRONTEND_URL = "https://realmFrontend.localtest.me:445";

    private HostnameV2Assertions() {
    }

    static void assertFrontendAndBackendUrls(OAuthClient oAuthClient, String realm,
                                             String expectedFrontendUrl, String expectedBackendUrl) {
        final OIDCConfigurationRepresentation config = oAuthClient.realm(realm).doWellKnownRequest();
        final String realmPath = "/realms/" + realm;

        assertThat("issuer for realm " + realm, config.getIssuer(), is(expectedFrontendUrl + realmPath));
        assertThat("authorization endpoint for realm " + realm,
                config.getAuthorizationEndpoint(), is(expectedFrontendUrl + realmPath + "/protocol/openid-connect/auth"));
        assertThat("token endpoint for realm " + realm,
                config.getTokenEndpoint(), is(expectedBackendUrl + realmPath + "/protocol/openid-connect/token"));
        assertThat("userinfo endpoint for realm " + realm,
                config.getUserinfoEndpoint(), is(expectedBackendUrl + realmPath + "/protocol/openid-connect/userinfo"));
    }

    static void assertAdminUrls(SimpleHttp simpleHttp, KeycloakUrls keycloakUrls, String realm,
                                String expectedFrontendUrl, String expectedAdminUrl) throws IOException {
        final String adminConsoleUrl = keycloakUrls.getBase() + "/admin/" + realm + "/console";
        final String adminIndexPage = simpleHttp.doGet(adminConsoleUrl).asString();

        assertThat("authServerUrl in admin console for realm " + realm,
                adminIndexPage, containsString("\"authServerUrl\": \"" + expectedFrontendUrl + "\""));
        assertThat("authUrl in admin console for realm " + realm,
                adminIndexPage, containsString("\"authUrl\": \"" + expectedAdminUrl + "\""));
    }
}
