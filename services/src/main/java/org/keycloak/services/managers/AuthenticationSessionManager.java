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

package org.keycloak.services.managers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.sessions.StickySessionEncoderProvider;

import org.jboss.logging.Logger;

import static org.keycloak.services.managers.AuthenticationManager.authenticateIdentityCookie;


/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionManager {

    private static final Logger log = Logger.getLogger(AuthenticationSessionManager.class);
    private static final Base64.Encoder BASE_64_ENCODER_NO_PADDING = Base64.getEncoder().withoutPadding();

    private final KeycloakSession session;

    public AuthenticationSessionManager(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Creates a fresh authentication session for the given realm . Optionally sets the browser
     * authentication session cookie with the ID of the new session.
     * @param browserCookie Set the cookie in the browser for the
     */
    public RootAuthenticationSessionModel createAuthenticationSession(RealmModel realm, boolean browserCookie) {
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);

        if (browserCookie) {
            setAuthSessionCookie(rootAuthSession.getId());
            setAuthSessionIdHashCookie(rootAuthSession.getId());
        }

        return rootAuthSession;
    }

    public RootAuthenticationSessionModel getCurrentRootAuthenticationSession(RealmModel realm) {
        AuthSessionCookie authSession = getAuthSessionCookies(realm);
        if (authSession == null) {
            return null;
        }

        reEncodeAuthSessionCookie(authSession);
        return authSession.rootSession();
    }

    /**
     * @return The current authentication session if it exists, otherwise returns {@code null}.
     */
    public AuthenticationSessionModel getCurrentAuthenticationSession(RealmModel realm, ClientModel client, String tabId) {
        AuthSessionCookie rootAuth = getAuthSessionCookies(realm);
        if (rootAuth == null) {
            return null;
        }

        AuthenticationSessionModel authSession = rootAuth.rootSession().getAuthenticationSession(client, tabId);
        if (authSession != null) {
            reEncodeAuthSessionCookie(rootAuth);
        }
        return authSession;
    }

    /**
     * @param authSessionId decoded authSessionId (without route info attached)
     */
    public void setAuthSessionCookie(String authSessionId) {
        StickySessionEncoderProvider encoder = session.getProvider(StickySessionEncoderProvider.class);
        String signedAuthSessionId = signAndEncodeToBase64AuthSessionId(authSessionId);
        String encodedWithRoute = encoder.encodeSessionId(signedAuthSessionId, authSessionId);

        session.getProvider(CookieProvider.class).set(CookieType.AUTH_SESSION_ID, encodedWithRoute);

        log.debugf("Set AUTH_SESSION_ID cookie with value %s", encodedWithRoute);
    }

    /**
     * @param authSessionId decoded authSessionId (without route info attached)
     */
    public void setAuthSessionIdHashCookie(String authSessionId) {
        String authSessionIdHash = BASE_64_ENCODER_NO_PADDING.encodeToString(HashUtils.hash(JavaAlgorithm.SHA256, authSessionId.getBytes(StandardCharsets.UTF_8)));

        session.getProvider(CookieProvider.class).set(CookieType.AUTH_SESSION_ID_HASH, authSessionIdHash);

        log.debugf("Set KC_AUTH_SESSION_HASH cookie with value %s", authSessionIdHash);
    }

    private void reEncodeAuthSessionCookie(AuthSessionCookie authSessionCookie) {
        if (authSessionCookie.routeChanged()) {
            setAuthSessionCookie(authSessionCookie.sessionId());
        }
    }

    public String decodeBase64AndValidateSignature(String encodedBase64AuthSessionId) {
        try {
            String decodedAuthSessionId = new String(Base64Url.decode(encodedBase64AuthSessionId), StandardCharsets.UTF_8);
            int dotIndex = decodedAuthSessionId.lastIndexOf('.');
            if (dotIndex == -1) {
                //not found / invalid
                return null;
            }
            String authSessionId = decodedAuthSessionId.substring(0, dotIndex);
            String signature = decodedAuthSessionId.substring(dotIndex + 1);
            return validateAuthSessionIdSignature(authSessionId, signature);
        } catch (Exception e) {
            log.errorf("Error decoding auth session id with value: %s", encodedBase64AuthSessionId, e);
        }
        return null;
    }

    private String validateAuthSessionIdSignature(String authSessionId, String signature) {
        //check if the signature has already been verified for the same request
        if(signature.equals(session.getAttribute(authSessionId))) {
            return authSessionId;
        }

        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, Constants.INTERNAL_SIGNATURE_ALGORITHM);
        SignatureSignerContext signer = signatureProvider.signer();
        try {
            boolean valid = signatureProvider.verifier(signer.getKid()).verify(authSessionId.getBytes(StandardCharsets.UTF_8), Base64Url.decode(signature));
            if (!valid) {
                return null;
            }
            //Save the signature to avoid re-verification for the same request
            session.setAttribute(authSessionId, signature);
            return authSessionId;
        } catch (Exception e) {
            log.errorf("Signature validation failed for auth session id: %s", authSessionId, e);
        }
        return null;
    }

    public String signAndEncodeToBase64AuthSessionId(String authSessionId) {
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, Constants.INTERNAL_SIGNATURE_ALGORITHM);
        SignatureSignerContext signer = signatureProvider.signer();
        StringBuilder buffer = new StringBuilder();
        byte[] signature =  signer.sign(authSessionId.getBytes(StandardCharsets.UTF_8));
        buffer.append(authSessionId);
        if (signature != null) {
            buffer.append('.');
            buffer.append(Base64Url.encode(signature));
        }
        return Base64Url.encode(buffer.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return the value of the AUTH_SESSION_ID cookie. It is assumed that values could be encoded with signature and with route added (EG. "NWUxNjFlMDAtZDQyNi00ZWE2LTk4ZTktNTJlYjk4NDRlMmQ3L.node1" )
     */
    AuthSessionCookie getAuthSessionCookies(RealmModel realm) {
        String oldEncodedId = session.getProvider(CookieProvider.class).get(CookieType.AUTH_SESSION_ID);
        if (oldEncodedId == null || oldEncodedId.isEmpty()) {
            return null;
        }

        StickySessionEncoderProvider routeEncoder = session.getProvider(StickySessionEncoderProvider.class);
        // in case the id is encoded with a route when running in a cluster
        var sessionIdAndRoute = routeEncoder.decodeSessionIdAndRoute(oldEncodedId);

        String decodedAuthSessionId = decodeBase64AndValidateSignature(sessionIdAndRoute.sessionId());
        if(decodedAuthSessionId == null) {
            return null;
        }

        // we can't blindly trust the cookie and assume it is valid and referencing a valid root auth session
        // but make sure the root authentication session actually exists
        // without this check there is a risk of resolving user sessions from invalid root authentication sessions as they share the same id
        RootAuthenticationSessionModel rootAuthenticationSession = session.authenticationSessions().getRootAuthenticationSession(realm, decodedAuthSessionId);
        if (rootAuthenticationSession == null) {
            return null;
        }
        String newRoute = routeEncoder.sessionIdRoute(decodedAuthSessionId);
        boolean routeChanged = !sessionIdAndRoute.isSameRoute(newRoute);
        if (routeChanged) {
            log.debugf("Route changed. Will update authentication session cookie. Old: '%s', New: '%s'", sessionIdAndRoute.route(), newRoute);
        }
        return new AuthSessionCookie(rootAuthenticationSession, routeChanged);
    }

    public void removeAuthenticationSession(RealmModel realm, AuthenticationSessionModel authSession, boolean expireRestartCookie) {
        RootAuthenticationSessionModel rootAuthSession = authSession.getParentSession();

        log.debugf("Removing root authSession '%s'. Expire restart cookie: %b", rootAuthSession.getId(), expireRestartCookie);
        session.authenticationSessions().removeRootAuthenticationSession(realm, rootAuthSession);

        // expire restart cookie
        if (expireRestartCookie) {
            RestartLoginCookie.expireRestartCookie(session);

            // With browser session, this makes sure that info/error pages will be rendered correctly when locale is changed on them
            session.getProvider(LoginFormsProvider.class).setDetachedAuthSession();
        }
    }

    /**
     * Remove authentication session from root session. Possibly remove whole root authentication session if there are no other browser tabs
     *
     * @return true if whole root authentication session was removed. False just if single tab was removed
     */
    public boolean removeTabIdInAuthenticationSession(RealmModel realm, AuthenticationSessionModel authSession) {
        RootAuthenticationSessionModel rootAuthSession = authSession.getParentSession();
        rootAuthSession.removeAuthenticationSessionByTabId(authSession.getTabId());
        if (rootAuthSession.getAuthenticationSessions().isEmpty()) {
            // no more tabs, remove the session completely
            removeAuthenticationSession(realm, authSession, true);
            return true;
        } else {
            return false;
        }
    }

    /**
     * This happens when one browser tab successfully finished authentication (including required actions and consent screen if applicable)
     * Just authenticationSession of the current browser tab is removed from "root authentication session" and other tabs are kept, so
     * authentication can be automatically finished in other browser tabs (typically with authChecker.js javascript)
     */
    public void updateAuthenticationSessionAfterSuccessfulAuthentication(RealmModel realm, AuthenticationSessionModel authSession) {
        boolean removedRootAuthSession = removeTabIdInAuthenticationSession(realm, authSession);
        if (removedRootAuthSession) {
            return;
        }
        if (realm.getSsoSessionIdleTimeout() < SessionExpiration.getAuthSessionLifespan(realm) && realm.getSsoSessionMaxLifespan() < SessionExpiration.getAuthSessionLifespan(realm)) {
            removeAuthenticationSession(realm, authSession, true);
            return;
        }
        RootAuthenticationSessionModel rootAuthSession = authSession.getParentSession();

        // 1 minute by default. Same timeout, which is used for client to complete "authorization code" flow
        // Very short timeout should be OK as when this cookie is set, other existing browser tabs are supposed to be refreshed immediately by JS script authChecker.js
        // and login user automatically. No need to have authenticationSession and cookie living any longer
        int authSessionExpiresIn = realm.getAccessCodeLifespan();

        // Set timestamp to the past to make sure that authSession is scheduled for expiration in "authSessionExpiresIn" seconds
        int authSessionExpirationTime = Time.currentTime() - SessionExpiration.getAuthSessionLifespan(realm) + authSessionExpiresIn;
        rootAuthSession.setTimestamp(authSessionExpirationTime);

        log.tracef("Removed authentication session of root session '%s' with tabId '%s'. But there are remaining tabs in the root session. Root authentication session will expire in %d seconds", rootAuthSession.getId(), authSession.getTabId(), authSessionExpiresIn);
    }

    // Check to see if we already have authenticationSession with same ID
    public UserSessionModel getUserSession(AuthenticationSessionModel authSession) {
        return getUserSessionProvider().getUserSession(authSession.getRealm(), authSession.getParentSession().getId());
    }


    // Don't look at cookie. Just lookup authentication session based on the ID and client. Return null if not found
    public AuthenticationSessionModel getAuthenticationSessionByIdAndClient(RealmModel realm, String authSessionId, ClientModel client, String tabId) {
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, authSessionId);
        return rootAuthSession==null ? null : rootAuthSession.getAuthenticationSession(client, tabId);
    }

    public AuthenticationSessionModel getAuthenticationSessionByEncodedIdAndClient(RealmModel realm, String encodedAuthSessionId, ClientModel client, String tabId) {
        String decodedAuthSessionId = decodeBase64AndValidateSignature(encodedAuthSessionId);
        return decodedAuthSessionId==null ? null : getAuthenticationSessionByIdAndClient(realm, decodedAuthSessionId, client, tabId);
    }

    public UserSessionModel getUserSessionFromAuthenticationCookie(RealmModel realm) {
        AuthSessionCookie rootAuth = getAuthSessionCookies(realm);

        if (rootAuth == null) {
            // ideally, we should not rely on auth session id to retrieve user sessions
            // in case the auth session was removed, we fall back to the identity cookie
            // we are here doing the user session lookup twice, however the second lookup is going to make sure the
            // session exists in remote caches
            return getUserSessionFromIdentityCookie(realm);
        }

        // This will remove userSession "locally" if it doesn't exist on remoteCache
        UserSessionModel userSession = getUserSessionProvider().getUserSession(realm, rootAuth.sessionId());

        if (userSession != null) {
            reEncodeAuthSessionCookie(rootAuth);
        }
        return userSession;
    }

    private UserSessionModel getUserSessionFromIdentityCookie(RealmModel realm) {
        AuthenticationManager.AuthResult authResult = authenticateIdentityCookie(session, realm, true);
        if (authResult == null) {
            return null;
        }

        assert authResult.session() != null;

        // if we reach this point, the cookie is not found. Set it.
        setAuthSessionCookie(authResult.session().getId());
        return authResult.session();
    }

    private UserSessionProvider getUserSessionProvider() {
        return session.sessions();
    }

    record AuthSessionCookie(RootAuthenticationSessionModel rootSession, boolean routeChanged) {

        public String sessionId() {
            return rootSession.getId();
        }

    }
}
