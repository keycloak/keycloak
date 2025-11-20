/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
import java.util.List;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.federation.kerberos.KerberosFederationProvider;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPAccountRestApiTest extends AbstractLDAPTest {

    @Rule
    public TokenUtil tokenUtil = new TokenUtil("johnkeycloak", "Password1");

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    protected CloseableHttpClient httpClient;

    @Before
    public void before() {
        httpClient = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        boolean isEmbeddedServer = ldapRule.isEmbeddedServer();
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            if (isEmbeddedServer) {
                MultivaluedHashMap<String, String> otherAttrs = new MultivaluedHashMap<>();

                otherAttrs.putSingle(LDAPConstants.PWD_CHANGED_TIME, "22000101000000Z");

                LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "johnkeycloak", "John", "Doe", "john@email.org", otherAttrs);
                LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");
            } else {
                LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
                LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");
            }
        });
    }

    @Test
    public void testGetProfile() throws IOException {
        UserRepresentation user = getProfile();
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("john@email.org", user.getEmail());
        assertFalse(user.isEmailVerified());
    }

    @Test
    public void testUpdateProfile() throws IOException {
        UserRepresentation user = getProfile();

        // Metadata attributes like LDAP_ID are not present
        Assert.assertNull(user.getAttributes());

        org.keycloak.representations.idm.UserRepresentation adminRestUserRep = testRealm().users()
                .search(user.getUsername()).get(0);
        List<String> origLdapId = adminRestUserRep.getAttributes().get(LDAPConstants.LDAP_ID);
        List<String> origLdapEntryDn = adminRestUserRep.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN);
        Assert.assertNotNull(origLdapId.get(0));
        Assert.assertNotNull(origLdapEntryDn.get(0));
        adminRestUserRep = testRealm().users().get(adminRestUserRep.getId()).toRepresentation();
        origLdapId = adminRestUserRep.getAttributes().get(LDAPConstants.LDAP_ID);
        origLdapEntryDn = adminRestUserRep.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN);
        Assert.assertNotNull(origLdapId.get(0));
        Assert.assertNotNull(origLdapEntryDn.get(0));

        // Trying to add KERBEROS_PRINCIPAL (Adding attribute, which was not yet present). Request does not fail, but attribute is not updated
        user.setFirstName("JohnUpdated");
        user.setLastName("DoeUpdated");
        user.singleAttribute(KerberosFederationProvider.KERBEROS_PRINCIPAL, "foo");
        updateProfileExpectSuccess(user);
        user = getProfile();
        Assert.assertEquals("JohnUpdated", user.getFirstName());
        Assert.assertEquals("DoeUpdated", user.getLastName());
        Assert.assertNull(user.getAttributes());

        // Trying to update LDAP_ID should fail (Updating existing attribute, which is present on the user even if not visible to the user)
        user.singleAttribute(LDAPConstants.LDAP_ID, "123");
        updateProfileExpectError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

        // ignore removal for read-only attributes
        user.getAttributes().remove(LDAPConstants.LDAP_ID);
        updateProfileExpectSuccess(user);
        user = getProfile();
        Assert.assertNull(user.getAttributes());

        // Trying to update LDAP_ENTRY_DN should fail
        user.singleAttribute(LDAPConstants.LDAP_ENTRY_DN, "ou=foo,dc=bar");
        updateProfileExpectError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

        // Update firstName and lastName should be fine
        user.getAttributes().remove(LDAPConstants.LDAP_ENTRY_DN);
        updateProfileExpectSuccess(user);

        user = getProfile();
        assertEquals("JohnUpdated", user.getFirstName());
        assertEquals("DoeUpdated", user.getLastName());
        Assert.assertNull(user.getAttributes());

        // Revert
        user.setFirstName("John");
        user.setLastName("Doe");
        updateProfileExpectSuccess(user);
    }

    @Test
    public void testUpdateProfileUnmanagedAttributes() throws IOException {
        // User profile unmanaged attributes supported
        UserProfileResource userProfileRes = testRealm().users().userProfile();
        UPConfig origConfig = UserProfileUtil.enableUnmanagedAttributes(userProfileRes);

        try {
            UserRepresentation user = getProfile();

            // Metadata attributes like LDAP_ID are not present
            Assert.assertNull(user.getAttributes());

            org.keycloak.representations.idm.UserRepresentation adminRestUserRep = testRealm().users()
                    .search(user.getUsername()).get(0);
            List<String> origLdapId = adminRestUserRep.getAttributes().get(LDAPConstants.LDAP_ID);
            List<String> origLdapEntryDn = adminRestUserRep.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN);
            Assert.assertNotNull(origLdapId.get(0));
            Assert.assertNotNull(origLdapEntryDn.get(0));

            // Trying to add KERBEROS_PRINCIPAL should fail (Adding attribute, which was not yet present)
            user.setFirstName("JohnUpdated");
            user.setLastName("DoeUpdated");
            user.singleAttribute(KerberosFederationProvider.KERBEROS_PRINCIPAL, "foo");
            updateProfileExpectError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

            // The same test, but consider case sensitivity
            user.getAttributes().remove(KerberosFederationProvider.KERBEROS_PRINCIPAL);
            user.singleAttribute("KERberos_principal", "foo");
            updateProfileExpectError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

            // Trying to update LDAP_ID should fail (Updating existing attribute, which was present)
            user.getAttributes().remove("KERberos_principal");
            user.setFirstName("JohnUpdated");
            user.setLastName("DoeUpdated");
            user.singleAttribute(LDAPConstants.LDAP_ID, "123");
            updateProfileExpectError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

            // Trying to delete LDAP_ID (by set to null) should fail (Removing attribute, which was present here already)
            user.singleAttribute(LDAPConstants.LDAP_ID, null);
            updateProfileExpectError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

            // ignore removal for read-only attributes
            user.getAttributes().remove(LDAPConstants.LDAP_ID);
            updateProfileExpectSuccess(user);
            user = getProfile();
            Assert.assertNull(user.getAttributes());

            user = getProfile();
            assertEquals("JohnUpdated", user.getFirstName());
            assertEquals("DoeUpdated", user.getLastName());
            Assert.assertNull(user.getAttributes());

            // Revert
            user.setFirstName("John");
            user.setLastName("Doe");
            updateProfileExpectSuccess(user);
        } finally {
            userProfileRes.update(origConfig);
        }
    }

    @Test
    public void testGetCredentials() throws IOException {
        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

        AccountCredentialResource.CredentialContainer password = credentials.get(0);
        Assert.assertEquals(PasswordCredentialModel.TYPE, password.getType());
        Assert.assertEquals(1, password.getUserCredentialMetadatas().size());
        CredentialRepresentation userPassword = password.getUserCredentialMetadatas().get(0).getCredential();

        // Password won't have createdDate and any metadata set
        Assert.assertEquals(PasswordCredentialModel.TYPE, userPassword.getType());
        Assert.assertTrue(userPassword.getCreatedDate() > -1L);
        Assert.assertNull(userPassword.getCredentialData());
        Assert.assertNull(userPassword.getSecretData());
    }


    @Test
    public void testUpdateProfileSimple() throws IOException {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            appRealm.setEditUsernameAllowed(false);
        });
        UserRepresentation user = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        user.setEmail("john-alias@email.org");
        SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).json(user).auth(tokenUtil.getToken()).asStatus();

        UserRepresentation usernew = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        assertEquals("johnkeycloak", usernew.getUsername());
        assertEquals("John", usernew.getFirstName());
        assertEquals("Doe", usernew.getLastName());
        assertEquals("john-alias@email.org", usernew.getEmail());
        assertFalse(usernew.isEmailVerified());

        //clean up
        usernew.setEmail("john@email.org");
        SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).json(usernew).auth(tokenUtil.getToken()).asStatus();

    }

    @Test
    public void testIgnoreReadOnlyAttributes() throws IOException {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            appRealm.setEditUsernameAllowed(false);
        });
        UserRepresentation user = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        user.setEmail("john-alias@email.org");
        SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).json(user).auth(tokenUtil.getToken()).asStatus();

        UserRepresentation usernew = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        assertEquals("johnkeycloak", usernew.getUsername());
        assertEquals("John", usernew.getFirstName());
        assertEquals("Doe", usernew.getLastName());
        assertEquals("john-alias@email.org", usernew.getEmail());
        assertFalse(usernew.isEmailVerified());

        // No metadata attributes like LDAP_ID or LDAP_ENTRY_DN present in account REST API
        Assert.assertNull(usernew.getAttributes());

        //clean up
        usernew.setEmail("john@email.org");
        final int i = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).json(usernew).auth(tokenUtil.getToken()).asStatus();

        org.keycloak.representations.idm.UserRepresentation userRep = testRealm().users()
                .search(usernew.getUsername()).get(0);

        // Metadata attributes present in admin REST API
        assertTrue(userRep.getAttributes().containsKey(LDAPConstants.LDAP_ID));
        assertTrue(userRep.getAttributes().containsKey(LDAPConstants.LDAP_ENTRY_DN));

        userRep.setAttributes(null);

        testRealm().users().get(userRep.getId()).update(userRep);
        usernew = SimpleHttpDefault.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);

        // Metadata attributes still not present in account REST
        Assert.assertNull(usernew.getAttributes());

        // Metadata attributes still present in admin REST API
        userRep = testRealm().users().search(usernew.getUsername()).get(0);
        assertTrue(userRep.getAttributes().containsKey(LDAPConstants.LDAP_ID));
        assertTrue(userRep.getAttributes().containsKey(LDAPConstants.LDAP_ENTRY_DN));
    }


    private String getAccountUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account" + (resource != null ? "/" + resource : "");
    }

    private UserRepresentation getProfile() throws IOException {
        return SimpleHttpDefault.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
    }

    private void updateProfileExpectSuccess(UserRepresentation user) throws IOException {
        int status = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asStatus();
        assertEquals(204, status);
    }

    private void updateProfileExpectError(UserRepresentation user, int expectedStatus, String expectedMessage) throws IOException {
        SimpleHttpResponse response = SimpleHttpDefault.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedMessage, response.asJson(ErrorRepresentation.class).getErrorMessage());
    }

    // Send REST request to get all credential containers and credentials of current user
    private List<AccountCredentialResource.CredentialContainer> getCredentials() throws IOException {
        return SimpleHttpDefault.doGet(getAccountUrl("credentials"), httpClient)
                .auth(tokenUtil.getToken()).asJson(new TypeReference<List<AccountCredentialResource.CredentialContainer>>() {});
    }



}
