/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.protocol.oidc.tokenexchange;

import java.util.List;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenCategory;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.TokenUtil;

/**
 * Issues an Identity Assertion JWT Authorization Grant (ID-JAG) via RFC 8693 token exchange, with
 * Keycloak acting as the enterprise IdP (MCP Enterprise-Managed Authorization / Cross-App Access).
 *
 * The client sends an ID token as the subject_token with requested_token_type=id-jag, and gets back
 * a short-lived JWT (typ=oauth-id-jag+jwt) that it redeems for an access token at the resource
 * authorization server. The other side of that exchange lives in JWTAuthorizationGrantType.
 *
 * https://datatracker.ietf.org/doc/draft-ietf-oauth-identity-assertion-authz-grant/
 */
public class IdentityAssertionGrantTokenExchangeProvider implements TokenExchangeProvider {

    // ID-JAGs are minted on demand and redeemed right away, so they are short-lived. 300s matches
    // the value used in the spec examples.
    static final int DEFAULT_ID_JAG_LIFESPAN_SECONDS = 300;

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public boolean supports(TokenExchangeContext context) {
        // Only handle requested_token_type=id-jag; anything else falls through to the other providers.
        String requestedTokenType = context.getParams().getRequestedTokenType();
        if (!OAuth2Constants.ID_JAG_TOKEN_TYPE.equals(requestedTokenType)) {
            context.setUnsupportedReason("Identity Assertion JWT issuance only supports requested_token_type="
                    + OAuth2Constants.ID_JAG_TOKEN_TYPE);
            return false;
        }
        return true;
    }

    @Override
    public Response exchange(TokenExchangeContext context) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        ClientModel client = context.getClient();
        EventBuilder event = context.getEvent();
        Cors cors = context.getCors();
        ClientConnection clientConnection = context.getClientConnection();
        TokenExchangeContext.Params params = context.getParams();

        event.detail(Details.REQUESTED_TOKEN_TYPE, OAuth2Constants.ID_JAG_TOKEN_TYPE);

        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientModel(client);

        // A public client can't issue an ID-JAG: it delegates the user's identity to a third-party
        // resource AS and so needs an authenticated, confidential client.
        if (client.isPublicClient()) {
            throw error(event, cors, Errors.INVALID_CLIENT, OAuthErrorException.INVALID_CLIENT,
                    "Public clients are not allowed to issue an Identity Assertion JWT", Response.Status.BAD_REQUEST);
        }
        if (!clientConfig.isIdentityAssertionGrantIssuanceEnabled()) {
            throw error(event, cors, Errors.NOT_ALLOWED, OAuthErrorException.UNAUTHORIZED_CLIENT,
                    "Client is not allowed to issue an Identity Assertion JWT", Response.Status.BAD_REQUEST);
        }

        // subject_token must be an ID token.
        String subjectToken = params.getSubjectToken();
        if (subjectToken == null) {
            throw error(event, cors, Errors.INVALID_REQUEST, OAuthErrorException.INVALID_REQUEST,
                    "Parameter '" + OAuth2Constants.SUBJECT_TOKEN + "' is required", Response.Status.BAD_REQUEST);
        }
        if (!OAuth2Constants.ID_TOKEN_TYPE.equals(params.getSubjectTokenType())) {
            throw error(event, cors, Errors.INVALID_REQUEST, OAuthErrorException.INVALID_REQUEST,
                    "Parameter '" + OAuth2Constants.SUBJECT_TOKEN_TYPE + "' must be " + OAuth2Constants.ID_TOKEN_TYPE,
                    Response.Status.BAD_REQUEST);
        }

        // audience = the resource AS issuer. Required, single-valued, and must be allow-listed for this client.
        String audience = getSingleAudience(event, cors, params.getAudience());
        if (!clientConfig.getIdentityAssertionGrantAllowedAudiences().contains(audience)) {
            event.detail(Details.AUDIENCE, audience);
            throw error(event, cors, Errors.NOT_ALLOWED, OAuthErrorException.ACCESS_DENIED,
                    "Audience is not allowed for this client", Response.Status.FORBIDDEN);
        }
        event.detail(Details.AUDIENCE, audience);

        // resource = the MCP server id. Optional; echoed into the JAG when present.
        String resource = getSingleResource(event, cors, params.getResource());

        // Verify the subject token. checkTokenType=false so an ID token (typ=ID) is accepted.
        AuthenticationManager.AuthResult authResult = AuthenticationManager.verifyIdentityToken(session, realm,
                session.getContext().getUri(), clientConnection, true, false, null, false, subjectToken,
                context.getHeaders(), verifier -> {});
        if (authResult == null) {
            throw error(event, cors, Errors.INVALID_TOKEN, OAuthErrorException.INVALID_GRANT,
                    "Invalid " + OAuth2Constants.SUBJECT_TOKEN, Response.Status.BAD_REQUEST);
        }

        UserModel user = authResult.user();
        AccessToken subjectJwt = authResult.token();
        event.user(user);
        event.detail(Details.USERNAME, user.getUsername());

        String scope = params.getScope();
        String idJag = signIdJag(session, buildIdJag(session, realm, clientConfig, client, subjectJwt, audience, resource, scope));

        AccessTokenResponse response = new AccessTokenResponse();
        response.setToken(idJag);
        response.setTokenType(TokenUtil.TOKEN_TYPE_NA);
        response.setExpiresIn(DEFAULT_ID_JAG_LIFESPAN_SECONDS);
        response.setOtherClaims(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ID_JAG_TOKEN_TYPE);
        if (scope != null) {
            response.setScope(scope);
        }

        event.success();
        return cors.add(Response.ok(response, MediaType.APPLICATION_JSON_TYPE));
    }

    // Builds the JAG claims. The typ header is set during signing, not here.
    private JsonWebToken buildIdJag(KeycloakSession session, RealmModel realm, OIDCAdvancedConfigWrapper clientConfig,
                                    ClientModel client, AccessToken subjectJwt, String audience, String resource, String scope) {
        int now = Time.currentTime();

        // client_id = the id this client uses at the resource AS (draft 6.1), which may differ from
        // its Keycloak clientId. Fall back to the clientId when not configured.
        String resourceServerClientId = clientConfig.getIdentityAssertionGrantClientId();
        if (resourceServerClientId == null || resourceServerClientId.isEmpty()) {
            resourceServerClientId = client.getClientId();
        }

        JsonWebToken idJag = new JsonWebToken()
                .id(KeycloakModelUtils.generateId())
                .issuer(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()))
                .subject(subjectJwt.getSubject())
                .audience(audience)
                .iat((long) now)
                .exp((long) (now + DEFAULT_ID_JAG_LIFESPAN_SECONDS));

        idJag.setOtherClaims("client_id", resourceServerClientId);
        if (resource != null) {
            idJag.setOtherClaims(OAuth2Constants.RESOURCE, resource);
        }
        if (scope != null) {
            idJag.setOtherClaims(OAuth2Constants.SCOPE, scope);
        }
        // email is optional - carried through for account linking when the subject token has it.
        if (subjectJwt.getEmail() != null) {
            idJag.setOtherClaims("email", subjectJwt.getEmail());
        }
        return idJag;
    }

    // Signs with the realm key and stamps the oauth-id-jag+jwt header so the resource AS recognises it.
    private String signIdJag(KeycloakSession session, JsonWebToken idJag) {
        String signatureAlgorithm = session.tokens().signatureAlgorithm(TokenCategory.ACCESS);
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
        SignatureSignerContext signer = signatureProvider.signer();
        return new JWSBuilder()
                .type(OAuth2Constants.IDENTITY_ASSERTION_JWT_HEADER_TYPE)
                .jsonContent(idJag)
                .sign(signer);
    }

    private String getSingleAudience(EventBuilder event, Cors cors, List<String> audiences) {
        if (audiences == null || audiences.isEmpty()) {
            throw error(event, cors, Errors.INVALID_REQUEST, OAuthErrorException.INVALID_REQUEST,
                    "Parameter '" + OAuth2Constants.AUDIENCE + "' is required", Response.Status.BAD_REQUEST);
        }
        if (audiences.size() > 1) {
            throw error(event, cors, Errors.INVALID_REQUEST, OAuthErrorException.INVALID_REQUEST,
                    "Only a single '" + OAuth2Constants.AUDIENCE + "' is supported for an Identity Assertion JWT",
                    Response.Status.BAD_REQUEST);
        }
        return audiences.get(0);
    }

    private String getSingleResource(EventBuilder event, Cors cors, List<String> resources) {
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        if (resources.size() > 1) {
            throw error(event, cors, Errors.INVALID_REQUEST, OAuthErrorException.INVALID_REQUEST,
                    "Only a single '" + OAuth2Constants.RESOURCE + "' is supported for an Identity Assertion JWT",
                    Response.Status.BAD_REQUEST);
        }
        return resources.get(0);
    }

    private CorsErrorResponseException error(EventBuilder event, Cors cors, String eventError, String oauthError,
                                             String description, Response.Status status) {
        event.detail(Details.REASON, description);
        event.error(eventError);
        return new CorsErrorResponseException(cors, oauthError, description, status);
    }

    @Override
    public void close() {
    }
}
