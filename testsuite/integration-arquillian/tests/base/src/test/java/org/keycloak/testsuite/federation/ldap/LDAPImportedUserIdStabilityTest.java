/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that users imported from LDAP are assigned a user id derived from stable federation anchors (realm, provider
 * and the UUID of the LDAP entry) instead of a random one, so that re-importing the same LDAP entry after the user
 * was deleted locally restores the previous id. Identifiers built on top of the user id, such as the WebAuthn user
 * handle of registered passkeys, stay valid across such a re-import.
 *
 * See https://github.com/keycloak/keycloak/issues/54325
 */
public class LDAPImportedUserIdStabilityTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        // The reported scenario runs against a read-only federation, where deleting the user removes just the
        // local record and the LDAP entry is re-imported on the next search.
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().put(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.toString());
            ctx.getRealm().updateComponent(ctx.getLdapModel());
        });
    }

    private void addLdapUserAndRecordExpectedId(String username, AtomicReference<String> expectedUserId) {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
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
        assertDoesNotThrow(() -> UUID.fromString(imported.getId()));
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
