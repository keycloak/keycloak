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
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.Profile;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.broker.BrokerTestTools;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;

/**
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
public class BrokerLinkAndTokenExchangeTest extends AbstractServletsAdapterTest {
    public static final String CHILD_IDP = "child";
    public static final String PARENT_IDP = "parent-idp";
    public static final String PARENT_USERNAME = "parent";
    public static final String PARENT2_USERNAME = "parent2";
    public static final String PARENT3_USERNAME = "parent3";
    public static final String UNAUTHORIZED_CHILD_CLIENT = "unauthorized-child-client";
    public static final String PARENT_CLIENT = "parent-client";

    @Deployment(name = ClientApp.DEPLOYMENT_NAME)
    protected static WebArchive accountLink() {
        return servletDeployment(ClientApp.DEPLOYMENT_NAME, LinkAndExchangeServlet.class, ServletTestUtils.class);
    }

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
        servlet.setEnabled(true);
        servlet.setAdminUrl(uri);
        servlet.setDirectAccessGrantsEnabled(true);
        servlet.setBaseUrl(uri);
        servlet.setRedirectUris(new LinkedList<>());
        servlet.getRedirectUris().add(uri + "/*");
        servlet.setSecret("password");
        servlet.setFullScopeAllowed(true);
        realm.setClients(new LinkedList<>());
        realm.getClients().add(servlet);

        ClientRepresentation unauthorized = new ClientRepresentation();
        unauthorized.setEnabled(true);
        unauthorized.setClientId(UNAUTHORIZED_CHILD_CLIENT);
        unauthorized.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        unauthorized.setDirectAccessGrantsEnabled(true);
        unauthorized.setSecret("password");
        unauthorized.setFullScopeAllowed(true);
        realm.getClients().add(unauthorized);

        testRealms.add(realm);

        realm = new RealmRepresentation();
        realm.setRealm(PARENT_IDP);
        realm.setEnabled(true);
        ClientRepresentation parentApp = new ClientRepresentation();
        parentApp.setEnabled(true);
        parentApp.setClientId(PARENT_CLIENT);
        parentApp.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        parentApp.setDirectAccessGrantsEnabled(true);
        parentApp.setSecret("password");
        parentApp.setFullScopeAllowed(true);
        realm.setClients(new LinkedList<>());
        realm.getClients().add(parentApp);
        testRealms.add(realm);

    }

    @Test
    @DisableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
    @UncaughtServerErrorExpected
    public void testFeatureDisabled() throws Exception {
        checkFeature(Response.Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public void testFeatureEnabled() throws Exception {
        checkFeature(Response.Status.OK.getStatusCode());
    }

    @Before
    public void beforeTest() throws Exception {
        addIdpUser();
        addChildUser();
        createBroker();
    }

    public void addIdpUser() {
        RealmResource realm = adminClient.realms().realm(PARENT_IDP);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(PARENT_USERNAME);
        user.setEnabled(true);
        createUserAndResetPasswordWithAdminClient(realm, user, "password");
        user = new UserRepresentation();
        user.setUsername(PARENT2_USERNAME);
        user.setEnabled(true);
        createUserAndResetPasswordWithAdminClient(realm, user, "password");
        user = new UserRepresentation();
        user.setUsername(PARENT3_USERNAME);
        user.setFirstName("first name");
        user.setLastName("last name");
        user.setEmail("email");
        user.setEnabled(true);
        createUserAndResetPasswordWithAdminClient(realm, user, "password");        
    }

    private String childUserId = null;

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

        ClientModel directExchanger = realm.addClient("direct-exchanger");
        directExchanger.setClientId("direct-exchanger");
        directExchanger.setPublicClient(false);
        directExchanger.setDirectAccessGrantsEnabled(true);
        directExchanger.setEnabled(true);
        directExchanger.setSecret("secret");
        directExchanger.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directExchanger.setFullScopeAllowed(false);


        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        management.idps().setPermissionsEnabled(idp, true);
        ClientPolicyRepresentation clientRep = new ClientPolicyRepresentation();
        clientRep.setName("toIdp");
        clientRep.addClient(client.getId());
        clientRep.addClient(directExchanger.getId());
        ResourceServer server = management.realmResourceServer();
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientRep);
        management.idps().exchangeToPermission(idp).addAssociatedPolicy(clientPolicy);


        // permission for user impersonation for a client

        ClientPolicyRepresentation clientImpersonateRep = new ClientPolicyRepresentation();
        clientImpersonateRep.setName("clientImpersonators");
        clientImpersonateRep.addClient(directExchanger.getId());
        server = management.realmResourceServer();
        Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientImpersonateRep);
        management.users().setPermissionsEnabled(true);
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

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

    public void createBroker() {
        createParentChild();
        testingClient.server().run(BrokerLinkAndTokenExchangeTest::setupRealm);
    }

    public void createParentChild() {
        BrokerTestTools.createKcOidcBroker(adminClient, CHILD_IDP, PARENT_IDP);
    }


    @Test
    @UncaughtServerErrorExpected
    public void testAccountLink() throws Exception {
        testingClient.server().run(BrokerLinkAndTokenExchangeTest::turnOnTokenStore);
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
        Client httpClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget exchangeUrl = childTokenExchangeWebTarget(httpClient);
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
            setTimeOffset((int) tokenResponse.getExpiresIn() + 1);

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

            // test direct exchange
            response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-exchanger", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "child")
                                    .param(OAuth2Constants.REQUESTED_ISSUER, PARENT_IDP)

                    ));
            Assert.assertEquals(200, response.getStatus());
            tokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();
            Assert.assertNotEquals(externalToken, tokenResponse.getToken());


            resetTimeOffset();
            logoutAll();


            realm.users().get(childUserId).removeFederatedIdentity(PARENT_IDP);
            links = realm.users().get(childUserId).getFederatedIdentity();
            Assert.assertTrue(links.isEmpty());
        } finally {
            httpClient.close();
        }
    }

    protected WebTarget childTokenExchangeWebTarget(Client httpClient) {
        return httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(CHILD_IDP)
                .path("protocol/openid-connect/token");

    }
    protected WebTarget childLogoutWebTarget(Client httpClient) {
        return httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(CHILD_IDP)
                .path("protocol/openid-connect/logout");

    }
    protected String parentJwksUrl() {
        return UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(PARENT_IDP)
                .path("protocol/openid-connect")
                .path(OIDCLoginProtocolService.class, "certs").build().toString();

    }

    @Test
    @UncaughtServerErrorExpected
    public void testAccountLinkNoTokenStore() {
        testingClient.server().run(BrokerLinkAndTokenExchangeTest::turnOffTokenStore);

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
        Assert.assertTrue("Unexpected page. Current Page URL: " + driver.getCurrentUrl(),loginPage.isCurrent(PARENT_IDP));
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


    /**
     * KEYCLOAK-6026
     */
    @Test
    @UncaughtServerErrorExpected
    public void testExportImport() throws Exception {
        ContainerAssume.assumeNotAuthServerRemote();

        testExternalExchange();

        try {
            testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
            String targetFilePath = testingClient.testing().exportImport().getExportImportTestDirectory() + File.separator + "singleFile-full.json";
            testingClient.testing().exportImport().setFile(targetFilePath);
            testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_EXPORT);
            testingClient.testing().exportImport().setRealmName(CHILD_IDP);
            testingClient.testing().exportImport().runExport();

            adminClient.realms().realm(CHILD_IDP).remove();
            testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

            testingClient.testing().exportImport().runImport();
        } finally {
            testingClient.testing().exportImport().clear();
        }

        testExternalExchange();
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExternalExchange() throws Exception {
        RealmResource childRealm = adminClient.realms().realm(CHILD_IDP);

        String accessToken = oauth.doGrantAccessTokenRequest(PARENT_IDP, PARENT2_USERNAME, "password", null, PARENT_CLIENT, "password").getAccessToken();
        Assert.assertEquals(0, adminClient.realm(CHILD_IDP).getClientSessionStats().size());

        Client httpClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget exchangeUrl = childTokenExchangeWebTarget(httpClient);
            System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

            checkFeature(200);

            IdentityProviderRepresentation rep = adminClient.realm(CHILD_IDP).identityProviders().get(PARENT_IDP).toRepresentation();
            rep.getConfig().put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, String.valueOf(true));
            rep.getConfig().put(OIDCIdentityProviderConfig.USE_JWKS_URL, String.valueOf(true));
            rep.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, parentJwksUrl());
            String parentIssuer = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)
                    .path("/realms")
                    .path(PARENT_IDP)
                    .build().toString();
            rep.getConfig().put("issuer", parentIssuer);
            adminClient.realm(CHILD_IDP).identityProviders().get(PARENT_IDP).update(rep);

            String exchangedUserId;
            String exchangedUsername;

            {
                // test signature validation
                Response response = exchangeUrl.request()
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(ClientApp.DEPLOYMENT_NAME, "password"))
                        .post(Entity.form(
                                new Form()
                                        .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                        .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                        .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.JWT_TOKEN_TYPE)
                                        .param(OAuth2Constants.SUBJECT_ISSUER, PARENT_IDP)
                                        .param(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)

                        ));
                Assert.assertEquals(200, response.getStatus());
                AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
                String idToken = tokenResponse.getIdToken();
                JWSInput jws = new JWSInput(tokenResponse.getToken());
                AccessToken token = jws.readJsonContent(AccessToken.class);
                response.close();

                exchangedUserId = token.getSubject();
                exchangedUsername = token.getPreferredUsername();

                System.out.println("exchangedUserId: " + exchangedUserId);
                System.out.println("exchangedUsername: " + exchangedUsername);


                // test that we can exchange back to external token
                response = exchangeUrl.request()
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(ClientApp.DEPLOYMENT_NAME, "password"))
                        .post(Entity.form(
                                new Form()
                                        .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                        .param(OAuth2Constants.SUBJECT_TOKEN, tokenResponse.getToken())
                                        .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                        .param(OAuth2Constants.REQUESTED_ISSUER, PARENT_IDP)

                        ));
                Assert.assertEquals(200, response.getStatus());
                tokenResponse = response.readEntity(AccessTokenResponse.class);
                Assert.assertEquals(accessToken, tokenResponse.getToken());
                response.close();

                Assert.assertEquals(1, adminClient.realm(CHILD_IDP).getClientSessionStats().size());

                // test logout
                response = childLogoutWebTarget(httpClient)
                        .queryParam("id_token_hint", idToken)
                        .request()
                        .get();
                response.close();

                Assert.assertEquals(0, adminClient.realm(CHILD_IDP).getClientSessionStats().size());


                List<FederatedIdentityRepresentation> links = childRealm.users().get(exchangedUserId).getFederatedIdentity();
                Assert.assertEquals(1, links.size());
            }
            {
                // check that we can request an exchange again and that the previously linked user is obtained
                Response response = exchangeUrl.request()
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(ClientApp.DEPLOYMENT_NAME, "password"))
                        .post(Entity.form(
                                new Form()
                                        .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                        .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                        .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.JWT_TOKEN_TYPE)
                                        .param(OAuth2Constants.SUBJECT_ISSUER, PARENT_IDP)
                                        .param(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)

                        ));
                Assert.assertEquals(200, response.getStatus());
                AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
                String idToken = tokenResponse.getIdToken();
                JWSInput jws = new JWSInput(tokenResponse.getToken());
                AccessToken token = jws.readJsonContent(AccessToken.class);
                response.close();

                String exchanged2UserId = token.getSubject();
                String exchanged2Username = token.getPreferredUsername();

                // assert that we get the same linked account as was previously imported

                Assert.assertEquals(exchangedUserId, exchanged2UserId);
                Assert.assertEquals(exchangedUsername, exchanged2Username);

                // test logout
                response = childLogoutWebTarget(httpClient)
                        .queryParam("id_token_hint", idToken)
                        .request()
                        .get();
                response.close();

                Assert.assertEquals(0, adminClient.realm(CHILD_IDP).getClientSessionStats().size());


                List<FederatedIdentityRepresentation> links = childRealm.users().get(exchangedUserId).getFederatedIdentity();
                Assert.assertEquals(1, links.size());
            }
            {
                // check that we can exchange without specifying an SUBJECT_ISSUER
                Response response = exchangeUrl.request()
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(ClientApp.DEPLOYMENT_NAME, "password"))
                        .post(Entity.form(
                                new Form()
                                        .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                        .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                        .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.JWT_TOKEN_TYPE)
                                        .param(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)

                        ));
                Assert.assertEquals(200, response.getStatus());
                AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
                String idToken = tokenResponse.getIdToken();
                JWSInput jws = new JWSInput(tokenResponse.getToken());
                AccessToken token = jws.readJsonContent(AccessToken.class);
                response.close();

                String exchanged2UserId = token.getSubject();
                String exchanged2Username = token.getPreferredUsername();

                // assert that we get the same linked account as was previously imported

                Assert.assertEquals(exchangedUserId, exchanged2UserId);
                Assert.assertEquals(exchangedUsername, exchanged2Username);

                // test logout
                response = childLogoutWebTarget(httpClient)
                        .queryParam("id_token_hint", idToken)
                        .request()
                        .get();
                response.close();

                Assert.assertEquals(0, adminClient.realm(CHILD_IDP).getClientSessionStats().size());


                List<FederatedIdentityRepresentation> links = childRealm.users().get(exchangedUserId).getFederatedIdentity();
                Assert.assertEquals(1, links.size());
            }
            // cleanup  remove the user
            childRealm.users().get(exchangedUserId).remove();

            {
                // test unauthorized client gets 403
                try (Response response = exchangeUrl.request()
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(UNAUTHORIZED_CHILD_CLIENT, "password"))
                        .post(Entity.form(
                                new Form()
                                        .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                        .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                        .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.JWT_TOKEN_TYPE)
                                        .param(OAuth2Constants.SUBJECT_ISSUER, PARENT_IDP)

                        ))) {
                    Assert.assertEquals(403, response.getStatus());
                }
            }
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * KEYCLOAK-14577, see also KEYCLOAK-10932
     */
    @Test
    public void testExternalExchange_extractIdentityFromProfile() throws Exception {
        RealmResource childRealm = adminClient.realms().realm(CHILD_IDP);

        String accessToken = oauth.doGrantAccessTokenRequest(PARENT_IDP, PARENT3_USERNAME, "password", null, PARENT_CLIENT, "password").getAccessToken();
        Assert.assertEquals(0, adminClient.realm(CHILD_IDP).getClientSessionStats().size());

        Client httpClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget exchangeUrl = childTokenExchangeWebTarget(httpClient);
            IdentityProviderRepresentation rep = adminClient.realm(CHILD_IDP).identityProviders().get(PARENT_IDP).toRepresentation();
            rep.getConfig().put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, String.valueOf(false));
            adminClient.realm(CHILD_IDP).identityProviders().get(PARENT_IDP).update(rep);

            AccessToken token;
            try (Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(ClientApp.DEPLOYMENT_NAME, "password"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                    .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.JWT_TOKEN_TYPE)
                                    .param(OAuth2Constants.SUBJECT_ISSUER, PARENT_IDP)
                                    .param(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
                    ))) {
                Assert.assertEquals(200, response.getStatus());

                AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
                JWSInput jws = new JWSInput(tokenResponse.getToken());
                token = jws.readJsonContent(AccessToken.class);
            }

            Assert.assertNotNull(token);
            Assert.assertNotNull(token.getSubject());
            Assert.assertEquals(PARENT3_USERNAME, token.getPreferredUsername());
            Assert.assertEquals("first name", token.getGivenName());
            Assert.assertEquals("last name", token.getFamilyName());
            Assert.assertEquals("email", token.getEmail());

            // cleanup remove the user
            childRealm.users().get(token.getSubject()).remove();
        } finally {
            httpClient.close();
        }
    }

    public void logoutAll() {
        adminClient.realm(CHILD_IDP).logoutAll();
        adminClient.realm(PARENT_IDP).logoutAll();
    }

    private void navigateTo(String uri) {
        driver.navigate().to(uri);
        WaitUtils.waitForPageToLoad();
    }

    private void checkFeature(int statusCode) throws Exception {
        String accessToken = oauth.doGrantAccessTokenRequest(PARENT_IDP, PARENT2_USERNAME, "password", null, PARENT_CLIENT, "password").getAccessToken();

        if (statusCode != Response.Status.NOT_IMPLEMENTED.getStatusCode()) {
            Assert.assertEquals(0, adminClient.realm(CHILD_IDP).getClientSessionStats().size());
        }

        Client httpClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget exchangeUrl = childTokenExchangeWebTarget(httpClient);
            {
                IdentityProviderRepresentation rep = adminClient.realm(CHILD_IDP).identityProviders().get(PARENT_IDP).toRepresentation();
                rep.getConfig().put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, String.valueOf(false));
                adminClient.realm(CHILD_IDP).identityProviders().get(PARENT_IDP).update(rep);
                // test user info validation.
                Response response = exchangeUrl.request()
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(ClientApp.DEPLOYMENT_NAME, "password"))
                        .post(Entity.form(
                                new Form()
                                        .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                        .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                        .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.JWT_TOKEN_TYPE)
                                        .param(OAuth2Constants.SUBJECT_ISSUER, PARENT_IDP)
                                        .param(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)

                        ));
                Assert.assertEquals(statusCode, response.getStatus());

                if (statusCode != Response.Status.NOT_IMPLEMENTED.getStatusCode()) {
                    AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
                    String idToken = tokenResponse.getIdToken();
                    Assert.assertNotNull(idToken);
                    response.close();

                    Assert.assertEquals(1, adminClient.realm(CHILD_IDP).getClientSessionStats().size());

                    // test logout
                    response = childLogoutWebTarget(httpClient)
                            .queryParam("id_token_hint", idToken)
                            .request()
                            .get();
                    response.close();

                    Assert.assertEquals(0, adminClient.realm(CHILD_IDP).getClientSessionStats().size());
                }
            }
        } finally {
            httpClient.close();
        }
    }
}
