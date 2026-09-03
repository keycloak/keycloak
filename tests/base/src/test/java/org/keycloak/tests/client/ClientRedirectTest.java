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

import java.io.IOException;
import java.net.URI;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.BasicUserConfig;

import org.apache.http.client.config.RequestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @InjectUser(config = BasicUserConfig.class, realmRef = "redirect")
    ManagedUser managedUser;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectOAuthClient(ref = "redirect-oauth", realmRef = "redirect", lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectEvents(realmRef = "redirect")
    Events events;
    
    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @BeforeEach
    @Override
    public void before() {
        // This class does not use dynamic client registration from the base class.
    }

    /**
     * Integration test for {@link org.keycloak.services.resources.RealmsResource#getRedirect(String, String)}.
     *
     */
    @Test
    public void testClientRedirectEndpoint() {
        oauth.doLogin(managedUser.getUsername(), managedUser.getPassword());

        driver.open(managedRealm.getBaseUrl() + "/clients/launchpad-test/redirect");
        assertEquals("http://example.org/launchpad", driver.getCurrentUrl());

        driver.open(managedRealm.getBaseUrl() + "/clients/dummy-test/redirect");
        assertEquals("http://example.org/dummy/base-path", driver.getCurrentUrl());
    }

    @Test
    public void testRedirectStatusCode() throws IOException {
        oauth.doLogin(managedUser.getEmail(), managedUser.getPassword());
        String code = oauth.parseLoginResponse().getCode();
        String token = oauth.doAccessTokenRequest(code).getAccessToken();

        String redirectUrl = managedRealm.getBaseUrl() + "/clients/launchpad-test/redirect";

        int status = simpleHttp.withRequestConfig(RequestConfig.custom().setRedirectsEnabled(false).build())
                .doGet(redirectUrl)
                .auth(token)
                .asStatus();
        assertEquals(303, status);
    }

    // KEYCLOAK-7707
    @Test
    public void testRedirectToDisabledClientRedirectURI() throws Exception {
        log.debug("Creating disabled-client with redirect uri \"*\"");
        String realmName = managedRealm.getName();
        managedRealm.updateWithCleanup(r -> r.clients(ClientBuilder.create().clientId("disabled-client").enabled(false).redirectUris("*")));

        log.debug("log in");
        driver.open(managedRealm.getBaseUrl());
        driver.cookies().deleteAll();
        oauth.doLogin(managedUser.getEmail(), managedUser.getPassword());
        EventAssertion.expectLoginSuccess(events.poll());

        String code = oauth.parseLoginResponse().getCode();
        String idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);

        URI logout = KeycloakUriBuilder.fromUri(getAuthServerRoot())
                .path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
                .queryParam(OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM, "http://example.org/redirected")
                .queryParam(OIDCLoginProtocol.ID_TOKEN_HINT, idTokenHint)
                .build(realmName);

        log.debug("log out using: " + logout.toURL());
        driver.open(logout.toString());
        log.debug("Current URL: " + driver.getCurrentUrl());
        EventAssertion.assertError(events.poll())
                .type(EventType.LOGOUT_ERROR)
                .error(OAuthErrorException.INVALID_REDIRECT_URI)
                .clientId(oauth.getClientId());
        assertThat(driver.getCurrentUrl(), is(not(equalTo("http://example.org/redirected"))));
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
