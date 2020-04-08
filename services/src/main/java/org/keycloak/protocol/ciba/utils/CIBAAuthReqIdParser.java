/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.ciba.utils;

import java.io.UnsupportedEncodingException;

import javax.crypto.SecretKey;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ciba.AuthenticationChannelStatus;
import org.keycloak.protocol.ciba.CIBAAuthReqId;
import org.keycloak.services.managers.UserSessionCrossDCManager;
import org.keycloak.util.TokenUtil;

public class CIBAAuthReqIdParser {

    private static final Logger logger = Logger.getLogger(CIBAAuthReqIdParser.class);

    public static String persistAuthReqId(KeycloakSession session, CIBAAuthReqId authReqIdJwt) {
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, Algorithm.HS256);
        SignatureSignerContext signer = signatureProvider.signer();
        String encodedJwt = new JWSBuilder().type("JWT").jsonContent(authReqIdJwt).sign(signer);
        SecretKey aesKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, Algorithm.AES).getSecretKey();
        SecretKey hmacKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, Algorithm.HS256).getSecretKey();
        byte[] contentBytes = null;
        try {
            contentBytes = encodedJwt.getBytes("UTF-8");
            encodedJwt = TokenUtil.jweDirectEncode(aesKey, hmacKey, contentBytes);
        } catch (JWEException | UnsupportedEncodingException e) {
            throw new RuntimeException("Error encoding auth_req_id.", e);
        }
        return encodedJwt;
    }

    public static CIBAAuthReqId getAuthReqIdJwt(KeycloakSession session, String encodedJwt) throws Exception {
        SecretKey aesKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, Algorithm.AES).getSecretKey();
        SecretKey hmacKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, Algorithm.HS256).getSecretKey();
        try {
            byte[] contentBytes = TokenUtil.jweDirectVerifyAndDecode(aesKey, hmacKey, encodedJwt);
            encodedJwt = new String(contentBytes, "UTF-8");
        } catch (JWEException | UnsupportedEncodingException e) {
            throw new RuntimeException("Error decoding auth_req_id.", e);
        }
        CIBAAuthReqId decodedJwt = session.tokens().decode(encodedJwt, CIBAAuthReqId.class);
        return decodedJwt;
    }

    public static ParseResult parseAuthReqId(KeycloakSession session, String encodedJwt, RealmModel realm, EventBuilder event) {
        CIBAAuthReqId authReqIdJwt = null;
        try {
            authReqIdJwt = getAuthReqIdJwt(session, encodedJwt);
        } catch (Exception e) {
            logger.warnf("illegal format of auth_req_id : e.getMessage() = %s", e.getMessage());
            return (new ParseResult(null)).illegalAuthReqId();
        }
        ParseResult result = new ParseResult(authReqIdJwt);

        // Auth Req ID expiration check
        if (Time.currentTime() > result.authReqIdJwt.getExp()) return result.expiredAuthReqId();

        // too early access before interval
        String throttlingId = result.authReqIdJwt.getThrottlingId();
        if (throttlingId != null) {
            EarlyAccessBlockerParser.ParseResult blocker = EarlyAccessBlockerParser.parseEarlyAccessBlocker(session, throttlingId);
            if (blocker != null) {
                if (blocker.isExpiredEarlyAccessBlocker()) {} // need to do first
                else if (blocker.isNotFoundEarlyAccessBlocker()) {}
                else {
                    logger.warnf("too early access not waiting interval : clientId = %s", result.authReqIdJwt.getIssuedFor());
                    result.throttlingId = throttlingId;
                    return result.tooEarlyAccess();
                }
            }
        }

        // get corresponding Authentication Channel Result entry
        AuthenticationChannelResultParser.ParseResult parseAuthResult =  AuthenticationChannelResultParser.parseAuthenticationChannelResult(session, result.authReqIdJwt.getAuthResultId());
        if (parseAuthResult.isNotYetAuthenticationChannelResult()) {
            logTrace("not yet authenticated by Authentication Device or auth_req_id has already been used to get tokens.", result);
            return result.userNotyetAuthenticatedOrAuthReqIdDuplicatedUse();
        }

        if (parseAuthResult.isExpiredAuthenticationChannelResult()) {
            logTrace("expired.", result);
            return result.expiredAuthentication();
        }

        String parseAuthResultStatus = parseAuthResult.authenticationChannelResultData().getStatus();
        if (parseAuthResultStatus.equals(AuthenticationChannelStatus.SUCCEEDED)) {
            logTrace("succeeded.", result);
        } else if (parseAuthResultStatus.equals(AuthenticationChannelStatus.FAILED)) {
            logTrace("failed.", result);
            return result.failedAuthentication();
        } else if (parseAuthResultStatus.equals(AuthenticationChannelStatus.CANCELLED)) {
            logTrace("cancelled.", result);
            return result.cancelledAuthentication();
        } else if (parseAuthResultStatus.equals(AuthenticationChannelStatus.UNAUTHORIZED)) {
            logTrace("unauthorized.", result);
            return result.unauthorizedAuthentication();
        } else if (parseAuthResultStatus.equals(AuthenticationChannelStatus.DIFFERENT)) {
            logTrace("different user authenticated.", result);
            return result.differentUserAuthenticated();
        } else {
            logTrace("unknown event happened.", result);
            return result.unknownEventHappendAuthentication();
        }

        String userSessionId = result.authReqIdJwt.getSessionState();
        String clientUUID = realm.getClientByClientId(result.authReqIdJwt.getIssuedFor()).getId();
        UserSessionModel userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, userSessionId, clientUUID);
        if (userSession == null) {
            // Needed to track if code is invalid or was already used.
            userSession = session.sessions().getUserSession(realm, userSessionId);
            if (userSession == null) {
                logger.warnf("authenticated but corresponding user session not found. clientId = %s, authResultId = %s", result.clientId, result.authReqIdJwt.getAuthResultId());
                return result.userSessionNotFound();
            }
        }

        result.clientSession = userSession.getAuthenticatedClientSessionByClient(clientUUID);
        result.clientId = result.authReqIdJwt.getIssuedFor();

        logger.tracef("Successfully verified Authe Req Id '%s'. User session: '%s', client: '%s'", encodedJwt, userSessionId, clientUUID);
        event.detail(Details.CODE_ID, userSessionId);
        event.session(userSessionId);

        return result;
    }

    private static void logTrace(String message, ParseResult result) {
        logger.tracef("CIBA Grant :: authentication channel %s clientId = %s, authResultId = %s", message, result.authReqIdJwt.getIssuedFor(), result.authReqIdJwt.getAuthResultId());
    }

    public static class ParseResult {

        private final CIBAAuthReqId authReqIdJwt;
        private String clientId;
        private AuthenticatedClientSessionModel clientSession;
        private String throttlingId;

        private boolean isIllegalAuthReqId = false;
        private boolean isExpiredAuthReqId = false;
        private boolean isUserNotyetAuthenticatedOrAuthReqIdDuplicatedUse = false;
        private boolean isExpiredAuthentication = false;
        private boolean isFailedAuthentication = false;
        private boolean isCancelledAuthentication = false;
        private boolean isUnauthorizedAuthentication = false;
        private boolean isUnknownEventHappendAuthentication = false;
        private boolean isUserSessionNotFound = false;
        private boolean isDifferentUserAuthenticated = false;
        private boolean isTooEarlyAccess = false;

        private ParseResult(CIBAAuthReqId authReqIdJwt) {
            this.authReqIdJwt = authReqIdJwt;
        }

        public CIBAAuthReqId getAuthReqIdData() {
            return authReqIdJwt;
        }

        public String getClientId() {
            return clientId;
        }

        public AuthenticatedClientSessionModel getClientSession() {
            return clientSession;
        }

        public String getThrottlingId() {
            return throttlingId;
        }

        public boolean isIllegalAuthReqId() {
            return isIllegalAuthReqId;
        }

        public boolean isExpiredAuthReqId() {
            return isExpiredAuthReqId;
        }

        public boolean isUserNotyetAuthenticated() {
            return isUserNotyetAuthenticatedOrAuthReqIdDuplicatedUse;
        }

        public boolean isExpiredAuthentication() {
            return isExpiredAuthentication;
        }

        public boolean isFailedAuthentication() {
            return isFailedAuthentication;
        }

        public boolean isCancelledAuthentication() {
            return isCancelledAuthentication;
        }

        public boolean isUnauthorizedAuthentication() {
            return isUnauthorizedAuthentication;
        }

        public boolean isUnknownEventHappendAuthentication() {
            return isUnknownEventHappendAuthentication;
        }

        public boolean isUserSessionNotFound() {
            return isUserSessionNotFound;
        }

        public boolean isDifferentUserAuthenticated() {
            return isDifferentUserAuthenticated;
        }

        public boolean isTooEarlyAccess() {
            return isTooEarlyAccess;
        }

        private ParseResult illegalAuthReqId() {
            this.isIllegalAuthReqId = true;
            return this;
        }

        private ParseResult expiredAuthReqId() {
            this.isExpiredAuthReqId = true;
            return this;
        }

        private ParseResult userNotyetAuthenticatedOrAuthReqIdDuplicatedUse() {
            this.isUserNotyetAuthenticatedOrAuthReqIdDuplicatedUse = true;
            return this;
        }

        private ParseResult expiredAuthentication() {
            this.isExpiredAuthentication = true;
            return this;
        }

        private ParseResult failedAuthentication() {
            this.isFailedAuthentication = true;
            return this;
        }

        private ParseResult cancelledAuthentication() {
            this.isCancelledAuthentication = true;
            return this;
        }

        private ParseResult unauthorizedAuthentication() {
            this.isUnauthorizedAuthentication = true;
            return this;
        }

        private ParseResult unknownEventHappendAuthentication() {
            this.isUnknownEventHappendAuthentication = true;
            return this;
        }

        private ParseResult userSessionNotFound() {
            this.isUserSessionNotFound = true;
            return this;
        }

        private ParseResult differentUserAuthenticated() {
            this.isDifferentUserAuthenticated = true;
            return this;
        }

        private ParseResult tooEarlyAccess() {
            this.isTooEarlyAccess = true;
            return this;
        }
    }
}
