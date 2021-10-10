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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.console.ConsoleUsernamePasswordAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.OAuthClient;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.AdminClientUtil;

/**
 * Test that clients can override auth flows
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class ChallengeFlowTest extends AbstractTestRealmKeycloakTest {

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

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void setupFlows() {
        SerializableApplicationData serializedApplicationData = new SerializableApplicationData(oauth.APP_AUTH_ROOT, oauth.APP_ROOT + "/admin", oauth.APP_AUTH_ROOT + "/*");

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            ClientModel client = session.clients().getClientByClientId(realm, "test-app-flow");
            if (client != null) {
                return;
            }

            // Parent flow
            AuthenticationFlowModel browser = new AuthenticationFlowModel();
            browser.setAlias("cli-challenge");
            browser.setDescription("challenge based authentication");
            browser.setProviderId("basic-flow");
            browser.setTopLevel(true);
            browser.setBuiltIn(true);
            browser = realm.addAuthenticationFlow(browser);

            // Subflow2 - push the button
            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browser.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(ConsoleUsernamePasswordAuthenticatorFactory.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            client = realm.addClient(TEST_APP_FLOW);
            client.setSecret("password");
            client.setBaseUrl(serializedApplicationData.applicationBaseUrl);
            client.setManagementUrl(serializedApplicationData.applicationManagementUrl);
            client.setEnabled(true);
            client.addRedirectUri(serializedApplicationData.applicationRedirectUrl);
            client.addRedirectUri("urn:ietf:wg:oauth:2.0:oob");
            client.setAuthenticationFlowBindingOverride(AuthenticationFlowBindings.BROWSER_BINDING, browser.getId());
            client.setPublicClient(false);
        });
    }

    //@Test
    public void testRunConsole() throws Exception {
        Thread.sleep(10000000);
    }


    @Test
    public void testChallengeFlow() throws Exception {
        oauth.clientId(TEST_APP_FLOW);
        String loginFormUrl = oauth.getLoginFormUrl();
        Client client = AdminClientUtil.createResteasyClient();
        WebTarget loginTarget = client.target(loginFormUrl);
        Response response = loginTarget.request().get();
        Assert.assertEquals(401, response.getStatus());
        String authenticateHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
        Assert.assertNotNull(authenticateHeader);
        //System.out.println(authenticateHeader);
        String splash = response.readEntity(String.class);
        //System.out.println(splash);
        response.close();

        // respin Client to make absolutely sure no cookie caching.  need to test that it works with null auth_session_id cookie.
        client.close();
        client = AdminClientUtil.createResteasyClient();


        authenticateHeader = authenticateHeader.trim();
        Pattern callbackPattern = Pattern.compile("callback\\s*=\\s*\"([^\"]+)\"");
        Pattern paramPattern = Pattern.compile("param=\"([^\"]+)\"\\s+label=\"([^\"]+)\"");
        Matcher m = callbackPattern.matcher(authenticateHeader);
        String callback = null;
        if (m.find()) {
            callback = m.group(1);
            //System.out.println("------");
            //System.out.println("callback:");
            //System.out.println("    " + callback);
        }
        m = paramPattern.matcher(authenticateHeader);
        List<String> params = new LinkedList<>();
        List<String> labels = new LinkedList<>();
        while (m.find()) {
            String param = m.group(1);
            String label = m.group(2);
            params.add(param);
            labels.add(label);
            //System.out.println("------");
            //System.out.println("param:" + param);
            //System.out.println("label:" + label);
        }
        Assert.assertEquals("username", params.get(0));
        Assert.assertEquals("Username:", labels.get(0).trim());
        Assert.assertEquals("password", params.get(1));
        Assert.assertEquals("Password:", labels.get(1).trim());

        Form form = new Form();
        form.param("username", "test-user@localhost");
        form.param("password", "password");
        response = client.target(callback)
                .request()
                .post(Entity.form(form));
        Assert.assertEquals(302, response.getStatus());
        String redirect = response.getHeaderString(HttpHeaders.LOCATION);
        System.out.println("------");
        System.out.println(redirect);
        Pattern codePattern = Pattern.compile("code=([^&]+)");
        m = codePattern.matcher(redirect);
        Assert.assertTrue(m.find());
        String code = m.group(1);
        OAuthClient.AccessTokenResponse oauthResponse = oauth.doAccessTokenRequest(code, "password");
        Assert.assertNotNull(oauthResponse.getAccessToken());
        client.close();


    }


}
