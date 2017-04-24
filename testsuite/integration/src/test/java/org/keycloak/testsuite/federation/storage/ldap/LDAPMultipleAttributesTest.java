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

package org.keycloak.testsuite.federation.storage.ldap;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPMultipleAttributesTest {

    protected String APP_SERVER_BASE_URL = "http://localhost:8081";
    protected String LOGIN_URL = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(APP_SERVER_BASE_URL + "/auth")).build("test").toString();


    // Skip this test on MSAD due to lack of supported user multivalued attributes
    private static LDAPRule ldapRule = new LDAPRule((Map<String, String> ldapConfig) -> {

        String vendor = ldapConfig.get(LDAPConstants.VENDOR);
        return (vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY));

    });


    public static UserStorageProviderModel ldapModel = null;

    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            MultivaluedHashMap<String,String> ldapConfig = LDAPTestUtils.getLdapRuleConfig(ldapRule);
            ldapConfig.putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setLastSync(0);
            model.setChangedSyncPeriod(-1);
            model.setFullSyncPeriod(-1);
            model.setName("test-ldap");
            model.setPriority(0);
            model.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
            model.setConfig(ldapConfig);
            ldapModel = new UserStorageProviderModel(appRealm.addComponentModel(model));

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);
            LDAPTestUtils.addUserAttributeMapper(appRealm, ldapModel, "streetMapper", "street", LDAPConstants.STREET);

            // Remove current users and add default users
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            LDAPObject james = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "jbrown", "James", "Brown", "jbrown@keycloak.org", null, "88441");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, james, "Password1");

            // User for testing duplicating surname and postalCode
            LDAPObject bruce = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "bwilson", "Bruce", "Wilson", "bwilson@keycloak.org", "Elm 5", "88441", "77332");
            bruce.setAttribute("sn", new LinkedHashSet<>(Arrays.asList("Wilson", "Schneider")));
            ldapFedProvider.getLdapIdentityStore().update(bruce);
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, bruce, "Password1");

            // Create ldap-portal client
            ClientModel ldapClient = KeycloakModelUtils.createClient(appRealm, "ldap-portal");
            ldapClient.addRedirectUri("/ldap-portal");
            ldapClient.addRedirectUri("/ldap-portal/*");
            ldapClient.setManagementUrl("/ldap-portal");
            ldapClient.addProtocolMapper(UserAttributeMapper.createClaimMapper("postalCode", "postal_code", "postal_code", "String", true, "", true, true, true));
            ldapClient.addProtocolMapper(UserAttributeMapper.createClaimMapper("street", "street", "street", "String", true, "", true, true, false));
            ldapClient.addScopeMapping(appRealm.getRole("user"));
            ldapClient.setSecret("password");

            // Deploy ldap-portal client
            URL url = getClass().getResource("/ldap/ldap-app-keycloak.json");
            keycloakRule.createApplicationDeployment()
                    .name("ldap-portal").contextPath("/ldap-portal")
                    .servletClass(LDAPExampleServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    protected void checkUserAndImportMode(KeycloakSession session, RealmModel realm, String username, String expectedFirstName, String expectedLastName, String expectedEmail, String expectedPostalCode) {
        LDAPTestUtils.assertUserImported(session.userLocalStorage(), realm, "jbrown", "James", "Brown", "jbrown@keycloak.org", "88441");
    }

    protected void checkImportMode(KeycloakSession session, RealmModel realm, UserModel user) {
        Assert.assertNotNull(session.userLocalStorage().getUserById(user.getId(), realm));

    }


    @Test
    public void testModel() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            session.userCache().clear();
            RealmModel appRealm = session.realms().getRealmByName("test");

            checkUserAndImportMode(session, appRealm, "jbrown", "James", "Brown", "jbrown@keycloak.org", "88441");

            UserModel user = session.users().getUserByUsername("bwilson", appRealm);
            checkImportMode(session, appRealm, user);
            Assert.assertEquals("bwilson@keycloak.org", user.getEmail());
            Assert.assertEquals("Bruce", user.getFirstName());

            // There are 2 lastnames in ldif
            Assert.assertTrue("Wilson".equals(user.getLastName()) || "Schneider".equals(user.getLastName()));

            // Actually there are 2 postalCodes
            List<String> postalCodes = user.getAttribute("postal_code");
            assertPostalCodes(postalCodes, "88441", "77332");
            List<String> tmp = new LinkedList<>();
            tmp.addAll(postalCodes);
            postalCodes = tmp;
            postalCodes.remove("77332");
            user.setAttribute("postal_code", postalCodes);

        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("bwilson", appRealm);
            List<String> postalCodes = user.getAttribute("postal_code");
            assertPostalCodes(postalCodes, "88441");
            List<String> tmp = new LinkedList<>();
            tmp.addAll(postalCodes);
            postalCodes = tmp;
            postalCodes.add("77332");
            user.setAttribute("postal_code", postalCodes);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("bwilson", appRealm);
            assertPostalCodes(user.getAttribute("postal_code"), "88441", "77332");
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    private void assertPostalCodes(List<String> postalCodes, String... expectedPostalCodes) {
        if (expectedPostalCodes == null && postalCodes.isEmpty()) {
            return;
        }


        Assert.assertEquals(expectedPostalCodes.length, postalCodes.size());
        for (String expected : expectedPostalCodes) {
            if (!postalCodes.contains(expected)) {
                Assert.fail("postalCode '" + expected + "' not in postalCodes: " + postalCodes);
            }
        }
    }

    @Test
    public void ldapPortalEndToEndTest() {
        // Login as bwilson
        driver.navigate().to(APP_SERVER_BASE_URL + "/ldap-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bwilson", "Password1");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(APP_SERVER_BASE_URL + "/ldap-portal"));
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("bwilson") && pageSource.contains("Bruce"));
        Assert.assertTrue(pageSource.contains("street") && pageSource.contains("Elm 5"));
        Assert.assertTrue(pageSource.contains("postal_code") && pageSource.contains("88441") && pageSource.contains("77332"));

        // Logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(APP_SERVER_BASE_URL + "/auth"))
                .queryParam(OAuth2Constants.REDIRECT_URI, APP_SERVER_BASE_URL + "/ldap-portal").build("test").toString();
        driver.navigate().to(logoutUri);

        // Login as jbrown
        driver.navigate().to(APP_SERVER_BASE_URL + "/ldap-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("jbrown", "Password1");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(APP_SERVER_BASE_URL + "/ldap-portal"));
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("jbrown") && pageSource.contains("James Brown"));
        Assert.assertFalse(pageSource.contains("street"));
        Assert.assertTrue(pageSource.contains("postal_code") && pageSource.contains("88441"));
        Assert.assertFalse(pageSource.contains("77332"));

        // Logout
        driver.navigate().to(logoutUri);
    }



}


