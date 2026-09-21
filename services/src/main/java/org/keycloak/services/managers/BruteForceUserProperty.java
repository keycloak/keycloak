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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.common.util.Base64Url;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;

/**
 * Resolves brute-force failure-counter keys for a user.
 *
 * <p>The realm {@link org.keycloak.representations.idm.RealmRepresentation.BruteForceLockPolicy}
 * decides whether login is locked by the per-user id counter, by selected user properties,
 * or by either. Failed attempts increment the user-id counter and/or the submitted
 * identifier's property counter. The user-id threshold disables every identifier for that
 * account. A property threshold blocks only attempts that reuse that property value, so a
 * locked email does not block a phone number or username that still has remaining attempts.
 * Property counters are shared by every user with the same property value. Counter keys
 * store a digest of the value rather than the raw attribute.</p>
 */
public final class BruteForceUserProperty {

    public static final String ID = "id";
    private static final String PROPERTY_KEY_PREFIX = "bf-property:";

    private BruteForceUserProperty() {
    }

    public static List<String> getFailureKeys(RealmModel realm, UserModel user) {
        Set<String> keys = new LinkedHashSet<>();
        for (String property : getProtectedProperties(realm)) {
            keys.addAll(getFailureKeys(realm, user, property));
        }
        return List.copyOf(keys);
    }

    /**
     * Counters to increment for this login attempt. {@code USER} always uses the user id.
     * {@code PROPERTIES} increments only property counters whose current value matches
     * {@code attemptedIdentifier}. {@code ANY} increments the user id and any matching
     * property counters.
     */
    public static List<String> getFailureKeysForAttempt(RealmModel realm, UserModel user, String attemptedIdentifier) {
        String identifier = attemptedIdentifier == null || attemptedIdentifier.isBlank()
                ? user.getUsername()
                : attemptedIdentifier;
        return switch (realm.getBruteForceLockPolicy()) {
            case PROPERTIES -> getMatchingPropertyKeys(realm, user, identifier);
            case ANY -> {
                Set<String> keys = new LinkedHashSet<>();
                keys.add(user.getId());
                keys.addAll(getMatchingPropertyKeys(realm, user, identifier));
                yield List.copyOf(keys);
            }
            case USER -> List.of(user.getId());
        };
    }

    public static List<String> getMatchingPropertyKeys(RealmModel realm, UserModel user, String attemptedIdentifier) {
        if (attemptedIdentifier == null || attemptedIdentifier.isBlank()) {
            return List.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        for (String property : getProtectedProperties(realm)) {
            if (ID.equals(property)) {
                keys.add(user.getId());
                continue;
            }
            String attempted = normalize(property, attemptedIdentifier);
            values(user, property)
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> normalize(property, value))
                    .filter(attempted::equals)
                    .map(value -> propertyKey(property, value))
                    .forEach(keys::add);
        }
        return List.copyOf(keys);
    }

    public static int getFailureFactor(RealmModel realm, String failureKey) {
        if (failureKey != null && failureKey.startsWith(PROPERTY_KEY_PREFIX)) {
            return realm.getBruteForcePropertyFailureFactor();
        }
        return realm.getFailureFactor();
    }

    /**
     * Counters that disable every identifier of this user. Property keys are omitted so a
     * locked email or phone number cannot disable login with a different identifier.
     */
    public static List<String> getAccountLockKeys(RealmModel realm, UserModel user) {
        if (!getProtectedProperties(realm).contains(ID)) {
            return List.of();
        }
        return List.of(user.getId());
    }

    public static boolean isPropertyKey(String failureKey) {
        return failureKey != null && failureKey.startsWith(PROPERTY_KEY_PREFIX);
    }

    /**
     * Find a user by a protected custom attribute such as {@code phoneNumber} after username
     * and email lookup missed. Built-in username and email are left to
     * {@link org.keycloak.models.utils.KeycloakModelUtils#findUserByNameOrEmail}.
     */
    public static UserModel findUserByProtectedPropertyValue(KeycloakSession session, RealmModel realm,
            String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        Map<String, UserModel> users = new LinkedHashMap<>();
        for (String property : realm.getBruteForceProtectedUserProperties()) {
            if (ID.equals(property) || UserModel.USERNAME.equals(property) || UserModel.EMAIL.equals(property)) {
                continue;
            }
            findUsersByPropertyValue(session, realm, property, identifier)
                    .forEach(found -> users.putIfAbsent(found.getId(), found));
        }
        if (users.size() > 1) {
            throw new ModelDuplicateException("Multiple users match protected property value");
        }
        return users.isEmpty() ? null : users.values().iterator().next();
    }

    public static boolean isPermanentlyLocked(RealmModel realm, UserLoginFailureModel model, String failureKey) {
        return realm.isPermanentLockout()
                && (model.getNumTemporaryLockouts() > realm.getMaxTemporaryLockouts()
                || (realm.getMaxTemporaryLockouts() == 0
                && model.getNumFailures() >= getFailureFactor(realm, failureKey)));
    }

    public static List<String> getProtectedProperties(RealmModel realm) {
        Set<String> properties = new LinkedHashSet<>();
        switch (realm.getBruteForceLockPolicy()) {
            case PROPERTIES -> {
                properties.addAll(realm.getBruteForceProtectedUserProperties());
                if (properties.isEmpty()) {
                    properties.add(ID);
                }
            }
            case ANY -> {
                properties.add(ID);
                properties.addAll(realm.getBruteForceProtectedUserProperties());
            }
            case USER -> properties.add(ID);
        }
        return List.copyOf(properties);
    }

    public static List<String> getFailureKeys(RealmModel realm, UserModel user, String property) {
        if (!getProtectedProperties(realm).contains(property)) {
            throw new IllegalArgumentException("User property is not protected by brute force detection: " + property);
        }
        if (ID.equals(property)) {
            return List.of(user.getId());
        }

        return values(user, property)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> propertyKey(property, normalize(property, value)))
                .distinct()
                .toList();
    }

    public static boolean removeLoginFailures(KeycloakSession session, RealmModel realm, UserModel user) {
        boolean removed = false;
        for (String failureKey : getFailureKeys(realm, user)) {
            if (session.loginFailures().getUserLoginFailure(realm, failureKey) != null) {
                session.loginFailures().removeUserLoginFailure(realm, failureKey);
                removed = true;
            }
        }
        return removed;
    }

    public static Stream<UserLoginFailureModel> getLoginFailures(KeycloakSession session, RealmModel realm,
            UserModel user) {
        return getFailureKeys(realm, user).stream()
                .map(failureKey -> session.loginFailures().getUserLoginFailure(realm, failureKey))
                .filter(Objects::nonNull);
    }

    public static Stream<UserLoginFailureModel> getLoginFailures(KeycloakSession session, RealmModel realm,
            UserModel user, String property) {
        return getFailureKeys(realm, user, property).stream()
                .map(failureKey -> session.loginFailures().getUserLoginFailure(realm, failureKey))
                .filter(Objects::nonNull);
    }

    /**
     * Users that currently share this user's value for {@code property}, including {@code user}.
     * {@code id} is per-account and never shared.
     */
    public static Stream<UserModel> getUsersSharingProperty(KeycloakSession session, RealmModel realm,
            UserModel user, String property) {
        Map<String, UserModel> users = new LinkedHashMap<>();
        users.put(user.getId(), user);
        if (!ID.equals(property)) {
            values(user, property)
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(value -> findUsersByPropertyValue(session, realm, property, value)
                            .forEach(found -> users.putIfAbsent(found.getId(), found)));
        }
        return users.values().stream();
    }

    public static boolean isLockedByRemainingCounters(KeycloakSession session, RealmModel realm, UserModel user,
            List<String> clearedKeys) {
        return getFailureKeys(realm, user).stream()
                .filter(failureKey -> !clearedKeys.contains(failureKey))
                .anyMatch(failureKey -> {
                    UserLoginFailureModel model = session.loginFailures().getUserLoginFailure(realm, failureKey);
                    return model != null && isPermanentlyLocked(realm, model, failureKey);
                });
    }

    static String propertyKey(String property, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((property + '\0' + value).getBytes(StandardCharsets.UTF_8));
            return PROPERTY_KEY_PREFIX + Base64Url.encode(bytes);
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("SHA-256 is not available", cause);
        }
    }

    private static Stream<UserModel> findUsersByPropertyValue(KeycloakSession session, RealmModel realm,
            String property, String value) {
        return switch (property) {
            case UserModel.USERNAME -> Stream.ofNullable(session.users().getUserByUsername(realm, value));
            case UserModel.EMAIL -> session.users()
                    .searchForUserStream(realm, Map.of(UserModel.EMAIL, value))
                    .filter(found -> found.getEmail() != null && found.getEmail().equalsIgnoreCase(value));
            default -> session.users().searchForUserByUserAttributeStream(realm, property, value);
        };
    }

    private static Stream<String> values(UserModel user, String property) {
        return switch (property) {
            case UserModel.USERNAME -> Stream.ofNullable(user.getUsername());
            case UserModel.EMAIL -> Stream.ofNullable(user.getEmail());
            case UserModel.FIRST_NAME -> Stream.ofNullable(user.getFirstName());
            case UserModel.LAST_NAME -> Stream.ofNullable(user.getLastName());
            default -> user.getAttributeStream(property);
        };
    }

    private static String normalize(String property, String value) {
        String normalized = value.trim();
        return switch (property) {
            case UserModel.USERNAME, UserModel.EMAIL -> normalized.toLowerCase(Locale.ROOT);
            default -> normalized;
        };
    }
}
