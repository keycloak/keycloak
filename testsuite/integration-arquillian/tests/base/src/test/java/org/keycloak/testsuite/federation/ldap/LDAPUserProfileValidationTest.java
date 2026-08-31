package org.keycloak.testsuite.federation.ldap;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

public class LDAPUserProfileValidationTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        // Empty - AbstractLDAPTest requires an implementation, but this test class needs no extra realm setup
    }

    @Test
    public void testImportUserFailsUserProfileValidation() {
        final String TEST_USERNAME = "invalidprofileuse<em>r";

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, TEST_USERNAME, "Invalid", "EmailUser", "invalid-email-format", null, "4578");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            LDAPStorageProvider provider = ctx.getLdapProvider();
            ComponentModel ldapModel = provider.getModel();

            ldapModel.getConfig().putSingle("validateUserProfile", "true");
            appRealm.updateComponent(ldapModel);

            // The test realm fixture allows editing the username; disable that here so a bad username is genuinely
            // something the user could never fix themselves, and must result in a hard rejection of the import.
            appRealm.setEditUsernameAllowed(false);

            UserModel loadedUser = provider.getUserByUsername(appRealm, TEST_USERNAME);

            Assertions.assertNull(loadedUser, "The user should have been stopped by the User Profile validation and null returned.");

            UserModel dbUser = session.users().getUserByUsername(appRealm, TEST_USERNAME);
            Assertions.assertNull(dbUser, "The user should not exist in the local database.");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            LDAPStorageProvider provider = ctx.getLdapProvider();
            LDAPTestUtils.removeAllLDAPUsers(provider, appRealm);

            ComponentModel ldapModel = provider.getModel();
            ldapModel.getConfig().putSingle("validateUserProfile", "false");
            appRealm.updateComponent(ldapModel);
            appRealm.setEditUsernameAllowed(true);
        });
    }

    @Test
    public void testImportUserWithEditableAttributeFailureGetsRequiredAction() {
        final String TEST_USERNAME = "editableattrfailureuser";

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            // Username and email are valid; only firstName violates the User Profile ("person-name-prohibited-characters")
            LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, TEST_USERNAME, "Invalid<b>Name", "ValidLastName", "editable-attr-failure@example.org", null, "4579");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            LDAPStorageProvider provider = ctx.getLdapProvider();
            ComponentModel ldapModel = provider.getModel();

            ldapModel.getConfig().putSingle("validateUserProfile", "true");
            appRealm.updateComponent(ldapModel);

            UserModel loadedUser = provider.getUserByUsername(appRealm, TEST_USERNAME);

            Assertions.assertNotNull(loadedUser, "The user should have been imported since firstName can be fixed by the user themselves.");
            Assertions.assertTrue(loadedUser.getRequiredActionsStream().anyMatch(UserModel.RequiredAction.UPDATE_PROFILE.name()::equals),
                    "The user should have been given the UPDATE_PROFILE required action.");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            LDAPStorageProvider provider = ctx.getLdapProvider();
            LDAPTestUtils.removeAllLDAPUsers(provider, appRealm);

            ComponentModel ldapModel = provider.getModel();
            ldapModel.getConfig().putSingle("validateUserProfile", "false");
            appRealm.updateComponent(ldapModel);
        });
    }

    @Test
    public void testFullSyncCountsValidationFailureAsFailed() {
        final String TEST_USERNAME = "invalidsyncuser<b>";

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            LDAPStorageProvider provider = ctx.getLdapProvider();
            ComponentModel ldapModel = provider.getModel();
            ldapModel.getConfig().putSingle("validateUserProfile", "true");
            appRealm.updateComponent(ldapModel);

            // See testImportUserFailsUserProfileValidation: disabled so the bad username is genuinely unfixable
            // by the user themselves, exercising the hard-rejection path during sync.
            appRealm.setEditUsernameAllowed(false);

            LDAPTestUtils.addLDAPUser(provider, appRealm, TEST_USERNAME, "Valid", "User", "valid-sync-user@example.org", null, "4580");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            SynchronizationResult result = UserStoragePrivateUtil.runFullSync(session.getKeycloakSessionFactory(), ctx.getLdapModel());

            Assertions.assertEquals(1, result.getFailed(), "The sync result should report the rejected user as failed, not as added.");
            Assertions.assertEquals(0, result.getAdded(), "The rejected user should not be counted as added.");
            // Sync operates on the local storage provider directly, so bypass the user cache here too (see
            // LDAPSyncTest for the same pattern) rather than risk reading a stale cached entry.
            Assertions.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, TEST_USERNAME),
                    "The user should not exist in the local database.");
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            LDAPStorageProvider provider = ctx.getLdapProvider();
            LDAPTestUtils.removeAllLDAPUsers(provider, appRealm);

            ComponentModel ldapModel = provider.getModel();
            ldapModel.getConfig().putSingle("validateUserProfile", "false");
            appRealm.updateComponent(ldapModel);
            appRealm.setEditUsernameAllowed(true);
        });
    }
}
