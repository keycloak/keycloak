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
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessor;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.protocol.oid4vc.model.Format;

import static org.keycloak.models.Constants.AUTHORIZATION_DETAILS_RESPONSE;
import static org.keycloak.protocol.oid4vc.model.Format.SUPPORTED_FORMATS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        List<String> supportedFormats = new ArrayList<>(SUPPORTED_FORMATS);
        Map<String, SupportedCredentialConfiguration> supportedCredentials = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session);
        List<AuthorizationDetailsResponse> authDetailsResponse = new ArrayList<>();

        // Retrieve authorization servers and issuer identifier for locations check
        List<String> authorizationServers = OID4VCIssuerWellKnownProvider.getAuthorizationServers(session);
        String issuerIdentifier = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());

        for (AuthorizationDetail detail : authDetails) {
            validateAuthorizationDetail(detail, supportedFormats, supportedCredentials, authorizationServers, issuerIdentifier);
            AuthorizationDetailsResponse responseDetail = buildAuthorizationDetailResponse(detail, userSession, supportedCredentials, supportedFormats, clientSessionCtx);
            authDetailsResponse.add(responseDetail);
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

    private void validateAuthorizationDetail(AuthorizationDetail detail, List<String> supportedFormats, Map<String, SupportedCredentialConfiguration> supportedCredentials, List<String> authorizationServers, String issuerIdentifier) {
        String type = detail.getType();
        String credentialConfigurationId = detail.getCredentialConfigurationId();
        String format = detail.getFormat();
        Object vct = detail.getAdditionalFields().get("vct");

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

        // Ensure exactly one of credential_configuration_id or format is present
        if ((credentialConfigurationId == null && format == null) || (credentialConfigurationId != null && format != null)) {
            logger.warnf("Exactly one of credential_configuration_id or format must be present. credentialConfigurationId: %s, format: %s", credentialConfigurationId, format);
            throw getInvalidRequestException("Invalid authorization_details: credentialConfigurationId=" + credentialConfigurationId + ", format=" + format + ". Exactly one must be present.");
        }

        if (credentialConfigurationId != null) {
            // Validate credential_configuration_id
            SupportedCredentialConfiguration config = supportedCredentials.get(credentialConfigurationId);
            if (config == null) {
                logger.warnf("Unsupported credential_configuration_id: %s", credentialConfigurationId);
                throw getInvalidRequestException("Invalid credential configuration: unsupported credential_configuration_id=" + credentialConfigurationId);
            }
        } else {
            // Validate format
            if (!supportedFormats.contains(format)) {
                logger.warnf("Unsupported format: %s", format);
                throw getInvalidRequestException("Invalid credential format: unsupported format=" + format + ", supported=" + supportedFormats);
            }

            // SD-JWT VC: vct is REQUIRED and must match a supported credential configuration
            if (Format.SD_JWT_VC.equals(format)) {
                if (!(vct instanceof String) || ((String) vct).isEmpty()) {
                    logger.warnf("Missing or invalid vct for format %s", Format.SD_JWT_VC);
                    throw getInvalidRequestException(String.format("Missing or invalid vct for format=%s", Format.SD_JWT_VC));
                }
                boolean vctSupported = supportedCredentials.values().stream()
                        .filter(config -> format.equals(config.getFormat()))
                        .anyMatch(config -> vct.equals(config.getVct()));
                if (!vctSupported) {
                    logger.warnf("Unsupported vct for format %s: %s", format, vct);
                    throw getInvalidRequestException("Invalid credential configuration: unsupported vct=" + vct + " for format=" + format);
                }
            } else {
                // For other formats, do not require vct; allow for future format-specific fields in additionalFields
                // No-op for now
            }
        }
    }

    private AuthorizationDetailsResponse buildAuthorizationDetailResponse(AuthorizationDetail detail, UserSessionModel userSession, Map<String, SupportedCredentialConfiguration> supportedCredentials, List<String> supportedFormats, ClientSessionContext clientSessionCtx) {
        String credentialConfigurationId = detail.getCredentialConfigurationId();
        String format = detail.getFormat();
        Object vct = detail.getAdditionalFields().get("vct");

        // Try to reuse identifier from authorizationDetailsResponse in client session context
        List<AuthorizationDetailsResponse> previousResponses = clientSessionCtx.getAttribute(AUTHORIZATION_DETAILS_RESPONSE, List.class);
        List<String> credentialIdentifiers = null;
        if (previousResponses != null) {
            for (AuthorizationDetailsResponse prev : previousResponses) {
                if (prev instanceof OID4VCAuthorizationDetailsResponse) {
                    OID4VCAuthorizationDetailsResponse oid4vcPrev = (OID4VCAuthorizationDetailsResponse) prev;
                    if ((credentialConfigurationId != null && credentialConfigurationId.equals(oid4vcPrev.getCredentialConfigurationId())) ||
                            (credentialConfigurationId == null && format != null && format.equals(oid4vcPrev.getFormat()))) {
                        credentialIdentifiers = oid4vcPrev.getCredentialIdentifiers();
                        break;
                    }
                }
            }
        }
        if (credentialIdentifiers == null) {
            credentialIdentifiers = new ArrayList<>();
            credentialIdentifiers.add(UUID.randomUUID().toString());
        }

        OID4VCAuthorizationDetailsResponse responseDetail = new OID4VCAuthorizationDetailsResponse();
        responseDetail.setType(OPENID_CREDENTIAL_TYPE);
        responseDetail.setCredentialIdentifiers(credentialIdentifiers);
        if (credentialConfigurationId != null) {
            responseDetail.setCredentialConfigurationId(credentialConfigurationId);
        } else {
            responseDetail.setFormat(format);
            if (Format.SD_JWT_VC.equals(format) && vct != null) {
                responseDetail.getOtherClaims().put("vct", vct);
            }
        }
        return responseDetail;
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}
