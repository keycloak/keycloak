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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.common.util.Base64Url;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;

/**
 * Resolves brute-force failure-counter keys for a user.
 *
 * <p>The realm {@link org.keycloak.representations.idm.RealmRepresentation.BruteForceLockPolicy}
 * decides whether login is locked by the per-user id counter, by selected user properties,
 * or by either. Property counters are shared by every user with the same property value.
 * Counter keys store a digest of the value rather than the raw attribute.</p>
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
            default -> properties.add(ID);
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

    static String propertyKey(String property, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((property + '\0' + value).getBytes(StandardCharsets.UTF_8));
            return PROPERTY_KEY_PREFIX + Base64Url.encode(bytes);
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("SHA-256 is not available", cause);
        }
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
