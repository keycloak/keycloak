package org.keycloak.protocol.ciba.utils;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;

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
import org.keycloak.models.CodeToTokenStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ciba.CIBAAuthReqIdJwt;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.UserSessionCrossDCManager;
import org.keycloak.util.TokenUtil;

public class CIBAAuthReqIdParser {

    private static final Logger logger = Logger.getLogger(CIBAAuthReqIdParser.class);

    public static String persistAuthReqId(KeycloakSession session, CIBAAuthReqId authReqIdData, int expires_in, UserModel user) {
        CodeToTokenStoreProvider codeStore = session.getProvider(CodeToTokenStoreProvider.class);
        UUID key = UUID.randomUUID();
        String jwtFormattedAuthReqId = createJwtFormattedAuthReqId(session, authReqIdData, expires_in, user, key);
        Map<String, String> serialized = authReqIdData.serializeCode();
        codeStore.put(key, expires_in, serialized);
        //return key.toString();
        return jwtFormattedAuthReqId;
    }

    private static String createJwtFormattedAuthReqId(KeycloakSession session, CIBAAuthReqId authReqIdData, int expires_in, UserModel user, UUID key) {
        CIBAAuthReqIdJwt jwt = new CIBAAuthReqIdJwt();
        jwt.id(KeycloakModelUtils.generateId());
        jwt.issuedNow();
        // TODO : check the way of generating issuer is appropriate or not
        jwt.issuer(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName()));
        jwt.audience(jwt.getIssuer());
        jwt.subject(user.getId());
        jwt.setKey(key.toString());
        jwt.exp(Long.valueOf(Time.currentTime() + expires_in));
        jwt.issuedFor(authReqIdData.getClientId());
        jwt.setScope(authReqIdData.getScope());
        jwt.setSessionState(authReqIdData.getUserSessionId());
        jwt.setAuthResultId(authReqIdData.getAuthResultId());
        jwt.setThrottlingId(authReqIdData.getThrottlingId());
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, Algorithm.HS256);
        SignatureSignerContext signer = signatureProvider.signer();
        String encodedJwt = new JWSBuilder().type("JWT").jsonContent(jwt).sign(signer);
        System.out.println("RRRRRRRRRR CIBAAuthReqIdParser.persistAuthReqId : JWS encodedJwt = " + encodedJwt);
        SecretKey aesKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, Algorithm.AES).getSecretKey();
        SecretKey hmacKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, Algorithm.HS256).getSecretKey();
        byte[] contentBytes = null;
        try {
            contentBytes = encodedJwt.getBytes("UTF-8");
            encodedJwt = TokenUtil.jweDirectEncode(aesKey, hmacKey, contentBytes);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JWEException e) {
            e.printStackTrace();
        }
        System.out.println("RRRRRRRRRR CIBAAuthReqIdParser.persistAuthReqId : JWE encodedJwt = " + encodedJwt);
        return encodedJwt;
    }

    private static String getAuthReqId(KeycloakSession session, String encodedJwt) {
        System.out.println("EEEEEEEEEE CIBAAuthReqIdParser.parseAuthReqId : JWE encodedJwt = " + encodedJwt);
        SecretKey aesKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, Algorithm.AES).getSecretKey();
        SecretKey hmacKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, Algorithm.HS256).getSecretKey();
        try {
            byte[] contentBytes = TokenUtil.jweDirectVerifyAndDecode(aesKey, hmacKey, encodedJwt);
            encodedJwt = new String(contentBytes, "UTF-8");
        } catch (JWEException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.out.println("EEEEEEEEEE CIBAAuthReqIdParser.parseAuthReqId : JWS encodedJwt = " + encodedJwt);
        CIBAAuthReqIdJwt decodedJwt = session.tokens().decode(encodedJwt, CIBAAuthReqIdJwt.class);
        System.out.println("EEEEEEEEEE CIBAAuthReqIdParser.parseAuthReqId : decodedJwt.getKey() = " + decodedJwt.getKey());
        return decodedJwt.getKey();
    }

    public static ParseResult parseAuthReqId(KeycloakSession session, String encodedJwt, RealmModel realm, EventBuilder event) {
        String authReqId = getAuthReqId(session, encodedJwt);
        ParseResult result = new ParseResult(authReqId);

        // Parse UUID
        UUID storeKeyUUID;
        try {
            storeKeyUUID = UUID.fromString(authReqId);
        } catch (IllegalArgumentException re) {
            logger.warn("Invalid format of the UUID in the code");
            return result.illegalAuthReqId();
        }

        // get Auth Req ID entry
        CodeToTokenStoreProvider authReqIdStore = session.getProvider(CodeToTokenStoreProvider.class);
        Map<String, String> authReqIdData = authReqIdStore.get(storeKeyUUID);

        // Either Auth Req ID not available or was already used
        if (authReqIdData == null) {
            logger.warnf("Auth Req Id '%s' has already been used.", storeKeyUUID);
            return result.illegalAuthReqId();
        }

        result.authReqIdData = CIBAAuthReqId.deserializeCode(authReqIdData);

        // Auth Req ID expiration check
        int currentTime = Time.currentTime();
        if (currentTime > result.authReqIdData.getExpiration()) {
            // remove Auth Req ID entry from store
            authReqIdStore.remove(storeKeyUUID);
            return result.expiredAuthReqId();
        }

        // too early access before interval
        String throttlingId = result.authReqIdData.getThrottlingId();
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
        DecoupledAuthnResultParser.ParseResult parseAuthResult =  DecoupledAuthnResultParser.parseDecoupledAuthnResult(session, result.authReqIdData.getAuthResultId());
        if (parseAuthResult.isNotYetDecoupledAuthnResult()) {
            logger.info("not yet authenticated by Authentication Device");
            return result.userNotyetAuthenticated();
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

        String userSessionId = result.authReqIdData.getUserSessionId();
        String clientUUID = realm.getClientByClientId(result.authReqIdData.getClientId()).getId();
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
        result.clientId = result.authReqIdData.getClientId();

        logger.tracef("Successfully verified Authe Req Id '%s'. User session: '%s', client: '%s'", storeKeyUUID, userSessionId, clientUUID);
        event.detail(Details.CODE_ID, userSessionId);
        event.session(userSessionId);

        // remove Auth Req ID entry from store
        authReqIdStore.remove(storeKeyUUID);

        return result;
    }

    public static boolean removeAuthReqId(KeycloakSession session, String authReqId, EventBuilder event) {
        // Parse UUID
        UUID codeUUID;
        try {
            codeUUID = UUID.fromString(authReqId);
        } catch (IllegalArgumentException re) {
            logger.warn("Invalid format of the UUID in the code");
            return false;
        }

        CodeToTokenStoreProvider authReqIdStore = session.getProvider(CodeToTokenStoreProvider.class);
        authReqIdStore.remove(codeUUID);

        return true;
    }

    public static class ParseResult {

        private final String authReqId;
        private CIBAAuthReqId authReqIdData;
        private String clientId;
        private AuthenticatedClientSessionModel clientSession;
        private String throttlingId;

        private boolean isIllegalAuthReqId = false;
        private boolean isExpiredAuthReqId = false;
        private boolean isUserNotyetAuthenticated = false;
        private boolean isExpiredAuthentication = false;
        private boolean isFailedAuthentication = false;
        private boolean isCancelledAuthentication = false;
        private boolean isUnauthorizedAuthentication = false;
        private boolean isUnknownEventHappendAuthentication = false;
        private boolean isUserSessionNotFound = false;
        private boolean isDifferentUserAuthenticated = false;
        private boolean isTooEarlyAccess = false;

        private ParseResult(String authReqId) {
            this.authReqId = authReqId;
        }

        public String getAuthReqId() {
            return authReqId;
        }

        public CIBAAuthReqId getAuthReqIdData() {
            return authReqIdData;
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
            return isUserNotyetAuthenticated;
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

        private ParseResult userNotyetAuthenticated() {
            this.isUserNotyetAuthenticated = true;
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
