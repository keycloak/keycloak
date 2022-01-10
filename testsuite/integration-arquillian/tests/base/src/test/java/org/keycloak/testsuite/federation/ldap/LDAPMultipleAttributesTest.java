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

package org.keycloak.testsuite.federation.ldap;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.ClientModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.representations.IDToken;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPMultipleAttributesTest extends AbstractLDAPTest {


    // Skip this test on MSAD due to lack of supported user multivalued attributes
    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule()
            .assumeTrue((LDAPTestConfiguration ldapConfig) -> {

                String vendor = ldapConfig.getLDAPConfig().get(LDAPConstants.VENDOR);
                return !LDAPConstants.VENDOR_ACTIVE_DIRECTORY.equals(vendor);

            });

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }


    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ctx.getLdapModel());
            LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "streetMapper", "street", LDAPConstants.STREET);

            // Remove current users and add default users
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            LDAPObject james = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "jbrown", "James", "Brown", "jbrown@keycloak.org", null, "88441");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, james, "Password1");

            // User for testing duplicating surname and postalCode
            LDAPObject bruce = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "bwilson", "Bruce", "Wilson", "bwilson@keycloak.org", "Elm 5", "88441", "77332");
            bruce.setAttribute("sn", new LinkedHashSet<>(Arrays.asList("Wilson", "Schneider")));
            ldapFedProvider.getLdapIdentityStore().update(bruce);
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, bruce, "Password1");

            // Create ldap-portal client
            ClientModel ldapClient = appRealm.addClient("ldap-portal");
            ldapClient.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            ldapClient.addRedirectUri("/ldap-portal");
            ldapClient.addRedirectUri("/ldap-portal/*");
            ldapClient.setManagementUrl("/ldap-portal");
            ldapClient.addProtocolMapper(UserAttributeMapper.createClaimMapper("postalCode", "postal_code", "postal_code", "String", true, true, true));
            ldapClient.addProtocolMapper(UserAttributeMapper.createClaimMapper("street", "street", "street", "String", true, true, false));
            ldapClient.addScopeMapping(appRealm.getRole("user"));
            ldapClient.setSecret("password");
        });
    }


    @Test
    public void testUserImport() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            session.userCache().clear();
            RealmModel appRealm = ctx.getRealm();

            // Test user imported in local storage now
            UserModel user = session.users().getUserByUsername(appRealm, "jbrown");
            Assert.assertNotNull(session.userLocalStorage().getUserById(appRealm, user.getId()));
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), appRealm, "jbrown", "James", "Brown", "jbrown@keycloak.org", "88441");
        });
    }


    @Test
    public void testModel() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            session.userCache().clear();
            RealmModel appRealm = ctx.getRealm();

            UserModel user = session.users().getUserByUsername(appRealm, "bwilson");
            Assert.assertEquals("bwilson@keycloak.org", user.getEmail());
            Assert.assertEquals("Bruce", user.getFirstName());

            // There are 2 lastnames in ldif
            Assert.assertTrue("Wilson".equals(user.getLastName()) || "Schneider".equals(user.getLastName()));

            // Actually there are 2 postalCodes
            List<String> postalCodes = user.getAttributeStream("postal_code").collect(Collectors.toList());
            assertPostalCodes(postalCodes, "88441", "77332");
            List<String> tmp = new LinkedList<>();
            tmp.addAll(postalCodes);
            postalCodes = tmp;
            postalCodes.remove("77332");
            user.setAttribute("postal_code", postalCodes);

        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel user = session.users().getUserByUsername(appRealm, "bwilson");
            List<String> postalCodes = user.getAttributeStream("postal_code").collect(Collectors.toList());
            assertPostalCodes(postalCodes, "88441");
            List<String> tmp = new LinkedList<>();
            tmp.addAll(postalCodes);
            postalCodes = tmp;
            postalCodes.add("77332");
            user.setAttribute("postal_code", postalCodes);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel user = session.users().getUserByUsername(appRealm, "bwilson");
            assertPostalCodes(user.getAttributeStream("postal_code").collect(Collectors.toList()), "88441", "77332");
        });
    }

    private static void assertPostalCodes(List<String> postalCodes, String... expectedPostalCodes) {
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
        oauth.clientId("ldap-portal");
        oauth.redirectUri(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/ldap-portal");

        loginPage.open();
        loginPage.login("bwilson", "Password1");

        String code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        Assert.assertEquals("Bruce Wilson", idToken.getName());
        Assert.assertEquals("Elm 5", idToken.getOtherClaims().get("street"));
        Collection postalCodes = (Collection) idToken.getOtherClaims().get("postal_code");
        Assert.assertEquals(2, postalCodes.size());
        Assert.assertTrue(postalCodes.contains("88441"));
        Assert.assertTrue(postalCodes.contains("77332"));

        oauth.doLogout(response.getRefreshToken(), "password");

        // Login as jbrown
        loginPage.open();
        loginPage.login("jbrown", "Password1");

        code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();
        response = oauth.doAccessTokenRequest(code, "password");

        org.keycloak.testsuite.Assert.assertEquals(200, response.getStatusCode());
        idToken = oauth.verifyIDToken(response.getIdToken());

        Assert.assertEquals("James Brown", idToken.getName());
        Assert.assertNull(idToken.getOtherClaims().get("street"));
        postalCodes = (Collection) idToken.getOtherClaims().get("postal_code");
        Assert.assertEquals(1, postalCodes.size());
        Assert.assertTrue(postalCodes.contains("88441"));
        Assert.assertFalse(postalCodes.contains("77332"));

        oauth.doLogout(response.getRefreshToken(), "password");
    }



}


