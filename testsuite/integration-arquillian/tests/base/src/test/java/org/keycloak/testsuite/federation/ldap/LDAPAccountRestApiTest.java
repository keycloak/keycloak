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
import java.util.ArrayList;
import java.util.List;

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
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.Profile;
import org.keycloak.federation.kerberos.KerberosFederationProvider;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.TokenUtil;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
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
        // don't run this test when map storage is enabled, as map storage doesn't support the legacy style federation
        ProfileAssume.assumeFeatureDisabled(Profile.Feature.MAP_STORAGE);
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
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");
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

        List<String> origLdapId = new ArrayList<>(user.getAttributes().get(LDAPConstants.LDAP_ID));
        List<String> origLdapEntryDn = new ArrayList<>(user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN));
        Assert.assertEquals(1, origLdapId.size());
        Assert.assertEquals(1, origLdapEntryDn.size());
        Assert.assertThat(user.getAttributes().keySet(), not(contains(KerberosFederationProvider.KERBEROS_PRINCIPAL)));

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
        user.getAttributes().get(LDAPConstants.LDAP_ID).remove(0);
        user.getAttributes().get(LDAPConstants.LDAP_ID).add("123");
        updateProfileExpectError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

        // Trying to delete LDAP_ID should fail (Removing attribute, which was present here already)
        user.getAttributes().get(LDAPConstants.LDAP_ID).remove(0);
        updateProfileExpectError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

        // ignore removal for read-only attributes
        user.getAttributes().remove(LDAPConstants.LDAP_ID);
        updateProfileExpectSuccess(user);
        user = getProfile();
        assertFalse(user.getAttributes().get(LDAPConstants.LDAP_ID).isEmpty());

        // Trying to update LDAP_ENTRY_DN should fail
        user.getAttributes().put(LDAPConstants.LDAP_ID, origLdapId);
        user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN).remove(0);
        user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN).add("ou=foo,dc=bar");
        updateProfileExpectError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

        // Update firstName and lastName should be fine
        user.getAttributes().put(LDAPConstants.LDAP_ENTRY_DN, origLdapEntryDn);
        updateProfileExpectSuccess(user);

        user = getProfile();
        assertEquals("JohnUpdated", user.getFirstName());
        assertEquals("DoeUpdated", user.getLastName());
        assertEquals(origLdapId, user.getAttributes().get(LDAPConstants.LDAP_ID));
        assertEquals(origLdapEntryDn, user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN));

        // Revert
        user.setFirstName("John");
        user.setLastName("Doe");
        updateProfileExpectSuccess(user);
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
        Assert.assertEquals(userPassword.getCreatedDate(), new Long(-1L));
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
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        user.setEmail("john-alias@email.org");
        SimpleHttp.doPost(getAccountUrl(null), httpClient).json(user).auth(tokenUtil.getToken()).asStatus();

        UserRepresentation usernew = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        assertEquals("johnkeycloak", usernew.getUsername());
        assertEquals("John", usernew.getFirstName());
        assertEquals("Doe", usernew.getLastName());
        assertEquals("john-alias@email.org", usernew.getEmail());
        assertFalse(usernew.isEmailVerified());

        //clean up
        usernew.setEmail("john@email.org");
        SimpleHttp.doPost(getAccountUrl(null), httpClient).json(usernew).auth(tokenUtil.getToken()).asStatus();

    }

    @Test
    public void testIgnoreReadOnlyAttributes() throws IOException {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            appRealm.setEditUsernameAllowed(false);
        });
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        user.setEmail("john-alias@email.org");
        SimpleHttp.doPost(getAccountUrl(null), httpClient).json(user).auth(tokenUtil.getToken()).asStatus();

        UserRepresentation usernew = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        assertEquals("johnkeycloak", usernew.getUsername());
        assertEquals("John", usernew.getFirstName());
        assertEquals("Doe", usernew.getLastName());
        assertEquals("john-alias@email.org", usernew.getEmail());
        assertFalse(usernew.isEmailVerified());

        usernew.getAttributes().clear();

        //clean up
        usernew.setEmail("john@email.org");
        final int i = SimpleHttp.doPost(getAccountUrl(null), httpClient).json(usernew).auth(tokenUtil.getToken()).asStatus();

        org.keycloak.representations.idm.UserRepresentation userRep = testRealm().users()
                .search(usernew.getUsername()).get(0);

        userRep.setAttributes(null);

        testRealm().users().get(userRep.getId()).update(userRep);
        usernew = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);

        assertTrue(usernew.getAttributes().containsKey(LDAPConstants.LDAP_ID));
        assertTrue(usernew.getAttributes().containsKey(LDAPConstants.LDAP_ENTRY_DN));
    }


    private String getAccountUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account" + (resource != null ? "/" + resource : "");
    }

    private UserRepresentation getProfile() throws IOException {
        return SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
    }

    private void updateProfileExpectSuccess(UserRepresentation user) throws IOException {
        int status = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asStatus();
        assertEquals(204, status);
    }

    private void updateProfileExpectError(UserRepresentation user, int expectedStatus, String expectedMessage) throws IOException {
        SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedMessage, response.asJson(ErrorRepresentation.class).getErrorMessage());
    }

    // Send REST request to get all credential containers and credentials of current user
    private List<AccountCredentialResource.CredentialContainer> getCredentials() throws IOException {
        return SimpleHttp.doGet(getAccountUrl("credentials"), httpClient)
                .auth(tokenUtil.getToken()).asJson(new TypeReference<List<AccountCredentialResource.CredentialContainer>>() {});
    }



}
