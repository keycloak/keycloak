/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.endpoints;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenCategory;
import org.keycloak.TokenVerifier;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.CekManagementProvider;
import org.keycloak.crypto.ContentEncryptionProvider;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.TokenManager.NotBeforeCheck;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.UserInfoRequestContext;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.MtlsHoKTokenUtil;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.OAuth2Error;

import org.jboss.resteasy.reactive.NoCache;

/**
 * @author pedroigor
 */
public class UserInfoEndpoint {

    private final HttpRequest request;

    private final KeycloakSession session;

    private final ClientConnection clientConnection;

    private final org.keycloak.protocol.oidc.TokenManager tokenManager;
    private final AppAuthManager appAuthManager;
    private final RealmModel realm;
    private final OAuth2Error error;
    private Cors cors;
    private TokenForUserInfo tokenForUserInfo = new TokenForUserInfo();

    public UserInfoEndpoint(KeycloakSession session, org.keycloak.protocol.oidc.TokenManager tokenManager) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.realm = session.getContext().getRealm();
        this.tokenManager = tokenManager;
        this.appAuthManager = new AppAuthManager();
        this.error = new OAuth2Error().json(false)
                .session(session)
                .realm(realm);
        this.request = session.getContext().getHttpRequest();
    }

    @Path("/")
    @OPTIONS
    public Response issueUserInfoPreflight() {
        return Cors.builder().auth().preflight().add(Response.ok());
    }

    @Path("/")
    @GET
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JWT})
    public Response issueUserInfoGet() {
        setupCors();
        AppAuthManager.AuthHeader accessToken = AppAuthManager.extractAuthorizationHeaderTokenOrReturnNull(session.getContext().getRequestHeaders());
        authorization(accessToken);
        return issueUserInfo();
    }

    @Path("/")
    @POST
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JWT})
    public Response issueUserInfoPost() {
        setupCors();

        // Try header first
        HttpHeaders headers = request.getHttpHeaders();
        AppAuthManager.AuthHeader authHeader = AppAuthManager.extractAuthorizationHeaderTokenOrReturnNull(headers);
        authorization(authHeader);

        try {

            String contentType = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
            jakarta.ws.rs.core.MediaType mediaType = jakarta.ws.rs.core.MediaType.valueOf(contentType);

            if (jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(mediaType)) {
                MultivaluedMap<String, String> formParams = request.getDecodedFormParameters();
                checkAccessTokenDuplicated(formParams);
                String accessToken = formParams.getFirst(OAuth2Constants.ACCESS_TOKEN);
                authHeader = accessToken == null ? null : new AppAuthManager.AuthHeader(AppAuthManager.BEARER, accessToken);
                authorization(authHeader);
            }
        } catch (IllegalArgumentException e) {
            // not application/x-www-form-urlencoded, ignore
        }

        return issueUserInfo();
    }

    private Response issueUserInfo() {
        cors.allowAllOrigins();

        EventBuilder event = new EventBuilder(realm, session, clientConnection)
                .event(EventType.USER_INFO_REQUEST)
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);

        try {
            session.clientPolicy().triggerOnEvent(new UserInfoRequestContext(tokenForUserInfo));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw error.error(cpe.getError()).errorDescription(cpe.getErrorDetail()).status(cpe.getErrorStatus()).build();
        }

        if (tokenForUserInfo.getToken() == null) {
            event.detail(Details.REASON, "Missing token");
            event.error(Errors.INVALID_TOKEN);
            throw error.unauthorized();
        }

        AccessToken token;
        ClientModel clientModel = null;
        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenForUserInfo.getToken(), AccessToken.class).withDefaultChecks()
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

            verifier = DPoPUtil.withDPoPVerifier(verifier, realm, new DPoPUtil.Validator(session).request(request).uriInfo(session.getContext().getUri()).accessToken(tokenForUserInfo.getToken()));

            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);

            token = verifier.verify().getToken();

            if (!TokenUtil.hasScope(token.getScope(), OAuth2Constants.SCOPE_OPENID)) {
                String errorMessage = "Missing openid scope";
                event.detail(Details.REASON, errorMessage);
                event.error(Errors.ACCESS_DENIED);
                throw error.insufficientScope(errorMessage);
            }

            clientModel = realm.getClientByClientId(token.getIssuedFor());
            if (clientModel == null) {
                event.error(Errors.CLIENT_NOT_FOUND);
                throw error.invalidToken("Client not found");
            }

            cors.allowedOrigins(session, clientModel);

            TokenVerifier.createWithoutSignature(token)
                    .withChecks(NotBeforeCheck.forModel(clientModel), new TokenManager.TokenRevocationCheck(session))
                    .verify();
        } catch (VerificationException e) {
            if (clientModel == null) {
                cors.allowAllOrigins();
            }
            event.detail(Details.REASON, e.getMessage());
            event.error(Errors.INVALID_TOKEN);
            throw error.invalidToken("Token verification failed");
        }

        if (!clientModel.getProtocol().equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            String errorMessage = "Wrong client protocol";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_CLIENT);
            throw error.invalidToken(errorMessage);
        }

        session.getContext().setClient(clientModel);

        event.client(clientModel);

        if (!clientModel.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw error.invalidToken("Client disabled");
        }

        event.session(token.getSessionId());
        UserSessionUtil.UserSessionValidationResult userSessionValidation = UserSessionUtil.findValidSessionForAccessToken(session, realm, token, clientModel, (invalidUserSession -> {}));
        if (userSessionValidation.getError() != null) {
            event.error(userSessionValidation.getError());
            throw error.invalidToken(userSessionValidation.getError());
        }

        UserSessionModel userSession = userSessionValidation.getUserSession();

        UserModel userModel = userSession.getUser();
        if (userModel == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw error.invalidToken("User not found");
        }

        event.user(userModel)
                .detail(Details.USERNAME, userModel.getUsername());

        if (!userModel.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw error.invalidToken("User disabled");
        }

        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
        if (OIDCAdvancedConfigWrapper.fromClientModel(clientModel).isUseMtlsHokToken()) {
            if (!MtlsHoKTokenUtil.verifyTokenBindingWithClientCertificate(token, request, session)) {
                String errorMessage = "Client certificate missing, or its thumbprint and one in the refresh token did NOT match";
                event.detail(Details.REASON, errorMessage);
                event.error(Errors.NOT_ALLOWED);
                throw error.invalidToken(errorMessage);
            }
        }

        // Existence of authenticatedClientSession for our client already handled before
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientModel.getId());

        // Retrieve by access token scope parameter
        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, token.getScope(), session);

        AccessToken userInfo = new AccessToken();

        userInfo = tokenManager.transformUserInfoAccessToken(session, userInfo, userSession, clientSessionCtx);
        Map<String, Object> claims = tokenManager.generateUserInfoClaims(userInfo, userModel);

        Response.ResponseBuilder responseBuilder;
        OIDCAdvancedConfigWrapper cfg = OIDCAdvancedConfigWrapper.fromClientModel(clientModel);

        if (cfg.isUserInfoSignatureRequired()) {
            String issuerUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
            String audience = clientModel.getClientId();
            claims.put("iss", issuerUrl);
            claims.put("aud", audience);

            String signatureAlgorithm = session.tokens().signatureAlgorithm(TokenCategory.USERINFO);

            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
            SignatureSignerContext signer = signatureProvider.signer();

            String signedUserInfo = new JWSBuilder().type("JWT").jsonContent(claims).sign(signer);

            try {
                responseBuilder = Response.ok(cfg.isUserInfoEncryptionRequired() ? jweFromContent(signedUserInfo, "JWT") :
                        signedUserInfo).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JWT);
            } catch (RuntimeException re) {
                throw error.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            event.detail(Details.SIGNATURE_REQUIRED, "true");
            event.detail(Details.SIGNATURE_ALGORITHM, cfg.getUserInfoSignedResponseAlg());
        } else if (cfg.isUserInfoEncryptionRequired()) {
            try {
                responseBuilder = Response.ok(jweFromContent(JsonSerialization.writeValueAsString(claims), null))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JWT);
            } catch (RuntimeException | IOException ex) {
                throw error.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

            event.detail(Details.SIGNATURE_REQUIRED, "false");
        } else {
            responseBuilder = Response.ok(claims).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

            event.detail(Details.SIGNATURE_REQUIRED, "false");
        }

        event.success();

        return cors.add(responseBuilder);
    }

    private String jweFromContent(String content, String jweContentType) {
        String encryptedToken = null;

        String algAlgorithm = session.tokens().cekManagementAlgorithm(TokenCategory.USERINFO);
        String encAlgorithm = session.tokens().encryptAlgorithm(TokenCategory.USERINFO);

        CekManagementProvider cekManagementProvider = session.getProvider(CekManagementProvider.class, algAlgorithm);
        JWEAlgorithmProvider jweAlgorithmProvider = cekManagementProvider.jweAlgorithmProvider();

        ContentEncryptionProvider contentEncryptionProvider = session.getProvider(ContentEncryptionProvider.class, encAlgorithm);
        JWEEncryptionProvider jweEncryptionProvider = contentEncryptionProvider.jweEncryptionProvider();

        ClientModel client = session.getContext().getClient();

        KeyWrapper keyWrapper = PublicKeyStorageManager.getClientPublicKeyWrapper(session, client, JWK.Use.ENCRYPTION, algAlgorithm);
        if (keyWrapper == null) {
            throw new RuntimeException("can not get encryption KEK");
        }
        Key encryptionKek = keyWrapper.getPublicKey();
        String encryptionKekId = keyWrapper.getKid();
        try {
            encryptedToken = TokenUtil.jweKeyEncryptionEncode(encryptionKek, content.getBytes(StandardCharsets.UTF_8), algAlgorithm,
                    encAlgorithm, encryptionKekId, jweAlgorithmProvider, jweEncryptionProvider, jweContentType);
        } catch (JWEException e) {
            throw new RuntimeException(e);
        }
        return encryptedToken;
    }

    private void checkAccessTokenDuplicated(MultivaluedMap<String, String> formParams) {
        // If access_token is not provided, error is thrown in issueUserInfo().
        // Only checks duplication of access token parameter in this function.
        if (formParams.containsKey(OAuth2Constants.ACCESS_TOKEN) && formParams.get(OAuth2Constants.ACCESS_TOKEN).size() != 1) {
            throw error.invalidRequest("Duplicate parameter");
        }
    }

    private void setupCors() {
        cors = Cors.builder().auth().allowedMethods(request.getHttpMethod()).auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);
        error.cors(cors);
    }

    private void authorization(AppAuthManager.AuthHeader authHeader) {
        if (authHeader != null) {
            if (tokenForUserInfo.getToken() == null) {
                error.authScheme(authHeader.getScheme());
                tokenForUserInfo.setToken(authHeader.getToken());
            } else {
                throw error.cors(cors.allowAllOrigins()).invalidRequest("More than one method used for including an access token");
            }
        }
    }

    public static class TokenForUserInfo {

        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
