/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.storage;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.federation.HardcodedClientStorageProviderFactory;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.federation.UserPropertyFileStorageFactory;
import org.keycloak.testsuite.forms.UsernameOnlyAuthenticator;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.By;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test that clients can override auth flows
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class ClientStorageTest extends AbstractTestRealmKeycloakTest {
    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class)
                .addPackages(true, "org.keycloak.testsuite");
    }

    protected String addComponent(ComponentRepresentation component) {
        Response resp = adminClient.realm("test").components().add(component);
        resp.close();
        String id = ApiUtil.getCreatedId(resp);
        getCleanup().addComponentId(id);
        return id;
    }

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("client-storage-hardcoded");
        provider.setProviderId(HardcodedClientStorageProviderFactory.PROVIDER_ID);
        provider.setProviderType(ClientStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle(HardcodedClientStorageProviderFactory.CLIENT_ID, "hardcoded-client");
        provider.getConfig().putSingle(HardcodedClientStorageProviderFactory.REDIRECT_URI, oauth.getRedirectUri());

        String providerId = addComponent(provider);
    }




    //@Test
    public void testRunConsole() throws Exception {
        Thread.sleep(10000000);
    }


    @Test
    public void testBrowser() throws Exception {
        String clientId = "hardcoded-client";
        testBrowser(clientId);
        //Thread.sleep(10000000);
    }

    private void testBrowser(String clientId) {
        oauth.clientId(clientId);
        String loginFormUrl = oauth.getLoginFormUrl();
        log.info("loginFormUrl: " + loginFormUrl);

        //Thread.sleep(10000000);

        driver.navigate().to(loginFormUrl);

        loginPage.assertCurrent();

        // Fill username+password. I am successfully authenticated
        oauth.fillLoginForm("test-user@localhost", "password");
        appPage.assertCurrent();

        events.expectLogin().client(clientId).detail(Details.USERNAME, "test-user@localhost").assertEvent();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        Assert.assertNotNull(tokenResponse.getAccessToken());
        Assert.assertNotNull(tokenResponse.getRefreshToken());

        events.clear();

    }

    @Test
    public void testGrantAccessTokenNoOverride() throws Exception {
        testDirectGrant("hardcoded-client");
    }

    private void testDirectGrant(String clientId) {
        Client httpClient = javax.ws.rs.client.ClientBuilder.newClient();
        String grantUri = oauth.getResourceOwnerPasswordCredentialGrantUrl();
        WebTarget grantTarget = httpClient.target(grantUri);

        {   // test no password
            String header = BasicAuthHelper.createHeader(clientId, "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }

        {   // test invalid password
            String header = BasicAuthHelper.createHeader(clientId, "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            form.param("password", "invalid");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }

        {   // test valid password
            String header = BasicAuthHelper.createHeader(clientId, "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            form.param("password", "password");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            response.close();
        }

        httpClient.close();
        events.clear();
    }
}
