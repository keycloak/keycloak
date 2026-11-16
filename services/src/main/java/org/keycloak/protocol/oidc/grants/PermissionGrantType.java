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

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.authorization.AuthorizationTokenService;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.PermissionTokenRequestContext;
import org.keycloak.services.managers.AppAuthManager;

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

        String accessTokenString = null;
        String authorizationHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer")) {
            accessTokenString = new AppAuthManager().extractAuthorizationHeaderToken(headers);
        }

        // we allow public clients to authenticate using a bearer token, where the token should be a valid access token.
        // public clients don't have secret and should be able to obtain a RPT by providing an access token previously issued by the server
        if (accessTokenString != null) {
            AccessToken accessToken = Tokens.getAccessToken(session);

            if (accessToken == null) {
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

            ClientModel client = realm.getClientByClientId(accessToken.getIssuedFor());

            session.getContext().setClient(client);

            cors.checkAllowedOrigins(session, client);
            event.client(client);

            checkClientPolicy(client);
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
            // in case no bearer token is provided, we force client authentication
            checkClient();

            checkClientPolicy(client);

            // if a claim token is provided, we check if the format is a OpenID Connect IDToken and assume the token represents the identity asking for permissions
            if (AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN.equalsIgnoreCase(claimTokenFormat)) {
                accessTokenString = claimToken;
            } else if (subjectToken != null) {
                accessTokenString = subjectToken;
            } else {
                // Clients need to authenticate in order to obtain a RPT from the server.
                // In order to support cases where the client is obtaining permissions on its on behalf, we issue a temporary access token
                OAuth2GrantType clientCredentialsGrant = session.getProvider(OAuth2GrantType.class, OAuth2Constants.CLIENT_CREDENTIALS);
                context.setClient(client);
                context.setClientConfig(clientConfig);
                context.setClientAuthAttributes(clientAuthAttributes);
                accessTokenString = AccessTokenResponse.class.cast(clientCredentialsGrant.process(context).getEntity()).getToken();
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
            AccessToken accessToken = session.tokens().decode(rpt, AccessToken.class);
            if (accessToken == null) {
                String errorMessage = "RPT signature is invalid";
                event.detail(Details.REASON, errorMessage);
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, "invalid_rpt", errorMessage, Response.Status.FORBIDDEN);
            }

            authorizationRequest.setRpt(accessToken);
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

        Response authorizationResponse = AuthorizationTokenService.instance().authorize(authorizationRequest);

        event.success();

        return authorizationResponse;
    }

    // triggered once the requesting client is resolved and before any token is issued
    private void checkClientPolicy(ClientModel requestingClient) {
        try {
            session.clientPolicy().triggerOnEvent(new PermissionTokenRequestContext(formParams, requestingClient));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public EventType getEventType() {
        return EventType.PERMISSION_TOKEN;
    }

    @Override
    public Set<String> getTokenParameterNames() {
        return Set.of("claim_token", "subject_token");
    }

}
