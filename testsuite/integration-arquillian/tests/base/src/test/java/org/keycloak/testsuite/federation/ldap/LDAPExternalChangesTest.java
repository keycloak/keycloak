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

import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProviderModel;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        List<UserRepresentation> users = testRealm().users().search("johnkeycloak", true);
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

        setTimeOffset(610);

        tokenResponse = oauth.doPasswordGrantRequest(originalEmail, "Password1");
        assertFalse(tokenResponse.isSuccess());

        tokenResponse = oauth.doPasswordGrantRequest(updatedEmail, "Password1");
        assertTrue(tokenResponse.isSuccess());

        users = testRealm().users().search(originalEmail, true);
        assertTrue(users.isEmpty());
        users = testRealm().users().search("johnkeycloak", true);
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
        List<UserRepresentation> users = testRealm().users().search(originalUsername, true);
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

        setTimeOffset(610);

        tokenResponse = oauth.doPasswordGrantRequest(originalUsername, "Password1");
        assertFalse(tokenResponse.isSuccess());

        tokenResponse = oauth.doPasswordGrantRequest(updatedUsername, "Password1");
        assertTrue(tokenResponse.isSuccess());

        users = testRealm().users().search(originalUsername, true);
        assertTrue(users.isEmpty());
        users = testRealm().users().search(updatedUsername, true);
        user = users.get(0);
        assertEquals(userId, user.getId());
        assertEquals(user.getUsername(), updatedUsername);
    }
}
