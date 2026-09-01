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

package org.keycloak.tests.federation.ldap;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Properties;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.SynchronizationResultRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestCleanup;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies that LDAP-imported users are checked against the realm's User Profile configuration when the LDAP
 * provider opts in via {@link LDAPConstants#VALIDATE_USER_PROFILE}.
 */
@KeycloakIntegrationTest
public class LDAPUserProfileValidationTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    // Must be static: @TestSetup runs once on the first JUnit test instance, but each @Test method gets its own
    // fresh instance (JUnit's default), so a plain instance field set here would read back as null in the tests.
    private static LDAPEmbeddedServer ldapEmbeddedServer;
    private static String ldapModelId;

    @TestSetup
    public void startLdapServerAndCreateProvider() throws Exception {
        // Use a freely available port rather than the ApacheDS default (10389) - hard-coding it is prone to
        // collisions when tests run in parallel or on a machine that already has something bound to that port.
        int ldapPort = findFreePort();

        Properties serverProperties = new Properties();
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "false");
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_PORT, String.valueOf(ldapPort));
        ldapEmbeddedServer = new LDAPEmbeddedServer(serverProperties);
        ldapEmbeddedServer.init();
        ldapEmbeddedServer.start();

        ComponentRepresentation ldapRep = new ComponentRepresentation();
        ldapRep.setName("test-ldap");
        ldapRep.setProviderId(LDAPConstants.LDAP_PROVIDER);
        ldapRep.setProviderType(UserStorageProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle(LDAPConstants.CONNECTION_URL, "ldap://localhost:" + ldapPort);
        config.putSingle(LDAPConstants.BASE_DN, "dc=keycloak,dc=org");
        config.putSingle(LDAPConstants.USERS_DN, "ou=People,dc=keycloak,dc=org");
        config.putSingle(LDAPConstants.BIND_DN, "uid=admin,ou=system");
        config.putSingle(LDAPConstants.BIND_CREDENTIAL, "secret");
        config.putSingle(LDAPConstants.VENDOR, LDAPConstants.VENDOR_OTHER);
        config.putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.name());
        config.putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
        config.putSingle(LDAPConstants.CONNECTION_POOLING, "true");
        config.putSingle(LDAPConstants.PAGINATION, "true");
        config.putSingle(LDAPConstants.BATCH_SIZE_FOR_SYNC, "3");
        config.putSingle(LDAPConstants.VALIDATE_USER_PROFILE, "true");
        ldapRep.setConfig(config);

        try (Response response = managedRealm.admin().components().add(ldapRep)) {
            Assertions.assertEquals(201, response.getStatus(), "Failed to create the LDAP provider");
            ldapModelId = ApiUtil.getCreatedId(response);
        }
    }

    @TestCleanup
    public void stopLdapServer() throws Exception {
        if (ldapEmbeddedServer != null) {
            ldapEmbeddedServer.stop();
        }
    }

    // The realm/LDAP provider are shared across test methods (@TestSetup only runs once per class), so a user
    // added by one method would otherwise still be there - and picked up by the full sync - in the next one.
    @AfterEach
    public void removeLdapUsers() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeAllLDAPUsers(ldapProvider, realm);
        });
    }

    @Test
    public void testImportUserFailsUserProfileValidation() {
        final String username = "invalidprofileuse<em>r";
        runOnServer.run(addLdapUser(username, "Invalid", "EmailUser", "invalid-email-format"));

        // The realm allows editing the username by default; disable that here so the bad username is genuinely
        // something the user could never fix themselves, forcing a hard rejection of the import.
        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(false));

        List<UserRepresentation> found = managedRealm.admin().users().search(username, true);
        Assertions.assertTrue(found.isEmpty(), "The user should have been stopped by the User Profile validation and not imported.");
    }

    @Test
    public void testImportUserWithEditableAttributeFailureGetsRequiredAction() {
        final String username = "editableattrfailureuser";
        // Username and email are valid; only firstName violates the User Profile ("person-name-prohibited-characters")
        runOnServer.run(addLdapUser(username, "Invalid<b>Name", "ValidLastName", "editable-attr-failure@example.org"));

        List<UserRepresentation> found = managedRealm.admin().users().search(username, true);

        Assertions.assertEquals(1, found.size(), "The user should have been imported since firstName can be fixed by the user themselves.");
        Assertions.assertTrue(found.get(0).getRequiredActions().contains(UserModel.RequiredAction.UPDATE_PROFILE.name()),
                "The user should have been given the UPDATE_PROFILE required action.");
    }

    @Test
    public void testFullSyncCountsValidationFailureAsFailed() {
        final String username = "invalidsyncuser<b>";
        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(false));

        runOnServer.run(addLdapUser(username, "Valid", "User", "valid-sync-user@example.org"));

        SynchronizationResultRepresentation result = managedRealm.admin().userStorage().syncUsers(ldapModelId, "triggerFullSync");

        Assertions.assertEquals(1, result.getFailed(), "The sync result should report the rejected user as failed, not as added.");
        Assertions.assertEquals(0, result.getAdded(), "The rejected user should not be counted as added.");
        Assertions.assertTrue(managedRealm.admin().users().search(username, true).isEmpty(),
                "The user should not exist in the local database.");
    }

    @Test
    public void testRenamingExistingUserToInvalidUsernameStillResolves() {
        final String originalUsername = "renamemevaliduser";
        final String invalidUsername = "renamemeinvalid<em>user";

        runOnServer.run(addLdapUser(originalUsername, "Valid", "User", "rename-user@example.org"));

        // Import the user once so it exists locally, linked to this LDAP entry by its (immutable) UUID
        List<UserRepresentation> found = managedRealm.admin().users().search(originalUsername, true);
        Assertions.assertEquals(1, found.size(), "Sanity check: user should have been imported with a valid username.");

        // Rename the same LDAP entry (same UUID) to a username that would fail User Profile validation
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject ldapUser = ldapProvider.loadLDAPUserByUsername(realm, originalUsername);
            String usernameLdapAttribute = ldapProvider.getLdapIdentityStore().getConfig().getUsernameLdapAttribute();
            ldapUser.setSingleAttribute(usernameLdapAttribute, invalidUsername);
            ldapProvider.getLdapIdentityStore().update(ldapUser);
        });

        // Re-resolving the renamed user (e.g. as part of authentication) re-imports the already-existing local user.
        // User Profile validation intentionally does not apply on this path (see LDAPStorageProvider#importUserFromLDAP):
        // doImportUser() has already applied the rename directly to the persisted user by the time validation would
        // run, so rejecting here could not be undone - it would only leave that user corrupted, not prevent anything.
        Boolean resolved = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            return ldapProvider.getUserByUsername(realm, invalidUsername) != null;
        }, Boolean.class);

        Assertions.assertTrue(resolved, "The renamed user should still resolve, instead of being rejected after already being mutated.");

        runOnServer.run(session -> {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, invalidUsername);
                Assertions.assertNotNull(user, "Local user should exist with the updated username");
            });
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static RunOnServer addLdapUser(String username, String firstName, String lastName, String email) {
        return session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapProvider, realm, username, firstName, lastName, email, null, "4578");
        };
    }
}
