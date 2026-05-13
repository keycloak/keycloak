/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.IssuerState;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.utils.ClaimsPathPointer;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessor;
import org.keycloak.protocol.oidc.rar.InvalidAuthorizationDetailsException;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Strings;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.ISSUER_STATE;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CONFIGURATION_ID;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.CREDENTIALS_OFFER_ID_ATTR;
import static org.keycloak.protocol.oid4vc.utils.CredentialScopeModelUtils.findCredentialScopeModelByConfigurationId;
import static org.keycloak.protocol.oid4vc.utils.CredentialScopeModelUtils.findCredentialScopeModelByName;
import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;

public class OID4VCAuthorizationDetailsProcessor implements AuthorizationDetailsProcessor<OID4VCAuthorizationDetail> {
    private static final Logger logger = Logger.getLogger(OID4VCAuthorizationDetailsProcessor.class);

    private final KeycloakSession session;

    public OID4VCAuthorizationDetailsProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean isSupported() {
        return session.getContext().getRealm().isVerifiableCredentialsEnabled();
    }

    @Override
    public String getSupportedType() {
        return OPENID_CREDENTIAL;
    }

    @Override
    public Class<OID4VCAuthorizationDetail> getSupportedResponseJavaType() {
        return OID4VCAuthorizationDetail.class;
    }

    @Override
    public OID4VCAuthorizationDetail process(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation authzDetail) {
        OID4VCAuthorizationDetail requestAuthDetail = authzDetail.asSubtype(OID4VCAuthorizationDetail.class);
        validateAuthorizationDetail(requestAuthDetail);
        return buildAuthorizationDetailResponse(clientSessionCtx, requestAuthDetail);
    }

    @Override
    public OID4VCAuthorizationDetail validateAuthorizationDetail(AuthorizationDetailsJSONRepresentation authzDetail) throws InvalidAuthorizationDetailsException {

        OID4VCAuthorizationDetail requestAuthDetail = authzDetail.asSubtype(OID4VCAuthorizationDetail.class);

        CredentialIssuer issuerMetadata = new OID4VCIssuerWellKnownProvider(session).getIssuerMetadata();
        Map<String, SupportedCredentialConfiguration> supportedCredentials = issuerMetadata.getCredentialsSupported();
        List<String> authorizationServers = issuerMetadata.getAuthorizationServers();
        String issuerIdentifier = issuerMetadata.getCredentialIssuer();

        String type = requestAuthDetail.getType();
        String credentialConfigurationId = requestAuthDetail.getCredentialConfigurationId();
        List<String> credentialIdentifiers = requestAuthDetail.getCredentialIdentifiers();
        List<ClaimsDescription> claims = requestAuthDetail.getClaims();

        // Validate type first
        if (!OPENID_CREDENTIAL.equals(type)) {
            logger.warnf("Invalid authorization_details type: %s", type);
            throw getInvalidRequestException("type: " + type + ", expected=" + OPENID_CREDENTIAL);
        }

        // If authorization_servers is present, locations must be set to issuer identifier
        if (authorizationServers != null && !authorizationServers.isEmpty()) {
            List<String> locations = requestAuthDetail.getLocations();
            if (locations == null || locations.size() != 1 || !issuerIdentifier.equals(locations.get(0))) {
                logger.warnf("Invalid locations field in authorization_details: %s, expected: %s", locations, issuerIdentifier);
                throw getInvalidRequestException("locations=" + locations + ", expected=" + issuerIdentifier);
            }
        }

        // credential_configuration_id is REQUIRED
        if (Strings.isEmpty(credentialConfigurationId)) {
            logger.warnf("Missing credential_configuration_id in authorization_details");
            throw getInvalidRequestException("credential_configuration_id is required");
        }

        // credential_identifiers not allowed
        if (credentialIdentifiers != null) {
            // we also reject an empty array of credential identifiers
            logger.warnf("Property credential_identifiers not allowed in authorization_details");
            throw getInvalidRequestException("credential_identifiers not allowed");
        }

        // Validate credential_configuration_id
        SupportedCredentialConfiguration credConfig = supportedCredentials.get(credentialConfigurationId);
        if (credConfig == null) {
            logger.warnf("Unsupported credential_configuration_id: %s", credentialConfigurationId);
            throw getInvalidRequestException("Invalid credential configuration: unsupported credential_configuration_id: " + credentialConfigurationId);
        }

        // Validate claims if present
        if (claims != null && !claims.isEmpty()) {
            validateClaims(claims, credConfig);
        }

        return requestAuthDetail;
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private InvalidAuthorizationDetailsException getInvalidRequestException(String errorDescription) {
        return new InvalidAuthorizationDetailsException("Invalid authorization_details: " + errorDescription);
    }

    /**
     * Validates that the requested claims are supported by the credential configuration.
     * This performs semantic validation by checking if Keycloak supports the requested claims.
     *
     * @param claims the list of claims to validate
     * @param config the credential configuration to validate against
     */
    private void validateClaims(List<ClaimsDescription> claims, SupportedCredentialConfiguration config) {

        // Get the exposed claims from credential metadata
        List<Claim> exposedClaims = null;
        if (config.getCredentialMetadata() != null && config.getCredentialMetadata().getClaims() != null && !config.getCredentialMetadata().getClaims().isEmpty()) {
            exposedClaims = config.getCredentialMetadata().getClaims();
        }

        if (exposedClaims == null || exposedClaims.isEmpty()) {
            throw getInvalidRequestException("Credential configuration does not expose any claims metadata");
        }

        // Convert exposed claims to a set of paths for easy comparison
        Set<String> exposedClaimPaths = exposedClaims.stream()
                .filter(claim -> claim.getPath() != null && !claim.getPath().isEmpty())
                .map(claim -> claim.getPath().toString())
                .collect(Collectors.toSet());

        // Validate each requested claim against exposed metadata
        for (ClaimsDescription requestedClaim : claims) {
            if (requestedClaim.getPath() == null || requestedClaim.getPath().isEmpty()) {
                throw getInvalidRequestException("Invalid claims description: path is required");
            }

            // Validate the claims path pointer format according to OID4VCI specification
            if (!ClaimsPathPointer.isValidPath(requestedClaim.getPath())) {
                throw getInvalidRequestException("Invalid claims path pointer: " + requestedClaim.getPath() +
                        ". Path must contain only strings, non-negative integers, and null values.");
            }

            String requestedPath = requestedClaim.getPath().toString();

            // Check if the requested claim path exists in the exposed metadata
            if (!exposedClaimPaths.contains(requestedPath)) {
                throw getInvalidRequestException("Unsupported claim: " + requestedPath +
                        ". This claim is not supported by the credential configuration.");
            }
        }

        // Check for conflicts using ClaimsPathPointer utility
        if (!ClaimsPathPointer.validateClaimsDescriptions(claims)) {
            throw getInvalidRequestException("Invalid claims descriptions: conflicting or contradictory claims found");
        }
    }

    private OID4VCAuthorizationDetail buildAuthorizationDetailResponse(ClientSessionContext clientSessionCtx, OID4VCAuthorizationDetail requestAuthDetail) {

        String requestedCredentialConfigurationId = requestAuthDetail.getCredentialConfigurationId();
        if (requestedCredentialConfigurationId == null) {
            throw getInvalidRequestException("No credential_configuration_id in access token request.");
        }

        // Handle AccessToken request with credential offer
        // Should work for pre-auth and auth-code grants
        //
        CredentialOfferState offerState = getCredentialOfferState(clientSessionCtx);
        if (offerState != null) {
            OID4VCAuthorizationDetail offeredAuthDetail = offerState.getAuthorizationDetails(requestedCredentialConfigurationId);
            if (offeredAuthDetail == null) {
                throw getInvalidRequestException("Unauthorized credential_configuration_id: " + requestedCredentialConfigurationId);
            }
            OID4VCAuthorizationDetail responseAuthDetail = offeredAuthDetail.clone();
            responseAuthDetail.setClaims(requestAuthDetail.getClaims());
            return responseAuthDetail;
        }

        // Handle AccessToken request without credential offer
        //
        RealmModel realmModel = clientSessionCtx.getClientSession().getRealm();
        CredentialScopeModel credScope = findCredentialScopeModelByConfigurationId(realmModel, clientSessionCtx::getClientScopesStream, requestedCredentialConfigurationId);
        if (credScope == null)
            throw getInvalidRequestException("Cannot find or access client scope for credential_configuration_id: " + requestedCredentialConfigurationId);

        OID4VCAuthorizationDetail responseAuthDetail = generateResponseAuthorizationDetails(credScope, null);
        responseAuthDetail.setClaims(requestAuthDetail.getClaims());

        return responseAuthDetail;
    }

    @Override
    public List<OID4VCAuthorizationDetail> handleMissingAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        RealmModel realmModel = userSession.getRealm();

        // AccessToken request with credential offer
        // Works for pre-auth and auth-code grants
        CredentialOfferState offerState = getCredentialOfferState(clientSessionCtx);
        if (offerState != null) {
            return offerState.getAuthorizationDetails();
        }

        // AccessToken request with no credential offer and no auth details
        // This is likely a "scope only" request
        String scopeParam = clientSessionCtx.getScopeString();
        if (scopeParam == null) {
            throw getInvalidRequestException("No 'scope' parameter in client session");
        }

        List<OID4VCAuthorizationDetail> authorizationDetails = new ArrayList<>();
        for (String scope : scopeParam.split(" ")) {
            CredentialScopeModel credScope = findCredentialScopeModelByName(realmModel, clientSessionCtx::getClientScopesStream, scope);
            if (credScope != null) {

                // Generate `authorization_details` for the AccessToken Response
                // This is the same logic as we use when a credential offer is created
                //
                OID4VCAuthorizationDetail authDetail = generateResponseAuthorizationDetails(credScope, null);
                authorizationDetails.add(authDetail);
            }
        }

        if (!authorizationDetails.isEmpty()) {
            logger.debugf("Generated authorization_details: %s", JsonSerialization.valueAsString(authorizationDetails));
        } else {
            logger.debug("No generated authorization_details");
        }
        return authorizationDetails;
    }

    @Override
    public OID4VCAuthorizationDetail processStoredAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation storedAuthDetails)
            throws InvalidAuthorizationDetailsException {
        if (storedAuthDetails == null) {
            return null;
        }

        logger.debugf("Processing stored authorization_details from authorization request: %s", storedAuthDetails);

        try {
            return process(userSession, clientSessionCtx, storedAuthDetails);
        } catch (InvalidAuthorizationDetailsException e) {
            // According to OID4VC spec, if authorization_details was used in authorization request,
            // it is required to be returned in token response. If it cannot be processed, return invalid_request error
            throw new InvalidAuthorizationDetailsException("authorization_details was used in authorization request but cannot be processed for token response: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // No cleanup needed
    }

    public OID4VCAuthorizationDetail generateResponseAuthorizationDetails(CredentialScopeModel credScope, String credOffersId) {

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setCredentialsOfferId(credOffersId);
        authDetail.setType(OPENID_CREDENTIAL);

        String credConfigId = Optional.ofNullable(credScope.getCredentialConfigurationId())
                .orElseThrow(() -> new IllegalStateException("No " + VC_CONFIGURATION_ID + " in client scope: " + credScope.getName()));

        authDetail.setCredentialConfigurationId(credConfigId);

        // The AccessToken Response should have authorization_details when ...
        //
        //  * provided in Authorization Request
        //  * provided in AccessToken Request
        //  * defined credential identifiers
        //
        // https://gitlab.com/openid/conformance-suite/-/work_items/1724

        String credIdentifier = credScope.getCredentialIdentifier();
        if (Strings.isEmpty(credIdentifier)) {
            credIdentifier = credConfigId + "_0000";
        }
        authDetail.setCredentialIdentifiers(List.of(credIdentifier));

        return authDetail;
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private CredentialOfferState getCredentialOfferState(ClientSessionContext clientSessionCtx) {

        CredentialOfferState offerState = null;

        // Check if we have a credential offer - this should work for pre-authorized
        //
        String credOfferId = clientSessionCtx.getAttribute(CREDENTIALS_OFFER_ID_ATTR, String.class);

        // Check if we have issuer_state - this should work for authorization_code
        //
        String issuerStateNote = clientSessionCtx.getClientSession().getNote(LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + ISSUER_STATE);
        if (credOfferId == null && issuerStateNote != null) {
            IssuerState issuerState = IssuerState.fromEncodedString(issuerStateNote);
            credOfferId = issuerState.getCredentialsOfferId();
        }

        if (credOfferId != null) {
            String auxCredOfferId = credOfferId;
            CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
            offerState = Optional.ofNullable(offerStorage.getOfferStateById(credOfferId))
                    .orElseThrow(() -> new IllegalStateException("No credential offer state for: " + auxCredOfferId));

            // Check same login client as the client for which the credential offer is target (in case of credential offer target for specific client only)
            String offerClientId = offerState.getTargetClientId();
            String loginClientId = clientSessionCtx.getClientSession().getClient().getClientId();
            if (offerClientId != null && !offerClientId.equals(loginClientId)) {
                throw new IllegalStateException("Credential offer target client '" + offerClientId + "' different from login client '" + loginClientId + "'");
            }
        }

        return offerState;
    }
}
