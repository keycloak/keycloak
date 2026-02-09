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

package org.keycloak.protocol;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

import org.keycloak.Token;
import org.keycloak.TokenCategory;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * This is an an encoded token that is stored as a cookie so that if there is a client timeout, then the authentication session
 * can be restarted.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RestartLoginCookie implements Token {
    private static final Logger logger = Logger.getLogger(RestartLoginCookie.class);
    public static final String KC_RESTART = "KC_RESTART";

    @JsonProperty("cid")
    protected String clientId;

    @JsonProperty("pty")
    protected String authMethod;

    @JsonProperty("ruri")
    protected String redirectUri;

    @JsonProperty("act")
    protected String action;

    @JsonProperty("notes")
    protected Map<String, String> notes = new HashMap<>();

    @Deprecated // Backwards compatibility
    @JsonProperty("cs")
    protected String cs;

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public RestartLoginCookie() {
    }

    public RestartLoginCookie(AuthenticationSessionModel authSession) {
        this.action = authSession.getAction();
        this.clientId = authSession.getClient().getClientId();
        this.authMethod = authSession.getProtocol();
        this.redirectUri = authSession.getRedirectUri();
        for (Map.Entry<String, String> entry : authSession.getClientNotes().entrySet()) {
            notes.put(entry.getKey(), entry.getValue());
        }
    }

    public static void setRestartCookie(KeycloakSession session, AuthenticationSessionModel authSession) {
        RestartLoginCookie restart = new RestartLoginCookie(authSession);
        String encoded = encodeAndEncrypt(session, restart);
        session.getProvider(CookieProvider.class).set(CookieType.AUTH_RESTART, encoded);
    }

    public static void expireRestartCookie(KeycloakSession session) {
        session.getProvider(CookieProvider.class).expire(CookieType.AUTH_RESTART);
    }

    public static String getRestartCookie(KeycloakSession session){
        String cook = session.getProvider(CookieProvider.class).get(CookieType.AUTH_RESTART);
        if (cook ==  null) {
            logger.debug("KC_RESTART cookie doesn't exist");
            return null;
        }
        return cook;
    }

    public static AuthenticationSessionModel restartSession(KeycloakSession session, RealmModel realm,
                                                            RootAuthenticationSessionModel rootSession, String expectedClientId,
                                                            String encodedCookie) throws Exception {
        RestartLoginCookie cookie = decryptAndDecode(session, encodedCookie);
        if (cookie == null) {
            logger.debug("Failed to verify encoded RestartLoginCookie");
            return null;
        }

        ClientModel client = realm.getClientByClientId(cookie.getClientId());
        if (client == null) return null;

        // Restart just if client from cookie matches client from the URL.
        if (!client.getClientId().equals(expectedClientId)) {
            logger.debugf("Skip restarting from the KC_RESTART. Clients doesn't match: Cookie client: %s, Requested client: %s", client.getClientId(), expectedClientId);
            return null;
        }

        // Need to create brand new session and setup cookie
        if (rootSession == null) {
            rootSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, true);
        }

        AuthenticationSessionModel authSession = rootSession.createAuthenticationSession(client);
        authSession.setProtocol(cookie.getAuthMethod());
        authSession.setRedirectUri(cookie.getRedirectUri());
        authSession.setAction(cookie.getAction());
        for (Map.Entry<String, String> entry : cookie.getNotes().entrySet()) {
            authSession.setClientNote(entry.getKey(), entry.getValue());
        }

        return authSession;
    }

    private static RestartLoginCookie decryptAndDecode(KeycloakSession session, String encodedToken) {
        try {
            String sigAlgorithm = session.tokens().signatureAlgorithm(TokenCategory.INTERNAL);
            String algAlgorithm = session.tokens().cekManagementAlgorithm(TokenCategory.INTERNAL);
            SecretKey encKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, algAlgorithm).getSecretKey();
            SecretKey signKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, sigAlgorithm).getSecretKey();

            byte[] contentBytes = TokenUtil.jweDirectVerifyAndDecode(encKey, signKey, encodedToken);
            String jwt = new String(contentBytes, StandardCharsets.UTF_8);
            return session.tokens().decode(jwt, RestartLoginCookie.class);
        } catch (Exception e) {
            // Might be the cookie from the older version
            return session.tokens().decode(encodedToken, RestartLoginCookie.class);
        }
    }

    private static String encodeAndEncrypt(KeycloakSession session, RestartLoginCookie cookie) {
        try {
            String sigAlgorithm = session.tokens().signatureAlgorithm(cookie.getCategory());
            String algAlgorithm = session.tokens().cekManagementAlgorithm(cookie.getCategory());
            SecretKey encKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, algAlgorithm).getSecretKey();
            SecretKey signKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, sigAlgorithm).getSecretKey();

            String encodedJwt = session.tokens().encode(cookie);
            return TokenUtil.jweDirectEncode(encKey, signKey, encodedJwt.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Error encoding cookie.", e);
        }
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.INTERNAL;
    }
}
