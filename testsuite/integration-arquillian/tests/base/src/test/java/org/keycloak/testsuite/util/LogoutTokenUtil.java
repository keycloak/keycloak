package org.keycloak.testsuite.util;

import org.apache.http.entity.ContentType;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.representations.LogoutToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.UUID;

public class LogoutTokenUtil {

    public static String generateSignedLogoutToken(PrivateKey privateKey, String keyId,
            String issuer, String clientId, String userId, String sessionId, boolean revokeOfflineSessions)
            throws IOException {
        JWSHeader jwsHeader =
                new JWSHeader(Algorithm.RS256, OAuth2Constants.JWT, ContentType.APPLICATION_JSON.toString(), keyId);
        String logoutTokenHeaderEncoded = Base64Url.encode(JsonSerialization.writeValueAsBytes(jwsHeader));

        LogoutToken logoutToken = new LogoutToken();
        logoutToken.setSid(sessionId);
        logoutToken.putEvents(TokenUtil.TOKEN_BACKCHANNEL_LOGOUT_EVENT, new HashMap<>());
        logoutToken.putEvents(TokenUtil.TOKEN_BACKCHANNEL_LOGOUT_EVENT_REVOKE_OFFLINE_TOKENS, revokeOfflineSessions);
        logoutToken.setSubject(userId);
        logoutToken.issuer(issuer);
        logoutToken.id(UUID.randomUUID().toString());
        logoutToken.issuedNow();
        logoutToken.audience(clientId);

        String logoutTokenPayloadEncoded = Base64Url.encode(JsonSerialization.writeValueAsBytes(logoutToken));

        try {
            Signature signature = Signature.getInstance(JavaAlgorithm.RS256);
            signature.initSign(privateKey);
            String data = logoutTokenHeaderEncoded + "." + logoutTokenPayloadEncoded;
            byte[] dataByteArray = data.getBytes();
            signature.update(dataByteArray);
            byte[] signatureByteArray = signature.sign();
            return data + "." + Base64Url.encode(signatureByteArray);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return null;
        }
    }
}
