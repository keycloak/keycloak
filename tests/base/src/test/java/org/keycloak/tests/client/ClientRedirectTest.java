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

package org.keycloak.tests.client;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.AdminClientUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.testsuite.util.Matchers.statusCodeIs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
@KeycloakIntegrationTest
public class ClientRedirectTest extends AbstractClientRegistrationTest {

    @InjectRealm(ref = "redirect", config = ClientRedirectRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient(ref = "redirect-oauth", realmRef = "redirect", lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @Override
    @BeforeEach
    public void before() {
        // This class does not use dynamic client registration from the base class.
    }

    /**
     * Integration test for {@link org.keycloak.services.resources.RealmsResource#getRedirect(String, String)}.
     *
     * @throws Exception
     */
    @Test
    public void testClientRedirectEndpoint() throws Exception {
        driver.driver().navigate().to(managedRealm.getBaseUrl());
        driver.driver().manage().deleteAllCookies();
        oauth.doLogin("test-user@localhost", "password");
        String realmName = managedRealm.getName();

        driver.driver().get(getAuthServerRoot().toString() + "realms/" + realmName + "/clients/launchpad-test/redirect");
        assertEquals("http://example.org/launchpad", driver.getCurrentUrl());

        driver.driver().get(getAuthServerRoot().toString() + "realms/" + realmName + "/clients/dummy-test/redirect");
        assertEquals("http://example.org/dummy/base-path", driver.getCurrentUrl());
    }

    @Test
    public void testRedirectStatusCode() {
        driver.driver().navigate().to(managedRealm.getBaseUrl());
        driver.driver().manage().deleteAllCookies();
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        String token = oauth.doAccessTokenRequest(code).getAccessToken();
        String realmName = managedRealm.getName();

        String redirectUrl = getAuthServerRoot().toString() + "realms/" + realmName + "/clients/launchpad-test/redirect";
        try (Client client = AdminClientUtil.createResteasyClient();
             Response response = client.target(redirectUrl).request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get()) {
            assertEquals(303, response.getStatus());
        }
    }

    // KEYCLOAK-7707
    @Test
    public void testRedirectToDisabledClientRedirectURI() throws Exception {
        log.debug("Creating disabled-client with redirect uri \"*\"");
        String clientId;
        String realmName = managedRealm.getName();
        try (Response create = adminClient.realm(realmName).clients().create(ClientBuilder.create().clientId("disabled-client").enabled(false).redirectUris("*").build())) {
            clientId = ApiUtil.getCreatedId(create);
            assertThat(create, statusCodeIs(Status.CREATED));
        }

        try {
            log.debug("log in");
            driver.driver().navigate().to(managedRealm.getBaseUrl());
            driver.driver().manage().deleteAllCookies();
            oauth.doLogin("test-user@localhost", "password");

            String code = oauth.parseLoginResponse().getCode();
            String idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();

            URI logout = KeycloakUriBuilder.fromUri(getAuthServerRoot())
                    .path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
                    .queryParam(OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM, "http://example.org/redirected")
                    .queryParam(OIDCLoginProtocol.ID_TOKEN_HINT, idTokenHint)
                    .build(realmName);

            log.debug("log out using: " + logout.toURL());
            driver.driver().navigate().to(logout.toString());
            log.debug("Current URL: " + driver.getCurrentUrl());
            assertThat(driver.getCurrentUrl(), is(not(equalTo("http://example.org/redirected"))));
        } finally {
            log.debug("removing disabled-client");
            adminClient.realm(realmName).clients().get(clientId).remove();
        }
    }

    private static class ClientRedirectRealmConfig extends AbstractClientRegistrationTest.ClientRegistrationRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return super.configure(realm)
                    .name("redirect")
                    .id("redirect")
                    .clients(ClientBuilder.create().clientId("launchpad-test").baseUrl("").rootUrl("http://example.org/launchpad"))
                    .clients(ClientBuilder.create().clientId("dummy-test").baseUrl("/base-path").rootUrl("http://example.org/dummy"));
        }
    }
}
