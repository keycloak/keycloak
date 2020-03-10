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
import org.junit.*;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.*;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.federation.ldap.LDAPTestContext;
import org.keycloak.testsuite.ui.account2.page.SigningInPage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author Alfredo Moises Boullosa <aboullos@redhat.com>
 */
public class LDAPAccountTest extends AbstractAccountTest {

    @Page
    private SigningInPage signingInPage;

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

        Assert.assertTrue("ROW is not present", userCredential.isPresent());
        Assert.assertFalse("Created at is present", userCredential.hasCreatedAt());
    }
}
