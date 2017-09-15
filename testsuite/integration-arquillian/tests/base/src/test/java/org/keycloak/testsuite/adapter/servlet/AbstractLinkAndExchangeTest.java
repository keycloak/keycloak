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
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.broker.BrokerTestTools;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
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

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractLinkAndExchangeTest extends AbstractServletsAdapterTest {
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

        public static final String DEPLOYMENT_NAME = "exchange-linking";

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
        servlet.setClientId(ClientApp.DEPLOYMENT_NAME);
        servlet.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String uri = "/" + ClientApp.DEPLOYMENT_NAME;
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


    @Deployment(name = ClientApp.DEPLOYMENT_NAME)
    protected static WebArchive accountLink() {
        return servletDeployment(ClientApp.DEPLOYMENT_NAME, LinkAndExchangeServlet.class, ServletTestUtils.class);
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

    public static void setupRealm(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(CHILD_IDP);
        ClientModel client = realm.getClientByClientId(ClientApp.DEPLOYMENT_NAME);
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(PARENT_IDP);
        Assert.assertNotNull(idp);

        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        management.idps().setPermissionsEnabled(idp, true);
        ClientPolicyRepresentation clientRep = new ClientPolicyRepresentation();
        clientRep.setName("toIdp");
        clientRep.addClient(client.getId());
        ResourceServer server = management.realmResourceServer();
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(clientRep, server);
        management.idps().exchangeToPermission(idp).addAssociatedPolicy(clientPolicy);

    }
    public static void turnOffTokenStore(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(CHILD_IDP);
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(PARENT_IDP);
        idp.setStoreToken(false);
        realm.updateIdentityProvider(idp);

    }
    public static void turnOnTokenStore(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(CHILD_IDP);
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(PARENT_IDP);
        idp.setStoreToken(true);
        realm.updateIdentityProvider(idp);
    }
    @Before
    public void createBroker() {
        createParentChild();
        testingClient.server().run(AbstractLinkAndExchangeTest::setupRealm);
    }

    public void createParentChild() {
        BrokerTestTools.createKcOidcBroker(adminClient, CHILD_IDP, PARENT_IDP, suiteContext);
    }


    @Test
    public void testAccountLink() throws Exception {
        testingClient.server().run(AbstractLinkAndExchangeTest::turnOnTokenStore);

        RealmResource realm = adminClient.realms().realm(CHILD_IDP);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

        String servletUri = appPage.getInjectedUrl().toString();
        UriBuilder linkBuilder = UriBuilder.fromUri(servletUri)
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
        Assert.assertTrue(driver.getPageSource().contains("Exchange token received"));

        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertFalse(links.isEmpty());



        // do exchange

        String accessToken = oauth.doGrantAccessTokenRequest(CHILD_IDP, "child", "password", null, ClientApp.DEPLOYMENT_NAME, "password").getAccessToken();
        Client httpClient = ClientBuilder.newClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(CHILD_IDP)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(ClientApp.DEPLOYMENT_NAME, "password"))
                .post(Entity.form(
                        new Form()
                        .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                        .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                        .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                        .param(OAuth2Constants.REQUESTED_ISSUER, PARENT_IDP)

                ));
        Assert.assertEquals(200, response.getStatus());
        AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
        response.close();
        String externalToken = tokenResponse.getToken();
        Assert.assertNotNull(externalToken);
        Assert.assertTrue(tokenResponse.getExpiresIn() > 0);
        setTimeOffset((int)tokenResponse.getExpiresIn() + 1);

        // test that token refresh happens

        // get access token again because we may have timed out
        accessToken = oauth.doGrantAccessTokenRequest(CHILD_IDP, "child", "password", null, ClientApp.DEPLOYMENT_NAME, "password").getAccessToken();
        response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(ClientApp.DEPLOYMENT_NAME, "password"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_ISSUER, PARENT_IDP)

                ));
        Assert.assertEquals(200, response.getStatus());
        tokenResponse = response.readEntity(AccessTokenResponse.class);
        response.close();
        Assert.assertNotEquals(externalToken, tokenResponse.getToken());


        logoutAll();


        realm.users().get(childUserId).removeFederatedIdentity(PARENT_IDP);
        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());



    }

    @Test
    public void testAccountLinkNoTokenStore() throws Exception {
        testingClient.server().run(AbstractLinkAndExchangeTest::turnOffTokenStore);

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
        Assert.assertTrue(driver.getPageSource().contains("Exchange token received"));

        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertFalse(links.isEmpty());



        logoutAll();


        realm.users().get(childUserId).removeFederatedIdentity(PARENT_IDP);
        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());



    }


    public void logoutAll() {
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder()).build(CHILD_IDP).toString();
        navigateTo(logoutUri);
        logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder()).build(PARENT_IDP).toString();
        navigateTo(logoutUri);
    }

    private void navigateTo(String uri) {
        driver.navigate().to(uri);
        WaitUtils.waitForPageToLoad();
    }

    

}
