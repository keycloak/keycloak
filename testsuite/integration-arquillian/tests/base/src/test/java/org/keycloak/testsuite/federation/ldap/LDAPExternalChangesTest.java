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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPExternalChangesTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
    }

    @Before
    public void onBefore() {
        testingClient.testing().setTestingInfinispanTimeService();
    }

    @After
    public void onAfter() {
        testingClient.testing().revertTestingInfinispanTimeService();
    }


    @Test
    public void failAuthenticationIfEmailDifferentThanExternalStorage() {
        testingClient.server().run((session) -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().setCachePolicy(UserStorageProviderModel.CachePolicy.MAX_LIFESPAN);
            ctx.getLdapModel().setMaxLifespan(600000);
            RealmModel realm = ctx.getRealm();
            realm.updateComponent(ctx.getLdapModel());
            LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");
            realm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
        });
        String originalEmail = "john@email.org";

        // import user from the ldap johnkeycloak and cache it reading it by id
        List<UserRepresentation> users = managedRealm.admin().users().search("johnkeycloak", true);
        assertEquals(1, users.size());
        UserRepresentation user = users.get(0);
        String userId = user.getId();
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(originalEmail, "Password1");
        assertTrue(tokenResponse.isSuccess());

        // modify the email of the user directly in ldap
        String updatedEmail = "updatedjohnkeycloak@email.org";
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPObject johnLdapObject = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), "johnkeycloak");
            johnLdapObject.setSingleAttribute(LDAPConstants.EMAIL, updatedEmail);
            ctx.getLdapProvider().getLdapIdentityStore().update(johnLdapObject);
        });

        tokenResponse = oauth.doPasswordGrantRequest(originalEmail, "Password1");
        assertTrue(tokenResponse.isSuccess());

        timeOffSet.set(610);

        tokenResponse = oauth.doPasswordGrantRequest(originalEmail, "Password1");
        assertFalse(tokenResponse.isSuccess());

        tokenResponse = oauth.doPasswordGrantRequest(updatedEmail, "Password1");
        assertTrue(tokenResponse.isSuccess());

        users = managedRealm.admin().users().search(originalEmail, true);
        assertTrue(users.isEmpty());
        users = managedRealm.admin().users().search("johnkeycloak", true);
        assertEquals(1, users.size());
        user = users.get(0);
        assertEquals(userId, user.getId());
        assertEquals(user.getEmail(), updatedEmail);
    }

    @Test
    public void failAuthenticationIfUsernameDifferentThanExternalStorage() {
        String originalUsername = "john";
        testingClient.server().run((session) -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().setCachePolicy(UserStorageProviderModel.CachePolicy.MAX_LIFESPAN);
            ctx.getLdapModel().setMaxLifespan(600000);
            RealmModel realm = ctx.getRealm();
            realm.updateComponent(ctx.getLdapModel());
            LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), realm, originalUsername, "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");
            realm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
        });
        // import user from the ldap johnkeycloak and cache it reading it by id
        List<UserRepresentation> users = managedRealm.admin().users().search(originalUsername, true);
        assertEquals(1, users.size());
        UserRepresentation user = users.get(0);
        String userId = user.getId();
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(originalUsername, "Password1");
        assertTrue(tokenResponse.isSuccess());

        // modify the email of the user directly in ldap
        String updatedUsername = "changed" + originalUsername;
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPObject johnLdapObject = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), originalUsername);
            johnLdapObject.setSingleAttribute(LDAPConstants.UID, updatedUsername);
            ctx.getLdapProvider().getLdapIdentityStore().update(johnLdapObject);
        });

        tokenResponse = oauth.doPasswordGrantRequest(originalUsername, "Password1");
        assertTrue(tokenResponse.isSuccess());

        timeOffSet.set(610);

        tokenResponse = oauth.doPasswordGrantRequest(originalUsername, "Password1");
        assertFalse(tokenResponse.isSuccess());

        tokenResponse = oauth.doPasswordGrantRequest(updatedUsername, "Password1");
        assertTrue(tokenResponse.isSuccess());

        users = managedRealm.admin().users().search(originalUsername, true);
        assertTrue(users.isEmpty());
        users = managedRealm.admin().users().search(updatedUsername, true);
        user = users.get(0);
        assertEquals(userId, user.getId());
        assertEquals(user.getUsername(), updatedUsername);
    }

    private void addLdapUserAndRecordExpectedId(String username, AtomicReference<String> expectedUserId) {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            // The reported scenario runs against a read-only federation, where deleting the user removes just the
            // local record and the LDAP entry is re-imported on the next search
            ctx.getLdapModel().put(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.toString());
            realm.updateComponent(ctx.getLdapModel());
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), realm, username, "Stable", "Id", username + "@ldap.org", null, "1234");
            LDAPObject ldapUser = ctx.getLdapProvider().loadLDAPUserByUsername(realm, username);
            expectedUserId.set(LDAPUtils.generateStableUserId(realm.getId(), ctx.getLdapModel().getId(), ldapUser.getUuid()));
        });
    }

    private UserRepresentation importUserFromLdap(String username) {
        List<UserRepresentation> users = managedRealm.admin().users().search(username, true);
        assertEquals(1, users.size(), "Expected the LDAP user '" + username + "' to be imported by the search");
        return users.get(0);
    }

    @Test
    public void testImportedUserIdDerivedFromFederationAnchors() {
        AtomicReference<String> expectedUserId = new AtomicReference<>();
        addLdapUserAndRecordExpectedId("stableid1", expectedUserId);
        UserRepresentation imported = importUserFromLdap("stableid1");
        assertEquals(expectedUserId.get(), imported.getId());
        assertNotNull(UUID.fromString(imported.getId()));
    }

    @Test
    public void testUserIdRestoredAfterDeleteAndReimport() {
        AtomicReference<String> expectedUserId = new AtomicReference<>();
        addLdapUserAndRecordExpectedId("stableid2", expectedUserId);
        UserRepresentation imported = importUserFromLdap("stableid2");
        assertEquals(expectedUserId.get(), imported.getId());
        try (Response resp = managedRealm.admin().users().delete(imported.getId())) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resp.getStatus());
        }
        // The LDAP entry was not removed by the delete, so re-importing the same entry restores the previous id
        UserRepresentation reimported = importUserFromLdap("stableid2");
        assertEquals(imported.getId(), reimported.getId());
    }

    @Test
    public void testUserIdStableAcrossRepeatedImports() {
        AtomicReference<String> expectedUserId = new AtomicReference<>();
        addLdapUserAndRecordExpectedId("stableid3", expectedUserId);
        UserRepresentation first = importUserFromLdap("stableid3");
        UserRepresentation second = importUserFromLdap("stableid3");
        assertEquals(expectedUserId.get(), first.getId());
        assertEquals(first.getId(), second.getId());
    }
}
