/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.federation.ldap;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.PrioritizedComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapperFactory;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.userprofile.config.UPConfigUtils;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;
import static org.keycloak.userprofile.UserProfileUtil.USER_METADATA_GROUP;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPUserProfileTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Page
    protected LoginUpdateProfilePage updateProfilePage;

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, "test-ldap");
            RealmModel appRealm = ctx.getRealm();

            UserModel user = LDAPTestUtils.addLocalUser(session, appRealm, "marykeycloak", "mary@test.com", "Password1");
            user.setFirstName("Mary");
            user.setLastName("Kelly");

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ctx.getLdapModel());

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");

            LDAPObject john2 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "johnkeycloak2", "John", "Doe", "john2@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john2, "Password1");
        });
    }

    @Test
    public void testUserProfile() {
        // Test user profile of user johnkeycloak in admin API
        UserResource johnResource = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
        UserRepresentation john = johnResource.toRepresentation(true);

        assertUser(john, "johnkeycloak", "john@email.org", "John", "Doe", "1234");
        assertProfileAttributes(john, null, false, "username", "email", "firstName", "lastName", "postal_code");
        assertProfileAttributes(john, USER_METADATA_GROUP, true,  LDAPConstants.LDAP_ID, LDAPConstants.LDAP_ENTRY_DN);

        // Test Update profile
        john.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PROFILE.toString());
        johnResource.update(john);

        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        updateProfilePage.assertCurrent();
        Assert.assertEquals("John", updateProfilePage.getFirstName());
        Assert.assertEquals("Doe", updateProfilePage.getLastName());
        Assert.assertTrue(updateProfilePage.getElementById("firstName").isEnabled());
        Assert.assertTrue(updateProfilePage.getElementById("lastName").isEnabled());
        Assert.assertNull(updateProfilePage.getElementById("postal_code"));
        updateProfilePage.prepareUpdate().submit();
    }

    @Test
    public void testUserProfileWithDefinedAttribute() throws IOException {
        UPConfig origConfig = testRealm().users().userProfile().getConfiguration();
        try {
            UPConfig config = testRealm().users().userProfile().getConfiguration();
            // Set postal code
            UPAttribute postalCode = new UPAttribute();
            postalCode.setName("postal_code");
            postalCode.setDisplayName("Postal Code");

            UPAttributePermissions permissions = new UPAttributePermissions();
            permissions.setView(Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN));
            permissions.setEdit(Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN));
            postalCode.setPermissions(permissions);
            config.getAttributes().add(postalCode);
            testRealm().users().userProfile().update(config);

            // Defined postal_code in user profile config should have preference
            UserResource johnResource = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
            UserRepresentation john = johnResource.toRepresentation(true);
            Assert.assertEquals("Postal Code", john.getUserProfileMetadata().getAttributeMetadata("postal_code").getDisplayName());

            // update profile now.
            john.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PROFILE.toString());
            johnResource.update(john);

            loginPage.open();
            loginPage.login("johnkeycloak", "Password1");
            updateProfilePage.assertCurrent();

            Assert.assertEquals("John", updateProfilePage.getFirstName());
            Assert.assertEquals("Doe", updateProfilePage.getLastName());
            Assert.assertEquals("1234", updateProfilePage.getElementById("postal_code").getAttribute("value"));
            Assert.assertTrue(updateProfilePage.getElementById("firstName").isEnabled());
            Assert.assertTrue(updateProfilePage.getElementById("lastName").isEnabled());
            Assert.assertTrue(updateProfilePage.getElementById("postal_code").isEnabled());
            updateProfilePage.prepareUpdate().submit();
        } finally {
            testRealm().users().userProfile().update(origConfig);
        }
    }

    @Test
    public void testUserProfileWithReadOnlyLdap() {
        // Test user profile of user johnkeycloak in admin console as well as account console. Check attributes are writable.
        setLDAPReadOnly();
        try {
            UserResource johnResource = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
            UserRepresentation john = johnResource.toRepresentation(true);

            assertProfileAttributes(john, null, true, "username", "email", "firstName", "lastName", "postal_code");
            assertProfileAttributes(john, USER_METADATA_GROUP, true,  LDAPConstants.LDAP_ID, LDAPConstants.LDAP_ENTRY_DN);

            // Test Update profile. Fields are read only
            john.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PROFILE.toString());
            johnResource.update(john);

            loginPage.open();
            loginPage.login("johnkeycloak", "Password1");
            updateProfilePage.assertCurrent();
            Assert.assertEquals("John", updateProfilePage.getFirstName());
            Assert.assertEquals("Doe", updateProfilePage.getLastName());
            Assert.assertFalse(updateProfilePage.getElementById("firstName").isEnabled());
            Assert.assertFalse(updateProfilePage.getElementById("lastName").isEnabled());
            Assert.assertNull(updateProfilePage.getElementById("postal_code"));
            updateProfilePage.prepareUpdate().submit();
        } finally {
            setLDAPWritable();
        }

    }

    @Test
    public void testUserProfileWithReadOnlyLdapLocalUser() {
        // Test local user is writable and has only attributes defined explicitly in user-profile
        setLDAPReadOnly();
        try {
            UserResource maryResource = ApiUtil.findUserByUsernameId(testRealm(), "marykeycloak");
            UserRepresentation mary = maryResource.toRepresentation(true);

            // LDAP is read-only, but local user has all the attributes writable
            assertProfileAttributes(mary, null, false, "username", "email", "firstName", "lastName");
            assertProfileAttributesNotPresent(mary, "postal_code", LDAPConstants.LDAP_ID, LDAPConstants.LDAP_ENTRY_DN);

            // Test Update profile
            mary.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PROFILE.toString());
            maryResource.update(mary);

            loginPage.open();
            loginPage.login("marykeycloak", "Password1");
            updateProfilePage.assertCurrent();
            Assert.assertEquals("Mary", updateProfilePage.getFirstName());
            Assert.assertEquals("Kelly", updateProfilePage.getLastName());
            Assert.assertTrue(updateProfilePage.getElementById("firstName").isEnabled());
            Assert.assertTrue(updateProfilePage.getElementById("lastName").isEnabled());
            Assert.assertNull(updateProfilePage.getElementById("postal_code"));
            updateProfilePage.prepareUpdate().submit();
        } finally {
            setLDAPWritable();
        }
    }

    @Test
    public void testUserProfileWithoutImport() {
        setLDAPImportDisabled();
        UPConfig origConfig = testRealm().users().userProfile().getConfiguration();
        try {
            UPConfig config = testRealm().users().userProfile().getConfiguration();
            // Set postal code
            UPAttribute postalCode = new UPAttribute();
            postalCode.setName("postal_code");
            postalCode.setDisplayName("Postal Code");

            UPAttributePermissions permissions = new UPAttributePermissions();
            permissions.setView(Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN));
            permissions.setEdit(Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN));
            postalCode.setPermissions(permissions);
            config.getAttributes().add(postalCode);
            testRealm().users().userProfile().update(config);
            // Test local user is writable and has only attributes defined explicitly in user-profile
            // Test user profile of user johnkeycloak in admin API
            UserResource johnResource = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak2");
            UserRepresentation john = johnResource.toRepresentation(true);

            assertUser(john, "johnkeycloak2", "john2@email.org", "John", "Doe", "1234");
            assertProfileAttributes(john, null, false, "username", "email", "firstName", "lastName", "postal_code");
            assertProfileAttributes(john, USER_METADATA_GROUP, true, LDAPConstants.LDAP_ID, LDAPConstants.LDAP_ENTRY_DN);
        } finally {
            setLDAPImportEnabled();
            testRealm().users().userProfile().update(origConfig);
        }
    }

    @Test
    public void testMultipleLDAPProviders() {
        testingClient.server().run(session -> {
            RealmModel testRealm = session.realms().getRealmByName(AbstractLDAPTest.TEST_REALM_NAME);
            ComponentModel ldapCompModel = LDAPTestUtils.getLdapProviderModel(testRealm);
            UserStorageProviderModel ldapModel = new UserStorageProviderModel(ldapCompModel);
            ldapModel.setId(null);
            ldapModel.setParentId(null);
            ldapModel.setName("other-ldap");
            ldapModel.put(LDAPConstants.USERS_DN, ldapModel.getConfig().getFirst(LDAPConstants.USERS_DN).replace("People", "OtherPeople"));
            ldapCompModel.put(PrioritizedComponentModel.PRIORITY, "100");
            testRealm.addComponentModel(ldapModel);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            // if AD or RHDS, create new OU in a base DN because users.ldif is ignored for AD/RHDS
            String vendor = ldapModel.getConfig().getFirst(LDAPConstants.VENDOR);
            if (LDAPConstants.VENDOR_ACTIVE_DIRECTORY.equals(vendor)) {
                LDAPTestUtils.addLdapOUinBaseDn(ldapProvider, "OtherPeople2");
                LDAPTestUtils.removeAllLDAPUsers(ldapProvider, testRealm);
            } else if (LDAPConstants.VENDOR_RHDS.equals(vendor)) {
                LDAPTestUtils.addLdapOUinBaseDn(ldapProvider, "OtherPeople");
                LDAPTestUtils.removeAllLDAPUsers(ldapProvider, testRealm);
            }
            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapProvider, testRealm, "anotherjohn", "AnotherJohn", "AnotherDoe", "anotherjohn@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapProvider, john, "Password1");
        });

        // the provider for this user does not have postal_code mapper
        UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), "anotherjohn");
        UserRepresentation userRep = userResource.toRepresentation(true);
        Assert.assertNull(userRep.getAttributes().get("postal_code"));

        // the provider for this user does have postal_code mapper
        userResource = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
        userRep = userResource.toRepresentation(true);
        Assert.assertNotNull(userRep.getAttributes().get("postal_code"));

        setLDAPReadOnly();
        try {
            // the second provider is not readonly
            userResource = ApiUtil.findUserByUsernameId(testRealm(), "anotherjohn");
            userRep = userResource.toRepresentation(true);
            assertProfileAttributes(userRep, null, false, "username", "email", "firstName", "lastName");

            // the original provider is readonly
            userResource = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
            userRep = userResource.toRepresentation(true);
            assertProfileAttributes(userRep, null, true, "username", "email", "firstName", "lastName", "postal_code");

            // the second provider is not readonly
            userResource = ApiUtil.findUserByUsernameId(testRealm(), "anotherjohn");
            userRep = userResource.toRepresentation(true);
            assertProfileAttributes(userRep, null, false, "username", "email", "firstName", "lastName");
        } finally {
          setLDAPWritable();
        }
    }

    @Test
    public void testUsernameRespectFormatFromExternalStore() {
        Assume.assumeFalse("Skip for AD", testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            return LDAPConstants.VENDOR_ACTIVE_DIRECTORY.equals(ctx.getLdapModel().getConfig().getFirst(LDAPConstants.VENDOR));
        }, Boolean.class));

        String upperCaseUsername = "JOHNKEYCLOAK3";
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, "test-ldap");
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().put(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, List.of(LDAPConstants.GIVENNAME));
            ctx.getLdapModel().getConfig().put(LDAPConstants.RDN_LDAP_ATTRIBUTE, List.of(LDAPConstants.GIVENNAME));

            ComponentModel ldapComponentMapper = LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "givename-mapper", "username", LDAPConstants.GIVENNAME);
            ldapComponentMapper.put(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, true);
            appRealm.updateComponent(ldapComponentMapper);

            appRealm.removeComponent(appRealm.getComponentsStream(ctx.getLdapModel().getId())
                    .filter(mapper -> UserAttributeLDAPStorageMapperFactory.PROVIDER_ID.equals(mapper.getProviderId()))
                    .filter((mapper) -> mapper.getName().equals(UserModel.USERNAME))
                    .findAny().orElse(null));

            appRealm.updateComponent(ctx.getLdapModel());

            MultivaluedHashMap<String, String> otherAttrs = new MultivaluedHashMap<>();
            otherAttrs.put(LDAPConstants.GIVENNAME, List.of(upperCaseUsername));

            LDAPObject john3 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, upperCaseUsername, "John", "Doe", "john3@email.org", otherAttrs);
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john3, "Password1");
        });

        UserResource johnResource = ApiUtil.findUserByUsernameId(testRealm(), upperCaseUsername);
        UserRepresentation john = johnResource.toRepresentation(true);
        Assert.assertEquals(upperCaseUsername, john.getUsername());

        johnResource = ApiUtil.findUserByUsernameId(testRealm(), upperCaseUsername.toLowerCase());
        john = johnResource.toRepresentation(true);
        Assert.assertEquals(upperCaseUsername, john.getUsername());

        loginPage.open();
        loginPage.login(upperCaseUsername, "Password1");
        appPage.assertCurrent();
        testRealm().users().get(john.getId()).logout();
        loginPage.open();
        loginPage.login(upperCaseUsername.toLowerCase(), "Password1");
        appPage.assertCurrent();
    }

    @Test
    public void testUsernameRespectFormatFromExternalStoreAD() {
        Assume.assumeTrue("Only run for AD", testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            return LDAPConstants.VENDOR_ACTIVE_DIRECTORY.equals(ctx.getLdapModel().getConfig().getFirst(LDAPConstants.VENDOR));
        }, Boolean.class));

        String upperCaseUsername = "JOHNKEYCLOAK3";
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, "test-ldap");
            RealmModel appRealm = ctx.getRealm();

            ComponentModel ldapComponentMapper = LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "username-cn-mapper", "username", LDAPConstants.CN);
            ldapComponentMapper.put(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, true);
            appRealm.updateComponent(ldapComponentMapper);

            LDAPObject john3 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, upperCaseUsername, "John", "Doe", "john3@email.org", "12345");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john3, "Password1");
        });

        UserResource johnResource = ApiUtil.findUserByUsernameId(testRealm(), upperCaseUsername);
        UserRepresentation john = johnResource.toRepresentation(true);
        Assert.assertEquals(upperCaseUsername, john.getUsername());

        johnResource = ApiUtil.findUserByUsernameId(testRealm(), upperCaseUsername.toLowerCase());
        john = johnResource.toRepresentation(true);
        Assert.assertEquals(upperCaseUsername, john.getUsername());

        loginPage.open();
        loginPage.login(upperCaseUsername, "Password1");
        appPage.assertCurrent();
        testRealm().users().get(john.getId()).logout();
        loginPage.open();
        loginPage.login(upperCaseUsername.toLowerCase(), "Password1");
        appPage.assertCurrent();
    }

    @Test
    public void testUpdateEmailWhenEmailAsUsernameEnabledAndEditUsernameDisabled() {
        String username = "johnkeycloak";
        UserResource johnResource = ApiUtil.findUserByUsernameId(testRealm(), username);
        UserRepresentation john = johnResource.toRepresentation(true);
        String email = "john@email.org";
        assertUser(john, username, email, "John", "Doe", "1234");

        // enable email as username
        RealmRepresentation realm = testRealm().toRepresentation();
        boolean initialEditUserNameAllowed = realm.isEditUsernameAllowed();
        boolean initialEmailUsernameEnabled = realm.isRegistrationEmailAsUsername();
        realm.setEditUsernameAllowed(false);
        realm.setRegistrationEmailAsUsername(true);
        testRealm().update(realm);

        // update the user to force updating the username as the email
        john.setEmail("john@newemail.org");
        johnResource.update(john);
        john = johnResource.toRepresentation(true);
        assertUser(john, "john@newemail.org", "john@newemail.org", "John", "Doe", "1234");
        getCleanup().addCleanup(() -> {
            try {
                realm.setEditUsernameAllowed(initialEditUserNameAllowed);
                realm.setRegistrationEmailAsUsername(initialEmailUsernameEnabled);
                testRealm().update(realm);
                UserRepresentation user = johnResource.toRepresentation(true);
                user.setUsername(username);
                user.setEmail(email);
                johnResource.update(user);
            } finally {
                testRealm().update(realm);
            }

        });
    }

    private void setLDAPReadOnly() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, "test-ldap");
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.toString());
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }

    private void setLDAPWritable() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, "test-ldap");
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }

    private void setLDAPImportDisabled() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, "test-ldap");
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(IMPORT_ENABLED, "false");
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }

    private void setLDAPImportEnabled() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, "test-ldap");
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(IMPORT_ENABLED, "true");
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }

    private void assertUser(UserRepresentation user, String expectedUsername, String expectedEmail, String expectedFirstName, String expectedLastname, String expectedPostalCode) {
        Assert.assertNotNull(user);
        Assert.assertEquals(expectedUsername, user.getUsername());
        Assert.assertEquals(expectedFirstName, user.getFirstName());
        Assert.assertEquals(expectedLastname, user.getLastName());
        Assert.assertEquals(expectedEmail, user.getEmail());
        Assert.assertEquals(expectedPostalCode, user.getAttributes().get("postal_code").get(0));

        Assert.assertNotNull(user.getAttributes().get(LDAPConstants.LDAP_ID));
        Assert.assertNotNull(user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN));
    }


    private void assertProfileAttributes(UserRepresentation user, String expectedGroup, boolean expectReadOnly, String... attributes) {
        for (String attrName : attributes) {
            UserProfileAttributeMetadata attrMetadata = user.getUserProfileMetadata().getAttributeMetadata(attrName);
            Assert.assertNotNull("Attribute " + attrName + " was not present for user " + user.getUsername(), attrMetadata);
            Assert.assertEquals("Attribute " + attrName + " for user " + user.getUsername() + ". Expected read-only: " + expectReadOnly + " but was not", expectReadOnly, attrMetadata.isReadOnly());
            Assert.assertEquals("Attribute " + attrName + " for user " + user.getUsername() + ". Expected group: " + expectedGroup + " but was " + attrMetadata.getGroup(), expectedGroup, attrMetadata.getGroup());
        }
    }

    private void assertProfileAttributesNotPresent(UserRepresentation user, String... attributes) {
        for (String attrName : attributes) {
            UserProfileAttributeMetadata attrMetadata = user.getUserProfileMetadata().getAttributeMetadata(attrName);
            Assert.assertNull("Attribute " + attrName + " was present for user " + user.getUsername(), attrMetadata);
        }
    }

}
