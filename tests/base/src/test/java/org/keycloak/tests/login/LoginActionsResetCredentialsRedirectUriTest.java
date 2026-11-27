/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.login;

import java.io.IOException;

import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakUrls;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for keycloak/keycloak#53823: the forbidden-OIDC-parameter check of
 * CVE-2026-9689 applies to the reset-credentials entry point even for a SAML client.
 *
 * The reset-credentials flow creates the authentication session through
 * LoginActionsService.createAuthenticationSessionForClient, which forces the OIDC protocol
 * and a code response type onto the session regardless of the client's protocol. The flow
 * therefore appends OIDC response parameters to the redirect URI on completion, and a
 * redirect URI carrying a pre-populated OIDC parameter (code, state, ...) would fall
 * into the parameter-pollution scenario of CVE-2026-9689. The SAML exemption used by the
 * call sites completed without appending OIDC parameters must not apply here.
 */
@KeycloakIntegrationTest
public class LoginActionsResetCredentialsRedirectUriTest {

    private static final String SAML_CLIENT_ID = "saml-reset-credentials-client";
    private static final String OIDC_CLIENT_ID = "oidc-reset-credentials-client";

    @InjectRealm(config = ResetCredentialsRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectHttpClient
    HttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testResetCredentialsRejectsOidcNamedParameterEvenForSamlClient() throws IOException {
        String resetCredentialsUri = keycloakUrls.getBase() + "/realms/" + managedRealm.getName()
                + "/login-actions/reset-credentials";

        // A SAML client: the reset-credentials flow forces OIDC onto the authentication session,
        // so the forbidden-parameter protection must stay in place - the redirect URI carrying a
        // pre-populated OIDC response parameter is rejected with 400.
        assertResetCredentialsRejected(resetCredentialsUri, SAML_CLIENT_ID, "https://sp.example/acs?code=x");
        assertResetCredentialsRejected(resetCredentialsUri, SAML_CLIENT_ID, "https://sp.example/acs?state=x");

        // An OIDC client is rejected as before (CVE-2026-9689).
        assertResetCredentialsRejected(resetCredentialsUri, OIDC_CLIENT_ID, "https://sp.example/acs?state=x");

        // Control: a redirect URI without any OIDC-named parameter passes the check for both
        // clients and reaches the reset-credentials page.
        assertResetCredentialsReached(resetCredentialsUri, SAML_CLIENT_ID, "https://sp.example/acs?foo=bar");
        assertResetCredentialsReached(resetCredentialsUri, OIDC_CLIENT_ID, "https://sp.example/acs?foo=bar");
    }

    private void assertResetCredentialsRejected(String resetCredentialsUri, String clientId, String redirectUri) throws IOException {
        HttpGet get = new HttpGet(resetCredentialsUri + "?client_id=" + clientId + "&redirect_uri=" + redirectUri);
        try (CloseableHttpResponse response = ((CloseableHttpClient) httpClient).execute(get)) {
            Assertions.assertEquals(400, response.getStatusLine().getStatusCode(),
                    "reset-credentials must reject a redirect URI pre-populating an OIDC response parameter for client " + clientId);
            EntityUtils.consume(response.getEntity());
        }
    }

    private void assertResetCredentialsReached(String resetCredentialsUri, String clientId, String redirectUri) throws IOException {
        HttpGet get = new HttpGet(resetCredentialsUri + "?client_id=" + clientId + "&redirect_uri=" + redirectUri);
        try (CloseableHttpResponse response = ((CloseableHttpClient) httpClient).execute(get)) {
            Assertions.assertTrue(response.getStatusLine().getStatusCode() < 400,
                    "reset-credentials must accept a redirect URI without OIDC-named parameters for client " + clientId
                            + ", got " + response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
        }
    }

    public static class ResetCredentialsRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name("test")
                    .resetPasswordAllowed(true)
                    .clients(samlClient(), oidcClient());
        }

        private static ClientBuilder samlClient() {
            return ClientBuilder.create(SAML_CLIENT_ID)
                    .protocol("saml")
                    .redirectUris("https://sp.example/acs*");
        }

        private static ClientBuilder oidcClient() {
            return ClientBuilder.create(OIDC_CLIENT_ID)
                    .redirectUris("https://sp.example/acs*");
        }
    }
}
