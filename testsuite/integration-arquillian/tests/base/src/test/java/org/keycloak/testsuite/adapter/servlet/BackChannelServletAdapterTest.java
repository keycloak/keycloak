/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter.servlet;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.BasicAuth;
import org.keycloak.testsuite.adapter.page.SessionPortal;
import org.keycloak.testsuite.adapter.page.TokenMinTTLPage;
import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.AuthServerConfigurationUtil;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.*;

@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT7)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT8)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT9)
@AppServerContainer(ContainerConstants.APP_SERVER_JETTY92)
@AppServerContainer(ContainerConstants.APP_SERVER_JETTY93)
@AppServerContainer(ContainerConstants.APP_SERVER_JETTY94)
public class BackChannelServletAdapterTest extends AbstractServletsAdapterTest {

    // NOTE: This test use an invalid frontchannel url, to verify that the adapter use it's (valid) backchannel url
    private static final String AUTH_SERVER_HOST_PROPERTY = "auth.server.host";
    private static final String INVALID_FRONTCHANNEL_HOST = "keycloak.invalid";

    private static String hostBackup;
    static {
        hostBackup = System.getProperty(AUTH_SERVER_HOST_PROPERTY, "localhost");
        System.setProperty(AUTH_SERVER_HOST_PROPERTY, INVALID_FRONTCHANNEL_HOST);
    }

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    @Page
    private SessionPortal sessionPortalPage;

    @Page
    private TokenMinTTLPage tokenMinTTLPage;

    @Page
    private BasicAuth basicAuthPage;


    @Deployment(name = SessionPortal.DEPLOYMENT_NAME)
    protected static WebArchive sessionPortalServlet() {
        return servletDeployment(SessionPortal.DEPLOYMENT_NAME, "keycloak-backchannel.json", SessionServlet.class);
    }

    @Deployment(name = TokenMinTTLPage.DEPLOYMENT_NAME)
    protected static WebArchive tokenMinTTLPage() {
        return servletDeployment(TokenMinTTLPage.DEPLOYMENT_NAME, "keycloak-backchannel.json", AdapterActionsFilter.class, AbstractShowTokensServlet.class, TokenMinTTLServlet.class, ErrorServlet.class);
    }

    @Deployment(name = BasicAuth.DEPLOYMENT_NAME, managed = false)
    protected static WebArchive basicAuthServlet() {
        return servletDeployment(BasicAuth.DEPLOYMENT_NAME, "keycloak-backchannel.json", BasicAuthServlet.class);
    }

    @AfterClass
    public static void resetAuthServerHost() {
        System.setProperty(AUTH_SERVER_HOST_PROPERTY, hostBackup);
    }

    @Before
    public void configureAuthServer() throws Exception {
        configureFixedHostname();
    }

    @After
    public void revertAuthServerConfiguration() throws Exception {
        clearFixedHostname();
    }

    @Test
    public void standardFlowLoginAndBackChannelLogout() {
        Client client = ClientBuilder.newClient();
        try {
            // login
            FakeStandardFlow standardFlow = new FakeStandardFlow(client, sessionPortalPage.toString());
            ApplicationLoginResponse loginResponse = standardFlow.login("bburke@redhat.com", "password");

            assertEquals(OK, loginResponse.getStatusInfo());
            assertEquals(sessionPortalPage.toString(), loginResponse.getUrl());
            assertTrue(loginResponse.getPageSource().contains("Counter=1"));

            // logout
            String logoutUrl = sessionPortalPage.logoutURL();
            try (Response logoutRedirect = client.target(logoutUrl).request().get()) {
                assertEquals(OK, logoutRedirect.getStatusInfo());
                assertTrue(logoutRedirect.readEntity(String.class).isEmpty());
            }

            try (Response authServerRedirect = client.target(sessionPortalPage.toString()).request().get()) {
                String authServerRedirectUrl = authServerRedirect.getHeaderString("Location");
                String invalidAuthServerUrl = getInvalidAuthServerUrl();

                assertEquals(FOUND, authServerRedirect.getStatusInfo());
                assertTrue(authServerRedirectUrl.startsWith(invalidAuthServerUrl));
            }
        } finally {
            client.close();
        }
    }

    @Test
    public void tokenRefresh() {
        RealmRepresentation realm = getDemoRealm().toRepresentation();
        int originalTokenLifespan = realm.getAccessTokenLifespan();
        realm.setAccessTokenLifespan(10);
        getDemoRealm().update(realm);

        Client client = ClientBuilder.newClient();
        try {
            FakeStandardFlow standardFlow = new FakeStandardFlow(client, tokenMinTTLPage.toString());
            ApplicationLoginResponse loginResponse = standardFlow.login("bburke@redhat.com", "password");

            assertEquals(OK, loginResponse.getStatusInfo());
            String initialToken = extractAccessToken(loginResponse.getPageSource(), TokenMinTTLPage.ACCESS_TOKEN_ID);
            assertNotNull(initialToken);

            setAdapterAndServerTimeOffset(12, tokenMinTTLPage.getUnsecuredUrl());

            try (Response applicationRefresh = client.target(tokenMinTTLPage.toString()).request().get()) {
                String pageSource = applicationRefresh.readEntity(String.class);
                String refreshedToken = extractAccessToken(pageSource, TokenMinTTLPage.ACCESS_TOKEN_ID);

                assertNotNull(refreshedToken);
                assertNotEquals(initialToken, refreshedToken);
            }
        } finally {
            realm.setAccessTokenLifespan(originalTokenLifespan);
            getDemoRealm().update(realm);
            setAdapterAndServerTimeOffset(0, tokenMinTTLPage.getUnsecuredUrl());
            client.close();
        }
    }

    @Test
    public void basicAuthAndClientRegistration() {
        String clientId = "basic-auth-service";

        try {
            assertNull(getClient(clientId).getRegisteredNodes());
            deployer.deploy(BasicAuth.DEPLOYMENT_NAME);

            Client client = ClientBuilder.newClient();
            try {
                String value = "hello";
                URI uri = basicAuthPage.setTemplateValues(value).buildUri();
                String authHeader = BasicAuthHelper.createHeader("mposolda", "password");

                Invocation.Builder request = client.target(uri).request().header("Authorization", authHeader);
                try (Response response = request.get()) {
                    // basic auth
                    assertEquals(OK, response.getStatusInfo());
                    assertEquals(value, response.readEntity(String.class));

                    // client registration
                    assertEquals(1, getClient(clientId).getRegisteredNodes().size());
                }
            } finally {
                client.close();
            }
        } finally {
            deployer.undeploy(BasicAuth.DEPLOYMENT_NAME);

            // it looks like the arquillian undeployment does not trigger client-unregistration on all appservers.
            // https://www.keycloak.org/docs/latest/securing_apps/index.html#_registration_app_nodes
            if(appServerTriggersNodeUnregistration()) {
                // client unregistration
                assertNull(getClient(clientId).getRegisteredNodes());
            }
        }
    }

    private boolean appServerTriggersNodeUnregistration() {
        return !testContext.getAppServerInfo().isJBossBased() && !AppServerTestEnricher.isJettyAppServer();
    }

    private String getInvalidAuthServerUrl() {
        String validRealmLoginPage = testRealmPage.toString();
        String validAuthServerHost = testRealmPage.getInjectedUrl().getHost();
        return validRealmLoginPage.replace(validAuthServerHost, INVALID_FRONTCHANNEL_HOST);
    }

    private String extractAccessToken(String html, String id) {
        return Jsoup.parse(html).getElementById(id).text();
    }


    private RealmResource getDemoRealm() {
        return adminClient.realm("demo");
    }

    private ClientRepresentation getClient(String clientId) {
        return getDemoRealm().clients().findByClientId(clientId).get(0);
    }

    private void configureFixedHostname() throws Exception {
        AuthServerConfigurationUtil authServerConfiguration = new AuthServerConfigurationUtil(suiteContext, controller);
        authServerConfiguration.configureFixedHostname(INVALID_FRONTCHANNEL_HOST, -1, -1, false);

        reconnectAdminClient();
    }

    private void clearFixedHostname() throws Exception {
        AuthServerConfigurationUtil authServerConfiguration = new AuthServerConfigurationUtil(suiteContext, controller);
        authServerConfiguration.clearFixedHostname();

        reconnectAdminClient();
    }

    /**
     * Do a standard flow login by replacing a invalid frontchannel host with a valid one.
     */
    private class FakeStandardFlow {
        private final Client client;
        private final String applicationUrl;

        FakeStandardFlow(Client client, String applicationUrl) {
            this.client = client;
            this.applicationUrl = applicationUrl;
        }

        ApplicationLoginResponse login(String username, String password) {
            String keycloakLoginFormRedirect = accessApplicationLoggedOut(applicationUrl);
            String keycloakLoginFormActionUrl = loadLoginFormAndGetActionUrl(keycloakLoginFormRedirect);
            String applicationCodeToTokenRedirect = submitLoginForm(keycloakLoginFormActionUrl, username, password);
            String applicationRedirect = applicationCodeToTokenExchange(applicationCodeToTokenRedirect);

            return accessApplicationLoggedIn(applicationRedirect);
        }

        private String accessApplicationLoggedOut(String applicationUrl) {
            try (Response applicationRedirect = client.target(applicationUrl).request().get()) {
                assertEquals("access application before login failed", FOUND, applicationRedirect.getStatusInfo());

                String applicationRedirectUrl = getLocationHeader(applicationRedirect);
                return replaceInvalidFrontChannelHost(applicationRedirectUrl);
            }
        }

        private String loadLoginFormAndGetActionUrl(String keycloakLoginFormUrl) {
            try (Response loginForm = client.target(keycloakLoginFormUrl).request().get()) {
                assertEquals("loading login form failed", OK, loginForm.getStatusInfo());

                String loginPageSource = loginForm.readEntity(String.class);
                String loginFormActionUrl = extractLoginFormActionUrl(loginPageSource);
                return replaceInvalidFrontChannelHost(loginFormActionUrl);
            }
        }

        private String submitLoginForm(String loginFormActionUrl, String username, String password) {
            Form form = new Form();
            form.param("username", username);
            form.param("password", password);

            Invocation.Builder loginFormRequest = client.target(loginFormActionUrl).request(MediaType.APPLICATION_FORM_URLENCODED);
            try (Response loginRedirect = loginFormRequest.post(Entity.form(form))) {
                assertEquals("submit login form failed", FOUND, loginRedirect.getStatusInfo());

                return getLocationHeader(loginRedirect);
            }
        }

        private String applicationCodeToTokenExchange(String applicationRedirect) {
            try (Response codeToTokenResponse = client.target(applicationRedirect).request().get()) {
                assertEquals("code-to-token exchange failed", FOUND, codeToTokenResponse.getStatusInfo());

                return getLocationHeader(codeToTokenResponse);
            }
        }

        private ApplicationLoginResponse accessApplicationLoggedIn(String applicationRedirect) {
            try (Response applicationResponse = client.target(applicationRedirect).request().get()) {
                assertEquals("access application after login failed", OK, applicationResponse.getStatusInfo());

                String applicationPageSource = applicationResponse.readEntity(String.class);
                return new ApplicationLoginResponse(applicationRedirect, applicationResponse.getStatusInfo(), applicationPageSource);
            }
        }

        private String extractLoginFormActionUrl(String loginPageSource) {
            Element loginForm = Jsoup.parse(loginPageSource).getElementById("kc-form-login");
            return loginForm.attr("action");
        }

        private String replaceInvalidFrontChannelHost(String frontChannelUrl) {
            String validFrontChannelHost = authServerContextRootPage.getInjectedUrl().getHost();
            return frontChannelUrl.replace(INVALID_FRONTCHANNEL_HOST, validFrontChannelHost);
        }

        private String getLocationHeader(Response redirect) {
            return redirect.getHeaderString("Location");
        }
    }

    private static class ApplicationLoginResponse {
        private final String url;
        private final Response.StatusType status;
        private final String pageSource;

        ApplicationLoginResponse(String url, Response.StatusType status, String pageSource) {
            this.url = url;
            this.status = status;
            this.pageSource = pageSource;
        }

        String getUrl() {
            return url;
        }

        Response.StatusType getStatusInfo() {
            return status;
        }

        String getPageSource() {
            return pageSource;
        }
    }
}