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
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.Constants;
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

    @Test
    public void authorizationRequest() throws IOException {
        oauth.state("mystate");

        OAuthClient.AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        assertEquals("mystate", response.getState());
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

        OAuthClient.AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());

        testingClient.testing().verifyCode("test", response.getCode());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
        assertCode(codeId, response.getCode());
    }

    @Test
    public void authorizationRequestNoState() throws IOException {
        oauth.state(null);

        OAuthClient.AuthorizationCodeResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        Assert.assertNull(response.getState());
        Assert.assertNull(response.getError());

        testingClient.testing().verifyCode("test", response.getCode());

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
        String code = testingClient.testing().verifyCode("test", actualCode);
        assertEquals(expectedCodeId, code);
    }

}
