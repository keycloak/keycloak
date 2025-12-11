/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager.AccessTokenResponseBuilder;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsResponse;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS;
import static org.keycloak.services.util.DefaultClientSessionContext.fromClientSessionAndScopeParameter;

public class PreAuthorizedCodeGrantType extends OAuth2GrantTypeBase {

    private static final Logger LOGGER = Logger.getLogger(PreAuthorizedCodeGrantType.class);

    public static final String VC_ISSUANCE_FLOW = "VC-Issuance-Flow";

    @Override
    public Response process(Context context) {
        LOGGER.debug("Process grant request for preauthorized.");
        setContext(context);

        // See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-token-request
        String code = formParams.getFirst(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM);

        if (code == null) {
            // See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-token-request
            String errorMessage = "Missing parameter: " + PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM;
            event.detail(Details.REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        var offerStorage = session.getProvider(CredentialOfferStorage.class);
        var offerState = offerStorage.findOfferStateByCode(session, code);
        if (offerState == null) {
            var errorMessage = "No credential offer state for code: " + code;
            event.detail(Details.REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        if (offerState.isExpired()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT,
                    "Code is expired", Response.Status.BAD_REQUEST);
        }
        var credOffer = offerState.getCredentialsOffer();

        var appUserId = offerState.getUserId();
        var userModel = session.users().getUserById(realm, appUserId);
        if (userModel == null) {
            var errorMessage = "No user with ID: " + appUserId;
            event.detail(Details.REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }
        if (!userModel.isEnabled()) {
            var errorMessage = "User '" + userModel.getUsername() + "' disabled";
            event.detail(Details.REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        var appClientId = offerState.getClientId();
        ClientModel clientModel = realm.getClientByClientId(appClientId);
        if (clientModel == null) {
            var errorMessage = "No client model for: " + appClientId;
            event.detail(Details.REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        UserSessionModel userSession = session.sessions().createUserSession(null, realm, userModel, userModel.getUsername(),
                null, "pre-authorized-code", false, null,
                null, UserSessionModel.SessionPersistenceState.PERSISTENT);

        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, clientModel, userSession);
        String credentialConfigurationIds = JsonSerialization.valueAsString(credOffer.getCredentialConfigurationIds());
        clientSession.setNote(OID4VCIssuerEndpoint.CREDENTIAL_CONFIGURATION_IDS_NOTE, credentialConfigurationIds);
        clientSession.setNote(OIDCLoginProtocol.ISSUER, credOffer.getCredentialIssuer());
        clientSession.setNote(VC_ISSUANCE_FLOW, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE);

        ClientSessionContext sessionContext = fromClientSessionAndScopeParameter(clientSession, OAuth2Constants.SCOPE_OPENID, session);
        sessionContext.setAttribute(Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE);

        // set the client as retrieved from the pre-authorized session
        session.getContext().setClient(clientModel);

        // Process authorization_details using provider discovery
        List<AuthorizationDetailsResponse> authorizationDetailsResponses = processAuthorizationDetails(userSession, sessionContext);
        LOGGER.infof("Initial authorization_details processing result: %s", authorizationDetailsResponses);

        // If no authorization_details were processed from the request, try to generate them from credential offer
        if (authorizationDetailsResponses == null || authorizationDetailsResponses.isEmpty()) {
            authorizationDetailsResponses = handleMissingAuthorizationDetails(userSession, sessionContext);
        }

        authorizationDetailsResponses = Optional.ofNullable(authorizationDetailsResponses).orElse(List.of());
        if (authorizationDetailsResponses.size() != 1) {
            boolean emptyAuthDetails = authorizationDetailsResponses.isEmpty();
            String errorMessage = (emptyAuthDetails ? "No" : "Multiple") + " authorization details";
            event.detail(Details.REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        // Add authorization_details to the OfferState and otherClaims
        var authDetails = (OID4VCAuthorizationDetailsResponse) authorizationDetailsResponses.get(0);
        offerState.setAuthorizationDetails(authDetails);
        offerStorage.replaceOfferState(session, offerState);

        AccessToken accessToken = tokenManager.createClientAccessToken(session,
                clientSession.getRealm(),
                clientSession.getClient(),
                userSession.getUser(),
                userSession,
                sessionContext);

        accessToken.setOtherClaims(AUTHORIZATION_DETAILS, authorizationDetailsResponses);

        AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(
                clientSession.getRealm(),
                clientSession.getClient(),
                event,
                session,
                userSession,
                sessionContext).accessToken(accessToken);

        AccessTokenResponse tokenResponse;
        try {
            tokenResponse = responseBuilder.build();
            tokenResponse.setOtherClaims(AUTHORIZATION_DETAILS, authorizationDetailsResponses);
        } catch (RuntimeException re) {
            String errorMessage = "Cannot get encryption KEK";
            if (errorMessage.equals(re.getMessage())) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, errorMessage, Response.Status.BAD_REQUEST);
            } else {
                throw re;
            }
        }

        event.success();
        return cors.allowAllOrigins().add(Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public EventType getEventType() {
        return EventType.CODE_TO_TOKEN;
    }
}
