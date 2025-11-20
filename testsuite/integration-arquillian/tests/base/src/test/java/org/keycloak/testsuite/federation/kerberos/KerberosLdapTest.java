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

package org.keycloak.testsuite.federation.kerberos;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.storage.managers.UserStorageSyncManager;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.KerberosEmbeddedServer;
import org.keycloak.testsuite.federation.ldap.LDAPTestAsserts;
import org.keycloak.testsuite.federation.ldap.LDAPTestContext;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.TestAppHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.common.constants.KerberosConstants.KERBEROS_PRINCIPAL;

/**
 * Test for the LDAPStorageProvider with kerberos enabled (kerberos with LDAP integration)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosLdapTest extends AbstractKerberosSingleRealmTest {
    private static final String PROVIDER_CONFIG_LOCATION = "classpath:kerberos/kerberos-ldap-connection.properties";

    @ClassRule
    public static KerberosRule kerberosRule = new KerberosRule(PROVIDER_CONFIG_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM);

    @Override
    protected KerberosRule getKerberosRule() {
        return kerberosRule;
    }


    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new LDAPProviderKerberosConfig(getUserStorageConfiguration());
    }

    @Override
    protected ComponentRepresentation getUserStorageConfiguration() {
        return getUserStorageConfiguration("kerberos-ldap", LDAPStorageProviderFactory.PROVIDER_NAME);
    }

    @Test
    public void spnegoLoginTest() throws Exception {
        assertSuccessfulSpnegoLogin("hnelson", "hnelson", "secret");

        // Assert user was imported and hasn't any required action on him. Profile info is synced from LDAP
        assertUser("hnelson", "hnelson@keycloak.org", "Horatio", "Nelson", "hnelson@KEYCLOAK.ORG", false);
    }

    @Test
    public void changeKerberosPrincipalWhenUserChangesInLDAPTest() throws Exception {
        ContainerAssume.assumeNotAuthServerQuarkus();

        try {
            AccessTokenResponse accessTokenResponse = assertSuccessfulSpnegoLogin("hnelson", "hnelson", "secret");

            // Assert user was imported
            assertUser("hnelson", "hnelson@keycloak.org", "Horatio", "Nelson", "hnelson@KEYCLOAK.ORG", false);

            appPage.logout(accessTokenResponse.getIdToken());

            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel testRealm = ctx.getRealm();

                ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
                UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();

                renameUserInLDAP(ctx, testRealm, "hnelson", "hnelson2", "hnelson2@keycloak.org", "hnelson2@KEYCLOAK.ORG", "secret2");

                // Assert still old users in local provider
                LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), testRealm, "hnelson", "Horatio", "Nelson", "hnelson@keycloak.org", null);

                // Trigger sync
                KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
                SynchronizationResult syncResult = usersSyncManager.syncAllUsers(sessionFactory, testRealm.getId(), ctx.getLdapModel());
                Assert.assertEquals(0, syncResult.getFailed());
            });

            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel testRealm = ctx.getRealm();
                UserProvider userProvider = UserStoragePrivateUtil.userLocalStorage(session);
                // Assert users updated in local provider
                LDAPTestAsserts.assertUserImported(session.users(), testRealm, "hnelson2", "Horatio", "Nelson", "hnelson2@keycloak.org", null);
                UserModel updatedLocalUser = userProvider.getUserByUsername(testRealm, "hnelson2");
                LDAPObject ldapUser = ctx.getLdapProvider().loadLDAPUserByUsername(testRealm, "hnelson2");
                Assert.assertNull(userProvider.getUserByUsername(testRealm, "hnelson"));
                // Assert UUID didn't change
                Assert.assertEquals(updatedLocalUser.getAttributeStream(LDAPConstants.LDAP_ID).findFirst().get(), ldapUser.getUuid());
                // Assert Kerberos principal was changed in Keycloak
                Assert.assertEquals(updatedLocalUser.getAttributeStream(KERBEROS_PRINCIPAL).findFirst().get(), ldapUser.getAttributeAsString(ctx.getLdapProvider().getKerberosConfig().getKerberosPrincipalAttribute()));
            });

            // login not possible with old user
            loginPage.open();
            loginPage.login("hnelson", "secret2");
            Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

            // login after update successful
            assertSuccessfulSpnegoLogin("hnelson2", "hnelson2", "secret2");
        } finally {
            // revert changes in LDAP
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel testRealm = ctx.getRealm();

                renameUserInLDAP(ctx, testRealm, "hnelson2", "hnelson", "hnelson@keycloak.org", "hnelson@KEYCLOAK.ORG", "secret");
            });
        }
    }

    private static void renameUserInLDAP(LDAPTestContext ctx, RealmModel testRealm, String username, String newUsername, String newEmail, String newKr5Principal, String secret) {
        // Update user in LDAP, change username, email, krb5Principal
        LDAPObject ldapUser = ctx.getLdapProvider().loadLDAPUserByUsername(testRealm, username);

        if (ldapUser != null) {
            ldapUser.removeReadOnlyAttributeName("uid");
            ldapUser.removeReadOnlyAttributeName("mail");
            ldapUser.removeReadOnlyAttributeName(ctx.getLdapProvider().getKerberosConfig().getKerberosPrincipalAttribute());
            String userNameLdapAttributeName = ctx.getLdapProvider().getLdapIdentityStore().getConfig().getUsernameLdapAttribute();
            ldapUser.setSingleAttribute(userNameLdapAttributeName, newUsername);
            ldapUser.setSingleAttribute(LDAPConstants.EMAIL, newEmail);
            ldapUser.setSingleAttribute(ctx.getLdapProvider().getKerberosConfig().getKerberosPrincipalAttribute(), newKr5Principal);
            ctx.getLdapProvider().getLdapIdentityStore().update(ldapUser);

            // update also password in LDAP to force propagation into KDC
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), ldapUser, secret);
        }
    }

    @Test
    public void validatePasswordPolicyTest() throws Exception{
         updateProviderEditMode(UserStorageProvider.EditMode.WRITABLE);

         loginPage.open();
         loginPage.login("jduke", "theduke");

         updateProviderValidatePasswordPolicy(true);

         Assert.assertFalse(AccountHelper.updatePassword(testRealmResource(), "jduke", "jduke"));

         updateProviderValidatePasswordPolicy(false);
         Assert.assertTrue(AccountHelper.updatePassword(testRealmResource(), "jduke", "jduke"));

         // Change password back
         Assert.assertTrue(AccountHelper.updatePassword(testRealmResource(), "jduke", "theduke"));
    }

    @Test
    public void writableEditModeTest() throws Exception {
        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);

        // Change editMode to WRITABLE
        updateProviderEditMode(UserStorageProvider.EditMode.WRITABLE);

        // Successfully change password now
        Assert.assertTrue(AccountHelper.updatePassword(testRealmResource(), "jduke", "newPass"));

        // Only needed if you are providing a click thru to bypass kerberos.  Currently there is a javascript
        // to forward the user if kerberos isn't enabled.
        //bypassPage.isCurrent();
        //bypassPage.clickContinue();

        // Login with old password doesn't work, but with new password works

        Assert.assertFalse(testAppHelper.login("jduke", "theduke"));
        Assert.assertTrue(testAppHelper.login("jduke", "newPass"));

        // Assert SPNEGO login with the new password as mode is writable
        events.clear();
        Response spnegoResponse = spnegoLogin("jduke", "newPass");
        org.keycloak.testsuite.Assert.assertEquals(302, spnegoResponse.getStatus());
        org.keycloak.testsuite.Assert.assertEquals(302, spnegoResponse.getStatus());
        List<UserRepresentation> users = testRealmResource().users().search("jduke", 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client("kerberos-app")
                .user(userId)
                .detail(Details.USERNAME, "jduke")
                .assertEvent();

        String codeUrl = spnegoResponse.getLocation().toString();

        assertAuthenticationSuccess(codeUrl);

        // Change password back
        Assert.assertTrue(AccountHelper.updatePassword(testRealmResource(), "jduke", "theduke"));
    }
}
