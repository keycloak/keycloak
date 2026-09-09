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

package org.keycloak.tests.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.resource.AttackDetectionResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RealmRepresentation.BruteForceLockPolicy;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class AttackDetectionUserPropertyTest {

    private static final String DEPARTMENT = "department";
    private static final String ID = "id";
    private static final String SALES = "sales";
    private static final String ENGINEERING = "engineering";

    @InjectRealm(config = BruteForceByPropertyRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectUser(config = SalesUserConfig.class)
    ManagedUser salesUser;

    @InjectUser(ref = "otherSalesUser", config = OtherSalesUserConfig.class)
    ManagedUser otherSalesUser;

    @InjectUser(ref = "engineeringUser", config = EngineeringUserConfig.class)
    ManagedUser engineeringUser;

    @InjectOAuthClient
    OAuthClient oauthClient;

    @BeforeEach
    public void resetUsersAndFailures() {
        enableUnmanagedAttributes();
        resetUser(salesUser, SALES);
        resetUser(otherSalesUser, SALES);
        resetUser(engineeringUser, ENGINEERING);
        managedRealm.admin().attackDetection().clearAllBruteForce();
    }

    /**
     * The department attribute is only persisted once the realm accepts unmanaged attributes,
     * which new realms reject by default.
     */
    private void enableUnmanagedAttributes() {
        UserProfileResource userProfile = managedRealm.admin().users().userProfile();
        UPConfig config = userProfile.getConfiguration();
        if (UnmanagedAttributePolicy.ENABLED != config.getUnmanagedAttributePolicy()) {
            config.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
            userProfile.update(config);
        }
    }

    /**
     * Clearing login failures does not lift a permanent lockout, so the account has to be
     * re-enabled explicitly to keep a lockout from leaking into the next test.
     */
    private static void resetUser(ManagedUser user, String department) {
        UserRepresentation rep = user.admin().toRepresentation();
        rep.setEnabled(true);
        rep.singleAttribute(DEPARTMENT, department);
        if (rep.getAttributes() != null) {
            rep.getAttributes().remove(UserModel.DISABLED_REASON);
        }
        user.admin().update(rep);
    }

    @Test
    public void propertiesPolicyLocksUsersSharingAProperty() {
        AttackDetectionResource detection = managedRealm.admin().attackDetection();

        failLogin(salesUser, 3);

        assertEquals(Set.of(UserModel.EMAIL, DEPARTMENT),
                properties(detection.bruteForceUserStatus(salesUser.getId())).keySet());
        assertPropertyLocked(detection, salesUser, "email", true);
        assertPropertyLocked(detection, salesUser, "department", true);
        assertTrue((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
        assertPropertyLocked(detection, otherSalesUser, "email", false);
        assertPropertyLocked(detection, otherSalesUser, "department", true);
        assertTrue((Boolean) detection.bruteForceUserStatus(otherSalesUser.getId()).get("disabled"));
        assertPropertyLocked(detection, engineeringUser, "email", false);
        assertPropertyLocked(detection, engineeringUser, "department", false);
        assertFalse((Boolean) detection.bruteForceUserStatus(engineeringUser.getId()).get("disabled"));
    }

    @ParameterizedTest
    @ValueSource(strings = {ID, UserModel.USERNAME, UserModel.EMAIL})
    public void locksOnlyByConfiguredUniqueBuiltInProperty(String property) {
        withProtectedProperties(List.of(property), () -> {
            failLogin(salesUser, 3);
            assertUniquePropertyLock(property, salesUser, otherSalesUser, engineeringUser);
        });
    }

    @Test
    public void locksUsersSharingAConfiguredLastName() {
        withProtectedProperties(List.of(UserModel.LAST_NAME), () -> {
            AttackDetectionResource detection = managedRealm.admin().attackDetection();
            failLogin(salesUser, 3);

            assertEquals(Set.of(UserModel.LAST_NAME),
                    properties(detection.bruteForceUserStatus(salesUser.getId())).keySet());
            assertPropertyLocked(detection, salesUser, UserModel.LAST_NAME, true);
            assertTrue((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
            assertPropertyLocked(detection, engineeringUser, UserModel.LAST_NAME, true);
            assertTrue((Boolean) detection.bruteForceUserStatus(engineeringUser.getId()).get("disabled"));
            assertPropertyLocked(detection, otherSalesUser, UserModel.LAST_NAME, false);
            assertFalse((Boolean) detection.bruteForceUserStatus(otherSalesUser.getId()).get("disabled"));
        });
    }

    @Test
    public void locksUsersSharingAConfiguredFirstName() {
        withProtectedProperties(List.of(UserModel.FIRST_NAME), () -> {
            String original = otherSalesUser.admin().toRepresentation().getFirstName();
            setFirstName(otherSalesUser, salesUser.admin().toRepresentation().getFirstName());
            try {
                AttackDetectionResource detection = managedRealm.admin().attackDetection();
                failLogin(salesUser, 3);

                assertEquals(Set.of(UserModel.FIRST_NAME),
                        properties(detection.bruteForceUserStatus(salesUser.getId())).keySet());
                assertPropertyLocked(detection, salesUser, UserModel.FIRST_NAME, true);
                assertPropertyLocked(detection, otherSalesUser, UserModel.FIRST_NAME, true);
                assertTrue((Boolean) detection.bruteForceUserStatus(otherSalesUser.getId()).get("disabled"));
                assertPropertyLocked(detection, engineeringUser, UserModel.FIRST_NAME, false);
            } finally {
                setFirstName(otherSalesUser, original);
            }
        });
    }

    @Test
    public void locksOnlyByConfiguredCustomProperty() {
        withProtectedProperties(List.of(DEPARTMENT), () -> {
            AttackDetectionResource detection = managedRealm.admin().attackDetection();
            failLogin(salesUser, 3);

            assertEquals(Set.of(DEPARTMENT),
                    properties(detection.bruteForceUserStatus(salesUser.getId())).keySet());
            assertPropertyLocked(detection, salesUser, DEPARTMENT, true);
            assertTrue((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
            assertPropertyLocked(detection, otherSalesUser, DEPARTMENT, true);
            assertTrue((Boolean) detection.bruteForceUserStatus(otherSalesUser.getId()).get("disabled"));
            assertPropertyLocked(detection, engineeringUser, DEPARTMENT, false);
            assertFalse((Boolean) detection.bruteForceUserStatus(engineeringUser.getId()).get("disabled"));
        });
    }

    @Test
    public void unlocksOnlyTheRequestedProperty() {
        AttackDetectionResource detection = managedRealm.admin().attackDetection();
        failLogin(salesUser, 3);

        detection.clearBruteForceForUserByProperty(salesUser.getId(), "email");

        assertPropertyLocked(detection, salesUser, "email", false);
        assertPropertyLocked(detection, salesUser, "department", true);
        assertTrue((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
        assertPropertyLocked(detection, otherSalesUser, "department", true);

        detection.clearBruteForceForUserByProperty(salesUser.getId(), "department");

        assertPropertyLocked(detection, salesUser, "department", false);
        assertPropertyLocked(detection, otherSalesUser, "department", false);
        assertFalse((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
    }

    @Test
    public void clearForUserWithoutPropertyStillClearsEveryProperty() {
        AttackDetectionResource detection = managedRealm.admin().attackDetection();
        failLogin(salesUser, 3);

        detection.clearBruteForceForUser(salesUser.getId());

        assertPropertyLocked(detection, salesUser, "email", false);
        assertPropertyLocked(detection, salesUser, "department", false);
        assertPropertyLocked(detection, otherSalesUser, "department", false);
    }

    @Test
    public void rejectsUnlockForAPropertyThatIsNotProtected() {
        AttackDetectionResource detection = managedRealm.admin().attackDetection();

        assertThrows(BadRequestException.class,
                () -> detection.clearBruteForceForUserByProperty(salesUser.getId(), "phoneNumber"));
    }

    @Test
    public void exposesIndependentStatusForEveryConfiguredProperty() {
        Map<String, Object> status = managedRealm.admin().attackDetection()
                .bruteForceUserStatus(salesUser.getId());
        Map<String, Map<String, Object>> properties = properties(status);

        assertEquals(2, properties.size());
        assertEquals(0, properties.get("email").get("numFailures"));
        assertEquals(0, properties.get("department").get("numFailures"));
        assertFalse((Boolean) properties.get("email").get("disabled"));
        assertFalse((Boolean) properties.get("department").get("disabled"));
    }

    @Test
    public void persistsConfiguredPropertiesInTheRealmRepresentation() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        assertEquals(List.of("email", "department"), realm.getBruteForceProtectedUserProperties());
        assertEquals(BruteForceLockPolicy.PROPERTIES, realm.getBruteForceLockPolicy());
    }

    @Test
    public void persistsAllLockPoliciesInTheRealmRepresentation() {
        for (BruteForceLockPolicy policy : BruteForceLockPolicy.values()) {
            withLockPolicy(policy, () -> assertEquals(policy,
                    managedRealm.admin().toRepresentation().getBruteForceLockPolicy()));
        }
    }

    @Test
    public void userPolicyLocksOnlyTheFailedAccount() {
        withLockPolicy(BruteForceLockPolicy.USER, () -> {
            AttackDetectionResource detection = managedRealm.admin().attackDetection();
            failLogin(salesUser, 3);

            assertEquals(Set.of(ID), properties(detection.bruteForceUserStatus(salesUser.getId())).keySet());
            assertPropertyLocked(detection, salesUser, ID, true);
            assertTrue((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
            assertPropertyLocked(detection, otherSalesUser, ID, false);
            assertFalse((Boolean) detection.bruteForceUserStatus(otherSalesUser.getId()).get("disabled"));
        });
    }

    @Test
    public void anyPolicyLocksByUserIdOrSharedProperty() {
        withLockPolicy(BruteForceLockPolicy.ANY, () -> {
            AttackDetectionResource detection = managedRealm.admin().attackDetection();
            failLogin(salesUser, 3);

            assertEquals(Set.of(ID, UserModel.EMAIL, DEPARTMENT),
                    properties(detection.bruteForceUserStatus(salesUser.getId())).keySet());
            assertPropertyLocked(detection, salesUser, ID, true);
            assertPropertyLocked(detection, salesUser, UserModel.EMAIL, true);
            assertPropertyLocked(detection, salesUser, DEPARTMENT, true);
            assertTrue((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
            assertPropertyLocked(detection, otherSalesUser, ID, false);
            assertPropertyLocked(detection, otherSalesUser, DEPARTMENT, true);
            assertTrue((Boolean) detection.bruteForceUserStatus(otherSalesUser.getId()).get("disabled"));
            assertPropertyLocked(detection, engineeringUser, ID, false);
            assertPropertyLocked(detection, engineeringUser, DEPARTMENT, false);
            assertFalse((Boolean) detection.bruteForceUserStatus(engineeringUser.getId()).get("disabled"));

            detection.clearBruteForceForUserByProperty(salesUser.getId(), DEPARTMENT);
            assertTrue((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
            assertFalse((Boolean) detection.bruteForceUserStatus(otherSalesUser.getId()).get("disabled"));

            detection.clearBruteForceForUserByProperty(salesUser.getId(), UserModel.EMAIL);
            assertTrue((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
            detection.clearBruteForceForUserByProperty(salesUser.getId(), ID);
            assertFalse((Boolean) detection.bruteForceUserStatus(salesUser.getId()).get("disabled"));
        });
    }

    @Test
    public void successfulLoginClearsEveryConfiguredPropertyCounter() {
        AttackDetectionResource detection = managedRealm.admin().attackDetection();
        failLogin(salesUser, 1);

        assertPropertyFailures(detection, salesUser, "email", 1);
        assertPropertyFailures(detection, salesUser, "department", 1);

        oauthClient.doPasswordGrantRequest(salesUser.getUsername(), salesUser.getPassword());

        assertPropertyFailures(detection, salesUser, "email", 0);
        assertPropertyFailures(detection, salesUser, "department", 0);
    }

    @Test
    public void permanentLockoutIsReleasedOnlyAfterEveryLockedPropertyIsUnlocked() {
        AttackDetectionResource detection = managedRealm.admin().attackDetection();
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setPermanentLockout(true);
        realm.setMaxTemporaryLockouts(0);
        managedRealm.admin().update(realm);

        try {
            failLogin(salesUser, 3);
            assertFalse(salesUser.admin().toRepresentation().isEnabled());
            assertPropertyLocked(detection, otherSalesUser, DEPARTMENT, true);
            assertTrue((Boolean) detection.bruteForceUserStatus(otherSalesUser.getId()).get("disabled"));

            detection.clearBruteForceForUserByProperty(salesUser.getId(), UserModel.EMAIL);
            assertFalse(salesUser.admin().toRepresentation().isEnabled());
            assertPropertyLocked(detection, salesUser, DEPARTMENT, true);
            assertPropertyLocked(detection, otherSalesUser, DEPARTMENT, true);

            detection.clearBruteForceForUserByProperty(salesUser.getId(), DEPARTMENT);
            assertTrue(salesUser.admin().toRepresentation().isEnabled());
            assertPropertyLocked(detection, otherSalesUser, DEPARTMENT, false);
            assertFalse((Boolean) detection.bruteForceUserStatus(otherSalesUser.getId()).get("disabled"));
        } finally {
            realm.setPermanentLockout(false);
            managedRealm.admin().update(realm);
        }
    }

    private void withProtectedProperties(List<String> properties, Runnable test) {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        List<String> previous = List.copyOf(realm.getBruteForceProtectedUserProperties());
        realm.setBruteForceProtectedUserProperties(properties);
        managedRealm.admin().update(realm);
        try {
            test.run();
        } finally {
            RealmRepresentation restore = managedRealm.admin().toRepresentation();
            restore.setBruteForceProtectedUserProperties(previous);
            managedRealm.admin().update(restore);
        }
    }

    private void withLockPolicy(BruteForceLockPolicy policy, Runnable test) {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        BruteForceLockPolicy previous = realm.getBruteForceLockPolicy();
        realm.setBruteForceLockPolicy(policy);
        managedRealm.admin().update(realm);
        try {
            test.run();
        } finally {
            RealmRepresentation restore = managedRealm.admin().toRepresentation();
            restore.setBruteForceLockPolicy(previous);
            managedRealm.admin().update(restore);
        }
    }

    private static void setFirstName(ManagedUser user, String firstName) {
        UserRepresentation rep = user.admin().toRepresentation();
        rep.setFirstName(firstName);
        user.admin().update(rep);
    }

    private void assertUniquePropertyLock(String property, ManagedUser locked, ManagedUser... unlocked) {
        AttackDetectionResource detection = managedRealm.admin().attackDetection();
        assertEquals(Set.of(property), properties(detection.bruteForceUserStatus(locked.getId())).keySet());
        assertPropertyLocked(detection, locked, property, true);
        assertTrue((Boolean) detection.bruteForceUserStatus(locked.getId()).get("disabled"));
        for (ManagedUser user : unlocked) {
            assertPropertyLocked(detection, user, property, false);
            assertFalse((Boolean) detection.bruteForceUserStatus(user.getId()).get("disabled"));
        }
    }

    private void failLogin(ManagedUser user, int attempts) {
        AttackDetectionResource detection = managedRealm.admin().attackDetection();
        for (int i = 0; i < attempts; i++) {
            oauthClient.doPasswordGrantRequest(user.getUsername(), "invalid");
            int expected = i + 1;
            await().atMost(5, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        Map<String, Object> status = detection.bruteForceUserStatus(user.getId());
                        int failures = (Integer) status.get("numFailures");
                        boolean disabled = Boolean.TRUE.equals(status.get("disabled"));
                        assertTrue(failures >= expected || disabled);
                    });
        }
    }

    private static void assertPropertyLocked(AttackDetectionResource detection, ManagedUser user,
            String property, boolean expected) {
        Map<String, Object> status = detection.bruteForceUserStatus(user.getId());
        assertEquals(expected, properties(status).get(property).get("disabled"));
    }

    private static void assertPropertyFailures(AttackDetectionResource detection, ManagedUser user,
            String property, int expected) {
        Map<String, Object> status = detection.bruteForceUserStatus(user.getId());
        assertEquals(expected, properties(status).get(property).get("numFailures"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> properties(Map<String, Object> status) {
        return (Map<String, Map<String, Object>>) status.get("properties");
    }

    public static class BruteForceByPropertyRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.bruteForceProtected(true)
                    .failureFactor(2)
                    .bruteForceLockPolicy(BruteForceLockPolicy.PROPERTIES)
                    .bruteForceProtectedUserProperties(UserModel.EMAIL, DEPARTMENT);
        }
    }

    public static class SalesUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("sales-user")
                    .name("Sales", "User")
                    .email("sales-user@example.com")
                    .emailVerified(true)
                    .password("password");
        }
    }

    public static class OtherSalesUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("other-sales-user")
                    .name("Other", "Sales")
                    .email("other-sales-user@example.com")
                    .emailVerified(true)
                    .password("password");
        }
    }

    public static class EngineeringUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("engineering-user")
                    .name("Engineering", "User")
                    .email("engineering-user@example.com")
                    .emailVerified(true)
                    .password("password");
        }
    }
}
