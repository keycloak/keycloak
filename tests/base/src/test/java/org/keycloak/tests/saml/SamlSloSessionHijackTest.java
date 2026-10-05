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
package org.keycloak.tests.saml;

import java.net.URI;

import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;


@KeycloakIntegrationTest
public class SamlSloSessionHijackTest extends AbstractSamlTest {

    //Victim SP — the client whose session is the attack target. Defined in testsaml.json.
    static final String VICTIM_CLIENT_ID = "http://localhost:8280/sales-post/";

    // The victim client's {@code saml_idp_initiated_sso_url_name}, used to log in with a single GET.
    static final String VICTIM_SSO_URL_NAME = "sales-post";

    // Rogue SP — a different SAML client that is not a participant in the victim's session.
    static final String ROGUE_CLIENT_ID = "http://localhost:8280/sales-post2/";

    @InjectUser(config = SloHijackUserConfig.class)
    ManagedUser victim;

    // Holds no SSO cookie, so Keycloak takes the back-channel SessionIndex branch.
    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void rogueSpCannotTerminateVictimSessionViaSessionIndex() throws Exception {
        String victimSessionIndex = loginVictimAndGetSessionIndex();

        int status = sendUnsignedLogoutRequest(ROGUE_CLIENT_ID, victimSessionIndex);

        assertEquals(400, status, "A LogoutRequest whose Issuer does not own the referenced SessionIndex must be rejected");
        assertThat("Victim's SSO session must survive a rogue SP's LogoutRequest", victim.admin().getUserSessions(), hasSize(1));
    }

    /**
     * Logs the victim into {@value #VICTIM_CLIENT_ID} using IdP-initiated SSO, which needs
     * nothing but a GET — no {@code AuthnRequest} and no SP to receive the assertion.
     */
    private String loginVictimAndGetSessionIndex() throws Exception {
        driver.open(getAuthServerRealmBase(REALM_NAME) + "/protocol/saml/clients/" + VICTIM_SSO_URL_NAME);
        loginPage.assertCurrent();
        loginPage.fillLogin(victim.getUsername(), victim.getPassword());
        loginPage.submit();

        var sessions = victim.admin().getUserSessions();
        assertThat("Victim must be logged in before the attack", sessions, hasSize(1));
        String clientUuid = samlRealm.admin().clients().findByClientId(VICTIM_CLIENT_ID).get(0).getId();

        return sessions.get(0).getId() + "::" + clientUuid;
    }

    /**
     * POSTs an unsigned SAML {@code LogoutRequest} straight at the realm's SAML endpoint and
     * returns the HTTP status.
     */
    private int sendUnsignedLogoutRequest(String issuer, String sessionIndex) throws Exception {
        URI samlEndpoint = getAuthServerSamlEndpoint(REALM_NAME);

        Document logoutRequest = new SAML2LogoutRequestBuilder()
                .destination(samlEndpoint.toString())
                .issuer(issuer)
                .sessionIndex(sessionIndex)
                .buildDocument();

        HttpPost post = SamlClient.Binding.POST.createSamlUnsignedRequest(samlEndpoint, null, logoutRequest);
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            return response.getStatusLine().getStatusCode();
        }
    }

    public static class SloHijackUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("slo-hijack-user")
                    .password("password")
                    .firstName("SLO")
                    .lastName("HijackTest")
                    .email("slo-hijack@example.org");
        }
    }
}
