package org.keycloak.jose.jws;

import org.keycloak.crypto.Algorithm;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;

public class TokenSignatureUtil {

    private static String DEFAULT_ALGORITHM_NAME = "RS256";

    public static String getCookieTokenSignatureAlgorithm(KeycloakSession session) {
        return Algorithm.HS256;
    }

    public static String getInitialAccessTokenSignatureAlgorithm(KeycloakSession session) {
        return getTokenSignatureAlgorithm(session, null);
    }

    public static String getAdminTokenSignatureAlgorithm(KeycloakSession session) {
        return getTokenSignatureAlgorithm(session, null);
    }

    public static String getRefreshTokenSignatureAlgorithm(KeycloakSession session) {
        return getTokenSignatureAlgorithm(session, null);
    }

    public static String getAccessTokenSignatureAlgorithm(KeycloakSession session) {
        return getTokenSignatureAlgorithm(session, OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG);
    }

    public static String getIdTokenSignatureAlgorithm(KeycloakSession session) {
        return getTokenSignatureAlgorithm(session, OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG);
    }

    public static String getUserinfoSignatureAlgorithm(KeycloakSession session) {
        return getTokenSignatureAlgorithm(session, OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG);
    }

    private static String getTokenSignatureAlgorithm(KeycloakSession session, String clientAttribute) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.getContext().getClient();

        String algorithm = client != null && clientAttribute != null ? client.getAttribute(clientAttribute) : null;
        if (algorithm != null && !algorithm.equals("")) {
            return algorithm;
        }

        algorithm = realm.getDefaultSignatureAlgorithm();
        if (algorithm != null && !algorithm.equals("")) {
            return algorithm;
        }

        return DEFAULT_ALGORITHM_NAME;
    }

}
