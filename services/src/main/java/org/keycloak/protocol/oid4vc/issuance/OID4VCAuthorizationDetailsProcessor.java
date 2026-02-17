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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.utils.ClaimsPathPointer;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantType;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessor;
import org.keycloak.protocol.oidc.rar.InvalidAuthorizationDetailsException;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.util.AuthorizationDetailsParser;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

import static org.keycloak.OID4VCConstants.CREDENTIAL_CONFIGURATION_ID;
import static org.keycloak.OID4VCConstants.CREDENTIAL_IDENTIFIERS;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.models.Constants.AUTHORIZATION_DETAILS_RESPONSE;
import static org.keycloak.protocol.oid4vc.model.ClaimsDescription.MANDATORY;
import static org.keycloak.protocol.oid4vc.model.ClaimsDescription.PATH;
import static org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail.CLAIMS;

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
        OID4VCAuthorizationDetail detail = authzDetail.asSubtype(OID4VCAuthorizationDetail.class);
        Map<String, SupportedCredentialConfiguration> supportedCredentials = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session);

        // Retrieve authorization servers and issuer identifier for locations check
        List<String> authorizationServers = OID4VCIssuerWellKnownProvider.getAuthorizationServers(session);
        String issuerIdentifier = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());

        validateAuthorizationDetail(detail, supportedCredentials, authorizationServers, issuerIdentifier);
        OID4VCAuthorizationDetail responseDetail = buildAuthorizationDetail(detail, userSession, clientSessionCtx);

        // For authorization code flow, create CredentialOfferState if credential identifiers are present
        // This allows credential requests with credential_identifier to find the associated offer state
        createOfferStateForAuthorizationCodeFlow(userSession, clientSessionCtx, responseDetail);

        return responseDetail;
    }

    /**
     * Creates CredentialOfferState for authorization code flow when credential identifiers are generated.
     * This is only done for authorization code flow (not pre-authorized flow which already has an offer state).
     * Processes all OID4VC authorization details to support multiple credential requests.
     */
    private void createOfferStateForAuthorizationCodeFlow(UserSessionModel userSession, ClientSessionContext clientSessionCtx,
                                                          OID4VCAuthorizationDetail oid4vcDetail) {
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        ClientModel client = clientSession != null ? clientSession.getClient() : null;
        UserModel user = userSession != null ? userSession.getUser() : null;

        if (client == null || user == null) {
            return;
        }

        // Skip if we're in pre-authorized code flow (it already has an offer state that will be updated)
        // Pre-authorized flow sets VC_ISSUANCE_FLOW note on the client session
        String vcIssuanceFlow = clientSession.getNote(PreAuthorizedCodeGrantType.VC_ISSUANCE_FLOW);
        if (vcIssuanceFlow != null && vcIssuanceFlow.equals(PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE)) {
            logger.debugf("Skipping offer state creation for pre-authorized code flow (offer state already exists and will be updated)");
            return;
        }

        CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);

        // Process all OID4VC authorization details to create offer states for each credential
        if (oid4vcDetail.getCredentialIdentifiers() != null && !oid4vcDetail.getCredentialIdentifiers().isEmpty()) {
            for (String credentialId : oid4vcDetail.getCredentialIdentifiers()) {
                // Check if offer state already exists
                CredentialOfferStorage.CredentialOfferState existingState = offerStorage.findOfferStateByCredentialId(session, credentialId);

                if (existingState == null) {
                    // Create a new offer state for authorization code flow
                    CredentialsOffer credOffer = new CredentialsOffer()
                            .setCredentialIssuer(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()))
                            .setCredentialConfigurationIds(List.of(oid4vcDetail.getCredentialConfigurationId()));

                    // Use a reasonable expiration time (e.g., 1 hour)
                    int expiration = Time.currentTime() + 3600;
                    CredentialOfferStorage.CredentialOfferState offerState = new CredentialOfferStorage.CredentialOfferState(
                            credOffer, client.getClientId(), user.getId(), expiration);
                    offerState.setAuthorizationDetails(oid4vcDetail);

                    offerStorage.putOfferState(session, offerState);
                    logger.debugf("Created credential offer state for authorization code flow: [cid=%s, uid=%s, credConfigId=%s, credId=%s]",
                            client.getClientId(), offerState.getUserId(), oid4vcDetail.getCredentialConfigurationId(), credentialId);
                } else {
                    // Update existing offer state with new authorization details (e.g., if same credential identifier is reused)
                    existingState.setAuthorizationDetails(oid4vcDetail);
                    offerStorage.replaceOfferState(session, existingState);
                    logger.debugf("Updated existing credential offer state for authorization code flow: [cid=%s, uid=%s, credConfigId=%s, credId=%s]",
                            client.getClientId(), existingState.getUserId(), oid4vcDetail.getCredentialConfigurationId(), credentialId);
                }
            }
        }
    }

    private InvalidAuthorizationDetailsException getInvalidRequestException(String errorDescription) {
        return new InvalidAuthorizationDetailsException("Invalid authorization_details: " + errorDescription);
    }

    /**
     * Validates an authorization detail against supported credentials and other constraints.
     *
     * @param detail               the authorization detail to validate
     * @param supportedCredentials map of supported credential configurations
     * @param authorizationServers list of authorization servers
     * @param issuerIdentifier     the issuer identifier
     */
    private void validateAuthorizationDetail(OID4VCAuthorizationDetail detail, Map<String, SupportedCredentialConfiguration> supportedCredentials, List<String> authorizationServers, String issuerIdentifier) {

        String type = detail.getType();
        String credentialConfigurationId = detail.getCredentialConfigurationId();
        List<String> credentialIdentifiers = detail.getCredentialIdentifiers();
        List<ClaimsDescription> claims = detail.getClaims();

        // Validate type first
        if (!OPENID_CREDENTIAL.equals(type)) {
            logger.warnf("Invalid authorization_details type: %s", type);
            throw getInvalidRequestException("type: " + type + ", expected=" + OPENID_CREDENTIAL);
        }

        // If authorization_servers is present, locations must be set to issuer identifier
        if (authorizationServers != null && !authorizationServers.isEmpty()) {
            List<String> locations = detail.getLocations();
            if (locations == null || locations.size() != 1 || !issuerIdentifier.equals(locations.get(0))) {
                logger.warnf("Invalid locations field in authorization_details: %s, expected: %s", locations, issuerIdentifier);
                throw getInvalidRequestException("locations=" + locations + ", expected=" + issuerIdentifier);
            }
        }

        // credential_identifiers not allowed
        if (credentialIdentifiers != null && !credentialIdentifiers.isEmpty()) {
            logger.warnf("Property credential_identifiers not allowed in authorization_details");
            throw getInvalidRequestException("credential_identifiers not allowed");
        }

        // credential_configuration_id is REQUIRED
        if (credentialConfigurationId == null) {
            logger.warnf("Missing credential_configuration_id in authorization_details");
            throw getInvalidRequestException("credential_configuration_id is required");
        }

        // Validate credential_configuration_id
        SupportedCredentialConfiguration config = supportedCredentials.get(credentialConfigurationId);
        if (config == null) {
            logger.warnf("Unsupported credential_configuration_id: %s", credentialConfigurationId);
            throw getInvalidRequestException("Invalid credential configuration: unsupported credential_configuration_id=" + credentialConfigurationId);
        }


        // Validate claims if present
        if (claims != null && !claims.isEmpty()) {
            validateClaims(claims, config);
        }
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

    private OID4VCAuthorizationDetail buildAuthorizationDetail(OID4VCAuthorizationDetail detail, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        String credentialConfigurationId = detail.getCredentialConfigurationId();

        // Try to reuse identifier from authorizationDetailsResponse in client session context
        List<AuthorizationDetailsJSONRepresentation> previousResponses = clientSessionCtx.getAttribute(AUTHORIZATION_DETAILS_RESPONSE, List.class);
        List<OID4VCAuthorizationDetail> oid4vcPreviousResponses = getSupportedAuthorizationDetails(previousResponses);
        List<String> credentialIdentifiers = oid4vcPreviousResponses != null && !oid4vcPreviousResponses.isEmpty()
                ? oid4vcPreviousResponses.get(0).getCredentialIdentifiers()
                : null;

        if (credentialIdentifiers == null) {
            credentialIdentifiers = new ArrayList<>();
            credentialIdentifiers.add(UUID.randomUUID().toString());
        }

        OID4VCAuthorizationDetail responseDetail = new OID4VCAuthorizationDetail();
        responseDetail.setType(OPENID_CREDENTIAL);
        responseDetail.setCredentialConfigurationId(credentialConfigurationId);
        responseDetail.setCredentialIdentifiers(credentialIdentifiers);
        responseDetail.setClaims(detail.getClaims());

        return responseDetail;
    }


    /**
     * Generate authorization_details from the credential offer when authorization_details parameter is not present in the token request.
     * This method generates authorization_details based on the credential_configuration_ids from the credential offer.
     *
     * @param clientSession the client session that contains the credential offer information
     * @return the authorization details response if generation was successful, null otherwise
     */
    private List<OID4VCAuthorizationDetail> generateAuthorizationDetailsFromCredentialOffer(AuthenticatedClientSessionModel clientSession) {
        logger.debug("Processing authorization_details from credential offer");

        // Get supported credentials
        Map<String, SupportedCredentialConfiguration> supportedCredentials = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session);
        if (supportedCredentials == null || supportedCredentials.isEmpty()) {
            logger.debug("No supported credentials found, cannot generate authorization_details from credential offer");
            return null;
        }

        // Extract credential_configuration_ids from the credential offer
        List<String> credentialConfigurationIds = extractCredentialConfigurationIds(clientSession);

        if (credentialConfigurationIds == null || credentialConfigurationIds.isEmpty()) {
            logger.debug("No credential_configuration_ids found in credential offer, cannot generate authorization_details");
            return null;
        }

        // Generate authorization_details for each credential configuration
        List<OID4VCAuthorizationDetail> authorizationDetailsList = new ArrayList<>();

        for (String credentialConfigurationId : credentialConfigurationIds) {
            SupportedCredentialConfiguration config = supportedCredentials.get(credentialConfigurationId);
            if (config == null) {
                logger.warnf("Credential configuration '%s' not found in supported credentials, skipping", credentialConfigurationId);
                continue;
            }

            String credentialIdentifier = UUID.randomUUID().toString();
            logger.debugf("Generated credential identifier '%s' for configuration '%s'",
                    credentialIdentifier, credentialConfigurationId);

            OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
            authDetail.setType(OPENID_CREDENTIAL);
            authDetail.setCredentialConfigurationId(credentialConfigurationId);
            authDetail.setCredentialIdentifiers(List.of(credentialIdentifier));

            authorizationDetailsList.add(authDetail);
        }

        if (authorizationDetailsList.isEmpty()) {
            logger.debug("No valid credential configurations found, cannot generate authorization_details");
            return null;
        }

        return authorizationDetailsList;
    }

    /**
     * Extract credential_configuration_ids from the credential offer stored in client session
     */
    private List<String> extractCredentialConfigurationIds(AuthenticatedClientSessionModel clientSession) {
        // Get credential configuration IDs from the predictable location
        // This is stored when the credential offer is created in getCredentialOfferURI
        String credentialConfigIdsJson = clientSession.getNote(OID4VCIssuerEndpoint.CREDENTIAL_CONFIGURATION_IDS_NOTE);
        if (credentialConfigIdsJson != null) {
            logger.debugf("Found credential configuration IDs in predictable location");
            try {
                List<String> configIds = JsonSerialization.readValue(credentialConfigIdsJson, List.class);
                logger.debugf("Successfully parsed credential configuration IDs: %s", configIds);
                return configIds;
            } catch (Exception e) {
                logger.warnf("Failed to parse credential configuration IDs from predictable location: %s", e.getMessage());
            }
        }

        logger.debugf("No credential_configuration_ids found in predictable location");
        return null;
    }

    @Override
    public List<OID4VCAuthorizationDetail> handleMissingAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // Only generate authorization_details from credential offer if:
        // 1. No authorization_details were processed yet, AND
        // 2. There's a credential offer note in the client session (indicating this is a credential offer flow)
        // This prevents generating authorization_details for regular SSO logins that don't request OID4VCI - Not 100% sure the check for CREDENTIAL_CONFIGURATION_IDS_NOTE is needed and sufficient
        if (clientSessionCtx.getClientSession().getNote(OID4VCIssuerEndpoint.CREDENTIAL_CONFIGURATION_IDS_NOTE) != null) {
            AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
            return generateAuthorizationDetailsFromCredentialOffer(clientSession);
        } else {
            return null;
        }
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


    public static class OID4VCAuthorizationDetailsParser implements AuthorizationDetailsParser {

        @Override
        public <T extends AuthorizationDetailsJSONRepresentation> T asSubtype(AuthorizationDetailsJSONRepresentation authzDetail, Class<T> clazz) {
            if (OID4VCAuthorizationDetail.class.equals(clazz)) {
                if (authzDetail instanceof OID4VCAuthorizationDetail) {
                    return clazz.cast(authzDetail);
                } else {
                    OID4VCAuthorizationDetail detail = new OID4VCAuthorizationDetail();
                    fillFields(authzDetail, detail);
                    return clazz.cast(detail);
                }
            } else {
                throw new IllegalArgumentException("Authorization details '" + authzDetail + "' is unsupported to be parsed to '" + clazz + "'.");
            }
        }

        private void fillFields(AuthorizationDetailsJSONRepresentation inDetail, OID4VCAuthorizationDetail outDetail) {
            outDetail.setType(inDetail.getType());
            outDetail.setLocations(inDetail.getLocations());
            outDetail.setCredentialConfigurationId((String) inDetail.getCustomData().get(CREDENTIAL_CONFIGURATION_ID));
            outDetail.setCredentialIdentifiers((List<String>) inDetail.getCustomData().get(CREDENTIAL_IDENTIFIERS));
            outDetail.setClaims(parseClaims((List<Map>) inDetail.getCustomData().get(CLAIMS)));
        }

        private static List<ClaimsDescription> parseClaims(List<Map> genericClaims) {
            if (genericClaims == null) {
                return null;
            }

            return genericClaims.stream()
                    .map(claim -> {
                        List<Object> path = (List<Object>) claim.get(PATH);
                        Boolean mandatory = (Boolean) claim.get(MANDATORY);
                        return new ClaimsDescription(path, mandatory);
                    })
                    .toList();
        }
    }

}
