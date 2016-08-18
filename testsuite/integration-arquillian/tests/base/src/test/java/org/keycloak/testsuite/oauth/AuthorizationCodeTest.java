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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.By;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationCodeTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected ErrorPage errorPage;


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        testRealms.add(realmRepresentation);

    }

    @Before
    public void clientConfiguration() {
        oauth.responseType(OAuth2Constants.CODE);
        oauth.responseMode(null);
    }

    @Test
    public void authorizationRequest() throws IOException {
        oauth.state("OpenIdConnect.AuthenticationProperties=2302984sdlk");

        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", response.getState());
        Assert.assertNull(response.getError());

        testingClient.testing().verifyCode("test", response.getCode());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, response.getCode());
    }

    @Test
    public void authorizationRequestInstalledApp() throws IOException {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris(Constants.INSTALLED_APP_URN);
        oauth.redirectUri(Constants.INSTALLED_APP_URN);

        oauth.doLogin("test-user@localhost", "password");

        String title = driver.getTitle();
        Assert.assertEquals("Success code", title);

        String code = driver.findElement(By.id(OAuth2Constants.CODE)).getAttribute("value");
        testingClient.testing().verifyCode("test", code);

        String codeId = events.expectLogin().detail(Details.REDIRECT_URI, "http://localhost:8180/auth/realms/test/protocol/openid-connect/oauth/oob").assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, code);

        ClientManager.realm(adminClient.realm("test")).clientId("test-app").removeRedirectUris(Constants.INSTALLED_APP_URN);
    }

    @Test
    public void authorizationValidRedirectUri() throws IOException {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris(oauth.getRedirectUri());

        oauth.state("mystate");

        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());

        testingClient.testing().verifyCode("test", response.getCode());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, response.getCode());
    }

    @Test
    public void authorizationRequestNoState() throws IOException {
        oauth.state(null);

        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        Assert.assertNull(response.getState());
        Assert.assertNull(response.getError());

        testingClient.testing().verifyCode("test", response.getCode());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, response.getCode());
    }

    @Test
    public void authorizationRequestInvalidResponseType() throws IOException {
        oauth.responseType("tokenn");
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        driver.navigate().to(b.build().toURL());

        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);
        Assert.assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE);

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().detail(Details.RESPONSE_TYPE, "tokenn").assertEvent();
    }

    // KEYCLOAK-3281
    @Test
    public void authorizationRequestFormPostResponseMode() throws IOException {
        oauth.responseMode(OIDCResponseMode.FORM_POST.toString().toLowerCase());
        oauth.state("OpenIdConnect.AuthenticationProperties=2302984sdlk");
        oauth.doLoginGrant("test-user@localhost", "password");

        String sources = driver.getPageSource();
        System.out.println(sources);

        String code = driver.findElement(By.id("code")).getText();
        String state = driver.findElement(By.id("state")).getText();

        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", state);

        testingClient.testing().verifyCode("test", code);
        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, code);
    }

    private void assertCode(String expectedCodeId, String actualCode) {
        String code = testingClient.testing().verifyCode("test", actualCode);
        assertEquals(expectedCodeId, code);
    }

}
