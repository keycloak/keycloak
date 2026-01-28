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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class AccountHelper {

    public static UserRepresentation getUserRepresentation(RealmResource realm, String username) {
        Optional<UserRepresentation> userResult = realm.users().search(username, true).stream().findFirst();
        if (userResult.isEmpty()) {
            throw new RuntimeException("User with username " + username + " not found");
        }

        return userResult.get();
    }

    private static UserResource getUserResource(RealmResource realm, String username) {
        UserRepresentation userRepresentation = getUserRepresentation(realm, username);

        return realm.users().get(userRepresentation.getId());
    }

    public static UserResource updateUser(RealmResource realm, String username, UserRepresentation userRepresentation) {
        AccountHelper.getUserResource(realm, username).update(userRepresentation);

        return AccountHelper.getUserResource(realm, username);
    }

    public static boolean updatePassword(RealmResource realm, String username, String password) {
        UserResource user = getUserResource(realm, username);

        CredentialRepresentation credentialRepresentation = CredentialBuilder.create().password(password).build();

        try {
            user.resetPassword(credentialRepresentation);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static List<Map<String, Object>> getUserConsents(RealmResource realm, String username) {
        UserResource user = getUserResource(realm, username);
        List<Map<String, Object>> consents = user.getConsents();
        return consents;
    }

    public static void revokeConsents(RealmResource realm, String username, String clientId) {
        UserResource user = getUserResource(realm, username);
        user.revokeConsent(clientId);
    }

    private static Optional<CredentialRepresentation> getOtpCredentials(UserResource user, String userLabel) {
        return user.credentials().stream().filter(c -> c.getType().equals(OTPCredentialModel.TYPE)).filter(l -> l.getUserLabel().equals(userLabel)).findFirst();
    }

    private static Optional<CredentialRepresentation> getOtpCredentials(UserResource user) {
        return user.credentials().stream().filter(c -> c.getType().equals(OTPCredentialModel.TYPE)).findFirst();
    }

    private static long getOtpCredentialsCount(UserResource user) {
        return user.credentials().stream().filter(c -> c.getType().equals(OTPCredentialModel.TYPE)).count();
    }

    public static boolean deleteTotpAuthentication(RealmResource realm, String username) {
        UserResource user = getUserResource(realm, username);
        Optional<CredentialRepresentation> credentials = getOtpCredentials(user);

        if (credentials.isEmpty()) {
            return false;
        }

        try {
            user.removeCredential(credentials.get().getId());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isTotpPresent(RealmResource realm, String username) {
        UserResource user = getUserResource(realm, username);
        Optional<CredentialRepresentation> credentials = getOtpCredentials(user);
        return credentials.isPresent();
    }

    public static boolean totpCountEquals(RealmResource realm, String username, int count) {
        UserResource user = getUserResource(realm, username);
        return (int) getOtpCredentialsCount(user) == count;
    }

    public static boolean totpUserLabelComparator(RealmResource realm, String username, String userLabel) {
        UserResource user = getUserResource(realm, username);
        Optional<CredentialRepresentation> credentials = getOtpCredentials(user, userLabel);

        return credentials.get().getUserLabel().equals(userLabel);
    }

    public static boolean updateTotpUserLabel(RealmResource realm, String username, String userLabel) {
        UserResource user = getUserResource(realm, username);
        Optional<CredentialRepresentation> credentials = getOtpCredentials(user);

        try {
            user.setCredentialUserLabel(credentials.get().getId(), userLabel);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Response addIdentityProvider(RealmResource childRealm, String childUsername, RealmResource providerRealm, String providerUsername, String providerId) {
        UserResource user = getUserResource(childRealm, childUsername);

        FederatedIdentityRepresentation identityRepresentation = FederatedIdentityBuilder.create()
                .identityProvider(providerId)
                .userId(getUserResource(providerRealm, providerUsername).toRepresentation().getId())
                .userName(providerUsername)
                .build();

        return user.addFederatedIdentity(providerId, identityRepresentation);
    }

    public static void deleteIdentityProvider(RealmResource realm, String username, String providerId) {
        UserResource user = getUserResource(realm, username);
        user.removeFederatedIdentity(providerId);
    }

    public static boolean isIdentityProviderLinked(RealmResource realm, String username, String providerId)  {
        UserResource user = getUserResource(realm, username);

        for (FederatedIdentityRepresentation rep : user.getFederatedIdentity()){
            if(rep.getIdentityProvider().equals(providerId)) {
                return true;
            }
        }
        return false;
    }

    public static void logout(RealmResource realm, String username) {
        UserResource user = getUserResource(realm, username);
        user.logout();
    }

}
