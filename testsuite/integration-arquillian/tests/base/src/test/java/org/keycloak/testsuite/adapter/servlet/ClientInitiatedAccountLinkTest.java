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
package org.keycloak.testsuite.adapter.servlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.broker.BrokerTestTools;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_LINKS;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
public class ClientInitiatedAccountLinkTest extends AbstractServletsAdapterTest {
    public static final String CHILD_IDP = "child";
    public static final String PARENT_IDP = "parent-idp";
    public static final String PARENT_USERNAME = "parent";

    @Page
    protected LoginUpdateProfilePage loginUpdateProfilePage;

    @Page
    protected AccountUpdateProfilePage profilePage;

    @Page
    private LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    public static class ClientApp extends AbstractPageWithInjectedUrl {

        public static final String DEPLOYMENT_NAME = "client-linking";

        @ArquillianResource
        @OperateOnDeployment(DEPLOYMENT_NAME)
        private URL url;

        @Override
        public URL getInjectedUrl() {
            return url;
        }

    }

    @Page
    private ClientApp appPage;

    @Override
    public void beforeAuthTest() {
    }
    
    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(CHILD_IDP);
        realm.setEnabled(true);
        ClientRepresentation servlet = new ClientRepresentation();
        servlet.setClientId("client-linking");
        servlet.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String uri = "/client-linking";
        servlet.setAdminUrl(uri);
        servlet.setDirectAccessGrantsEnabled(true);
        servlet.setBaseUrl(uri);
        servlet.setRedirectUris(new LinkedList<>());
        servlet.getRedirectUris().add(uri + "/*");
        servlet.setSecret("password");
        servlet.setFullScopeAllowed(true);
        realm.setClients(new LinkedList<>());
        realm.getClients().add(servlet);
        testRealms.add(realm);


        realm = new RealmRepresentation();
        realm.setRealm(PARENT_IDP);
        realm.setEnabled(true);

        testRealms.add(realm);

    }
    
    @Deployment(name = ClientApp.DEPLOYMENT_NAME)
    protected static WebArchive accountLink() {
        return servletDeployment(ClientApp.DEPLOYMENT_NAME, ClientInitiatedAccountLinkServlet.class, ServletTestUtils.class);
    }
    
    @Before
    public void addIdpUser() {
        RealmResource realm = adminClient.realms().realm(PARENT_IDP);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(PARENT_USERNAME);
        user.setEnabled(true);
        String userId = createUserAndResetPasswordWithAdminClient(realm, user, "password");

    }

    private String childUserId = null;

    @Before
    public void addChildUser() {
        RealmResource realm = adminClient.realms().realm(CHILD_IDP);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("child");
        user.setEnabled(true);
        childUserId = createUserAndResetPasswordWithAdminClient(realm, user, "password");
        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername("child2");
        user2.setEnabled(true);
        String user2Id = createUserAndResetPasswordWithAdminClient(realm, user2, "password");

        // have to add a role as undertow default auth manager doesn't like "*". todo we can remove this eventually as undertow fixes this in later versions
        realm.roles().create(new RoleRepresentation("user", null, false));
        RoleRepresentation role = realm.roles().get("user").toRepresentation();
        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(role);
        realm.users().get(childUserId).roles().realmLevel().add(roles);
        realm.users().get(user2Id).roles().realmLevel().add(roles);
        ClientRepresentation brokerService = realm.clients().findByClientId(Constants.BROKER_SERVICE_CLIENT_ID).get(0);
        role = realm.clients().get(brokerService.getId()).roles().get(Constants.READ_TOKEN_ROLE).toRepresentation();
        roles.clear();
        roles.add(role);
        realm.users().get(childUserId).roles().clientLevel(brokerService.getId()).add(roles);
        realm.users().get(user2Id).roles().clientLevel(brokerService.getId()).add(roles);

    }

    @Before
    public void createBroker() {
        createParentChild();
    }

    public void createParentChild() {
        BrokerTestTools.createKcOidcBroker(adminClient, CHILD_IDP, PARENT_IDP);
    }


    @Test
    public void testErrorConditions() throws Exception {
        String helloUrl = appPage.getUriBuilder().clone().path("hello").build().toASCIIString();

        RealmResource realm = adminClient.realms().realm(CHILD_IDP);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        ClientRepresentation client = adminClient.realms().realm(CHILD_IDP).clients().findByClientId("client-linking").get(0);

        UriBuilder redirectUri = UriBuilder.fromUri(appPage.getInjectedUrl().toString())
                .path("link")
                .queryParam("response", "true");

        UriBuilder directLinking = UriBuilder.fromUri(getAuthServerContextRoot() + "/auth")
                .path("realms/child/broker/{provider}/link")
                .queryParam("client_id", "client-linking")
                .queryParam("redirect_uri", redirectUri.build())
                .queryParam("hash", Base64Url.encode("crap".getBytes()))
                .queryParam("nonce", UUID.randomUUID().toString());

        String linkUrl = directLinking
                .build(PARENT_IDP).toString();

        // test not logged in

        navigateTo(linkUrl);
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");

        Assert.assertTrue(driver.getCurrentUrl().contains("link_error=not_logged_in"));

        logoutAll();

        // now log in


        navigateTo(helloUrl);
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(helloUrl));
        Assert.assertTrue(driver.getPageSource().contains("Unknown request:"));

        // now test CSRF with bad hash.

        navigateTo(linkUrl);

        Assert.assertTrue(driver.getPageSource().contains("We are sorry..."));

        logoutAll();

        // now log in again with client that does not have scope

        String accountId = adminClient.realms().realm(CHILD_IDP).clients().findByClientId(ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation manageAccount = adminClient.realms().realm(CHILD_IDP).clients().get(accountId).roles().get(MANAGE_ACCOUNT).toRepresentation();
        RoleRepresentation manageLinks = adminClient.realms().realm(CHILD_IDP).clients().get(accountId).roles().get(MANAGE_ACCOUNT_LINKS).toRepresentation();
        RoleRepresentation userRole = adminClient.realms().realm(CHILD_IDP).roles().get("user").toRepresentation();

        client.setFullScopeAllowed(false);
        ClientResource clientResource = adminClient.realms().realm(CHILD_IDP).clients().get(client.getId());
        clientResource.update(client);

        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(userRole);
        clientResource.getScopeMappings().realmLevel().add(roles);

        navigateTo(helloUrl);
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(helloUrl));
        Assert.assertTrue(driver.getPageSource().contains("Unknown request:"));


        UriBuilder linkBuilder = UriBuilder.fromUri(appPage.getInjectedUrl().toString())
                .path("link");
        String clientLinkUrl = linkBuilder.clone()
                .queryParam("realm", CHILD_IDP)
                .queryParam("provider", PARENT_IDP).build().toString();


        navigateTo(clientLinkUrl);

        Assert.assertTrue(driver.getCurrentUrl().contains("error=not_allowed"));

        logoutAll();

        // add MANAGE_ACCOUNT_LINKS scope should pass.

        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());


        roles = new LinkedList<>();
        roles.add(manageLinks);
        clientResource.getScopeMappings().clientLevel(accountId).add(roles);

        navigateTo(clientLinkUrl);
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");
        Assert.assertTrue(loginPage.isCurrent(PARENT_IDP));
        loginPage.login(PARENT_USERNAME, "password");

        Assert.assertTrue(driver.getCurrentUrl().startsWith(linkBuilder.toTemplate()));
        Assert.assertTrue(driver.getPageSource().contains("Account Linked"));

        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertFalse(links.isEmpty());

        realm.users().get(childUserId).removeFederatedIdentity(PARENT_IDP);
        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        clientResource.getScopeMappings().clientLevel(accountId).remove(roles);

        logoutAll();

        navigateTo(clientLinkUrl);
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");

        Assert.assertTrue(driver.getCurrentUrl().contains("link_error=not_allowed"));

        logoutAll();

        // add MANAGE_ACCOUNT scope should pass

        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());


        roles = new LinkedList<>();
        roles.add(manageAccount);
        clientResource.getScopeMappings().clientLevel(accountId).add(roles);

        navigateTo(clientLinkUrl);
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");
        Assert.assertTrue(loginPage.isCurrent(PARENT_IDP));
        loginPage.login(PARENT_USERNAME, "password");

        Assert.assertTrue(driver.getCurrentUrl().startsWith(linkBuilder.toTemplate()));
        Assert.assertTrue(driver.getPageSource().contains("Account Linked"));

        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertFalse(links.isEmpty());

        realm.users().get(childUserId).removeFederatedIdentity(PARENT_IDP);
        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        clientResource.getScopeMappings().clientLevel(accountId).remove(roles);

        logoutAll();

        navigateTo(clientLinkUrl);
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");

        Assert.assertTrue(driver.getCurrentUrl().contains("link_error=not_allowed"));

        logoutAll();


        // undo fullScopeAllowed

        client = adminClient.realms().realm(CHILD_IDP).clients().findByClientId("client-linking").get(0);
        client.setFullScopeAllowed(true);
        clientResource.update(client);

        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        logoutAll();
    }

    @Test
    public void testAccountLink() throws Exception {
        RealmResource realm = adminClient.realms().realm(CHILD_IDP);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        UriBuilder linkBuilder = UriBuilder.fromUri(appPage.getInjectedUrl().toString())
                .path("link");
        String linkUrl = linkBuilder.clone()
                .queryParam("realm", CHILD_IDP)
                .queryParam("provider", PARENT_IDP).build().toString();
        System.out.println("linkUrl: " + linkUrl);
        navigateTo(linkUrl);
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        Assert.assertTrue(driver.getPageSource().contains(PARENT_IDP));
        loginPage.login("child", "password");
        Assert.assertTrue(loginPage.isCurrent(PARENT_IDP));
        loginPage.login(PARENT_USERNAME, "password");
        System.out.println("After linking: " + driver.getCurrentUrl());
        System.out.println(driver.getPageSource());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(linkBuilder.toTemplate()));
        Assert.assertTrue(driver.getPageSource().contains("Account Linked"));

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest(CHILD_IDP, "child", "password", null, "client-linking", "password");
        Assert.assertNotNull(response.getAccessToken());
        Assert.assertNull(response.getError());
        Client httpClient = AdminClientUtil.createResteasyClient();
        String firstToken = getToken(response, httpClient);
        Assert.assertNotNull(firstToken);


        navigateTo(linkUrl);
        Assert.assertTrue(driver.getPageSource().contains("Account Linked"));
        String nextToken = getToken(response, httpClient);
        Assert.assertNotNull(nextToken);
        Assert.assertNotEquals(firstToken, nextToken);






        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertFalse(links.isEmpty());

        realm.users().get(childUserId).removeFederatedIdentity(PARENT_IDP);
        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        logoutAll();


    }

    private String getToken(OAuthClient.AccessTokenResponse response, Client httpClient) throws Exception {
        String idpToken =  httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("realms")
                .path("child/broker")
                .path(PARENT_IDP)
                .path("token")
                .request()
                .header("Authorization", "Bearer " + response.getAccessToken())
                .get(String.class);
        AccessTokenResponse res = JsonSerialization.readValue(idpToken, AccessTokenResponse.class);
        return res.getToken();
    }

    public void logoutAll() {
        adminClient.realm(CHILD_IDP).logoutAll();
        adminClient.realm(PARENT_IDP).logoutAll();
    }

    @Test
    public void testLinkOnlyProvider() throws Exception {
        RealmResource realm = adminClient.realms().realm(CHILD_IDP);
        IdentityProviderRepresentation rep = realm.identityProviders().get(PARENT_IDP).toRepresentation();
        rep.setLinkOnly(true);
        realm.identityProviders().get(PARENT_IDP).update(rep);
        try {

            List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
            Assert.assertTrue(links.isEmpty());

            UriBuilder linkBuilder = UriBuilder.fromUri(appPage.getInjectedUrl().toString())
                    .path("link");
            String linkUrl = linkBuilder.clone()
                    .queryParam("realm", CHILD_IDP)
                    .queryParam("provider", PARENT_IDP).build().toString();
            navigateTo(linkUrl);
            Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));

            // should not be on login page.  This is what we are testing
            Assert.assertFalse(driver.getPageSource().contains(PARENT_IDP));

            // now test that we can still link.
            loginPage.login("child", "password");
            Assert.assertTrue(loginPage.isCurrent(PARENT_IDP));
            loginPage.login(PARENT_USERNAME, "password");
            System.out.println("After linking: " + driver.getCurrentUrl());
            System.out.println(driver.getPageSource());
            Assert.assertTrue(driver.getCurrentUrl().startsWith(linkBuilder.toTemplate()));
            Assert.assertTrue(driver.getPageSource().contains("Account Linked"));

            links = realm.users().get(childUserId).getFederatedIdentity();
            Assert.assertFalse(links.isEmpty());

            realm.users().get(childUserId).removeFederatedIdentity(PARENT_IDP);
            links = realm.users().get(childUserId).getFederatedIdentity();
            Assert.assertTrue(links.isEmpty());

            logoutAll();

            System.out.println("testing link-only attack");

            navigateTo(linkUrl);
            Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));

            System.out.println("login page uri is: " + driver.getCurrentUrl());

            // ok, now scrape the code from page
            String pageSource = driver.getPageSource();
            String action = ActionURIUtils.getActionURIFromPageSource(pageSource);
            System.out.println("action uri: " + action);

            Map<String, String> queryParams = ActionURIUtils.parseQueryParamsFromActionURI(action);
            System.out.println("query params: " + queryParams);

            // now try and use the code to login to remote link-only idp

            String uri = "/auth/realms/child/broker/parent-idp/login";

            uri = UriBuilder.fromUri(getAuthServerContextRoot())
                    .path(uri)
                    .queryParam(LoginActionsService.SESSION_CODE, queryParams.get(LoginActionsService.SESSION_CODE))
                    .queryParam(Constants.CLIENT_ID, queryParams.get(Constants.CLIENT_ID))
                    .queryParam(Constants.TAB_ID, queryParams.get(Constants.TAB_ID))
                    .build().toString();

            System.out.println("hack uri: " + uri);

            navigateTo(uri);

            Assert.assertTrue(driver.getPageSource().contains("Could not send authentication request to identity provider."));





        } finally {

            rep.setLinkOnly(false);
            realm.identityProviders().get(PARENT_IDP).update(rep);
        }


    }


    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void testAccountLinkingExpired() throws Exception {
        RealmResource realm = adminClient.realms().realm(CHILD_IDP);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        // Login to account mgmt first
        profilePage.open(CHILD_IDP);
        WaitUtils.waitForPageToLoad();

        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");
        profilePage.assertCurrent();

        // Now in another tab, request account linking
        UriBuilder linkBuilder = UriBuilder.fromUri(appPage.getInjectedUrl().toString())
                .path("link");
        String linkUrl = linkBuilder.clone()
                .queryParam("realm", CHILD_IDP)
                .queryParam("provider", PARENT_IDP).build().toString();
        navigateTo(linkUrl);

        Assert.assertTrue(loginPage.isCurrent(PARENT_IDP));

        // Logout "child" userSession in the meantime (for example through admin request)
        realm.logoutAll();

        // Finish login on parent.
        loginPage.login(PARENT_USERNAME, "password");

        // Test I was not automatically linked
        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        errorPage.assertCurrent();
        Assert.assertEquals("Requested broker account linking, but current session is no longer valid.", errorPage.getError());

        logoutAll();
    }

    private void navigateTo(String uri) {
        driver.navigate().to(uri);
        WaitUtils.waitForPageToLoad();
    }

    

}
