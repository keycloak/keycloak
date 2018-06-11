package org.keycloak.jose.jws;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public class TokenSignatureUtil {
    public static final String REALM_SIGNATURE_ALGORITHM_KEY = "token.signed.response.alg";
    private static String DEFAULT_ALGORITHM_NAME = "RS256";

    public static String getTokenSignatureAlgorithm(KeycloakSession session, RealmModel realm, ClientModel client) {
        String realmSigAlgName = realm.getAttribute(REALM_SIGNATURE_ALGORITHM_KEY);
        String clientSigAlgname = null;
        if (client != null) clientSigAlgname = OIDCAdvancedConfigWrapper.fromClientModel(client).getIdTokenSignedResponseAlg();
        String sigAlgName = clientSigAlgname;
        if (sigAlgName == null) sigAlgName = (realmSigAlgName == null ? DEFAULT_ALGORITHM_NAME : realmSigAlgName);
        return sigAlgName;
    }
}
