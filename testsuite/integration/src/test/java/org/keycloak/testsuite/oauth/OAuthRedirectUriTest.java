/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.oauth;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthRedirectUriTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            ApplicationModel installedApp = appRealm.addApplication("test-installed");
            installedApp.setEnabled(true);
            installedApp.addRedirectUri(Constants.INSTALLED_APP_URN);
            installedApp.addRedirectUri(Constants.INSTALLED_APP_URL);
            installedApp.setSecret("password");

            ApplicationModel installedApp2 = appRealm.addApplication("test-installed2");
            installedApp2.setEnabled(true);
            installedApp2.addRedirectUri(Constants.INSTALLED_APP_URL + "/myapp");
            installedApp2.setSecret("password");

            ApplicationModel installedApp3 = appRealm.addApplication("test-wildcard");
            installedApp3.setEnabled(true);
            installedApp3.addRedirectUri("http://example.com/foo/*");
            installedApp3.addRedirectUri("http://localhost:8081/foo/*");
            installedApp3.setSecret("password");
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
                appRealm.getApplicationNameMap().get("test-app").addRedirectUri("http://localhost:8081/app2");
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
                    appRealm.getApplicationNameMap().get("test-app").removeRedirectUri("http://localhost:8081/app2");
                }
            });
        }
    }

    @Test
    public void testNoParamNoValidUris() throws IOException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getApplicationNameMap().get("test-app").removeRedirectUri("http://localhost:8081/app/*");
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
                    appRealm.getApplicationNameMap().get("test-app").addRedirectUri("http://localhost:8081/app/*");
                }
            });
        }
    }

    @Test
    public void testNoValidUris() throws IOException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getApplicationNameMap().get("test-app").removeRedirectUri("http://localhost:8081/app/*");
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
                    appRealm.getApplicationNameMap().get("test-app").addRedirectUri("http://localhost:8081/app/*");
                }
            });
        }
    }

    @Test
    public void testValid() throws IOException {
        oauth.redirectUri("http://localhost:8081/app");
        OAuthClient.AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertNotNull(response.getCode());
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/app?code="));
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
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/app?key=value&code="));
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
