/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authentication.requiredactions.util;

import java.util.Map;
import java.util.function.Supplier;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.authenticators.util.LoAUtil;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;

import static org.keycloak.models.Constants.NO_LOA;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CredentialDeleteHelper {

    private static final Logger logger = Logger.getLogger(CredentialDeleteHelper.class);

    /**
     * Removing credential of given ID of specified user. It does the necessary validation to validate if specified credential can be removed.
     * In case of step-up authentication enabled, it verifies if user authenticated with corresponding level in order to be able to remove this credential.
     *
     * For instance removing 2nd-factor credential require authentication with 2nd-factor as well for security reasons.
     *
     * @param session
     * @param user
     * @param credentialId
     * @param currentLoAProvider supplier of current authenticated level. Can be retrieved for instance from session or from the token
     * @return removed credential. It can return null if credential was not found or if it was legacy format of federated credential ID
     */
    public static CredentialModel removeCredential(KeycloakSession session, UserModel user, String credentialId, Supplier<Integer> currentLoAProvider) {
        CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialId);
        if (credential == null) {
            if (user.isFederated()) {
                credential = user.credentialManager().getFederatedCredentialsStream().filter(c -> credentialId.equals(c.getId())).findAny().orElse(null);
                if (credential != null) {
                    String type = credential.getType();
                    checkIfCanBeRemoved(session, user, type, currentLoAProvider);
                    user.credentialManager().disableCredentialType(type);
                    return null;
                }
            }
            // Backwards compatibility with account console 1 - When stored credential is not found, it may be federated credential.
            // In this case, it's ID needs to be something like "otp-id", which is returned by account REST GET endpoint as a placeholder
            // for federated credentials (See CredentialHelper.createUserStorageCredentialRepresentation )
            if (credentialId.endsWith("-id")) {
                String credentialType = credentialId.substring(0, credentialId.length() - 3);
                checkIfCanBeRemoved(session, user, credentialType, currentLoAProvider);
                user.credentialManager().disableCredentialType(credentialType);
                return null;
            }
            throw new NotFoundException("Credential not found");
        }
        checkIfCanBeRemoved(session, user, credential.getType(), currentLoAProvider);
        user.credentialManager().removeStoredCredentialById(credentialId);
        return credential;
    }

    private static void checkIfCanBeRemoved(KeycloakSession session, UserModel user, String credentialType, Supplier<Integer> currentLoAProvider) {
        CredentialProvider credentialProvider = AuthenticatorUtil.getCredentialProviders(session)
                .filter(credentialProvider1 -> credentialProvider1.supportsCredentialType(credentialType))
                .findAny().orElse(null);
        if (credentialProvider == null) {
            logger.warnf("Credential provider %s not found", credentialType);
            throw new NotFoundException("Credential provider not found");
        }
        CredentialTypeMetadataContext ctx = CredentialTypeMetadataContext.builder().user(user).build(session);
        CredentialTypeMetadata metadata = credentialProvider.getCredentialTypeMetadata(ctx);
        if (!metadata.isRemoveable()) {
            logger.warnf("Credential type %s cannot be removed", credentialType);
            throw new BadRequestException("Credential type cannot be removed");
        }

        // Check if current accessToken has permission to remove credential in case of step-up authentication was used
        checkAuthenticatedLoASufficientForCredentialRemove(session, credentialType, currentLoAProvider);
    }

    private static void checkAuthenticatedLoASufficientForCredentialRemove(KeycloakSession session, String credentialType, Supplier<Integer> currentLoAProvider) {
        int requestedLoaForCredentialRemove = getRequestedLoaForCredential(session, session.getContext().getRealm(), credentialType);

        int currentAuthenticatedLevel = currentLoAProvider.get();
        if (currentAuthenticatedLevel < requestedLoaForCredentialRemove) {
            throw new ForbiddenException("Insufficient level of authentication for removing credential of type '" + credentialType + "'.");
        }
    }

    private static int getRequestedLoaForCredential(KeycloakSession session, RealmModel realm, String credentialType) {
        Map<String, Integer> credentialTypesToLoa = LoAUtil.getCredentialTypesToLoAMap(session, realm, realm.getBrowserFlow());
        return credentialTypesToLoa.getOrDefault(credentialType, NO_LOA);
    }
}
