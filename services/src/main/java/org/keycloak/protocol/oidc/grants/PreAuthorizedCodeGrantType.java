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

import java.security.MessageDigest;
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
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.utils.OID4VCUtil;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager.AccessTokenResponseBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Strings;
import org.keycloak.utils.MediaType;

import org.jboss.logging.Logger;

import static org.keycloak.events.Details.REASON;
import static org.keycloak.protocol.oid4vc.model.ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION;
import static org.keycloak.services.util.DefaultClientSessionContext.fromClientSessionAndScopeParameter;

public class PreAuthorizedCodeGrantType extends OAuth2GrantTypeBase {

    private static final Logger LOGGER = Logger.getLogger(PreAuthorizedCodeGrantType.class);

    public static final String VC_ISSUANCE_FLOW = "VC-Issuance-Flow";

    @Override
    public Response process(Context context) {
        LOGGER.debug("Process grant request for preauthorized.");
        setContext(context);

        // Check if OID4VCI functionality is enabled for the realm
        if (!realm.isVerifiableCredentialsEnabled()) {
            LOGGER.debugf("OID4VCI functionality is disabled for realm '%s'. Verifiable Credentials switch is off.", realm.getName());
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT,
                    "OID4VCI functionality is disabled for this realm",
                    Response.Status.FORBIDDEN);
        }

        // See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-token-request
        String preAuthCode = formParams.getFirst(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM);
        String txCode = formParams.getFirst(PreAuthorizedCodeGrantTypeFactory.TX_CODE_PARAM);

        if (preAuthCode == null) {
            // See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-token-request
            String errorMessage = "Missing parameter: " + PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM;
            event.detail(REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        var offerStorage = session.getProvider(CredentialOfferStorage.class);
        var offerState = offerStorage.findOfferStateByCode(session, preAuthCode);
        if (offerState == null) {
            var errorMessage = "No credential offer state for pre-auth code: " + preAuthCode;
            event.detail(REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        if (offerState.isExpired()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT,
                    "Code is expired", Response.Status.BAD_REQUEST);
        }

        String expTxCode = offerState.getTxCode();
        if (expTxCode != null) {
            if (Strings.isEmpty(txCode)) {
                event.error(Errors.MISSING_TX_CODE);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT,
                        "Missing TxCode", Response.Status.BAD_REQUEST);
            }
            // Prevent timing attacks - execution time does not depend on where the first difference occurs
            if (!MessageDigest.isEqual(expTxCode.getBytes(), txCode.getBytes())) {
                event.error(Errors.INVALID_TX_CODE);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT,
                        "Invalid TxCode", Response.Status.BAD_REQUEST);
            }
        }

        var credOffer = offerState.getCredentialsOffer();

        var targetUserId = offerState.getUserId();
        var targetUserModel = session.users().getUserById(realm, targetUserId);
        if (targetUserModel == null) {
            var errorMessage = "No user with ID: " + targetUserId;
            event.detail(REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }
        if (!targetUserModel.isEnabled()) {
            var errorMessage = "User '" + targetUserModel.getUsername() + "' disabled";
            event.detail(REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        var targetClientId = offerState.getClientId();
        ClientModel clientModel = realm.getClientByClientId(targetClientId);
        if (clientModel == null) {
            var errorMessage = "No client model for: " + targetClientId;
            event.detail(REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        UserSessionModel userSession = session.sessions().createUserSession(null, realm, targetUserModel, targetUserModel.getUsername(),
                null, "pre-authorized-code", false, null,
                null, UserSessionModel.SessionPersistenceState.TRANSIENT);

        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, clientModel, userSession);
        String credentialConfigurationIds = JsonSerialization.valueAsString(credOffer.getCredentialConfigurationIds());
        clientSession.setNote(OID4VCIssuerEndpoint.CREDENTIAL_CONFIGURATION_IDS_NOTE, credentialConfigurationIds);
        clientSession.setNote(OIDCLoginProtocol.ISSUER, credOffer.getCredentialIssuer());
        clientSession.setNote(VC_ISSUANCE_FLOW, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE);

        ClientSessionContext sessionContext = fromClientSessionAndScopeParameter(clientSession, OAuth2Constants.SCOPE_OPENID, session);
        sessionContext.setAttribute(Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE);

        // set the client as retrieved from the pre-authorized session
        session.getContext().setClient(clientModel);

        event.client(clientModel)
                .user(targetUserModel);

        // Check if authorization_details parameter was explicitly provided
        String authorizationDetailsParam = formParams.getFirst(OAuth2Constants.AUTHORIZATION_DETAILS);

        // Validate empty authorization_details - if parameter is provided but empty, reject it
        if (authorizationDetailsParam != null && (authorizationDetailsParam.trim().isEmpty() || "[]".equals(authorizationDetailsParam.trim()))) {
            var errorMessage = "Invalid authorization_details: parameter cannot be empty";
            event.detail(REASON, errorMessage).error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        // Process authorization_details using provider discovery
        List<AuthorizationDetailsJSONRepresentation> authorizationDetailsResponses = processAuthorizationDetails(userSession, sessionContext);
        LOGGER.debugf("Initial authorization_details processing result: %s", authorizationDetailsResponses);

        // If no authorization_details were processed from the request, try to generate them from credential offer
        // (only if authorization_details parameter was not explicitly provided)
        if ((authorizationDetailsResponses == null || authorizationDetailsResponses.isEmpty()) && authorizationDetailsParam == null) {
            authorizationDetailsResponses = handleMissingAuthorizationDetails(userSession, sessionContext);
        }

        authorizationDetailsResponses = Optional.ofNullable(authorizationDetailsResponses).orElse(List.of());
        if (authorizationDetailsResponses.size() != 1) {
            boolean emptyAuthDetails = authorizationDetailsResponses.isEmpty();
            String errorMessage = (emptyAuthDetails ? "No" : "Multiple") + " authorization details";
            event.detail(REASON, errorMessage).error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        // Add authorization_details to the OfferState and otherClaims
        OID4VCAuthorizationDetail authDetails = (OID4VCAuthorizationDetail) authorizationDetailsResponses.get(0);
        offerState.setAuthorizationDetails(authDetails);
        offerStorage.replaceOfferState(session, offerState);

        AccessToken accessToken = tokenManager.createClientAccessToken(session,
                clientSession.getRealm(),
                clientSession.getClient(),
                userSession.getUser(),
                userSession,
                sessionContext);

        // Add the scope referenced by the credential from specified credential offer to the token scopes
        String credentialConfigId = authDetails.getCredentialConfigurationId();
        ClientScopeModel clientScope = OID4VCUtil.getClientScopeByCredentialConfigId(session, realm, credentialConfigId);
        if (clientScope == null) {
            String errorMessage = "Client scope was not found for credential configuration ID: " + credentialConfigId;
            event.detail(Details.CREDENTIAL_TYPE, credentialConfigId);
            event.detail(REASON, errorMessage)
                    .error(UNKNOWN_CREDENTIAL_CONFIGURATION.getValue());
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }
        accessToken.setScope(clientScope.getName());

        accessToken.setSessionId(null);
        accessToken.setAuthorizationDetails(authorizationDetailsResponses);

        // Set audience to credential endpoint for pre-authorized tokens
        String credentialEndpoint = OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
        accessToken.audience(credentialEndpoint);

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
            tokenResponse.setAuthorizationDetails(authorizationDetailsResponses);
            tokenResponse.setScope(clientScope.getName());
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

    /**
     * Restrict pre-authorized tokens to the VC credential endpoint.
     */
    @Override
    public boolean isTokenAllowed(KeycloakSession session, AccessToken token) {
        // Check if the request path ends with the credential endpoint path
        boolean isCredentialEndpoint = Optional.ofNullable(session.getContext().getUri())
                .map(uri -> uri.getPath())
                .map(path -> path.endsWith("/" + OID4VCIssuerEndpoint.CREDENTIAL_PATH))
                .orElse(false);

        if (!isCredentialEndpoint) {
            return false;
        }

        // Check if token has exactly one audience and it matches the credential endpoint
        // Being strict about audience prevents potential security issues with multi-audience tokens
        String expectedAudience = OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
        String[] audiences = token.getAudience();
        return audiences != null && audiences.length == 1 && expectedAudience.equals(audiences[0]);
    }
}
