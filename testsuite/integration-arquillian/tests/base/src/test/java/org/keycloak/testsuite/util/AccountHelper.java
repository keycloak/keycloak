/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import java.util.List;
import java.util.Map;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Optional;

public class AccountHelper {

    public static boolean updatePassword(RealmResource realm, String username, String password) {
        Optional<UserRepresentation> userResult = realm.users().search(username).stream().findFirst();
        if (userResult.isEmpty()) {
            throw new RuntimeException("User with username " + username + " not found");
        }

        UserRepresentation userRepresentation = userResult.get();
        UserResource user = realm.users().get(getUserId(realm, username));

        CredentialRepresentation credentialRepresentation = CredentialBuilder.create().password(password).build();

        try {
            user.resetPassword(credentialRepresentation);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static List<Map<String, Object>> getUserConsents(RealmResource realm, String username) {
        UserResource user = realm.users().get(getUserId(realm, username));
        List<Map<String, Object>> consents = user.getConsents();
        return consents;
    }

    public static void revokeConsents(RealmResource realm, String username, String clientId) {
        UserResource user = realm.users().get(getUserId(realm, username));
        user.revokeConsent(clientId);
    }

    public static void logout(RealmResource realm, String username) {
        UserResource user = realm.users().get(getUserId(realm, username));
        user.logout();
    }

    private static String getUserId(RealmResource realm, String username) {
        Optional<UserRepresentation> userResult = realm.users().search(username).stream().findFirst();
        if (userResult.isEmpty()) {
            throw new RuntimeException("User with username " + username + " not found");
        }

        UserRepresentation userRepresentation = userResult.get();
        return userRepresentation.getId();
    }

}
