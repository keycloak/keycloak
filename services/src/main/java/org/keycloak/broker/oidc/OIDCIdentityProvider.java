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
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.oidc.util.JsonSimpleHttp;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.truststore.JSSETruststoreConfigurator;
import org.keycloak.util.JsonSerialization;
import org.keycloak.common.util.PemUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.security.PublicKey;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProvider extends AbstractOAuth2IdentityProvider<OIDCIdentityProviderConfig> {
    protected static final Logger logger = Logger.getLogger(OIDCIdentityProvider.class);

    public static final String OAUTH2_PARAMETER_PROMPT = "prompt";
    public static final String SCOPE_OPENID = "openid";
    public static final String FEDERATED_ID_TOKEN = "FEDERATED_ID_TOKEN";
    public static final String USER_INFO = "UserInfo";
    public static final String FEDERATED_ACCESS_TOKEN_RESPONSE = "FEDERATED_ACCESS_TOKEN_RESPONSE";
    public static final String VALIDATED_ID_TOKEN = "VALIDATED_ID_TOKEN";

    public OIDCIdentityProvider(OIDCIdentityProviderConfig config) {
        super(config);

        String defaultScope = config.getDefaultScope();

        if (!defaultScope.contains(SCOPE_OPENID)) {
            config.setDefaultScope(SCOPE_OPENID + " " + defaultScope);
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new OIDCEndpoint(callback, realm, event);
    }

    protected PublicKey getExternalIdpKey() {
        String signingCert = getConfig().getCertificateSignatureVerifier();
        try {
            if (signingCert != null && !signingCert.trim().equals("")) {
                return PemUtils.decodeCertificate(signingCert).getPublicKey();
            } else if (getConfig().getPublicKeySignatureVerifier() != null && !getConfig().getPublicKeySignatureVerifier().trim().equals("")) {
                return PemUtils.decodePublicKey(getConfig().getPublicKeySignatureVerifier());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;

    }

    protected class OIDCEndpoint extends Endpoint {
        public OIDCEndpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event) {
            super(callback, realm, event);
        }



        @GET
        @Path("logout_response")
        public Response logoutResponse(@Context UriInfo uriInfo,
                                       @QueryParam("state") String state) {
            UserSessionModel userSession = session.sessions().getUserSession(realm, state);
            if (userSession == null) {
                logger.error("no valid user session");
                EventBuilder event = new EventBuilder(realm, session, clientConnection);
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.error("usersession in different state");
                EventBuilder event = new EventBuilder(realm, session, clientConnection);
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, Messages.SESSION_NOT_ACTIVE);
            }
            return AuthenticationManager.finishBrowserLogout(session, realm, userSession, uriInfo, clientConnection, headers);
        }

    }

    @Override
    public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        if (getConfig().getLogoutUrl() == null || getConfig().getLogoutUrl().trim().equals("") || !getConfig().isBackchannelSupported()) return;
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
            int status = JsonSimpleHttp.doGet(url).asStatus();
            boolean success = status >=200 && status < 400;
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

    /**
     * Returns access token response as a string from a refresh token invocation on the remote OIDC broker
     *
     * @param session
     * @param userSession
     * @return
     */
    public String refreshToken(KeycloakSession session, UserSessionModel userSession) {
        String refreshToken = userSession.getNote(FEDERATED_REFRESH_TOKEN);
        JSSETruststoreConfigurator configurator = new JSSETruststoreConfigurator(session);
        try {
            return SimpleHttp.doPost(getConfig().getTokenUrl())
                    .param("refresh_token", refreshToken)
                    .param(OAUTH2_PARAMETER_GRANT_TYPE, OAUTH2_GRANT_TYPE_REFRESH_TOKEN)
                    .param(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                    .param(OAUTH2_PARAMETER_CLIENT_SECRET, getConfig().getClientSecret())
                    .sslFactory(configurator.getSSLSocketFactory())
                    .hostnameVerifier(configurator.getHostnameVerifier()).asString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getIDTokenForLogout(KeycloakSession session, UserSessionModel userSession) {
        long exp = Long.parseLong(userSession.getNote(FEDERATED_TOKEN_EXPIRATION));
        int currentTime = Time.currentTime();
        if (exp > 0 && currentTime > exp) {
            String response = refreshToken(session, userSession);
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

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        UriBuilder authorizationUrl = super.createAuthorizationUrl(request);
        String prompt = getConfig().getPrompt();

        if (prompt != null && !prompt.isEmpty()) {
            authorizationUrl.queryParam(OAUTH2_PARAMETER_PROMPT, prompt);
        }

        return authorizationUrl;
    }

    protected void processAccessTokenResponse(BrokeredIdentityContext context, PublicKey idpKey, AccessTokenResponse response) {

    }

    @Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        AccessTokenResponse tokenResponse = null;
        try {
            tokenResponse = JsonSerialization.readValue(response, AccessTokenResponse.class);
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not decode access token response.", e);
        }
        PublicKey key = getExternalIdpKey();
        String accessToken = verifyAccessToken(key, tokenResponse);

        String encodedIdToken = tokenResponse.getIdToken();



        JsonWebToken idToken = validateToken(key, encodedIdToken);

        try {
            String id = idToken.getSubject();
            BrokeredIdentityContext identity = new BrokeredIdentityContext(id);
            String name = (String)idToken.getOtherClaims().get(IDToken.NAME);
            String preferredUsername = (String)idToken.getOtherClaims().get(IDToken.PREFERRED_USERNAME);
            String email = (String)idToken.getOtherClaims().get(IDToken.EMAIL);

            if (getConfig().getUserInfoUrl() != null && (id == null || name == null || preferredUsername == null || email == null) ) {
                SimpleHttp request = JsonSimpleHttp.doGet(getConfig().getUserInfoUrl())
                        .header("Authorization", "Bearer " + accessToken);
                JsonNode userInfo = JsonSimpleHttp.asJson(request);

                id = getJsonProperty(userInfo, "sub");
                name = getJsonProperty(userInfo, "name");
                preferredUsername = getJsonProperty(userInfo, "preferred_username");
                email = getJsonProperty(userInfo, "email");
                AbstractJsonUserAttributeMapper.storeUserProfileForMapper(identity, userInfo, getConfig().getAlias());
            }
            identity.getContextData().put(FEDERATED_ACCESS_TOKEN_RESPONSE, tokenResponse);
            identity.getContextData().put(VALIDATED_ID_TOKEN, idToken);
            processAccessTokenResponse(identity, key, tokenResponse);

            identity.setId(id);
            identity.setName(name);
            identity.setEmail(email);

            identity.setBrokerUserId(getConfig().getAlias() + "." + id);
            if (tokenResponse.getSessionState() != null) {
                identity.setBrokerSessionId(getConfig().getAlias() + "." + tokenResponse.getSessionState());
            }

            if (preferredUsername == null) {
                preferredUsername = email;
            }

            if (preferredUsername == null) {
                preferredUsername = id;
            }

            identity.setUsername(preferredUsername);

            if (getConfig().isStoreToken()) {
                identity.setToken(response);
            }

            return identity;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not fetch attributes from userinfo endpoint.", e);
        }
    }

    private String verifyAccessToken(PublicKey key, AccessTokenResponse tokenResponse) {
        String accessToken = tokenResponse.getToken();

        if (accessToken == null) {
            throw new IdentityBrokerException("No access_token from server.");
        }
        return accessToken;
    }

    protected boolean verify(JWSInput jws, PublicKey key) {
        if (key == null) return true;
        if (!getConfig().isValidateSignature()) return true;
        return RSAProvider.verify(jws, key);

    }

    protected JsonWebToken validateToken(PublicKey key, String encodedToken) {
        if (encodedToken == null) {
            throw new IdentityBrokerException("No token from server.");
        }

        JsonWebToken token;
        try {
            JWSInput jws = new JWSInput(encodedToken);
            if (!verify(jws, key)) {
                throw new IdentityBrokerException("token signature validation failed");
            }
            token = jws.readJsonContent(JsonWebToken.class);
        } catch (JWSInputException e) {
            throw new IdentityBrokerException("Invalid token", e);
        }

        String iss = token.getIssuer();

        if (!token.hasAudience(getConfig().getClientId())) {
            throw new IdentityBrokerException("Wrong audience from token.");
        }

        if (!token.isActive()) {
            throw new IdentityBrokerException("Token is no longer valid");
        }

        String trustedIssuers = getConfig().getIssuer();

        if (trustedIssuers != null) {
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
    public void attachUserSession(UserSessionModel userSession, ClientSessionModel clientSession, BrokeredIdentityContext context) {
        AccessTokenResponse tokenResponse = (AccessTokenResponse)context.getContextData().get(FEDERATED_ACCESS_TOKEN_RESPONSE);
        int currentTime = Time.currentTime();
        long expiration = tokenResponse.getExpiresIn() > 0 ? tokenResponse.getExpiresIn() + currentTime : 0;
        userSession.setNote(FEDERATED_TOKEN_EXPIRATION, Long.toString(expiration));
        userSession.setNote(FEDERATED_REFRESH_TOKEN, tokenResponse.getRefreshToken());
        userSession.setNote(FEDERATED_ACCESS_TOKEN, tokenResponse.getToken());
        userSession.setNote(FEDERATED_ID_TOKEN, tokenResponse.getIdToken());
    }

    @Override
    protected String getDefaultScopes() {
        return "openid";
    }
}
