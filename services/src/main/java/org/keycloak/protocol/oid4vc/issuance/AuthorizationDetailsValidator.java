package org.keycloak.protocol.oid4vc.issuance;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage.CredentialOfferState;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;

/**
 * Validates and retrieves authorization details for OID4VCI credential requests.
 * Handles differences between Pre-Authorized Code and Authorization Code flows.
 * <p>
 * For Pre-Authorized flows, the offer state is the authoritative source.
 * For Authorization Code flows, the access token must contain authorization_details.
 */
public class AuthorizationDetailsValidator {
    private static final Logger LOGGER = Logger.getLogger(AuthorizationDetailsValidator.class);

    /**
     * Public controller method that orchestrates the authorization details validation.
     * Determines the flow type and delegates to the appropriate validation method.
     *
     * @param offerState       The credential offer state from the issuance flow
     * @param accessToken      The access token used for the request
     * @param getErrorResponse Callback to format error responses
     * @return The validated authorization details
     * @throws BadRequestException if validation fails
     */
    public static OID4VCAuthorizationDetailResponse getValidatedAuthorizationDetails(
            CredentialOfferState offerState,
            AccessToken accessToken,
            ErrorResponseProvider errorResponseProvider) {

        OID4VCAuthorizationDetailResponse authDetails = offerState.getAuthorizationDetails();
        Object tokenAuthDetails = accessToken.getOtherClaims().get(OAuth2Constants.AUTHORIZATION_DETAILS);

        // Prefer grant-type-aware detection: pre-authorized code present in offer state
        boolean isPreAuthorizedFlow = offerState.getPreAuthorizedCode().isPresent();

        if (isPreAuthorizedFlow) {
            return validatePreAuthorizedCodeFlow(authDetails, errorResponseProvider);
        } else {
            // Per OID4VCI spec, authorization_details in token response is REQUIRED only if sent in auth/token request.
            // If not sent in the request, it's optional. When absent from token, use offer state details.
            if (tokenAuthDetails == null) {
                return authDetails;
            }
            return validateAuthorizationCodeFlow(tokenAuthDetails, errorResponseProvider);
        }
    }

    /**
     * Private method validating Pre-Authorized Code flow authorization details.
     * In pre-authorized flows, the offer state contains the authorization details directly.
     *
     * @param authDetails      Authorization details from offer state
     * @param getErrorResponse Callback to format error responses
     * @return The validated authorization details
     * @throws BadRequestException if validation fails
     */
    private static OID4VCAuthorizationDetailResponse validatePreAuthorizedCodeFlow(
            OID4VCAuthorizationDetailResponse authDetails,
            ErrorResponseProvider errorResponseProvider) {

        if (authDetails == null) {
            var errorMessage = "Pre-Authorized Code flow requires authorization_details in credential offer state";
            LOGGER.debugf(errorMessage);
            throw new BadRequestException(errorResponseProvider.getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage).build());
        }

        // Validate type for pre-authorized flow
        validateAuthorizationDetailsType(authDetails, errorResponseProvider);
        return authDetails;
    }

    /**
     * Private method validating Authorization Code flow authorization details.
     * In authorization code flows, the access token contains the authorization details.
     *
     * @param tokenAuthDetails Authorization details from access token claims
     * @param getErrorResponse Callback to format error responses
     * @return The validated authorization details
     * @throws BadRequestException if validation fails
     */
    private static OID4VCAuthorizationDetailResponse validateAuthorizationCodeFlow(
            Object tokenAuthDetails,
            ErrorResponseProvider errorResponseProvider) {

        if (tokenAuthDetails == null) {
            var errorMessage = "Authorization Code flow requires authorization_details in access token";
            LOGGER.debugf(errorMessage);
            throw new BadRequestException(errorResponseProvider.getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage).build());
        }

        OID4VCAuthorizationDetailResponse authDetails = null;

        try {
            List<OID4VCAuthorizationDetailResponse> tokenAuthDetailsList = null;
            if (tokenAuthDetails instanceof List<?> list) {
                if (list.isEmpty()) {
                    throw new BadRequestException(errorResponseProvider.getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, "Empty authorization_details list in token").build());
                }
                // Try to convert generic List (e.g. List<Map>) to specific type
                tokenAuthDetailsList = JsonSerialization.readValue(JsonSerialization.writeValueAsString(tokenAuthDetails),
                        new TypeReference<List<OID4VCAuthorizationDetailResponse>>() {
                        });
            } else if (tokenAuthDetails instanceof String authDetailsStr) {
                if (authDetailsStr.isEmpty()) {
                    throw new BadRequestException(errorResponseProvider.getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, "Empty authorization_details string in token").build());
                }
                tokenAuthDetailsList = JsonSerialization.readValue(authDetailsStr,
                        new TypeReference<List<OID4VCAuthorizationDetailResponse>>() {
                        });
            } else {
                var errorMessage = "Invalid shape for authorization_details in token. Expected List or String, but got: " + tokenAuthDetails.getClass().getSimpleName();
                throw new BadRequestException(errorResponseProvider.getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage).build());
            }

            if (tokenAuthDetailsList != null && !tokenAuthDetailsList.isEmpty()) {
                authDetails = tokenAuthDetailsList.get(0);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            var errorMessage = "Malformed authorization_details in token: " + e.getMessage();
            LOGGER.debug(errorMessage, e);
            throw new BadRequestException(errorResponseProvider.getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage).build());
        } catch (Exception e) {
            LOGGER.debug("Unexpected error processing authorization_details in token", e);
            throw new BadRequestException(errorResponseProvider.getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, "Unexpected error processing authorization_details in token").build());
        }

        if (authDetails == null) {
            var errorMessage = "No authorization_details found in access token";
            throw new BadRequestException(errorResponseProvider.getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage).build());
        }

        // Validate type for authorization code flow
        validateAuthorizationDetailsType(authDetails, errorResponseProvider);
        return authDetails;
    }

    /**
     * Validates that authorization_details type is "openid_credential".
     *
     * @param authDetails      The authorization details to validate
     * @param getErrorResponse Callback to format error responses
     * @throws BadRequestException if type is invalid
     */
    private static void validateAuthorizationDetailsType(
            OID4VCAuthorizationDetailResponse authDetails,
            ErrorResponseProvider errorResponseProvider) {

        String authDetailsType = authDetails.getType();
        if (!OPENID_CREDENTIAL.equals(authDetailsType)) {
            var errorMessage = String.format("Invalid authorization_details type: %s, expected: %s",
                    authDetailsType, OPENID_CREDENTIAL);
            LOGGER.debugf(errorMessage);
            throw new BadRequestException(errorResponseProvider.getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage).build());
        }
    }

    /**
     * Functional interface for formatting error responses.
     * This abstraction allows the validator to remain independent of the endpoint implementation.
     */
    @FunctionalInterface
    public interface ErrorResponseProvider {
        Response.ResponseBuilder getErrorResponse(ErrorType type, String message);
    }
}
