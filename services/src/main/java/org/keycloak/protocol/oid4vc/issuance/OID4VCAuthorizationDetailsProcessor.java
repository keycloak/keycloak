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

import org.keycloak.OAuthErrorException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.utils.ClaimsPathPointer;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessor;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

import static org.keycloak.models.Constants.AUTHORIZATION_DETAILS_RESPONSE;

public class OID4VCAuthorizationDetailsProcessor implements AuthorizationDetailsProcessor {
    private static final Logger logger = Logger.getLogger(OID4VCAuthorizationDetailsProcessor.class);
    private final KeycloakSession session;

    public static final String OPENID_CREDENTIAL_TYPE = "openid_credential";

    public OID4VCAuthorizationDetailsProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean isSupported() {
        return session.getContext().getRealm().isVerifiableCredentialsEnabled();
    }

    @Override
    public List<AuthorizationDetailsResponse> process(UserSessionModel userSession, ClientSessionContext clientSessionCtx, String authorizationDetailsParameter) {
        if (authorizationDetailsParameter == null) {
            return null; // authorization_details is optional
        }

        List<AuthorizationDetail> authDetails = parseAuthorizationDetails(authorizationDetailsParameter);
        Map<String, SupportedCredentialConfiguration> supportedCredentials = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session);
        List<AuthorizationDetailsResponse> authDetailsResponse = new ArrayList<>();

        // Retrieve authorization servers and issuer identifier for locations check
        List<String> authorizationServers = OID4VCIssuerWellKnownProvider.getAuthorizationServers(session);
        String issuerIdentifier = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());

        for (AuthorizationDetail detail : authDetails) {
            validateAuthorizationDetail(detail, supportedCredentials, authorizationServers, issuerIdentifier);
            AuthorizationDetailsResponse responseDetail = buildAuthorizationDetailResponse(detail, userSession, clientSessionCtx);
            authDetailsResponse.add(responseDetail);
        }

        if (authDetailsResponse.isEmpty()) {
            throw getInvalidRequestException("no valid authorization details found");
        }

        return authDetailsResponse;
    }

    private List<AuthorizationDetail> parseAuthorizationDetails(String authorizationDetailsParam) {
        try {
            return JsonSerialization.readValue(authorizationDetailsParam, new TypeReference<List<AuthorizationDetail>>() {
            });
        } catch (Exception e) {
            logger.warnf(e, "Invalid authorization_details format: %s", authorizationDetailsParam);
            throw getInvalidRequestException("format: " + authorizationDetailsParam);
        }
    }

    private RuntimeException getInvalidRequestException(String errorDescription) {
        return new RuntimeException("Invalid authorization_details: " + errorDescription);
    }

    /**
     * Validates an authorization detail against supported credentials and other constraints.
     *
     * @param detail               the authorization detail to validate
     * @param supportedCredentials map of supported credential configurations
     * @param authorizationServers list of authorization servers
     * @param issuerIdentifier     the issuer identifier
     */
    private void validateAuthorizationDetail(AuthorizationDetail detail, Map<String, SupportedCredentialConfiguration> supportedCredentials, List<String> authorizationServers, String issuerIdentifier) {

        String type = detail.getType();
        String credentialConfigurationId = detail.getCredentialConfigurationId();
        List<ClaimsDescription> claims = detail.getClaims();

        // Validate type first
        if (!OPENID_CREDENTIAL_TYPE.equals(type)) {
            logger.warnf("Invalid authorization_details type: %s", type);
            throw getInvalidRequestException("type: " + type + ", expected=" + OPENID_CREDENTIAL_TYPE);
        }

        // If authorization_servers is present, locations must be set to issuer identifier
        if (authorizationServers != null && !authorizationServers.isEmpty()) {
            List<String> locations = detail.getLocations();
            if (locations == null || locations.size() != 1 || !issuerIdentifier.equals(locations.get(0))) {
                logger.warnf("Invalid locations field in authorization_details: %s, expected: %s", locations, issuerIdentifier);
                throw getInvalidRequestException("locations=" + locations + ", expected=" + issuerIdentifier);
            }
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

    private AuthorizationDetailsResponse buildAuthorizationDetailResponse(AuthorizationDetail detail, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        String credentialConfigurationId = detail.getCredentialConfigurationId();

        // Try to reuse identifier from authorizationDetailsResponse in client session context
        List<AuthorizationDetailsResponse> previousResponses = clientSessionCtx.getAttribute(AUTHORIZATION_DETAILS_RESPONSE, List.class);
        List<String> credentialIdentifiers = null;
        if (previousResponses != null) {
            for (AuthorizationDetailsResponse prev : previousResponses) {
                if (prev instanceof OID4VCAuthorizationDetailsResponse) {
                    OID4VCAuthorizationDetailsResponse oid4vcResponse = (OID4VCAuthorizationDetailsResponse) prev;
                    credentialIdentifiers = oid4vcResponse.getCredentialIdentifiers();
                    break;
                }
            }
        }

        if (credentialIdentifiers == null) {
            credentialIdentifiers = new ArrayList<>();
            credentialIdentifiers.add(UUID.randomUUID().toString());
        }

        OID4VCAuthorizationDetailsResponse responseDetail = new OID4VCAuthorizationDetailsResponse();
        responseDetail.setType(OPENID_CREDENTIAL_TYPE);
        responseDetail.setCredentialConfigurationId(credentialConfigurationId);
        responseDetail.setCredentialIdentifiers(credentialIdentifiers);

        // Store credential identifier mapping in client session for later use during credential issuance
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        for (String credentialIdentifier : credentialIdentifiers) {
            // Store the mapping between credential identifier and configuration ID in client session
            String mappingKey = OID4VCIssuerEndpoint.CREDENTIAL_IDENTIFIER_PREFIX + credentialIdentifier;
            clientSession.setNote(mappingKey, credentialConfigurationId);
            logger.debugf("Stored credential identifier mapping: %s -> %s", credentialIdentifier, credentialConfigurationId);
        }

        // Store claims in user session for later use during credential issuance
        if (detail.getClaims() != null) {
            // Store claims with a unique key based on credential configuration ID
            String claimsKey = OID4VCIssuerEndpoint.AUTHORIZATION_DETAILS_CLAIMS_PREFIX + credentialConfigurationId;
            try {
                userSession.setNote(claimsKey, JsonSerialization.writeValueAsString(detail.getClaims()));
            } catch (Exception e) {
                logger.warnf(e, "Failed to store claims in user session for credential configuration %s", credentialConfigurationId);
            }

            // Include claims in response
            responseDetail.setClaims(detail.getClaims());
        }

        return responseDetail;
    }


    /**
     * Generate authorization_details from the credential offer when authorization_details parameter is not present in the token request.
     * This method generates authorization_details based on the credential_configuration_ids from the credential offer.
     *
     * @param clientSession the client session that contains the credential offer information
     * @return the authorization details response if generation was successful, null otherwise
     */
    private List<AuthorizationDetailsResponse> generateAuthorizationDetailsFromCredentialOffer(AuthenticatedClientSessionModel clientSession) {
        logger.info("Processing authorization_details from credential offer");

        // Get supported credentials
        Map<String, SupportedCredentialConfiguration> supportedCredentials = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session);
        if (supportedCredentials == null || supportedCredentials.isEmpty()) {
            logger.info("No supported credentials found, cannot generate authorization_details from credential offer");
            return null;
        }

        // Extract credential_configuration_ids from the credential offer
        List<String> credentialConfigurationIds = extractCredentialConfigurationIds(clientSession);

        if (credentialConfigurationIds == null || credentialConfigurationIds.isEmpty()) {
            logger.info("No credential_configuration_ids found in credential offer, cannot generate authorization_details");
            return null;
        }

        // Generate authorization_details for each credential configuration
        List<AuthorizationDetailsResponse> authorizationDetailsList = new ArrayList<>();

        for (String credentialConfigurationId : credentialConfigurationIds) {
            SupportedCredentialConfiguration config = supportedCredentials.get(credentialConfigurationId);
            if (config == null) {
                logger.warnf("Credential configuration '%s' not found in supported credentials, skipping", credentialConfigurationId);
                continue;
            }

            String credentialIdentifier = UUID.randomUUID().toString();

            // Store the mapping between credential identifier and configuration ID in client session
            // This will be used later when processing credential requests
            String mappingKey = OID4VCIssuerEndpoint.CREDENTIAL_IDENTIFIER_PREFIX + credentialIdentifier;
            clientSession.setNote(mappingKey, credentialConfigurationId);

            logger.debugf("Generated credential identifier '%s' for configuration '%s'",
                    credentialIdentifier, credentialConfigurationId);

            OID4VCAuthorizationDetailsResponse authDetail = new OID4VCAuthorizationDetailsResponse();
            authDetail.setType(OPENID_CREDENTIAL_TYPE);
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
    public List<AuthorizationDetailsResponse> handleMissingAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        return generateAuthorizationDetailsFromCredentialOffer(clientSession);
    }

    @Override
    public List<AuthorizationDetailsResponse> processStoredAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx, String storedAuthDetails) throws OAuthErrorException {
        if (storedAuthDetails == null) {
            return null;
        }

        logger.debugf("Processing stored authorization_details from authorization request: %s", storedAuthDetails);

        try {
            return process(userSession, clientSessionCtx, storedAuthDetails);
        } catch (RuntimeException e) {
            logger.warnf(e, "Error when processing stored authorization_details, cannot fulfill OID4VC requirement");
            // According to OID4VC spec, if authorization_details was used in authorization request,
            // it is required to be returned in token response. If it cannot be processed, return invalid_request error
            throw new OAuthErrorException(OAuthErrorException.INVALID_REQUEST, "authorization_details was used in authorization request but cannot be processed for token response: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}
