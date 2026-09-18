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
package org.keycloak.services.managers;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation.BruteForceLockPolicy;

import org.junit.Assert;
import org.junit.Test;

public class BruteForceUserPropertyTest {

    @Test
    public void propertyKeysDifferByPropertyName() {
        Assert.assertNotEquals(
                BruteForceUserProperty.propertyKey("email", "same-value"),
                BruteForceUserProperty.propertyKey("username", "same-value"));
    }

    @Test
    public void propertyKeysAreStable() {
        Assert.assertEquals(
                BruteForceUserProperty.propertyKey("department", "sales"),
                BruteForceUserProperty.propertyKey("department", "sales"));
    }

    @Test
    public void emailAndPhoneNumberUseDifferentCounters() {
        Assert.assertNotEquals(
                BruteForceUserProperty.propertyKey("email", "user@example.com"),
                BruteForceUserProperty.propertyKey("phoneNumber", "user@example.com"));
    }

    @Test
    public void defaultsToUserIdWhenNoPropertiesAreConfigured() {
        RealmModel realm = realm();
        UserModel user = user("user-id", "UserName", "User@Example.com", Map.of());

        Assert.assertEquals(List.of(BruteForceUserProperty.ID),
                BruteForceUserProperty.getProtectedProperties(realm));
        Assert.assertEquals(List.of("user-id"), BruteForceUserProperty.getFailureKeys(realm, user));
    }

    @Test
    public void userPolicyIgnoresConfiguredProperties() {
        RealmModel realm = realm(BruteForceLockPolicy.USER, "email", "phoneNumber");
        UserModel user = user("user-id", "UserName", "User@Example.com",
                Map.of("phoneNumber", List.of("+1-555-0100")));

        Assert.assertEquals(List.of(BruteForceUserProperty.ID),
                BruteForceUserProperty.getProtectedProperties(realm));
        Assert.assertEquals(List.of("user-id"), BruteForceUserProperty.getFailureKeys(realm, user));
    }

    @Test
    public void propertiesPolicyUsesOnlyConfiguredProperties() {
        RealmModel realm = realm(BruteForceLockPolicy.PROPERTIES, "email", "phoneNumber");
        UserModel user = user("user-id", "UserName", "User@Example.com",
                Map.of("phoneNumber", List.of("+1-555-0100")));

        Assert.assertEquals(List.of("email", "phoneNumber"),
                BruteForceUserProperty.getProtectedProperties(realm));
        Assert.assertEquals(List.of(
                BruteForceUserProperty.propertyKey("email", "user@example.com"),
                BruteForceUserProperty.propertyKey("phoneNumber", "+1-555-0100")),
                BruteForceUserProperty.getFailureKeys(realm, user));
    }

    @Test
    public void propertiesPolicyWithNoPropertiesFallsBackToUserId() {
        RealmModel realm = realm(BruteForceLockPolicy.PROPERTIES);
        UserModel user = user("user-id", "UserName", "User@Example.com", Map.of());

        Assert.assertEquals(List.of(BruteForceUserProperty.ID),
                BruteForceUserProperty.getProtectedProperties(realm));
        Assert.assertEquals(List.of("user-id"), BruteForceUserProperty.getFailureKeys(realm, user));
        Assert.assertEquals(List.of("user-id"),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "UserName"));
    }

    @Test
    public void propertiesPolicyWithIdUsesUserCounterForAnySubmittedIdentifier() {
        RealmModel realm = realm(BruteForceLockPolicy.PROPERTIES, BruteForceUserProperty.ID);
        UserModel user = user("user-id", "UserName", "User@Example.com", Map.of());

        Assert.assertEquals(List.of("user-id"),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "UserName"));
        Assert.assertEquals(List.of("user-id"),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "User@Example.com"));
    }

    @Test
    public void propertiesPolicyFallsBackToUsernameWithoutSubmittedIdentifier() {
        RealmModel realm = realm(BruteForceLockPolicy.PROPERTIES, "username");
        UserModel user = user("user-id", "UserName", "User@Example.com", Map.of());
        String usernameKey = BruteForceUserProperty.propertyKey("username", "username");

        Assert.assertEquals(List.of(usernameKey),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, null));
        Assert.assertEquals(List.of(usernameKey),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, " "));
    }

    @Test
    public void anyPolicyIncludesUserIdAndConfiguredProperties() {
        RealmModel realm = realm(BruteForceLockPolicy.ANY, "email", "id");
        UserModel user = user("user-id", "UserName", "User@Example.com", Map.of());

        Assert.assertEquals(List.of(BruteForceUserProperty.ID, "email"),
                BruteForceUserProperty.getProtectedProperties(realm));
        Assert.assertEquals(List.of("user-id",
                BruteForceUserProperty.propertyKey("email", "user@example.com")),
                BruteForceUserProperty.getFailureKeys(realm, user));
    }

    @Test
    public void anyPolicyWithNoPropertiesUsesOnlyUserId() {
        RealmModel realm = realm(BruteForceLockPolicy.ANY);
        UserModel user = user("user-id", "UserName", "User@Example.com", Map.of());

        Assert.assertEquals(List.of(BruteForceUserProperty.ID),
                BruteForceUserProperty.getProtectedProperties(realm));
        Assert.assertEquals(List.of("user-id"), BruteForceUserProperty.getFailureKeys(realm, user));
    }

    @Test
    public void attemptIncrementsOnlyTheMatchingIdentifier() {
        RealmModel realm = realm(BruteForceLockPolicy.PROPERTIES, "username", "email", "department");
        UserModel user = user("user-id", "UserName", "User@Example.com",
                Map.of("department", List.of("sales")));

        Assert.assertEquals(List.of(BruteForceUserProperty.propertyKey("username", "username")),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "UserName"));
        Assert.assertEquals(List.of(BruteForceUserProperty.propertyKey("email", "user@example.com")),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "User@Example.com"));
        Assert.assertEquals(List.of(BruteForceUserProperty.propertyKey("department", "sales")),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "sales"));
        Assert.assertEquals(List.of(), BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "other"));
    }

    @Test
    public void anyPolicyAttemptIncrementsGlobalAndMatchingIdentifier() {
        RealmModel realm = realm(BruteForceLockPolicy.ANY, "email");
        UserModel user = user("user-id", "UserName", "User@Example.com", Map.of());

        Assert.assertEquals(List.of("user-id",
                BruteForceUserProperty.propertyKey("email", "user@example.com")),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "User@Example.com"));
        Assert.assertEquals(List.of("user-id"),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "UserName"));
    }

    @Test
    public void userPolicyAttemptIgnoresTheSubmittedIdentifier() {
        RealmModel realm = realm(BruteForceLockPolicy.USER, "email");
        UserModel user = user("user-id", "UserName", "User@Example.com", Map.of());

        Assert.assertEquals(List.of("user-id"),
                BruteForceUserProperty.getFailureKeysForAttempt(realm, user, "User@Example.com"));
    }

    @Test
    public void propertyFailureFactorFallsBackToFailureFactor() {
        RealmModel realm = realm(BruteForceLockPolicy.ANY, "email");
        Assert.assertEquals(30, BruteForceUserProperty.getFailureFactor(realm, "user-id"));
        Assert.assertEquals(30, BruteForceUserProperty.getFailureFactor(realm,
                BruteForceUserProperty.propertyKey("email", "user@example.com")));
    }

    @Test
    public void propertyFailureFactorIsIndependentOfTheUserFactor() {
        RealmModel realm = realm(BruteForceLockPolicy.ANY, 30, 2, "email");
        Assert.assertEquals(30, BruteForceUserProperty.getFailureFactor(realm, "user-id"));
        Assert.assertEquals(2, BruteForceUserProperty.getFailureFactor(realm,
                BruteForceUserProperty.propertyKey("email", "user@example.com")));
    }

    @Test
    public void permanentLockoutUsesTheFactorForEachCounter() {
        RealmModel realm = realm(BruteForceLockPolicy.ANY, 30, 2, "email");
        UserLoginFailureModel twoFailures = loginFailure(2);

        Assert.assertFalse(BruteForceUserProperty.isPermanentlyLocked(realm, twoFailures, "user-id"));
        Assert.assertTrue(BruteForceUserProperty.isPermanentlyLocked(realm, twoFailures,
                BruteForceUserProperty.propertyKey("email", "user@example.com")));
    }

    @Test
    public void normalizesUsernameAndEmailCaseAndWhitespace() {
        RealmModel realm = realm("username", "email");
        UserModel user = user("user-id", "  UserName  ", "  User@Example.com  ", Map.of());

        Assert.assertEquals(List.of(
                BruteForceUserProperty.propertyKey("username", "username"),
                BruteForceUserProperty.propertyKey("email", "user@example.com")),
                BruteForceUserProperty.getFailureKeys(realm, user));
    }

    @Test
    public void customPropertiesAreCaseSensitiveAndSupportMultipleValues() {
        RealmModel realm = realm("department");
        UserModel user = user("user-id", "username", "user@example.com",
                Map.of("department", List.of(" Sales ", "sales", "Sales", "", " ")));

        Assert.assertEquals(List.of(
                BruteForceUserProperty.propertyKey("department", "Sales"),
                BruteForceUserProperty.propertyKey("department", "sales")),
                BruteForceUserProperty.getFailureKeys(realm, user, "department"));
    }

    @Test
    public void usersSharingAPropertyValueUseTheSameCounter() {
        RealmModel realm = realm("department");
        UserModel firstUser = user("first-user", "first", "first@example.com",
                Map.of("department", List.of("sales")));
        UserModel secondUser = user("second-user", "second", "second@example.com",
                Map.of("department", List.of("sales")));

        Assert.assertEquals(
                BruteForceUserProperty.getFailureKeys(realm, firstUser, "department"),
                BruteForceUserProperty.getFailureKeys(realm, secondUser, "department"));
    }

    @Test
    public void getsAndRemovesEveryFailureCounterForAUser() {
        RealmModel realm = realm(BruteForceLockPolicy.ANY, "email");
        UserModel user = user("user-id", "UserName", "User@Example.com", Map.of());
        List<String> failureKeys = BruteForceUserProperty.getFailureKeys(realm, user);
        Map<String, UserLoginFailureModel> failures = new HashMap<>();
        failureKeys.forEach(key -> failures.put(key, loginFailure(1)));
        KeycloakSession session = session(failures);

        Assert.assertEquals(2, BruteForceUserProperty.getLoginFailures(session, realm, user).count());
        Assert.assertTrue(BruteForceUserProperty.removeLoginFailures(session, realm, user));
        Assert.assertTrue(failures.isEmpty());
        Assert.assertFalse(BruteForceUserProperty.removeLoginFailures(session, realm, user));
    }

    @Test
    public void ignoresConfiguredPropertiesWithoutValues() {
        RealmModel realm = realm("email", "phoneNumber");
        UserModel user = user("user-id", "username", null, Map.of());

        Assert.assertTrue(BruteForceUserProperty.getFailureKeys(realm, user).isEmpty());
    }

    @Test
    public void rejectsAccessToAnUnconfiguredProperty() {
        RealmModel realm = realm("email");
        UserModel user = user("user-id", "username", "user@example.com", Map.of());

        IllegalArgumentException cause = Assert.assertThrows(IllegalArgumentException.class,
                () -> BruteForceUserProperty.getFailureKeys(realm, user, "phoneNumber"));
        Assert.assertTrue(cause.getMessage().contains("phoneNumber"));
    }

    @Test
    public void propertyKeysDoNotExposePropertyValues() {
        String key = BruteForceUserProperty.propertyKey("email", "private@example.com");

        Assert.assertTrue(key.startsWith("bf-property:"));
        Assert.assertFalse(key.contains("private@example.com"));
    }

    private static RealmModel realm(String... properties) {
        return realm(properties.length == 0 ? BruteForceLockPolicy.USER : BruteForceLockPolicy.PROPERTIES,
                properties);
    }

    private static RealmModel realm(BruteForceLockPolicy policy, String... properties) {
        return realm(policy, 30, null, properties);
    }

    private static RealmModel realm(BruteForceLockPolicy policy, int failureFactor, Integer propertyFailureFactor,
            String... properties) {
        return (RealmModel) Proxy.newProxyInstance(
                BruteForceUserPropertyTest.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> {
                    if ("getBruteForceProtectedUserProperties".equals(method.getName())) {
                        return List.of(properties);
                    }
                    if ("getBruteForceLockPolicy".equals(method.getName())) {
                        return policy;
                    }
                    if ("getFailureFactor".equals(method.getName())) {
                        return failureFactor;
                    }
                    if ("getBruteForcePropertyFailureFactor".equals(method.getName())) {
                        return propertyFailureFactor != null ? propertyFailureFactor : failureFactor;
                    }
                    if ("isPermanentLockout".equals(method.getName())) {
                        return true;
                    }
                    if ("getMaxTemporaryLockouts".equals(method.getName())) {
                        return 0;
                    }
                    if ("getAttribute".equals(method.getName())) {
                        return propertyFailureFactor == null ? null : Integer.toString(propertyFailureFactor);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static UserLoginFailureModel loginFailure(int failures) {
        return (UserLoginFailureModel) Proxy.newProxyInstance(
                BruteForceUserPropertyTest.class.getClassLoader(),
                new Class<?>[] { UserLoginFailureModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getNumFailures" -> failures;
                    case "getNumTemporaryLockouts" -> 0;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static KeycloakSession session(Map<String, UserLoginFailureModel> failures) {
        UserLoginFailureProvider provider = (UserLoginFailureProvider) Proxy.newProxyInstance(
                BruteForceUserPropertyTest.class.getClassLoader(),
                new Class<?>[] { UserLoginFailureProvider.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUserLoginFailure" -> failures.get((String) args[1]);
                    case "removeUserLoginFailure" -> failures.remove((String) args[1]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (KeycloakSession) Proxy.newProxyInstance(
                BruteForceUserPropertyTest.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    if ("loginFailures".equals(method.getName())) {
                        return provider;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static UserModel user(String id, String username, String email, Map<String, List<String>> attributes) {
        return (UserModel) Proxy.newProxyInstance(
                BruteForceUserPropertyTest.class.getClassLoader(),
                new Class<?>[] { UserModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "getUsername" -> username;
                    case "getEmail" -> email;
                    case "getFirstName", "getLastName" -> null;
                    case "getAttributeStream" -> attributes.getOrDefault((String) args[0], List.of()).stream();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
