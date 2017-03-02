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
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
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

import javax.ws.rs.core.UriBuilder;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
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

        // have to add a role as stupid undertow auth manager doesn't like "*"
        realm.roles().create(new RoleRepresentation("user", null, false));
        RoleRepresentation role = realm.roles().get("user").toRepresentation();
        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(role);
        realm.users().get(childUserId).roles().realmLevel().add(roles);

    }

    @Before
    public void createBroker() {
        createParentChild();
    }

    public void createParentChild() {
        BrokerTestTools.createKcOidcBroker(adminClient, CHILD_IDP, PARENT_IDP, suiteContext);
    }

    @Test
    public void testErrorConditions() {
        RealmResource realm = adminClient.realms().realm(CHILD_IDP);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertTrue(links.isEmpty());

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

        Assert.assertTrue(driver.getCurrentUrl().contains("error=not_logged_in"));


        // now log in

        driver.navigate().to( appPage.getInjectedUrl() + "/hello");
        loginPage.login("child", "password");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(appPage.getInjectedUrl() + "/hello"));

        // now test CSRF with bad hash.

        driver.navigate().to(linkUrl);

        Assert.assertTrue(driver.getPageSource().contains("We're sorry..."));



    }

    @Test
    public void testAccountLink() {
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
        loginPage.login("child", "password");
        Assert.assertTrue(loginPage.isCurrent(PARENT_IDP));
        loginPage.login(PARENT_USERNAME, "password");
        System.out.println("After linking: " + driver.getCurrentUrl());
        System.out.println(driver.getPageSource());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(linkBuilder.toTemplate()));
        Assert.assertTrue(driver.getPageSource().contains("Account Linked"));

        links = realm.users().get(childUserId).getFederatedIdentity();
        Assert.assertFalse(links.isEmpty());
    }




}
