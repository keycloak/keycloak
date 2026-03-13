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
package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.CredentialOfferException;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.AuthorizationCodeGrant;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.IssuerState;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant;
import org.keycloak.protocol.oid4vc.utils.CredentialScopeModelUtils;
import org.keycloak.util.Strings;

import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE;

/**
 * Default implementation of {@link CredentialOfferProvider}.
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
class DefaultCredentialOfferProvider implements CredentialOfferProvider {

    private final KeycloakSession session;

    DefaultCredentialOfferProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public CredentialOfferState createCredentialOffer(
            UserSessionModel userSession,
            String grantType,
            List<String> credentialConfigurationIds,
            String targetClientId,
            String targetUsername,
            Integer expireAt) {

        // Ensure at least one credential_configuration_id
        //
        if (credentialConfigurationIds == null || credentialConfigurationIds.isEmpty()) {
            throw new CredentialOfferException(Errors.INVALID_REQUEST, "No credentialConfigurationIds");
        }

        // Validate the target user
        //
        String targetUserId = Optional.ofNullable(targetUsername)
                .map(tu -> validateTargetUser(session, userSession, tu))
                .map(UserModel::getId).orElse(null);

        // Create the CredentialsOffer
        //
        CredentialsOffer credOffer = new CredentialsOffer()
                .setCredentialIssuer(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()))
                .setCredentialConfigurationIds(credentialConfigurationIds);

        // Create the CredentialOfferState
        //
        RealmModel realmModel = userSession.getRealm();
        CredentialOfferState offerState = new CredentialOfferState(credOffer, targetClientId, targetUserId, expireAt, credOffersId -> {
            List<OID4VCAuthorizationDetail> authDetails = new ArrayList<>();
            for (String credConfigId : credentialConfigurationIds) {
                CredentialScopeModel credScope = CredentialScopeModelUtils.findCredentialScopeModelByConfigurationId(
                        realmModel, () -> session.clientScopes().getClientScopesStream(realmModel), credConfigId);
                if (credScope == null) {
                    throw new CredentialOfferException(Errors.INVALID_REQUEST, "No credential scope model for: " + credConfigId);
                }
                authDetails.add(CredentialScopeModelUtils.buildOID4VCAuthorizationDetail(credScope, credOffersId));
            }
            return authDetails;
        });

        if (PRE_AUTH_GRANT_TYPE.equals(grantType)) {
            String code = "urn:oid4vci:code:" + SecretGenerator.getInstance().randomString(64);
            credOffer.addGrant(new PreAuthorizedCodeGrant().setPreAuthorizedCode(code));
        } else {
            IssuerState issuerState = new IssuerState().setCredentialsOfferId(offerState.getCredentialsOfferId());
            credOffer.addGrant(new AuthorizationCodeGrant().setIssuerState(issuerState.encodeToString()));
        }

        return offerState;
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private UserModel validateTargetUser(KeycloakSession session, UserSessionModel userSession, String targetUser) {
        UserModel loginUserModel = userSession.getUser();

        // Verify that the target user exists
        //
        RealmModel realmModel = userSession.getRealm();
        UserModel targetUserModel = session.users().getUserByUsername(realmModel, targetUser);
        if (targetUserModel == null) {
            throw new CredentialOfferException(Errors.USER_NOT_FOUND, "User not found: " + targetUser);
        }

        // Verify that the target user is enabled
        //
        if (!targetUserModel.isEnabled()) {
            throw new CredentialOfferException(Errors.USER_DISABLED, "User '" + targetUser + "' disabled");
        }

        // Verify that the issuing user holds the required {@code credential_offer_create} role if `loginUser != targetUser`
        // i.e. A self issued credential offer does not require the {@code credential_offer_create} role
        //
        //   - Targeted or Anonymous `authorization_code` grant
        //   - Targeted `pre-authorized_code` grant
        //
        if (Strings.isEmpty(targetUser) || !loginUserModel.getUsername().equals(targetUser)) {
            boolean hasCredentialOfferRole = loginUserModel.getRoleMappingsStream()
                    .anyMatch(rm -> rm.getName().equals(CREDENTIAL_OFFER_CREATE.getName()));
            if (!hasCredentialOfferRole) {
                throw new CredentialOfferException(Errors.NOT_ALLOWED, "Credential offer creation requires role: " + CREDENTIAL_OFFER_CREATE.getName());
            }
        }

        return targetUserModel;
    }
}
