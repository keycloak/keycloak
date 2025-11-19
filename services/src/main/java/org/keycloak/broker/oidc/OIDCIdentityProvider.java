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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.authenticators.client.FederatedJWTClientValidator;
import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantIdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ClientAssertionIdentityProvider;
import org.keycloak.broker.provider.ExchangeExternalToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.JWTAuthorizationGrantProvider;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.JOSE;
import org.keycloak.jose.JOSEParser;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.keys.loader.OIDCIdentityProviderPublicKeyLoader;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.JWTAuthorizationGrantValidationContext;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenExchangeContext;
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
import org.keycloak.util.Booleans;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.vault.VaultStringSecret;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProvider extends AbstractOAuth2IdentityProvider<OIDCIdentityProviderConfig> implements ExchangeExternalToken, ClientAssertionIdentityProvider<OIDCIdentityProviderConfig>, JWTAuthorizationGrantProvider<OIDCIdentityProviderConfig> {
    protected static final Logger logger = Logger.getLogger(OIDCIdentityProvider.class);

    public static final String SCOPE_OPENID = "openid";
    public static final String FEDERATED_ID_TOKEN = "FEDERATED_ID_TOKEN";
    public static final String USER_INFO = "UserInfo";
    public static final String FEDERATED_ACCESS_TOKEN_RESPONSE = "FEDERATED_ACCESS_TOKEN_RESPONSE";
    public static final String VALIDATED_ID_TOKEN = "VALIDATED_ID_TOKEN";
    public static final String EXCHANGE_PROVIDER = "EXCHANGE_PROVIDER";
    public static final String VALIDATED_ACCESS_TOKEN = "VALIDATED_ACCESS_TOKEN";
    private static final String BROKER_NONCE_PARAM = "BROKER_NONCE";
    private static final List<String> SUPPORTED_TOKEN_TYPES = Arrays.asList(TokenUtil.TOKEN_TYPE_ID, TokenUtil.TOKEN_TYPE_BEARER, TokenUtil.TOKEN_TYPE_JWT_ACCESS_TOKEN, TokenUtil.TOKEN_TYPE_JWT_ACCESS_TOKEN_PREFIXED);

    public OIDCIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);

        String defaultScope = config.getDefaultScope();

        if (!defaultScope.contains(SCOPE_OPENID)) {
            config.setDefaultScope((SCOPE_OPENID + " " + defaultScope).trim());
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new OIDCEndpoint(callback, realm, event, this);
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
        String idToken = userSession.getNote(FEDERATED_ID_TOKEN);
        if (idToken == null) return;
        backchannelLogout(userSession, idToken);
    }

    protected void backchannelLogout(UserSessionModel userSession, String idToken) {
        String sessionId = userSession.getId();
        UriBuilder logoutUri = UriBuilder.fromUri(getConfig().getLogoutUrl())
                .queryParam("state", sessionId);
        if (getConfig().isSendIdTokenOnLogout() && idToken != null) {
            logoutUri.queryParam("id_token_hint", idToken);
        }
        if (getConfig().isSendClientIdOnLogout()) {
            logoutUri.queryParam("client_id", getConfig().getClientId());
        }
        String url = logoutUri.build().toString();
        try {
            int status = SimpleHttp.create(session).doGet(url).asStatus();
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
        String idToken = userSession.getNote(FEDERATED_ID_TOKEN);
        if (getConfig().isBackchannelSupported()) {
            backchannelLogout(userSession, idToken);
            return null;
        } else {
            String sessionId = userSession.getId();
            UriBuilder logoutUri = UriBuilder.fromUri(getConfig().getLogoutUrl())
                    .queryParam("state", sessionId);
            if (getConfig().isSendIdTokenOnLogout() && idToken != null) {
                logoutUri.queryParam("id_token_hint", idToken);
            }
            if (getConfig().isSendClientIdOnLogout()) {
                logoutUri.queryParam("client_id", getConfig().getClientId());
            }
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
            final int currentTime = Time.currentTime();
            if (exp != null && exp <= currentTime + getConfig().getMinValidityToken()) {
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
                    int accessTokenExpiration = currentTime + (int) newResponse.getExpiresIn();
                    newResponse.getOtherClaims().put(ACCESS_TOKEN_EXPIRATION, accessTokenExpiration);
                }

                if (newResponse.getRefreshToken() == null && tokenResponse.getRefreshToken() != null) {
                    newResponse.setRefreshToken(tokenResponse.getRefreshToken());
                    newResponse.setRefreshExpiresIn(tokenResponse.getRefreshExpiresIn());
                }
                response = JsonSerialization.writeValueAsString(newResponse);

                String oldToken = tokenUserSession.getNote(FEDERATED_ACCESS_TOKEN);
                if (oldToken != null && oldToken.equals(tokenResponse.getToken())) {
                    int accessTokenExpiration = newResponse.getExpiresIn() > 0 ? currentTime + (int) newResponse.getExpiresIn() : 0;
                    tokenUserSession.setNote(FEDERATED_TOKEN_EXPIRATION, Long.toString(accessTokenExpiration));
                    tokenUserSession.setNote(FEDERATED_REFRESH_TOKEN, newResponse.getRefreshToken());
                    tokenUserSession.setNote(FEDERATED_ACCESS_TOKEN, newResponse.getToken());
                    tokenUserSession.setNote(FEDERATED_ID_TOKEN, newResponse.getIdToken());

                }
                model.setToken(response);
                session.users().updateFederatedIdentity(authorizedClient.getRealm(), tokenSubject, model);
                tokenResponse = newResponse;
            } else if (exp != null) {
                tokenResponse.setExpiresIn(exp - currentTime);
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

    @Override
    protected BrokeredIdentityContext validateExternalTokenThroughUserInfo(EventBuilder event, String subjectToken, String subjectTokenType) {
        BrokeredIdentityContext context = super.validateExternalTokenThroughUserInfo(event, subjectToken, subjectTokenType);

        if (context != null && (OAuth2Constants.ID_TOKEN_TYPE.equals(subjectTokenType) ||
            (OAuth2Constants.ACCESS_TOKEN_TYPE.equals(subjectTokenType) && getConfig().isAccessTokenJwt()))) {

            JsonWebToken parsedToken;
            try {
                parsedToken = validateToken(subjectToken, true);
            } catch (IdentityBrokerException e) {
                logger.debug("Unable to validate token for exchange", e);
                event.detail(Details.REASON, "token validation failure");
                event.error(Errors.INVALID_TOKEN);
                throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
            }

            boolean idTokenType = OAuth2Constants.ID_TOKEN_TYPE.equals(subjectTokenType);

            if (idTokenType) {
                context.getContextData().put(VALIDATED_ID_TOKEN, parsedToken);
            } else {
                context.getContextData().put(KeycloakOIDCIdentityProvider.VALIDATED_ACCESS_TOKEN, parsedToken);
            }
            context.getContextData().put(EXCHANGE_PROVIDER, getConfig().getAlias());
            context.setIdp(this);
            return context;
        }

        return context;
    }

    protected void processAccessTokenResponse(BrokeredIdentityContext context, AccessTokenResponse response) {
        // Don't verify audience on accessToken as it may not be there. It was verified on IDToken already
        if (getConfig().isAccessTokenJwt()) {
            JsonWebToken access = validateToken(response.getToken(), true);
            context.getContextData().put(VALIDATED_ACCESS_TOKEN, access);
        }
    }

    @Override
    protected Response exchangeSessionToken(UriInfo uriInfo, EventBuilder event, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        String refreshToken = tokenUserSession.getNote(FEDERATED_REFRESH_TOKEN);
        String accessToken = tokenUserSession.getNote(FEDERATED_ACCESS_TOKEN);

        if (accessToken == null) {
            event.detail(Details.REASON, "requested_issuer is not linked");
            event.error(Errors.INVALID_TOKEN);
            return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            long expiration = Long.parseLong(tokenUserSession.getNote(FEDERATED_TOKEN_EXPIRATION));
            final int currentTime = Time.currentTime();
            if (expiration == 0 || expiration > currentTime + getConfig().getMinValidityToken()) {
                AccessTokenResponse tokenResponse = new AccessTokenResponse();
                tokenResponse.setExpiresIn(expiration > 0? expiration - currentTime : 0);
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
            long accessTokenExpiration = newResponse.getExpiresIn() > 0 ? currentTime + newResponse.getExpiresIn() : 0;
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

    protected static class OIDCEndpoint extends Endpoint {
        public OIDCEndpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event, OIDCIdentityProvider provider) {
            super(callback, realm, event, provider);
        }

        @Override
        public SimpleHttpRequest generateTokenRequest(String authorizationCode) {
            SimpleHttpRequest simpleHttp = super.generateTokenRequest(authorizationCode);
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

        if (Booleans.isTrue(getConfig().isPassMaxAge())) {
            AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();

            if (isAuthTimeExpired(idToken, authSession)) {
                throw new IdentityBrokerException("User not re-authenticated by the target OpenID Provider");
            }

            Object authTime = idToken.getOtherClaims().get(IDToken.AUTH_TIME);

            if (authTime != null) {
                authSession.setClientNote(AuthenticationManager.AUTH_TIME_BROKER, authTime.toString());
            }
        }

        try {
            BrokeredIdentityContext identity = extractIdentity(tokenResponse, accessToken, idToken);

            if (!identity.getId().equals(idToken.getSubject())) {
                throw new IdentityBrokerException("Mismatch between the subject in the id_token and the subject from the user_info endpoint");
            }

            if (getConfig().isFilteredByClaims()) {
                String filterName = getConfig().getClaimFilterName();
                String filterValue = getConfig().getClaimFilterValue();

                logger.tracef("Filtering user %s by %s=%s", idToken.getOtherClaims().get(getusernameClaimNameForIdToken()), filterName, filterValue);
                if (idToken.getOtherClaims().containsKey(filterName)) {
                    Object claimObject = idToken.getOtherClaims().get(filterName);
                    List<String> claimValues = new ArrayList<>();
                    if (claimObject instanceof List) {
                        ((List<?>)claimObject).forEach(v->claimValues.add(Objects.toString(v)));
                    } else {
                        claimValues.add(Objects.toString(claimObject));
                    }
                    logger.tracef("Found claim %s with values %s", filterName, claimValues);
                    if (!claimValues.stream().anyMatch(v->v.matches(filterValue))) {
                        logger.warnf("Claim %s has values \"%s\" that does not match the expected filter \"%s\"", filterName, claimValues, filterValue);
                        throw new IdentityBrokerException(String.format("Unmatched claim value for %s.", filterName)).
                            withMessageCode(Messages.IDENTITY_PROVIDER_UNMATCHED_ESSENTIAL_CLAIM_ERROR);
                    }
                } else {
                    logger.debugf("Claim %s was not found", filterName);
                    throw new IdentityBrokerException(String.format("Claim %s not found", filterName)).
                        withMessageCode(Messages.IDENTITY_PROVIDER_UNMATCHED_ESSENTIAL_CLAIM_ERROR);
                }
            }

            if (!getConfig().isDisableNonce()) {
                identity.getContextData().put(BROKER_NONCE_PARAM, idToken.getOtherClaims().get(OIDCLoginProtocol.NONCE_PARAM));
            }

            if (Booleans.isTrue(getConfig().isStoreToken())) {
                if (tokenResponse.getExpiresIn() > 0) {
                    long accessTokenExpiration = Time.currentTime() + tokenResponse.getExpiresIn();
                    tokenResponse.getOtherClaims().put(ACCESS_TOKEN_EXPIRATION, accessTokenExpiration);
                    response = JsonSerialization.writeValueAsString(tokenResponse);
                }
                identity.setToken(response);
            }

            return identity;
        } catch (IdentityBrokerException e) {
            throw e;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not fetch attributes from userinfo endpoint.", e);
        }
    }

    protected boolean isAuthTimeExpired(JsonWebToken idToken, AuthenticationSessionModel authSession) {
        String maxAge = authSession.getClientNote(OIDCLoginProtocol.MAX_AGE_PARAM);

        if (maxAge == null) {
            return false;
        }

        String authTime = idToken.getOtherClaims().getOrDefault(IDToken.AUTH_TIME, "0").toString();
        int authTimeInt = authTime == null ? 0 : Integer.parseInt(authTime);
        int maxAgeInt = Integer.parseInt(maxAge);

        if (authTimeInt + maxAgeInt < Time.currentTime()) {
            logger.debugf("Invalid auth_time claim. User not re-authenticated by the target OP.");
            return true;
        }

        return false;
    }

    private static final MediaType APPLICATION_JWT_TYPE = MediaType.valueOf("application/jwt");

    protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {
        String id = idToken.getSubject();
        BrokeredIdentityContext identity = new BrokeredIdentityContext(id, getConfig());
        String name = (String) idToken.getOtherClaims().get(IDToken.NAME);
        String givenName = (String)idToken.getOtherClaims().get(IDToken.GIVEN_NAME);
        String familyName = (String)idToken.getOtherClaims().get(IDToken.FAMILY_NAME);
        String preferredUsername = (String) idToken.getOtherClaims().get(getusernameClaimNameForIdToken());
        String email = (String) idToken.getOtherClaims().get(IDToken.EMAIL);

        if (!getConfig().isDisableUserInfoService()) {
            String userInfoUrl = getUserInfoUrl();
            if (userInfoUrl != null && !userInfoUrl.isEmpty()) {

                if (accessToken != null) {
                    SimpleHttpResponse response = executeRequest(userInfoUrl, SimpleHttp.create(session).doGet(userInfoUrl).header("Authorization", "Bearer " + accessToken));
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
                        userInfo = JsonSerialization.readValue(parseTokenInput(response.asString(), false), JsonNode.class);
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

    private SimpleHttpResponse executeRequest(String url, SimpleHttpRequest request) throws IOException {
        SimpleHttpResponse response = request.asResponse();
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

    protected KeyWrapper getIdentityProviderKeyWrapper(JWSInput jws) {
        return PublicKeyStorageManager.getIdentityProviderKeyWrapper(session, session.getContext().getRealm(), getConfig(), jws);
    }

    protected boolean verify(JWSInput jws) {
        if (!getConfig().isValidateSignature()) return true;

        try {
            KeyWrapper key = getIdentityProviderKeyWrapper(jws);
            if (key == null) {
                logger.debugf("Failed to verify token, key not found for algorithm %s", jws.getHeader().getRawAlgorithm());
                return false;
            }
            String algorithm = jws.getHeader().getRawAlgorithm();
            if (key.getAlgorithm() == null) {
                key.setAlgorithm(algorithm);
            }
            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, algorithm);
            if (signatureProvider == null) {
                logger.debugf("Failed to verify token, signature provider not found for algorithm %s", algorithm);
                return false;
            }

            return signatureProvider.verifier(key).verify(jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8), jws.getSignature());
        } catch (Exception e) {
            logger.debug("Failed to verify token", e);
            return false;
        }
    }

    /**
     * Parses a JWT token that can be a JWE, JWS or JWE/JWS. It returns the content
     * as a string. If JWS is involved the signature is also validated. A
     * IdentityBrokerException is thrown on any error.
     *
     * @param encodedToken The token in the encoded string format.
     * @param shouldBeSigned true if the token should be signed (id token),
     * false if the token can be only encrypted and not signed (user info).
     * @return The content in string format.
     */
    protected String parseTokenInput(String encodedToken, boolean shouldBeSigned) {
        if (encodedToken == null) {
            throw new IdentityBrokerException("No token from server.");
        }

        try {
            JWSInput jws;
            JOSE joseToken = JOSEParser.parse(encodedToken);
            if (joseToken instanceof JWE) {
                // encrypted JWE token
                JWE jwe = (JWE) joseToken;

                KeyWrapper key;
                if (jwe.getHeader().getKeyId() == null) {
                    key = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, jwe.getHeader().getRawAlgorithm());
                } else {
                    key = session.keys().getKey(session.getContext().getRealm(), jwe.getHeader().getKeyId(), KeyUse.ENC, jwe.getHeader().getRawAlgorithm());
                }
                if (key == null || key.getPrivateKey() == null) {
                    throw new IdentityBrokerException("Private key not found in the realm to decrypt token algorithm " + jwe.getHeader().getRawAlgorithm());
                }

                jwe.getKeyStorage().setDecryptionKey(key.getPrivateKey());
                jwe.verifyAndDecodeJwe();
                String content = new String(jwe.getContent(), StandardCharsets.UTF_8);

                try {
                    // try to decode the token just in case it is a JWS
                    joseToken = JOSEParser.parse(content);
                } catch(Exception e) {
                    if (shouldBeSigned) {
                        throw new IdentityBrokerException("Token is not a signed JWS", e);
                    }
                    // the token is only a encrypted JWE (user-info)
                    return content;
                }

                if (!(joseToken instanceof JWSInput)) {
                    throw new IdentityBrokerException("Invalid token type");
                }

                jws = (JWSInput) joseToken;
            } else if (joseToken instanceof JWSInput) {
                // common signed JWS token
                jws = (JWSInput) joseToken;
            } else {
                throw new IdentityBrokerException("Invalid token type");
            }

            // verify signature of the JWS
            if (!verify(jws)) {
                throw new IdentityBrokerException("token signature validation failed");
            }
            return new String(jws.getContent(), StandardCharsets.UTF_8);
        } catch (JWEException e) {
            throw new IdentityBrokerException("Invalid token", e);
        }
    }

    public JsonWebToken validateToken(String encodedToken) {
        boolean ignoreAudience = false;

        return validateToken(encodedToken, ignoreAudience);
    }

    protected JsonWebToken validateToken(String encodedToken, boolean ignoreAudience) {
        JsonWebToken token;
        try {
            token = JsonSerialization.readValue(parseTokenInput(encodedToken, true), JsonWebToken.class);
        } catch (IOException e) {
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
        BrokeredIdentityContext identity = new BrokeredIdentityContext(id, getConfig());

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

        JsonWebToken parsedToken;
        try {
            parsedToken = validateToken(subjectToken, true);
        } catch (IdentityBrokerException e) {
            logger.debug("Unable to validate token for exchange", e);
            event.detail(Details.REASON, "token validation failure");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
        }

        try {
            if (!getConfig().isDisableTypeClaimCheck() && !isTokenTypeSupported(parsedToken)) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "token type not supported", Response.Status.BAD_REQUEST);
            }
            boolean idTokenType = OAuth2Constants.ID_TOKEN_TYPE.equals(subjectTokenType);
            BrokeredIdentityContext context = extractIdentity(null, idTokenType ? null : subjectToken, parsedToken);
            if (context == null) {
                event.detail(Details.REASON, "Failed to extract identity from token");
                event.error(Errors.INVALID_TOKEN);
                throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);

            }
            if (idTokenType) {
                context.getContextData().put(VALIDATED_ID_TOKEN, parsedToken);
            } else {
                context.getContextData().put(KeycloakOIDCIdentityProvider.VALIDATED_ACCESS_TOKEN, parsedToken);
            }
            context.getContextData().put(EXCHANGE_PROVIDER, getConfig().getAlias());
            context.setIdp(this);
            return context;
        } catch (IOException e) {
            logger.debug("Unable to extract identity from identity token", e);
            event.detail(Details.REASON, "Unable to extract identity from identity token: " + e.getMessage());
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
        }


    }

    protected boolean isTokenTypeSupported(JsonWebToken parsedToken) {
        return SUPPORTED_TOKEN_TYPES.contains(parsedToken.getType());
    }

    @Override
    protected BrokeredIdentityContext exchangeExternalTokenV1Impl(EventBuilder event, MultivaluedMap<String, String> params) {
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
    protected BrokeredIdentityContext exchangeExternalTokenV2Impl(TokenExchangeContext tokenExchangeContext) {
        // Supporting only introspection-endpoint validation for now
        validateExternalTokenWithIntrospectionEndpoint(tokenExchangeContext);

        return exchangeExternalUserInfoValidationOnly(tokenExchangeContext.getEvent(), tokenExchangeContext.getFormParams());
    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        UriBuilder uriBuilder = super.createAuthorizationUrl(request);
        AuthenticationSessionModel authenticationSession = request.getAuthenticationSession();

        if (!getConfig().isDisableNonce()) {
            String nonce = Base64Url.encode(SecretGenerator.getInstance().randomBytes(16));
            authenticationSession.setClientNote(BROKER_NONCE_PARAM, nonce);
            uriBuilder.queryParam(OIDCLoginProtocol.NONCE_PARAM, nonce);
        }

        String maxAge = request.getAuthenticationSession().getClientNote(OIDCLoginProtocol.MAX_AGE_PARAM);

        if (Booleans.isTrue(getConfig().isPassMaxAge()) && maxAge != null) {
            uriBuilder.queryParam(OIDCLoginProtocol.MAX_AGE_PARAM, maxAge);
        }

        return uriBuilder;
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context) {
        AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();

        if (authenticationSession == null || getConfig().isDisableNonce()) {
            // no interacting with the brokered OP, likely doing token exchanges or no nonce
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

    @Override
    public boolean reloadKeys() {
        if (getConfig().isEnabled() && getConfig().isUseJwksUrl()) {
            String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(session.getContext().getRealm().getId(), getConfig().getInternalId());
            PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
            return keyStorage.reloadKeys(modelKey, new OIDCIdentityProviderPublicKeyLoader(session, getConfig()));
        }
        return false;
    }

    @Override
    protected void setEmailVerified(UserModel user, BrokeredIdentityContext context) {
        OIDCIdentityProviderConfig config = getConfig();
        Map<String, Object> contextData = context.getContextData();
        JsonWebToken token = (JsonWebToken) Optional.ofNullable(contextData.get(VALIDATED_ID_TOKEN))
                .orElseGet(() -> contextData.get(VALIDATED_ACCESS_TOKEN));
        Boolean emailVerified = getEmailVerifiedClaim(token);

        if (Booleans.isFalse(config.isTrustEmail()) || emailVerified == null) {
            // fallback to the default behavior if trust is disabled or there is no email_verified claim
            super.setEmailVerified(user, context);
            return;
        }

        user.setEmailVerified(emailVerified);
    }

    private Boolean getEmailVerifiedClaim(JsonWebToken token) {
        if (token == null) {
            return null;
        }

        Object emailVerified = token.getOtherClaims().get(IDToken.EMAIL_VERIFIED);

        if (emailVerified == null) {
            return null;
        }

        return Boolean.valueOf(emailVerified.toString());
    }

    @Override
    public boolean verifyClientAssertion(ClientAuthenticationFlowContext context) throws Exception {
        OIDCIdentityProviderConfig config = getConfig();

        FederatedJWTClientValidator validator = new FederatedJWTClientValidator(context, v -> verify(v.getJws()),
                config.getIssuer(), config.getAllowedClockSkew(), config.isSupportsClientAssertionReuse());

        if (!Profile.isFeatureEnabled(Profile.Feature.CLIENT_AUTH_FEDERATED)) {
            return false;
        }

        if (!config.isSupportsClientAssertions()) {
            throw new RuntimeException("Issuer does not support client assertions");
        }

        if (!config.isValidateSignature()) {
            throw new RuntimeException("Signature validation not enabled for issuer");
        }

        return validator.validate();
    }

    public BrokeredIdentityContext validateAuthorizationGrantAssertion(JWTAuthorizationGrantValidationContext context) throws IdentityBrokerException {
        if (!getConfig().getJWTAuthorizationGrantEnabled()) {
            throw new IdentityBrokerException("JWT Authorization Granted is not enabled for the identity provider");
        }

        // verify signature
        if (!verify(context.getJws())) {
            throw new IdentityBrokerException("Invalid signature");
        }

        BrokeredIdentityContext user = new BrokeredIdentityContext(context.getJWT().getSubject(), getConfig());
        user.setUsername(context.getJWT().getSubject());
        user.setIdp(this);
        return user;
    }

    @Override
    public int getAllowedClockSkew() {
        return getConfig().getAllowedClockSkew();
    }

    @Override
    public boolean isAssertionReuseAllowed() {
        return getConfig().getJWTAuthorizationGrantAssertionReuseAllowed();
    }

    @Override
    public List<String> getAllowedAudienceForJWTGrant() {
        return new JWTAuthorizationGrantIdentityProvider(session, getConfig()).getAllowedAudienceForJWTGrant();
    }

    @Override
    public int getMaxAllowedExpiration() {
        return getConfig().getJWTAuthorizationGrantMaxAllowedAssertionExpiration();
    }

    @Override
    public String getAssertionSignatureAlg() {
        return getConfig().getJWTAuthorizationGrantAssertionSignatureAlg();
    }
}
