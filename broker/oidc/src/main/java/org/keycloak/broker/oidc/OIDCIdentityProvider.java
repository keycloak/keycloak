/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.oidc;

import org.codehaus.jackson.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.RSATokenVerifier;
import org.keycloak.broker.oidc.util.SimpleHttp;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.PemUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Map;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProvider extends AbstractOAuth2IdentityProvider<OIDCIdentityProviderConfig> {
    protected static final Logger logger = Logger.getLogger(OIDCIdentityProvider.class);

    public static final String OAUTH2_PARAMETER_PROMPT = "prompt";
    public static final String SCOPE_OPENID = "openid";
    public static final String FEDERATED_ID_TOKEN = "FEDERATED_ID_TOKEN";

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

    protected boolean verify(JWSInput jws, PublicKey key) {
        if (key == null) return true;
        if (!getConfig().isValidateSignature()) return true;
        return RSAProvider.verify(jws, key);

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
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.error("usersession in different state");
                EventBuilder event = new EventBuilder(realm, session, clientConnection);
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.SESSION_NOT_ACTIVE);
            }
            return AuthenticationManager.finishBrowserLogout(session, realm, userSession, uriInfo, clientConnection, headers);
        }

    }

    @Override
    public Response keycloakInitiatedBrowserLogout(UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        if (getConfig().getLogoutUrl() == null || getConfig().getLogoutUrl().trim().equals("")) return null;
        UriBuilder logoutUri = UriBuilder.fromUri(getConfig().getLogoutUrl())
                                         .queryParam("state", userSession.getId());
        String idToken = userSession.getNote(FEDERATED_ID_TOKEN);
        if (idToken != null) logoutUri.queryParam("id_token_hint", idToken);
        String redirect = RealmsResource.brokerUrl(uriInfo)
                                        .path(IdentityBrokerService.class, "getEndpoint")
                                        .path(OIDCEndpoint.class, "logoutResponse")
                                        .build(realm.getName(), getConfig().getAlias()).toString();
        logoutUri.queryParam("post_logout_redirect_uri", redirect);
        return Response.status(302).location(logoutUri.build()).build();
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

    @Override
    protected FederatedIdentity getFederatedIdentity(Map<String, String> notes, String response) {
        AccessTokenResponse tokenResponse = null;
        try {
            tokenResponse = JsonSerialization.readValue(response, AccessTokenResponse.class);
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not decode access token response.", e);
        }
        PublicKey key = getExternalIdpKey();
        String accessToken = verifyAccessToken(key, tokenResponse);

        String encodedIdToken = tokenResponse.getIdToken();

        notes.put(FEDERATED_ACCESS_TOKEN, accessToken);
        notes.put(FEDERATED_ID_TOKEN, encodedIdToken);
        notes.put(FEDERATED_REFRESH_TOKEN, tokenResponse.getRefreshToken());
        notes.put(FEDERATED_TOKEN_EXPIRATION, Long.toString(tokenResponse.getExpiresIn()));


        IDToken idToken = validateIdToken(key, encodedIdToken);

        try {
            String id = idToken.getSubject();
            String name = idToken.getName();
            String preferredUsername = idToken.getPreferredUsername();
            String email = idToken.getEmail();

            if (id == null || name == null || preferredUsername == null || email == null && getConfig().getUserInfoUrl() != null) {
                JsonNode userInfo = SimpleHttp.doGet(getConfig().getUserInfoUrl())
                        .header("Authorization", "Bearer " + accessToken)
                        .asJson();

                id = getJsonProperty(userInfo, "sub");
                name = getJsonProperty(userInfo, "name");
                preferredUsername = getJsonProperty(userInfo, "preferred_username");
                email = getJsonProperty(userInfo, "email");
            }

            FederatedIdentity identity = new FederatedIdentity(id);

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

   private IDToken validateIdToken(PublicKey key, String encodedToken) {
        if (encodedToken == null) {
            throw new IdentityBrokerException("No id_token from server.");
        }

        try {
            JWSInput jws = new JWSInput(encodedToken);
            if (!verify(jws, key)) {
                throw new IdentityBrokerException("IDToken signature validation failed");
            }
            IDToken idToken = jws.readJsonContent(IDToken.class);

            String aud = idToken.getAudience();
            String iss = idToken.getIssuer();

            if (aud != null && !aud.equals(getConfig().getClientId())) {
                throw new IdentityBrokerException("Wrong audience from id_token..");
            }

            String trustedIssuers = getConfig().getIssuer();

            if (trustedIssuers != null) {
                String[] issuers = trustedIssuers.split(",");

                for (String trustedIssuer : issuers) {
                    if (iss != null && iss.equals(trustedIssuer.trim())) {
                        return idToken;
                    }
                }

                throw new IdentityBrokerException("Wrong issuer from id_token. Got: " + iss + " expected: " + getConfig().getIssuer());
            }
            return idToken;
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not decode id token.", e);
        }
    }

    @Override
    protected String getDefaultScopes() {
        return "openid";
    }
}
