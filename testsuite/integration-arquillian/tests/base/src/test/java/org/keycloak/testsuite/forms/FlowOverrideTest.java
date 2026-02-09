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

package org.keycloak.testsuite.forms;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.AuthenticationFlowEntity;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.util.BasicAuthHelper;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.junit.Assert.assertEquals;

/**
 * Test that clients can override auth flows
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class FlowOverrideTest extends AbstractFlowTest {

    public static final String TEST_APP_DIRECT_OVERRIDE = "test-app-direct-override";
    public static final String TEST_APP_FLOW = "test-app-flow";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    private TimeBasedOTP totp = new TimeBasedOTP();

    @Before
    public void setupFlows() {
        SerializableApplicationData serializedApplicationData = new SerializableApplicationData(oauth.APP_AUTH_ROOT, oauth.APP_ROOT + "/admin", oauth.APP_AUTH_ROOT + "/*");

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            ClientModel client = session.clients().getClientByClientId(realm, "test-app-flow");
            if (client != null) {
                return;
            }

            client = session.clients().getClientByClientId(realm, "test-app");
            client.setDirectAccessGrantsEnabled(true);

            // Parent flow
            AuthenticationFlowModel browser = new AuthenticationFlowModel();
            browser.setAlias("parent-flow");
            browser.setDescription("browser based authentication");
            browser.setProviderId("basic-flow");
            browser.setTopLevel(true);
            browser.setBuiltIn(true);
            browser = realm.addAuthenticationFlow(browser);

            // Subflow2
            AuthenticationFlowModel subflow2 = new AuthenticationFlowModel();
            subflow2.setTopLevel(false);
            subflow2.setBuiltIn(true);
            subflow2.setAlias("subflow-2");
            subflow2.setDescription("username+password AND pushButton");
            subflow2.setProviderId("basic-flow");
            subflow2 = realm.addAuthenticationFlow(subflow2);

            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browser.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(subflow2.getId());
            execution.setPriority(20);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            // Subflow2 - push the button
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow2.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(PushButtonAuthenticatorFactory.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);

            realm.addAuthenticatorExecution(execution);

            // Subflow2 - username-password
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow2.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(UsernamePasswordFormFactory.PROVIDER_ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);

            realm.addAuthenticatorExecution(execution);

            client = realm.addClient(TEST_APP_FLOW);
            client.setSecret("password");
            client.setBaseUrl(serializedApplicationData.applicationBaseUrl);
            client.setManagementUrl(serializedApplicationData.applicationManagementUrl);
            client.setEnabled(true);
            client.addRedirectUri(serializedApplicationData.applicationRedirectUrl);
            client.setAuthenticationFlowBindingOverride(AuthenticationFlowBindings.BROWSER_BINDING, browser.getId());
            client.setPublicClient(false);

            // Parent flow
            AuthenticationFlowModel directGrant = new AuthenticationFlowModel();
            directGrant.setAlias("direct-override-flow");
            directGrant.setDescription("direct grant based authentication");
            directGrant.setProviderId("basic-flow");
            directGrant.setTopLevel(true);
            directGrant.setBuiltIn(true);
            directGrant = realm.addAuthenticationFlow(directGrant);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(directGrant.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(UsernameOnlyAuthenticator.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);

            realm.addAuthenticatorExecution(execution);

            client = realm.addClient(TEST_APP_DIRECT_OVERRIDE);
            client.setSecret("password");
            client.setBaseUrl(serializedApplicationData.applicationBaseUrl);
            client.setManagementUrl(serializedApplicationData.applicationManagementUrl);
            client.setEnabled(true);
            client.addRedirectUri(serializedApplicationData.applicationRedirectUrl);
            client.setPublicClient(false);
            client.setDirectAccessGrantsEnabled(true);
            client.setAuthenticationFlowBindingOverride(AuthenticationFlowBindings.BROWSER_BINDING, browser.getId());
            client.setAuthenticationFlowBindingOverride(AuthenticationFlowBindings.DIRECT_GRANT_BINDING, directGrant.getId());
        });
    }

    //@Test
    public void testRunConsole() throws Exception {
        Thread.sleep(10000000);
    }


    @Test
    public void testWithClientBrowserOverride() throws Exception {
        oauth.clientId(TEST_APP_FLOW);
        oauth.openLoginForm();

        Assert.assertEquals("PushTheButton", driver.getTitle());

        // Push the button. I am redirected to username+password form
        UIUtils.clickLink(driver.findElement(By.name("submit1")));


        loginPage.assertCurrent();

        // Fill username+password. I am successfully authenticated
        oauth.fillLoginForm("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        events.expectLogin().client("test-app-flow").detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void testRemovedFlowOverrideByClientThenFallbackToToNoOverrideBrowserFlow() {
        testWithRemovedFlowOverrideByClient(AuthenticationFlowBindings.BROWSER_BINDING, this::testNoOverrideBrowser);
    }

    // TODO remove this once DYNAMIC_SCOPES feature is enabled by default
    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void testWithClientBrowserOverrideWithDynamicScope() throws Exception {
        // Just use existing test with DYNAMIC_SCOPES feature enabled as it was failing with DYNAMIC_SCOPES
        testWithClientBrowserOverride();
    }

    @Test
    public void testNoOverrideBrowser() throws Exception {
        String clientId = "test-app";
        testNoOverrideBrowser(clientId);
    }

    private void testNoOverrideBrowser(String clientId) {
        oauth.clientId(clientId);
        oauth.openLoginForm();

        loginPage.assertCurrent();

        // Fill username+password. I am successfully authenticated
        oauth.fillLoginForm("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        events.expectLogin().client(clientId).detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void testGrantAccessTokenNoOverride() throws Exception {
        testDirectGrantNoOverride("test-app");
    }

    @Test
    public void testRemovedFlowOverrideByClientThenFallbackToNoOverrideDirectGrantFlow() {
        testWithRemovedFlowOverrideByClient(AuthenticationFlowBindings.DIRECT_GRANT_BINDING, this::testNoOverrideBrowser);
    }

    private void testDirectGrantNoOverride(String clientId) {
        Client httpClient = AdminClientUtil.createResteasyClient();
        String grantUri = oauth.getEndpoints().getToken();
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
            form.param("password", getPassword("test-user@localhost"));

            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            response.close();
        }

        httpClient.close();
        events.clear();
    }

    @Test
    public void testGrantAccessTokenWithClientOverride() throws Exception {
        String clientId = TEST_APP_DIRECT_OVERRIDE;
        Client httpClient = AdminClientUtil.createResteasyClient();
        String grantUri = oauth.getEndpoints().getToken();
        WebTarget grantTarget = httpClient.target(grantUri);

        {   // test no password
            String header = BasicAuthHelper.createHeader(clientId, "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            response.close();
        }

        httpClient.close();
        events.clear();
    }

    @Test
    public void testRestInterface() throws Exception {
        ClientsResource clients = adminClient.realm("test").clients();
        List<ClientRepresentation> query = clients.findByClientId(TEST_APP_DIRECT_OVERRIDE);
        ClientRepresentation clientRep = query.get(0);
        String directGrantFlowId = clientRep.getAuthenticationFlowBindingOverrides().get(AuthenticationFlowBindings.DIRECT_GRANT_BINDING);
        Assert.assertNotNull(directGrantFlowId);
        clientRep.getAuthenticationFlowBindingOverrides().put(AuthenticationFlowBindings.DIRECT_GRANT_BINDING, "");
        clients.get(clientRep.getId()).update(clientRep);
        testDirectGrantNoOverride(TEST_APP_DIRECT_OVERRIDE);
        clientRep.getAuthenticationFlowBindingOverrides().put(AuthenticationFlowBindings.DIRECT_GRANT_BINDING, directGrantFlowId);
        clients.get(clientRep.getId()).update(clientRep);
        testGrantAccessTokenWithClientOverride();

        query = clients.findByClientId(TEST_APP_FLOW);
        clientRep = query.get(0);
        String browserFlowId = clientRep.getAuthenticationFlowBindingOverrides().get(AuthenticationFlowBindings.BROWSER_BINDING);
        Assert.assertNotNull(browserFlowId);
        clientRep.getAuthenticationFlowBindingOverrides().put(AuthenticationFlowBindings.BROWSER_BINDING, "");
        clients.get(clientRep.getId()).update(clientRep);
        testNoOverrideBrowser(TEST_APP_FLOW);
        clientRep.getAuthenticationFlowBindingOverrides().put(AuthenticationFlowBindings.BROWSER_BINDING, browserFlowId);
        clients.get(clientRep.getId()).update(clientRep);
        testWithClientBrowserOverride();
    }

    @Test
    @UncaughtServerErrorExpected
    public void testRestInterfaceWithBadId() throws Exception {
        ClientsResource clients = adminClient.realm("test").clients();
        List<ClientRepresentation> query = clients.findByClientId(TEST_APP_FLOW);
        ClientRepresentation clientRep = query.get(0);
        String browserFlowId = clientRep.getAuthenticationFlowBindingOverrides().get(AuthenticationFlowBindings.BROWSER_BINDING);

        clientRep.getAuthenticationFlowBindingOverrides().put(AuthenticationFlowBindings.BROWSER_BINDING, "bad-id");
        try {
            clients.get(clientRep.getId()).update(clientRep);
            Assert.fail();
        } catch (Exception e) {

        }
        query = clients.findByClientId(TEST_APP_FLOW);
        clientRep = query.get(0);
        Assert.assertEquals(browserFlowId, clientRep.getAuthenticationFlowBindingOverrides().get(AuthenticationFlowBindings.BROWSER_BINDING));

    }

    private void testWithRemovedFlowOverrideByClient(String binding, Consumer<String> testNoOverride) {
        ClientResource clientResource = ApiUtil.findClientByClientId(testRealm(), "test-app");
        Assert.assertNotNull(clientResource);
        ClientRepresentation clientRep = clientResource.toRepresentation();

        String newFlowAlias = "Copy of Browser Flow";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        AuthenticationFlowRepresentation newFlow = findFlowByAlias(newFlowAlias);

        try {
            // 1. set a flow override for client
            clientRep.setAuthenticationFlowBindingOverrides(Map.of(binding, newFlow.getId()));
            clientResource.update(clientRep);

            // 2. remove the flow through database, since we could not delete the flow which is used by client
            testingClient.server("test").run(session -> {
                EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                AuthenticationFlowEntity entity = em.find(AuthenticationFlowEntity.class, newFlow.getId(), LockModeType.PESSIMISTIC_WRITE);
                em.remove(entity);
                em.flush();
            });

            // 3. login with client
            testNoOverride.accept(clientRep.getClientId());
        } finally {
            clientRep.setAuthenticationFlowBindingOverrides(Map.of(binding, ""));
            clientResource.update(clientRep);
        }
    }
}
