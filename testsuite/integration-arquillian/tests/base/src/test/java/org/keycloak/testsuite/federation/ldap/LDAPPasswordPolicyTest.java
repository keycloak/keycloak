/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.federation.ldap;

import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPRule.LDAPConnectionParameters;
import org.keycloak.testsuite.util.LDAPRule.LDAPPasswordPolicy;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class LDAPPasswordPolicyTest extends AbstractLDAPTest {

    @Rule
    // Start an embedded LDAP server with configuration derived from test annotations before each test.
    public LDAPRule ldapRule = new LDAPRule()
        .assumeTrue((LDAPTestConfiguration ldapConfig) -> {
            return ldapConfig.isStartEmbeddedLdapServer();
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

            LDAPObject user = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "mustchange", "John", "Doe",
                    "john_old@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), user, "Password1");
        });

        ldapRule.getLdapEmbeddedServer().setPwdReset("uid=mustchange,ou=People,dc=keycloak,dc=org", true);
    }

    @Test
    @LDAPPasswordPolicy(mustChange=true)
    public void testForcedPasswordChangeAfterReset() throws Exception {
        // Login with user that has to change password.
        oauth.openLoginForm();
        loginPage.login("mustchange", "Password1");

        // Forced password change sends user to update password page.
        passwordUpdatePage.assertCurrent();

        // Repeated login without changing password should still send user to update password page.
        oauth.openLoginForm();
        loginPage.login("mustchange", "Password1");
        passwordUpdatePage.assertCurrent();

        // Change password and verify that user can login with new password.
        passwordUpdatePage.changePassword("changedpassword", "changedpassword");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = managedRealm.admin().users().search("mustchange").get(0);
        managedRealm.admin().users().get(user.getId()).logout();
        oauth.openLoginForm();
        loginPage.login("mustchange", "changedpassword");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    @LDAPConnectionParameters(bindType = LDAPConnectionParameters.BindType.NONE, encryption = LDAPConnectionParameters.Encryption.NONE)
    public void testHistoryPasswordPolicyConsidersFederatedPassword() {
        // The embedded LDAP server starts fresh for each test, so create the user in this test.
        // The realm provider binds as a non-admin user (the mustChange workaround in LDAPRule overrides the bind DN
        // for this class), so this test's server must run without access control for the provider to be able to write.
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPObject historyUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "historyuser", "History",
                    "Policy", "historyuser@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), historyUser, "OldPassword1");
        });

        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        String originalPasswordPolicy = realmRep.getPasswordPolicy();
        ComponentRepresentation ldapProvider = managedRealm.admin().components().query(realmRep.getId(),
                UserStorageProvider.class.getName()).get(0);

        try {
            // The LDAP provider validates Keycloak password policies on password updates when configured to do so.
            ldapProvider.getConfig().put(LDAPConstants.VALIDATE_PASSWORD_POLICY, List.of("true"));
            managedRealm.admin().components().component(ldapProvider.getId()).update(ldapProvider);

            realmRep.setPasswordPolicy("passwordHistory(1)");
            managedRealm.admin().update(realmRep);

            // The federation lookup imports the user from LDAP; their password is held by LDAP, not by Keycloak.
            UserRepresentation user = managedRealm.admin().users().search("historyuser").stream()
                    .filter(u -> u.getUsername().equals("historyuser")).findFirst().orElseThrow();
            UserResource userResource = managedRealm.admin().users().get(user.getId());

            // Reusing the current federated password is rejected by the history policy.
            try {
                userResource.resetPassword(passwordCredential("OldPassword1"));
                Assertions.fail("Reusing the current LDAP password is expected to be rejected by the history policy");
            } catch (BadRequestException expected) {
            }

            // A different password is accepted.
            userResource.resetPassword(passwordCredential("NewPassword2"));

            // The storage provider exposes only the current password, it has no password history,
            // so the previous federated password can be used again.
            userResource.resetPassword(passwordCredential("OldPassword1"));
        } finally {
            realmRep.setPasswordPolicy(originalPasswordPolicy);
            managedRealm.admin().update(realmRep);
            ldapProvider.getConfig().put(LDAPConstants.VALIDATE_PASSWORD_POLICY, List.of("false"));
            managedRealm.admin().components().component(ldapProvider.getId()).update(ldapProvider);
        }
    }

    private CredentialRepresentation passwordCredential(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        return credential;
    }

}
