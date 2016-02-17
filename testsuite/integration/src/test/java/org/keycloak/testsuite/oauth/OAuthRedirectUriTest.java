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
package org.keycloak.testsuite.oauth;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthRedirectUriTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            ClientModel installedApp = KeycloakModelUtils.createClient(appRealm, "test-installed");
            installedApp.setEnabled(true);
            installedApp.addRedirectUri(Constants.INSTALLED_APP_URN);
            installedApp.addRedirectUri(Constants.INSTALLED_APP_URL);
            installedApp.setSecret("password");

            ClientModel installedApp2 = KeycloakModelUtils.createClient(appRealm, "test-installed2");
            installedApp2.setEnabled(true);
            installedApp2.addRedirectUri(Constants.INSTALLED_APP_URL + "/myapp");
            installedApp2.setSecret("password");

            ClientModel installedApp3 = KeycloakModelUtils.createClient(appRealm, "test-wildcard");
            installedApp3.setEnabled(true);
            installedApp3.addRedirectUri("http://example.com/foo/*");
            installedApp3.addRedirectUri("http://with-dash.example.com/foo/*");
            installedApp3.addRedirectUri("http://localhost:8081/foo/*");
            installedApp3.setSecret("password");

            ClientModel installedApp4 = KeycloakModelUtils.createClient(appRealm, "test-dash");
            installedApp4.setEnabled(true);
            installedApp4.addRedirectUri("http://with-dash.example.com");
            installedApp4.addRedirectUri("http://with-dash.example.com/foo");
            installedApp4.setSecret("password");

            ClientModel installedApp5 = KeycloakModelUtils.createClient(appRealm, "test-root-url");
            installedApp5.setEnabled(true);
            installedApp5.setRootUrl("http://with-dash.example.com");
            installedApp5.addRedirectUri("/foo");
            installedApp5.setSecret("password");

            ClientModel installedApp6 = KeycloakModelUtils.createClient(appRealm, "test-relative-url");
            installedApp6.setEnabled(true);
            installedApp6.setRootUrl("");
            installedApp6.addRedirectUri("/foo");
            installedApp6.setSecret("password");
        }
    });

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected ErrorPage errorPage;

    @Test
    public void testNoParam() throws IOException {
        oauth.redirectUri(null);
        OAuthClient.AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertNotNull(response.getCode());
        Assert.assertEquals(oauth.getCurrentRequest(), "http://localhost:8081/app");
    }

    @Test
    public void testNoParamMultipleValidUris() throws IOException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("test-app").addRedirectUri("http://localhost:8081/app2");
            }
        });

        try {
            oauth.redirectUri(null);
            oauth.openLoginForm();

            Assert.assertTrue(errorPage.isCurrent());
            Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        } finally {
            keycloakRule.update(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.getClientByClientId("test-app").removeRedirectUri("http://localhost:8081/app2");
                }
            });
        }
    }

    @Test
    public void testNoParamNoValidUris() throws IOException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("test-app").removeRedirectUri("http://localhost:8081/app/*");
            }
        });

        try {
            oauth.redirectUri(null);
            oauth.openLoginForm();

            Assert.assertTrue(errorPage.isCurrent());
            Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        } finally {
            keycloakRule.update(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.getClientByClientId("test-app").addRedirectUri("http://localhost:8081/app/*");
                }
            });
        }
    }

    @Test
    public void testNoValidUris() throws IOException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("test-app").removeRedirectUri("http://localhost:8081/app/*");
            }
        });

        try {
            oauth.redirectUri(null);
            oauth.openLoginForm();

            Assert.assertTrue(errorPage.isCurrent());
            Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        } finally {
            keycloakRule.update(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.getClientByClientId("test-app").addRedirectUri("http://localhost:8081/app/*");
                }
            });
        }
    }

    @Test
    public void testValid() throws IOException {
        oauth.redirectUri("http://localhost:8081/app");
        OAuthClient.AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertNotNull(response.getCode());
        URL url = new URL(driver.getCurrentUrl());
        Assert.assertTrue(url.toString().startsWith("http://localhost:8081/app"));
        Assert.assertTrue(url.getQuery().contains("code="));
        Assert.assertTrue(url.getQuery().contains("state="));
    }

    @Test
    public void testInvalid() throws IOException {
        oauth.redirectUri("http://localhost:8081/app2");
        oauth.openLoginForm();

        Assert.assertTrue(errorPage.isCurrent());
        Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
    }

    @Test
    public void testWithParams() throws IOException {
        oauth.redirectUri("http://localhost:8081/app?key=value");
        OAuthClient.AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertNotNull(response.getCode());
        URL url = new URL(driver.getCurrentUrl());
        Assert.assertTrue(url.toString().startsWith("http://localhost:8081/app"));
        Assert.assertTrue(url.getQuery().contains("key=value"));
        Assert.assertTrue(url.getQuery().contains("state="));
        Assert.assertTrue(url.getQuery().contains("code="));
    }

    @Test
    public void testWildcard() throws IOException {
        oauth.clientId("test-wildcard");
        checkRedirectUri("http://example.com", false);
        checkRedirectUri("http://localhost:8080", false, true);
        checkRedirectUri("http://example.com/foo", true);
        checkRedirectUri("http://example.com/foo/bar", true);
        checkRedirectUri("http://localhost:8081/foo", true, true);
        checkRedirectUri("http://localhost:8081/foo/bar", true, true);
        checkRedirectUri("http://example.com/foobar", false);
        checkRedirectUri("http://localhost:8081/foobar", false, true);
    }

    @Test
    public void testDash() throws IOException {
        oauth.clientId("test-dash");

        checkRedirectUri("http://with-dash.example.com/foo", true);
    }

    @Test
    public void testDifferentCaseInHostname() throws IOException {
        oauth.clientId("test-dash");

        checkRedirectUri("http://with-dash.example.com", true);
        checkRedirectUri("http://wiTh-dAsh.example.com", true);
        checkRedirectUri("http://with-dash.example.com/foo", true);
        checkRedirectUri("http://wiTh-dAsh.example.com/foo", true);
        checkRedirectUri("http://with-dash.eXampLe.com/foo", true);
        checkRedirectUri("http://wiTh-dAsh.eXampLe.com/foo", true);
        checkRedirectUri("http://wiTh-dAsh.eXampLe.com/Foo", false);
        checkRedirectUri("http://wiTh-dAsh.eXampLe.com/foO", false);
    }

    @Test
    public void testDifferentCaseInScheme() throws IOException {
        oauth.clientId("test-dash");

        checkRedirectUri("HTTP://with-dash.example.com", true);
        checkRedirectUri("Http://wiTh-dAsh.example.com", true);
    }

    @Test
    public void testRelativeWithRoot() throws IOException {
        oauth.clientId("test-root-url");

        checkRedirectUri("http://with-dash.example.com/foo", true);
        checkRedirectUri("http://localhost:8081/foo", false);
    }

    @Test
    public void testRelative() throws IOException {
        oauth.clientId("test-relative-url");

        checkRedirectUri("http://with-dash.example.com/foo", false);
        checkRedirectUri("http://localhost:8081/foo", true);
    }

    @Test
    public void testLocalhost() throws IOException {
        oauth.clientId("test-installed");

        checkRedirectUri("urn:ietf:wg:oauth:2.0:oob", true, true);
        checkRedirectUri("http://localhost", true);

        checkRedirectUri("http://localhost:8081", true, true);

        checkRedirectUri("http://localhosts", false);
        checkRedirectUri("http://localhost/myapp", false);
        checkRedirectUri("http://localhost:8081/myapp", false, true);

        oauth.clientId("test-installed2");

        checkRedirectUri("http://localhost/myapp", true);
        checkRedirectUri("http://localhost:8081/myapp", true, true);

        checkRedirectUri("http://localhosts/myapp", false);
        checkRedirectUri("http://localhost", false);
        checkRedirectUri("http://localhost/myapp2", false);
    }

    private void checkRedirectUri(String redirectUri, boolean expectValid) throws IOException {
        checkRedirectUri(redirectUri, expectValid, false);
    }

    private void checkRedirectUri(String redirectUri, boolean expectValid, boolean checkCodeToToken) throws IOException {
        oauth.redirectUri(redirectUri);
        oauth.openLoginForm();

        if (expectValid) {
            Assert.assertTrue(loginPage.isCurrent());
        } else {
            Assert.assertTrue(errorPage.isCurrent());
            Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        }

        if (expectValid) {
            Assert.assertTrue(loginPage.isCurrent());

            if (checkCodeToToken) {
                loginPage.login("test-user@localhost", "password");

                String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
                Assert.assertNotNull(code);

                OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

                Assert.assertEquals("Expected success, but got error: " + tokenResponse.getError(), 200, tokenResponse.getStatusCode());

                oauth.doLogout(tokenResponse.getRefreshToken(), "password");
            }
        }
    }

}
