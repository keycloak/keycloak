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

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;

import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.util.Matchers.statusCodeIs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

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
    public void testClientRedirectEndpoint() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/launchpad-test/redirect");
        assertEquals("http://example.org/launchpad", driver.getCurrentUrl());

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/dummy-test/redirect");
        assertEquals("http://example.org/dummy/base-path", driver.getCurrentUrl());
    }

    @Test
    public void testRedirectStatusCode() {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        String token = oauth.doAccessTokenRequest(code).getAccessToken();

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

            String code = oauth.parseLoginResponse().getCode();
            String idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
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
            events.expectLogoutError(OAuthErrorException.INVALID_REDIRECT_URI).client(AssertEvents.DEFAULT_CLIENT_ID).assertEvent();
            assertThat(driver.getCurrentUrl(), is(not(equalTo("http://example.org/redirected"))));
        } finally {
            log.debug("removing disabled-client");
            adminClient.realm("test").clients().get(clientId).remove();
        }
    }
}
