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
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.ProductPortal;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
public class UserStorageConsentTest extends AbstractServletsAdapterTest {

    @Page
    private ProductPortal productPortal;

    @Page
    protected ConsentPage consentPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Page
    protected InfoPage infoPage;

    @Deployment(name = ProductPortal.DEPLOYMENT_NAME)
    protected static WebArchive productPortal() {
        return servletDeployment(ProductPortal.DEPLOYMENT_NAME, ProductServlet.class);
    }

    @BeforeClass
    public static void checkNotMapStorage() {
        // This test requires user storage SPI
        ProfileAssume.assumeFeatureDisabled(Feature.MAP_STORAGE);
    }

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("memory");
        memProvider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));
        memProvider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));

        addComponent(memProvider);
    }

    protected String addComponent(ComponentRepresentation component) {
        Response resp = testRealmResource().components().add(component);
        resp.close();
        String id = ApiUtil.getCreatedId(resp);
        getCleanup().addComponentId(id);
        return id;
    }

    public static void setupConsent(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("demo");
        ClientModel product = session.clients().getClientByClientId(realm, "product-portal");
        product.setConsentRequired(true);
        ClientScopeModel clientScope = realm.addClientScope("clientScope");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        System.err.println("client scope protocol mappers size: " + clientScope.getProtocolMappersStream().count());

        for (ProtocolMapperModel mapper : product.getProtocolMappersStream().collect(Collectors.toList())) {
            if (mapper.getProtocol().equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
                if (mapper.getName().equals(OIDCLoginProtocolFactory.USERNAME)
                        || mapper.getName().equals(OIDCLoginProtocolFactory.EMAIL)
                        || mapper.getName().equals(OIDCLoginProtocolFactory.GIVEN_NAME)
                ) {
                    ProtocolMapperModel copy = new ProtocolMapperModel();
                    copy.setName(mapper.getName());
                    copy.setProtocol(mapper.getProtocol());
                    Map<String, String> config = new HashMap<>();
                    config.putAll(mapper.getConfig());
                    copy.setConfig(config);
                    copy.setProtocolMapper(mapper.getProtocolMapper());
                    clientScope.addProtocolMapper(copy);
                }
            }
            product.removeProtocolMapper(mapper);
        }
        product.addClientScope(clientScope, true);
    }

    public static void setupDisplayClientOnConsentScreen(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("demo");
        ClientModel product = session.clients().getClientByClientId(realm, "product-portal");
        product.setDisplayOnConsentScreen(true);
    }

    /**
     * KEYCLOAK-5273
     *
     * @throws Exception
     */
    @Test
    public void testLogin() throws Exception {
        assertLogin();
    }

    @Test
    public void testLoginDisplayClientOnConsentScreen() throws Exception {
        testingClient.server().run(UserStorageConsentTest::setupDisplayClientOnConsentScreen);
        assertLogin();
    }

    private void assertLogin() throws InterruptedException {
        testingClient.server().run(UserStorageConsentTest::setupConsent);
        UserRepresentation memuser = new UserRepresentation();
        memuser.setUsername("memuser");
        String uid = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealmResource(), memuser, "password");
        System.out.println("uid: " + uid);
        Assert.assertTrue(uid.startsWith("f:"));  // make sure its federated
        RoleRepresentation roleRep = adminClient.realm("demo").roles().get("user").toRepresentation();
        List<RoleRepresentation> roleList = new ArrayList<>();
        roleList.add(roleRep);
        adminClient.realm("demo").users().get(uid).roles().realmLevel().add(roleList);

        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("memuser", "password");
        Assert.assertTrue(consentPage.isCurrent());
        consentPage.confirm();
        assertCurrentUrlEquals(productPortal.toString());
        Assert.assertTrue(driver.getPageSource().contains("iPhone"));

        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .build("demo").toString();

        driver.navigate().to(logoutUri);
        waitForPageToLoad();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();
        waitForPageToLoad();
        infoPage.assertCurrent();

        driver.navigate().to(productPortal.toString());
        waitForPageToLoad();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("memuser", "password");
        assertCurrentUrlEquals(productPortal.toString());
        Assert.assertTrue(driver.getPageSource().contains("iPhone"));

        driver.navigate().to(logoutUri);
        waitForPageToLoad();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();
        adminClient.realm("demo").users().delete(uid).close();
    }
}
