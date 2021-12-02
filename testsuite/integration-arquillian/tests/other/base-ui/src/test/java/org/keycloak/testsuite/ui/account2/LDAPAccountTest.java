/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.*;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.ldap.LDAPTestContext;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;
import org.keycloak.testsuite.ui.account2.page.SigningInPage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.ClassRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author Alfredo Moises Boullosa <aboullos@redhat.com>
 */
public class LDAPAccountTest extends AbstractAccountTest {

    @Page
    private SigningInPage signingInPage;

    @Page
    private PersonalInfoPage personalInfoPage;

    private SigningInPage.CredentialType passwordCredentialType;
    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Before
    public void beforeSigningInTest() {
        passwordCredentialType = signingInPage.getCredentialType(PasswordCredentialModel.TYPE);

        testingClient.testing().ldap(TEST).createLDAPProvider(ldapRule.getConfig(), true);
        log.infof("LDAP Provider created");

        String userName = "johnkeycloak";
        String firstName = "Jonh";
        String lastName = "Doe";
        String email = "john@email.org";

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, userName, firstName, lastName, email, null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, PASSWORD);
        });

        testRealmLoginPage.setAuthRealm(testRealmPage);
        testRealmAccountPage.setAuthRealm(testRealmPage);

        testUser = createUserRepresentation(userName, email, firstName, lastName, true);
        setPasswordFor(testUser, PASSWORD);

        resetTestRealmSession();
    }

    @Test
    public void createdNotVisibleTest() {
        signingInPage.navigateTo();
        loginPage.form().login(testUser);

        SigningInPage.UserCredential userCredential = passwordCredentialType.getUserCredential("password");

        assertTrue("ROW is not present", userCredential.isPresent());
        assertFalse("Created at is present", userCredential.hasCreatedAt());
    }

    // KEYCLOAK-15634
    @Test
    public void updateProfileWithAttributePresent() {

        RealmResource testRealm = adminClient.realm("test");
        assertEquals(getAccountThemeName(), testRealm.toRepresentation().getAccountTheme());

        UserRepresentation userRepBefore = ApiUtil.findUserByUsername(testRealm,"keycloak-15634");
        assertNull("User should not exist", userRepBefore);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            ldapFedProvider.getModel().put(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.toString());
            appRealm.updateComponent(ldapFedProvider.getModel());

            LDAPObject testUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(),
                    appRealm, "keycloak-15634",
                    "firstName",
                    "lastName",
                    "keycloak-15634@test.local",
                    null,
                    "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), testUser, PASSWORD);
        });

        // Check our test user is ok before updating profile
        userRepBefore = ApiUtil.findUserByUsername(testRealm,"keycloak-15634");
        assertEquals("Test user should have an email address set", "keycloak-15634@test.local", userRepBefore.getEmail());
        assertTrue("Test user should have the LDAP_ID attribute set", userRepBefore.getAttributes().containsKey("LDAP_ID"));
        assertFalse("Test user should not have locale attribute set", userRepBefore.getAttributes().containsKey("locale"));

        personalInfoPage.navigateTo();
        loginPage.assertCurrent();
        loginPage.form().login("keycloak-15634","password");
        personalInfoPage.assertCurrent();
        assertEquals("keycloak-15634@test.local", personalInfoPage.getEmail());

        // Trigger the JS involved in KEYCLOAK-15634
        personalInfoPage.setEmail("keycloak-15634@domain.local");
        personalInfoPage.clickSave();

        // Check if updateProfile went well and if user is still there
        UserRepresentation userRepAfter = ApiUtil.findUserByUsername(testRealm,"keycloak-15634");
        assertNotNull("Test user should still be there", userRepAfter);
        assertEquals("Email should have been updated","keycloak-15634@domain.local", userRepAfter.getEmail());
        assertTrue("LDAP_ID attribute should still be there", userRepAfter.getAttributes().containsKey("LDAP_ID"));

        // Clean up
        ApiUtil.removeUserByUsername(testRealm, "keycloak-15634");
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);
        });
    }
}
