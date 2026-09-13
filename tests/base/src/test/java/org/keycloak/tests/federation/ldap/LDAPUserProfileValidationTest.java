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
import java.net.BindException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.SynchronizationResultRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.HardcodedAttributeMapper;
import org.keycloak.storage.ldap.mappers.HardcodedAttributeMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
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
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.util.ldap.LDAPEmbeddedServer;
import org.keycloak.validate.validators.PatternValidator;

import org.apache.directory.api.ldap.model.exception.LdapConfigurationException;
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
        int ldapPort = startLdapEmbeddedServer();

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
        // Explicitly remove the LDAP component rather than relying solely on realm teardown to cascade-delete it:
        // this class doesn't request a GLOBAL-lifecycle realm today, but if that ever changed the realm (and this
        // component with it) would outlive the class and leak into whatever test runs next.
        //
        // Both steps below are attempted unconditionally - one failing must not stop the embedded server from
        // still being asked to shut down (or vice versa) - but any failure is still rethrown afterward so cleanup
        // problems show up as a visible test failure rather than being silently swallowed.
        Exception failure = null;

        if (ldapModelId != null) {
            try {
                managedRealm.admin().components().component(ldapModelId).remove();
            } catch (Exception e) {
                failure = e;
            }
            ldapModelId = null;
        }

        if (ldapEmbeddedServer != null) {
            try {
                ldapEmbeddedServer.stop();
            } catch (Exception e) {
                if (failure != null) {
                    failure.addSuppressed(e);
                } else {
                    failure = e;
                }
            }
            ldapEmbeddedServer = null;
        }

        if (failure != null) {
            throw failure;
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
    public void testImportUserWithReadOnlyMapperAttributeFailureCannotBeFixedByUser() {
        final String username = "readonlymapperuser";
        // Same "person-name-prohibited-characters" firstName failure as
        // testImportUserWithEditableAttributeFailureGetsRequiredAction, but here firstName's mapper is individually
        // configured read-only even though the provider itself is WRITABLE: an edit made through UPDATE_PROFILE
        // would never actually reach LDAP and would just be overwritten again with the invalid value on the next
        // import, so this must be a hard rejection rather than a soft-fixable required action.
        setFirstNameMapperReadOnly(true);
        try {
            runOnServer.run(addLdapUser(username, "Invalid<b>Name", "ValidLastName", "readonly-mapper-user@example.org"));

            List<UserRepresentation> found = managedRealm.admin().users().search(username, true);
            Assertions.assertTrue(found.isEmpty(),
                    "The user should have been rejected: firstName cannot really be fixed by the user since its mapper is read-only.");
        } finally {
            setFirstNameMapperReadOnly(false);
        }
    }

    @Test
    public void testUnsyncedEditModeDoesNotTreatMapperReadOnlyAttributeAsUnfixable() {
        final String username = "unsyncedmapperuser";
        // Same setup as testImportUserWithReadOnlyMapperAttributeFailureCannotBeFixedByUser (firstName's mapper
        // individually configured read-only), but with the provider itself UNSYNCED rather than WRITABLE.
        // isUserAttributeReadOnly() (see LDAPStorageMapper) is documented to be consulted only while the
        // provider is WRITABLE: under UNSYNCED, LDAPStorageProviderFactory sets read.only=true on every default
        // mapper - that only means "not written back to LDAP", which is true of every attribute under UNSYNCED
        // by definition, since edits are still genuinely persisted in Keycloak, just never pushed back to LDAP.
        // So this must still be a soft-fixable required action, not a hard rejection.
        setEditMode(UserStorageProvider.EditMode.UNSYNCED);
        setFirstNameMapperReadOnly(true);
        try {
            runOnServer.run(addLdapUser(username, "Invalid<b>Name", "ValidLastName", "unsynced-mapper-user@example.org"));

            List<UserRepresentation> found = managedRealm.admin().users().search(username, true);
            Assertions.assertEquals(1, found.size(),
                    "The user should have been imported: under UNSYNCED, firstName can still be fixed by the "
                            + "user through Keycloak even though its mapper reports read-only.");
            Assertions.assertTrue(found.get(0).getRequiredActions().contains(UserModel.RequiredAction.UPDATE_PROFILE.name()),
                    "The user should have been given the UPDATE_PROFILE required action.");
        } finally {
            setFirstNameMapperReadOnly(false);
            setEditMode(UserStorageProvider.EditMode.WRITABLE);
        }
    }

    @Test
    public void testExistingInvalidReadOnlyAttributeOnlyValidatedWhenOptedIn() {
        final String username = "readonlybypassuser<em>";
        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(false));

        // Import with validation disabled: the invalid username is let through untouched, exactly as it would be
        // for an LDAP user imported before this feature existed, or on a realm that never opts in.
        setValidateUserProfile(false);
        try {
            runOnServer.run(addLdapUser(username, "Valid", "User", "readonly-bypass-user@example.org"));

            List<UserRepresentation> found = managedRealm.admin().users().search(username, true);
            Assertions.assertEquals(1, found.size(), "Sanity check: import must succeed while validation is disabled.");

            // The username is now read-only (editUsernameAllowed=false) and invalid, but unchanged. Validating the
            // profile again - e.g. as part of some unrelated profile update - must still forgive it via the legacy
            // read-only bypass while VALIDATE_USER_PROFILE stays disabled, or the user would be stuck in an
            // unfixable required-action loop with no relation to LDAP import at all.
            Assertions.assertFalse(validateUserProfileThrows(username),
                    "An invalid read-only value must still be forgiven when VALIDATE_USER_PROFILE is disabled.");

            // Opting in afterward activates decorateUserProfile()'s override, so the very same already-invalid
            // value now fails validation - proving the bypass really was in effect a moment ago, not just
            // coincidentally passing.
            setValidateUserProfile(true);
            Assertions.assertTrue(validateUserProfileThrows(username),
                    "Once opted in, the same read-only invalid value should no longer be forgiven.");
        } finally {
            setValidateUserProfile(true); // restore the class-wide default relied on by every other test
        }
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

    @Test
    public void testReadOnlyModeDefeatsBypassForUnmappedRequiredAttribute() {
        final String username = "readonlyunmappedrequser";
        final String customAttribute = "department";

        // "department" has no LDAP mapper at all - its value is never established from LDAP, and it stays unset
        // on every user imported below. It is required here purely via the realm's User Profile configuration.
        UserProfileResource upResource = managedRealm.admin().users().userProfile();
        UPConfig originalUpConfig = upResource.getConfiguration();
        try {
            UPConfig upConfig = upResource.getConfiguration();
            upConfig.addOrReplaceAttribute(new UPAttribute(customAttribute,
                    new UPAttributePermissions(Set.of("user", "admin"), Set.of("user", "admin")),
                    new UPAttributeRequired(Set.of(), Set.of())));
            upResource.update(upConfig);

            // Under READ_ONLY, decorateUserProfile() makes every profile attribute read-only for this provider's
            // users - including "department", which has no LDAP mapper of its own. Before the fix, only attributes
            // an LDAP mapper explicitly targets had their read-only bypass defeated, so a missing (and therefore
            // "unchanged empty") required attribute like this one would still be silently forgiven.
            setEditMode(UserStorageProvider.EditMode.READ_ONLY);
            try {
                runOnServer.run(addLdapUser(username, "Valid", "User", "readonly-unmapped-required@example.org"));

                List<UserRepresentation> found = managedRealm.admin().users().search(username, true);
                Assertions.assertTrue(found.isEmpty(),
                        "The user should have been rejected: '" + customAttribute + "' is required, has no LDAP "
                                + "mapper, and is unset - it must not be forgiven just because it's read-only and "
                                + "unchanged under a READ_ONLY provider with VALIDATE_USER_PROFILE enabled.");
            } finally {
                setEditMode(UserStorageProvider.EditMode.WRITABLE); // restore the class-wide default
            }
        } finally {
            upResource.update(originalUpConfig);
        }
    }

    @Test
    public void testHardcodedAttributeMapperInvalidValueCannotBeFixedByUser() {
        final String username = "hardcodedmapperuser";
        final String customAttribute = "hardcodeddept";
        final String invalidHardcodedValue = "not-digits";

        // A digits-only pattern that the hardcoded value below deliberately violates - any User Profile validator
        // would do, this one just makes the failure trivial to force.
        UserProfileResource upResource = managedRealm.admin().users().userProfile();
        UPConfig originalUpConfig = upResource.getConfiguration();
        try {
            UPConfig upConfig = upResource.getConfiguration();
            UPAttribute attribute = new UPAttribute(customAttribute,
                    new UPAttributePermissions(Set.of("user", "admin"), Set.of("user", "admin")),
                    new UPAttributeRequired(Set.of(), Set.of()));
            attribute.addValidation(PatternValidator.ID, Map.of(PatternValidator.CFG_PATTERN, "^[0-9]+$"));
            upConfig.addOrReplaceAttribute(attribute);
            upResource.update(upConfig);

            // HardcodedAttributeMapper.onImportUserFromLDAP() unconditionally overwrites this attribute with
            // invalidHardcodedValue on every single import - a UPDATE_PROFILE fix would be silently discarded
            // again on the very next import, so this must be a hard rejection, exactly like an individually
            // read-only UserAttributeLDAPStorageMapper attribute (see
            // testImportUserWithReadOnlyMapperAttributeFailureCannotBeFixedByUser above).
            addHardcodedAttributeMapper(customAttribute, invalidHardcodedValue);
            try {
                runOnServer.run(addLdapUser(username, "Valid", "User", "hardcoded-mapper-user@example.org"));

                List<UserRepresentation> found = managedRealm.admin().users().search(username, true);
                Assertions.assertTrue(found.isEmpty(),
                        "The user should have been rejected: '" + customAttribute + "' is hardcoded to an "
                                + "invalid value by HardcodedAttributeMapper, and the user can never actually fix "
                                + "it - any edit is discarded again on the next import.");
            } finally {
                removeHardcodedAttributeMapper();
            }
        } finally {
            upResource.update(originalUpConfig);
        }
    }

    @Test
    public void testHardcodedAttributeMapperNotSearchableInLdap() {
        final String username = "hardcodedsearchuser";
        final String customAttribute = "hardcodedsearchdept";
        final String hardcodedValue = "12345";

        addHardcodedAttributeMapper(customAttribute, hardcodedValue);
        try {
            runOnServer.run(addLdapUser(username, "Valid", "User", "hardcoded-search-user@example.org"));

            List<UserRepresentation> found = managedRealm.admin().users().search(username, true);
            Assertions.assertEquals(1, found.size(), "Sanity check: user should have been imported with a valid hardcoded value.");

            // HardcodedAttributeMapper.onImportUserFromLDAP() sets this attribute directly on the Keycloak user -
            // it has no backing LDAP attribute of its own and is never read from LDAP. Searching by it must be
            // treated as unmapped (exactly like an attribute with no LDAP mapper at all) rather than turned into
            // an LDAP filter against a non-existent (or unrelated) LDAP attribute, which would either fail
            // outright against the LDAP schema or silently match unrelated data.
            runOnServer.run(session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
                LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
                List<UserModel> results = ldapProvider.searchForUserStream(realm, Map.of(customAttribute, hardcodedValue), null, null).toList();
                Assertions.assertTrue(results.isEmpty(),
                        "Searching by '" + customAttribute + "' must not be treated as an LDAP-searchable attribute.");
            });
        } finally {
            removeHardcodedAttributeMapper();
        }
    }

    @Test
    public void testHardcodedAttributeMapperModelPropertyNotAddedAsUserProfileAttribute() {
        final String username = "hardcodedenableduser";

        addHardcodedAttributeMapper("enabled", "true");
        try {
            runOnServer.run(addLdapUser(username, "Valid", "User", "hardcoded-enabled-user@example.org"));

            List<UserRepresentation> found = managedRealm.admin().users().search(username, true);
            Assertions.assertEquals(1, found.size(), "Sanity check: user should have been imported.");

            // "enabled" is a UserModel bean property, not a real User Profile attribute - User Profile has no
            // concept of it. Exposing it via getUserProfileAttributes() would make decorateUserProfile() fabricate
            // a bogus, disconnected "enabled" attribute whose edits would just write a plain custom attribute
            // instead of actually enabling/disabling the user.
            runOnServer.run(session -> {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, username);
                UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);
                UserProfile profile = profileProvider.create(UserProfileContext.UPDATE_PROFILE, user);
                Assertions.assertFalse(profile.getAttributes().nameSet().contains("enabled"),
                        "'enabled' must not be exposed as a User Profile attribute by HardcodedAttributeMapper.");
            });
        } finally {
            removeHardcodedAttributeMapper();
        }
    }

    private void addHardcodedAttributeMapper(String userModelAttribute, String attributeValue) {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            ComponentModel mapperModel = KeycloakModelUtils.createComponentModel("hardcodedAttributeMapper",
                    ldapModel.getId(), HardcodedAttributeMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    HardcodedAttributeMapper.USER_MODEL_ATTRIBUTE, userModelAttribute,
                    HardcodedAttributeMapper.ATTRIBUTE_VALUE, attributeValue);
            realm.addComponentModel(mapperModel);
        });
    }

    private void removeHardcodedAttributeMapper() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            realm.getComponentsStream(ldapModel.getId(), LDAPStorageMapper.class.getName())
                    .filter(m -> "hardcodedAttributeMapper".equals(m.getName()))
                    .findFirst()
                    .ifPresent(realm::removeComponent);
        });
    }

    private static final int MAX_LDAP_BIND_ATTEMPTS = 5;

    // Probing for a free port and then closing the socket before the embedded server binds to it is inherently
    // racy: something else can claim that exact port in between. Rather than try to close that window (there is no
    // way to reserve a port for another process to bind to later), retry with a newly-probed port whenever the
    // failure we get back is actually that race and not a real startup problem.
    private static int startLdapEmbeddedServer() throws Exception {
        for (int attempt = 1; ; attempt++) {
            int port = findFreePort();

            Properties serverProperties = new Properties();
            serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
            serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "false");
            serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_PORT, String.valueOf(port));

            LDAPEmbeddedServer server = new LDAPEmbeddedServer(serverProperties);
            server.init();
            try {
                server.start();
                ldapEmbeddedServer = server;
                return port;
            } catch (LdapConfigurationException e) {
                // init() already created a directory service (and its work files) before start() ever attempted
                // to bind, so a server we're abandoning here - whether we're about to retry or about to rethrow -
                // still needs to be stopped, or that gets leaked for the rest of the test run.
                try {
                    server.stop();
                } catch (Exception cleanupException) {
                    e.addSuppressed(cleanupException);
                }
                if (attempt >= MAX_LDAP_BIND_ATTEMPTS || !isBindConflict(e)) {
                    throw e;
                }
            }
        }
    }

    private static boolean isBindConflict(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof BindException) {
                return true;
            }
        }
        return false;
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

    // The "first name" mapper (like every default mapper LDAPStorageProviderFactory#onCreate creates) is shared
    // across test methods, so this is always reverted in a finally block to avoid leaking into whichever test runs
    // next.
    private void setFirstNameMapperReadOnly(boolean readOnly) {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            ComponentModel firstNameMapper = realm.getComponentsStream(ldapModel.getId(), LDAPStorageMapper.class.getName())
                    .filter(m -> "first name".equals(m.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Expected a 'first name' LDAP mapper to already exist"));
            firstNameMapper.getConfig().putSingle(UserAttributeLDAPStorageMapper.READ_ONLY, String.valueOf(readOnly));
            realm.updateComponent(firstNameMapper);
        });
    }

    private void setEditMode(UserStorageProvider.EditMode editMode) {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            ldapModel.getConfig().putSingle(LDAPConstants.EDIT_MODE, editMode.name());
            realm.updateComponent(ldapModel);
        });
    }

    private void setValidateUserProfile(boolean enabled) {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            ldapModel.getConfig().putSingle(LDAPConstants.VALIDATE_USER_PROFILE, String.valueOf(enabled));
            realm.updateComponent(ldapModel);
        });
    }

    private boolean validateUserProfileThrows(String username) {
        return runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);
            UserProfile profile = profileProvider.create(UserProfileContext.UPDATE_PROFILE, user);
            try {
                profile.validate();
                return false;
            } catch (ValidationException e) {
                return true;
            }
        }, Boolean.class);
    }
}
