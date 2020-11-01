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
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ciba.CIBAAuthReqId;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.UserSessionCrossDCManager;
import org.keycloak.util.TokenUtil;

public class CIBAAuthReqIdParser {

    private static final Logger logger = Logger.getLogger(CIBAAuthReqIdParser.class);

    public static String persistAuthReqId(KeycloakSession session, CIBAAuthReqId authReqIdJwt) {
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, Algorithm.HS256);
        SignatureSignerContext signer = signatureProvider.signer();
        String encodedJwt = new JWSBuilder().type("JWT").jsonContent(authReqIdJwt).sign(signer);
        System.out.println("RRRRRRRRRR CIBAAuthReqIdParser.persistAuthReqId : JWS encodedJwt = " + encodedJwt);
        SecretKey aesKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, Algorithm.AES).getSecretKey();
        SecretKey hmacKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, Algorithm.HS256).getSecretKey();
        byte[] contentBytes = null;
        try {
            contentBytes = encodedJwt.getBytes("UTF-8");
            encodedJwt = TokenUtil.jweDirectEncode(aesKey, hmacKey, contentBytes);
        } catch (JWEException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.out.println("RRRRRRRRRR CIBAAuthReqIdParser.persistAuthReqId : JWE encodedJwt = " + encodedJwt);
        return encodedJwt;
    }

    private static CIBAAuthReqId getAuthReqIdJwt(KeycloakSession session, String encodedJwt) throws Exception {
        System.out.println("EEEEEEEEEE CIBAAuthReqIdParser.parseAuthReqId : JWE encodedJwt = " + encodedJwt);
        SecretKey aesKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, Algorithm.AES).getSecretKey();
        SecretKey hmacKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, Algorithm.HS256).getSecretKey();
        try {
            byte[] contentBytes = TokenUtil.jweDirectVerifyAndDecode(aesKey, hmacKey, encodedJwt);
            encodedJwt = new String(contentBytes, "UTF-8");
        } catch (JWEException | UnsupportedEncodingException e) {
            e.printStackTrace();
            throw e;
        }
        System.out.println("EEEEEEEEEE CIBAAuthReqIdParser.parseAuthReqId : JWS encodedJwt = " + encodedJwt);
        CIBAAuthReqId decodedJwt = session.tokens().decode(encodedJwt, CIBAAuthReqId.class);
        System.out.println("EEEEEEEEEE CIBAAuthReqIdParser.parseAuthReqId : decodedJwt.getExp() = " + decodedJwt.getExp());
        return decodedJwt;
    }

    public static ParseResult parseAuthReqId(KeycloakSession session, String encodedJwt, RealmModel realm, EventBuilder event) {
        CIBAAuthReqId authReqIdJwt = null;
        try {
            authReqIdJwt = getAuthReqIdJwt(session, encodedJwt);
        } catch (Exception e) {
            logger.info("illegal format of auth_req_id : e.getMessage() = " + e.getMessage());
            e.printStackTrace();
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
                    logger.info("too early access not waiting interval");
                    result.throttlingId = throttlingId;
                    return result.tooEarlyAccess();
                }
            }
        }

        // get corresponding Decoupled Authentication Result entry
        DecoupledAuthnResultParser.ParseResult parseAuthResult =  DecoupledAuthnResultParser.parseDecoupledAuthnResult(session, result.authReqIdJwt.getAuthResultId());
        if (parseAuthResult.isNotYetDecoupledAuthnResult()) {
            logger.info("not yet authenticated by Authentication Device or auth_req_id has already been used to get tokens");
            return result.userNotyetAuthenticatedOrAuthReqIdDuplicatedUse();
        }

        if (parseAuthResult.isExpiredDecoupledAuthnResult()) {
            logger.info("decoupled authentication expired");
            return result.expiredAuthentication();
        }

        String parseAuthResultStatus = parseAuthResult.decoupledAuthnResultData().getStatus();
        if (parseAuthResultStatus.equals(DecoupledAuthStatus.SUCCEEDED)) {
            logger.info("decoupled authentication succeeded");
        } else if (parseAuthResultStatus.equals(DecoupledAuthStatus.FAILED)) {
            logger.info("decoupled authentication failed");
            return result.failedAuthentication();
        } else if (parseAuthResultStatus.equals(DecoupledAuthStatus.CANCELLED)) {
            logger.info("decoupled authentication cancelled");
            return result.cancelledAuthentication();
        } else if (parseAuthResultStatus.equals(DecoupledAuthStatus.UNAUTHORIZED)) {
            logger.info("decoupled authentication unauthorized");
            return result.unauthorizedAuthentication();
        } else if (parseAuthResultStatus.equals(DecoupledAuthStatus.DIFFERENT)) {
            logger.info("decoupled authentication different user authenticated");
            return result.differentUserAuthenticated();
        } else {
            logger.info("decoupled authentication unknown event happened");
            return result.unknownEventHappendAuthentication();
        }

        String userSessionId = result.authReqIdJwt.getSessionState();
        String clientUUID = realm.getClientByClientId(result.authReqIdJwt.getIssuedFor()).getId();
        UserSessionModel userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, userSessionId, clientUUID);
        if (userSession == null) {
            // Needed to track if code is invalid or was already used.
            userSession = session.sessions().getUserSession(realm, userSessionId);
            if (userSession == null) {
                logger.warn("authenticated but corresponding user session not found");
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
