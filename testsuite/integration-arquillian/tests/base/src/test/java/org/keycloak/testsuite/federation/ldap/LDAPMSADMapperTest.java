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

import java.util.Collections;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.msad.MSADUserAccountControlStorageMapper;
import org.keycloak.storage.ldap.mappers.msad.MSADUserAccountControlStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.msad.UserAccountControl;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPMSADMapperTest extends AbstractLDAPTest {

    // Run this test just on MSAD
    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule()

            .assumeTrue((LDAPTestConfiguration ldapConfig) -> {
                String vendor = ldapConfig.getLDAPConfig().get(LDAPConstants.VENDOR);
                return LDAPConstants.VENDOR_ACTIVE_DIRECTORY.equals(vendor);
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

            LDAPTestUtils.addLocalUser(session, appRealm, "marykeycloak", "mary@test.com", "password-app");

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ctx.getLdapModel());

            // Delete all LDAP users and add some new for testing
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
        });
    }


    @Page
    protected LoginPasswordUpdatePage passwordUpdatePage;


    // TODO: This is skipped as it requires that MSAD server is set to not allow weak passwords (There needs to be pwdProperties=1 set on MSAD side).
    // TODO: Currently we can't rely on it. See KEYCLOAK-4276
    @Ignore
    @Test
    public void test01RegisterUserWithWeakPasswordFirst() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // Weak password. This will fail to update password to MSAD due to password policy.
        registerPage.register("firstName", "lastName", "email2@check.cz", "registerUserSuccess2", "password", "password");

        // Another weak password
        passwordUpdatePage.assertCurrent();
        passwordUpdatePage.changePassword("pass", "pass");
        Assert.assertEquals("Invalid password: new password doesn't match password policies.", passwordUpdatePage.getError());

        // Strong password. Successfully update password and being redirected to the app
        passwordUpdatePage.changePassword("Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel user = session.users().getUserByUsername(appRealm, "registerUserSuccess2");
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ctx.getLdapModel().getId());
            Assert.assertEquals("registerusersuccess2", user.getUsername());
            Assert.assertEquals("firstName", user.getFirstName());
            Assert.assertEquals("lastName", user.getLastName());
            Assert.assertTrue(user.isEnabled());
            Assert.assertEquals(0, user.getRequiredActionsStream().count());
        });
    }


    @Test
    public void test02UpdatePasswordTest() {
        // Add required action to user johnkeycloak through Keycloak admin API
        UserResource john = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "johnkeycloak");
        UserRepresentation johnRep = john.toRepresentation();
        johnRep.setRequiredActions(Collections.singletonList(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
        john.update(johnRep);

        // Check in LDAP, that johnkeycloak has pwdLastSet set to 0 in LDAP
        Assert.assertEquals(0, getPwdLastSetOfJohn());

        // Login as johnkeycloak and update password after login
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");

        passwordUpdatePage.assertCurrent();
        passwordUpdatePage.changePassword("Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Check in LDAP, that johnkeycloak does not have pwdLastSet set to 0
        assertThat(getPwdLastSetOfJohn(), Matchers.greaterThan(0L));

        // Check in admin REST API, that johnkeycloak does not have required action on him
        johnRep = john.toRepresentation();
        Assert.assertTrue(johnRep.getRequiredActions().isEmpty());

        // Logout and login again. There should not be a need to update required action anymore
        john.logout();
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }


    // KEYCLOAK-19039
    @Test
    public void test03UpdatePasswordWithLDAPDirectly() {
        // Add required action to user johnkeycloak through Keycloak admin API
        UserResource john = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "johnkeycloak");
        UserRepresentation johnRep = john.toRepresentation();
        johnRep.setRequiredActions(Collections.singletonList(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
        john.update(johnRep);

        // Check in LDAP, that johnkeycloak has pwdLastSet set to 0 in LDAP
        Assert.assertEquals(0, getPwdLastSetOfJohn());

        // Update password directly in MSAD
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPObject ldapJohn = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), ldapJohn, "Password1");
        });

        // Check in LDAP, that johnkeycloak does not have pwdLastSet set to 0
        assertThat(getPwdLastSetOfJohn(), Matchers.greaterThan(0L));

        // Check in admin REST API, that johnkeycloak does not have required action on him
        johnRep = john.toRepresentation();
        Assert.assertTrue(johnRep.getRequiredActions().isEmpty());

        // Logout and login again. There should not be a need to update required action anymore
        john.logout();
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }


    @Test
    public void test04UpdateLDAPDirectlyToSetUpdatePassword() {
        // Add required action to user johnkeycloak through Keycloak admin API
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPObject ldapJohn = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");

            ldapJohn.removeReadOnlyAttributeName(LDAPConstants.PWD_LAST_SET);
            ldapJohn.setSingleAttribute(LDAPConstants.PWD_LAST_SET, "0");
            ctx.getLdapProvider().getLdapIdentityStore().update(ldapJohn);
        });

        // Check in LDAP, that johnkeycloak has pwdLastSet set to 0 in LDAP
        Assert.assertEquals(0, getPwdLastSetOfJohn());

        // Check Admin REST API contains UPDATE_PASSWORD required action
        UserResource john = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "johnkeycloak");
        UserRepresentation johnRep = john.toRepresentation();
        Assert.assertEquals(UserModel.RequiredAction.UPDATE_PASSWORD.name(), johnRep.getRequiredActions().get(0));

        // Login as johnkeycloak and update password after login
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");

        passwordUpdatePage.assertCurrent();
        passwordUpdatePage.changePassword("Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Check in LDAP, that johnkeycloak does not have pwdLastSet set to 0
        assertThat(getPwdLastSetOfJohn(), Matchers.greaterThan(0L));

        // Check in admin REST API, that johnkeycloak does not have required action on him
        johnRep = john.toRepresentation();
        Assert.assertTrue(johnRep.getRequiredActions().isEmpty());

        // Logout and login again. There should not be a need to update required action anymore
        john.logout();
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }


    @Test
    public void test05UpdatePasswordUnsyncedMode() {
        // Switch edit mode to UNSYNCED
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.toString());
            appRealm.updateComponent(ctx.getLdapModel());
        });

        // Add required action to user johnkeycloak through Keycloak admin API. Due UNSYNCED mode, this should update Keycloak DB, but not MSAD
        UserResource john = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "johnkeycloak");
        UserRepresentation johnRep = john.toRepresentation();
        johnRep.setRequiredActions(Collections.singletonList(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
        john.update(johnRep);

        // Check in LDAP, that johnkeycloak has pwdLastSet set attribute set in MSAD to bigger value than 0. Previous update of requiredAction did not update LDAP
        long pwdLastSetFromLDAP = getPwdLastSetOfJohn();
        assertThat(pwdLastSetFromLDAP, Matchers.greaterThan(0L));

        // Login as johnkeycloak and update password after login
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");

        passwordUpdatePage.assertCurrent();
        passwordUpdatePage.changePassword("Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Check in LDAP, that pwdLastSet attribute of MSAD user johnkeycloak did not change in MSAD
        Assert.assertEquals(pwdLastSetFromLDAP, getPwdLastSetOfJohn());

        // Check in admin REST API, that johnkeycloak does not have required action on him
        johnRep = john.toRepresentation();
        Assert.assertTrue(johnRep.getRequiredActions().isEmpty());

        // Logout and login again. There should not be a need to update required action anymore
        john.logout();
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Switch edit mode back to WRITABLE
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }


    @Test
    public void test06RegisterNewUser() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // Register user
        registerPage.register("firstName", "lastName", "email3@check.cz", "registeruser3", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Check user enabled in MSAD
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            LDAPObject ldapJohn = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");

            String pwdLastSet = ldapJohn.getAttributeAsString(LDAPConstants.PWD_LAST_SET);
            Assert.assertTrue(Long.parseLong(pwdLastSet) > 0);

            String userAccountControl = ldapJohn.getAttributeAsString(LDAPConstants.USER_ACCOUNT_CONTROL);
            Assert.assertFalse(UserAccountControl.of(userAccountControl).has(UserAccountControl.ACCOUNTDISABLE));
        });

        // Logout and login again. Success
        ApiUtil.findUserByUsernameId(adminClient.realm("test"), "registeruser3").logout();
        loginPage.open();
        loginPage.login("registeruser3", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }


    @Test
    public void test07DisabledUserInMSADSwitchedToEnabledInKeycloak() {
        // Disable user in MSAD
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPObject ldapJohn = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");

            String userAccountControlStr = ldapJohn.getAttributeAsString(LDAPConstants.USER_ACCOUNT_CONTROL);
            UserAccountControl control = UserAccountControl.of(userAccountControlStr);
            control.add(UserAccountControl.ACCOUNTDISABLE);

            ldapJohn.setSingleAttribute(LDAPConstants.USER_ACCOUNT_CONTROL, String.valueOf(control.getValue()));
            ctx.getLdapProvider().getLdapIdentityStore().update(ldapJohn);
        });

        // Check user disabled in both admin REST API and MSAD
        UserResource john = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "johnkeycloak");
        UserRepresentation johnRep = john.toRepresentation();
        Assert.assertFalse(johnRep.isEnabled());

        Assert.assertFalse(isJohnEnabledInMSAD());

        // Login as johnkeycloak, but user disabled
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals("Account is disabled, contact your administrator.", loginPage.getError());

        // Enable user in admin REST API
        johnRep.setEnabled(true);
        john.update(johnRep);

        // Assert user enabled also in MSAD
        Assert.assertTrue(isJohnEnabledInMSAD());

        // Logout and login again. There should not be a need to update required action anymore
        john.logout();
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }


    @Test
    public void test08DisabledUserUnsyncedMode() {
        // Switch edit mode to UNSYNCED
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.toString());
            appRealm.updateComponent(ctx.getLdapModel());

            // change MSAD mapper config "ALWAYS_READ_ENABLED_VALUE_FROM_LDAP" to false, so that local db has priority.
            ComponentModel msadMapperComponent = appRealm.getComponentsStream(ctx.getLdapModel().getId(), LDAPStorageMapper.class.getName())
                .filter(c -> MSADUserAccountControlStorageMapperFactory.PROVIDER_ID.equals(c.getProviderId()))
                    .findFirst().orElse(null);
            if (msadMapperComponent != null) {
                msadMapperComponent.getConfig().putSingle(MSADUserAccountControlStorageMapper.ALWAYS_READ_ENABLED_VALUE_FROM_LDAP, "false");
                appRealm.updateComponent(msadMapperComponent);
            }
        });

        // Disable user johnkeycloak through Keycloak admin API. Due UNSYNCED mode, this should update Keycloak DB, but not MSAD
        UserResource john = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "johnkeycloak");
        UserRepresentation johnRep = john.toRepresentation();
        johnRep.setEnabled(false);
        john.update(johnRep);

        // Check in LDAP, that johnkeycloak is still enabled in MSAD
        Assert.assertTrue(isJohnEnabledInMSAD());

        // Login as johnkeycloak and see the user is disabled
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals("Account is disabled, contact your administrator.", loginPage.getError());

        // Enable johnkeycloak in admin REST API
        johnRep = john.toRepresentation();
        johnRep.setEnabled(true);
        john.update(johnRep);

        // Check in LDAP, that johnkeycloak is still enabled in MSAD
        Assert.assertTrue(isJohnEnabledInMSAD());

        // Login again. User should be enabled
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Switch edit mode back to WRITABLE
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            appRealm.updateComponent(ctx.getLdapModel());

            // reset MSAD mapper config "ALWAYS_READ_ENABLED_VALUE_FROM_LDAP" to true.
            ComponentModel msadMapperComponent = appRealm.getComponentsStream(ctx.getLdapModel().getId(), LDAPStorageMapper.class.getName())
                    .filter(c -> MSADUserAccountControlStorageMapperFactory.PROVIDER_ID.equals(c.getProviderId()))
                    .findFirst().orElse(null);
            if (msadMapperComponent != null) {
                msadMapperComponent.getConfig().putSingle(MSADUserAccountControlStorageMapper.ALWAYS_READ_ENABLED_VALUE_FROM_LDAP, "true");
                appRealm.updateComponent(msadMapperComponent);
            }

        });
    }

    @Test
    public void test09DisableUserImportDisabled() {
        testingClient.server().run(session -> {
            // set import enabled to false - in this case only attributes known to LDAP (via one of the mappers) are written
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            ctx.getLdapModel().getConfig().putSingle(UserStorageProviderModel.IMPORT_ENABLED, "false");
            appRealm.updateComponent(ctx.getLdapModel());
        });

        // check user is enabled both locally and on MSAD.
        UserResource john = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "johnkeycloak");
        UserRepresentation johnRep = john.toRepresentation();
        Assert.assertTrue(johnRep.isEnabled());
        Assert.assertTrue(isJohnEnabledInMSAD());

        // disable user johnkeycloak - it should disable both locally and on MSAD.
        johnRep.setEnabled(false);
        john.update(johnRep);

        // Login as johnkeycloak and see the user is disabled.
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals("Account is disabled, contact your administrator.", loginPage.getError());

        // check user is disabled in all places.
        johnRep = john.toRepresentation();
        Assert.assertFalse(johnRep.isEnabled());
        Assert.assertFalse(isJohnEnabledInMSAD());

        // restore john to enabled state.
        johnRep.setEnabled(true);
        john.update(johnRep);

        // Login again. User should be enabled.
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        testingClient.server().run(session -> {
            // restore import enabled mode in the storage provider.
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            ctx.getLdapModel().getConfig().putSingle(UserStorageProviderModel.IMPORT_ENABLED, "true");
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }

    @Test
    public void test10DisabledUserSwitchedToEnabledOnMSAD() {
        // disable user johnkeycloak via REST API - should be disabled in MSAD as well.
        UserResource john = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "johnkeycloak");
        UserRepresentation johnRep = john.toRepresentation();
        johnRep.setEnabled(false);
        john.update(johnRep);

        Assert.assertFalse(isJohnEnabledInMSAD());

        // Login as johnkeycloak and see the user is disabled.
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals("Account is disabled, contact your administrator.", loginPage.getError());

        // enable user johnkeycloak in MSAD only
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPObject ldapJohn = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");
            String userAccountControlStr = ldapJohn.getAttributeAsString(LDAPConstants.USER_ACCOUNT_CONTROL);
            UserAccountControl control = UserAccountControl.of(userAccountControlStr);
            control.remove(UserAccountControl.ACCOUNTDISABLE);
            ldapJohn.setSingleAttribute(LDAPConstants.USER_ACCOUNT_CONTROL, String.valueOf(control.getValue()));
            ctx.getLdapProvider().getLdapIdentityStore().update(ldapJohn);
        });

        Assert.assertTrue(isJohnEnabledInMSAD());

        // Login again. User should be enabled.
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    private long getPwdLastSetOfJohn() {
        String pwdLastSett = testingClient.server().fetchString(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            LDAPObject ldapJohn = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");

            String pwdLastSet = ldapJohn.getAttributeAsString(LDAPConstants.PWD_LAST_SET);
            return pwdLastSet;
        });

        if (pwdLastSett == null) {
            Assert.fail("LDAP user johnkeycloak does not have pwdLastSet on him");
        }

        // Need to remove double quotes TODO: Ideally fix fetchString method and all the tests, which uses it as it is dummy to need to remove quotes in each test individually...
        return Long.parseLong(pwdLastSett.replace("\"",""));
    }


    private boolean isJohnEnabledInMSAD() {
        String userAccountControls = testingClient.server().fetchString(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            LDAPObject ldapJohn = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");

            String userAccountControl = ldapJohn.getAttributeAsString(LDAPConstants.USER_ACCOUNT_CONTROL);
            return userAccountControl;
        });

        if (userAccountControls == null) {
            Assert.fail("LDAP user johnkeycloak does not have userAccountControl attribute on him");
        }

        // Need to remove double quotes TODO: Ideally fix fetchString method and all the tests, which uses it as it is dummy to need to remove quotes in each test individually...
        UserAccountControl acControl = UserAccountControl.of(userAccountControls.replace("\"",""));
        return !acControl.has(UserAccountControl.ACCOUNTDISABLE);
    }
}
