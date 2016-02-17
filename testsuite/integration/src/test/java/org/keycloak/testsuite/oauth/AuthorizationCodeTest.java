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
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
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
                appRealm.getClientByClientId("test-app").addRedirectUri(Constants.INSTALLED_APP_URN);
            }
        });
        oauth.redirectUri(Constants.INSTALLED_APP_URN);

        oauth.doLogin("test-user@localhost", "password");

        String title = driver.getTitle();
        Assert.assertEquals("Success code", title);

        String code = driver.findElement(By.id(OAuth2Constants.CODE)).getAttribute("value");
        keycloakRule.verifyCode(code);

        String codeId = events.expectLogin().detail(Details.REDIRECT_URI, "http://localhost:8081/auth/realms/test/protocol/openid-connect/oauth/oob").assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, code);

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("test-app").removeRedirectUri(Constants.INSTALLED_APP_URN);
            }
        });
    }

    @Test
    public void authorizationValidRedirectUri() throws IOException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("test-app").addRedirectUri(oauth.getRedirectUri());
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
    public void authorizationRequestImplicitFlowDisabled() throws IOException {
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        b.replaceQueryParam(OAuth2Constants.RESPONSE_TYPE, "token id_token");
        driver.navigate().to(b.build().toURL());
        assertEquals("Client is not allowed to initiate browser login with given response_type. Implicit flow is disabled for the client.", errorPage.getError());
        events.expectLogin().error(Errors.NOT_ALLOWED).user((String) null).session((String) null).clearDetails().detail(Details.RESPONSE_TYPE, "token id_token").assertEvent();
    }

    @Test
    public void authorizationRequestInvalidResponseType() throws IOException {
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        b.replaceQueryParam(OAuth2Constants.RESPONSE_TYPE, "tokenn");
        driver.navigate().to(b.build().toURL());
        assertEquals("Invalid parameter: response_type", errorPage.getError());
        events.expectLogin().error(Errors.INVALID_REQUEST).client((String) null).user((String) null).session((String) null).clearDetails().detail(Details.RESPONSE_TYPE, "tokenn").assertEvent();
    }

    private void assertCode(String expectedCodeId, String actualCode) {
        ClientSessionCode code = keycloakRule.verifyCode(actualCode);
        assertEquals(expectedCodeId, code.getClientSession().getId());
    }

}
