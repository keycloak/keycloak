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
package org.keycloak.broker.oidc;

import com.fasterxml.jackson.databind.JsonNode;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ExchangeExternalToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.vault.VaultStringSecret;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.security.PublicKey;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProvider extends AbstractOAuth2IdentityProvider<OIDCIdentityProviderConfig> implements ExchangeExternalToken {
    protected static final Logger logger = Logger.getLogger(OIDCIdentityProvider.class);

    public static final String SCOPE_OPENID = "openid";
    public static final String FEDERATED_ID_TOKEN = "FEDERATED_ID_TOKEN";
    public static final String USER_INFO = "UserInfo";
    public static final String FEDERATED_ACCESS_TOKEN_RESPONSE = "FEDERATED_ACCESS_TOKEN_RESPONSE";
    public static final String VALIDATED_ID_TOKEN = "VALIDATED_ID_TOKEN";
    public static final String ACCESS_TOKEN_EXPIRATION = "accessTokenExpiration";
    public static final String EXCHANGE_PROVIDER = "EXCHANGE_PROVIDER";
    private static final String BROKER_NONCE_PARAM = "BROKER_NONCE";

    public OIDCIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);

        String defaultScope = config.getDefaultScope();

        if (!defaultScope.contains(SCOPE_OPENID)) {
            config.setDefaultScope((SCOPE_OPENID + " " + defaultScope).trim());
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new OIDCEndpoint(callback, realm, event);
    }

    /**
     * Returns access token response as a string from a refresh token invocation on the remote OIDC broker
     *
     * @param session
     * @param userSession
     * @return
     */
    public String refreshTokenForLogout(KeycloakSession session, UserSessionModel userSession) {
        String refreshToken = userSession.getNote(FEDERATED_REFRESH_TOKEN);
        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            return getRefreshTokenRequest(session, refreshToken, getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret())).asString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        if (getConfig().getLogoutUrl() == null || getConfig().getLogoutUrl().trim().equals("") || !getConfig().isBackchannelSupported())
            return;
        String idToken = getIDTokenForLogout(session, userSession);
        if (idToken == null) return;
        backchannelLogout(userSession, idToken);
    }

    protected void backchannelLogout(UserSessionModel userSession, String idToken) {
        String sessionId = userSession.getId();
        UriBuilder logoutUri = UriBuilder.fromUri(getConfig().getLogoutUrl())
                .queryParam("state", sessionId);
        logoutUri.queryParam("id_token_hint", idToken);
        String url = logoutUri.build().toString();
        try {
            int status = SimpleHttp.doGet(url, session).asStatus();
            boolean success = status >= 200 && status < 400;
            if (!success) {
                logger.warn("Failed backchannel broker logout to: " + url);
            }
        } catch (Exception e) {
            logger.warn("Failed backchannel broker logout to: " + url, e);
        }
    }


    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        if (getConfig().getLogoutUrl() == null || getConfig().getLogoutUrl().trim().equals("")) return null;
        String idToken = getIDTokenForLogout(session, userSession);
        if (idToken != null && getConfig().isBackchannelSupported()) {
            backchannelLogout(userSession, idToken);
            return null;
        } else {
            String sessionId = userSession.getId();
            UriBuilder logoutUri = UriBuilder.fromUri(getConfig().getLogoutUrl())
                    .queryParam("state", sessionId);
            if (idToken != null) logoutUri.queryParam("id_token_hint", idToken);
            String redirect = RealmsResource.brokerUrl(uriInfo)
                    .path(IdentityBrokerService.class, "getEndpoint")
                    .path(OIDCEndpoint.class, "logoutResponse")
                    .build(realm.getName(), getConfig().getAlias()).toString();
            logoutUri.queryParam("post_logout_redirect_uri", redirect);
            Response response = Response.status(302).location(logoutUri.build()).build();
            return response;
        }
    }

    @Override
    protected Response exchangeStoredToken(UriInfo uriInfo, EventBuilder event, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        FederatedIdentityModel model = session.users().getFederatedIdentity(authorizedClient.getRealm(), tokenSubject, getConfig().getAlias());
        if (model == null || model.getToken() == null) {
            event.detail(Details.REASON, "requested_issuer is not linked");
            event.error(Errors.INVALID_TOKEN);
            return exchangeNotLinked(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            String modelTokenString = model.getToken();
            AccessTokenResponse tokenResponse = JsonSerialization.readValue(modelTokenString, AccessTokenResponse.class);
            Integer exp = (Integer) tokenResponse.getOtherClaims().get(ACCESS_TOKEN_EXPIRATION);
            if (exp != null && exp < Time.currentTime()) {
                if (tokenResponse.getRefreshToken() == null) {
                    return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
                }
                String response = getRefreshTokenRequest(session, tokenResponse.getRefreshToken(),
                        getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret())).asString();
                if (response.contains("error")) {
                    logger.debugv("Error refreshing token, refresh token expiration?: {0}", response);
                    model.setToken(null);
                    session.users().updateFederatedIdentity(authorizedClient.getRealm(), tokenSubject, model);
                    event.detail(Details.REASON, "requested_issuer token expired");
                    event.error(Errors.INVALID_TOKEN);
                    return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
                }
                AccessTokenResponse newResponse = JsonSerialization.readValue(response, AccessTokenResponse.class);
                if (newResponse.getExpiresIn() > 0) {
                    int accessTokenExpiration = Time.currentTime() + (int) newResponse.getExpiresIn();
                    newResponse.getOtherClaims().put(ACCESS_TOKEN_EXPIRATION, accessTokenExpiration);
                }

                if (newResponse.getRefreshToken() == null && tokenResponse.getRefreshToken() != null) {
                    newResponse.setRefreshToken(tokenResponse.getRefreshToken());
                    newResponse.setRefreshExpiresIn(tokenResponse.getRefreshExpiresIn());
                }
                response = JsonSerialization.writeValueAsString(newResponse);

                String oldToken = tokenUserSession.getNote(FEDERATED_ACCESS_TOKEN);
                if (oldToken != null && oldToken.equals(tokenResponse.getToken())) {
                    int accessTokenExpiration = newResponse.getExpiresIn() > 0 ? Time.currentTime() + (int) newResponse.getExpiresIn() : 0;
                    tokenUserSession.setNote(FEDERATED_TOKEN_EXPIRATION, Long.toString(accessTokenExpiration));
                    tokenUserSession.setNote(FEDERATED_REFRESH_TOKEN, newResponse.getRefreshToken());
                    tokenUserSession.setNote(FEDERATED_ACCESS_TOKEN, newResponse.getToken());
                    tokenUserSession.setNote(FEDERATED_ID_TOKEN, newResponse.getIdToken());

                }
                model.setToken(response);
                tokenResponse = newResponse;
            } else if (exp != null) {
                tokenResponse.setExpiresIn(exp - Time.currentTime());
            }
            tokenResponse.setIdToken(null);
            tokenResponse.setRefreshToken(null);
            tokenResponse.setRefreshExpiresIn(0);
            tokenResponse.getOtherClaims().clear();
            tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);
            tokenResponse.getOtherClaims().put(ACCOUNT_LINK_URL, getLinkingUrl(uriInfo, authorizedClient, tokenUserSession));
            event.success();
            return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getIDTokenForLogout(KeycloakSession session, UserSessionModel userSession) {
        String tokenExpirationString = userSession.getNote(FEDERATED_TOKEN_EXPIRATION);
        long exp = tokenExpirationString == null ? 0 : Long.parseLong(tokenExpirationString);
        int currentTime = Time.currentTime();
        if (exp > 0 && currentTime > exp) {
            String response = refreshTokenForLogout(session, userSession);
            AccessTokenResponse tokenResponse = null;
            try {
                tokenResponse = JsonSerialization.readValue(response, AccessTokenResponse.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return tokenResponse.getIdToken();
        } else {
            return userSession.getNote(FEDERATED_ID_TOKEN);

        }
    }

    protected void processAccessTokenResponse(BrokeredIdentityContext context, AccessTokenResponse response) {


    }

    protected SimpleHttp getRefreshTokenRequest(KeycloakSession session, String refreshToken, String clientId, String clientSecret) {
        SimpleHttp refreshTokenRequest = SimpleHttp.doPost(getConfig().getTokenUrl(), session)
                .param(OAUTH2_GRANT_TYPE_REFRESH_TOKEN, refreshToken)
                .param(OAUTH2_PARAMETER_GRANT_TYPE, OAUTH2_GRANT_TYPE_REFRESH_TOKEN);
        return authenticateTokenRequest(refreshTokenRequest);
    }

    @Override
    protected Response exchangeSessionToken(UriInfo uriInfo, EventBuilder event, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        String refreshToken = tokenUserSession.getNote(FEDERATED_REFRESH_TOKEN);
        String accessToken = tokenUserSession.getNote(FEDERATED_ACCESS_TOKEN);
        String idToken = tokenUserSession.getNote(FEDERATED_ID_TOKEN);

        if (accessToken == null) {
            event.detail(Details.REASON, "requested_issuer is not linked");
            event.error(Errors.INVALID_TOKEN);
            return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            long expiration = Long.parseLong(tokenUserSession.getNote(FEDERATED_TOKEN_EXPIRATION));
            if (expiration == 0 || expiration > Time.currentTime()) {
                AccessTokenResponse tokenResponse = new AccessTokenResponse();
                tokenResponse.setExpiresIn(expiration);
                tokenResponse.setToken(accessToken);
                tokenResponse.setIdToken(null);
                tokenResponse.setRefreshToken(null);
                tokenResponse.setRefreshExpiresIn(0);
                tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);
                tokenResponse.getOtherClaims().put(ACCOUNT_LINK_URL, getLinkingUrl(uriInfo, authorizedClient, tokenUserSession));
                event.success();
                return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
            }
            String response = getRefreshTokenRequest(session, refreshToken, getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret())).asString();
            if (response.contains("error")) {
                logger.debugv("Error refreshing token, refresh token expiration?: {0}", response);
                event.detail(Details.REASON, "requested_issuer token expired");
                event.error(Errors.INVALID_TOKEN);
                return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
            }
            AccessTokenResponse newResponse = JsonSerialization.readValue(response, AccessTokenResponse.class);
            long accessTokenExpiration = newResponse.getExpiresIn() > 0 ? Time.currentTime() + newResponse.getExpiresIn() : 0;
            tokenUserSession.setNote(FEDERATED_TOKEN_EXPIRATION, Long.toString(accessTokenExpiration));
            tokenUserSession.setNote(FEDERATED_REFRESH_TOKEN, newResponse.getRefreshToken());
            tokenUserSession.setNote(FEDERATED_ACCESS_TOKEN, newResponse.getToken());
            tokenUserSession.setNote(FEDERATED_ID_TOKEN, newResponse.getIdToken());
            newResponse.setIdToken(null);
            newResponse.setRefreshToken(null);
            newResponse.setRefreshExpiresIn(0);
            newResponse.getOtherClaims().clear();
            newResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);
            newResponse.getOtherClaims().put(ACCOUNT_LINK_URL, getLinkingUrl(uriInfo, authorizedClient, tokenUserSession));
            event.success();
            return Response.ok(newResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected class OIDCEndpoint extends Endpoint {
        public OIDCEndpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event) {
            super(callback, realm, event);
        }

        @Override
        public SimpleHttp generateTokenRequest(String authorizationCode) {
            SimpleHttp simpleHttp = super.generateTokenRequest(authorizationCode);
            return simpleHttp;
        }

        @GET
        @Path("logout_response")
        public Response logoutResponse(@QueryParam("state") String state) {
            if (state == null){
                logger.error("no state parameter returned");
                EventBuilder event = new EventBuilder(realm, session, clientConnection);
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);

            }
            UserSessionModel userSession = session.sessions().getUserSession(realm, state);
            if (userSession == null) {
                logger.error("no valid user session");
                EventBuilder event = new EventBuilder(realm, session, clientConnection);
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.error("usersession in different state");
                EventBuilder event = new EventBuilder(realm, session, clientConnection);
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.SESSION_NOT_ACTIVE);
            }
            return AuthenticationManager.finishBrowserLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers);
        }
    }


    @Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        AccessTokenResponse tokenResponse = null;
        try {
            tokenResponse = JsonSerialization.readValue(response, AccessTokenResponse.class);
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not decode access token response.", e);
        }
        String accessToken = verifyAccessToken(tokenResponse);

        String encodedIdToken = tokenResponse.getIdToken();

        JsonWebToken idToken = validateToken(encodedIdToken);

        try {
            BrokeredIdentityContext identity = extractIdentity(tokenResponse, accessToken, idToken);
            
            if (!identity.getId().equals(idToken.getSubject())) {
                throw new IdentityBrokerException("Mismatch between the subject in the id_token and the subject from the user_info endpoint");
            }

            identity.getContextData().put(BROKER_NONCE_PARAM, idToken.getOtherClaims().get(OIDCLoginProtocol.NONCE_PARAM));
            
            if (getConfig().isStoreToken()) {
                if (tokenResponse.getExpiresIn() > 0) {
                    long accessTokenExpiration = Time.currentTime() + tokenResponse.getExpiresIn();
                    tokenResponse.getOtherClaims().put(ACCESS_TOKEN_EXPIRATION, accessTokenExpiration);
                    response = JsonSerialization.writeValueAsString(tokenResponse);
                }
                identity.setToken(response);
            }

            return identity;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not fetch attributes from userinfo endpoint.", e);
        }
    }

    private static final MediaType APPLICATION_JWT_TYPE = MediaType.valueOf("application/jwt");

    protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {
        String id = idToken.getSubject();
        BrokeredIdentityContext identity = new BrokeredIdentityContext(id);
        String name = (String) idToken.getOtherClaims().get(IDToken.NAME);
        String givenName = (String)idToken.getOtherClaims().get(IDToken.GIVEN_NAME);
        String familyName = (String)idToken.getOtherClaims().get(IDToken.FAMILY_NAME);
        String preferredUsername = (String) idToken.getOtherClaims().get(getusernameClaimNameForIdToken());
        String email = (String) idToken.getOtherClaims().get(IDToken.EMAIL);

        if (!getConfig().isDisableUserInfoService()) {
            String userInfoUrl = getUserInfoUrl();
            if (userInfoUrl != null && !userInfoUrl.isEmpty()) {

                if (accessToken != null) {
                    SimpleHttp.Response response = executeRequest(userInfoUrl, SimpleHttp.doGet(userInfoUrl, session).header("Authorization", "Bearer " + accessToken));
                    String contentType = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
                    MediaType contentMediaType;
                    try {
                        contentMediaType = MediaType.valueOf(contentType);
                    } catch (IllegalArgumentException ex) {
                        contentMediaType = null;
                    }
                    if (contentMediaType == null || contentMediaType.isWildcardSubtype() || contentMediaType.isWildcardType()) {
                        throw new RuntimeException("Unsupported content-type [" + contentType + "] in response from [" + userInfoUrl + "].");
                    }
                    JsonNode userInfo;

                    if (MediaType.APPLICATION_JSON_TYPE.isCompatible(contentMediaType)) {
                        userInfo = response.asJson();
                    } else if (APPLICATION_JWT_TYPE.isCompatible(contentMediaType)) {
                        JWSInput jwsInput;

                        try {
                            jwsInput = new JWSInput(response.asString());
                        } catch (JWSInputException cause) {
                            throw new RuntimeException("Failed to parse JWT userinfo response", cause);
                        }

                        if (verify(jwsInput)) {
                            userInfo = JsonSerialization.readValue(jwsInput.getContent(), JsonNode.class);
                        } else {
                            throw new RuntimeException("Failed to verify signature of userinfo response from [" + userInfoUrl + "].");
                        }
                    } else {
                        throw new RuntimeException("Unsupported content-type [" + contentType + "] in response from [" + userInfoUrl + "].");
                    }

                    id = getJsonProperty(userInfo, "sub");
                    name = getJsonProperty(userInfo, "name");
                    givenName = getJsonProperty(userInfo, IDToken.GIVEN_NAME);
                    familyName = getJsonProperty(userInfo, IDToken.FAMILY_NAME);
                    preferredUsername = getUsernameFromUserInfo(userInfo);
                    email = getJsonProperty(userInfo, "email");
                    AbstractJsonUserAttributeMapper.storeUserProfileForMapper(identity, userInfo, getConfig().getAlias());
                }
            }
        }
        identity.getContextData().put(VALIDATED_ID_TOKEN, idToken);

        identity.setId(id);

        if (givenName != null) {
            identity.setFirstName(givenName);
        }

        if (familyName != null) {
            identity.setLastName(familyName);
        }

        if (givenName == null && familyName == null) {
            identity.setName(name);
        }

        identity.setEmail(email);

        identity.setBrokerUserId(getConfig().getAlias() + "." + id);

        if (preferredUsername == null) {
            preferredUsername = email;
        }

        if (preferredUsername == null) {
            preferredUsername = id;
        }

        identity.setUsername(preferredUsername);
        if (tokenResponse != null && tokenResponse.getSessionState() != null) {
            identity.setBrokerSessionId(getConfig().getAlias() + "." + tokenResponse.getSessionState());
        }
        if (tokenResponse != null) identity.getContextData().put(FEDERATED_ACCESS_TOKEN_RESPONSE, tokenResponse);
        if (tokenResponse != null) processAccessTokenResponse(identity, tokenResponse);
        
        return identity;
    }

    protected String getusernameClaimNameForIdToken() {
        return IDToken.PREFERRED_USERNAME;
    }

    protected String getUserInfoUrl() {
        return getConfig().getUserInfoUrl();
    }

    private SimpleHttp.Response executeRequest(String url, SimpleHttp request) throws IOException {
        SimpleHttp.Response response = request.asResponse();
        if (response.getStatus() != 200) {
            String msg = "failed to invoke url [" + url + "]";
            try {
                String tmp = response.asString();
                if (tmp != null) msg = tmp;

            } catch (IOException e) {

            }
            throw new IdentityBrokerException("Failed to invoke url [" + url + "]: " + msg);
        }
        return  response;
    }

    private String verifyAccessToken(AccessTokenResponse tokenResponse) {
        String accessToken = tokenResponse.getToken();

        if (accessToken == null) {
            throw new IdentityBrokerException("No access_token from server. error='" + tokenResponse.getError() +
                    "', error_description='" + tokenResponse.getErrorDescription() +
                    "', error_uri='" + tokenResponse.getErrorUri() + "'");
        }
        return accessToken;
    }

    protected boolean verify(JWSInput jws) {
        if (!getConfig().isValidateSignature()) return true;

        try {
            PublicKey publicKey = PublicKeyStorageManager.getIdentityProviderPublicKey(session, session.getContext().getRealm(), getConfig(), jws);

            return publicKey != null && RSAProvider.verify(jws, publicKey);
        } catch (Exception e) {
            logger.debug("Failed to verify token", e);
            return false;
        }
    }

    public JsonWebToken validateToken(String encodedToken) {
        boolean ignoreAudience = false;

        return validateToken(encodedToken, ignoreAudience);
    }

    protected JsonWebToken validateToken(String encodedToken, boolean ignoreAudience) {
        if (encodedToken == null) {
            throw new IdentityBrokerException("No token from server.");
        }

        JsonWebToken token;
        try {
            JWSInput jws = new JWSInput(encodedToken);
            if (!verify(jws)) {
                throw new IdentityBrokerException("token signature validation failed");
            }
            token = jws.readJsonContent(JsonWebToken.class);
        } catch (JWSInputException e) {
            throw new IdentityBrokerException("Invalid token", e);
        }

        String iss = token.getIssuer();

        if (!token.isActive(getConfig().getAllowedClockSkew())) {
            throw new IdentityBrokerException("Token is no longer valid");
        }

        if (!ignoreAudience && !token.hasAudience(getConfig().getClientId())) {
            throw new IdentityBrokerException("Wrong audience from token.");
        }
        
        if (!ignoreAudience && (token.getIssuedFor() != null && !getConfig().getClientId().equals(token.getIssuedFor()))) {
            throw new IdentityBrokerException("Token issued for does not match client id");
        }

        String trustedIssuers = getConfig().getIssuer();

        if (trustedIssuers != null && trustedIssuers.length() > 0) {
            String[] issuers = trustedIssuers.split(",");

            for (String trustedIssuer : issuers) {
                if (iss != null && iss.equals(trustedIssuer.trim())) {
                    return token;
                }
            }

            throw new IdentityBrokerException("Wrong issuer from token. Got: " + iss + " expected: " + getConfig().getIssuer());
        }

        return token;
    }

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
        AccessTokenResponse tokenResponse = (AccessTokenResponse) context.getContextData().get(FEDERATED_ACCESS_TOKEN_RESPONSE);
        int currentTime = Time.currentTime();
        long expiration = tokenResponse.getExpiresIn() > 0 ? tokenResponse.getExpiresIn() + currentTime : 0;
        authSession.setUserSessionNote(FEDERATED_TOKEN_EXPIRATION, Long.toString(expiration));
        authSession.setUserSessionNote(FEDERATED_REFRESH_TOKEN, tokenResponse.getRefreshToken());
        authSession.setUserSessionNote(FEDERATED_ACCESS_TOKEN, tokenResponse.getToken());
        authSession.setUserSessionNote(FEDERATED_ID_TOKEN, tokenResponse.getIdToken());
    }

    @Override
    protected String getDefaultScopes() {
        return "openid";
    }

    @Override
    public boolean isIssuer(String issuer, MultivaluedMap<String, String> params) {
        if (!supportsExternalExchange()) return false;
        String requestedIssuer = params == null ? null : params.getFirst(OAuth2Constants.SUBJECT_ISSUER);
        if (requestedIssuer == null) requestedIssuer = issuer;
        if (requestedIssuer.equals(getConfig().getAlias())) return true;
        
        String trustedIssuers = getConfig().getIssuer();

        if (trustedIssuers != null && trustedIssuers.length() > 0) {
            String[] issuers = trustedIssuers.split(",");

            for (String trustedIssuer : issuers) {
                if (requestedIssuer.equals(trustedIssuer.trim())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    protected boolean supportsExternalExchange() {
        return true;
    }

    @Override
    protected String getProfileEndpointForValidation(EventBuilder event) {
        String userInfoUrl = getUserInfoUrl();
        if (getConfig().isDisableUserInfoService() || userInfoUrl == null || userInfoUrl.isEmpty()) {
            event.detail(Details.REASON, "user info service disabled");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);

        }
        return userInfoUrl;
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode userInfo) {
        String id = getJsonProperty(userInfo, "sub");
        if (id == null) {
            event.detail(Details.REASON, "sub claim is null from user info json");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
        }
        BrokeredIdentityContext identity = new BrokeredIdentityContext(id);

        String name = getJsonProperty(userInfo, "name");
        String preferredUsername = getUsernameFromUserInfo(userInfo);
        String givenName = getJsonProperty(userInfo, "given_name");
        String familyName = getJsonProperty(userInfo, "family_name");
        String email = getJsonProperty(userInfo, "email");

        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(identity, userInfo, getConfig().getAlias());

        identity.setId(id);
        
        if (givenName != null) {
            identity.setFirstName(givenName);
        }
        
        if (familyName != null) {
            identity.setLastName(familyName);
        }
        
        if (givenName == null && familyName == null) {
            identity.setName(name);
        }
        
        identity.setEmail(email);

        identity.setBrokerUserId(getConfig().getAlias() + "." + id);

        if (preferredUsername == null) {
            preferredUsername = email;
        }

        if (preferredUsername == null) {
            preferredUsername = id;
        }

        identity.setUsername(preferredUsername);
        return identity;
    }

    protected String getUsernameFromUserInfo(JsonNode userInfo) {
        return getJsonProperty(userInfo, "preferred_username");
    }

    final protected BrokeredIdentityContext validateJwt(EventBuilder event, String subjectToken, String subjectTokenType) {
        if (!getConfig().isValidateSignature()) {
            return validateExternalTokenThroughUserInfo(event, subjectToken, subjectTokenType);
        }
        event.detail("validation_method", "signature");
        if (getConfig().isUseJwksUrl()) {
            if (getConfig().getJwksUrl() == null) {
                event.detail(Details.REASON, "jwks url unset");
                event.error(Errors.INVALID_CONFIG);
                throw new ErrorResponseException(Errors.INVALID_CONFIG, "Invalid server config", Response.Status.BAD_REQUEST);
            }
        } else if (getConfig().getPublicKeySignatureVerifier() == null) {
            event.detail(Details.REASON, "public key unset");
            event.error(Errors.INVALID_CONFIG);
            throw new ErrorResponseException(Errors.INVALID_CONFIG, "Invalid server config", Response.Status.BAD_REQUEST);
        }

        JsonWebToken parsedToken = null;
        try {
            parsedToken = validateToken(subjectToken, true);
        } catch (IdentityBrokerException e) {
            logger.debug("Unable to validate token for exchange", e);
            event.detail(Details.REASON, "token validation failure");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
        }

        try {

            boolean idTokenType = OAuth2Constants.ID_TOKEN_TYPE.equals(subjectTokenType);
            BrokeredIdentityContext context = extractIdentity(null, idTokenType ? null : subjectToken, parsedToken);
            if (context == null) {
                event.detail(Details.REASON, "Failed to extract identity from token");
                event.error(Errors.INVALID_TOKEN);
                throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);

            }
            if (idTokenType) {
                context.getContextData().put(VALIDATED_ID_TOKEN, subjectToken);
            } else {
                context.getContextData().put(KeycloakOIDCIdentityProvider.VALIDATED_ACCESS_TOKEN, parsedToken);
            }
            context.getContextData().put(EXCHANGE_PROVIDER, getConfig().getAlias());
            context.setIdp(this);
            context.setIdpConfig(getConfig());
            return context;
        } catch (IOException e) {
            logger.debug("Unable to extract identity from identity token", e);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
        }


    }

    @Override
    protected BrokeredIdentityContext exchangeExternalImpl(EventBuilder event, MultivaluedMap<String, String> params) {
        if (!supportsExternalExchange()) return null;
        String subjectToken = params.getFirst(OAuth2Constants.SUBJECT_TOKEN);
        if (subjectToken == null) {
            event.detail(Details.REASON, OAuth2Constants.SUBJECT_TOKEN + " param unset");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "token not set", Response.Status.BAD_REQUEST);
        }
        String subjectTokenType = params.getFirst(OAuth2Constants.SUBJECT_TOKEN_TYPE);
        if (subjectTokenType == null) {
            subjectTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
        }
        if (OAuth2Constants.JWT_TOKEN_TYPE.equals(subjectTokenType) || OAuth2Constants.ID_TOKEN_TYPE.equals(subjectTokenType)) {
            return validateJwt(event, subjectToken, subjectTokenType);
        } else if (OAuth2Constants.ACCESS_TOKEN_TYPE.equals(subjectTokenType)) {
            return validateExternalTokenThroughUserInfo(event, subjectToken, subjectTokenType);
        } else {
            event.detail(Details.REASON, OAuth2Constants.SUBJECT_TOKEN_TYPE + " invalid");
            event.error(Errors.INVALID_TOKEN_TYPE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token type", Response.Status.BAD_REQUEST);
        }
    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        UriBuilder uriBuilder = super.createAuthorizationUrl(request);
        String nonce = Base64Url.encode(SecretGenerator.getInstance().randomBytes(16));
        AuthenticationSessionModel authenticationSession = request.getAuthenticationSession();

        authenticationSession.setClientNote(BROKER_NONCE_PARAM, nonce);
        uriBuilder.queryParam(OIDCLoginProtocol.NONCE_PARAM, nonce);

        return uriBuilder;
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context) {
        AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();
        
        if (authenticationSession == null) {
            // no interacting with the brokered OP, likely doing token exchanges
            return;
        }

        String nonce = (String) context.getContextData().get(BROKER_NONCE_PARAM);

        if (nonce == null) {
            throw new IdentityBrokerException("OpenID Provider [" + getConfig().getProviderId() + "] did not return a nonce");
        }

        String expectedNonce = authenticationSession.getClientNote(BROKER_NONCE_PARAM);

        if (!nonce.equals(expectedNonce)) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid nonce", Response.Status.BAD_REQUEST);
        }
    }
}
