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

package org.keycloak.testsuite.client;

import org.junit.Test;
import org.junit.Rule;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;

import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.statusCodeIs;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ClientRedirectTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        RealmBuilder.edit(testRealm)
                .client(ClientBuilder.create().clientId("launchpad-test").baseUrl("").rootUrl("http://example.org/launchpad"))
                .client(ClientBuilder.create().clientId("dummy-test").baseUrl("/base-path").rootUrl("http://example.org/dummy"));
    }

    /**
     * Integration test for {@link org.keycloak.services.resources.RealmsResource#getRedirect(String, String)}.
     *
     * @throws Exception
     */
    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void testClientRedirectEndpoint() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/launchpad-test/redirect");
        assertEquals("http://example.org/launchpad", driver.getCurrentUrl());

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/dummy-test/redirect");
        assertEquals("http://example.org/dummy/base-path", driver.getCurrentUrl());

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/account/redirect");
        assertEquals(getAuthServerRoot().toString() + "realms/test/account/", driver.getCurrentUrl());
    }

    @Test
    public void testRedirectStatusCode() {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String token = oauth.doAccessTokenRequest(code, "password").getAccessToken();

        Client client = AdminClientUtil.createResteasyClient();
        String redirectUrl = getAuthServerRoot().toString() + "realms/test/clients/launchpad-test/redirect";
        Response response = client.target(redirectUrl).request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        assertEquals(303, response.getStatus());
        client.close();
    }

    // KEYCLOAK-7707
    @Test
    public void testRedirectToDisabledClientRedirectURI() throws Exception {
        log.debug("Creating disabled-client with redirect uri \"*\"");
        String clientId;
        try (Response create = adminClient.realm("test").clients().create(ClientBuilder.create().clientId("disabled-client").enabled(false).redirectUris("*").build())) {
            clientId = ApiUtil.getCreatedId(create);
            assertThat(create, statusCodeIs(Status.CREATED));
        }

        try {
            log.debug("log in");
            oauth.doLogin("test-user@localhost", "password");
            events.expectLogin().assertEvent();

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            String idTokenHint = oauth.doAccessTokenRequest(code,"password").getIdToken();
            events.poll();

            URI logout = KeycloakUriBuilder.fromUri(suiteContext.getAuthServerInfo().getBrowserContextRoot().toURI())
                    .path("auth" + ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
                    .queryParam(OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM, "http://example.org/redirected")
                    .queryParam(OIDCLoginProtocol.ID_TOKEN_HINT, idTokenHint)
                    .build("test");

            log.debug("log out using: " + logout.toURL());
            driver.navigate().to(logout.toURL());
            log.debug("Current URL: " + driver.getCurrentUrl());

            log.debug("check logout_error");
            events.expectLogoutError(OAuthErrorException.INVALID_REDIRECT_URI).assertEvent();
            assertThat(driver.getCurrentUrl(), is(not(equalTo("http://example.org/redirected"))));
        } finally {
            log.debug("removing disabled-client");
            adminClient.realm("test").clients().get(clientId).remove();
        }
    }
}
