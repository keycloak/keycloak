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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.naming.AuthenticationException;
import javax.naming.directory.SearchControls;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPAttributeMapper;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPAttributeMapperFactory;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPGroupStorageMapper;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPGroupStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPRoleStorageMapper;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPRoleStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPProvidersIntegrationTest extends AbstractLDAPTest {

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
            session.getContext().setRealm(appRealm);

            LDAPTestUtils.addLocalUser(session, appRealm, "marykeycloak", "mary@test.com", "password-app");

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ctx.getLdapModel());

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");

            LDAPObject existing = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "existing", "Existing", "Foo", "existing@email.org", null, "5678");

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);

        });
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    /**
     * KEYCLOAK-3986
     *
     */
    @Test
    public void testSyncRegistrationOff() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().put(LDAPConstants.SYNC_REGISTRATIONS, "false");
            ctx.getRealm().updateComponent(ctx.getLdapModel());
        });

        UserRepresentation newUser1 = AbstractAuthTest.createUserRepresentation("newUser1", "newUser1@email.cz", null, null, true);
        Response resp = testRealm().users().create(newUser1);
        String userId = ApiUtil.getCreatedId(resp);
        resp.close();

        testRealm().users().get(userId).toRepresentation();
        Assert.assertTrue(StorageId.isLocalStorage(userId));
        Assert.assertNull(newUser1.getFederationLink());

        // Revert
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ctx.getRealm().updateComponent(ctx.getLdapModel());
        });
    }

    @Test
    public void testSyncRegistrationWithCreateDNRelativeToBaseDN() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            // use a broader DN to fetch users from - should still fetch all users if search scope is set to subtree
            ctx.getLdapModel().put(LDAPConstants.USERS_DN, "dc=keycloak,dc=org");
            ctx.getLdapModel().put(LDAPConstants.SEARCH_SCOPE, String.valueOf(SearchControls.SUBTREE_SCOPE));
            // use a relative DN to store the users - final DN should be this DN + the users DN set above
            ctx.getLdapModel().put(LDAPConstants.RELATIVE_CREATE_DN, "ou=People");
            ctx.getRealm().updateComponent(ctx.getLdapModel());
        });

        // ensure users are still found when searching using a broader DN with subtree scope
        Integer count = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            return ctx.getLdapProvider().searchForUserStream(ctx.getRealm(), Map.of(), -1, -1).count();
        }, Integer.class);
        MatcherAssert.assertThat(count, Matchers.greaterThan(0));

        // register a new user and check it was added to the right DN
        UserRepresentation newUser = AbstractAuthTest.createUserRepresentation("newuser0", "newuser0@email.com", "New", "User0", true);
        String userId;
        try (Response resp = testRealm().users().create(newUser)) {
            userId = ApiUtil.getCreatedId(resp);
        }
        newUser = testRealm().users().get(userId).toRepresentation();
        assertFederatedUserLink(newUser);
        MatcherAssert.assertThat(newUser, Matchers.notNullValue());
        MatcherAssert.assertThat(newUser.firstAttribute(LDAPConstants.LDAP_ENTRY_DN), Matchers.containsString("=newuser0,ou=People,dc=keycloak,dc=org"));

        // remove the created user
        try (Response resp = testRealm().users().delete(userId)) {
            Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resp.getStatus());
        }

        // revert changes to the LDAP storage provider
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().put(LDAPConstants.USERS_DN, "ou=People,dc=keycloak,dc=org");
            ctx.getLdapModel().getConfig().remove(LDAPConstants.SEARCH_SCOPE);
            ctx.getLdapModel().getConfig().remove(LDAPConstants.RELATIVE_CREATE_DN);
            ctx.getRealm().updateComponent(ctx.getLdapModel());
        });
    }

    @Test
    public void testSyncRegistrationForceDefault() {
        // test force default is true by default and works as before
        UserRepresentation newUser1 = AbstractAuthTest.createUserRepresentation("newuser1", null, null, null, true);
        String userId;
        try (Response resp = testRealm().users().create(newUser1)) {
            userId = ApiUtil.getCreatedId(resp);
        }
        newUser1 = testRealm().users().get(userId).toRepresentation();
        assertFederatedUserLink(newUser1);
        Assert.assertNotNull(newUser1.getAttributes().get(LDAPConstants.LDAP_ID));
        MatcherAssert.assertThat(newUser1.firstAttribute(LDAPConstants.LDAP_ENTRY_DN), Matchers.containsString("=newuser1,"));

        String cnValue = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(ctx.getRealm());
            ComponentModel cnMapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ldapModel, "last name");
            LDAPObject userLdap = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "newuser1");
            return userLdap.getAttributeAsString(cnMapper.get(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE));
        }, String.class);
        Assert.assertEquals("Attribute CN was not set with the forced default value", "", cnValue);

        // remove the created user
        try (Response resp = testRealm().users().delete(userId)) {
            Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resp.getStatus());
        }
        Assert.assertTrue(testRealm().users().search("newuser1").isEmpty());
    }

    private static LDAPObject searchObjectInBase(LDAPStorageProvider ldapProvider, String dn, String... attrs) {
        LDAPQuery q = new LDAPQuery(ldapProvider)
                            .setSearchDn(dn)
                            .setSearchScope(SearchControls.OBJECT_SCOPE);
        if (attrs != null) {
            for (String attr: attrs) {
                q.addReturningLdapAttribute(attr);
            }
        }
        return q.getFirstResult();
    }


    @Test
    public void testSyncRegistrationEmailRDNNoDefault() {
        testingClient.server().run(session -> {
            // configure mail as mandatory but not forcing default and create a hardcoded attribute for description
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(ctx.getRealm());
            ComponentModel emailMapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ldapModel, "email");
            emailMapper.getConfig().putSingle(UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
            emailMapper.getConfig().putSingle(UserAttributeLDAPStorageMapper.FORCE_DEFAULT_VALUE, "false");
            ctx.getRealm().updateComponent(emailMapper);
            ComponentModel hardcodedMapperModel = KeycloakModelUtils.createComponentModel("hardcodedAttr-description", ctx.getLdapModel().getId(), HardcodedLDAPAttributeMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_NAME, "description",
                HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_VALUE, "some-${RANDOM}");
            ctx.getRealm().addComponentModel(hardcodedMapperModel);
        });
        try {
            // test the user cannot be created without email
            UserRepresentation newUser1 = AbstractAuthTest.createUserRepresentation("newuser1", null, "newuser1", "newuser1", true);
            try (Response resp = testRealm().users().create(newUser1)) {
                Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
                OAuth2ErrorRepresentation error = resp.readEntity(OAuth2ErrorRepresentation.class);
                Assert.assertEquals("unknown_error", error.getError());
            }
            Assert.assertTrue(testRealm().users().search("newuser1").isEmpty());

            // test the user is correctly created with email
            newUser1 = AbstractAuthTest.createUserRepresentation("newuser1", "newuser1@keycloak.org", "newuser1", "newuser1", true);
            String userId;
            try (Response resp = testRealm().users().create(newUser1)) {
                userId = ApiUtil.getCreatedId(resp);
            }
            newUser1 = testRealm().users().get(userId).toRepresentation();
            assertFederatedUserLink(newUser1);
            Assert.assertNotNull(newUser1.getAttributes().get(LDAPConstants.LDAP_ID));
            final String userDN = newUser1.firstAttribute(LDAPConstants.LDAP_ENTRY_DN);
            MatcherAssert.assertThat(userDN, Matchers.containsString("=newuser1,"));
            Assert.assertEquals("newuser1@keycloak.org", newUser1.getEmail());
            String emailValueInLdap = testingClient.server().fetch(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(ctx.getRealm());
                ComponentModel emailMapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ldapModel, "email");
                LDAPObject userLdap = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "newuser1");
                return userLdap.getAttributeAsString(emailMapper.get(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE));
            }, String.class);
            Assert.assertEquals("newuser1@keycloak.org", emailValueInLdap);
            // check description is in ldap assigned
            String description = testingClient.server().fetch(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                LDAPObject ldapUser = searchObjectInBase(ctx.getLdapProvider(), userDN, "description");
                return ldapUser.getAttributeAsString("description");
            }, String.class);
            MatcherAssert.assertThat(description, Matchers.startsWith("some-"));

            // remove the created user
            try (Response resp = testRealm().users().delete(userId)) {
                Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resp.getStatus());
            }
            Assert.assertTrue(testRealm().users().search("newuser1").isEmpty());
        } finally {
            testingClient.server().run(session -> {
                // revert
                LDAPTestContext ctx = LDAPTestContext.init(session);
                ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(ctx.getRealm());
                ComponentModel emailMapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ldapModel, "email");
                emailMapper.getConfig().putSingle(UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "false");
                emailMapper.getConfig().remove(UserAttributeLDAPStorageMapper.FORCE_DEFAULT_VALUE);
                ctx.getRealm().updateComponent(emailMapper);
                ComponentModel hardcodedMapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "hardcodedAttr-description");
                ctx.getRealm().removeComponent(hardcodedMapperModel);
            });
        }
    }

    @Test
    public void testSyncRegistrationEmailRDNDefaultValue() {
        testingClient.server().run(session -> {
            // configure mail as mandatory but with a default value
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(ctx.getRealm());
            ComponentModel emailMapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ldapModel, "email");
            emailMapper.getConfig().putSingle(UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
            emailMapper.getConfig().putSingle(UserAttributeLDAPStorageMapper.ATTRIBUTE_DEFAULT_VALUE, "empty@keycloak.org");
            ctx.getRealm().updateComponent(emailMapper);
        });
        try {
            // the user should be created with the default value
            UserRepresentation newUser1 = AbstractAuthTest.createUserRepresentation("newuser1", null, "newuser1", "newuser1", true);
            String userId;
            try (Response resp = testRealm().users().create(newUser1)) {
                userId = ApiUtil.getCreatedId(resp);
            }
            newUser1 = testRealm().users().get(userId).toRepresentation();
            assertFederatedUserLink(newUser1);
            Assert.assertNotNull(newUser1.getAttributes().get(LDAPConstants.LDAP_ID));
            MatcherAssert.assertThat(newUser1.firstAttribute(LDAPConstants.LDAP_ENTRY_DN), Matchers.containsString("=newuser1,"));
            String emailValueInLdap = testingClient.server().fetch(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(ctx.getRealm());
                ComponentModel emailMapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ldapModel, "email");
                LDAPObject userLdap = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "newuser1");
                return userLdap.getAttributeAsString(emailMapper.get(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE));
            }, String.class);
            Assert.assertEquals("empty@keycloak.org", emailValueInLdap);

            // remove the created user
            try (Response resp = testRealm().users().delete(userId)) {
                Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resp.getStatus());
            }
            Assert.assertTrue(testRealm().users().search("newuser1").isEmpty());
        } finally {
            testingClient.server().run(session -> {
                // revert
                LDAPTestContext ctx = LDAPTestContext.init(session);
                ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(ctx.getRealm());
                ComponentModel emailMapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ldapModel, "email");
                emailMapper.getConfig().putSingle(UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "false");
                emailMapper.getConfig().remove(UserAttributeLDAPStorageMapper.ATTRIBUTE_DEFAULT_VALUE);
                ctx.getRealm().updateComponent(emailMapper);
            });
        }
    }

    @Test
    public void testRemoveImportedUsers() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            UserModel user = session.users().getUserByUsername(ctx.getRealm(), "johnkeycloak");
            Assert.assertEquals(ctx.getLdapModel().getId(), user.getFederationLink());
        });

        adminClient.realm("test").userStorage().removeImportedUsers(ldapModelId);

        testingClient.server().run(session -> {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealmByName("test");
            UserModel user = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertNull(user);
        });
    }

    // test name prefixed with zz to make sure it runs last as we are unlinking imported users
    @Test
    public void zzTestUnlinkUsers() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            UserModel user = session.users().getUserByUsername(ctx.getRealm(), "johnkeycloak");
            Assert.assertEquals(ctx.getLdapModel().getId(), user.getFederationLink());
        });

        adminClient.realm("test").userStorage().unlink(ldapModelId);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            UserModel user = session.users().getUserByUsername(ctx.getRealm(), "johnkeycloak");
            Assert.assertNotNull(user);
            Assert.assertNull(user.getFederationLink());
        });
    }

    @Test
    public void caseInSensitiveImport() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPObject jbrown2 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "JBrown2", "John", "Brown2", "jbrown2@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), jbrown2, "Password1");
            LDAPObject jbrown3 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "jbrown3", "John", "Brown3", "JBrown3@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), jbrown3, "Password1");
        });

        loginSuccessAndLogout("jbrown2", "Password1");
        loginSuccessAndLogout("JBrown2", "Password1");
        loginSuccessAndLogout("jbrown2@email.org", "Password1");
        loginSuccessAndLogout("JBrown2@email.org", "Password1");

        loginSuccessAndLogout("jbrown3", "Password1");
        loginSuccessAndLogout("JBrown3", "Password1");
        loginSuccessAndLogout("jbrown3@email.org", "Password1");
        loginSuccessAndLogout("JBrown3@email.org", "Password1");
    }

    private void loginSuccessAndLogout(String username, String password) {
        events.clear();
        loginPage.open();
        loginPage.login(username, password);
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(events.poll());
        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).withRedirect().open();
        events.poll();
    }

    @Test
    public void caseInsensitiveSearch() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            LDAPObject jbrown4 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "JBrown4", "John", "Brown4", "jbrown4@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), jbrown4, "Password1");
            LDAPObject jbrown5 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "jbrown5", "John", "Brown5", "JBrown5@Email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), jbrown5, "Password1");
        });

        // search by username
        List<UserRepresentation> users = testRealm().users().search("JBROwn4", 0, 10);
        UserRepresentation user4 = users.get(0);
        Assert.assertEquals("jbrown4", user4.getUsername());
        Assert.assertEquals("jbrown4@email.org", user4.getEmail());

        // search by email
        users = testRealm().users().search("JBROwn5@eMAil.org", 0, 10);
        Assert.assertEquals(1, users.size());
        UserRepresentation user5 = users.get(0);
        Assert.assertEquals("jbrown5", user5.getUsername());
        Assert.assertEquals("jbrown5@email.org", user5.getEmail());
    }

    @Test
    public void testUsernameAndEmailCaseSensitiveIfImportDisabled() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            UserStorageProviderModel ldapModel = ctx.getLdapProvider().getModel();
            ldapModel.setImportEnabled(false);
            ctx.getRealm().updateComponent(ldapModel);
            LDAPObject ldapObject = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "JBrown8", "John", "Brown8", "JBrown8@Email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), ldapObject, "Password1");
            UserModel model = session.users().searchForUserStream(ctx.getRealm(), Map.of(UserModel.USERNAME, "JBrown8")).findAny().orElse(null);
            Assert.assertNotNull(model);
            assertEquals("JBrown8", model.getUsername());
            assertEquals("JBrown8@Email.org", model.getEmail());
            ldapModel.setImportEnabled(true);
            ctx.getRealm().updateComponent(ldapModel);
        });
    }

    @Test
    public void testUsernameAndEmailCaseInSensitiveIfImportEnabled() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            UserStorageProviderModel ldapModel = ctx.getLdapProvider().getModel();
            ldapModel.setImportEnabled(true);
            ctx.getRealm().updateComponent(ldapModel);
            LDAPObject ldapObject = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "JBrown9", "John", "Brown9", "JBrown9@Email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), ldapObject, "Password1");
            UserModel model = session.users().searchForUserStream(ctx.getRealm(), Map.of(UserModel.USERNAME, "JBrown9")).findAny().orElse(null);
            Assert.assertNotNull(model);
            assertEquals("jbrown9", model.getUsername());
            assertEquals("jbrown9@email.org", model.getEmail());
        });
    }

    @Test
    public void deleteFederationLink() throws Exception {
        // KEYCLOAK-4789: Login in client, which requires consent
        oauth.clientId("third-party");
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");

        grantPage.assertCurrent();
        grantPage.accept();

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        ComponentRepresentation ldapRep = testRealm().components().component(ldapModelId).toRepresentation();
        testRealm().components().component(ldapModelId).remove();

        // User not available once LDAP provider was removed
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        // Re-add LDAP provider
        Map<String, String> cfg = getLDAPRule().getConfig();
        ldapModelId = testingClient.testing().ldap(TEST_REALM_NAME).createLDAPProvider(cfg, isImportEnabled());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            LDAPTestUtils.addZipCodeLDAPMapper(ctx.getRealm(), ctx.getLdapModel());
        });

        oauth.clientId("test-app");

        loginLdap();

    }

    @Test
    public void loginClassic() {
        loginPage.open();
        loginPage.login("marykeycloak", "password-app");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

    }

    @Test
    public void loginLdap() {
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(TEST_REALM_NAME), "johnkeycloak");

        Assert.assertEquals("John", userRepresentation.getFirstName());
        Assert.assertEquals("Doe", userRepresentation.getLastName());
        Assert.assertEquals("john@email.org", userRepresentation.getEmail());
    }

    @Test
    public void loginLdapWithDirectGrant() throws Exception {
        AccessTokenResponse response = oauth.doPasswordGrantRequest("johnkeycloak", "Password1");
        Assert.assertEquals(200, response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

        response = oauth.doPasswordGrantRequest("johnkeycloak", "");
        Assert.assertEquals(401, response.getStatusCode());
    }

    @Test
    public void loginLdapWithEmail() {
        loginPage.open();
        loginPage.login("john@email.org", "Password1");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());
    }

    @Test
    public void loginLdapWithoutPassword() {
        loginPage.open();
        loginPage.login("john@email.org", "");
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());
    }

    @Test
    public void ldapPasswordChangeWithAccountConsole() throws Exception {
        Assert.assertTrue(AccountHelper.updatePassword(testRealm(), "johnkeycloak", "New-password1"));

        loginPage.open();
        loginPage.login("johnkeycloak", "Bad-password1");
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        loginPage.open();
        loginPage.login("johnkeycloak", "New-password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Change password back to previous value
        Assert.assertTrue(AccountHelper.updatePassword(testRealm(), "johnkeycloak", "Password1"));
    }


    // KEYCLOAK-12340
    @Test
    public void ldapPasswordChangeWithAdminEndpointAndRequiredAction() throws Exception {
        String username = "admin-endpoint-req-act";
        String email = username + "@email.cz";

        // Register new LDAP user with password, logout user
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", email,
            username, "Password1", "Password1");


        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), username);
        String userId = user.toRepresentation().getId();

        events.expectRegister(username, email).assertEvent();
        EventRepresentation loginEvent = events.expectLogin().user(userId).assertEvent();
        AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        appPage.logout(tokenResponse.getIdToken());
        events.expectLogout(loginEvent.getSessionId()).user(userId).assertEvent();

        // Test admin endpoint. Assert federated endpoint returns password in LDAP "supportedCredentials", but there is no stored password
        assertPasswordConfiguredThroughLDAPOnly(user);

        // Update password through admin REST endpoint. Assert user can authenticate with the new password
        ApiUtil.resetUserPassword(user, "Password1-updated1", false);

        loginPage.open();

        loginSuccessAndLogout(username, "Password1-updated1");

        // Test admin endpoint. Assert federated endpoint returns password in LDAP "supportedCredentials", but there is no stored password
        assertPasswordConfiguredThroughLDAPOnly(user);

        // Test this just for the import mode. No-import mode doesn't support requiredActions right now
        if (isImportEnabled()) {
            // Update password through required action.
            UserRepresentation user2 = user.toRepresentation();
            user2.setRequiredActions(Arrays.asList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));
            user.update(user2);

            loginPage.open();
            loginPage.login(username, "Password1-updated1");
            requiredActionChangePasswordPage.assertCurrent();

            requiredActionChangePasswordPage.changePassword("Password1-updated2", "Password1-updated2");

            appPage.assertCurrent();
            events.expect(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).assertEvent();
            events.expect(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).assertEvent();
            loginEvent = events.expectLogin().user(userId).assertEvent();
            tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
            appPage.logout(tokenResponse.getIdToken());
            events.expectLogout(loginEvent.getSessionId()).user(userId);

            // Assert user can authenticate with the new password
            loginSuccessAndLogout(username, "Password1-updated2");

            // Test admin endpoint. Assert federated endpoint returns password in LDAP "supportedCredentials", but there is no stored password
            assertPasswordConfiguredThroughLDAPOnly(user);
        }
    }


    // Use admin REST endpoints
    private void assertPasswordConfiguredThroughLDAPOnly(UserResource user) {
        // Assert password not stored locally
        List<CredentialRepresentation> storedCredentials = user.credentials();
        assertEquals(1, storedCredentials.size());
        for (CredentialRepresentation credential : storedCredentials) {
            Assert.assertTrue(PasswordCredentialModel.TYPE.equals(credential.getType()));
            Assert.assertNotNull(credential.getFederationLink());
        }

        // Assert password is stored in the LDAP
        List<String> userStorageCredentials = user.getConfiguredUserStorageCredentialTypes();
        Assert.assertTrue(userStorageCredentials.contains(PasswordCredentialModel.TYPE));
    }

    @Test
    public void registerExistingLdapUser() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // check existing username
        registerPage.register("firstName", "lastName", "email@mail.cz", "existing", "Password1", "Password1");
        registerPage.assertCurrent();
        Assert.assertEquals("Username already exists.", registerPage.getInputAccountErrors().getUsernameError());

        // Check existing email
        registerPage.register("firstName", "lastName", "existing@email.org", "nonExisting", "Password1", "Password1");
        registerPage.assertCurrent();
        Assert.assertEquals("Email already exists.", registerPage.getInputAccountErrors().getEmailError());
    }



    //
    // KEYCLOAK-4533
    //
    @Test
    public void testLDAPUserDeletionImport() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPConfig config = ctx.getLdapProvider().getLdapIdentityStore().getConfig();

            // Make sure mary is gone
            LDAPTestUtils.removeLDAPUserByUsername(ctx.getLdapProvider(), ctx.getRealm(), config, "maryjane");

            // Create the user in LDAP and register him

            LDAPObject mary = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "maryjane", "mary", "yram", "mj@testing.redhat.cz", null, "12398");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), mary, "Password1");

        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPConfig config = ctx.getLdapProvider().getLdapIdentityStore().getConfig();

            // Delete LDAP User
            LDAPTestUtils.removeLDAPUserByUsername(ctx.getLdapProvider(), ctx.getRealm(), config, "maryjane");

            // Make sure the deletion took place.
            Assert.assertEquals(0, session.users().searchForUserStream(ctx.getRealm(), Map.of(UserModel.SEARCH, "mary yram")).count());
        });
    }


    @Test
    public void registerUserLdapSuccess() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "email2@check.cz", "register-user-success2", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(),"register-user-success2");
        Assert.assertNotNull(user);
        assertFederatedUserLink(user);
        Assert.assertEquals("register-user-success2", user.getUsername());
        Assert.assertEquals("firstName", user.getFirstName());
        Assert.assertEquals("lastName", user.getLastName());
        Assert.assertTrue(user.isEnabled());
    }


    protected void assertFederatedUserLink(UserRepresentation user) {
        Assert.assertTrue(StorageId.isLocalStorage(user.getId()));
        Assert.assertNotNull(user.getFederationLink());
        Assert.assertEquals(user.getFederationLink(), ldapModelId);
    }


    @Test
    public void testCaseSensitiveAttributeName() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject johnZip = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnzip", "John", "Zip", "johnzip@email.org", null, "12398");

            // Remove default zipcode mapper and add the mapper for "POstalCode" to test case sensitivity
            ComponentModel currentZipMapper = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "zipCodeMapper");
            appRealm.removeComponent(currentZipMapper);
            LDAPTestUtils.addUserAttributeMapper(appRealm, ldapModel, "zipCodeMapper-cs", "postal_code", "POstalCode");

            // Fetch user from LDAP and check that postalCode is filled
            UserModel user = session.users().getUserByUsername(appRealm, "johnzip");
            String postalCode = user.getFirstAttribute("postal_code");
            Assert.assertEquals("12398", postalCode);
        });

        // modify postal_code in the user
        RealmResource realm = testRealm();
        List<UserRepresentation> users = realm.users().search("johnzip", true);
        Assert.assertEquals("User not found", 1, users.size());
        UserRepresentation user = users.iterator().next();
        Assert.assertEquals("Incorrect postal code", Collections.singletonList("12398"), user.getAttributes().get("postal_code"));
        UserResource userRes = realm.users().get(user.getId());
        user.getAttributes().put("postal_code", Collections.singletonList("9876"));
        userRes.update(user);
        user = userRes.toRepresentation();
        Assert.assertEquals("Incorrect postal code", Collections.singletonList("9876"), user.getAttributes().get("postal_code"));

        // ensure the ldap contains the correct value
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPStorageProvider ldapProvider = ctx.getLdapProvider();
            LDAPObject ldapUser = ldapProvider.loadLDAPUserByUsername(appRealm, "johnzip");
            Assert.assertEquals("Incorrect postal code", "9876", ldapUser.getAttributeAsString("POstalCode"));
        });
    }

    @Test
    public void testCommaInUsername() {
        Boolean skipTest = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            boolean skip = false;

            // Workaround as comma is not allowed in sAMAccountName on active directory. So we will skip the test for this configuration
            LDAPConfig config = ctx.getLdapProvider().getLdapIdentityStore().getConfig();
            if (config.isActiveDirectory() && config.getUsernameLdapAttribute().equals(LDAPConstants.SAM_ACCOUNT_NAME)) {
                skip = true;
            }

            if (!skip) {
                LDAPObject johnComma = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "john,comma", "John", "Comma", "johncomma@email.org", null, "12387");
                LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), johnComma, "Password1");

                LDAPObject johnPlus = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "john+plus,comma", "John", "Plus", "johnplus@email.org", null, "12387");
                LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), johnPlus, "Password1");
            }

            return skip;

        }, Boolean.class);

        if (!skipTest) {
            // Try to import the user with comma in username into Keycloak
            loginSuccessAndLogout("john,comma", "Password1");
            loginSuccessAndLogout("john+plus,comma", "Password1");
        }
    }


    @Test
    public void testHardcodedAttributeMapperTest() throws Exception {
        // Create hardcoded mapper for "description"
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            ComponentModel hardcodedMapperModel = KeycloakModelUtils.createComponentModel("hardcodedAttr-description", ctx.getLdapModel().getId(), HardcodedLDAPAttributeMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_NAME, "description",
                HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_VALUE, "some-${RANDOM}");
            ctx.getRealm().addComponentModel(hardcodedMapperModel);
        });

        // Register new user
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "email34@check.cz", "register123", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // See that user don't yet have any description
            UserModel user = LDAPTestAsserts.assertUserImported(session.users(), appRealm, "register123", "firstName", "lastName", "email34@check.cz", null);
            Assert.assertNull(user.getFirstAttribute("desc"));
            Assert.assertNull(user.getFirstAttribute("description"));

            // Remove hardcoded mapper for "description" and create regular userAttribute mapper for description
            ComponentModel hardcodedMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "hardcodedAttr-description");
            appRealm.removeComponent(hardcodedMapperModel);

            ComponentModel userAttrMapper = LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "desc-attribute-mapper", "desc", "description");
            userAttrMapper.put(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true");
            appRealm.updateComponent(userAttrMapper);
        });

        // Check that user has description on him now
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserStorageUtil.userCache(session).evict(appRealm, session.users().getUserByUsername(appRealm, "register123"));

            // See that user don't yet have any description
            UserModel user = session.users().getUserByUsername(appRealm, "register123");
            Assert.assertNull(user.getFirstAttribute("description"));
            Assert.assertNotNull(user.getFirstAttribute("desc"));
            String desc = user.getFirstAttribute("desc");
            Assert.assertTrue(desc.startsWith("some-"));
            Assert.assertEquals(35, desc.length());

            // Remove mapper for "description"
            ComponentModel userAttrMapper = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "desc-attribute-mapper");
            appRealm.removeComponent(userAttrMapper);
        });
    }


    @Test
    public void testHardcodedRoleMapper() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            RoleModel hardcodedRole = appRealm.addRole("hardcoded-role");

            // assert that user "johnkeycloak" doesn't have hardcoded role
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertFalse(john.hasRole(hardcodedRole));

            ComponentModel hardcodedMapperModel = KeycloakModelUtils.createComponentModel("hardcoded role", ctx.getLdapModel().getId(),
                HardcodedLDAPRoleStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                HardcodedLDAPRoleStorageMapper.ROLE, "hardcoded-role");
            appRealm.addComponentModel(hardcodedMapperModel);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            RoleModel hardcodedRole = appRealm.getRole("hardcoded-role");

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertTrue(john.hasRole(hardcodedRole));

            // Can't remove user from hardcoded role
            try {
                john.deleteRoleMapping(hardcodedRole);
                Assert.fail("Didn't expected to remove role mapping");
            } catch (ModelException expected) {
            }

            // Revert mappers
            ComponentModel hardcodedMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "hardcoded role");
            appRealm.removeComponent(hardcodedMapperModel);
        });
    }

    @Test
    public void testHardcodedGroupMapper() {
        final String uuid = UUID.randomUUID().toString();
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            GroupModel hardcodedGroup = appRealm.createGroup(uuid, "hardcoded-group");

            // assert that user "johnkeycloak" doesn't have hardcoded group
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertFalse(john.isMemberOf(hardcodedGroup));

            ComponentModel hardcodedMapperModel = KeycloakModelUtils.createComponentModel("hardcoded group",
                ctx.getLdapModel().getId(), HardcodedLDAPGroupStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                HardcodedLDAPGroupStorageMapper.GROUP, "hardcoded-group");
            appRealm.addComponentModel(hardcodedMapperModel);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            GroupModel hardcodedGroup = appRealm.getGroupById(uuid);

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertTrue(john.isMemberOf(hardcodedGroup));

            // Can't remove user from hardcoded role
            try {
                john.leaveGroup(hardcodedGroup);
                Assert.fail("Didn't expected to leave group");
            } catch (ModelException expected) {
            }

            // Revert mappers
            ComponentModel hardcodedMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "hardcoded group");
            appRealm.removeComponent(hardcodedMapperModel);
        });
    }

    @Test
    public void testImportExistingUserFromLDAP() throws Exception {
        // Add LDAP user with same email like existing model user
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "marykeycloak", "Mary1", "Kelly1", "mary1@email.org", null, "123");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "mary-duplicatemail", "Mary2", "Kelly2", "mary@test.com", null, "123");
            LDAPObject marynoemail = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "marynoemail", "Mary1", "Kelly1", null, null, "123");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), marynoemail, "Password1");
        });


        // Try to import the duplicated LDAP user into Keycloak
        loginPage.open();
        loginPage.login("mary-duplicatemail", "password");
        Assert.assertEquals("Email already exists.", loginPage.getError());

        loginPage.login("mary1@email.org", "password");
        Assert.assertEquals("Username already exists.", loginPage.getError());

        loginSuccessAndLogout("marynoemail", "Password1");
    }

    @Test
    public void testReadonly() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.toString());
            appRealm.updateComponent(ctx.getLdapModel());
        });

        UserRepresentation userRep = ApiUtil.findUserByUsername(testRealm(), "johnkeycloak");
        assertFederatedUserLink(userRep);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel user = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertNotNull(user);
            try {
                user.setEmail("error@error.com");
                Assert.fail("should fail");
            } catch (ReadOnlyException e) {

            }
            try {
                user.setLastName("Berk");
                Assert.fail("should fail");
            } catch (ReadOnlyException e) {

            }
            try {
                user.setFirstName("Bilbo");
                Assert.fail("should fail");
            } catch (ReadOnlyException e) {

            }
            try {
                UserCredentialModel cred = UserCredentialModel.password("PoopyPoop1", true);
                user.credentialManager().updateCredential(cred);
                Assert.fail("should fail");
            } catch (ReadOnlyException e) {

            }

            Assert.assertTrue(session.users().removeUser(appRealm, user));
        });

        // Revert
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().put(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            appRealm.updateComponent(ctx.getLdapModel());

            Assert.assertEquals(UserStorageProvider.EditMode.WRITABLE.toString(),
                appRealm.getComponent(ctx.getLdapModel().getId()).getConfig().getFirst(LDAPConstants.EDIT_MODE));
        });
    }

    @Test
    public void testRemoveFederatedUser() {
        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(), "register-user-success2");

        // Case when this test was executed "alone" (User "registerusersuccess2" is registered inside registerUserLdapSuccess)
        if (user == null) {
            registerUserLdapSuccess();
            user = ApiUtil.findUserByUsername(testRealm(), "register-user-success2");
        }

        assertFederatedUserLink(user);
        testRealm().users().get(user.getId()).remove();
        user = ApiUtil.findUserByUsername(testRealm(), "register-user-success2");
        Assert.assertNull(user);
    }

    @Test
    public void testSearch() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username1", "John1", "Doel1", "user1@email.org", null, "121");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username2", "John2", "Doel2", "user2@email.org", null, "122");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username3", "John3", "Doel3", "user3@email.org", null, "123");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username4", "John4", "Doel4", "user4@email.org", null, "124");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username11", "John11", "Doel11", "user11@email.org", null, "124");

            // Users are not at local store at this moment
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "username1"));
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "username2"));
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "username3"));
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "username4"));

            // search by username (we use a terminal operation on the stream to ensure it is consumed)
            Assert.assertEquals(1, session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "\"username1\"")).count());
            LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), appRealm, "username1", "John1", "Doel1", "user1@email.org", "121");

            // search by email (we use a terminal operation on the stream to ensure it is consumed)
            Assert.assertEquals(1, session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "user2@email.org")).count());
            LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), appRealm, "username2", "John2", "Doel2", "user2@email.org", "122");

            // search by lastName (we use a terminal operation on the stream to ensure it is consumed)
            Assert.assertEquals(1, session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "Doel3")).count());
            LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), appRealm, "username3", "John3", "Doel3", "user3@email.org", "123");

            // search by firstName + lastName (we use a terminal operation on the stream to ensure it is consumed)
            Assert.assertEquals(1, session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "John4 Doel4")).count());
            LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), appRealm, "username4", "John4", "Doel4", "user4@email.org", "124");

            // search by a string that matches multiple fields. Should still return the one entity it matches.
            Assert.assertEquals(1, session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "*11*")).count());
            LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), appRealm, "username11", "John11", "Doel11", "user11@email.org", "124");

            // search by a string that has special characters. Should succeed with an empty set, but no exceptions.
            Assert.assertEquals(0, session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH,"John)")).count());
        });
    }

    @Test
    public void testSearchWithCustomLDAPFilter() {
        // Add custom filter for searching users
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(|(mail=user5@email.org)(mail=user6@email.org))");
            appRealm.updateComponent(ctx.getLdapModel());
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username5", "John5", "Doel5", "user5@email.org", null, "125");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username6", "John6", "Doel6", "user6@email.org", null, "126");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username7", "John7", "Doel7", "user7@email.org", null, "127");

            // search by email (we use a terminal operation on the stream to ensure it is consumed)
            session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "user5@email.org")).count();
            LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), appRealm, "username5", "John5", "Doel5", "user5@email.org", "125");

            session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "John6 Doel6")).count();
            LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), appRealm, "username6", "John6", "Doel6", "user6@email.org", "126");

            session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "user7@email.org")).count();
            session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "John7 Doel7")).count();
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "username7"));

            // Remove custom filter
            ctx.getLdapModel().getConfig().remove(LDAPConstants.CUSTOM_USER_SEARCH_FILTER);
            appRealm.updateComponent(ctx.getLdapModel());
        });

        // Get username5 ID. Username5 is covered by the custom filter
        String user5Id = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            // Fetch user from LDAP
            UserModel testedUser = session.users().getUserByUsername(ctx.getRealm(), "username5");
            return testedUser.getId();
        },String.class);

        // Get username7 ID. Username7 is not covered by the custom filter
        String user7Id = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            // Fetch user from LDAP
            UserModel testedUser = session.users().getUserByUsername(ctx.getRealm(), "username7");
            return testedUser.getId();
        },String.class);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);
            UserStorageUtil.userCache(session).clear();
            // Add custom filter again
            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(|(mail=user5@email.org)(mail=user6@email.org))");

            appRealm.updateComponent(ctx.getLdapModel());
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);
            UserStorageUtil.userCache(session).clear();

            // search by id using custom filter. Must return the user
            UserModel testUser5 = session.users().getUserById(appRealm, user5Id);
            Assert.assertNotNull(testUser5);

            // search by id using custom filter. Must not return the user
            UserModel testUser7 = session.users().getUserById(appRealm, user7Id);
            Assert.assertNull(testUser7);

            // Remove custom filter
            ctx.getLdapModel().getConfig().remove(LDAPConstants.CUSTOM_USER_SEARCH_FILTER);
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }

    @Test
    public void testUnsynced() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            UserStorageProviderModel model = new UserStorageProviderModel(ctx.getLdapModel());
            model.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.toString());
            appRealm.updateComponent(model);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            UserModel user = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ctx.getLdapModel().getId());

            UserCredentialModel cred = UserCredentialModel.password("Candycand1", true);
            user.credentialManager().updateCredential(cred);
            CredentialModel userCredentialValueModel = user.credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE)
                .findFirst().orElse(null);
            Assert.assertNotNull(userCredentialValueModel);
            Assert.assertEquals(PasswordCredentialModel.TYPE, userCredentialValueModel.getType());
            Assert.assertTrue(user.credentialManager().isValid(cred));

            // LDAP password is still unchanged
            try {
                LDAPObject ldapUser = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");
                ctx.getLdapProvider().getLdapIdentityStore().validatePassword(ldapUser, "Password1");
            } catch (AuthenticationException ex) {
                throw new RuntimeException(ex);
            }
        });

        // Test admin REST endpoints
        UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");

        // Assert password is stored locally
        List<String> storedCredentials = userResource.credentials().stream()
            .map(CredentialRepresentation::getType)
            .collect(Collectors.toList());
        Assert.assertTrue(storedCredentials.contains(PasswordCredentialModel.TYPE));

        // Assert password is supported in the LDAP too.
        List<String> userStorageCredentials = userResource.getConfiguredUserStorageCredentialTypes();
        Assert.assertTrue(userStorageCredentials.contains(PasswordCredentialModel.TYPE));

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            UserModel user = session.users().getUserByUsername(appRealm, "johnkeycloak");

            // User is deleted just locally
            Assert.assertTrue(session.users().removeUser(appRealm, user));

            // Assert user not available locally, but will be reimported from LDAP once searched
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "johnkeycloak"));
            Assert.assertNotNull(session.users().getUserByUsername(appRealm, "johnkeycloak"));
        });

        // change username
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);
            UserModel user = session.users().getUserByUsername(appRealm, "johnkeycloak");

            // change username locally
            user.setUsername("johnkeycloak-renamed");
        });

        // check user is found just once
        List<UserRepresentation> users = testRealm().users().search("johnkeycloak", 0, 2);
        Assert.assertEquals("More than one user is found", 1, users.size());
        List<ComponentRepresentation> components = testRealm().components().query(
                testRealm().toRepresentation().getId(), UserStorageProvider.class.getName(), "test-ldap");
        Assert.assertEquals("LDAP component not found", 1, users.size());
        Assert.assertEquals(components.iterator().next().getId(), users.iterator().next().getFederationLink());

        // Revert
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());

            appRealm.updateComponent(ctx.getLdapModel());

            Assert.assertEquals(UserStorageProvider.EditMode.WRITABLE.toString(), appRealm.getComponent(ctx.getLdapModel().getId()).getConfig().getFirst(LDAPConstants.EDIT_MODE));
        });
    }


    @Test
    public void testSearchByAttributes() {
        testingClient.server().run(session -> {
            final String ATTRIBUTE = "postal_code";
            final String ATTRIBUTE_VALUE = "80330340";

            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            session.getContext().setRealm(appRealm);

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username8", "John8", "Doel8", "user8@email.org", null, ATTRIBUTE_VALUE);
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username9", "John9", "Doel9", "user9@email.org", null, ATTRIBUTE_VALUE);
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username10", "John10", "Doel10", "user10@email.org", null, "1210");

            // Users are not at local store at this moment
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "username8"));
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "username9"));
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "username10"));

            // search for user by attribute
            List<UserModel> users = ctx.getLdapProvider().searchForUserByUserAttributeStream(appRealm, ATTRIBUTE, ATTRIBUTE_VALUE)
                .collect(Collectors.toList());
            assertEquals(2, users.size());
            List<String> attrList = users.get(0).getAttributeStream(ATTRIBUTE).collect(Collectors.toList());
            assertEquals(1, attrList.size());
            assertEquals(ATTRIBUTE_VALUE, attrList.get(0));

            attrList = users.get(1).getAttributeStream(ATTRIBUTE).collect(Collectors.toList());
            assertEquals(1, attrList.size());
            assertEquals(ATTRIBUTE_VALUE, attrList.get(0));

            // user are now imported to local store
            LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), appRealm, "username8", "John8", "Doel8", "user8@email.org", ATTRIBUTE_VALUE);
            LDAPTestAsserts.assertUserImported(UserStoragePrivateUtil.userLocalStorage(session), appRealm, "username9", "John9", "Doel9", "user9@email.org", ATTRIBUTE_VALUE);
            // but the one not looked up is not
            Assert.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(appRealm, "username10"));

        });
    }


    // KEYCLOAK-9002
    @Test
    public void testSearchWithPartiallyCachedUser() {
        testingClient.server().run(session -> {
            UserStorageUtil.userCache(session).clear();
        });


        // This will load user from LDAP and partially cache him (including attributes)
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            UserModel user = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertNotNull(user);

            user.getAttributes();
        });


        // Assert search without arguments won't blow up with StackOverflowError
        adminClient.realm("test").users().search(null, 0, 10, false);

        List<UserRepresentation> users = adminClient.realm("test").users().search("johnkeycloak", 0, 10, false);
        Assert.assertTrue(users.stream().anyMatch(userRepresentation -> "johnkeycloak".equals(userRepresentation.getUsername())));
    }


    @Test
    public void testLDAPUserRefreshCache() {
        try {
            testingClient.testing().setTestingInfinispanTimeService();
            testingClient.server().run(session -> {
                UserStorageUtil.userCache(session).clear();
            });

            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel appRealm = ctx.getRealm();
                session.getContext().setRealm(appRealm);

                LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
                LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "johndirect", "John", "Direct", "johndirect@email.org", null, "1234");

                // Fetch user from LDAP and check that postalCode is filled
                UserModel user = session.users().getUserByUsername(appRealm, "johndirect");
                String postalCode = user.getFirstAttribute("postal_code");
                Assert.assertEquals("1234", postalCode);

                LDAPTestUtils.removeLDAPUserByUsername(ldapProvider, appRealm, ldapProvider.getLdapIdentityStore().getConfig(), "johndirect");
            });

            setTimeOffset(60 * 5); // 5 minutes in future, user should be cached still

            testingClient.server().run(session -> {
                RealmModel appRealm = new RealmManager(session).getRealmByName("test");
                session.getContext().setRealm(appRealm);
                CachedUserModel user = (CachedUserModel) session.users().getUserByUsername(appRealm, "johndirect");
                String postalCode = user.getFirstAttribute("postal_code");
                String email = user.getEmail();
                Assert.assertEquals("1234", postalCode);
                Assert.assertEquals("johndirect@email.org", email);
            });

            setTimeOffset(60 * 20); // 20 minutes into future, cache will be invalidated

            testingClient.server().run(session -> {
                RealmModel appRealm = new RealmManager(session).getRealmByName("test");
                session.getContext().setRealm(appRealm);
                UserModel user = session.users().getUserByUsername(appRealm, "johndirect");
                Assert.assertNull(user);
            });
        } finally {
            resetTimeOffset();
            testingClient.testing().revertTestingInfinispanTimeService();
        }
    }

    @Test
    public void testCacheUser() {
        String userId = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().setCachePolicy(UserStorageProviderModel.CachePolicy.NO_CACHE);
            ctx.getRealm().updateComponent(ctx.getLdapModel());

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "testCacheUser", "John", "Cached", "johndirect@test.com", null, "1234");

            // Fetch user from LDAP and check that postalCode is filled
            UserModel testedUser = session.users().getUserByUsername(ctx.getRealm(), "testCacheUser");

            String usserId = testedUser.getId();
            Assert.assertNotNull(usserId);
            Assert.assertFalse(usserId.isEmpty());

            return usserId;
        }, String.class);

        testingClient.server().run(session -> {

            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            UserModel testedUser = session.users().getUserById(appRealm, userId);
            Assert.assertFalse(testedUser instanceof CachedUserModel);
        });

        // restore default cache policy
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            ctx.getLdapModel().setCachePolicy(UserStorageProviderModel.CachePolicy.MAX_LIFESPAN);
            ctx.getLdapModel().setMaxLifespan(600000); // Lifetime is 10 minutes
            ctx.getRealm().updateComponent(ctx.getLdapModel());
        });


        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            UserModel testedUser = session.users().getUserById(appRealm, userId);
            Assert.assertTrue(testedUser instanceof CachedUserModel);
        });

        setTimeOffset(60 * 5); // 5 minutes in future, should be cached still
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            UserModel testedUser = session.users().getUserById(appRealm, userId);
            Assert.assertTrue(testedUser instanceof CachedUserModel);
        });

        setTimeOffset(60 * 10); // 10 minutes into future, cache will be invalidated
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            UserModel testedUser = session.users().getUserByUsername(appRealm, "thor");
            Assert.assertFalse(testedUser instanceof CachedUserModel);
        });

        setTimeOffset(0);
    }

    @Test
    public void testAlwaysReadValueFromLdapCached() throws Exception {
        try {
            testingClient.testing().setTestingInfinispanTimeService();
            // import user from the ldap johnkeycloak and cache it reading it by id
            List<UserRepresentation> users = testRealm().users().search("johnkeycloak", true);
            Assert.assertEquals(1, users.size());
            UserRepresentation john = users.iterator().next();
            Assert.assertEquals("Doe", john.getLastName());
            john = testRealm().users().get(john.getId()).toRepresentation();
            Assert.assertEquals("Doe", john.getLastName());

            // modify the sn of the user directly in ldap
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                LDAPObject johnLdapObject = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "johnkeycloak");
                johnLdapObject.setSingleAttribute(LDAPConstants.SN, "sn-modified");
                ctx.getLdapProvider().getLdapIdentityStore().update(johnLdapObject);
            });

            // it's cached so it should be still the initial one
            users = testRealm().users().search("johnkeycloak", true);
            Assert.assertEquals(1, users.size());
            john = users.iterator().next();
            Assert.assertEquals("Doe", john.getLastName());
            john = testRealm().users().get(john.getId()).toRepresentation();
            Assert.assertEquals("Doe", john.getLastName());

            // expire the cache which is 10 minutes
            setTimeOffset(610);

            // new sn should be present
            users = testRealm().users().search("johnkeycloak", true);
            Assert.assertEquals(1, users.size());
            john = users.iterator().next();
            Assert.assertEquals("sn-modified", john.getLastName());
            john = testRealm().users().get(john.getId()).toRepresentation();
            Assert.assertEquals("sn-modified", john.getLastName());
        } finally {
            // revert
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                LDAPObject johnLdapObject = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "johnkeycloak");
                johnLdapObject.setSingleAttribute(LDAPConstants.SN, "Doe");
                ctx.getLdapProvider().getLdapIdentityStore().update(johnLdapObject);
            });
            resetTimeOffset();
            testingClient.testing().revertTestingInfinispanTimeService();
        }
    }

    @Test
    public void testAlwaysReadValueFromLdapNoCache() throws Exception {
        // set to NO_CACHE
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            ctx.getLdapModel().setCachePolicy(UserStorageProviderModel.CachePolicy.NO_CACHE);
            appRealm.updateComponent(ctx.getLdapModel());
        });

        try {
            // import user from the ldap johnkeycloak
            List<UserRepresentation> users = testRealm().users().search("johnkeycloak", true);
            Assert.assertEquals(1, users.size());
            UserRepresentation john = users.iterator().next();
            Assert.assertEquals("Doe", john.getLastName());
            john = testRealm().users().get(john.getId()).toRepresentation();
            Assert.assertEquals("Doe", john.getLastName());

            // modify the sn of the user directly in ldap
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel appRealm = ctx.getRealm();
                LDAPObject johnLdapObject = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");
                johnLdapObject.setSingleAttribute(LDAPConstants.SN, "sn-modified");
                ctx.getLdapProvider().getLdapIdentityStore().update(johnLdapObject);
            });

            // no cache, so it should be validated and new data received
            users = testRealm().users().search("johnkeycloak", true);
            Assert.assertEquals(1, users.size());
            john = users.iterator().next();
            Assert.assertEquals("sn-modified", john.getLastName());
            john = testRealm().users().get(john.getId()).toRepresentation();
            Assert.assertEquals("sn-modified", john.getLastName());
        } finally {
            // revert cache to default max-liespan setting
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                LDAPObject johnLdapObject = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "johnkeycloak");
                johnLdapObject.setSingleAttribute(LDAPConstants.SN, "Doe");
                ctx.getLdapProvider().getLdapIdentityStore().update(johnLdapObject);
                ctx.getLdapModel().setCachePolicy(UserStorageProviderModel.CachePolicy.MAX_LIFESPAN);
                ctx.getLdapModel().setMaxLifespan(600000);
                ctx.getRealm().updateComponent(ctx.getLdapModel());
            });
        }
    }

    @Test
    public void testEmailVerifiedFromImport(){

        // Test trusted email option
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().put(LDAPConstants.TRUST_EMAIL, "true");
            ctx.getRealm().updateComponent(ctx.getLdapModel());
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "testUserVerified", "John", "Email", "john@test.com", null, "1234");
        });
        loginPage.open();
        loginPage.login("testuserVerified", "password");

        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            Optional<UserModel> userVerified = session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "john@test.com")).findFirst();
            Assert.assertTrue(userVerified.get().isEmailVerified());
        });

        //Test untrusted email option
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().put(LDAPConstants.TRUST_EMAIL, "false");
            ctx.getRealm().updateComponent(ctx.getLdapModel());
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "testUserNotVerified", "John", "Email", "john2@test.com", null, "1234");
        });

        loginPage.open();
        loginPage.login("testuserNotVerified", "password");

        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            Optional<UserModel> userNotVerified = session.users().searchForUserStream(appRealm, Map.of(UserModel.SEARCH, "john2@test.com")).findFirst();
            Assert.assertFalse(userNotVerified.get().isEmailVerified());
        });
    }

    @Test
    public void testUserAttributeLDAPStorageMapperHandlingUsernameLowercasing() {
        setEditingUsernameAllowed(false);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel johnkeycloak = session.users().getUserByUsername(appRealm, "johnkeycloak");
            // If the username was case sensitive in the username-cn mapper, then this would throw an exception
            johnkeycloak.setSingleAttribute(UserModel.USERNAME, "JohnKeycloak");
        });

        // Cleanup
        setEditingUsernameAllowed(true);
    }

    private void setEditingUsernameAllowed(boolean allowed) {
        RealmRepresentation realmRepresentation = testRealm().toRepresentation();
        realmRepresentation.setEditUsernameAllowed(allowed);
        testRealm().update(realmRepresentation);
    }

    @Test
    public void updateLDAPUsernameTest() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            // Add user to LDAP
            LDAPObject becky = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "beckybecks", "Becky", "Becks", "becky-becks@email.org", null, "123");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), becky, "Password1");
        });

        loginSuccessAndLogout("beckybecks", "Password1");

        String origKeycloakUserId = testingClient.server().fetchString(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel testRealm = ctx.getRealm();

            UserModel importedUser = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(testRealm, "beckybecks");
            Assert.assertNotNull(importedUser);

            // Update user 'beckybecks' in LDAP
            LDAPObject becky = ctx.getLdapProvider().loadLDAPUserByUsername(testRealm, importedUser.getUsername());
            // NOTE: Changing LDAP Username directly here
            String userNameLdapAttributeName = ctx.getLdapProvider().getLdapIdentityStore().getConfig().getUsernameLdapAttribute();
            becky.setSingleAttribute(userNameLdapAttributeName, "beckyupdated");
            becky.setSingleAttribute(LDAPConstants.EMAIL, "becky-updated@email.org");
            ctx.getLdapProvider().getLdapIdentityStore().update(becky);
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), becky, "MyChangedPassword11");
            return importedUser.getId();
        });

        loginSuccessAndLogout("beckyupdated", "MyChangedPassword11");

        loginPage.open();
        loginPage.login("beckybecks", "Password1");
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            // The original username is not possible to use as username was changed in LDAP.
            // However the call to LDAPStorageProvider.loadAndValidateUser shouldn't delete the user just because his username changed in LDAP
            UserModel user = session.users().getUserByUsername(ctx.getRealm(), "beckybecks");
            Assert.assertNull(user);

            // Assert user can be found with new username from LDAP. And it is same user as before
            user = session.users().getUserByUsername(ctx.getRealm(), "beckyupdated");
            Assert.assertNotNull(user);
            String newKeycloakUserId = user.getId();
            // Need to remove double quotes from server response
            Assert.assertEquals(origKeycloakUserId.replace("\"",""), newKeycloakUserId);
        });
    }

}
