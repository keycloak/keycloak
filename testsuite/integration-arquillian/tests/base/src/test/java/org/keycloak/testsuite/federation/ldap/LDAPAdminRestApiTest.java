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
 *
 */

package org.keycloak.testsuite.federation.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.federation.kerberos.KerberosFederationProvider;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.setUserProfileConfiguration;
import static org.keycloak.util.JsonSerialization.writeValueAsString;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPAdminRestApiTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

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
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");
        });

        UPConfig cfg = testRealm().users().userProfile().getConfiguration();
        cfg.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        testRealm().users().userProfile().update(cfg);
    }

    @Test
    public void createUserWithAdminRest() throws Exception {
        // Create user just with the username
        UserRepresentation user1 = UserBuilder.create()
                .username("admintestuser1")
                .password("userpass")
                .enabled(true)
                .build();
        String newUserId1 = createUserExpectSuccess(user1);
        getCleanup().addUserId(newUserId1);

        // Create user with firstName and lastNAme
        UserRepresentation user2 = UserBuilder.create()
                .username("admintestuser2")
                .password("userpass")
                .email("admintestuser2@keycloak.org")
                .firstName("Some")
                .lastName("OtherUser")
                .enabled(true)
                .build();
        String newUserId2 = createUserExpectSuccess(user2);
        getCleanup().addUserId(newUserId2);

        // Create user with filled LDAP_ID should fail
        UserRepresentation user3 = UserBuilder.create()
                .username("admintestuser3")
                .password("userpass")
                .addAttribute(LDAPConstants.LDAP_ID, "123456")
                .enabled(true)
                .build();
        createUserExpectError(user3);

        // Create user with filled LDAP_ENTRY_DN should fail
        UserRepresentation user4 = UserBuilder.create()
                .username("admintestuser4")
                .password("userpass")
                .addAttribute(LDAPConstants.LDAP_ENTRY_DN, "ou=users,dc=foo")
                .enabled(true)
                .build();
        createUserExpectError(user4);
    }

    @Test
    public void updateUserWithAdminRest() throws Exception {
        UserResource userRes = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
        UserRepresentation user = userRes.toRepresentation();

        List<String> origLdapId = new ArrayList<>(user.getAttributes().get(LDAPConstants.LDAP_ID));
        List<String> origLdapEntryDn = new ArrayList<>(user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN));
        Assert.assertEquals(1, origLdapId.size());
        Assert.assertEquals(1, origLdapEntryDn.size());
        assertThat(user.getAttributes().keySet(), not(contains(KerberosFederationProvider.KERBEROS_PRINCIPAL)));

        // Trying to add KERBEROS_PRINCIPAL should fail (Adding attribute, which was not yet present)
        user.setFirstName("JohnUpdated");
        user.setLastName("DoeUpdated");
        user.singleAttribute(KerberosFederationProvider.KERBEROS_PRINCIPAL, "foo");
        updateUserExpectError(userRes, user);

        // The same test, but consider case sensitivity
        user.getAttributes().remove(KerberosFederationProvider.KERBEROS_PRINCIPAL);
        user.singleAttribute("KERberos_principal", "foo");
        updateUserExpectError(userRes, user);

        // Trying to update LDAP_ID should fail (Updating existing attribute, which was present)
        user.getAttributes().remove("KERberos_principal");
        user.getAttributes().get(LDAPConstants.LDAP_ID).remove(0);
        user.getAttributes().get(LDAPConstants.LDAP_ID).add("123");
        updateUserExpectError(userRes, user);

        // Trying to delete LDAP_ID should fail (Removing attribute, which was present here already)
        user.getAttributes().get(LDAPConstants.LDAP_ID).remove(0);
        updateUserExpectError(userRes, user);

        user.getAttributes().remove(LDAPConstants.LDAP_ID);
        userRes.update(user);
        user = userRes.toRepresentation();
        assertEquals(origLdapId, user.getAttributes().get(LDAPConstants.LDAP_ID));

        // Trying to update LDAP_ENTRY_DN should fail
        user.getAttributes().put(LDAPConstants.LDAP_ID, origLdapId);
        user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN).remove(0);
        user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN).add("ou=foo,dc=bar");
        updateUserExpectError(userRes, user);

        // Update firstName and lastName should be fine
        user.getAttributes().put(LDAPConstants.LDAP_ENTRY_DN, origLdapEntryDn);
        userRes.update(user);

        user = userRes.toRepresentation();
        assertEquals("JohnUpdated", user.getFirstName());
        assertEquals("DoeUpdated", user.getLastName());
        assertEquals(origLdapId, user.getAttributes().get(LDAPConstants.LDAP_ID));
        assertEquals(origLdapEntryDn, user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN));

        // Revert
        user.setFirstName("John");
        user.setLastName("Doe");
        userRes.update(user);
    }


    private String createUserExpectSuccess(UserRepresentation user) {
        Response response = testRealm().users().create(user);
        String newUserId = ApiUtil.getCreatedId(response);
        response.close();

        UserRepresentation userRep = testRealm().users().get(newUserId).toRepresentation();
        userRep.getAttributes().containsKey(LDAPConstants.LDAP_ID);
        userRep.getAttributes().containsKey(LDAPConstants.LDAP_ENTRY_DN);
        return newUserId;
    }

    private void createUserExpectError(UserRepresentation user) {
        Response response = testRealm().users().create(user);
        Assert.assertEquals(400, response.getStatus());
        response.close();
    }

    private void updateUserExpectError(UserResource userRes, UserRepresentation user) {
        try {
            userRes.update(user);
            Assert.fail("Not expected to successfully update user");
        } catch (BadRequestException e) {
            // Expected
        }
    }

    @Test
    public void testErrorResponseWhenLdapIsFailing() {
        // Create user just with the username
        UserRepresentation user1 = UserBuilder.create()
                .username("admintestuser1")
                .password("userpass")
                .enabled(true)
                .build();
        String newUserId1 = createUserExpectSuccess(user1);
        getCleanup().addUserId(newUserId1);

        String realmId = testRealm().toRepresentation().getId();
        List<ComponentRepresentation> storageProviders = testRealm().components().query(realmId, UserStorageProvider.class.getName());
        ComponentRepresentation ldapProvider = storageProviders.get(0);
        List<String> originalUrl = ldapProvider.getConfig().get(LDAPConstants.CONNECTION_URL);

        getCleanup().addCleanup(() -> {
            ldapProvider.getConfig().put(LDAPConstants.CONNECTION_URL, originalUrl);
            testRealm().components().component(ldapProvider.getId()).update(ldapProvider);
        });

        ldapProvider.getConfig().put(LDAPConstants.CONNECTION_URL, List.of("ldap://invalid"));
        testRealm().components().component(ldapProvider.getId()).update(ldapProvider);

        List<UserRepresentation> search = testRealm().users().search("*", -1, -1, true);
        assertThat(search.isEmpty(), is(false));
        user1 = search.stream().filter(u -> u.getUsername().equals("admintestuser1")).findFirst().orElseThrow();
        assertThat(user1.getAttributes().containsKey(LDAPConstants.LDAP_ID), is(true));
        assertThat(user1.isEnabled(), is(false));

        UserResource userResource = testRealm().users().get(newUserId1);

        try {
            user1.setFirstName(user1.getFirstName() + " updated");
            userResource.update(user1);
            Assert.fail("Not expected to successfully update user");
        } catch (WebApplicationException expected) {
            Response response = expected.getResponse();
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertTrue(error.getErrorMessage().contains("The user is read-only. The user storage provider 'test-ldap' is currently unavailable. Check the server logs for more details."));
        }

        // fix the LDAP connection configuration and try again to update the user
        storageProviders = testRealm().components().query(realmId, UserStorageProvider.class.getName());
        ComponentRepresentation ldapProviderValid = storageProviders.get(0);
        ldapProviderValid.getConfig().put(LDAPConstants.CONNECTION_URL, originalUrl);
        testRealm().components().component(ldapProviderValid.getId()).update(ldapProviderValid);
        user1 = userResource.toRepresentation();
        user1.setLastName("changed");
        userResource.update(user1);
        user1 = userResource.toRepresentation();
        assertTrue(user1.isEnabled());
        assertEquals("changed", user1.getLastName());
    }

    @Test
    public void testUpdateReadOnlyAttributeWhenNotSetToUser() throws Exception {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        enableSyncRegistration(realmRep, Boolean.FALSE);

        UserRepresentation newUser = UserBuilder.create()
                .username("admintestuser1")
                .password("userpass")
                .addAttribute("foo", "foo-value")
                .enabled(true)
                .build();

        UPConfig origUpConfig = testRealm().users().userProfile().getConfiguration();

        try (Response response = testRealm().users().create(newUser)) {
            enableDynamicUserProfileConfig();
            String newUserId = ApiUtil.getCreatedId(response);

            getCleanup().addUserId(newUserId);

            UserResource user = testRealm().users().get(newUserId);
            UserRepresentation userRep = user.toRepresentation();
            assertNull(userRep.getAttributes());

            userRep.singleAttribute(LDAPConstants.LDAP_ID, "");
            user.update(userRep);
            userRep = testRealm().users().get(newUserId).toRepresentation();
            assertNull(userRep.getAttributes());
            userRep.singleAttribute(LDAPConstants.LDAP_ID, null);
            user.update(userRep);
            userRep = testRealm().users().get(newUserId).toRepresentation();
            assertNull(userRep.getAttributes());

            try {
                userRep.singleAttribute(LDAPConstants.LDAP_ID, "should-fail");
                user.update(userRep);
                fail("Should fail, attribute is read-only");
            } catch (BadRequestException ignore) {
            }
        } finally {
            enableSyncRegistration(realmRep, Boolean.TRUE);
            testRealm().users().userProfile().update(origUpConfig);
        }
    }

    private void enableDynamicUserProfileConfig() throws IOException {
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(null);

        UPAttribute attribute = new UPAttribute();

        attribute.setName(LDAPConstants.LDAP_ID);

        UPAttributePermissions permissions = new UPAttributePermissions();

        permissions.setView(Collections.singleton("admin"));

        attribute.setPermissions(permissions);

        upConfig.addOrReplaceAttribute(attribute);

        setUserProfileConfiguration(testRealm(), writeValueAsString(upConfig));
    }

    private void enableSyncRegistration(RealmRepresentation realmRep, Boolean aFalse) {
        ComponentRepresentation ldapStorage = testRealm().components()
                .query(realmRep.getId(), UserStorageProvider.class.getName()).get(0);
        ldapStorage.getConfig().put(LDAPConstants.SYNC_REGISTRATIONS, Collections.singletonList(aFalse.toString()));
        testRealm().components().component(ldapStorage.getId()).update(ldapStorage);
    }
}
