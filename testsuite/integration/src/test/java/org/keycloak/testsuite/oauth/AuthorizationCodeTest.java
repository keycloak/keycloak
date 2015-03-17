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
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AuthorizationCodeResponse;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationCodeTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

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

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Test
    public void authorizationRequest() throws IOException {
        oauth.state("mystate");

        AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        assertEquals("mystate", response.getState());
        Assert.assertNull(response.getError());

        keycloakRule.verifyCode(response.getCode());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, response.getCode());
    }

    @Test
    public void authorizationRequestInstalledApp() throws IOException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getApplicationNameMap().get("test-app").addRedirectUri(Constants.INSTALLED_APP_URN);
            }
        });
        oauth.redirectUri(Constants.INSTALLED_APP_URN);

        oauth.doLogin("test-user@localhost", "password");

        String title = driver.getTitle();
        Assert.assertTrue(title.startsWith("Success code="));

        String code = driver.findElement(By.id(OAuth2Constants.CODE)).getAttribute("value");
        keycloakRule.verifyCode(code);

        String codeId = events.expectLogin().detail(Details.REDIRECT_URI, "http://localhost:8081/auth/realms/test/protocol/openid-connect/oauth/oob").assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, code);

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getApplicationNameMap().get("test-app").removeRedirectUri(Constants.INSTALLED_APP_URN);
            }
        });
    }

    @Test
    public void authorizationRequestInstalledAppCancel() throws IOException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getApplicationNameMap().get("test-app").addRedirectUri(Constants.INSTALLED_APP_URN);
            }
        });
        oauth.redirectUri(Constants.INSTALLED_APP_URN);

        oauth.openLoginForm();
        driver.findElement(By.name("cancel")).click();

        String title = driver.getTitle();
        Assert.assertTrue(title.equals("Error error=access_denied"));

        String error = driver.findElement(By.id(OAuth2Constants.ERROR)).getText();
        assertEquals("access_denied", error);

        events.expectLogin().error("rejected_by_user").user((String) null).session((String) null)
                .removeDetail(Details.USERNAME)
                .detail(Details.REDIRECT_URI, "http://localhost:8081/auth/realms/test/protocol/openid-connect/oauth/oob")
                .assertEvent().getDetails().get(Details.CODE_ID);

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getApplicationNameMap().get("test-app").removeRedirectUri(Constants.INSTALLED_APP_URN);
            }
        });
    }

    @Test
    public void authorizationValidRedirectUri() throws IOException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getApplicationByName("test-app").addRedirectUri(oauth.getRedirectUri());
            }
        });

        oauth.state("mystate");

        AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());

        keycloakRule.verifyCode(response.getCode());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, response.getCode());
    }

    @Test
    public void authorizationRequestNoState() throws IOException {
        oauth.state(null);

        AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        Assert.assertNull(response.getState());
        Assert.assertNull(response.getError());

        keycloakRule.verifyCode(response.getCode());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, response.getCode());
    }

    @Test
    public void authorizationRequestInvalidResponseType() throws IOException {
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        b.replaceQueryParam(OAuth2Constants.RESPONSE_TYPE, "token");
        driver.navigate().to(b.build().toURL());
        assertEquals("Invalid parameter: response_type", errorPage.getError());
        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().detail(Details.RESPONSE_TYPE, "token").assertEvent();
    }

    private void assertCode(String expectedCodeId, String actualCode) {
        ClientSessionCode code = keycloakRule.verifyCode(actualCode);
        assertEquals(expectedCodeId, code.getClientSession().getId());
    }

}
