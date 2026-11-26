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
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.authorization.AuthorizationTokenService;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.verifier.TokenVerificationContext;
import org.keycloak.protocol.oidc.verifier.TokenVerifierProviderManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * User-Managed Access (UMA) 2.0 Grant for OAuth 2.0 Authorization
 * https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html#uma-grant-type
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a> (et al.)
 */
public class PermissionGrantType extends OAuth2GrantTypeBase {

    @Override
    public Response process(Context context) {
        setContext(context);

        event.detail(Details.AUTH_METHOD, "oauth_credentials");

        AppAuthManager.AuthHeader authHeader = AppAuthManager.extractAuthorizationHeaderTokenOrReturnNull(headers);
        String accessTokenString = authHeader == null ? null : authHeader.getToken();
        AccessToken verifiedAccessToken = null;

        // Public clients can obtain an RPT using an access token instead of client credentials.
        if (accessTokenString != null) {
            verifiedAccessToken = Tokens.getAccessToken(session);

            if (verifiedAccessToken == null) {
                // This allows SPA clients to receive CORS headers on error responses (e.g. expired tokens, disabled users)
                AccessToken signatureVerifiedToken = session.tokens().decode(accessTokenString, AccessToken.class);
                if (signatureVerifiedToken != null) {
                    ClientModel clientFromToken = realm.getClientByClientId(signatureVerifiedToken.getIssuedFor());
                    cors.checkAllowedOrigins(session, clientFromToken);
                    event.client(clientFromToken);
                }
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Invalid bearer token", Response.Status.BAD_REQUEST);
            }

            ClientModel client = realm.getClientByClientId(verifiedAccessToken.getIssuedFor());

            session.getContext().setClient(client);

            cors.checkAllowedOrigins(session, client);
            event.client(client);
        }

        String claimToken = null;

        // claim_token is optional, if provided we just grab it from the request
        if (formParams.containsKey("claim_token")) {
            claimToken = formParams.get("claim_token").get(0);
        }

        String claimTokenFormat = formParams.getFirst("claim_token_format");

        if (claimToken != null && claimTokenFormat == null) {
            claimTokenFormat = AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN;
        }

        String subjectToken = formParams.getFirst("subject_token");

        if (accessTokenString == null) {
            // if a claim token is provided, we check if the format is a OpenID Connect IDToken and assume the token represents the identity asking for permissions
            if (AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN.equalsIgnoreCase(claimTokenFormat)) {
                accessTokenString = claimToken;
            } else if (subjectToken != null) {
                accessTokenString = subjectToken;
            } else {
                // Issue a temporary token for clients requesting permissions on their own behalf.
                OAuth2GrantType clientCredentialsGrant = session.getProvider(OAuth2GrantType.class, OAuth2Constants.CLIENT_CREDENTIALS);
                accessTokenString = AccessTokenResponse.class.cast(clientCredentialsGrant.process(context).getEntity()).getToken();
                AuthenticationManager.AuthResult result = AuthenticationManager.verifyAccessToken(session, accessTokenString);
                if (result == null) {
                    event.error(Errors.INVALID_TOKEN);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Invalid client credentials access token", Response.Status.BAD_REQUEST);
                }
                verifiedAccessToken = result.token();
            }
        }

        AuthorizationTokenService.KeycloakAuthorizationRequest authorizationRequest = new AuthorizationTokenService.KeycloakAuthorizationRequest(session.getProvider(AuthorizationProvider.class),
                tokenManager, event, this.request, cors, clientConnection);

        authorizationRequest.setTicket(formParams.getFirst("ticket"));
        authorizationRequest.setClaimToken(claimToken);
        authorizationRequest.setClaimTokenFormat(claimTokenFormat);
        authorizationRequest.setPct(formParams.getFirst("pct"));

        String rpt = formParams.getFirst("rpt");

        if (rpt != null) {
            authorizationRequest.setRpt(authHeader != null && rpt.equals(authHeader.getToken())
                    ? verifiedAccessToken : verifyPreviousRpt(rpt));
        }

        authorizationRequest.setScope(formParams.getFirst("scope"));
        String audienceParam = formParams.getFirst("audience");
        authorizationRequest.setAudience(audienceParam);
        authorizationRequest.setSubjectToken(accessTokenString);

        event.detail(Details.AUDIENCE, audienceParam);

        String submitRequest = formParams.getFirst("submit_request");

        authorizationRequest.setSubmitRequest(submitRequest == null ? true : Boolean.valueOf(submitRequest));

        // permissions have a format like RESOURCE#SCOPE1,SCOPE2
        List<String> permissions = formParams.get("permission");
        String responsePermissionsLimit = formParams.getFirst("response_permissions_limit");
        Integer maxResults = responsePermissionsLimit != null ? Integer.parseInt(responsePermissionsLimit) : null;

        if (permissions != null) {
            event.detail(Details.PERMISSION, String.join("|", permissions));
            String permissionResourceFormat = formParams.getFirst("permission_resource_format");
            boolean permissionResourceMatchingUri = Boolean.parseBoolean(formParams.getFirst("permission_resource_matching_uri"));
            authorizationRequest.addPermissions(permissions, permissionResourceFormat, permissionResourceMatchingUri, maxResults);
        }

        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();

        String responseIncludeResourceName = formParams.getFirst("response_include_resource_name");

        if (responseIncludeResourceName != null) {
            metadata.setIncludeResourceName(Boolean.parseBoolean(responseIncludeResourceName));
        }

        if (responsePermissionsLimit != null) {
            metadata.setLimit(maxResults);
        }

        metadata.setResponseMode(formParams.getFirst("response_mode"));

        authorizationRequest.setMetadata(metadata);

        Response authorizationResponse = AuthorizationTokenService.instance().authorize(authorizationRequest, verifiedAccessToken);

        event.success();

        return authorizationResponse;
    }

    private AccessToken verifyPreviousRpt(String rpt) {
        try {
            AccessToken token = session.tokens().decode(rpt, AccessToken.class);
            if (token == null) {
                throw new VerificationException("RPT signature is invalid");
            }
            TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(token);
            new TokenVerifierProviderManager().addResourceAccessChecks(verifier, TokenVerificationContext.forResourceParameter(session, rpt));
            return verifier.verify().getToken();
        } catch (VerificationException e) {
            event.detail(Details.REASON, e.getMessage()).error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, "invalid_rpt", e.getMessage(), Response.Status.FORBIDDEN);
        }
    }

    @Override
    public EventType getEventType() {
        return EventType.PERMISSION_TOKEN;
    }

    @Override
    public Set<String> getTokenParameterNames() {
        return Set.of("claim_token", "subject_token", "rpt");
    }

}
