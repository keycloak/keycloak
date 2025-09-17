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

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.util.JsonSerialization;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessor;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.keycloak.protocol.oid4vc.utils.ClaimsPathPointer;

import static org.keycloak.models.Constants.AUTHORIZATION_DETAILS_RESPONSE;

public class OID4VCAuthorizationDetailsProcessor implements AuthorizationDetailsProcessor {
    private static final Logger logger = Logger.getLogger(OID4VCAuthorizationDetailsProcessor.class);
    private final KeycloakSession session;

    public static final String OPENID_CREDENTIAL_TYPE = "openid_credential";

    public OID4VCAuthorizationDetailsProcessor(KeycloakSession session) {
        this.session = session;
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
            AuthorizationDetailsResponse responseDetail = buildAuthorizationDetailResponse(detail, userSession, supportedCredentials, clientSessionCtx);
            authDetailsResponse.add(responseDetail);
        }

        if (authDetailsResponse.isEmpty()) {
            throw getInvalidRequestException("Invalid authorization_details: no valid authorization details found");
        }

        return authDetailsResponse;
    }

    private List<AuthorizationDetail> parseAuthorizationDetails(String authorizationDetailsParam) {
        try {
            return JsonSerialization.readValue(authorizationDetailsParam, new TypeReference<List<AuthorizationDetail>>() {
            });
        } catch (Exception e) {
            logger.warnf(e, "Invalid authorization_details format: %s", authorizationDetailsParam);
            throw getInvalidRequestException("Invalid authorization_details format: " + authorizationDetailsParam);
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

        // If authorization_servers is present, locations must be set to issuer identifier
        if (authorizationServers != null && !authorizationServers.isEmpty() && OPENID_CREDENTIAL_TYPE.equals(type)) {
            List<String> locations = detail.getLocations();
            if (locations == null || locations.size() != 1 || !issuerIdentifier.equals(locations.get(0))) {
                logger.warnf("Invalid locations field in authorization_details: %s, expected: %s", locations, issuerIdentifier);
                throw getInvalidRequestException("Invalid authorization_details: locations=" + locations + ", expected=" + issuerIdentifier);
            }
        }

        // Validate type
        if (!OPENID_CREDENTIAL_TYPE.equals(type)) {
            logger.warnf("Invalid authorization_details type: %s", type);
            throw getInvalidRequestException("Invalid authorization_details type: " + type + ", expected=" + OPENID_CREDENTIAL_TYPE);
        }

        // credential_configuration_id is REQUIRED
        if (credentialConfigurationId == null) {
            logger.warnf("Missing credential_configuration_id in authorization_details");
            throw getInvalidRequestException("Invalid authorization_details: credential_configuration_id is required");
        }

        // Validate credential_configuration_id
        SupportedCredentialConfiguration config = supportedCredentials.get(credentialConfigurationId);
        if (config == null) {
            logger.warnf("Unsupported credential_configuration_id: %s", credentialConfigurationId);
            throw getInvalidRequestException("Invalid credential configuration: unsupported credential_configuration_id=" + credentialConfigurationId);
        }


        // Validate claims if present
        if (claims != null && !claims.isEmpty()) {
            validateClaims(claims, supportedCredentials, credentialConfigurationId);
        }
    }

    /**
     * Validates that the requested claims are supported by the credential configuration.
     * This performs semantic validation by checking if Keycloak supports the requested claims.
     *
     * @param claims                    the list of claims to validate
     * @param supportedCredentials      map of supported credential configurations
     * @param credentialConfigurationId the ID of the credential configuration
     */
    private void validateClaims(List<ClaimsDescription> claims, Map<String, SupportedCredentialConfiguration> supportedCredentials, String credentialConfigurationId) {
        SupportedCredentialConfiguration config = supportedCredentials.get(credentialConfigurationId);

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

    private AuthorizationDetailsResponse buildAuthorizationDetailResponse(AuthorizationDetail detail, UserSessionModel userSession, Map<String, SupportedCredentialConfiguration> supportedCredentials, ClientSessionContext clientSessionCtx) {
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

        // Store claims and credential context in user session notes for later use during credential issuance
        if (detail.getClaims() != null) {
            // Store claims with a unique key based on credential configuration ID
            String claimsKey = "AUTHORIZATION_DETAILS_CLAIMS_" + credentialConfigurationId;
            try {
                userSession.setNote(claimsKey, JsonSerialization.writeValueAsString(detail.getClaims()));
            } catch (Exception e) {
                logger.warnf(e, "Failed to store claims in user session for credential configuration %s", credentialConfigurationId);
            }

            // Store credential context mapping using credential identifier as key
            for (String credentialIdentifier : credentialIdentifiers) {
                String contextKey = "CREDENTIAL_CONTEXT_" + credentialIdentifier;
                try {
                    // Store the complete credential context for later retrieval
                    Map<String, Object> credentialContext = Map.of(
                            "credentialConfigurationId", credentialConfigurationId,
                            "claims", detail.getClaims(),
                            "type", OPENID_CREDENTIAL_TYPE
                    );
                    userSession.setNote(contextKey, JsonSerialization.writeValueAsString(credentialContext));
                } catch (Exception e) {
                    logger.warnf(e, "Failed to store credential context for identifier %s", credentialIdentifier);
                }
            }

            // Include claims in response
            responseDetail.setClaims(detail.getClaims());
        }

        return responseDetail;
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}
