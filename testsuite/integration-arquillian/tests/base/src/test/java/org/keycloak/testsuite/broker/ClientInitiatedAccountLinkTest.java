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
package org.keycloak.testsuite.broker;

import org.apache.http.client.utils.URIBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
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
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.adapter.page.AppServerContextRoot;
import org.keycloak.testsuite.adapter.servlet.ClientInitiatedAccountLinkServlet;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.federation.PassThroughFederatedUserStorageProvider;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.pages.AccountFederatedIdentityPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.util.AdapterServletDeployment;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_LINKS;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@AppServerContainer("auth-server-undertow")
public class ClientInitiatedAccountLinkTest extends AbstractKeycloakTest {
    public static final String CHILD_IDP = "child";
    public static final String PARENT_IDP = "parent-idp";
    public static final String PARENT_USERNAME = "parent";

    @Page
    protected UpdateAccountInformationPage profilePage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppServerContextRoot appServerContextRootPage;

    @ArquillianResource
    protected OAuthClient oauth;


    public boolean isRelative() {
        return testContext.isRelativeAdapterTest();
    }

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
    protected ClientApp appPage;

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(CHILD_IDP);
        realm.setEnabled(true);
        ClientRepresentation servlet = new ClientRepresentation();
        servlet.setClientId("client-linking");
        servlet.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String uri = "/client-linking";
        if (!isRelative()) {
            uri = appServerContextRootPage.toString() + uri;
        }
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

    @Deployment(name = "client-linking")
    public static WebArchive customerPortal() {
        return AdapterServletDeployment.oidcDeployment("client-linking", "/account-link-test", ClientInitiatedAccountLinkServlet.class);
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

        // have to add a role as undertow default auth manager doesn't like "*". todo we can remove this eventually as undertow fixes this in later versions
        realm.roles().create(new RoleRepresentation("user", null, false));
        RoleRepresentation role = realm.roles().get("user").toRepresentation();
        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(role);
        realm.users().get(childUserId).roles().realmLevel().add(roles);
        ClientRepresentation brokerService = realm.clients().findByClientId(Constants.BROKER_SERVICE_CLIENT_ID).get(0);
        role = realm.clients().get(brokerService.getId()).roles().get(Constants.READ_TOKEN_ROLE).toRepresentation();
        roles.clear();
        roles.add(role);
        realm.users().get(childUserId).roles().clientLevel(brokerService.getId()).add(roles);

    }

    @Before
    public void createBroker() {
        createParentChild();
    }

    public void createParentChild() {
        BrokerTestTools.createKcOidcBroker(adminClient, CHILD_IDP, PARENT_IDP, suiteContext);
    }

    //@Test
    public void testUi() throws Exception {
        Thread.sleep(1000000000);

    }

    @Test
    public void testErrorConditions() throws Exception {

        RealmResource realm = adminClient.realms().realm(CHILD_IDP);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        ClientRepresentation client = adminClient.realms().realm(CHILD_IDP).clients().findByClientId("client-linking").get(0);

        UriBuilder redirectUri = UriBuilder.fromUri(appPage.getInjectedUrl().toString())
                .path("link")
                .queryParam("response", "true");

        UriBuilder directLinking = UriBuilder.fromUri(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth")
                .path("realms/child/broker/{provider}/link")
                .queryParam("client_id", "client-linking")
                .queryParam("redirect_uri", redirectUri.build())
                .queryParam("hash", Base64Url.encode("crap".getBytes()))
                .queryParam("nonce", UUID.randomUUID().toString());

        String linkUrl = directLinking
                .build(PARENT_IDP).toString();

        // test not logged in

        driver.navigate().to(linkUrl);
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");

        Assert.assertTrue(driver.getCurrentUrl().contains("link_error=not_logged_in"));

        logoutAll();

        // now log in

        driver.navigate().to( appPage.getInjectedUrl() + "/hello");
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(appPage.getInjectedUrl() + "/hello"));
        Assert.assertTrue(driver.getPageSource().contains("Unknown request:"));

        // now test CSRF with bad hash.

        driver.navigate().to(linkUrl);

        Assert.assertTrue(driver.getPageSource().contains("We're sorry..."));

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

        driver.navigate().to( appPage.getInjectedUrl() + "/hello");
        Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));
        loginPage.login("child", "password");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(appPage.getInjectedUrl() + "/hello"));
        Assert.assertTrue(driver.getPageSource().contains("Unknown request:"));


        UriBuilder linkBuilder = UriBuilder.fromUri(appPage.getInjectedUrl().toString())
                .path("link");
        String clientLinkUrl = linkBuilder.clone()
                .queryParam("realm", CHILD_IDP)
                .queryParam("provider", PARENT_IDP).build().toString();


        driver.navigate().to(clientLinkUrl);

        Assert.assertTrue(driver.getCurrentUrl().contains("error=not_allowed"));

        logoutAll();

        // add MANAGE_ACCOUNT_LINKS scope should pass.

        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());


        roles = new LinkedList<>();
        roles.add(manageLinks);
        clientResource.getScopeMappings().clientLevel(accountId).add(roles);

        driver.navigate().to(clientLinkUrl);
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

        driver.navigate().to(clientLinkUrl);
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

        driver.navigate().to(clientLinkUrl);
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

        driver.navigate().to(clientLinkUrl);
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
        driver.navigate().to(linkUrl);
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
        Client httpClient = ClientBuilder.newClient();
        String firstToken = getToken(response, httpClient);
        Assert.assertNotNull(firstToken);


        driver.navigate().to(linkUrl);
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
        String idpToken =  httpClient.target(oauth.AUTH_SERVER_ROOT)
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
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder()).build(CHILD_IDP).toString();
        driver.navigate().to(logoutUri);
        logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder()).build(PARENT_IDP).toString();
        driver.navigate().to(logoutUri);
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
            driver.navigate().to(linkUrl);
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

            driver.navigate().to(linkUrl);
            Assert.assertTrue(loginPage.isCurrent(CHILD_IDP));

            System.out.println("login page uri is: " + driver.getCurrentUrl());

            // ok, now scrape the code from page
            String pageSource = driver.getPageSource();
            Pattern p = Pattern.compile("action=\"(.+)\"");
            Matcher m = p.matcher(pageSource);
            String action = null;
            if (m.find()) {
                action = m.group(1);

            }
            System.out.println("action: " + action);

            p = Pattern.compile("code=(.+)&");
            m = p.matcher(action);
            String code = null;
            if (m.find()) {
                code = m.group(1);

            }
            System.out.println("code: " + code);

            // now try and use the code to login to remote link-only idp

            String uri = "/auth/realms/child/broker/parent-idp/login";

            uri = UriBuilder.fromUri(AuthServerTestEnricher.getAuthServerContextRoot())
                    .path(uri)
                    .queryParam("code", code)
                    .build().toString();

            System.out.println("hack uri: " + uri);

            driver.navigate().to(uri);

            Assert.assertTrue(driver.getPageSource().contains("Could not send authentication request to identity provider."));





        } finally {

            rep.setLinkOnly(false);
            realm.identityProviders().get(PARENT_IDP).update(rep);
        }


    }


}
