/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.forms;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestCleanup;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.TestApp;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectTestApp;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RepresentationUtils;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class RPInitiatedFrontChannelLogoutTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectUser(config = TestUserConfig.class)
    ManagedUser managedUser;

    @InjectTestApp
    TestApp testApp;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @TestSetup
    public void testSetup() {
        ClientResource testAppResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), oauth.getClientId());
        ClientRepresentation testAppRep = ClientBuilder.update(testAppResource.toRepresentation())
                .name("My Testing App")
                .frontchannelLogout(true)
                .attribute(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, testApp.getFrontChannelLogoutUri())
                .build();
        testAppResource.update(testAppRep);
    }

    @TestCleanup
    public void testCleanup() {
        ClientResource testAppResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), oauth.getClientId());
        ClientRepresentation testAppRep = new DefaultOAuthClientConfiguration().configure(ClientBuilder.create()).build();
        testAppResource.update(testAppRep);
    }

    @BeforeEach
    void beforeEach() {
        testApp.kcAdmin().clear();
    }

    @Test
    void testFrontChannelLogoutWithPostLogoutRedirectUri() throws Exception {
        oauth.doLogin(managedUser.getUsername(), managedUser.getPassword());
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String idTokenString = tokenResponse.getIdToken();
        oauth.logoutForm().idTokenHint(idTokenString)
                .postLogoutRedirectUri(testApp.getRedirectionUri()).open();
        LogoutToken logoutToken = testApp.kcAdmin().getFrontChannelLogoutToken();
        Assertions.assertNotNull(logoutToken);

        IDToken idToken = new JWSInput(idTokenString).readJsonContent(IDToken.class);

        Assertions.assertEquals(logoutToken.getIssuer(), idToken.getIssuer());
        Assertions.assertEquals(logoutToken.getSid(), idToken.getSessionId());
    }

    @Test
    void testFrontChannelLogoutWithoutSessionRequired() throws Exception {
        ClientResource testAppResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), oauth.getClientId());
        ClientRepresentation original = testAppResource.toRepresentation();
        managedRealm.cleanup().add(r -> r.clients().get(original.getId()).update(original));
        ClientRepresentation updated = RepresentationUtils.clone(original);
        updated.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED, "false");
        testAppResource.update(updated);

        oauth.doLogin(managedUser.getUsername(), managedUser.getPassword());
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String idTokenString = tokenResponse.getIdToken();
        oauth.logoutForm().idTokenHint(idTokenString)
                .postLogoutRedirectUri(testApp.getRedirectionUri()).open();
        LogoutToken logoutToken = testApp.kcAdmin().getFrontChannelLogoutToken();
        Assertions.assertNotNull(logoutToken);

        Assertions.assertNull(logoutToken.getIssuer());
        Assertions.assertNull(logoutToken.getSid());
    }

    @Test
    void testFrontChannelLogout() throws Exception {
        oauth.doLogin(managedUser.getUsername(), managedUser.getPassword());
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String idTokenString = tokenResponse.getIdToken();
        oauth.logoutForm().idTokenHint(idTokenString).open();
        LogoutToken logoutToken = testApp.kcAdmin().getFrontChannelLogoutToken();
        Assertions.assertNotNull(logoutToken);
        IDToken idToken = new JWSInput(idTokenString).readJsonContent(IDToken.class);
        Assertions.assertEquals(logoutToken.getIssuer(), idToken.getIssuer());
        Assertions.assertEquals(logoutToken.getSid(), idToken.getSessionId());
        Assertions.assertEquals("Logging out", driver.driver().getTitle());
        Assertions.assertTrue(driver.driver().getPageSource().contains("You are logging out from following apps"));
        Assertions.assertTrue(driver.driver().getPageSource().contains("My Testing App"));
    }

    @Test
    void testFrontChannelLogoutRedirectUriIsEscapedInJs() {
        String specialCharsUri = testApp.getRedirectionUri() + "/');alert(document.cookie);//";

        ClientResource testAppResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), oauth.getClientId());
        ClientRepresentation original = testAppResource.toRepresentation();
        managedRealm.cleanup().add(r -> r.clients().get(original.getId()).update(original));
        ClientRepresentation updated = RepresentationUtils.clone(original);
        updated.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, testApp.getRedirectionUri() + "/*");
        testAppResource.update(updated);

        oauth.doLogin(managedUser.getUsername(), managedUser.getPassword());
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String logoutUrl = oauth.logoutForm().idTokenHint(tokenResponse.getIdToken())
                .postLogoutRedirectUri(specialCharsUri).build();

        driver.driver().get(keycloakUrls.getBase());
        String pageSource = (String) ((org.openqa.selenium.JavascriptExecutor) driver.driver()).executeScript(
                "var xhr = new XMLHttpRequest();" +
                "xhr.open('GET', arguments[0], false);" +
                "xhr.send();" +
                "return xhr.responseText;", logoutUrl);

        Assertions.assertFalse(pageSource.contains("window.location.replace('"),
                "Redirect URI must not be rendered in a single-quoted JS string");
        Assertions.assertTrue(pageSource.contains("window.location.replace(\"" + specialCharsUri + "\")"),
                "Redirect URI should be rendered as a double-quoted JS string via FreeMarker ?c outputformat escaping");
    }

    @Test
    void testFrontChannelLogoutCustomCSP() throws Exception {
        managedRealm.updateWithCleanup(r -> r.browserSecurityHeader(BrowserSecurityHeaders.CONTENT_SECURITY_POLICY.getKey(),
                "frame-src 'keycloak.org'; frame-ancestors 'self'; object-src 'none'; style-src 'self';"));

        oauth.doLogin(managedUser.getUsername(), managedUser.getPassword());
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String idTokenString = tokenResponse.getIdToken();
        oauth.logoutForm().idTokenHint(idTokenString).open();
        LogoutToken logoutToken = testApp.kcAdmin().getFrontChannelLogoutToken();
        Assertions.assertNotNull(logoutToken);
        IDToken idToken = new JWSInput(idTokenString).readJsonContent(IDToken.class);
        Assertions.assertEquals(logoutToken.getIssuer(), idToken.getIssuer());
        Assertions.assertEquals(logoutToken.getSid(), idToken.getSessionId());
        Assertions.assertEquals("Logging out", driver.driver().getTitle());
        Assertions.assertTrue(driver.driver().getPageSource().contains("You are logging out from following apps"));
        Assertions.assertTrue(driver.driver().getPageSource().contains("My Testing App"));
     }

    private final static class TestUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("test-user@localhost")
                    .email("test-user@localhost")
                    .password("password")
                    .name("test", "user");
        }
    }
}
